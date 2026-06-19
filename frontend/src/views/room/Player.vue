<template>
  <div class="player-shell">
    <video ref="videoElementRef" id="videoElement" :class="{ 'is-unmute-blocked': playbackBlocked }" autoplay playsinline></video>
    <div class="player-topbar">
      <div v-if="statusText" class="status-chip">{{ statusText }}</div>
      <div class="player-actions">
        <a-button
          v-if="cohostEnabled"
          size="small"
          class="cohost-btn"
          :loading="cohostState === 'requesting'"
          :danger="cohostActive"
          @click="toggleCohost"
        >
          {{ cohostButtonText }}
        </a-button>
      </div>
    </div>
    <div v-if="cohostActive || cohostState === 'connecting'" class="cohost-self-card">
      <div class="cohost-self-card__head">
        <span>{{ cohostStatusText }}</span>
        <button type="button" @click="endCohost">挂断</button>
      </div>
      <video ref="cohostPreviewRef" autoplay muted playsinline></video>
    </div>
    <div
      v-if="playbackBlocked"
      class="playback-overlay"
      @pointerdown.stop.prevent="resumePlayback"
      @mousedown.stop.prevent="resumePlayback"
      @touchstart.stop.prevent="resumePlayback"
      @click.stop.prevent="resumePlayback"
    >
      <button
        type="button"
        class="playback-unmute-btn"
        @pointerdown.stop.prevent="resumePlayback"
        @mousedown.stop.prevent="resumePlayback"
        @touchstart.stop.prevent="resumePlayback"
        @click.stop.prevent="resumePlayback"
      >
        点击开启声音
      </button>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue"
import $modal from "@/utils/message"
import { createBrowserLiveFallbackUrls, createPeerConnection } from "@/utils/browserLive"
import { toPublicLiveUrl } from "@/utils/publicLiveUrl"

const HEARTBEAT_INTERVAL = 15000
const LIVE_SYNC_LATENCY_SECONDS = 1
const LIVE_SYNC_DRIFT_TOLERANCE_SECONDS = 0.25
const STALL_CHECK_INTERVAL = 2000
const STALL_RECONNECT_THRESHOLD = 3
const RECORDING_EXTENSIONS = [".mp4", ".webm", ".mov", ".ogg"]
const PRIVATE_PAGE_HOST_RE = /^(localhost|127\.0\.0\.1|::1|10\.\d+\.\d+\.\d+|172\.(1[6-9]|2\d|3[0-1])\.\d+\.\d+|192\.168\.\d+\.\d+)$/

const props = defineProps({
  roomId: {
    type: [String, Number],
    default: undefined,
  },
  pullUrl: {
    type: String,
    default: "",
  },
  browserLive: {
    type: Boolean,
    default: false,
  },
  cohostEnabled: {
    type: Boolean,
    default: false,
  },
  applicantName: {
    type: String,
    default: "观众",
  },
  applicantAvatar: {
    type: String,
    default: "",
  },
})

const emit = defineEmits(["cohost-state-change", "require-login"])

const flvPlayer = ref(null)
const hlsPlayer = ref(null)
const videoElementRef = ref(null)
const cohostPreviewRef = ref(null)
const statusText = ref("")
const volumeValue = ref(100)
const volumeMuted = ref(false)
const playbackBlocked = ref(false)
const cohostState = ref("idle")
const cohostMessage = ref("")

let browserLiveFallbackTimer = null
let signalSocket = null
let peer = null
let cohostPeer = null
let remoteStream = null
let cohostLocalStream = null
let broadcasterSessionId = null
let signalUrlIndex = 0
let heartbeatTimer = null
let latencySyncTimer = null
let playbackWatchTimer = null
let lastVideoTime = 0
let stalledTicks = 0
let resumePlaybackPromise = null
let hlsModulePromise = null
let flvModulePromise = null
let pullPlaybackFailed = false

