<template>
  <div class="app-layout">
    <Header></Header>
    <div class="app-content">
      <RouterView />
    </div>
    <Footer></Footer>
  </div>
</template>

<script setup>
import Header from "@/components/Header.vue"
import Footer from "@/components/Footer.vue"
</script>

<style lang="scss" scoped>
.app-layout {
  min-height: 100vh;
  background:
    linear-gradient(180deg, rgba(255, 153, 0, 0.05) 0, rgba(255, 255, 255, 0) 220px),
    var(--bg-primary);
  .app-content {
    min-height: calc(100vh - 136px);
  }
}
</style>
