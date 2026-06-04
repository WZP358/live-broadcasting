import argparse
import os
import queue
import sys
import threading
import time
from dataclasses import dataclass
from typing import Optional

import numpy as np
import torch


ROOT = os.path.dirname(os.path.abspath(__file__))
PROJECT_DIR = os.path.join(ROOT, "DeepFilterNet")
MODEL_DIR = os.path.join(ROOT, "models", "DeepFilterNet3")
VENDOR_DIR = os.path.join(ROOT, ".vendor")
if PROJECT_DIR not in sys.path:
    sys.path.insert(0, PROJECT_DIR)
if os.path.isdir(VENDOR_DIR):
    vendor_entries = []
    for name in os.listdir(VENDOR_DIR):
        path = os.path.join(VENDOR_DIR, name)
        if os.path.isdir(path):
            vendor_entries.append(path)
    for name in os.listdir(VENDOR_DIR):
        path = os.path.join(VENDOR_DIR, name)
        if path.endswith(".whl"):
            vendor_entries.append(path)
    for path in reversed(vendor_entries):
        sys.path.insert(0, path)

from df.enhance import df_features, init_df  # noqa: E402
from df.model import ModelParams  # noqa: E402
from df.utils import as_complex  # noqa: E402


def load_sound_backend():
    try:
        import sounddevice as sd
    except ImportError as e:
        raise SystemExit(
            "Missing dependency: sounddevice\n"
            "Install with: python -m pip install sounddevice"
        ) from e
    return sd


def parse_device(value: Optional[str]) -> Optional[int]:
    if value is None:
        return None
    value = str(value).strip()
    if not value:
        return None
    return int(value)


def to_bool_text(flag: bool) -> str:
    return "on" if flag else "off"


@dataclass
class StreamConfig:
    input_device: Optional[int]
    output_device: Optional[int]
    samplerate: int
    channels: int
    blocksize: int
    context_blocks: int
    atten_lim: Optional[float]
    post_filter: bool
    no_df_stage: bool


class RealTimeDf3:
    def __init__(self, cfg: StreamConfig):
        self.cfg = cfg
        self.model, self.df_state, _, _ = init_df(
            model_base_dir=MODEL_DIR,
            post_filter=cfg.post_filter,
            log_level="INFO",
            log_file=None,
            config_allow_defaults=True,
            epoch="best",
            mask_only=cfg.no_df_stage,
        )
        self.model.eval()
        self.device = next(self.model.parameters()).device
        self.params = ModelParams()
        self.hop = self.params.hop_size
        self.sr = self.params.sr
        if cfg.samplerate != self.sr:
            raise ValueError(f"DeepFilterNet3 requires {self.sr} Hz, got {cfg.samplerate}")
        if cfg.blocksize % self.hop != 0:
            raise ValueError(f"blocksize must be a multiple of hop_size ({self.hop})")
        self.block_hops = cfg.blocksize // self.hop
        self.context_samples = cfg.context_blocks * cfg.blocksize
        self.audio_buffer = np.zeros((1, self.context_samples), dtype=np.float32)
        self.lock = threading.Lock()
        self.last_rtf = 0.0
        self.frames_processed = 0

    @torch.no_grad()
    def process_block(self, in_block: np.ndarray) -> np.ndarray:
        mono = np.asarray(in_block, dtype=np.float32)
        if mono.ndim == 2:
            mono = mono.mean(axis=1)
        mono = mono.reshape(1, -1)
        with self.lock:
            self.audio_buffer = np.roll(self.audio_buffer, -mono.shape[1], axis=1)
            self.audio_buffer[:, -mono.shape[1] :] = mono
            audio = torch.from_numpy(self.audio_buffer.copy())
            t0 = time.perf_counter()
            spec, erb_feat, spec_feat = df_features(
                audio, self.df_state, self.model.nb_df, device=self.device
            )
            enhanced = self.model(spec.clone(), erb_feat, spec_feat)[0].cpu()
            enhanced = as_complex(enhanced.squeeze(1))
            if self.cfg.atten_lim is not None and abs(self.cfg.atten_lim) > 0:
                lim = 10 ** (-abs(self.cfg.atten_lim) / 20)
                enhanced = as_complex(spec.squeeze(1).cpu()) * lim + enhanced * (1 - lim)
            out_audio = torch.as_tensor(self.df_state.synthesis(enhanced.numpy())).numpy()
            dt = time.perf_counter() - t0
            block_seconds = mono.shape[1] / self.sr
            self.last_rtf = dt / block_seconds if block_seconds > 0 else 0.0
            self.frames_processed += mono.shape[1]
            out = out_audio[:, -mono.shape[1] :].T.astype(np.float32, copy=False)
        return np.clip(out, -1.0, 1.0)


def print_devices():
    sd = load_sound_backend()
    devices = sd.query_devices()
    print("Available audio devices:")
    for idx, dev in enumerate(devices):
        hostapi = sd.query_hostapis(dev["hostapi"])["name"]
        print(
            f"[{idx}] {dev['name']} | host={hostapi} | "
            f"in={dev['max_input_channels']} | out={dev['max_output_channels']} | "
            f"default_sr={int(dev['default_samplerate'])}"
        )


def _format_device(sd, idx: int) -> str:
    dev = sd.query_devices(idx)
    hostapi = sd.query_hostapis(dev["hostapi"])["name"]
    return (
        f"[{idx}] {dev['name']} | host={hostapi} | "
        f"in={dev['max_input_channels']} | out={dev['max_output_channels']} | "
        f"default_sr={int(dev['default_samplerate'])}"
    )


