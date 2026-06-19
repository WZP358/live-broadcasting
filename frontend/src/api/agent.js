import request from '@/utils/request'

const AGENT_BASE = '/api/v1/agent'

export default {
  /**
   * 弹幕情感分析
   * @param {Array} messages - [{username, content}, ...]
   */
  analyzeSentiment(messages) {
    return request({
      url: `${AGENT_BASE}/sentiment`,
      method: 'post',
      data: { messages },
    })
  },

  /**
   * 生成直播摘要 + 标签 + 欢迎语
   * @param {Object} params - {title, category, anchorName, highlights}
   */
  generateSummary(params) {
    return request({
      url: `${AGENT_BASE}/summarize`,
      method: 'post',
      data: {
        title: params.title,
        category: params.category,
        anchor_name: params.anchorName || params.anchor_name,
        highlights: params.highlights || [],
      },
    }).then((res) => {
      const data = res?.data || {}
      return {
        ...res,
        data: {
          ...data,
          welcomeMsg: data.welcomeMsg || data.welcome_msg || '',
        },
      }
    })
  },

  /**
   * 平台小助手问答
   * @param {string} question
   */
  askHelper(question, context = {}) {
    return request({
      url: `${AGENT_BASE}/helper`,
      method: 'post',
      data: { question, context },
    })
  },

  /**
   * 小助手状态检查
   */
  healthCheck() {
    return request({
      url: `${AGENT_BASE}/health`,
      method: 'get',
    })
  },
}
