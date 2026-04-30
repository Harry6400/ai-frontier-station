<script setup>
import { computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAdminThemeStore } from './stores/useAdminThemeStore'

const route = useRoute()
const themeStore = useAdminThemeStore()

const navItems = [
  { label: '概览', to: '/' },
  { label: '内容管理', to: '/contents' },
  { label: '分类管理', to: '/categories' },
  { label: '标签管理', to: '/tags' },
  { label: '来源管理', to: '/sources' },
  { label: 'API 设置', to: '/api-settings' }
]

const headerMetaMap = {
  dashboard: {
    kicker: 'Overview',
    title: '内容运营控制台',
    description: '集中查看当前内容平台的结构与运营状态，保持前后台叙事一致。'
  },
  contents: {
    kicker: 'Content Editor',
    title: '内容录入与发布',
    description: '围绕标题、来源、标签和正文组织内容，保证录入体验与前台展示质量。'
  },
  categories: {
    kicker: 'Taxonomy',
    title: '分类体系维护',
    description: '保持栏目结构稳定，为未来接入更多 AI 内容类型预留统一入口。'
  },
  tags: {
    kicker: 'Tag System',
    title: '标签体系维护',
    description: '通过标签组织主题、技术方向和公司信号，方便后续筛选与推荐扩展。'
  },
  sources: {
    kicker: 'Source Registry',
    title: '来源体系维护',
    description: '管理 GitHub、论文站点、官方博客与人工录入来源，支撑未来采集扩展。'
  },
  'api-settings': {
    kicker: 'Runtime Credentials',
    title: 'API 能力设置',
    description: '在后台临时启用百炼与 GitHub 能力，密钥只保存在后端运行内存中。'
  }
}

const currentHeaderMeta = computed(() => headerMetaMap[route.name] || headerMetaMap.dashboard)
const nextThemeLabel = computed(() => (themeStore.isDark ? '切换浅色' : '切换深色'))

onMounted(() => {
  themeStore.initTheme()
})
</script>

<template>
  <div class="admin-shell">
    <aside class="admin-sidebar">
      <div class="admin-sidebar-top">
        <div>
          <p class="sidebar-kicker">AI Content Ops</p>
          <h1>AI前沿情报站</h1>
          <p class="sidebar-subtitle">后台内容管理端</p>
        </div>

        <nav class="sidebar-nav">
          <RouterLink v-for="item in navItems" :key="item.to" :to="item.to">
            {{ item.label }}
          </RouterLink>
        </nav>
      </div>

      <div class="sidebar-footnote">
        <span class="sidebar-kicker">GitHub-like UI</span>
        <p>这一版后台采用更克制的中性色、低阴影和统一边框，强调稳定、清晰、长期维护。</p>
      </div>
    </aside>

    <main class="admin-main">
      <header class="admin-header">
        <div class="admin-header-copy">
          <span class="sidebar-kicker">{{ currentHeaderMeta.kicker }}</span>
          <h2>{{ currentHeaderMeta.title }}</h2>
          <p>{{ currentHeaderMeta.description }}</p>
        </div>

        <div class="admin-header-actions">
          <button class="admin-theme-toggle" type="button" @click="themeStore.toggleTheme()">
            <span>{{ themeStore.modeLabel }}</span>
            <strong>{{ nextThemeLabel }}</strong>
          </button>

          <a class="portal-entry" href="http://localhost:5173/" target="_blank" rel="noreferrer">
            查看前台门户
          </a>
        </div>
      </header>

      <router-view />
    </main>
  </div>
</template>
