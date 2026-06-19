import test from "node:test"
import assert from "node:assert/strict"

import {
  createBrowserLiveFallbackUrls,
  getBrowserLiveIceServers,
} from "../../src/utils/browserLive.js"

const createStorage = (values = {}) => ({
  getItem: (key) => values[key] || "",
})

test("browser live websocket uses same-origin proxy before stored custom urls", () => {
  const urls = createBrowserLiveFallbackUrls({
    locationLike: {
      protocol: "http:",
      host: "118bn7be57830.vicp.fun:10022",
      hostname: "118bn7be57830.vicp.fun",
    },
    storage: createStorage({
      "live.browser.signalUrl": "ws://old.example.com:10022/",
    }),
  })

  assert.deepEqual(urls.slice(0, 2), [
    "ws://118bn7be57830.vicp.fun:10022/ws-netty",
    "ws://118bn7be57830.vicp.fun:10022/ws/browser-live",
  ])
  assert.equal(urls[2], "ws://old.example.com:10022/")
})

test("browser live websocket keeps direct local fallbacks only for private hosts", () => {
  const urls = createBrowserLiveFallbackUrls({
    locationLike: {
      protocol: "http:",
      host: "172.19.14.201:5173",
      hostname: "172.19.14.201",
    },
    storage: createStorage(),
  })

  assert.ok(urls.includes("ws://172.19.14.201:10022/"))
  assert.ok(urls.includes("ws://172.19.14.201:9000/ws/browser-live"))
})

test("browser live ice servers can be configured for TURN relay", () => {
  const iceServers = getBrowserLiveIceServers({
    envValue: JSON.stringify([
      {
        urls: "turn:turn.example.com:3478",
        username: "demo",
        credential: "secret",
      },
    ]),
    storage: createStorage(),
  })

  assert.deepEqual(iceServers, [
    {
      urls: "turn:turn.example.com:3478",
      username: "demo",
      credential: "secret",
    },
  ])
})

