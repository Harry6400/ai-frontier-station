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

const PAPER_SUB_CATEGORIES = [
  { value: '3d_ct_denoising', label: '3D CT去噪' },
  { value: 'medical_imaging', label: '医学影像' },
  { value: 'large_model', label: '大模型' }
]

const PAPER_SOURCE_OPTIONS = computed(() =>
  props.contentOptions.sources.filter(s => s.sourceType === 'paper')
)

const FEATURED_LEVEL_OPTIONS = [
  { value: 0, label: '无' },
  { value: 1, label: '普通推荐' },
  { value: 2, label: '重点推荐' },
  { value: 3, label: '首页精选' }
]

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

const isPaper = computed(() => props.form.contentType === 'paper')
const typeMeta = computed(() => getContentTypeMeta(props.form.contentType))

const submitButtonText = computed(() => (props.editingId ? '保存修改' : '创建内容'))

function submit() {
  emit('save')
}

function onDialogClosed() {
  editorTab.value = 'basic'
  formRef.value?.clearValidate()
  emit('dialog-closed')
}

function onVisibleChange(val) {
  emit('update:visible', val)
}

function clearValidate() {
  nextTick(() => formRef.value?.clearValidate())
}

defineExpose({ clearValidate, formRef })
</script>

<template>
  <el-dialog
    :model-value="visible"
    :title="editingId ? `编辑${typeMeta.label}` : `新建${typeMeta.label}`"
    width="min(90vw, 900px)"
    class="content-dialog"
    @update:model-value="onVisibleChange"
    @closed="onDialogClosed"
  >
    <div class="editor-tabs-layout">
      <aside class="editor-tabs-side">
        <div class="type-badge">{{ typeMeta.label }}</div>
        <nav class="editor-tabs-nav">
          <a :class="{ 'is-active': editorTab === 'basic' }" @click="editorTab = 'basic'">基本信息</a>
          <a :class="{ 'is-active': editorTab === 'body' }" @click="editorTab = 'body'">正文内容</a>
        </nav>
      </aside>
      <div class="editor-tabs-main">

        <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
          <!-- ===== BASIC TAB ===== -->
          <div v-show="editorTab === 'basic'">
            <div class="form-grid">
              <el-form-item label="标题" prop="title">
                <el-input v-model="form.title" />
              </el-form-item>
              <el-form-item label="内容标识">
                <el-input v-model="form.slug" placeholder="可选，不填自动生成" />
              </el-form-item>

              <!-- Paper-specific: sub category -->
              <el-form-item v-if="isPaper" label="研究方向" prop="subCategory">
                <el-select v-model="form.subCategory" clearable placeholder="选择研究方向">
                  <el-option
                    v-for="item in PAPER_SUB_CATEGORIES"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>

              <!-- Paper-specific: source (arXiv / HuggingFace) -->
              <el-form-item v-if="isPaper" label="论文来源">
                <el-select v-model="form.sourceId" clearable placeholder="选择来源">
                  <el-option
                    v-for="item in PAPER_SOURCE_OPTIONS"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>

              <!-- Non-paper: category + source (original) -->
              <el-form-item v-if="!isPaper" label="分类" prop="categoryId">
                <el-select v-model="form.categoryId">
                  <el-option
                    v-for="item in contentOptions.categories"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item v-if="!isPaper" label="来源">
                <el-select v-model="form.sourceId" clearable>
                  <el-option
                    v-for="item in contentOptions.sources"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>

              <!-- Paper: two link fields -->
              <el-form-item v-if="isPaper" label="原文链接">
                <el-input v-model="form.paperOriginalUrl" placeholder="https://arxiv.org/abs/..." />
              </el-form-item>
              <el-form-item v-if="isPaper" label="代码链接">
                <el-input v-model="form.paperCodeUrl" placeholder="https://github.com/..." />
              </el-form-item>

              <!-- Non-paper: single source URL -->
              <el-form-item v-if="!isPaper" label="原始链接" prop="sourceUrl">
                <el-input v-model="form.sourceUrl" />
              </el-form-item>

              <el-form-item label="作者">
                <el-input v-model="form.authorName" />
              </el-form-item>
              <el-form-item label="精选级别">
                <el-select v-model="form.featuredLevel">
                  <el-option
                    v-for="item in FEATURED_LEVEL_OPTIONS"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
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
              <el-input v-model="form.summary" type="textarea" :rows="isPaper ? 5 : 3" />
            </el-form-item>
          </div>

          <!-- ===== BODY TAB ===== -->
          <div v-show="editorTab === 'body'">
            <el-form-item label="正文" prop="bodyMarkdown">
              <el-input v-model="form.bodyMarkdown" type="textarea" :rows="18" />
            </el-form-item>
          </div>
        </el-form>

      </div>
    </div>

    <template #footer>
      <el-button @click="onVisibleChange(false)">取消</el-button>
      <el-button type="primary" @click="submit">{{ submitButtonText }}</el-button>
    </template>
  </el-dialog>
</template>

<style scoped>
.type-badge {
  display: inline-block;
  padding: 4px 14px;
  margin-bottom: 16px;
  border-radius: 20px;
  background: #ecf5ff;
  color: #409eff;
  font-size: 13px;
  font-weight: 600;
}

.editor-tabs-layout {
  display: flex;
  gap: 24px;
  min-height: 420px;
}

.editor-tabs-side {
  flex: 0 0 140px;
  padding: 16px 0;
  border-right: 1px solid #ebeef5;
}

.editor-tabs-nav {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.editor-tabs-nav a {
  display: block;
  padding: 8px 16px;
  border-radius: 6px;
  font-size: 14px;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
}

.editor-tabs-nav a:hover {
  background: #f5f7fa;
  color: #409eff;
}

.editor-tabs-nav a.is-active {
  background: #ecf5ff;
  color: #409eff;
  font-weight: 600;
}

.editor-tabs-main {
  flex: 1;
  padding: 16px 0;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 24px;
}
</style>
