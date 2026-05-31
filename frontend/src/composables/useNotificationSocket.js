import { useNotificationStore } from "@/stores/modules/notification"
import { useUserStore } from "@/stores/modules/user"
import { storeToRefs } from "pinia"

let socket = null
let heartbeatTimer = null
let reconnectTimer = null
let reconnectAttempts = 0
const MAX_RECONNECT_ATTEMPTS = 50
const RECONNECT_DELAY = 5000
const HEARTBEAT_INTERVAL = 9500

export function useNotificationSocket() {
    const userStore = useUserStore()
    const notificationStore = useNotificationStore()
    const { isLogin, userToken } = storeToRefs(userStore)

    const connect = () => {
        if (!isLogin.value || !userToken.value) {
            return
        }
        if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
            return
        }

        const wsUrl = "ws://" + location.hostname + ":10022?token=" + encodeURIComponent(userToken.value)
        socket = new WebSocket(wsUrl)

        socket.onopen = () => {
            notificationStore.setWsConnected(true)
            reconnectAttempts = 0
            startHeartbeat()
        }

        socket.onmessage = (event) => {
            try {
                const msg = JSON.parse(event.data)
                if (msg.method === "notification" && msg.data) {
                    notificationStore.addNotification(msg.data)
                }
            } catch (e) {
                // ignore parse errors
            }
        }

        socket.onclose = (event) => {
            notificationStore.setWsConnected(false)
            stopHeartbeat()
            if (event.code !== 1000 && event.code !== 1005 && reconnectAttempts < MAX_RECONNECT_ATTEMPTS) {
                scheduleReconnect()
            }
        }

        socket.onerror = () => {
            notificationStore.setWsConnected(false)
            stopHeartbeat()
        }
    }

    const disconnect = () => {
        stopHeartbeat()
        if (reconnectTimer) {
            clearTimeout(reconnectTimer)
            reconnectTimer = null
        }
        if (socket) {
            socket.close(1000)
            socket = null
        }
        notificationStore.setWsConnected(false)
    }

    const startHeartbeat = () => {
        stopHeartbeat()
        heartbeatTimer = setInterval(() => {
            if (socket && socket.readyState === WebSocket.OPEN) {
                socket.send(JSON.stringify({ msgType: 2 }))
            }
        }, HEARTBEAT_INTERVAL)
    }

    const stopHeartbeat = () => {
        if (heartbeatTimer) {
            clearInterval(heartbeatTimer)
            heartbeatTimer = null
        }
    }

    const scheduleReconnect = () => {
        if (reconnectTimer) {
            clearTimeout(reconnectTimer)
        }
        reconnectAttempts++
        reconnectTimer = setTimeout(() => {
            connect()
        }, RECONNECT_DELAY)
    }

    return {
        connect,
        disconnect,
    }
}
