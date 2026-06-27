const DEFAULT_FPS = 30

const waitForVideoReady = (video) =>
  new Promise((resolve) => {
    if (video.readyState >= 2) {
      resolve()
      return
    }
    const finish = () => {
      video.removeEventListener("loadedmetadata", finish)
      video.removeEventListener("canplay", finish)
      resolve()
    }
    video.addEventListener("loadedmetadata", finish, { once: true })
    video.addEventListener("canplay", finish, { once: true })
    window.setTimeout(finish, 500)
  })

const resizeCanvas = (canvas, width, height) => {
  if (canvas.width !== width) {
    canvas.width = width
  }
  if (canvas.height !== height) {
    canvas.height = height
  }
}

export const createDelayedVideoStream = async (
  sourceStream,
  delayMs = 0,
  { fps = DEFAULT_FPS } = {},
) => {
  const [sourceVideoTrack] = sourceStream?.getVideoTracks?.() || []
  if (!sourceVideoTrack || !delayMs || typeof document === "undefined") {
    return sourceStream
  }

  const settings = sourceVideoTrack.getSettings?.() || {}
  const width = Number(settings.width || 1280)
  const height = Number(settings.height || 720)
  const frameIntervalMs = Math.max(16, Math.round(1000 / Math.max(1, fps)))
  const maxBufferedFrames = Math.max(4, Math.ceil(delayMs / frameIntervalMs) + 6)

  const sourceVideo = document.createElement("video")
  sourceVideo.muted = true
  sourceVideo.playsInline = true
  sourceVideo.srcObject = new MediaStream([sourceVideoTrack])

  const outputCanvas = document.createElement("canvas")
  resizeCanvas(outputCanvas, width, height)
  const outputContext = outputCanvas.getContext("2d", { alpha: false })

  const frames = []
  let stopped = false
  let captureTimer = null
  let renderTimer = null

  const stopVideoSyncDelay = () => {
    if (stopped) {
      return
    }
    stopped = true
    if (captureTimer) {
      window.clearInterval(captureTimer)
      captureTimer = null
    }
    if (renderTimer) {
      window.clearInterval(renderTimer)
      renderTimer = null
    }
    frames.length = 0
    sourceVideo.pause()
    sourceVideo.srcObject = null
  }

  sourceVideoTrack.addEventListener?.("ended", stopVideoSyncDelay, { once: true })

  await sourceVideo.play().catch(() => {})
  await waitForVideoReady(sourceVideo)

  const captureFrame = () => {
    if (stopped || sourceVideo.readyState < 2) {
      return
    }
    const nextWidth = sourceVideo.videoWidth || width
    const nextHeight = sourceVideo.videoHeight || height
    resizeCanvas(outputCanvas, nextWidth, nextHeight)

    const frameCanvas = document.createElement("canvas")
    resizeCanvas(frameCanvas, nextWidth, nextHeight)
    const frameContext = frameCanvas.getContext("2d", { alpha: false })
    frameContext.drawImage(sourceVideo, 0, 0, nextWidth, nextHeight)
    frames.push({
      time: performance.now(),
      canvas: frameCanvas,
    })
    while (frames.length > maxBufferedFrames) {
      frames.shift()
    }
  }

  const renderDelayedFrame = () => {
    if (stopped || !frames.length) {
      return
    }
    const targetTime = performance.now() - delayMs
    let frameIndex = frames.findIndex((frame) => frame.time > targetTime) - 1
    if (frameIndex < 0 && frames[0].time <= targetTime) {
      frameIndex = 0
    }
    if (frameIndex < 0) {
      return
    }

    const frame = frames[frameIndex]
    outputContext.drawImage(frame.canvas, 0, 0, outputCanvas.width, outputCanvas.height)
    if (frameIndex > 0) {
      frames.splice(0, frameIndex)
    }
  }

  outputContext.fillStyle = "#050609"
  outputContext.fillRect(0, 0, outputCanvas.width, outputCanvas.height)
  captureFrame()
  captureTimer = window.setInterval(captureFrame, frameIntervalMs)
  renderTimer = window.setInterval(renderDelayedFrame, frameIntervalMs)

  const delayedVideoStream = outputCanvas.captureStream(Math.max(1, fps))
  const delayedVideoTrack = delayedVideoStream.getVideoTracks()[0]
  delayedVideoTrack.addEventListener?.("ended", stopVideoSyncDelay, { once: true })

  const delayedStream = new MediaStream([
    delayedVideoTrack,
    ...sourceStream.getAudioTracks(),
  ])
  delayedStream.stopVideoSyncDelay = stopVideoSyncDelay
  delayedStream.sourceVideoTrackId = sourceVideoTrack.id
  return delayedStream
}
