<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { fetchDashboardSnapshot } from '../api/admin'

const loading = ref(false)
const overview = ref({
  totals: {
    contents: 0,
    published: 0,
    drafts: 0,
    archived: 0,
    categories: 0,
    tags: 0,
    sources: 0,
    externalRefs: 0
  },
  publishStatusStats: [],
  contentTypeStats: [],
  sourceTypeStats: [],
  recentContents: [],
  recentExternalRefs: []
})

const statCards = computed(() => [
  { label: '内容总量', value: overview.value.totals.contents, note: '统一内容中心', tone: 'primary' },
  { label: '已发布', value: overview.value.totals.published, note: '前台可见内容', tone: 'success' },
  { label: '来源入口', value: overview.value.totals.sources, note: 'GitHub / 论文 / 官方博客', tone: 'neutral' },
  { label: '外部引用', value: overview.value.totals.externalRefs, note: '后续同步映射', tone: 'accent' }
])

const operationCards = computed(() => [
  {
    title: '发布完成度',
    value: `${findRatio(overview.value.publishStatusStats, 'PUBLISHED')}%`,
    description: '已发布内容占全部内容的比例，适合说明内容运营进度。'
  },
  {
    title: '项目内容占比',
    value: `${findRatio(overview.value.contentTypeStats, 'project')}%`,
    description: '项目类内容越多，越能体现 AI 信息聚合平台方向。'
  },
  {
    title: 'GitHub 来源占比',
    value: `${findRatio(overview.value.sourceTypeStats, 'github')}%`,
    description: '来源结构可以帮助解释后续采集模块如何扩展。'
  }
])

function findRatio(items, value) {
  return items.find((item) => item.value === value)?.ratio || 0
}

