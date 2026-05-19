<script setup>
import { ref, computed, onMounted } from 'vue'
import PortalTopbar from '../components/PortalTopbar.vue'
import { getContentByType } from '../api/portal'

// Tab 数据源
const sources = [
  { id: 'arena', name: 'Chatbot Arena', badge: '每日更新', active: true },
  { id: 'opencompass', name: 'OpenCompass', badge: '即将接入', active: false },
  { id: 'alpaca', name: 'AlpacaEval', badge: '即将接入', active: false }
]
const activeSource = ref('arena')

// 分类标签
const categories = ['总榜', '编程', '数学', '创意写作', '中文', '多模态']
const activeCategory = ref('总榜')

// 模拟 ELO 排名数据
const models = ref([
  { rank: 1, name: 'Gemini 2.5 Pro', org: 'Google', elo: 1342, change: 2, votes: 28456, color: '#4285F4' },
  { rank: 2, name: 'GPT-4o', org: 'OpenAI', elo: 1338, change: 0, votes: 31289, color: '#10A37F' },
  { rank: 3, name: 'Claude 3.5 Sonnet', org: 'Anthropic', elo: 1335, change: -1, votes: 25678, color: '#D97706' },
  { rank: 4, name: 'DeepSeek-V3', org: 'DeepSeek', elo: 1310, change: 3, votes: 18923, color: '#7C3AED' },
  { rank: 5, name: 'Llama 3.1', org: 'Meta', elo: 1298, change: -1, votes: 22345, color: '#0EA5E9' },
  { rank: 6, name: 'Qwen-2.5', org: 'Alibaba', elo: 1285, change: 1, votes: 15678, color: '#F97316' },
  { rank: 7, name: 'Mistral Large', org: 'Mistral AI', elo: 1278, change: -2, votes: 14567, color: '#EC4899' },
  { rank: 8, name: 'GLM-4-Plus', org: 'Zhipu AI', elo: 1265, change: 0, votes: 12345, color: '#14B8A6' },
  { rank: 9, name: 'Command R+', org: 'Cohere', elo: 1258, change: -1, votes: 11234, color: '#8B5CF6' },
  { rank: 10, name: 'Yi-Lightning', org: '01.AI', elo: 1250, change: 2, votes: 10123, color: '#EF4444' }
])

// AI 解读 - 从后端获取
const aiSummary = ref({
  highlights: [],
  date: new Date().toISOString().slice(0, 10)
})
const arenaContents = ref([])
const arenaLoading = ref(true)

onMounted(async () => {
  try {
    const res = await getContentByType('arena')
    arenaContents.value = res.data?.data?.list || res.data?.list || res.data || []
  } catch (e) {
    console.warn('获取Arena内容失败:', e)
    arenaContents.value = []
  } finally {
    arenaLoading.value = false
  }
})

// 30 天趋势数据（简化为关键时间点）
const trendData = computed(() => {
  const days = 30
  const data = {
    'Gemini 2.5 Pro': { start: 1320, end: 1342, color: '#4285F4' },
    'GPT-4o': { start: 1340, end: 1338, color: '#10A37F' },
    'Claude 3.5 Sonnet': { start: 1338, end: 1335, color: '#D97706' },
    'DeepSeek-V3': { start: 1280, end: 1310, color: '#7C3AED' },
    'Llama 3.1': { start: 1300, end: 1298, color: '#0EA5E9' }
  }
  return { days, data }
})

// 计算柱状图高度比例
const maxElo = computed(() => Math.max(...models.value.map(m => m.elo)))
const minElo = computed(() => Math.min(...models.value.map(m => m.elo)))

function getBarHeight(elo) {
  const range = maxElo.value - minElo.value
  const minH = 60
  const maxH = 280
  if (range === 0) return maxH
  return minH + ((elo - minElo.value) / range) * (maxH - minH)
}

