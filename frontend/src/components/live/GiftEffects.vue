<template>
  <div class="gift-effects-layer">
    <div class="gift-toast-stack">
      <div
        v-for="toast in toasts"
        :key="toast.id"
        class="gift-toast"
        :class="`gift-toast--${toast.tier}`"
      >
        <span class="gift-toast__icon">{{ toast.icon }}</span>
        <span class="gift-toast__copy">
          <b>{{ toast.senderName || "观众" }}</b>
          <span>送出 {{ toast.giftName }} x {{ toast.count }}</span>
        </span>
      </div>
    </div>

    <div v-for="burst in bursts" :key="burst.id" class="gift-burst" :class="`gift-burst--${burst.tier}`" :style="burst.style">
      <span class="gift-burst__halo"></span>
      <span class="gift-burst__core">{{ burst.icon }}</span>
      <span
        v-for="i in burst.particleCount"
        :key="i"
        class="gift-burst__particle"
        :style="{ '--angle': `${(360 / burst.particleCount) * i}deg`, '--delay': `${i * 0.035}s` }"
      ></span>
    </div>

    <div v-for="float in floats" :key="float.id" class="gift-float" :style="float.style">
      <span>{{ float.icon }}</span>
    </div>

    <div v-for="streak in streaks" :key="streak.id" class="gift-streak" :class="`gift-streak--${streak.tier}`" :style="streak.style">
      <span class="gift-streak__light"></span>
      <span class="gift-streak__icon">{{ streak.icon }}</span>
      <span class="gift-streak__text">{{ streak.senderName || "观众" }} 送出 {{ streak.giftName }}</span>
    </div>

    <div v-for="combo in combos" :key="combo.id" class="gift-combo" :class="`gift-combo--${combo.tier}`">
      <span>{{ combo.giftName }}</span>
      <strong>x{{ combo.count }}</strong>
    </div>
  </div>
</template>

<script setup>
import { ref } from "vue"

const toasts = ref([])
const bursts = ref([])
const floats = ref([])
const streaks = ref([])
const combos = ref([])
let counter = 0

const EFFECT_KEYWORDS = [
  { pattern: /火箭|rocket/i, icon: "🚀", type: "rocket" },
  { pattern: /飞机|plane/i, icon: "✈️", type: "plane" },
  { pattern: /钻石|diamond/i, icon: "💎", type: "diamond" },
  { pattern: /冠军|奖杯|trophy/i, icon: "🏆", type: "trophy" },
  { pattern: /星光|star/i, icon: "⭐", type: "star" },
  { pattern: /应援|荧光|棒|glow/i, icon: "✨", type: "spark" },
  { pattern: /饮料|能量|beverage/i, icon: "🥤", type: "boost" },
  { pattern: /心|爱|heart|like/i, icon: "💗", type: "heart" },
]

const resolveGiftMeta = (giftName = "", options = {}) => {
  const matched = EFFECT_KEYWORDS.find((item) => item.pattern.test(giftName || ""))
  return {
    type: matched?.type || "star",
    icon: options.iconEmoji || matched?.icon || "🎁",
  }
}

const resolveTier = (giftName = "", count = 1, options = {}) => {
  const total = Number(options.totalPrice || 0)
  const unit = Number(options.price || options.unitPrice || 0)
  if (total >= 1000 || unit >= 188 || count >= 188 || /超级|火箭|飞机/.test(giftName)) return "legend"
  if (total >= 300 || unit >= 88 || count >= 66 || /冠军|奖杯|钻石/.test(giftName)) return "epic"
  if (total >= 80 || unit >= 38 || count >= 10) return "rare"
  return "normal"
}

const clearLater = (collection, id, delay) => {
  setTimeout(() => {
    collection.value = collection.value.filter((item) => item.id !== id)
  }, delay)
}

const addToast = (payload) => {
  const id = ++counter
  toasts.value.unshift({ id, ...payload })
  toasts.value = toasts.value.slice(0, 3)
  clearLater(toasts, id, 3600)
}

