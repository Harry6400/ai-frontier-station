<script setup>
import { computed, onMounted, ref } from 'vue'
import PortalTopbar from '../components/PortalTopbar.vue'
import { usePortalStore } from '../stores/usePortalStore'
import { getPortalHome } from '../api/portal'
import { formatDateTime, getContentTypeMeta } from '../utils/content'

const portalStore = usePortalStore()
const loading = ref(false)
const errorMessage = ref('')
const featuredCards = ref([])
const latestCards = ref([])
const categories = ref([])
const sources = ref([])
const allContents = ref([])
const sidebarCollapsed = ref(false)

function toggleSidebar() {
  sidebarCollapsed.value = !sidebarCollapsed.value
}

const navSections = [
  { emoji: '📄', title: '论文速递', description: 'arXiv 论文', contentType: 'paper', route: '/papers' },
  { emoji: '🐙', title: 'GitHub 开源', description: '开源仓库', contentType: 'project', route: '/github' },
  { emoji: '📰', title: 'AI 资讯', description: '行业新闻', contentType: 'news', route: '/news' },
  { emoji: '⚡', title: '产品动态', description: '模型/API/IDE', contentType: 'company_update', route: '/company' },
  { emoji: '🏆', title: '模型评测', description: '模型评测', contentType: 'arena', route: '/arena', coming: true },
  { emoji: '🔧', title: '工具实践', description: '最佳实践', contentType: 'practice', route: '/tools' }
]

const typeCountMap = computed(() => {
  const map = {}
  for (const item of allContents.value) {
    const t = item.contentType || 'unknown'
    map[t] = (map[t] || 0) + 1
  }
  return map
})

function getSectionCount(ct) {
  return typeCountMap.value[ct] || 0
}

const statCards = computed(() => [
  { label: '已收录', value: allContents.value.length, note: '全部内容' },
  { label: '论文', value: getSectionCount('paper'), note: 'arXiv + HuggingFace' },
  { label: '项目', value: getSectionCount('project'), note: 'GitHub 导入' },
  { label: '数据源', value: sources.value.length, note: '多源聚合' }
])

const heroLeadItem = computed(() => featuredCards.value[0] || latestCards.value[0] || null)
const heroSecondaryItems = computed(() => {
  const bucket = [...featuredCards.value.slice(1), ...latestCards.value]
  const unique = []
  const seen = new Set()
  for (const item of bucket) {
    if (!item || seen.has(item.id) || item.id === heroLeadItem.value?.id) continue
    seen.add(item.id)
    unique.push(item)
    if (unique.length === 3) break
  }
  return unique
})

