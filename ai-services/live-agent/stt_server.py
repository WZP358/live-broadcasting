"""
PulseLive 实时语音转字幕服务 (Whisper STT)

基于 OpenAI Whisper 模型，将直播音频实时转为中文字幕。
通过 WebSocket 接收音频块，返回识别文本。
"""
import os
import json
import asyncio
import argparse
import numpy as np
from pathlib import Path

import torch
import whisper
from websockets.asyncio.server import serve

BASE_DIR = Path(__file__).resolve().parent
MODEL_NAME = os.environ.get("PULSELIVE_WHISPER_MODEL", "base")
DEFAULT_PORT = 8200

print(f"[STT] Loading Whisper model: {MODEL_NAME}...", flush=True)
model = whisper.load_model(MODEL_NAME)
print(f"[STT] Model loaded. Device: {next(model.parameters()).device}", flush=True)

# 音频缓冲区：累积到一定长度后推理
audio_buffer = {}
BUFFER_SECONDS = 3  # 每3秒识别一次
SAMPLE_RATE = 16000


async def handle_stt(websocket):
    """WebSocket STT 处理：接收 float32 PCM 音频，返回识别文本。"""
    peer = websocket.remote_address
    print(f"[STT] Client connected: {peer}", flush=True)
    audio_buffer[peer] = np.array([], dtype=np.float32)

    try:
        async for message in websocket:
            if isinstance(message, str):
                # 控制消息
                try:
                    cmd = json.loads(message)
                    if cmd.get("type") == "reset":
                        audio_buffer[peer] = np.array([], dtype=np.float32)
                        await websocket.send(json.dumps({"type": "reset_ok"}))
                except json.JSONDecodeError:
                    pass
                continue

            # 音频数据 (float32 PCM)
            chunk = np.frombuffer(message, dtype=np.float32).copy()
            audio_buffer[peer] = np.concatenate([audio_buffer[peer], chunk])

            # 累积足够时长后推理
            if len(audio_buffer[peer]) >= SAMPLE_RATE * BUFFER_SECONDS:
                audio = audio_buffer[peer][:SAMPLE_RATE * BUFFER_SECONDS]
                audio_buffer[peer] = audio_buffer[peer][SAMPLE_RATE * BUFFER_SECONDS:]

                # 异步转文字
                result = await asyncio.to_thread(transcribe, audio)
                if result:
                    await websocket.send(json.dumps({
                        "type": "subtitle",
                        "text": result,
                        "language": "zh",
                    }, ensure_ascii=False))

    except Exception as e:
        print(f"[STT] Error: {e}", flush=True)
    finally:
        audio_buffer.pop(peer, None)
        print(f"[STT] Client disconnected: {peer}", flush=True)


def transcribe(audio: np.ndarray) -> str:
    """Whisper 推理，返回识别文本。"""
    try:
        # 确保音频格式正确
        audio = audio.astype(np.float32)
        if np.abs(audio).max() < 0.001:
            return ""  # 静音跳过

        result = model.transcribe(
            audio,
            language="zh",
            task="transcribe",
            fp16=False,
            no_speech_threshold=0.6,
        )
        text = result.get("text", "").strip()
        return text
    except Exception as e:
        print(f"[STT] Transcribe error: {e}", flush=True)
        return ""


async def main():
    parser = argparse.ArgumentParser(description="Whisper STT service")
    parser.add_argument("--host", default="127.0.0.1")
    parser.add_argument("--port", type=int, default=DEFAULT_PORT)
    args = parser.parse_args()

    print(f"[STT] WebSocket service at ws://{args.host}:{args.port}/ws", flush=True)
    async with serve(handle_stt, args.host, args.port, ping_interval=10, ping_timeout=20, max_size=2**22):
        await asyncio.Future()


if __name__ == "__main__":
    asyncio.run(main())
