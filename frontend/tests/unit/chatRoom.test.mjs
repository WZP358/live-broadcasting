import test from "node:test"
import assert from "node:assert/strict"
import {
  appendChatMessages,
  createChatWebSocketUrl,
  getSameOriginWebSocketBase,
  normalizeChatPayload,
  normalizeWebSocketToken,
} from "../../src/utils/chatRoom.js"

test("chat websocket url uses same-origin proxy path and optional token", () => {
  assert.equal(
    createChatWebSocketUrl({
      protocol: "http:",
      host: "localhost:5173",
      token: "Bearer abc 123",
    }),
    "ws://localhost:5173/ws-netty?token=abc+123",
  )
  assert.equal(
    createChatWebSocketUrl({
      protocol: "https:",
      host: "live.example.com",
      token: "",
    }),
    "wss://live.example.com/ws-netty",
  )
})

test("same-origin websocket base follows current page protocol", () => {
  assert.equal(getSameOriginWebSocketBase({ protocol: "http:", host: "127.0.0.1:5173" }), "ws://127.0.0.1:5173/ws-netty")
  assert.equal(getSameOriginWebSocketBase({ protocol: "https:", host: "demo.example.com" }), "wss://demo.example.com/ws-netty")
  assert.equal(normalizeWebSocketToken("Bearer token-value"), "token-value")
})

test("normalize chat payload accepts system string, object and array", () => {
  assert.deepEqual(normalizeChatPayload("欢迎进入直播间"), {
    nickname: "系统消息",
    text: "欢迎进入直播间",
    isSystem: true,
  })
  assert.deepEqual(normalizeChatPayload({ username: "小明", content: "来了" }), {
    username: "小明",
    content: "来了",
    nickname: "小明",
    text: "来了",
  })
  assert.deepEqual(normalizeChatPayload([null, { nickname: "主播", text: "晚上好" }]), [
    { nickname: "主播", text: "晚上好" },
  ])
})

test("append chat messages keeps the newest bounded messages", () => {
  const existing = Array.from({ length: 39 }, (_, index) => ({
    nickname: `用户${index}`,
    text: `${index}`,
  }))
  const result = appendChatMessages(existing, [
    { nickname: "用户39", text: "39" },
    { nickname: "用户40", text: "40" },
  ])

  assert.equal(result.length, 40)
  assert.equal(result[0].text, "1")
  assert.equal(result.at(-1).text, "40")
})
