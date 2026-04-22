<template>
  <a-modal v-model:open="visible" title="信息绑定">
    <a-form
      v-if="!bind"
      class="form"
      :model="formState"
      :label-col="{ span: 6 }"
      :wrapper-col="{ span: 14 }"
      autocomplete="off"
      @finish="onFinish"
      ref="formRef">
      <a-form-item
        label="手机号码"
        name="target"
        :rules="[
          { required: true, message: '请输入手机号码!' },
          { len: 11, message: '请输入正确的手机号码!' },
        ]">
        <a-flex gap="small">
          <a-input v-model:value="formState.target" />
          <a-button :disabled="sendBtnDisaled" style="width: 90px" @click="onSend" type="primary">
            {{ !sendBtnDisaled ? "发送" : `${countdown}秒` }}
          </a-button>
        </a-flex>
      </a-form-item>
      <a-form-item label="验证码" name="verifyCode" :rules="[{ required: true, message: '请输入验证码!' }]">
        <a-input v-model:value="formState.verifyCode" />
      </a-form-item>
      <a-form-item :wrapper-col="{ offset: 6, span: 14 }">
        <a-button type="primary" html-type="submit">绑定</a-button>
      </a-form-item>
    </a-form>
    <a-result v-else status="success" title="已绑定！" sub-title="如果需要重新绑定，请联系人工客服。"></a-result>
    <template #footer> </template>
  </a-modal>
</template>

<script setup>
import commonApi from "@/api/common"
import { message } from "ant-design-vue"
import { ref, reactive } from "vue"

let timer = null
const formRef = ref()
const visible = ref(false)
const countdown = ref(10)
const sendBtnDisaled = ref(false)
const formState = reactive({
  target: "",
  verifyCode: "",
})

const props = defineProps({
  bind: {
    type: Boolean,
    default: false,
  },
})

const show = () => {
  visible.value = true
  formRef.value.resetFields()
}

const onSend = async () => {
  if (timer) {
    return
  }
  try {
    await formRef.value.validateFields(["target"])
  } catch (errorInfo) {
    return
  }
  sendBtnDisaled.value = true
  let res = await commonApi.sendVerifyCode({ verifyType: "phone", target: formState.target })
  if (res.code === 0) {
    message.success("发送成功")
  }
  startTimer()
}

const startTimer = () => {
  timer = setInterval(() => {
    if (countdown.value == 1) {
      sendBtnDisaled.value = false
      timer && clearInterval(timer)
      timer = null
      countdown.value = 10
    } else {
      countdown.value--
    }
  }, 1000)
}

const onFinish = (values) => {
  console.log("Success:", values)
}

defineExpose({ show })
</script>

<style lang="scss" scoped>
.form {
  margin-top: 30px;
}
</style>
