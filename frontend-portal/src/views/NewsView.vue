<script setup>
import { computed, ref, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import PortalTopbar from '../components/PortalTopbar.vue'
import { useViewMode, useTabs } from '../composables/useViewMode'
import { getContentByType } from '../api/portal'

const { viewMode, setViewMode } = useViewMode('news-view', 'stream')
const { activeTab, setTab } = useTabs('全部')

const tabs = ['全部', 'AI政策', '行业实践', '技术突破']

const today = new Date().toLocaleDateString('zh-CN', { month: 'long', day: 'numeric', weekday: 'long' })

const loading = ref(false)
const newsItems = ref([])

async function fetchData() {
  loading.value = true
  try {
    const res = await getContentByType('news', { pageNum: 1, pageSize: 50 })
    const data = res.data?.data || res.data
    const records = data?.records || []
    newsItems.value = records.map((item, index) => ({
      id: item.id,
      category: item.categoryName || '行业实践',
      title: item.title || '',
      excerpt: item.summary || '',
      source: item.sourceName || item.authorName || '',
      region: '',
      time: item.publishedAt || '',
      color: 'purple'
    }))
  } catch (e) {
    console.error('Failed to fetch news:', e)
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

const filteredNews = computed(() => {
  if (activeTab.value === '全部') return newsItems.value
  return newsItems.value.filter(n => n.category === activeTab.value)
})

const categoryColors = {
  'AI政策': { bg: 'var(--red-soft)', color: 'var(--red)', label: '政策' },
  '行业实践': { bg: 'var(--teal-soft)', color: 'var(--teal)', label: '实践' },
  '技术突破': { bg: 'var(--purple-soft)', color: 'var(--purple)', label: '技术' }
}

const todayCount = computed(() => newsItems.value.length)
const sourceCount = computed(() => new Set(newsItems.value.map(n => n.source)).size)

const heroItem = computed(() => filteredNews.value[0] || null)

const groupedByCategory = computed(() => {
  const groups = {}
  for (const item of filteredNews.value.slice(1)) {
    if (!groups[item.category]) groups[item.category] = []
    groups[item.category].push(item)
  }
  return groups
})
</script>

<template>
  <div class="portal-shell">
    <PortalTopbar />

    <main class="news-page">
      <!-- Daily Header -->
      <header class="news-header">
        <div class="news-header-left">
          <span class="news-header-icon">📰</span>
          <div>
            <h1>AI 资讯</h1>
            <p class="news-header-date">{{ today }}</p>
          </div>
        </div>
        <div class="news-header-stats">
          <div class="news-stat-chip">
            <span class="news-stat-num">{{ todayCount }}</span>
            <span class="news-stat-label">今日资讯</span>
          </div>
          <div class="news-stat-chip">
            <span class="news-stat-num">{{ sourceCount }}</span>
            <span class="news-stat-label">信息来源</span>
          </div>
        </div>
      </header>

      <!-- Category Tabs -->
      <nav class="news-tabs">
        <button
          v-for="tab in tabs"
          :key="tab"
          :class="['news-tab', { active: activeTab === tab }]"
          @click="setTab(tab)"
        >
          {{ tab }}
        </button>
      </nav>

      <!-- Toolbar -->
      <div class="news-toolbar">
        <span class="news-toolbar-info">共 {{ filteredNews.length }} 条资讯</span>
        <div class="news-view-toggle">
          <button
            :class="['news-toggle-btn', { active: viewMode === 'stream' }]"
            @click="setViewMode('stream')"
            title="信息流视图"
          >
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <rect x="1" y="2" width="14" height="2" rx="1" fill="currentColor"/>
              <rect x="1" y="7" width="14" height="2" rx="1" fill="currentColor"/>
              <rect x="1" y="12" width="14" height="2" rx="1" fill="currentColor"/>
            </svg>
          </button>
          <button
            :class="['news-toggle-btn', { active: viewMode === 'magazine' }]"
            @click="setViewMode('magazine')"
            title="杂志视图"
          >
            <svg width="16" height="16" viewBox="0 0 16 16" fill="none">
              <rect x="1" y="1" width="14" height="7" rx="1.5" fill="currentColor"/>
              <rect x="1" y="10" width="6" height="5" rx="1.5" fill="currentColor"/>
              <rect x="9" y="10" width="6" height="5" rx="1.5" fill="currentColor"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- Loading State -->
      <div v-if="loading" style="text-align:center;padding:40px;color:var(--text-tertiary);">加载中...</div>

      <!-- Stream View -->
      <div v-else-if="viewMode === 'stream'" class="news-stream">
        <RouterLink
          v-for="(item, index) in filteredNews"
          :key="item.id"
          :to="'/contents/' + item.id"
          class="news-stream-item"
          style="text-decoration: none; color: inherit;"
        >
          <span class="news-stream-num">{{ index + 1 }}</span>
          <div class="news-stream-content">
            <div class="news-stream-top">
              <span
                class="news-cat-tag"
                :style="{ background: categoryColors[item.category]?.bg, color: categoryColors[item.category]?.color }"
              >
                {{ categoryColors[item.category]?.label || item.category }}
              </span>
            </div>
            <h3 class="news-stream-title">{{ item.title }}</h3>
            <p class="news-stream-excerpt">{{ item.excerpt }}</p>
            <div class="news-stream-meta">
              <span class="news-meta-source">{{ item.source }}</span>
              <span class="news-meta-sep">·</span>
              <span class="news-meta-time">{{ item.time }}</span>
            </div>
          </div>
        </RouterLink>
      </div>

      <!-- Magazine View -->
      <div v-else class="news-magazine">
        <!-- Hero Card -->
        <RouterLink v-if="heroItem" :to="'/contents/' + heroItem.id" class="news-hero" style="text-decoration: none; color: inherit;">
          <div class="news-hero-inner">
            <span
              class="news-cat-tag news-cat-tag--lg"
              :style="{ background: categoryColors[heroItem.category]?.bg, color: categoryColors[heroItem.category]?.color }"
            >
              {{ categoryColors[heroItem.category]?.label || heroItem.category }}
            </span>
            <h2 class="news-hero-title">{{ heroItem.title }}</h2>
            <p class="news-hero-excerpt">{{ heroItem.excerpt }}</p>
            <div class="news-hero-meta">
              <span class="news-meta-source">{{ heroItem.source }}</span>
              <span class="news-meta-sep">·</span>
              <span class="news-meta-time">{{ heroItem.time }}</span>
            </div>
          </div>
        </RouterLink>

        <!-- Grouped Cards -->
        <div v-for="(items, cat) in groupedByCategory" :key="cat" class="news-group">
          <h3 class="news-group-title">
            <span
              class="news-group-dot"
              :style="{ background: categoryColors[cat]?.color }"
            ></span>
            {{ cat }}
            <span class="news-group-count">{{ items.length }}</span>
          </h3>
          <div class="news-card-grid">
            <RouterLink v-for="item in items" :key="item.id" :to="'/contents/' + item.id" class="news-card" style="text-decoration: none; color: inherit;">
              <span
                class="news-cat-tag"
                :style="{ background: categoryColors[item.category]?.bg, color: categoryColors[item.category]?.color }"
              >
                {{ categoryColors[item.category]?.label || item.category }}
              </span>
              <h4 class="news-card-title">{{ item.title }}</h4>
              <p class="news-card-excerpt">{{ item.excerpt }}</p>
              <div class="news-card-meta">
                <span class="news-meta-source">{{ item.source }}</span>
                <span class="news-meta-sep">·</span>
                <span class="news-meta-time">{{ item.time }}</span>
              </div>
            </RouterLink>
          </div>
        </div>
      </div>

      <footer class="news-footer">
        AI 前沿情报站 · 每日 AI 资讯聚合
      </footer>
    </main>
  </div>
</template>

<style scoped>
/* Color tokens */
:root {
  --green: #059669;
  --green-soft: rgba(5, 150, 105, 0.1);
  --orange: #ea580c;
  --orange-soft: rgba(234, 88, 12, 0.1);
  --purple: #7c3aed;
  --purple-soft: rgba(124, 58, 237, 0.1);
  --red: #dc2626;
  --red-soft: rgba(220, 38, 38, 0.1);
  --teal: #0d9488;
  --teal-soft: rgba(13, 148, 136, 0.1);
}

.news-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 28px 32px 48px;
}

/* Header */
.news-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 20px;
}

.news-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.news-header-icon {
  font-size: 2rem;
}

.news-header h1 {
  font-size: 22px;
  font-weight: 750;
  letter-spacing: -0.03em;
  margin: 0;
}

.news-header-date {
  font-size: 13px;
  color: var(--text-tertiary);
  margin: 4px 0 0;
}

.news-header-stats {
  display: flex;
  gap: 10px;
}

.news-stat-chip {
  display: flex;
  align-items: baseline;
  gap: 6px;
  padding: 10px 16px;
  border-radius: var(--radius-sm);
  background: var(--paper);
  border: 1px solid var(--line);
}

.news-stat-num {
  font-size: 20px;
  font-weight: 750;
  font-family: var(--font-mono);
  color: var(--accent);
  letter-spacing: -0.02em;
}

.news-stat-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

/* Tabs */
.news-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 16px;
  padding: 4px;
  background: var(--paper);
  border-radius: var(--radius-sm);
  border: 1px solid var(--line);
  width: fit-content;
}

