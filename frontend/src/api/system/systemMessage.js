import request from '@/utils/request'

export default {
    pageDetail(params) {
        return request({
            url: '/api/v1/system/message/pageDetail',
            method: 'get',
            params,
        })
    },
}