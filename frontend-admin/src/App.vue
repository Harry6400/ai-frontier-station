<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { useAdminThemeStore } from './stores/useAdminThemeStore'
import { useAuthStore } from './stores/useAuthStore'

const route = useRoute()
const router = useRouter()
const themeStore = useAdminThemeStore()
const authStore = useAuthStore()

const isLoginPage = computed(() => route.name === 'login')
const isSidebarCollapsed = ref(false)

function toggleSidebar() {
  isSidebarCollapsed.value = !isSidebarCollapsed.value
}

const navItems = [
  { label: '概览', to: '/' },
  { label: '内容管理', to: '/contents' },
  { label: '分类管理', to: '/categories' },
  { label: '来源管理', to: '/sources' },
  { label: 'API 设置', to: '/api-settings' }
]

const pageTitleMap = {
  dashboard: '概览',
  contents: '内容管理',
  categories: '分类管理',
  sources: '来源管理',
  'api-settings': 'API 设置'
}

const currentTitle = computed(() => pageTitleMap[route.name] || '概览')
const nextThemeLabel = computed(() => (themeStore.isDark ? '切换浅色' : '切换深色'))

onMounted(() => {
  themeStore.initTheme()
  if (authStore.isLoggedIn) {
    authStore.fetchCurrentUser()
  }
})

async function handleLogout() {
  try {
    await ElMessageBox.confirm('确认退出登录吗？', '退出登录', { type: 'warning' })
    authStore.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  } catch {
    // cancelled
  }
}
</script>

<template>
  <div v-if="isLoginPage">
    <router-view />
  </div>
  <div v-else class="admin-shell" :class="{ 'admin-shell--collapsed': isSidebarCollapsed }">
    <aside class="admin-sidebar">
      <div class="admin-sidebar-top">
        <button class="sidebar-toggle-btn" type="button" @click="toggleSidebar" :title="isSidebarCollapsed ? '展开侧边栏' : '收起侧边栏'">
          <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
            <rect v-if="!isSidebarCollapsed" x="2" y="3" width="14" height="2" rx="1" fill="currentColor"/>
            <rect v-if="!isSidebarCollapsed" x="2" y="8" width="10" height="2" rx="1" fill="currentColor"/>
            <rect v-if="!isSidebarCollapsed" x="2" y="13" width="14" height="2" rx="1" fill="currentColor"/>
            <rect v-if="isSidebarCollapsed" x="2" y="3" width="14" height="2" rx="1" fill="currentColor"/>
            <rect v-if="isSidebarCollapsed" x="4" y="8" width="12" height="2" rx="1" fill="currentColor"/>
            <rect v-if="isSidebarCollapsed" x="2" y="13" width="14" height="2" rx="1" fill="currentColor"/>
          </svg>
        </button>
        <div class="sidebar-brand">
          <h1>情报站</h1>
        </div>

        <nav class="sidebar-nav">
          <RouterLink v-for="item in navItems" :key="item.to" :to="item.to">
            {{ item.label }}
          </RouterLink>
        </nav>
      </div>
    </aside>

    <main class="admin-main">
      <header class="admin-header">
        <div class="admin-header-copy">
          <h2>{{ currentTitle }}</h2>
        </div>

        <div class="admin-header-actions">
          <div class="user-info">
            <span>{{ authStore.displayName || authStore.username }}</span>
            <el-button size="small" @click="handleLogout">退出</el-button>
          </div>

          <button class="admin-theme-toggle" type="button" @click="themeStore.toggleTheme()" :title="themeStore.isDark ? '切换浅色' : '切换深色'">
            <svg v-if="themeStore.isDark" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <circle cx="12" cy="12" r="5"/><line x1="12" y1="1" x2="12" y2="3"/><line x1="12" y1="21" x2="12" y2="23"/><line x1="4.22" y1="4.22" x2="5.64" y2="5.64"/><line x1="18.36" y1="18.36" x2="19.78" y2="19.78"/><line x1="1" y1="12" x2="3" y2="12"/><line x1="21" y1="12" x2="23" y2="12"/><line x1="4.22" y1="19.78" x2="5.64" y2="18.36"/><line x1="18.36" y1="5.64" x2="19.78" y2="4.22"/>
            </svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <path d="M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"/>
            </svg>
          </button>

          <a class="portal-entry" href="http://localhost:5173/" target="_blank" rel="noreferrer">
            查看前台
          </a>
        </div>
      </header>

      <router-view />
    </main>
  </div>
</template>

<style scoped>
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: var(--color-text-secondary);
}
</style>