const cohostActive = computed(() => ["connecting", "active"].includes(cohostState.value))
const playablePullUrl = computed(() => toPublicLiveUrl(props.pullUrl))
const playablePullPath = computed(() => String(playablePullUrl.value || "").split("?")[0].toLowerCase())
const isRecordingUrl = computed(() => {
  return RECORDING_EXTENSIONS.some((extension) => playablePullPath.value.endsWith(extension))
})
const isHlsUrl = computed(() => playablePullPath.value.endsWith(".m3u8"))
const isFlvUrl = computed(() => playablePullPath.value.endsWith(".flv"))
const isPrivatePageHost = computed(() => PRIVATE_PAGE_HOST_RE.test(String(globalThis.location?.hostname || "")))
const shouldPreferPullStream = computed(() => Boolean(playablePullUrl.value) && (!props.browserLive || !isPrivatePageHost.value))
const cohostButtonText = computed(() => {
  if (cohostState.value === "requesting") return "申请中"
  if (cohostState.value === "connecting") return "连接中"
  if (cohostState.value === "active") return "结束连麦"
  return "申请连麦"
})
const cohostStatusText = computed(() => cohostMessage.value || cohostButtonText.value)

const updateCohostState = (state, message = "") => {
  cohostState.value = state
  cohostMessage.value = message
  emit("cohost-state-change", { state, message })
}

const loadHls = () => {
  if (!hlsModulePromise) {
    hlsModulePromise = import("hls.js").then((module) => module.default || module)
  }
  return hlsModulePromise
}

const loadFlv = () => {
  if (!flvModulePromise) {
    flvModulePromise = import("flv.js").then((module) => module.default || module)
  }
  return flvModulePromise
}

const getLiveLatency = (video) => {
  const ranges = video?.buffered
  if (!ranges?.length) {
    return null
  }
  const liveEdge = ranges.end(ranges.length - 1)
  return liveEdge - video.currentTime
}

const syncPlaybackToOneSecondLatency = () => {
  const video = videoElementRef.value
  if (!video || video.paused || video.seeking) {
    return
  }
  const latency = getLiveLatency(video)
  if (latency === null) {
    return
  }
  const drift = latency - LIVE_SYNC_LATENCY_SECONDS
  if (Math.abs(drift) > LIVE_SYNC_DRIFT_TOLERANCE_SECONDS) {
    const targetTime = video.buffered.end(video.buffered.length - 1) - LIVE_SYNC_LATENCY_SECONDS
    if (Number.isFinite(targetTime) && targetTime > 0) {
      video.currentTime = targetTime
    }
  }
  video.playbackRate = 1
}

const startLatencySync = () => {
  if (isRecordingUrl.value) {
    return
  }
  stopLatencySync()
  latencySyncTimer = window.setInterval(syncPlaybackToOneSecondLatency, 500)
}

const stopLatencySync = () => {
  if (latencySyncTimer) {
    window.clearInterval(latencySyncTimer)
    latencySyncTimer = null
  }
}

const startPlaybackWatch = () => {
  stopPlaybackWatch()
  lastVideoTime = videoElementRef.value?.currentTime || 0
  stalledTicks = 0
  playbackWatchTimer = window.setInterval(async () => {
    const video = videoElementRef.value
    if (!video || !remoteStream || video.paused || video.readyState < 2) {
      return
    }

    const currentTime = video.currentTime || 0
    if (currentTime > lastVideoTime + 0.05) {
      lastVideoTime = currentTime
      stalledTicks = 0
      return
    }

    stalledTicks += 1
    await ensureVideoPlayback("正在播放直播")
    if (stalledTicks >= STALL_RECONNECT_THRESHOLD) {
      stalledTicks = 0
      statusText.value = "直播画面卡住，正在重新连接..."
      closePeer()
      closeSignalOnly()
      connectBrowserLiveViewer()
    }
  }, STALL_CHECK_INTERVAL)
}

const stopPlaybackWatch = () => {
  if (playbackWatchTimer) {
    window.clearInterval(playbackWatchTimer)
    playbackWatchTimer = null
  }
  stalledTicks = 0
}

const ensureVideoPlayback = async (hintText = "正在播放直播") => {
  const video = videoElementRef.value
  if (!video) {
    return
  }

  try {
    await video.play()
    startLatencySync()
    if (video.muted || video.volume === 0) {
      playbackBlocked.value = true
      statusText.value = "正在播放直播，点击开启声音"
    } else {
      playbackBlocked.value = false
      statusText.value = hintText
    }
  } catch (error) {
    video.muted = true
    try {
      await video.play()
      startLatencySync()
      playbackBlocked.value = true
      statusText.value = "正在播放直播，点击开启声音"
    } catch (mutedError) {
      playbackBlocked.value = true
      statusText.value = "直播画面已准备好，请点击播放器开始观看"
    }
  }
}