async function loadHome() {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getPortalHome()
    featuredCards.value = res.data.featuredContents || []
    latestCards.value = res.data.latestContents || []
    categories.value = res.data.categories || []
    sources.value = res.data.sources || []
    const merged = [...featuredCards.value, ...latestCards.value]
    const seen = new Set()
    const unique = []
    for (const item of merged) {
      if (item && !seen.has(item.id)) {
        seen.add(item.id)
        unique.push(item)
      }
    }
    allContents.value = unique
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

onMounted(loadHome)
</script>

<template>
  <div class="portal-shell">
    <PortalTopbar />

    <div class="dashboard-layout" :class="{ 'dashboard-layout--collapsed': sidebarCollapsed }">
      <!-- Left Sidebar -->
      <aside class="dash-sidebar">
        <button class="sb-toggle" type="button" @click="toggleSidebar" :title="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'">
          <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
            <path v-if="!sidebarCollapsed" d="M10 3L5 8L10 13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            <path v-else d="M6 3L11 8L6 13" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>

        <div class="sb-section">
          <div class="sb-title">板块</div>
          <nav class="sb-nav">
            <RouterLink
              v-for="item in navSections"
              :key="item.route"
              :to="item.route"
              class="sb-nav-item"
              :title="item.title"
            >
              <span class="sb-emoji">{{ item.emoji }}</span>
              <span class="sb-label">{{ item.title }}</span>
              <span v-if="!item.coming" class="sb-count">{{ getSectionCount(item.contentType) }}</span>
              <span v-else class="sb-count sb-count--muted">—</span>
            </RouterLink>
          </nav>
        </div>

        <div class="sb-section">
          <div class="sb-title">来源</div>
          <nav class="sb-nav">
            <a
              v-for="item in sources"
              :key="item.id"
              class="sb-nav-item sb-nav-item--static"
              :title="item.name"
            >
              <span class="sb-dot"></span>
              <span class="sb-label">{{ item.name }}</span>
            </a>
          </nav>
        </div>
      </aside>

      <!-- Main Content -->
      <main class="dash-main">
        <!-- Stats Row -->
        <div class="stat-row">
          <article v-for="item in statCards" :key="item.label" class="stat-card">
            <div class="stat-label">{{ item.label }}</div>
            <div class="stat-value">{{ loading ? '—' : item.value }}</div>
            <div class="stat-note">{{ item.note }}</div>
          </article>
        </div>

        <!-- 2-Column Grid: Nav + Featured -->
        <div class="dash-grid-2">
          <!-- Section Nav Panel -->
          <section class="dash-panel">
            <div class="panel-head">
              <h3>板块导航</h3>
              <span class="panel-tag">{{ navSections.length }} 板块</span>
            </div>
            <div class="panel-body">
              <div class="nav-chip-grid">
                <RouterLink
                  v-for="card in navSections"
                  :key="card.route"
                  :to="card.route"
                  class="nav-chip"
                >
                  <span class="nav-chip-emoji">{{ card.emoji }}</span>
                  <div class="nav-chip-info">
                    <strong>{{ card.title }}</strong>
                    <p>{{ card.description }}</p>
                  </div>
                  <span v-if="card.coming" class="nav-chip-num nav-chip-num--muted">敬请期待</span>
                  <span v-else class="nav-chip-num">{{ getSectionCount(card.contentType) }}</span>
                </RouterLink>
              </div>
            </div>
          </section>

          <!-- Featured Panel -->
          <section class="dash-panel">
            <div class="panel-head">
              <h3>精选内容</h3>
              <span class="panel-tag">Top {{ featuredCards.length }}</span>
            </div>
            <div class="panel-body">
              <div v-if="loading" class="panel-loading">加载中...</div>
              <div v-else-if="errorMessage" class="panel-error">{{ errorMessage }}</div>
              <div v-else class="item-list">
                <RouterLink
                  v-for="item in heroSecondaryItems"
                  :key="item.id"
                  :to="`/contents/${item.id}`"
                  class="item-row"
                >
                  <span class="item-chip">{{ getContentTypeMeta(item.contentType).label }}</span>
                  <strong>{{ item.title }}</strong>
                  <span class="item-date">{{ formatDateTime(item.publishedAt) }}</span>
                </RouterLink>
              </div>
            </div>
          </section>
        </div>

        <!-- Latest Panel -->
        <section class="dash-panel">
          <div class="panel-head">
            <h3>最新入库</h3>
            <span class="panel-tag">最近 {{ Math.min(latestCards.length, 6) }} 条</span>
          </div>
          <div class="panel-body">
            <div v-if="loading" class="panel-loading">加载中...</div>
            <div v-else-if="errorMessage" class="panel-error">{{ errorMessage }}</div>
            <div v-else class="item-list">
              <RouterLink
                v-for="item in latestCards.slice(0, 6)"
                :key="item.id"
                :to="`/contents/${item.id}`"
                class="item-row"
              >
                <span class="item-chip">{{ getContentTypeMeta(item.contentType).label }}</span>
                <strong>{{ item.title }}</strong>
                <span class="item-date">{{ formatDateTime(item.publishedAt) }}</span>
              </RouterLink>
            </div>
          </div>
        </section>

        <footer class="dash-footer">
          AI 前沿情报站 · 吴皓睿的个人 AI 信息雷达
        </footer>
      </main>
    </div>
  </div>
</template>
