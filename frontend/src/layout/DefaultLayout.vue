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
    radial-gradient(circle at top, rgba(59, 130, 246, 0.08), transparent 24%),
    linear-gradient(180deg, #f8fafc 0%, #eff4fb 100%);
  .app-content {
    min-height: calc(100vh - 136px);
  }
}
</style>
