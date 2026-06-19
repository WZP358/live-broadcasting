import request from '@/utils/request'

export default {
  /**
   * 获取个性化推荐直播间（基于余弦相似度的混合推荐）
   * @param {number} limit 返回数量
   */
  getRecommendedRooms(limit = 12) {
    return request({ url: '/api/v1/recommend/rooms', method: 'get', params: { limit } })
  },

  /**
   * 获取与指定直播间最相似的直播间（物品-物品余弦相似度）
   * @param {number} roomId 源房间 ID
   * @param {number} limit 返回数量
   */
  getSimilarRooms(roomId, limit = 6) {
    return request({ url: `/api/v1/recommend/similar-rooms/${roomId}`, method: 'get', params: { limit }, silentError: true })
  },
}
