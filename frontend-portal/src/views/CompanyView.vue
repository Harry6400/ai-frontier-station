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
          <span class="stat-number">{{ products.length - 1 }}</span>
          <span class="stat-label">追踪产品</span>
        </div>
      </div>
    </header>

    <!-- AI Weekly Summary -->
    <section class="ai-weekly">
      <div class="weekly-badge">✦ AI 周报</div>
      <ul class="weekly-points">
        <li>Gemini 2.5 Pro 正式发布，推理能力大幅提升</li>
        <li>Claude Code 新增 MCP 协议支持，工具集成更灵活</li>
        <li>DeepSeek V3.1 开源，中文理解能力再创新高</li>
        <li>多家厂商调降 API 价格，AI 应用成本持续下降</li>
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
      <div v-if="loading" class="empty-state">加载中...</div>
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
        <div v-if="!totalVisible && !loading" class="empty-state">暂无匹配的动态</div>
      </template>
    </div>
  </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import PortalTopbar from '../components/PortalTopbar.vue'
import { getContentByType } from '../api/portal'

const products = [
  { key: 'all', name: '全部', logo: '🌐' },
  { key: 'claude', name: 'Claude', logo: '🤖' },
  { key: 'chatgpt', name: 'ChatGPT', logo: '🟢' },
  { key: 'gemini', name: 'Gemini', logo: '🔵' },
  { key: 'deepseek', name: 'DeepSeek', logo: '🐋' },
  { key: 'qwen', name: 'Qwen', logo: '☁️' },
  { key: 'glm', name: 'GLM', logo: '💡' },
  { key: 'doubao', name: '豆包', logo: '🫘' },
  { key: 'trae', name: 'Trae', logo: '⚡' },
  { key: 'opencode', name: 'OpenCode', logo: '📝' },
  { key: 'hermes', name: 'Hermes Agent', logo: '🔮' },
  { key: 'cursor', name: 'Cursor', logo: '🎯' },
  { key: 'ollama', name: 'Ollama', logo: '🦙' },
]

const types = [
  { key: 'all', name: '全部类型' },
  { key: 'model', name: '模型' },
  { key: 'api', name: 'API/产品' },
  { key: 'coding', name: '编程工具' },
  { key: 'ide', name: 'IDE' },
  { key: 'pricing', name: '价格/套餐' },
]

const activeProduct = ref('all')
const activeType = ref('all')
const feedItems = ref([])
const loading = ref(true)

const typeBadgeMap = { model: '模型', api: 'API/产品', coding: '编程工具', ide: 'IDE', pricing: '价格/套餐' }
const typeLabel = (t) => typeBadgeMap[t] || t

// Fetch data from API on mount
onMounted(async () => {
  try {
    const res = await getContentByType('company_update', { pageNum: 1, pageSize: 50 })
    if (res.code === 200 && res.data?.records) {
      feedItems.value = res.data.records.map(item => ({
        id: item.id,
        date: item.publishedAt?.slice(0, 10) || '',
        product: (item.subCategory || 'other').toLowerCase(),
        productName: item.subCategory || '通用',
        logo: '📢',
        title: item.title,
        type: item.subCategory?.toLowerCase() || 'api',
        description: item.summary || '',
        company: item.authorName || item.sourceName || '',
        source: item.sourceName || '',
        sourceUrl: item.sourceUrl || ''
      }))
    }
  } catch (e) {
    console.error('Failed to load company updates:', e)
  } finally {
    loading.value = false
  }
})

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
