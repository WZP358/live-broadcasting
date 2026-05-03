const DEFAULT_TARGET_LATENCY_MS = 1000;

const closeAudioNode = (node) => {
  try {
    node?.disconnect?.();
  } catch (error) {
    // ignore cleanup errors
  }
};

export const createAlignedLatencyStream = async (
  sourceStream,
  targetLatencyMs = DEFAULT_TARGET_LATENCY_MS,
) => {
  const cleanupTasks = [];
  const outputTracks = [];
  const targetLatencySeconds = Math.max(0, targetLatencyMs / 1000);

  const videoTrack = sourceStream.getVideoTracks()[0];
  if (videoTrack) {
    outputTracks.push(videoTrack);
  }

  const audioTrack = sourceStream.getAudioTracks()[0];
  if (audioTrack) {
    const AudioContextCtor = window.AudioContext || window.webkitAudioContext;
    if (AudioContextCtor) {
      const audioContext = new AudioContextCtor({
        latencyHint: targetLatencySeconds,
      });
      if (audioContext.state === "suspended") {
        await audioContext.resume();
      }
      const sourceNode = audioContext.createMediaStreamSource(
        new MediaStream([audioTrack]),
      );
      const delayNode = audioContext.createDelay(
        Math.max(1, targetLatencySeconds + 0.25),
      );
      const destinationNode = audioContext.createMediaStreamDestination();
      delayNode.delayTime.value = targetLatencySeconds;
      sourceNode.connect(delayNode);
      delayNode.connect(destinationNode);
      outputTracks.push(...destinationNode.stream.getAudioTracks());
      cleanupTasks.push(async () => {
        closeAudioNode(sourceNode);
        closeAudioNode(delayNode);
        closeAudioNode(destinationNode);
        await audioContext.close().catch(() => {});
      });
    } else {
      outputTracks.push(audioTrack);
    }
  }

  const stream = new MediaStream(outputTracks);
  stream.stopLatencyAlignment = async () => {
    await Promise.all(cleanupTasks.map((task) => task()));
  };
  return stream;
};
