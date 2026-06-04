<template>
  <AdminPageLayout title="礼物管理" description="统一维护礼物名称、价格、图标、排序和上下架状态。">
    <AdminCard title="筛选条件" subtitle="按礼物名称检索配置项">
      <a-form ref="formRef" :model="formState" @finish="getData">
        <div class="admin-query-grid">
          <a-form-item name="name" label="礼物名称">
            <a-input v-model:value="formState.name" placeholder="请输入礼物名称" />
          </a-form-item>
          <div class="admin-query-actions">
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </div>
        </div>
      </a-form>
    </AdminCard>

    <AdminCard title="礼物列表" subtitle="集中管理礼物展示、价格和可用状态">
      <AdminToolbar>
        <template #left>
          <a-button type="primary" @click="getData">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
          <a-button type="primary" @click="handleAdd">新增礼物</a-button>
          <a-button @click="getData">刷新</a-button>
        </template>
        <template #right>
          <span>礼物配置建议维护图标、价格与展示顺序，便于前台和直播间统一渲染</span>
        </template>
      </AdminToolbar>

      <div class="admin-list-meta">
        <span class="admin-list-meta__highlight">当前结果 {{ total }} 条</span>
        <span>礼物图标建议统一尺寸，价格建议按平台货币最小单位维护</span>
      </div>

      <div ref="containerRef" class="admin-table-shell">
        <a-table :columns="columns" :data-source="dataSource" :pagination="false" :scroll="{ y: tableScrollY }" row-key="id" size="middle">
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'name'">
              <div class="admin-avatar-cell">
                <a-avatar shape="square" :src="record.icon">
                  {{ (record.name || "礼").slice(0, 1) }}
                </a-avatar>
                <div class="admin-avatar-meta">
                  <div class="admin-avatar-meta__title">{{ record.name }}</div>
                  <div class="admin-avatar-meta__desc">{{ record.icon || "未配置图标地址" }}</div>
                </div>
              </div>
            </template>
            <template v-else-if="column.key === 'price'">
              <span>¥ {{ Number(record.price || 0).toFixed(2) }}</span>
            </template>
            <template v-else-if="column.key === 'disabled'">
              <a-tag :color="record.disabled === 0 ? 'success' : 'error'">
                {{ record.disabled === 0 ? "启用" : "禁用" }}
              </a-tag>
            </template>
            <template v-else-if="column.key === 'action'">
              <a-space>
                <a @click="handleEdit(record)">编辑</a>
                <a style="color: #dc2626" @click="handleDelete(record)">删除</a>
              </a-space>
            </template>
          </template>
        </a-table>
      </div>

      <AdminPagination :total="total" :current="current" :page-size="pageSize" @change="handlePageChange" />
    </AdminCard>

    <GiftDialog v-model:visible="dialogVisible" :title="dialogTitle" :edit-data="editData" @success="getData" />
  </AdminPageLayout>
</template>

<script setup>
import { createVNode, onMounted, reactive, ref } from "vue"
import { ExclamationCircleOutlined } from "@ant-design/icons-vue"
import { Modal, message } from "ant-design-vue"
import systemGiftApi from "@/api/systemGift"
import systemApi from "@/api/system"
import { useTableScroll } from "@/composables/useTableScroll"
import AdminPageLayout from "@/components/admin/AdminPageLayout.vue"
import AdminCard from "@/components/admin/AdminCard.vue"
import AdminToolbar from "@/components/admin/AdminToolbar.vue"
import AdminPagination from "@/components/admin/AdminPagination.vue"
import { createGiftColumns } from "./columns"
import GiftDialog from "./GiftDialog.vue"

const formRef = ref()
const formState = reactive({
  name: "",
})
const columns = createGiftColumns()
const dataSource = ref([])
const total = ref(0)
const current = ref(1)
const pageSize = ref(10)
const dialogVisible = ref(false)
const dialogTitle = ref("新增礼物")
const editData = ref({})
const { containerRef, tableScrollY } = useTableScroll()

const getData = async () => {
  const res = await systemGiftApi.getPageGifts({
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
  dialogTitle.value = "新增礼物"
  editData.value = {}
  dialogVisible.value = true
}

const handleEdit = (record) => {
  dialogTitle.value = "编辑礼物"
  editData.value = { ...record }
  dialogVisible.value = true
}

const handleDelete = (record) => {
  Modal.confirm({
    title: "确认删除该礼物吗？",
    icon: createVNode(ExclamationCircleOutlined),
    content: `删除对象：${record.name}`,
    okText: "确认",
    cancelText: "取消",
    onOk: async () => {
      await systemApi.delete("gift", [record.id])
      message.success("删除成功")
      getData()
    },
  })
}

onMounted(() => {
  getData()
})
</script>
