/**
 * 本地 SVG 回退图片 — 不依赖任何外部服务，永不断链
 */
export const FALLBACK_AVATAR =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 96 96'%3E%3Crect fill='%23e2e8f0' width='96' height='96' rx='16'/%3E%3Ccircle fill='%2394a3b8' cx='48' cy='38' r='14'/%3E%3Cellipse fill='%2394a3b8' cx='48' cy='80' rx='24' ry='16'/%3E%3C/svg%3E"

export const FALLBACK_COVER =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 640 360'%3E%3Crect fill='%23f1f5f9' width='640' height='360'/%3E%3Ctext fill='%2394a3b8' font-family='sans-serif' font-size='20' text-anchor='middle' x='320' y='180'%3E暂无封面%3C/text%3E%3C/svg%3E"

/** 图片加载失败时设置 src 为回退图 */
export function onImgError(event, fallback = FALLBACK_AVATAR) {
  const img = event.target || event.currentTarget
  if (img && img.tagName === "IMG") {
    img.src = fallback
  }
}
