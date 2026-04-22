<template>
  <AdminPageLayout title="角色管理" description="保留角色菜单授权能力，同时把页面拆成更成熟的双栏结构：左侧管理角色，右侧处理菜单授权。">
    <div class="role-layout">
      <AdminCard title="角色列表" subtitle="先选择角色，再在右侧分配菜单">
        <a-form ref="formRef" :model="formState" @finish="loadRoles">
          <div class="admin-query-grid">
            <a-form-item name="name" label="角色名称">
              <a-input v-model:value="formState.name" placeholder="请输入角色名称" />
            </a-form-item>
            <div class="admin-query-actions">
              <a-button type="primary" html-type="submit">搜索</a-button>
              <a-button @click="handleReset">重置</a-button>
            </div>
          </div>
        </a-form>

        <AdminToolbar>
          <template #left>
            <a-button type="primary" @click="loadRoles">查询</a-button>
            <a-button @click="handleReset">重置</a-button>
            <a-button type="primary" @click="handleAddRole">新增角色</a-button>
          </template>
          <template #right>
            <span>建议先完成角色基础信息，再进行菜单授权分配</span>
          </template>
        </AdminToolbar>

        <div class="admin-list-meta">
          <span class="admin-list-meta__highlight">当前结果 {{ roleTotal }} 条</span>
          <span>选择左侧角色后，右侧立即切换到对应菜单授权视图</span>
        </div>

        <div ref="rolePanelRef" class="admin-table-shell">
          <a-table
            :columns="roleColumns"
            :data-source="roleDataSource"
            :pagination="false"
            row-key="id"
            size="middle"
            :custom-row="customRow"
            :row-class-name="getRowClassName"
            :scroll="{ y: roleTableScrollY }"
          >
            <template #bodyCell="{ column, record }">
              <template v-if="column.key === 'level'">
                <a-tag color="blue">级别 {{ record.level }}</a-tag>
              </template>
              <template v-else-if="column.key === 'action'">
                <a-space>
                  <a @click.stop="handleEditRole(record)">编辑</a>
                  <a style="color: #dc2626" @click.stop="handleDeleteRole(record)">删除</a>
                </a-space>
              </template>
            </template>
          </a-table>
        </div>

        <AdminPagination :total="roleTotal" :current="roleCurrent" :page-size="rolePageSize" @change="handleRolePageChange" />
      </AdminCard>

      <AdminCard title="菜单授权" subtitle="为所选角色分配后台菜单权限">
        <template #extra>
          <a-space v-if="selectedRole">
            <a-button type="primary" @click="openAssignDialog">分配菜单</a-button>
            <a-button danger :disabled="selectedAssignedMenuKeys.length === 0" @click="handleBatchRemove">取消分配</a-button>
          </a-space>
        </template>

        <div v-if="!selectedRole" class="empty-panel">
          <a-empty description="请选择左侧角色后再分配菜单" />
        </div>
        <template v-else>
          <div class="role-current">
            当前角色：<strong>{{ selectedRole.name }}</strong>
          </div>
          <div class="admin-list-meta">
            <span class="admin-list-meta__highlight">已分配 {{ currentAssignedIds.length }} 项</span>
            <span>支持单个取消，也支持勾选后批量取消分配</span>
          </div>
          <div class="admin-table-shell">
            <a-table
              :columns="assignedMenuColumns"
              :data-source="assignedMenus"
              :pagination="false"
              row-key="id"
              size="middle"
              :row-selection="{ selectedRowKeys: selectedAssignedMenuKeys, onChange: onAssignedMenuSelectChange }"
              childrenColumnName="children"
            >
              <template #bodyCell="{ column, record }">
                <template v-if="column.key === 'menuType'">
                  <a-tag :color="record.children?.length ? 'blue' : 'green'">
                    {{ record.children?.length ? "菜单" : "节点" }}
                  </a-tag>
                </template>
                <template v-else-if="column.key === 'action'">
                  <a @click="handleRemoveSingle(record)">取消</a>
                </template>
              </template>
            </a-table>
          </div>
        </template>
      </AdminCard>
    </div>

    <RoleDialog v-model:visible="roleModalVisible" :title="roleModalTitle" :edit-data="editData" @success="loadRoles" />

    <a-modal title="分配菜单" :open="assignMenuModalVisible" :confirm-loading="assignConfirmLoading" width="900px" @ok="handleAssignMenuModalOk" @cancel="handleAssignMenuModalCancel">
      <a-table
        :columns="assignColumns"
        :data-source="availableMenus"
        :pagination="false"
        row-key="id"
        size="middle"
        :row-selection="{ selectedRowKeys: selectedAvailableMenuKeys, onChange: onAvailableMenuSelectChange }"
        childrenColumnName="children"
        :scroll="{ y: 420 }"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'menuType'">
            <a-tag :color="record.children?.length ? 'blue' : 'green'">
              {{ record.children?.length ? "菜单" : "节点" }}
            </a-tag>
          </template>
        </template>
      </a-table>
    </a-modal>
  </AdminPageLayout>
