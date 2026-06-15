<template>
  <AdminPageLayout
    title="客服处理"
    description="集中处理用户提交的账号、直播、充值、申诉和功能反馈工单，支持筛选、回复、关闭和重新打开。"
  >
    <AdminStatGrid :items="statItems" />

    <AdminCard title="筛选条件" subtitle="按状态和关键词快速定位需要处理的客服工单">
      <a-form :model="queryState" @finish="handleSearch">
        <div class="admin-query-grid">
          <a-form-item label="处理状态">
            <a-select v-model:value="queryState.status" :options="statusOptions" allow-clear placeholder="全部状态" />
          </a-form-item>
          <a-form-item label="关键词">
            <a-input v-model:value="queryState.keyword" placeholder="搜索标题、内容或分类" />
          </a-form-item>
          <div class="admin-query-actions">
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </div>
        </div>
      </a-form>
    </AdminCard>

    <AdminCard title="客服工单" subtitle="待处理工单会优先显示，方便管理员快速响应">
      <AdminToolbar>
        <template #left>
          <a-button type="primary" @click="loadTickets">刷新</a-button>
          <a-button @click="setPendingFilter">只看待处理</a-button>
        </template>
        <template #right>
          <span>当前结果 {{ total }} 条</span>
        </template>
      </AdminToolbar>

      <div ref="containerRef" class="admin-table-shell">
        <a-table
          :columns="columns"
          :data-source="tickets"
          :loading="loading"
          :pagination="false"
          :scroll="{ y: tableScrollY }"
          row-key="id"
          size="middle"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'title'">
              <div class="ticket-title-cell">
                <strong>{{ record.title }}</strong>
                <span>{{ record.content }}</span>
              </div>
            </template>
            <template v-else-if="column.key === 'category'">
              <a-tag>{{ categoryLabel(record.category) }}</a-tag>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="statusMeta(record.status).color">{{ statusMeta(record.status).label }}</a-tag>
            </template>
            <template v-else-if="column.key === 'createTime'">
              {{ formatTime(record.createTime) }}
            </template>
            <template v-else-if="column.key === 'action'">
              <a-button type="link" size="small" @click="openDrawer(record)">处理</a-button>
            </template>
          </template>
        </a-table>
      </div>

      <AdminPagination :total="total" :current="current" :page-size="pageSize" @change="handlePageChange" />
    </AdminCard>

    <a-drawer v-model:open="drawerOpen" width="560" title="客服工单详情" destroy-on-close>
      <div v-if="activeTicket" class="ticket-detail">
        <div class="ticket-detail__head">
          <div>
            <h3>{{ activeTicket.title }}</h3>
            <p>用户ID：{{ activeTicket.userId }} · {{ categoryLabel(activeTicket.category) }}</p>
          </div>
          <a-tag :color="statusMeta(activeTicket.status).color">{{ statusMeta(activeTicket.status).label }}</a-tag>
        </div>

        <section class="ticket-detail__section">
          <h4>用户描述</h4>
          <p>{{ activeTicket.content }}</p>
          <span>{{ formatTime(activeTicket.createTime) }}</span>
        </section>

        <section v-if="activeTicket.reply" class="ticket-detail__section ticket-detail__section--reply">
          <h4>当前回复</h4>
          <p>{{ activeTicket.reply }}</p>
          <span>{{ formatTime(activeTicket.replyTime) }}</span>
        </section>

        <a-form layout="vertical" class="reply-form">
          <a-form-item label="处理结果">
            <a-select v-model:value="replyState.status" :options="replyStatusOptions" />
          </a-form-item>
          <a-form-item label="客服回复">
            <a-textarea
              v-model:value="replyState.reply"
              placeholder="填写给用户看的回复内容"
              :auto-size="{ minRows: 5, maxRows: 8 }"
              :maxlength="1000"
              show-count
            />
          </a-form-item>
          <a-button type="primary" :loading="replying" block @click="submitReply">提交处理</a-button>
        </a-form>
      </div>
    </a-drawer>
  </AdminPageLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import adminCustomerServiceApi from '@/api/adminCustomerService'
import { useTableScroll } from '@/composables/useTableScroll'
import AdminPageLayout from '@/components/admin/AdminPageLayout.vue'
import AdminCard from '@/components/admin/AdminCard.vue'
import AdminToolbar from '@/components/admin/AdminToolbar.vue'
import AdminStatGrid from '@/components/admin/AdminStatGrid.vue'
import AdminPagination from '@/components/admin/AdminPagination.vue'
import $modal from '@/utils/message'

const categoryOptions = [
  { label: '账号与登录', value: 'account' },
  { label: '直播开播', value: 'live' },
  { label: '充值与钱包', value: 'wallet' },
  { label: '举报与申诉', value: 'appeal' },
  { label: '功能建议', value: 'feedback' },
  { label: '其他问题', value: 'general' },
]

