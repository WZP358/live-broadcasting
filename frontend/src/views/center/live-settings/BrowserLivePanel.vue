<template>
  <section class="browser-live-panel">
    <header class="studio-topbar">
      <div class="studio-brand">
        <span class="studio-logo">PL</span>
        <div>
          <h3>开播工作台</h3>
          <span>房间 {{ roomId || "--" }}</span>
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
          <button class="active" type="button">直播源</button>
          <button type="button">工具</button>
        </div>

        <section class="studio-panel source-panel">
          <h4>开播方式</h4>
          <button
            class="source-item"
            :class="{ selected: state.liveActive && state.isScreenSharing }"
            type="button"
            :disabled="state.starting"
            @click="startScreenLive"
          >
            <span class="source-icon">屏</span>
            <span>
              <strong>屏幕直播</strong>
              <small>{{ state.liveActive && state.isScreenSharing ? "正在使用" : "共享屏幕" }}</small>
            </span>
          </button>
          <button
            class="source-item"
            :class="{ selected: state.liveActive && !state.isScreenSharing }"
            type="button"
            :disabled="state.starting"
            @click="startCameraLive"
          >
            <span class="source-icon">摄</span>
            <span>
              <strong>摄像头直播</strong>
              <small>{{ state.liveActive && !state.isScreenSharing ? "正在使用" : "使用摄像头" }}</small>
            </span>
          </button>
        </section>

        <section class="studio-panel tool-panel">
          <h4>直播工具</h4>
          <button class="tool-row" type="button" @click="toggleCaption">
            <span>
              <strong>实时字幕</strong>
              <small>{{ captionSummary }}</small>
            </span>
            <em :class="{ on: state.captionActive, warn: !state.captionSupported }">
              {{ state.captionActive ? "开" : state.captionSupported ? "关" : "不可用" }}
            </em>
          </button>

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
        </section>
      </aside>

      <main class="studio-center">
        <div class="room-strip">
          <div>
            <h4>{{ streamModeText }}</h4>
            <span>{{ state.message || "准备好后即可开始直播" }}</span>
          </div>
          <div class="room-strip-actions">
            <a-button :loading="state.starting" @click="startScreenLive">屏幕</a-button>
            <a-button :loading="state.starting" @click="startCameraLive">摄像头</a-button>
            <a-button type="primary" danger :disabled="!state.liveActive" @click="stopBrowserLive">
              停止直播
            </a-button>
          </div>
        </div>

        <div class="preview-stage">
          <video ref="previewRef" class="studio-preview" autoplay muted playsinline controls></video>
          <video
            v-if="state.isScreenSharing && state.cameraPipEnabled && state.liveActive"
            ref="cameraVideoRef"
            class="camera-pip-preview"
            autoplay
            muted
            playsinline
          ></video>
          <div v-if="!state.liveActive" class="preview-empty">
            <strong>等待开播</strong>
            <span>选择屏幕或摄像头</span>
          </div>
          <span v-if="state.liveActive" class="preview-live-tag">直播中</span>
          <div v-if="state.subtitleText" class="subtitle-preview">{{ state.subtitleText }}</div>
        </div>

        <div class="control-dock">
          <div class="dock-metrics">
            <div>
              <span>连接</span>
              <strong>{{ state.signalConnected ? "正常" : "等待" }}</strong>
            </div>
            <div>
              <span>字幕</span>
              <strong>{{ captionSummary }}</strong>
            </div>
            <div>
              <span>降噪</span>
              <strong>{{ denoiseCompactSummary }}</strong>
            </div>
          </div>
          <a-button type="primary" size="large" :loading="state.starting" @click="startScreenLive">
            {{ state.liveActive ? "切换屏幕" : "开始直播" }}
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
                <strong>{{ roomId || "--" }}</strong>
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
        <div class="chat-feed">
          <div v-if="state.liveActive" class="chat-message system">
            <strong>系统</strong>
            <span>直播已开始，等待观众互动</span>
          </div>
          <div v-else class="chat-empty">
            <strong>暂无互动</strong>
            <span>开播后显示弹幕和礼物消息</span>
          </div>
        </div>
        <div class="chat-tools">
          <button type="button">弹幕</button>
          <button type="button">礼物</button>
          <button type="button">关注</button>
        </div>
      </aside>
    </div>
  </section>
</template>

<script setup>
import { computed, onBeforeUnmount, reactive, ref } from "vue"
import $modal from "@/utils/message"
import liveAPI from "@/api/live"
import { createBrowserLiveFallbackUrls, createPeerConnection } from "@/utils/browserLive"
import { createLiveCaptionEngine, isLiveCaptionSupported } from "@/utils/liveCaption"
import { createLiveDenoiseEngine } from "@/utils/liveDenoise"
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
const cameraVideoRef = ref(null)

