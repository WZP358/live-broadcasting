<template>
  <div class="chat-wrapper">
    <div class="chat-top-section" @mouseleave="showRankDropdown = false">
      <div class="chat-header">
        <div class="rank-toolbar">
          <span class="rank-title">本月亲密榜</span>
          <div class="rank-dropdown-wrapper" @mouseenter="showRankDropdown = true">
            <a-button type="link" size="small">TOP 10</a-button>
          </div>
        </div>
        <div class="rank-board">
          <div class="rank-item" :class="`rank-item-${item.rankNo}`" v-for="item in displayRanks" :key="item.key">
            <div class="rank-badge">NO.{{ item.rankNo }}</div>
            <img class="avatar" :src="safeAvatar(item.avatar)" @error="onImgError" />
            <span class="name">{{ item.nickName }}</span>
            <span class="charm">{{ formatIntimacy(item.intimacyValue) }}</span>
          </div>
        </div>
      </div>
      <div class="rank-dropdown-list" :class="{ 'is-open': showRankDropdown }" @mouseenter="showRankDropdown = true">
        <div class="rank-dropdown-item" v-for="item in rankList" :key="item.userId">
          <div class="rank-dropdown-left">
            <span class="rank-dropdown-no" :class="`rank-no-${item.rankNo}`">{{ item.rankNo }}</span>
            <img class="rank-dropdown-avatar" :src="safeAvatar(item.avatar)" @error="onImgError" />
            <span class="rank-dropdown-name">{{ item.nickName || `观众${item.userId}` }}</span>
          </div>
          <span class="rank-dropdown-value">{{ formatIntimacy(item.intimacyValue) }}</span>
        </div>
        <a-empty v-if="rankList.length === 0" description="本月还没有亲密值记录" />
      </div>
    </div>
    <div class="chat-tools">
      <span>弹幕互动</span>
      <div>
        <button :class="{ active: isUserScrolling }" type="button" @click="toggleScrollLock">
          {{ isUserScrolling ? "跟随" : "锁屏" }}
        </button>
        <button type="button" @click="clearMessages">清屏</button>
      </div>
    </div>
    <div class="chat-main" ref="scrollContainer" @scroll="handleScroll">
      <a-list size="small" :data-source="data">
        <template #renderItem="{ item }">
          <a-list-item>
            <MessageItem
              :data="item"
              :is-moderator="isModerator"
              @mute-user="handleMuteUser"
              @kick-user="handleKickUser"
              @report-message="handleReportMessage"
            />
          </a-list-item>
        </template>
      </a-list>
    </div>
    <div class="chat-footer">
      <p class="chat-safety">请勿轻信任何主播或个人提供的兼职信息，谨防受骗</p>
      <a-flex vertical>
        <a-textarea class="chat-box" v-model:value="messageText" :placeholder="mutedUntil > Date.now() ? '你已被禁言' : isLogin ? '发个弹幕，别刷屏' : '登录后才能发送弹幕'"
          show-count :maxlength="100" :disabled="!isLogin || mutedUntil > Date.now()" :auto-size="{ minRows: 2, maxRows: 2 }" />
        <div class="chat-btn-wrapper">
          <span class="popularity">{{ formatOnlineCount(popularity) }}在线</span>
          <a-button type="primary" size="small" @click="handleMessageSend" :disabled="!isLogin || mutedUntil > Date.now()">发送</a-button>
        </div>
      </a-flex>
    </div>
  </div>

</template>

<script setup>
import MessageItem from "./MessageItem.vue"
import { onMounted, ref, computed, nextTick, watch, onBeforeUnmount } from "vue"
import { useStore } from "@/stores"
import $modal from "@/utils/message"
import ChatApi from "@/api/chat"
import roomApi from "@/api/room"
import moderatorApi from "@/api/moderator"
import { appendChatMessages, createChatWebSocketUrl } from "@/utils/chatRoom"

const props = defineProps({
  roomId: {
    type: Number,
    default: null,
  },
  isModerator: {
    type: Boolean,
    default: false,
  },
})

const emits = defineEmits(["sendGift", "reportMessage", "messagesChange"])