const addBurst = (payload) => {
  const id = ++counter
  bursts.value.push({
    id,
    ...payload,
    particleCount: payload.tier === "legend" ? 22 : payload.tier === "epic" ? 18 : 12,
    style: {
      left: `${28 + Math.random() * 44}%`,
      top: `${24 + Math.random() * 30}%`,
    },
  })
  clearLater(bursts, id, payload.tier === "legend" ? 2800 : 2200)
}

const addFloats = ({ icon, tier, count }) => {
  const floatCount = Math.min(tier === "legend" ? 34 : tier === "epic" ? 24 : tier === "rare" ? 16 : 9, Math.max(8, count))
  for (let i = 0; i < floatCount; i++) {
    const id = ++counter
    floats.value.push({
      id,
      icon,
      style: {
        left: `${6 + Math.random() * 88}%`,
        animationDelay: `${i * 0.045}s`,
        animationDuration: `${2.4 + Math.random() * 1.8}s`,
        fontSize: `${18 + Math.random() * (tier === "legend" ? 28 : 18)}px`,
        "--drift": `${Math.random() > 0.5 ? "" : "-"}${18 + Math.random() * 44}px`,
      },
    })
    clearLater(floats, id, 5200 + i * 45)
  }
}

const addStreak = (payload) => {
  const id = ++counter
  streaks.value.push({
    id,
    ...payload,
    style: {
      top: `${18 + Math.random() * 42}%`,
    },
  })
  clearLater(streaks, id, payload.tier === "legend" ? 5200 : 4300)
}

const addCombo = (payload) => {
  if (Number(payload.count || 1) <= 1) return
  const id = ++counter
  combos.value.push({ id, ...payload })
  clearLater(combos, id, 2600)
}

const playGiftEffect = (giftName, senderName = "", options = {}) => {
  const count = Math.max(1, Number(options.count || options.number || 1))
  const meta = resolveGiftMeta(giftName, options)
  const tier = resolveTier(giftName, count, options)
  const payload = {
    giftName: giftName || "礼物",
    senderName,
    count,
    icon: meta.icon,
    tier,
  }

  addToast(payload)
  addCombo(payload)

  if (tier === "legend" || meta.type === "rocket" || meta.type === "plane") {
    addStreak(payload)
  }

  if (tier !== "normal" || ["rocket", "diamond", "trophy"].includes(meta.type)) {
    addBurst(payload)
  }

  addFloats(payload)
}

const triggerEffect = (effectType, giftName = "", senderName = "") => {
  const effectMap = {
    rocket: "超级火箭",
    plane: "飞机",
    diamond: "钻石",
    heart: "小心心",
  }
  playGiftEffect(giftName || effectMap[effectType] || "礼物", senderName, {
    count: effectType === "heart" ? 10 : 1,
  })
}

defineExpose({ triggerEffect, playGiftEffect })
</script>

<style scoped lang="scss">
.gift-effects-layer {
  position: fixed;
  inset: 0;
  z-index: 9999;
  overflow: hidden;
  pointer-events: none;
}

.gift-toast-stack {
  position: absolute;
  top: 92px;
  left: 50%;
  display: grid;
  gap: 8px;
  width: min(420px, calc(100vw - 28px));
  transform: translateX(-50%);
}

.gift-toast {
  display: grid;
  grid-template-columns: 42px minmax(0, 1fr);
  align-items: center;
  gap: 10px;
  min-height: 54px;
  padding: 8px 14px 8px 8px;
  border: 1px solid color-mix(in srgb, var(--accent) 36%, rgba(255, 255, 255, 0.22));
  border-radius: 8px;
  color: var(--text-primary);
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--accent) 18%, var(--bg-card)), color-mix(in srgb, var(--bg-card) 86%, transparent)),
    var(--bg-card);
  box-shadow: 0 16px 42px rgba(0, 0, 0, 0.18);
  animation: giftToastIn 3.6s ease forwards;
}

.gift-toast__icon {
  display: grid;
  place-items: center;
  width: 42px;
  height: 38px;
  border-radius: 8px;
  background: var(--accent-light);
  font-size: 24px;
}

