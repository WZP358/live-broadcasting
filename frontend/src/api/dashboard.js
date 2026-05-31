import request from '@/utils/request'

export default {
  getMyRooms() {
    return request({ url: '/api/v1/dashboard/my-rooms', method: 'get' })
  },
  getRoomStats(roomId) {
    return request({ url: `/api/v1/dashboard/room-stats/${roomId}`, method: 'get' })
  }
}
