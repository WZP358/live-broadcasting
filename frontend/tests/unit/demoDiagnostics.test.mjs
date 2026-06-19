import test from "node:test"
import assert from "node:assert/strict"

import {
  checkHttpEndpoint,
  checkWebSocketEndpoint,
  getCaptureSupportItems,
} from "../../src/utils/demoDiagnostics.js"

test("broadcast preflight marks insecure public http as blocking error", () => {
  const items = getCaptureSupportItems({
    mode: "screen",
    mediaDevices: {
      getDisplayMedia() {},
      getUserMedia() {},
    },
    windowLike: {
      isSecureContext: false,
      location: {
        protocol: "http:",
        hostname: "example.com",
      },
    },
  })

  const secureItem = items.find((item) => item.key === "secure-context")
  assert.equal(secureItem.status, "error")
  assert.equal(secureItem.blocking, true)
})

test("broadcast preflight accepts localhost browser capture context", () => {
  const items = getCaptureSupportItems({
    mode: "camera",
    mediaDevices: {
      getUserMedia() {},
    },
    windowLike: {
      isSecureContext: false,
      location: {
        protocol: "http:",
        hostname: "localhost",
      },
    },
  })

  assert.equal(items.find((item) => item.key === "secure-context").status, "ok")
  assert.equal(items.find((item) => item.key === "camera-capture").status, "ok")
})

test("http diagnostics can accept reachable non-2xx proxy responses", async () => {
  const originalFetch = globalThis.fetch
  globalThis.fetch = async () => ({
    ok: false,
    status: 404,
    json: async () => ({}),
  })

  try {
    const item = await checkHttpEndpoint({
      key: "live-stream-proxy",
      label: "录播流代理",
      url: "/live-stream/",
      accept: (response) => response.status < 500,
    })

    assert.equal(item.status, "ok")
  } finally {
    globalThis.fetch = originalFetch
  }
})

test("websocket diagnostics pass once the socket opens", async () => {
  class OpenSocket {
    constructor(url) {
      this.url = url
      setTimeout(() => this.onopen?.(), 0)
    }

    send() {}

    close() {
      this.closed = true
    }
  }

  const item = await checkWebSocketEndpoint({
    key: "chat-ws",
    label: "聊天室 WebSocket",
    url: "ws://localhost/ws-netty",
    timeoutMs: 50,
    WebSocketCtor: OpenSocket,
  })

  assert.equal(item.status, "ok")
})

