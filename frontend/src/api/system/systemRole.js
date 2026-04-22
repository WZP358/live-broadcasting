import request from '@/utils/request'

export default {
    listMenusByRole(params) {
        return request({
            url: '/api/v1/system/role/listMenusByRole',
            method: 'get',
            params,
        })
    },
    listMenus(params) {
        return request({
            url: '/api/v1/system/role/listMenus',
            method: 'get',
            params,
        })
    },
    /**
     * 保存角色菜单
     * @param {*} params 
     * @returns 
     */
    saveRoleMenus(data) {
        return request({
            url: '/api/v1/system/role/saveRoleMenus',
            method: 'post',
            data,
        })
    },
    /**
     * 删除角色菜单
     * @param {*} params 
     * @returns 
     */
    removeRoleMenus(data) {
        return request({
            url: '/api/v1/system/role/removeRoleMenus',
            method: 'post',
            data,
        })
    },
}