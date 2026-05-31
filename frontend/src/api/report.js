import request from '@/utils/request'

export default {
    submit(data) {
        return request({
            url: '/api/v1/report/submit',
            method: 'post',
            data,
        })
    },
    myReports(params) {
        return request({
            url: '/api/v1/report/my',
            method: 'get',
            params,
        })
    },
}
