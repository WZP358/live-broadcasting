"""Test DeepFilterNet3 end-to-end inference."""
import sys
import time
import numpy as np
import torch

sys.path.insert(0, r'd:\code\live\ai-services\deepfilternet3\engine\DeepFilterNet')
from df.enhance import init_df, df_features
from df.utils import as_complex

MODEL_DIR = r'd:\code\live\ai-services\deepfilternet3\weights\DeepFilterNet3'

print("Loading DeepFilterNet3 model...")
model, df_state, _, _ = init_df(
    model_base_dir=MODEL_DIR,
    post_filter=False,
    log_level='WARNING',
    config_allow_defaults=True,
    epoch='best',
)
model.eval()
print(f"Model loaded. Device: {next(model.parameters()).device}")

# Generate test audio: 440Hz sine + Gaussian noise
sr = 48000
duration = 2.0
t = np.linspace(0, duration, int(sr * duration), endpoint=False)
audio = (0.5 * np.sin(2 * np.pi * 440 * t) + 0.08 * np.random.randn(len(t))).astype(np.float32)

# Process in 480-sample chunks (simulating real-time streaming)
chunk_size = 480
context_samples = chunk_size * 12
buffer = np.zeros((1, context_samples), dtype=np.float32)
output_chunks = []

t0 = time.perf_counter()
with torch.no_grad():
    for i in range(0, len(audio), chunk_size):
        chunk = audio[i:i + chunk_size]
        if len(chunk) < chunk_size:
            chunk = np.pad(chunk, (0, chunk_size - len(chunk)))

        buffer = np.roll(buffer, -chunk_size, axis=1)
        buffer[:, -chunk_size:] = chunk.reshape(1, -1)

        audio_tensor = torch.from_numpy(buffer.copy())
        spec, erb_feat, spec_feat = df_features(audio_tensor, df_state, model.nb_df, device='cpu')
        enhanced = model(spec.clone(), erb_feat, spec_feat)[0].cpu()
        enhanced = as_complex(enhanced.squeeze(1))
        out_audio = torch.as_tensor(df_state.synthesis(enhanced.numpy())).numpy()
        out = out_audio[:, -chunk_size:].reshape(-1)
        output_chunks.append(out[:min(chunk_size, len(audio) - i)])

result = np.concatenate(output_chunks)
elapsed = time.perf_counter() - t0
rtf = elapsed / duration

# Compute simple SNR improvement estimate
input_rms = np.sqrt(np.mean(audio ** 2))
output_rms = np.sqrt(np.mean(result ** 2))
noise_reduction_db = 20 * np.log10(input_rms / (output_rms + 1e-10))

print(f"Input shape:  {audio.shape}")
print(f"Output shape: {result.shape}")
print(f"Processing time: {elapsed:.3f}s (duration: {duration}s)")
print(f"RTF: {rtf:.4f} {'*** REAL-TIME ***' if rtf < 1.0 else '*** TOO SLOW ***'}")
print(f"Output range: [{result.min():.4f}, {result.max():.4f}]")
print(f"Noise reduction estimate: {noise_reduction_db:.1f} dB")

# Verify output is valid
assert result.shape[0] == audio.shape[0], "Output length mismatch"
assert np.isfinite(result).all(), "Output contains NaN/Inf"
assert rtf < 1.0, f"RTF {rtf:.2f} exceeds real-time threshold"
print("\n*** DeepFilterNet3 inference: ALL CHECKS PASSED ***")
