import { createAlignedLatencyStream } from "../../src/utils/mediaLatency.js"

const assert = (condition, message) => {
  if (!condition) {
    throw new Error(message)
  }
}

class FakeMediaStream {
  constructor(tracks = []) {
    this.tracks = tracks
  }
  getVideoTracks() {
    return this.tracks.filter((track) => track.kind === "video")
  }
  getAudioTracks() {
    return this.tracks.filter((track) => track.kind === "audio")
  }
  getTracks() {
    return [...this.tracks]
  }
}

globalThis.MediaStream = FakeMediaStream

globalThis.window = {
  AudioContext: null,
  webkitAudioContext: null,
}

const stoppedTracks = []
const videoTrack = {
  kind: "video",
  id: "camera-track",
  readyState: "live",
  stop: () => stoppedTracks.push("video"),
}
const audioTrack = {
  kind: "audio",
  id: "microphone-track",
  readyState: "live",
  stop: () => stoppedTracks.push("audio"),
}

const stream = await createAlignedLatencyStream(new MediaStream([videoTrack, audioTrack]), 1000)

assert(stream.getVideoTracks()[0] === videoTrack, "BDD: 摄像头直播必须发布原始视频轨道，不能替换成 canvas 轨道")
assert(stream.getAudioTracks()[0] === audioTrack, "BDD: 没有 WebAudio 时音频应回退原始轨道")
assert(typeof stream.stopLatencyAlignment === "function", "BDD: 对齐流必须提供清理函数")

await stream.stopLatencyAlignment()
assert(!stoppedTracks.includes("video"), "BDD: 清理延迟对齐不能停止原始摄像头视频轨，否则观众端会停在最后一帧")
console.log("BDD PASS: browser live publishes continuous source video track")
