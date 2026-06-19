import test from "node:test"
import assert from "node:assert/strict"

import {
  FALLBACK_AVATAR,
  FALLBACK_GIFT_ICON,
  resolveSafeImageUrl,
} from "../../src/utils/fallback.js"

test("legacy image.imhtb gift icons are replaced with local fallback icons", () => {
  assert.equal(resolveSafeImageUrl("http://image.imhtb.cn/飞机.png", ""), FALLBACK_GIFT_ICON)
  assert.equal(resolveSafeImageUrl("https://image.imhtb.cn/%E9%A3%9E%E6%9C%BA1.png", ""), FALLBACK_GIFT_ICON)
})

test("bare gift icon filenames use local fallback instead of root-domain requests", () => {
  assert.equal(resolveSafeImageUrl("rocket.svg", FALLBACK_GIFT_ICON), FALLBACK_GIFT_ICON)
  assert.equal(resolveSafeImageUrl("star.svg", FALLBACK_GIFT_ICON), FALLBACK_GIFT_ICON)
})

test("legacy image.imhtb avatar is replaced with local avatar fallback", () => {
  assert.equal(resolveSafeImageUrl("http://image.imhtb.cn/avatar.png", ""), FALLBACK_AVATAR)
})

test("safe relative and data image urls are kept as-is", () => {
  assert.equal(resolveSafeImageUrl("/demo-covers/tech-lab.jpg", ""), "/demo-covers/tech-lab.jpg")
  assert.equal(resolveSafeImageUrl("data:image/png;base64,abc", ""), "data:image/png;base64,abc")
})
