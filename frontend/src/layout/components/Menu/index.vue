<template>
  <a-menu
    class="sider-menu"
    v-model:openKeys="state.openKeys"
    v-model:selectedKeys="state.selectedKeys"
    mode="inline"
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
  state.openKeys = []
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
  background-color: transparent;
  color: var(--header-text);
}

:deep(.sider-menu.ant-menu) {
  background: transparent;
  color: var(--header-text);
}

:deep(.sider-menu .ant-menu-item),
:deep(.sider-menu .ant-menu-submenu-title) {
  margin-inline: 8px;
  width: auto;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
}

:deep(.sider-menu .ant-menu-item-selected) {
  background: color-mix(in srgb, var(--accent) 18%, transparent);
  color: var(--header-text-active);
  box-shadow: inset 3px 0 0 var(--accent);
}

:deep(.sider-menu .ant-menu-item:hover),
:deep(.sider-menu .ant-menu-submenu-title:hover) {
  color: var(--header-text-active);
  background: color-mix(in srgb, var(--header-text) 12%, transparent);
}

:deep(.sider-menu .ant-menu-sub.ant-menu-inline) {
  background: color-mix(in srgb, var(--header-text) 5%, transparent);
}
</style>
