import request from '@/utils/request'

export default {
    appoint(data) {
        return request({
            url: '/api/v1/moderator/appoint',
            method: 'post',
            data,
        })
    },
    dismiss(data) {
        return request({
            url: '/api/v1/moderator/dismiss',
            method: 'post',
            data,
        })
    },
    list(params) {
        return request({
            url: '/api/v1/moderator/list',
            method: 'get',
            params,
        })
    },
    check(params) {
        return request({
            url: '/api/v1/moderator/check',
            method: 'get',
            params,
        })
    },
    mute(data) {
        return request({
            url: '/api/v1/moderator/mute',
            method: 'post',
            data,
        })
    },
    kick(data) {
        return request({
            url: '/api/v1/moderator/kick',
            method: 'post',
            data,
        })
    },
}
