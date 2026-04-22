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

  recognition.onresult = (event) => {
    let text = ""
    for (let i = event.resultIndex; i < event.results.length; i += 1) {
      text += event.results[i][0]?.transcript || ""
    }
    onText?.(text.trim())
  }

  recognition.onerror = (event) => {
    onError?.(event)
  }

  recognition.onend = () => {
    if (active) {
      try {
        recognition.start()
      } catch (error) {
        onError?.(error)
      }
    }
  }

  return {
    start() {
      active = true
      recognition.start()
    },
    stop() {
      active = false
      recognition.stop()
    },
  }
}
