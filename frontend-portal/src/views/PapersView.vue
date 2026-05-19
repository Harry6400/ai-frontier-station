<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import PortalTopbar from '../components/PortalTopbar.vue'
import { useViewMode, useTabs } from '../composables/useViewMode.js'
import { getPapers } from '../api/portal'

// 视图模式切换
const { viewMode, setViewMode } = useViewMode('papers-view', 'list')

// SubCategory中文标签映射
const subCategoryLabels = {
  '3d_ct_denoising': '3D CT去噪',
  'medical_imaging': '医学影像',
  'large_model': '大模型'
}

// 展开/收起详情
const expanded = ref(false)

// 论文数据
const allPapers = ref([])
const loading = ref(false)
const totalPapers = ref(0)

// 从后端获取全部论文（不按subCategory过滤，用于动态发现分类）
async function fetchPapers() {
  loading.value = true
  try {
    const res = await getPapers({ pageNum: 1, pageSize: 200 })
    allPapers.value = (res.data?.records || []).map((r, i) => ({
      id: r.id,
      rank: i + 1,
      title: r.title,
      source: r.sourceName || 'arXiv',
      author: r.authorName || '',
      dataset: '',
      abstract: r.summary || '',
      date: r.publishedAt?.split('T')[0] || '',
      paperLink: `/contents/${r.id}`,
      codeLink: null,
      subCategory: r.subCategory || ''
    }))
    totalPapers.value = res.data?.total || 0
  } catch (e) {
    console.error('Failed to fetch papers:', e)
  } finally {
    loading.value = false
  }
}

// 动态Tab：从数据中提取唯一subCategory
const tabs = computed(() => {
  const subCats = [...new Set(allPapers.value.map(p => p.subCategory).filter(Boolean))]
  return subCats.map(sc => subCategoryLabels[sc] || sc)
})

// 反向映射：中文标签 -> subCategory key
const reverseLabelMap = computed(() => {
  const map = {}
  Object.entries(subCategoryLabels).forEach(([key, label]) => { map[label] = key })
  return map
})

const { activeTab, setTab } = useTabs('')

// 数据加载后自动选中第一个Tab
watch(tabs, (newTabs) => {
  if (newTabs.length && !newTabs.includes(activeTab.value)) {
    setTab(newTabs[0])
  }
}, { immediate: true })

onMounted(fetchPapers)

// AI摘要信息（基于真实数据）
const summaryInfo = computed(() => ({
  paperCount: filteredPapers.value.length,
  period: '最近更新',
  category: activeTab.value || '全部',
  summary: `当前${activeTab.value || '全部'}板块共收录 ${filteredPapers.value.length} 篇论文。`,
  keywords: [],
  updateTime: new Date().toLocaleString('zh-CN')
}))

// 当前Tab下的论文（客户端过滤）
const filteredPapers = computed(() => {
  if (!activeTab.value) return allPapers.value
  const subCat = reverseLabelMap.value[activeTab.value] || activeTab.value
  return allPapers.value.filter(p => p.subCategory === subCat)
})
</script>