.gift-toast__copy {
  min-width: 0;
}

.gift-toast__copy b,
.gift-toast__copy span {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gift-toast__copy b {
  color: var(--accent);
  font-size: 13px;
  font-weight: 900;
}

.gift-toast__copy span {
  margin-top: 2px;
  color: var(--text-primary);
  font-size: 13px;
  font-weight: 800;
}

.gift-toast--epic,
.gift-toast--legend {
  border-color: color-mix(in srgb, var(--warning) 58%, var(--border));
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--warning) 18%, var(--bg-card)), color-mix(in srgb, var(--bg-card) 86%, transparent)),
    var(--bg-card);
}

.gift-toast--legend {
  transform-origin: center;
  animation: giftToastIn 3.6s ease forwards, legendPulse 0.86s ease-in-out 2;
}

.gift-burst {
  position: absolute;
  width: 150px;
  height: 150px;
  transform: translate(-50%, -50%);
  animation: burstOut 2.2s ease forwards;
}

.gift-burst__halo,
.gift-burst__core,
.gift-burst__particle {
  position: absolute;
  top: 50%;
  left: 50%;
}

.gift-burst__halo {
  width: 130px;
  height: 130px;
  border: 1px solid color-mix(in srgb, var(--accent) 42%, transparent);
  border-radius: 50%;
  background: radial-gradient(circle, color-mix(in srgb, var(--accent) 20%, transparent), transparent 66%);
  transform: translate(-50%, -50%);
  animation: haloPulse 1.7s ease-out forwards;
}

.gift-burst__core {
  z-index: 2;
  font-size: 60px;
  filter: drop-shadow(0 12px 22px rgba(0, 0, 0, 0.22));
  transform: translate(-50%, -50%);
  animation: corePop 1.8s ease forwards;
}

.gift-burst__particle {
  width: 7px;
  height: 20px;
  border-radius: 999px;
  background: linear-gradient(180deg, var(--accent), transparent);
  opacity: 0;
  transform: translate(-50%, -50%) rotate(var(--angle)) translateY(0);
  animation: particleFly 1.35s ease-out forwards;
  animation-delay: var(--delay);
}

.gift-burst--epic .gift-burst__particle,
.gift-burst--legend .gift-burst__particle {
  background: linear-gradient(180deg, var(--warning), transparent);
}

.gift-burst--legend .gift-burst__halo {
  width: 170px;
  height: 170px;
  border-color: color-mix(in srgb, var(--warning) 70%, transparent);
  background: radial-gradient(circle, color-mix(in srgb, var(--warning) 26%, transparent), transparent 68%);
}

.gift-float {
  position: absolute;
  bottom: -44px;
  animation: floatUp 4s ease-out forwards;
  animation-delay: var(--delay, 0s);
}

.gift-float span {
  display: block;
  filter: drop-shadow(0 8px 12px rgba(0, 0, 0, 0.2));
  animation: floatWiggle 0.86s ease-in-out infinite;
}

.gift-streak {
  position: absolute;
  left: -360px;
  display: grid;
  grid-template-columns: 76px minmax(0, auto);
  align-items: center;
  min-width: 330px;
  animation: streakFly 4.3s cubic-bezier(0.18, 0.78, 0.28, 1) forwards;
}

.gift-streak__light {
  position: absolute;
  inset: 12px 0 12px -120px;
  z-index: -1;
  border-radius: 999px;
  background: linear-gradient(90deg, transparent, color-mix(in srgb, var(--accent) 42%, transparent), transparent);
  filter: blur(2px);
}

.gift-streak__icon {
  font-size: 58px;
  filter: drop-shadow(0 8px 20px color-mix(in srgb, var(--accent) 42%, transparent));
}

