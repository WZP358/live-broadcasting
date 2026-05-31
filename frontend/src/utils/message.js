import { message, Modal, notification } from 'ant-design-vue'
import { h } from 'vue'

const DEFAULT_DURATION = 3

const msg = (content, type = 'info', duration = DEFAULT_DURATION) => {
  message[type](String(content), duration)
}

const notify = (content, type = 'info', title = '系统提示') => {
  notification[type]({
    message: title,
    description: String(content),
    duration: 4.5,
  })
}

/**
 * 仿若依框架 $modal 风格的消息工具。
 * 主站使用 ant-design-vue，此处封装统一入口，
 * 调用方无需关心底层 UI 库差异。
 */
export default {
  // ---- 轻提示 (message) ----
  msg(content) {
    msg(content, 'info')
  },
  msgSuccess(content) {
    msg(content, 'success')
  },
  msgError(content) {
    msg(content, 'error', 4)
  },
  msgWarning(content) {
    msg(content, 'warning')
  },
  msgLoading(content) {
    msg(content, 'loading', 0)
  },

  // ---- 通知提示 (notification) ----
  notify(content) {
    notify(content, 'info')
  },
  notifySuccess(content) {
    notify(content, 'success')
  },
  notifyError(content) {
    notify(content, 'error')
  },
  notifyWarning(content) {
    notify(content, 'warning')
  },

  // ---- 确认弹窗 ----
  confirm(content, { title = '系统提示', okText = '确定', cancelText = '取消' } = {}) {
    return new Promise((resolve, reject) => {
      Modal.confirm({
        title,
        content: h('div', { style: { wordBreak: 'break-all' } }, String(content)),
        okText,
        cancelText,
        onOk: () => resolve(true),
        onCancel: () => reject(new Error('cancel')),
      })
    })
  },

  // ---- 模态弹窗 ----
  alert(content) {
    return Modal.info({ title: '系统提示', content: String(content) })
  },
  alertSuccess(content) {
    return Modal.success({ title: '系统提示', content: String(content) })
  },
  alertError(content) {
    return Modal.error({ title: '系统提示', content: String(content) })
  },
  alertWarning(content) {
    return Modal.warning({ title: '系统提示', content: String(content) })
  },
}
