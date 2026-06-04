<template>
  <div class="gift-effects-layer">
    <!-- 火箭升空 -->
    <div v-for="rocket in rockets" :key="rocket.id" class="rocket-effect" :style="rocket.style">
      <div class="rocket-body">🚀</div>
      <div class="rocket-trail">
        <span v-for="i in 8" :key="i" class="trail-particle" :style="{ animationDelay: i * 0.08 + 's' }">🔥</span>
      </div>
      <div class="rocket-explosion" v-if="rocket.exploding">
        <span v-for="i in 12" :key="i" class="explosion-particle"
          :style="{ '--angle': (i * 30) + 'deg', '--delay': (i * 0.05) + 's' }">✨</span>
      </div>
    </div>

    <!-- 满屏小心心 -->
    <div v-for="heart in hearts" :key="heart.id" class="heart-float" :style="heart.style">
      <span :class="['heart-icon', heart.color]">{{ heart.icon }}</span>
    </div>

    <!-- 飞机飞过 -->
    <div v-for="plane in planes" :key="plane.id" class="plane-effect" :style="plane.style">
      <div class="plane-body">✈️</div>
      <div class="plane-banner">{{ plane.text }}</div>
    </div>

    <!-- 钻石闪烁 -->
    <div v-for="diamond in diamonds" :key="diamond.id" class="diamond-effect" :style="diamond.style">
      <div class="diamond-core">💎</div>
      <div class="diamond-sparkle">
        <span v-for="i in 6" :key="i" class="sparkle-ray" :style="{ '--ray': (i * 60) + 'deg' }"></span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount } from "vue"

const rockets = ref([])
const hearts = ref([])
const planes = ref([])
const diamonds = ref([])
let counter = 0

// 礼物 → 动画类型映射
const GIFT_EFFECTS = {
  "火箭": "rocket",
  "飞机": "plane",
  "钻石": "diamond",
  "小心心": "heart",
  "爱心": "heart",
  "heart": "heart",
  "like": "heart",
  // 可扩展更多礼物类型
}

const triggerEffect = (effectType, giftName = "", senderName = "") => {
  const id = ++counter

  switch (effectType) {
    case "rocket":
      rockets.value.push({
        id,
        style: {
          left: Math.random() * 80 + 10 + "%",
          animationDuration: 2 + Math.random() + "s",
        },
        exploding: false,
      })
      setTimeout(() => {
        const idx = rockets.value.findIndex(r => r.id === id)
        if (idx >= 0) rockets.value[idx].exploding = true
      }, 1800)
      setTimeout(() => {
        rockets.value = rockets.value.filter(r => r.id !== id)
      }, 3500)
      break

    case "heart":
      const colors = ["heart-red", "heart-pink", "heart-purple", "heart-orange"]
      const icons = ["❤️", "💕", "💗", "💖", "💝", "🩷"]
      const count = giftName.includes("小心心") ? 15 : 5
      for (let i = 0; i < count; i++) {
        const heartId = ++counter
        hearts.value.push({
          id: heartId,
          color: colors[i % colors.length],
          icon: icons[i % icons.length],
          style: {
            left: Math.random() * 90 + 5 + "%",
            animationDuration: 2.5 + Math.random() * 2 + "s",
            animationDelay: i * 0.08 + "s",
            fontSize: 18 + Math.random() * 22 + "px",
          },
        })
        setTimeout(() => {
          hearts.value = hearts.value.filter(h => h.id !== heartId)
        }, 5000 + i * 80)
      }
      break

    case "plane":
      planes.value.push({
        id,
        text: `${senderName} 送出 ${giftName}`,
        style: {
          top: Math.random() * 60 + 10 + "%",
        },
      })
      setTimeout(() => {
        planes.value = planes.value.filter(p => p.id !== id)
      }, 5000)
      break

    case "diamond":
      diamonds.value.push({
        id,
        style: {
          left: Math.random() * 70 + 15 + "%",
          top: Math.random() * 50 + 20 + "%",
        },
      })
      setTimeout(() => {
        diamonds.value = diamonds.value.filter(d => d.id !== id)
      }, 3000)
      break
  }
}

// 默认触发映射：根据礼物名自动选择动画
const playGiftEffect = (giftName, senderName) => {
  const lower = (giftName || "").toLowerCase()
  for (const [key, effect] of Object.entries(GIFT_EFFECTS)) {
    if (lower.includes(key.toLowerCase())) {
      triggerEffect(effect, giftName, senderName)
      return
    }
  }
  // 默认：小心心
  triggerEffect("heart", giftName, senderName)
}

defineExpose({ triggerEffect, playGiftEffect })
</script>

