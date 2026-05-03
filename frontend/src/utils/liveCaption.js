const getSpeechRecognitionCtor = () => {
  if (typeof window === "undefined") {
    return null
  }
  return window.SpeechRecognition || window.webkitSpeechRecognition || null
}

export const isLiveCaptionSupported = () => Boolean(getSpeechRecognitionCtor())

export const createLiveCaptionEngine = ({ lang = "zh-CN", onText, onError } = {}) => {
  const SpeechRecognitionCtor = getSpeechRecognitionCtor()
  if (!SpeechRecognitionCtor) {
    return null
  }

  const recognition = new SpeechRecognitionCtor()
  recognition.lang = lang
  recognition.continuous = true
  recognition.interimResults = true

  let active = false
  let manuallyStopped = false

  const fatalErrors = new Set(["audio-capture", "not-allowed", "service-not-allowed", "network"])

  recognition.onresult = (event) => {
    let text = ""
    for (let i = event.resultIndex; i < event.results.length; i += 1) {
      text += event.results[i][0]?.transcript || ""
    }
    onText?.(text.trim())
  }

  recognition.onerror = (event) => {
    const error = event?.error || "unknown"
    if (fatalErrors.has(error)) {
      active = false
      onError?.(event)
      return
    }
    onError?.(event, { recoverable: true })
  }

  recognition.onend = () => {
    if (active && !manuallyStopped) {
      try {
        recognition.start()
      } catch (error) {
        onError?.(error, { recoverable: true })
      }
    }
  }

  return {
    start() {
      active = true
      manuallyStopped = false
      recognition.start()
    },
    stop() {
      active = false
      manuallyStopped = true
      recognition.stop()
    },
  }
}
