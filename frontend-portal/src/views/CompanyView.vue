<template>
  <div class="portal-shell">
    <PortalTopbar />
    <main class="company-view">
    <!-- Header -->
    <header class="page-header">
      <div class="header-top">
        <span class="header-icon">🚀</span>
        <div class="header-text">
          <h1>产品动态速览</h1>
          <p class="subtitle">实时追踪全球 AI 产品与模型更新</p>
        </div>
      </div>
      <div class="header-stats">
        <div class="stat-item">
          <span class="stat-number">{{ feedItems.length }}</span>
          <span class="stat-label">本周更新</span>
        </div>
        <div class="stat-item">
          <span class="stat-number">{{ products.length ? products.length - 1 : 0 }}</span>
          <span class="stat-label">追踪产品</span>
        </div>
      </div>
    </header>

    <!-- AI Weekly Summary -->
    <section class="ai-weekly" v-if="latestItems.length">
      <div class="weekly-badge">✦ 最新动态</div>
      <ul class="weekly-points">
        <li v-for="item in latestItems" :key="item.id">
          <strong>{{ item.productName }}</strong>: {{ item.title }}
        </li>
      </ul>
    </section>

    <!-- Product Filter Chips -->
    <div class="filter-section">
      <div class="chip-row">
        <button
          v-for="p in products"
          :key="p.key"
          class="chip"
          :class="{ active: activeProduct === p.key }"
          @click="activeProduct = p.key"
        >
          <span class="chip-logo">{{ p.logo }}</span>
          <span class="chip-name">{{ p.name }}</span>
          <span class="chip-count">{{ getProductCount(p.key) }}</span>
        </button>
      </div>
    </div>

    <!-- Module Type Tabs -->
    <div class="type-tabs">
      <button
        v-for="t in types"
        :key="t.key"
        class="type-tab"
        :class="{ active: activeType === t.key }"
        @click="activeType = t.key"
      >
        {{ t.name }}
      </button>
    </div>

    <!-- Feed List grouped by day -->
    <div class="feed-list">
      <LoadingState v-if="loading" />
      <ErrorState v-else-if="error" :message="error" @retry="fetchData" />
      <EmptyState v-else-if="!totalVisible" message="暂无匹配的动态" />
      <template v-else>
        <template v-for="day in groupedFeed" :key="day.date">
          <div v-if="day.items.length" class="day-divider">
            <span>{{ day.label }}</span>
          </div>
          <RouterLink
            v-for="item in day.items"
            :key="item.id"
            :to="'/contents/' + item.id"
            class="feed-item"
          >
            <div class="feed-logo">{{ item.logo }}</div>
            <div class="feed-body">
              <div class="feed-title-row">
                <h3 class="feed-title">{{ item.title }}</h3>
                <span class="badge" :class="'badge-' + item.type">{{ typeLabel(item.type) }}</span>
                <span class="product-tag">{{ item.productName }}</span>
              </div>
              <p class="feed-desc">{{ item.description }}</p>
              <div class="feed-meta">
                <span>{{ item.company }}</span>
                <span class="meta-sep">·</span>
                <span>{{ item.source }}</span>
              </div>
            </div>
          </RouterLink>
        </template>
      </template>
    </div>

    <PaginationBar
      :total="total"
      :page-size="pageSize"
      :current-page="page"
      @page-change="handlePageChange"
    />

  </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { RouterLink } from 'vue-router'
import PortalTopbar from '../components/PortalTopbar.vue'
import LoadingState from '../components/LoadingState.vue'
import ErrorState from '../components/ErrorState.vue'
import EmptyState from '../components/EmptyState.vue'
import PaginationBar from '../components/PaginationBar.vue'
import { getContentByType } from '../api/portal'

const activeProduct = ref('all')
const activeType = ref('all')
const feedItems = ref([])
const loading = ref(true)
const error = ref(null)
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

function handlePageChange(p) { page.value = p; fetchData() }

const typeBadgeMap = { model: '模型', api: 'API/产品', coding: '编程工具', ide: 'IDE', pricing: '价格/套餐' }
const typeLabel = (t) => typeBadgeMap[t] || t