<template>
  <div class="papers-view">
    <PortalTopbar />
    
    <main class="papers-main">
      <!-- AI摘要栏 -->
      <section class="summary-bar">
        <div class="summary-left">
          <div class="summary-count">
            <span class="count-number">{{ summaryInfo.paperCount }}</span>
            <span class="count-label">篇论文</span>
          </div>
          <div class="summary-period">{{ summaryInfo.period }}</div>
        </div>
        
        <div class="summary-body">
          <div class="summary-category">{{ summaryInfo.category }}</div>
          <p class="summary-text">{{ summaryInfo.summary }}</p>
          <div class="summary-meta">
            <div class="summary-keywords">
              <span v-for="kw in summaryInfo.keywords" :key="kw" class="keyword-tag">{{ kw }}</span>
            </div>
            <span class="summary-update">更新于 {{ summaryInfo.updateTime }}</span>
          </div>
        </div>
        
        <button class="expand-btn" @click="expanded = !expanded">
          <svg :class="{ 'is-expanded': expanded }" width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <polyline points="6 9 12 15 18 9"></polyline>
          </svg>
        </button>
      </section>
      
      <!-- 展开的热门论文 -->
      <transition name="slide">
        <section v-if="expanded" class="top-papers">
          <h3>本周热门论文</h3>
          <ul>
            <li v-for="p in allPapers.slice(0, 3)" :key="p.id">
              <span class="top-rank">#{{ p.rank }}</span>
              <span class="top-title">{{ p.title }}</span>
            </li>
          </ul>
        </section>
      </transition>
      
      <!-- Tab栏和视图切换 -->
      <div class="toolbar">
        <div v-if="tabs.length" class="tabs">
          <button
            v-for="tab in tabs"
            :key="tab"
            class="tab-btn"
            :class="{ active: activeTab === tab }"
            @click="setTab(tab)"
          >
            {{ tab }}
          </button>
        </div>
        
        <div class="view-toggle">
          <button
            class="toggle-btn"
            :class="{ active: viewMode === 'list' }"
            @click="setViewMode('list')"
            title="列表视图"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="8" y1="6" x2="21" y2="6"></line>
              <line x1="8" y1="12" x2="21" y2="12"></line>
              <line x1="8" y1="18" x2="21" y2="18"></line>
              <line x1="3" y1="6" x2="3.01" y2="6"></line>
              <line x1="3" y1="12" x2="3.01" y2="12"></line>
              <line x1="3" y1="18" x2="3.01" y2="18"></line>
            </svg>
          </button>
          <button
            class="toggle-btn"
            :class="{ active: viewMode === 'grid' }"
            @click="setViewMode('grid')"
            title="网格视图"
          >
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <rect x="3" y="3" width="7" height="7"></rect>
              <rect x="14" y="3" width="7" height="7"></rect>
              <rect x="14" y="14" width="7" height="7"></rect>
              <rect x="3" y="14" width="7" height="7"></rect>
            </svg>
          </button>
        </div>
      </div>
      
      <!-- 论文列表视图 -->
      <div v-if="loading" class="loading-state">
        <p>加载中...</p>
      </div>
      <div v-else-if="viewMode === 'list'" class="papers-list">
        <div v-for="paper in filteredPapers" :key="paper.id" class="paper-row">
          <div class="paper-rank">{{ paper.rank }}</div>
          <div class="paper-content">
            <h3 class="paper-title">
              <a :href="paper.paperLink" target="_blank">{{ paper.title }}</a>
            </h3>
            <div class="paper-meta">
              <span class="meta-source">{{ paper.source }}</span>
              <span class="meta-sep">·</span>
              <span class="meta-author">{{ paper.author }}</span>
              <span class="meta-sep">·</span>
              <span class="meta-dataset">{{ paper.dataset }}</span>
            </div>
            <p class="paper-abstract">{{ paper.abstract }}</p>
          </div>
          <div class="paper-right">
            <span class="paper-date">{{ paper.date }}</span>
            <div class="paper-links">
              <a :href="paper.paperLink" target="_blank" class="link-btn">论文</a>
              <a v-if="paper.codeLink" :href="paper.codeLink" target="_blank" class="link-btn code">代码</a>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 论文网格视图 -->
      <div v-else-if="!loading" class="papers-grid">
        <div v-for="paper in filteredPapers" :key="paper.id" class="paper-card">
          <div class="card-header">
            <span class="card-source">{{ paper.source }}</span>
            <span class="card-date">{{ paper.date }}</span>
          </div>
          <h3 class="card-title">
            <a :href="paper.paperLink" target="_blank">{{ paper.title }}</a>
          </h3>
          <p class="card-abstract">{{ paper.abstract }}</p>
          <div class="card-footer">
            <span class="card-authors">{{ paper.author }}</span>
            <div class="card-links">
              <a :href="paper.paperLink" target="_blank" class="link-btn">论文</a>
              <a v-if="paper.codeLink" :href="paper.codeLink" target="_blank" class="link-btn code">代码</a>
            </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.papers-view {
  min-height: 100vh;
  background: var(--canvas);
}

.papers-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

.loading-state {
  text-align: center;
  padding: 60px 20px;
  color: var(--text-tertiary);
  font-size: 14px;
}

/* 摘要栏 */
.summary-bar {
  display: flex;
  align-items: flex-start;
  gap: 24px;
  padding: 20px 24px;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  margin-bottom: 24px;
}

.summary-left {
  flex-shrink: 0;
  text-align: center;
  padding-right: 24px;
  border-right: 1px solid var(--line);
}

.summary-count {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.count-number {
  font-size: 28px;
  font-weight: 750;
  color: var(--accent);
  line-height: 1;
}

.count-label {
  font-size: 13px;
  color: var(--text-secondary);
  margin-top: 4px;
}

.summary-period {
  font-family: var(--font-mono);
  font-size: 12px;
  color: var(--text-tertiary);
  margin-top: 8px;
}

.summary-body {
  flex: 1;
  min-width: 0;
}

.summary-category {
  display: inline-block;
  padding: 2px 10px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 12px;
  border-radius: 20px;
  margin-bottom: 8px;
}

.summary-text {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin: 0 0 12px;
}

.summary-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
}

