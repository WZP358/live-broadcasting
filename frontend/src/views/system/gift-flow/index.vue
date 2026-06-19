<template>
  <AdminPageLayout title="礼物流水" description="面向管理员的全平台送礼审计视图，统一核对观众消费、主播收益和直播间礼物明细。">
    <AdminStatGrid :items="summaryCards" />

    <AdminCard title="筛选条件" subtitle="支持按房间、用户、主播、礼物和时间范围筛选流水">
      <a-form ref="formRef" :model="formState" @finish="handleSearch">
        <div class="admin-query-grid admin-query-grid--gift-flow">
          <a-form-item label="直播间ID" name="roomId">
            <a-input-number v-model:value="formState.roomId" :min="1" placeholder="请输入直播间ID" style="width: 100%" />
          </a-form-item>
          <a-form-item label="送礼用户ID" name="fromId">
            <a-input-number v-model:value="formState.fromId" :min="1" placeholder="请输入用户ID" style="width: 100%" />
          </a-form-item>
          <a-form-item label="主播ID" name="toId">
            <a-input-number v-model:value="formState.toId" :min="1" placeholder="请输入主播ID" style="width: 100%" />
          </a-form-item>
          <a-form-item label="礼物ID" name="presentId">
            <a-input-number v-model:value="formState.presentId" :min="1" placeholder="请输入礼物ID" style="width: 100%" />
          </a-form-item>
          <a-form-item label="流水类型" name="type">
            <a-select v-model:value="formState.type" :options="typeOptions" allow-clear placeholder="请选择类型" />
          </a-form-item>
          <a-form-item label="时间范围" name="timeRange">
            <a-range-picker
              v-model:value="formState.timeRange"
              show-time
              value-format="YYYY-MM-DD HH:mm:ss"
              format="YYYY-MM-DD HH:mm"
              style="width: 100%"
            />
          </a-form-item>
          <div class="admin-query-actions">
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </div>
        </div>
      </a-form>
    </AdminCard>

    <AdminCard title="流水列表" subtitle="只读展示礼物交易明细，避免管理员误改资金与收益记录">
      <AdminToolbar>
        <template #left>
          <a-space>
            <a-button type="primary" @click="loadData">刷新</a-button>
            <a-button @click="handleReset">重置</a-button>
          </a-space>
        </template>
        <template #right>
          <span>礼物配置在“礼物管理”维护，资金变动在“账单管理”核对</span>
        </template>
      </AdminToolbar>

      <div class="admin-list-meta">
        <span class="admin-list-meta__highlight">当前结果 {{ total }} 条</span>
        <span>展示送礼人、主播、直播间、礼物、数量、金额和赠送时间</span>
      </div>

      <div ref="containerRef" class="admin-table-shell">
        <a-table
          :columns="columns"
          :data-source="dataSource"
          :pagination="false"
          :scroll="{ x: 1480, y: tableScrollY }"
          row-key="id"
          size="middle"
        >
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'index'">
              {{ index + 1 }}
            </template>
            <template v-else-if="column.key === 'presentName'">
              <div class="admin-avatar-cell">
                <a-avatar shape="square" :src="safeGiftIcon(record.presentIcon)">
                  {{ firstChar(record.presentName, "礼") }}
                </a-avatar>
                <div class="admin-avatar-meta">
                  <div class="admin-avatar-meta__title">{{ record.presentName || "未知礼物" }}</div>
                  <div class="admin-avatar-meta__desc">ID：{{ record.presentId || "-" }}</div>
                </div>
              </div>
            </template>
            <template v-else-if="column.key === 'fromUserNickname'">
              <div class="admin-avatar-cell">
                <a-avatar :src="safeAvatar(record.fromUserAvatar)">{{ firstChar(record.fromUserNickname, "用") }}</a-avatar>
                <div class="admin-avatar-meta">
                  <div class="admin-avatar-meta__title">{{ record.fromUserNickname || "未知用户" }}</div>
                  <div class="admin-avatar-meta__desc">ID：{{ record.fromId || "-" }}</div>
                </div>
              </div>
            </template>
            <template v-else-if="column.key === 'anchorNickname'">
              <div class="admin-avatar-cell">
                <a-avatar :src="safeAvatar(record.anchorAvatar)">{{ firstChar(record.anchorNickname, "主") }}</a-avatar>
                <div class="admin-avatar-meta">
                  <div class="admin-avatar-meta__title">{{ record.anchorNickname || "未知主播" }}</div>
                  <div class="admin-avatar-meta__desc">ID：{{ record.toId || "-" }}</div>
                </div>
              </div>
            </template>
            <template v-else-if="column.key === 'roomTitle'">
              <div class="gift-flow-room">
                <strong>{{ record.roomTitle || "未关联直播间" }}</strong>
                <span>房间 {{ record.roomId || "-" }}</span>
              </div>
            </template>
            <template v-else-if="column.key === 'type'">
              <a-tag :color="typeMeta(record.type).color">{{ typeMeta(record.type).text }}</a-tag>
            </template>
            <template v-else-if="column.key === 'unitPrice'">
              {{ formatAmount(record.unitPrice) }}
            </template>
            <template v-else-if="column.key === 'totalPrice'">
              <span class="gift-flow-amount">{{ formatAmount(record.totalPrice) }}</span>
            </template>
          </template>
        </a-table>
      </div>

      <AdminPagination :total="total" :current="current" :page-size="pageSize" @change="handlePageChange" />
    </AdminCard>
  </AdminPageLayout>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from "vue"
