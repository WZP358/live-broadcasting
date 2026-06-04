<template>
  <AdminPageLayout title="菜单管理" description="统一维护后台导航菜单、父子层级和状态信息，保证管理入口清晰可用。">
    <AdminCard title="筛选条件" subtitle="按菜单名称快速检索">
      <a-form ref="formRef" :model="formState" @finish="loadMenus">
        <div class="admin-query-grid">
          <a-form-item name="name" label="菜单名称">
            <a-input v-model:value="formState.name" placeholder="请输入菜单名称" />
          </a-form-item>
          <div class="admin-query-actions">
            <a-button type="primary" html-type="submit">搜索</a-button>
            <a-button @click="handleReset">重置</a-button>
          </div>
        </div>
      </a-form>
    </AdminCard>

    <AdminCard title="菜单树" subtitle="支持树形查看与父子菜单维护">
      <AdminToolbar>
        <template #left>
          <a-button type="primary" @click="loadMenus">查询</a-button>
          <a-button @click="handleReset">重置</a-button>
          <a-button type="primary" @click="handleAdd">新增菜单</a-button>
          <a-button @click="loadMenus">刷新</a-button>
        </template>
        <template #right>
          <span>菜单管理直接影响后台导航结构，新增前建议先确认父级层级和路由归属</span>
        </template>
      </AdminToolbar>

      <div class="admin-list-meta">
        <span class="admin-list-meta__highlight">当前树节点 {{ menuOptions.length }} 个</span>
        <span>目录、页面、图标和状态字段统一在这一个入口维护</span>
      </div>

      <div class="admin-table-shell">
        <a-table
          :columns="columns"
          :data-source="dataSource"
          :pagination="false"
          row-key="id"
          size="middle"
          childrenColumnName="children"
          :scroll="{ x: 1450, y: 'calc(100vh - 360px)' }"
        >
          <template #bodyCell="{ column, record }">
            <template v-if="column.key === 'title'">
              <div class="admin-avatar-cell">
                <span class="menu-indicator">{{ record.children?.length ? "目录" : "页面" }}</span>
                <div class="admin-avatar-meta">
                  <div class="admin-avatar-meta__title">{{ record.title || record.label }}</div>
                  <div class="admin-avatar-meta__desc">父级ID：{{ record.pid ?? 0 }}</div>
                </div>
              </div>
            </template>
            <template v-else-if="column.key === 'icon'">
              <div class="admin-avatar-cell">
                <SvgIcon :icon="record.icon" size="15px" />
                <span>{{ record.icon || "-" }}</span>
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
    </AdminCard>

    <MenuDialog v-model:visible="dialogVisible" :title="dialogTitle" :edit-data="editData" :menu-options="menuOptions" @success="loadMenus" />
  </AdminPageLayout>
</template>

<script setup>
import { computed, createVNode, onMounted, reactive, ref } from "vue"
import { ExclamationCircleOutlined } from "@ant-design/icons-vue"
import { Modal, message } from "ant-design-vue"
import systemRoleApi from "@/api/system/systemRole"
import systemApi from "@/api/system"
import SvgIcon from "@/components/SvgIcon/index.vue"
import AdminPageLayout from "@/components/admin/AdminPageLayout.vue"
import AdminCard from "@/components/admin/AdminCard.vue"
import AdminToolbar from "@/components/admin/AdminToolbar.vue"
import { createMenuColumns } from "./columns"
import MenuDialog from "./MenuDialog.vue"

const formRef = ref()
const formState = reactive({
  name: "",
})
const columns = createMenuColumns()
const dataSource = ref([])
const dialogVisible = ref(false)
const dialogTitle = ref("新增菜单")
const editData = ref({})

const flattenMenus = (items = []) =>
  items.flatMap((item) => [item, ...flattenMenus(item.children || [])])

const menuOptions = computed(() =>
  flattenMenus(dataSource.value).map((item) => ({
    id: item.id,
    label: item.label || item.title,
  }))
)

const loadMenus = async () => {
  const res = await systemRoleApi.listMenus({ pid: 0, hidden: 0 })
  const menus = res?.data || []
  if (!formState.name) {
    dataSource.value = menus
    return
  }
  const keyword = formState.name.trim()
  const filterTree = (items = []) =>
    items
      .map((item) => {
        const children = filterTree(item.children || [])
        const matched = (item.label || item.title || "").includes(keyword)
        if (!matched && !children.length) {
          return null
        }
        return {
          ...item,
          children,
        }
      })
      .filter(Boolean)
  dataSource.value = filterTree(menus)
}

const handleReset = () => {
  formRef.value?.resetFields()
  loadMenus()
}

const handleAdd = () => {
  dialogTitle.value = "新增菜单"
  editData.value = {}
  dialogVisible.value = true
}

const handleEdit = (record) => {
  dialogTitle.value = "编辑菜单"
  editData.value = { ...record }
  dialogVisible.value = true
}

const handleDelete = (record) => {
  Modal.confirm({
    title: "确认删除该菜单吗？",
    icon: createVNode(ExclamationCircleOutlined),
    content: `删除对象：${record.title || record.label}`,
    okText: "确认",
    cancelText: "取消",
    onOk: async () => {
      await systemApi.delete("menu", [record.id])
      message.success("删除成功")
      loadMenus()
    },
  })
}

onMounted(() => {
  loadMenus()
})
</script>

<style scoped lang="scss">
.menu-indicator {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 44px;
  padding: 2px 8px;
  border-radius: 999px;
  background: var(--accent-light);
  color: var(--accent);
  font-size: 12px;
}
</style>
