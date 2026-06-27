import request from '@/utils/request'

export default {
    getNotifications(params) {
        return request({
            url: '/api/v1/notification/list',
            method: 'get',
            params,
        })
    },
    getUnreadCount(options = {}) {
        return request({
            url: '/api/v1/notification/unread/count',
            method: 'get',
            silentError: Boolean(options.silentError),
        })
    },
    markRead(data) {
        return request({
            url: '/api/v1/notification/read',
            method: 'post',
            data,
        })
    },
    markAllRead() {
        return request({
            url: '/api/v1/notification/read/all',
            method: 'post',
        })
    },
}
