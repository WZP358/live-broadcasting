<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from "vue"
import { useRouter } from "vue-router"
import ChatApi from "@/api/chat"

const router = useRouter()
const props = defineProps({
  room: {
    type: Object,
    default: () => ({}),
  },
})

const roomId = computed(() => Number(props.room?.id || 0))
const popularity = ref(0)
const popularityTimer = ref(null)
const fallbackCover = "https://dummyimage.com/640x360/e2e8f0/64748b&text=LIVE"
const fallbackAvatar = "https://dummyimage.com/96x96/e2e8f0/64748b&text=A"

const handleItemClick = () => {
  router.push({ path: `/room/${props.room.id}` })
}

const formatPopularity = (value) => {
  const count = Number(value || 0)
  if (!Number.isFinite(count) || count <= 0) {
    return "0"
  }
  if (count >= 100000000) {
    return `${(count / 100000000).toFixed(1).replace(/\.0$/, "")}亿`
  }
  if (count >= 10000) {
    return `${(count / 10000).toFixed(1).replace(/\.0$/, "")}万`
  }
  return `${count}`
}

const roomSubtitle = computed(() => props.room?.introduce || props.room?.notice || "低延迟观看、实时互动、礼物和聊天能力已接入")
const anchorName = computed(() => props.room?.userInfo?.name || props.room?.userInfo?.nickName || "主播")
const categoryName = computed(() => props.room?.categoryInfo?.name || "推荐")
const qualityText = computed(() => (props.room?.browserLive ? "低延迟" : "稳定播放"))
const isBrowserLive = computed(() => Boolean(props.room?.browserLive))

const loadPopularity = async () => {
  if (!roomId.value) {
    popularity.value = 0
    return
  }
  try {
    const res = await ChatApi.getPopularity({ roomId: roomId.value })
    popularity.value = Number(res?.data || 0)
  } catch (error) {
    popularity.value = Number(props.room?.popularity || 0)
  }
}

const clearPopularityTimer = () => {
  if (popularityTimer.value) {
    clearInterval(popularityTimer.value)
    popularityTimer.value = null
  }
}

const startPopularityPolling = () => {
  clearPopularityTimer()
  loadPopularity()
  popularityTimer.value = setInterval(() => {
    loadPopularity()
  }, 12000)
}

onMounted(() => {
  startPopularityPolling()
})

watch(roomId, () => {
  startPopularityPolling()
})

onBeforeUnmount(() => {
  clearPopularityTimer()
})
</script>

<template>
  <article class="live-room-card" @click="handleItemClick">
    <div class="card-cover-wrap">
      <img draggable="false" class="cover" :src="room.cover || fallbackCover" />
      <div class="cover-overlay"></div>
      <div class="cover-top">
        <span class="channel-pill">{{ categoryName }}</span>
        <span class="quality-badge">{{ qualityText }}</span>
      </div>
      <div class="cover-bottom">
        <span class="status-pill">直播中</span>
        <span class="hot-pill">{{ formatPopularity(popularity) }} 热度</span>
      </div>
    </div>

    <div class="live-room-info-container">
      <div class="title-row">
        <div class="room-title">{{ room.title }}</div>
        <span class="latency-dot" :class="{ active: isBrowserLive }"></span>
      </div>

      <div class="room-subtitle">{{ roomSubtitle }}</div>

      <div class="bottom">
        <div class="anchor-meta">
          <img class="avatar" :src="room.userInfo?.avatar || fallbackAvatar" />
          <div class="anchor-copy">
            <span class="nick-name">{{ anchorName }}</span>
            <span class="anchor-tag">{{ isBrowserLive ? "网页直播优先" : "拉流回退可用" }}</span>
          </div>
        </div>
        <span class="fire">
          <svg class="icon" aria-hidden="true">
            <use xlink:href="#icon-renqi"></use>
          </svg>
          <span>{{ formatPopularity(popularity) }}</span>
        </span>
      </div>
    </div>
  </article>
</template>

<style lang="scss" scoped>
.live-room-card {
  overflow: hidden;
  border: 1px solid rgba(15, 23, 42, 0.08);
  border-radius: 18px;
  background: #fff;
  cursor: pointer;
  transition: transform 0.22s ease, box-shadow 0.22s ease, border-color 0.22s ease;
  box-shadow: 0 16px 36px rgba(15, 23, 42, 0.05);
}

.live-room-card:hover {
  transform: translateY(-4px);
  border-color: rgba(0, 174, 236, 0.26);
  box-shadow: 0 22px 42px rgba(15, 23, 42, 0.12);
}

.card-cover-wrap {
  position: relative;
  aspect-ratio: 16 / 9;
  overflow: hidden;
  background: #0f172a;
}

.cover {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
  transition: transform 0.35s ease;
}

.live-room-card:hover .cover {
  transform: scale(1.04);
}

.cover-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(2, 6, 23, 0.08) 0%, rgba(2, 6, 23, 0.7) 100%);
}

.cover-top,
.cover-bottom {
  position: absolute;
  left: 12px;
  right: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  z-index: 1;
}

.cover-top {
  top: 12px;
}

.cover-bottom {
  bottom: 12px;
}

.channel-pill,
.quality-badge,
.status-pill,
.hot-pill {
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 999px;
  color: #fff;
  font-size: 12px;
  backdrop-filter: blur(8px);
}

.channel-pill {
  background: rgba(17, 24, 39, 0.55);
}

.quality-badge {
  background: rgba(0, 174, 236, 0.82);
}

.status-pill {
  background: rgba(251, 114, 153, 0.92);
}

.hot-pill {
  background: rgba(17, 24, 39, 0.55);
}

.live-room-info-container {
  padding: 16px;
}

.title-row {
  display: flex;
  align-items: flex-start;
  gap: 10px;
}

.room-title {
  flex: 1;
  min-height: 48px;
  color: #18191c;
  font-size: 16px;
  font-weight: 700;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.latency-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #cbd5e1;
  margin-top: 8px;
  flex-shrink: 0;
}

.latency-dot.active {
  background: #22c55e;
  box-shadow: 0 0 0 6px rgba(34, 197, 94, 0.12);
}

.room-subtitle {
  min-height: 42px;
  margin-top: 10px;
  color: #61666d;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 14px;
}

.anchor-meta {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: 10px;
}

.avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  object-fit: cover;
  background: #e2e8f0;
}

.anchor-copy {
  min-width: 0;
}

.nick-name,
.anchor-tag {
  display: block;
}

.nick-name {
  max-width: 160px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #4b5563;
  font-size: 13px;
}

.anchor-tag {
  margin-top: 2px;
  color: #94a3b8;
  font-size: 12px;
}

.fire {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #fb7299;
  font-size: 13px;
  font-weight: 700;
}

.icon {
  width: 14px;
  height: 14px;
}
</style>
