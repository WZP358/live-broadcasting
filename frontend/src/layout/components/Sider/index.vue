<template>
  <a-layout-sider class="sider" :collapsed="webStore.menuCollapse">
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
  background: #304156;
  border-inline-end: 0;
  box-shadow: 2px 0 8px rgba(0, 21, 41, 0.08);
}
</style>
