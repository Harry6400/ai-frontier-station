<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import PortalTopbar from '../components/PortalTopbar.vue'
import LoadingState from '../components/LoadingState.vue'
import ErrorState from '../components/ErrorState.vue'
import EmptyState from '../components/EmptyState.vue'
import PaginationBar from '../components/PaginationBar.vue'
import { getContentByType } from '../api/portal'

const activeSource = ref('全部来源')
const activeType = ref('全部类型')

const sources = [
  { label: '全部来源', icon: '' },
  { label: 'X/Twitter', icon: '𝕏', count: 0 },
  { label: 'Reddit', icon: '🤖', count: 0 },
  { label: 'Hacker News', icon: '📰', count: 0 },
  { label: 'Product Hunt', icon: '🚀', count: 0 }
]

const types = ['全部类型', '使用指南', '工作流', '工具推荐', '行业观点']

const loading = ref(false)
const error = ref(null)
const feedItems = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

function handlePageChange(p) { page.value = p; fetchData() }

async function fetchData() {
  loading.value = true
  error.value = null
  try {
    const res = await getContentByType('practice', { pageNum: page.value, pageSize: pageSize.value })
    const data = res.data
    total.value = data?.total || 0
    const records = data?.records || []
    feedItems.value = records.map((item) => ({
      id: item.id,
      avatar: '📝',
      title: item.title || '',
      type: item.categoryName || '工具推荐',
      typeColor: 'green',
      author: item.authorName || item.sourceName || '',
      isKol: false,
      source: item.sourceName || '',
      excerpt: item.summary || '',
      likes: 0,
      retweets: 0,
      comments: 0,
      time: item.publishedAt || '',
      day: '今天',
      link: item.sourceUrl || '#'
    }))
  } catch (e) {
    console.error('Failed to fetch practices:', e)
    error.value = '加载数据失败，请重试'
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)

const filteredItems = computed(() => {
  return feedItems.value.filter(item => {
    const sourceMatch = activeSource.value === '全部来源' || item.source === activeSource.value
    const typeMatch = activeType.value === '全部类型' || item.type === activeType.value
    return sourceMatch && typeMatch
  })
})

const groupedItems = computed(() => {
  const groups = {}
  filteredItems.value.forEach(item => {
    if (!groups[item.day]) groups[item.day] = []
    groups[item.day].push(item)
  })
  return groups
})

function formatNum(n) {
  if (n >= 10000) return (n / 10000).toFixed(1) + 'w'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n
}
</script>

<template>
  <div class="tools-page">
    <PortalTopbar />

    <div class="tools-container">
      <!-- Header -->
      <div class="page-header">
        <div class="header-top">
          <h1>🔧 AI 工具与实践</h1>
          <p class="subtitle">追踪 AI 编程工具动态，汇集 KOL 实战经验</p>
        </div>
        <div class="header-stats">
          <span class="stat">📰 共 <strong>{{ feedItems.length }}</strong> 条</span>
        </div>
      </div>

      <!-- Daily Summary -->
      <div class="daily-summary">
        <div class="summary-badge">✦ AI 每日精选</div>
        <div class="summary-highlights">
          <span>Karpathy 深度解读 AI 编程新范式</span>
          <span>MCP 生态持续扩张，开发者工具链迎来变革</span>
          <span>Cursor 工作流优化技巧引发社区热议</span>
        </div>
      </div>

      <!-- Source Filter -->
      <div class="filter-bar">
        <button
          v-for="s in sources"
          :key="s.label"
          class="chip"
          :class="{ active: activeSource === s.label }"
          @click="activeSource = s.label"
        >
          <span v-if="s.icon" class="chip-icon">{{ s.icon }}</span>
          {{ s.label }}
          <span v-if="s.count" class="chip-count">{{ s.count }}</span>
        </button>
      </div>

      <!-- Type Filter -->
      <div class="type-tabs">
        <button
          v-for="t in types"
          :key="t"
          class="type-tab"
          :class="{ active: activeType === t }"
          @click="activeType = t"
        >{{ t }}</button>
      </div>

      <!-- Loading State -->
      <LoadingState v-if="loading" />
      <ErrorState v-else-if="error" :message="error" @retry="fetchData" />
      <EmptyState v-else-if="!filteredItems.length" message="暂无工具与实践数据" />

      <!-- Feed -->
      <div v-else class="feed">
        <template v-for="(items, day) in groupedItems" :key="day">
          <div class="day-divider">
            <span>{{ day }}</span>
          </div>
          <RouterLink
            v-for="item in items"
            :key="item.id"
            :to="'/contents/' + item.id"
            class="feed-item"
            style="text-decoration: none; color: inherit;"
          >
            <div class="item-avatar" :class="{ kol: item.isKol }">{{ item.avatar }}</div>
            <div class="item-body">
              <div class="item-header">
                <span class="item-title">{{ item.title }}</span>
                <span class="type-badge" :class="'badge-' + item.typeColor">{{ item.type }}</span>
              </div>
              <div class="item-meta">
                <span class="author-tag" :class="{ kol: item.isKol }">{{ item.author }}</span>
                <span class="source-tag">{{ item.source }}</span>
              </div>
              <div class="item-excerpt">{{ item.excerpt }}</div>
              <div class="item-footer">

                <span class="item-time">{{ item.time }}</span>
                <span class="item-link">阅读全文 →</span>
              </div>
            </div>
          </RouterLink>
        </template>
      </div>

      <PaginationBar
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @page-change="handlePageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.tools-page {
  min-height: 100vh;
  background: var(--canvas);
  color: var(--text-main);
}

