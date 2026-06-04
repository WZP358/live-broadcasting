import argparse
import asyncio
import json
import sys
import threading
import time
from pathlib import Path

import numpy as np
import torch
from scipy.signal import resample_poly
from websockets.asyncio.server import serve


ROOT = Path(__file__).resolve().parents[1]
DF_REPO_ROOT = ROOT / "engine"
DF_PROJECT_DIR = DF_REPO_ROOT / "DeepFilterNet"
DF_MODEL_DIR = ROOT / "weights" / "DeepFilterNet3"
DEFAULT_PORT = 18765
DEFAULT_CONTEXT_BLOCKS = 12

if str(DF_PROJECT_DIR) not in sys.path:
    sys.path.insert(0, str(DF_PROJECT_DIR))

from df.enhance import df_features, init_df  # noqa: E402
from df.model import ModelParams  # noqa: E402
from df.utils import as_complex  # noqa: E402


class DeepFilterNetServiceError(RuntimeError):
    pass


class DeepFilterNet3Runtime:
    def __init__(self, source_rate: int, chunk_samples: int, context_blocks: int):
        if not DF_MODEL_DIR.exists():
            raise DeepFilterNetServiceError(f"DeepFilterNet3 model directory not found: {DF_MODEL_DIR}")
        self.source_rate = source_rate
        self.chunk_samples = chunk_samples
        self.context_blocks = context_blocks
        self.model, self.df_state, _, _ = init_df(
            model_base_dir=str(DF_MODEL_DIR),
            post_filter=False,
            log_level="INFO",
            log_file=None,
            config_allow_defaults=True,
            epoch="best",
        )
        self.model.eval()
        self.device = next(self.model.parameters()).device
        self.params = ModelParams()
        self.model_rate = self.params.sr
        self.lock = threading.Lock()
        self.context_samples = max(self.chunk_samples * self.context_blocks, self.chunk_samples)
        self.audio_buffer = np.zeros((1, self.context_samples), dtype=np.float32)
        self.last_rtf = 0.0

    def ready_payload(self):
        return {
            "type": "ready",
            "backend": "deepfilternet3-websocket",
            "modelName": "DeepFilterNet3",
            "modelSampleRate": self.model_rate,
            "frameSize": self.chunk_samples,
        }

    def _to_model_rate(self, chunk: np.ndarray) -> np.ndarray:
        if self.source_rate == self.model_rate:
            return chunk.astype(np.float32, copy=False)
        return resample_poly(chunk, self.model_rate, self.source_rate).astype(np.float32)

    def _to_source_rate(self, chunk: np.ndarray) -> np.ndarray:
        if self.source_rate == self.model_rate:
            return chunk.astype(np.float32, copy=False)
        return resample_poly(chunk, self.source_rate, self.model_rate).astype(np.float32)

    @torch.no_grad()
    def enhance(self, chunk: np.ndarray) -> np.ndarray:
        if chunk.size == 0:
            return chunk.astype(np.float32)
        normalized = np.clip(chunk.astype(np.float32, copy=False), -1.0, 1.0)
        model_chunk = self._to_model_rate(normalized)
        with self.lock:
            self.audio_buffer = np.roll(self.audio_buffer, -model_chunk.shape[0], axis=1)
            self.audio_buffer[:, -model_chunk.shape[0] :] = model_chunk.reshape(1, -1)
            audio = torch.from_numpy(self.audio_buffer.copy())
            t0 = time.perf_counter()
            spec, erb_feat, spec_feat = df_features(
                audio, self.df_state, self.model.nb_df, device=self.device
            )
            enhanced = self.model(spec.clone(), erb_feat, spec_feat)[0].cpu()
            enhanced = as_complex(enhanced.squeeze(1))
            out_audio = torch.as_tensor(self.df_state.synthesis(enhanced.numpy())).numpy()
            self.last_rtf = (time.perf_counter() - t0) / max(model_chunk.shape[0] / self.model_rate, 1e-6)
            out = out_audio[:, -model_chunk.shape[0] :].reshape(-1)
        out = self._to_source_rate(out)
        if out.shape[0] > normalized.shape[0]:
            out = out[: normalized.shape[0]]
        elif out.shape[0] < normalized.shape[0]:
            out = np.pad(out, (0, normalized.shape[0] - out.shape[0]))
        return np.clip(out, -1.0, 1.0).astype(np.float32, copy=False)


class SessionState:
    def __init__(self):
        self.sample_rate = None
        self.channels = 1
        self.chunk_samples = None
        self.runtime = None

    def configured(self) -> bool:
        return bool(self.sample_rate and self.channels == 1 and self.chunk_samples)


async def send_json(websocket, payload):
    await websocket.send(json.dumps(payload, ensure_ascii=False))


async def handle_connection(websocket, context_blocks: int):
    session = SessionState()
    print(f"[df3] client connected: {websocket.remote_address}", flush=True)
    try:
        async for message in websocket:
            if isinstance(message, str):
                payload = json.loads(message)
                if payload.get("type") != "config":
                    await send_json(websocket, {"type": "error", "message": "Missing config message."})
                    continue
                session.sample_rate = int(payload.get("sampleRate") or 0)
                session.channels = int(payload.get("channels") or 1)
                session.chunk_samples = int(payload.get("chunkSamples") or 0)
                if not session.configured():
                    await send_json(websocket, {"type": "error", "message": "Invalid denoise config."})
                    continue
                try:
                    session.runtime = await asyncio.to_thread(
                        DeepFilterNet3Runtime,
                        session.sample_rate,
                        session.chunk_samples,
                        context_blocks,
                    )
                except Exception as error:
                    await send_json(websocket, {"type": "error", "message": str(error)})
                    continue
                await send_json(websocket, session.runtime.ready_payload())
                continue

            if not session.configured() or session.runtime is None:
                await send_json(websocket, {"type": "error", "message": "Please send config first."})
                continue

            chunk = np.frombuffer(message, dtype=np.float32).copy()
            enhanced = await asyncio.to_thread(session.runtime.enhance, chunk)
            await websocket.send(enhanced.tobytes())
    except Exception as error:
        print(f"[df3] client error: {error}", flush=True)
    finally:
        print(f"[df3] client disconnected: {websocket.remote_address}", flush=True)


async def main():
    parser = argparse.ArgumentParser(description="Realtime denoise bridge powered by DeepFilterNet3")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT)
    parser.add_argument("--context-blocks", type=int, default=DEFAULT_CONTEXT_BLOCKS)
    args = parser.parse_args()

    print(f"[df3] websocket service starting at ws://{args.host}:{args.port}/ws", flush=True)
    async with serve(
        lambda websocket: handle_connection(websocket, args.context_blocks),
        args.host,
        args.port,
        ping_interval=10,
        ping_timeout=20,
        max_size=2**22,
    ):
        await asyncio.Future()


if __name__ == "__main__":
    asyncio.run(main())
