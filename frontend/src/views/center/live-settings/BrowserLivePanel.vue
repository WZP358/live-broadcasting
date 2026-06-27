<template>
  <section class="browser-live-panel">
    <header class="studio-topbar">
      <div class="studio-brand">
        <span class="studio-logo">PL</span>
        <div>
          <h3>开播工作台</h3>
          <span>房间 {{ activeRoomId || "--" }}</span>
        </div>
      </div>
      <div class="studio-top-status">
        <span class="signal-pill" :class="{ active: state.signalConnected }">
          {{ state.signalConnected ? "连接正常" : "等待连接" }}
        </span>
        <span class="live-pill" :class="{ active: state.liveActive }">
          {{ state.liveActive ? "直播中" : "未开播" }}
        </span>
      </div>
    </header>

    <div class="studio-workspace">
      <aside class="studio-left">
        <div class="section-tabs">
          <button :class="{ active: activeLeftPanel === 'source' }" type="button" @click="activeLeftPanel = 'source'">直播源</button>
          <button :class="{ active: activeLeftPanel === 'tools' }" type="button" @click="activeLeftPanel = 'tools'">工具</button>
        </div>

        <section v-show="activeLeftPanel === 'source'" class="studio-panel source-panel">
          <h4>开播方式</h4>
          <button
            class="source-item"
            :class="{ selected: state.liveActive ? state.isScreenSharing : state.selectedLiveMode === 'screen' }"
            type="button"
            :disabled="state.starting || state.liveActive"
            @click="setLiveMode('screen')"
          >
            <span class="source-icon"><DesktopOutlined /></span>
            <span>
              <strong>屏幕直播</strong>
              <small>{{ state.liveActive && state.isScreenSharing ? "正在使用" : state.selectedLiveMode === "screen" ? "已选中" : "点击选择" }}</small>
            </span>
          </button>
          <button
            class="source-item"
            :class="{ selected: state.liveActive ? !state.isScreenSharing : state.selectedLiveMode === 'camera' }"
            type="button"
            :disabled="state.starting || state.liveActive"
            @click="setLiveMode('camera')"
          >
            <span class="source-icon"><VideoCameraOutlined /></span>
            <span>
              <strong>摄像头直播</strong>
              <small>{{ state.liveActive && !state.isScreenSharing ? "正在使用" : state.selectedLiveMode === "camera" ? "已选中" : "点击选择" }}</small>
            </span>
          </button>
        </section>

        <section v-show="activeLeftPanel === 'tools'" class="studio-panel tool-panel">
          <h4>直播工具</h4>
          <div class="tool-row">
            <span>
              <strong>实时降噪</strong>
              <small>{{ denoiseCompactSummary }}</small>
            </span>
            <a-switch
              :checked="state.denoiseEnabled"
              :disabled="state.starting || state.denoiseSwitching"
              :loading="state.denoiseSwitching"
              checked-children="开"
              un-checked-children="关"
              @change="handleDenoiseSwitch"
            />
          </div>

          <div class="tool-row">
            <span>
              <strong>打赏动效</strong>
              <small>{{ giftEffectSummary }}</small>
            </span>
            <a-switch
              :checked="state.anchorGiftEffectsEnabled"
              checked-children="开"
              un-checked-children="关"
              @change="handleAnchorGiftEffectsSwitch"
            />
          </div>

          <div v-if="state.isScreenSharing && state.liveActive" class="tool-row">
            <span>
              <strong>摄像头浮窗</strong>
              <small>{{ state.cameraPipEnabled ? "已开启" : "未开启" }}</small>
            </span>
            <a-switch
              :checked="state.cameraPipEnabled"
              :disabled="state.pipSwitching"
              :loading="state.pipSwitching"
              checked-children="开"
              un-checked-children="关"
              @change="toggleCameraPip"
            />
          </div>
        </section>

        <section class="studio-panel preflight-panel">
          <div class="preflight-head">
            <h4>开播前检测</h4>
            <a-button size="small" :loading="preflight.checking" @click="runManualPreflight">检测</a-button>
          </div>
          <div class="preflight-list">
            <div
              v-for="item in visiblePreflightItems"
              :key="item.key"
              class="preflight-item"
              :class="`preflight-item--${item.status}`"
            >
              <span>
                <i></i>
                {{ item.label }}
              </span>
              <small>{{ item.detail }}</small>
            </div>
          </div>
        </section>

        <section class="studio-panel status-panel">
          <h4>直播状态</h4>
          <div class="status-line">
            <span>观众</span>
            <strong>{{ state.viewerCount }}</strong>
          </div>
          <div class="status-line">
            <span>内容安全</span>
            <strong>{{ state.guardActive ? "守护中" : "待开启" }}</strong>
          </div>
          <div class="status-line">
            <span>当前画面</span>
            <strong>{{ streamModeText }}</strong>
          </div>
          <div class="status-line">
            <span>连麦</span>
            <strong>{{ cohostStatusText }}</strong>
          </div>
          <div class="status-line">
            <span>PK</span>
            <strong>{{ pkStatusText }}</strong>
          </div>
        </section>
      </aside>

      <main class="studio-center">
        <div class="room-strip">
          <div>
            <h4>{{ streamModeText }}</h4>
            <span>{{ state.message || "准备好后即可开始直播" }}</span>
          </div>
        </div>

        <div
          ref="previewStageRef"
          class="preview-stage"
          :class="{ 'preview-stage--fullscreen': previewFullscreen, 'preview-stage--pk': pkStageActive }"
        >
          <video ref="previewRef" class="studio-preview" autoplay muted playsinline controlslist="nofullscreen nodownload noremoteplayback"></video>
          <div v-if="!state.liveActive && !pkStageActive" class="preview-empty">
            <strong>等待开播</strong>
            <span>选择屏幕或摄像头</span>
          </div>
          <span v-if="state.liveActive" class="preview-live-tag">直播中</span>
          <div v-if="pkStageActive" class="pk-stage-card">
            <div class="interaction-video-head">
              <span>{{ pk.remoteName || "对方主播" }}</span>
              <button type="button" @click="endPk">结束 PK</button>
            </div>
            <video ref="pkVideoRef" autoplay playsinline></video>
            <small>{{ pk.connectionText || "正在连接对方主播" }}</small>
          </div>
          <button
            class="preview-fullscreen-btn"
            type="button"
            :aria-label="previewFullscreen ? '退出放大预览' : '放大预览'"
            :title="previewFullscreen ? '退出放大预览' : '放大预览'"
            @click="togglePreviewFullscreen"
          >
            <FullscreenExitOutlined v-if="previewFullscreen" />
            <FullscreenOutlined v-else />
          </button>
          <div v-if="cohost.active || cohost.status === 'connecting'" class="cohost-stage-card">
            <div class="interaction-video-head">
              <span>连麦</span>
              <button type="button" @click="endCohost">挂断</button>
            </div>
            <video ref="cohostVideoRef" autoplay playsinline></video>
            <small>{{ cohost.connectionText || cohostStatusText }}</small>
          </div>
        </div>

        <div class="control-dock">
          <div class="dock-metrics">
            <div>
              <span>连接</span>
              <strong>{{ state.signalConnected ? "正常" : "等待" }}</strong>
            </div>
            <div>
              <span>降噪</span>
              <strong>{{ denoiseCompactSummary }}</strong>
            </div>
          </div>
          <a-button
            :type="state.liveActive ? 'default' : 'primary'"
            :danger="state.liveActive"
            size="large"
            :loading="state.starting"
            @click="handlePrimaryLiveAction"
          >
            <template v-if="state.liveActive">
              <PauseCircleOutlined />
              停止直播
            </template>
            <template v-else>
              <PlayCircleOutlined />
              开始直播
            </template>
          </a-button>
        </div>

        <div class="studio-bottom">
          <section>
            <h4>直播数据</h4>
            <div class="data-grid">
              <div>
                <span>观看</span>
                <strong>{{ state.viewerCount }}</strong>
              </div>
              <div>
                <span>房间</span>
                <strong>{{ activeRoomId || "--" }}</strong>
              </div>
              <div>
                <span>状态</span>
                <strong>{{ state.liveActive ? "直播中" : "未开播" }}</strong>
              </div>
            </div>
          </section>
          <section>
            <h4>声音</h4>
            <p>{{ denoiseSummary }}</p>
          </section>
        </div>
      </main>

      <aside class="studio-right">
        <div class="chat-header">
          <h4>互动消息</h4>
          <span>{{ state.viewerCount }} 人在线</span>
        </div>
        <div class="interaction-panel">
          <div class="interaction-section">
            <div class="interaction-section__head">
              <div>
                <strong>观众连麦</strong>
                <span>{{ cohost.requests.length ? `${cohost.requests.length} 个申请待处理` : cohostStatusText }}</span>
              </div>
              <a-button size="small" :disabled="!cohost.active && cohost.status !== 'connecting'" @click="endCohost">
                结束
              </a-button>
            </div>
            <div v-if="cohost.requests.length" class="request-list">
              <div v-for="item in cohost.requests" :key="item.fromSessionId" class="request-item">
                <img :src="safeInteractionAvatar(item.applicantAvatar)" alt="" @error="onInteractionImgError" />
                <div>
                  <strong>{{ item.applicantName || "观众" }}</strong>
                  <span>申请上麦互动</span>
                </div>
                <div class="request-actions">
                  <a-button size="small" type="primary" @click="acceptCohostRequest(item)">同意</a-button>
                  <a-button size="small" @click="rejectCohostRequest(item)">拒绝</a-button>
                </div>
              </div>
            </div>
            <p v-else class="interaction-empty">观众发起申请后会显示在这里，主播同意后建立音视频连麦。</p>
          </div>

          <div class="interaction-section">
            <div class="interaction-section__head">
              <div>
                <strong>主播 PK</strong>
                <span>{{ pkStatusText }}</span>
              </div>
              <a-button size="small" :disabled="!pkActive" @click="endPk">结束</a-button>
            </div>
            <div v-if="pk.pendingInvite" class="pk-invite-card">
              <strong>{{ pk.pendingInvite.inviterName || "对方主播" }} 发起 PK</strong>
              <span>房间 {{ pk.pendingInvite.fromRoomId || "--" }}</span>
              <div>
                <a-button size="small" type="primary" @click="acceptPkInvite">接受</a-button>
                <a-button size="small" @click="rejectPkInvite">拒绝</a-button>
              </div>
            </div>
            <div v-if="pkActive" class="pk-status-card">
              <strong>{{ pk.remoteName || "对方主播" }}</strong>
              <span>{{ pk.connectionText || "正在连接对方主播" }}</span>
            </div>
            <div v-else class="pk-form">
              <a-input
                v-model:value="pk.targetRoomId"
                class="pk-target-input"
                size="small"
                placeholder="输入对方房间号"
                :disabled="!state.liveActive || pk.status === 'inviting'"
                @pressEnter="invitePk"
              />
              <a-button size="small" type="primary" :loading="pk.status === 'inviting'" :disabled="!state.liveActive" @click="invitePk">
                发起
              </a-button>
            </div>
          </div>
        </div>
        <div class="chat-feed">
          <div
            v-for="item in anchorChatMessages"
            :key="item.id"
            class="chat-message"
            :class="{ system: item.isSystem || item.nickname === '系统消息', self: item.isSelf, gift: item.isGift }"
          >
            <strong>{{ item.isSelf ? "我" : item.nickname || "观众" }}</strong>
            <span>{{ item.text }}</span>
          </div>
          <div v-if="!anchorChatMessages.length" class="chat-empty">
            <strong>暂无互动</strong>
            <span>开播后显示弹幕和礼物消息</span>
          </div>
        </div>
        <div class="anchor-chat-composer">
          <a-input
            v-model:value="anchorChatText"
            size="small"
            :maxlength="100"
            :disabled="!state.liveActive || anchorChatSending"
            :placeholder="state.liveActive ? '以主播身份发送弹幕' : '开播后可发送弹幕'"
            @pressEnter="sendAnchorChat"
          />
          <a-button
            type="primary"
            size="small"
            :loading="anchorChatSending"
            :disabled="!state.liveActive || !anchorChatText.trim()"
            @click="sendAnchorChat"
          >
            发送
          </a-button>
        </div>
      </aside>
    </div>
    <GiftEffects v-if="state.anchorGiftEffectsEnabled" ref="giftEffectsRef" />
  </section>
</template>

<script setup>
import { computed, defineAsyncComponent, nextTick, onBeforeUnmount, onMounted, reactive, ref, watch } from "vue"
import {
  DesktopOutlined,
  FullscreenExitOutlined,
  FullscreenOutlined,
  PauseCircleOutlined,
  PlayCircleOutlined,
  VideoCameraOutlined,
} from "@ant-design/icons-vue"
import $modal from "@/utils/message"
import liveAPI from "@/api/live"
import ChatApi from "@/api/chat"
import { useStore } from "@/stores"
import { createBrowserLiveFallbackUrls, createPeerConnection } from "@/utils/browserLive"
import { appendChatMessages, createChatWebSocketUrl } from "@/utils/chatRoom"
import { createLiveDenoiseEngine } from "@/utils/liveDenoise"
import { createDelayedVideoStream } from "@/utils/videoDelay"
import { runBroadcastPreflight } from "@/utils/demoDiagnostics"
import { resolveSafeImageUrl } from "@/utils/fallback"

const GiftEffects = defineAsyncComponent(() => import("@/components/live/GiftEffects.vue"))

