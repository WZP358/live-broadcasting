<script setup>
const handleClick = () => {
  window.open("https://beian.miit.gov.cn/", "_blank")
}
</script>

<template>
  <div class="footer">
    <span @click="handleClick()">Copyright © 2026 Ant-Live · 统一直播平台体验演示</span>
  </div>
</template>

<style lang="scss" scoped>
.footer {
  height: 60px;
  text-align: center;
  color: #94a3b8;
  span {
    line-height: 60px;
    font-size: 12px;
  }
  span:hover {
    cursor: pointer;
  }
}
</style>