.summary-keywords {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.keyword-tag {
  padding: 2px 8px;
  background: var(--canvas);
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  font-size: 12px;
  color: var(--text-tertiary);
}

.summary-update {
  font-size: 12px;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

.expand-btn {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: none;
  border: 1px solid var(--line);
  border-radius: var(--radius-sm);
  cursor: pointer;
  color: var(--text-secondary);
  transition: all 0.2s;
}

.expand-btn:hover {
  background: var(--canvas);
  color: var(--accent);
}

.expand-btn svg {
  transition: transform 0.2s;
}

.expand-btn svg.is-expanded {
  transform: rotate(180deg);
}

/* 展开区域 */
.top-papers {
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 16px 20px;
  margin-bottom: 24px;
}

.top-papers h3 {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-main);
  margin: 0 0 12px;
}

.top-papers ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.top-papers li {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid var(--line);
}

.top-papers li:last-child {
  border-bottom: none;
}

.top-rank {
  font-weight: 700;
  color: var(--accent);
  font-size: 14px;
  min-width: 28px;
}

.top-title {
  font-size: 14px;
  color: var(--text-main);
}

.slide-enter-active,
.slide-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.slide-enter-from,
.slide-leave-to {
  opacity: 0;
  max-height: 0;
  margin-bottom: 0;
  padding-top: 0;
  padding-bottom: 0;
}

/* 工具栏 */
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.tabs {
  display: flex;
  gap: 8px;
  background: var(--canvas);
  padding: 4px;
  border-radius: var(--radius);
}

.tab-btn {
  padding: 8px 20px;
  border: none;
  background: none;
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover {
  color: var(--text-main);
}

.tab-btn.active {
  background: var(--paper);
  color: var(--accent);
  font-weight: 600;
  box-shadow: var(--shadow);
}

.view-toggle {
  display: flex;
  gap: 4px;
  padding: 4px;
  background: var(--canvas);
  border-radius: var(--radius-sm);
}

.toggle-btn {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: none;
  border-radius: var(--radius-sm);
  color: var(--text-tertiary);
  cursor: pointer;
  transition: all 0.2s;
}

.toggle-btn:hover {
  color: var(--text-secondary);
}

.toggle-btn.active {
  background: var(--paper);
  color: var(--accent);
  box-shadow: var(--shadow);
}

/* 列表视图 */
.papers-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.paper-row {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  transition: all 0.2s;
}

.paper-row:hover {
  box-shadow: var(--shadow);
}

.paper-rank {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--accent-soft);
  color: var(--accent);
  font-weight: 700;
  font-size: 14px;
  border-radius: var(--radius-sm);
}

.paper-content {
  flex: 1;
  min-width: 0;
}

.paper-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0 0 8px;
  line-height: 1.4;
}

.paper-title a {
  color: var(--text-main);
  text-decoration: none;
}

.paper-title a:hover {
  color: var(--accent);
}

.paper-meta {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 8px;
  font-size: 13px;
  color: var(--text-secondary);
}

.meta-sep {
  color: var(--line);
}

.meta-source {
  padding: 1px 8px;
  background: var(--accent-soft);
  color: var(--accent);
  border-radius: var(--radius-sm);
  font-size: 12px;
}

.paper-abstract {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.paper-right {
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 8px;
}

.paper-date {
  font-size: 13px;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

.paper-links {
  display: flex;
  gap: 6px;
}

.link-btn {
  padding: 4px 12px;
  font-size: 12px;
  color: var(--accent);
  background: var(--accent-soft);
  border-radius: var(--radius-sm);
  text-decoration: none;
  transition: all 0.2s;
}

.link-btn:hover {
  background: var(--accent);
  color: #fff;
}

.link-btn.code {
  color: var(--text-secondary);
  background: var(--canvas);
  border: 1px solid var(--line);
}

.link-btn.code:hover {
  color: var(--accent);
  border-color: var(--accent);
}

/* 网格视图 */
.papers-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}

.paper-card {
  padding: 20px;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  transition: all 0.2s;
  display: flex;
  flex-direction: column;
}

.paper-card:hover {
  box-shadow: var(--shadow);
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.card-source {
  padding: 2px 10px;
  background: var(--accent-soft);
  color: var(--accent);
  font-size: 12px;
  border-radius: 20px;
}

.card-date {
  font-size: 12px;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

.card-title {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 10px;
  line-height: 1.4;
}

.card-title a {
  color: var(--text-main);
  text-decoration: none;
}

.card-title a:hover {
  color: var(--accent);
}

.card-abstract {
  font-size: 13px;
  color: var(--text-secondary);
  line-height: 1.6;
  margin: 0 0 12px;
  flex: 1;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.card-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 12px;
  border-top: 1px solid var(--line);
}

.card-authors {
  font-size: 13px;
  color: var(--text-tertiary);
}

.card-links {
  display: flex;
  gap: 6px;
}

/* 响应式 */
@media (max-width: 768px) {
  .papers-main {
    padding: 16px 12px 40px;
  }
  
  .summary-bar {
    flex-direction: column;
    gap: 16px;
    padding: 16px;
  }
  
  .summary-left {
    border-right: none;
    border-bottom: 1px solid var(--line);
    padding-right: 0;
    padding-bottom: 16px;
    display: flex;
    align-items: center;
    gap: 16px;
  }
  
  .summary-count {
    flex-direction: row;
    gap: 8px;
  }
  
  .toolbar {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .paper-row {
    flex-direction: column;
    gap: 12px;
  }
  
  .paper-right {
    flex-direction: row;
    align-items: center;
    width: 100%;
    justify-content: space-between;
  }
  
  .papers-grid {
    grid-template-columns: 1fr;
  }
}
</style>
