import test from "node:test"
import assert from "node:assert/strict"
import {
  appendChatMessages,
  createChatWebSocketUrl,
  normalizeChatPayload,
} from "../../src/utils/chatRoom.js"

test("chat websocket url uses protocol and optional token", () => {
  assert.equal(
    createChatWebSocketUrl({
      protocol: "http:",
      hostname: "localhost",
      port: 10022,
      token: "abc 123",
    }),
    "ws://localhost:10022/?token=abc+123",
  )
  assert.equal(
    createChatWebSocketUrl({
      protocol: "https:",
      hostname: "live.example.com",
      port: 10022,
      token: "",
    }),
    "wss://live.example.com:10022/",
  )
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
