import request from '@/utils/request'

export default {
    /**
     * 分页获取用户列表
     * @param {*} params 
     * @returns 
     */
    getPageUsers(params) {
        return request({
            url: '/api/v1/system/user/page',
            method: 'get',
            params
        })
    },
    /**
     * 保存用户信息
     * @param {*} data 
     * @returns 
     */
    saveUser(data) {
        return request({
            url: '/api/v1/system/user/save',
            method: 'post',
            data
        })
    },
    /**
     * 获取用户详情
     * @param {*} id 
     * @returns 
     */
    getUserDetail(id) {
        return request({
            url: `/api/v1/system/user/detail/${id}`,
            method: 'get'
        })
    },
    /**
     * 切换用户状态
     * @param {*} data 
     * @returns 
     */
    toggleUserStatus(data) {
        return request({
            url: '/api/v1/system/user/toggleStatus',
            method: 'post',
            data
        })
    },
}