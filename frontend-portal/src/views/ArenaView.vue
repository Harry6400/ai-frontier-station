<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import PortalTopbar from '../components/PortalTopbar.vue'
import { getContentByType } from '../api/portal'

// 侧边栏导航
const activeTab = ref('ranking')

// 分类标签
const categories = ['总榜', '编程', '数学', '创意写作', '中文', '多模态']
const activeCategory = ref('总榜')

// 模拟 ELO 排名数据（Top 15）
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
  { rank: 10, name: 'Yi-Lightning', org: '01.AI', elo: 1250, change: 2, votes: 10123, color: '#EF4444' },
  { rank: 11, name: 'Grok-2', org: 'xAI', elo: 1245, change: 1, votes: 9876, color: '#6366F1' },
  { rank: 12, name: 'Phi-4', org: 'Microsoft', elo: 1238, change: -1, votes: 8765, color: '#0EA5E9' },
  { rank: 13, name: 'InternLM-3', org: 'Shanghai AI Lab', elo: 1230, change: 3, votes: 7654, color: '#D946EF' },
  { rank: 14, name: 'Jamba-1.5', org: 'AI21 Labs', elo: 1225, change: 0, votes: 6543, color: '#F59E0B' },
  { rank: 15, name: 'DBRX', org: 'Databricks', elo: 1218, change: -2, votes: 5432, color: '#64748B' }
])

// 30天趋势数据
const trendModels = ref([
  { name: 'Gemini 2.5 Pro', color: '#4285F4', data: [1320, 1322, 1325, 1324, 1328, 1330, 1329, 1332, 1335, 1334, 1336, 1338, 1337, 1339, 1340, 1338, 1340, 1341, 1339, 1340, 1341, 1343, 1342, 1344, 1343, 1341, 1342, 1343, 1344, 1342] },
  { name: 'GPT-4o', color: '#10A37F', data: [1340, 1341, 1340, 1339, 1340, 1341, 1340, 1339, 1338, 1339, 1340, 1339, 1338, 1337, 1338, 1339, 1338, 1337, 1338, 1339, 1338, 1337, 1338, 1339, 1338, 1337, 1338, 1339, 1338, 1338] },
  { name: 'Claude 3.5 Sonnet', color: '#D97706', data: [1338, 1337, 1338, 1337, 1336, 1337, 1338, 1337, 1336, 1335, 1336, 1337, 1336, 1335, 1334, 1335, 1336, 1335, 1334, 1335, 1336, 1335, 1334, 1335, 1336, 1335, 1334, 1335, 1336, 1335] },
  { name: 'DeepSeek-V3', color: '#7C3AED', data: [1280, 1282, 1285, 1284, 1287, 1290, 1289, 1292, 1295, 1294, 1296, 1298, 1297, 1300, 1302, 1301, 1303, 1305, 1304, 1306, 1305, 1307, 1308, 1309, 1308, 1307, 1308, 1309, 1310, 1310] },
  { name: 'Llama 3.1', color: '#0EA5E9', data: [1300, 1301, 1300, 1299, 1300, 1301, 1300, 1299, 1298, 1299, 1300, 1299, 1298, 1297, 1298, 1299, 1298, 1297, 1298, 1299, 1298, 1297, 1298, 1299, 1298, 1297, 1298, 1299, 1298, 1298] }
])

// AI 解读数据
const arenaContents = ref([])
const arenaLoading = ref(true)
const today = new Date().toISOString().slice(0, 10)

// ECharts 实例
const rankingChartRef = ref(null)
const trendChartRef = ref(null)
let rankingChart = null
let trendChart = null

// 获取当前主题色
function getThemeColors() {
  const style = getComputedStyle(document.documentElement)
  return {
    text: style.getPropertyValue('--text-main').trim() || '#1a1a2e',
    textSecondary: style.getPropertyValue('--text-secondary').trim() || '#64748b',
    textTertiary: style.getPropertyValue('--text-tertiary').trim() || '#94a3b8',
    line: style.getPropertyValue('--line').trim() || '#e2e8f0',
    paper: style.getPropertyValue('--paper').trim() || '#ffffff',
    canvas: style.getPropertyValue('--canvas').trim() || '#f8fafc',
    accent: style.getPropertyValue('--accent').trim() || '#6366f1'
  }
}

