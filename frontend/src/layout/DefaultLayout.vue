<template>
  <div class="app-layout">
    <Header></Header>
    <div class="app-content">
      <RouterView />
    </div>
    <Footer></Footer>
    <XiaoMaiAssistant />
  </div>
</template>

<script setup>
import Header from "@/components/Header.vue"
import Footer from "@/components/Footer.vue"
import XiaoMaiAssistant from "@/components/XiaoMaiAssistant.vue"
</script>

<style lang="scss" scoped>
.app-layout {
  min-height: 100vh;
  background:
    linear-gradient(180deg, color-mix(in srgb, var(--accent) 7%, transparent) 0, transparent 220px),
    var(--bg-primary);
  .app-content {
    min-height: calc(100vh - 136px);
  }
}
</style>
