<template>
  <a-modal :title="title" :open="visible" :confirm-loading="confirmLoading" width="820px" @ok="handleOk" @cancel="handleCancel">
    <div class="admin-modal-intro">
      <h4>直播间信息编辑</h4>
      <p>维护直播间标题、分类、封面、简介和公告，保证主播侧与后台内容配置一致。</p>
    </div>

    <a-form ref="formRef" class="admin-dialog-form" :model="formState" :rules="formRules" layout="vertical">
      <a-row :gutter="16">
        <a-col :span="12">
          <a-form-item label="直播间标题" name="title">
            <a-input v-model:value="formState.title" placeholder="请输入直播间标题" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="分类" name="categoryId">
            <a-select v-model:value="formState.categoryId" placeholder="请选择分类">
              <a-select-option v-for="item in categoryOptions" :key="item.id" :value="item.id">
                {{ item.name }}
              </a-select-option>
            </a-select>
          </a-form-item>
        </a-col>
        <a-col :span="24">
          <a-form-item label="封面地址" name="cover">
            <a-input v-model:value="formState.cover" placeholder="请输入封面 URL" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="直播间简介" name="introduce">
            <a-textarea v-model:value="formState.introduce" :rows="4" placeholder="请输入直播间简介" />
          </a-form-item>
        </a-col>
        <a-col :span="12">
          <a-form-item label="直播公告" name="notice">
            <a-textarea v-model:value="formState.notice" :rows="4" placeholder="请输入直播公告" />
          </a-form-item>
        </a-col>
      </a-row>
    </a-form>
  </a-modal>
</template>

<script setup>
import { reactive, ref, watch } from "vue"
import { message } from "ant-design-vue"
import systemRoomApi from "@/api/systemRoom"

const props = defineProps({
  visible: { type: Boolean, default: false },
  title: { type: String, default: "编辑直播间" },
  editData: { type: Object, default: () => ({}) },
  categoryOptions: { type: Array, default: () => [] },
})

const emit = defineEmits(["update:visible", "success"])
const formRef = ref()
const confirmLoading = ref(false)

const formState = reactive({
  id: null,
  title: "",
  categoryId: undefined,
  cover: "",
  introduce: "",
  notice: "",
})

const formRules = {
  title: [{ required: true, message: "请输入直播间标题", trigger: "blur" }],
  categoryId: [{ required: true, message: "请选择分类", trigger: "change" }],
}

watch(
  () => props.editData,
  (value) => {
    Object.assign(formState, {
      id: value?.id || null,
      title: value?.title || "",
      categoryId: value?.categoryId,
      cover: value?.cover || "",
      introduce: value?.introduce || "",
      notice: value?.notice || "",
    })
  },
  { immediate: true, deep: true }
)

const resetForm = () => {
  formRef.value?.resetFields()
  Object.assign(formState, {
    id: null,
    title: "",
    categoryId: undefined,
    cover: "",
    introduce: "",
    notice: "",
  })
}

const handleOk = async () => {
  try {
    await formRef.value.validate()
    confirmLoading.value = true
    await systemRoomApi.saveRoom({ ...formState })
    message.success("直播间信息已保存")
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
