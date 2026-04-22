import request from '@/utils/request'

export default {
    /**
     * 获取钱包信息
     * @returns 
     */
    getBalance() {
        return request({
            url: '/api/v1/wallet/getBalance',
            method: 'get'
        })
    },
    /**
     * 获取钱包明细
     * @returns 
     */
    listRecentWalletLogs() {
        return request({
            url: '/api/v1/wallet/listRecentWalletLogs',
            method: 'get'
        })
    },
    /**
     * 获取钱包明细
     * @returns 
     */
    listWalletLogs(params) {
        return request({
            url: '/api/v1/wallet/listWalletLogs',
            method: 'get',
            params
        })
    },
    /**
     * 充值账户
     * @param {*} data 
     * @returns 
     */
    recharge(data) {
        return request({
            url: '/api/v1/wallet/recharge',
            method: 'post',
            data
        })
    }
}