const prepareAudiblePlayback = () => {
  const video = videoElementRef.value
  if (!video || playbackBlocked.value) {
    return
  }
  video.muted = false
  if (video.volume === 0) {
    video.volume = 1
  }
  syncVolumeState()
}

const syncVolumeState = () => {
  const video = videoElementRef.value
  if (!video) {
    return
  }
  volumeValue.value = Math.round((video.volume || 0) * 100)
  volumeMuted.value = Boolean(video.muted || video.volume === 0)
}

const handleVolumeChange = (nextValue) => {
  const video = videoElementRef.value
  if (!video) {
    return
  }
  const normalized = Math.max(0, Math.min(100, Number(nextValue) || 0)) / 100
  video.muted = normalized === 0 ? true : false
  video.volume = normalized
  syncVolumeState()
}

const setVolume = (nextValue) => {
  handleVolumeChange(nextValue)
}

const toggleMute = () => {
  const video = videoElementRef.value
  if (!video) {
    return
  }
  if (video.muted || video.volume === 0) {
    video.muted = false
    if (video.volume === 0) {
      video.volume = 1
    }
  } else {
    video.muted = true
  }
  syncVolumeState()
}

const sendSignal = (payload) => {
  if (signalSocket?.readyState === WebSocket.OPEN) {
    signalSocket.send(JSON.stringify(payload))
  }
}

const ensureCohostMediaAvailable = () => {
  if (navigator.mediaDevices?.getUserMedia) {
    return true
  }
  const message = "连麦需要摄像头和麦克风权限。外网演示请使用 HTTPS 访问，HTTP 域名只能观看、聊天和送礼。"
  $modal.msgWarning(message)
  updateCohostState("idle", message)
  return false
}

const createCohostStream = async () => {
  const stream = await navigator.mediaDevices.getUserMedia({
    video: { width: { ideal: 640 }, height: { ideal: 360 } },
    audio: {
      channelCount: 1,
      echoCancellation: true,
      noiseSuppression: true,
      autoGainControl: true,
    },
  })
  cohostLocalStream = stream
  await nextTick()
  if (cohostPreviewRef.value) {
    cohostPreviewRef.value.srcObject = stream
    cohostPreviewRef.value.muted = true
    cohostPreviewRef.value.play?.().catch(() => {})
  }
  return stream
}

const releaseCohostStream = () => {
  cohostLocalStream?.getTracks?.().forEach((track) => track.stop())
  cohostLocalStream = null
  if (cohostPreviewRef.value) {
    cohostPreviewRef.value.srcObject = null
  }
}

const toggleCohost = async () => {
  if (cohostState.value === "idle") {
    await requestCohost()
    return
  }
  endCohost()
}

const requestCohost = async () => {
  if (!props.cohostEnabled) {
    emit("require-login")
    return
  }
  if (!broadcasterSessionId) {
    $modal.msgWarning("主播信令尚未连接，稍后再申请连麦")
    return
  }
  if (!ensureCohostMediaAvailable()) {
    return
  }
  updateCohostState("requesting", "等待主播同意")
  sendSignal({
    type: "cohost-request",
    roomId: Number(props.roomId),
    targetSessionId: broadcasterSessionId,
    applicantName: props.applicantName,
    applicantAvatar: props.applicantAvatar,
  })
}

const endCohost = () => {
  if (broadcasterSessionId && cohostState.value !== "idle") {
    sendSignal({
      type: "cohost-ended",
      roomId: Number(props.roomId),
      targetSessionId: broadcasterSessionId,
    })
  }
  closeCohostPeer()
  releaseCohostStream()
  updateCohostState("idle", "")
}

const answerCohostOffer = async (data) => {
  if (!data?.fromSessionId || !data?.sdp) {
    return
  }
  broadcasterSessionId = data.fromSessionId
  updateCohostState("connecting", "主播已同意，正在接入")
  closeCohostPeer()
  const stream = cohostLocalStream || (await createCohostStream())
  cohostPeer = createPeerConnection()
  stream.getTracks().forEach((track) => cohostPeer.addTrack(track, stream))

  cohostPeer.onicecandidate = (event) => {
    if (!event.candidate) return
    sendSignal({
      type: "cohost-ice-candidate",
      roomId: Number(props.roomId),
      targetSessionId: broadcasterSessionId,
      candidate: event.candidate,
    })
  }

  cohostPeer.onconnectionstatechange = () => {
    if (cohostPeer?.connectionState === "connected") {
      updateCohostState("active", "连麦中")
    }
    if (["failed", "disconnected", "closed"].includes(cohostPeer?.connectionState)) {
      closeCohostPeer()
      releaseCohostStream()
      updateCohostState("idle", "")
    }
  }

  await cohostPeer.setRemoteDescription(new RTCSessionDescription(data.sdp))
  const answer = await cohostPeer.createAnswer()
  await cohostPeer.setLocalDescription(answer)
  sendSignal({
    type: "cohost-answer",
    roomId: Number(props.roomId),
    targetSessionId: broadcasterSessionId,
    sdp: answer,
  })
}

