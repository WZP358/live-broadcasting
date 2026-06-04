<template>
  <a-layout-header class="header">
    <a-flex align="center" class="header-inner">
      <div class="header-left">
        <MenuFoldBtn />
        <div class="header-title">
          <strong>直播平台后台</strong>
          <span>统一运营、直播与内容管理</span>
        </div>
      </div>
      <div>
        <HeaderRightBar />
      </div>
    </a-flex>
  </a-layout-header>
</template>

<script setup>
import MenuFoldBtn from "../MenuFoldBtn.vue"
import HeaderRightBar from "../HeaderRightBar/index.vue"
</script>

<style lang="scss" scoped>
.header {
  padding: 0 20px;
  background-color: var(--bg-card);
  border-bottom: 1px solid var(--border);
  box-shadow: 0 1px 0 color-mix(in srgb, var(--text-primary) 4%, transparent);
}

.header-inner {
  height: 100%;
  justify-content: space-between;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-title {
  display: flex;
  flex-direction: column;
  line-height: 1.25;
}

.header-title strong {
  font-size: 15px;
  color: var(--text-primary);
}

.header-title span {
  font-size: 12px;
  color: var(--text-secondary);
}
</style>
