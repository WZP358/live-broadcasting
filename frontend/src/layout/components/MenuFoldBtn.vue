<template>
  <a-button shape="circle" class="menu-btn" @click="handleBtnClick">
    <template #icon>
      <MenuUnfoldOutlined v-if="webStore.menuCollapse" />
      <MenuFoldOutlined v-else />
    </template>
  </a-button>
</template>

<script setup>
import { MenuFoldOutlined, MenuUnfoldOutlined } from "@ant-design/icons-vue"
import { useStore } from "@/stores"

const webStore = useStore().web()
const handleBtnClick = () => {
  webStore.setMenuCollapse(!webStore.menuCollapse)
}
</script>

<style scoped lang="scss">
.menu-btn {
  border-color: #dcdfe6;
  box-shadow: none;
  color: #606266;
}
</style>