const resumePlayback = async () => {
  if (resumePlaybackPromise) {
    return resumePlaybackPromise
  }

  resumePlaybackPromise = doResumePlayback().finally(() => {
    resumePlaybackPromise = null
  })
  return resumePlaybackPromise
}

const doResumePlayback = async () => {
  const video = videoElementRef.value
  if (!video) {
    return
  }
  video.muted = false
  if (video.volume === 0) {
    video.volume = 1
  }
  syncVolumeState()
  try {
    await video.play()
    startLatencySync()
    playbackBlocked.value = false
    statusText.value = "正在播放直播声音"
  } catch (error) {
    playbackBlocked.value = true
    statusText.value = "浏览器仍然阻止声音播放，请再点一次开启声音"
  } finally {
    syncVolumeState()
  }
}

onMounted(() => {
  playLive()
  syncVolumeState()
})

watch(
  () => [props.pullUrl, props.browserLive, props.roomId],
  () => {
    playLive()
  }
)

onBeforeUnmount(() => {
  destroy()
})

defineExpose({ setVolume, toggleMute })

const handlePullPlaybackError = () => {
  pullPlaybackFailed = true
  if (props.browserLive && props.roomId) {
    connectBrowserLiveViewer()
    return true
  }
  return false
}

const playPullStream = () => {
  if (!playablePullUrl.value || pullPlaybackFailed) {
    return false
  }
  if (isRecordingUrl.value) {
    playRecording()
    return true
  }
  if (isHlsUrl.value) {
    playHls().catch(handlePullPlaybackError)
    return true
  }
  if (isFlvUrl.value) {
    playFlv().catch(handlePullPlaybackError)
    return true
  }
  return false
}

const playLive = () => {
  destroy()
  pullPlaybackFailed = false
  if (shouldPreferPullStream.value && playPullStream()) {
    return
  }
  if (props.roomId) {
    connectBrowserLiveViewer()
    return
  }
  if (!playPullStream()) {
    fallbackToPullStream()
  }
}

const connectBrowserLiveViewer = () => {
  if (!props.roomId || !videoElementRef.value) {
    return
  }

  statusText.value = "正在连接直播..."
  signalUrlIndex = 0
  browserLiveFallbackTimer = window.setTimeout(() => {
    if (!remoteStream) {
      fallbackToPullStream("直播连接较慢，正在重新加载画面...")
    }
  }, props.browserLive ? 4000 : 1200)
  connectNextSignal()
}

const connectNextSignal = () => {
  const urls = createBrowserLiveFallbackUrls()
  const signalUrl = urls[signalUrlIndex]
  if (!signalUrl) {
    fallbackToPullStream("直播连接暂不可用")
    return
  }

  closeSignalOnly()
  signalSocket = new WebSocket(signalUrl)
  signalSocket.onopen = () => {
    signalSocket.send(
      JSON.stringify({
        type: "join",
        role: "viewer",
        roomId: Number(props.roomId),
      })
    )
  }
  signalSocket.onmessage = async (event) => {
    const data = JSON.parse(event.data)
    await handleSignal(data)
  }
  signalSocket.onerror = () => {
    signalSocket?.close?.()
  }
  signalSocket.onclose = () => {
    stopHeartbeat()
    if (!remoteStream) {
      closeSignalOnly()
      signalUrlIndex += 1
      if (signalUrlIndex < urls.length) {
        connectNextSignal()
        return
      }
      fallbackToPullStream("直播连接已断开")
    }
  }
}

