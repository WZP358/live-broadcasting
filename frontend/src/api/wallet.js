import request from '@/utils/request'

export default {
    /**
     * 获取钱包信息
     * @returns 
     */
    getBalance(options = {}) {
        return request({
            url: '/api/v1/wallet/getBalance',
            method: 'get',
            silentError: Boolean(options.silentError),
        })
    },
    /**
     * 获取充值金额档位
     */
    getRechargeTiers() {
        return request({
            url: '/api/v1/wallet/recharge-tiers',
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
    },
    /**
     * 查询充值订单状态，支付成功后后端会确认入账
     * @param {*} params
     * @returns
     */
    getRechargeStatus(params) {
        return request({
            url: '/api/v1/wallet/recharge/status',
            method: 'get',
            params
        })
    }
}