const HEARTBEAT_INTERVAL = 15000
const CHAT_HEARTBEAT_INTERVAL = 9500
const GUARD_CHECK_INTERVAL = 2000
const DENOISE_STORAGE_KEY = "live.browser.denoise.enabled"
const ANCHOR_GIFT_EFFECT_STORAGE_KEY = "live.browser.anchorGiftEffect.enabled"
const DENOISE_VIDEO_SYNC_DELAY_MS = Math.max(
  0,
  Number(window.localStorage.getItem("live.browser.denoiseVideoDelayMs") || 240),
)

const props = defineProps({
  roomId: {
    type: [Number, String],
    default: null,
  },
  liveStatus: {
    type: Number,
    default: 0,
  },
})

const emit = defineEmits(["status-change"])
const store = useStore()
const previewStageRef = ref(null)
const previewRef = ref(null)
const cohostVideoRef = ref(null)
const pkVideoRef = ref(null)
const giftEffectsRef = ref(null)
const previewFullscreen = ref(false)
const resolvedRoomId = ref(null)
const activeRoomId = computed(() => props.roomId || resolvedRoomId.value)
const activeLeftPanel = ref("source")
const anchorChatText = ref("")
const anchorChatSending = ref(false)
const anchorChatMessages = ref([])
let anchorChatMessageId = 0

const state = reactive({
  starting: false,
  liveActive: false,
  isScreenSharing: false,
  selectedLiveMode: "screen",
  cameraPipEnabled: true,
  pipSwitching: false,
  message: "",
  viewerCount: 0,
  signalConnected: false,
  denoiseEnabled: window.localStorage.getItem(DENOISE_STORAGE_KEY) === "1",
  anchorGiftEffectsEnabled: window.localStorage.getItem(ANCHOR_GIFT_EFFECT_STORAGE_KEY) === "1",
  denoiseStatus: "idle",
  denoiseDetail: "",
  denoiseSwitching: false,
  denoiseUsingEnhanced: false,
  denoiseBackend: "",
  denoiseModelName: "",
  denoiseRuntimeInfo: "",
  guardActive: false,
})

const preflight = reactive({
  checking: false,
  items: [],
  lastCheckedAt: null,
})

let signalSocket = null
let captureStream = null
let publishingStream = null
let cameraStream = null
let pipCanvas = null
let pipAnimationId = null
let heartbeatTimer = null
let chatSocket = null
let chatHeartbeatTimer = null
let chatReconnectTimer = null
let chatReconnectCount = 0
let guardTimer = null
let guardChecking = false
let denoiseEngine = null
let auxiliaryStreams = []
let liveRecorder = null
let liveRecordChunks = []
let liveRecordStartedAt = 0
const peerMap = new Map()
const defaultInteractionAvatar =
  "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='96' height='96' viewBox='0 0 96 96'%3E%3Crect width='96' height='96' rx='16' fill='%23ff9f1a'/%3E%3Ccircle cx='48' cy='36' r='18' fill='%23fff7df'/%3E%3Cpath d='M18 82c5-18 17-28 30-28s25 10 30 28' fill='%23fff7df'/%3E%3C/svg%3E"
const safeInteractionAvatar = (url) => resolveSafeImageUrl(url, defaultInteractionAvatar)
const cohost = reactive({
  status: "idle",
  requests: [],
  active: false,
  peer: null,
  remoteStream: null,
  remoteSessionId: "",
  remoteName: "",
  connectionText: "",
})
const pk = reactive({
  status: "idle",
  targetRoomId: "",
  pendingInvite: null,
  peer: null,
  remoteStream: null,
  remoteSessionId: "",
  remoteRoomId: null,
  remoteName: "",
  connectionText: "",
})
let cohostPeer = null
let cohostRemoteStream = null
let pkPeer = null
let pkRemoteStream = null

const resetDenoiseState = () => {
  state.denoiseStatus = "idle"
  state.denoiseDetail = ""
  state.denoiseBackend = ""
  state.denoiseModelName = ""
  state.denoiseUsingEnhanced = false
  state.denoiseRuntimeInfo = ""
}

const syncIdleDenoiseHint = () => {
  if (!state.denoiseEnabled) {
    resetDenoiseState()
    return
  }

  state.denoiseStatus = "idle"
  state.denoiseDetail = "已启用。开播时会自动优化麦克风声音。"
  state.denoiseBackend = ""
  state.denoiseModelName = ""
  state.denoiseUsingEnhanced = false
  state.denoiseRuntimeInfo = ""
}

const denoiseSummary = computed(() => {
  if (!state.denoiseEnabled) {
    return "未启用。开播时将直接使用原始麦克风音频。"
  }
  if (!state.liveActive) {
    return "已启用。开播时会自动优化麦克风声音。"
  }
  if (state.denoiseStatus === "active" && state.denoiseUsingEnhanced) {
    return [state.denoiseDetail || "降噪正在优化声音。", state.denoiseRuntimeInfo]
      .filter(Boolean)
      .join(" ")
  }
  if (state.denoiseStatus === "warming") {
    return state.denoiseDetail || "降噪正在准备中。"
  }
  if (state.denoiseDetail) {
    return state.denoiseDetail
  }
  return "实时降噪运行中"
})

const denoiseCompactSummary = computed(() => {
  if (!state.denoiseEnabled) {
    return "未开启"
  }
  if (state.denoiseStatus === "fallback" || state.denoiseStatus === "error") {
    return "暂不可用"
  }
  if (state.denoiseStatus === "connecting" || state.denoiseStatus === "warming") {
    return "准备中"
  }
  return state.liveActive ? "运行中" : "已开启"
})

const giftEffectSummary = computed(() =>
  state.anchorGiftEffectsEnabled ? "主播和观众都能看到" : "仅观众端显示"
)

const currentUserProfile = computed(() => {
  const info = store.user().userInfo || {}
  return {
    name: info.nickName || info.nickname || info.name || info.username || "主播",
    avatar: info.avatar || "",
  }
})

const cohostStatusText = computed(() => {
  if (cohost.active) {
    return cohost.remoteName ? `与 ${cohost.remoteName} 连麦中` : "连麦中"
  }
  if (cohost.status === "connecting") {
    return "正在建立连麦"
  }
  if (cohost.status === "invited") {
    return "有观众申请"
  }
  return "待申请"
})

const pkActive = computed(() => pk.status === "inviting" || pk.status === "connecting" || pk.status === "active")
const pkStageActive = computed(() => pk.status === "connecting" || pk.status === "active")

const pkStatusText = computed(() => {
  if (pk.status === "active") {
    return pk.remoteName ? `与 ${pk.remoteName} PK 中` : "PK 中"
  }
  if (pk.status === "connecting") {
    return "正在建立 PK"
  }
  if (pk.status === "inviting") {
    return "等待对方接受"
  }
  if (pk.pendingInvite) {
    return "收到 PK 邀请"
  }
  return "未开始"
})

const streamModeText = computed(() => {
  if (!state.liveActive) {
    return state.selectedLiveMode === "screen" ? "屏幕直播" : "摄像头直播"
  }
  return state.isScreenSharing ? "屏幕直播" : "摄像头直播"
})

const preflightPlaceholderItems = computed(() => [
  {
    key: "capture",
    label: state.selectedLiveMode === "screen" ? "屏幕采集能力" : "摄像头采集能力",
    status: "warn",
    detail: "点击检测或开始直播时自动检查。",
  },
  {
    key: "live-signal",
    label: "直播信令通道",
    status: "warn",
    detail: "点击检测或开始直播时自动检查。",
  },
])

const visiblePreflightItems = computed(() =>
  preflight.items.length ? preflight.items : preflightPlaceholderItems.value
)

const setMessage = (text) => {
  state.message = text
}

const ignoreMediaSideEffect = () => {}

const getDenoiseStatusText = (status, useEnhancedOutput = false) => {
  if (!state.denoiseEnabled) {
    return ""
  }
  if (status === "active") {
    return useEnhancedOutput ? "降噪正在优化声音" : "降噪已开启"
  }
  if (status === "warming" || status === "connecting") {
    return "正在准备降噪..."
  }
  if (status === "fallback" || status === "error") {
    return "降噪暂不可用，已继续使用原始麦克风声音"
  }
  return "降噪已启用"
}

const getCurrentUserId = () => {
  const info = store.user().userInfo || {}
  return Number(info.userId || info.id || 0)
}

const normalizeAnchorChatPayload = (payload = {}, options = {}) => {
  if (typeof payload === "string") {
    return {
      id: ++anchorChatMessageId,
      nickname: "系统消息",
      text: payload,
      isSystem: true,
      createdAt: Date.now(),
    }
  }

  const fromUserId = Number(payload?.fromUserId || payload?.senderId || 0)
  const text = payload?.text ?? payload?.content ?? ""
  return {
    ...payload,
    id: payload?.id || `${Date.now()}-${++anchorChatMessageId}`,
    nickname: payload?.nickname || payload?.senderName || payload?.username || "观众",
    text: String(text || ""),
    fromUserId,
    isSelf: Boolean(fromUserId && fromUserId === getCurrentUserId()),
    isSystem: Boolean(options.isSystem || payload?.isSystem || payload?.nickname === "系统消息"),
    isGift: Boolean(options.isGift),
    createdAt: Date.now(),
  }
}

const pushAnchorChatMessage = (payload, options = {}) => {
  const normalized = normalizeAnchorChatPayload(payload, options)
  if (!normalized.text) {
    return
  }
  anchorChatMessages.value = appendChatMessages(anchorChatMessages.value, normalized, 80)
}

const pushAnchorSystemMessage = (text) => {
  pushAnchorChatMessage({ nickname: "系统消息", text, isSystem: true })
}

const parseGiftPayload = (payload = {}) => {
  if (typeof payload !== "string") {
    return payload
  }
  try {
    return JSON.parse(payload)
  } catch (error) {
    return payload
  }
}

const resolveGiftPayload = (payload = {}) => {
  const parsedPayload = parseGiftPayload(payload)
  if (parsedPayload && typeof parsedPayload === "object") {
    return {
      giftName: parsedPayload.giftName || parsedPayload.presentName || "礼物",
      senderName: parsedPayload.senderName || parsedPayload.nickname || "观众",
      count: Number(parsedPayload.count || parsedPayload.number || 1),
      giftId: parsedPayload.giftId || parsedPayload.presentId || parsedPayload.id || 0,
      text: parsedPayload.text || parsedPayload.content || "",
    }
  }
  return {
    giftName: String(parsedPayload || "礼物"),
    senderName: "观众",
    count: 1,
    giftId: 0,
    text: "",
  }
}

const playAnchorGiftEffect = (payload) => {
  if (!state.anchorGiftEffectsEnabled) {
    return
  }
  const gift = resolveGiftPayload(payload)
  giftEffectsRef.value?.playGiftEffect(gift.giftName, gift.senderName, {
    count: gift.count,
    giftId: gift.giftId,
    text: gift.text,
  })
}

const handleAnchorGiftEffectsSwitch = (checked) => {
  state.anchorGiftEffectsEnabled = checked
  window.localStorage.setItem(ANCHOR_GIFT_EFFECT_STORAGE_KEY, checked ? "1" : "0")
}

const appendAnchorGiftMessage = (payload) => {
  const gift = resolveGiftPayload(payload)
  const count = Math.max(1, Number(gift.count || 1))
  pushAnchorChatMessage({
    nickname: "系统消息",
    text: gift.text || `${gift.senderName || "观众"} 送出了 ${gift.giftName || "礼物"} x ${count}`,
    isSystem: true,
  }, { isGift: true, isSystem: true })
}

const sendAnchorChat = async () => {
  if (!state.liveActive) {
    $modal.msgWarning("开播后才能发送弹幕")
    return
  }
  const roomId = activeRoomId.value
  const text = anchorChatText.value.trim()
  if (!roomId || !text || anchorChatSending.value) {
    return
  }

  anchorChatSending.value = true
  anchorChatText.value = ""
  try {
    await ChatApi.sendChatMsg({
      roomId: Number(roomId),
      text,
    })
  } catch (error) {
    anchorChatText.value = text
    $modal.msgError(error?.message || "弹幕发送失败，请稍后重试")
  } finally {
    anchorChatSending.value = false
  }
}

watch(
  () => props.roomId,
  (roomId) => {
    if (roomId) {
      resolvedRoomId.value = roomId
    }
  },
  { immediate: true }
)

watch(
  () => [state.liveActive, activeRoomId.value],
  ([liveActive, roomId]) => {
    if (liveActive && roomId) {
      connectAnchorChatSocket()
      return
    }
    closeAnchorChatSocket()
  }
)

const ensureRoomId = async () => {
  if (activeRoomId.value) {
    return activeRoomId.value
  }

  const res = await liveAPI.getRoomSettingsInfo()
  const roomId = res?.data?.id
  if (roomId) {
    resolvedRoomId.value = roomId
    emit("status-change")
    return roomId
  }

  return null
}

const ensureRoomActive = async () => {
  if (props.liveStatus === 1) {
    return
  }
  await liveAPI.applySecret()
  emit("status-change")
}

const createMicrophoneConstraints = () => ({
  channelCount: 1,
  echoCancellation: true,
  noiseSuppression: true,
  autoGainControl: true,
})

