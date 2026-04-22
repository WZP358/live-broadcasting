<template>
  <a-modal :title="title" :open="visible" :confirm-loading="confirmLoading" width="760px" @ok="handleOk" @cancel="handleCancel">
    <div class="admin-modal-intro">
      <h4>礼物配置</h4>
      <p>统一维护礼物名称、价格、图标和上下架状态，方便礼物运营与商城扩展。</p>
    </div>

    <a-form ref="formRef" class="admin-dialog-form" :model="formState" :rules="formRules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="礼物名称" name="name">
            <a-input v-model:value="formState.name" placeholder="请输入礼物名称" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="图标地址" name="icon">
            <a-input v-model:value="formState.icon" placeholder="请输入礼物图标 URL" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="礼物价格" name="price">
            <a-input-number v-model:value="formState.price" :min="0" :precision="2" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="排序值" name="sort">
            <a-input-number v-model:value="formState.sort" :min="0" style="width: 100%" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="状态" name="disabled">
            <a-select v-model:value="formState.disabled">
              <a-select-option :value="0">启用</a-select-option>
              <a-select-option :value="-1">禁用</a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="描述" name="description">
            <a-textarea v-model:value="formState.description" :rows="3" placeholder="请输入礼物描述" />
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
  title: { type: String, default: "编辑礼物" },
  editData: { type: Object, default: () => ({}) },
})

const emit = defineEmits(["update:visible", "success"])
const formRef = ref()
const confirmLoading = ref(false)

const formState = reactive({
  id: null,
  name: "",
  icon: "",
  price: 0,
  description: "",
  sort: 0,
  disabled: 0,
})

const formRules = {
  name: [{ required: true, message: "请输入礼物名称", trigger: "blur" }],
  price: [{ required: true, message: "请输入礼物价格", trigger: "blur" }],
}

watch(
  () => props.editData,
  (value) => {
    Object.assign(formState, {
      id: value?.id || null,
      name: value?.name || "",
      icon: value?.icon || "",
      price: value?.price ?? 0,
      description: value?.description || "",
      sort: value?.sort ?? 0,
      disabled: value?.disabled ?? 0,
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
    price: 0,
    description: "",
    sort: 0,
    disabled: 0,
  })
}

const handleOk = async () => {
  try {
    await formRef.value.validate()
    confirmLoading.value = true
    await systemApi.save("gift", { ...formState })
    message.success("礼物信息已保存")
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
