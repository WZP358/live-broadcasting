import request from '@/utils/request'

export default {
    /**
     * 获取分类信息列表
     * @returns 
     */
    getCategories() {
        return request({
            url: '/api/v1/common/getCategories',
            method: 'get'
        })
    },
    sendVerifyCode(data) {
        return request({
            url: '/api/v1/common/sendVerifyCode',
            method: 'post',
            data
        })
    }
}