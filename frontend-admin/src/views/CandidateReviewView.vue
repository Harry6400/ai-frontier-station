<script setup>
import { onMounted, ref, computed } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import {
  getCandidates,
  getCandidateDetail,
  updateCandidate,
  approveCandidate,
  rejectCandidate,
  triggerFetch,
  aiProcessCandidate
} from '../api/admin'

/* ── source type meta (no arena) ── */
const SOURCE_TYPE_META = {
  github: { label: 'GitHub', color: '#6e40c9' },
  news: { label: 'AI资讯', color: '#2563eb' },
  product: { label: '产品动态', color: '#059669' },
  tools: { label: '工具实践', color: '#e11d48' }
}

const STATUS_META = {
  pending: { label: '待审核', type: 'warning' },
  approved: { label: '已批准', type: 'success' },
  rejected: { label: '已拒绝', type: 'danger' },
  imported: { label: '已导入', type: 'info' }
}

// No '全部' — just the actual source types
const sourceTypeOptions = Object.entries(SOURCE_TYPE_META).map(([value, meta]) => ({ value, label: meta.label }))

// No '全部' — just actual statuses
const statusOptions = Object.entries(STATUS_META).map(([value, meta]) => ({ value, label: meta.label }))

/* ── state ── */
const activeSourceType = ref('news')
const activeStatus = ref('pending')
const candidateList = ref([])
const loading = ref(false)
const total = ref(0)
const page = ref(1)
const pageSize = ref(20)

const selectedCandidate = ref(null)
const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const rawContentExpanded = ref(false)
const detailTab = ref('ai') // 'prompt' | 'ai' | 'raw'
const detailForm = ref({ raw_content: '', ai_summary: '', ai_body: '' })
const saving = ref(false)
const aiProcessing = ref(false)

const fetchDialogVisible = ref(false)
const fetchSourceType = ref('github')
const fetching = ref(false)

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

function savePrompt() {
  localStorage.setItem('ai_review_prompt', customPrompt.value)
  promptDialogVisible.value = false
  ElMessage.success('提示词已保存')
}

// Load saved prompt on init
const savedPrompt = localStorage.getItem('ai_review_prompt')
if (savedPrompt) customPrompt.value = savedPrompt

/* ── data loading ── */
async function loadCandidates() {
  loading.value = true
  try {
    const params = { pageNum: page.value, pageSize: pageSize.value }
    if (activeSourceType.value) params.sourceType = activeSourceType.value
    if (activeStatus.value) params.status = activeStatus.value
    const res = await getCandidates(params)
    candidateList.value = res.data?.records || res.data?.items || res.data?.list || res.data || []
    total.value = res.data?.total || candidateList.value.length
  } catch (err) {
    ElMessage.error('加载候选列表失败：' + (err.response?.data?.message || err.message))
  } finally {
    loading.value = false
  }
}

function handleSourceTypeChange(val) {
  activeSourceType.value = val
  page.value = 1
  loadCandidates()
}

function handleStatusChange(val) {
  activeStatus.value = val
  page.value = 1
  loadCandidates()
}

function handlePageChange(p) {
  page.value = p
  loadCandidates()
}

/* ── detail (opens dialog) ── */
async function selectCandidate(row) {
  detailLoading.value = true
  detailDialogVisible.value = true
  rawContentExpanded.value = false
  try {
    const res = await getCandidateDetail(row.id)
    const data = res.data || row
    selectedCandidate.value = data
  } catch {
    selectedCandidate.value = row
  } finally {
    detailLoading.value = false
  }
}

function closeDetailDialog() {
  detailDialogVisible.value = false
  selectedCandidate.value = null
}

/* ── actions ── */
async function handleSave() {
  if (!selectedCandidate.value) return
  saving.value = true
  try {
    await updateCandidate(selectedCandidate.value.id, {
      aiSummary: selectedCandidate.value.aiSummary,
      aiBody: selectedCandidate.value.aiBody,
      rawContent: selectedCandidate.value.rawContent
    })
    ElMessage.success('保存成功')
    loadCandidates()
  } catch (err) {
    ElMessage.error('保存失败：' + (err.response?.data?.message || err.message))
  } finally {
    saving.value = false
  }
}

async function handleApprove() {
  if (!selectedCandidate.value) return
  try {
    await ElMessageBox.confirm('确认批准该候选内容？', '批准确认', { type: 'info' })
    await approveCandidate(selectedCandidate.value.id)
    ElMessage.success('已批准')
    closeDetailDialog()
    loadCandidates()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('批准失败：' + (err.response?.data?.message || err.message))
    }
  }
}

