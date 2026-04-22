const DEFAULT_SERVICE_PORT = 18765
const DEFAULT_CHUNK_SAMPLES = 8192
const WS_OPEN_TIMEOUT = 120000

const createChunkQueue = () => {
  const chunks = []
  let offset = 0

  return {
    push(chunk) {
      if (!chunk?.length) {
        return
      }
      chunks.push(chunk)
    },
    clear() {
      chunks.length = 0
      offset = 0
    },
    read(target) {
      let written = 0
      while (written < target.length && chunks.length) {
        const current = chunks[0]
        const available = current.length - offset
        const consume = Math.min(available, target.length - written)
        target.set(current.subarray(offset, offset + consume), written)
        written += consume
        offset += consume
        if (offset >= current.length) {
          chunks.shift()
          offset = 0
        }
      }
      return written
    },
  }
}

const mergeFloat32 = (left, right) => {
  if (!left.length) {
    return right
  }
  if (!right.length) {
    return left
  }
  const merged = new Float32Array(left.length + right.length)
  merged.set(left, 0)
  merged.set(right, left.length)
  return merged
}

const buildServiceUrl = (port = DEFAULT_SERVICE_PORT) => {
  if (typeof window === "undefined") {
    return `ws://127.0.0.1:${port}/ws`
  }

  const customUrl = window.localStorage.getItem("live.denoise.serviceUrl")
  if (customUrl) {
    return customUrl
  }

  return `ws://127.0.0.1:${port}/ws`
}

const closeAudioNode = (node) => {
  try {
    node?.disconnect?.()
  } catch (error) {
    // ignore disconnect errors during cleanup
  }
}

export const getLiveDenoiseServiceUrl = () => buildServiceUrl()

