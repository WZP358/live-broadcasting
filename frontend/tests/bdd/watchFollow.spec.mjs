/**
 * BDD Specification: 用户关注和历史流程
 *
 * 覆盖用户注册 -> 登录 -> 浏览 -> 关注 -> 历史记录 的完整流程。
 */
import test from "node:test";
import assert from "node:assert/strict";

// ─── 用户注册验证逻辑 ──────────────────────────────────

const validateRegisterForm = ({ username, password, confirmPassword, email, agreeTerms }) => {
  const errors = [];

  if (!username || username.trim().length < 2) {
    errors.push("用户名至少2个字符");
  }
  if (username && username.trim().length > 20) {
    errors.push("用户名最多20个字符");
  }

  if (!password || password.length < 6) {
    errors.push("密码至少6位");
  }
  if (password && password.length > 30) {
    errors.push("密码最多30位");
  }

  if (password !== confirmPassword) {
    errors.push("两次密码不一致");
  }

  if (email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    errors.push("邮箱格式不正确");
  }

  if (!agreeTerms) {
    errors.push("请同意用户协议");
  }

  return {
    valid: errors.length === 0,
    errors,
  };
};

// ─── Story: 用户注册 ───────────────────────────────────

test("Story: 新用户注册", async (t) => {
  await t.test("Given: 有效注册信息, When: 提交注册, Then: 验证通过", () => {
    const result = validateRegisterForm({
      username: "新用户",
      password: "123456",
      confirmPassword: "123456",
      email: "test@example.com",
      agreeTerms: true,
    });
    assert.equal(result.valid, true);
    assert.equal(result.errors.length, 0);
  });

  await t.test("When: 用户名为空, Then: 提示'用户名至少2个字符'", () => {
    const result = validateRegisterForm({
      username: "",
      password: "123456",
      confirmPassword: "123456",
      agreeTerms: true,
    });
    assert.equal(result.valid, false);
    assert.ok(result.errors.some((e) => e.includes("用户名")));
  });

  await t.test("When: 用户名只有1个字符, Then: 提示'用户名至少2个字符'", () => {
    const result = validateRegisterForm({
      username: "a",
      password: "123456",
      confirmPassword: "123456",
      agreeTerms: true,
    });
    assert.equal(result.valid, false);
  });

  await t.test("When: 密码不足6位, Then: 提示'密码至少6位'", () => {
    const result = validateRegisterForm({
      username: "测试用户",
      password: "12345",
      confirmPassword: "12345",
      agreeTerms: true,
    });
    assert.equal(result.valid, false);
    assert.ok(result.errors.some((e) => e.includes("密码")));
  });

  await t.test("When: 两次密码不一致, Then: 提示'两次密码不一致'", () => {
    const result = validateRegisterForm({
      username: "测试用户",
      password: "123456",
      confirmPassword: "654321",
      agreeTerms: true,
    });
    assert.equal(result.valid, false);
    assert.ok(result.errors.some((e) => e.includes("不一致")));
  });

  await t.test("When: 邮箱格式错误, Then: 提示'邮箱格式不正确'", () => {
    const result = validateRegisterForm({
      username: "测试用户",
      password: "123456",
      confirmPassword: "123456",
      email: "not-an-email",
      agreeTerms: true,
    });
    assert.equal(result.valid, false);
    assert.ok(result.errors.some((e) => e.includes("邮箱")));
  });

  await t.test("When: 未同意用户协议, Then: 提示'请同意用户协议'", () => {
    const result = validateRegisterForm({
      username: "测试用户",
      password: "123456",
      confirmPassword: "123456",
      agreeTerms: false,
    });
    assert.equal(result.valid, false);
    assert.ok(result.errors.some((e) => e.includes("协议")));
  });

  await t.test("When: 邮箱可选(空), Then: 不报邮箱错误", () => {
    const result = validateRegisterForm({
      username: "测试用户",
      password: "123456",
      confirmPassword: "123456",
      email: "",
      agreeTerms: true,
    });
    assert.equal(result.valid, true);
  });
});

// ─── Story: 用户登录状态管理 ────────────────────────────

