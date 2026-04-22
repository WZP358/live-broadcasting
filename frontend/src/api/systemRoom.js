import request from '@/utils/request'

export default {
    /**
     * 分页获取直播间列表
     * @param {*} params 
     * @returns 
     */
    getPageRooms(params) {
        return request({
            url: '/api/v1/system/room/page',
            method: 'get',
            params
        })
    },
    /**
     * 保存直播间信息
     * @param {*} data 
     * @returns 
     */
    saveRoom(data) {
        return request({
            url: '/api/v1/system/room/save',
            method: 'post',
            data
        })
    },
    /**
     * 切换直播间封禁状态
     * @param {*} data 
     * @returns 
     */
    toggleRoomStatus(data) {
        return request({
            url: '/api/v1/system/room/toggleStatus',
            method: 'post',
            data
        })
    },
    /**
     * 获取直播间详情
     * @param {*} id 
     * @returns 
     */
    getRoomDetail(id) {
        return request({
            url: '/api/v1/system/room/detail',
            method: 'get',
            params: { id }
        })
    },
}