</template>

<script setup>
import { computed, createVNode, onMounted, reactive, ref, watch } from "vue"
import { ExclamationCircleOutlined } from "@ant-design/icons-vue"
import { message, Modal } from "ant-design-vue"
import systemApi from "@/api/system"
import systemRoleApi from "@/api/system/systemRole"
import { useTableScroll } from "@/composables/useTableScroll"
import AdminPageLayout from "@/components/admin/AdminPageLayout.vue"
import AdminCard from "@/components/admin/AdminCard.vue"
import AdminToolbar from "@/components/admin/AdminToolbar.vue"
import AdminPagination from "@/components/admin/AdminPagination.vue"
import RoleDialog from "./RoleDialog.vue"
import { assignedMenuColumns, createRoleColumns } from "./columns"

const formRef = ref()
const formState = reactive({
  name: "",
})

const { containerRef: rolePanelRef, tableScrollY: roleTableScrollY } = useTableScroll({
  fixedSelectors: [".admin-card__header"],
  minY: 260,
  extraOffset: 20,
})

const roleColumns = createRoleColumns()
const assignColumns = assignedMenuColumns
const roleDataSource = ref([])
const roleTotal = ref(0)
const roleCurrent = ref(1)
const rolePageSize = ref(10)
const selectedRoleKeys = ref([])
const selectedRole = ref(null)
const assignedMenus = ref([])
const availableMenus = ref([])
const selectedAssignedMenuKeys = ref([])
const selectedAvailableMenuKeys = ref([])
const roleModalVisible = ref(false)
const roleModalTitle = ref("新增角色")
const editData = ref({})
const assignMenuModalVisible = ref(false)
const assignConfirmLoading = ref(false)

const normalizeMenuTree = (items = []) =>
  items.map((item) => ({
    ...item,
    title: item.label || item.title,
    menuType: item.children?.length ? "menu" : "node",
    children: normalizeMenuTree(item.children || []),
  }))

const collectNodeIds = (items = []) =>
  items.flatMap((item) => [item.id, ...collectNodeIds(item.children || [])])

const currentAssignedIds = computed(() => collectNodeIds(assignedMenus.value))

const loadRoles = async () => {
  const res = await systemApi.page("role", {
    pageNo: roleCurrent.value,
    pageSize: rolePageSize.value,
    ...formState,
  })
  roleDataSource.value = res?.data?.list || []
  roleTotal.value = Number(res?.data?.total || 0)
}

const loadAssignedMenus = async (roleId) => {
  const res = await systemRoleApi.listMenusByRole({ roleId, pid: 0 })
  assignedMenus.value = normalizeMenuTree(res?.data || [])
}

watch(selectedRoleKeys, async (keys) => {
  selectedAssignedMenuKeys.value = []
  if (!keys.length) {
    selectedRole.value = null
    assignedMenus.value = []
    return
  }
  selectedRole.value = roleDataSource.value.find((item) => item.id === keys[0]) || null
  if (selectedRole.value) {
    await loadAssignedMenus(selectedRole.value.id)
  }
})

