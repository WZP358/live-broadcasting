import request from '@/utils/request'

export default {
  submit(data) {
    return request({
      url: '/api/v1/customer-service/submit',
      method: 'post',
      data,
    })
  },
  myTickets(params) {
    return request({
      url: '/api/v1/customer-service/my',
      method: 'get',
      params,
    })
  },
  close(data) {
    return request({
      url: '/api/v1/customer-service/close',
      method: 'post',
      data,
    })
  },
}