export const createLiveDenoiseEngine = ({
  serviceUrl = buildServiceUrl(),
  chunkSamples = DEFAULT_CHUNK_SAMPLES,
  onStateChange,
} = {}) => {
  let audioContext = null
  let sourceNode = null
  let captureNode = null
  let renderNode = null
  let silentGainNode = null
  let destinationNode = null
  let socket = null
  let processingBuffer = new Float32Array(0)
  let started = false
  let useEnhancedOutput = false

  const dryQueue = createChunkQueue()
  const enhancedQueue = createChunkQueue()

  const notifyState = (status, detail = "") => {
    onStateChange?.({ status, detail, useEnhancedOutput })
  }

  const stop = async () => {
    started = false
    useEnhancedOutput = false
    processingBuffer = new Float32Array(0)
    dryQueue.clear()
    enhancedQueue.clear()

    if (socket) {
      socket.onopen = null
      socket.onmessage = null
      socket.onerror = null
      socket.onclose = null
      try {
        socket.close()
      } catch (error) {
        // ignore socket close errors
      }
      socket = null
    }

    closeAudioNode(captureNode)
    closeAudioNode(renderNode)
    closeAudioNode(sourceNode)
    closeAudioNode(silentGainNode)
    closeAudioNode(destinationNode)
    captureNode = null
    renderNode = null
    sourceNode = null
    silentGainNode = null
    destinationNode = null

    if (audioContext) {
      try {
        await audioContext.close()
      } catch (error) {
        // ignore close errors
      }
      audioContext = null
    }
  }

  const openSocket = (config) =>
    new Promise((resolve, reject) => {
      const ws = new WebSocket(serviceUrl)
      let settled = false
      const timer = window.setTimeout(() => {
        if (settled) {
          return
        }
        settled = true
        try {
          ws.close()
        } catch (error) {
          // ignore
        }
        reject(new Error("连接本地 DeepFilterNet3 降噪服务超时，请确认服务已经启动。"))
      }, WS_OPEN_TIMEOUT)

      ws.binaryType = "arraybuffer"

      ws.onopen = () => {
        ws.send(
          JSON.stringify({
            type: "config",
            ...config,
          })
        )
      }

      ws.onmessage = (event) => {
        if (typeof event.data === "string") {
          const payload = JSON.parse(event.data)
          if (payload.type === "ready" && !settled) {
            settled = true
            window.clearTimeout(timer)
            resolve({ ws, payload })
            return
          }
          if (payload.type === "error") {
            const errorMessage = payload.message || "本地 DeepFilterNet3 降噪服务初始化失败。"
            notifyState("error", errorMessage)
            if (!settled) {
              settled = true
              window.clearTimeout(timer)
              try {
                ws.close()
              } catch (error) {
                // ignore
              }
              reject(new Error(errorMessage))
            }
          }
          return
        }

        const chunk = new Float32Array(event.data)
        if (!chunk.length) {
          return
        }
        if (!useEnhancedOutput) {
          useEnhancedOutput = true
          dryQueue.clear()
          notifyState("active", "DeepFilterNet3 已开始输出增强音频。")
        }
        enhancedQueue.push(chunk)
      }

      ws.onerror = () => {
        if (!settled) {
          settled = true
          window.clearTimeout(timer)
          reject(new Error("本地 DeepFilterNet3 降噪服务连接失败。"))
        }
      }

      ws.onclose = () => {
        if (!settled) {
          settled = true
          window.clearTimeout(timer)
          reject(new Error("本地 DeepFilterNet3 降噪服务已关闭。"))
          return
        }
        if (started) {
          useEnhancedOutput = false
          notifyState("fallback", "本地 DeepFilterNet3 降噪服务已断开，已回退到原始麦克风音频。")
        }
      }
    })

  const start = async (sourceStream) => {
    const [audioTrack] = sourceStream.getAudioTracks()
    if (!audioTrack) {
      throw new Error("未检测到麦克风音轨，无法启用 DeepFilterNet3 降噪。")
    }

    await stop()

    const AudioContextCtor = window.AudioContext || window.webkitAudioContext
    if (!AudioContextCtor) {
      throw new Error("当前浏览器不支持 Web Audio，无法接入 DeepFilterNet3。")
    }

    audioContext = new AudioContextCtor({
      latencyHint: "interactive",
    })

    if (audioContext.state === "suspended") {
      await audioContext.resume()
    }

    destinationNode = audioContext.createMediaStreamDestination()
    silentGainNode = audioContext.createGain()
    silentGainNode.gain.value = 0
    silentGainNode.connect(audioContext.destination)

    sourceNode = audioContext.createMediaStreamSource(new MediaStream([audioTrack]))
    captureNode = audioContext.createScriptProcessor(4096, 1, 1)
    renderNode = audioContext.createScriptProcessor(4096, 1, 1)

    const sampleRate = audioContext.sampleRate
    const { ws, payload } = await openSocket({
      sampleRate,
      channels: 1,
      chunkSamples,
    })
    socket = ws

    captureNode.onaudioprocess = (event) => {
      const input = new Float32Array(event.inputBuffer.getChannelData(0))
      if (!input.length) {
        return
      }

      dryQueue.push(input)
      processingBuffer = mergeFloat32(processingBuffer, input)

      while (processingBuffer.length >= chunkSamples) {
        const frame = processingBuffer.slice(0, chunkSamples)
        processingBuffer = processingBuffer.slice(chunkSamples)
        if (socket?.readyState === WebSocket.OPEN) {
          socket.send(frame.buffer)
        }
      }
    }

    renderNode.onaudioprocess = (event) => {
      const output = event.outputBuffer.getChannelData(0)
      output.fill(0)

      const activeQueue = useEnhancedOutput ? enhancedQueue : dryQueue
      const written = activeQueue.read(output)
      if (written < output.length && !useEnhancedOutput) {
        output.fill(0, written)
      }
    }

    sourceNode.connect(captureNode)
    // Keep the render node clocked by the live microphone input so the processed
    // destination stream continuously pulls audio instead of staying silent.
    sourceNode.connect(renderNode)
    captureNode.connect(silentGainNode)
    renderNode.connect(destinationNode)
    renderNode.connect(silentGainNode)

    started = true
    notifyState("warming", "本地 DeepFilterNet3 已连接，正在等待增强音频输出。")

    const processedStream = new MediaStream([
      ...sourceStream.getVideoTracks(),
      ...destinationNode.stream.getAudioTracks(),
    ])

    return {
      stream: processedStream,
      serviceUrl,
      sampleRate,
      backend: payload?.backend || "unknown",
      modelName: payload?.modelName || "unknown",
      stop,
    }
  }

  return {
    start,
    stop,
  }
}
