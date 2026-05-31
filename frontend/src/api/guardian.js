import request from '@/utils/request'

export default {
  subscribe(data) {
    return request({ url: '/api/v1/guardian/subscribe', method: 'post', data })
  },
  cancelAutoRenew(data) {
    return request({ url: '/api/v1/guardian/cancel-renew', method: 'post', data })
  },
  myGuardians(params) {
    return request({ url: '/api/v1/guardian/my-guardians', method: 'get', params })
  },
  myFans(params) {
    return request({ url: '/api/v1/guardian/my-fans', method: 'get', params })
  },
  check(targetUserId) {
    return request({ url: '/api/v1/guardian/check', method: 'get', params: { targetUserId } })
  },
  getPrices() {
    return request({ url: '/api/v1/guardian/prices', method: 'get' })
  }
}
