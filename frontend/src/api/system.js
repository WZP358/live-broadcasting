import request from '@/utils/request'

export default {
    // 获取菜单
    getMenus() {
        return request({
            url: '/api/v1/system/getMenus',
            method: 'get'
        })
    },
    page(type, params) {
        return request({
            url: `/api/v1/system/${type}/page`,
            method: 'get',
            params
        })
    },
    list(type, params) {
        return request({
            url: `/api/v1/system/${type}/list`,
            method: 'get',
            params
        })
    },
    detail(type, id) {
        return request({
            url: `/api/v1/system/${type}/list`,
            method: 'get',
            params: {
                id
            }
        })
    },
    save(type, data) {
        return request({
            url: `/api/v1/system/${type}/save`,
            method: 'post',
            data
        })
    },
    delete(type, ids) {
        return request({
            url: `/api/v1/system/${type}/delete`,
            method: 'post',
            data: {
                ids
            }
        })
    }
}