<style scoped lang="scss">
.gift-effects-layer {
  position: fixed;
  inset: 0;
  pointer-events: none;
  z-index: 9999;
  overflow: hidden;
}

// ─── 火箭 ─────────────────────────────────
.rocket-effect {
  position: absolute;
  bottom: -60px;
  animation: rocketLaunch 2.5s ease-out forwards;
}

.rocket-body {
  font-size: 52px;
  filter: drop-shadow(0 4px 12px rgba(255, 107, 0, 0.5));
}

.rocket-trail {
  position: absolute;
  bottom: -30px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column-reverse;
  align-items: center;
}

.trail-particle {
  font-size: 14px;
  animation: trailFade 0.6s ease-out forwards;
  opacity: 0;
}

@keyframes rocketLaunch {
  0% { bottom: -60px; opacity: 0; transform: scale(0.5); }
  15% { opacity: 1; transform: scale(1); }
  85% { opacity: 1; }
  100% { bottom: 110%; opacity: 0; transform: scale(0.3) rotate(15deg); }
}

@keyframes trailFade {
  0% { opacity: 1; transform: translateY(0) scale(1); }
  100% { opacity: 0; transform: translateY(-30px) scale(0.3); }
}

// 爆炸粒子
.rocket-explosion {
  position: absolute;
  top: -20px;
  left: 50%;
  transform: translateX(-50%);
}

.explosion-particle {
  position: absolute;
  font-size: 20px;
  animation: explode 1s ease-out forwards;
  animation-delay: var(--delay);
  opacity: 0;
  transform: rotate(var(--angle)) translateY(0);
}

@keyframes explode {
  0% { opacity: 1; transform: rotate(var(--angle)) translateY(0) scale(1); }
  100% { opacity: 0; transform: rotate(var(--angle)) translateY(-120px) scale(0.2); }
}

// ─── 满屏小心心 ─────────────────────────
.heart-float {
  position: absolute;
  bottom: -40px;
  animation: heartFloat 4s ease-out forwards;
  animation-delay: var(--delay, 0s);
}

.heart-icon {
  display: block;
  animation: heartWiggle 0.6s ease-in-out infinite;
}

.heart-red { filter: drop-shadow(0 0 8px #ef4444); }
.heart-pink { filter: drop-shadow(0 0 8px #ec4899); }
.heart-purple { filter: drop-shadow(0 0 8px #a855f7); }
.heart-orange { filter: drop-shadow(0 0 8px #f97316); }

@keyframes heartFloat {
  0% { bottom: -40px; opacity: 0; transform: translateX(0) rotate(0deg); }
  10% { opacity: 1; }
  90% { opacity: 1; }
  100% { bottom: 110%; opacity: 0; transform: translateX(30px) rotate(25deg); }
}

@keyframes heartWiggle {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.3); }
}

// ─── 飞机 ─────────────────────────────────
.plane-effect {
  position: absolute;
  left: -100px;
  animation: planeFly 5s linear forwards;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.plane-body {
  font-size: 48px;
  filter: drop-shadow(0 4px 12px rgba(255, 153, 0, 0.5));
}

.plane-banner {
  padding: 4px 14px;
  border-radius: 14px;
  background: linear-gradient(135deg, #ffd84d, #ff9900);
  color: #1f232b;
  font-size: 13px;
  font-weight: 600;
  white-space: nowrap;
  box-shadow: 0 4px 16px rgba(255, 153, 0, 0.34);
}

@keyframes planeFly {
  0% { left: -120px; }
  100% { left: 110%; }
}

// ─── 钻石 ─────────────────────────────────
.diamond-effect {
  position: absolute;
  animation: diamondPulse 1.5s ease-in-out infinite;
}

.diamond-core {
  font-size: 56px;
  filter: drop-shadow(0 0 20px rgba(168, 85, 247, 0.6));
  animation: diamondSpin 2s linear infinite;
}

.diamond-sparkle {
  position: absolute;
  inset: -10px;
}

.sparkle-ray {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 3px;
  height: 30px;
  background: linear-gradient(to top, #c084fc, transparent);
  transform-origin: bottom center;
  transform: translate(-50%, -100%) rotate(var(--ray));
  animation: sparkleFade 1s ease-out infinite;
  animation-delay: calc(var(--ray) * 0.01s);
  border-radius: 2px;
}

@keyframes diamondPulse {
  0%, 100% { transform: scale(1); opacity: 1; }
  50% { transform: scale(1.3); opacity: 0.8; }
}

@keyframes diamondSpin {
  from { transform: rotateY(0deg); }
  to { transform: rotateY(360deg); }
}

@keyframes sparkleFade {
  0%, 100% { opacity: 0.3; }
  50% { opacity: 1; }
}
</style>