const handleSignal = async (data) => {
  if (data.type === "joined") {
    startHeartbeat()
    return
  }
  if (data.type === "heartbeat-ack") {
    return
  }
  if (data.type === "broadcaster-offline") {
    closePeer()
    fallbackToPullStream("主播暂未开播")
    return
  }
  if (data.type === "broadcaster-online") {
    broadcasterSessionId = data.sessionId
    statusText.value = "已连接到主播，正在等待直播画面..."
    return
  }
  if (data.type === "guard-violation") {
    const reason = formatGuardReason(data)
    statusText.value = reason
    $modal.msgError(reason)
    closeSignalOnly()
    closePeer()
    return
  }
  if (data.type === "offer" && data.fromSessionId && data.sdp) {
    clearFallbackTimer()
    broadcasterSessionId = data.fromSessionId
    await answerOffer(data.sdp)
    return
  }
  if (data.type === "ice-candidate" && data.candidate && peer) {
    await peer.addIceCandidate(new RTCIceCandidate(data.candidate))
    return
  }
  if (data.type === "cohost-accepted") {
    updateCohostState("connecting", "主播已同意，正在接入")
    return
  }
  if (data.type === "cohost-rejected") {
    $modal.msgWarning(data.reason || "主播暂时无法连麦")
    closeCohostPeer()
    releaseCohostStream()
    updateCohostState("idle", "")
    return
  }
  if (data.type === "cohost-offer") {
    try {
      await answerCohostOffer(data)
    } catch (error) {
      $modal.msgError("连麦接入失败，请检查摄像头和麦克风权限")
      endCohost()
    }
    return
  }
  if (data.type === "cohost-ice-candidate" && data.candidate && cohostPeer) {
    await cohostPeer.addIceCandidate(new RTCIceCandidate(data.candidate))
    return
  }
  if (data.type === "cohost-ended") {
    closeCohostPeer()
    releaseCohostStream()
    updateCohostState("idle", "")
    return
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
  return label ? `直播内容不符合平台规范：${label}，直播间已关闭` : "直播内容不符合平台规范，直播间已关闭"
}

const answerOffer = async (sdp) => {
  closePeer()
  peer = createPeerConnection()
  remoteStream = new MediaStream()
  if (videoElementRef.value) {
    videoElementRef.value.srcObject = remoteStream
    prepareAudiblePlayback()
    syncVolumeState()
  }

  peer.ontrack = (event) => {
    const incomingStream = event.streams?.[0]
    if (incomingStream) {
      remoteStream = incomingStream
      if (videoElementRef.value) {
        videoElementRef.value.srcObject = incomingStream
      }
    } else {
      const track = event.track
      if (track && !remoteStream.getTracks().some((item) => item.id === track.id)) {
        remoteStream.addTrack(track)
      }
      if (videoElementRef.value) {
        videoElementRef.value.srcObject = remoteStream
      }
    }
    const audioTrackCount = remoteStream.getAudioTracks().length
    statusText.value = `正在播放直播${audioTrackCount ? "，声音已接入" : "，等待声音"}`
    clearFallbackTimer()
    ensureVideoPlayback("正在播放直播")
    startPlaybackWatch()
  }

  peer.onicecandidate = (event) => {
    if (!event.candidate || !signalSocket || signalSocket.readyState !== WebSocket.OPEN) {
      return
    }
    signalSocket.send(
      JSON.stringify({
        type: "ice-candidate",
        roomId: Number(props.roomId),
        targetSessionId: broadcasterSessionId,
        candidate: event.candidate,
      })
    )
  }

  peer.onconnectionstatechange = () => {
    if (["failed", "disconnected", "closed"].includes(peer.connectionState)) {
      statusText.value = "直播连接已断开"
    }
  }

  await peer.setRemoteDescription(new RTCSessionDescription(sdp))
  const answer = await peer.createAnswer()
  await peer.setLocalDescription(answer)
  signalSocket.send(
    JSON.stringify({
      type: "answer",
      roomId: Number(props.roomId),
      targetSessionId: broadcasterSessionId,
      sdp: answer,
    })
  )
}

const playHls = async () => {
  const video = videoElementRef.value
  if (!video) {
    return
  }
  statusText.value = "正在加载直播画面"
  const pullUrl = playablePullUrl.value
  prepareAudiblePlayback()
  if (video.canPlayType("application/vnd.apple.mpegurl")) {
    video.src = pullUrl
    syncVolumeState()
    ensureVideoPlayback("正在播放直播")
    return
  }
  const Hls = await loadHls()
  if (!videoElementRef.value || videoElementRef.value !== video) {
    return
  }
  if (!Hls.isSupported()) {
    $modal.msgError("当前浏览器暂不支持播放直播")
    return
  }
  hlsPlayer.value = new Hls({
    enableWorker: true,
    lowLatencyMode: false,
    liveSyncDuration: LIVE_SYNC_LATENCY_SECONDS,
    liveMaxLatencyDuration: LIVE_SYNC_LATENCY_SECONDS + 1,
    maxLiveSyncPlaybackRate: 1,
  })
  hlsPlayer.value.loadSource(pullUrl)
  hlsPlayer.value.attachMedia(video)
  hlsPlayer.value.on(Hls.Events.MANIFEST_PARSED, () => {
    ensureVideoPlayback("正在播放直播")
  })
  hlsPlayer.value.on(Hls.Events.ERROR, () => {
    hlsPlayer.value?.destroy()
    hlsPlayer.value = null
    if (handlePullPlaybackError()) {
      return
    }
    statusText.value = "直播画面加载失败"
  })
}

const playFlv = async () => {
  const flvjs = await loadFlv()
  if (!videoElementRef.value) {
    return
  }
  if (!flvjs.isSupported()) {
    $modal.msgError("当前浏览器暂不支持播放直播")
    return
  }
  statusText.value = "正在加载直播画面"
  prepareAudiblePlayback()
  flvPlayer.value = flvjs.createPlayer(
    {
      type: "flv",
      url: playablePullUrl.value,
      isLive: true,
    },
    {
      enableWorker: true,
      enableStashBuffer: true,
      stashInitialSize: 1024 * 1024,
      lazyLoad: false,
      autoCleanupSourceBuffer: true,
    }
  )
  flvPlayer.value.attachMediaElement(videoElementRef.value)
  flvPlayer.value.on(flvjs.Events.ERROR, () => {
    flvPlayer.value?.destroy()
    flvPlayer.value = null
    if (handlePullPlaybackError()) {
      return
    }
    statusText.value = "直播画面加载失败"
  })
  flvPlayer.value.load()
  syncVolumeState()
  ensureVideoPlayback("正在播放直播")
}

const playRecording = () => {
  const video = videoElementRef.value
  const pullUrl = playablePullUrl.value
  if (!video || !pullUrl) {
    return
  }
  statusText.value = "正在加载录播画面"
  video.srcObject = null
  video.src = pullUrl
  video.loop = true
  video.controls = false
  prepareAudiblePlayback()
  video.playsInline = true
  video.oncanplay = () => {
    ensureVideoPlayback("正在播放演示录播")
  }
  video.onerror = () => {
    statusText.value = "录播画面加载失败"
  }
  syncVolumeState()
  video.load()
}

const fallbackToPullStream = (fallbackMessage = "") => {
  clearFallbackTimer()
  const pullUrl = playablePullUrl.value
  if (!pullUrl || !videoElementRef.value) {
    statusText.value = fallbackMessage || "暂时没有可观看的直播画面"
    return
  }
  if (pullPlaybackFailed) {
    statusText.value = fallbackMessage || "直播画面暂时无法加载"
    return
  }
  statusText.value = fallbackMessage || "正在重新加载直播画面..."
  closeSignalOnly()
  closePeer()
  if (isHlsUrl.value) {
    playHls()
    return
  }
  if (isFlvUrl.value) {
    playFlv()
    return
  }
  if (isRecordingUrl.value) {
    playRecording()
  }
}
const startHeartbeat = () => {
  stopHeartbeat()
  heartbeatTimer = window.setInterval(() => {
    if (signalSocket?.readyState === WebSocket.OPEN) {
      signalSocket.send(
        JSON.stringify({
          type: "heartbeat",
          roomId: Number(props.roomId),
        })
      )
    }
  }, HEARTBEAT_INTERVAL)
}

const stopHeartbeat = () => {
  if (heartbeatTimer) {
    window.clearInterval(heartbeatTimer)
    heartbeatTimer = null
  }
}

const clearFallbackTimer = () => {
  if (browserLiveFallbackTimer) {
    window.clearTimeout(browserLiveFallbackTimer)
    browserLiveFallbackTimer = null
  }
}

const closeSignalOnly = () => {
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

const closePeer = () => {
  stopPlaybackWatch()
  if (peer) {
    peer.close()
    peer = null
  }
  remoteStream = null
  if (videoElementRef.value) {
    videoElementRef.value.srcObject = null
  }
}

const closeCohostPeer = () => {
  if (cohostPeer) {
    cohostPeer.close()
    cohostPeer = null
  }
}

const destroy = () => {
  stopLatencySync()
  clearFallbackTimer()
  endCohost()
  if (signalSocket?.readyState === WebSocket.OPEN) {
    signalSocket.send(
      JSON.stringify({
        type: "leave",
        roomId: Number(props.roomId),
      })
    )
  }
  closeSignalOnly()
  closePeer()
  broadcasterSessionId = null
  if (hlsPlayer.value) {
    hlsPlayer.value.destroy()
    hlsPlayer.value = null
  }
  if (flvPlayer.value) {
    flvPlayer.value.pause()
    flvPlayer.value.unload()
    flvPlayer.value.detachMediaElement()
    flvPlayer.value.destroy()
    flvPlayer.value = null
  }
  if (videoElementRef.value) {
    videoElementRef.value.oncanplay = null
    videoElementRef.value.onerror = null
    videoElementRef.value.loop = false
    videoElementRef.value.removeAttribute("src")
    videoElementRef.value.load()
    videoElementRef.value.volume = 1
    videoElementRef.value.muted = false
    syncVolumeState()
  }
  playbackBlocked.value = false
}
</script>

<style scoped lang="scss">
.player-shell {
  position: relative;
  isolation: isolate;
  min-height: 552px;
  background: #02040a;
}

#videoElement {
  display: block;
  width: 100%;
  height: clamp(360px, 56vw, 620px);
  min-height: 552px;
  object-fit: cover;
  background:
    linear-gradient(135deg, color-mix(in srgb, var(--accent) 10%, transparent), transparent 34%),
    #02040a;
}

#videoElement.is-unmute-blocked {
  pointer-events: none;
}

