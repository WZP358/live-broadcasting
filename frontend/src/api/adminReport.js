import request from '@/utils/request'

export default {
  list(params) {
    return request({
      url: '/api/v1/admin/report/list',
      method: 'get',
      params,
    })
  },
  handle(data) {
    return request({
      url: '/api/v1/admin/report/handle',
      method: 'post',
      data,
    })
  },
}