async function handleReject() {
  if (!selectedCandidate.value) return
  try {
    await ElMessageBox.confirm('确认拒绝该候选内容？', '拒绝确认', { type: 'warning' })
    await rejectCandidate(selectedCandidate.value.id)
    ElMessage.success('已拒绝')
    closeDetailDialog()
    loadCandidates()
  } catch (err) {
    if (err !== 'cancel') {
      ElMessage.error('拒绝失败：' + (err.response?.data?.message || err.message))
    }
  }
}

/* ── trigger fetch ── */
function openFetchDialog() {
  fetchDialogVisible.value = true
}

async function handleTriggerFetch() {
  fetching.value = true
  try {
    await triggerFetch(fetchSourceType.value)
    ElMessage.success(`已触发 ${SOURCE_TYPE_META[fetchSourceType.value]?.label || fetchSourceType.value} 数据采集`)
    fetchDialogVisible.value = false
    setTimeout(() => loadCandidates(), 2000)
  } catch (err) {
    ElMessage.error('触发采集失败：' + (err.response?.data?.message || err.message))
  } finally {
    fetching.value = false
  }
}

/* ── AI process ── */
async function handleAiProcess() {
  if (!selectedCandidate.value) return
  aiProcessing.value = true
  try {
    const savedPrompt = localStorage.getItem('ai_review_prompt') || ''
    const res = await aiProcessCandidate(selectedCandidate.value.id, { prompt: savedPrompt })
    const data = res.data || {}
    if (selectedCandidate.value) {
      Object.assign(selectedCandidate.value, data)
    }
    ElMessage.success('AI 处理完成')
    loadCandidates()
  } catch (err) {
    ElMessage.error('AI 处理失败：' + (err.response?.data?.message || err.message))
  } finally {
    aiProcessing.value = false
  }
}

const parsedAiMeta = computed(() => {
  if (!selectedCandidate.value) return null
  const raw = selectedCandidate.value.metadataJson || selectedCandidate.value.metadata_json
  if (!raw) return null
  try {
    return typeof raw === 'string' ? JSON.parse(raw) : raw
  } catch { return null }
})