// 生成 SVG 趋势线路径
function generateTrendPath(config, index) {
  const { days, data } = trendData.value
  const entries = Object.entries(data)
  const modelName = entries[index]?.[0]
  if (!modelName) return ''
  
  const { start, end } = data[modelName]
  const w = 800
  const h = 240
  const padding = 40
  
  const points = []
  for (let i = 0; i <= days; i++) {
    const x = padding + (i / days) * (w - padding * 2)
    const progress = i / days
    const noise = Math.sin(i * 0.5) * 3 + Math.cos(i * 0.3) * 2
    const y = h - padding - ((start + (end - start) * progress + noise - 1240) / 110) * (h - padding * 2)
    points.push(`${x},${y}`)
  }
  return points.join(' ')
}



function renderMarkdown(text) {
  return text
    .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
    .replace(/\n\n/g, '</p><p>')
    .replace(/^/, '<p>')
    .replace(/$/, '</p>')
}
</script>

<template>
  <div class="arena-view">
    <PortalTopbar />
    
    <main class="arena-main">
      <!-- 页面头部 -->
      <header class="page-header">
        <div class="header-content">
          <div class="header-left">
            <div class="header-icon">🏆</div>
            <div>
              <h1>模型评测追踪</h1>
              <p class="header-subtitle">实时追踪全球主流 AI 模型的竞技场排名与性能变化</p>
            </div>
          </div>
          <div class="header-stats">
            <div class="stat-item">
              <span class="stat-num">3</span>
              <span class="stat-label">数据源</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">每日</span>
              <span class="stat-label">更新频率</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ aiSummary.date }}</span>
              <span class="stat-label">最后更新</span>
            </div>
          </div>
        </div>
      </header>
      
      <!-- 数据源 Tab -->
      <div class="source-tabs">
        <button
          v-for="src in sources"
          :key="src.id"
          class="source-tab"
          :class="{ active: activeSource === src.id }"
          @click="activeSource = src.id"
        >
          <span class="tab-name">{{ src.name }}</span>
          <span class="tab-badge" :class="{ 'badge-update': src.badge === '每日更新', 'badge-coming': src.badge === '即将接入' }">
            {{ src.badge }}
          </span>
        </button>
      </div>
      
      <!-- Chatbot Arena 内容 -->
      <template v-if="activeSource === 'arena'">
        <!-- AI 速报 -->
        <section class="ai-summary">
          <div class="ai-badge">✦ AI 解读</div>
          <div class="ai-content">
            <h3>Arena 速报 · {{ aiSummary.date }}</h3>
            <ul v-if="arenaContents.length > 0">
              <li v-for="(item, i) in arenaContents.slice(0, 3)" :key="item.id || i">
                <span class="highlight-dot"></span>
                {{ item.title || item.summary?.slice(0, 60) || 'Arena 分析报告' }}
              </li>
            </ul>
            <p v-else-if="arenaLoading" style="color:#78350F;font-size:14px;">加载中...</p>
            <p v-else style="color:#78350F;font-size:14px;">暂无Arena分析数据，请在后台数据采集页面触发采集</p>
          </div>
        </section>
        
        <!-- 分类标签 -->
        <div class="category-chips">
          <button
            v-for="cat in categories"
            :key="cat"
            class="category-chip"
            :class="{ active: activeCategory === cat }"
            @click="activeCategory = cat"
          >
            {{ cat }}
          </button>
        </div>
        
        <!-- ELO 排名柱状图 -->
        <section class="chart-card">
          <div class="card-head">
            <div>
              <h3>ELO 评分排名</h3>
              <span class="card-tag">Chatbot Arena · {{ activeCategory }}</span>
            </div>
            <span class="card-date">{{ aiSummary.date }} 更新</span>
          </div>
          <div class="card-body">
            <div class="bar-chart">
              <div v-for="model in models" :key="model.rank" class="bar-item">
                <div class="bar-elo">{{ model.elo }}</div>
                <div
                  class="bar"
                  :style="{
                    height: getBarHeight(model.elo) + 'px',
                    background: `linear-gradient(180deg, ${model.color}, ${model.color}88)`
                  }"
                >
                  <div class="bar-rank">#{{ model.rank }}</div>
                </div>
                <div class="bar-name">{{ model.name }}</div>
              </div>
            </div>
            
            <!-- 排名表格 -->
            <div class="ranking-table">
              <table>
                <thead>
                  <tr>
                    <th>排名</th>
                    <th>模型</th>
                    <th>机构</th>
                    <th>ELO 评分</th>
                    <th>变化</th>
                    <th>投票数</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="model in models" :key="model.rank">
                    <td>
                      <span class="rank-badge" :class="{ gold: model.rank === 1, silver: model.rank === 2, bronze: model.rank === 3 }">
                        {{ model.rank }}
                      </span>
                    </td>
                    <td class="model-name">
                      <span class="model-dot" :style="{ background: model.color }"></span>
                      {{ model.name }}
                    </td>
                    <td class="org-name">{{ model.org }}</td>
                    <td class="elo-score">{{ model.elo }}</td>
                    <td>
                      <span v-if="model.change > 0" class="change up">↑{{ model.change }}</span>
                      <span v-else-if="model.change < 0" class="change down">↓{{ Math.abs(model.change) }}</span>
                      <span v-else class="change same">-</span>
                    </td>
                    <td class="votes">{{ model.votes.toLocaleString() }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </section>
        
        <!-- 30 天趋势图 -->
        <section class="chart-card">
          <div class="card-head">
            <div>
              <h3>30 天 ELO 趋势</h3>
              <span class="card-tag">TOP 5 模型走势</span>
            </div>
            <div class="legend">
              <span v-for="(config, name) in trendData.data" :key="name" class="legend-item">
                <span class="legend-dot" :style="{ background: config.color }"></span>
                {{ name }}
              </span>
            </div>
          </div>
          <div class="card-body">
            <div class="trend-chart">
              <svg viewBox="0 0 800 240" preserveAspectRatio="xMidYMid meet">
                <!-- 网格线 -->
                <line v-for="i in 5" :key="'grid-'+i" :x1="40" :y1="40 + i * 36" :x2="760" :y2="40 + i * 36" stroke="var(--line)" stroke-dasharray="4 4" />
                
                <!-- Y 轴标签 -->
                <text v-for="i in 6" :key="'label-'+i" x="35" :y="44 + (i-1) * 36" text-anchor="end" fill="var(--text-tertiary)" font-size="11">
                  {{ 1350 - (i-1) * 10 }}
                </text>
                
                <!-- 趋势线 -->
                <g v-for="(config, name, index) in trendData.data" :key="name">
                  <polyline
                    :points="generateTrendPath(config, index)"
                    fill="none"
                    :stroke="config.color"
                    stroke-width="2.5"
                    stroke-linecap="round"
                    stroke-linejoin="round"
                  />
                  <!-- 末端标签 -->
                  <circle
                    :cx="760"
                    :cy="200 - ((config.end - 1240) / 110) * 160"
                    r="4"
                    :fill="config.color"
                  />
                  <text
                    x="768"
                    :y="204 - ((config.end - 1240) / 110) * 160"
                    :fill="config.color"
                    font-size="11"
                    font-weight="600"
                  >
                    {{ config.end }}
                  </text>
                </g>
              </svg>
            </div>
          </div>
        </section>

        <!-- AI 解读 -->
        <section class="chart-card ai-analysis-section">
          <div class="card-head">
            <div>
              <h3>🤖 AI 解读</h3>
              <span class="card-tag">基于Arena排行榜数据的智能分析</span>
            </div>
          </div>
          <div class="card-body">
            <div v-if="arenaLoading" class="arena-empty-state">
              <p>正在加载AI解读数据...</p>
            </div>
            <div v-else-if="arenaContents.length === 0" class="arena-empty-state">
              <p>暂无Arena分析数据，请在后台数据采集页面触发采集</p>
            </div>
            <div v-else>
              <div v-for="item in arenaContents" :key="item.id || item._id" class="arena-content-item">
                <div class="arena-content-header">
                  <h4>{{ item.title || 'Arena 分析报告' }}</h4>
                  <span class="arena-content-date">{{ (item.createdAt || item.publishDate || '').slice(0, 10) }}</span>
                </div>
                <div class="ai-analysis-content" v-html="renderMarkdown(item.summary || item.content || '')"></div>
              </div>
            </div>
          </div>
        </section>
      </template>
      
      <!-- 其他数据源占位 -->
      <template v-if="activeSource !== 'arena'">
        <div class="coming-soon">
          <div class="coming-icon">🚧</div>
          <h3>{{ sources.find(s => s.id === activeSource)?.name }}</h3>
          <p>该数据源正在接入中，敬请期待...</p>
          <div class="coming-features">
            <div class="feature-item">
              <span class="feature-icon">📊</span>
              <span>全面的评测维度</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">🔄</span>
              <span>实时数据同步</span>
            </div>
            <div class="feature-item">
              <span class="feature-icon">📈</span>
              <span>趋势分析报告</span>
            </div>
          </div>
        </div>
      </template>
    </main>
  </div>
</template>

<style scoped>
.arena-view {
  min-height: 100vh;
  background: var(--canvas);
}

.arena-main {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px 20px 60px;
}

/* 页面头部 */
.page-header {
  background: linear-gradient(135deg, var(--accent-soft), var(--paper));
  border: 1px solid var(--line);
  border-radius: var(--radius);
  padding: 28px 32px;
  margin-bottom: 24px;
}

.header-content {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 24px;
}

.header-left {
  display: flex;
  align-items: flex-start;
  gap: 20px;
}

.header-icon {
  font-size: 48px;
  line-height: 1;
}

.header-left h1 {
  font-size: 28px;
  font-weight: 750;
  letter-spacing: -0.02em;
  margin: 0 0 8px;
}

.header-subtitle {
  font-size: 15px;
  color: var(--text-secondary);
  margin: 0;
  max-width: 400px;
}

.header-stats {
  display: flex;
  gap: 24px;
  flex-shrink: 0;
}

.stat-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.stat-num {
  font-size: 18px;
  font-weight: 700;
  color: var(--accent);
}

.stat-label {
  font-size: 12px;
  color: var(--text-tertiary);
}

/* 数据源 Tab */
.source-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 24px;
  border-bottom: 1px solid var(--line);
  padding-bottom: 0;
}

