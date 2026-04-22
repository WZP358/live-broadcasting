import request from '@/utils/request'

export default {
    getPageGifts(params) {
        return request({
            url: '/api/v1/system/gift/page',
            method: 'get',
            params
        })
    },
}