const ensureMediaCaptureAvailable = (captureType = "camera") => {
  const mediaDevices = navigator.mediaDevices
  const requiredMethod = captureType === "screen" ? "getDisplayMedia" : "getUserMedia"
  if (mediaDevices?.[requiredMethod]) {
    return true
  }

  const message = captureType === "screen"
    ? "屏幕开播需要安全浏览器环境。请在主播电脑使用 http://localhost:5173 开播，或把外网域名配置为 HTTPS。"
    : "摄像头开播需要安全浏览器环境。请在主播电脑使用 http://localhost:5173 开播，或把外网域名配置为 HTTPS。"
  setMessage(message)
  $modal.msgWarning(message)
  return false
}

const runPreflightCheck = async ({ silent = false } = {}) => {
  if (preflight.checking) {
    return {
      ok: false,
      items: preflight.items,
    }
  }

  preflight.checking = true
  try {
    const result = await runBroadcastPreflight({
      mode: state.selectedLiveMode,
      denoiseEnabled: state.denoiseEnabled,
    })
    preflight.items = result.items
    preflight.lastCheckedAt = result.checkedAt
    const blockingErrors = result.items.filter((item) => item.blocking && item.status === "error")
    if (blockingErrors.length) {
      const message = blockingErrors[0].detail || "开播前检测未通过，请先处理异常项。"
      setMessage(message)
      if (!silent) {
        $modal.msgWarning(message)
      }
      return { ...result, ok: false }
    }
    if (!silent) {
      $modal.msgSuccess("开播前检测通过")
    }
    return { ...result, ok: true }
  } catch (error) {
    const message = error?.message || "开播前检测失败，请稍后重试。"
    preflight.items = [{
      key: "preflight-error",
      label: "开播前检测",
      status: "error",
      detail: message,
      blocking: true,
    }]
    setMessage(message)
    if (!silent) {
      $modal.msgError(message)
    }
    return { ok: false, items: preflight.items }
  } finally {
    preflight.checking = false
  }
}

const runManualPreflight = () => {
  runPreflightCheck()
}

const describeAudioProcessingState = (stream = captureStream) => {
  const [audioTrack] = stream?.getAudioTracks?.() || []
  if (!audioTrack) {
    state.denoiseRuntimeInfo = "未检测到麦克风声音。"
    return
  }

  const settings = typeof audioTrack.getSettings === "function" ? audioTrack.getSettings() : {}
  const lines = [
    audioTrack.label ? `当前麦克风：${audioTrack.label}` : "麦克风已连接",
    state.denoiseEnabled ? "降噪已启用" : "",
    stream?.stopVideoSyncDelay ? `视频已补偿约 ${DENOISE_VIDEO_SYNC_DELAY_MS}ms` : "",
  ].filter(Boolean)

  state.denoiseRuntimeInfo = lines.join("；")
}

const createScreenStream = async () => {
  const displayStream = await navigator.mediaDevices.getDisplayMedia({
    video: true,
    audio: false,
  })

  try {
    const microphoneStream = await navigator.mediaDevices.getUserMedia({
      audio: createMicrophoneConstraints(),
    })

    auxiliaryStreams = [displayStream]
    auxiliaryStreams.push(microphoneStream)

    // 尝试获取摄像头用于浮窗。
    if (state.cameraPipEnabled) {
      try {
        cameraStream = await navigator.mediaDevices.getUserMedia({
          video: { width: { ideal: 320 }, height: { ideal: 240 } },
          audio: false,
        })
        auxiliaryStreams.push(cameraStream)
      } catch (error) {
        cameraStream = null
      }
    }

    const videoTracks = displayStream.getVideoTracks()
    const audioTracks = microphoneStream.getAudioTracks()

    if (!audioTracks.length) {
      throw new Error("屏幕直播需要可用麦克风，不能使用电脑系统声音代替。")
    }

    // 如果有摄像头浮窗，用 Canvas 合成画面
    if (cameraStream && cameraStream.getVideoTracks().length > 0) {
      return createPipCompositeStream(displayStream, cameraStream, [...videoTracks, ...audioTracks])
    }

    return new MediaStream([...videoTracks, ...audioTracks])
  } catch (error) {
    displayStream.getTracks().forEach((track) => track.stop())
    throw error
  }
}

const startScreenLive = async () => {
  if (!ensureMediaCaptureAvailable("screen")) {
    return
  }
  state.selectedLiveMode = "screen"
  state.isScreenSharing = true
  state.cameraPipEnabled = false
  await startBrowserLive(() => createScreenStream())
}

const startCameraLive = async () => {
  if (!ensureMediaCaptureAvailable("camera")) {
    return
  }
  state.selectedLiveMode = "camera"
  state.isScreenSharing = false
  state.cameraPipEnabled = false
  await startBrowserLive(() =>
    navigator.mediaDevices.getUserMedia({
      video: true,
      audio: createMicrophoneConstraints(),
    })
  )
}

const setLiveMode = (mode) => {
  if (state.liveActive || state.starting) {
    return
  }
  state.selectedLiveMode = mode === "camera" ? "camera" : "screen"
}

const handlePrimaryLiveAction = async () => {
  if (state.liveActive) {
    await stopBrowserLive()
    return
  }
  const preflightResult = await runPreflightCheck({ silent: true })
  if (!preflightResult.ok) {
    $modal.msgWarning("开播前检测未通过，请先处理异常项。")
    return
  }
  if (state.selectedLiveMode === "camera") {
    await startCameraLive()
    return
  }
  await startScreenLive()
}

const syncPreviewFullscreenState = () => {
  previewFullscreen.value = document.fullscreenElement === previewStageRef.value
}

const togglePreviewFullscreen = async () => {
  const previewStage = previewStageRef.value
  if (!previewStage || !previewStage.requestFullscreen) {
    $modal.msgWarning("当前浏览器不支持放大预览")
    return
  }
  try {
    if (document.fullscreenElement === previewStage) {
      await document.exitFullscreen?.()
      return
    }
    if (document.fullscreenElement) {
      await document.exitFullscreen?.()
      await nextTick()
    }
    await previewStage.requestFullscreen()
  } catch (error) {
    $modal.msgWarning("放大预览失败，请检查浏览器权限")
  }
}

/**
 * Canvas 合成：屏幕画面 + 右下角摄像头浮窗
 */
const createPipCompositeStream = (screenStream, camStream, audioTracks) => {
  stopPipComposite()

  const screenVideo = document.createElement("video")
  screenVideo.srcObject = screenStream
  screenVideo.autoplay = true
  screenVideo.muted = true
  screenVideo.playsInline = true
  screenVideo.play?.().catch(ignoreMediaSideEffect)

  const camVideo = document.createElement("video")
  camVideo.srcObject = camStream
  camVideo.autoplay = true
  camVideo.muted = true
  camVideo.playsInline = true
  camVideo.play?.().catch(ignoreMediaSideEffect)

  pipCanvas = document.createElement("canvas")
  const ctx = pipCanvas.getContext("2d")

  const PIP_WIDTH_RATIO = 0.25   // 浮窗占画面宽度 25%
  const PIP_MARGIN = 16          // 距边缘的像素
  const PIP_RADIUS = 10          // 圆角

  const drawFrame = () => {
    if (!pipCanvas || !ctx) return

    const sw = screenVideo.videoWidth || 1280
    const sh = screenVideo.videoHeight || 720
    if (pipCanvas.width !== sw || pipCanvas.height !== sh) {
      pipCanvas.width = sw
      pipCanvas.height = sh
    }

    // 绘制屏幕画面
    ctx.clearRect(0, 0, sw, sh)
    if (screenVideo.readyState >= 2) {
      ctx.drawImage(screenVideo, 0, 0, sw, sh)
    }

    // 绘制摄像头浮窗（右下角）
    if (camVideo.readyState >= 2) {
      const pipW = Math.round(sw * PIP_WIDTH_RATIO)
      const pipH = Math.round(pipW * (camVideo.videoHeight / Math.max(1, camVideo.videoWidth)))
      const pipX = sw - pipW - PIP_MARGIN
      const pipY = sh - pipH - PIP_MARGIN

      ctx.save()
      // 圆角裁剪
      ctx.beginPath()
      ctx.moveTo(pipX + PIP_RADIUS, pipY)
      ctx.lineTo(pipX + pipW - PIP_RADIUS, pipY)
      ctx.quadraticCurveTo(pipX + pipW, pipY, pipX + pipW, pipY + PIP_RADIUS)
      ctx.lineTo(pipX + pipW, pipY + pipH - PIP_RADIUS)
      ctx.quadraticCurveTo(pipX + pipW, pipY + pipH, pipX + pipW - PIP_RADIUS, pipY + pipH)
      ctx.lineTo(pipX + PIP_RADIUS, pipY + pipH)
      ctx.quadraticCurveTo(pipX, pipY + pipH, pipX, pipY + pipH - PIP_RADIUS)
      ctx.lineTo(pipX, pipY + PIP_RADIUS)
      ctx.quadraticCurveTo(pipX, pipY, pipX + PIP_RADIUS, pipY)
      ctx.closePath()
      ctx.clip()

      // 白色边框
      ctx.strokeStyle = "#fff"
      ctx.lineWidth = 3
      ctx.stroke()

      ctx.drawImage(camVideo, pipX, pipY, pipW, pipH)
      ctx.restore()
    }

    pipAnimationId = requestAnimationFrame(drawFrame)
  }

  drawFrame()

  const compositeStream = pipCanvas.captureStream(30)
  audioTracks.forEach((track) => compositeStream.addTrack(track))
  return compositeStream
}

const stopPipComposite = () => {
  if (pipAnimationId) {
    cancelAnimationFrame(pipAnimationId)
    pipAnimationId = null
  }
  if (pipCanvas) {
    const ctx = pipCanvas.getContext("2d")
    if (ctx) ctx.clearRect(0, 0, pipCanvas.width, pipCanvas.height)
    pipCanvas = null
  }
}

/**
 * 直播中切换摄像头浮窗
 */
const toggleCameraPip = async (enabled) => {
  if (!state.liveActive || !state.isScreenSharing) return
  if (enabled && !ensureMediaCaptureAvailable("camera")) return
  state.pipSwitching = true
  state.cameraPipEnabled = enabled

  try {
    if (enabled) {
      // 开启浮窗：获取摄像头并重建合成流
      cameraStream = await navigator.mediaDevices.getUserMedia({
        video: { width: { ideal: 320 }, height: { ideal: 240 } },
        audio: false,
      })
      auxiliaryStreams.push(cameraStream)

      const audioTracks = publishingStream?.getAudioTracks?.() || []
      const screenVideoTrack = captureStream?.getVideoTracks?.()?.[0]
      if (!screenVideoTrack) throw new Error("屏幕视频轨丢失")

      // 重建屏幕流用于合成
      const screenOnly = new MediaStream([screenVideoTrack])
      const newStream = createPipCompositeStream(screenOnly, cameraStream, audioTracks)

      // 切换直播画面
      await replacePublishingStream(newStream)
    } else {
      // 关闭浮窗：恢复纯屏幕画面
      stopPipComposite()
      if (cameraStream) {
        cameraStream.getTracks().forEach((t) => t.stop())
        cameraStream = null
      }

      // 用原始 captureStream 的视频 + 当前音频重建
      const audioTracks = publishingStream?.getAudioTracks?.() || []
      const videoTrack = captureStream?.getVideoTracks?.()?.[0]
      if (videoTrack) {
        const plainStream = new MediaStream([videoTrack, ...audioTracks])
        await replacePublishingStream(plainStream)
      }
    }
  } catch (error) {
    state.cameraPipEnabled = !enabled
    $modal.msgWarning("摄像头浮窗切换失败：" + (error.message || "未知错误"))
  } finally {
    state.pipSwitching = false
  }
}

/**
 * 切换直播画面：更新所有观众连接的画面与声音，并刷新预览。
 */
const replacePublishingStream = async (newStream) => {
  const oldStream = publishingStream
  publishingStream = await preparePublishingStreamForSend(newStream)
  attachPreview()

  // 更新所有已连接观众的画面与声音
  const peers = [...Array.from(peerMap.values()), cohostPeer, pkPeer].filter(Boolean)
  for (const peer of peers) {
    if (!["connected", "connecting", "new"].includes(peer.connectionState)) continue
    const senders = peer.getSenders()
    oldStream?.getTracks().forEach((oldTrack) => {
      const sender = senders.find((s) => s.track?.kind === oldTrack.kind)
      const newTrack = publishingStream.getTracks().find((t) => t.kind === oldTrack.kind)
      if (sender && newTrack) {
        sender.replaceTrack(newTrack).catch(ignoreMediaSideEffect)
      }
    })
  }

  // 清理旧画面（但保留原始采集内容）
  if (oldStream && oldStream !== captureStream && oldStream !== publishingStream) {
    stopUnprotectedTracks(oldStream, [captureStream, newStream, publishingStream])
  }
}

const startBrowserLive = async (streamFactory) => {
  const roomId = await ensureRoomId()
  if (!roomId) {
    $modal.msgError("房间信息未初始化完成，暂时无法开播")
    return
  }

  state.starting = true
  setMessage("")
  state.denoiseStatus = state.denoiseEnabled ? "connecting" : "idle"
  state.denoiseDetail = state.denoiseEnabled ? "正在准备降噪..." : ""

  try {
    await ensureRoomActive()
    captureStream = await streamFactory()
    publishingStream = await buildPublishingStream(captureStream)
    attachPreview()
    await connectSignal()
    state.liveActive = true
    startLiveRecording()
    startGuardLoop()
    setMessage("直播已开始，观众可以进入房间观看")
    emit("status-change")
  } catch (error) {
    closeAllPeers()
    closeSignal()
    await releaseMediaResources()
    const errorMessage = getMediaErrorMessage(error)
    setMessage(errorMessage)
    $modal.msgError(errorMessage)
  } finally {
    state.starting = false
  }
}

