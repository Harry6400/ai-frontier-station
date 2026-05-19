<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useThemeStore } from '../stores/useThemeStore'

const props = defineProps({
  contextLabel: { type: String, default: 'Current Stage' },
  contextValue: { type: String, default: '' }
})

const router = useRouter()
const searchKeyword = ref('')
const mobileMenuOpen = ref(false)

const navItems = [
  { label: '首页', to: '/' },
  { label: '论文', to: '/papers' },
  { label: 'GitHub', to: '/github' },
  { label: 'AI 资讯', to: '/news' },
  { label: '产品动态', to: '/company' },
  { label: '模型评测', to: '/arena' },
  { label: '工具实践', to: '/tools' }
]

const themeStore = useThemeStore()

function handleSearch() {
  const keyword = searchKeyword.value.trim()
  if (keyword) {
    router.push({ path: '/', query: { keyword } })
  }
}
</script>

<template>
  <header class="topbar">
    <div class="topbar-inner">
      <RouterLink class="topbar-brand" to="/">
        <span class="topbar-mark"></span>
        <strong>AI 前沿情报站</strong>
      </RouterLink>

      <nav class="topbar-nav" :class="{ 'is-open': mobileMenuOpen }">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          @click="mobileMenuOpen = false"
        >
          {{ item.label }}
        </RouterLink>
      </nav>

      <div class="topbar-side">
        <div class="topbar-search">
          <input
            v-model="searchKeyword"
            type="text"
            placeholder="搜索内容..."
            @keyup.enter="handleSearch"
          />
        </div>

        <button
          class="theme-toggle"
          type="button"
          @click="themeStore.toggleTheme()"
          :title="themeStore.isDark ? '切换浅色' : '切换深色'"
        >
          <svg v-if="themeStore.isDark" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
          </svg>
          <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
          </svg>
        </button>

        <button class="mobile-menu-btn" type="button" @click="mobileMenuOpen = !mobileMenuOpen">
          <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
            <rect x="2" y="4" width="16" height="2" rx="1" fill="currentColor"/>
            <rect x="2" y="9" width="16" height="2" rx="1" fill="currentColor"/>
            <rect x="2" y="14" width="16" height="2" rx="1" fill="currentColor"/>
          </svg>
        </button>
      </div>
    </div>
  </header>
</template>
