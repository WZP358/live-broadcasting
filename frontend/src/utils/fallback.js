/**
 * 本地 SVG 回退图片 — 不依赖任何外部服务，永不断链
 */
export const FALLBACK_AVATAR =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 96 96'%3E%3Crect fill='%23e2e8f0' width='96' height='96' rx='16'/%3E%3Ccircle fill='%2394a3b8' cx='48' cy='38' r='14'/%3E%3Cellipse fill='%2394a3b8' cx='48' cy='80' rx='24' ry='16'/%3E%3C/svg%3E"

export const FALLBACK_COVER =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 640 360'%3E%3Crect fill='%23f1f5f9' width='640' height='360'/%3E%3Ctext fill='%2394a3b8' font-family='sans-serif' font-size='20' text-anchor='middle' x='320' y='180'%3E暂无封面%3C/text%3E%3C/svg%3E"

/** 图片加载失败时设置 src 为回退图 */
export const FALLBACK_GIFT_ICON =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 96 96'%3E%3Crect width='96' height='96' rx='22' fill='%23fff4c7'/%3E%3Cpath d='M20 42h56v34H20z' fill='%23ff9900'/%3E%3Cpath d='M17 32h62v14H17z' fill='%23ffd84d'/%3E%3Cpath d='M44 28h8v48h-8z' fill='%23fff' fill-opacity='.86'/%3E%3Cpath d='M31 23c0-7 9-9 17 7-13 1-17-1-17-7zm34 0c0-7-9-9-17 7 13 1 17-1 17-7z' fill='%23ff5a5f'/%3E%3C/svg%3E"

const BROKEN_IMAGE_HOSTS = new Set(["image.imhtb.cn", "api.iconify.design", "q1.qlogo.cn"])
const BARE_FILE_RE = /^[^:/?#]+(?:\.(?:svg|png|jpe?g|webp|gif))?(?:[?#].*)?$/i
const isHttpsPage = () => globalThis.location?.protocol === "https:"

export function resolveSafeImageUrl(url, fallback = FALLBACK_AVATAR) {
  if (!url) {
    return fallback
  }

  const rawUrl = String(url).trim()
  if (!rawUrl) {
    return fallback
  }

  if (rawUrl.startsWith("data:") || rawUrl.startsWith("blob:") || rawUrl.startsWith("/")) {
    return rawUrl
  }

  if (BARE_FILE_RE.test(rawUrl)) {
    return fallback
  }

  try {
    const parsed = new URL(rawUrl, globalThis.location?.origin || "http://localhost")
    if (parsed.protocol === "http:" && isHttpsPage()) {
      return fallback
    }
    if (BROKEN_IMAGE_HOSTS.has(parsed.hostname)) {
      const pathname = decodeURIComponent(parsed.pathname || "")
      if (/avatar/i.test(pathname)) {
        return FALLBACK_AVATAR
      }
      if (/plane|rocket|gift|present|飞机|火箭/i.test(pathname)) {
        return FALLBACK_GIFT_ICON
      }
      return fallback
    }
  } catch (error) {
    return fallback
  }

  return rawUrl
}

export function onImgError(event, fallback = FALLBACK_AVATAR) {
  const img = event.target || event.currentTarget
  if (img && img.tagName === "IMG") {
    img.src = fallback
  }
}