.news-tab {
  padding: 8px 18px;
  border: none;
  border-radius: 8px;
  background: transparent;
  color: var(--text-tertiary);
  font-size: 13.5px;
  font-weight: 550;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.news-tab:hover {
  color: var(--text-main);
  background: rgba(0,0,0,0.03);
}

.news-tab.active {
  color: var(--text-main);
  background: var(--accent-soft);
  font-weight: 650;
}

/* Toolbar */
.news-toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.news-toolbar-info {
  font-size: 13px;
  color: var(--text-tertiary);
}

.news-view-toggle {
  display: flex;
  gap: 4px;
  padding: 3px;
  border-radius: 8px;
  background: var(--paper);
  border: 1px solid var(--line);
}

.news-toggle-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  border-radius: 6px;
  background: transparent;
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all 0.15s;
}

.news-toggle-btn:hover {
  color: var(--text-main);
  background: rgba(0,0,0,0.03);
}

.news-toggle-btn.active {
  color: var(--accent);
  background: var(--accent-soft);
}

/* Category Tag */
.news-cat-tag {
  display: inline-flex;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 650;
  letter-spacing: 0.02em;
}

.news-cat-tag--lg {
  padding: 5px 14px;
  font-size: 12px;
}

/* Stream View */
.news-stream {
  display: flex;
  flex-direction: column;
  gap: 2px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--paper);
  overflow: hidden;
}