const maxReconnectCount = ref(50)
const lockReconnect = ref(false)
const scrollContainer = ref(null)
const isUserScrolling = ref(false)
const roomId = computed(() => props.roomId)
const store = useStore()
const isLogin = computed(() => store.user().isLogin)
const popularity = ref(1)
const rankList = ref([])
const showRankDropdown = ref(false)
import { FALLBACK_AVATAR, onImgError, resolveSafeImageUrl } from "@/utils/fallback";
const fallbackAvatar = FALLBACK_AVATAR
const safeAvatar = (url) => resolveSafeImageUrl(url, FALLBACK_AVATAR)

let websocket = null
const reconnectTimer = ref()
const heartBeatTimer = ref()
const popularityInterval = ref()

const messageText = ref("")
const mutedUntil = ref(0) // timestamp when mute expires

const data = ref([])
const createPlaceholderRank = (rankNo) => ({
  key: `placeholder-${rankNo}`,
  rankNo,
  nickName: ["待上榜", "虚位以待", "冲榜中"][rankNo - 1] || "待上榜",
  avatar: "",
  intimacyValue: 0,
})

const displayRanks = computed(() => {
  const topThree = [1, 2, 3].map((rankNo) => {
    const item = rankList.value[rankNo - 1]
    return item ? { ...item, key: item.userId } : createPlaceholderRank(rankNo)
  })
  return [topThree[1], topThree[0], topThree[2]]
})

onMounted(async () => {
  initWebSocket()
  if (!isLogin.value) {
    addMessages({
      nickname: "系统消息",
      text: "游客可以观看直播，登录后可发送弹幕、送礼和参与亲密榜。",
      isSystem: true,
    })
  }
  getPopularity()
  startPopularityPolling()
  getIntimacyRank()
})

onBeforeUnmount(() => {
  close()
})

watch(data, () => {
  emits("messagesChange", data.value)
  if (!isUserScrolling.value) {
    scrollToBottom()
  }
})

watch(roomId, async (nextRoomId, prevRoomId) => {
  if (!nextRoomId || nextRoomId === prevRoomId) return
  close({ keepPopularity: true })
  data.value = []
  rankList.value = []
  mutedUntil.value = 0
  maxReconnectCount.value = 50
  lockReconnect.value = false
  initWebSocket()
  if (!isLogin.value) {
    addMessages({
      nickname: "系统消息",
      text: "游客可以观看直播，登录后可发送弹幕、送礼和参与亲密榜。",
      isSystem: true,
    })
  }
  await Promise.all([getPopularity(), getIntimacyRank()])
})

watch(isLogin, (loggedIn, wasLoggedIn) => {
  if (loggedIn === wasLoggedIn) return
  close({ keepPopularity: true })
  data.value = []
  mutedUntil.value = 0
  maxReconnectCount.value = 50
  lockReconnect.value = false
  initWebSocket()
  if (!loggedIn) {
    addMessages({
      nickname: "系统消息",
      text: "游客可以观看直播，登录后可发送弹幕、送礼和参与亲密榜。",
      isSystem: true,
    })
  }
})

const scrollToBottom = () => {
  nextTick(() => {
    if (scrollContainer.value) {
      scrollContainer.value.scrollTop = scrollContainer.value.scrollHeight
    }
  })
}

const toggleScrollLock = () => {
  if (isUserScrolling.value) {
    isUserScrolling.value = false
    scrollToBottom()
    return
  }
  isUserScrolling.value = true
}

const clearMessages = () => {
  data.value = []
  isUserScrolling.value = false
}

const addMessages = (payload) => {
  data.value = appendChatMessages(data.value, payload)
}

const handleScroll = () => {
  const el = scrollContainer.value
  if (!el) return
  const distanceToBottom = el.scrollHeight - el.scrollTop - el.clientHeight
  isUserScrolling.value = distanceToBottom > 72
}

/**
 * 发送消息
 */
const handleMessageSend = () => {
  if (!isLogin.value) {
    $modal.msgWarning("登录后才能发送弹幕")
    return
  }
  if (messageText.value.trim() === "") {
    return
  }
  let text = messageText.value.trim()
  messageText.value = ""
  ChatApi.sendChatMsg({
    roomId: roomId.value,
    text,
  }).catch((error) => {
    messageText.value = text
    $modal.msgError(error?.message || "弹幕发送失败，请稍后重试")
  })
}

const getIntimacyRank = async () => {
  try {
    const res = await roomApi.getIntimacyRank({ roomId: roomId.value }, { silentError: true })
    rankList.value = (res.data || []).map((item, index) => ({
      ...item,
      rankNo: item.rankNo || index + 1,
    }))
  } catch (error) {
    rankList.value = []
  }
}

