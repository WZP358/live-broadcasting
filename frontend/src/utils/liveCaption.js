const DEFAULT_SERVICE_PORT = 8200
const SERVICE_OPEN_TIMEOUT = 3500
const STT_SAMPLE_RATE = 16000
const CAPTION_CHUNK_SIZE = 4096

const getWindow = () => (typeof window === "undefined" ? null : window)

const getAudioContextCtor = () => {
  const win = getWindow()
  if (!win) {
    return null
  }
  return win.AudioContext || win.webkitAudioContext || null
}

const buildCaptionServiceUrl = (port = DEFAULT_SERVICE_PORT) => {
  const win = getWindow()
  if (!win) {
    return `ws://127.0.0.1:${port}/ws`
  }

  const customUrl = win.localStorage?.getItem?.("live.caption.serviceUrl")
  if (customUrl) {
    return customUrl
  }

  return `ws://127.0.0.1:${port}/ws`
}

const canUseServiceCaption = () => {
  const win = getWindow()
  return Boolean(win?.WebSocket && getAudioContextCtor())
}

export const isLiveCaptionSupported = () => canUseServiceCaption()

const createCaptionError = (code, message) => {
  const error = new Error(message || code)
  error.code = code
  return error
}

const downsampleBuffer = (input, inputSampleRate, outputSampleRate = STT_SAMPLE_RATE) => {
  if (outputSampleRate === inputSampleRate) {
    return new Float32Array(input)
  }

  const ratio = inputSampleRate / outputSampleRate
  const outputLength = Math.max(1, Math.floor(input.length / ratio))
  const output = new Float32Array(outputLength)

  for (let i = 0; i < outputLength; i += 1) {
    const start = Math.floor(i * ratio)
    const end = Math.min(input.length, Math.floor((i + 1) * ratio))
    let sum = 0
    let count = 0
    for (let j = start; j < end; j += 1) {
      sum += input[j]
      count += 1
    }
    output[i] = count ? sum / count : input[start] || 0
  }

  return output
}

const safeJsonParse = (text) => {
  try {
    return JSON.parse(text)
  } catch (error) {
    return null
  }
}

const createServiceCaptionEngine = ({
  sourceStream,
  serviceUrl = buildCaptionServiceUrl(),
  lang = "zh-CN",
  onText,
  onError,
} = {}) => {
  const win = getWindow()
  const AudioContextCtor = getAudioContextCtor()

  if (!win?.WebSocket || !AudioContextCtor) {
    return null
  }

  let socket = null
  let audioContext = null
  let sourceNode = null
  let processorNode = null
  let silentGainNode = null
  let active = false

  const cleanup = async () => {
    active = false

    try {
      socket?.close?.()
    } catch (error) {
      // ignore cleanup errors
    }
    socket = null

    for (const node of [sourceNode, processorNode, silentGainNode]) {
      try {
        node?.disconnect?.()
      } catch (error) {
        // ignore cleanup errors
      }
    }
    sourceNode = null
    processorNode = null
    silentGainNode = null

    if (audioContext) {
      try {
        await audioContext.close?.()
      } catch (error) {
        // ignore cleanup errors
      }
      audioContext = null
    }
  }

  const openSocket = () =>
    new Promise((resolve, reject) => {
      const ws = new win.WebSocket(serviceUrl)
      let settled = false
      const timer = win.setTimeout(() => {
        if (settled) {
          return
        }
        settled = true
        try {
          ws.close()
        } catch (error) {
          // ignore
        }
        reject(createCaptionError("service-unavailable", "实时字幕暂不可用"))
      }, SERVICE_OPEN_TIMEOUT)

      ws.binaryType = "arraybuffer"

      ws.onopen = () => {
        if (settled) {
          return
        }
        settled = true
        win.clearTimeout(timer)
        resolve(ws)
      }

      ws.onerror = () => {
        if (settled) {
          return
        }
        settled = true
        win.clearTimeout(timer)
        try {
          ws.close()
        } catch (error) {
          // ignore
        }
        reject(createCaptionError("service-unavailable", "实时字幕暂不可用"))
      }

      ws.onclose = () => {
        if (!settled) {
          settled = true
          win.clearTimeout(timer)
          reject(createCaptionError("service-unavailable", "实时字幕暂不可用"))
          return
        }

        if (active) {
          active = false
          onError?.(createCaptionError("service-unavailable", "实时字幕已断开"))
        }
      }
    })

  const startAudioCapture = () => {
    audioContext = new AudioContextCtor({ sampleRate: STT_SAMPLE_RATE, latencyHint: "interactive" })

    if (audioContext.state === "suspended") {
      audioContext.resume?.()
    }

    sourceNode = audioContext.createMediaStreamSource(sourceStream)
    processorNode = audioContext.createScriptProcessor(CAPTION_CHUNK_SIZE, 1, 1)
    silentGainNode = audioContext.createGain()
    silentGainNode.gain.value = 0

    processorNode.onaudioprocess = (event) => {
      if (!active || socket?.readyState !== win.WebSocket.OPEN) {
        return
      }

      const input = event.inputBuffer.getChannelData(0)
      const pcm = downsampleBuffer(input, audioContext.sampleRate, STT_SAMPLE_RATE)
      socket.send(pcm.buffer)
    }

    sourceNode.connect(processorNode)
    processorNode.connect(silentGainNode)
    silentGainNode.connect(audioContext.destination)
  }

  return {
    async start() {
      const hasAudio = sourceStream?.getAudioTracks?.().some((track) => track.readyState !== "ended")
      if (!hasAudio) {
        throw createCaptionError("audio-capture", "未检测到可用麦克风声音")
      }

      socket = await openSocket()
      socket.onmessage = (event) => {
        if (typeof event.data !== "string") {
          return
        }
        const payload = safeJsonParse(event.data)
        if (payload?.type === "subtitle") {
          onText?.(String(payload.text || "").trim())
        }
        if (payload?.type === "subtitle-clear") {
          onText?.("")
        }
      }
      socket.send(JSON.stringify({ type: "reset", lang }))

      active = true
      startAudioCapture()
    },

    stop() {
      cleanup()
    },

    mode: "service",
  }
}

export const createLiveCaptionEngine = ({
  sourceStream,
  lang = "zh-CN",
  serviceUrl,
  onText,
  onError,
} = {}) => {
  let activeEngine = null
  const serviceEngine = createServiceCaptionEngine({ sourceStream, serviceUrl, lang, onText, onError })

  if (!serviceEngine) {
    return null
  }

  return {
    async start() {
      try {
        await serviceEngine.start()
        activeEngine = serviceEngine
        return serviceEngine.mode
      } catch (error) {
        serviceEngine.stop()
        throw error
      }
    },

    stop() {
      activeEngine?.stop?.()
      activeEngine = null
    },

    get mode() {
      return activeEngine?.mode || null
    },
  }
}
