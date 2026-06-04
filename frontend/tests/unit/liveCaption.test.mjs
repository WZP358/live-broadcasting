import test from "node:test"
import assert from "node:assert/strict"

import { createLiveCaptionEngine, isLiveCaptionSupported } from "../../src/utils/liveCaption.js"

const createSourceStream = () => ({
  getAudioTracks: () => [{ kind: "audio", readyState: "live" }],
})

const createFakeAudioContext = (state = {}) => {
  class FakeAudioContext {
    constructor(options = {}) {
      this.options = options
      this.sampleRate = state.sampleRate || 48000
      this.state = "running"
      this.destination = { label: "destination" }
      state.context = this
    }

    createMediaStreamSource(stream) {
      state.sourceStream = stream
      return {
        connect: (node) => {
          state.sourceConnectedTo = node
        },
        disconnect: () => {
          state.sourceDisconnected = true
        },
      }
    }

    createScriptProcessor(size, inputChannels, outputChannels) {
      state.processorOptions = { size, inputChannels, outputChannels }
      state.processor = {
        connect: (node) => {
          state.processorConnectedTo = node
        },
        disconnect: () => {
          state.processorDisconnected = true
        },
        onaudioprocess: null,
      }
      return state.processor
    }

    createGain() {
      state.gain = {
        gain: { value: 1 },
        connect: (node) => {
          state.gainConnectedTo = node
        },
        disconnect: () => {
          state.gainDisconnected = true
        },
      }
      return state.gain
    }

    async close() {
      state.closed = true
    }
  }

  return FakeAudioContext
}

test("live caption uses subtitle service and forwards recognized text", async () => {
  const audioState = {}
  const sockets = []

  class FakeWebSocket {
    static OPEN = 1

    constructor(url) {
      this.url = url
      this.readyState = FakeWebSocket.OPEN
      this.sent = []
      sockets.push(this)
      setTimeout(() => this.onopen?.(), 0)
    }

    send(payload) {
      this.sent.push(payload)
    }

    close() {
      this.closed = true
    }
  }

  globalThis.window = {
    WebSocket: FakeWebSocket,
    AudioContext: createFakeAudioContext(audioState),
    webkitAudioContext: null,
    localStorage: { getItem: () => "" },
    setTimeout,
    clearTimeout,
  }

  const texts = []
  const engine = createLiveCaptionEngine({
    sourceStream: createSourceStream(),
    onText: (text) => texts.push(text),
  })

  assert.equal(isLiveCaptionSupported(), true)
  assert.equal(await engine.start(), "service")
  assert.equal(sockets.length, 1)
  assert.equal(JSON.parse(sockets[0].sent[0]).type, "reset")

  sockets[0].onmessage?.({ data: JSON.stringify({ type: "subtitle", text: "你好" }) })
  assert.deepEqual(texts, ["你好"])

  audioState.processor.onaudioprocess({
    inputBuffer: {
      getChannelData: () => new Float32Array([0.1, 0.2, 0.3, 0.4, 0.5, 0.6]),
    },
  })

  assert.ok(sockets[0].sent.some((payload) => payload instanceof ArrayBuffer))

  engine.stop()
  assert.equal(sockets[0].closed, true)
})

test("live caption reports unavailable when subtitle service is unavailable", async () => {
  let browserStarted = false
  let socket = null

  class FailingWebSocket {
    static OPEN = 1

    constructor() {
      socket = this
      setTimeout(() => this.onerror?.(new Error("unavailable")), 0)
    }

    close() {
      this.closed = true
    }
  }

  class FakeSpeechRecognition {
    start() {
      browserStarted = true
    }

    stop() {
      this.stopped = true
    }
  }

  globalThis.window = {
    WebSocket: FailingWebSocket,
    AudioContext: createFakeAudioContext({}),
    webkitAudioContext: null,
    SpeechRecognition: FakeSpeechRecognition,
    webkitSpeechRecognition: null,
    localStorage: { getItem: () => "" },
    setTimeout,
    clearTimeout,
  }

  const engine = createLiveCaptionEngine({
    sourceStream: createSourceStream(),
  })

  await assert.rejects(() => engine.start(), { code: "service-unavailable" })
  assert.equal(browserStarted, false)
  assert.equal(socket.closed, true)
})

test("live caption does not use browser recognition as a fallback", () => {
  class FakeSpeechRecognition {
    start() {
      throw new Error("browser recognition should not start")
    }
  }

  globalThis.window = {
    SpeechRecognition: FakeSpeechRecognition,
    webkitSpeechRecognition: null,
    localStorage: { getItem: () => "" },
    setTimeout,
    clearTimeout,
  }

  assert.equal(isLiveCaptionSupported(), false)
  assert.equal(createLiveCaptionEngine({ sourceStream: createSourceStream() }), null)
})
