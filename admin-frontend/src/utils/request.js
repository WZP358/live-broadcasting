import axios from 'axios'
import { Message, MessageBox, Notification } from 'element-ui'
import store from '@/store'
import { getToken } from '@/utils/auth'
import { tansParams } from '@/utils/ruoyi'

export let isRelogin = { show: false }

axios.defaults.headers['Content-Type'] = 'application/json;charset=utf-8'

const service = axios.create({
  baseURL: process.env.VUE_APP_BASE_API,
  timeout: 30000
})

service.interceptors.request.use(config => {
  const isToken = (config.headers || {}).isToken === false
  if (getToken() && !isToken) {
    config.headers.Authorization = 'Bearer ' + getToken()
  }

  if (config.method === 'get' && config.params) {
    let url = config.url + '?' + tansParams(config.params)
    url = url.slice(0, -1)
    config.params = {}
    config.url = url
  }

  return config
}, error => Promise.reject(error))

service.interceptors.response.use(response => {
  if (response.request.responseType === 'blob' || response.request.responseType === 'arraybuffer') {
    return response.data
  }

  const body = response.data || {}
  const code = body.code === undefined ? 200 : body.code
  const msg = body.msg || '操作失败'

  if (code === 0 || code === 200) {
    return body
  }

  if (code === 401) {
    if (!isRelogin.show) {
      isRelogin.show = true
      MessageBox.confirm('登录状态已过期，您可以继续留在该页面，或者重新登录', '系统提示', {
        confirmButtonText: '重新登录',
        cancelButtonText: '取消',
        type: 'warning'
      }).then(() => {
        isRelogin.show = false
        store.dispatch('LogOut').then(() => {
          location.href = '/index'
        })
      }).catch(() => {
        isRelogin.show = false
      })
    }
    return Promise.reject(new Error(msg))
  }

  Notification.error({ title: msg })
  return Promise.reject(new Error(msg))
}, error => {
  let message = error.message || '网络异常'
  if (message === 'Network Error') {
    message = '后端服务连接异常'
  } else if (message.includes('timeout')) {
    message = '系统接口请求超时'
  } else if (message.includes('Request failed with status code')) {
    message = '系统接口' + message.slice(-3) + '异常'
  }
  Message({ message, type: 'error', duration: 5 * 1000 })
  return Promise.reject(error)
})

export default service

export function download() {
  Message.warning('下载能力未接入 PulseLive 后端。')
}
