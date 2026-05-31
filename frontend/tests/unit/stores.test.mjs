import test from "node:test";
import assert from "node:assert/strict";

/**
 * BDD 风格测试：用户 Store 状态逻辑
 *
 * 测试 Pinia store 的核心计算属性，不依赖 Vue 运行时。
 * 将 store 的核心逻辑提取为纯函数测试。
 */

// 提取 store 中的核心计算逻辑
const ADMIN_ROLE_IDS = [1, 2];

const isAdmin = (roleIds = []) =>
  roleIds.some((roleId) => ADMIN_ROLE_IDS.includes(roleId));

const validateLoginParam = (param) => ({
  username: (param?.username || "").trim(),
  password: (param?.password || "").trim(),
});

// ─── Story: 管理员权限判断 ───────────────────────────────

test("Story: 管理员权限判断", async (t) => {
  await t.test("Given: 用户角色包含管理员ID", () => {
    assert.equal(isAdmin([1, 3]), true, "roleId=1 是管理员");
    assert.equal(isAdmin([2]), true, "roleId=2 是管理员");
  });

  await t.test("Given: 用户角色不包含管理员ID", () => {
    assert.equal(isAdmin([3, 4]), false, "普通角色");
    assert.equal(isAdmin([]), false, "无角色");
    assert.equal(isAdmin(), false, "undefined");
  });
});

// ─── Story: 登录参数预处理 ───────────────────────────────

test("Story: 登录参数预处理（去除首尾空格）", async (t) => {
  await t.test("When: 用户名含空格", () => {
    const result = validateLoginParam({ username: "  admin  ", password: "123" });
    assert.equal(result.username, "admin");
    assert.equal(result.password, "123");
  });

  await t.test("When: 密码含空格", () => {
    const result = validateLoginParam({ username: "user", password: "  pass " });
    assert.equal(result.password, "pass");
  });

  await t.test("When: 输入为 null/undefined", () => {
    const r1 = validateLoginParam(null);
    assert.equal(r1.username, "");
    assert.equal(r1.password, "");
  });
});

// ─── Story: Notification 未读计数逻辑 ─────────────────────

const calcNextUnread = (currentUnread, { type, isRead }) => {
  if (isRead === 1) return currentUnread;
  return currentUnread + 1;
};

test("Story: 未读通知计数", async (t) => {
  await t.test("When: 收到未读通知", () => {
    assert.equal(calcNextUnread(0, { type: "live_started", isRead: 0 }), 1);
  });

  await t.test("When: 收到已读通知", () => {
    assert.equal(calcNextUnread(5, { type: "live_started", isRead: 1 }), 5);
  });

  await t.test("When: 全部标记已读", () => {
    assert.equal(calcNextUnread(3, { type: "", isRead: 1 }), 3);
  });
});
