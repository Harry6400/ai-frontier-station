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

const hasContentData = computed(() => overview.value.totals.contents > 0)
const hasStructureData = computed(() =>
  overview.value.totals.categories > 0 || overview.value.totals.tags > 0 || overview.value.totals.sources > 0
)

const statCards = computed(() => [
  { label: '内容总量', value: overview.value.totals.contents, note: '当前数据库内容' },
  { label: '已发布', value: overview.value.totals.published, note: '前台可见内容' },
  { label: '草稿', value: overview.value.totals.drafts, note: '待整理内容' },
  { label: '外部引用', value: overview.value.totals.externalRefs, note: '来源映射记录' }
])

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
      <h3>内容工作台概览</h3>
      <p>这里只保留最有用的状态：当前库里有没有内容、有哪些基础结构、下一步该去哪里操作。</p>
    </div>

    <section class="card-panel dashboard-panel dashboard-connection-panel">
      <div class="dashboard-panel-head">
        <div>
          <span class="sidebar-kicker">Data Status</span>
          <h4>{{ hasContentData ? '数据库已有内容' : '当前库暂无内容' }}</h4>
        </div>
      </div>
      <p>
        {{ hasContentData
          ? 'Dashboard 已从后端读取到内容数据，可以继续整理、发布或导入外部来源。'
          : '后端接口正常返回，但当前连接的数据库内容表为空；这不是页面故障，而是当前库里没有可统计的内容。' }}
      </p>
      <p class="micro-note">
        基础结构：分类 {{ overview.totals.categories }} · 标签 {{ overview.totals.tags }} · 来源 {{ overview.totals.sources }}
        <span v-if="!hasStructureData">。如果这里也为 0，说明当前库还没有初始化基础数据。</span>
      </p>
    </section>

    <div class="stat-grid dashboard-stat-grid dashboard-stat-grid--lean">
      <article v-for="item in statCards" :key="item.label" class="stat-card dashboard-stat-card">
        <p>{{ item.label }}</p>
        <strong>{{ item.value }}</strong>
        <span>{{ item.note }}</span>
      </article>
    </div>

    <div class="dashboard-grid dashboard-grid--wide dashboard-grid--lean">
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
          </article>
        </div>

        <p v-else class="dashboard-empty">当前库暂无内容。去内容管理页新建、导入 GitHub 项目或整理 AI 来源后，这里会显示最近记录。</p>
      </section>

      <section class="card-panel dashboard-panel">
        <div class="dashboard-panel-head">
          <div>
            <span class="sidebar-kicker">Quick Actions</span>
            <h4>下一步操作</h4>
          </div>
        </div>

        <div class="dashboard-action-list">
          <RouterLink class="dashboard-action-card" to="/contents">整理内容</RouterLink>
          <RouterLink class="dashboard-action-card" to="/api-settings">配置 API Key</RouterLink>
          <RouterLink class="dashboard-action-card" to="/sources">维护来源</RouterLink>
        </div>
      </section>
    </div>
  </section>
</template>
