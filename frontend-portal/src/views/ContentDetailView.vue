<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import PortalTopbar from '../components/PortalTopbar.vue'
import { getPortalContentDetail } from '../api/portal'
import {
  formatDateTime,
  getContentTypeMeta,
  getExternalRefMeta,
  getSourceTypeMeta,
  parseExtraJson
} from '../utils/content'
import { renderSafeMarkdown } from '../utils/safeMarkdown'

const route = useRoute()
const loading = ref(false)
const errorMessage = ref('')
const detail = ref(null)

const typeMeta = computed(() => getContentTypeMeta(detail.value?.contentType))
const sourceTypeMeta = computed(() => getSourceTypeMeta(detail.value?.sourceType))
const renderedBody = computed(() => renderSafeMarkdown(detail.value?.bodyMarkdown || ''))
const extraEntries = computed(() => parseExtraJson(detail.value?.extraJson))
const extraObject = computed(() => {
  if (!detail.value?.extraJson) {
    return {}
  }
  try {
    return JSON.parse(detail.value.extraJson)
  } catch (error) {
    return {}
  }
})
const aiGuide = computed(() => ({
  aiSummary: extraObject.value.aiSummary,
  recommendationReason: extraObject.value.recommendationReason,
  importanceScore: extraObject.value.importanceScore,
  sourceBrief: extraObject.value.sourceBrief,
  sourceTitle: extraObject.value.sourceTitle,
  sourceImage: extraObject.value.sourceImage,
  originalSummary: extraObject.value.originalSummary,
  originalExcerpt: extraObject.value.originalExcerpt
}))
const hasAiGuide = computed(() => Boolean(
  aiGuide.value.aiSummary ||
  aiGuide.value.recommendationReason ||
  aiGuide.value.sourceBrief ||
  aiGuide.value.originalSummary ||
  aiGuide.value.originalExcerpt
))
const externalRefs = computed(() => detail.value?.externalRefs || [])

const subCategoryLabelMap = {
  '3d_ct_denoising': '3D CT去噪',
  'medical_imaging': '医学影像',
  'large_model': '大模型'
}
const subCategoryLabel = computed(() => {
  const sub = detail.value?.subCategory
  if (!sub) return ''
  return subCategoryLabelMap[sub] || sub
})

const sectionPathMap = {
  paper: '/papers',
  project: '/github',
  news: '/news',
  company_update: '/company',
  arena: '/arena',
  practice: '/tools'
}
const backPath = computed(() => sectionPathMap[detail.value?.contentType] || '/contents')

async function loadDetail(id) {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getPortalContentDetail(id)
    detail.value = res.data
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

onMounted(() => loadDetail(route.params.id))
watch(() => route.params.id, (id) => loadDetail(id))
</script>

<template>
  <div class="portal-shell">
    <PortalTopbar context-label="Reading Mode" context-value="内容详情" />

    <main class="page-stack" v-if="detail">
      <section class="detail-hero">
        <article class="section-shell detail-hero-main">
          <span class="eyebrow-line">
            {{ typeMeta.label }} · {{ detail.categoryName }}
            <span v-if="subCategoryLabel" class="sub-category-tag">{{ subCategoryLabel }}</span>
          </span>
          <h1>{{ detail.title }}</h1>
          <p class="lead-copy">{{ detail.summary }}</p>

          <div class="detail-meta-grid">
            <div class="detail-meta-cell">
              <span>发布时间</span>
              <strong>{{ formatDateTime(detail.publishedAt) }}</strong>
            </div>
            <div class="detail-meta-cell">
              <span>来源</span>
              <strong>{{ detail.sourceName || '人工录入' }}</strong>
            </div>

            <div class="detail-meta-cell">
              <span>浏览量</span>
              <strong>{{ detail.viewCount }}</strong>
            </div>
          </div>

          <div class="hero-actions">
            <RouterLink class="secondary-btn" :to="backPath">返回内容广场</RouterLink>
            <a
              v-if="detail.sourceUrl"
              class="primary-btn"
              :href="detail.sourceUrl"
              target="_blank"
              rel="noreferrer"
            >
              查看原始链接
            </a>
          </div>
        </article>


      </section>

      <section v-if="hasAiGuide" class="section-shell ai-guide-panel">
        <div class="ai-guide-copy">
          <span class="section-kicker">AI Reading Brief</span>
          <h2>AI 导读</h2>
          <p>{{ aiGuide.aiSummary || detail.summary }}</p>
          <div class="ai-guide-reason" v-if="aiGuide.recommendationReason">
            <strong>推荐理由</strong>
            <span>{{ aiGuide.recommendationReason }}</span>
          </div>
        </div>

        <aside class="ai-source-card">
          <img v-if="aiGuide.sourceImage" :src="aiGuide.sourceImage" :alt="aiGuide.sourceTitle || detail.title" />
          <span class="section-kicker">Original Source</span>
          <h3>{{ aiGuide.sourceTitle || detail.sourceName || '原始来源' }}</h3>
          <p>{{ aiGuide.sourceBrief || '该内容由后台人工录入来源信息，并经过 AI 总结层整理。' }}</p>
          <div class="ai-score-pill" v-if="aiGuide.importanceScore">
            <span>重要性评分</span>
            <strong>{{ aiGuide.importanceScore }}</strong>
          </div>
        </aside>
      </section>

      <section class="detail-content-grid">
        <article class="section-shell article-shell">
          <div class="section-head section-head--tight">
            <div>
              <span class="section-kicker">Original Reading</span>
              <h2>正文与原文摘录</h2>
            </div>
          </div>

          <div v-if="aiGuide.originalSummary || aiGuide.originalExcerpt" class="source-excerpt-panel">
            <span class="section-kicker">Source Excerpt</span>
            <p v-if="aiGuide.originalSummary">{{ aiGuide.originalSummary }}</p>
            <blockquote v-if="aiGuide.originalExcerpt">{{ aiGuide.originalExcerpt }}</blockquote>
            <a
              v-if="detail.sourceUrl"
              :href="detail.sourceUrl"
              target="_blank"
              rel="noreferrer"
            >
              查看原始来源
            </a>
          </div>

          <div class="article-rendered" v-html="renderedBody"></div>
        </article>
      </section>
    </main>

    <main class="page-stack" v-else>
      <article class="section-shell status-panel">
        <span class="section-kicker">System State</span>
        <h1>{{ loading ? '正在加载详情' : '详情暂时无法显示' }}</h1>
        <p class="lead-copy">
          {{ loading ? '正在从后端读取内容详情。' : errorMessage || '当前内容不存在或尚未发布。' }}
        </p>
      </article>
    </main>
  </div>
</template>

<style scoped>
.sub-category-tag {
  display: inline-block;
  margin-left: 8px;
  padding: 2px 10px;
  font-size: 12px;
  font-weight: 600;
  color: #3b82f6;
  background: rgba(59, 130, 246, 0.1);
  border: 1px solid rgba(59, 130, 246, 0.25);
  border-radius: 999px;
  vertical-align: middle;
}
</style>
