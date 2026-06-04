<template>
  <AdminPageLayout title="用户管理" description="统一维护用户档案、账号状态与基础联系信息，页面结构按查询区、工具栏、表格区分层组织。">
    <AdminCard title="筛选条件" subtitle="支持按用户ID、账号、昵称等维度快速查询">
      <a-form ref="formRef" :model="formState" @finish="getData">
        <div class="admin-query-grid">
          <a-form-item name="id" label="用户ID">
            <a-input v-model:value="formState.id" placeholder="请输入用户ID" />
          </a-form-item>
          <a-form-item name="username" label="账号">
            <a-input v-model:value="formState.username" placeholder="请输入账号" />
          </a-form-item>
          <a-form-item name="nickname" label="昵称">
            <a-input v-model:value="formState.nickname" placeholder="请输入昵称" />
          </a-form-item>
          <div class="admin-query-actions">
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </div>
        </div>
      </a-form>
    </AdminCard>

    <AdminCard title="用户列表" subtitle="集中查看用户资料、账号状态和运营操作">
      <AdminToolbar>
        <template #left>
          <a-button type="primary" @click="getData">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
          <a-button @click="getData">刷新</a-button>
        </template>
        <template #right>
          <span>默认按最新结果展示，可继续执行导出、批量封禁等运营动作</span>
        </template>
      </AdminToolbar>

      <div class="admin-list-meta">
        <span class="admin-list-meta__highlight">当前结果 {{ total }} 条</span>
        <span>支持账号、昵称、ID 联合检索</span>
      </div>

      <div ref="containerRef" class="admin-table-shell">
        <a-table :columns="columns" :data-source="dataSource" :pagination="false" :scroll="{ x: 1400, y: tableScrollY }" row-key="id" size="middle">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'nickname'">
              <div class="admin-avatar-cell">
                <a-avatar :src="record.avatar">
                  {{ (record.nickname || "用户").slice(0, 1) }}
                </a-avatar>
                <div class="admin-avatar-meta">
                  <div class="admin-avatar-meta__title">{{ record.nickname || "-" }}</div>
                  <div class="admin-avatar-meta__desc">{{ record.signature || "暂无个性签名" }}</div>
                </div>
              </div>
            </template>
            <template v-else-if="column.key === 'sex'">
              <a-tag>{{ sexText(record.sex) }}</a-tag>
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

    <Edit v-model:visible="modalVisible" :title="modalTitle" :edit-data="editData" @success="getData" />
    <Detail v-model:visible="detailVisible" :detail-data="detailData" />
  </AdminPageLayout>
</template>

<script setup>
import { createVNode, onMounted, reactive, ref } from "vue"
import { Modal, message } from "ant-design-vue"
import { ExclamationCircleOutlined } from "@ant-design/icons-vue"
import AdminPageLayout from "@/components/admin/AdminPageLayout.vue"
import AdminCard from "@/components/admin/AdminCard.vue"
import AdminToolbar from "@/components/admin/AdminToolbar.vue"
import AdminPagination from "@/components/admin/AdminPagination.vue"
import { useTableScroll } from "@/composables/useTableScroll"
import systemUserApi from "@/api/systemUser"
import { createUserColumns } from "./columns"
import Detail from "./Detail.vue"
import Edit from "./Edit.vue"

const formRef = ref()
const formState = reactive({
  id: undefined,
  username: "",
  nickname: "",
})

const columns = createUserColumns()
const dataSource = ref([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const modalVisible = ref(false)
const modalTitle = ref("编辑用户")
const editData = ref({})
const detailVisible = ref(false)
const detailData = ref({})
const { containerRef, tableScrollY } = useTableScroll()

const getData = async () => {
  const res = await systemUserApi.getPageUsers({
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

const handleView = (record) => {
  detailData.value = { ...record }
  detailVisible.value = true
}

const handleEdit = (record) => {
  modalTitle.value = "编辑用户"
  editData.value = { ...record }
  modalVisible.value = true
}

const handleToggleStatus = (record, disabled) => {
  const actionText = disabled === -1 ? "封禁" : "解封"
  Modal.confirm({
    title: `确认${actionText}该用户吗？`,
    icon: createVNode(ExclamationCircleOutlined),
    content: `操作对象：${record.nickname || record.username}`,
    okText: "确认",
    cancelText: "取消",
    onOk: async () => {
      await systemUserApi.toggleUserStatus({ id: record.id, disabled })
      message.success(`${actionText}成功`)
      getData()
    },
  })
}

const sexText = (sex) => {
  if (sex === 1) return "男"
  if (sex === 2) return "女"
  return "未知"
}

onMounted(() => {
  getData()
})
</script>
