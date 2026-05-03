<template>
  <section class="browser-live-panel">
    <div class="panel-header">
      <div>
        <h3>网页一键开播</h3>
        <p>直接在浏览器里采集屏幕或摄像头，观众优先通过 WebRTC 观看，延迟更低。</p>
      </div>
      <span class="status" :class="{ active: state.liveActive }">
        {{ state.liveActive ? "直播中" : "未开播" }}
      </span>
    </div>

    <div class="capability-grid">
      <article class="capability-card">
        <div class="capability-head">
          <div>
            <h4>实时字幕</h4>
            <p>识别主播说话内容，并同步展示给观众。</p>
          </div>
          <a-button
            :disabled="!state.liveActive || !state.captionSupported"
            :type="state.captionActive ? 'default' : 'dashed'"
            @click="toggleCaption"
          >
            {{ state.captionActive ? "关闭字幕" : "字幕" }}
          </a-button>
        </div>
        <span class="capability-tip" :class="{ warning: !state.captionSupported }">
          {{
            !state.captionSupported
              ? "当前浏览器不支持实时字幕，建议使用最新版 Chrome 或 Edge。"
              : state.captionActive
                ? "字幕识别中"
                : "未开启"
          }}
        </span>
      </article>

      <article class="capability-card">
        <div class="capability-head">
          <div>
            <h4>实时降噪</h4>
            <p>启用后会连接本地 DeepFilterNet3 降噪服务，浏览器内置降噪链路不会参与处理。</p>
          </div>
          <a-switch
            :checked="state.denoiseEnabled"
            :disabled="state.starting || state.denoiseSwitching"
            :loading="state.denoiseSwitching"
            checked-children="开"
            un-checked-children="关"
            @change="handleDenoiseSwitch"
          />
        </div>
        <span class="capability-tip" :class="{ warning: state.denoiseStatus === 'fallback' || state.denoiseStatus === 'error' }">
          {{ denoiseSummary }}
        </span>
      </article>
    </div>

    <div class="actions">
      <a-button type="primary" :loading="state.starting" @click="startScreenLive">共享屏幕开播</a-button>
      <a-button :loading="state.starting" @click="startCameraLive">摄像头开播</a-button>
      <a-button danger :disabled="!state.liveActive" @click="stopBrowserLive">停止直播</a-button>
    </div>

    <div class="tips">
      <span>房间号：{{ roomId || "--" }}</span>
      <span>当前观众连接：{{ state.viewerCount }}</span>
      <span>信令状态：{{ state.signalConnected ? "已连接" : "未连接" }}</span>
      <span>违规检测：{{ state.guardActive ? "运行中" : "未开启" }}</span>
      <span v-if="state.message">{{ state.message }}</span>
    </div>

    <div class="preview-card">
      <video ref="previewRef" autoplay muted playsinline controls></video>
      <div v-if="state.subtitleText" class="subtitle-preview">{{ state.subtitleText }}</div>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref } from "vue"
import { message } from "ant-design-vue"
import liveAPI from "@/api/live"
import { createBrowserLiveFallbackUrls, createPeerConnection } from "@/utils/browserLive"
import { createLiveCaptionEngine, isLiveCaptionSupported } from "@/utils/liveCaption"
import { createLiveDenoiseEngine, getLiveDenoiseServiceUrl } from "@/utils/liveDenoise"
import { createAlignedLatencyStream } from "@/utils/mediaLatency"

const HEARTBEAT_INTERVAL = 15000
const GUARD_CHECK_INTERVAL = 2000
const DENOISE_STORAGE_KEY = "live.browser.denoise.enabled"
const LIVE_SYNC_LATENCY_MS = 1000

const props = defineProps({
  roomId: {
    type: Number,
    default: null,
  },
  liveStatus: {
    type: Number,
    default: 0,
  },
})

const emit = defineEmits(["status-change"])
const previewRef = ref(null)

const state = reactive({
  starting: false,
  liveActive: false,
  message: "",
  viewerCount: 0,
  signalConnected: false,
  captionSupported: isLiveCaptionSupported(),
  captionActive: false,
  subtitleText: "",
  denoiseEnabled: window.localStorage.getItem(DENOISE_STORAGE_KEY) === "1",
  denoiseStatus: "idle",
  denoiseDetail: "",
  denoiseSwitching: false,
  denoiseUsingEnhanced: false,
  denoiseBackend: "",
  denoiseModelName: "",
  denoiseRuntimeInfo: "",
  guardActive: false,
})

