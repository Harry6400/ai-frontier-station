import { defineStore } from 'pinia'

const STORAGE_KEY = 'ai-frontier-portal-theme'

function getSystemTheme() {
  if (typeof window === 'undefined') {
    return 'light'
  }
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light'
}

function applyThemeToDocument(theme) {
  if (typeof document === 'undefined') {
    return
  }
  document.documentElement.dataset.theme = theme
  document.documentElement.style.colorScheme = theme
}

export const useThemeStore = defineStore('portalTheme', {
  state: () => ({
    theme: 'light',
    initialized: false,
    followingSystem: true
  }),
  getters: {
    isDark: (state) => state.theme === 'dark',
    modeLabel: (state) => (state.theme === 'dark' ? '深色模式' : '浅色模式')
  },
  actions: {
    initTheme() {
      if (this.initialized || typeof window === 'undefined') {
        return
      }

      const savedTheme = window.localStorage.getItem(STORAGE_KEY)
      if (savedTheme === 'light' || savedTheme === 'dark') {
        this.followingSystem = false
        this.theme = savedTheme
      } else {
        this.followingSystem = true
        this.theme = getSystemTheme()
      }

      applyThemeToDocument(this.theme)

      const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)')
      mediaQuery.addEventListener('change', (event) => {
        if (!this.followingSystem) {
          return
        }
        this.theme = event.matches ? 'dark' : 'light'
        applyThemeToDocument(this.theme)
      })

      this.initialized = true
    },
    setTheme(theme) {
      this.theme = theme
      this.followingSystem = false
      window.localStorage.setItem(STORAGE_KEY, theme)
      applyThemeToDocument(theme)
    },
    toggleTheme() {
      const nextTheme = this.theme === 'dark' ? 'light' : 'dark'
      this.setTheme(nextTheme)
    }
  }
})
