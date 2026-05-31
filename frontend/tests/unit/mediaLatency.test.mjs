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

test("stopLatencyAlignment stops generated audio tracks without stopping source tracks", async () => {
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
  const generatedAudioTrack = {
    kind: "audio",
    id: "generated-delayed-audio",
    stop: () => stoppedTracks.push("generated-audio"),
  };
  const disconnectedNodes = [];

  class FakeAudioContext {
    constructor(options) {
      this.options = options;
      this.state = "running";
    }

    createMediaStreamSource(stream) {
      assert.equal(stream.getAudioTracks()[0], sourceAudioTrack);
      return {
        connect: () => {},
        disconnect: () => disconnectedNodes.push("source"),
      };
    }

    createDelay(maxDelayTime) {
      assert.equal(maxDelayTime, 1.25);
      return {
        delayTime: { value: 0 },
        connect: () => {},
        disconnect: () => disconnectedNodes.push("delay"),
      };
    }

    createMediaStreamDestination() {
      return {
        stream: new MediaStream([generatedAudioTrack]),
        disconnect: () => disconnectedNodes.push("destination"),
      };
    }

    async close() {
      disconnectedNodes.push("context");
    }
  }

  globalThis.window = {
    AudioContext: FakeAudioContext,
    webkitAudioContext: null,
  };

  const stream = await createAlignedLatencyStream(
    new MediaStream([sourceVideoTrack, sourceAudioTrack]),
    1000,
  );

  assert.equal(stream.getVideoTracks()[0], sourceVideoTrack);
  assert.equal(stream.getAudioTracks()[0], generatedAudioTrack);

  await stream.stopLatencyAlignment();

  assert.deepEqual(stoppedTracks, ["generated-audio"]);
  assert.deepEqual(disconnectedNodes, ["source", "delay", "destination", "context"]);
});
