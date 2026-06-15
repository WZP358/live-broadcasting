import request from '@/utils/request'

export default {
  list(params) {
    return request({
      url: '/admin/report/list',
      method: 'get',
      params,
    })
  },
  handle(data) {
    return request({
      url: '/admin/report/handle',
      method: 'post',
      data,
    })
  },
}
