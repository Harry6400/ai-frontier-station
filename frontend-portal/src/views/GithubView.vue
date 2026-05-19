<script setup>
import { ref, computed, onMounted } from 'vue'
import { RouterLink } from 'vue-router'
import PortalTopbar from '../components/PortalTopbar.vue'
import LoadingState from '../components/LoadingState.vue'
import ErrorState from '../components/ErrorState.vue'
import EmptyState from '../components/EmptyState.vue'
import PaginationBar from '../components/PaginationBar.vue'
import { getContentByType } from '../api/portal'

const loading = ref(false)
const error = ref(null)
const repos = ref([])
const page = ref(1)
const pageSize = ref(20)
const total = ref(0)

const activeDirection = ref('全部')
const activeSort = ref('趋势分')

const directions = ['全部', 'LLM', 'Agent', 'Diffusion', 'CV', 'NLP', 'Infra', 'Tool']
const sortOptions = ['趋势分', 'Star增长', '最近更新']

function parseTags(raw) {
  if (!raw) return []
  try {
    const arr = typeof raw === 'string' ? JSON.parse(raw) : raw
    return Array.isArray(arr) ? arr : []
  } catch {
    return []
  }
}

function formatStars(n) {
  if (n >= 100000) return (n / 1000).toFixed(0) + 'k'
  if (n >= 1000) return (n / 1000).toFixed(1) + 'k'
  return n?.toString() || '0'
}

function formatGrowth(n) {
  if (!n) return '0'
  if (n >= 1000) return '+' + (n / 1000).toFixed(1) + 'k'
  return '+' + n
}

function scoreClass(score) {
  if (score >= 90) return 'trend-score--hot'
  if (score >= 70) return 'trend-score--warm'
  if (score >= 50) return 'trend-score--mid'
  return 'trend-score--low'
}

const filteredRepos = computed(() => {
  let list = [...repos.value]

  // Filter by direction
  if (activeDirection.value !== '全部') {
    list = list.filter(r => r.direction === activeDirection.value)
  }

  // Sort
  if (activeSort.value === '趋势分') {
    list.sort((a, b) => b.trendScore - a.trendScore)
  } else if (activeSort.value === 'Star增长') {
    list.sort((a, b) => b.starGrowth7d - a.starGrowth7d)
  } else if (activeSort.value === '最近更新') {
    list.sort((a, b) => new Date(b.publishedAt) - new Date(a.publishedAt))
  }

  return list
})

function handlePageChange(p) { page.value = p; fetchData() }

