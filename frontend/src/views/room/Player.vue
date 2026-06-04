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
    <div v-if="subtitleVisible && subtitleText" class="subtitle-overlay">{{ subtitleText }}</div>
  </div>
</template>

<script setup>
import { onBeforeUnmount, onMounted, ref, watch } from "vue"
import $modal from "@/utils/message"
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
    playbackBlocked.value = false
    statusText.value = hintText
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
    subtitleText.value = ""
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
    subtitleText.value = ""
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
  return label ? `直播内容不符合平台规范：${label}，直播间已关闭` : "直播内容不符合平台规范，直播间已关闭"
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

const playHls = () => {
  const video = videoElementRef.value
  if (!video) {
    return
  }
  statusText.value = "正在加载直播画面"
  if (video.canPlayType("application/vnd.apple.mpegurl")) {
    video.src = props.pullUrl
    syncVolumeState()
    ensureVideoPlayback("正在播放直播")
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
  hlsPlayer.value.loadSource(props.pullUrl)
  hlsPlayer.value.attachMedia(video)
  hlsPlayer.value.on(Hls.Events.MANIFEST_PARSED, () => {
    ensureVideoPlayback("正在播放直播")
  })
  hlsPlayer.value.on(Hls.Events.ERROR, () => {
    statusText.value = "直播画面加载失败"
  })
}

const playFlv = () => {
  if (!flvjs.isSupported()) {
    $modal.msgError("当前浏览器暂不支持播放直播")
    return
  }
  statusText.value = "正在加载直播画面"
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
  ensureVideoPlayback("正在播放直播")
}

const fallbackToPullStream = (fallbackMessage = "") => {
  clearFallbackTimer()
  subtitleText.value = ""
  if (!props.pullUrl || !videoElementRef.value) {
    statusText.value = fallbackMessage || "暂时没有可观看的直播画面"
    return
  }
  statusText.value = fallbackMessage || "正在重新加载直播画面..."
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
    linear-gradient(135deg, rgba(255, 153, 0, 0.09), transparent 34%),
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
.caption-btn {
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

.caption-btn {
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 4px;
  color: #fff;
  background: rgba(5, 6, 9, 0.52);
  backdrop-filter: blur(10px);
}

.subtitle-overlay {
  position: absolute;
  left: 50%;
  bottom: 18px;
  transform: translateX(-50%);
  max-width: calc(100% - 60px);
  padding: 10px 18px;
  border-radius: 4px;
  background: rgba(5, 6, 9, 0.76);
  backdrop-filter: blur(10px);
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