const buildPublishingStream = async (stream) => {
  let baseStream = stream
  if (!state.denoiseEnabled) {
    resetDenoiseState()
    describeAudioProcessingState(stream)
    setMessage("画面和声音已准备就绪")
    return stream
  }

  denoiseEngine = createLiveDenoiseEngine({
    onStateChange: ({ status, detail, useEnhancedOutput }) => {
      state.denoiseStatus = status
      state.denoiseUsingEnhanced = Boolean(useEnhancedOutput)
      const nextDetail = getDenoiseStatusText(status, useEnhancedOutput)
      state.denoiseDetail = nextDetail
      if (nextDetail) {
        setMessage(nextDetail)
      }
    },
  })

  try {
    const result = await denoiseEngine.start(stream)
    state.denoiseStatus = "warming"
    state.denoiseBackend = result.backend || ""
    state.denoiseModelName = result.modelName || ""
    state.denoiseDetail = "降噪已准备就绪"
    state.denoiseUsingEnhanced = false
    baseStream = await applyDenoiseVideoSync(result.stream)
    describeAudioProcessingState(baseStream)
    setMessage("画面和声音已准备就绪")
    return baseStream
  } catch (error) {
    state.denoiseStatus = "error"
    state.denoiseDetail = "降噪暂不可用，已继续使用原始麦克风声音"
    state.denoiseBackend = ""
    state.denoiseModelName = ""
    state.denoiseUsingEnhanced = false
    state.denoiseRuntimeInfo = ""
    await denoiseEngine.stop()
    denoiseEngine = null
    setMessage("画面和声音已准备就绪")
    return stream
  }
}

const applyDenoiseVideoSync = async (stream) => {
  if (!stream?.getVideoTracks?.().length || !DENOISE_VIDEO_SYNC_DELAY_MS) {
    return stream
  }
  try {
    return await createDelayedVideoStream(stream, DENOISE_VIDEO_SYNC_DELAY_MS)
  } catch (error) {
    console.warn("denoise video sync delay unavailable", error)
    return stream
  }
}

const preparePublishingStreamForSend = async (stream) => {
  if (!state.denoiseEnabled || !denoiseEngine || stream?.stopVideoSyncDelay) {
    return stream
  }
  return applyDenoiseVideoSync(stream)
}

const getProtectedTrackIds = (streams = []) => {
  const ids = new Set()
  streams.forEach((stream) => {
    stream?.getTracks?.().forEach((track) => ids.add(track.id))
  })
  return ids
}

const stopUnprotectedTracks = (stream, protectedStreams = []) => {
  const protectedTrackIds = getProtectedTrackIds(protectedStreams)
  stream?.stopVideoSyncDelay?.()
  stream?.getTracks?.().forEach((track) => {
    if (!protectedTrackIds.has(track.id)) {
      track.stop()
    }
  })
}

const replacePeerTrack = async (kind, nextTrack) => {
  const peers = Array.from(peerMap.values())
  if (cohostPeer) {
    peers.push(cohostPeer)
  }
  if (pkPeer) {
    peers.push(pkPeer)
  }
  await Promise.all(
    peers.map(async (peer) => {
      const sender = peer.getSenders().find((item) => item.track?.kind === kind)
      if (!sender) {
        return
      }
      await sender.replaceTrack(nextTrack || null)
    })
  )
}

const updatePreviewStream = () => {
  if (previewRef.value) {
    previewRef.value.srcObject = publishingStream
    previewRef.value.muted = true
    previewRef.value.playsInline = true
    previewRef.value.play?.().catch(() => {})
  }
}

const switchPublishingStream = async (nextStream) => {
  const previousStream = publishingStream
  nextStream = await preparePublishingStreamForSend(nextStream)
  const nextAudioTrack = nextStream?.getAudioTracks?.()[0] || null
  const nextVideoTrack = nextStream?.getVideoTracks?.()[0] || null

  publishingStream = nextStream
  updatePreviewStream()

  if (peerMap.size) {
    await replacePeerTrack("audio", nextAudioTrack)
    if (nextVideoTrack) {
      await replacePeerTrack("video", nextVideoTrack)
    }
  }

  if (previousStream && previousStream !== captureStream && previousStream !== nextStream) {
    stopUnprotectedTracks(previousStream, [captureStream, nextStream])
  }
}

const enableDenoiseDuringLive = async () => {
  if (!captureStream) {
    return
  }

  state.denoiseStatus = "connecting"
  state.denoiseDetail = "正在准备降噪..."

  denoiseEngine = createLiveDenoiseEngine({
    onStateChange: ({ status, detail, useEnhancedOutput }) => {
      state.denoiseStatus = status
      state.denoiseUsingEnhanced = Boolean(useEnhancedOutput)
      const nextDetail = getDenoiseStatusText(status, useEnhancedOutput)
      state.denoiseDetail = nextDetail
      if (nextDetail) {
        setMessage(nextDetail)
      }
    },
  })

  try {
    const result = await denoiseEngine.start(captureStream)
    const nextStream = await applyDenoiseVideoSync(result.stream)
    await switchPublishingStream(nextStream)
    state.denoiseStatus = "warming"
    state.denoiseBackend = result.backend || ""
    state.denoiseModelName = result.modelName || ""
    state.denoiseDetail = "降噪已开启"
    state.denoiseUsingEnhanced = false
    describeAudioProcessingState(nextStream)
    setMessage("降噪已开启，直播不中断")
  } catch (error) {
    state.denoiseStatus = "error"
    state.denoiseDetail = "降噪暂不可用，已继续使用原始麦克风声音"
    state.denoiseBackend = ""
    state.denoiseModelName = ""
    state.denoiseUsingEnhanced = false
    state.denoiseRuntimeInfo = ""
    $modal.msgWarning(state.denoiseDetail)
    if (denoiseEngine) {
      await denoiseEngine.stop()
      denoiseEngine = null
    }
    state.denoiseEnabled = false
    window.localStorage.setItem(DENOISE_STORAGE_KEY, "0")
  }
}

const disableDenoiseDuringLive = async () => {
  await switchPublishingStream(captureStream)
  if (denoiseEngine) {
    await denoiseEngine.stop()
    denoiseEngine = null
  }
  resetDenoiseState()
  describeAudioProcessingState(captureStream)
  setMessage("已切回原始麦克风音频")
}

const getMediaErrorMessage = (error) => {
  const errorName = error?.name || ""
  const rawMessage = error?.message || ""
  const normalizedMessage = rawMessage.toLowerCase()
  if (errorName === "NotAllowedError" || rawMessage.includes("Permission denied")) {
    return "浏览器尚未允许摄像头或麦克风权限，请在地址栏里放行后再试。"
  }
  if (errorName === "NotFoundError" || errorName === "DevicesNotFoundError") {
    return "没有检测到可用的摄像头或麦克风设备。"
  }
  if (errorName === "NotReadableError" || errorName === "TrackStartError") {
    return "摄像头或麦克风当前被其他应用占用，请先关闭占用它们的软件。"
  }
  if (errorName === "OverconstrainedError" || errorName === "ConstraintNotSatisfiedError") {
    return "当前设备不满足采集条件，请切换设备后重试。"
  }
  if (errorName === "AbortError") {
    return "媒体采集被中断，请重新尝试开播。"
  }
  if (errorName === "SecurityError") {
    return "当前访问方式不支持开播，请换一个安全的访问方式后重试。"
  }
  if (/deepfilter|denoise|降噪服务|web audio/.test(normalizedMessage)) {
    return "降噪暂不可用，已继续使用原始麦克风声音。"
  }
  if (/系统声音|电脑系统声音|screen live needs microphone|屏幕直播需要可用麦克风/.test(rawMessage)) {
    return "屏幕直播只采集麦克风声音，不采集电脑系统声音；请允许麦克风权限后再开播。"
  }
  if (/connection|socket/.test(normalizedMessage)) {
    return "直播连接暂不可用，请稍后重试。"
  }
  return rawMessage || "网页开播失败，请稍后重试"
}

const attachPreview = () => {
  if (previewRef.value) {
    previewRef.value.srcObject = publishingStream
    previewRef.value.muted = true
    previewRef.value.playsInline = true
    previewRef.value.play?.().catch((e) => setMessage("预览播放失败，请检查浏览器权限"))
  }
  const [videoTrack] = captureStream?.getVideoTracks?.() || []
  if (videoTrack) {
    videoTrack.onended = () => {
      stopBrowserLive()
    }
  }
}

const connectSignal = () =>
  new Promise((resolve, reject) => {
    closeSignal()
    const urls = createBrowserLiveFallbackUrls()
    let joined = false
    let currentIndex = 0

    const tryNext = () => {
      const signalUrl = urls[currentIndex]
      if (!signalUrl) {
        reject(new Error(state.message || "开播连接暂不可用"))
        return
      }

      setMessage("正在准备直播连接...")
      signalSocket = new WebSocket(signalUrl)

      signalSocket.onopen = () => {
        sendSignal({
          type: "join",
          role: "broadcaster",
          roomId: activeRoomId.value,
        })
      }

      signalSocket.onmessage = async (event) => {
        const data = JSON.parse(event.data)
        if (data.type === "joined") {
          joined = true
          state.signalConnected = true
          startHeartbeat()
          resolve()
          return
        }
        await handleSignalMessage(data)
      }

      signalSocket.onerror = () => {
        if (!joined) {
          signalSocket?.close?.()
        }
      }

      signalSocket.onclose = () => {
        stopHeartbeat()
        state.signalConnected = false
        if (!joined && !state.liveActive) {
          closeSignal()
          currentIndex += 1
          tryNext()
          return
        }
        if (state.liveActive) {
          state.liveActive = false
          setMessage("直播连接已断开，请重新开播")
          closeAllPeers()
          resetCohost()
          resetPk()
          releaseMediaResources()
        }
      }
    }

    tryNext()
  })

const handleSignalMessage = async (data) => {
  if (data.type === "viewer-joined" && data.sessionId) {
    await createOfferForViewer(data.sessionId)
    return
  }
  if (data.type === "viewer-left" && data.sessionId) {
    closePeer(data.sessionId)
    return
  }
  if (data.type === "answer" && data.fromSessionId && data.sdp) {
    const peer = peerMap.get(data.fromSessionId)
    if (peer) {
      await peer.setRemoteDescription(new RTCSessionDescription(data.sdp))
    }
    return
  }
  if (data.type === "ice-candidate" && data.fromSessionId && data.candidate) {
    const peer = peerMap.get(data.fromSessionId)
    if (peer) {
      await peer.addIceCandidate(new RTCIceCandidate(data.candidate))
    }
    return
  }
  if (data.type === "cohost-request" && data.fromSessionId) {
    if (cohost.active || cohost.status === "connecting") {
      sendSignal({
        type: "cohost-rejected",
        roomId: activeRoomId.value,
        targetSessionId: data.fromSessionId,
        reason: "主播当前已有连麦",
      })
      return
    }
    removeCohostRequest(data.fromSessionId)
    cohost.requests.unshift({
      ...data,
      receivedAt: Date.now(),
    })
    cohost.status = "invited"
    return
  }
  if (data.type === "cohost-answer" && data.fromSessionId && data.sdp && cohostPeer) {
    await cohostPeer.setRemoteDescription(new RTCSessionDescription(data.sdp))
    cohost.active = true
    cohost.status = "active"
    cohost.connectionText = "音视频已接入"
    return
  }
  if (data.type === "cohost-ice-candidate" && data.candidate && cohostPeer) {
    await cohostPeer.addIceCandidate(new RTCIceCandidate(data.candidate))
    return
  }
  if (data.type === "cohost-ended") {
    resetCohost()
    return
  }
  if (data.type === "pk-invite" && data.fromSessionId) {
    if (pkActive.value) {
      sendSignal({
        type: "pk-rejected",
        roomId: activeRoomId.value,
        targetSessionId: data.fromSessionId,
        targetRoomId: data.fromRoomId,
        reason: "对方主播正在 PK 或邀请中",
      })
      return
    }
    pk.pendingInvite = data
    pk.status = "idle"
    return
  }
  if (data.type === "pk-accepted" && data.fromSessionId) {
    pk.status = "connecting"
    pk.remoteSessionId = data.fromSessionId
    pk.remoteRoomId = data.fromRoomId || data.targetRoomId
    pk.remoteName = data.acceptorName || "对方主播"
    pk.connectionText = "对方已接受，正在建立 PK"
    await createPkOffer(data.fromSessionId, pk.remoteRoomId)
    return
  }
  if (data.type === "pk-rejected") {
    const reason = data.reason || "对方主播拒绝了 PK"
    $modal.msgWarning(reason)
    resetPk()
    return
  }
  if (data.type === "pk-unavailable") {
    $modal.msgWarning(data.message || "目标主播暂不可用")
    resetPk()
    return
  }
  if (data.type === "pk-offer" && data.fromSessionId && data.sdp) {
    await answerPkOffer(data)
    return
  }
  if (data.type === "pk-answer" && data.fromSessionId && data.sdp && pkPeer) {
    await pkPeer.setRemoteDescription(new RTCSessionDescription(data.sdp))
    pk.status = "active"
    pk.connectionText = "对方画面已接入"
    return
  }
  if (data.type === "pk-ice-candidate" && data.candidate && pkPeer) {
    await pkPeer.addIceCandidate(new RTCIceCandidate(data.candidate))
    return
  }
  if (data.type === "pk-ended") {
    resetPk()
    return
  }
  if (data.type === "heartbeat-ack") {
    return
  }
  if (data.type === "guard-violation") {
    await forceStopByGuard(formatGuardReason(data))
    return
  }
  if (data.type === "error") {
    setMessage("直播连接异常，请稍后重试")
    $modal.msgError(state.message)
  }
}

