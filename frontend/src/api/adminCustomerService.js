import request from '@/utils/request'

export default {
  list(params) {
    return request({
      url: '/api/v1/admin/customer-service/list',
      method: 'get',
      params,
    })
  },
  reply(data) {
    return request({
      url: '/api/v1/admin/customer-service/reply',
      method: 'post',
      data,
    })
  },
}