async function fetchData() {
  loading.value = true
  error.value = null
  try {
    const res = await getContentByType('project', { pageNum: page.value, pageSize: pageSize.value })
    const data = res.data
    total.value = data?.total || 0
    const records = data?.records || []
    repos.value = records.map(item => ({
      id: item.id,
      title: item.title || '',
      summary: item.summary || item.aiSummary || '',
      trendScore: item.trendScore || 0,
      starGrowth7d: item.starGrowth7d || 0,
      forkCount: item.forkCount || 0,
      tags: parseTags(item.aiTags),
      direction: item.aiDirection || '',
      difficulty: item.aiDifficulty || '',
      sourceUrl: item.sourceUrl || '',
      authorName: item.authorName || '',
      publishedAt: item.publishedAt || '',
      categoryName: item.categoryName || '',
      sourceName: item.sourceName || '',
      starCount: item.starCount || 0
    }))
  } catch (e) {
    console.error('Failed to fetch projects:', e)
    error.value = '加载项目失败，请重试'
  } finally {
    loading.value = false
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="portal-shell">
    <PortalTopbar />

    <main class="trend-page">
      <!-- Header -->
      <header class="trend-header">
        <div class="trend-header-left">
          <span class="trend-header-icon">🔥</span>
          <div>
            <h1>AI Trend Board</h1>
            <p class="trend-header-sub">发现正在爆发的AI项目</p>
          </div>
        </div>
        <div class="trend-header-count">
          <span class="trend-count-num">{{ filteredRepos.length }}</span>
          <span class="trend-count-label">个项目</span>
        </div>
      </header>

      <!-- Filter Bar -->
      <div class="trend-filters">
        <div class="trend-filter-row">
          <div class="trend-chips">
            <button
              v-for="d in directions"
              :key="d"
              :class="['trend-chip', { 'trend-chip--active': activeDirection === d }]"
              @click="activeDirection = d"
            >{{ d }}</button>
          </div>
          <div class="trend-sort-group">
            <span class="trend-sort-label">排序</span>
            <button
              v-for="s in sortOptions"
              :key="s"
              :class="['trend-sort-btn', { 'trend-sort-btn--active': activeSort === s }]"
              @click="activeSort = s"
            >{{ s }}</button>
          </div>
        </div>
      </div>

      <!-- Loading State -->
      <LoadingState v-if="loading" />
      <ErrorState v-else-if="error" :message="error" @retry="fetchData" />

      <!-- Empty State -->
      <EmptyState v-else-if="filteredRepos.length === 0" message="暂无趋势数据" />

      <!-- Trend List -->
      <div v-else class="trend-list">
        <RouterLink
          v-for="(repo, index) in filteredRepos"
          :key="repo.id"
          :to="'/contents/' + repo.id"
          class="trend-item"
          :class="{
            'trend-item--top3': index < 3,
            'trend-item--hot': repo.trendScore >= 90
          }"
        >
          <!-- Rank -->
          <div class="trend-rank" :class="{ 'trend-rank--top3': index < 3 }">
            {{ index + 1 }}
          </div>

          <!-- Main Content -->
          <div class="trend-content">
            <div class="trend-title-row">
              <h3 class="trend-title">{{ repo.title }}</h3>
              <span v-if="repo.direction" class="trend-direction">{{ repo.direction }}</span>
              <span v-if="repo.difficulty" class="trend-difficulty">{{ repo.difficulty }}</span>
            </div>

            <p class="trend-summary">{{ repo.summary }}</p>

            <div class="trend-meta">
              <span v-if="repo.sourceName" class="trend-author">{{ repo.authorName || repo.sourceName }}</span>

              <div class="trend-stats">
                <span class="trend-stat trend-stat--stars">
                  ⭐ {{ formatStars(repo.starCount) }}
                  <span v-if="repo.starGrowth7d" class="trend-stat--growth">{{ formatGrowth(repo.starGrowth7d) }}</span>
                </span>
                <span v-if="repo.forkCount" class="trend-stat trend-stat--forks">
                  🍴 {{ formatStars(repo.forkCount) }}
                </span>
              </div>

              <div v-if="repo.tags.length" class="trend-tags">
                <span v-for="tag in repo.tags" :key="tag" class="trend-tag">{{ tag }}</span>
              </div>
            </div>
          </div>

          <!-- Trend Score -->
          <div class="trend-score-col">
            <div :class="['trend-score', scoreClass(repo.trendScore)]">
              <span class="trend-score-num">{{ repo.trendScore.toFixed(1) }}</span>
              <span class="trend-score-label">趋势分</span>
            </div>
          </div>
        </RouterLink>
      </div>

      <PaginationBar
        :total="total"
        :page-size="pageSize"
        :current-page="page"
        @page-change="handlePageChange"
      />

      <footer class="trend-footer">
        AI 前沿情报站 · Trend Board
      </footer>
    </main>
  </div>
</template>

<style scoped>
/* ===== PAGE ===== */
.trend-page {
  max-width: 1080px;
  margin: 0 auto;
  padding: 28px 32px 48px;
}

/* ===== HEADER ===== */
.trend-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  margin-bottom: 24px;
}

.trend-header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}

.trend-header-icon {
  font-size: 2.2rem;
}

.trend-header h1 {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.04em;
  margin: 0;
  background: linear-gradient(135deg, var(--accent), #f97316);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.trend-header-sub {
  font-size: 13.5px;
  color: var(--text-tertiary);
  margin: 4px 0 0;
}

.trend-header-count {
  display: flex;
  align-items: baseline;
  gap: 4px;
  padding: 10px 18px;
  border-radius: var(--radius-sm);
  background: var(--paper);
  border: 1px solid var(--line);
}

.trend-count-num {
  font-size: 24px;
  font-weight: 800;
  font-family: var(--font-mono);
  color: var(--accent);
  letter-spacing: -0.03em;
}

.trend-count-label {
  font-size: 13px;
  color: var(--text-tertiary);
}

/* ===== FILTER BAR ===== */
.trend-filters {
  margin-bottom: 20px;
  padding: 14px 18px;
  border-radius: var(--radius);
  border: 1px solid var(--line);
  background: var(--paper);
}

.trend-filter-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.trend-chips {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.trend-chip {
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-secondary);
  font-size: 12.5px;
  font-weight: 550;
  cursor: pointer;
  transition: all 0.18s;
  font-family: inherit;
  white-space: nowrap;
}

.trend-chip:hover {
  border-color: var(--accent);
  color: var(--accent);
  background: var(--accent-soft);
}

.trend-chip--active {
  border-color: var(--accent);
  background: var(--accent);
  color: #fff;
}

.trend-chip--active:hover {
  background: var(--accent-strong);
  color: #fff;
  border-color: var(--accent-strong);
}

.trend-sort-group {
  display: flex;
  align-items: center;
  gap: 6px;
}

.trend-sort-label {
  font-size: 12px;
  color: var(--text-tertiary);
  margin-right: 4px;
}

.trend-sort-btn {
  padding: 5px 12px;
  border-radius: 8px;
  border: 1px solid var(--line);
  background: transparent;
  color: var(--text-secondary);
  font-size: 12px;
  font-weight: 550;
  cursor: pointer;
  transition: all 0.18s;
  font-family: inherit;
}

.trend-sort-btn:hover {
  border-color: var(--accent);
  color: var(--accent);
}

.trend-sort-btn--active {
  border-color: var(--accent);
  background: var(--accent-soft);
  color: var(--accent);
}

/* ===== LOADING SKELETON ===== */
.trend-loading {
  display: flex;
  flex-direction: column;
  gap: 2px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--paper);
  overflow: hidden;
}

