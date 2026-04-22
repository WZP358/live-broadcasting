import request from '@/utils/request'

export default {
    /**
     * 保存历史记录
     * @param {*} params 
     * @returns 
     */
    saveHistory(data) {
        return request({
            url: '/api/v1/watch/history/save',
            method: 'post',
            data,
        })
    },
    /**
     * 关注
     * @param {*} data 
     * @returns 
     */
    follow(data) {
        return request({
            url: '/api/v1/watch/follow',
            method: 'post',
            data,
        })
    },
    /**
     * 取消关注
     * @param {*} data 
     * @returns 
     */
    unFollow(data) {
        return request({
            url: '/api/v1/watch/unFollow',
            method: 'post',
            data,
        })
    },
}