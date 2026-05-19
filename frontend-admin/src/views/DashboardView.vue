<script setup>
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { fetchDashboardSnapshot } from '../api/admin'

const loading = ref(false)
const distributionTab = ref('content')
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
  { label: '内容总量', value: overview.value.totals.contents, tone: 'primary' },
  { label: '来源入口', value: overview.value.totals.sources, tone: 'neutral' },
  { label: '外部引用', value: overview.value.totals.externalRefs, tone: 'accent' }
])

function formatDateTime(value) {
  if (!value) return '暂无时间'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return new Intl.DateTimeFormat('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit', hour12: false }).format(date)
}

function getContentTypeLabel(type) {
  const map = { news: '资讯', paper: '论文', project: '项目', company_update: '公司动态', practice: '技术实践' }
  return map[type] || type || '内容'
}

function getRefTypeLabel(type) {
  const map = { github_repo: 'GitHub 仓库', arxiv_paper: 'arXiv 论文', official_post: '官方公告', community_practice: '社区实践' }
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
    <!-- 统计卡片 -->
    <div class="stat-grid dashboard-stat-grid">
      <article
        v-for="item in statCards"
        :key="item.label"
        class="stat-card dashboard-stat-card"
        :class="`dashboard-stat-card--${item.tone}`"
      >
        <p>{{ item.label }}</p>
        <strong>{{ item.value }}</strong>
      </article>
    </div>

    <!-- 数据分布：全宽扁平 -->
    <section class="card-panel dashboard-panel dashboard-dist-panel">
      <div class="dashboard-panel-head">
        <h4>数据分布</h4>
        <div class="dashboard-tab-group">
          <button
            :class="['dashboard-tab', { active: distributionTab === 'content' }]"
            @click="distributionTab = 'content'"
          >内容类型</button>
          <button
            :class="['dashboard-tab', { active: distributionTab === 'source' }]"
            @click="distributionTab = 'source'"
          >来源类型</button>
        </div>
      </div>
      <div v-show="distributionTab === 'content'" class="dashboard-dist-strip">
        <article v-for="item in overview.contentTypeStats" :key="item.value" class="dashboard-dist-chip">
          <span>{{ item.label }}</span>
          <strong>{{ item.count }}</strong>
          <em>{{ item.ratio }}%</em>
        </article>
      </div>
      <div v-show="distributionTab === 'source'" class="dashboard-dist-strip">
        <article v-for="item in overview.sourceTypeStats" :key="item.value" class="dashboard-dist-chip">
          <span>{{ item.label }}</span>
          <strong>{{ item.count }}</strong>
          <em>{{ item.ratio }}%</em>
        </article>
      </div>
    </section>

    <!-- 近期内容 + 外部引用 并排 -->
    <div class="dashboard-grid dashboard-grid--wide">
      <section class="card-panel dashboard-panel">
        <div class="dashboard-panel-head">
          <h4>近期内容</h4>
          <RouterLink class="dashboard-text-link" to="/contents">内容管理</RouterLink>
        </div>

        <div v-if="overview.recentContents.length" class="dashboard-content-list">
          <article v-for="item in overview.recentContents" :key="item.id" class="dashboard-content-card">
            <div>
              <span>{{ getContentTypeLabel(item.contentType) }} · {{ item.categoryName || '未分类' }}</span>
              <strong>{{ item.title }}</strong>
              <p>{{ item.sourceName || '人工录入' }} · {{ formatDateTime(item.updatedAt) }}</p>
            </div>
          </article>
        </div>

        <p v-else class="dashboard-empty">暂无内容</p>
      </section>

      <section class="card-panel dashboard-panel">
        <div class="dashboard-panel-head">
          <h4>外部引用</h4>
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
            <strong>{{ item.externalId || '未设置' }}</strong>
            <em>{{ formatDateTime(item.syncedAt) }}</em>
          </a>
        </div>

        <p v-else class="dashboard-empty">暂无外部引用</p>
      </section>
    </div>
  </section>
</template>