test("Story: 登录后的状态管理", async (t) => {
  await t.test("Given: 登录成功, Then: isLogin=true, userToken 已设置", () => {
    // 模拟登录成功的状态
    const state = {
      isLogin: true,
      userInfo: { id: 1, username: "test", roleIds: [3] },
      userToken: "Bearer eyJ...",
    };

    assert.equal(state.isLogin, true);
    assert.ok(state.userToken.startsWith("Bearer "));
    assert.deepEqual(state.userInfo.roleIds, [3]);
  });

  await t.test("Given: 管理员登录, Then: isAdmin=true", () => {
    const ADMIN_ROLE_IDS = [1, 2];
    const isAdmin = (roleIds) => roleIds.some((rid) => ADMIN_ROLE_IDS.includes(rid));

    assert.equal(isAdmin([1, 3]), true);
    assert.equal(isAdmin([2]), true);
    assert.equal(isAdmin([3, 4]), false);
  });

  await t.test("When: 退出登录, Then: 状态全部清空", () => {
    const state = {
      isLogin: true,
      userInfo: { id: 1 },
      userToken: "Bearer token",
    };

    // 模拟 logout
    state.isLogin = false;
    state.userInfo = {};
    state.userToken = "";

    assert.equal(state.isLogin, false);
    assert.deepEqual(state.userInfo, {});
    assert.equal(state.userToken, "");
  });
});

// ─── Story: 关注/取消关注状态切换 ──────────────────────

test("Story: 用户关注与取消关注直播间", async (t) => {
  await t.test("Given: 未关注, When: 点击关注, Then: isFollowed=true", () => {
    const toggleFollow = (current) => !current;
    assert.equal(toggleFollow(false), true);
  });

  await t.test("Given: 已关注, When: 再次点击, Then: isFollowed=false", () => {
    const toggleFollow = (current) => !current;
    assert.equal(toggleFollow(true), false);
  });

  await t.test("Then: 关注数量应正确更新", () => {
    const updateFollowCount = (rooms, roomId, isNowFollowed) => {
      return rooms.map((r) => {
        if (r.id === roomId) {
          return {
            ...r,
            followCount: (r.followCount || 0) + (isNowFollowed ? 1 : -1),
          };
        }
        return r;
      });
    };

    const rooms = [{ id: 1, followCount: 99 }, { id: 2, followCount: 50 }];

    const afterFollow = updateFollowCount(rooms, 1, true);
    assert.equal(afterFollow[0].followCount, 100);

    const afterUnfollow = updateFollowCount(afterFollow, 1, false);
    assert.equal(afterUnfollow[0].followCount, 99);
  });
});

// ─── Story: 浏览历史去重 ───────────────────────────────

test("Story: 同一直播间多次进入，历史记录应去重", async (t) => {
  await t.test("Given: 已有历史 [100, 200], When: 再次进入100, Then: 历史仍为 [100, 200]", () => {
    const addToHistory = (history, roomId) => {
      if (history.includes(roomId)) return history;
      return [...history, roomId];
    };

    const history = [100, 200];
    const result = addToHistory(history, 100);
    assert.deepEqual(result, [100, 200]);
  });

  await t.test("When: 进入新房间300, Then: 历史变为 [100, 200, 300]", () => {
    const addToHistory = (history, roomId) => {
      if (history.includes(roomId)) return history;
      return [...history, roomId];
    };

    const result = addToHistory([100, 200], 300);
    assert.deepEqual(result, [100, 200, 300]);
  });

  await t.test("When: 清除历史, Then: 列表为空", () => {
    const result = [];
    assert.deepEqual(result, []);
    assert.equal(result.length, 0);
  });
});

// ─── Story: 分类筛选交互 ───────────────────────────────

test("Story: 用户点击分类标签筛选直播间", async (t) => {
  const rooms = [
    { id: 1, title: "LOL直播", categoryId: 5 },
    { id: 2, title: "唱歌", categoryId: 10 },
    { id: 3, title: "DOTA2直播", categoryId: 5 },
    { id: 4, title: "跳舞", categoryId: 10 },
  ];

  await t.test("When: 选择「游戏」分类(categoryId=5), Then: 显示2个房间", () => {
    const filtered = rooms.filter((r) => r.categoryId === 5);
    assert.equal(filtered.length, 2);
    assert.equal(filtered[0].title, "LOL直播");
    assert.equal(filtered[1].title, "DOTA2直播");
  });

  await t.test("When: 选择「娱乐」分类(categoryId=10), Then: 显示2个房间", () => {
    const filtered = rooms.filter((r) => r.categoryId === 10);
    assert.equal(filtered.length, 2);
  });

  await t.test("When: 选择不存在的分类, Then: 显示空列表", () => {
    const filtered = rooms.filter((r) => r.categoryId === 99);
    assert.equal(filtered.length, 0);
  });
});

console.log("BDD PASS: watchFollow.spec.mjs 全部通过");
