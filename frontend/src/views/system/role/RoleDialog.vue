<template>
  <a-modal :title="title" :open="visible" :confirm-loading="confirmLoading" width="720px" @ok="handleOk" @cancel="handleCancel">
    <div class="admin-modal-intro">
      <h4>角色基础信息</h4>
      <p>维护后台角色名称、级别和权限说明，方便后续继续扩展菜单授权与页面权限控制。</p>
    </div>

    <a-form ref="formRef" class="admin-dialog-form" :model="formState" :rules="formRules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="14">
          <a-form-item label="角色名称" name="name">
            <a-input v-model:value="formState.name" placeholder="请输入角色名称" />
          </a-form-item>
        </a-col>
        <a-col :span="10">
          <a-form-item label="角色级别" name="level">
            <a-input-number v-model:value="formState.level" :min="1" :max="100" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="权限描述" name="permission">
            <a-textarea v-model:value="formState.permission" :rows="4" placeholder="请输入权限描述" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref, watch } from "vue"
import { message } from "ant-design-vue"
import systemApi from "@/api/system"

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: "编辑角色" },
  editData: { type: Object, default: () => ({}) },
})

const emit = defineEmits(["update:visible", "success"])
const formRef = ref()
const confirmLoading = ref(false)

const formState = reactive({
  id: null,
  name: "",
  level: 1,
  permission: "",
})

const formRules = {
  name: [{ required: true, message: "请输入角色名称", trigger: "blur" }],
  level: [{ required: true, message: "请输入角色级别", trigger: "blur" }],
  permission: [{ required: true, message: "请输入权限描述", trigger: "blur" }],
}

watch(
  () => props.editData,
  (value) => {
    Object.assign(formState, {
      id: value?.id || null,
      name: value?.name || "",
      level: value?.level ?? 1,
      permission: value?.permission || "",
    })
  },
  { immediate: true, deep: true }
)

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(formState, {
    id: null,
    name: "",
    level: 1,
    permission: "",
  })
}

const handleOk = async () => {
  try {
    await formRef.value.validate()
    confirmLoading.value = true
    await systemApi.save("role", { ...formState })
    message.success("角色信息已保存")
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
