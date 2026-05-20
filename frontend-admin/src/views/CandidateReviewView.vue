<script setup>
import { onMounted, ref, computed } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import {
  triggerFetch,
  getCustomPrompt,
  saveCustomPrompt as saveCustomPromptApi,
  getEvents,
  approveEvent,
  rejectEvent,
  triggerAutoCluster,
  getContents,
  getEventDetail
} from '../api/admin'

/* ── fetch groups ── */
const REVIEW_SOURCES = [
  { key: 'news', label: 'AI资讯', icon: '📰', color: '#2563eb' },
  { key: 'product', label: '产品动态', icon: '📦', color: '#059669' }
]

const AUTO_SOURCES = [
  { key: 'github', label: 'GitHub', icon: '🐙', color: '#6e40c9' },
  { key: 'tools', label: '工具实践', icon: '🔧', color: '#e11d48' }
]

/* ── state ── */
const fetchingSource = ref(null) // which source is currently fetching

/* ── events state ── */
const eventList = ref([])
const eventLoading = ref(false)
const eventTotal = ref(0)
const eventPage = ref(1)
const eventPageSize = ref(10)
const expandedEventId = ref(null)
const expandedEventDetail = ref(null)
const eventDetailLoading = ref(false)
const eventDetailTab = ref('body')
const eventReviewTab = ref('all')

const filteredEventList = computed(() => {
  if (eventReviewTab.value === 'all') return eventList.value
  return eventList.value.filter(ev => {
    // Check tags array first
    if (ev.tags && Array.isArray(ev.tags)) {
      return ev.tags.some(t => {
        const tag = (typeof t === 'string' ? t : t.name || t.label || '').toLowerCase()
        if (eventReviewTab.value === 'news') return tag.includes('资讯') || tag.includes('news') || tag.includes('ai')
        if (eventReviewTab.value === 'product') return tag.includes('产品') || tag.includes('product') || tag.includes('动态')
        return false
      })
    }
    // Fallback: check sourceType or eventType
    const src = (ev.sourceType || ev.eventType || ev.type || '').toLowerCase()
    if (eventReviewTab.value === 'news') return src.includes('news')
    if (eventReviewTab.value === 'product') return src.includes('product')
    // Fallback: check title for product patterns
    const title = (ev.title || '').toLowerCase()
    if (eventReviewTab.value === 'product') return title.includes('最新动态') || title.includes('发布') || title.includes('产品')
    if (eventReviewTab.value === 'news') return !title.includes('最新动态')
    return true
  })
})

/* ── recent auto-published state ── */
const recentList = ref([])
const recentLoading = ref(false)

/* ── prompt editor ── */
const promptDialogVisible = ref(false)
const customPrompt = ref(`你是一个专业的AI技术内容分析助手。请对以下原始内容进行分析和整理，输出要求：

1. **必须使用中文输出**
2. **使用结构化 Markdown 格式**，包含以下部分：

## 一句话摘要
用一句简洁的话概括核心内容。

## 核心要点
列出 3-5 个关键要点，使用列表格式。

## 技术分析
对技术细节进行分析，包括技术原理、创新点、实现方式等。

## 影响/意义
分析该内容对AI行业/技术发展的影响和意义。

要求：简洁、专业、信息密度高，避免冗余描述。`)

function openPromptEditor() {
  promptDialogVisible.value = true
}

async function savePrompt() {
  try {
    await saveCustomPromptApi(customPrompt.value)
    promptDialogVisible.value = false
    ElMessage.success('提示词已保存')
  } catch (e) {
    ElMessage.error('保存提示词失败')
  }
}

// Load saved prompt from backend on init
getCustomPrompt().then(res => {
  if (res.data) customPrompt.value = res.data
}).catch(() => {
  const savedPrompt = localStorage.getItem('ai_review_prompt')
  if (savedPrompt) customPrompt.value = savedPrompt
})

/* ── trigger fetch (review sources: fetch + cluster) ── */
async function handleFetchReview(source) {
  fetchingSource.value = source.key
  try {
    await triggerFetch(source.key)
    ElMessage.success(`已触发 ${source.label} 数据采集`)
    try {
      await triggerAutoCluster()
      ElMessage.success('自动聚类已触发')
    } catch {
      // clustering may fail silently if no new data
    }
    setTimeout(() => { loadEvents() }, 2000)
  } catch (err) {
    ElMessage.error('触发采集失败：' + (err.response?.data?.message || err.message))
  } finally {
    fetchingSource.value = null
  }
}

