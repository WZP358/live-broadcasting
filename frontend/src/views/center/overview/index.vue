<template>
  <div class="data-panel">
    <section class="data-panel__header">
      <div>
        <h2>数据总览</h2>
        <p>按场次查看直播开始结束时间、时长、点赞、评论与礼物数据。</p>
      </div>
      <a-button @click="getData">刷新</a-button>
    </section>

    <section class="data-panel__body">
      <a-table :data-source="dataSource" :columns="columns" :loading="loading" :pagination="pagination" row-key="id" size="middle" @change="handleTableChange">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'clickCount'">
            <a-flex align="center">
              <LikeOutlined />
              <section style="width: 5px"></section>
              <span>{{ record.clickCount || "0" }}</span>
            </a-flex>
          </template>
          <template v-else-if="column.key === 'messageCount'">
            <a-flex align="center">
              <CommentOutlined />
              <section style="width: 5px"></section>
              <span>{{ record.messageCount || "0" }}</span>
            </a-flex>
          </template>
          <template v-else-if="column.key === 'presentCount'">
            <a-flex align="center">
              <GiftOutlined />
              <section style="width: 5px"></section>
              <span>{{ record.presentCount || "0" }}</span>
            </a-flex>
          </template>
        </template>
      </a-table>
    </section>
  </div>
</template>

<script setup>
import { LikeOutlined, CommentOutlined, GiftOutlined } from "@ant-design/icons-vue"
import liveAPI from "@/api/live"
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
  liveAPI
    .getLiveStatInfo({
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
    title: "开始时间",
    dataIndex: "startTime",
    key: "startTime",
    width: 220,
  },
  {
    title: "结束时间",
    dataIndex: "endTime",
    key: "endTime",
    width: 220,
  },
  {
    title: "直播时长",
    dataIndex: "time",
    key: "time",
    width: 220,
    customRender: ({ record }) => {
      if (!record.endTime) return "-"
      const startTime = new Date(record.startTime).getTime()
      const endTime = new Date(record.endTime).getTime()
      const totalSeconds = Math.floor((endTime - startTime) / 1000)
      const days = Math.floor(totalSeconds / (3600 * 24))
      const hours = Math.floor((totalSeconds % (3600 * 24)) / 3600)
      const minutes = Math.floor((totalSeconds % 3600) / 60)
      const seconds = totalSeconds % 60
      return `${days ? days + "天" : ""}${hours ? hours + "时" : ""}${minutes ? minutes + "分" : ""}${seconds}秒`
    },
  },
  {
    title: "点赞数",
    dataIndex: "clickCount",
    key: "clickCount",
    width: 120,
  },
  {
    title: "评论数",
    dataIndex: "messageCount",
    key: "messageCount",
    width: 120,
  },
  {
    title: "礼物数据",
    dataIndex: "presentCount",
    key: "presentCount",
    width: 120,
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
