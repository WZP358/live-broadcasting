<script setup>
import { onMounted } from "vue"
import { useRouter, useRoute } from "vue-router"

const router = useRouter()
const route = useRoute()

onMounted(() => {
  const redirect = route.query.redirect ? `?redirect=${encodeURIComponent(decodeURIComponent(route.query.redirect))}` : ""
  router.replace(`/login${redirect}`)
})
</script>

<template>
  <div></div>
</template>