let signalSocket = null
let captureStream = null
let publishingStream = null
let heartbeatTimer = null
let guardTimer = null
let guardChecking = false
let captionEngine = null
let denoiseEngine = null
let latencyAlignedStream = null
let auxiliaryStreams = []
const peerMap = new Map()

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
  state.denoiseDetail = `已启用。开播时会连接本地 DeepFilterNet3 服务：${getLiveDenoiseServiceUrl()}`
  state.denoiseBackend = "deepfilternet3"
  state.denoiseModelName = "DeepFilterNet3"
  state.denoiseUsingEnhanced = false
  state.denoiseRuntimeInfo = ""
}

const denoiseSummary = computed(() => {
  if (!state.denoiseEnabled) {
    return "未启用。开播时将直接使用原始麦克风音频。"
  }
  if (!state.liveActive) {
    return `已启用。开播时会连接本地 DeepFilterNet3 服务：${getLiveDenoiseServiceUrl()}`
  }
  if (state.denoiseStatus === "active" && state.denoiseUsingEnhanced) {
    return [state.denoiseDetail || "DeepFilterNet3 正在输出增强音频。", state.denoiseRuntimeInfo]
      .filter(Boolean)
      .join(" ")
  }
  if (state.denoiseStatus === "warming") {
    return state.denoiseDetail || "本地 DeepFilterNet3 已连接，正在等待增强音频输出。"
  }
  if (state.denoiseDetail) {
    return state.denoiseDetail
  }
  return "实时降噪运行中"
})

const setMessage = (text) => {
  state.message = text
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
  echoCancellation: false,
  noiseSuppression: false,
  autoGainControl: false,
})

const describeAudioProcessingState = (stream = captureStream) => {
  const [audioTrack] = stream?.getAudioTracks?.() || []
  if (!audioTrack) {
    state.denoiseRuntimeInfo = "未检测到麦克风音轨。"
    return
  }

  const settings = typeof audioTrack.getSettings === "function" ? audioTrack.getSettings() : {}
  const lines = [
    audioTrack.label ? `当前麦克风:${audioTrack.label}` : "",
    settings.sampleRate ? `采样率:${settings.sampleRate}` : "",
    state.denoiseEnabled ? "浏览器原生降噪已关闭，由本地 DeepFilterNet3 处理。" : "",
  ].filter(Boolean)

  state.denoiseRuntimeInfo = lines.join("，")
}

const createScreenStream = async () => {
  const displayStream = await navigator.mediaDevices.getDisplayMedia({
    video: true,
    audio: true,
  })

  let microphoneStream = null
  try {
    microphoneStream = await navigator.mediaDevices.getUserMedia({
      audio: createMicrophoneConstraints(),
    })
  } catch (error) {
    console.warn("microphone is unavailable during screen live", error)
  }

  auxiliaryStreams = [displayStream]
  if (microphoneStream) {
    auxiliaryStreams.push(microphoneStream)
  }

  const videoTracks = displayStream.getVideoTracks()
  const microphoneTracks = microphoneStream?.getAudioTracks?.() || []
  const displayAudioTracks = displayStream.getAudioTracks()
  const audioTracks = microphoneTracks.length ? microphoneTracks : displayAudioTracks

  return new MediaStream([...videoTracks, ...audioTracks])
}

const startScreenLive = async () => {
  await startBrowserLive(() => createScreenStream())
}

const startCameraLive = async () => {
  await startBrowserLive(() =>
    navigator.mediaDevices.getUserMedia({
      video: true,
      audio: createMicrophoneConstraints(),
    })
  )
}

