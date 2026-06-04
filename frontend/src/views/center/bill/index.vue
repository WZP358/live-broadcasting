<template>
  <div class="bill-page">
    <section class="bill-header">
      <div>
        <h2>交易记录</h2>
        <p>查看钱包收支明细，方便核对最近的充值、消费和余额变动。</p>
      </div>
      <a-button @click="getData">刷新</a-button>
    </section>

    <section class="bill-card">
      <a-table
        :data-source="dataSource"
        :columns="columns"
        :loading="loading"
        :pagination="pagination"
        row-key="id"
        size="middle"
        @change="handleTableChange"
      />
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from "vue"
import walletAPI from "@/api/wallet"

const loading = ref(false)
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const dataSource = ref([])

const pagination = computed(() => ({
  total: total.value,
  current: current.value,
  pageSize: pageSize.value,
  size: "default",
  showSizeChanger: true,
  showQuickJumper: true,
}))

const columns = [
  {
    title: "序号",
    dataIndex: "index",
    key: "index",
    width: 90,
    customRender: ({ index }) => index + 1,
  },
  {
    title: "变动金额",
    dataIndex: "fee",
    key: "fee",
    width: 160,
    customRender: ({ text }) => (Number(text) > 0 ? `+${text}` : `${text}`),
  },
  {
    title: "变动类型",
    dataIndex: "actionTypeName",
    key: "actionTypeName",
    width: 220,
  },
  {
    title: "时间",
    dataIndex: "createTime",
    key: "createTime",
  },
]

const getData = async () => {
  loading.value = true
  try {
    const res = await walletAPI.listWalletLogs({
      pageNo: current.value,
      pageSize: pageSize.value,
    })
    if (res.code === 0) {
      total.value = res.data.total
      dataSource.value = res.data.list || []
    }
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag) => {
  current.value = pag.current
  pageSize.value = pag.pageSize
  getData()
}

onMounted(() => {
  getData()
})
</script>

<style scoped lang="scss">
.bill-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.bill-header,
.bill-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #fff;
  box-shadow: var(--shadow);
}

.bill-header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 24px 26px;
}

.bill-header h2 {
  margin: 0 0 8px;
  color: var(--text-primary);
  font-size: 24px;
  font-weight: 900;
}

.bill-header p {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.bill-card {
  padding: 18px;
}

@media (max-width: 960px) {
  .bill-header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