.trend-skeleton {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 18px 20px;
  border-bottom: 1px solid var(--line);
}

.trend-skel-rank {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  background: rgba(0,0,0,0.04);
  animation: skel-pulse 1.5s ease-in-out infinite;
}

.trend-skel-body {
  flex: 1;
}

.trend-skel-title {
  height: 16px;
  width: 55%;
  border-radius: 4px;
  background: rgba(0,0,0,0.06);
  margin-bottom: 10px;
  animation: skel-pulse 1.5s ease-in-out infinite;
}

.trend-skel-desc {
  height: 12px;
  width: 80%;
  border-radius: 4px;
  background: rgba(0,0,0,0.04);
  margin-bottom: 10px;
  animation: skel-pulse 1.5s ease-in-out 0.2s infinite;
}

.trend-skel-tags {
  height: 18px;
  width: 40%;
  border-radius: 999px;
  background: rgba(0,0,0,0.03);
  animation: skel-pulse 1.5s ease-in-out 0.4s infinite;
}

.trend-skel-score {
  width: 64px;
  height: 56px;
  border-radius: 12px;
  background: rgba(0,0,0,0.04);
  animation: skel-pulse 1.5s ease-in-out 0.1s infinite;
}

@keyframes skel-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

:root[data-theme='dark'] .trend-skel-rank,
:root[data-theme='dark'] .trend-skel-title,
:root[data-theme='dark'] .trend-skel-desc,
:root[data-theme='dark'] .trend-skel-tags,
:root[data-theme='dark'] .trend-skel-score {
  background: rgba(255,255,255,0.06);
}

/* ===== EMPTY STATE ===== */
.trend-empty {
  text-align: center;
  padding: 80px 20px;
  border: 1px dashed var(--line-strong);
  border-radius: var(--radius);
}

.trend-empty-icon {
  font-size: 3rem;
  display: block;
  margin-bottom: 12px;
}

.trend-empty-title {
  font-size: 16px;
  font-weight: 650;
  color: var(--text-main);
  margin-bottom: 6px;
}

.trend-empty-sub {
  font-size: 13.5px;
  color: var(--text-tertiary);
  margin: 0;
}

/* ===== TREND LIST ===== */
.trend-list {
  display: flex;
  flex-direction: column;
  gap: 2px;
  border: 1px solid var(--line);
  border-radius: var(--radius);
  background: var(--paper);
  overflow: hidden;
}

.trend-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 18px 20px;
  transition: background 0.18s;
  text-decoration: none;
  color: inherit;
}

.trend-item:hover {
  background: rgba(0,0,0,0.012);
}

.trend-item + .trend-item {
  border-top: 1px solid var(--line);
}

.trend-item--top3 {
  border-left: 3px solid var(--accent);
  background: var(--accent-soft);
}

.trend-item--top3:hover {
  background: rgba(37, 99, 235, 0.06);
}

.trend-item--hot {
  position: relative;
}

.trend-item--hot::after {
  content: '🔥';
  position: absolute;
  top: 8px;
  right: 10px;
  font-size: 12px;
  opacity: 0.7;
}

/* ===== RANK ===== */
.trend-rank {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 800;
  font-family: var(--font-mono);
  color: var(--text-tertiary);
  background: rgba(0,0,0,0.03);
  margin-top: 2px;
}

.trend-rank--top3 {
  color: #fff;
  background: var(--accent);
  box-shadow: 0 2px 8px rgba(37, 99, 235, 0.25);
}

/* ===== CONTENT ===== */
.trend-content {
  flex: 1;
  min-width: 0;
}

