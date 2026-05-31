import request from '@/utils/request'

export default {
  listByRoom(roomId) {
    return request({ url: `/api/v1/tag/room/${roomId}`, method: 'get' })
  },
  save(data) {
    return request({ url: '/api/v1/tag/room/save', method: 'post', data })
  }
}
