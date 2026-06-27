import request from '@/utils/request'

export default {
    /**
     *  获取直播分类
     * @param {*} params 
     * @returns 
     */
    listCategories(params) {
        return request({
            url: '/api/v1/category/query',
            method: 'get',
            params
        })
    },
    /**
     * 获取正在直播的直播间
     * @param {*} params 
     * @returns 
     */
    listLivingRooms(params, options = {}) {
        return request({
            url: '/api/v1/room/living',
            method: 'get',
            params,
            silentError: Boolean(options.silentError),
        })
    },
    /**
     * 获取历史记录
     * @param {*} params 
     * @returns 
     */
    listHistory(params, options = {}) {
        return request({
            url: '/api/v1/watch/list',
            method: 'get',
            params,
            silentError: Boolean(options.silentError),
        })
    },
    /**
     * 清除历史记录
     * @returns 
     */
    clearHistory() {
        return request({
            url: '/api/v1/watch/history/clear',
            method: 'post'
        })
    },
    /**
     * 获取直播间信息
     * @param {*} params
     * @returns 
     */
    getRoomSettingsInfo() {
        return request({
            url: '/api/v1/room/setting/info',
            method: 'get',
        })
    },
    getLiveStatus() {
        return request({
            url: '/api/v1/live/getLiveStatus',
            method: 'get',
        })
    },
    applySecret() {
        return request({
            url: '/api/v1/live/applySecret',
            method: 'post',
        })
    },
    stopLive() {
        return request({
            url: '/api/v1/live/stopLive',
            method: 'post',
        })
    },
    checkGuardFrame(roomId, file) {
        const data = new FormData()
        data.append('roomId', roomId)
        data.append('file', file, 'frame.jpg')
        return request({
            url: '/api/v1/live/guard/check',
            method: 'post',
            data,
            timeout: 30000,
            headers: { 'Content-Type': 'multipart/form-data' },
        })
    },
    uploadLiveRecord({ roomId, file, duration }) {
        const data = new FormData()
        data.append('roomId', roomId)
        if (duration) {
            data.append('duration', duration)
        }
        data.append('file', file, file?.name || 'live-record.webm')
        return request({
            url: '/api/v1/live/record/upload',
            method: 'post',
            data,
            timeout: 120000,
            headers: { 'Content-Type': 'multipart/form-data' },
        })
    },
    /**
     * 保存直播间信息
     * @param {*} data
     * @returns
     */
    saveRoomInfo(data) {
        return request({
            url: '/api/v1/room/info/save',
            method: 'post',
            data,
        })
    },
    getLiveStatInfo(data) {
        return request({
            url: '/api/v1/live/getLiveRecords',
            method: 'post',
            data
        })
    }
} 
