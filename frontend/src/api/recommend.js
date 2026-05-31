import request from '@/utils/request'

export default {
  getRecommendedRooms(limit = 12) {
    return request({ url: '/api/v1/recommend/rooms', method: 'get', params: { limit } })
  }
}