const formatOnlineCount = (value) => {
  const count = Number(value || 0)
  if (!Number.isFinite(count) || count <= 0) return "0"
  if (count >= 10000) return `${(count / 10000).toFixed(1).replace(/\.0$/, "")}万`
  return `${count}`
}

const formatIntimacy = (value) => {
  const num = Number(value || 0)
  if (Number.isInteger(num)) {
    return `${num}`
  }
  return `${num.toFixed(2)}`
}

/**
 * 获取人气
 */
const getPopularity = async () => {
  try {
    const res = await ChatApi.getPopularity({ roomId: roomId.value }, { silentError: true })
    popularity.value = res?.data ?? 0
  } catch (error) {
    popularity.value = popularity.value || 0
  }
}

const startPopularityPolling = () => {
  popularityInterval.value = setInterval(async () => {
    await getPopularity()
  }, 10000)
}

const initWebSocket = () => {
  const token = store.user().userToken
  if (!roomId.value) {
    return
  }
  const wsUrl = createChatWebSocketUrl({ token })
  websocket = new WebSocket(wsUrl)
  websocket.onopen = () => {
    connetRoom()
    heartBeat()
  }
  websocket.onclose = (e) => {
    if (e.code === 1000) {
      return
    }
    if (e.code === 1005) {
      return
    }
    reconnectWebSocket()
  }
  websocket.onerror = () => {}
  websocket.onmessage = (event) => {
    let message
    try {
      message = JSON.parse(event.data)
    } catch (e) {
      return
    }
    if (message.method === "intimacyRank") {
      rankList.value = (message.data || []).map((item, index) => ({
        ...item,
        rankNo: item.rankNo || index + 1,
      }))
      return
    }
    if (message.method === "giftMessage") {
      emits("sendGift", message.data)
      return
    }
    if (message.method === "guardViolation") {
      const reason = formatGuardReason(message.data)
      addMessages({
        nickname: "系统消息",
        text: reason,
        isSystem: true,
      })
      $modal.msgError(reason)
      return
    }
    if (message.method === "muteUser") {
      addMessages({
        nickname: "系统消息",
        text: message.data,
        isSystem: true,
      })
      const match = message.data && message.data.match(/(\d+)\s*秒/)
      mutedUntil.value = Date.now() + (match ? parseInt(match[1]) * 1000 : 60 * 1000)
      $modal.msgWarning(message.data)
      return
    }
    if (message.method === "kickUser") {
      $modal.msgError(message.data)
      websocket && websocket.close(1000)
      return
    }
    addMessages(message.data)
  }
}

const formatGuardReason = (payload = {}) => {
  if (payload.reason) {
    return payload.reason
  }
  const labelMap = {
    WEAPON: "违规刀具",
    VIOLENCE: "暴力行为",
    EXPOSURE: "过于暴露",
  }
  const label = payload.violationLabel || labelMap[payload.violationType]
  return label
    ? `直播内容不符合平台规范：${label}，直播间已关闭`
    : "直播内容不符合平台规范，直播间已关闭"
}

const connetRoom = () => {
  let reqBody = {
    msgType: 0,
    data: {
      roomId: roomId.value,
    },
  }
  if (websocket) {
    websocket.send(JSON.stringify(reqBody))
  }
}

const heartBeat = () => {
  heartBeatTimer.value = setInterval(() => {
    if (websocket && websocket.readyState === 1) {
      websocket.send(JSON.stringify({ msgType: 2 }))
    }
  }, 9500)
}

const reconnectWebSocket = () => {
  stopTimers({ keepPopularity: true })

  if (lockReconnect.value) {
    return
  }
  lockReconnect.value = true
  if (maxReconnectCount.value == 0) {
    return
  }
  reconnectTimer.value = setTimeout(() => {
    initWebSocket()
    maxReconnectCount.value--
    lockReconnect.value = false
  }, 5000)
}

const handleMuteUser = async (targetUserId, duration) => {
  try {
    await moderatorApi.mute({ roomId: roomId.value, targetUserId, duration: duration || 60 })
    $modal.msgSuccess('已禁言')
  } catch (e) { $modal.msgError(e?.message || '禁言失败') }
}