/* ── trigger fetch (auto sources: fetch + auto-publish) ── */
async function handleFetchAuto(source) {
  fetchingSource.value = source.key
  try {
    await triggerFetch(source.key)
    ElMessage.success(`已触发 ${source.label} 数据采集，将自动发布`)
    setTimeout(() => { loadRecentPublished() }, 3000)
  } catch (err) {
    ElMessage.error('触发采集失败：' + (err.response?.data?.message || err.message))
  } finally {
    fetchingSource.value = null
  }
}

/* ── events ── */
async function loadEvents() {
  eventLoading.value = true
  try {
    const res = await getEvents({ pageNum: eventPage.value, pageSize: eventPageSize.value })
    eventList.value = res.data?.records || res.data?.items || res.data?.list || res.data || []
    eventTotal.value = res.data?.total || eventList.value.length
  } catch (err) {
    ElMessage.error('加载事件列表失败：' + (err.response?.data?.message || err.message))
  } finally {
    eventLoading.value = false
  }
}

async function handleApproveEvent(event) {
  try {
    await ElMessageBox.confirm(`确认批准事件「${event.title}」？将发布所有关联内容。`, '批准事件', { type: 'info' })
    await approveEvent(event.id)
    ElMessage.success('事件已批准，关联内容已发布')
    loadEvents()
    loadRecentPublished()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('批准失败：' + (err.response?.data?.message || err.message))
    }
  }
}

async function handleRejectEvent(event) {
  try {
    await ElMessageBox.confirm(`确认拒绝事件「${event.title}」？`, '拒绝事件', { type: 'warning' })
    await rejectEvent(event.id)
    ElMessage.success('事件已拒绝')
    loadEvents()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('拒绝失败：' + (err.response?.data?.message || err.message))
    }
  }
}

function handleEventPageChange(p) {
  eventPage.value = p
  loadEvents()
}

async function toggleEventExpand(event) {
  if (expandedEventId.value === event.id) {
    expandedEventId.value = null
    expandedEventDetail.value = null
    eventDetailTab.value = 'body'
    return
  }
  expandedEventId.value = event.id
  eventDetailTab.value = 'body'
  eventDetailLoading.value = true
  try {
    const res = await getEventDetail(event.id)
    expandedEventDetail.value = res.data
  } catch (err) {
    ElMessage.error('加载事件详情失败：' + (err.response?.data?.message || err.message))
    expandedEventDetail.value = null
  } finally {
    eventDetailLoading.value = false
  }
}

/* ── recent auto-published ── */
async function loadRecentPublished() {
  recentLoading.value = true
  try {
    const res = await getContents({ pageNum: 1, pageSize: 10, publishStatus: 'published', sort: 'publishedAt,desc' })
    recentList.value = res.data?.records || res.data?.items || res.data?.list || res.data || []
  } catch {
    // silent fail
  } finally {
    recentLoading.value = false
  }
}

