<script setup>
import { computed } from 'vue'
import zhCN from 'ant-design-vue/es/locale/zh_CN'
import { theme as antThemePreset } from 'ant-design-vue'
import { useThemeStore } from '@/stores/modules/theme'

const themeStore = useThemeStore()

const antDesignTheme = computed(() => {
  const key = themeStore.current || 'default'
  const vars = themeStore.THEMES[key]?.colors || themeStore.THEMES.default.colors
  const isDark = key === 'dark' || key === 'cyberpunk'

  return {
    algorithm: isDark ? antThemePreset.darkAlgorithm : antThemePreset.defaultAlgorithm,
    token: {
      borderRadius: 4,
      colorPrimary: vars['--accent'],
      colorInfo: vars['--accent'],
      colorSuccess: vars['--success'],
      colorWarning: vars['--warning'],
      colorError: vars['--danger'],
      colorTextLightSolid: vars['--accent-text'],
      colorBgBase: vars['--bg-primary'],
      colorBgLayout: vars['--bg-primary'],
      colorBgContainer: vars['--bg-card'],
      colorBgElevated: vars['--bg-card'],
      colorText: vars['--text-primary'],
      colorTextSecondary: vars['--text-secondary'],
      colorTextTertiary: vars['--text-muted'],
      colorBorder: vars['--border'],
      colorBorderSecondary: vars['--border'],
      colorFillSecondary: vars['--bg-secondary'],
      colorFillTertiary: vars['--accent-light'],
      colorFillQuaternary: vars['--bg-secondary'],
      colorPrimaryBg: vars['--accent-light'],
      colorPrimaryBgHover: vars['--accent-light'],
      colorBgTextHover: vars['--accent-light'],
      colorLink: vars['--accent'],
      boxShadow: vars['--shadow'],
      boxShadowSecondary: vars['--shadow'],
    },
    components: {
      Button: {
        primaryShadow: 'none',
        defaultShadow: 'none',
      },
      Menu: {
        itemSelectedBg: vars['--accent-light'],
        itemSelectedColor: vars['--accent'],
        itemHoverBg: vars['--accent-light'],
      },
      Table: {
        headerBg: vars['--bg-secondary'],
        headerColor: vars['--text-primary'],
        rowHoverBg: vars['--accent-light'],
        borderColor: vars['--border'],
      },
      Tabs: {
        itemActiveColor: vars['--accent'],
        itemHoverColor: vars['--accent'],
        inkBarColor: vars['--accent'],
      },
      Pagination: {
        itemActiveBg: vars['--bg-card'],
        itemActiveColor: vars['--accent'],
      },
    },
  }
})
</script>

<template>
  <a-config-provider :locale="zhCN" :theme="antDesignTheme">
    <RouterView />
  </a-config-provider>
</template>