const handleKickUser = async (targetUserId) => {
  try {
    await moderatorApi.kick({ roomId: roomId.value, targetUserId })
    $modal.msgSuccess('已踢出')
  } catch (e) { $modal.msgError(e?.message || '踢出失败') }
}

const handleReportMessage = (message) => {
  emits("reportMessage", message)
}

const close = ({ keepPopularity = false } = {}) => {
  closeSocket()
  stopTimers({ keepPopularity })
}

const closeSocket = () => {
  if (websocket) {
    websocket.close(1000)
    websocket = null
  }
}

const stopTimers = ({ keepPopularity = false } = {}) => {
  heartBeatTimer.value && clearInterval(heartBeatTimer.value)
  heartBeatTimer.value = null
  reconnectTimer.value && clearTimeout(reconnectTimer.value)
  reconnectTimer.value = null
  if (!keepPopularity) {
    popularityInterval.value && clearInterval(popularityInterval.value)
    popularityInterval.value = null
  }
}
</script>

<style lang="scss" scoped>
.chat-wrapper {
  display: flex;
  flex-direction: column;
  position: relative;
  flex: 1;
  height: 700px;
  min-height: 0;
  overflow: hidden;
  background: var(--chat-bg);

  .chat-top-section {
    position: relative;
    z-index: 2;
    flex: 0 0 auto;
    height: 176px;
  }

  .chat-header {
    height: 154px;
    background:
      linear-gradient(180deg, color-mix(in srgb, var(--accent) 13%, transparent), var(--accent-light) 48%, var(--bg-card) 100%);
    padding: 12px 10px 9px;
    box-sizing: border-box;

    .rank-toolbar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 8px;
      position: relative;

      .rank-title {
        font-size: 14px;
        font-weight: 900;
        color: var(--text-primary);
      }

      :deep(.ant-btn-link) {
        color: var(--accent);
        font-weight: 800;
      }

      .rank-dropdown-wrapper {
        position: relative;
      }
    }
  }

  .rank-dropdown-list {
    position: absolute;
    top: calc(100% - 8px);
    left: 10px;
    right: 10px;
    max-height: 320px;
    overflow-y: auto;
    border: 1px solid var(--border);
    background: var(--bg-card);
    border-radius: 8px;
    box-shadow: var(--shadow-hover);
    z-index: 100;
    opacity: 0;
    visibility: hidden;
    pointer-events: none;
    transform: translateY(-8px);
    transition: opacity 0.2s ease, transform 0.2s ease, visibility 0.2s ease;

    &.is-open {
      opacity: 1;
      visibility: visible;
      pointer-events: auto;
      transform: translateY(0);
    }

    .rank-dropdown-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 8px 12px;
      border-bottom: 1px solid var(--border);

      &:last-child {
        border-bottom: none;
      }

      &:hover {
        background: var(--accent-light);
      }
    }

    .rank-dropdown-left {
      display: flex;
      align-items: center;
      min-width: 0;
      flex: 1;
      overflow: hidden;
    }

    .rank-dropdown-no {
      flex: 0 0 24px;
      height: 24px;
      border-radius: 50%;
      background: var(--bg-secondary);
      font-size: 12px;
      color: var(--text-muted);
      text-align: center;
      line-height: 24px;
      font-weight: 800;

      &.rank-no-1 {
        color: var(--accent);
        background: var(--accent-light);
      }

      &.rank-no-2 {
        color: var(--text-secondary);
        background: var(--bg-secondary);
      }

      &.rank-no-3 {
        color: var(--warning);
        background: color-mix(in srgb, var(--warning) 13%, var(--bg-card));
      }
    }

    .rank-dropdown-avatar {
      width: 28px;
      height: 28px;
      border-radius: 50%;
      object-fit: cover;
      margin-right: 8px;
      margin-left: 4px;
    }

    .rank-dropdown-name {
      font-size: 13px;
      color: var(--text-primary);
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }

    .rank-dropdown-value {
      font-size: 12px;
      color: var(--accent);
      font-weight: 600;
      margin-left: 8px;
    }

    ::v-deep(.ant-empty) {
      margin: 16px 0;

      .ant-empty-description {
        font-size: 12px;
        color: var(--text-muted);
      }
    }
  }
}

.rank-board {
  display: flex;
  gap: 8px;
  align-items: end;
  height: 112px;
}

