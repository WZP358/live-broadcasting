<template>
  <div class="player-shell">
    <video ref="videoElementRef" id="videoElement" :class="{ 'is-unmute-blocked': playbackBlocked }" autoplay playsinline></video>
    <div class="player-topbar">
      <div v-if="statusText" class="status-chip">{{ statusText }}</div>
      <a-button size="small" class="caption-btn" @click="subtitleVisible = !subtitleVisible">
        {{ subtitleVisible ? "隐藏字幕" : "字幕" }}
      </a-button>
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
    <div class="player-volume">
      <a-button size="small" class="volume-btn" @click="toggleMute">
        <template #icon>
          <AudioMutedOutlined v-if="volumeMuted || volumeValue === 0" />
          <SoundOutlined v-else />
        </template>
      </a-button>
      <a-slider :min="0" :max="100" :value="volumeValue" class="volume-slider" @change="handleVolumeChange" />
    </div>
    <div v-if="subtitleVisible && subtitleText" class="subtitle-overlay">{{ subtitleText }}</div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from "vue"
import { AudioMutedOutlined, SoundOutlined } from "@ant-design/icons-vue"
import { message } from "ant-design-vue"
import Hls from "hls.js"
import flvjs from "flv.js"
import { createBrowserLiveFallbackUrls, createPeerConnection } from "@/utils/browserLive"

const HEARTBEAT_INTERVAL = 15000
const LIVE_SYNC_LATENCY_SECONDS = 1
const LIVE_SYNC_DRIFT_TOLERANCE_SECONDS = 0.25
const STALL_CHECK_INTERVAL = 2000
const STALL_RECONNECT_THRESHOLD = 3

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
})

const flvPlayer = ref(null)
const hlsPlayer = ref(null)
const videoElementRef = ref(null)
const statusText = ref("")
const subtitleText = ref("")
const subtitleVisible = ref(true)
const volumeValue = ref(100)
const volumeMuted = ref(false)
const playbackBlocked = ref(false)

let browserLiveFallbackTimer = null
let signalSocket = null
let peer = null
let remoteStream = null
let broadcasterSessionId = null
let signalUrlIndex = 0
let heartbeatTimer = null
let latencySyncTimer = null
let playbackWatchTimer = null
let lastVideoTime = 0
let stalledTicks = 0
let resumePlaybackPromise = null

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
    await ensureVideoPlayback("正在播放网页直播")
    if (stalledTicks >= STALL_RECONNECT_THRESHOLD) {
      stalledTicks = 0
      statusText.value = "网页直播画面卡住，正在重新连接..."
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

const ensureVideoPlayback = async (hintText = "正在播放网页直播") => {
  const video = videoElementRef.value
  if (!video) {
    return
  }

  try {
    await video.play()
    startLatencySync()
    playbackBlocked.value = false
    statusText.value = hintText
  } catch (error) {
    video.muted = true
    try {
      await video.play()
      startLatencySync()
      playbackBlocked.value = true
      statusText.value = "正在播放网页直播，点击开启声音"
    } catch (mutedError) {
      playbackBlocked.value = true
      statusText.value = "已收到音视频流，请点击播放器开始播放"
    }
  }
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
  const normalized = Number(nextValue) / 100
  video.muted = normalized === 0 ? true : false
  video.volume = normalized
  syncVolumeState()
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

const playLive = () => {
  destroy()
  if (props.roomId) {
    connectBrowserLiveViewer()
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
      fallbackToPullStream("网页直播暂未连上，正在切换到拉流播放...")
    }
  }, props.browserLive ? 4000 : 1200)
  connectNextSignal()
}

