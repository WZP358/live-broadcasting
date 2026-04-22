<template>
  <AdminPageLayout title="认证管理" description="集中处理实名认证审核，兼容旧接口但页面结构已经统一到新的后台框架中。">
    <AdminCard title="筛选条件" subtitle="可按姓名、证件号和审核状态筛查">
      <a-form ref="formRef" :model="formState" @finish="handleSearch">
        <div class="admin-query-grid">
          <a-form-item label="真实姓名" name="realName">
            <a-input v-model:value="formState.realName" placeholder="请输入真实姓名" />
          </a-form-item>
          <a-form-item label="身份证号" name="cardNo">
            <a-input v-model:value="formState.cardNo" placeholder="请输入身份证号" />
          </a-form-item>
          <a-form-item label="审核状态" name="status">
            <a-select v-model:value="formState.status" :options="statusOptions" allow-clear placeholder="请选择审核状态" />
          </a-form-item>
          <div class="admin-query-actions">
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </div>
        </div>
      </a-form>
    </AdminCard>

    <AdminCard title="认证记录" subtitle="支持批量通过、驳回和重置状态">
      <AdminToolbar>
        <template #left>
          <a-space>
            <a-button type="primary" :disabled="!selectedRowKeys.length" @click="handleBatchAction('pass')">批量通过</a-button>
            <a-button :disabled="!selectedRowKeys.length" @click="handleBatchAction('reset')">重置状态</a-button>
            <a-button danger :disabled="!selectedRowKeys.length" @click="handleBatchAction('reject')">批量驳回</a-button>
            <a-button danger ghost :disabled="!selectedRowKeys.length" @click="handleDelete">删除记录</a-button>
          </a-space>
        </template>
        <template #right>
          <a-button @click="getData">刷新</a-button>
        </template>
      </AdminToolbar>

      <div ref="containerRef">
        <a-table
          :columns="columns"
          :data-source="dataSource"
          :pagination="false"
          :row-selection="{ selectedRowKeys, onChange: onSelectChange }"
          :scroll="{ x: 1280, y: tableScrollY }"
          row-key="id"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'status'">
              <a-tag :color="getStatusMeta(record.status).color">{{ getStatusMeta(record.status).text }}</a-tag>
            </template>
            <template v-else-if="column.key === 'images'">
              <a-space>
                <a v-if="record.positiveUrl" @click="handlePreview(record, 'positiveUrl')">正面</a>
                <a v-if="record.reverseUrl" @click="handlePreview(record, 'reverseUrl')">反面</a>
                <a v-if="record.handUrl" @click="handlePreview(record, 'handUrl')">手持</a>
                <span v-if="!record.positiveUrl && !record.reverseUrl && !record.handUrl">-</span>
              </a-space>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a @click="handleView(record)">详情</a>
                <a v-if="record.status !== 1" @click="handleSingleAction('pass', record)">通过</a>
                <a v-if="record.status !== 3" @click="handleSingleAction('reject', record)">驳回</a>
                <a v-if="record.status !== 0" @click="handleSingleAction('reset', record)">重置</a>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>

      <AdminPagination :total="total" :current="current" :page-size="pageSize" @change="handlePageChange" />
    </AdminCard>

    <Detail v-model:visible="detailVisible" :detail-data="detailData" />

    <a-modal :open="previewVisible" :title="previewTitle" :footer="null" @cancel="handlePreviewCancel">
      <img :src="previewImage" alt="preview" class="preview-image" />
    </a-modal>
  </AdminPageLayout>
</template>

<script setup>
import { createVNode, onMounted, reactive, ref } from "vue"
import { ExclamationCircleOutlined } from "@ant-design/icons-vue"
import { message, Modal } from "ant-design-vue"
import { useTableScroll } from "@/composables/useTableScroll"
import systemAuthApi from "@/api/systemAuth"
import AdminPageLayout from "@/components/admin/AdminPageLayout.vue"
import AdminCard from "@/components/admin/AdminCard.vue"
import AdminToolbar from "@/components/admin/AdminToolbar.vue"
import AdminPagination from "@/components/admin/AdminPagination.vue"
import Detail from "./Detail.vue"
import { createAuthColumns } from "./columns"

