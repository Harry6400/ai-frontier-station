<script setup>
import { computed } from 'vue'
import { useThemeStore } from '../stores/useThemeStore'

const props = defineProps({
  contextLabel: {
    type: String,
    default: 'Current Stage'
  },
  contextValue: {
    type: String,
    default: '门户设计强化'
  }
})

const navItems = [
  { label: '首页', to: '/' },
  { label: '内容广场', to: '/contents' },
  { label: '项目说明', to: '/about' }
]

const themeStore = useThemeStore()

const nextThemeLabel = computed(() => (themeStore.isDark ? '切换浅色' : '切换深色'))
</script>

<template>
  <header class="topbar">
    <RouterLink class="topbar-brand" to="/">
      <span class="topbar-mark"></span>
      <div class="topbar-brand-text">
        <strong>AI前沿情报站</strong>
        <span>AI Insight Curation Platform</span>
      </div>
    </RouterLink>

    <nav class="topbar-nav">
      <RouterLink v-for="item in navItems" :key="item.to" :to="item.to">
        {{ item.label }}
      </RouterLink>
    </nav>

    <div class="topbar-side">
      <div class="topbar-context">
        <span>{{ props.contextLabel }}</span>
        <strong>{{ props.contextValue }}</strong>
      </div>

      <button class="theme-toggle" type="button" @click="themeStore.toggleTheme()">
        <span class="theme-toggle-state">{{ themeStore.modeLabel }}</span>
        <strong>{{ nextThemeLabel }}</strong>
      </button>
    </div>
  </header>
</template>
