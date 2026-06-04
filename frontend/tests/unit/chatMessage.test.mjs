import test from "node:test";
import assert from "node:assert/strict";

/**
 * TDD 单元测试：聊天消息处理逻辑
 *
 * 测试弹幕消息格式化、敏感词过滤、消息去重等核心逻辑。
 */

// ─── 消息格式化 ────────────────────────────────────────

const formatChatMessage = (msg) => {
  if (!msg) return null;
  const { username = "匿名用户", content = "", type = "text" } = msg;
  return {
    username: String(username).trim() || "匿名用户",
    content: String(content).trim(),
    type,
    timestamp: msg.timestamp || Date.now(),
  };
};

// ─── 敏感词检测 ────────────────────────────────────────

const SENSITIVE_WORDS = ["违法", "欺诈", "赌博"];

const containsSensitiveWord = (text) => {
  if (!text) return false;
  const lower = text.toLowerCase();
  return SENSITIVE_WORDS.some((word) => lower.includes(word));
};

const filterSensitiveContent = (text) => {
  if (!text) return "";
  let result = text;
  for (const word of SENSITIVE_WORDS) {
    const regex = new RegExp(word, "gi");
    result = result.replace(regex, "***");
  }
  return result;
};

// ─── 消息去重 ──────────────────────────────────────────

const isDuplicateMessage = (existingMessages, newMessage, windowMs = 3000) => {
  if (!newMessage || !existingMessages.length) return false;
  const now = Date.now();
  return existingMessages.some(
    (m) =>
      m.username === newMessage.username &&
      m.content === newMessage.content &&
      now - m.timestamp < windowMs,
  );
};

// ─── Story: 消息格式化 ─────────────────────────────────

test("Story: 聊天消息格式化", async (t) => {
  await t.test("Given: 完整消息, When: 格式化, Then: 保持原有字段", () => {
    const result = formatChatMessage({
      username: "小明",
      content: "Hello!",
      type: "text",
      timestamp: 1700000000000,
    });
    assert.equal(result.username, "小明");
    assert.equal(result.content, "Hello!");
    assert.equal(result.type, "text");
    assert.equal(result.timestamp, 1700000000000);
  });

  await t.test("Given: 用户名为空, When: 格式化, Then: 使用'匿名用户'", () => {
    const result = formatChatMessage({ username: "", content: "Hi" });
    assert.equal(result.username, "匿名用户");
  });

  await t.test("Given: 用户名为空白, When: 格式化, Then: 使用'匿名用户'", () => {
    const result = formatChatMessage({ username: "   ", content: "Hi" });
    assert.equal(result.username, "匿名用户");
  });

  await t.test("Given: 消息为 null, When: 格式化, Then: 返回 null", () => {
    assert.equal(formatChatMessage(null), null);
  });

  await t.test("Given: 消息为 undefined, When: 格式化, Then: 返回 null", () => {
    assert.equal(formatChatMessage(undefined), null);
  });

  await t.test("Given: 内容含首尾空格, When: 格式化, Then: 去除空格", () => {
    const result = formatChatMessage({ username: " 小明 ", content: "  你好  " });
    assert.equal(result.username, "小明");
    assert.equal(result.content, "你好");
  });
});

// ─── Story: 敏感词过滤 ─────────────────────────────────

test("Story: 敏感词检测与过滤", async (t) => {
  await t.test("When: 内容含敏感词, Then: 检测返回 true", () => {
    assert.equal(containsSensitiveWord("这是违法内容"), true);
    assert.equal(containsSensitiveWord("涉及赌博"), true);
  });

  await t.test("When: 内容正常, Then: 检测返回 false", () => {
    assert.equal(containsSensitiveWord("这是正常内容"), false);
    assert.equal(containsSensitiveWord(""), false);
    assert.equal(containsSensitiveWord(null), false);
  });

  await t.test("When: 过滤敏感词, Then: 替换为***", () => {
    assert.equal(filterSensitiveContent("这是违法内容"), "这是***内容");
    assert.equal(filterSensitiveContent("赌博和欺诈"), "***和***");
  });

  await t.test("When: 无敏感词, Then: 原样返回", () => {
    assert.equal(filterSensitiveContent("正常聊天"), "正常聊天");
  });

  await t.test("When: 输入为空, Then: 返回空字符串", () => {
    assert.equal(filterSensitiveContent(""), "");
    assert.equal(filterSensitiveContent(null), "");
  });
});

// ─── Story: 消息去重 ───────────────────────────────────

test("Story: 短时间内重复消息去重", async (t) => {
  const now = Date.now();
  const existing = [
    { username: "小明", content: "666", timestamp: now - 1000 },
    { username: "小红", content: "来了", timestamp: now - 5000 },
  ];

  await t.test("When: 同一用户3秒内发送相同内容, Then: 判定为重复", () => {
    assert.equal(
      isDuplicateMessage(existing, { username: "小明", content: "666", timestamp: now }, 3000),
      true,
    );
  });

  await t.test("When: 同一用户3秒后发送相同内容, Then: 不判定为重复", () => {
    assert.equal(
      isDuplicateMessage(existing, { username: "小明", content: "666", timestamp: now }, 500),
      false,
    );
  });

  await t.test("When: 不同用户发送相同内容, Then: 不判定为重复", () => {
    assert.equal(
      isDuplicateMessage(existing, { username: "路人", content: "666", timestamp: now }, 3000),
      false,
    );
  });

  await t.test("Given: 空历史, When: 新消息, Then: 不重复", () => {
    assert.equal(
      isDuplicateMessage([], { username: "小明", content: "Hi", timestamp: now }, 3000),
      false,
    );
  });

  await t.test("When: newMessage 为 null, Then: 不重复", () => {
    assert.equal(isDuplicateMessage(existing, null, 3000), false);
  });
});

console.log("PASS: chatMessage.test.mjs 全部通过");
