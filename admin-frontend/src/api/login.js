import request from '@/utils/request'

export function login(username, password) {
  return request({
    url: '/api/login',
    headers: {
      isToken: false
    },
    method: 'post',
    data: { username, password }
  })
}

export function getInfo() {
  const cached = localStorage.getItem('PulseLive-Admin-User')
  const user = cached ? JSON.parse(cached) : {}
  return Promise.resolve({
    code: 0,
    user: {
      userId: user.userId || user.id || 1,
      userName: user.username || 'admin',
      nickName: user.nickName || user.nickname || '管理员',
      avatar: user.avatar || ''
    },
    roles: ['admin'],
    permissions: ['*:*:*']
  })
}

export function logout() {
  return Promise.resolve({ code: 0 })
}

export function getCodeImg() {
  return Promise.resolve({ captchaEnabled: false })
}