.gift-streak__text {
  height: 38px;
  max-width: 270px;
  overflow: hidden;
  padding: 0 18px;
  border-radius: 19px;
  color: var(--accent-text);
  background: var(--accent-gradient);
  box-shadow: 0 12px 28px color-mix(in srgb, var(--accent) 30%, transparent);
  font-size: 14px;
  font-weight: 900;
  line-height: 38px;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gift-streak--legend {
  animation-duration: 5.2s;
}

.gift-streak--legend .gift-streak__text {
  color: #171b24;
  background: linear-gradient(135deg, #ffe08a 0%, #ffb020 42%, #ff6b35 100%);
}

.gift-combo {
  position: absolute;
  right: 48px;
  top: 42%;
  display: grid;
  justify-items: end;
  color: #fff;
  text-shadow: 0 6px 22px rgba(0, 0, 0, 0.38);
  animation: comboPunch 2.6s ease forwards;
}

.gift-combo span {
  max-width: 220px;
  overflow: hidden;
  color: var(--accent);
  font-size: 18px;
  font-weight: 900;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.gift-combo strong {
  color: #fff;
  font-size: 58px;
  font-weight: 900;
  line-height: 0.95;
}

.gift-combo--epic strong,
.gift-combo--legend strong {
  color: #ffb020;
}

@keyframes giftToastIn {
  0% { opacity: 0; transform: translateY(-18px) scale(0.96); }
  12%, 82% { opacity: 1; transform: translateY(0) scale(1); }
  100% { opacity: 0; transform: translateY(-10px) scale(0.98); }
}

@keyframes legendPulse {
  0%, 100% { box-shadow: 0 16px 42px rgba(0, 0, 0, 0.18); }
  50% { box-shadow: 0 16px 54px color-mix(in srgb, var(--warning) 36%, transparent); }
}

@keyframes burstOut {
  0% { opacity: 0; transform: translate(-50%, -50%) scale(0.62); }
  12%, 70% { opacity: 1; }
  100% { opacity: 0; transform: translate(-50%, -50%) scale(1.14); }
}

@keyframes haloPulse {
  0% { opacity: 0; transform: translate(-50%, -50%) scale(0.38); }
  36% { opacity: 1; }
  100% { opacity: 0; transform: translate(-50%, -50%) scale(1.55); }
}

@keyframes corePop {
  0% { transform: translate(-50%, -50%) scale(0.2) rotate(-18deg); }
  24% { transform: translate(-50%, -50%) scale(1.18) rotate(8deg); }
  100% { transform: translate(-50%, -50%) scale(0.94) rotate(0); }
}

@keyframes particleFly {
  0% { opacity: 0; transform: translate(-50%, -50%) rotate(var(--angle)) translateY(0) scale(0.6); }
  20% { opacity: 1; }
  100% { opacity: 0; transform: translate(-50%, -50%) rotate(var(--angle)) translateY(-112px) scale(0.1); }
}

@keyframes floatUp {
  0% { opacity: 0; transform: translateX(0) translateY(0) scale(0.72); }
  10%, 76% { opacity: 1; }
  100% { opacity: 0; transform: translateX(var(--drift)) translateY(-104vh) scale(1.08); }
}

@keyframes floatWiggle {
  0%, 100% { transform: rotate(-7deg) scale(1); }
  50% { transform: rotate(8deg) scale(1.18); }
}

@keyframes streakFly {
  0% { opacity: 0; transform: translateX(0) scale(0.84); }
  8%, 74% { opacity: 1; }
  100% { opacity: 0; transform: translateX(calc(100vw + 760px)) scale(1); }
}

@keyframes comboPunch {
  0% { opacity: 0; transform: translateY(26px) scale(0.62); }
  16% { opacity: 1; transform: translateY(0) scale(1.12); }
  72% { opacity: 1; transform: translateY(0) scale(1); }
  100% { opacity: 0; transform: translateY(-18px) scale(0.96); }
}

@media (max-width: 640px) {
  .gift-toast-stack {
    top: 72px;
  }

  .gift-streak {
    min-width: 270px;
    grid-template-columns: 58px minmax(0, auto);
  }

  .gift-streak__icon {
    font-size: 44px;
  }

  .gift-streak__text {
    max-width: 210px;
    height: 34px;
    font-size: 12px;
    line-height: 34px;
  }

  .gift-combo {
    right: 18px;
  }

  .gift-combo strong {
    font-size: 44px;
  }
}
</style>
