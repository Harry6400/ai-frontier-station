<script setup>
import { ref, computed, watch, nextTick } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  editingId: { type: [Number, String, null], default: null },
  form: { type: Object, required: true },
  contentOptions: { type: Object, default: () => ({ categories: [], sources: [], contentTypes: [], publishStatuses: [] }) },
  externalRefs: { type: Array, default: () => [] },
  rules: { type: Object, default: () => ({}) },
  externalRefForm: { type: Object, required: true },
  externalRefRules: { type: Object, default: () => ({}) },
  externalRefTypeOptions: { type: Array, default: () => [] },
  editingExternalRefId: { type: [Number, String, null], default: null },
  externalRefDialogVisible: { type: Boolean, default: false }
})

const emit = defineEmits([
  'update:visible',
  'update:externalRefDialogVisible',
  'save',
  'dialog-closed',
  'submit-external-ref',
  'open-create-external-ref',
  'open-edit-external-ref',
  'delete-external-ref',
  'apply-extension-template'
])

const formRef = ref(null)
const externalRefFormRef = ref(null)
const editorTab = ref('basic')
const previewMode = ref('card')

const previewModes = [
  { label: '门户卡片', value: 'card' },
  { label: '详情头图', value: 'detail' },
  { label: '正文排版', value: 'article' }
]

function getExternalRefMeta(type) {
  const EXTERNAL_REF_META = {
    github_repo: { label: 'GitHub 仓库', note: '用于追踪外部仓库记录，后续可同步 Star、语言和 README。' },
    arxiv_paper: { label: 'arXiv 论文', note: '用于追踪论文记录，后续可同步作者、摘要和版本。' },
    official_post: { label: '官方公告', note: '用于追踪公司官方博客或公告原文。' },
    community_practice: { label: '社区实践', note: '用于追踪技术社区中的工程经验文章。' },
    leaderboard_item: { label: '结构化榜单', note: '用于追踪 Arena、HELM、MTEB、SWE-bench 等榜单条目。' },
    manual_source: { label: '人工来源', note: '用于记录人工录入并经过 AI 总结的来源。' }
  }
  return EXTERNAL_REF_META[type] || { label: type || '外部引用', note: '外部系统记录，可用于后续同步和追踪。' }
}

function getContentTypeMeta(type) {
  const CONTENT_TYPE_META = {
    news: { label: 'AI资讯', english: 'News Pulse' },
    paper: { label: '论文速递', english: 'Research Digest' },
    project: { label: '热门项目', english: 'Project Watch' },
    company_update: { label: '公司动态', english: 'Company Signal' },
    practice: { label: '技术实践', english: 'Practice Note' }
  }
  return CONTENT_TYPE_META[type] || { label: type || '内容', english: 'Content' }
}

function escapeHtml(raw) {
  return raw
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#39;')
}

function renderInlineMarkdown(raw) {
  return escapeHtml(raw)
    .replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>')
    .replace(/\*(.+?)\*/g, '<em>$1</em>')
    .replace(/`([^`]+)`/g, '<code>$1</code>')
}

function renderMarkdownPreview(raw) {
  const input = raw.trim()
  if (!input) {
    return '<p>正文输入后，这里会以更接近前台阅读区的方式显示排版预览。</p>'
  }
  const codeFenceToken = []
  let normalized = input.replace(/```([\s\S]*?)```/g, (_, code) => {
    const token = `__CODE_BLOCK_${codeFenceToken.length}__`
    codeFenceToken.push(`<pre><code>${escapeHtml(code.trim())}</code></pre>`)
    return token
  })
  const blocks = normalized.split(/\n\s*\n/).filter(Boolean)
  return blocks
    .map((block) => {
      const trimmed = block.trim()
      if (trimmed.startsWith('__CODE_BLOCK_')) {
        const index = Number(trimmed.replace(/[^0-9]/g, ''))
        return codeFenceToken[index] || ''
      }
      if (/^###\s+/.test(trimmed)) {
        return `<h3>${renderInlineMarkdown(trimmed.replace(/^###\s+/, ''))}</h3>`
      }
      if (/^##\s+/.test(trimmed)) {
        return `<h2>${renderInlineMarkdown(trimmed.replace(/^##\s+/, ''))}</h2>`
      }
      if (/^#\s+/.test(trimmed)) {
        return `<h1>${renderInlineMarkdown(trimmed.replace(/^#\s+/, ''))}</h1>`
      }
      if (/^[-*]\s+/m.test(trimmed)) {
        const items = trimmed
          .split('\n')
          .filter((line) => /^[-*]\s+/.test(line))
          .map((line) => `<li>${renderInlineMarkdown(line.replace(/^[-*]\s+/, ''))}</li>`)
          .join('')
        return `<ul>${items}</ul>`
      }
      const paragraph = trimmed
        .split('\n')
        .map((line) => renderInlineMarkdown(line))
        .join('<br />')
      return `<p>${paragraph}</p>`
    })
    .join('')
}