const startBrowserLive = async (streamFactory) => {
  if (!props.roomId) {
    message.error("房间信息未初始化完成，暂时无法开播")
    return
  }

  state.starting = true
  setMessage("")
  state.denoiseStatus = state.denoiseEnabled ? "connecting" : "idle"
  state.denoiseDetail = state.denoiseEnabled ? "正在连接本地 DeepFilterNet3 服务..." : ""

  try {
    await ensureRoomActive()
    captureStream = await streamFactory()
    publishingStream = await buildPublishingStream(captureStream)
    attachPreview()
    await connectSignal()
    state.liveActive = true
    startGuardLoop()
    setMessage("网页直播已启动，观众正在通过低延迟通道进入房间")
    emit("status-change")
  } catch (error) {
    closeAllPeers()
    closeSignal()
    await releaseMediaResources()
    stopCaption()
    const errorMessage = getMediaErrorMessage(error)
    setMessage(errorMessage)
    message.error(errorMessage)
  } finally {
    state.starting = false
  }
}

const buildPublishingStream = async (stream) => {
  let baseStream = stream
  if (!state.denoiseEnabled) {
    resetDenoiseState()
    describeAudioProcessingState(stream)
    return alignPublishingLatency(stream)
  }

  denoiseEngine = createLiveDenoiseEngine({
    onStateChange: ({ status, detail, useEnhancedOutput }) => {
      state.denoiseStatus = status
      state.denoiseDetail = detail
      state.denoiseUsingEnhanced = Boolean(useEnhancedOutput)
      if (detail) {
        setMessage(detail)
      }
    },
  })

  try {
    const result = await denoiseEngine.start(stream)
    state.denoiseStatus = "warming"
    state.denoiseBackend = result.backend || ""
    state.denoiseModelName = result.modelName || ""
    state.denoiseDetail = `DeepFilterNet3 已接入，后端：${result.backend || "unknown"}，模型：${result.modelName || "unknown"}`
    state.denoiseUsingEnhanced = false
    baseStream = result.stream
    describeAudioProcessingState(baseStream)
    return alignPublishingLatency(baseStream)
  } catch (error) {
    state.denoiseStatus = "error"
    state.denoiseDetail = `DeepFilterNet3 服务不可用，已回退原始音频：${error.message}`
    state.denoiseBackend = ""
    state.denoiseModelName = ""
    state.denoiseUsingEnhanced = false
    state.denoiseRuntimeInfo = ""
    message.warning(state.denoiseDetail)
    await denoiseEngine.stop()
    denoiseEngine = null
    return alignPublishingLatency(stream)
  }
}

const alignPublishingLatency = async (stream) => {
  await stopLatencyAlignment({ stopDerivedTracks: true, protectedStreams: [captureStream, stream] })
  latencyAlignedStream = await createAlignedLatencyStream(stream, LIVE_SYNC_LATENCY_MS)
  setMessage(`音视频已统一延迟为 ${LIVE_SYNC_LATENCY_MS / 1000}s`)
  return latencyAlignedStream
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
  stream?.getTracks?.().forEach((track) => {
    if (!protectedTrackIds.has(track.id)) {
      track.stop()
    }
  })
}

const stopLatencyAlignment = async ({ stopDerivedTracks = false, protectedStreams = [] } = {}) => {
  if (!latencyAlignedStream) {
    return
  }
  const stream = latencyAlignedStream
  latencyAlignedStream = null
  await stream.stopLatencyAlignment?.()
  if (stopDerivedTracks) {
    stopUnprotectedTracks(stream, protectedStreams)
  }
}

const replacePeerTrack = async (kind, nextTrack) => {
  const peers = Array.from(peerMap.values())
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
  state.denoiseDetail = "正在连接本地 DeepFilterNet3 服务..."

  denoiseEngine = createLiveDenoiseEngine({
    onStateChange: ({ status, detail, useEnhancedOutput }) => {
      state.denoiseStatus = status
      state.denoiseDetail = detail
      state.denoiseUsingEnhanced = Boolean(useEnhancedOutput)
      if (detail) {
        setMessage(detail)
      }
    },
  })

  try {
    const result = await denoiseEngine.start(captureStream)
    const alignedStream = await alignPublishingLatency(result.stream)
    await switchPublishingStream(alignedStream)
    state.denoiseStatus = "warming"
    state.denoiseBackend = result.backend || ""
    state.denoiseModelName = result.modelName || ""
    state.denoiseDetail = `DeepFilterNet3 已开启，后端：${result.backend || "unknown"}，模型：${result.modelName || "unknown"}`
    state.denoiseUsingEnhanced = false
    describeAudioProcessingState(result.stream)
    setMessage("DeepFilterNet3 降噪已开启，直播不中断")
  } catch (error) {
    state.denoiseStatus = "error"
    state.denoiseDetail = `DeepFilterNet3 服务不可用，已保持原始音频：${error.message}`
    state.denoiseBackend = ""
    state.denoiseModelName = ""
    state.denoiseUsingEnhanced = false
    state.denoiseRuntimeInfo = ""
    message.warning(state.denoiseDetail)
    if (denoiseEngine) {
      await denoiseEngine.stop()
      denoiseEngine = null
    }
    state.denoiseEnabled = false
    window.localStorage.setItem(DENOISE_STORAGE_KEY, "0")
  }
}

