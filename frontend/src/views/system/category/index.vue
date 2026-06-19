<template>
  <AdminPageLayout title="分类管理" description="统一维护直播分类名称、排序与启用状态，支持频道筛选和内容推荐。">
    <AdminCard title="筛选条件" subtitle="支持按分类名称快速检索">
      <a-form ref="formRef" :model="formState" @finish="getData">
        <div class="admin-query-grid">
          <a-form-item name="name" label="分类名称">
            <a-input v-model:value="formState.name" placeholder="请输入分类名称" />
          </a-form-item>
          <div class="admin-query-actions">
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </div>
        </div>
      </a-form>
    </AdminCard>

    <AdminCard title="分类列表" subtitle="风格与用户、直播间、礼物页面保持一致">
      <AdminToolbar>
        <template #left>
          <a-button type="primary" @click="getData">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
          <a-button type="primary" @click="handleAdd">新增分类</a-button>
          <a-button @click="getData">刷新</a-button>
        </template>
        <template #right>
          <span>分类适合作为直播推荐、频道筛选和后台统计的基础维度</span>
        </template>
      </AdminToolbar>

      <div class="admin-list-meta">
        <span class="admin-list-meta__highlight">当前结果 {{ total }} 条</span>
        <span>建议分类名称保持简洁，排序值越大越靠前</span>
      </div>

      <div ref="containerRef" class="admin-table-shell">
        <a-table :columns="columns" :data-source="dataSource" :pagination="false" :scroll="{ y: tableScrollY }" row-key="id" size="middle">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <div class="admin-avatar-cell">
                <a-avatar shape="square">
                  {{ (record.name || "分").slice(0, 1) }}
                </a-avatar>
                <div class="admin-avatar-meta">
                  <div class="admin-avatar-meta__title">{{ record.name }}</div>
                  <div class="admin-avatar-meta__desc">ID：{{ record.id }} · 排序：{{ record.sort ?? 0 }}</div>
                </div>
              </div>
            </template>
            <template v-else-if="column.key === 'status'">
              <a-tag :color="record.status === 0 ? 'success' : 'error'">
                {{ record.status === 0 ? "启用" : "禁用" }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a @click="handleEdit(record)">编辑</a>
                <a style="color: var(--danger)" @click="handleDelete(record)">删除</a>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>

      <AdminPagination :total="total" :current="current" :page-size="pageSize" @change="handlePageChange" />
    </AdminCard>

    <CategoryDialog v-model:visible="dialogVisible" :title="dialogTitle" :edit-data="editData" @success="getData" />
  </AdminPageLayout>
</template>

<script setup>
import { createVNode, onMounted, reactive, ref } from "vue"
import { ExclamationCircleOutlined } from "@ant-design/icons-vue"
import { Modal, message } from "ant-design-vue"
import { useTableScroll } from "@/composables/useTableScroll"
import systemApi from "@/api/system"
import AdminPageLayout from "@/components/admin/AdminPageLayout.vue"
import AdminCard from "@/components/admin/AdminCard.vue"
import AdminToolbar from "@/components/admin/AdminToolbar.vue"
import AdminPagination from "@/components/admin/AdminPagination.vue"
import { createCategoryColumns } from "./columns"
import CategoryDialog from "./CategoryDialog.vue"

const formRef = ref()
const formState = reactive({
  name: "",
})
const columns = createCategoryColumns()
const dataSource = ref([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref("新增分类")
const editData = ref({})
const { containerRef, tableScrollY } = useTableScroll()

const getData = async () => {
  const res = await systemApi.page("category", {
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

const handleAdd = () => {
  dialogTitle.value = "新增分类"
  editData.value = {}
  dialogVisible.value = true
}

const handleEdit = (record) => {
  dialogTitle.value = "编辑分类"
  editData.value = { ...record }
  dialogVisible.value = true
}

const handleDelete = (record) => {
  Modal.confirm({
    title: "确认删除该分类吗？",
    icon: createVNode(ExclamationCircleOutlined),
    content: `删除对象：${record.name}`,
    okText: "确认",
    cancelText: "取消",
    onOk: async () => {
      await systemApi.delete("category", [record.id])
      message.success("删除成功")
      getData()
    },
  })
}

onMounted(() => {
  getData()
})
</script>
