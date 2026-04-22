<template>
  <a-result status="403" title="403" sub-title="你没有权限访问这个页面">
    <template #extra>
      <a-button type="primary" @click="handleGoBack">返回首页</a-button>
    </template>
  </a-result>
</template>

<script setup>
import { useRouter } from "vue-router"

const router = useRouter()

const handleGoBack = () => {
  router.push({ path: "/" })
}
</script>

<style scoped lang="scss"></style>
