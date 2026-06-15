<template>
  <AdminPageLayout title="内容审核" description="集中处理直播风控和用户举报，确认违规后再执行封禁。">
    <AdminStatGrid :items="statCards" />

    <AdminCard title="审核队列" subtitle="风控命中只进入待审列表，由管理员确认或驳回">
      <AdminToolbar>
        <template #left>
          <a-space>
            <a-button type="primary" @click="getData">刷新</a-button>
            <a-select v-model:value="filterType" class="type-filter" @change="handleSearch">
              <a-select-option value="">全部来源</a-select-option>
              <a-select-option value="live_guard">直播风控</a-select-option>
              <a-select-option value="room">用户举报</a-select-option>
              <a-select-option value="message">消息举报</a-select-option>
              <a-select-option value="user">用户举报</a-select-option>
            </a-select>
          </a-space>
        </template>
        <template #right>
          <span>待处理 {{ total }} 条</span>
        </template>
      </AdminToolbar>

      <div ref="containerRef" class="admin-table-shell">
        <a-table
          :columns="columns"
          :data-source="displayRows"
          :loading="loading"
          :pagination="false"
          :scroll="{ x: 1280, y: tableScrollY }"
          row-key="id"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'source'">
              <a-tag :color="record.targetType === 'live_guard' ? 'orange' : 'blue'">{{ sourceText(record.targetType) }}</a-tag>
            </template>
            <template v-else-if="column.key === 'target'">
              <div class="target-cell">
                <strong>Room {{ record.roomId || '-' }}</strong>
                <span>User {{ record.targetUserId || '-' }}</span>
              </div>
            </template>
            <template v-else-if="column.key === 'reason'">
              <a-tag color="red">{{ record.reason || '-' }}</a-tag>
            </template>
            <template v-else-if="column.key === 'description'">
              <a-typography-paragraph :ellipsis="{ rows: 2, expandable: false }" class="desc">
                {{ record.description || '-' }}
              </a-typography-paragraph>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a @click="openDetail(record)">详情</a>
                <a v-if="record.targetType === 'live_guard'" class="danger-link" @click="confirmViolation(record)">确认违规</a>
                <a v-else @click="handleReport(record, 1)">通过</a>
                <a @click="handleReport(record, 2)">驳回</a>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>

      <AdminPagination :total="total" :current="current" :page-size="pageSize" @change="handlePageChange" />
    </AdminCard>

    <a-modal v-model:open="detailVisible" title="审核详情" :footer="null" width="720px">
      <a-descriptions bordered :column="1" size="small">
        <a-descriptions-item label="审核单">{{ detailData.id }}</a-descriptions-item>
        <a-descriptions-item label="来源">{{ sourceText(detailData.targetType) }}</a-descriptions-item>
        <a-descriptions-item label="房间">{{ detailData.roomId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="目标用户">{{ detailData.targetUserId || '-' }}</a-descriptions-item>
        <a-descriptions-item label="原因">{{ detailData.reason || '-' }}</a-descriptions-item>
        <a-descriptions-item label="提交时间">{{ detailData.createTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="证据">
          <pre class="evidence">{{ formatEvidence(detailData.description) }}</pre>
        </a-descriptions-item>
      </a-descriptions>
    </a-modal>
  </AdminPageLayout>
</template>

<script setup>
import { computed, createVNode, onMounted, reactive, ref } from "vue"
import { ExclamationCircleOutlined } from "@ant-design/icons-vue"
import { message, Modal } from "ant-design-vue"
import adminReportApi from "@/api/adminReport"
import { useTableScroll } from "@/composables/useTableScroll"
import AdminPageLayout from "@/components/admin/AdminPageLayout.vue"
import AdminCard from "@/components/admin/AdminCard.vue"
import AdminToolbar from "@/components/admin/AdminToolbar.vue"
import AdminPagination from "@/components/admin/AdminPagination.vue"
import AdminStatGrid from "@/components/admin/AdminStatGrid.vue"

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const filterType = ref("")
const detailVisible = ref(false)
const detailData = reactive({})
const { containerRef, tableScrollY } = useTableScroll()
const statCards = computed(() => [
  { key: "pending", label: "待处理", value: total.value, extra: "当前审核队列" },
  { key: "guard", label: "风控单", value: rows.value.filter((item) => item.targetType === "live_guard").length, extra: "系统自动命中" },
  { key: "report", label: "举报单", value: rows.value.filter((item) => item.targetType !== "live_guard").length, extra: "用户提交举报" },
  { key: "room", label: "直播间", value: new Set(rows.value.map((item) => item.roomId).filter(Boolean)).size, extra: "涉及房间数" },
])

const columns = [
  { title: "ID", dataIndex: "id", key: "id", width: 90, fixed: "left" },
  { title: "来源", dataIndex: "targetType", key: "source", width: 130 },
  { title: "对象", key: "target", width: 170 },
  { title: "原因", dataIndex: "reason", key: "reason", width: 180 },
  { title: "说明 / 证据", dataIndex: "description", key: "description", width: 420 },
  { title: "提交时间", dataIndex: "createTime", key: "createTime", width: 180 },
  { title: "操作", key: "action", width: 220, fixed: "right", align: "center" },
]

const displayRows = computed(() => {
  if (!filterType.value) return rows.value
  return rows.value.filter((item) => item.targetType === filterType.value)
})

onMounted(() => {
  getData()
})

const getData = async () => {
  loading.value = true
  try {
    const res = await adminReportApi.list({ page: current.value, limit: pageSize.value })
    const data = res?.data || {}
    rows.value = data.list || data.records || []
    total.value = Number(data.total || rows.value.length || 0)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  current.value = 1
  getData()
}

const handlePageChange = (page, size) => {
  current.value = page
  pageSize.value = size
  getData()
}

const openDetail = (record) => {
  Object.assign(detailData, record)
  detailVisible.value = true
}

const confirmViolation = (record) => {
  Modal.confirm({
    title: "确认违规并封禁直播间？",
    icon: createVNode(ExclamationCircleOutlined),
    content: `房间 ${record.roomId || "-"} 将被封禁，当前直播会结束。`,
    okText: "确认封禁",
    okType: "danger",
    cancelText: "取消",
    onOk: () => handleReport(record, 1, "管理员确认直播内容违规，封禁直播间"),
  })
}

const handleReport = async (record, status, result) => {
  await adminReportApi.handle({
    reportId: record.id,
    status,
    result: result || (status === 1 ? "管理员已处理" : "管理员驳回"),
  })
  message.success(status === 1 ? "处理成功" : "已驳回")
  getData()
}

const sourceText = (type) => {
  if (type === "live_guard") return "直播风控"
  if (type === "room") return "房间举报"
  if (type === "message") return "消息举报"
  if (type === "user") return "用户举报"
  return type || "未知"
}

const formatEvidence = (value) => {
  if (!value) return "-"
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch (error) {
    return value
  }
}
</script>

<style scoped lang="scss">
.type-filter {
  width: 150px;
}

.admin-stat-grid {
  margin-bottom: 0;
}

.target-cell {
  display: grid;
  gap: 2px;

  strong {
    color: var(--text-primary);
    font-weight: 800;
  }

  span {
    color: var(--text-muted);
    font-size: 12px;
  }
}

.desc {
  margin-bottom: 0;
}

.danger-link {
  color: var(--danger);
}

.evidence {
  max-height: 300px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