// Computed: extract unique products from feed items
const products = computed(() => {
  const uniqueProducts = new Map()
  uniqueProducts.set('all', { key: 'all', name: '全部', logo: '🌐' })
  for (const item of feedItems.value) {
    if (item.product && !uniqueProducts.has(item.product)) {
      uniqueProducts.set(item.product, {
        key: item.product,
        name: item.productName || item.product,
        logo: '📢'
      })
    }
  }
  return Array.from(uniqueProducts.values())
})

// Computed: extract unique types from feed items
const types = computed(() => {
  const uniqueTypes = new Map()
  uniqueTypes.set('all', { key: 'all', name: '全部类型' })
  for (const item of feedItems.value) {
    if (item.type && !uniqueTypes.has(item.type)) {
      const label = typeBadgeMap[item.type] || item.type
      uniqueTypes.set(item.type, { key: item.type, name: label })
    }
  }
  return Array.from(uniqueTypes.values())
})

// Computed: latest 3 items for summary
const latestItems = computed(() => {
  return feedItems.value.slice(0, 3)
})

async function fetchData() {
  loading.value = true
  error.value = null
  try {
    const res = await getContentByType('company_update', { pageNum: page.value, pageSize: pageSize.value })
    if (res.code === 200 && res.data?.records) {
      total.value = res.data?.total || 0
      feedItems.value = res.data.records.map(item => {
        // Parse metadataJson to extract product and updateType
        let metadata = {}
        try {
          metadata = item.metadataJson ? JSON.parse(item.metadataJson) : {}
        } catch (e) {
          metadata = {}
        }
        
        return {
          id: item.id,
          date: item.publishedAt?.slice(0, 10) || '',
          product: (metadata.product || item.subCategory || 'other').toLowerCase(),
          productName: metadata.product || item.subCategory || '通用',
          logo: '📢',
          title: item.title,
          type: (metadata.updateType || item.subCategory || 'api').toLowerCase(),
          description: item.summary || '',
          company: metadata.company || item.authorName || item.sourceName || '',
          source: item.sourceName || '',
          sourceUrl: item.sourceUrl || ''
        }
      })
    }
  } catch (e) {
    console.error('Failed to load company updates:', e)
    error.value = '加载产品动态失败，请重试'
  } finally {
    loading.value = false
  }
}

// Fetch data from API on mount
onMounted(fetchData)

const filteredFeed = computed(() => {
  return feedItems.value.filter(item => {
    const matchProduct = activeProduct.value === 'all' || item.product === activeProduct.value
    const matchType = activeType.value === 'all' || item.type === activeType.value
    return matchProduct && matchType
  })
})

const totalVisible = computed(() => filteredFeed.value.length)

const groupedFeed = computed(() => {
  const map = {}
  for (const item of filteredFeed.value) {
    if (!map[item.date]) map[item.date] = []
    map[item.date].push(item)
  }
  function dayLabel(dateStr) {
    if (!dateStr) return '未知日期'
    const today = new Date().toISOString().slice(0, 10)
    const yesterday = new Date(Date.now() - 86400000).toISOString().slice(0, 10)
    if (dateStr === today) return '今天 · ' + formatDate(dateStr)
    if (dateStr === yesterday) return '昨天 · ' + formatDate(dateStr)
    return formatDate(dateStr)
  }
  function formatDate(d) {
    const parts = d.split('-')
    return parts[1] + '月' + parseInt(parts[2]) + '日'
  }
  return Object.entries(map).map(([date, items]) => ({
    date,
    label: dayLabel(date),
    items,
  }))
})

function getProductCount(key) {
  if (key === 'all') return feedItems.value.length
  return feedItems.value.filter(i => i.product === key).length
}

// Reset filter if selected product/type no longer exists
watch(products, (newProducts) => {
  if (!newProducts.find(p => p.key === activeProduct.value)) {
    activeProduct.value = 'all'
  }
})
watch(types, (newTypes) => {
  if (!newTypes.find(t => t.key === activeType.value)) {
    activeType.value = 'all'
  }
})
</script>

