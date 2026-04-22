const RTC_CONFIG = {
  iceServers: [
    { urls: "stun:stun.l.google.com:19302" },
    { urls: "stun:stun1.l.google.com:19302" },
  ],
}

export const createBrowserLiveFallbackUrls = () => {
  const wsProtocol = location.protocol === "https:" ? "wss" : "ws"
  const host = location.hostname
  return [
    `${wsProtocol}://${host}:10022/`,
    `${wsProtocol}://${host}:9000/ws/browser-live`,
  ]
}

export const createBrowserLiveSignalUrl = () => createBrowserLiveFallbackUrls()[0]

export const createPeerConnection = () => new RTCPeerConnection(RTC_CONFIG)
