import request from '@/utils/request'

export default {
    get() {
        return request({ url: '/api/v1/notification/pref', method: 'get' })
    },
    save(data) {
        return request({ url: '/api/v1/notification/pref', method: 'post', data })
    },
}
