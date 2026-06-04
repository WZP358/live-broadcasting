<template>
  <main class="live-studio-page">
    <section class="studio-page-head">
      <div class="head-main">
        <span class="head-kicker">主播中心</span>
        <h1>{{ roomInfo.title || "开播工作台" }}</h1>
        <p>{{ headSubtitle }}</p>
      </div>
      <div class="head-actions">
        <div class="status-card" :class="{ active: isLiving }">
          <span>直播状态</span>
          <strong>{{ liveStatusText }}</strong>
        </div>
        <a-button size="large" @click="goSettings">房间设置</a-button>
        <a-button v-if="roomInfo.id" size="large" @click="goRoom">查看直播间</a-button>
      </div>
    </section>

    <BrowserLivePanel
      :room-id="roomInfo.id"
      :live-status="roomLiveInfo.liveStatus || 0"
      @status-change="refreshState"
    />
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from "vue"
import { useRouter } from "vue-router"
import liveAPI from "@/api/live"
import BrowserLivePanel from "@/views/center/live-settings/BrowserLivePanel.vue"

const router = useRouter()
const roomInfo = ref({})
const roomLiveInfo = ref({})

const isLiving = computed(() => Number(roomLiveInfo.value?.liveStatus || 0) === 1)
const liveStatusText = computed(() => (isLiving.value ? "直播中" : "未开播"))
const headSubtitle = computed(() =>
  isLiving.value ? "直播正在进行，可以在这里管理画面、字幕、声音和互动。" : "选择屏幕或摄像头，准备好后即可开始直播。"
)

const getRoomInfo = async () => {
  const res = await liveAPI.getRoomSettingsInfo()
  roomInfo.value = res.data || {}
}

const getLiveStatus = async () => {
  const res = await liveAPI.getLiveStatus()
  roomLiveInfo.value = res.data || {}
}

const refreshState = async () => {
  await Promise.all([getRoomInfo(), getLiveStatus()])
}

const goSettings = () => {
  router.push("/center/live/live-settings")
}

const goRoom = () => {
  router.push(`/room/${roomInfo.value.id}`)
}

onMounted(() => {
  refreshState()
})
</script>

<style scoped lang="scss">
.live-studio-page {
  width: min(100%, 1560px);
  margin: 0 auto;
  padding: 18px 20px 38px;
}

.studio-page-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
  padding: 18px 20px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: var(--shadow);
}

.head-main {
  min-width: 0;
}

.head-kicker {
  display: inline-flex;
  height: 24px;
  align-items: center;
  padding: 0 9px;
  border-radius: 4px;
  color: var(--accent-strong);
  background: var(--accent-light);
  font-size: 12px;
  font-weight: 900;
}

.head-main h1 {
  margin: 10px 0 4px;
  overflow: hidden;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 900;
  line-height: 1.25;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.head-main p {
  margin: 0;
  color: var(--text-secondary);
  font-size: 13px;
}

.head-actions {
  display: flex;
  flex: 0 0 auto;
  align-items: center;
  gap: 12px;
}

.status-card {
  min-width: 116px;
  padding: 9px 12px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-secondary);
}

.status-card span,
.status-card strong {
  display: block;
}

.status-card span {
  color: var(--text-muted);
  font-size: 12px;
}

.status-card strong {
  margin-top: 3px;
  color: var(--text-primary);
  font-size: 14px;
  font-weight: 900;
}

.status-card.active strong {
  color: var(--danger);
}

@media (max-width: 920px) {
  .live-studio-page {
    padding: 12px;
  }

  .studio-page-head,
  .head-actions {
    align-items: stretch;
    flex-direction: column;
  }

  .head-main h1 {
    white-space: normal;
  }
}
</style>
