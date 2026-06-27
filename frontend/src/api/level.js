import request from '@/utils/request'

export default {
    myLevel(options = {}) {
        return request({
            url: '/api/v1/level/my',
            method: 'get',
            silentError: Boolean(options.silentError),
        })
    },
    getMyLevel(options = {}) {
        return this.myLevel(options)
    },
    userLevel(userId, options = {}) {
        return request({
            url: '/api/v1/level/user/' + userId,
            method: 'get',
            silentError: Boolean(options.silentError),
        })
    },
}