def validate_devices(sd, cfg: StreamConfig):
    devices = sd.query_devices()
    n = len(devices)
    if cfg.input_device is None or not (0 <= cfg.input_device < n):
        raise SystemExit(f"Invalid input device index: {cfg.input_device}")
    if cfg.output_device is None or not (0 <= cfg.output_device < n):
        raise SystemExit(f"Invalid output device index: {cfg.output_device}")
    in_dev = devices[cfg.input_device]
    out_dev = devices[cfg.output_device]
    if in_dev["max_input_channels"] < cfg.channels:
        raise SystemExit(
            "Selected input device cannot capture the requested channels.\n"
            f"input={_format_device(sd, cfg.input_device)}"
        )
    if out_dev["max_output_channels"] < cfg.channels:
        raise SystemExit(
            "Selected output device cannot play the requested channels.\n"
            f"output={_format_device(sd, cfg.output_device)}"
        )
    if cfg.input_device == cfg.output_device:
        raise SystemExit(
            "Input and output devices should not be the same index unless that device truly supports both.\n"
            f"input={_format_device(sd, cfg.input_device)}\n"
            f"output={_format_device(sd, cfg.output_device)}"
        )


def run_stream(cfg: StreamConfig):
    sd = load_sound_backend()
    validate_devices(sd, cfg)
    bridge = RealTimeDf3(cfg)
    status_q: "queue.Queue[str]" = queue.Queue()

    def callback(indata, outdata, frames, time_info, status):
        if status:
            status_q.put(str(status))
        if frames != cfg.blocksize:
            block = np.zeros((cfg.blocksize, cfg.channels), dtype=np.float32)
            n = min(frames, cfg.blocksize)
            block[:n] = indata[:n]
            out = bridge.process_block(block)
            outdata.fill(0)
            outdata[:n, 0] = out[:n, 0]
            return
        out = bridge.process_block(indata[:, :1])
        outdata[:, 0] = out[:, 0]

    try:
        stream = sd.Stream(
            samplerate=cfg.samplerate,
            blocksize=cfg.blocksize,
            device=(cfg.input_device, cfg.output_device),
            channels=(cfg.channels, cfg.channels),
            dtype="float32",
            callback=callback,
            latency="low",
        )
    except Exception as e:
        raise SystemExit(
            "Failed to open real-time audio stream.\n"
            f"input={_format_device(sd, cfg.input_device)}\n"
            f"output={_format_device(sd, cfg.output_device)}\n"
            f"error={e}"
        ) from e
    print("Starting DeepFilterNet3 real-time bridge")
    print(f"input_device={cfg.input_device} output_device={cfg.output_device}")
    print(
        f"samplerate={cfg.samplerate} blocksize={cfg.blocksize} "
        f"context_blocks={cfg.context_blocks} atten_lim={cfg.atten_lim}"
    )
    print(
        f"post_filter={to_bool_text(cfg.post_filter)} no_df_stage={to_bool_text(cfg.no_df_stage)}"
    )
    print("Press Ctrl+C to stop.")
    with stream:
        last_log = time.time()
        while True:
            try:
                msg = status_q.get_nowait()
                print(f"[audio] {msg}")
            except queue.Empty:
                pass
            now = time.time()
            if now - last_log >= 3:
                seconds = bridge.frames_processed / bridge.sr
                print(f"[stats] processed={seconds:.1f}s rtf={bridge.last_rtf:.3f}")
                last_log = now
            time.sleep(0.1)


def build_parser():
    parser = argparse.ArgumentParser(
        description="Real-time DeepFilterNet3 bridge for Windows live streaming."
    )
    parser.add_argument("--list-devices", action="store_true", help="List audio devices and exit.")
    parser.add_argument("--input-device", type=str, help="Input device index.")
    parser.add_argument("--output-device", type=str, help="Output device index.")
    parser.add_argument("--samplerate", type=int, default=48000, help="Must be 48000 for DF3.")
    parser.add_argument("--channels", type=int, default=1, help="Use mono for streaming.")
    parser.add_argument(
        "--blocksize",
        type=int,
        default=480,
        help="Audio block size in samples. Must be a multiple of 480.",
    )
    parser.add_argument(
        "--context-blocks",
        type=int,
        default=20,
        help="How many blocks to keep as context. Larger is more stable, smaller is lower latency.",
    )
    parser.add_argument(
        "--atten-lim",
        type=float,
        default=None,
        help="Optional attenuation limit in dB.",
    )
    parser.add_argument("--pf", action="store_true", help="Enable post-filter.")
    parser.add_argument("--no-df-stage", action="store_true", help="Disable DF stage for testing.")
    return parser


def main():
    parser = build_parser()
    args = parser.parse_args()
    if args.list_devices:
        print_devices()
        return
    cfg = StreamConfig(
        input_device=parse_device(args.input_device),
        output_device=parse_device(args.output_device),
        samplerate=args.samplerate,
        channels=args.channels,
        blocksize=args.blocksize,
        context_blocks=max(4, args.context_blocks),
        atten_lim=args.atten_lim,
        post_filter=args.pf,
        no_df_stage=args.no_df_stage,
    )
    if cfg.input_device is None or cfg.output_device is None:
        raise SystemExit("Please provide both --input-device and --output-device.")
    run_stream(cfg)


if __name__ == "__main__":
    main()