/* ── helpers ── */
function formatDate(val) {
  if (!val) return '—'
  return new Date(val).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function truncateText(text, len = 80) {
  if (!text) return ''
  return text.length > len ? text.slice(0, len) + '…' : text
}

/* ── lifecycle ── */
onMounted(() => {
  loadEvents()
  loadRecentPublished()
})
</script>

<template>
  <section class="candidate-page">

    <!-- ══════════════════════════════════════════════════════
         TOP: Trigger Fetch Buttons
    ══════════════════════════════════════════════════════ -->
    <div class="fetch-section">
      <div class="fetch-section-header">
        <h3>🚀 数据采集</h3>
        <el-button size="small" @click="openPromptEditor" plain>
          ✏️ 编辑提示词
        </el-button>
      </div>

      <div class="fetch-groups">
        <!-- 需要审核 group -->
        <div class="fetch-group">
          <div class="fetch-group-label">
            <span class="fetch-group-badge review">需要审核</span>
            <span class="fetch-group-desc">采集后需人工审核事件</span>
          </div>
          <div class="fetch-group-buttons">
            <button
              v-for="src in REVIEW_SOURCES"
              :key="src.key"
              class="fetch-btn"
              :class="{ fetching: fetchingSource === src.key }"
              :style="{ '--src-color': src.color }"
              :disabled="fetchingSource !== null"
              @click="handleFetchReview(src)"
            >
              <span class="fetch-btn-icon">{{ src.icon }}</span>
              <span class="fetch-btn-label">{{ src.label }}</span>
              <span v-if="fetchingSource === src.key" class="fetch-btn-spinner"></span>
              <span v-else class="fetch-btn-arrow">→</span>
            </button>
          </div>
        </div>

        <!-- 自动发布 group -->
        <div class="fetch-group">
          <div class="fetch-group-label">
            <span class="fetch-group-badge auto">自动发布</span>
            <span class="fetch-group-desc">采集后直接发布，无需审核</span>
          </div>
          <div class="fetch-group-buttons">
            <button
              v-for="src in AUTO_SOURCES"
              :key="src.key"
              class="fetch-btn"
              :class="{ fetching: fetchingSource === src.key }"
              :style="{ '--src-color': src.color }"
              :disabled="fetchingSource !== null"
              @click="handleFetchAuto(src)"
            >
              <span class="fetch-btn-icon">{{ src.icon }}</span>
              <span class="fetch-btn-label">{{ src.label }}</span>
              <span v-if="fetchingSource === src.key" class="fetch-btn-spinner"></span>
              <span v-else class="fetch-btn-auto-badge">AUTO</span>
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- ══════════════════════════════════════════════════════
         MIDDLE: Event Review
    ══════════════════════════════════════════════════════ -->
    <div class="event-section">
      <div class="event-section-header">
        <h3>📋 事件审核</h3>
        <el-button size="small" @click="loadEvents" :loading="eventLoading" plain>刷新</el-button>
      </div>

      <!-- Event Review Tabs -->
      <div class="event-review-tabs">
        <button
          class="event-review-tab"
          :class="{ active: eventReviewTab === 'all' }"
          @click="eventReviewTab = 'all'"
        >全部</button>
        <button
          class="event-review-tab"
          :class="{ active: eventReviewTab === 'news' }"
          @click="eventReviewTab = 'news'"
        >📰 AI资讯</button>
        <button
          class="event-review-tab"
          :class="{ active: eventReviewTab === 'product' }"
          @click="eventReviewTab = 'product'"
        >📦 产品动态</button>
      </div>

      <div v-if="eventLoading && eventList.length === 0" class="event-loading">
        <span class="event-loading-spinner"></span>
        加载中...
      </div>
      <div v-else-if="filteredEventList.length === 0" class="event-empty">
        <span class="event-empty-icon">📭</span>
        {{ eventReviewTab === 'all' ? '暂无待审核事件' : '该分类暂无事件' }}
      </div>
      <div v-else class="event-list">
        <div v-for="event in filteredEventList" :key="event.id" class="event-card-wrapper">
          <div
            class="event-card"
            :class="{ 'event-card--expanded': expandedEventId === event.id }"
            @click="toggleEventExpand(event)"
          >
            <div class="event-card-body">
              <div class="event-title">{{ event.title }}</div>
              <div class="event-summary">{{ event.summary }}</div>
              <div class="event-meta">
                <el-tag size="small" type="info">
                  {{ event.contentCount || event.contents?.length || 0 }} 条关联内容
                </el-tag>
                <span v-if="event.status" class="event-status-tag">
                  <el-tag
                    size="small"
                    :type="event.status === 'approved' ? 'success' : event.status === 'rejected' ? 'danger' : 'warning'"
                  >
                    {{ event.status === 'approved' ? '已批准' : event.status === 'rejected' ? '已拒绝' : '待审核' }}
                  </el-tag>
                </span>
                <span class="event-expand-hint">
                  {{ expandedEventId === event.id ? '收起详情 ▲' : '展开详情 ▼' }}
                </span>
              </div>
            </div>
            <div
              class="event-card-actions"
              v-if="event.status !== 'approved' && event.status !== 'rejected'"
              @click.stop
            >
              <button class="cd-btn cd-btn-primary" @click.stop="handleApproveEvent(event)">✓ 批准</button>
              <button class="cd-btn cd-btn-outline-danger" @click.stop="handleRejectEvent(event)">✗ 拒绝</button>
            </div>
          </div>

          <!-- ═══ Expanded Detail Panel ═══ -->
          <transition name="slide-down">
            <div v-if="expandedEventId === event.id" class="event-detail-panel">
              <div v-if="eventDetailLoading" class="event-detail-loading">
                <span class="event-loading-spinner"></span>
                加载详情中...
              </div>
              <div v-else-if="expandedEventDetail" class="event-detail-content">
                <!-- Sidebar: Event Info -->
                <div class="event-detail-sidebar">
                  <div class="event-detail-sidebar-title">{{ expandedEventDetail.title }}</div>
                  <div class="event-detail-sidebar-summary">{{ expandedEventDetail.summary }}</div>
                  <div class="event-detail-sidebar-status">
                    <el-tag
                      size="small"
                      :type="expandedEventDetail.status === 'approved' ? 'success' : expandedEventDetail.status === 'rejected' ? 'danger' : 'warning'"
                    >
                      {{ expandedEventDetail.status === 'approved' ? '已批准' : expandedEventDetail.status === 'rejected' ? '已拒绝' : '待审核' }}
                    </el-tag>
                  </div>
                  <div
                    v-if="expandedEventDetail.status !== 'approved' && expandedEventDetail.status !== 'rejected'"
                    class="event-detail-sidebar-actions"
                  >
                    <button class="cd-btn cd-btn-primary" style="width:100%;" @click="handleApproveEvent(expandedEventDetail)">✓ 批准</button>
                    <button class="cd-btn cd-btn-outline-danger" style="width:100%;" @click="handleRejectEvent(expandedEventDetail)">✗ 拒绝</button>
                  </div>
                </div>

                <!-- Main Content: Tabs -->
                <div class="event-detail-main">
                  <div class="event-detail-tabs">
                    <button
                      class="event-detail-tab"
                      :class="{ active: eventDetailTab === 'body' }"
                      @click="eventDetailTab = 'body'"
                    >📄 正文</button>
                    <button
                      class="event-detail-tab"
                      :class="{ active: eventDetailTab === 'sources' }"
                      @click="eventDetailTab = 'sources'"
                    >🔗 内容来源
                      <span v-if="expandedEventDetail.contents?.length" class="tab-badge">
                        {{ expandedEventDetail.contents.length }}
                      </span>
                    </button>
                  </div>

                  <!-- Tab: 正文 -->
                  <div v-if="eventDetailTab === 'body'" class="event-detail-body">
                    <div v-if="expandedEventDetail.aiDescription || expandedEventDetail.description" class="event-detail-body-text">
                      <pre>{{ expandedEventDetail.aiDescription || expandedEventDetail.description }}</pre>
                    </div>
                    <div v-else class="event-detail-empty">
                      暂无正文内容
                    </div>
                  </div>

                  <!-- Tab: 内容来源 -->
                  <div v-if="eventDetailTab === 'sources'" class="event-detail-sources">
                    <div v-if="!expandedEventDetail.contents?.length" class="event-detail-empty">
                      暂无关联内容
                    </div>
                    <div v-else class="source-list">
                      <div v-for="item in expandedEventDetail.contents" :key="item.id" class="source-item">
                        <div class="source-item-header">
                          <a
                            v-if="item.sourceUrl || item.url"
                            :href="item.sourceUrl || item.url"
                            target="_blank"
                            rel="noopener noreferrer"
                            class="source-item-title"
                          >{{ item.title || '未命名' }}</a>
                          <span v-else class="source-item-title">{{ item.title || '未命名' }}</span>
                          <el-tag v-if="item.sourceType" size="small" effect="plain" class="source-item-type">
                            {{ item.sourceType === 'github' ? 'GitHub' : item.sourceType === 'tools' ? '工具实践' : item.sourceType === 'news' ? 'AI资讯' : item.sourceType === 'product' ? '产品动态' : item.sourceType }}
                          </el-tag>
                        </div>
                        <div v-if="item.summary || item.aiSummary" class="source-item-summary">
                          {{ truncateText(item.summary || item.aiSummary, 120) }}
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </transition>
        </div>
        <div v-if="eventTotal > eventPageSize" class="event-pagination">
          <el-pagination
            background
            layout="prev, pager, next"
            :total="eventTotal"
            :page-size="eventPageSize"
            :current-page="eventPage"
            @current-change="handleEventPageChange"
          />
        </div>
      </div>
    </div>

    <!-- ══════════════════════════════════════════════════════
         BOTTOM: Recently Auto-Published
    ══════════════════════════════════════════════════════ -->
    <div class="recent-section">
      <div class="recent-section-header">
        <h3>✅ 最近自动发布</h3>
        <el-button size="small" @click="loadRecentPublished" :loading="recentLoading" plain>刷新</el-button>
      </div>

      <div v-if="recentLoading && recentList.length === 0" class="recent-loading">
        <span class="event-loading-spinner"></span>
        加载中...
      </div>
      <div v-else-if="recentList.length === 0" class="recent-empty">
        <span class="event-empty-icon">📭</span>
        暂无已发布内容
      </div>
      <div v-else class="recent-list">
        <div v-for="item in recentList" :key="item.id" class="recent-item">
          <div class="recent-item-main">
            <span class="recent-item-title">{{ truncateText(item.title || item.aiSummary) }}</span>
            <span class="recent-item-time">{{ formatDate(item.publishedAt || item.createdAt) }}</span>
          </div>
          <div v-if="item.sourceType" class="recent-item-source">
            <el-tag size="small" :type="item.sourceType === 'github' ? '' : 'success'" effect="plain">
              {{ item.sourceType === 'github' ? 'GitHub' : item.sourceType === 'tools' ? '工具实践' : item.sourceType }}
            </el-tag>
          </div>
        </div>
      </div>
    </div>

    <!-- ══════════════════════════════════════════════════════
         DIALOGS
    ══════════════════════════════════════════════════════ -->

    <!-- prompt editor dialog -->
    <el-dialog
      v-model="promptDialogVisible"
      title="✏️ 编辑 AI 处理提示词"
      width="600px"
      append-to-body
    >
      <p style="margin-bottom:12px;color:var(--admin-text-secondary,#666);font-size:13px;">
        自定义发送给 AI 的提示词模板。修改后点击保存，后续 AI 处理将使用新的提示词。
      </p>
      <el-input
        v-model="customPrompt"
        type="textarea"
        :rows="16"
        placeholder="输入提示词..."
        style="font-family: monospace;"
      />
      <template #footer>
        <el-button @click="promptDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="savePrompt">保存提示词</el-button>
      </template>
    </el-dialog>

  </section>
</template>

<style scoped>
.candidate-page {
  padding: 0;
  display: flex;
  flex-direction: column;
  gap: 20px;
}

/* ═══════════════════════════════════════════
   FETCH SECTION
═══════════════════════════════════════════ */
.fetch-section {
  background: var(--admin-surface, #fff);
  border: 1px solid var(--admin-border, #e5e7eb);
  border-radius: 8px;
  padding: 20px;
}

.fetch-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.fetch-section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--admin-text, #1f2937);
}

.fetch-groups {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.fetch-group {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.fetch-group-label {
  display: flex;
  align-items: center;
  gap: 10px;
}

.fetch-group-badge {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 6px;
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.3px;
}

.fetch-group-badge.review {
  background: #eff6ff;
  color: #2563eb;
  border: 1px solid #bfdbfe;
}

.fetch-group-badge.auto {
  background: #f0fdf4;
  color: #16a34a;
  border: 1px solid #bbf7d0;
}

.fetch-group-desc {
  font-size: 12px;
  color: var(--admin-text-secondary, #9ca3af);
}

.fetch-group-buttons {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.fetch-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 10px 18px;
  border-radius: 10px;
  border: 1px solid var(--admin-border, #e5e7eb);
  background: var(--admin-surface, #fff);
  cursor: pointer;
  transition: all 0.2s;
  font-size: 14px;
  font-weight: 500;
  color: var(--admin-text, #1f2937);
  min-width: 140px;
}

.fetch-btn:hover:not(:disabled) {
  border-color: var(--src-color, #3b82f6);
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  transform: translateY(-1px);
}

.fetch-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.fetch-btn.fetching {
  border-color: var(--src-color, #3b82f6);
  background: color-mix(in srgb, var(--src-color, #3b82f6) 5%, white);
}

.fetch-btn-icon {
  font-size: 18px;
}

.fetch-btn-label {
  flex: 1;
  text-align: left;
}

.fetch-btn-arrow {
  font-size: 14px;
  color: var(--admin-text-secondary, #9ca3af);
  font-weight: 600;
}

.fetch-btn-auto-badge {
  display: inline-block;
  padding: 1px 6px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 700;
  background: #dcfce7;
  color: #16a34a;
  letter-spacing: 0.5px;
}

.fetch-btn-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(0,0,0,0.1);
  border-top-color: var(--src-color, #3b82f6);
  border-radius: 50%;
  animation: fetch-spin 0.6s linear infinite;
}

@keyframes fetch-spin {
  to { transform: rotate(360deg); }
}

/* ═══════════════════════════════════════════
   EVENT SECTION
═══════════════════════════════════════════ */
.event-section {
  background: var(--admin-surface, #fff);
  border: 1px solid var(--admin-border, #e5e7eb);
  border-radius: 8px;
  padding: 20px;
}

.event-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.event-section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--admin-text, #1f2937);
}

.event-review-tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 16px;
  border-bottom: 1px solid var(--admin-border, #e5e7eb);
  padding-bottom: 0;
}

.event-review-tab {
  padding: 8px 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--admin-text-secondary, #6b7280);
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: all 0.15s;
}

.event-review-tab:hover {
  color: var(--admin-primary, #3b82f6);
}

.event-review-tab.active {
  color: var(--admin-primary, #3b82f6);
  border-bottom-color: var(--admin-primary, #3b82f6);
  font-weight: 600;
}

.event-loading,
.event-empty {
  text-align: center;
  padding: 24px;
  color: var(--admin-text-secondary, #6b7280);
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.event-loading-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(0,0,0,0.1);
  border-top-color: var(--admin-brand, #2563eb);
  border-radius: 50%;
  animation: fetch-spin 0.6s linear infinite;
}

.event-empty-icon {
  font-size: 18px;
}

.event-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.event-card {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  background: var(--admin-surface-secondary, #f9fafb);
  border: 1px solid var(--admin-border, #e5e7eb);
  border-radius: 8px;
  padding: 16px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.event-card:hover {
  border-color: var(--admin-primary, #3b82f6);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.08);
}

.event-card-body {
  flex: 1;
  min-width: 0;
}

.event-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--admin-text, #1f2937);
  margin-bottom: 6px;
  line-height: 1.4;
}

.event-summary {
  font-size: 13px;
  color: var(--admin-text-secondary, #6b7280);
  margin-bottom: 8px;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.event-meta {
  display: flex;
  align-items: center;
  gap: 8px;
}

.event-card-actions {
  display: flex;
  flex-direction: column;
  gap: 6px;
  flex-shrink: 0;
}

.event-pagination {
  display: flex;
  justify-content: center;
  padding-top: 12px;
}

/* ═══════════════════════════════════════════
   RECENT AUTO-PUBLISHED SECTION
═══════════════════════════════════════════ */
.recent-section {
  background: var(--admin-surface, #fff);
  border: 1px solid var(--admin-border, #e5e7eb);
  border-radius: 8px;
  padding: 20px;
}

.recent-section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.recent-section-header h3 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--admin-text, #1f2937);
}

.recent-loading,
.recent-empty {
  text-align: center;
  padding: 24px;
  color: var(--admin-text-secondary, #6b7280);
  font-size: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.recent-list {
  display: flex;
  flex-direction: column;
  gap: 1px;
  background: var(--admin-border, #e5e7eb);
  border-radius: 8px;
  overflow: hidden;
}

.recent-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: var(--admin-surface, #fff);
  transition: background 0.15s;
}

.recent-item:hover {
  background: var(--admin-surface-secondary, #f9fafb);
}

.recent-item-main {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.recent-item-title {
  font-size: 13px;
  font-weight: 500;
  color: var(--admin-text, #1f2937);
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.recent-item-time {
  font-size: 12px;
  color: var(--admin-text-secondary, #9ca3af);
  white-space: nowrap;
  flex-shrink: 0;
}

.recent-item-source {
  flex-shrink: 0;
}

/* ═══════════════════════════════════════════
   SHARED BUTTONS
═══════════════════════════════════════════ */
.cd-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 600;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.12s;
  white-space: nowrap;
  line-height: 1;
}

.cd-btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.cd-btn-primary {
  background: var(--admin-brand, #2563EB);
  color: #fff;
  border: 0;
}

.cd-btn-primary:hover:not(:disabled) {
  background: #1d4ed8;
}

.cd-btn-outline-danger {
  background: transparent;
  color: #dc2626;
  border-color: #fca5a5;
}

.cd-btn-outline-danger:hover:not(:disabled) {
  background: #fef2f2;
}

/* ═══════════════════════════════════════════
   EVENT DETAIL PANEL
═══════════════════════════════════════════ */
.event-card-wrapper {
  display: flex;
  flex-direction: column;
}

.event-card {
  cursor: pointer;
}

.event-card--expanded {
  border-color: var(--admin-primary, #3b82f6);
  box-shadow: 0 2px 8px rgba(59, 130, 246, 0.12);
}

.event-expand-hint {
  font-size: 12px;
  color: var(--admin-text-secondary, #9ca3af);
  margin-left: auto;
  transition: color 0.2s;
}

.event-card:hover .event-expand-hint {
  color: var(--admin-primary, #3b82f6);
}

.slide-down-enter-active,
.slide-down-leave-active {
  transition: all 0.3s ease;
  overflow: hidden;
}

.slide-down-enter-from,
.slide-down-leave-to {
  opacity: 0;
  max-height: 0;
  transform: translateY(-8px);
}

.slide-down-enter-to,
.slide-down-leave-from {
  opacity: 1;
  max-height: 800px;
}

.event-detail-panel {
  background: var(--admin-surface, #fff);
  border: 1px solid var(--admin-primary, #3b82f6);
  border-top: none;
  border-radius: 0 0 8px 8px;
  margin-top: -1px;
  overflow: hidden;
}

.event-detail-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 24px;
  color: var(--admin-text-secondary, #6b7280);
  font-size: 14px;
}

.event-detail-content {
  display: flex;
  gap: 0;
  min-height: 200px;
}

.event-detail-sidebar {
  width: 220px;
  flex-shrink: 0;
  padding: 16px;
  background: var(--admin-surface-secondary, #f9fafb);
  border-right: 1px solid var(--admin-border, #e5e7eb);
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.event-detail-sidebar-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--admin-text, #1f2937);
  line-height: 1.4;
}

.event-detail-sidebar-summary {
  font-size: 13px;
  color: var(--admin-text-secondary, #6b7280);
  line-height: 1.5;
}

.event-detail-sidebar-status {
  display: flex;
  align-items: center;
}

.event-detail-sidebar-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  margin-top: 4px;
}

.event-detail-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.event-detail-tabs {
  display: flex;
  border-bottom: 1px solid var(--admin-border, #e5e7eb);
  background: var(--admin-surface, #fff);
  padding: 0 16px;
}

.event-detail-tab {
  padding: 10px 16px;
  font-size: 13px;
  font-weight: 500;
  color: var(--admin-text-secondary, #6b7280);
  background: none;
  border: none;
  border-bottom: 2px solid transparent;
  cursor: pointer;
  transition: all 0.15s;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.event-detail-tab:hover {
  color: var(--admin-primary, #3b82f6);
}

.event-detail-tab.active {
  color: var(--admin-primary, #3b82f6);
  border-bottom-color: var(--admin-primary, #3b82f6);
}

.tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: var(--admin-primary, #3b82f6);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
}

.event-detail-body,
.event-detail-sources {
  flex: 1;
  padding: 16px;
  overflow-y: auto;
  max-height: 400px;
}

.event-detail-body-text pre {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  font-size: 13px;
  line-height: 1.7;
  color: var(--admin-text, #1f2937);
  white-space: pre-wrap;
  word-wrap: break-word;
  margin: 0;
}

.event-detail-empty {
  text-align: center;
  padding: 32px;
  color: var(--admin-text-secondary, #9ca3af);
  font-size: 14px;
}

.source-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.source-item {
  padding: 12px 14px;
  background: var(--admin-surface-secondary, #f9fafb);
  border: 1px solid var(--admin-border, #e5e7eb);
  border-radius: 6px;
  transition: border-color 0.15s;
}

.source-item:hover {
  border-color: var(--admin-primary, #3b82f6);
}

.source-item-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.source-item-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--admin-primary, #2563eb);
  text-decoration: none;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.source-item-title:hover {
  text-decoration: underline;
}

span.source-item-title {
  color: var(--admin-text, #1f2937);
  cursor: default;
}

.source-item-type {
  flex-shrink: 0;
}

.source-item-summary {
  font-size: 12px;
  color: var(--admin-text-secondary, #6b7280);
  line-height: 1.5;
}
</style>
