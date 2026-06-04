<template>
  <AdminPageLayout title="直播间管理" description="统一审核直播间标题、分类、公告和封禁状态，支持按房间、主播、开播状态快速筛查。">
    <AdminCard title="筛选条件" subtitle="先检索直播间，再进行查看、编辑或封禁操作">
      <a-form ref="formRef" :model="formState" @finish="getData">
        <div class="admin-query-grid">
          <a-form-item name="id" label="房间ID">
            <a-input v-model:value="formState.id" placeholder="请输入房间ID" />
          </a-form-item>
          <a-form-item name="title" label="标题">
            <a-input v-model:value="formState.title" placeholder="请输入直播间标题" />
          </a-form-item>
          <a-form-item name="userId" label="主播ID">
            <a-input v-model:value="formState.userId" placeholder="请输入主播ID" />
          </a-form-item>
          <a-form-item name="status" label="直播状态">
            <a-select v-model:value="formState.status" placeholder="请选择直播状态" allow-clear>
              <a-select-option :value="1">直播中</a-select-option>
              <a-select-option :value="0">未开播</a-select-option>
            </a-select>
          </a-form-item>
          <a-form-item name="disabled" label="封禁状态">
            <a-select v-model:value="formState.disabled" placeholder="请选择封禁状态" allow-clear>
              <a-select-option :value="0">正常</a-select-option>
              <a-select-option :value="-1">已封禁</a-select-option>
            </a-select>
          </a-form-item>
          <div class="admin-query-actions">
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </div>
        </div>
      </a-form>
    </AdminCard>

    <AdminCard title="房间列表" subtitle="集中查看直播间资料、开播状态和运营操作">
      <AdminToolbar>
        <template #left>
          <a-button type="primary" @click="getData">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
          <a-button @click="getData">刷新</a-button>
        </template>
        <template #right>
          <span>适合继续扩展推荐位、审核状态、违规标签等直播运营字段</span>
        </template>
      </AdminToolbar>

      <div class="admin-list-meta">
        <span class="admin-list-meta__highlight">当前结果 {{ total }} 条</span>
        <span>按房间、主播、开播状态快速筛查</span>
      </div>

      <div ref="containerRef" class="admin-table-shell">
        <a-table :columns="columns" :data-source="dataSource" :pagination="false" :scroll="{ x: 1500, y: tableScrollY }" row-key="id" size="middle">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'title'">
              <div class="admin-avatar-cell">
                <a-avatar shape="square" :src="record.cover">
                  {{ (record.title || "直播").slice(0, 1) }}
                </a-avatar>
                <div class="admin-avatar-meta">
                  <div class="admin-avatar-meta__title">{{ record.title || "-" }}</div>
                  <div class="admin-avatar-meta__desc">{{ record.introduce || "暂无简介" }}</div>
                </div>
              </div>
            </template>
            <template v-else-if="column.key === 'userInfo'">
              <div class="admin-avatar-cell">
                <a-avatar :src="record.userAvatar">
                  {{ (record.userNickname || "播").slice(0, 1) }}
                </a-avatar>
                <div class="admin-avatar-meta">
                  <div class="admin-avatar-meta__title">{{ record.userNickname || "-" }}</div>
                  <div class="admin-avatar-meta__desc">主播ID：{{ record.userId || "-" }}</div>
                </div>
              </div>
            </template>
            <template v-else-if="column.key === 'categoryInfo'">
              <a-tag color="blue">{{ record.categoryName || "未分类" }}</a-tag>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="record.status === 1 ? 'success' : 'default'">
                {{ record.status === 1 ? "直播中" : "未开播" }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'disabled'">
              <a-tag :color="record.disabled === 0 ? 'success' : 'error'">
                {{ record.disabled === 0 ? "正常" : "已封禁" }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a @click="handleView(record)">详情</a>
                <a @click="handleEdit(record)">编辑</a>
                <a :style="{ color: record.disabled === 0 ? '#dc2626' : '#16a34a' }" @click="handleToggleStatus(record, record.disabled === 0 ? -1 : 0)">
                  {{ record.disabled === 0 ? "封禁" : "解封" }}
                </a>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>

      <AdminPagination :total="total" :current="current" :page-size="pageSize" @change="handlePageChange" />
    </AdminCard>

    <Edit v-model:visible="modalVisible" :title="modalTitle" :edit-data="editData" :category-options="categoryOptions" @success="getData" />
    <Detail v-model:visible="detailVisible" :detail-data="detailData" />
  </AdminPageLayout>
</template>

<script setup>
import { createVNode, onMounted, reactive, ref } from "vue"
import { ExclamationCircleOutlined } from "@ant-design/icons-vue"
import { Modal, message } from "ant-design-vue"
import liveApi from "@/api/live"
import systemRoomApi from "@/api/systemRoom"
import { useTableScroll } from "@/composables/useTableScroll"
import AdminPageLayout from "@/components/admin/AdminPageLayout.vue"
import AdminCard from "@/components/admin/AdminCard.vue"
import AdminToolbar from "@/components/admin/AdminToolbar.vue"
import AdminPagination from "@/components/admin/AdminPagination.vue"
import { createRoomColumns } from "./columns"
import Detail from "./Detail.vue"
import Edit from "./Edit.vue"

const formRef = ref()
const formState = reactive({
  id: undefined,
  title: "",
  userId: undefined,
  status: undefined,
  disabled: undefined,
})

const columns = createRoomColumns()
const dataSource = ref([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const categoryOptions = ref([])
const modalVisible = ref(false)
const modalTitle = ref("编辑直播间")
const editData = ref({})
const detailVisible = ref(false)
const detailData = ref({})
const { containerRef, tableScrollY } = useTableScroll()

const getData = async () => {
  const res = await systemRoomApi.getPageRooms({
    pageNo: current.value,
    pageSize: pageSize.value,
    ...formState,
  })
  total.value = Number(res?.data?.total || 0)
  dataSource.value = res?.data?.list || []
}

const getCategories = async () => {
  const res = await liveApi.listCategories({})
  categoryOptions.value = res?.data?.list || []
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

const handleView = (record) => {
  detailData.value = { ...record }
  detailVisible.value = true
}

const handleEdit = (record) => {
  modalTitle.value = "编辑直播间"
  editData.value = { ...record }
  modalVisible.value = true
}

const handleToggleStatus = (record, disabled) => {
  const actionText = disabled === -1 ? "封禁" : "解封"
  Modal.confirm({
    title: `确认${actionText}该直播间吗？`,
    icon: createVNode(ExclamationCircleOutlined),
    content: `操作对象：${record.title || `房间 ${record.id}`}`,
    okText: "确认",
    cancelText: "取消",
    onOk: async () => {
      await systemRoomApi.toggleRoomStatus({ id: record.id, disabled })
      message.success(`${actionText}成功`)
      getData()
    },
  })
}

onMounted(() => {
  getCategories()
  getData()
})
</script>