const state = reactive({
  starting: false,
  liveActive: false,
  isScreenSharing: false,
  cameraPipEnabled: true,
  pipSwitching: false,
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
let cameraStream = null
let pipCanvas = null
let pipAnimationId = null
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

const captionSummary = computed(() => {
  if (state.captionActive) {
    return "识别中"
  }
  if (!state.captionSupported) {
    return "暂不可用"
  }
  return "未开启"
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

const streamModeText = computed(() => {
  if (!state.liveActive) {
    return "未选择画面"
  }
  return state.isScreenSharing ? "屏幕直播" : "摄像头直播"
})

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
    state.denoiseRuntimeInfo = "未检测到麦克风声音。"
    return
  }

  const settings = typeof audioTrack.getSettings === "function" ? audioTrack.getSettings() : {}
  const lines = [
    audioTrack.label ? `当前麦克风：${audioTrack.label}` : "麦克风已连接",
    state.denoiseEnabled ? "降噪已启用" : "",
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
    microphoneStream = null
  }

  auxiliaryStreams = [displayStream]
  if (microphoneStream) {
    auxiliaryStreams.push(microphoneStream)
  }

  // 尝试获取摄像头用于浮窗
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
  const microphoneTracks = microphoneStream?.getAudioTracks?.() || []
  const displayAudioTracks = displayStream.getAudioTracks()
  const audioTracks = microphoneTracks.length ? microphoneTracks : displayAudioTracks

  // 如果有摄像头浮窗，用 Canvas 合成画面
  if (cameraStream && cameraStream.getVideoTracks().length > 0) {
    return createPipCompositeStream(displayStream, cameraStream, [...videoTracks, ...audioTracks])
  }

  return new MediaStream([...videoTracks, ...audioTracks])
}

const startScreenLive = async () => {
  state.isScreenSharing = true
  state.cameraPipEnabled = false
  await startBrowserLive(() => createScreenStream())
}

const startCameraLive = async () => {
  state.isScreenSharing = false
  state.cameraPipEnabled = false
  await startBrowserLive(() =>
    navigator.mediaDevices.getUserMedia({
      video: true,
      audio: createMicrophoneConstraints(),
    })
  )
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

  // 驱动摄像头 PiP 预览
  if (cameraVideoRef.value) {
    cameraVideoRef.value.srcObject = camStream
    cameraVideoRef.value.play?.().catch(ignoreMediaSideEffect)
  }

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
      if (cameraVideoRef.value) {
        cameraVideoRef.value.srcObject = null
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
    $modal.msgWarning("摄像头浮窗切换失败: " + (error.message || "未知错误"))
  } finally {
    state.pipSwitching = false
  }
}

/**
 * 切换直播画面：更新所有观众连接的画面与声音，并刷新预览
 */
const replacePublishingStream = async (newStream) => {
  const oldStream = publishingStream
  publishingStream = await alignPublishingLatency(newStream)
  attachPreview()

  // 更新所有已连接观众的画面与声音
  for (const [sessionId, peer] of peerMap) {
    if (peer.connectionState !== "connected") continue
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
  if (oldStream) {
    const captureIds = new Set(captureStream?.getTracks().map((t) => t.id) || [])
    oldStream.getTracks().forEach((track) => {
      if (!captureIds.has(track.id)) track.stop()
    })
  }
}

const startBrowserLive = async (streamFactory) => {
  if (!props.roomId) {
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
    startGuardLoop()
    setMessage("直播已开始，观众可以进入房间观看")
    emit("status-change")
  } catch (error) {
    closeAllPeers()
    closeSignal()
    await releaseMediaResources()
    stopCaption()
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
    return alignPublishingLatency(stream)
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
    baseStream = result.stream
    describeAudioProcessingState(baseStream)
    return alignPublishingLatency(baseStream)
  } catch (error) {
    state.denoiseStatus = "error"
    state.denoiseDetail = "降噪暂不可用，已继续使用原始麦克风声音"
    state.denoiseBackend = ""
    state.denoiseModelName = ""
    state.denoiseUsingEnhanced = false
    state.denoiseRuntimeInfo = ""
    $modal.msgWarning(state.denoiseDetail)
    await denoiseEngine.stop()
    denoiseEngine = null
    return alignPublishingLatency(stream)
  }
}

const alignPublishingLatency = async (stream) => {
  await stopLatencyAlignment({ stopDerivedTracks: true, protectedStreams: [captureStream, stream] })
  latencyAlignedStream = await createAlignedLatencyStream(stream, LIVE_SYNC_LATENCY_MS)
  setMessage("画面和声音已准备就绪")
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
    const alignedStream = await alignPublishingLatency(result.stream)
    await switchPublishingStream(alignedStream)
    state.denoiseStatus = "warming"
    state.denoiseBackend = result.backend || ""
    state.denoiseModelName = result.modelName || ""
    state.denoiseDetail = "降噪已开启"
    state.denoiseUsingEnhanced = false
    describeAudioProcessingState(result.stream)
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
          setMessage("直播连接已断开，请重新开播")
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
    $modal.msgWarning("实时字幕暂不可用，请稍后重试")
    return
  }
  if (!state.liveActive) {
    $modal.msgWarning("请先开播，再开启实时字幕")
    return
  }
  if (!captureStream?.getAudioTracks?.().some((track) => track.readyState === "live")) {
    $modal.msgWarning("未检测到可用麦克风声音，请重新开启摄像头直播")
    return
  }
  if (state.captionActive) {
    stopCaption()
    setMessage("实时字幕已关闭")
    return
  }

  captionEngine = createLiveCaptionEngine({
    sourceStream: captureStream,
    onText: (text) => {
      state.subtitleText = text
      sendSignal({
        type: text ? "subtitle" : "subtitle-clear",
        roomId: props.roomId,
        text,
      })
    },
    onError: (event, options = {}) => {
      const error = event?.error || event?.code || event?.message || ""
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
            : error === "network" || error === "service-unavailable"
              ? "实时字幕暂不可用，请稍后重试"
              : "字幕识别已中断，请检查麦克风权限后重试"
      $modal.msgWarning(hint)
      setMessage(hint)
    },
  })

  if (!captionEngine) {
    $modal.msgWarning("实时字幕暂不可用，请稍后重试")
    return
  }

  try {
    setMessage("正在开启实时字幕...")
    await captionEngine.start()
    state.captionActive = true
    setMessage("实时字幕已开启")
  } catch (error) {
    captionEngine = null
    const hint =
      error?.code === "audio-capture"
        ? "字幕识别没有可用麦克风，请检查设备后重试"
        : "实时字幕暂不可用，请稍后重试"
    $modal.msgError(hint)
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

const stopBrowserLive = async (options = {}) => {
  state.liveActive = false
  state.signalConnected = false
  state.isScreenSharing = false
  state.cameraPipEnabled = false
  stopGuardLoop()
  const stopMessage = options.guardReason || null
  setMessage("直播已停止")
  if (stopMessage) {
    setMessage(stopMessage)
  }
  stopCaption()
  stopPipComposite()
  if (cameraStream) {
    cameraStream.getTracks().forEach((t) => t.stop())
    cameraStream = null
  }
  if (cameraVideoRef.value) {
    cameraVideoRef.value.srcObject = null
  }
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
  stopPipComposite()
  if (cameraStream) {
    cameraStream.getTracks().forEach((t) => t.stop())
    cameraStream = null
  }
  closeSignal()
  closeAllPeers()
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
.room-strip-actions,
.control-dock,
.dock-metrics,
.chat-header,
.chat-tools {
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
.tool-row,
.chat-tools button {
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

.room-strip-actions {
  flex-shrink: 0;
  gap: 8px;
}

.preview-stage {
  position: relative;
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

.studio-preview {
  width: 100%;
  height: 100%;
  min-height: 380px;
  object-fit: contain;
  background: #05070a;
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

.camera-pip-preview {
  position: absolute;
  right: 16px;
  bottom: 16px;
  width: 25%;
  min-width: 120px;
  max-width: 240px;
  height: auto;
  border-radius: 8px;
  border: 2px solid rgba(255, 255, 255, 0.82);
  box-shadow: 0 8px 22px rgba(0, 0, 0, 0.45);
  z-index: 10;
  object-fit: cover;
  background: #0f172a;
}

.subtitle-preview {
  position: absolute;
  left: 50%;
  bottom: 26px;
  transform: translateX(-50%);
  max-width: calc(100% - 48px);
  padding: 10px 16px;
  border-radius: 999px;
  background: rgba(15, 23, 42, 0.86);
  color: #ffffff;
  font-size: 15px;
  line-height: 1.5;
  text-align: center;
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
  height: calc(100% - 92px);
  min-height: 500px;
  padding: 10px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  overflow: auto;
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

.chat-tools {
  height: 46px;
  gap: 8px;
  padding: 8px;
  background: #242830;
  border-top: 1px solid #383d46;
}

.chat-tools button {
  flex: 1;
  height: 30px;
  border-radius: 4px;
  background: #333840;
  color: #cdd4df;
  cursor: pointer;
}

.chat-tools button:hover {
  color: #ffffff;
  background: #414751;
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

  .room-strip-actions,
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
</style>
