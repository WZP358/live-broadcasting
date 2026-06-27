<template>
  <a-modal
    v-model:open="visible"
    title="修改登录密码"
    :confirm-loading="submitting"
    ok-text="保存"
    cancel-text="取消"
    @ok="submit"
    @cancel="close"
  >
    <a-form ref="formRef" :model="formState" :rules="rules" layout="vertical" autocomplete="off" class="password-form">
      <a-form-item label="原密码" name="oldPassword">
        <a-input-password v-model:value="formState.oldPassword" placeholder="请输入当前登录密码" autocomplete="current-password" />
      </a-form-item>
      <a-form-item label="新密码" name="newPassword">
        <a-input-password v-model:value="formState.newPassword" placeholder="6-24 位，建议包含字母和数字" autocomplete="new-password" />
      </a-form-item>
      <a-form-item label="确认新密码" name="confirmPassword">
        <a-input-password v-model:value="formState.confirmPassword" placeholder="请再次输入新密码" autocomplete="new-password" @keyup.enter="submit" />
      </a-form-item>
    </a-form>
  </a-modal>
</template>

<script setup>
import { nextTick, reactive, ref } from "vue"
import userApi from "@/api/user"
import { useStore } from "@/stores"
import $modal from "@/utils/message"

const visible = ref(false)
const submitting = ref(false)
const formRef = ref()
const store = useStore()

const formState = reactive({
  oldPassword: "",
  newPassword: "",
  confirmPassword: "",
})

const validateConfirmPassword = async (_rule, value) => {
  if (!value) {
    return Promise.reject("请再次输入新密码")
  }
  if (value !== formState.newPassword) {
    return Promise.reject("两次输入的新密码不一致")
  }
  return Promise.resolve()
}

const rules = {
  oldPassword: [{ required: true, message: "请输入原密码" }],
  newPassword: [
    { required: true, message: "请输入新密码" },
    { min: 6, max: 24, message: "新密码长度为 6-24 位" },
  ],
  confirmPassword: [{ validator: validateConfirmPassword, trigger: "change" }],
}

const resetForm = () => {
  formState.oldPassword = ""
  formState.newPassword = ""
  formState.confirmPassword = ""
  formRef.value?.clearValidate?.()
}

const show = async () => {
  visible.value = true
  await nextTick()
  resetForm()
}

const close = () => {
  visible.value = false
  resetForm()
}

const submit = async () => {
  try {
    await formRef.value?.validate?.()
  } catch (error) {
    return
  }
  submitting.value = true
  try {
    await userApi.updatePassword({
      oldPassword: formState.oldPassword,
      newPassword: formState.newPassword,
      confirmPassword: formState.confirmPassword,
    })
    visible.value = false
    $modal.msgSuccess("密码已修改，请重新登录")
    setTimeout(() => {
      store.user().logout()
    }, 800)
  } catch (error) {
    $modal.msgError(error?.message || "密码修改失败")
  } finally {
    submitting.value = false
  }
}

defineExpose({ show })
</script>

<style lang="scss" scoped>
.password-form {
  padding-top: 8px;
}
</style>
