<template>
  <AdminPageLayout title="账单管理" description="统一查看平台账单流水，支持按类型和备注筛选，便于运营核对充值、提现和消费记录。">
    <AdminCard title="筛选条件" subtitle="支持按账单类型与备注标识筛选">
      <a-form ref="formRef" :model="formState" @finish="handleSearch">
        <div class="admin-query-grid admin-query-grid--bill">
          <a-form-item label="账单类型" name="type">
            <a-select v-model:value="formState.type" :options="typeOptions" allow-clear placeholder="请选择账单类型" />
          </a-form-item>
          <a-form-item label="业务标记" name="mark">
            <a-input v-model:value="formState.mark" placeholder="请输入业务标记，如充值、提现" />
          </a-form-item>
          <div class="admin-query-actions">
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </div>
        </div>
      </a-form>
    </AdminCard>

    <AdminCard title="账单列表" subtitle="沿用统一后台列表壳子，后续可继续接入统计图和导出能力">
      <AdminToolbar>
        <template #left>
          <a-space>
            <a-button type="primary" @click="getData">刷新</a-button>
          </a-space>
        </template>
        <template #right>
          <span>当前页面为只读账单视图，避免误操作影响资金记录</span>
        </template>
      </AdminToolbar>

      <div class="admin-list-meta">
        <span class="admin-list-meta__highlight">当前结果 {{ total }} 条</span>
        <span>展示用户、金额、余额、账单标记与创建时间</span>
      </div>

      <div ref="containerRef" class="admin-table-shell">
        <a-table :columns="columns" :data-source="dataSource" :pagination="false" :scroll="{ x: 1280, y: tableScrollY }" row-key="id" size="middle">
          <template #bodyCell="{ column, record, index }">
            <template v-if="column.key === 'index'">
              {{ index + 1 }}
            </template>
            <template v-else-if="column.key === 'billChange'">
              <span :style="{ color: Number(record.billChange) >= 0 ? 'var(--success)' : 'var(--danger)', fontWeight: 600 }">
                {{ Number(record.billChange) >= 0 ? `+${record.billChange}` : `${record.billChange}` }}
              </span>
            </template>
            <template v-else-if="column.key === 'type'">
              <a-tag :color="typeMeta(record.type).color">{{ typeMeta(record.type).text }}</a-tag>
            </template>
          </template>
        </a-table>
      </div>

      <AdminPagination :total="total" :current="current" :page-size="pageSize" @change="handlePageChange" />
    </AdminCard>
  </AdminPageLayout>
</template>

<script setup>
import { onMounted, reactive, ref } from "vue"
import request from "@/utils/request"
import AdminPageLayout from "@/components/admin/AdminPageLayout.vue"
import AdminCard from "@/components/admin/AdminCard.vue"
import AdminToolbar from "@/components/admin/AdminToolbar.vue"
import AdminPagination from "@/components/admin/AdminPagination.vue"
import { useTableScroll } from "@/composables/useTableScroll"

const formRef = ref()
const { containerRef, tableScrollY } = useTableScroll()

const formState = reactive({
  type: undefined,
  mark: "",
})

const typeOptions = [
  { label: "收入", value: 0 },
  { label: "支出", value: 1 },
]

const columns = [
  { title: "序号", dataIndex: "index", key: "index", width: 80, align: "center" },
  { title: "账单ID", dataIndex: "id", key: "id", width: 90 },
  { title: "用户ID", dataIndex: "userId", key: "userId", width: 100 },
  { title: "订单号", dataIndex: "orderNo", key: "orderNo", width: 220 },
  { title: "变动金额", dataIndex: "billChange", key: "billChange", width: 120 },
  { title: "账单类型", dataIndex: "type", key: "type", width: 110 },
  { title: "变动后余额", dataIndex: "balance", key: "balance", width: 130 },
  { title: "业务标记", dataIndex: "mark", key: "mark", width: 140 },
  { title: "IP", dataIndex: "ip", key: "ip", width: 160 },
  { title: "创建时间", dataIndex: "createTime", key: "createTime", width: 180 },
]

const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const dataSource = ref([])

const typeMeta = (type) => {
  if (Number(type) === 1) {
    return { text: "支出", color: "volcano" }
  }
  return { text: "收入", color: "success" }
}

const getData = async () => {
  const res = await request({
    url: "/api/v1/system/bill/page",
    method: "get",
    params: {
      pageNo: current.value,
      pageSize: pageSize.value,
      type: formState.type,
      mark: formState.mark || undefined,
    },
  })

  const pageData = res?.data || {}
  total.value = Number(pageData.total || 0)
  dataSource.value = pageData.records || pageData.list || []
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

onMounted(() => {
  getData()
})
</script>

<style scoped lang="scss">
.admin-query-grid--bill {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

@media (max-width: 960px) {
  .admin-query-grid--bill {
    grid-template-columns: 1fr;
  }
}
</style>