const handleRolePageChange = (page, size) => {
  roleCurrent.value = page
  rolePageSize.value = size
  loadRoles()
}

const handleReset = () => {
  formRef.value?.resetFields()
  roleCurrent.value = 1
  loadRoles()
}

const customRow = (record) => ({
  style: { cursor: "pointer" },
  onClick: () => {
    selectedRoleKeys.value = [record.id]
  },
})

const getRowClassName = (record) => (selectedRoleKeys.value.includes(record.id) ? "ant-table-row-selected" : "")

const handleAddRole = () => {
  roleModalTitle.value = "新增角色"
  editData.value = {}
  roleModalVisible.value = true
}

const handleEditRole = (record) => {
  roleModalTitle.value = "编辑角色"
  editData.value = { ...record }
  roleModalVisible.value = true
}

const handleDeleteRole = (record) => {
  Modal.confirm({
    title: "确认删除该角色吗？",
    icon: createVNode(ExclamationCircleOutlined),
    content: `删除对象：${record.name}`,
    okText: "确认",
    cancelText: "取消",
    onOk: async () => {
      await systemApi.delete("role", [record.id])
      message.success("删除成功")
      if (selectedRoleKeys.value.includes(record.id)) {
        selectedRoleKeys.value = []
      }
      loadRoles()
    },
  })
}

const onAssignedMenuSelectChange = (keys) => {
  selectedAssignedMenuKeys.value = keys
}

const onAvailableMenuSelectChange = (keys) => {
  selectedAvailableMenuKeys.value = keys
}

const openAssignDialog = async () => {
  if (!selectedRole.value) {
    message.warning("请先选择角色")
    return
  }
  const res = await systemRoleApi.listMenus({ pid: 0, hidden: 0 })
  availableMenus.value = normalizeMenuTree(res?.data || [])
  selectedAvailableMenuKeys.value = currentAssignedIds.value
  assignMenuModalVisible.value = true
}

const handleAssignMenuModalCancel = () => {
  assignMenuModalVisible.value = false
  selectedAvailableMenuKeys.value = []
}

const handleAssignMenuModalOk = async () => {
  if (!selectedRole.value) {
    return
  }
  assignConfirmLoading.value = true
  try {
    await systemRoleApi.saveRoleMenus({
      roleId: selectedRole.value.id,
      menuIds: selectedAvailableMenuKeys.value,
    })
    message.success("菜单分配成功")
    assignMenuModalVisible.value = false
    await loadAssignedMenus(selectedRole.value.id)
  } finally {
    assignConfirmLoading.value = false
  }
}

const handleRemoveSingle = async (record) => {
  if (!selectedRole.value) {
    return
  }
  await systemRoleApi.removeRoleMenus({
    roleId: selectedRole.value.id,
    menuIds: [record.id],
  })
  message.success("已取消分配")
  loadAssignedMenus(selectedRole.value.id)
}

const handleBatchRemove = async () => {
  if (!selectedRole.value || !selectedAssignedMenuKeys.value.length) {
    return
  }
  await systemRoleApi.removeRoleMenus({
    roleId: selectedRole.value.id,
    menuIds: selectedAssignedMenuKeys.value,
  })
  message.success("已取消所选菜单分配")
  selectedAssignedMenuKeys.value = []
  loadAssignedMenus(selectedRole.value.id)
}

onMounted(() => {
  loadRoles()
})
</script>

<style scoped lang="scss">
.role-layout {
  display: grid;
  grid-template-columns: 0.95fr 1.25fr;
  gap: 16px;
}

.empty-panel {
  min-height: 320px;
  display: grid;
  place-items: center;
}

.role-current {
  margin-bottom: 14px;
  color: #4e5969;
}

@media (max-width: 1200px) {
  .role-layout {
    grid-template-columns: 1fr;
  }
}
</style>