.source-tab {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
  cursor: pointer;
  transition: all 0.2s;
  color: var(--text-secondary);
}

.source-tab:hover {
  color: var(--text-main);
}

.source-tab.active {
  color: var(--accent);
  border-bottom-color: var(--accent);
}

.tab-name {
  font-size: 14px;
  font-weight: 600;
}

.tab-badge {
  font-size: 11px;
  padding: 2px 8px;
  border-radius: 20px;
  font-weight: 500;
}

.badge-update {
  background: rgba(16, 185, 129, 0.1);
  color: #10B981;
}

.badge-coming {
  background: var(--accent-soft);
  color: var(--accent);
}

/* AI 解读 */
.ai-summary {
  display: flex;
  gap: 16px;
  padding: 20px 24px;
  background: linear-gradient(135deg, #FEF3C7, #FDE68A33);
  border: 1px solid #FDE68A;
  border-radius: var(--radius);
  margin-bottom: 20px;
}

.ai-badge {
  flex-shrink: 0;
  padding: 6px 14px;
  background: linear-gradient(135deg, #F59E0B, #D97706);
  color: #fff;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
  height: fit-content;
}

.ai-content h3 {
  font-size: 15px;
  font-weight: 600;
  margin: 0 0 12px;
  color: #92400E;
}

.ai-content ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.ai-content li {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  font-size: 14px;
  color: #78350F;
  line-height: 1.6;
}

.highlight-dot {
  flex-shrink: 0;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #F59E0B;
  margin-top: 8px;
}

/* 分类标签 */
.category-chips {
  display: flex;
  gap: 10px;
  margin-bottom: 24px;
  flex-wrap: wrap;
}

.category-chip {
  padding: 8px 20px;
  border: 1px solid var(--line);
  border-radius: 20px;
  background: var(--paper);
  font-size: 14px;
  color: var(--text-secondary);
  cursor: pointer;
  transition: all 0.2s;
}

.category-chip:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.category-chip.active {
  background: var(--accent);
  color: #fff;
  border-color: var(--accent);
}

/* 图表卡片 */
.chart-card {
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  margin-bottom: 24px;
  overflow: hidden;
}

.card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 24px;
  border-bottom: 1px solid var(--line);
}

.card-head h3 {
  font-size: 16px;
  font-weight: 650;
  margin: 0 0 4px;
}

.card-tag {
  font-size: 12px;
  color: var(--text-tertiary);
}

.card-date {
  font-size: 12px;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

.card-body {
  padding: 24px;
}

/* 柱状图 */
.bar-chart {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 12px;
  height: 340px;
  padding: 20px 0;
  margin-bottom: 32px;
}

.bar-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.bar-elo {
  font-size: 12px;
  font-weight: 700;
  color: var(--text-main);
  font-family: var(--font-mono);
}

.bar {
  width: 100%;
  min-height: 40px;
  border-radius: 8px 8px 4px 4px;
  position: relative;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding-top: 12px;
  transition: all 0.3s ease;
}

.bar:hover {
  opacity: 0.9;
  transform: scaleX(1.05);
}

.bar-rank {
  font-size: 14px;
  font-weight: 700;
  color: #fff;
}

.bar-name {
  font-size: 11px;
  color: var(--text-secondary);
  text-align: center;
  writing-mode: horizontal-tb;
  max-width: 80px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

/* 排名表格 */
.ranking-table {
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
  font-size: 14px;
}

thead {
  background: var(--canvas);
}

th {
  padding: 12px 16px;
  text-align: left;
  font-weight: 600;
  color: var(--text-tertiary);
  font-size: 12px;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

td {
  padding: 14px 16px;
  border-bottom: 1px solid var(--line);
}

tr:hover td {
  background: rgba(0, 0, 0, 0.01);
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  font-weight: 700;
  font-size: 13px;
  background: var(--canvas);
  color: var(--text-secondary);
}

.rank-badge.gold {
  background: linear-gradient(135deg, #FCD34D, #F59E0B);
  color: #78350F;
}

.rank-badge.silver {
  background: linear-gradient(135deg, #E5E7EB, #9CA3AF);
  color: #374151;
}

.rank-badge.bronze {
  background: linear-gradient(135deg, #FED7AA, #F97316);
  color: #7C2D12;
}

.model-name {
  display: flex;
  align-items: center;
  gap: 10px;
  font-weight: 600;
}

.model-dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  flex-shrink: 0;
}

.org-name {
  color: var(--text-secondary);
}

.elo-score {
  font-weight: 700;
  font-family: var(--font-mono);
  color: var(--accent);
}

.change {
  font-weight: 600;
  font-size: 13px;
}

.change.up {
  color: #10B981;
}

.change.down {
  color: #EF4444;
}

.change.same {
  color: var(--text-tertiary);
}

.votes {
  font-family: var(--font-mono);
  font-size: 13px;
  color: var(--text-secondary);
}

/* 趋势图 */
.legend {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-secondary);
}

.legend-dot {
  width: 10px;
  height: 3px;
  border-radius: 2px;
}

.trend-chart {
  width: 100%;
  overflow: hidden;
}

.trend-chart svg {
  width: 100%;
  height: auto;
}

/* 占位卡片 */
.coming-soon {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 40px;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  text-align: center;
}

.coming-icon {
  font-size: 64px;
  margin-bottom: 20px;
}

.coming-soon h3 {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 8px;
}

.coming-soon p {
  font-size: 15px;
  color: var(--text-secondary);
  margin: 0 0 32px;
}

.coming-features {
  display: flex;
  gap: 24px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: var(--canvas);
  border-radius: var(--radius-sm);
  font-size: 14px;
  color: var(--text-secondary);
}

.feature-icon {
  font-size: 20px;
}

/* AI 解读分析卡片 */
.ai-analysis-section .card-head {
  background: linear-gradient(135deg, #1a1a2e, #16213e);
}

.ai-analysis-section .card-head h3 {
  color: #e2e8f0;
}

.ai-analysis-section .card-head .card-tag {
  color: #94a3b8;
}

.ai-analysis-content {
  font-size: 15px;
  line-height: 1.8;
  color: var(--text-main);
}

.ai-analysis-content p {
  margin: 0 0 16px;
}

.ai-analysis-content p:last-child {
  margin-bottom: 0;
}

.ai-analysis-content strong {
  color: var(--accent);
  font-weight: 700;
}

.arena-empty-state {
  text-align: center;
  padding: 32px 16px;
  color: var(--text-secondary);
  font-size: 15px;
}

.arena-content-item {
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--line);
}

.arena-content-item:last-child {
  margin-bottom: 0;
  padding-bottom: 0;
  border-bottom: none;
}

.arena-content-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.arena-content-header h4 {
  font-size: 16px;
  font-weight: 650;
  margin: 0;
  color: var(--text-main);
}

.arena-content-date {
  font-size: 12px;
  color: var(--text-tertiary);
  font-family: var(--font-mono);
}

/* 响应式 */
@media (max-width: 768px) {
  .arena-main {
    padding: 16px 12px 40px;
  }
  
  .page-header {
    padding: 20px;
  }
  
  .header-content {
    flex-direction: column;
    gap: 20px;
  }
  
  .header-left {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  
  .header-icon {
    font-size: 36px;
  }
  
  .header-left h1 {
    font-size: 22px;
  }
  
  .header-stats {
    width: 100%;
    justify-content: space-around;
  }
  
  .source-tabs {
    overflow-x: auto;
    scrollbar-width: none;
    -ms-overflow-style: none;
  }
  
  .source-tabs::-webkit-scrollbar {
    display: none;
  }
  
  .source-tab {
    flex-shrink: 0;
    padding: 10px 16px;
  }
  
  .ai-summary {
    flex-direction: column;
    gap: 12px;
  }
  
  .category-chips {
    overflow-x: auto;
    flex-wrap: nowrap;
    scrollbar-width: none;
    -ms-overflow-style: none;
    padding-bottom: 4px;
  }
  
  .category-chips::-webkit-scrollbar {
    display: none;
  }
  
  .category-chip {
    flex-shrink: 0;
  }
  
  .card-head {
    flex-direction: column;
    gap: 12px;
    align-items: flex-start;
  }
  
  .bar-chart {
    height: 260px;
    gap: 6px;
    overflow-x: auto;
    padding-bottom: 8px;
  }
  
  .bar-item {
    min-width: 60px;
  }
  
  .bar-name {
    font-size: 10px;
    max-width: 60px;
  }
  
  .ranking-table {
    margin: 0 -12px;
    padding: 0 12px;
  }
  
  th, td {
    padding: 10px 12px;
  }
  
  .legend {
    gap: 10px;
  }
  
  .coming-features {
    flex-direction: column;
    gap: 12px;
    width: 100%;
  }
  
  .feature-item {
    justify-content: center;
  }
}
</style>
