<template>
  <AdminPageLayout title="字典管理" description="统一维护字典类型、标签和值，页面结构与后台其他列表页保持一致，方便继续扩展导入导出和批量操作。">
    <AdminCard title="筛选条件" subtitle="支持按类型名称和标签快速检索字典项">
      <a-form ref="formRef" :model="formState" @finish="handleSearch">
        <div class="admin-query-grid">
          <a-form-item label="类型名称" name="typeName">
            <a-input v-model:value="formState.typeName" placeholder="请输入类型名称" />
          </a-form-item>
          <a-form-item label="标签" name="label">
            <a-input v-model:value="formState.label" placeholder="请输入标签" />
          </a-form-item>
          <div class="admin-query-actions">
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </div>
        </div>
      </a-form>
    </AdminCard>

    <AdminCard title="字典列表" subtitle="统一维护类型、标签、排序和启停状态">
      <AdminToolbar>
        <template #left>
          <a-space>
            <a-button type="primary" @click="handleAdd">新增字典</a-button>
            <a-button @click="getData">刷新</a-button>
          </a-space>
        </template>
        <template #right>
          <span>适合维护状态枚举、配置标签和运营规则映射</span>
        </template>
      </AdminToolbar>

      <div class="admin-list-meta">
        <span class="admin-list-meta__highlight">当前结果 {{ total }} 条</span>
        <span>字段包含类型、标签、值、排序和启停状态</span>
      </div>

      <div ref="containerRef" class="admin-table-shell">
        <a-table :columns="columns" :data-source="dataSource" :pagination="false" :scroll="{ x: 1220, y: tableScrollY }" row-key="id" size="middle">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'index'">
              {{ index + 1 }}
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="Number(record.status) === 0 ? 'success' : 'error'">
                {{ Number(record.status) === 0 ? "启用" : "禁用" }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a @click="handleEdit(record)">编辑</a>
                <a style="color: #dc2626" @click="handleDelete(record)">删除</a>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>

      <AdminPagination :total="total" :current="current" :page-size="pageSize" @change="handlePageChange" />
    </AdminCard>

    <a-modal
      v-model:open="modalVisible"
      :title="modalTitle"
      :confirm-loading="confirmLoading"
      width="680px"
      @ok="handleModalOk"
      @cancel="handleModalCancel"
    >
      <a-form ref="modalFormRef" :model="modalFormState" :rules="modalFormRules" layout="vertical" class="admin-dialog-form">
        <a-row :gutter="16">
          <a-col :span="12">
            <a-form-item label="字典类型" name="type">
              <a-input v-model:value="modalFormState.type" placeholder="请输入字典类型" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="类型名称" name="typeName">
              <a-input v-model:value="modalFormState.typeName" placeholder="请输入类型名称" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="标签" name="label">
              <a-input v-model:value="modalFormState.label" placeholder="请输入标签" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="标签值" name="value">
              <a-input v-model:value="modalFormState.value" placeholder="请输入标签值" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="排序" name="sort">
              <a-input-number v-model:value="modalFormState.sort" :min="0" style="width: 100%" />
            </a-form-item>
          </a-col>
          <a-col :span="12">
            <a-form-item label="状态" name="status">
              <a-select v-model:value="modalFormState.status" placeholder="请选择状态">
                <a-select-option :value="0">启用</a-select-option>
                <a-select-option :value="-1">禁用</a-select-option>
              </a-select>
            </a-form-item>
          </a-col>
          <a-col :span="24">
            <a-form-item label="描述" name="description">
              <a-textarea v-model:value="modalFormState.description" :rows="3" placeholder="请输入描述信息" />
            </a-form-item>
          </a-col>
        </a-row>
      </a-form>
    </a-modal>
  </AdminPageLayout>
</template>

<script setup>
import { createVNode, onMounted, reactive, ref } from "vue"
import { ExclamationCircleOutlined } from "@ant-design/icons-vue"
import { message, Modal } from "ant-design-vue"
import AdminPageLayout from "@/components/admin/AdminPageLayout.vue"
import AdminCard from "@/components/admin/AdminCard.vue"
import AdminToolbar from "@/components/admin/AdminToolbar.vue"
import AdminPagination from "@/components/admin/AdminPagination.vue"
import { useTableScroll } from "@/composables/useTableScroll"
import systemApi from "@/api/system"

const formRef = ref()
const modalFormRef = ref()
const { containerRef, tableScrollY } = useTableScroll()

const formState = reactive({
  typeName: "",
  label: "",
})

const columns = [
  { title: "序号", dataIndex: "index", key: "index", width: 80, align: "center" },
  { title: "字典类型", dataIndex: "type", key: "type", width: 140 },
  { title: "类型名称", dataIndex: "typeName", key: "typeName", width: 180 },
  { title: "标签", dataIndex: "label", key: "label", width: 160 },
  { title: "标签值", dataIndex: "value", key: "value", width: 160 },
  { title: "排序", dataIndex: "sort", key: "sort", width: 100 },
  { title: "状态", dataIndex: "status", key: "status", width: 110 },
  { title: "描述", dataIndex: "description", key: "description" },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 },
  { title: "操作", key: "action", width: 140, align: "center", fixed: "right" },
]

