const LOCAL_HOSTS = new Set(["localhost", "127.0.0.1", "0.0.0.0", "::1", "[::1]"])
const LIVE_PORTS = new Set(["8080", "8081", "8082"])
const PRIVATE_HOST_RE = /^(10\.\d+\.\d+\.\d+|172\.(1[6-9]|2\d|3[0-1])\.\d+\.\d+|192\.168\.\d+\.\d+)$/

const isPrivateHost = (hostname = "") => PRIVATE_HOST_RE.test(String(hostname || "").trim())
const getLocationHostname = (locationLike = globalThis.location) =>
  locationLike?.hostname || String(locationLike?.host || "").split(":")[0]
const isSamePageHost = (hostname = "", locationLike = globalThis.location) =>
  Boolean(hostname && getLocationHostname(locationLike) && hostname === getLocationHostname(locationLike))

const isLocalLiveUrl = (url, locationLike = globalThis.location) => {
  if (!url) return false
  if (url.startsWith("/")) return false
  try {
    const parsed = new URL(url)
    if (!LIVE_PORTS.has(parsed.port)) {
      return false
    }
    return LOCAL_HOSTS.has(parsed.hostname) || isPrivateHost(parsed.hostname) || isSamePageHost(parsed.hostname, locationLike)
  } catch (error) {
    return false
  }
}

export const toPublicLiveUrl = (url = "", locationLike = globalThis.location) => {
  if (!url || !isLocalLiveUrl(url, locationLike)) {
    return url || ""
  }
  const parsed = new URL(url)
  const path = parsed.pathname.startsWith("/") ? parsed.pathname : `/${parsed.pathname}`
  const query = parsed.search || ""
  return `${locationLike?.protocol || "http:"}//${locationLike?.host || "localhost:5173"}/live-stream${path}${query}`
}