const formRef = ref()
const formState = reactive({
  realName: "",
  cardNo: "",
  status: undefined,
})

const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const dataSource = ref([])
const selectedRowKeys = ref([])
const detailVisible = ref(false)
const detailData = ref({})
const previewVisible = ref(false)
const previewImage = ref("")
const previewTitle = ref("证件预览")
const { containerRef, tableScrollY } = useTableScroll()

const statusOptions = [
  { label: "待审核", value: 0 },
  { label: "已通过", value: 1 },
  { label: "自动通过", value: 2 },
  { label: "已驳回", value: 3 },
]

const columns = createAuthColumns()

onMounted(() => {
  getData()
})

const handleSearch = () => {
  current.value = 1
  getData()
}

const getData = () => {
  systemAuthApi
    .getPageAuths({
      page: current.value,
      limit: pageSize.value,
      status: formState.status,
    })
    .then((res) => {
      const pageData = res.data || {}
      const records = pageData.records || pageData.list || []
      const keyword = `${formState.realName || ""}${formState.cardNo || ""}`.trim()

      dataSource.value = keyword
        ? records.filter((item) => {
            const realName = item.realName || ""
            const cardNo = item.cardNo || ""
            return realName.includes(formState.realName || "") && cardNo.includes(formState.cardNo || "")
          })
        : records
      total.value = keyword ? dataSource.value.length : pageData.total || records.length
      selectedRowKeys.value = []
    })
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

const onSelectChange = (keys) => {
  selectedRowKeys.value = keys
}

const handleView = (record) => {
  detailData.value = { ...record }
  detailVisible.value = true
}

const handlePreview = (record, key) => {
  const titleMap = {
    positiveUrl: "身份证正面",
    reverseUrl: "身份证反面",
    handUrl: "手持证件照",
  }
  previewTitle.value = titleMap[key]
  previewImage.value = record[key]
  previewVisible.value = true
}

const handlePreviewCancel = () => {
  previewVisible.value = false
  previewImage.value = ""
}

const handleSingleAction = (type, record) => {
  confirmAction(type, [record.id], getData)
}

const handleBatchAction = (type) => {
  confirmAction(type, selectedRowKeys.value, getData)
}

const handleDelete = () => {
  if (!selectedRowKeys.value.length) {
    return
  }

  Modal.confirm({
    title: "确认删除选中的认证记录？",
    icon: createVNode(ExclamationCircleOutlined),
    content: `共 ${selectedRowKeys.value.length} 条记录，删除后不可恢复。`,
    okText: "确认删除",
    okType: "danger",
    cancelText: "取消",
    onOk() {
      return systemAuthApi.deleteAuths(selectedRowKeys.value).then(() => {
        message.success("认证记录已删除")
        getData()
      })
    },
  })
}

const confirmAction = (type, ids, onSuccess) => {
  if (!ids.length) {
    return
  }

  const actionMap = {
    pass: { title: "确认通过认证？", text: "通过" },
    reject: { title: "确认驳回认证？", text: "驳回" },
    reset: { title: "确认重置认证状态？", text: "重置" },
  }
  const action = actionMap[type]

  Modal.confirm({
    title: action.title,
    icon: createVNode(ExclamationCircleOutlined),
    content: `将处理 ${ids.length} 条认证记录。`,
    okText: "确认",
    cancelText: "取消",
    okType: type === "reject" ? "danger" : "primary",
    onOk() {
      return systemAuthApi.updateStatus(type, ids).then(() => {
        message.success(`${action.text}成功`)
        onSuccess()
      })
    },
  })
}

const getStatusMeta = (status) => {
  if (status === 1) return { text: "已通过", color: "success" }
  if (status === 2) return { text: "自动通过", color: "processing" }
  if (status === 3) return { text: "已驳回", color: "error" }
  return { text: "待审核", color: "warning" }
}
</script>

<style scoped lang="scss">
.preview-image {
  width: 100%;
}
</style>