.player-topbar {
  position: absolute;
  top: 14px;
  left: 14px;
  right: 14px;
  display: flex;
  justify-content: space-between;
  gap: 12px;
  pointer-events: none;
  z-index: 5;
}

.status-chip,
.cohost-btn {
  pointer-events: auto;
}

.status-chip {
  display: inline-flex;
  align-items: center;
  min-height: 28px;
  padding: 0 10px;
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
  background: rgba(5, 6, 9, 0.64);
  backdrop-filter: blur(10px);
  color: #fff;
  font-size: 12px;
  font-weight: 800;
}

.player-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  pointer-events: auto;
}

.cohost-btn {
  border: 1px solid rgba(255, 159, 26, 0.45);
  border-radius: 4px;
  color: #fff;
  background: rgba(255, 159, 26, 0.26);
  backdrop-filter: blur(10px);
}

.cohost-self-card {
  position: absolute;
  right: 14px;
  bottom: 82px;
  z-index: 8;
  width: min(230px, 34%);
  min-width: 172px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.14);
  border-radius: 8px;
  background: rgba(5, 6, 9, 0.82);
  box-shadow: 0 14px 28px rgba(0, 0, 0, 0.28);
}

.cohost-self-card video {
  display: block;
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #050609;
  object-fit: cover;
}

.cohost-self-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  padding: 8px 9px;
  color: #fff;
  background: rgba(5, 6, 9, 0.72);
}

.cohost-self-card__head span {
  overflow: hidden;
  font-size: 12px;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.cohost-self-card__head button {
  flex: 0 0 auto;
  padding: 0;
  border: 0;
  color: #ffb4b4;
  background: transparent;
  font-size: 12px;
  cursor: pointer;
}

.playback-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(2, 6, 23, 0.34);
  pointer-events: auto;
  z-index: 20;
  cursor: pointer;
}

.playback-unmute-btn {
  appearance: none;
  border: 0;
  border-radius: 20px;
  background: var(--accent);
  color: #fff;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  line-height: 1;
  padding: 12px 24px;
  pointer-events: auto;
}

.playback-unmute-btn:hover {
  background: var(--accent-strong);
}

.playback-unmute-btn:active {
  background: var(--accent-strong);
}

video::-webkit-media-controls-timeline,
video::-webkit-media-controls-current-time-display,
video::-webkit-media-controls-play-button {
  display: none;
}

@media (max-width: 768px) {
  .player-shell,
  #videoElement {
    min-height: 360px;
  }

}
</style>
