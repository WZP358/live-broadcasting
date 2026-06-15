export const CHAT_MESSAGE_LIMIT = 40

export const createChatWebSocketUrl = ({
  protocol = globalThis.location?.protocol || "http:",
  hostname = globalThis.location?.hostname || "localhost",
  port = 10022,
  token = "",
} = {}) => {
  const wsProtocol = protocol === "https:" ? "wss" : "ws"
  const url = new URL(`${wsProtocol}://${hostname}:${port}/`)
  if (token) {
    url.searchParams.set("token", token)
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