const statusOptions = [
  { label: '待处理', value: 0 },
  { label: '已回复', value: 1 },
  { label: '已关闭', value: 2 },
]

const replyStatusOptions = [
  { label: '回复后保持已回复', value: 1 },
  { label: '关闭工单', value: 2 },
  { label: '重新打开为待处理', value: 0 },
]

const columns = [
  { title: '工单ID', dataIndex: 'id', key: 'id', width: 90 },
  { title: '用户ID', dataIndex: 'userId', key: 'userId', width: 100 },
  { title: '问题', dataIndex: 'title', key: 'title' },
  { title: '类型', dataIndex: 'category', key: 'category', width: 120 },
  { title: '状态', dataIndex: 'status', key: 'status', width: 110 },
  { title: '提交时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 100, fixed: 'right' },
]

const queryState = reactive({
  status: undefined,
  keyword: '',
})
const tickets = ref([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const loading = ref(false)
const drawerOpen = ref(false)
const activeTicket = ref(null)
const replying = ref(false)
const replyState = reactive({
  status: 1,
  reply: '',
})
const { containerRef, tableScrollY } = useTableScroll()

const statItems = computed(() => {
  const pending = tickets.value.filter((item) => item.status === 0).length
  const replied = tickets.value.filter((item) => item.status === 1).length
  const closed = tickets.value.filter((item) => item.status === 2).length
  return [
    { key: 'total', label: '当前页工单', value: tickets.value.length, extra: `筛选结果共 ${total.value} 条` },
    { key: 'pending', label: '待处理', value: pending, extra: '需要管理员响应' },
    { key: 'replied', label: '已回复', value: replied, extra: '用户可在个人中心查看' },
    { key: 'closed', label: '已关闭', value: closed, extra: '流程已结束' },
  ]
})

const categoryLabel = (value) => categoryOptions.find((item) => item.value === value)?.label || '其他问题'

const statusMeta = (status) => {
  if (status === 1) return { label: '已回复', color: 'processing' }
  if (status === 2) return { label: '已关闭', color: 'default' }
  return { label: '待处理', color: 'warning' }
}

const formatTime = (time) => (time ? dayjs(time).format('YYYY-MM-DD HH:mm') : '-')

const loadTickets = async () => {
  loading.value = true
  try {
    const res = await adminCustomerServiceApi.list({
      page: current.value,
      limit: pageSize.value,
      keyword: queryState.keyword || undefined,
      status: queryState.status,
    })
    tickets.value = res?.data?.list || []
    total.value = Number(res?.data?.total || 0)
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  current.value = 1
  loadTickets()
}

const handleReset = () => {
  queryState.status = undefined
  queryState.keyword = ''
  current.value = 1
  loadTickets()
}

const setPendingFilter = () => {
  queryState.status = 0
  current.value = 1
  loadTickets()
}

const handlePageChange = (page, size) => {
  current.value = page
  pageSize.value = size
  loadTickets()
}

const openDrawer = (record) => {
  activeTicket.value = record
  replyState.status = record.status === 2 ? 2 : 1
  replyState.reply = record.reply || ''
  drawerOpen.value = true
}

const submitReply = async () => {
  if (!activeTicket.value) return
  if (replyState.status !== 0 && !replyState.reply.trim()) {
    $modal.msgWarning('请填写客服回复')
    return
  }
  replying.value = true
  try {
    await adminCustomerServiceApi.reply({
      ticketId: activeTicket.value.id,
      reply: replyState.reply.trim(),
      status: replyState.status,
    })
    $modal.msgSuccess('客服工单已处理')
    drawerOpen.value = false
    loadTickets()
  } finally {
    replying.value = false
  }
}

onMounted(loadTickets)
</script>

<style scoped lang="scss">
.ticket-title-cell {
  display: grid;
  gap: 4px;
}

.ticket-title-cell strong {
  color: var(--text-primary);
}

.ticket-title-cell span {
  max-width: 520px;
  overflow: hidden;
  color: var(--text-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
}

.ticket-detail {
  display: grid;
  gap: 18px;
}

.ticket-detail__head {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
}

.ticket-detail__head h3 {
  margin: 0 0 8px;
  color: var(--text-primary);
  font-size: 20px;
  font-weight: 900;
}

.ticket-detail__head p {
  margin: 0;
  color: var(--text-secondary);
}

.ticket-detail__section {
  padding: 14px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-secondary);
}

.ticket-detail__section--reply {
  background: color-mix(in srgb, var(--accent) 8%, var(--bg-card));
}

.ticket-detail__section h4 {
  margin: 0 0 10px;
  color: var(--text-primary);
  font-weight: 900;
}

.ticket-detail__section p {
  margin: 0 0 10px;
  color: var(--text-secondary);
  line-height: 1.75;
  white-space: pre-wrap;
}

.ticket-detail__section span {
  color: var(--text-muted);
  font-size: 12px;
}

.reply-form {
  padding-top: 4px;
}
</style>
