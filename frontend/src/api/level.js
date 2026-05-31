import request from '@/utils/request'

export default {
    myLevel() {
        return request({
            url: '/api/v1/level/my',
            method: 'get',
        })
    },
    userLevel(userId) {
        return request({
            url: '/api/v1/level/user/' + userId,
            method: 'get',
        })
    },
}
