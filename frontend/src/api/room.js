import request from '@/utils/request'

export default {
    /**
     * 获取房间信息
     * @param {*} params 
     * @returns 
     */
    getRoomInfo(params, options = {}) {
        return request({
            url: '/api/v1/room/detail',
            method: 'get',
            params,
            silentError: Boolean(options.silentError),
        })
    },
    /**
     * 获取房间额外信息
     * @param {*} params 
     * @returns 
     */
    getRoomExtraInfo(params, options = {}) {
        return request({
            url: '/api/v1/room/extra/info',
            method: 'get',
            params,
            silentError: options.silentError !== false,
        })
    },
    getIntimacyRank(params, options = {}) {
        return request({
            url: '/api/v1/room/intimacy/rank',
            method: 'get',
            params,
            silentError: Boolean(options.silentError),
        })
    },
    submitSatisfaction(data) {
        return request({
            url: '/api/v1/room/satisfaction/submit',
            method: 'post',
            data,
        })
    },
}
