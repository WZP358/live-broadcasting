import axios from 'axios'
import { useStore } from '@/stores'
import $modal from '@/utils/message'

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
        return Promise.reject(new Error(msg || 'Unauthorized'))
    }

    // 业务错误不在此处弹提示，由调用方决定如何展示
    return Promise.reject(new Error(msg || '请求失败'))
}, (error) => {
    const msg = error?.response?.data?.msg

    if (error?.response?.status === 401) {
        handleUnauthorized(msg)
        return Promise.reject(new Error(msg || 'Unauthorized'))
    }

    if (error?.config?.silentError) {
        return Promise.reject(new Error(msg || error.message || '网络异常'))
    }

    const errMsg = msg || error.message || ''
    if (errMsg === 'Network Error') {
        $modal.msgError('网络连接异常，请稍后重试')
    } else if (errMsg.includes('timeout')) {
        $modal.msgError('请求超时，请稍后重试')
    } else if (error?.response?.status >= 500) {
        $modal.msgError('服务器繁忙，请稍后重试')
    } else {
        $modal.msgError('网络异常，请稍后重试')
    }

    return Promise.reject(new Error(msg || error.message || '网络异常'))
})

const handleUnauthorized = (msg) => {
    localStorage.clear()
    sessionStorage.clear()

    if (redirectingToLogin) {
        return
    }

    redirectingToLogin = true
    $modal.msgWarning(msg || '登录已过期，请重新登录')

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
