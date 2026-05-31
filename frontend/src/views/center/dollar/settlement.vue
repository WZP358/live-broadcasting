<template>
  <div class="settlement-page">
    <a-table :columns="columns" :data-source="settlements" :pagination="pagination" row-key="id" size="small" :loading="loading" @change="handleTableChange">
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'status'">
          <a-tag :color="statusColor(record.status)">{{ statusName(record.status) }}</a-tag>
        </template>
        <template v-if="column.key === 'giftIncome'">
          ¥{{ record.giftIncome || 0 }}
        </template>
        <template v-if="column.key === 'platformFee'">
          ¥{{ record.platformFee || 0 }}
        </template>
        <template v-if="column.key === 'netIncome'">
          <strong>¥{{ record.netIncome || 0 }}</strong>
        </template>
        <template v-if="column.key === 'settleTime'">
          {{ record.settleTime ? new Date(record.settleTime).toLocaleDateString('zh-CN') : '-' }}
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import settlementApi from '@/api/settlement'
import $modal from '@/utils/message'

const settlements = ref([])
const loading = ref(false)
const pagination = ref({ current: 1, pageSize: 12, total: 0 })

const columns = [
  { title: '结算周期', dataIndex: 'period', key: 'period' },
  { title: '礼物收入', key: 'giftIncome' },
  { title: '平台服务费', key: 'platformFee' },
  { title: '实际收入', key: 'netIncome' },
  { title: '可提现', dataIndex: 'withdrawable', key: 'withdrawable' },
  { title: '已提现', dataIndex: 'withdrawn', key: 'withdrawn' },
  { title: '状态', key: 'status' },
  { title: '结算时间', key: 'settleTime' }
]

const statusName = (s) => ['待结算', '已结算', '已打款'][s] || '未知'
const statusColor = (s) => ['orange', 'blue', 'green'][s] || 'default'

const loadData = async (page = 1) => {
  loading.value = true
  try {
    const res = await settlementApi.getSettlements({ page, limit: pagination.value.pageSize })
    if (res.data) {
      settlements.value = res.data.records || []
      pagination.value.total = res.data.total || 0
    }
  } catch (e) {
    $modal.msgError('加载失败')
  } finally {
    loading.value = false
  }
}

const handleTableChange = (pag) => {
  pagination.value.current = pag.current
  loadData(pag.current)
}

onMounted(() => { loadData() })
</script>

<style scoped>
.settlement-page { padding: 8px 0; }
</style>
