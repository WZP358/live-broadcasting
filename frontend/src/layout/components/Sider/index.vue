<template>
  <a-layout-sider class="sider" :class="{ 'sider--collapsed': webStore.menuCollapse }" :collapsed="webStore.menuCollapse">
    <Logo></Logo>
    <Menu></Menu>
  </a-layout-sider>
</template>

<script setup>
import { useStore } from "@/stores"
import Logo from "../Logo.vue"
import Menu from "../Menu/index.vue"

const webStore = useStore().web()
</script>

<style lang="scss" scoped>
.sider {
  overflow: hidden;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--bg-header-soft) 72%, var(--bg-header)) 0%, var(--bg-header) 100%);
  border-inline-end: 0;
  box-shadow: 8px 0 24px color-mix(in srgb, var(--bg-header) 26%, transparent);
}

.sider :deep(.ant-layout-sider-children) {
  display: flex;
  min-height: 100vh;
  flex-direction: column;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--bg-header-soft) 72%, var(--bg-header)) 0%, var(--bg-header) 100%);
}

.sider--collapsed :deep(.logo-wrapper) {
  justify-content: center;
  padding-inline: 0;
}

.sider--collapsed :deep(.logo-copy) {
  display: none;
}
</style>
