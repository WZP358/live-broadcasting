import request from '@/utils/request'

export default {
    searchRooms(params) {
        return request({
            url: '/api/v1/search/rooms',
            method: 'get',
            params,
        })
    },
}
