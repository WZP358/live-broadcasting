<template>
  <a-dropdown trigger="click" placement="bottomRight">
    <button class="theme-switch-btn" type="button">
      {{ currentTheme.icon }} {{ currentTheme.name }}
    </button>
    <template #overlay>
      <div class="theme-dropdown">
        <div class="theme-dropdown__title">选择主题</div>
        <button
          v-for="t in themeList"
          :key="t.key"
          :class="['theme-option', { active: store.current === t.key }]"
          type="button"
          @click="store.applyTheme(t.key)"
        >
          <span class="theme-option__icon">{{ t.icon }}</span>
          <span class="theme-option__name">{{ t.name }}</span>
          <span v-if="store.current === t.key" class="theme-option__check">✓</span>
        </button>
      </div>
    </template>
  </a-dropdown>
</template>

<script setup>
import { computed } from 'vue'
import { useThemeStore } from '@/stores/modules/theme'

const store = useThemeStore()
const themeList = store.themeList

const currentTheme = computed(() => {
  const t = store.THEMES[store.current]
  return t || { icon: '🏠', name: '主题' }
})
</script>

<style scoped lang="scss">
.theme-switch-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  height: 36px;
  padding: 0 10px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.06);
  color: var(--header-text);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;

  &:hover {
    color: #fff;
    border-color: rgba(255, 153, 0, 0.4);
    background: rgba(255, 153, 0, 0.16);
  }
}

.theme-dropdown {
  width: 200px;
  padding: 8px;
}

.theme-dropdown__title {
  padding: 8px 12px;
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 1px;
}

.theme-option {
  display: flex;
  align-items: center;
  gap: 10px;
  width: 100%;
  padding: 10px 12px;
  border: 1px solid transparent;
  border-radius: 8px;
  background: transparent;
  color: var(--text-primary);
  cursor: pointer;
  transition: all 0.15s;

  &:hover {
    background: var(--accent-light);
  }

  &.active {
    border-color: var(--accent);
    background: var(--accent-light);
    font-weight: 600;
  }
}

.theme-option__icon {
  font-size: 18px;
}

.theme-option__name {
  flex: 1;
  text-align: left;
  font-size: 14px;
}

.theme-option__check {
  color: var(--accent);
  font-weight: 700;
  font-size: 16px;
}
</style>
