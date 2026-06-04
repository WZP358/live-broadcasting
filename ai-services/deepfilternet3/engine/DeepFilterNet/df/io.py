import os
import wave
from typing import Any, Dict, Optional, Tuple, Union

import numpy as np
import torch
import torchaudio as ta
from loguru import logger
from numpy import ndarray
from torch import Tensor

try:
    from torchaudio import AudioMetaData

    TA_RESAMPLE_SINC = "sinc_interp_hann"
    TA_RESAMPLE_KAISER = "sinc_interp_kaiser"
except ImportError:
    try:
        from torchaudio.backend.common import AudioMetaData
    except ImportError:
        from dataclasses import dataclass

        @dataclass
        class AudioMetaData:
            sample_rate: int
            num_frames: int
            num_channels: int
            bits_per_sample: int
            encoding: str

    TA_RESAMPLE_SINC = "sinc_interpolation"
    TA_RESAMPLE_KAISER = "kaiser_window"

from df.logger import warn_once
from df.utils import download_file, get_cache_dir, get_git_root


def _load_wav_with_wave(file: str) -> Tuple[Tensor, AudioMetaData]:
    with wave.open(file, "rb") as wf:
        sample_rate = wf.getframerate()
        num_frames = wf.getnframes()
        num_channels = wf.getnchannels()
        sample_width = wf.getsampwidth()
        frames = wf.readframes(num_frames)
    if sample_width == 1:
        audio = np.frombuffer(frames, dtype=np.uint8).astype(np.float32)
        audio = (audio - 128.0) / 128.0
    elif sample_width == 2:
        audio = np.frombuffer(frames, dtype=np.int16).astype(np.float32) / 32768.0
    elif sample_width == 4:
        audio = np.frombuffer(frames, dtype=np.int32).astype(np.float32) / 2147483648.0
    else:
        raise ValueError(f"Unsupported WAV sample width: {sample_width * 8} bits")
    audio = torch.from_numpy(audio.reshape(-1, num_channels).T.copy())
    info = AudioMetaData(
        sample_rate=sample_rate,
        num_frames=num_frames,
        num_channels=num_channels,
        bits_per_sample=sample_width * 8,
        encoding="PCM",
    )
    return audio, info


def _save_wav_with_wave(file: str, audio: Tensor, sr: int) -> None:
    sr = int(sr)
    audio = audio.detach().cpu()
    if audio.ndim == 1:
        audio = audio.unsqueeze(0)
    audio = torch.clamp(audio, -1.0, 1.0)
    pcm = (audio.T.contiguous().numpy() * 32767.0).astype(np.int16)
    with wave.open(file, "wb") as wf:
        wf.setnchannels(audio.shape[0])
        wf.setsampwidth(2)
        wf.setframerate(sr)
        wf.writeframes(pcm.tobytes())


def load_audio(
    file: str, sr: Optional[int] = None, verbose=True, **kwargs
) -> Tuple[Tensor, AudioMetaData]:
    """Loads an audio file using torchaudio.

    Args:
        file (str): Path to an audio file.
        sr (int): Optionally resample audio to specified target sampling rate.
        **kwargs: Passed to torchaudio.load(). Depends on the backend. The resample method
            may be set via `method` which is passed to `resample()`.

    Returns:
        audio (Tensor): Audio tensor of shape [C, T], if channels_first=True (default).
        info (AudioMetaData): Meta data of the original audio file. Contains the original sr.
    """
    ikwargs = {}
    if "format" in kwargs:
        ikwargs["format"] = kwargs["format"]
    rkwargs = {}
    if "method" in kwargs:
        rkwargs["method"] = kwargs.pop("method")
    info = None
    if hasattr(ta, "info"):
        try:
            info = ta.info(file, **ikwargs)
        except Exception:
            info = None
    if info is None:
        audio, info = _load_wav_with_wave(file)
    else:
        if "num_frames" in kwargs and sr is not None:
            kwargs["num_frames"] *= info.sample_rate // sr
        try:
            audio, orig_sr = ta.load(file, **kwargs)
        except Exception:
            audio, wave_info = _load_wav_with_wave(file)
            orig_sr = wave_info.sample_rate
            info = wave_info
        else:
            info = info
    orig_sr = info.sample_rate if info is not None else orig_sr
    if sr is not None and orig_sr != sr:
        if verbose:
            warn_once(
                f"Audio sampling rate does not match model sampling rate ({orig_sr}, {sr}). "
                "Resampling..."
            )
        audio = resample(audio, orig_sr, sr, **rkwargs)
    return audio.contiguous(), info


def save_audio(
    file: str,
    audio: Union[Tensor, ndarray],
    sr: int,
    output_dir: Optional[str] = None,
    suffix: Optional[str] = None,
    log: bool = False,
    dtype=torch.int16,
):
    sr = int(sr)
    outpath = file
    if suffix is not None:
        file, ext = os.path.splitext(file)
        outpath = file + f"_{suffix}" + ext
    if output_dir is not None:
        outpath = os.path.join(output_dir, os.path.basename(outpath))
    if log:
        logger.info(f"Saving audio file '{outpath}'")
    audio = torch.as_tensor(audio)
    if audio.ndim == 1:
        audio.unsqueeze_(0)
    if dtype == torch.int16 and audio.dtype != torch.int16:
        audio = (audio * (1 << 15)).to(torch.int16)
    if dtype == torch.float32 and audio.dtype != torch.float32:
        audio = audio.to(torch.float32) / (1 << 15)
    try:
        ta.save(outpath, audio, sr)
    except Exception:
        if dtype == torch.int16:
            audio = audio.to(torch.float32) / (1 << 15)
        _save_wav_with_wave(outpath, audio, sr)


try:
    from torchaudio.functional import resample as ta_resample
except ImportError:
    from torchaudio.compliance.kaldi import resample_waveform as ta_resample  # type: ignore


def get_resample_params(method: str) -> Dict[str, Any]:
    params = {
        "sinc_fast": {"resampling_method": TA_RESAMPLE_SINC, "lowpass_filter_width": 16},
        "sinc_best": {"resampling_method": TA_RESAMPLE_SINC, "lowpass_filter_width": 64},
        "kaiser_fast": {
            "resampling_method": TA_RESAMPLE_KAISER,
            "lowpass_filter_width": 16,
            "rolloff": 0.85,
            "beta": 8.555504641634386,
        },
        "kaiser_best": {
            "resampling_method": TA_RESAMPLE_KAISER,
            "lowpass_filter_width": 16,
            "rolloff": 0.9475937167399596,
            "beta": 14.769656459379492,
        },
    }
    assert method in params.keys(), f"method must be one of {list(params.keys())}"
    return params[method]


def resample(audio: Tensor, orig_sr: int, new_sr: int, method="sinc_fast"):
    params = get_resample_params(method)
    return ta_resample(audio, orig_sr, new_sr, **params)


def get_test_sample(sr: int = 48000) -> Tensor:
    dir = get_git_root()
    file_path = os.path.join("assets", "clean_freesound_33711.wav")
    if dir is None:
        url = "https://github.com/Rikorose/DeepFilterNet/raw/main/" + file_path
        save_dir = get_cache_dir()
        path = download_file(url, save_dir)
    else:
        path = os.path.join(dir, file_path)
    sample, _ = load_audio(path, sr=sr)
    return sample
