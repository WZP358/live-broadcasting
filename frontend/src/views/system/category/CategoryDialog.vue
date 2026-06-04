<template>
  <a-modal :title="title" :open="visible" :confirm-loading="confirmLoading" width="720px" @ok="handleOk" @cancel="handleCancel">
    <div class="admin-modal-intro">
      <h4>分类配置</h4>
      <p>维护直播分类名称、图标和排序值，保证前台分类导航与后台运营配置一致。</p>
    </div>

    <a-form ref="formRef" class="admin-dialog-form" :model="formState" :rules="formRules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="分类名称" name="name">
            <a-input v-model:value="formState.name" placeholder="请输入分类名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="图标地址" name="icon">
            <a-input v-model:value="formState.icon" placeholder="请输入分类图标 URL" />
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
  title: { type: String, default: "编辑分类" },
  editData: { type: Object, default: () => ({}) },
})

const emit = defineEmits(["update:visible", "success"])
const formRef = ref()
const confirmLoading = ref(false)

const formState = reactive({
  id: null,
  name: "",
  icon: "",
  sort: 0,
  status: 0,
})

const formRules = {
  name: [{ required: true, message: "请输入分类名称", trigger: "blur" }],
}

watch(
  () => props.editData,
  (value) => {
    Object.assign(formState, {
      id: value?.id || null,
      name: value?.name || "",
      icon: value?.icon || "",
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
    name: "",
    icon: "",
    sort: 0,
    status: 0,
  })
}

const handleOk = async () => {
  try {
    await formRef.value.validate()
    confirmLoading.value = true
    await systemApi.save("category", { ...formState })
    message.success("分类信息已保存")
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
  background: var(--accent-light);
  border: 1px solid color-mix(in srgb, var(--accent) 22%, var(--border));
}

.admin-modal-intro h4 {
  margin: 0 0 6px;
  color: var(--text-primary);
  font-size: 16px;
}

.admin-modal-intro p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}
</style>
