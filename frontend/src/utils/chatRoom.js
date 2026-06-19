export const CHAT_MESSAGE_LIMIT = 40

export const getSameOriginWebSocketBase = ({
  protocol = globalThis.location?.protocol || "http:",
  host = globalThis.location?.host || "localhost:5173",
  path = "/ws-netty",
} = {}) => {
  const wsProtocol = protocol === "https:" ? "wss" : "ws"
  return `${wsProtocol}://${host}${path}`
}

export const normalizeWebSocketToken = (token = "") =>
  String(token || "").replace(/^Bearer\s+/i, "")

export const createChatWebSocketUrl = ({
  protocol = globalThis.location?.protocol || "http:",
  host = globalThis.location?.host || "localhost:5173",
  path = "/ws-netty",
  token = "",
} = {}) => {
  const url = new URL(getSameOriginWebSocketBase({ protocol, host, path }))
  const normalizedToken = normalizeWebSocketToken(token)
  if (normalizedToken) {
    url.searchParams.set("token", normalizedToken)
  }
  return url.toString()
}

export const normalizeChatPayload = (payload) => {
  if (!payload) return null
  if (Array.isArray(payload)) {
    return payload.map(normalizeChatPayload).filter(Boolean)
  }
  if (typeof payload === "string") {
    return { nickname: "系统消息", text: payload, isSystem: true }
  }
  return {
    ...payload,
    nickname: payload.nickname || payload.username || "观众",
    text: payload.text ?? payload.content ?? "",
  }
}

export const appendChatMessages = (messages, payload, limit = CHAT_MESSAGE_LIMIT) => {
  const normalized = normalizeChatPayload(payload)
  if (!normalized) return messages
  const incoming = Array.isArray(normalized) ? normalized : [normalized]
  return messages.concat(incoming).slice(-limit)
}
