import request from '@/utils/request'

export default {
    /**
     * 获取房间信息
     * @param {*} params 
     * @returns 
     */
    getRoomInfo(params) {
        return request({
            url: '/api/v1/room/detail',
            method: 'get',
            params,
        })
    },
    /**
     * 获取房间额外信息
     * @param {*} params 
     * @returns 
     */
    getRoomExtraInfo(params) {
        return request({
            url: '/api/v1/room/extra/info',
            method: 'get',
            params,
        })
    },
    getIntimacyRank(params) {
        return request({
            url: '/api/v1/room/intimacy/rank',
            method: 'get',
            params,
        })
    },
}
