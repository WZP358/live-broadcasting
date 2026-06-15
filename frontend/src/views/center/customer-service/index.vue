<template>
  <div class="service-page">
    <section class="service-header">
      <div>
        <h2>联系客服</h2>
        <p>账号、开播、充值、违规申诉等问题都可以在这里提交，客服处理后会显示回复结果。</p>
      </div>
      <a-button @click="loadTickets">刷新记录</a-button>
    </section>

    <div class="service-layout">
      <section class="service-card service-submit">
        <div class="service-card__header">
          <h3>提交问题</h3>
          <span>请尽量描述页面、操作和异常提示</span>
        </div>
        <a-form layout="vertical" :model="formState" @finish="submitTicket">
          <a-form-item label="问题类型" name="category">
            <a-select v-model:value="formState.category" :options="categoryOptions" />
          </a-form-item>
          <a-form-item label="问题标题" name="title" :rules="[{ required: true, message: '请输入问题标题' }]">
            <a-input v-model:value="formState.title" placeholder="例如：开播提示房间信息未初始化" :maxlength="80" />
          </a-form-item>
          <a-form-item label="详细描述" name="content" :rules="[{ required: true, message: '请填写详细描述' }]">
            <a-textarea
              v-model:value="formState.content"
              placeholder="描述你看到的提示、发生时间、直播间或订单信息"
              :auto-size="{ minRows: 6, maxRows: 9 }"
              :maxlength="1000"
              show-count
            />
          </a-form-item>
          <a-button type="primary" html-type="submit" :loading="submitting" block>提交客服工单</a-button>
        </a-form>
      </section>

      <section class="service-card service-records">
        <div class="service-card__header">
          <h3>我的工单</h3>
          <span>共 {{ total }} 条记录</span>
        </div>
        <a-spin :spinning="loading">
          <div v-if="tickets.length" class="ticket-list">
            <article v-for="item in tickets" :key="item.id" class="ticket-card">
              <div class="ticket-card__top">
                <div>
                  <h4>{{ item.title }}</h4>
                  <span>{{ categoryLabel(item.category) }} · {{ formatTime(item.createTime) }}</span>
                </div>
                <a-tag :color="statusMeta(item.status).color">{{ statusMeta(item.status).label }}</a-tag>
              </div>
              <p class="ticket-card__content">{{ item.content }}</p>
              <div v-if="item.reply" class="ticket-reply">
                <strong>客服回复</strong>
                <p>{{ item.reply }}</p>
                <span>{{ formatTime(item.replyTime) }}</span>
              </div>
              <div class="ticket-card__actions">
                <a-button v-if="item.status !== 2" size="small" @click="closeTicket(item)">关闭工单</a-button>
              </div>
            </article>
          </div>
          <a-empty v-else description="暂无客服工单" />
        </a-spin>
        <div class="service-pagination">
          <a-pagination
            :current="page"
            :page-size="pageSize"
            :total="total"
            show-less-items
            @change="handlePageChange"
          />
        </div>
      </section>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import dayjs from 'dayjs'
import customerServiceApi from '@/api/customerService'
import $modal from '@/utils/message'

const categoryOptions = [
  { label: '账号与登录', value: 'account' },
  { label: '直播开播', value: 'live' },
  { label: '充值与钱包', value: 'wallet' },
  { label: '举报与申诉', value: 'appeal' },
  { label: '功能建议', value: 'feedback' },
  { label: '其他问题', value: 'general' },
]

const formState = reactive({
  category: 'live',
  title: '',
  content: '',
})
const tickets = ref([])
const loading = ref(false)
const submitting = ref(false)
const page = ref(1)
const pageSize = ref(6)
const total = ref(0)

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
    const res = await customerServiceApi.myTickets({ page: page.value, limit: pageSize.value })
    tickets.value = res?.data?.list || []
    total.value = Number(res?.data?.total || 0)
  } finally {
    loading.value = false
  }
}

const submitTicket = async () => {
  if (!formState.title.trim() || !formState.content.trim()) {
    $modal.msgWarning('请填写问题标题和详细描述')
    return
  }
  submitting.value = true
  try {
    await customerServiceApi.submit({
      category: formState.category,
      title: formState.title.trim(),
      content: formState.content.trim(),
    })
    $modal.msgSuccess('客服工单已提交')
    formState.title = ''
    formState.content = ''
    page.value = 1
    loadTickets()
  } finally {
    submitting.value = false
  }
}

const closeTicket = async (item) => {
  await customerServiceApi.close({ ticketId: item.id })
  $modal.msgSuccess('工单已关闭')
  loadTickets()
}

const handlePageChange = (nextPage) => {
  page.value = nextPage
  loadTickets()
}

onMounted(loadTickets)
</script>

<style scoped lang="scss">
.service-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.service-header,
.service-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
  box-shadow: var(--shadow);
}

.service-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 24px 26px;
}

.service-header h2,
.service-card__header h3 {
  margin: 0;
  color: var(--text-primary);
  font-weight: 900;
}

.service-header h2 {
  margin-bottom: 8px;
  font-size: 24px;
}

.service-header p,
.service-card__header span {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.service-layout {
  display: grid;
  grid-template-columns: minmax(320px, 420px) minmax(0, 1fr);
  gap: 18px;
}

.service-card {
  padding: 20px;
}

.service-card__header {
  display: flex;
  justify-content: space-between;
  gap: 14px;
  align-items: flex-start;
  margin-bottom: 18px;
}

.ticket-list {
  display: grid;
  gap: 14px;
}

.ticket-card {
  padding: 16px;
  border: 1px solid var(--border);
  border-radius: 8px;
  background: var(--bg-card);
}

.ticket-card__top {
  display: flex;
  justify-content: space-between;
  gap: 14px;
}

.ticket-card h4 {
  margin: 0 0 6px;
  color: var(--text-primary);
  font-size: 16px;
  font-weight: 900;
}

.ticket-card__top span,
.ticket-reply span {
  color: var(--text-muted);
  font-size: 12px;
}

.ticket-card__content {
  margin: 12px 0;
  color: var(--text-secondary);
  line-height: 1.75;
  white-space: pre-wrap;
}

.ticket-reply {
  padding: 12px;
  border-radius: 8px;
  background: var(--bg-secondary);
  border: 1px solid var(--border);
}

.ticket-reply strong {
  color: var(--text-primary);
}

.ticket-reply p {
  margin: 8px 0 6px;
  color: var(--text-secondary);
  line-height: 1.7;
  white-space: pre-wrap;
}

.ticket-card__actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.service-pagination {
  display: flex;
  justify-content: center;
  padding-top: 18px;
}

@media (max-width: 1024px) {
  .service-layout {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .service-header,
  .service-card__header {
    flex-direction: column;
  }
}
</style>