const disableDenoiseDuringLive = async () => {
  const alignedStream = await alignPublishingLatency(captureStream)
  await switchPublishingStream(alignedStream)
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
    return "浏览器拦截了媒体采集，请使用 localhost、127.0.0.1 或 HTTPS 地址访问。"
  }
  return rawMessage || "网页开播失败，请稍后重试"
}

const attachPreview = () => {
  if (previewRef.value) {
    previewRef.value.srcObject = publishingStream
    previewRef.value.muted = true
    previewRef.value.playsInline = true
    previewRef.value.play?.().catch(() => {})
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
        reject(new Error(state.message || "直播信令服务暂不可用"))
        return
      }

      setMessage(`正在连接直播信令：${signalUrl}`)
      signalSocket = new WebSocket(signalUrl)

      signalSocket.onopen = () => {
        sendSignal({
          type: "join",
          role: "broadcaster",
          roomId: props.roomId,
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
          setMessage("直播信令连接已断开")
          closeAllPeers()
          stopCaption()
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
  if (data.type === "heartbeat-ack") {
    return
  }
  if (data.type === "subtitle") {
    state.subtitleText = data.text || ""
    return
  }
  if (data.type === "subtitle-clear") {
    state.subtitleText = ""
    return
  }
  if (data.type === "guard-violation") {
    await forceStopByGuard(formatGuardReason(data))
    return
  }
  if (data.type === "error") {
    setMessage(data.message || "直播信令服务返回异常")
    message.error(state.message)
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
      roomId: props.roomId,
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
    roomId: props.roomId,
    targetSessionId: viewerSessionId,
    sdp: offer,
  })
}

const toggleCaption = async () => {
  if (!state.captionSupported) {
    message.warning("当前浏览器不支持实时字幕，请使用最新版 Chrome 或 Edge")
    return
  }
  if (!state.liveActive) {
    message.warning("请先开播，再开启实时字幕")
    return
  }
  if (!captureStream?.getAudioTracks?.().some((track) => track.readyState === "live")) {
    message.warning("未检测到可用麦克风音轨，请重新开启摄像头直播")
    return
  }
  if (state.captionActive) {
    stopCaption()
    setMessage("实时字幕已关闭")
    return
  }

  captionEngine = createLiveCaptionEngine({
    onText: (text) => {
      state.subtitleText = text
      sendSignal({
        type: text ? "subtitle" : "subtitle-clear",
        roomId: props.roomId,
        text,
      })
    },
    onError: (event, options = {}) => {
      const error = event?.error || event?.message || ""
      if (options.recoverable) {
        setMessage(error === "no-speech" ? "实时字幕等待主播说话..." : "实时字幕正在自动恢复")
        return
      }
      state.captionActive = false
      captionEngine = null
      const hint =
        error === "not-allowed" || error === "service-not-allowed"
          ? "字幕识别没有麦克风权限，请在浏览器地址栏允许麦克风后重试"
          : error === "audio-capture"
            ? "字幕识别没有可用麦克风，请检查设备后重试"
            : error === "network"
              ? "字幕识别服务连接失败，请稍后重试"
              : "字幕识别已中断，请检查麦克风权限后重试"
      message.warning(hint)
      setMessage(hint)
    },
  })

  if (!captionEngine) {
    message.warning("当前浏览器不支持实时字幕，请使用最新版 Chrome 或 Edge")
    return
  }

  try {
    captionEngine.start()
    state.captionActive = true
    setMessage("实时字幕识别已开启")
  } catch (error) {
    const hint = "实时字幕启动失败，请检查麦克风权限"
    message.error(hint)
    setMessage(hint)
  }
}

const stopCaption = () => {
  if (captionEngine) {
    captionEngine.stop()
    captionEngine = null
  }
  if (state.captionActive || state.subtitleText) {
    sendSignal({
      type: "subtitle-clear",
      roomId: props.roomId,
    })
  }
  state.captionActive = false
  state.subtitleText = ""
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

const startHeartbeat = () => {
  stopHeartbeat()
  heartbeatTimer = window.setInterval(() => {
    sendSignal({
      type: "heartbeat",
      roomId: props.roomId,
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
  if (!state.liveActive || guardChecking || !props.roomId) {
    return
  }
  guardChecking = true
  try {
    const frame = await captureGuardFrame()
    if (!frame) {
      return
    }
    const response = await liveAPI.checkGuardFrame(props.roomId, frame)
    const result = response?.data || {}
    if (result.banned) {
      await forceStopByGuard(formatGuardReason(result))
    }
  } catch (error) {
    console.warn("live guard check failed", error)
  } finally {
    guardChecking = false
  }
}

const forceStopByGuard = async (reason) => {
  message.error(reason)
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
    ? `直播内容触发违规检测：${label}，直播间已封停`
    : "直播内容触发违规检测，直播间已封停"
}

const sendSignal = (payload) => {
  if (signalSocket?.readyState === WebSocket.OPEN) {
    signalSocket.send(JSON.stringify(payload))
  }
}

const stopBrowserLive = async (options = {}) => {
  state.liveActive = false
  state.signalConnected = false
  stopGuardLoop()
  const stopMessage = options.guardReason || null
  setMessage("直播已停止")
  if (stopMessage) {
    setMessage(stopMessage)
  }
  stopCaption()
  sendSignal({
    type: "leave",
    roomId: props.roomId,
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

  await stopLatencyAlignment({ stopDerivedTracks: true, protectedStreams: [captureStream] })

  if (publishingStream) {
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

onBeforeUnmount(async () => {
  stopGuardLoop()
  stopCaption()
  closeSignal()
  closeAllPeers()
  await releaseMediaResources()
})
</script>

<style scoped lang="scss">
.browser-live-panel {
  background: #fff;
  border-radius: 18px;
  padding: 24px;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.06);
}

.panel-header,
.actions,
.tips,
.capability-head {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
  justify-content: space-between;
}

.panel-header h3,
.capability-head h4 {
  margin: 0;
}

.panel-header p,
.capability-head p {
  margin: 8px 0 0;
  color: #64748b;
}

.status {
  padding: 8px 14px;
  border-radius: 999px;
  background: #e2e8f0;
  color: #475569;
  font-weight: 700;
}

.status.active {
  background: #dcfce7;
  color: #166534;
}

.capability-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin-top: 20px;
}

.capability-card {
  padding: 18px;
  border-radius: 16px;
  background: linear-gradient(180deg, #f8fafc 0%, #eff6ff 100%);
  border: 1px solid #dbeafe;
}

.capability-tip {
  display: block;
  margin-top: 12px;
  color: #475569;
  line-height: 1.7;
}

.capability-tip.warning {
  color: #b45309;
}

.actions {
  margin-top: 20px;
  justify-content: flex-start;
}

.tips {
  margin-top: 16px;
  color: #64748b;
  justify-content: flex-start;
}

.preview-card {
  position: relative;
  margin-top: 18px;
  overflow: hidden;
  border-radius: 16px;
  background: #020617;
}

.preview-card video {
  width: 100%;
  min-height: 420px;
  background: #020617;
}

.subtitle-preview {
  position: absolute;
  left: 50%;
  bottom: 24px;
  transform: translateX(-50%);
  max-width: calc(100% - 48px);
  padding: 10px 14px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.84);
  color: #f8fafc;
  font-size: 15px;
  line-height: 1.5;
  text-align: center;
}

@media (max-width: 960px) {
  .capability-grid {
    grid-template-columns: 1fr;
  }

  .preview-card video {
    min-height: 280px;
  }
}
</style>
