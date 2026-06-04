<script setup>
import { computed } from 'vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { useThemeStore } from '@/stores/modules/theme'

const themeStore = useThemeStore()

const antTheme = computed(() => {
  const isDark = themeStore.current === 'dark' || themeStore.current === 'cyberpunk'
  return {
    token: {
      borderRadius: 4,
      colorPrimary: isDark ? '#ffb020' : '#ff9900',
    },
    algorithm: isDark ? undefined : undefined,
  }
})
</script>

<template>
  <a-config-provider :locale="zhCN" :theme="antTheme">
    <RouterView />
  </a-config-provider>
</template>
