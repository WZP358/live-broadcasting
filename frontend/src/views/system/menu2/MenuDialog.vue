<template>
  <a-modal :title="title" :open="visible" :confirm-loading="confirmLoading" width="760px" @ok="handleOk" @cancel="handleCancel">
    <div class="admin-modal-intro">
      <h4>菜单配置</h4>
      <p>维护后台菜单层级、图标、路由路径和可用状态，保证管理端导航结构清晰可扩展。</p>
    </div>

    <a-form ref="formRef" class="admin-dialog-form" :model="formState" :rules="formRules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="菜单名称" name="title">
            <a-input v-model:value="formState.title" placeholder="请输入菜单名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="父级菜单" name="pid">
            <a-select v-model:value="formState.pid" placeholder="请选择父级菜单" allow-clear>
              <a-select-option :value="0">根菜单</a-select-option>
              <a-select-option v-for="menu in menuOptions" :key="menu.id" :value="menu.id">
                {{ menu.label }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="图标" name="icon">
            <a-input v-model:value="formState.icon" placeholder="请输入图标标识" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="路由路径" name="path">
            <a-input v-model:value="formState.path" placeholder="请输入路由路径" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="排序值" name="sort">
            <a-input-number v-model:value="formState.sort" :min="0" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态" name="status">
            <a-select v-model:value="formState.status">
              <a-select-option :value="0">启用</a-select-option>
              <a-select-option :value="-1">禁用</a-select-option>
            </a-select>
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
  title: { type: String, default: "编辑菜单" },
  editData: { type: Object, default: () => ({}) },
  menuOptions: { type: Array, default: () => [] },
})

const emit = defineEmits(["update:visible", "success"])
const formRef = ref()
const confirmLoading = ref(false)

const formState = reactive({
  id: null,
  title: "",
  pid: 0,
  icon: "",
  path: "",
  sort: 0,
  status: 0,
})

const formRules = {
  title: [{ required: true, message: "请输入菜单名称", trigger: "blur" }],
  path: [{ required: true, message: "请输入路由路径", trigger: "blur" }],
}

watch(
  () => props.editData,
  (value) => {
    Object.assign(formState, {
      id: value?.id || null,
      title: value?.title || "",
      pid: value?.pid ?? 0,
      icon: value?.icon || "",
      path: value?.path || "",
      sort: value?.sort ?? 0,
      status: value?.status ?? 0,
    })
  },
  { immediate: true, deep: true }
)

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(formState, {
    id: null,
    title: "",
    pid: 0,
    icon: "",
    path: "",
    sort: 0,
    status: 0,
  })
}

const handleOk = async () => {
  try {
    await formRef.value.validate()
    confirmLoading.value = true
    await systemApi.save("menu", { ...formState })
    message.success("菜单信息已保存")
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
