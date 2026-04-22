<template>
  <AdminPageLayout title="消息管理" description="集中查看直播间聊天消息，支持按消息类型、标签和来源用户快速筛查，为后续风控审查留出扩展空间。">
    <AdminCard title="筛选条件" subtitle="用于回溯直播间互动和异常消息">
      <a-form ref="formRef" :model="formState" @finish="getData">
        <div class="admin-query-grid">
          <a-form-item name="typeName" label="类型名称">
            <a-input v-model:value="formState.typeName" placeholder="请输入类型名称" />
          </a-form-item>
          <a-form-item name="label" label="标签">
            <a-input v-model:value="formState.label" placeholder="请输入标签" />
          </a-form-item>
          <div class="admin-query-actions">
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </div>
        </div>
      </a-form>
    </AdminCard>

    <AdminCard title="消息列表" subtitle="消息列表已经切换为统一后台表格容器">
      <AdminToolbar>
        <template #left>
          <a-button type="primary" @click="getData">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
          <a-button @click="getData">刷新</a-button>
        </template>
        <template #right>
          <span>消息表缺失时已做后端降级处理，不会再把整个后台页面带崩</span>
        </template>
      </AdminToolbar>

      <div class="admin-list-meta">
        <span class="admin-list-meta__highlight">当前结果 {{ total }} 条</span>
        <span>适合后续接入敏感词、风控标签、消息撤回审计</span>
      </div>

      <div ref="containerRef" class="admin-table-shell">
        <a-table :columns="columns" :data-source="dataSource" :pagination="false" :scroll="{ y: tableScrollY }" row-key="id" size="middle">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'roomId'">
              <div class="admin-avatar-meta">
                <div class="admin-avatar-meta__title">{{ record.roomUserNickname || "未知主播" }} 的直播间</div>
                <div class="admin-avatar-meta__desc">房间ID：{{ record.roomId || "-" }}</div>
              </div>
            </template>
            <template v-else-if="column.key === 'type'">
              <a-tag :color="record.type === 1 ? 'blue' : 'orange'">
                {{ record.type === 1 ? "普通文本" : "撤回消息" }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="record.status === 0 ? 'success' : 'error'">
                {{ record.status === 0 ? "正常" : "已删除" }}
              </a-tag>
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
import systemMessageApi from "@/api/system/systemMessage"
import { useTableScroll } from "@/composables/useTableScroll"
import AdminPageLayout from "@/components/admin/AdminPageLayout.vue"
import AdminCard from "@/components/admin/AdminCard.vue"
import AdminToolbar from "@/components/admin/AdminToolbar.vue"
import AdminPagination from "@/components/admin/AdminPagination.vue"
import { createMessageColumns } from "./columns"

const formRef = ref()
const formState = reactive({
  typeName: "",
  label: "",
})
const columns = createMessageColumns()
const dataSource = ref([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const { containerRef, tableScrollY } = useTableScroll()

const getData = async () => {
  const res = await systemMessageApi.pageDetail({
    pageNo: current.value,
    pageSize: pageSize.value,
    ...formState,
  })
  total.value = Number(res?.data?.total || 0)
  dataSource.value = res?.data?.list || []
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
