import axios from 'axios'
import { useStore } from '@/stores'
import { Modal, message } from 'ant-design-vue'
import { createVNode } from 'vue'
import { ExclamationCircleOutlined } from '@ant-design/icons-vue'

const service = axios.create({
    baseURL: '',
    timeout: 10 * 1000,
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
        localStorage.clear()
    } else {
        message.error(msg || 'This is an error message', 2.5)
    }

    return Promise.reject(new Error(msg || 'Error'))
}, (error) => {
    const msg = error?.response?.data?.msg

    if (error?.response?.status === 401) {
        Modal.confirm({
            title: '登录提示',
            icon: createVNode(ExclamationCircleOutlined),
            content: '您的登录状态已过期，请重新登录',
            cancelText: '取消',
            okText: '重新登录',
            onOk() {
                localStorage.clear()
                window.location.href = '/#/login'
            },
            onCancel() {},
        })
        return Promise.reject(new Error(msg || 'Unauthorized'))
    }

    message.error(msg || '网络异常，请稍后重试', 2.5)
    return Promise.reject(new Error(msg || 'Network Error'))
})

export default service
