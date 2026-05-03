import axios from 'axios'
import { useStore } from '@/stores'
import { message } from 'ant-design-vue'

let redirectingToLogin = false

const service = axios.create({
    baseURL: '',
    timeout: 60 * 1000,
    headers: { 'Content-Type': 'application/json;charset=utf-8' },
})

service.interceptors.request.use((config) => {
    if (!config.headers) {
        throw new Error(`Expected 'config' and 'config.headers' not to be undefined`)
    }

    const store = useStore()
    const userToken = store.user().userToken
    if (userToken) {
        config.headers.Authorization = userToken
    }

    return config
}, (error) => Promise.reject(error))

service.interceptors.response.use((response) => {
    const res = response.data
    const { code, msg } = res

    if (code === 0) {
        return res
    }

    if (code === 401) {
        handleUnauthorized(msg)
    } else {
        message.error(msg || '请求失败', 2.5)
    }

    return Promise.reject(new Error(msg || 'Error'))
}, (error) => {
    const msg = error?.response?.data?.msg

    if (error?.response?.status === 401) {
        handleUnauthorized(msg)
        return Promise.reject(new Error(msg || 'Unauthorized'))
    }

    message.error(msg || '网络异常，请稍后重试', 2.5)
    return Promise.reject(new Error(msg || 'Network Error'))
})

const handleUnauthorized = (msg) => {
    localStorage.clear()
    sessionStorage.clear()

    if (redirectingToLogin) {
        return
    }

    redirectingToLogin = true
    message.warning(msg || '登录已过期，请重新登录', 2)

    const currentHash = window.location.hash || '#/'
    const currentPath = currentHash.replace(/^#/, '') || '/'
    const redirect = currentPath && currentPath !== '/login'
        ? `?redirect=${encodeURIComponent(currentPath)}`
        : ''

    setTimeout(() => {
        window.location.href = `/#/login${redirect}`
    }, 300)
}

export default service
