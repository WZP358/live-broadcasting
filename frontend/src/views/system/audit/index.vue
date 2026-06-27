<template>
  <AdminPageLayout title="内容审核" description="集中处理直播风控和用户举报，确认违规后再执行封禁。">
    <AdminStatGrid :items="statCards" />

    <AdminCard title="审核队列" subtitle="风控命中只进入待审列表，由管理员确认或驳回">
      <AdminToolbar>
        <template #left>
          <a-space>
            <a-button type="primary" @click="getData">刷新</a-button>
            <a-select v-model:value="filterStatus" class="status-filter" @change="handleSearch">
              <a-select-option :value="0">待处理</a-select-option>
              <a-select-option :value="1">已处理</a-select-option>
              <a-select-option :value="2">已驳回</a-select-option>
              <a-select-option :value="-1">全部记录</a-select-option>
            </a-select>
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
          :locale="{ emptyText: '暂无审核数据。提交直播间举报或触发风控后，会出现在这里。' }"
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
                <strong>{{ record.roomTitle || `Room ${record.roomId || '-'}` }}</strong>
                <span>Room {{ record.roomId || '-' }} / User {{ record.targetUserId || '-' }}</span>
                <a-tag v-if="hasRoomStatus(record)" :color="roomDisabled(record) ? 'error' : 'success'" class="room-status-tag">
                  {{ roomDisabled(record) ? "已封禁" : "正常" }}
                </a-tag>
              </div>
            </template>
            <template v-else-if="column.key === 'reason'">
              <a-tag color="red">{{ record.reason || '-' }}</a-tag>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="statusColor(record.status)">{{ statusText(record.status) }}</a-tag>
            </template>
            <template v-else-if="column.key === 'description'">
              <a-typography-paragraph :ellipsis="{ rows: 2, expandable: false }" class="desc">
                {{ record.description || '-' }}
              </a-typography-paragraph>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a @click="openDetail(record)">详情</a>
                <template v-if="Number(record.status || 0) === 0">
                  <a v-if="record.targetType === 'live_guard'" class="danger-link" @click="confirmViolation(record)">确认违规</a>
                  <a v-else @click="handleReport(record, 1)">通过</a>
                  <a @click="handleReport(record, 2)">驳回</a>
                </template>
                <a v-if="canUnbanRoom(record)" class="success-link" @click="confirmUnban(record)">解封直播间</a>
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
        <a-descriptions-item label="状态">{{ statusText(detailData.status) }}</a-descriptions-item>
        <a-descriptions-item label="提交时间">{{ detailData.createTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="处理结果">{{ detailData.handleResult || '-' }}</a-descriptions-item>
        <a-descriptions-item label="处理时间">{{ detailData.handleTime || '-' }}</a-descriptions-item>
        <a-descriptions-item label="证据截图">
          <div v-if="detailEvidenceImage" class="evidence-image-wrap">
            <a-image
              :src="detailEvidenceImage"
              :fallback="FALLBACK_COVER"
              class="evidence-image"
            />
            <a :href="detailEvidenceImage" target="_blank" rel="noreferrer">打开原图</a>
          </div>
          <span v-else class="evidence-empty">暂无截图证据</span>
        </a-descriptions-item>
        <a-descriptions-item label="证据明细">
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
import systemRoomApi from "@/api/systemRoom"
import { useTableScroll } from "@/composables/useTableScroll"
import AdminPageLayout from "@/components/admin/AdminPageLayout.vue"
import AdminCard from "@/components/admin/AdminCard.vue"
import AdminToolbar from "@/components/admin/AdminToolbar.vue"
import AdminPagination from "@/components/admin/AdminPagination.vue"
import AdminStatGrid from "@/components/admin/AdminStatGrid.vue"
import { FALLBACK_COVER, resolveSafeImageUrl } from "@/utils/fallback"

const loading = ref(false)
const rows = ref([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const filterStatus = ref(0)
const filterType = ref("")
const detailVisible = ref(false)
const detailData = reactive({})
const { containerRef, tableScrollY } = useTableScroll()
const statCards = computed(() => [
  { key: "pending", label: statusText(filterStatus.value), value: total.value, extra: filterStatus.value === -1 ? "全部审核记录" : "当前筛选结果" },
  { key: "guard", label: "风控单", value: rows.value.filter((item) => item.targetType === "live_guard").length, extra: "系统自动命中" },
  { key: "report", label: "举报单", value: rows.value.filter((item) => item.targetType !== "live_guard").length, extra: "用户提交举报" },
  { key: "room", label: "直播间", value: new Set(rows.value.map((item) => item.roomId).filter(Boolean)).size, extra: "涉及房间数" },
])

const columns = [
  { title: "ID", dataIndex: "id", key: "id", width: 90, fixed: "left" },
  { title: "来源", dataIndex: "targetType", key: "source", width: 130 },
  { title: "对象", key: "target", width: 220 },
  { title: "原因", dataIndex: "reason", key: "reason", width: 180 },
  { title: "状态", dataIndex: "status", key: "status", width: 110 },
  { title: "说明 / 证据", dataIndex: "description", key: "description", width: 420 },
  { title: "提交时间", dataIndex: "createTime", key: "createTime", width: 180 },
  { title: "操作", key: "action", width: 260, fixed: "right", align: "center" },
]

const displayRows = computed(() => {
  return rows.value
})
const detailEvidenceImage = computed(() => resolveEvidenceImage(detailData.description))

onMounted(() => {
  getData()
})

const getData = async () => {
  loading.value = true
  try {
    const res = await adminReportApi.list({
      page: current.value,
      limit: pageSize.value,
      status: filterStatus.value,
      targetType: filterType.value || undefined,
    })
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

const confirmUnban = (record) => {
  Modal.confirm({
    title: "确认解封直播间？",
    icon: createVNode(ExclamationCircleOutlined),
    content: `${record.roomTitle || `房间 ${record.roomId || "-"}`} 将恢复为正常状态，主播可重新开播。`,
    okText: "确认解封",
    cancelText: "取消",
    onOk: async () => {
      await systemRoomApi.toggleRoomStatus({ id: record.roomId, disabled: 0 })
      message.success("解封成功")
      getData()
    },
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

const statusText = (status) => {
  if (Number(status) === 0) return "待处理"
  if (Number(status) === 1) return "已处理"
  if (Number(status) === 2) return "已驳回"
  return "未知"
}

const statusColor = (status) => {
  if (Number(status) === 0) return "processing"
  if (Number(status) === 1) return "success"
  if (Number(status) === 2) return "default"
  return "default"
}

const formatEvidence = (value) => {
  if (!value) return "-"
  try {
    return JSON.stringify(JSON.parse(value), null, 2)
  } catch (error) {
    return value
  }
}

const parseEvidence = (value) => {
  if (!value) return null
  if (typeof value === "object") return value
  try {
    return JSON.parse(value)
  } catch (error) {
    return null
  }
}

const resolveEvidenceImage = (value) => {
  const payload = parseEvidence(value)
  if (!payload) return ""
  const url = payload.evidenceImageUrl
    || payload.screenshotUrl
    || payload.imageUrl
    || payload.screenshot
    || payload.evidence?.imageUrl
    || payload.evidence?.screenshotUrl
  return url ? resolveSafeImageUrl(url, "") : ""
}

const hasRoomStatus = (record) => record && record.roomDisabled !== undefined && record.roomDisabled !== null

const roomDisabled = (record) => hasRoomStatus(record) && Number(record.roomDisabled) !== 0

const canUnbanRoom = (record) => Boolean(record?.roomId) && roomDisabled(record)
</script>

<style scoped lang="scss">
.type-filter,
.status-filter {
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

.room-status-tag {
  width: fit-content;
  margin-inline-end: 0;
}

.desc {
  margin-bottom: 0;
}

.danger-link {
  color: var(--danger);
}

.success-link {
  color: var(--success);
}

.evidence {
  max-height: 300px;
  margin: 0;
  overflow: auto;
  white-space: pre-wrap;
  word-break: break-word;
}

.evidence-image-wrap {
  display: grid;
  gap: 8px;
}

.evidence-image-wrap :deep(.ant-image) {
  overflow: hidden;
  max-width: 100%;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-secondary);
}

.evidence-image {
  display: block;
  max-width: 100%;
  max-height: 360px;
  object-fit: contain;
}

.evidence-empty {
  color: var(--text-muted);
}
</style>
