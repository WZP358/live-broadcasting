<template>
  <div class="data-panel">
    <section class="data-panel__header">
      <div>
        <h2>礼物流水</h2>
        <p>查看观众送礼明细，统一展示礼物名称、数量、单价和赠送时间。</p>
      </div>
      <a-button @click="getData">刷新</a-button>
    </section>

    <section class="data-panel__body">
      <a-table :data-source="dataSource" :columns="columns" :loading="loading" :pagination="pagination" row-key="id" size="middle" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'fromUserNickname'">
            <a-flex align="center">
              <a-avatar v-if="record.fromUserAvatar" :size="28" :src="record.fromUserAvatar" alt="U" />
              <a-avatar v-else :size="28" style="color: #f56a00; background-color: #fde3cf">
                {{ (record.fromUserNickname || "用户").substring(0, 2) }}
              </a-avatar>
              <section style="width: 10px"></section>
              <span>{{ record.fromUserNickname }}</span>
            </a-flex>
          </template>
          <template v-else-if="column.key === 'presentName'">
            <a-flex align="center">
              <a-avatar :size="28" :src="record.presentIcon" alt="U" />
              <section style="width: 10px"></section>
              <span>{{ `${record.presentName} x ${record.number}` }}</span>
            </a-flex>
          </template>
        </template>
      </a-table>
    </section>
  </div>
</template>

<script setup>
import giftApi from "@/api/gift"
import { computed, onMounted, reactive, ref } from "vue"

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

onMounted(() => {
  getData()
})

const getData = () => {
  loading.value = true
  giftApi
    .getRewardRecords({
      pageNo: current.value,
      pageSize: pageSize.value,
    })
    .then((res) => {
      if (res.code === 0) {
        total.value = res.data.total
        dataSource.value = res.data.list
      }
    })
    .finally(() => {
      loading.value = false
    })
}

const handleTableChange = (pag) => {
  current.value = pag.current
  pageSize.value = pag.pageSize
  getData()
}

const columns = reactive([
  {
    title: "序号",
    dataIndex: "index",
    customRender: ({ index }) => index + 1,
    width: 80,
  },
  {
    title: "赠送人",
    dataIndex: "fromUserNickname",
    key: "fromUserNickname",
  },
  {
    title: "礼物",
    dataIndex: "presentName",
    key: "presentName",
  },
  {
    title: "礼物单价",
    dataIndex: "unitPrice",
    key: "unitPrice",
    width: 120,
  },
  {
    title: "开心果总数",
    dataIndex: "totalPrice",
    key: "totalPrice",
    width: 140,
  },
  {
    title: "赠送时间",
    dataIndex: "createTime",
    key: "createTime",
    width: 220,
  },
])
</script>

<style scoped lang="scss">
.data-panel {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.data-panel__header,
.data-panel__body {
  border: 1px solid rgba(148, 163, 184, 0.16);
  border-radius: 22px;
  background: rgba(255, 255, 255, 0.96);
  box-shadow: 0 20px 50px rgba(15, 23, 42, 0.06);
}

.data-panel__header {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 24px 26px;
}

.data-panel__header h2 {
  margin: 0 0 8px;
  color: #0f172a;
  font-size: 24px;
}

.data-panel__header p {
  margin: 0;
  color: #64748b;
  line-height: 1.7;
}

.data-panel__body {
  padding: 18px;
}

@media (max-width: 960px) {
  .data-panel__header {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