const createOfferForViewer = async (viewerSessionId) => {
  closePeer(viewerSessionId)
  const peer = createPeerConnection()
  peerMap.set(viewerSessionId, peer)
  updateViewerCount()

  publishingStream.getTracks().forEach((track) => {
    peer.addTrack(track, publishingStream)
  })

  peer.onicecandidate = (event) => {
    if (!event.candidate) {
      return
    }
    sendSignal({
      type: "ice-candidate",
      roomId: activeRoomId.value,
      targetSessionId: viewerSessionId,
      candidate: event.candidate,
    })
  }

  peer.onconnectionstatechange = () => {
    if (["failed", "closed", "disconnected"].includes(peer.connectionState)) {
      closePeer(viewerSessionId)
    }
  }

  const offer = await peer.createOffer()
  await peer.setLocalDescription(offer)
  sendSignal({
    type: "offer",
    roomId: activeRoomId.value,
    targetSessionId: viewerSessionId,
    sdp: offer,
  })
}

const onInteractionImgError = (event) => {
  event.target.src = defaultInteractionAvatar
}

const getPublishTracks = () => publishingStream?.getTracks?.() || []

const attachInteractionStream = async (videoRef, stream, muted = false) => {
  await nextTick()
  const video = videoRef.value
  if (!video || !stream) {
    return
  }
  video.srcObject = stream
  video.muted = muted
  video.playsInline = true
  video.play?.().catch(() => {})
}

const resetCohost = ({ notify = false } = {}) => {
  if (notify && cohost.remoteSessionId) {
    sendSignal({
      type: "cohost-ended",
      roomId: activeRoomId.value,
      targetSessionId: cohost.remoteSessionId,
    })
  }
  cohostPeer?.close?.()
  cohostPeer = null
  cohostRemoteStream = null
  if (cohostVideoRef.value) {
    cohostVideoRef.value.srcObject = null
  }
  cohost.status = "idle"
  cohost.active = false
  cohost.peer = null
  cohost.remoteStream = null
  cohost.remoteSessionId = ""
  cohost.remoteName = ""
  cohost.connectionText = ""
}

const resetPk = ({ notify = false } = {}) => {
  if (notify && pk.remoteSessionId) {
    sendSignal({
      type: "pk-ended",
      roomId: activeRoomId.value,
      targetSessionId: pk.remoteSessionId,
      targetRoomId: pk.remoteRoomId,
    })
  }
  pkPeer?.close?.()
  pkPeer = null
  pkRemoteStream = null
  if (pkVideoRef.value) {
    pkVideoRef.value.srcObject = null
  }
  pk.status = "idle"
  pk.pendingInvite = null
  pk.peer = null
  pk.remoteStream = null
  pk.remoteSessionId = ""
  pk.remoteRoomId = null
  pk.remoteName = ""
  pk.connectionText = ""
}

const removeCohostRequest = (sessionId) => {
  cohost.requests = cohost.requests.filter((item) => item.fromSessionId !== sessionId)
}

const acceptCohostRequest = async (request) => {
  if (!state.liveActive || !publishingStream) {
    $modal.msgWarning("请先开播，再处理连麦申请")
    return
  }
  if (!request?.fromSessionId) {
    return
  }
  resetCohost()
  removeCohostRequest(request.fromSessionId)
  cohost.status = "connecting"
  cohost.remoteSessionId = request.fromSessionId
  cohost.remoteName = request.applicantName || "观众"
  cohost.connectionText = "正在建立连麦"

  sendSignal({
    type: "cohost-accepted",
    roomId: activeRoomId.value,
    targetSessionId: request.fromSessionId,
    acceptorName: currentUserProfile.value.name,
  })
  await createCohostOffer(request.fromSessionId)
}

const rejectCohostRequest = (request, reason = "主播暂时无法连麦") => {
  if (!request?.fromSessionId) {
    return
  }
  removeCohostRequest(request.fromSessionId)
  sendSignal({
    type: "cohost-rejected",
    roomId: activeRoomId.value,
    targetSessionId: request.fromSessionId,
    reason,
  })
}

const endCohost = () => {
  resetCohost({ notify: true })
}

const createCohostOffer = async (targetSessionId) => {
  const peer = createPeerConnection()
  cohostPeer = peer
  cohost.peer = peer
  cohostRemoteStream = new MediaStream()
  cohost.remoteStream = cohostRemoteStream
  await attachInteractionStream(cohostVideoRef, cohostRemoteStream)

  getPublishTracks().forEach((track) => peer.addTrack(track, publishingStream))
  peer.ontrack = (event) => {
    const incoming = event.streams?.[0]
    if (incoming) {
      cohostRemoteStream = incoming
      cohost.remoteStream = incoming
      attachInteractionStream(cohostVideoRef, incoming)
    } else if (event.track && !cohostRemoteStream.getTracks().some((track) => track.id === event.track.id)) {
      cohostRemoteStream.addTrack(event.track)
      attachInteractionStream(cohostVideoRef, cohostRemoteStream)
    }
    cohost.active = true
    cohost.status = "active"
    cohost.connectionText = "音视频已接入"
  }
  peer.onicecandidate = (event) => {
    if (!event.candidate) return
    sendSignal({
      type: "cohost-ice-candidate",
      roomId: activeRoomId.value,
      targetSessionId,
      candidate: event.candidate,
    })
  }
  peer.onconnectionstatechange = () => {
    if (peer.connectionState === "connected") {
      cohost.active = true
      cohost.status = "active"
      cohost.connectionText = "音视频已接入"
    }
    if (["failed", "disconnected", "closed"].includes(peer.connectionState)) {
      resetCohost()
    }
  }

  const offer = await peer.createOffer()
  await peer.setLocalDescription(offer)
  sendSignal({
    type: "cohost-offer",
    roomId: activeRoomId.value,
    targetSessionId,
    sdp: offer,
  })
}

const invitePk = async () => {
  if (!state.liveActive) {
    $modal.msgWarning("请先开播，再发起 PK")
    return
  }
  const targetRoomId = Number(pk.targetRoomId)
  if (!targetRoomId || targetRoomId === Number(activeRoomId.value)) {
    $modal.msgWarning("请输入另一个正在开播的房间号")
    return
  }
  resetPk()
  pk.status = "inviting"
  pk.remoteRoomId = targetRoomId
  pk.connectionText = "等待对方接受"
  sendSignal({
    type: "pk-invite",
    roomId: activeRoomId.value,
    targetRoomId,
    inviterName: currentUserProfile.value.name,
    inviterAvatar: currentUserProfile.value.avatar,
  })
}

const acceptPkInvite = async () => {
  const invite = pk.pendingInvite
  if (!invite?.fromSessionId) {
    return
  }
  if (!state.liveActive || !publishingStream) {
    $modal.msgWarning("请先开播，再接受 PK")
    return
  }
  resetPk()
  pk.status = "connecting"
  pk.remoteSessionId = invite.fromSessionId
  pk.remoteRoomId = invite.fromRoomId
  pk.remoteName = invite.inviterName || "对方主播"
  pk.connectionText = "正在建立 PK"
  sendSignal({
    type: "pk-accepted",
    roomId: activeRoomId.value,
    targetSessionId: invite.fromSessionId,
    targetRoomId: invite.fromRoomId,
    acceptorName: currentUserProfile.value.name,
  })
}

const rejectPkInvite = () => {
  const invite = pk.pendingInvite
  if (!invite?.fromSessionId) {
    pk.pendingInvite = null
    return
  }
  sendSignal({
    type: "pk-rejected",
    roomId: activeRoomId.value,
    targetSessionId: invite.fromSessionId,
    targetRoomId: invite.fromRoomId,
    reason: "对方主播暂时无法 PK",
  })
  pk.pendingInvite = null
}

const endPk = () => {
  resetPk({ notify: true })
}

const createPkOffer = async (targetSessionId, targetRoomId) => {
  const peer = createPeerConnection()
  pkPeer = peer
  pk.peer = peer
  pkRemoteStream = new MediaStream()
  pk.remoteStream = pkRemoteStream
  pk.remoteSessionId = targetSessionId
  pk.remoteRoomId = targetRoomId || pk.remoteRoomId
  await attachInteractionStream(pkVideoRef, pkRemoteStream)

  getPublishTracks().forEach((track) => peer.addTrack(track, publishingStream))
  peer.ontrack = (event) => {
    const incoming = event.streams?.[0]
    if (incoming) {
      pkRemoteStream = incoming
      pk.remoteStream = incoming
      attachInteractionStream(pkVideoRef, incoming)
    } else if (event.track && !pkRemoteStream.getTracks().some((track) => track.id === event.track.id)) {
      pkRemoteStream.addTrack(event.track)
      attachInteractionStream(pkVideoRef, pkRemoteStream)
    }
    pk.status = "active"
    pk.connectionText = "对方画面已接入"
  }
  peer.onicecandidate = (event) => {
    if (!event.candidate) return
    sendSignal({
      type: "pk-ice-candidate",
      roomId: activeRoomId.value,
      targetSessionId,
      targetRoomId,
      candidate: event.candidate,
    })
  }
  peer.onconnectionstatechange = () => {
    if (peer.connectionState === "connected") {
      pk.status = "active"
      pk.connectionText = "对方画面已接入"
    }
    if (["failed", "disconnected", "closed"].includes(peer.connectionState)) {
      resetPk()
    }
  }

  const offer = await peer.createOffer()
  await peer.setLocalDescription(offer)
  sendSignal({
    type: "pk-offer",
    roomId: activeRoomId.value,
    targetSessionId,
    targetRoomId,
    inviterName: currentUserProfile.value.name,
    sdp: offer,
  })
}

const answerPkOffer = async (data) => {
  if (!state.liveActive || !publishingStream) {
    sendSignal({
      type: "pk-rejected",
      roomId: activeRoomId.value,
      targetSessionId: data.fromSessionId,
      targetRoomId: data.fromRoomId,
      reason: "对方主播未开播",
    })
    return
  }
  resetPk()
  pk.status = "connecting"
  pk.remoteSessionId = data.fromSessionId
  pk.remoteRoomId = data.fromRoomId
  pk.remoteName = data.inviterName || data.acceptorName || "对方主播"

  const peer = createPeerConnection()
  pkPeer = peer
  pk.peer = peer
  pkRemoteStream = new MediaStream()
  pk.remoteStream = pkRemoteStream
  await attachInteractionStream(pkVideoRef, pkRemoteStream)
  getPublishTracks().forEach((track) => peer.addTrack(track, publishingStream))
  peer.ontrack = (event) => {
    const incoming = event.streams?.[0]
    if (incoming) {
      pkRemoteStream = incoming
      pk.remoteStream = incoming
      attachInteractionStream(pkVideoRef, incoming)
    } else if (event.track && !pkRemoteStream.getTracks().some((track) => track.id === event.track.id)) {
      pkRemoteStream.addTrack(event.track)
      attachInteractionStream(pkVideoRef, pkRemoteStream)
    }
    pk.status = "active"
    pk.connectionText = "对方画面已接入"
  }
  peer.onicecandidate = (event) => {
    if (!event.candidate) return
    sendSignal({
      type: "pk-ice-candidate",
      roomId: activeRoomId.value,
      targetSessionId: data.fromSessionId,
      targetRoomId: data.fromRoomId,
      candidate: event.candidate,
    })
  }
  peer.onconnectionstatechange = () => {
    if (peer.connectionState === "connected") {
      pk.status = "active"
      pk.connectionText = "对方画面已接入"
    }
    if (["failed", "disconnected", "closed"].includes(peer.connectionState)) {
      resetPk()
    }
  }

  await peer.setRemoteDescription(new RTCSessionDescription(data.sdp))
  const answer = await peer.createAnswer()
  await peer.setLocalDescription(answer)
  sendSignal({
    type: "pk-answer",
    roomId: activeRoomId.value,
    targetSessionId: data.fromSessionId,
    targetRoomId: data.fromRoomId,
    sdp: answer,
  })
}

const handleDenoiseSwitch = async (checked) => {
  state.denoiseSwitching = true
  state.denoiseEnabled = checked
  window.localStorage.setItem(DENOISE_STORAGE_KEY, checked ? "1" : "0")

  try {
    if (!state.liveActive) {
      syncIdleDenoiseHint()
      return
    }

    if (checked) {
      await enableDenoiseDuringLive()
    } else {
      await disableDenoiseDuringLive()
    }
  } finally {
    state.denoiseSwitching = false
  }
}