import systemGiftFlowApi from "@/api/systemGiftFlow"
import AdminPageLayout from "@/components/admin/AdminPageLayout.vue"
import AdminCard from "@/components/admin/AdminCard.vue"
import AdminToolbar from "@/components/admin/AdminToolbar.vue"
import AdminPagination from "@/components/admin/AdminPagination.vue"
import AdminStatGrid from "@/components/admin/AdminStatGrid.vue"
import { useTableScroll } from "@/composables/useTableScroll"
import { FALLBACK_AVATAR, FALLBACK_GIFT_ICON, resolveSafeImageUrl } from "@/utils/fallback"

const formRef = ref()
const { containerRef, tableScrollY } = useTableScroll()

const formState = reactive({
  roomId: undefined,
  fromId: undefined,
  toId: undefined,
  presentId: undefined,
  type: undefined,
  timeRange: [],
})

const typeOptions = [
  { label: "直播打赏", value: 0 },
  { label: "视频打赏", value: 1 },
]

const columns = [
  { title: "序号", dataIndex: "index", key: "index", width: 80, align: "center" },
  { title: "流水ID", dataIndex: "id", key: "id", width: 100 },
  { title: "礼物", dataIndex: "presentName", key: "presentName", width: 230 },
  { title: "送礼用户", dataIndex: "fromUserNickname", key: "fromUserNickname", width: 230 },
  { title: "收礼主播", dataIndex: "anchorNickname", key: "anchorNickname", width: 230 },
  { title: "直播间", dataIndex: "roomTitle", key: "roomTitle", width: 230 },
  { title: "类型", dataIndex: "type", key: "type", width: 110 },
  { title: "数量", dataIndex: "number", key: "number", width: 90, align: "right" },
  { title: "单价", dataIndex: "unitPrice", key: "unitPrice", width: 120, align: "right" },
  { title: "总额", dataIndex: "totalPrice", key: "totalPrice", width: 130, align: "right" },
  { title: "赠送时间", dataIndex: "createTime", key: "createTime", width: 190 },
]

const summary = ref({})
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const dataSource = ref([])

const summaryCards = computed(() => [
  {
    key: "totalAmount",
    label: "累计礼物流水",
    value: formatAmount(summary.value.totalAmount),
    extra: `共 ${summary.value.totalCount || 0} 笔`,
  },
  {
    key: "todayAmount",
    label: "今日礼物流水",
    value: formatAmount(summary.value.todayAmount),
    extra: `今日 ${summary.value.todayCount || 0} 笔`,
  },
  {
    key: "liveAmount",
    label: "直播打赏",
    value: formatAmount(summary.value.liveAmount),
    extra: "直播间礼物收入",
  },
  {
    key: "videoAmount",
    label: "视频打赏",
    value: formatAmount(summary.value.videoAmount),
    extra: "视频内容礼物收入",
  },
])

const buildParams = () => {
  const [startTime, endTime] = formState.timeRange || []
  return {
    pageNo: current.value,
    pageSize: pageSize.value,
    roomId: formState.roomId,
    fromId: formState.fromId,
    toId: formState.toId,
    presentId: formState.presentId,
    type: formState.type,
    startTime,
    endTime,
  }
}

const loadData = async () => {
  const [pageRes, summaryRes] = await Promise.all([
    systemGiftFlowApi.getPage(buildParams()),
    systemGiftFlowApi.getSummary(buildParams()),
  ])
  const pageData = pageRes?.data || {}
  total.value = Number(pageData.total || 0)
  dataSource.value = pageData.records || pageData.list || []
  summary.value = summaryRes?.data || {}
}

const handleSearch = () => {
  current.value = 1
  loadData()
}

const handleReset = () => {
  formRef.value?.resetFields()
  current.value = 1
  loadData()
}

const handlePageChange = (page, size) => {
  current.value = page
  pageSize.value = size
  loadData()
}

const typeMeta = (type) => {
  if (Number(type) === 1) {
    return { text: "视频打赏", color: "purple" }
  }
  return { text: "直播打赏", color: "success" }
}

const formatAmount = (value) => `${Number(value || 0).toFixed(2)} 开心果`

const firstChar = (value, fallback) => String(value || fallback).slice(0, 1)
const safeAvatar = (url) => resolveSafeImageUrl(url, FALLBACK_AVATAR)
const safeGiftIcon = (url) => resolveSafeImageUrl(url, FALLBACK_GIFT_ICON)

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
.admin-query-grid--gift-flow {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.gift-flow-room {
  display: grid;
  gap: 4px;
}

.gift-flow-room strong {
  color: var(--admin-text);
  font-weight: 600;
}

.gift-flow-room span {
  color: var(--admin-text-secondary);
  font-size: 12px;
}

.gift-flow-amount {
  color: var(--success);
  font-weight: 700;
}

@media (max-width: 1100px) {
  .admin-query-grid--gift-flow {
    grid-template-columns: 1fr;
  }
}
</style>
