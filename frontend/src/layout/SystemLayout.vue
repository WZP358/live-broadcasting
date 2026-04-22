<template>
  <a-layout class="system-layout">
    <Sider />
    <a-layout>
      <Header />
      <a-layout-content class="system-layout__content">
        <div class="system-layout__breadcrumb">
          <a-breadcrumb>
            <a-breadcrumb-item>管理后台</a-breadcrumb-item>
            <a-breadcrumb-item v-for="item in breadcrumbs" :key="item.path || item.title">
              {{ item.title }}
            </a-breadcrumb-item>
          </a-breadcrumb>
        </div>
        <div class="system-layout__page">
          <RouterView />
        </div>
      </a-layout-content>
      <Footer />
    </a-layout>
  </a-layout>
</template>

<script setup>
import { computed } from "vue"
import { useRoute } from "vue-router"
import Header from "./components/Header/index.vue"
import Sider from "./components/Sider/index.vue"
import Footer from "./components/Footer/index.vue"

const route = useRoute()

const breadcrumbs = computed(() =>
  route.matched
    .filter((item) => item.path.startsWith("/system") && item.meta?.title)
    .map((item) => ({
      path: item.path,
      title: item.meta.title,
    }))
)
</script>

<style scoped lang="scss">
.system-layout {
  min-height: 100vh;
  background: #f5f7f9;
}

.system-layout__content {
  padding: 16px 20px 0;
}

.system-layout__breadcrumb {
  padding: 12px 18px;
  border: 1px solid #ebeef5;
  border-radius: 8px;
  background: #fff;
}

.system-layout__page {
  min-height: calc(100vh - 140px);
  padding: 16px 0;
}
</style>
