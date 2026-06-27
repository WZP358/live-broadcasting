import { defineStore } from "pinia"
import { ref } from "vue"
import notificationApi from "@/api/notification"

export const useNotificationStore = defineStore("notification", () => {
    const unreadCount = ref(0)
    const wsConnected = ref(false)
    const latestNotifications = ref([])

    const fetchUnreadCount = async () => {
        try {
            const res = await notificationApi.getUnreadCount({ silentError: true })
            if (res && res.code === 0) {
                unreadCount.value = res.data || 0
            }
        } catch (e) {}
    }

    const incrementUnread = () => {
        unreadCount.value++
    }

    const markRead = async (notificationId) => {
        const previousCount = unreadCount.value
        try {
            await notificationApi.markRead({ notificationId })
            if (unreadCount.value > 0) {
                unreadCount.value--
            }
        } catch (e) {
            unreadCount.value = previousCount
        }
    }

    const markAllRead = async () => {
        const previousCount = unreadCount.value
        try {
            await notificationApi.markAllRead()
            unreadCount.value = 0
        } catch (e) {
            unreadCount.value = previousCount
        }
    }

    const addNotification = (notification) => {
        latestNotifications.value.unshift(notification)
        if (latestNotifications.value.length > 50) {
            latestNotifications.value.pop()
        }
        if (!notification || notification.isRead !== 1) {
            unreadCount.value++
        }
    }

    const setWsConnected = (connected) => {
        wsConnected.value = connected
    }

    return {
        unreadCount,
        wsConnected,
        latestNotifications,
        fetchUnreadCount,
        incrementUnread,
        markRead,
        markAllRead,
        addNotification,
        setWsConnected,
    }
})
