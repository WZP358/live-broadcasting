<script setup>
const handleClick = () => {
  window.open("https://beian.miit.gov.cn/", "_blank")
}
</script>

<template>
  <div class="footer">
    <span @click="handleClick()">PulseLive · 弹幕互动直播平台</span>
  </div>
</template>

<style lang="scss" scoped>
.footer {
  height: 60px;
  text-align: center;
  color: var(--text-muted);
  span {
    line-height: 60px;
    font-size: 12px;
  }
  span:hover {
    cursor: pointer;
  }
}
</style>