const dataSource = ref([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)

const modalVisible = ref(false)
const modalTitle = ref("新增字典")
const confirmLoading = ref(false)
const modalFormState = reactive({
  id: undefined,
  type: "",
  typeName: "",
  label: "",
  value: "",
  sort: 0,
  status: 0,
  description: "",
})

const modalFormRules = {
  type: [{ required: true, message: "请输入字典类型", trigger: "blur" }],
  typeName: [{ required: true, message: "请输入类型名称", trigger: "blur" }],
  label: [{ required: true, message: "请输入标签", trigger: "blur" }],
  value: [{ required: true, message: "请输入标签值", trigger: "blur" }],
  sort: [{ required: true, message: "请输入排序", trigger: "change" }],
  status: [{ required: true, message: "请选择状态", trigger: "change" }],
}

const resetModalForm = () => {
  Object.assign(modalFormState, {
    id: undefined,
    type: "",
    typeName: "",
    label: "",
    value: "",
    sort: 0,
    status: 0,
    description: "",
  })
  modalFormRef.value?.clearValidate()
}

const getData = async () => {
  const res = await systemApi.page("dict", {
    pageNo: current.value,
    pageSize: pageSize.value,
    ...formState,
  })
  total.value = Number(res?.data?.total || 0)
  dataSource.value = res?.data?.list || []
}

const handleSearch = () => {
  current.value = 1
  getData()
}

const handleReset = () => {
  formRef.value?.resetFields()
  current.value = 1
  getData()
}

const handlePageChange = (page, size) => {
  current.value = page
  pageSize.value = size
  getData()
}

const handleAdd = () => {
  modalTitle.value = "新增字典"
  resetModalForm()
  modalVisible.value = true
}

const handleEdit = (record) => {
  modalTitle.value = "编辑字典"
  resetModalForm()
  Object.assign(modalFormState, { ...record })
  modalVisible.value = true
}

const handleModalCancel = () => {
  modalVisible.value = false
  resetModalForm()
}

const handleModalOk = async () => {
  await modalFormRef.value?.validate()
  confirmLoading.value = true
  try {
    await systemApi.save("dict", { ...modalFormState })
    message.success(`${modalFormState.id ? "编辑" : "新增"}成功`)
    modalVisible.value = false
    getData()
  } finally {
    confirmLoading.value = false
  }
}

const handleDelete = (record) => {
  Modal.confirm({
    title: "确认删除该字典项吗？",
    icon: createVNode(ExclamationCircleOutlined),
    content: `删除对象：${record.label}`,
    okType: "danger",
    okText: "确认删除",
    cancelText: "取消",
    onOk: async () => {
      await systemApi.delete("dict", [record.id])
      message.success("删除成功")
      getData()
    },
  })
}

onMounted(() => {
  getData()
})
</script>
