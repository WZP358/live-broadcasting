import request from '@/utils/request'

export default {
    list(params) {
        return request({
            url: '/api/v1/replay/list',
            method: 'get',
            params,
        })
    },
    latest(params) {
        return request({
            url: '/api/v1/replay/latest',
            method: 'get',
            params,
        })
    },
}
