import request from '@/utils/request'

export default {
  getSettlements(params) {
    return request({ url: '/api/v1/settlement/list', method: 'get', params })
  },
  getSummary() {
    return request({ url: '/api/v1/settlement/summary', method: 'get' })
  }
}