function formatDateTime(value) {
  if (!value) {
    return '保存后自动生成'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(date)
}

const categoryMap = computed(() =>
  Object.fromEntries(props.contentOptions.categories.map((item) => [item.id, item.name]))
)
const sourceMap = computed(() =>
  Object.fromEntries(props.contentOptions.sources.map((item) => [item.id, item.name]))
)
const previewTypeMeta = computed(() => getContentTypeMeta(props.form.contentType))
const selectedCategoryName = computed(() => categoryMap.value[props.form.categoryId] || '未选择分类')
const selectedSource = computed(() =>
  props.contentOptions.sources.find((item) => item.id === props.form.sourceId) || null
)
const bodyStats = computed(() => {
  const trimmed = props.form.bodyMarkdown.trim()
  const lines = trimmed ? trimmed.split('\n').filter(Boolean).length : 0
  const paragraphs = trimmed ? trimmed.split(/\n\s*\n/).filter(Boolean).length : 0
  return { characters: trimmed.length, lines, paragraphs }
})
const EXTENSION_TEMPLATES = {
  project: { label: 'GitHub 项目模板', payload: { externalType: 'github_repo', githubRepo: 'owner/repo', starTrend: 'up', language: 'Python' } },
  paper: { label: '论文信息模板', payload: { externalType: 'arxiv_paper', arxivId: '2501.00000', paperVenue: 'arXiv', projectPage: 'https://example.com' } },
  company_update: { label: '公司动态模板', payload: { externalType: 'official_post', companyFocus: 'OpenAI / DeepSeek', productLine: 'model', announcementType: 'release' } },
  practice: { label: '技术实践模板', payload: { externalType: 'community_practice', practiceScope: 'rag-agent', difficulty: 'intermediate', benchmark: 'manual' } },
  news: { label: '资讯来源模板', payload: { externalType: 'news_item', topic: 'model-update', importance: 'medium' } }
}
const extensionTemplate = computed(() => EXTENSION_TEMPLATES[props.form.contentType] || EXTENSION_TEMPLATES.news)
const previewExtraEntries = computed(() => {
  if (!props.form.extraJson.trim()) {
    return []
  }
  try {
    return Object.entries(JSON.parse(props.form.extraJson))
  } catch (error) {
    return [['JSON格式', '当前扩展字段暂未通过校验']]
  }
})
const formChecklist = computed(() => [
  { label: '标题已填写', passed: !!props.form.title.trim() },
  { label: '摘要已填写', passed: !!props.form.summary.trim() },
  { label: '分类已选择', passed: !!props.form.categoryId },
  { label: '正文长度适合展示', passed: props.form.bodyMarkdown.trim().length >= 80, hint: '建议至少 80 字' },
  { label: '已发布内容建议补来源链接', passed: props.form.publishStatus !== 'PUBLISHED' || !!props.form.sourceUrl.trim(), hint: '发布内容建议补充原始来源链接' }
])
const passedChecklistCount = computed(() => formChecklist.value.filter((item) => item.passed).length)
const submitNotice = computed(() => {
  if (props.form.publishStatus === 'PUBLISHED' && !props.form.publishedAt) {
    return '当前状态为"已发布"，如果不手动设置发布时间，保存后后端会自动补当前时间。'
  }
  if (!props.form.sourceId && !props.form.sourceUrl) {
    return '当前内容未绑定来源，适合手动录入示例；后续如果接外部数据源，再补来源映射即可。'
  }
  return '右侧预览区会随着输入实时更新，适合在录入时同步检查展示效果。'
})
const submitButtonText = computed(() => (props.editingId ? '保存修改' : '创建内容'))
const previewPublishedAt = computed(() => formatDateTime(props.form.publishedAt))

function applyExtensionTemplate() {
  emit('apply-extension-template')
}

function openCreateExternalRef() {
  emit('open-create-external-ref')
}

function openEditExternalRef(item) {
  emit('open-edit-external-ref', item)
}

function handleExternalRefDelete(item) {
  emit('delete-external-ref', item)
}

function submit() {
  emit('save')
}

function submitExternalRef() {
  emit('submit-external-ref')
}

function onDialogClosed() {
  editorTab.value = 'basic'
  formRef.value?.clearValidate()
  emit('dialog-closed')
}

function onVisibleChange(val) {
  emit('update:visible', val)
}

function onExternalRefVisibleChange(val) {
  emit('update:externalRefDialogVisible', val)
}

function clearValidate() {
  nextTick(() => formRef.value?.clearValidate())
}

function clearExternalRefValidate() {
  nextTick(() => externalRefFormRef.value?.clearValidate())
}

defineExpose({ clearValidate, clearExternalRefValidate, formRef, externalRefFormRef })
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="editingId ? '编辑内容' : '新建内容'"
    width="min(90vw, 1100px)"
    class="content-dialog"
    @update:model-value="onVisibleChange"
    @closed="onDialogClosed"
  >
    <div class="editor-tabs-layout">
      <aside class="editor-tabs-side">
        <h3>{{ editingId ? '编辑' : '新建' }}</h3>
        <nav class="editor-tabs-nav">
          <a :class="{ 'is-active': editorTab === 'basic' }" @click="editorTab = 'basic'">基本信息</a>
          <a :class="{ 'is-active': editorTab === 'body' }" @click="editorTab = 'body'">正文内容</a>
          <a :class="{ 'is-active': editorTab === 'refs' }" @click="editorTab = 'refs'">外部引用</a>
        </nav>
      </aside>
      <div class="editor-tabs-main">

        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
          <div v-show="editorTab === 'basic'">
            <div class="form-grid">
              <el-form-item label="标题" prop="title">
                <el-input v-model="form.title" />
              </el-form-item>
              <el-form-item label="内容标识">
                <el-input v-model="form.slug" placeholder="可选，不填自动生成" />
              </el-form-item>
              <el-form-item label="内容类型" prop="contentType">
                <el-select v-model="form.contentType">
                  <el-option
                    v-for="item in contentOptions.contentTypes"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="分类" prop="categoryId">
                <el-select v-model="form.categoryId">
                  <el-option
                    v-for="item in contentOptions.categories"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="来源">
                <el-select v-model="form.sourceId" clearable>
                  <el-option
                    v-for="item in contentOptions.sources"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="原始链接" prop="sourceUrl">
                <el-input v-model="form.sourceUrl" />
              </el-form-item>
              <el-form-item label="作者">
                <el-input v-model="form.authorName" />
              </el-form-item>
              <el-form-item label="精选级别">
                <el-input-number v-model="form.featuredLevel" :min="0" :max="5" />
              </el-form-item>
              <el-form-item label="发布时间">
                <el-date-picker
                  v-model="form.publishedAt"
                  type="datetime"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  placeholder="可选"
                />
              </el-form-item>
            </div>

            <el-form-item label="摘要" prop="summary">
              <el-input v-model="form.summary" type="textarea" :rows="3" />
            </el-form-item>
          </div>

          <div v-show="editorTab === 'body'">
            <el-form-item label="正文" prop="bodyMarkdown">
              <el-input v-model="form.bodyMarkdown" type="textarea" :rows="18" />
            </el-form-item>
            <div style="margin-top: -8px; margin-bottom: 16px; padding: 12px 16px; background: #f4f4f5; border-radius: 6px; font-size: 12px; color: #909399; line-height: 1.6;">
              <div style="font-weight: 600; color: #606266; margin-bottom: 6px;">📋 AI 输出模板（推荐格式）</div>
              <pre style="margin: 0; white-space: pre-wrap; font-family: monospace; font-size: 12px;">## 核心贡献
一句话说明论文/产品的核心贡献。

## 方法简述
简要描述使用的方法或技术路线。

## 关键结果
- 结果1
- 结果2

## 与CT去噪/医学影像的关联度
评分(1-10) + 一句话说明。

## 推荐理由
为什么值得阅读/关注。</pre>
            </div>
          </div>
        </el-form>

        <div v-show="editorTab === 'refs'">
          <div class="refs-tab-header">
            <el-button size="small" type="primary" @click="openCreateExternalRef">
              {{ editingId ? '新增外部引用' : '请先保存内容' }}
            </el-button>
          </div>
          <div v-if="externalRefs.length" class="external-ref-preview-list">
            <article
              v-for="item in externalRefs"
              :key="item.id"
              class="external-ref-preview-card"
            >
              <span>{{ getExternalRefMeta(item.refType).label }}</span>
              <strong>{{ item.externalId || '未设置外部 ID' }}</strong>
              <p>{{ getExternalRefMeta(item.refType).note }}</p>
              <em>{{ formatDateTime(item.syncedAt) }}</em>
              <div class="external-ref-actions">
                <a
                  v-if="item.externalUrl"
                  :href="item.externalUrl"
                  target="_blank"
                  rel="noreferrer"
                >
                  打开
                </a>
                <button type="button" @click="openEditExternalRef(item)">编辑</button>
                <button type="button" class="is-danger" @click="handleExternalRefDelete(item)">删除</button>
              </div>
            </article>
          </div>
          <p v-else class="preview-summary">
            {{ editingId ? '当前内容还没有外部引用。后续可以在这里绑定 GitHub 仓库、arXiv 论文或官方公告。' : '请先保存内容，再维护外部引用。' }}
          </p>
        </div>

      </div>
    </div>

    <template #footer>
      <el-button @click="onVisibleChange(false)">取消</el-button>
      <el-button type="primary" @click="submit">{{ submitButtonText }}</el-button>
    </template>
  </el-dialog>

  <!-- External Ref Sub-Dialog -->
  <el-dialog
    :model-value="externalRefDialogVisible"
    :title="editingExternalRefId ? '编辑外部引用' : '新增外部引用'"
    width="680px"
    @update:model-value="onExternalRefVisibleChange"
  >
    <el-form ref="externalRefFormRef" :model="externalRefForm" :rules="externalRefRules" label-width="110px">
      <el-form-item label="引用类型" prop="refType">
        <el-select v-model="externalRefForm.refType">
          <el-option
            v-for="item in externalRefTypeOptions"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="外部ID">
        <el-input v-model="externalRefForm.externalId" placeholder="例如：owner/repo 或 arxiv-demo-001" />
      </el-form-item>
      <el-form-item label="外部链接" prop="externalUrl">
        <el-input v-model="externalRefForm.externalUrl" placeholder="https://example.com" />
      </el-form-item>
      <el-form-item label="同步时间">
        <el-date-picker
          v-model="externalRefForm.syncedAt"
          type="datetime"
          value-format="YYYY-MM-DDTHH:mm:ss"
          placeholder="可选"
        />
      </el-form-item>
      <el-form-item label="原始快照" prop="rawPayloadJson">
        <el-input
          v-model="externalRefForm.rawPayloadJson"
          type="textarea"
          :rows="5"
          placeholder='例如：{"source":"manual","note":"demo"}'
        />
      </el-form-item>
    </el-form>

    <template #footer>
      <el-button @click="onExternalRefVisibleChange(false)">取消</el-button>
      <el-button type="primary" @click="submitExternalRef">
        {{ editingExternalRefId ? '保存修改' : '创建引用' }}
      </el-button>
    </template>
  </el-dialog>
</template>
