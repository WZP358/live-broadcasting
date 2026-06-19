<template>
  <a-menu
    class="sider-menu"
    v-model:openKeys="state.openKeys"
    v-model:selectedKeys="state.selectedKeys"
    mode="inline"
    theme="dark"
    :inline-collapsed="menuCollapse"
    :items="items"
    @click="handleClick"
  />
</template>

<script setup>
import systemApi from "@/api/system"
import { ref, watch, h, onMounted, reactive, computed } from "vue"
import { useStore } from "@/stores"
import { useRouter } from "vue-router"
import SvgIcon from "@/components/SvgIcon/index.vue"

const router = useRouter()
const state = reactive({
  collapsed: false,
  selectedKeys: [""],
  openKeys: [""],
  preOpenKeys: [""],
})
const items = ref([])
const webStore = useStore().web()
const menuCollapse = computed(() => webStore.menuCollapse)
const selectItems = ref([])
const availableSystemPaths = new Set(
  router.getRoutes()
    .map((route) => route.path)
    .filter((path) => path.startsWith("/system"))
)
const temporarilyHiddenPaths = new Set([
  "/system/message-manage",
  "/system/system-manage/dictionary-manage",
])

watch(
  () => state.openKeys,
  (_val, oldVal) => {
    state.preOpenKeys = oldVal
  }
)

onMounted(() => {
  getMenus()
})

const handleClick = (e) => {
  selectItems.value = []
  getSelectMenus(0, e.keyPath, items.value)
  if (selectItems.value.some((item) => !item)) {
    return
  }
  webStore.setMenuSelect(selectItems.value)
  router.push("/system/" + selectItems.value.map((item) => item.path).join("/"))
}

const getSelectMenus = (i, path, menus) => {
  if (path.length <= i) return
  const key = path[i]
  const menu = menus.find((item) => item.key === key)
  if (!menu) return
  selectItems.value.push(menu)
  getSelectMenus(i + 1, path, menu.children || [])
}

const getMenus = async () => {
  const res = await systemApi.getMenus()
  items.value = buildMenuItems(res.data)
  ensureAuditMenu()
  ensureCustomerServiceMenu()
  ensureGiftFlowMenu()
  state.openKeys = []
}

const ensureAuditMenu = () => {
  const hasAuditMenu = items.value.some((item) => item.path === "content-audit")
  if (hasAuditMenu || !availableSystemPaths.has("/system/content-audit")) {
    return
  }
  items.value.push({
    key: "content-audit",
    label: "内容审核",
    title: "内容审核",
    path: "content-audit",
  })
}

const ensureCustomerServiceMenu = () => {
  const hasCustomerServiceMenu = items.value.some((item) => item.path === "customer-service")
  if (hasCustomerServiceMenu || !availableSystemPaths.has("/system/customer-service")) {
    return
  }
  items.value.push({
    key: "customer-service",
    label: "客服处理",
    title: "客服处理",
    path: "customer-service",
  })
}

const ensureGiftFlowMenu = () => {
  const hasGiftFlowMenu = items.value.some((item) => item.path === "gift-flow")
  if (hasGiftFlowMenu || !availableSystemPaths.has("/system/gift-flow")) {
    return
  }
  items.value.push({
    key: "gift-flow",
    label: "礼物流水",
    title: "礼物流水",
    path: "gift-flow",
  })
}

const buildMenuItems = (menus, parentPaths = []) => {
  return (menus || []).flatMap((item) => {
    const currentPaths = [...parentPaths, item.path]
    const fullPath = "/system/" + currentPaths.join("/")
    const children = buildMenuItems(item.children, currentPaths)
    const hasRoute = availableSystemPaths.has(fullPath)

    if (temporarilyHiddenPaths.has(fullPath)) {
      return []
    }

    if (!hasRoute && children.length === 0) {
      return []
    }

    const menuItem = {
      key: `${item.id}`,
      label: item.label,
      title: item.label,
      path: item.path,
      icon: () => h(SvgIcon, { icon: item.icon, size: "15px" }),
    }

    if (children.length > 0) {
      menuItem.children = children
    }

    return [menuItem]
  })
}
</script>

