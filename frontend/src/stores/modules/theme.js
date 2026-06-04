import { defineStore } from 'pinia'
import { ref } from 'vue'

const THEMES = {
  default: {
    name: '默认主题',
    icon: '默',
    colors: {
      '--bg-primary': '#f4f5f8',
      '--bg-secondary': '#fafbfc',
      '--bg-card': '#ffffff',
      '--bg-header': '#151820',
      '--bg-header-soft': '#1f232d',
      '--header-text': '#c7ccd7',
      '--header-text-active': '#ffffff',
      '--text-primary': '#16181d',
      '--text-secondary': '#686d78',
      '--text-muted': '#9aa1ad',
      '--accent': '#ff9900',
      '--accent-text': '#171b24',
      '--accent-strong': '#f59e0b',
      '--accent-soft': '#fff4c7',
      '--accent-light': '#fff8dc',
      '--accent-gradient': 'linear-gradient(135deg, #ffd84d 0%, #ff9900 100%)',
      '--success': '#12a594',
      '--danger': '#ef4444',
      '--warning': '#f59e0b',
      '--border': '#e8ebf0',
      '--border-strong': '#d8dde6',
      '--shadow': '0 2px 10px rgba(21,24,32,0.06)',
      '--shadow-hover': '0 10px 28px rgba(21,24,32,0.14)',
      '--chat-bg': '#ffffff',
      '--player-bg': '#050609',
    },
  },

  cyberpunk: {
    name: '赛博夜场',
    icon: '赛',
    colors: {
      '--bg-primary': '#0a0a1a',
      '--bg-secondary': '#0f0f2a',
      '--bg-card': '#151535',
      '--bg-header': '#0a0a1a',
      '--bg-header-soft': '#151535',
      '--header-text': '#7b8cba',
      '--header-text-active': '#00ffcc',
      '--text-primary': '#00ffcc',
      '--text-secondary': '#7b8cba',
      '--text-muted': '#657399',
      '--accent': '#ff00ff',
      '--accent-text': '#050512',
      '--accent-strong': '#e000e0',
      '--accent-soft': 'rgba(255,0,255,0.18)',
      '--accent-light': 'rgba(255,0,255,0.12)',
      '--accent-gradient': 'linear-gradient(135deg, #00ffcc 0%, #ff00ff 100%)',
      '--success': '#00ffcc',
      '--danger': '#ff4d88',
      '--warning': '#facc15',
      '--border': '#2a2a5a',
      '--border-strong': '#383878',
      '--shadow': '0 4px 24px rgba(0,255,200,0.08)',
      '--shadow-hover': '0 12px 32px rgba(255,0,255,0.18)',
      '--chat-bg': '#101028',
      '--player-bg': '#000010',
    },
  },

  nature: {
    name: '清新绿场',
    icon: '清',
    colors: {
      '--bg-primary': '#f0fdf4',
      '--bg-secondary': '#ffffff',
      '--bg-card': '#ffffff',
      '--bg-header': '#123524',
      '--bg-header-soft': '#17412c',
      '--header-text': '#bbf7d0',
      '--header-text-active': '#ffffff',
      '--text-primary': '#14532d',
      '--text-secondary': '#3f8f5f',
      '--text-muted': '#7aa889',
      '--accent': '#22c55e',
      '--accent-text': '#052e16',
      '--accent-strong': '#16a34a',
      '--accent-soft': '#dcfce7',
      '--accent-light': '#f0fdf4',
      '--accent-gradient': 'linear-gradient(135deg, #86efac 0%, #22c55e 100%)',
      '--success': '#16a34a',
      '--danger': '#dc2626',
      '--warning': '#ca8a04',
      '--border': '#bbf7d0',
      '--border-strong': '#86efac',
      '--shadow': '0 4px 16px rgba(34,197,94,0.08)',
      '--shadow-hover': '0 12px 28px rgba(34,197,94,0.16)',
      '--chat-bg': '#ffffff',
      '--player-bg': '#052e16',
    },
  },

  dark: {
    name: '暗夜模式',
    icon: '夜',
    colors: {
      '--bg-primary': '#0f172a',
      '--bg-secondary': '#1e293b',
      '--bg-card': '#1e293b',
      '--bg-header': '#0f172a',
      '--bg-header-soft': '#1e293b',
      '--header-text': '#94a3b8',
      '--header-text-active': '#e2e8f0',
      '--text-primary': '#e2e8f0',
      '--text-secondary': '#94a3b8',
      '--text-muted': '#64748b',
      '--accent': '#ffb020',
      '--accent-text': '#171b24',
      '--accent-strong': '#ff9900',
      '--accent-soft': 'rgba(255,176,32,0.18)',
      '--accent-light': 'rgba(255,176,32,0.12)',
      '--accent-gradient': 'linear-gradient(135deg, #ffd166 0%, #ff9900 100%)',
      '--success': '#22c55e',
      '--danger': '#fb7185',
      '--warning': '#f59e0b',
      '--border': '#334155',
      '--border-strong': '#475569',
      '--shadow': '0 4px 16px rgba(0,0,0,0.3)',
      '--shadow-hover': '0 14px 34px rgba(0,0,0,0.38)',
      '--chat-bg': '#1e293b',
      '--player-bg': '#020617',
    },
  },
}

export const useThemeStore = defineStore('theme', () => {
  const current = ref(localStorage.getItem('live.theme') || 'default')

  const themeList = Object.entries(THEMES).map(([key, val]) => ({
    key,
    name: val.name,
    icon: val.icon,
  }))

  const applyTheme = (key) => {
    const theme = THEMES[key]
    if (!theme) return

    const root = document.documentElement
    Object.entries(theme.colors).forEach(([cssVar, value]) => {
      root.style.setProperty(cssVar, value)
    })

    root.setAttribute('data-theme', key)
    root.style.colorScheme = key === 'dark' || key === 'cyberpunk' ? 'dark' : 'light'

    current.value = key
    localStorage.setItem('live.theme', key)
  }

  applyTheme(current.value)

  return {
    current,
    themeList,
    applyTheme,
    THEMES,
  }
})