const connectNextSignal = () => {
  const urls = createBrowserLiveFallbackUrls()
  const signalUrl = urls[signalUrlIndex]
  if (!signalUrl) {
    fallbackToPullStream("直播信令服务不可用")
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
      fallbackToPullStream("直播信令连接已断开")
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
    subtitleText.value = ""
    closePeer()
    fallbackToPullStream("主播暂未开播")
    return
  }
  if (data.type === "broadcaster-online") {
    broadcasterSessionId = data.sessionId
    statusText.value = "已连接到主播，等待音视频流..."
    return
  }
  if (data.type === "guard-violation") {
    const reason = formatGuardReason(data)
    subtitleText.value = ""
    statusText.value = reason
    message.error(reason)
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
  if (data.type === "subtitle") {
    subtitleText.value = data.text || ""
    return
  }
  if (data.type === "subtitle-clear") {
    subtitleText.value = ""
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
  return label ? `直播内容触发违规检测：${label}，直播间已封停` : "直播内容触发违规检测，直播间已封停"
}

const answerOffer = async (sdp) => {
  closePeer()
  peer = createPeerConnection()
  remoteStream = new MediaStream()
  if (videoElementRef.value) {
    videoElementRef.value.srcObject = remoteStream
    videoElementRef.value.muted = true
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
    statusText.value = `正在播放网页直播${audioTrackCount ? "，音频已接入" : "，等待音频"}`
    clearFallbackTimer()
    ensureVideoPlayback("正在播放网页直播")
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

const playHls = () => {
  const video = videoElementRef.value
  if (!video) {
    return
  }
  statusText.value = "正在播放 HLS 直播流"
  if (video.canPlayType("application/vnd.apple.mpegurl")) {
    video.src = props.pullUrl
    syncVolumeState()
    ensureVideoPlayback("正在播放 HLS 直播流")
    return
  }
  if (!Hls.isSupported()) {
    message.error("当前浏览器不支持 HLS 播放")
    return
  }
  hlsPlayer.value = new Hls({
    enableWorker: true,
    lowLatencyMode: false,
    liveSyncDuration: LIVE_SYNC_LATENCY_SECONDS,
    liveMaxLatencyDuration: LIVE_SYNC_LATENCY_SECONDS + 1,
    maxLiveSyncPlaybackRate: 1,
  })
  hlsPlayer.value.loadSource(props.pullUrl)
  hlsPlayer.value.attachMedia(video)
  hlsPlayer.value.on(Hls.Events.MANIFEST_PARSED, () => {
    ensureVideoPlayback("正在播放 HLS 直播流")
  })
  hlsPlayer.value.on(Hls.Events.ERROR, () => {
    statusText.value = "HLS 流加载失败"
  })
}

const playFlv = () => {
  if (!flvjs.isSupported()) {
    message.error("当前浏览器不支持 FLV 播放")
    return
  }
  statusText.value = "正在播放 FLV 直播流"
  flvPlayer.value = flvjs.createPlayer(
    {
      type: "flv",
      url: props.pullUrl,
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
  flvPlayer.value.load()
  syncVolumeState()
  ensureVideoPlayback("正在播放 FLV 直播流")
}

const fallbackToPullStream = (fallbackMessage = "") => {
  clearFallbackTimer()
  subtitleText.value = ""
  if (!props.pullUrl || !videoElementRef.value) {
    statusText.value = fallbackMessage || "暂时没有可播放的直播流"
    return
  }
  statusText.value = fallbackMessage || "正在切换到拉流播放..."
  closeSignalOnly()
  closePeer()
  if (props.pullUrl.endsWith(".m3u8")) {
    playHls()
    return
  }
  if (props.pullUrl.endsWith(".flv")) {
    playFlv()
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

const destroy = () => {
  stopLatencySync()
  clearFallbackTimer()
  subtitleText.value = ""
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
}

#videoElement {
  display: block;
  width: 100%;
  height: 510px;
  object-fit: cover;
  background: #020617;
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
.caption-btn {
  pointer-events: auto;
}

.status-chip {
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.7);
  color: #fff;
  font-size: 12px;
}

.subtitle-overlay {
  position: absolute;
  left: 50%;
  bottom: 18px;
  transform: translateX(-50%);
  max-width: calc(100% - 60px);
  padding: 10px 18px;
  border-radius: 14px;
  background: rgba(15, 23, 42, 0.76);
  color: #fff;
  font-size: 18px;
  line-height: 1.6;
  text-align: center;
}

.playback-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(2, 6, 23, 0.28);
  pointer-events: auto;
  z-index: 20;
  cursor: pointer;
}

.playback-unmute-btn {
  appearance: none;
  border: 0;
  border-radius: 6px;
  background: #1677ff;
  color: #fff;
  cursor: pointer;
  font-size: 16px;
  font-weight: 600;
  line-height: 1;
  padding: 12px 24px;
  pointer-events: auto;
}

.playback-unmute-btn:hover {
  background: #4096ff;
}

.playback-unmute-btn:active {
  background: #0958d9;
}

.player-volume {
  position: absolute;
  right: 14px;
  bottom: 18px;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.78);
  pointer-events: auto;
  z-index: 6;
}

.volume-btn {
  flex: 0 0 auto;
}

.volume-slider {
  width: 120px;
  margin: 0;
}

.player-volume :deep(.ant-slider) {
  margin: 0;
}

.player-volume :deep(.ant-slider-rail) {
  background: rgba(255, 255, 255, 0.24);
}

.player-volume :deep(.ant-slider-track) {
  background: #60a5fa;
}

.player-volume :deep(.ant-slider-handle::after) {
  box-shadow: 0 0 0 2px #60a5fa;
}

video::-webkit-media-controls-timeline,
video::-webkit-media-controls-current-time-display,
video::-webkit-media-controls-play-button {
  display: none;
}

@media (max-width: 768px) {
  .player-volume {
    left: 14px;
    right: 14px;
    bottom: 14px;
  }

  .volume-slider {
    width: 100%;
  }
}
</style>