// 初始化排名柱状图
function initRankingChart() {
  if (!rankingChartRef.value) return
  if (rankingChart) rankingChart.dispose()

  rankingChart = echarts.init(rankingChartRef.value)
  const colors = getThemeColors()

  const sorted = [...models.value].sort((a, b) => b.elo - a.elo)
  const names = sorted.map(m => m.name)
  const elos = sorted.map(m => m.elo)
  const barColors = sorted.map(m => m.color)

  rankingChart.setOption({
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: colors.paper,
      borderColor: colors.line,
      textStyle: { color: colors.text, fontSize: 13 },
      formatter: (params) => {
        const m = sorted[params[0].dataIndex]
        return `<div style="font-weight:600;margin-bottom:4px">${m.name}</div>
                <div style="color:${colors.textSecondary};font-size:12px">${m.org}</div>
                <div style="margin-top:6px">ELO: <strong style="color:${m.color}">${m.elo}</strong></div>
                <div style="font-size:12px;color:${colors.textTertiary}">投票数: ${m.votes.toLocaleString()}</div>`
      }
    },
    grid: { left: 130, right: 50, top: 10, bottom: 20 },
    xAxis: {
      type: 'value',
      min: Math.min(...elos) - 20,
      max: Math.max(...elos) + 10,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: colors.line, type: 'dashed' } },
      axisLabel: { color: colors.textTertiary, fontSize: 12 }
    },
    yAxis: {
      type: 'category',
      data: names,
      inverse: true,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: { color: colors.text, fontSize: 13, fontWeight: 500 }
    },
    series: [{
      type: 'bar',
      data: elos.map((v, i) => ({
        value: v,
        itemStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
            { offset: 0, color: barColors[i] + 'cc' },
            { offset: 1, color: barColors[i] }
          ]),
          borderRadius: [0, 6, 6, 0]
        }
      })),
      barWidth: 22,
      label: {
        show: true,
        position: 'right',
        formatter: '{c}',
        color: colors.textSecondary,
        fontSize: 12,
        fontWeight: 600,
        fontFamily: 'var(--font-mono, monospace)'
      }
    }]
  })
}

// 初始化趋势折线图
function initTrendChart() {
  if (!trendChartRef.value) return
  if (trendChart) trendChart.dispose()

  trendChart = echarts.init(trendChartRef.value)
  const colors = getThemeColors()

  const days = Array.from({ length: 30 }, (_, i) => {
    const d = new Date()
    d.setDate(d.getDate() - 29 + i)
    return `${d.getMonth() + 1}/${d.getDate()}`
  })

  trendChart.setOption({
    tooltip: {
      trigger: 'axis',
      backgroundColor: colors.paper,
      borderColor: colors.line,
      textStyle: { color: colors.text, fontSize: 13 },
      axisPointer: { lineStyle: { color: colors.line } }
    },
    legend: {
      data: trendModels.value.map(m => m.name),
      bottom: 0,
      textStyle: { color: colors.textSecondary, fontSize: 12 },
      icon: 'roundRect',
      itemWidth: 16,
      itemHeight: 3
    },
    grid: { left: 50, right: 20, top: 20, bottom: 50 },
    xAxis: {
      type: 'category',
      data: days,
      boundaryGap: false,
      axisLine: { lineStyle: { color: colors.line } },
      axisTick: { show: false },
      axisLabel: { color: colors.textTertiary, fontSize: 11, interval: 4 }
    },
    yAxis: {
      type: 'value',
      min: 1260,
      max: 1360,
      axisLine: { show: false },
      axisTick: { show: false },
      splitLine: { lineStyle: { color: colors.line, type: 'dashed' } },
      axisLabel: { color: colors.textTertiary, fontSize: 12 }
    },
    series: trendModels.value.map(m => ({
      name: m.name,
      type: 'line',
      data: m.data,
      smooth: true,
      symbol: 'circle',
      symbolSize: 6,
      lineStyle: { width: 2.5, color: m.color },
      itemStyle: { color: m.color },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: m.color + '20' },
          { offset: 1, color: m.color + '05' }
        ])
      }
    }))
  })
}

// 窗口 resize 处理
function handleResize() {
  rankingChart?.resize()
  trendChart?.resize()
}

// 切换 tab 时渲染对应图表
watch(activeTab, async (tab) => {
  await nextTick()
  if (tab === 'ranking') {
    initRankingChart()
  } else if (tab === 'trend') {
    initTrendChart()
  }
})

