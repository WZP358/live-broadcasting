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
        label="邮箱"
        name="target"
        :rules="[
          { required: true, message: '请输入邮箱!' },
          { type: 'email', message: '请输入有效的邮箱!' },
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
import { ref, reactive } from "vue"

let timer = null
const formRef = ref()
const countdown = ref(10)
const visible = ref(false)
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
  sendBtnDisaled.value = true
  //   let res = await commonApi.sendVerifyCode({ verifyType: "email", target: formState.username })
  startTimer()
}

const startTimer = () => {
  timer = setInterval(() => {
    if (countdown.value == 0) {
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
