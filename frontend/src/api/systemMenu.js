import request from '@/utils/request'

export default {
    getPage(params) {
        return request({
            url: '/api/v1/system/menu/page',
            method: 'get',
            params
        })
    },
}