function formatDate(val) {
  if (!val) return '—'
  return new Date(val).toLocaleString('zh-CN', { month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' })
}

function truncateText(text, len = 60) {
  if (!text) return ''
  return text.length > len ? text.slice(0, len) + '…' : text
}

/* ── lifecycle ── */
onMounted(() => {
  loadCandidates()
})
</script>

<template>
  <section class="candidate-page">
    <!-- top toolbar -->
    <div class="candidate-toolbar">
      <!-- source type tabs -->
      <div class="type-tab-strip">
        <button
          v-for="opt in sourceTypeOptions"
          :key="opt.value"
          class="type-tab"
          :class="{ active: activeSourceType === opt.value }"
          @click="handleSourceTypeChange(opt.value)"
        >
          {{ opt.label }}
        </button>
      </div>

      <div class="toolbar-right">
        <!-- status filter -->
        <div class="status-strip">
          <button
            v-for="opt in statusOptions"
            :key="opt.value"
            class="status-tab"
            :class="{ active: activeStatus === opt.value }"
            @click="handleStatusChange(opt.value)"
          >
            {{ opt.label }}
          </button>
        </div>

        <!-- trigger fetch -->
        <el-button type="primary" @click="openFetchDialog">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-right:4px;">
            <polyline points="23 4 23 10 17 10"/><polyline points="1 20 1 14 7 14"/>
            <path d="M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"/>
          </svg>
          触发采集
        </el-button>
      </div>
    </div>

    <!-- candidate table -->
    <div class="candidate-table-wrap">
      <el-table
        :data="candidateList"
        v-show="!loading"
        stripe
        highlight-current-row
        style="width: 100%"
        @row-click="selectCandidate"
        row-key="id"
      >
        <el-table-column prop="title" label="标题" min-width="280" show-overflow-tooltip>
          <template #default="{ row }">
            <div class="cell-title">
              <el-tag
                size="small"
                :color="SOURCE_TYPE_META[row.source_type]?.color"
                style="color:#fff;border:0;margin-right:6px;"
                disable-transitions
              >
                {{ SOURCE_TYPE_META[row.source_type]?.label || row.source_type }}
              </el-tag>
              <span>{{ truncateText(row.title, 50) }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="status" label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="STATUS_META[row.status]?.type || 'info'" size="small" disable-transitions>
              {{ STATUS_META[row.status]?.label || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="author" label="作者" width="120" show-overflow-tooltip />
        <el-table-column prop="publishedAt" label="发布时间" width="130" align="center">
          <template #default="{ row }">
            {{ formatDate(row.publishedAt) }}
          </template>
        </el-table-column>
      </el-table>

      <!-- pagination -->
      <div v-if="total > pageSize" class="candidate-pagination">
        <el-pagination
          background
          layout="prev, pager, next"
          :total="total"
          :page-size="pageSize"
          :current-page="page"
          @current-change="handlePageChange"
        />
      </div>
    </div>

    <!-- detail dialog (modal) -->
    <el-dialog
      v-model="detailDialogVisible"
      width="860px"
      append-to-body
      destroy-on-close
      :show-close="false"
      class="cd-dialog"
      @close="selectedCandidate = null"
    >
      <div class="cd-body">
        <template v-if="selectedCandidate">
          <!-- header -->
          <div class="cd-header">
            <div class="cd-header-row">
              <div class="cd-header-left">
                <span
                  class="cd-source-badge"
                  :style="{ background: SOURCE_TYPE_META[selectedCandidate.sourceType]?.color || '#666' }"
                >{{ SOURCE_TYPE_META[selectedCandidate.sourceType]?.label || selectedCandidate.sourceType }}</span>
                <h2 class="cd-title">{{ selectedCandidate.title }}</h2>
              </div>
              <button class="cd-close" @click="closeDetailDialog" aria-label="close">&times;</button>
            </div>
            <div class="cd-meta-line">
              <span v-if="selectedCandidate.author">{{ selectedCandidate.author }}</span>
              <span class="cd-dot" v-if="selectedCandidate.author && selectedCandidate.publishedAt">&middot;</span>
              <span v-if="selectedCandidate.publishedAt">{{ formatDate(selectedCandidate.publishedAt) }}</span>
              <template v-if="selectedCandidate.url">
                <span class="cd-dot">&middot;</span>
                <a :href="selectedCandidate.url" target="_blank" rel="noreferrer" class="cd-link">原始链接 &#x2197;</a>
              </template>
            </div>
          </div>

          <!-- sidebar + content layout -->
          <div class="cd-layout">
            <nav class="cd-sidebar">
              <button :class="['cd-nav-item', {active: detailTab==='ai'}]" @click="detailTab='ai'">AI 内容</button>
              <button :class="['cd-nav-item', {active: detailTab==='prompt'}]" @click="detailTab='prompt'">提示词</button>
              <button :class="['cd-nav-item', {active: detailTab==='raw'}]" @click="detailTab='raw'">原始内容</button>
            </nav>
            <div class="cd-content">
              <!-- AI content tab -->
              <div v-show="detailTab==='ai'" class="cd-tab-panel">
                <div class="cd-field">
                  <label>AI 摘要</label>
                  <el-input v-model="selectedCandidate.aiSummary" type="textarea" :rows="3" placeholder="点击 AI 处理生成摘要..." />
                </div>
                <div class="cd-field">
                  <label>AI 正文</label>
                  <el-input v-model="selectedCandidate.aiBody" type="textarea" :rows="12" placeholder="点击 AI 处理生成正文..." />
                </div>
              </div>
              <!-- prompt tab -->
              <div v-show="detailTab==='prompt'" class="cd-tab-panel">
                <div class="cd-field">
                  <label>AI 处理提示词</label>
                  <el-input v-model="customPrompt" type="textarea" :rows="18" placeholder="自定义提示词..." />
                </div>
              </div>
              <!-- raw content tab -->
              <div v-show="detailTab==='raw'" class="cd-tab-panel">
                <div class="cd-field">
                  <label>原始抓取内容</label>
                  <el-input v-model="selectedCandidate.rawContent" type="textarea" :rows="18" placeholder="原始内容..." />
                </div>
              </div>
            </div>
          </div>
        </template>
      </div>

      <template #footer>
        <div class="cd-footer">
          <button class="cd-btn cd-btn-accent" :disabled="aiProcessing" @click="handleAiProcess">
            <span v-if="aiProcessing" class="cd-spinner"></span>
            AI 处理
          </button>
          <button class="cd-btn cd-btn-ghost" :disabled="saving" @click="handleSave">保存</button>
          <button class="cd-btn cd-btn-outline-danger" @click="handleReject">拒绝</button>
          <button class="cd-btn cd-btn-primary" @click="handleApprove">批准</button>
        </div>
      </template>
    </el-dialog>

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

    <!-- trigger fetch dialog -->
    <el-dialog v-model="fetchDialogVisible" title="触发数据采集" width="400px" append-to-body>
      <div class="fetch-dialog-body">
        <p style="margin-bottom:12px;color:var(--admin-text-secondary,#666);font-size:13px;">
          选择一个来源类型，系统将自动从该来源抓取候选内容。
        </p>
        <el-select v-model="fetchSourceType" style="width:100%;">
          <el-option
            v-for="[key, meta] in Object.entries(SOURCE_TYPE_META)"
            :key="key"
            :label="meta.label"
            :value="key"
          />
        </el-select>
      </div>
      <template #footer>
        <el-button @click="fetchDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="fetching" @click="handleTriggerFetch">开始采集</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.candidate-page {
  padding: 0;
}

/* toolbar */
.candidate-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  flex-wrap: wrap;
  gap: 12px;
  margin-bottom: 16px;
}

.toolbar-right {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

/* source type tabs */
.type-tab-strip {
  display: flex; gap: 2px; background: var(--admin-surface-muted, #f5f5f5);
  border-radius: 10px; padding: 3px;
}
.type-tab {
  padding: 7px 16px; border-radius: 8px; border: 0; background: transparent;
  font-size: 13px; font-weight: 600; color: var(--admin-text-secondary, #666);
  cursor: pointer; transition: all 0.15s; white-space: nowrap;
}
.type-tab:hover { color: var(--admin-text, #333); }
.type-tab.active {
  background: var(--admin-surface-elevated, #fff);
  color: var(--admin-text, #333);
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

/* status tabs */
.status-strip {
  display: flex; gap: 4px;
}
.status-tab {
  padding: 5px 12px; border-radius: 6px;
  border: 1px solid var(--admin-line-soft, #e0e0e0);
  background: transparent; font-size: 12px; font-weight: 600;
  color: var(--admin-text-secondary, #666); cursor: pointer; transition: all 0.12s;
}
.status-tab:hover { border-color: var(--admin-brand, #2563EB); color: var(--admin-brand, #2563EB); }
.status-tab.active {
  background: var(--admin-brand, #2563EB); color: #fff;
  border-color: var(--admin-brand, #2563EB);
}

/* table */
.candidate-table-wrap {
  background: var(--admin-surface-elevated, #fff);
  border-radius: 10px;
  border: 1px solid var(--admin-line-soft, #e8e8e8);
  overflow: hidden;
}

.cell-title {
  display: flex;
  align-items: center;
  font-size: 13px;
  font-weight: 500;
}

.candidate-pagination {
  display: flex;
  justify-content: center;
  padding: 12px 0;
}

/* ── Claude Design Dialog ── */

.cd-dialog :deep(.el-dialog__header) {
  display: none;
}
.cd-dialog :deep(.el-dialog__body) {
  padding: 0;
}
.cd-dialog :deep(.el-dialog__footer) {
  padding: 0;
}
.cd-dialog :deep(.el-dialog) {
  border-radius: 14px;
  overflow: hidden;
  border: 1px solid var(--admin-line-soft, #e5e5e5);
  box-shadow: 0 24px 48px rgba(0,0,0,0.12);
}

/* header */
.cd-header {
  padding: 24px 28px 16px;
  border-bottom: 1px solid var(--admin-line-soft, #f0f0f0);
}
.cd-header-row {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
.cd-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}
.cd-source-badge {
  flex-shrink: 0;
  padding: 3px 10px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 0.3px;
  text-transform: uppercase;
}
.cd-title {
  margin: 0;
  font-size: 17px;
  font-weight: 700;
  color: var(--admin-text, #1a1a1a);
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.cd-close {
  flex-shrink: 0;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  border: 0;
  background: transparent;
  color: var(--admin-text-secondary, #999);
  font-size: 20px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.12s;
  margin-top: -2px;
}
.cd-close:hover {
  background: var(--admin-surface-muted, #f5f5f5);
  color: var(--admin-text, #333);
}
.cd-meta-line {
  margin-top: 8px;
  font-size: 12.5px;
  color: var(--admin-text-secondary, #999);
  display: flex;
  align-items: center;
  gap: 0;
  flex-wrap: wrap;
}
.cd-dot {
  margin: 0 6px;
  color: var(--admin-line-soft, #ccc);
}
.cd-link {
  color: var(--admin-brand, #2563EB);
  text-decoration: none;
  font-weight: 500;
}
.cd-link:hover {
  text-decoration: underline;
}

/* body */
.cd-body {
  padding: 20px 28px;
  display: flex;
  flex-direction: column;
  gap: 18px;
  max-height: 65vh;
  overflow-y: auto;
}

/* sections */
.cd-section {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.cd-section-label {
  font-size: 11.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.6px;
  color: var(--admin-text-secondary, #999);
}

/* textarea styling */
.cd-textarea :deep(.el-textarea__inner) {
  background: var(--admin-surface-muted, #fafafa);
  border: 1px solid var(--admin-line-soft, #e8e8e8);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13.5px;
  line-height: 1.6;
  color: var(--admin-text, #1a1a1a);
  resize: vertical;
  transition: border-color 0.15s, box-shadow 0.15s;
  font-family: inherit;
}
.cd-textarea :deep(.el-textarea__inner:focus) {
  border-color: var(--admin-brand, #2563EB);
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.1);
}
.cd-textarea-body :deep(.el-textarea__inner) {
  min-height: 200px;
}

/* collapse toggle */
.cd-collapse-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 0;
  border: 0;
  background: transparent;
  font-size: 12.5px;
  font-weight: 600;
  color: var(--admin-text-secondary, #888);
  cursor: pointer;
  transition: color 0.12s;
}
.cd-collapse-btn:hover {
  color: var(--admin-text, #333);
}
.cd-collapse-icon {
  font-size: 11px;
  transition: transform 0.15s;
}

/* analysis card */
.cd-analysis {
  background: var(--admin-surface-muted, #f9f9fb);
  border-radius: 10px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.cd-tags-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.cd-tag-chip {
  display: inline-block;
  padding: 3px 10px;
  border-radius: 100px;
  font-size: 12px;
  font-weight: 600;
  background: rgba(37, 99, 235, 0.08);
  color: var(--admin-brand, #2563EB);
  border: 1px solid rgba(37, 99, 235, 0.15);
}
.cd-badges-row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.cd-badge {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  padding: 4px 12px;
  border-radius: 7px;
  font-size: 12.5px;
  font-weight: 500;
  background: var(--admin-surface-elevated, #fff);
  color: var(--admin-text, #333);
  border: 1px solid var(--admin-line-soft, #e5e5e5);
}
.cd-badge-key {
  font-size: 10.5px;
  font-weight: 700;
  text-transform: uppercase;
  letter-spacing: 0.4px;
  color: var(--admin-text-secondary, #999);
}

/* footer */
.cd-footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  padding: 14px 28px;
  border-top: 1px solid var(--admin-line-soft, #f0f0f0);
  background: var(--admin-surface-muted, #fafafa);
}

/* buttons */
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
.cd-btn-ghost {
  background: transparent;
  color: var(--admin-text-secondary, #666);
  border-color: var(--admin-line-soft, #e0e0e0);
}
.cd-btn-ghost:hover:not(:disabled) {
  background: var(--admin-surface-muted, #f5f5f5);
  color: var(--admin-text, #333);
}
.cd-btn-accent {
  background: linear-gradient(135deg, #6366f1, #8b5cf6);
  color: #fff;
  border: 0;
  box-shadow: 0 1px 3px rgba(99,102,241,0.3);
}
.cd-btn-accent:hover:not(:disabled) {
  box-shadow: 0 2px 8px rgba(99,102,241,0.4);
  transform: translateY(-0.5px);
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

/* spinner */
.cd-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: cd-spin 0.6s linear infinite;
}
.cd-btn-ghost .cd-spinner {
  border-color: rgba(0,0,0,0.15);
  border-top-color: var(--admin-text-secondary, #666);
}
@keyframes cd-spin {
  to { transform: rotate(360deg); }
}

/* collapse transition */
.cd-collapse-enter-active { transition: all 0.2s ease-out; }
.cd-collapse-leave-active { transition: all 0.15s ease-in; }
.cd-collapse-enter-from { opacity: 0; max-height: 0; }
.cd-collapse-leave-to { opacity: 0; max-height: 0; }

/* fetch dialog */
.fetch-dialog-body {
  padding: 8px 0;
}

/* ── detail dialog sidebar layout ── */
.cd-dialog :deep(.el-dialog) {
  border-radius: 16px;
  overflow: hidden;
}
.cd-dialog :deep(.el-dialog__header) { display: none; }
.cd-dialog :deep(.el-dialog__footer) { padding: 0; }

.cd-body {
  min-height: 400px;
}

.cd-header {
  padding: 20px 24px 12px;
  border-bottom: 1px solid var(--el-border-color-lighter);
}
.cd-header-row {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}
.cd-header-left {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}
.cd-source-badge {
  flex-shrink: 0;
  font-size: 11px;
  color: #fff;
  padding: 2px 8px;
  border-radius: 4px;
  font-weight: 600;
}
.cd-title {
  font-size: 16px;
  font-weight: 600;
  margin: 0;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.cd-close {
  background: none;
  border: none;
  font-size: 20px;
  cursor: pointer;
  color: var(--el-text-color-secondary);
  padding: 4px 8px;
  border-radius: 6px;
  line-height: 1;
}
.cd-close:hover { background: var(--el-fill-color-light); }

.cd-meta-line {
  margin-top: 6px;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}
.cd-dot { opacity: 0.4; }
.cd-link { color: var(--el-color-primary); text-decoration: none; }
.cd-link:hover { text-decoration: underline; }

/* sidebar + content layout */
.cd-layout {
  display: flex;
  min-height: 420px;
}
.cd-sidebar {
  width: 120px;
  flex-shrink: 0;
  border-right: 1px solid var(--el-border-color-lighter);
  padding: 12px 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.cd-nav-item {
  display: block;
  width: 100%;
  padding: 10px 16px;
  border: none;
  background: transparent;
  font-size: 13px;
  color: var(--el-text-color-regular);
  cursor: pointer;
  text-align: left;
  transition: all 0.15s;
  border-left: 3px solid transparent;
}
.cd-nav-item:hover {
  background: var(--el-fill-color-lighter);
  color: var(--el-text-color-primary);
}
.cd-nav-item.active {
  color: var(--el-color-primary);
  background: var(--el-fill-color-light);
  border-left-color: var(--el-color-primary);
  font-weight: 600;
}

.cd-content {
  flex: 1;
  padding: 16px 20px;
  overflow-y: auto;
  max-height: 420px;
}
.cd-tab-panel { display: flex; flex-direction: column; gap: 16px; }

.cd-field label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  color: var(--el-text-color-secondary);
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}
.cd-textarea {
  width: 100%;
  box-sizing: border-box;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  line-height: 1.6;
  resize: vertical;
  background: var(--el-fill-color-blank);
  color: var(--el-text-color-primary);
  font-family: inherit;
  transition: border-color 0.2s;
}
.cd-textarea:focus {
  outline: none;
  border-color: var(--el-color-primary);
  box-shadow: 0 0 0 2px var(--el-color-primary-light-9);
}

/* footer */
.cd-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  padding: 12px 24px;
  border-top: 1px solid var(--el-border-color-lighter);
}
.cd-btn {
  padding: 8px 18px;
  border-radius: 8px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  border: 1px solid transparent;
  transition: all 0.15s;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}
.cd-btn-primary {
  background: var(--el-color-primary);
  color: #fff;
  border-color: var(--el-color-primary);
}
.cd-btn-primary:hover { opacity: 0.9; }
.cd-btn-accent {
  background: linear-gradient(135deg, var(--el-color-primary), var(--el-color-success));
  color: #fff;
}
.cd-btn-accent:hover { opacity: 0.9; }
.cd-btn-accent:disabled { opacity: 0.5; cursor: not-allowed; }
.cd-btn-ghost {
  background: transparent;
  color: var(--el-text-color-regular);
  border-color: var(--el-border-color);
}
.cd-btn-ghost:hover { background: var(--el-fill-color-lighter); }
.cd-btn-outline-danger {
  background: transparent;
  color: var(--el-color-danger);
  border-color: var(--el-color-danger);
}
.cd-btn-outline-danger:hover { background: var(--el-color-danger-light-9); }

.cd-spinner {
  display: inline-block;
  width: 14px;
  height: 14px;
  border: 2px solid rgba(255,255,255,0.3);
  border-top-color: #fff;
  border-radius: 50%;
  animation: cd-spin 0.6s linear infinite;
}
@keyframes cd-spin { to { transform: rotate(360deg); } }
</style>