onMounted(async () => {
  window.addEventListener('resize', handleResize)

  // 加载 AI 解读
  try {
    const res = await getContentByType('arena')
    arenaContents.value = res.data?.data?.list || res.data?.list || res.data || []
  } catch (e) {
    console.warn('获取Arena内容失败:', e)
    arenaContents.value = []
  } finally {
    arenaLoading.value = false
  }

  // 延迟初始化图表
  await nextTick()
  initRankingChart()
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  rankingChart?.dispose()
  trendChart?.dispose()
})

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
              <h1>模型评测</h1>
              <p class="header-subtitle">实时追踪全球主流 AI 模型的竞技场 ELO 评分排名与性能变化趋势</p>
            </div>
          </div>
          <div class="header-stats">
            <div class="stat-item">
              <span class="stat-num">15</span>
              <span class="stat-label">参评模型</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">每日</span>
              <span class="stat-label">更新频率</span>
            </div>
            <div class="stat-item">
              <span class="stat-num">{{ today }}</span>
              <span class="stat-label">最后更新</span>
            </div>
          </div>
        </div>
      </header>

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

      <!-- 主体布局：侧边栏 + 图表区 -->
      <div class="content-layout">
        <!-- 左侧边栏 -->
        <aside class="sidebar">
          <nav class="sidebar-nav">
            <button
              class="sidebar-btn"
              :class="{ active: activeTab === 'ranking' }"
              @click="activeTab = 'ranking'"
            >
              <span class="sidebar-icon">📊</span>
              <span class="sidebar-label">ELO 评分排名</span>
            </button>
            <button
              class="sidebar-btn"
              :class="{ active: activeTab === 'trend' }"
              @click="activeTab = 'trend'"
            >
              <span class="sidebar-icon">📈</span>
              <span class="sidebar-label">30天 ELO 趋势</span>
            </button>
          </nav>
        </aside>

        <!-- 右侧图表区 -->
        <div class="chart-area">
          <!-- ELO 评分排名 -->
          <section v-if="activeTab === 'ranking'" class="chart-card">
            <div class="card-head">
              <div>
                <h3>ELO 评分排名 · TOP 15</h3>
                <span class="card-tag">Chatbot Arena · {{ activeCategory }}</span>
              </div>
              <span class="card-date">{{ today }} 更新</span>
            </div>
            <div class="card-body">
              <div ref="rankingChartRef" class="echart-container" style="height: 520px;"></div>

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

          <!-- 30天 ELO 趋势 -->
          <section v-if="activeTab === 'trend'" class="chart-card">
            <div class="card-head">
              <div>
                <h3>30 天 ELO 趋势</h3>
                <span class="card-tag">TOP 5 模型走势</span>
              </div>
              <div class="legend">
                <span v-for="m in trendModels" :key="m.name" class="legend-item">
                  <span class="legend-dot" :style="{ background: m.color }"></span>
                  {{ m.name }}
                </span>
              </div>
            </div>
            <div class="card-body">
              <div ref="trendChartRef" class="echart-container" style="height: 420px;"></div>
            </div>
          </section>
        </div>
      </div>

      <!-- AI 解读 -->
      <section class="chart-card ai-analysis-section">
        <div class="card-head">
          <div>
            <h3>🤖 AI 解读</h3>
            <span class="card-tag">基于 Arena 排行榜数据的智能分析</span>
          </div>
        </div>
        <div class="card-body">
          <div v-if="arenaLoading" class="arena-empty-state">
            <p>正在加载 AI 解读数据...</p>
          </div>
          <div v-else-if="arenaContents.length === 0" class="arena-empty-state">
            <p>暂无 Arena 分析数据，请在后台数据采集页面触发采集</p>
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
  max-width: 420px;
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

/* 主体布局 */
.content-layout {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

/* 侧边栏 */
.sidebar {
  width: 200px;
  flex-shrink: 0;
}

.sidebar-nav {
  display: flex;
  flex-direction: column;
  gap: 8px;
  position: sticky;
  top: 80px;
}

.sidebar-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 16px;
  background: var(--paper);
  border: 1px solid var(--line);
  border-radius: var(--radius);
  cursor: pointer;
  transition: all 0.2s;
  text-align: left;
  color: var(--text-secondary);
}

.sidebar-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-soft);
}

.sidebar-btn.active {
  background: var(--accent);
  border-color: var(--accent);
  color: #fff;
  box-shadow: 0 4px 12px rgba(99, 102, 241, 0.25);
}

.sidebar-icon {
  font-size: 18px;
  flex-shrink: 0;
}

.sidebar-label {
  font-size: 14px;
  font-weight: 600;
}

/* 图表区 */
.chart-area {
  flex: 1;
  min-width: 0;
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

.echart-container {
  width: 100%;
}

/* 图例 */
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

/* 排名表格 */
.ranking-table {
  overflow-x: auto;
  margin-top: 24px;
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

  .content-layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .sidebar-nav {
    flex-direction: row;
    position: static;
  }

  .sidebar-btn {
    flex: 1;
    justify-content: center;
  }

  .category-chips {
    overflow-x: auto;
    flex-wrap: nowrap;
    scrollbar-width: none;
    -ms-overflow-style: none;
  }

  .category-chips::-webkit-scrollbar {
    display: none;
  }

  .category-chip {
    flex-shrink: 0;
  }
}
</style>