<style lang="scss" scoped>
.sider-menu {
  flex: 1;
  min-height: 0;
  padding: 12px 10px 18px;
  overflow-y: auto;
  background: transparent !important;
  border-inline-end: 0 !important;
  color: var(--header-text);
}

:deep(.sider-menu.ant-menu) {
  background: transparent !important;
  border-inline-end: 0 !important;
  color: var(--header-text) !important;
}

:deep(.sider-menu.ant-menu-dark),
:deep(.sider-menu.ant-menu-dark .ant-menu-sub),
:deep(.sider-menu.ant-menu-dark .ant-menu-inline.ant-menu-sub) {
  background: transparent !important;
  color: var(--header-text) !important;
}

:deep(.sider-menu .ant-menu-item),
:deep(.sider-menu .ant-menu-submenu-title) {
  margin: 3px 0;
  padding-inline: 14px !important;
  width: auto;
  height: 42px;
  border-radius: 7px;
  color: var(--header-text) !important;
  font-size: 14px;
  line-height: 42px;
}

:deep(.sider-menu .ant-menu-title-content) {
  font-weight: 700;
}

:deep(.sider-menu .ant-menu-item .ant-menu-item-icon),
:deep(.sider-menu .ant-menu-submenu-title .ant-menu-item-icon),
:deep(.sider-menu .ant-menu-submenu-arrow) {
  color: color-mix(in srgb, var(--header-text) 88%, transparent) !important;
}

:deep(.sider-menu .ant-menu-item-selected) {
  color: var(--header-text-active) !important;
  background:
    linear-gradient(90deg, color-mix(in srgb, var(--accent) 28%, transparent), color-mix(in srgb, var(--accent) 12%, transparent)) !important;
  box-shadow:
    inset 3px 0 0 var(--accent),
    0 8px 18px color-mix(in srgb, var(--bg-header) 36%, transparent);
}

:deep(.sider-menu .ant-menu-item-selected .ant-menu-item-icon),
:deep(.sider-menu .ant-menu-submenu-selected > .ant-menu-submenu-title),
:deep(.sider-menu .ant-menu-submenu-selected > .ant-menu-submenu-title .ant-menu-item-icon),
:deep(.sider-menu .ant-menu-submenu-selected > .ant-menu-submenu-title .ant-menu-submenu-arrow) {
  color: var(--header-text-active) !important;
}

:deep(.sider-menu .ant-menu-item:hover),
:deep(.sider-menu .ant-menu-submenu-title:hover) {
  color: var(--header-text-active) !important;
  background: color-mix(in srgb, var(--header-text) 11%, transparent) !important;
}

:deep(.sider-menu .ant-menu-item:hover .ant-menu-item-icon),
:deep(.sider-menu .ant-menu-submenu-title:hover .ant-menu-item-icon),
:deep(.sider-menu .ant-menu-submenu-title:hover .ant-menu-submenu-arrow) {
  color: var(--header-text-active) !important;
}

:deep(.sider-menu .ant-menu-sub.ant-menu-inline) {
  margin: 2px 0 6px;
  padding: 4px 0 4px 10px;
  border-left: 1px solid color-mix(in srgb, var(--header-text) 12%, transparent);
  background: color-mix(in srgb, var(--bg-header-soft) 42%, transparent) !important;
}

:deep(.sider-menu .ant-menu-sub.ant-menu-inline .ant-menu-item) {
  height: 38px;
  margin: 2px 0;
  padding-inline-start: 18px !important;
  line-height: 38px;
}

:deep(.sider-menu.ant-menu-inline-collapsed) {
  width: 100%;
  padding-inline: 8px;
}

:deep(.sider-menu.ant-menu-inline-collapsed .ant-menu-item),
:deep(.sider-menu.ant-menu-inline-collapsed .ant-menu-submenu-title) {
  display: flex;
  justify-content: center;
  padding-inline: 0 !important;
}

:deep(.sider-menu.ant-menu-inline-collapsed .ant-menu-item-selected) {
  box-shadow: inset 0 -3px 0 var(--accent);
}
</style>
