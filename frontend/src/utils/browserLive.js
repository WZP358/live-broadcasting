const DEFAULT_ICE_SERVERS = [
  { urls: "stun:stun.l.google.com:19302" },
  { urls: "stun:stun1.l.google.com:19302" },
]

const PRIVATE_HOST_RE = /^(localhost|127\.0\.0\.1|172\.\d+\.\d+\.\d+|192\.168\.\d+\.\d+|10\.\d+\.\d+\.\d+)$/

const readStorageValue = (storage, key) => {
  try {
    return storage?.getItem?.(key) || ""
  } catch (error) {
    return ""
  }
}

const normalizeIceServer = (server) => {
  if (!server) {
    return null
  }
  if (typeof server === "string") {
    return server.trim() ? { urls: server.trim() } : null
  }
  if (typeof server === "object" && server.urls) {
    return server
  }
  return null
}

const parseIceServers = (value = "") => {
  const raw = String(value || "").trim()
  if (!raw) {
    return []
  }

  try {
    const parsed = JSON.parse(raw)
    const list = Array.isArray(parsed) ? parsed : [parsed]
    return list.map(normalizeIceServer).filter(Boolean)
  } catch (error) {
    return raw
      .split(",")
      .map(normalizeIceServer)
      .filter(Boolean)
  }
}

export const getBrowserLiveIceServers = ({
  envValue = import.meta.env?.VITE_WEBRTC_ICE_SERVERS,
  storage = globalThis.localStorage,
} = {}) => {
  const configured = parseIceServers(readStorageValue(storage, "live.browser.iceServers") || envValue)
  return configured.length ? configured : DEFAULT_ICE_SERVERS
}

export const createBrowserLiveFallbackUrls = ({
  locationLike = globalThis.location,
  storage = globalThis.localStorage,
} = {}) => {
  const wsProtocol = locationLike?.protocol === "https:" ? "wss" : "ws"
  const host = locationLike?.host || "localhost:5173"
  const hostname = locationLike?.hostname || "localhost"
  const urls = [
    `${wsProtocol}://${host}/ws-netty`,
    `${wsProtocol}://${host}/ws/browser-live`,
  ]

  const customUrl = readStorageValue(storage, "live.browser.signalUrl")
  if (customUrl && !urls.includes(customUrl)) {
    urls.push(customUrl)
  }

  if (PRIVATE_HOST_RE.test(hostname)) {
    urls.push(`${wsProtocol}://${hostname}:10022/`)
    urls.push(`${wsProtocol}://${hostname}:9000/ws/browser-live`)
  }
  return urls
}

export const createBrowserLiveSignalUrl = () => createBrowserLiveFallbackUrls()[0]

export const createPeerConnection = () =>
  new RTCPeerConnection({
    iceServers: getBrowserLiveIceServers(),
  })