const connectAnchorChatSocket = () => {
  const roomId = activeRoomId.value
  if (!roomId || chatSocket?.readyState === WebSocket.OPEN || chatSocket?.readyState === WebSocket.CONNECTING) {
    return
  }

  clearAnchorChatTimers()
  const token = store.user().userToken
  chatSocket = new WebSocket(createChatWebSocketUrl({ token }))

  chatSocket.onopen = () => {
    chatReconnectCount = 0
    sendAnchorChatMessage({
      msgType: 0,
      data: JSON.stringify({
        roomId: Number(roomId),
        anchorMonitor: true,
      }),
    })
    pushAnchorSystemMessage("已连接直播间弹幕")
    startAnchorChatHeartbeat()
  }

  chatSocket.onmessage = (event) => {
    let message
    try {
      message = JSON.parse(event.data)
    } catch (error) {
      return
    }
    if (message.method === "giftMessage") {
      appendAnchorGiftMessage(message.data)
      playAnchorGiftEffect(message.data)
      return
    }
    if (message.method === "chatMessage" || message.method === "welcomeMessage") {
      pushAnchorChatMessage(message.data)
      return
    }
    if (message.method === "guardViolation") {
      pushAnchorSystemMessage("直播内容触发风控，请前往审核记录查看处理结果")
      return
    }
    if (message.method === "muteUser" || message.method === "kickUser") {
      pushAnchorChatMessage(message.data)
    }
  }

  chatSocket.onerror = () => {}

  chatSocket.onclose = (event) => {
    clearAnchorChatTimers()
    chatSocket = null
    if (!state.liveActive || event.code === 1000 || event.code === 1005) {
      return
    }
    scheduleAnchorChatReconnect()
  }
}

const sendAnchorChatMessage = (payload) => {
  if (chatSocket?.readyState === WebSocket.OPEN) {
    chatSocket.send(JSON.stringify(payload))
  }
}

const startAnchorChatHeartbeat = () => {
  if (chatHeartbeatTimer) {
    window.clearInterval(chatHeartbeatTimer)
  }
  chatHeartbeatTimer = window.setInterval(() => {
    sendAnchorChatMessage({ msgType: 2 })
  }, CHAT_HEARTBEAT_INTERVAL)
}

const scheduleAnchorChatReconnect = () => {
  if (chatReconnectTimer || chatReconnectCount >= 12) {
    return
  }
  chatReconnectCount += 1
  chatReconnectTimer = window.setTimeout(() => {
    chatReconnectTimer = null
    connectAnchorChatSocket()
  }, 3000)
}

const clearAnchorChatTimers = () => {
  if (chatHeartbeatTimer) {
    window.clearInterval(chatHeartbeatTimer)
    chatHeartbeatTimer = null
  }
  if (chatReconnectTimer) {
    window.clearTimeout(chatReconnectTimer)
    chatReconnectTimer = null
  }
}

const closeAnchorChatSocket = () => {
  clearAnchorChatTimers()
  if (!chatSocket) {
    return
  }
  chatSocket.onopen = null
  chatSocket.onmessage = null
  chatSocket.onclose = null
  chatSocket.onerror = null
  chatSocket.close(1000)
  chatSocket = null
}

const startHeartbeat = () => {
  stopHeartbeat()
  heartbeatTimer = window.setInterval(() => {
    sendSignal({
      type: "heartbeat",
      roomId: activeRoomId.value,
    })
  }, HEARTBEAT_INTERVAL)
}

