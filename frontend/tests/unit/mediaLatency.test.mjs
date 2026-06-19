import test from "node:test";
import assert from "node:assert/strict";

import { createAlignedLatencyStream } from "../../src/utils/mediaLatency.js";

class FakeMediaStream {
  constructor(tracks = []) {
    this.tracks = tracks;
  }

  getVideoTracks() {
    return this.tracks.filter((track) => track.kind === "video");
  }

  getAudioTracks() {
    return this.tracks.filter((track) => track.kind === "audio");
  }

  getTracks() {
    return [...this.tracks];
  }
}

globalThis.MediaStream = FakeMediaStream;

test("latency alignment keeps source tracks and cleanup does not stop capture", async () => {
  const stoppedTracks = [];
  const sourceVideoTrack = {
    kind: "video",
    id: "source-camera",
    stop: () => stoppedTracks.push("source-video"),
  };
  const sourceAudioTrack = {
    kind: "audio",
    id: "source-microphone",
    stop: () => stoppedTracks.push("source-audio"),
  };

  const stream = await createAlignedLatencyStream(
    new MediaStream([sourceVideoTrack, sourceAudioTrack]),
    1000,
  );

  assert.equal(stream.getVideoTracks()[0], sourceVideoTrack);
  assert.equal(stream.getAudioTracks()[0], sourceAudioTrack);

  await stream.stopLatencyAlignment();

  assert.deepEqual(stoppedTracks, []);
});