.news-stream-item {
  display: flex;
  gap: 16px;
  padding: 20px;
  transition: background 0.15s;
}

.news-stream-item:hover {
  background: rgba(0,0,0,0.01);
}

.news-stream-item + .news-stream-item {
  border-top: 1px solid var(--line);
}

.news-stream-num {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 700;
  font-family: var(--font-mono);
  color: var(--text-tertiary);
  background: rgba(0,0,0,0.03);
}

.news-stream-content {
  flex: 1;
  min-width: 0;
}

.news-stream-top {
  margin-bottom: 8px;
}

.news-stream-title {
  font-size: 15px;
  font-weight: 650;
  margin: 0 0 6px;
  line-height: 1.45;
}

.news-stream-excerpt {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 0 0 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.news-stream-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-tertiary);
}

.news-meta-source {
  font-weight: 600;
  color: var(--text-secondary);
}

.news-meta-sep {
  opacity: 0.4;
}

/* Magazine View */
.news-magazine {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

/* Hero Card */
.news-hero {
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--paper);
  overflow: hidden;
  transition: all 0.2s;
  display: block;
}

.news-hero:hover {
  box-shadow: 0 8px 28px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}

.news-hero-inner {
  padding: 32px;
}

.news-hero-title {
  font-size: 22px;
  font-weight: 750;
  letter-spacing: -0.02em;
  margin: 14px 0 12px;
  line-height: 1.35;
}

.news-hero-excerpt {
  font-size: 14.5px;
  color: var(--text-secondary);
  line-height: 1.75;
  margin: 0 0 16px;
}

.news-hero-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--text-tertiary);
}

/* Group */
.news-group {
  margin-bottom: 8px;
}

.news-group-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 650;
  margin: 0 0 14px;
}

.news-group-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.news-group-count {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

.news-card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 14px;
}

.news-card {
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--paper);
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  transition: all 0.2s;
}

.news-card:hover {
  box-shadow: 0 8px 28px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}

.news-card-title {
  font-size: 14.5px;
  font-weight: 650;
  margin: 0;
  line-height: 1.45;
}

.news-card-excerpt {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.65;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
  flex: 1;
}

.news-card-meta {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: auto;
  padding-top: 10px;
  border-top: 1px solid var(--line);
}

/* Footer */
.news-footer {
  padding: 32px 0;
  text-align: center;
  font-size: 12.5px;
  color: var(--text-tertiary);
  border-top: 1px solid var(--line);
  margin-top: 24px;
}

/* Responsive */
@media (max-width: 1024px) {
  .news-page { padding: 24px 20px 40px; }
  .news-card-grid { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .news-page { padding: 20px 16px 36px; }
  .news-header { flex-direction: column; align-items: flex-start; }
  .news-header-stats { align-self: flex-start; }
  .news-stream-excerpt { -webkit-line-clamp: 2; }
  .news-hero-inner { padding: 24px 20px; }
  .news-hero-title { font-size: 18px; }
}

@media (max-width: 480px) {
  .news-tabs { width: 100%; overflow-x: auto; }
  .news-tab { padding: 6px 12px; font-size: 13px; }
  .news-card-grid { grid-template-columns: 1fr; }
}
</style>
