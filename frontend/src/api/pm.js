import request from '@/utils/request'

export default {
  getContacts() {
    return request({ url: '/api/v1/pm/contacts', method: 'get' })
  },
  getConversation(withUserId, page = 1, limit = 20) {
    return request({ url: '/api/v1/pm/conversation', method: 'get', params: { withUserId, page, limit } })
  },
  send(data) {
    return request({ url: '/api/v1/pm/send', method: 'post', data })
  },
  markRead(data) {
    return request({ url: '/api/v1/pm/read', method: 'post', data })
  }
}
