export const createAlignedLatencyStream = async (
  sourceStream,
  targetLatencyMs = 0,
) => {
  const stream = new MediaStream(sourceStream.getTracks());
  stream.stopLatencyAlignment = async () => {
    // No derived tracks are created. Keeping the hook preserves the existing
    // cleanup contract for callers that switch publishing streams.
  };
  return stream;
};
