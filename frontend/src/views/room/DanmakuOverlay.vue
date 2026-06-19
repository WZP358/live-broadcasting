<template>
  <div class="danmaku-layer" aria-hidden="true">
    <div
      v-for="item in visibleItems"
      :key="item.id"
      class="danmaku-item"
      :class="{ 'danmaku-item--self': item.isSelf }"
      :style="item.style"
      @animationend="removeItem(item.id)"
    >
      <span v-if="item.nickname" class="danmaku-name">{{ item.nickname }}：</span>
      <span class="danmaku-text">{{ item.text }}</span>
    </div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref } from "vue"

const props = defineProps({
  enabled: {
    type: Boolean,
    default: true,
  },
  maxItems: {
    type: Number,
    default: 24,
  },
  tracks: {
    type: Number,
    default: 7,
  },
})

const visibleItems = ref([])
const timers = new Map()
let serial = 0
let lastTrack = -1

const cleanText = (value = "") => String(value || "").replace(/\s+/g, " ").trim().slice(0, 80)
const cleanName = (value = "") => String(value || "").replace(/\s+/g, "").trim().slice(0, 12)

const pickTrack = () => {
  const total = Math.max(1, Number(props.tracks) || 1)
  lastTrack = (lastTrack + 1) % total
  return lastTrack
}

const getColorIndex = (text) => {
  let hash = 0
  for (const char of text) {
    hash = (hash + char.charCodeAt(0)) % 4
  }
  return hash
}

const removeItem = (id) => {
  const timer = timers.get(id)
  if (timer) {
    window.clearTimeout(timer)
  }
  timers.delete(id)
  visibleItems.value = visibleItems.value.filter((item) => item.id !== id)
}

const clear = () => {
  timers.forEach((timer) => window.clearTimeout(timer))
  timers.clear()
  visibleItems.value = []
}

const push = (message = {}) => {
  if (!props.enabled) return

  const text = cleanText(message.text ?? message.content)
  if (!text) return

  const nickname = cleanName(message.nickname || message.username || "")
  const track = pickTrack()
  const duration = Math.min(12, Math.max(7.2, 5.8 + text.length * 0.18))
  const delay = Math.min(0.8, track * 0.08)
  const id = `${Date.now()}-${++serial}`
  const colorIndex = getColorIndex(text)

  const item = {
    id,
    text,
    nickname,
    isSelf: Boolean(message.isSelf),
    style: {
      top: `calc(14px + ${track} * var(--danmaku-track-height))`,
      animationDelay: `${delay}s`,
      animationDuration: `${duration}s`,
      "--danmaku-color": `var(--danmaku-color-${colorIndex})`,
    },
  }

  visibleItems.value = visibleItems.value.concat(item).slice(-props.maxItems)
  timers.set(id, window.setTimeout(() => removeItem(id), (duration + delay + 0.5) * 1000))
}

defineExpose({
  push,
  clear,
})

onBeforeUnmount(clear)
</script>

<style scoped>
.danmaku-layer {
  position: absolute;
  inset: 0;
  z-index: 8;
  overflow: hidden;
  pointer-events: none;
  --danmaku-track-height: 42px;
  --danmaku-color-0: #ffffff;
  --danmaku-color-1: #ffd166;
  --danmaku-color-2: #8be9fd;
  --danmaku-color-3: #b7f7c1;
}

.danmaku-item {
  position: absolute;
  left: 100%;
  display: inline-flex;
  align-items: center;
  max-width: min(76%, 720px);
  height: 31px;
  padding: 0 12px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  color: var(--danmaku-color);
  background: rgba(0, 0, 0, 0.32);
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.18);
  font-size: 15px;
  font-weight: 800;
  line-height: 31px;
  text-shadow: 0 1px 2px rgba(0, 0, 0, 0.9);
  white-space: nowrap;
  will-change: transform;
  animation-name: danmaku-move;
  animation-timing-function: linear;
  animation-fill-mode: forwards;
}

.danmaku-item--self {
  border-color: rgba(255, 159, 26, 0.44);
  background: rgba(255, 159, 26, 0.22);
}

.danmaku-name {
  flex: 0 0 auto;
  color: #ffb020;
}

.danmaku-text {
  overflow: hidden;
  text-overflow: ellipsis;
}

@keyframes danmaku-move {
  from {
    transform: translateX(0);
  }

  to {
    transform: translateX(calc(-100vw - 100% - 80px));
  }
}

@media (max-width: 760px) {
  .danmaku-layer {
    --danmaku-track-height: 34px;
  }

  .danmaku-item {
    height: 27px;
    padding: 0 10px;
    font-size: 13px;
    line-height: 27px;
  }
}
</style>
