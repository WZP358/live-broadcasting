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
  background: #171a22;
  border-inline-end: 0;
  box-shadow: 2px 0 16px rgba(0, 0, 0, 0.12);
}
</style>
