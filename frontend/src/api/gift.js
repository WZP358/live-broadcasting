import request from '@/utils/request'

export default {
    getGiftList() {
        return request({
            url: '/api/v1/present/list',
            method: 'get',
        })
    },
    rewardGift(data) {
        return request({
            url: '/api/v1/present/reward',
            method: 'post',
            data
        })
    },
    /**
     * 获取礼物列表
     * @param {*} data 
     * @returns 
     */
    getRewardRecords(data) {
        return request({
            url: '/api/v1/present/reward/records',
            method: 'post',
            data
        })
    }
}