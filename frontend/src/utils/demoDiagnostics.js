import { createBrowserLiveFallbackUrls } from "./browserLive.js"
import { createChatWebSocketUrl } from "./chatRoom.js"
import { getLiveDenoiseServiceUrl } from "./liveDenoise.js"

const DEFAULT_TIMEOUT = 2500
const PRIVATE_HOST_RE = /^(localhost|127\.0\.0\.1|\[::1\]|172\.\d+\.\d+\.\d+|192\.168\.\d+\.\d+|10\.\d+\.\d+\.\d+)$/

const ok = (key, label, detail, extra = {}) => ({ key, label, status: "ok", detail, ...extra })
const warn = (key, label, detail, extra = {}) => ({ key, label, status: "warn", detail, ...extra })
const error = (key, label, detail, extra = {}) => ({ key, label, status: "error", detail, ...extra })

export const createTimeout = (timeoutMs = DEFAULT_TIMEOUT) => {
  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), timeoutMs)
  return {
    signal: controller.signal,
    clear: () => clearTimeout(timer),
  }
}

const isPrivateHost = (hostname = "") => PRIVATE_HOST_RE.test(String(hostname || "").trim())

const isSecureCaptureContext = (windowLike = globalThis.window) => {
  if (windowLike?.isSecureContext) {
    return true
  }
  const protocol = windowLike?.location?.protocol || globalThis.location?.protocol || ""
  const hostname = windowLike?.location?.hostname || globalThis.location?.hostname || ""
  return protocol === "https:" || isPrivateHost(hostname)
}

export const getCaptureSupportItems = ({
  mode = "screen",
  mediaDevices = globalThis.navigator?.mediaDevices,
  windowLike = globalThis.window,
} = {}) => {
  const items = []
  const secureContext = isSecureCaptureContext(windowLike)

  items.push(
    secureContext
      ? ok("secure-context", "安全访问环境", "当前访问方式支持浏览器采集能力。", { blocking: true })
      : error("secure-context", "安全访问环境", "开播必须使用 localhost 或 HTTPS，HTTP 外网域名无法调用摄像头/屏幕采集。", { blocking: true })
  )

  if (mode === "screen") {
    items.push(
      mediaDevices?.getDisplayMedia
        ? ok("screen-capture", "屏幕采集能力", "浏览器支持屏幕直播。", { blocking: true })
        : error("screen-capture", "屏幕采集能力", "当前浏览器没有 getDisplayMedia，无法进行屏幕直播。", { blocking: true })
    )
  } else {
    items.push(
      mediaDevices?.getUserMedia
        ? ok("camera-capture", "摄像头采集能力", "浏览器支持摄像头和麦克风采集。", { blocking: true })
        : error("camera-capture", "摄像头采集能力", "当前浏览器没有 getUserMedia，无法进行摄像头直播。", { blocking: true })
    )
  }

  items.push(
    mediaDevices?.getUserMedia
      ? ok("microphone-capture", "麦克风采集能力", "直播只采集麦克风声音，避免电脑外放造成回环。", { blocking: true })
      : error("microphone-capture", "麦克风采集能力", "未检测到麦克风采集能力，无法完成带声音的直播。", { blocking: true })
  )

  return items
}

const readJsonSafe = async (response) => {
  try {
    return await response.json()
  } catch (err) {
    return null
  }
}

export const checkHttpEndpoint = async ({
  key,
  label,
  url,
  method = "GET",
  timeoutMs = DEFAULT_TIMEOUT,
  headers = {},
  accept,
  describe,
} = {}) => {
  const timeout = createTimeout(timeoutMs)
  try {
    const response = await fetch(url, {
      method,
      headers,
      signal: timeout.signal,
    })
    const body = await readJsonSafe(response)
    const accepted = accept ? accept(response, body) : response.ok
    if (!accepted) {
      return error(key, label, describe?.(response, body) || `HTTP ${response.status}`, { response, body })
    }
    return ok(key, label, describe?.(response, body) || "连接正常", { response, body })
  } catch (err) {
    const detail = err?.name === "AbortError" ? "请求超时，请检查服务是否已启动。" : "无法连接，请检查服务地址和代理配置。"
    return error(key, label, detail, { cause: err })
  } finally {
    timeout.clear()
  }
}