function formatDateTime(value) {
  if (!value) {
    return '暂无时间'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(date)
}

function getContentTypeLabel(type) {
  const map = {
    news: '资讯',
    paper: '论文',
    project: '项目',
    company_update: '公司动态',
    practice: '技术实践'
  }
  return map[type] || type || '内容'
}

function getStatusType(status) {
  if (status === 'PUBLISHED') {
    return 'success'
  }
  if (status === 'ARCHIVED') {
    return 'info'
  }
  return 'warning'
}

function getRefTypeLabel(type) {
  const map = {
    github_repo: 'GitHub 仓库',
    arxiv_paper: 'arXiv 论文',
    official_post: '官方公告',
    community_practice: '社区实践'
  }
  return map[type] || type || '外部引用'
}

async function loadDashboard() {
  loading.value = true
  try {
    const res = await fetchDashboardSnapshot()
    overview.value = res.data
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

onMounted(loadDashboard)
</script>

<template>
  <section class="admin-page" v-loading="loading">
    <div class="page-intro dashboard-hero">
      <span class="sidebar-kicker">Dashboard</span>
      <h3>AI 内容运营控制台</h3>
      <p>统计口径已由后端统一提供，展示内容规模、发布状态、来源结构和近期外部引用动态。</p>
    </div>

    <div class="stat-grid dashboard-stat-grid">
      <article
        v-for="item in statCards"
        :key="item.label"
        class="stat-card dashboard-stat-card"
        :class="`dashboard-stat-card--${item.tone}`"
      >
        <p>{{ item.label }}</p>
        <strong>{{ item.value }}</strong>
        <span>{{ item.note }}</span>
      </article>
    </div>

    <div class="dashboard-grid dashboard-grid--wide">
      <section class="card-panel dashboard-panel">
        <div class="dashboard-panel-head">
          <div>
            <span class="sidebar-kicker">Distribution</span>
            <h4>发布状态分布</h4>
          </div>
          <strong>{{ overview.totals.published }}/{{ overview.totals.contents }}</strong>
        </div>

        <div class="dashboard-bar-list">
          <article v-for="item in overview.publishStatusStats" :key="item.value" class="dashboard-bar-item">
            <div class="dashboard-bar-meta">
              <span>{{ item.label }}</span>
              <strong>{{ item.count }} 条 · {{ item.ratio }}%</strong>
            </div>
            <div class="dashboard-bar-track">
              <span :style="{ width: `${item.ratio}%` }"></span>
            </div>
          </article>
        </div>
      </section>

      <section class="card-panel dashboard-panel">
        <div class="dashboard-panel-head">
          <div>
            <span class="sidebar-kicker">Operating Signals</span>
            <h4>运营信号</h4>
          </div>
        </div>

        <div class="dashboard-signal-grid">
          <article v-for="item in operationCards" :key="item.title" class="dashboard-signal-card">
            <strong>{{ item.value }}</strong>
            <span>{{ item.title }}</span>
            <p>{{ item.description }}</p>
          </article>
        </div>
      </section>
    </div>

    <div class="dashboard-grid dashboard-grid--wide">
      <section class="card-panel dashboard-panel">
        <div class="dashboard-panel-head">
          <div>
            <span class="sidebar-kicker">Content Types</span>
            <h4>内容类型分布</h4>
          </div>
        </div>

        <div class="dashboard-pill-list">
          <article v-for="item in overview.contentTypeStats" :key="item.value" class="dashboard-pill-row">
            <span>{{ item.label }}</span>
            <strong>{{ item.count }}</strong>
            <em>{{ item.ratio }}%</em>
          </article>
        </div>
      </section>

      <section class="card-panel dashboard-panel">
        <div class="dashboard-panel-head">
          <div>
            <span class="sidebar-kicker">Source Types</span>
            <h4>来源类型分布</h4>
          </div>
        </div>

        <div class="dashboard-pill-list">
          <article v-for="item in overview.sourceTypeStats" :key="item.value" class="dashboard-pill-row">
            <span>{{ item.label }}</span>
            <strong>{{ item.count }}</strong>
            <em>{{ item.ratio }}%</em>
          </article>
        </div>
      </section>
    </div>

    <div class="dashboard-grid dashboard-grid--wide">
      <section class="card-panel dashboard-panel dashboard-panel--large">
        <div class="dashboard-panel-head">
          <div>
            <span class="sidebar-kicker">Recent Contents</span>
            <h4>近期内容</h4>
          </div>
          <RouterLink class="dashboard-text-link" to="/contents">进入内容管理</RouterLink>
        </div>

        <div v-if="overview.recentContents.length" class="dashboard-content-list">
          <article v-for="item in overview.recentContents" :key="item.id" class="dashboard-content-card">
            <div>
              <span>{{ getContentTypeLabel(item.contentType) }} · {{ item.categoryName || '未分类' }}</span>
              <strong>{{ item.title }}</strong>
              <p>{{ item.sourceName || '人工录入' }} · 浏览 {{ item.viewCount || 0 }} · 更新 {{ formatDateTime(item.updatedAt) }}</p>
            </div>
            <el-tag :type="getStatusType(item.publishStatus)" effect="plain">{{ item.publishStatus }}</el-tag>
          </article>
        </div>

        <p v-else class="dashboard-empty">暂无内容，先去内容管理页创建第一条内容。</p>
      </section>

      <section class="card-panel dashboard-panel">
        <div class="dashboard-panel-head">
          <div>
            <span class="sidebar-kicker">External Refs</span>
            <h4>外部引用动态</h4>
          </div>
        </div>

        <div v-if="overview.recentExternalRefs.length" class="dashboard-ref-list">
          <a
            v-for="item in overview.recentExternalRefs"
            :key="item.id"
            class="dashboard-ref-card"
            :href="item.externalUrl"
            target="_blank"
            rel="noreferrer"
          >
            <span>{{ getRefTypeLabel(item.refType) }}</span>
            <strong>{{ item.externalId || '未设置外部 ID' }}</strong>
            <p>{{ item.contentTitle }}</p>
            <em>{{ formatDateTime(item.syncedAt) }}</em>
          </a>
        </div>

        <p v-else class="dashboard-empty">暂无外部引用，后续导入 GitHub 项目后会显示在这里。</p>
      </section>
    </div>
  </section>
</template>