.trend-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
  flex-wrap: wrap;
}

.trend-title {
  font-size: 15px;
  font-weight: 680;
  margin: 0;
  color: var(--text-main);
}

.trend-direction {
  display: inline-flex;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 10.5px;
  font-weight: 650;
  background: rgba(124, 58, 237, 0.1);
  color: #7c3aed;
  white-space: nowrap;
}

:root[data-theme='dark'] .trend-direction {
  background: rgba(167, 139, 250, 0.15);
  color: #a78bfa;
}

.trend-difficulty {
  display: inline-flex;
  padding: 2px 9px;
  border-radius: 999px;
  font-size: 10.5px;
  font-weight: 650;
  background: rgba(13, 148, 136, 0.1);
  color: #0d9488;
  white-space: nowrap;
}

:root[data-theme='dark'] .trend-difficulty {
  background: rgba(45, 212, 191, 0.15);
  color: #2dd4bf;
}

.trend-summary {
  font-size: 13px;
  color: var(--text-secondary);
  margin: 0 0 10px;
  line-height: 1.65;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.trend-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
}

.trend-author {
  font-size: 12px;
  color: var(--text-tertiary);
  white-space: nowrap;
}

.trend-stats {
  display: flex;
  align-items: center;
  gap: 12px;
}

.trend-stat {
  font-size: 12.5px;
  font-family: var(--font-mono);
  color: var(--text-secondary);
  white-space: nowrap;
}

.trend-stat--growth {
  font-weight: 700;
  color: #059669;
  margin-left: 2px;
}

:root[data-theme='dark'] .trend-stat--growth {
  color: #34d399;
}

.trend-stat--forks {
  color: var(--text-tertiary);
}

.trend-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.trend-tag {
  display: inline-flex;
  padding: 2px 8px;
  border-radius: 999px;
  border: 1px solid var(--line);
  font-size: 10.5px;
  font-weight: 550;
  color: var(--text-tertiary);
  white-space: nowrap;
  text-transform: capitalize;
}

/* ===== SCORE COLUMN ===== */
.trend-score-col {
  flex-shrink: 0;
  margin-top: 2px;
}

.trend-score {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  width: 64px;
  padding: 10px 8px;
  border-radius: 12px;
  gap: 2px;
}

.trend-score-num {
  font-size: 20px;
  font-weight: 800;
  font-family: var(--font-mono);
  letter-spacing: -0.03em;
  line-height: 1;
}

.trend-score-label {
  font-size: 10px;
  font-weight: 600;
  opacity: 0.7;
}

.trend-score--hot {
  background: rgba(220, 38, 38, 0.1);
  color: #dc2626;
}

:root[data-theme='dark'] .trend-score--hot {
  background: rgba(248, 113, 113, 0.15);
  color: #f87171;
}

.trend-score--warm {
  background: rgba(234, 88, 12, 0.1);
  color: #ea580c;
}

:root[data-theme='dark'] .trend-score--warm {
  background: rgba(251, 146, 60, 0.15);
  color: #fb923c;
}

.trend-score--mid {
  background: rgba(37, 99, 235, 0.1);
  color: #2563eb;
}

:root[data-theme='dark'] .trend-score--mid {
  background: rgba(96, 165, 250, 0.15);
  color: #60a5fa;
}

.trend-score--low {
  background: rgba(0,0,0,0.04);
  color: var(--text-tertiary);
}

:root[data-theme='dark'] .trend-score--low {
  background: rgba(255,255,255,0.06);
}

/* ===== FOOTER ===== */
.trend-footer {
  padding: 32px 0;
  text-align: center;
  font-size: 12.5px;
  color: var(--text-tertiary);
  border-top: 1px solid var(--line);
  margin-top: 24px;
}

/* ===== RESPONSIVE ===== */
@media (max-width: 768px) {
  .trend-page {
    padding: 20px 16px 36px;
  }

  .trend-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .trend-header-count {
    align-self: flex-start;
  }

  .trend-filter-row {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }

  .trend-sort-group {
    flex-wrap: wrap;
  }

  .trend-item {
    flex-wrap: wrap;
    gap: 12px;
    padding: 14px 16px;
  }

  .trend-score-col {
    width: 100%;
  }

  .trend-score {
    width: 100%;
    flex-direction: row;
    gap: 8px;
    padding: 8px 12px;
  }

  .trend-meta {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}

@media (max-width: 480px) {
  .trend-chips {
    gap: 4px;
  }

  .trend-chip {
    padding: 5px 10px;
    font-size: 11.5px;
  }

  .trend-title {
    font-size: 14px;
  }
}
</style>