.tools-container {
  max-width: 960px;
  margin: 0 auto;
  padding: 32px 24px;
}

.page-header {
  margin-bottom: 24px;
}

.header-top h1 {
  font-size: 28px;
  margin: 0 0 6px;
}

.subtitle {
  color: var(--text-secondary);
  margin: 0 0 12px;
  font-size: 14px;
}

.header-stats {
  display: flex;
  gap: 20px;
}

.stat {
  font-size: 13px;
  color: var(--text-secondary);
}

.stat strong {
  color: var(--accent);
}

.daily-summary {
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 16px 20px;
  margin-bottom: 20px;
}

.summary-badge {
  font-size: 14px;
  font-weight: 600;
  color: var(--accent);
  margin-bottom: 8px;
}

.summary-highlights {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.summary-highlights span {
  font-size: 13px;
  color: var(--text-secondary);
}

.summary-highlights span::before {
  content: '• ';
  color: var(--accent);
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 14px;
}

.chip {
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: 20px;
  padding: 6px 14px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}

.chip:hover {
  border-color: var(--accent);
  color: var(--text-main);
}

.chip.active {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}

.chip-count {
  background: rgba(0,0,0,0.08);
  border-radius: 10px;
  padding: 1px 6px;
  font-size: 11px;
}

.type-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--line);
  padding-bottom: 0;
}

.type-tab {
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  padding: 8px 14px;
  font-size: 13px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.type-tab:hover {
  color: var(--text-main);
}

.type-tab.active {
  color: var(--accent);
  border-bottom-color: var(--accent);
}

.day-divider {
  display: flex;
  align-items: center;
  margin: 20px 0 12px;
}

.day-divider span {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  background: var(--canvas);
  padding-right: 12px;
  position: relative;
  z-index: 1;
}

.day-divider::before {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--line);
  margin-right: -12px;
}

.feed-item {
  display: flex;
  gap: 14px;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: 12px;
  padding: 16px;
  margin-bottom: 10px;
  transition: border-color 0.2s;
  cursor: pointer;
}

.feed-item:hover {
  border-color: var(--accent);
}

.item-avatar {
  font-size: 32px;
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  background: var(--canvas);
}

.item-avatar.kol {
  border: 2px solid var(--accent);
}

.item-body {
  flex: 1;
  min-width: 0;
}

.item-header {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-bottom: 6px;
}

.item-title {
  font-size: 15px;
  font-weight: 600;
  flex: 1;
}

.type-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 10px;
  white-space: nowrap;
  flex-shrink: 0;
}

.badge-teal { background: rgba(32,194,167,0.15); color: #20c2a7; }
.badge-purple { background: rgba(168,130,255,0.15); color: #a882ff; }
.badge-green { background: rgba(63,185,80,0.15); color: #3fb950; }
.badge-orange { background: rgba(210,153,34,0.15); color: #d29922; }
.badge-pink { background: rgba(219,97,162,0.15); color: #db61a2; }

.item-meta {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.author-tag {
  font-size: 12px;
  color: var(--text-secondary);
}

.author-tag.kol {
  color: var(--accent);
  font-weight: 600;
}

.source-tag {
  font-size: 11px;
  color: var(--text-secondary);
  background: var(--canvas);
  padding: 1px 8px;
  border-radius: 8px;
}

.item-excerpt {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.5;
  margin-bottom: 10px;
}

.item-footer {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.engagement {
  font-size: 12px;
  color: var(--text-secondary);
}

.item-time {
  font-size: 12px;
  color: var(--text-secondary);
  margin-left: auto;
}

.item-link {
  font-size: 12px;
  color: var(--accent);
  text-decoration: none;
}

.item-link:hover {
  text-decoration: underline;
}

@media (max-width: 640px) {
  .tools-container {
    padding: 20px 14px;
  }

  .header-top h1 {
    font-size: 22px;
  }

  .kol-highlight {
    flex-direction: column;
    align-items: flex-start;
  }

  .filter-bar {
    overflow-x: auto;
    flex-wrap: nowrap;
    padding-bottom: 4px;
  }

  .type-tabs {
    overflow-x: auto;
  }

  .feed-item {
    flex-direction: column;
    gap: 10px;
  }

  .item-time {
    margin-left: 0;
  }
}
</style>