export const checkWebSocketEndpoint = ({
  key,
  label,
  url,
  payload,
  timeoutMs = DEFAULT_TIMEOUT,
  WebSocketCtor = globalThis.WebSocket,
} = {}) =>
  new Promise((resolve) => {
    if (!WebSocketCtor) {
      resolve(error(key, label, "当前运行环境不支持 WebSocket。"))
      return
    }

    let settled = false
    let opened = false
    let socket = null

    const settle = (item) => {
      if (settled) {
        return
      }
      settled = true
      clearTimeout(timer)
      try {
        socket?.close?.()
      } catch (err) {
        // ignore close errors during diagnostics
      }
      resolve(item)
    }

    const timer = setTimeout(() => {
      settle(error(key, label, "连接超时，请检查 WebSocket 端口或内网穿透映射。"))
    }, timeoutMs)

    try {
      socket = new WebSocketCtor(url)
      socket.onopen = () => {
        opened = true
        if (payload) {
          socket.send(JSON.stringify(payload))
        }
        settle(ok(key, label, "WebSocket 可以连接。"))
      }
      socket.onerror = () => {
        settle(error(key, label, opened ? "WebSocket 连接中断。" : "无法连接 WebSocket，请确认端口已对外映射。"))
      }
      socket.onclose = () => {
        if (!opened) {
          settle(error(key, label, "WebSocket 被服务端拒绝，请检查 token、协议或代理路径。"))
        }
      }
    } catch (err) {
      settle(error(key, label, "WebSocket 地址无效或被浏览器拦截。", { cause: err }))
    }
  })

const normalizeBearer = (token = "") => {
  const raw = String(token || "").trim()
  if (!raw) {
    return ""
  }
  return /^Bearer\s+/i.test(raw) ? raw : `Bearer ${raw}`
}

const checkAgentHealth = async () => {
  const item = await checkHttpEndpoint({
    key: "agent-health",
    label: "小脉 AI 服务",
    url: "/api/v1/agent/health",
    timeoutMs: 3500,
    accept: (response) => response.ok,
    describe: (response, body) => {
      const data = body?.data || body || {}
      const llmStatus = String(data?.llm?.status || data?.status || "").toLowerCase()
      if (llmStatus === "ok" || llmStatus === "ready") {
        return "AI Agent 和本地大模型连接正常。"
      }
      return data?.llm?.message || data?.message || "AI Agent 可访问，但大模型状态异常。"
    },
  })
  const data = item.body?.data || item.body || {}
  const llmStatus = String(data?.llm?.status || data?.status || "").toLowerCase()
  if (item.status === "ok" && llmStatus && !["ok", "ready"].includes(llmStatus)) {
    return warn(item.key, item.label, item.detail)
  }
  return item
}

export const runBroadcastPreflight = async ({
  mode = "screen",
  denoiseEnabled = false,
  timeoutMs = DEFAULT_TIMEOUT,
} = {}) => {
  const items = [...getCaptureSupportItems({ mode })]
  const signalUrl = createBrowserLiveFallbackUrls()[0]
  items.push(
    await checkWebSocketEndpoint({
      key: "live-signal",
      label: "直播信令通道",
      url: signalUrl,
      timeoutMs,
    }).then((item) => ({ ...item, blocking: true }))
  )

  if (denoiseEnabled) {
    const denoiseUrl = getLiveDenoiseServiceUrl()
    items.push(
      await checkWebSocketEndpoint({
        key: "denoise-service",
        label: "实时降噪服务",
        url: denoiseUrl,
        timeoutMs: 1800,
      }).then((item) =>
        item.status === "ok"
          ? item
          : warn("denoise-service", "实时降噪服务", "降噪服务暂不可用，开播时会继续使用原始麦克风声音。")
      )
    )
  }

  return {
    checkedAt: new Date(),
    items,
    ok: items.every((item) => item.status !== "error" || !item.blocking),
  }
}

export const runPublicDemoDiagnostics = async ({
  token = "",
  timeoutMs = DEFAULT_TIMEOUT,
} = {}) => {
  const headers = {}
  const bearer = normalizeBearer(token)
  if (bearer) {
    headers.Authorization = bearer
  }

  const items = [
    ...getCaptureSupportItems({ mode: "screen" }).map((item) => ({
      ...item,
      blocking: false,
      status: item.status === "error" ? "warn" : item.status,
    })),
  ]

  items.push(await checkAgentHealth())
  items.push(
    await checkHttpEndpoint({
      key: "demo-api",
      label: "演示数据接口",
      url: "/api/v1/system/demo/status",
      headers,
      timeoutMs,
      accept: (response) => response.ok,
      describe: (response) => (response.ok ? "管理员演示数据接口可访问。" : `HTTP ${response.status}`),
    })
  )
  items.push(
    await checkHttpEndpoint({
      key: "live-stream-proxy",
      label: "录播流代理",
      url: "/live-stream/",
      timeoutMs,
      accept: (response) => response.status < 500,
      describe: (response) => (response.status < 500 ? "录播流代理已响应。" : `HTTP ${response.status}`),
    })
  )
  items.push(
    await checkWebSocketEndpoint({
      key: "chat-ws",
      label: "聊天室 WebSocket",
      url: createChatWebSocketUrl({ token }),
      timeoutMs,
    })
  )
  items.push(
    await checkWebSocketEndpoint({
      key: "live-signal",
      label: "直播信令 WebSocket",
      url: createBrowserLiveFallbackUrls()[0],
      timeoutMs,
    })
  )

  return {
    checkedAt: new Date(),
    items,
    ok: items.every((item) => item.status !== "error"),
  }
}

