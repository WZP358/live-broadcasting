import test from "node:test"
import assert from "node:assert/strict"
import { toPublicLiveUrl } from "../../src/utils/publicLiveUrl.js"

const publicLocation = {
  protocol: "http:",
  host: "118bn7be57830.vicp.fun",
}

test("local live stream url is rewritten to same-origin proxy", () => {
  assert.equal(
    toPublicLiveUrl("http://127.0.0.1:8080/live/23.flv", publicLocation),
    "http://118bn7be57830.vicp.fun/live-stream/live/23.flv",
  )
  assert.equal(
    toPublicLiveUrl("http://localhost:8080/live/23.m3u8?token=abc", publicLocation),
    "http://118bn7be57830.vicp.fun/live-stream/live/23.m3u8?token=abc",
  )
  assert.equal(
    toPublicLiveUrl("http://172.19.14.201:8080/live/23.flv", publicLocation),
    "http://118bn7be57830.vicp.fun/live-stream/live/23.flv",
  )
  assert.equal(
    toPublicLiveUrl("http://10.240.249.33:8080/live/23.flv?token=abc", publicLocation),
    "http://118bn7be57830.vicp.fun/live-stream/live/23.flv?token=abc",
  )
  assert.equal(
    toPublicLiveUrl("http://118bn7be57830.vicp.fun:8080/live/23.flv", publicLocation),
    "http://118bn7be57830.vicp.fun/live-stream/live/23.flv",
  )
})

test("public and relative live stream urls stay unchanged", () => {
  assert.equal(toPublicLiveUrl("/demo-videos/live.mp4", publicLocation), "/demo-videos/live.mp4")
  assert.equal(toPublicLiveUrl("https://cdn.example.com/live/23.flv", publicLocation), "https://cdn.example.com/live/23.flv")
})
