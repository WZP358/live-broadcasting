<template>
  <a-modal :title="title" :open="visible" :confirm-loading="confirmLoading" width="760px" @ok="handleOk" @cancel="handleCancel">
    <div class="admin-modal-intro">
      <h4>用户档案编辑</h4>
      <p>集中维护账号、昵称、联系方式和头像资料，便于后台统一管理用户基础档案。</p>
    </div>

    <a-form ref="formRef" class="admin-dialog-form" :model="formState" :rules="formRules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="账号" name="username">
            <a-input v-model:value="formState.username" placeholder="请输入账号" :disabled="Boolean(formState.id)" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="昵称" name="nickname">
            <a-input v-model:value="formState.nickname" placeholder="请输入昵称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="性别" name="sex">
            <a-select v-model:value="formState.sex" placeholder="请选择性别">
              <a-select-option :value="0">未知</a-select-option>
              <a-select-option :value="1">男</a-select-option>
              <a-select-option :value="2">女</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="手机" name="mobile">
            <a-input v-model:value="formState.mobile" placeholder="请输入手机号" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="邮箱" name="email">
            <a-input v-model:value="formState.email" placeholder="请输入邮箱" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="头像地址" name="avatar">
            <a-input v-model:value="formState.avatar" placeholder="请输入头像 URL" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="个性签名" name="signature">
            <a-textarea v-model:value="formState.signature" :rows="3" placeholder="请输入个性签名" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref, watch } from "vue"
import { message } from "ant-design-vue"
import systemUserApi from "@/api/systemUser"

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: "编辑用户" },
  editData: { type: Object, default: () => ({}) },
})

const emit = defineEmits(["update:visible", "success"])
const formRef = ref()
const confirmLoading = ref(false)

const formState = reactive({
  id: null,
  username: "",
  nickname: "",
  sex: 0,
  mobile: "",
  email: "",
  avatar: "",
  signature: "",
})

const formRules = {
  username: [
    { required: true, message: "请输入账号", trigger: "blur" },
    { min: 3, max: 20, message: "账号长度为 3 到 20 个字符", trigger: "blur" },
  ],
  nickname: [{ required: true, message: "请输入昵称", trigger: "blur" }],
  mobile: [{ pattern: /^$|^1[3-9]\d{9}$/, message: "请输入正确的手机号", trigger: "blur" }],
  email: [{ type: "email", message: "请输入正确的邮箱地址", trigger: "blur" }],
}

watch(
  () => props.editData,
  (value) => {
    Object.assign(formState, {
      id: value?.id || null,
      username: value?.username || "",
      nickname: value?.nickname || "",
      sex: value?.sex ?? 0,
      mobile: value?.mobile || "",
      email: value?.email || "",
      avatar: value?.avatar || "",
      signature: value?.signature || "",
    })
  },
  { immediate: true, deep: true }
)

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(formState, {
    id: null,
    username: "",
    nickname: "",
    sex: 0,
    mobile: "",
    email: "",
    avatar: "",
    signature: "",
  })
}

const handleOk = async () => {
  try {
    await formRef.value.validate()
    confirmLoading.value = true
    await systemUserApi.saveUser({ ...formState })
    message.success("用户信息已保存")
    emit("update:visible", false)
    emit("success")
    resetForm()
  } catch (error) {
    if (error?.errorFields) {
      return
    }
    message.error(`保存失败：${error?.message || "未知错误"}`)
  } finally {
    confirmLoading.value = false
  }
}

const handleCancel = () => {
  emit("update:visible", false)
  resetForm()
}
</script>

<style scoped lang="scss">
.admin-modal-intro {
  margin-bottom: 18px;
  padding: 16px 18px;
  border-radius: 16px;
  background: #f8fbff;
  border: 1px solid #dbeafe;
}

.admin-modal-intro h4 {
  margin: 0 0 6px;
  color: #0f172a;
  font-size: 16px;
}

.admin-modal-intro p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}
</style>