.rank-item {
  flex: 1;
  min-width: 0;
  text-align: center;
  padding: 8px 6px 7px;
  border-radius: 8px;
  background: linear-gradient(180deg, var(--accent-light) 0%, var(--bg-card) 100%);
  border: 1px solid color-mix(in srgb, var(--accent) 20%, var(--border));
  box-shadow: var(--shadow);

  .rank-badge {
    font-size: 11px;
    color: var(--accent);
    margin-bottom: 5px;
    font-weight: 900;
  }

  .avatar {
    width: 40px;
    height: 40px;
    border: 2px solid var(--border-strong);
    border-radius: 50%;
    object-fit: cover;
  }

  .name {
    display: block;
    font-size: 12px;
    margin-top: 6px;
    color: var(--text-primary);
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .charm {
    display: block;
    margin-top: 2px;
    color: var(--accent);
    font-size: 12px;
    font-weight: 800;
  }
}

.rank-item-1 {
  background: linear-gradient(180deg, var(--accent-light) 0%, var(--bg-card) 100%);
  border-color: var(--accent);
  padding-top: 12px;
  min-height: 108px;

  .avatar {
    border: gold 2px solid;
  }
}

.rank-item-2 .avatar {
  border-color: #c8d0da;
}

.rank-item-3 .avatar {
  border-color: #d8a478;
}

.chat-main {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 8px 7px 6px;
  background:
    linear-gradient(180deg, var(--bg-card) 0, var(--bg-secondary) 100%);

  &::-webkit-scrollbar {
    display: none;
  }

  ::v-deep(.ant-list-item) {
    padding: 2px 0;
    border: none;
  }

  ::v-deep(.ant-list-items) {
    display: flex;
    flex-direction: column;
  }
}

.chat-tools {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex: 0 0 auto;
  height: 32px;
  padding: 0 10px;
  border-top: 1px solid var(--border);
  border-bottom: 1px solid var(--border);
  background: var(--bg-card);
  color: var(--text-muted);
  font-size: 12px;
}

.chat-tools span {
  color: var(--text-secondary);
  font-weight: 800;
}

.chat-tools div {
  display: inline-flex;
  gap: 6px;
}

.chat-tools button {
  height: 22px;
  padding: 0 7px;
  border: 0;
  border-radius: 4px;
  color: var(--text-muted);
  background: var(--bg-secondary);
  font-size: 12px;
  cursor: pointer;
}

.chat-tools button:hover,
.chat-tools button.active {
  color: var(--accent);
  background: var(--accent-light);
}

.chat-footer {
  flex: 0 0 auto;
  padding: 10px;
  border-top: 1px solid var(--border);
  background: var(--bg-card);

  .chat-safety {
    margin: 0 0 8px;
    padding: 6px 8px;
    border-radius: 6px;
    color: var(--warning);
    background: color-mix(in srgb, var(--warning) 13%, var(--bg-card));
    font-size: 12px;
    line-height: 1.4;
  }

  .chat-box {
    width: 100%;
    border-radius: 8px;

    :deep(.ant-input) {
      border-radius: 8px;
      border-color: var(--border);
      background: var(--bg-secondary);
      font-size: 13px;
      resize: none;
    }

    :deep(.ant-input:focus) {
      background: var(--bg-card);
    }
  }

  .chat-btn-wrapper {
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    margin-top: 10px;
    text-align: right;

    ::v-deep(.ant-btn) {
      width: 82px;
      height: 28px;
      border-radius: 16px;
      font-weight: 800;
    }

    .popularity {
      color: var(--text-muted);
      font-size: 12px;
      margin: 0;
      white-space: nowrap;
    }
  }

  ::v-deep(.ant-input-textarea-show-count::after) {
    position: absolute;
    top: 35px;
    right: 6px;
    font-size: 12px;
    color: $font-color-light;
  }
}

.rank-modal {
  .rank-modal-item {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 10px 0;
    border-bottom: 1px solid var(--border);

    &:last-child {
      border-bottom: none;
    }
  }

  .left {
    display: flex;
    align-items: center;
    min-width: 0;
  }

  .rank-no {
    width: 38px;
    font-size: 13px;
    color: var(--text-muted);
  }

  .avatar {
    width: 36px;
    height: 36px;
    border-radius: 50%;
    object-fit: cover;
    margin-right: 10px;
  }

  .name {
    color: var(--text-primary);
  }

  .value {
    color: var(--accent);
    font-weight: 600;
  }
}
</style>
