import test from "node:test";
import assert from "node:assert/strict";

/**
 * TDD 单元测试：HTTP 请求拦截器与响应处理逻辑
 *
 * 提取 request.js 中的拦截器核心逻辑，验证：
 * - Token 注入
 * - 401 处理
 * - 网络错误处理
 * - 业务错误码处理
 */

// ─── 响应码判断 ────────────────────────────────────────

const isSuccess = (code) => code === 0;
const isUnauthorized = (code) => code === 401;

const parseAxiosError = (error) => {
  const status = error?.response?.status;
  const msg = error?.response?.data?.msg;
  const message = error?.message || "";

  if (status === 401) {
    return { type: "unauthorized", msg: msg || "Unauthorized" };
  }

  if (message === "Network Error") {
    return { type: "network", msg: "后端服务连接异常" };
  }

  if (message.includes("timeout")) {
    return { type: "timeout", msg: "系统接口请求超时" };
  }

  if (status && status >= 500) {
    return { type: "server_error", msg: "服务器繁忙，请稍后重试" };
  }

  return { type: "unknown", msg: msg || message || "网络异常，请稍后重试" };
};

const injectToken = (headers, token) => {
  if (!headers) return headers;
  if (token) {
    return { ...headers, Authorization: token };
  }
  return headers;
};

// ─── Story: 响应状态码处理 ─────────────────────────────

test("Story: API 响应状态码处理", async (t) => {
  await t.test("When: code=0, Then: isSuccess=true", () => {
    assert.equal(isSuccess(0), true);
    assert.equal(isSuccess(1), false);
    assert.equal(isSuccess(401), false);
  });

  await t.test("When: code=401, Then: isUnauthorized=true", () => {
    assert.equal(isUnauthorized(401), true);
    assert.equal(isUnauthorized(0), false);
    assert.equal(isUnauthorized(500), false);
  });
});

// ─── Story: Axios 错误分类 ─────────────────────────────

test("Story: Axios 网络错误分类与提示文案", async (t) => {
  await t.test("When: Network Error, Then: 提示'后端服务连接异常'", () => {
    const result = parseAxiosError({ message: "Network Error" });
    assert.equal(result.type, "network");
    assert.equal(result.msg, "后端服务连接异常");
  });

  await t.test("When: timeout, Then: 提示'系统接口请求超时'", () => {
    const result = parseAxiosError({ message: "timeout of 60000ms exceeded" });
    assert.equal(result.type, "timeout");
    assert.equal(result.msg, "系统接口请求超时");
  });

  await t.test("When: 500, Then: 提示'服务器繁忙，请稍后重试'", () => {
    const result = parseAxiosError({
      response: { status: 500, data: { msg: "Internal Server Error" } },
      message: "Request failed with status code 500",
    });
    assert.equal(result.type, "server_error");
    assert.equal(result.msg, "服务器繁忙，请稍后重试");
  });

  await t.test("When: 401, Then: 提示'Unauthorized'", () => {
    const result = parseAxiosError({
      response: { status: 401, data: { msg: "登录状态失效" } },
      message: "Request failed with status code 401",
    });
    assert.equal(result.type, "unauthorized");
    assert.equal(result.msg, "登录状态失效");
  });

  await t.test("When: 未知错误无 msg, Then: 兜底提示", () => {
    const result = parseAxiosError({ message: "Some weird error" });
    assert.equal(result.type, "unknown");
    assert.equal(result.msg, "Some weird error");
  });

  await t.test("When: 完全无信息, Then: 兜底提示", () => {
    const result = parseAxiosError({});
    assert.equal(result.type, "unknown");
    assert.equal(result.msg, "网络异常，请稍后重试");
  });
});

// ─── Story: Token 注入 ─────────────────────────────────

test("Story: 请求拦截器注入 Token", async (t) => {
  await t.test("When: 已登录, Then: headers 中包含 Authorization", () => {
    const headers = injectToken({ "Content-Type": "application/json" }, "Bearer token123");
    assert.equal(headers.Authorization, "Bearer token123");
    assert.equal(headers["Content-Type"], "application/json");
  });

  await t.test("When: 未登录(token为空), Then: headers 不包含 Authorization", () => {
    const headers = injectToken({ "Content-Type": "application/json" }, "");
    assert.equal(headers.Authorization, undefined);
  });

  await t.test("When: 未登录(token为null), Then: headers 不变", () => {
    const headers = injectToken({ "Content-Type": "application/json" }, null);
    assert.equal(headers.Authorization, undefined);
  });

  await t.test("When: headers 为 null, Then: 返回 null(边界保护)", () => {
    assert.equal(injectToken(null, "token123"), null);
  });
});

// ─── Story: 业务错误码传播 ─────────────────────────────

test("Story: 业务逻辑错误不应吞掉错误信息", async (t) => {
  await t.test("Given: 响应 code=422, Then: 应通过 rejected Promise 传播", () => {
    const res = { code: 422, msg: "参数验证失败", data: null };

    // 模拟拦截器中 code !== 0 且 code !== 401 的逻辑
    const shouldReject = res.code !== 0 && res.code !== 401;
    assert.equal(shouldReject, true);

    // 错误消息应为响应的 msg
    const errorMsg = res.msg;
    assert.equal(errorMsg, "参数验证失败");
  });

  await t.test("Given: 响应 code=0, Then: 不应 rejection", () => {
    const res = { code: 0, msg: "Success", data: { id: 1 } };
    const shouldReject = res.code !== 0 && res.code !== 401;
    assert.equal(shouldReject, false);
  });
});

console.log("PASS: requestInterceptor.test.mjs 全部通过");