<style scoped>
.company-view {
  max-width: 960px;
  margin: 0 auto;
  padding: 24px 20px 60px;
  color: var(--text-primary, #1a1a2e);
}

/* Header */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 24px;
}
.header-top { display: flex; align-items: center; gap: 14px; }
.header-icon { font-size: 36px; }
.header-text h1 { font-size: 24px; font-weight: 700; margin: 0; }
.subtitle { font-size: 14px; color: var(--text-secondary, #666); margin: 4px 0 0; }
.header-stats { display: flex; gap: 20px; }
.stat-item { text-align: center; }
.stat-number { display: block; font-size: 22px; font-weight: 700; color: var(--primary, #6366f1); }
.stat-label { font-size: 12px; color: var(--text-secondary, #666); }

/* AI Weekly */
.ai-weekly {
  background: linear-gradient(135deg, #eef2ff 0%, #faf5ff 100%);
  border-radius: 12px;
  padding: 18px 22px;
  margin-bottom: 20px;
}
.weekly-badge {
  display: inline-block;
  background: var(--primary, #6366f1);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  padding: 3px 10px;
  border-radius: 20px;
  margin-bottom: 10px;
}
.weekly-points { margin: 0; padding-left: 18px; }
.weekly-points li { font-size: 14px; line-height: 1.8; color: var(--text-primary, #1a1a2e); }

/* Filter Chips */
.filter-section { margin-bottom: 14px; }
.chip-row { display: flex; flex-wrap: wrap; gap: 8px; }
.chip {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 14px;
  border-radius: 20px;
  border: 1px solid var(--border, #e2e8f0);
  background: var(--bg-card, #fff);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}
.chip:hover { border-color: var(--primary, #6366f1); }
.chip.active {
  background: var(--primary, #6366f1);
  color: #fff;
  border-color: var(--primary, #6366f1);
}
.chip.active .chip-count { background: rgba(255,255,255,0.25); color: #fff; }
.chip-logo { font-size: 15px; }
.chip-count {
  font-size: 11px;
  background: var(--bg-secondary, #f1f5f9);
  padding: 1px 6px;
  border-radius: 10px;
  color: var(--text-secondary, #666);
}

/* Type Tabs */
.type-tabs {
  display: flex;
  gap: 6px;
  margin-bottom: 20px;
  border-bottom: 1px solid var(--border, #e2e8f0);
  padding-bottom: 10px;
  overflow-x: auto;
}
.type-tab {
  padding: 6px 14px;
  font-size: 13px;
  border-radius: 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: var(--text-secondary, #666);
  white-space: nowrap;
  transition: all 0.15s;
}
.type-tab:hover { background: var(--bg-secondary, #f1f5f9); }
.type-tab.active {
  background: var(--primary, #6366f1);
  color: #fff;
}

/* Day Divider */
.day-divider {
  display: flex;
  align-items: center;
  margin: 20px 0 12px;
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary, #666);
}
.day-divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: var(--border, #e2e8f0);
  margin-left: 12px;
}

/* Feed Item */
.feed-item {
  display: flex;
  gap: 14px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border-light, #f1f5f9);
  cursor: pointer;
  text-decoration: none;
  color: inherit;
  transition: background 0.15s;
}
.feed-item:hover {
  background: var(--bg-secondary, #f8fafc);
}
.feed-logo {
  font-size: 28px;
  width: 44px;
  height: 44px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-secondary, #f1f5f9);
  border-radius: 10px;
  flex-shrink: 0;
}
.feed-body { flex: 1; min-width: 0; }
.feed-title-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
.feed-title { font-size: 15px; font-weight: 600; margin: 0; }

.badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 6px;
  font-weight: 500;
}
.badge-model { background: #dbeafe; color: #1d4ed8; }
.badge-api { background: #ffedd5; color: #c2410c; }
.badge-coding { background: #ede9fe; color: #6d28d9; }
.badge-ide { background: #fce7f3; color: #be185d; }
.badge-pricing { background: #ccfbf1; color: #0f766e; }

.product-tag {
  font-size: 11px;
  background: #e0e7ff;
  color: #3730a3;
  padding: 2px 8px;
  border-radius: 6px;
  font-weight: 500;
}

.feed-desc {
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-secondary, #555);
  margin: 6px 0 8px;
}
.feed-meta {
  font-size: 12px;
  color: var(--text-tertiary, #999);
}
.meta-sep { margin: 0 6px; }

.empty-state {
  text-align: center;
  padding: 40px;
  color: var(--text-secondary, #999);
  font-size: 14px;
}

@media (max-width: 640px) {
  .page-header { flex-direction: column; gap: 12px; }
  .header-stats { align-self: flex-start; }
  .chip { padding: 5px 10px; font-size: 12px; }
  .feed-logo { width: 36px; height: 36px; font-size: 22px; }
  .feed-title { font-size: 14px; }
}
</style>