const stopHeartbeat = () => {
  if (heartbeatTimer) {
    window.clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

const startGuardLoop = () => {
  stopGuardLoop()
  state.guardActive = true
  runGuardCheck()
  guardTimer = window.setInterval(runGuardCheck, GUARD_CHECK_INTERVAL)
}

const stopGuardLoop = () => {
  if (guardTimer) {
    window.clearInterval(guardTimer)
    guardTimer = null
  }
  guardChecking = false
  state.guardActive = false
}

const captureGuardFrame = () =>
  new Promise((resolve) => {
    const video = previewRef.value
    if (!video || !video.videoWidth || !video.videoHeight) {
      resolve(null)
      return
    }
    const maxWidth = 960
    const scale = Math.min(1, maxWidth / video.videoWidth)
    const canvas = document.createElement("canvas")
    canvas.width = Math.max(1, Math.round(video.videoWidth * scale))
    canvas.height = Math.max(1, Math.round(video.videoHeight * scale))
    const context = canvas.getContext("2d")
    if (!context) {
      resolve(null)
      return
    }
    context.drawImage(video, 0, 0, canvas.width, canvas.height)
    canvas.toBlob((blob) => resolve(blob), "image/jpeg", 0.72)
  })

const runGuardCheck = async () => {
  if (!state.liveActive || guardChecking || !activeRoomId.value) {
    return
  }
  guardChecking = true
  try {
    const frame = await captureGuardFrame()
    if (!frame) {
      return
    }
    const response = await liveAPI.checkGuardFrame(activeRoomId.value, frame)
    const result = response?.data || {}
    if (result.banned) {
      await forceStopByGuard(formatGuardReason(result))
    } else if (result.status === "REVIEW") {
      const hint = result.reason || "风险内容已提交给管理员审核，直播继续进行"
      $modal.msgWarning(hint)
      setMessage(hint)
    }
  } catch (error) {
    // Keep the live room running; the next scheduled check will retry.
  } finally {
    guardChecking = false
  }
}

const forceStopByGuard = async (reason) => {
  $modal.msgError(reason)
  setMessage(reason)
  await stopBrowserLive({ skipApi: true, guardReason: reason })
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

const sendSignal = (payload) => {
  if (signalSocket?.readyState === WebSocket.OPEN) {
    signalSocket.send(JSON.stringify(payload))
  }
}

const getSupportedRecordMimeType = () => {
  if (typeof MediaRecorder === "undefined") {
    return ""
  }
  const candidates = [
    "video/webm;codecs=vp9,opus",
    "video/webm;codecs=vp8,opus",
    "video/webm",
  ]
  return candidates.find((type) => MediaRecorder.isTypeSupported(type)) || ""
}

const startLiveRecording = () => {
  if (!publishingStream || typeof MediaRecorder === "undefined") {
    return
  }

  const tracks = publishingStream.getTracks?.().filter((track) => track.readyState === "live") || []
  if (!tracks.length) {
    return
  }

  try {
    const mimeType = getSupportedRecordMimeType()
    liveRecordChunks = []
    liveRecordStartedAt = Date.now()
    liveRecorder = new MediaRecorder(new MediaStream(tracks), mimeType ? { mimeType } : undefined)
    liveRecorder.ondataavailable = (event) => {
      if (event.data && event.data.size > 0) {
        liveRecordChunks.push(event.data)
      }
    }
    liveRecorder.onerror = () => {
      liveRecordChunks = []
    }
    liveRecorder.start(3000)
  } catch (error) {
    liveRecorder = null
    liveRecordChunks = []
    liveRecordStartedAt = 0
  }
}

const stopLiveRecordingAndUpload = async () => {
  const recorder = liveRecorder
  if (!recorder) {
    return
  }

  const mimeType = recorder.mimeType || getSupportedRecordMimeType() || "video/webm"
  const duration = liveRecordStartedAt ? Math.max(1, Math.round((Date.now() - liveRecordStartedAt) / 1000)) : null
  const chunks = await new Promise((resolve) => {
    const done = () => resolve([...liveRecordChunks])
    recorder.onstop = done
    if (recorder.state === "inactive") {
      done()
      return
    }
    try {
      recorder.requestData?.()
      recorder.stop()
    } catch (error) {
      done()
    }
  })

  liveRecorder = null
  liveRecordChunks = []
  liveRecordStartedAt = 0

  if (!chunks.length || !activeRoomId.value) {
    return
  }

  const extension = mimeType.includes("mp4") ? "mp4" : "webm"
  const file = new File(chunks, `live-record-${activeRoomId.value}-${Date.now()}.${extension}`, { type: mimeType })
  await liveAPI.uploadLiveRecord({
    roomId: activeRoomId.value,
    duration,
    file,
  })
}

const stopBrowserLive = async (options = {}) => {
  const lastMode = state.isScreenSharing ? "screen" : "camera"
  state.liveActive = false
  state.signalConnected = false
  state.isScreenSharing = false
  state.selectedLiveMode = lastMode
  state.cameraPipEnabled = false
  closeAnchorChatSocket()
  stopGuardLoop()
  const stopMessage = options.guardReason || null
  setMessage("直播已停止")
  if (stopMessage) {
    setMessage(stopMessage)
  }
  stopPipComposite()
  resetCohost({ notify: true })
  resetPk({ notify: true })
  if (cameraStream) {
    cameraStream.getTracks().forEach((t) => t.stop())
    cameraStream = null
  }
  try {
    await stopLiveRecordingAndUpload()
  } catch (error) {
    $modal.msgWarning(error.message || "直播录像上传失败，请确认 MinIO 服务状态")
  }
  sendSignal({
    type: "leave",
    roomId: activeRoomId.value,
  })
  closeSignal()
  closeAllPeers()
  await releaseMediaResources()

  if (previewRef.value) {
    previewRef.value.srcObject = null
  }

  if (!options.skipApi && props.liveStatus === 1) {
    try {
      await liveAPI.stopLive()
    } catch (error) {
      // ignore stop errors to keep UI responsive
    }
  }

  emit("status-change")
}

const releaseMediaResources = async () => {
  const captureTrackIds = new Set(captureStream?.getTracks?.().map((track) => track.id) || [])

  if (publishingStream) {
    publishingStream.stopVideoSyncDelay?.()
    publishingStream.getTracks().forEach((track) => {
      if (!captureTrackIds.has(track.id)) {
        track.stop()
      }
    })
  }

  if (captureStream) {
    captureStream.getTracks().forEach((track) => track.stop())
  }

  auxiliaryStreams.forEach((stream) => {
    stream?.getTracks?.().forEach((track) => track.stop())
  })
  auxiliaryStreams = []

  captureStream = null
  publishingStream = null

  if (denoiseEngine) {
    await denoiseEngine.stop()
    denoiseEngine = null
  }
  if (state.denoiseEnabled) {
    syncIdleDenoiseHint()
  } else {
    resetDenoiseState()
  }
  state.denoiseRuntimeInfo = ""
}

const closeSignal = () => {
  stopHeartbeat()
  if (!signalSocket) {
    return
  }
  signalSocket.onopen = null
  signalSocket.onmessage = null
  signalSocket.onclose = null
  signalSocket.onerror = null
  signalSocket.close()
  signalSocket = null
}

const closePeer = (sessionId) => {
  const peer = peerMap.get(sessionId)
  if (!peer) {
    return
  }
  peer.close()
  peerMap.delete(sessionId)
  updateViewerCount()
}

const closeAllPeers = () => {
  Array.from(peerMap.keys()).forEach((sessionId) => closePeer(sessionId))
}

const updateViewerCount = () => {
  state.viewerCount = peerMap.size
}

syncIdleDenoiseHint()

onMounted(() => {
  document.addEventListener("fullscreenchange", syncPreviewFullscreenState)
})

onBeforeUnmount(async () => {
  document.removeEventListener("fullscreenchange", syncPreviewFullscreenState)
  if (document.fullscreenElement === previewStageRef.value) {
    await document.exitFullscreen?.().catch(() => {})
  }
  stopGuardLoop()
  stopPipComposite()
  if (cameraStream) {
    cameraStream.getTracks().forEach((t) => t.stop())
    cameraStream = null
  }
  closeAnchorChatSocket()
  closeSignal()
  closeAllPeers()
  resetCohost()
  resetPk()
  if (liveRecorder && liveRecorder.state !== "inactive") {
    try {
      liveRecorder.stop()
    } catch (error) {
      // ignore recorder cleanup errors
    }
  }
  liveRecorder = null
  liveRecordChunks = []
  liveRecordStartedAt = 0
  await releaseMediaResources()
})
</script>

<style scoped lang="scss">
.browser-live-panel {
  min-height: 720px;
  overflow: hidden;
  border-radius: 10px;
  background: #202329;
  color: #d8dde6;
  border: 1px solid #353a42;
  box-shadow: 0 18px 42px rgba(16, 20, 28, 0.22);
}

.studio-topbar {
  height: 52px;
  padding: 0 16px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #191c21;
  border-bottom: 1px solid #343840;
}

.studio-brand,
.studio-top-status,
.room-strip,
.control-dock,
.dock-metrics,
.chat-header {
  display: flex;
  align-items: center;
}

.studio-brand {
  gap: 10px;
}

.studio-logo {
  width: 26px;
  height: 26px;
  display: inline-grid;
  place-items: center;
  border-radius: 50%;
  background: #ffd22e;
  color: #22252b;
  font-weight: 900;
  font-size: 11px;
}

.studio-brand h3,
.studio-panel h4,
.room-strip h4,
.studio-bottom h4,
.chat-header h4 {
  margin: 0;
  color: #f4f7fb;
  font-size: 14px;
  font-weight: 700;
}

.studio-brand span:not(.studio-logo),
.room-strip span,
.chat-header span,
.source-item small,
.tool-row small,
.chat-empty span,
.chat-message span,
.studio-bottom p,
.dock-metrics span,
.data-grid span,
.status-line span {
  color: #9aa3b2;
  font-size: 12px;
}

.studio-top-status {
  gap: 8px;
}

.signal-pill,
.live-pill {
  padding: 5px 10px;
  border-radius: 999px;
  background: #2a2e35;
  color: #aab3c2;
  font-size: 12px;
  font-weight: 700;
}

.signal-pill.active {
  color: #8ce99a;
  background: rgba(40, 167, 69, 0.16);
}

.live-pill.active {
  color: #ffcf33;
  background: rgba(255, 191, 0, 0.16);
}

.studio-workspace {
  display: grid;
  grid-template-columns: 236px minmax(520px, 1fr) 252px;
  gap: 10px;
  padding: 10px;
}

.studio-left,
.studio-right,
.studio-center {
  min-width: 0;
}

.studio-left,
.studio-right {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.section-tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  height: 34px;
  background: #2a2e35;
  border: 1px solid #373c45;
}

.section-tabs button,
.source-item,
.tool-row {
  border: 0;
  font: inherit;
}

.section-tabs button {
  background: transparent;
  color: #aab3c2;
  cursor: pointer;
}

.section-tabs button.active {
  background: #3a3f48;
  color: #ffffff;
}

.studio-panel,
.studio-bottom section,
.studio-right {
  background: #282c33;
  border: 1px solid #383d46;
}

.studio-panel {
  padding: 12px;
}

.source-panel,
.tool-panel {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.source-item {
  width: 100%;
  min-height: 58px;
  padding: 10px;
  display: flex;
  align-items: center;
  gap: 10px;
  text-align: left;
  color: #edf2f8;
  background: #22262d;
  border: 1px solid #373c45;
  cursor: pointer;
}

.source-item:hover,
.source-item.selected {
  border-color: #f5c542;
  background: #30343b;
}

.source-item:disabled {
  cursor: wait;
  opacity: 0.72;
}

.source-icon {
  width: 32px;
  height: 32px;
  display: inline-grid;
  place-items: center;
  border-radius: 8px;
  background: #414751;
  color: #ffd44f;
  font-weight: 800;
}

.source-item span:last-child,
.tool-row span {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.source-item strong,
.tool-row strong,
.status-line strong,
.dock-metrics strong,
.data-grid strong,
.chat-message strong,
.chat-empty strong {
  color: #f5f7fb;
  font-size: 13px;
}

.tool-row {
  min-height: 54px;
  padding: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  color: #edf2f8;
  background: #22262d;
  border: 1px solid #373c45;
}

button.tool-row {
  width: 100%;
  cursor: pointer;
}

.tool-row em {
  min-width: 48px;
  padding: 4px 8px;
  border-radius: 999px;
  background: #363b44;
  color: #9aa3b2;
  font-style: normal;
  font-size: 12px;
  font-weight: 700;
  text-align: center;
}

.tool-row em.on {
  background: rgba(40, 167, 69, 0.18);
  color: #8ce99a;
}

.tool-row em.warn {
  background: rgba(245, 158, 11, 0.18);
  color: #f6c453;
}

.status-panel {
  display: grid;
  gap: 10px;
}

.status-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 9px;
  border-bottom: 1px solid #383d46;
}

.status-line:last-child {
  padding-bottom: 0;
  border-bottom: 0;
}

.studio-center {
  display: grid;
  grid-template-rows: auto minmax(340px, 1fr) auto auto;
  gap: 10px;
}

.room-strip {
  min-height: 54px;
  justify-content: space-between;
  gap: 14px;
  padding: 10px 12px;
  background: #282c33;
  border: 1px solid #383d46;
}

.room-strip > div:first-child {
  min-width: 0;
}

.room-strip span {
  display: block;
  margin-top: 4px;
  overflow: hidden;
  white-space: nowrap;
  text-overflow: ellipsis;
}

.preview-stage {
  position: relative;
  width: 100%;
  height: 100%;
  min-height: 380px;
  display: grid;
  place-items: center;
  overflow: hidden;
  background:
    linear-gradient(45deg, rgba(255, 255, 255, 0.035) 25%, transparent 25%),
    linear-gradient(-45deg, rgba(255, 255, 255, 0.035) 25%, transparent 25%),
    #06080b;
  background-size: 28px 28px;
  border: 1px solid #111418;
}

.preview-stage:fullscreen {
  width: 100vw;
  height: 100vh;
  min-height: 100vh;
  border: 0;
  background: #050609;
}

.studio-preview {
  width: 100%;
  height: 100%;
  min-height: 380px;
  object-fit: contain;
  background: #05070a;
}

.preview-stage--pk {
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: 0;
  align-items: stretch;
}

.preview-stage--pk .studio-preview {
  min-width: 0;
  border-right: 1px solid rgba(255, 255, 255, 0.1);
}

.preview-stage:fullscreen .studio-preview {
  min-height: 0;
  max-width: 100vw;
  max-height: 100vh;
  object-fit: contain;
}

.preview-empty {
  position: absolute;
  inset: 0;
  display: grid;
  place-content: center;
  gap: 8px;
  text-align: center;
  background: rgba(6, 8, 11, 0.72);
}

.preview-empty strong {
  color: #f5f7fb;
  font-size: 20px;
}

.preview-empty span {
  color: #9aa3b2;
}

.preview-live-tag {
  position: absolute;
  left: 14px;
  top: 14px;
  padding: 4px 8px;
  border-radius: 4px;
  background: #f43f5e;
  color: #ffffff;
  font-size: 12px;
  font-weight: 800;
}

.preview-fullscreen-btn {
  position: absolute;
  right: 14px;
  bottom: 14px;
  z-index: 12;
  width: 38px;
  height: 38px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 8px;
  color: #fff;
  background: rgba(5, 6, 9, 0.68);
  box-shadow: 0 10px 24px rgba(0, 0, 0, 0.28);
  backdrop-filter: blur(10px);
  cursor: pointer;
  transition:
    transform 0.2s ease,
    border-color 0.2s ease,
    background 0.2s ease;
}

.preview-fullscreen-btn:hover {
  transform: translateY(-1px);
  border-color: rgba(255, 159, 26, 0.58);
  background: rgba(255, 159, 26, 0.86);
}

.control-dock {
  justify-content: space-between;
  gap: 14px;
  padding: 12px;
  background: #282c33;
  border: 1px solid #383d46;
}

.dock-metrics {
  gap: 10px;
}

.dock-metrics div {
  min-width: 84px;
  padding: 7px 10px;
  display: grid;
  gap: 2px;
  background: #22262d;
  border: 1px solid #383d46;
}

.studio-bottom {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(0, 1fr);
  gap: 10px;
}

.studio-bottom section {
  min-height: 94px;
  padding: 12px;
}

.data-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.data-grid div {
  display: grid;
  gap: 4px;
  padding: 9px;
  background: #22262d;
}

.studio-bottom p {
  margin: 12px 0 0;
  line-height: 1.7;
}

.studio-right {
  min-height: 610px;
  overflow: hidden;
}

.chat-header {
  height: 45px;
  justify-content: space-between;
  padding: 0 12px;
  background: #242830;
  border-bottom: 1px solid #383d46;
}

.chat-feed {
  flex: 1;
  min-height: 260px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: auto;
}

.interaction-panel {
  margin: 14px 14px 0;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(min(100%, 220px), 1fr));
  gap: 12px;
}

.interaction-section {
  min-width: 0;
  padding: 14px;
  display: grid;
  gap: 12px;
  border: 1px solid var(--studio-border);
  border-radius: 8px;
  background: var(--studio-surface-raised);
}

.interaction-section__head,
.request-item,
.pk-form,
.pk-invite-card > div,
.interaction-video-head {
  display: flex;
  align-items: center;
}

.interaction-section__head {
  justify-content: space-between;
  gap: 10px;
}

.interaction-section__head > div {
  min-width: 0;
  display: grid;
  gap: 3px;
}

.interaction-section__head strong,
.request-item strong,
.pk-invite-card strong {
  color: var(--studio-text);
  font-size: 13px;
}

.interaction-section__head span,
.request-item span,
.interaction-empty,
.pk-invite-card span,
.pk-status-card span {
  color: var(--studio-muted);
  font-size: 12px;
}

.request-list {
  display: grid;
  gap: 8px;
}

.request-item {
  display: grid;
  grid-template-columns: 34px minmax(0, 1fr);
  gap: 8px;
  min-width: 0;
  padding: 8px;
  align-items: center;
  border-radius: 6px;
  background: var(--studio-surface);
}

.request-item img {
  width: 34px;
  height: 34px;
  flex: 0 0 auto;
  border-radius: 50%;
  object-fit: cover;
}

.request-item > div {
  min-width: 0;
  display: grid;
  gap: 2px;
}

.request-actions {
  grid-column: 1 / -1;
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8px;
  min-width: 0;
}

.request-actions :deep(.ant-btn) {
  min-width: 64px;
}

.request-item strong,
.request-item span {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.interaction-empty {
  margin: 0;
  line-height: 1.6;
}

.pk-form {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
}

.pk-target-input,
.pk-form :deep(.ant-input) {
  min-width: 0;
  width: 100%;
}

.pk-invite-card {
  display: grid;
  gap: 8px;
  padding: 10px;
  border-left: 3px solid var(--studio-accent);
  border-radius: 6px;
  background: var(--studio-surface);
}

.pk-invite-card > div {
  gap: 8px;
}

.pk-status-card {
  display: grid;
  gap: 8px;
  padding: 10px;
  border-left: 3px solid var(--studio-accent);
  border-radius: 6px;
  background: var(--studio-surface);
}

.pk-stage-card {
  position: relative;
  z-index: 5;
  display: grid;
  grid-template-rows: auto minmax(0, 1fr) auto;
  min-width: 0;
  width: 100%;
  height: 100%;
  min-height: 380px;
  overflow: hidden;
  background: #05070a;
}

.pk-stage-card video {
  width: 100%;
  height: 100%;
  min-height: 0;
  background: var(--player-bg);
  object-fit: contain;
}

.pk-stage-card small {
  display: block;
  padding: 8px 10px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
  background: rgba(5, 7, 10, 0.74);
}

.cohost-stage-card {
  position: absolute;
  right: 18px;
  top: 18px;
  z-index: 6;
  width: min(260px, 34%);
  min-width: 180px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.16);
  border-radius: 8px;
  background: rgba(12, 14, 18, 0.88);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.28);
}

.cohost-stage-card video {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #05070a;
  object-fit: cover;
}

.cohost-stage-card small {
  display: block;
  padding: 7px 9px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 12px;
}

.interaction-video-head {
  justify-content: space-between;
  gap: 8px;
  padding: 8px 9px;
  color: #fff;
  background: rgba(5, 7, 10, 0.74);
}

.interaction-video-head span {
  font-size: 12px;
  font-weight: 800;
}

.interaction-video-head button {
  padding: 0;
  border: 0;
  color: #ffb4b4;
  background: transparent;
  font-size: 12px;
  cursor: pointer;
}

.chat-message,
.chat-empty {
  padding: 10px;
  display: grid;
  gap: 5px;
  background: #22262d;
  border-left: 3px solid #f5c542;
}

.chat-empty {
  margin-top: auto;
  margin-bottom: auto;
  text-align: center;
  border-left: 0;
  background: transparent;
}

.anchor-chat-composer {
  min-height: 56px;
  padding: 10px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  align-items: center;
  gap: 8px;
  background: #242830;
  border-top: 1px solid #383d46;
}

.anchor-chat-composer :deep(.ant-input) {
  min-width: 0;
  color: #f5f7fb;
  background: #191c21;
  border-color: #383d46;
}

.chat-message.self {
  border-left-color: #5db7ff;
}

.chat-message.gift {
  border-left-color: #ffcf4a;
}

@media (max-width: 1180px) {
  .studio-workspace {
    grid-template-columns: 220px minmax(0, 1fr);
  }

  .studio-right {
    grid-column: 1 / -1;
    min-height: 240px;
  }

  .chat-feed {
    min-height: 150px;
  }
}

@media (max-width: 820px) {
  .browser-live-panel {
    border-radius: 0;
  }

  .studio-topbar,
  .room-strip,
  .control-dock {
    align-items: flex-start;
    flex-direction: column;
  }

  .studio-workspace {
    grid-template-columns: 1fr;
  }

  .studio-bottom {
    grid-template-columns: 1fr;
  }

  .dock-metrics {
    width: 100%;
    flex-wrap: wrap;
  }

  .dock-metrics div {
    flex: 1;
  }

  .preview-stage,
  .studio-preview {
    min-height: 280px;
  }
}

.browser-live-panel {
  --studio-bg: #111317;
  --studio-surface: #1b1e23;
  --studio-surface-raised: #22262c;
  --studio-surface-soft: #252a31;
  --studio-border: #303640;
  --studio-border-strong: #3b424d;
  --studio-text: #eef2f7;
  --studio-muted: #a3abb8;
  --studio-subtle: #798190;
  --studio-accent: #ff9f1a;
  --studio-accent-soft: rgba(255, 159, 26, 0.16);
  --studio-success: #21c083;
  --studio-danger: #f04f5f;
  min-height: 760px;
  border-radius: 8px;
  background: var(--studio-bg);
  color: var(--studio-text);
  border-color: var(--studio-border);
  box-shadow: 0 18px 46px rgba(10, 13, 18, 0.28);
}

.studio-topbar {
  height: 68px;
  padding: 0 22px;
  background: #16191e;
  border-bottom-color: var(--studio-border);
}

.studio-brand {
  gap: 14px;
}

.studio-logo {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background: var(--studio-accent);
  color: #171717;
  font-size: 13px;
  box-shadow: 0 8px 18px rgba(255, 159, 26, 0.22);
}

.studio-brand h3,
.studio-panel h4,
.room-strip h4,
.studio-bottom h4,
.chat-header h4 {
  color: var(--studio-text);
  font-size: 15px;
}

.studio-brand h3 {
  font-size: 17px;
}

.studio-brand span:not(.studio-logo),
.room-strip span,
.chat-header span,
.source-item small,
.tool-row small,
.chat-empty span,
.chat-message span,
.studio-bottom p,
.dock-metrics span,
.data-grid span,
.status-line span {
  color: var(--studio-muted);
}

.studio-top-status {
  gap: 10px;
}

.signal-pill,
.live-pill {
  padding: 7px 12px;
  background: var(--studio-surface-soft);
  color: var(--studio-muted);
}

.signal-pill.active {
  color: var(--studio-success);
  background: rgba(33, 192, 131, 0.15);
}

.live-pill.active {
  color: var(--studio-accent);
  background: var(--studio-accent-soft);
}

.studio-workspace {
  grid-template-columns: 272px minmax(0, 1fr);
  gap: 16px;
  padding: 16px;
  background: var(--studio-bg);
}

.studio-left,
.studio-right {
  gap: 14px;
}

.section-tabs {
  height: 44px;
  padding: 4px;
  border-radius: 8px;
  background: #191c21;
  border-color: var(--studio-border);
}

.section-tabs button {
  border-radius: 6px;
  color: var(--studio-muted);
  font-weight: 700;
}

.section-tabs button.active {
  background: var(--studio-surface-soft);
  color: var(--studio-text);
}

.studio-panel,
.studio-bottom section,
.studio-right,
.room-strip,
.control-dock {
  border-radius: 8px;
  background: var(--studio-surface);
  border-color: var(--studio-border);
}

.studio-panel {
  padding: 16px;
}

.source-panel,
.tool-panel,
.status-panel {
  gap: 14px;
}

.source-item,
.tool-row {
  border-radius: 8px;
  background: var(--studio-surface-raised);
  border-color: var(--studio-border);
}

.source-item {
  min-height: 74px;
  padding: 14px;
  gap: 14px;
}

.source-item:hover,
.source-item.selected {
  border-color: var(--studio-accent);
  background: #292e36;
}

.source-icon {
  width: 42px;
  height: 42px;
  border-radius: 8px;
  background: var(--studio-accent-soft);
  color: var(--studio-accent);
  font-size: 14px;
}

.source-item span:last-child,
.tool-row span {
  gap: 5px;
}

.source-item strong,
.tool-row strong,
.status-line strong,
.dock-metrics strong,
.data-grid strong,
.chat-message strong,
.chat-empty strong {
  color: var(--studio-text);
  font-size: 14px;
}

.tool-row {
  min-height: 68px;
  padding: 14px;
  gap: 16px;
}

button.tool-row:hover {
  border-color: var(--studio-accent);
  background: #292e36;
}

.tool-row em {
  min-width: 58px;
  padding: 6px 10px;
  border-radius: 999px;
  background: #303640;
  color: var(--studio-muted);
}

.tool-row em.on {
  background: rgba(33, 192, 131, 0.16);
  color: var(--studio-success);
}

.tool-row em.warn {
  background: var(--studio-accent-soft);
  color: var(--studio-accent);
}

.status-line {
  padding-bottom: 12px;
  border-bottom-color: var(--studio-border);
}

.studio-center {
  grid-template-rows: auto minmax(430px, 1fr) auto auto;
  gap: 16px;
}

.room-strip {
  min-height: 76px;
  padding: 16px 18px;
  gap: 20px;
}

.room-strip span {
  margin-top: 6px;
}

.preview-stage {
  min-height: 460px;
  border-radius: 8px;
  border-color: #252b34;
  background:
    linear-gradient(45deg, rgba(255, 255, 255, 0.025) 25%, transparent 25%),
    linear-gradient(-45deg, rgba(255, 255, 255, 0.025) 25%, transparent 25%),
    #080a0d;
  background-size: 32px 32px;
}

.studio-preview {
  min-height: 460px;
  background: #07080a;
}

.preview-empty {
  gap: 10px;
  background: rgba(8, 10, 13, 0.78);
}

.preview-empty strong {
  font-size: 22px;
}

.preview-live-tag {
  left: 18px;
  top: 18px;
  padding: 6px 10px;
  border-radius: 4px;
  background: var(--studio-danger);
}

.control-dock {
  padding: 16px 18px;
  gap: 18px;
}

.control-dock :deep(.ant-btn) {
  min-width: 136px;
  height: 44px;
  border-radius: 6px;
  font-weight: 800;
}

.dock-metrics {
  gap: 12px;
}

.dock-metrics div {
  min-width: 106px;
  padding: 10px 14px;
  border-radius: 8px;
  background: var(--studio-surface-raised);
  border-color: var(--studio-border);
}

.studio-bottom {
  gap: 16px;
}

.studio-bottom section {
  min-height: 108px;
  padding: 16px;
}

.data-grid {
  gap: 10px;
}

.data-grid div {
  padding: 12px;
  border-radius: 6px;
  background: var(--studio-surface-raised);
}

.studio-right {
  grid-column: 1 / -1;
  min-height: 260px;
}

.chat-header {
  height: 56px;
  padding: 0 16px;
  background: #191c21;
  border-bottom-color: var(--studio-border);
}

.chat-feed {
  height: calc(100% - 116px);
  min-height: 160px;
  padding: 14px;
  gap: 10px;
}

.chat-message,
.chat-empty {
  padding: 14px;
  border-radius: 8px;
  background: var(--studio-surface-raised);
  border-left-color: var(--studio-accent);
}

.anchor-chat-composer {
  min-height: 62px;
  padding: 12px;
  background: #191c21;
  border-top-color: var(--studio-border);
}

@media (min-width: 1540px) {
  .studio-workspace {
    grid-template-columns: 272px minmax(620px, 1fr) 300px;
  }

  .studio-right {
    grid-column: auto;
    min-height: 650px;
  }

  .chat-feed {
    min-height: 520px;
  }
}

@media (max-width: 1080px) {
  .studio-workspace {
    grid-template-columns: 1fr;
  }

  .studio-center {
    grid-template-rows: auto minmax(320px, auto) auto auto;
  }

  .preview-stage,
  .studio-preview {
    min-height: 340px;
  }
}

@media (max-width: 820px) {
  .browser-live-panel {
    min-height: 0;
  }

  .studio-topbar,
  .room-strip,
  .control-dock {
    gap: 14px;
  }

  .studio-workspace {
    grid-template-columns: 1fr;
    gap: 14px;
    padding: 12px;
  }

  .studio-center {
    grid-template-rows: auto auto auto auto;
  }

  .dock-metrics {
    gap: 10px;
  }
  .control-dock :deep(.ant-btn) {
    width: 100%;
    flex: 1;
    min-width: 0;
  }

  .dock-metrics {
    flex-direction: column;
  }

  .dock-metrics div {
    width: 100%;
  }

  .preview-stage,
  .studio-preview {
    min-height: 320px;
  }

  .control-dock :deep(.ant-btn) {
    min-width: 0;
  }
}

.browser-live-panel {
  --studio-bg: color-mix(in srgb, var(--bg-primary) 72%, var(--bg-card));
  --studio-topbar-bg: var(--bg-card);
  --studio-surface: var(--bg-card);
  --studio-surface-raised: color-mix(in srgb, var(--bg-card) 82%, var(--bg-secondary));
  --studio-surface-soft: var(--bg-secondary);
  --studio-border: var(--border);
  --studio-border-strong: var(--border-strong);
  --studio-text: var(--text-primary);
  --studio-muted: var(--text-secondary);
  --studio-subtle: var(--text-muted);
  --studio-accent: var(--accent);
  --studio-accent-strong: var(--accent-strong);
  --studio-accent-soft: var(--accent-light);
  --studio-success: var(--success);
  --studio-danger: var(--danger);
  min-height: 720px;
  border-color: var(--studio-border);
  background: var(--studio-bg);
  color: var(--studio-text);
  box-shadow: var(--shadow);
}

:global(html[data-theme="dark"]) .browser-live-panel,
:global(html[data-theme="cyberpunk"]) .browser-live-panel {
  --studio-bg: color-mix(in srgb, var(--bg-primary) 86%, #000);
  --studio-topbar-bg: var(--bg-header-soft);
  --studio-surface: var(--bg-card);
  --studio-surface-raised: color-mix(in srgb, var(--bg-card) 82%, #000);
  --studio-surface-soft: var(--bg-secondary);
  --studio-border: var(--border);
  --studio-border-strong: var(--border-strong);
  --studio-accent-soft: var(--accent-soft);
}

.studio-topbar,
.chat-header,
.anchor-chat-composer {
  background: var(--studio-topbar-bg);
  border-color: var(--studio-border);
}

.studio-logo {
  background: var(--studio-accent);
  color: var(--accent-text);
  box-shadow: 0 8px 18px color-mix(in srgb, var(--studio-accent) 24%, transparent);
}

.studio-brand h3,
.studio-panel h4,
.room-strip h4,
.studio-bottom h4,
.chat-header h4,
.source-item strong,
.tool-row strong,
.status-line strong,
.dock-metrics strong,
.data-grid strong,
.chat-message strong,
.chat-empty strong {
  color: var(--studio-text);
}

.studio-brand span:not(.studio-logo),
.room-strip span,
.chat-header span,
.source-item small,
.tool-row small,
.chat-empty span,
.chat-message span,
.studio-bottom p,
.dock-metrics span,
.data-grid span,
.status-line span {
  color: var(--studio-muted);
}

.signal-pill,
.live-pill,
.tool-row em {
  background: var(--studio-surface-soft);
  color: var(--studio-muted);
}

.signal-pill.active {
  color: var(--studio-success);
  background: color-mix(in srgb, var(--studio-success) 14%, transparent);
}

.live-pill.active,
.tool-row em.warn {
  color: var(--studio-accent-strong);
  background: var(--studio-accent-soft);
}

.studio-workspace {
  grid-template-columns: 280px minmax(0, 1fr);
  gap: 20px;
  padding: 20px;
  background: var(--studio-bg);
}

.section-tabs,
.studio-panel,
.studio-bottom section,
.studio-right,
.room-strip,
.control-dock,
.source-item,
.tool-row,
.dock-metrics div,
.data-grid div,
.chat-message,
.chat-empty {
  border-color: var(--studio-border);
  background: var(--studio-surface);
}

.section-tabs,
.source-item,
.tool-row,
.dock-metrics div,
.data-grid div,
.chat-message {
  background: var(--studio-surface-raised);
}

.section-tabs button {
  color: var(--studio-muted);
}

.section-tabs button.active {
  background: var(--studio-accent-soft);
  color: var(--studio-accent-strong);
}

.source-item,
.tool-row {
  color: var(--studio-text);
}

.source-item:hover,
.source-item.selected,
button.tool-row:hover {
  border-color: color-mix(in srgb, var(--studio-accent) 55%, var(--studio-border));
  background: color-mix(in srgb, var(--studio-accent-soft) 42%, var(--studio-surface-raised));
}

.source-icon {
  background: var(--studio-accent-soft);
  color: var(--studio-accent-strong);
}

.tool-row em.on {
  background: color-mix(in srgb, var(--studio-success) 14%, transparent);
  color: var(--studio-success);
}

.preflight-panel {
  gap: 12px;
}

.preflight-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.preflight-head h4 {
  margin: 0;
}

.preflight-list {
  display: grid;
  gap: 8px;
}

.preflight-item {
  display: grid;
  gap: 4px;
  padding: 10px;
  border: 1px solid var(--studio-border);
  border-radius: 6px;
  background: var(--studio-surface-raised);
}

.preflight-item span {
  display: flex;
  align-items: center;
  gap: 7px;
  min-width: 0;
  color: var(--studio-text);
  font-size: 13px;
  font-weight: 800;
}

.preflight-item i {
  width: 7px;
  height: 7px;
  flex: 0 0 auto;
  border-radius: 50%;
  background: var(--studio-subtle);
}

.preflight-item small {
  color: var(--studio-muted);
  font-size: 12px;
  line-height: 1.5;
}

.preflight-item--ok i {
  background: var(--studio-success);
}

.preflight-item--warn i {
  background: var(--studio-accent);
}

.preflight-item--error i {
  background: var(--studio-danger);
}

.status-line {
  border-bottom-color: var(--studio-border);
}

.preview-stage {
  border-color: var(--studio-border-strong);
  background:
    linear-gradient(45deg, rgba(255, 255, 255, 0.055) 25%, transparent 25%),
    linear-gradient(-45deg, rgba(255, 255, 255, 0.055) 25%, transparent 25%),
    var(--player-bg);
  background-size: 32px 32px;
}

.studio-preview {
  background: var(--player-bg);
}

.preview-empty {
  background: color-mix(in srgb, var(--player-bg) 76%, transparent);
}

.preview-empty strong {
  color: #fff;
}

.preview-empty span {
  color: rgba(255, 255, 255, 0.72);
}

.preview-live-tag {
  background: var(--studio-danger);
}

.chat-message,
.chat-empty {
  border-left-color: var(--studio-accent);
}

.anchor-chat-composer :deep(.ant-input) {
  color: var(--studio-text);
  background: var(--studio-surface-raised);
  border-color: var(--studio-border);
}

@media (min-width: 1540px) {
  .studio-workspace {
    grid-template-columns: 280px minmax(640px, 1fr) 320px;
  }
}

@media (max-width: 1080px) {
  .studio-workspace {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 820px) {
  .studio-workspace {
    gap: 14px;
    padding: 12px;
  }
}
</style>
