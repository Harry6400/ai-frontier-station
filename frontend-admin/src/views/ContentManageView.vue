<script setup>
import { computed, nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import {
  createContent,
  createContentExternalRef,
  getContentDetail,
  getContentOptions,
  getContents,
  getHuggingFaceDailyPapers,
  importAiSource,
  importArxivPaper,
  importGithubRepo,
  importHuggingFacePaper,
  queryGithubRepo,
  removeContentExternalRef,
  removeContent,
  searchArxivPapers,
  searchGithubRepos,
  summarizeAiSource,
  updateContent,
  updateContentExternalRef,
  updateContentStatus
} from '../api/admin'
import { sanitizePreviewHtml } from '../utils/safeMarkdown'

const CONTENT_TYPE_META = {
  news: { label: 'AI资讯', english: 'News Pulse' },
  paper: { label: '论文速递', english: 'Research Digest' },
  project: { label: '热门项目', english: 'Project Watch' },
  company_update: { label: '公司动态', english: 'Company Signal' },
  practice: { label: '技术实践', english: 'Practice Note' }
}

const EXTENSION_TEMPLATES = {
  project: {
    label: 'GitHub 项目模板',
    payload: {
      externalType: 'github_repo',
      githubRepo: 'owner/repo',
      starTrend: 'up',
      language: 'Python'
    }
  },
  paper: {
    label: '论文信息模板',
    payload: {
      externalType: 'arxiv_paper',
      arxivId: '2501.00000',
      paperVenue: 'arXiv',
      projectPage: 'https://example.com'
    }
  },
  company_update: {
    label: '公司动态模板',
    payload: {
      externalType: 'official_post',
      companyFocus: 'OpenAI / DeepSeek',
      productLine: 'model',
      announcementType: 'release'
    }
  },
  practice: {
    label: '技术实践模板',
    payload: {
      externalType: 'community_practice',
      practiceScope: 'rag-agent',
      difficulty: 'intermediate',
      benchmark: 'manual'
    }
  },
  news: {
    label: '资讯来源模板',
    payload: {
      externalType: 'news_item',
      topic: 'model-update',
      importance: 'medium'
    }
  }
}

const EXTERNAL_REF_META = {
  github_repo: { label: 'GitHub 仓库', note: '用于追踪外部仓库记录，后续可同步 Star、语言和 README。' },
  arxiv_paper: { label: 'arXiv 论文', note: '用于追踪论文记录，后续可同步作者、摘要和版本。' },
  official_post: { label: '官方公告', note: '用于追踪公司官方博客或公告原文。' },
  community_practice: { label: '社区实践', note: '用于追踪技术社区中的工程经验文章。' },
  leaderboard_item: { label: '结构化榜单', note: '用于追踪 Arena、HELM、MTEB、SWE-bench 等榜单条目。' },
  manual_source: { label: '人工来源', note: '用于记录人工录入并经过 AI 总结的来源。' }
}

const externalRefTypeOptions = Object.entries(EXTERNAL_REF_META).map(([value, meta]) => ({
  value,
  label: meta.label
}))
const aiSourceTypeOptions = [
  { value: 'leaderboard', label: '结构化榜单源' },
  { value: 'official_blog', label: '官方动态源' },
  { value: 'github', label: 'GitHub / 开源项目' },
  { value: 'paper', label: '论文 / 研究源' },
  { value: 'community', label: '社区热度源' },
  { value: 'manual', label: '人工录入源' }
]
const AI_SOURCE_TEMPLATES = {
  official_blog: {
    label: '官方动态源',
    sourceType: 'official_blog',
    contentType: 'company_update',
    sourceName: 'OpenAI / Anthropic / DeepMind 官方博客',
    sourceTitle: '官方发布新的模型或产品动态',
    sourceUrl: 'https://example.com/official-ai-update',
    originalSummary: '这里记录官方博客或公告的核心信息，例如模型能力更新、产品发布、API 变化、价格或安全策略调整。',
    instruction: '请用产品动态和开发者影响的角度总结，重点说明对 AI 开发者有什么价值。'
  },
  leaderboard: {
    label: '结构化榜单源',
    sourceType: 'leaderboard',
    contentType: 'news',
    sourceName: 'Arena / HELM / MTEB / SWE-bench',
    sourceTitle: 'AI 模型榜单出现新的排名变化',
    sourceUrl: 'https://example.com/leaderboard-update',
    originalSummary: '这里记录榜单名称、模型名、指标变化和排名变化，例如某模型在推理、代码、检索或多模态评测上出现明显变化。',
    instruction: '请强调榜单指标含义和变化价值，避免把榜单排名直接等同于模型绝对能力。'
  },
  github: {
    label: 'GitHub 项目源',
    sourceType: 'github',
    contentType: 'project',
    sourceName: 'GitHub Trending',
    sourceTitle: 'GitHub 上出现值得关注的 AI 开源项目',
    sourceUrl: 'https://github.com/owner/repo',
    originalSummary: '这里记录仓库解决的问题、主要功能、语言、Star 或趋势、README 中最关键的项目定位。',
    instruction: '请按开源项目观察方式总结，突出项目用途、适合人群、工程价值和后续可跟踪字段。'
  },
  paper: {
    label: '论文研究源',
    sourceType: 'paper',
    contentType: 'paper',
    sourceName: 'arXiv / Papers with Code / Hugging Face Papers',
    sourceTitle: 'AI 研究论文提出新的方法或评测结果',
    sourceUrl: 'https://example.com/paper',
    originalSummary: '这里记录论文问题、核心方法、实验结果、项目链接，以及它和 RAG、Agent、推理优化或评测方向的关系。',
    instruction: '请按科研动态摘要方式总结，避免过度技术细节，突出研究问题、方法亮点和应用启发。'
  },
  community: {
    label: '社区实践源',
    sourceType: 'community',
    contentType: 'practice',
    sourceName: '技术社区 / X 关键账号',
    sourceTitle: '社区分享了大模型工程实践经验',
    sourceUrl: 'https://example.com/community-practice',
    originalSummary: '这里记录社区文章或帖子中的实践场景、解决方案、踩坑点、性能优化经验或工具链组合。',
    instruction: '请按工程实践笔记方式总结，突出可复用做法、适用条件和可能风险。'
  },
  manual: {
    label: '人工录入源',
    sourceType: 'manual',
    contentType: 'news',
    sourceName: '人工精选来源',
    sourceTitle: '人工发现的一条 AI 前沿信息',
    sourceUrl: 'https://example.com/manual-source',
    originalSummary: '这里记录你自己发现的 AI 信息重点，适合暂时还没有结构化来源接口的内容。',
    instruction: '请按内容平台导读方式总结，保持中性、可信，方便后续检索和复盘。'
  }
}

const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const contents = ref([])
const contentOptions = ref({
  categories: [],
  tags: [],
  sources: [],
  contentTypes: [],
  publishStatuses: []
})
const pagination = reactive({
  pageNum: 1,
  pageSize: 10,
  total: 0
})
const filters = reactive({
  keyword: '',
  contentType: '',
  categoryId: null,
  sourceId: null,
  publishStatus: ''
})
const formRef = ref()
const externalRefFormRef = ref()
const githubImportFormRef = ref()
const aiSourceFormRef = ref()
const previewMode = ref('card')
const form = reactive({
  title: '',
  slug: '',
  contentType: 'news',
  summary: '',
  coverImage: '',
  categoryId: null,
  sourceId: null,
  sourceUrl: '',
  authorName: 'Harry',
  publishStatus: 'DRAFT',
  featuredLevel: 0,
  readingTime: 5,
  publishedAt: '',
  bodyMarkdown: '',
  extraJson: '',
  tagIds: []
})
const externalRefs = ref([])
const externalRefDialogVisible = ref(false)
const editingExternalRefId = ref(null)
const githubImportDialogVisible = ref(false)
const githubQueryLoading = ref(false)
const githubSearchLoading = ref(false)
const githubSearchResults = ref([])
const aiSourceDialogVisible = ref(false)
const aiSourceGenerating = ref(false)
const aiSourceGenerated = ref(null)
const aiSourceTemplateKey = ref('official_blog')
const externalRefForm = reactive({
  refType: 'github_repo',
  externalId: '',
  externalUrl: '',
  rawPayloadJson: '',
  syncedAt: ''
})
const githubImportForm = reactive({
  repoFullName: '',
  repoUrl: '',
  description: '',
  stars: 0,
  language: 'Python',
  topics: 'ai,llm,agent',
  homepage: '',
  categoryId: null,
  sourceId: null,
  tagIds: [],
  publishStatus: 'DRAFT'
})
const githubSearchForm = reactive({
  keyword: 'llm agent',
  sort: 'stars',
  pageNum: 1,
  pageSize: 8
})
const arxivImportDialogVisible = ref(false)
const arxivSearchLoading = ref(false)
const arxivSearchResults = ref([])
const arxivSearchForm = reactive({
  query: 'cat:cs.AI',
  start: 0,
  maxResults: 8
})
const arxivImportForm = reactive({
  arxivId: '',
  title: '',
  authors: [],
  abstractText: '',
  pdfUrl: '',
  categoryId: null,
  sourceId: null,
  tagIds: [],
  publishStatus: 'DRAFT'
})
const arxivImportFormRef = ref()
const huggingfaceImportDialogVisible = ref(false)
const huggingfaceSearchLoading = ref(false)
const huggingfaceSearchResults = ref([])
const huggingfaceImportForm = reactive({
  paperId: '',
  title: '',
  authors: [],
  abstractText: '',
  htmlUrl: '',
  likes: 0,
  comments: 0,
  categoryId: null,
  sourceId: null,
  tagIds: [],
  publishStatus: 'DRAFT'
})
const huggingfaceImportFormRef = ref()
const aiSourceForm = reactive({
  sourceTitle: '',
  sourceUrl: '',
  sourceName: '',
  sourceType: 'official_blog',
  contentType: 'news',
  originalSummary: '',
  originalExcerpt: '',
  imageUrl: '',
  instruction: '请按 AI 开发者视角提炼价值，避免夸大。',
  categoryId: null,
  sourceId: null,
  tagIds: [],
  provider: 'bailian'
})

function getContentTypeMeta(type) {
  return CONTENT_TYPE_META[type] || { label: type || '内容', english: 'Content' }
}

function getExternalRefMeta(type) {
  return EXTERNAL_REF_META[type] || { label: type || '外部引用', note: '外部系统记录，可用于后续同步和追踪。' }
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

function isHttpUrl(value) {
  try {
    const url = new URL(value)
    return ['http:', 'https:'].includes(url.protocol)
  } catch (error) {
    return false
  }
}

function validateSourceUrl(_, value, callback) {
  if (!value) {
    callback()
    return
  }
  if (isHttpUrl(value)) {
    callback()
    return
  }
  callback(new Error('请输入合法的 http 或 https 链接'))
}

function validateExtraJson(_, value, callback) {
  if (!value) {
    callback()
    return
  }
  try {
    JSON.parse(value)
    callback()
  } catch (error) {
    callback(new Error('请输入合法 JSON，例如 {"externalType":"github_repo"}'))
  }
}

const rules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  contentType: [{ required: true, message: '请选择内容类型', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  publishStatus: [{ required: true, message: '请选择发布状态', trigger: 'change' }],
  summary: [{ required: true, message: '请输入摘要', trigger: 'blur' }],
  sourceUrl: [{ validator: validateSourceUrl, trigger: 'blur' }],
  bodyMarkdown: [{ required: true, message: '请输入正文', trigger: 'blur' }],
  extraJson: [{ validator: validateExtraJson, trigger: 'blur' }]
}

const externalRefRules = {
  refType: [{ required: true, message: '请选择外部引用类型', trigger: 'change' }],
  externalUrl: [{ validator: validateSourceUrl, trigger: 'blur' }],
  rawPayloadJson: [{ validator: validateExtraJson, trigger: 'blur' }]
}
const githubImportRules = {
  repoFullName: [{ required: true, message: '请输入仓库全名，例如 openai/openai-cookbook', trigger: 'blur' }],
  repoUrl: [
    { required: true, message: '请输入 GitHub 仓库链接', trigger: 'blur' },
    { validator: validateSourceUrl, trigger: 'blur' }
  ],
  description: [{ required: true, message: '请输入项目简介', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  publishStatus: [{ required: true, message: '请选择发布状态', trigger: 'change' }]
}
const aiSourceRules = {
  sourceTitle: [{ required: true, message: '请输入来源标题', trigger: 'blur' }],
  sourceUrl: [
    { required: true, message: '请输入来源链接', trigger: 'blur' },
    { validator: validateSourceUrl, trigger: 'blur' }
  ],
  contentType: [{ required: true, message: '请选择内容类型', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  originalSummary: [{ required: true, message: '请输入原文摘要或你看到的主要信息', trigger: 'blur' }]
}

const categoryMap = computed(() =>
  Object.fromEntries(contentOptions.value.categories.map((item) => [item.id, item.name]))
)
const sourceMap = computed(() =>
  Object.fromEntries(contentOptions.value.sources.map((item) => [item.id, item.name]))
)
const statusMap = computed(() =>
  Object.fromEntries(contentOptions.value.publishStatuses.map((item) => [item.value, item.label]))
)
const activeAdminFilters = computed(() => {
  const chips = []
  if (filters.keyword.trim()) {
    chips.push(`关键词：${filters.keyword.trim()}`)
  }
  if (filters.contentType) {
    chips.push(`类型：${getContentTypeMeta(filters.contentType).label}`)
  }
  if (filters.categoryId) {
    chips.push(`分类：${categoryMap.value[filters.categoryId] || filters.categoryId}`)
  }
  if (filters.sourceId) {
    chips.push(`来源：${sourceMap.value[filters.sourceId] || filters.sourceId}`)
  }
  if (filters.publishStatus) {
    chips.push(`状态：${statusMap.value[filters.publishStatus] || filters.publishStatus}`)
  }
  return chips
})
const filterSummaryText = computed(() => {
  if (!pagination.total && activeAdminFilters.value.length) {
    return '当前筛选条件下没有匹配内容，可以清空条件后重新查看。'
  }
  if (!activeAdminFilters.value.length) {
    return `当前显示全部内容，共 ${pagination.total} 条。`
  }
  return `当前命中 ${pagination.total} 条内容，已应用 ${activeAdminFilters.value.length} 个筛选条件。`
})

const previewTypeMeta = computed(() => getContentTypeMeta(form.contentType))
const selectedCategoryName = computed(() => categoryMap.value[form.categoryId] || '未选择分类')
const selectedSource = computed(() =>
  contentOptions.value.sources.find((item) => item.id === form.sourceId) || null
)
const selectedTags = computed(() =>
  contentOptions.value.tags.filter((item) => form.tagIds.includes(item.id))
)
const publishStatusOption = computed(() =>
  contentOptions.value.publishStatuses.find((item) => item.value === form.publishStatus)
)
const bodyStats = computed(() => {
  const trimmed = form.bodyMarkdown.trim()
  const lines = trimmed ? trimmed.split('\n').filter(Boolean).length : 0
  const paragraphs = trimmed ? trimmed.split(/\n\s*\n/).filter(Boolean).length : 0
  return {
    characters: trimmed.length,
    lines,
    paragraphs
  }
})
const extensionTemplate = computed(() => EXTENSION_TEMPLATES[form.contentType] || EXTENSION_TEMPLATES.news)
const previewExtraEntries = computed(() => {
  if (!form.extraJson.trim()) {
    return []
  }
  try {
    return Object.entries(JSON.parse(form.extraJson))
  } catch (error) {
    return [['JSON格式', '当前扩展字段暂未通过校验']]
  }
})
const formChecklist = computed(() => [
  {
    label: '标题已填写',
    passed: !!form.title.trim()
  },
  {
    label: '摘要已填写',
    passed: !!form.summary.trim()
  },
  {
    label: '分类已选择',
    passed: !!form.categoryId
  },
  {
    label: '正文长度适合展示',
    passed: form.bodyMarkdown.trim().length >= 80,
    hint: '建议至少 80 字'
  },
  {
    label: '已发布内容建议补来源链接',
    passed: form.publishStatus !== 'PUBLISHED' || !!form.sourceUrl.trim(),
    hint: '发布内容建议保留原始链接，方便追溯来源'
  }
])
const passedChecklistCount = computed(() => formChecklist.value.filter((item) => item.passed).length)
const previewModes = [
  { label: '门户卡片', value: 'card' },
  { label: '详情头图', value: 'detail' },
  { label: '正文排版', value: 'article' }
]
const submitNotice = computed(() => {
  if (form.publishStatus === 'PUBLISHED' && !form.publishedAt) {
    return '当前状态为“已发布”，如果不手动设置发布时间，保存后后端会自动补当前时间。'
  }
  if (!form.sourceId && !form.sourceUrl) {
    return '当前内容未绑定来源，适合手动录入示例；后续如果接外部数据源，再补来源映射即可。'
  }
  return '右侧预览区会随着输入实时更新，适合在录入时同步检查展示效果。'
})
const submitButtonText = computed(() => (editingId.value ? '保存修改' : '创建内容'))
const previewPublishedAt = computed(() => formatDateTime(form.publishedAt))
const previewBodyHtml = computed(() => sanitizePreviewHtml(renderMarkdownPreview(form.bodyMarkdown)))
const aiMatchedTags = computed(() => matchSuggestedTags(aiSourceGenerated.value?.tagSuggestions || []))
const aiUnmatchedTagSuggestions = computed(() => {
  const matchedNames = new Set(aiMatchedTags.value.map((item) => normalizeTagName(item.suggestion)))
  return (aiSourceGenerated.value?.tagSuggestions || [])
    .filter((item) => !matchedNames.has(normalizeTagName(item)))
})
const aiSelectedTags = computed(() =>
  contentOptions.value.tags.filter((item) => aiSourceForm.tagIds.includes(item.id))
)
const aiSourceStep = computed(() => {
  if (aiSourceGenerated.value) {
    return '确认结果'
  }
  if (aiSourceGenerating.value) {
    return '生成中'
  }
  return '填写来源'
})

function normalizeTagName(value) {
  return String(value || '').trim().toLowerCase()
}

function matchSuggestedTags(suggestions) {
  return suggestions
    .map((suggestion) => {
      const matched = contentOptions.value.tags.find((tag) => normalizeTagName(tag.name) === normalizeTagName(suggestion))
      return matched ? { suggestion, tag: matched } : null
    })
    .filter(Boolean)
}

function resetForm() {
  editingId.value = null
  externalRefs.value = []
  previewMode.value = 'card'
  form.title = ''
  form.slug = ''
  form.contentType = contentOptions.value.contentTypes[0]?.value || 'news'
  form.summary = ''
  form.coverImage = ''
  form.categoryId = contentOptions.value.categories[0]?.id || null
  form.sourceId = null
  form.sourceUrl = ''
  form.authorName = 'Harry'
  form.publishStatus = 'DRAFT'
  form.featuredLevel = 0
  form.readingTime = 5
  form.publishedAt = ''
  form.bodyMarkdown = ''
  form.extraJson = ''
  form.tagIds = []
}

function resetExternalRefForm() {
  editingExternalRefId.value = null
  externalRefForm.refType = extensionTemplate.value.payload.externalType || 'github_repo'
  externalRefForm.externalId = ''
  externalRefForm.externalUrl = form.sourceUrl || selectedSource.value?.websiteUrl || ''
  externalRefForm.rawPayloadJson = ''
  externalRefForm.syncedAt = ''
}

function findDefaultGitHubSourceId() {
  const githubSource = contentOptions.value.sources.find((item) => item.sourceType === 'github')
  return githubSource?.id || null
}

function resetGitHubImportForm() {
  githubImportForm.repoFullName = 'openai/openai-cookbook'
  githubImportForm.repoUrl = 'https://github.com/openai/openai-cookbook'
  githubImportForm.description = 'OpenAI 官方维护的示例与最佳实践集合，适合追踪大模型应用开发、工具调用、RAG 和评测等工程实践。'
  githubImportForm.stars = 0
  githubImportForm.language = 'Jupyter Notebook'
  githubImportForm.topics = 'openai,llm,cookbook,rag,agent'
  githubImportForm.homepage = ''
  githubImportForm.categoryId = contentOptions.value.categories[0]?.id || null
  githubImportForm.sourceId = findDefaultGitHubSourceId()
  githubImportForm.tagIds = contentOptions.value.tags.slice(0, 2).map((item) => item.id)
  githubImportForm.publishStatus = 'DRAFT'
  githubSearchForm.keyword = 'llm agent'
  githubSearchForm.sort = 'stars'
  githubSearchResults.value = []
}

function applyGitHubCandidate(candidate) {
  githubImportForm.repoFullName = candidate.repoFullName || ''
  githubImportForm.repoUrl = candidate.repoUrl || ''
  githubImportForm.description = candidate.description || candidate.readmeSummary || '该仓库来自 GitHub API 查询，建议补充一句项目价值说明。'
  githubImportForm.stars = candidate.stars || 0
  githubImportForm.language = candidate.language || ''
  githubImportForm.topics = (candidate.topics || []).join(',')
  githubImportForm.homepage = candidate.homepage || ''
}

async function queryGitHubRepository() {
  if (!githubImportForm.repoFullName.trim()) {
    ElMessage.warning('请先输入仓库全名，例如 openai/openai-cookbook')
    return
  }
  githubQueryLoading.value = true
  try {
    const res = await queryGithubRepo(githubImportForm.repoFullName.trim())
    applyGitHubCandidate(res.data)
    ElMessage.success('GitHub 仓库信息已回填')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    githubQueryLoading.value = false
  }
}

async function searchGitHubRepositories() {
  if (!githubSearchForm.keyword.trim()) {
    ElMessage.warning('请输入搜索关键词')
    return
  }
  githubSearchLoading.value = true
  try {
    const res = await searchGithubRepos(githubSearchForm)
    githubSearchResults.value = res.data.records || []
    ElMessage.success(`找到 ${githubSearchResults.value.length} 个候选仓库`)
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    githubSearchLoading.value = false
  }
}

function resetAiSourceForm() {
  aiSourceTemplateKey.value = 'official_blog'
  applyAiSourceTemplate('official_blog')
  aiSourceForm.categoryId = contentOptions.value.categories[0]?.id || null
  aiSourceForm.sourceId = null
  aiSourceForm.tagIds = []
  aiSourceGenerated.value = null
}

function applyAiSourceTemplate(templateKey) {
  const template = AI_SOURCE_TEMPLATES[templateKey] || AI_SOURCE_TEMPLATES.official_blog
  aiSourceTemplateKey.value = templateKey
  aiSourceForm.sourceTitle = template.sourceTitle
  aiSourceForm.sourceUrl = template.sourceUrl
  aiSourceForm.sourceName = template.sourceName
  aiSourceForm.sourceType = template.sourceType
  aiSourceForm.contentType = template.contentType
  aiSourceForm.originalSummary = template.originalSummary
  aiSourceForm.originalExcerpt = ''
  aiSourceForm.imageUrl = ''
  aiSourceForm.instruction = template.instruction
  aiSourceForm.provider = 'bailian'
  aiSourceGenerated.value = null
}

function openAiSourceImport() {
  resetAiSourceForm()
  aiSourceDialogVisible.value = true
  nextTick(() => aiSourceFormRef.value?.clearValidate())
}

async function generateAiSourceSummary() {
  await aiSourceFormRef.value.validate()
  aiSourceGenerating.value = true
  try {
    const res = await summarizeAiSource({
      sourceTitle: aiSourceForm.sourceTitle,
      sourceUrl: aiSourceForm.sourceUrl,
      sourceName: aiSourceForm.sourceName || null,
      sourceType: aiSourceForm.sourceType,
      contentType: aiSourceForm.contentType,
      originalSummary: aiSourceForm.originalSummary,
      originalExcerpt: aiSourceForm.originalExcerpt || null,
      imageUrl: aiSourceForm.imageUrl || null,
      instruction: aiSourceForm.instruction || null,
      provider: aiSourceForm.provider || 'bailian'
    })
    aiSourceGenerated.value = res.data
    applyMatchedSuggestedTags()
    ElMessage.success('AI 导读生成成功，请确认标签和内容后继续')
  } catch (error) {
    ElMessage.error(formatAiSourceError(error))
  } finally {
    aiSourceGenerating.value = false
  }
}

function applyMatchedSuggestedTags() {
  const matchedIds = aiMatchedTags.value.map((item) => item.tag.id)
  aiSourceForm.tagIds = Array.from(new Set([...aiSourceForm.tagIds, ...matchedIds]))
}

function formatAiSourceError(error) {
  const message = error?.message || 'AI 导读生成失败'
  if (message.includes('DASHSCOPE_API_KEY') || message.includes('MIMO_API_KEY') || message.includes('未启用 AI 总结能力')) {
    return '未启用 AI 总结能力：请先在 API 设置页配置对应 Provider 的 API Key。'
  }
  if (message.includes('Network Error') || message.includes('timeout') || message.includes('无法访问')) {
    return 'AI Provider 服务暂时无法访问：请检查网络、API Key 是否有效，或稍后重试。'
  }
  if (message.includes('解析失败')) {
    return 'AI Provider 返回格式不符合预期：请补充更清楚的来源摘要后重试。'
  }
  if (message.includes('过短')) {
    return message
  }
  return message
}

function buildAiSourceImportPayload() {
  const generated = aiSourceGenerated.value
  return {
    title: generated.suggestedTitle || aiSourceForm.sourceTitle,
    contentType: aiSourceForm.contentType,
    summary: generated.summary || aiSourceForm.originalSummary,
    coverImage: aiSourceForm.imageUrl || null,
    categoryId: aiSourceForm.categoryId,
    sourceId: aiSourceForm.sourceId || null,
    sourceUrl: aiSourceForm.sourceUrl,
    sourceTitle: aiSourceForm.sourceTitle,
    sourceName: aiSourceForm.sourceName || null,
    sourceType: aiSourceForm.sourceType,
    originalSummary: aiSourceForm.originalSummary,
    originalExcerpt: aiSourceForm.originalExcerpt || null,
    sourceBrief: generated.sourceBrief,
    aiSummary: generated.aiSummary,
    recommendationReason: generated.recommendationReason,
    importanceScore: generated.importanceScore,
    bodyMarkdown: generated.bodyMarkdown,
    tagSuggestions: generated.tagSuggestions || [],
    tagIds: aiSourceForm.tagIds
  }
}

async function submitAiSourceImport() {
  await aiSourceFormRef.value.validate()
  if (!aiSourceGenerated.value) {
    ElMessage.warning('请先生成 AI 导读，再创建草稿')
    return
  }
  try {
    await importAiSource(buildAiSourceImportPayload())
    ElMessage.success('AI 来源整理内容已创建为草稿')
    aiSourceDialogVisible.value = false
    filters.contentType = aiSourceForm.contentType
    pagination.pageNum = 1
    await loadContents()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function fillContentFormFromAiSource() {
  if (!aiSourceGenerated.value) {
    ElMessage.warning('请先生成 AI 导读，再回填编辑表单')
    return
  }
  const payload = buildAiSourceImportPayload()
  resetForm()
  editingId.value = null
  form.title = payload.title
  form.contentType = payload.contentType
  form.summary = payload.summary
  form.coverImage = payload.coverImage || ''
  form.categoryId = payload.categoryId
  form.sourceId = payload.sourceId
  form.sourceUrl = payload.sourceUrl
  form.authorName = aiSourceForm.provider === 'mimo' ? 'MiMo辅助整理' : '百炼辅助整理'
  form.publishStatus = 'DRAFT'
  form.featuredLevel = payload.importanceScore >= 85 ? 3 : payload.importanceScore >= 70 ? 2 : 1
  form.readingTime = aiSourceGenerated.value.readingTime || 6
  form.bodyMarkdown = payload.bodyMarkdown
  form.extraJson = aiSourceGenerated.value.extraJson || ''
  form.tagIds = payload.tagIds || []
  previewMode.value = 'detail'
  externalRefs.value = []
  aiSourceDialogVisible.value = false
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
  ElMessage.success('AI 生成结果已回填到内容编辑表单，可继续人工修改')
}

function applyExtensionTemplate() {
  form.extraJson = JSON.stringify(extensionTemplate.value.payload, null, 2)
}

function openCreateExternalRef() {
  if (!editingId.value) {
    ElMessage.warning('请先保存内容，再维护外部引用')
    return
  }
  resetExternalRefForm()
  externalRefDialogVisible.value = true
  nextTick(() => externalRefFormRef.value?.clearValidate())
}

function openEditExternalRef(item) {
  editingExternalRefId.value = item.id
  externalRefForm.refType = item.refType
  externalRefForm.externalId = item.externalId || ''
  externalRefForm.externalUrl = item.externalUrl || ''
  externalRefForm.rawPayloadJson = item.rawPayloadJson || ''
  externalRefForm.syncedAt = item.syncedAt ? item.syncedAt.replace(' ', 'T') : ''
  externalRefDialogVisible.value = true
  nextTick(() => externalRefFormRef.value?.clearValidate())
}

async function refreshContentDetail() {
  if (!editingId.value) {
    return
  }
  const res = await getContentDetail(editingId.value)
  externalRefs.value = res.data.externalRefs || []
}

async function submitExternalRef() {
  if (!editingId.value) {
    ElMessage.warning('请先保存内容，再维护外部引用')
    return
  }
  await externalRefFormRef.value.validate()
  try {
    const payload = {
      ...externalRefForm,
      syncedAt: externalRefForm.syncedAt || null
    }
    if (editingExternalRefId.value) {
      await updateContentExternalRef(editingId.value, editingExternalRefId.value, payload)
      ElMessage.success('外部引用更新成功')
    } else {
      await createContentExternalRef(editingId.value, payload)
      ElMessage.success('外部引用创建成功')
    }
    externalRefDialogVisible.value = false
    await refreshContentDetail()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function handleExternalRefDelete(item) {
  if (!editingId.value) {
    return
  }
  try {
    await ElMessageBox.confirm('删除后无法恢复，确认删除这条外部引用吗？', '删除外部引用', { type: 'warning' })
    await removeContentExternalRef(editingId.value, item.id)
    ElMessage.success('外部引用删除成功')
    await refreshContentDetail()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function loadOptions() {
  const res = await getContentOptions()
  contentOptions.value = res.data
}

async function loadContents() {
  loading.value = true
  try {
    const res = await getContents({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: filters.keyword || undefined,
      contentType: filters.contentType || undefined,
      categoryId: filters.categoryId || undefined,
      sourceId: filters.sourceId || undefined,
      publishStatus: filters.publishStatus || undefined
    })
    contents.value = res.data.records
    pagination.total = res.data.total
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function initialize() {
  try {
    await loadOptions()
    resetForm()
    await loadContents()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function handleSearch() {
  pagination.pageNum = 1
  loadContents()
}

function clearFilters() {
  filters.keyword = ''
  filters.contentType = ''
  filters.categoryId = null
  filters.sourceId = null
  filters.publishStatus = ''
  handleSearch()
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function openGitHubImport() {
  resetGitHubImportForm()
  githubImportDialogVisible.value = true
  nextTick(() => githubImportFormRef.value?.clearValidate())
}

async function submitGitHubImport() {
  await githubImportFormRef.value.validate()
  try {
    await importGithubRepo({
      ...githubImportForm,
      homepage: githubImportForm.homepage || null,
      sourceId: githubImportForm.sourceId || null
    })
    ElMessage.success('GitHub 项目导入成功')
    githubImportDialogVisible.value = false
    filters.contentType = 'project'
    pagination.pageNum = 1
    await loadContents()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function openEdit(row) {
  try {
    const res = await getContentDetail(row.id)
    const detail = res.data
    editingId.value = row.id
    form.title = detail.title
    form.slug = detail.slug
    form.contentType = detail.contentType
    form.summary = detail.summary || ''
    form.coverImage = detail.coverImage || ''
    form.categoryId = detail.categoryId
    form.sourceId = detail.sourceId
    form.sourceUrl = detail.sourceUrl || ''
    form.authorName = detail.authorName || ''
    form.publishStatus = detail.publishStatus
    form.featuredLevel = detail.featuredLevel ?? 0
    form.readingTime = detail.readingTime ?? 5
    form.publishedAt = detail.publishedAt ? detail.publishedAt.replace(' ', 'T') : detail.publishedAt
    form.bodyMarkdown = detail.bodyMarkdown || ''
    form.extraJson = detail.extraJson || ''
    form.tagIds = (detail.tags || []).map((item) => item.id)
    externalRefs.value = detail.externalRefs || []
    previewMode.value = 'detail'
    dialogVisible.value = true
    nextTick(() => formRef.value?.clearValidate())
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function submit() {
  await formRef.value.validate()
  try {
    const payload = {
      ...form,
      publishedAt: form.publishedAt || null
    }
    if (editingId.value) {
      await updateContent(editingId.value, payload)
      ElMessage.success('内容更新成功')
    } else {
      await createContent(payload)
      ElMessage.success('内容创建成功')
    }
    dialogVisible.value = false
    resetForm()
    loadContents()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function handleDialogClosed() {
  formRef.value?.clearValidate()
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('删除后无法恢复，确认继续吗？', '删除内容', { type: 'warning' })
    await removeContent(id)
    ElMessage.success('内容删除成功')
    loadContents()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

async function handleStatus(row, status) {
  try {
    await updateContentStatus(row.id, status)
    ElMessage.success('状态更新成功')
    loadContents()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function openArxivImport() {
  resetArxivImportForm()
  arxivImportDialogVisible.value = true
  nextTick(() => arxivImportFormRef.value?.clearValidate())
}

async function searchArxiv() {
  if (!arxivSearchForm.query.trim()) {
    ElMessage.warning('请先输入 arXiv 搜索条件')
    return
  }
  arxivSearchLoading.value = true
  try {
    const res = await searchArxivPapers({
      query: arxivSearchForm.query,
      start: arxivSearchForm.start,
      maxResults: arxivSearchForm.maxResults
    })
    arxivSearchResults.value = res.data.records || []
    if (!arxivSearchResults.value.length) {
      ElMessage.info('没有找到匹配的 arXiv 论文')
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    arxivSearchLoading.value = false
  }
}

function applyArxivCandidate(paper) {
  arxivImportForm.arxivId = paper.arxivId
  arxivImportForm.title = paper.title
  arxivImportForm.authors = paper.authors || []
  arxivImportForm.abstractText = paper.abstractText || ''
  arxivImportForm.pdfUrl = paper.pdfUrl || ''
}

function resetArxivImportForm() {
  arxivImportForm.arxivId = ''
  arxivImportForm.title = ''
  arxivImportForm.authors = []
  arxivImportForm.abstractText = ''
  arxivImportForm.pdfUrl = ''
  arxivImportForm.categoryId = contentOptions.value.categories.find((item) => item.slug === 'paper-digest')?.id || null
  arxivImportForm.sourceId = contentOptions.value.sources.find((item) => item.slug === 'arxiv')?.id || null
  arxivImportForm.tagIds = []
  arxivImportForm.publishStatus = 'DRAFT'
  arxivSearchResults.value = []
}

async function submitArxivImport() {
  await arxivImportFormRef.value.validate()
  try {
    const authors = typeof arxivImportForm.authors === 'string'
      ? arxivImportForm.authors.split(',').map((item) => item.trim()).filter(Boolean)
      : arxivImportForm.authors
    await importArxivPaper({
      ...arxivImportForm,
      authors,
      sourceId: arxivImportForm.sourceId || null
    })
    ElMessage.success('arXiv 论文导入成功')
    arxivImportDialogVisible.value = false
    filters.contentType = 'paper'
    pagination.pageNum = 1
    await loadContents()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

function openHuggingFaceImport() {
  resetHuggingFaceImportForm()
  huggingfaceImportDialogVisible.value = true
  loadHuggingFaceDailyPapers()
}

async function loadHuggingFaceDailyPapers() {
  huggingfaceSearchLoading.value = true
  try {
    const res = await getHuggingFaceDailyPapers()
    huggingfaceSearchResults.value = res.data || []
    if (!huggingfaceSearchResults.value.length) {
      ElMessage.info('暂未获取到 HuggingFace 每日论文')
    }
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    huggingfaceSearchLoading.value = false
  }
}

function applyHuggingFaceCandidate(paper) {
  huggingfaceImportForm.paperId = paper.paperId
  huggingfaceImportForm.title = paper.title
  huggingfaceImportForm.authors = paper.authors || []
  huggingfaceImportForm.abstractText = paper.abstractText || ''
  huggingfaceImportForm.htmlUrl = paper.htmlUrl || ''
  huggingfaceImportForm.likes = paper.likes || 0
  huggingfaceImportForm.comments = paper.comments || 0
}

function resetHuggingFaceImportForm() {
  huggingfaceImportForm.paperId = ''
  huggingfaceImportForm.title = ''
  huggingfaceImportForm.authors = []
  huggingfaceImportForm.abstractText = ''
  huggingfaceImportForm.htmlUrl = ''
  huggingfaceImportForm.likes = 0
  huggingfaceImportForm.comments = 0
  huggingfaceImportForm.categoryId = contentOptions.value.categories.find((item) => item.slug === 'paper-digest')?.id || null
  huggingfaceImportForm.sourceId = contentOptions.value.sources.find((item) => item.slug === 'arxiv')?.id || null
  huggingfaceImportForm.tagIds = []
  huggingfaceImportForm.publishStatus = 'DRAFT'
  huggingfaceSearchResults.value = []
}

async function submitHuggingFaceImport() {
  await huggingfaceImportFormRef.value.validate()
  try {
    const authors = typeof huggingfaceImportForm.authors === 'string'
      ? huggingfaceImportForm.authors.split(',').map((item) => item.trim()).filter(Boolean)
      : huggingfaceImportForm.authors
    await importHuggingFacePaper({
      ...huggingfaceImportForm,
      authors,
      sourceId: huggingfaceImportForm.sourceId || null
    })
    ElMessage.success('HuggingFace 论文导入成功')
    huggingfaceImportDialogVisible.value = false
    filters.contentType = 'paper'
    pagination.pageNum = 1
    await loadContents()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

onMounted(initialize)
</script>

<template>
  <section class="admin-page" v-loading="loading">
    <div class="page-intro">
      <span class="sidebar-kicker">Contents</span>
      <h3>内容管理</h3>
      <p>这里已经接入真实内容表，你现在可以新增、编辑、删除和切换发布状态。</p>
    </div>

    <div class="card-panel">
      <div class="toolbar-row toolbar-row--wrap">
        <el-input
          v-model="filters.keyword"
          placeholder="搜索标题"
          class="toolbar-input"
          clearable
          @keyup.enter="handleSearch"
        />
        <el-select v-model="filters.contentType" placeholder="内容类型" clearable class="toolbar-select">
          <el-option
            v-for="item in contentOptions.contentTypes"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-select v-model="filters.categoryId" placeholder="分类" clearable class="toolbar-select">
          <el-option
            v-for="item in contentOptions.categories"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-select v-model="filters.sourceId" placeholder="来源" clearable class="toolbar-select">
          <el-option
            v-for="item in contentOptions.sources"
            :key="item.id"
            :label="item.name"
            :value="item.id"
          />
        </el-select>
        <el-select v-model="filters.publishStatus" placeholder="发布状态" clearable class="toolbar-select">
          <el-option
            v-for="item in contentOptions.publishStatuses"
            :key="item.value"
            :label="item.label"
            :value="item.value"
          />
        </el-select>
        <el-button type="primary" @click="handleSearch">查询</el-button>
        <el-button @click="clearFilters">清空筛选</el-button>
        <el-button type="warning" @click="openAiSourceImport">AI 来源整理</el-button>
        <el-button type="success" @click="openGitHubImport">GitHub 项目导入</el-button>
        <el-button type="primary" plain @click="openArxivImport">arXiv 论文导入</el-button>
        <el-button type="info" @click="openHuggingFaceImport">HuggingFace 论文导入</el-button>
        <el-button @click="openCreate">新建内容</el-button>
      </div>

      <div class="filter-summary">
        <p>{{ filterSummaryText }}</p>
        <div class="filter-chip-row">
          <span
            v-for="item in activeAdminFilters"
            :key="item"
            class="admin-filter-chip"
          >
            {{ item }}
          </span>
          <span v-if="!activeAdminFilters.length" class="admin-filter-chip">
            无筛选
          </span>
        </div>
      </div>

      <el-table :data="contents" stripe>
        <el-table-column prop="title" label="标题" min-width="240" />
        <el-table-column prop="contentType" label="类型" width="140" />
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column prop="sourceName" label="来源" width="140" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="row.publishStatus === 'PUBLISHED' ? 'success' : 'info'">
              {{ row.publishStatus }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标签" min-width="180">
          <template #default="{ row }">
            <div class="table-tag-list">
              <el-tag v-for="tag in row.tags" :key="tag.id" size="small" effect="plain">
                {{ tag.name }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="publishedAt" label="发布时间" width="180" />
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button
              v-if="row.publishStatus !== 'PUBLISHED'"
              link
              type="success"
              @click="handleStatus(row, 'PUBLISHED')"
            >
              发布
            </el-button>
            <el-button
              v-else
              link
              type="warning"
              @click="handleStatus(row, 'DRAFT')"
            >
              转草稿
            </el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-row">
        <el-pagination
          background
          layout="prev, pager, next, total"
          :current-page="pagination.pageNum"
          :page-size="pagination.pageSize"
          :total="pagination.total"
          @current-change="(page) => { pagination.pageNum = page; loadContents() }"
        />
      </div>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="editingId ? '编辑内容' : '新建内容'"
      width="1240px"
      class="content-dialog"
      @closed="handleDialogClosed"
    >
      <div class="content-editor-layout">
        <section class="content-editor-main">
          <div class="editor-guide">
            <span class="sidebar-kicker">Editor</span>
            <h4>{{ editingId ? '编辑内容录入台' : '新建内容录入台' }}</h4>
            <p>左侧录入字段，右侧同步查看预览和提交前检查，方便你在发布前确认内容结构和展示效果。</p>
          </div>

          <el-alert
            class="editor-alert"
            :title="submitNotice"
            type="info"
            :closable="false"
            show-icon
          />

          <el-form ref="formRef" :model="form" :rules="rules" label-width="96px">
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
              <el-form-item label="状态" prop="publishStatus">
                <el-select v-model="form.publishStatus">
                  <el-option
                    v-for="item in contentOptions.publishStatuses"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="精选级别">
                <el-input-number v-model="form.featuredLevel" :min="0" :max="5" />
              </el-form-item>
              <el-form-item label="阅读时长">
                <el-input-number v-model="form.readingTime" :min="1" :max="60" />
              </el-form-item>
              <el-form-item label="发布时间">
                <el-date-picker
                  v-model="form.publishedAt"
                  type="datetime"
                  value-format="YYYY-MM-DDTHH:mm:ss"
                  placeholder="可选"
                />
              </el-form-item>
              <el-form-item label="标签">
                <el-select v-model="form.tagIds" multiple clearable collapse-tags>
                  <el-option
                    v-for="item in contentOptions.tags"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>
            </div>

            <el-form-item label="摘要" prop="summary">
              <el-input v-model="form.summary" type="textarea" :rows="3" />
            </el-form-item>
            <el-form-item label="封面图">
              <el-input v-model="form.coverImage" placeholder="可选，留空时使用默认内容样式" />
            </el-form-item>
            <el-form-item label="正文" prop="bodyMarkdown">
              <el-input v-model="form.bodyMarkdown" type="textarea" :rows="12" />
            </el-form-item>
            <el-form-item label="扩展JSON" prop="extraJson">
              <el-input
                v-model="form.extraJson"
                type="textarea"
                :rows="3"
                placeholder='例如：{"externalType":"github_repo"}'
              />
            </el-form-item>
            <div class="extension-template-panel">
              <div>
                <span class="sidebar-kicker">Source Mapping</span>
                <strong>{{ extensionTemplate.label }}</strong>
                <p>
                  这不是自动采集，而是先把未来采集模块会写入的关键字段预留出来。
                  当前来源类型：{{ selectedSource?.sourceType || '未绑定来源' }}。
                </p>
              </div>
              <el-button @click="applyExtensionTemplate">填入模板</el-button>
            </div>
          </el-form>
        </section>

        <aside class="content-preview-shell">
          <div class="content-preview-sticky">
            <div class="preview-head">
              <div>
                <span class="sidebar-kicker">Preview</span>
                <h4>实时预览</h4>
              </div>
              <span class="preview-status-badge">{{ publishStatusOption?.label || '未设置状态' }}</span>
            </div>

            <div class="preview-mode-switcher">
              <button
                v-for="item in previewModes"
                :key="item.value"
                type="button"
                class="preview-mode-button"
                :class="{ 'is-active': previewMode === item.value }"
                @click="previewMode = item.value"
              >
                {{ item.label }}
              </button>
            </div>

            <article class="content-preview-card">
              <template v-if="previewMode === 'card'">
                <div class="preview-topline">
                  <span class="preview-chip">{{ previewTypeMeta.label }}</span>
                  <span class="preview-meta-text">{{ previewPublishedAt }}</span>
                </div>
                <p class="preview-content-eyebrow">{{ previewTypeMeta.english }}</p>
                <h3>{{ form.title || '这里会显示内容标题预览' }}</h3>
                <p class="preview-summary">
                  {{ form.summary || '摘要会显示在这里，建议保持 1 到 2 句话，便于前台列表和首页卡片展示。' }}
                </p>

                <div class="preview-card-meta">
                  <span>{{ selectedCategoryName }}</span>
                  <span>{{ selectedSource?.name || '人工录入' }}</span>
                </div>

                <div class="preview-tag-row">
                  <span v-for="tag in selectedTags" :key="tag.id" class="preview-tag">
                    {{ tag.name }}
                  </span>
                  <span v-if="!selectedTags.length" class="preview-tag preview-tag--muted">未选择标签</span>
                </div>

                <div class="preview-card-footer">
                  <span class="preview-action-ghost">阅读全文</span>
                </div>
              </template>

              <template v-else-if="previewMode === 'detail'">
                <span class="preview-detail-kicker">{{ previewTypeMeta.label }} · {{ selectedCategoryName }}</span>
                <h3 class="preview-detail-title">{{ form.title || '这里会显示详情页标题预览' }}</h3>
                <p class="preview-summary">
                  {{ form.summary || '这里会显示详情页摘要预览，方便你提前确认详情页头图的文字密度。' }}
                </p>

                <div class="preview-meta-grid">
                  <div class="preview-meta-item">
                    <span>发布时间</span>
                    <strong>{{ previewPublishedAt }}</strong>
                  </div>
                  <div class="preview-meta-item">
                    <span>来源</span>
                    <strong>{{ selectedSource?.name || '人工录入 / 未绑定来源' }}</strong>
                  </div>
                  <div class="preview-meta-item">
                    <span>作者</span>
                    <strong>{{ form.authorName || '未填写作者' }}</strong>
                  </div>
                  <div class="preview-meta-item">
                    <span>阅读时长</span>
                    <strong>{{ form.readingTime || '-' }} 分钟</strong>
                  </div>
                </div>

                <div class="preview-tag-row">
                  <span v-for="tag in selectedTags" :key="tag.id" class="preview-tag">
                    {{ tag.name }}
                  </span>
                  <span v-if="!selectedTags.length" class="preview-tag preview-tag--muted">未选择标签</span>
                </div>
              </template>

              <template v-else>
                <div class="preview-topline">
                  <span class="preview-chip">Article Body</span>
                  <span class="preview-meta-text">{{ bodyStats.paragraphs }} 段</span>
                </div>

                <div class="preview-article-shell">
                  <div class="preview-article-rendered" v-html="previewBodyHtml"></div>
                </div>
              </template>
            </article>

            <section v-if="previewMode !== 'detail'" class="preview-support-panel">
              <div class="preview-meta-grid">
                <div class="preview-meta-item">
                  <span>分类</span>
                  <strong>{{ selectedCategoryName }}</strong>
                </div>
                <div class="preview-meta-item">
                  <span>来源</span>
                  <strong>{{ selectedSource?.name || '人工录入 / 未绑定来源' }}</strong>
                </div>
                <div class="preview-meta-item">
                  <span>作者</span>
                  <strong>{{ form.authorName || '未填写作者' }}</strong>
                </div>
                <div class="preview-meta-item">
                  <span>阅读时长</span>
                  <strong>{{ form.readingTime || '-' }} 分钟</strong>
                </div>
              </div>
            </section>

            <section v-if="previewExtraEntries.length" class="preview-check-panel">
              <div class="preview-check-head">
                <div>
                  <span class="sidebar-kicker">External Mapping</span>
                  <h4>扩展字段预览</h4>
                </div>
                <strong>{{ previewExtraEntries.length }}</strong>
              </div>

              <div class="preview-check-list">
                <article
                  v-for="[key, value] in previewExtraEntries"
                  :key="key"
                  class="preview-check-item"
                >
                  <strong>{{ key }}</strong>
                  <span>{{ value }}</span>
                </article>
              </div>
            </section>

            <section v-if="externalRefs.length" class="preview-check-panel">
              <div class="preview-check-head">
                <div>
                  <span class="sidebar-kicker">External Refs</span>
                  <h4>外部引用记录</h4>
                </div>
                <div class="preview-head-actions">
                  <strong>{{ externalRefs.length }}</strong>
                  <el-button size="small" @click="openCreateExternalRef">新增</el-button>
                </div>
              </div>

              <div class="external-ref-preview-list">
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
            </section>

            <section v-else-if="editingId" class="preview-check-panel">
              <div class="preview-check-head">
                <div>
                  <span class="sidebar-kicker">External Refs</span>
                  <h4>外部引用管理</h4>
                </div>
                <el-button size="small" @click="openCreateExternalRef">新增</el-button>
              </div>
              <p class="preview-summary">
                当前内容还没有外部引用。后续可以在这里绑定 GitHub 仓库、arXiv 论文或官方公告。
              </p>
            </section>

            <section class="preview-check-panel">
              <div class="preview-check-head">
                <div>
                  <span class="sidebar-kicker">Checklist</span>
                  <h4>提交前检查</h4>
                </div>
                <strong>{{ passedChecklistCount }}/{{ formChecklist.length }}</strong>
              </div>

              <div class="preview-check-list">
                <article
                  v-for="item in formChecklist"
                  :key="item.label"
                  class="preview-check-item"
                  :class="{ 'is-passed': item.passed }"
                >
                  <strong>{{ item.label }}</strong>
                  <span>{{ item.passed ? '已就绪' : item.hint || '待补充' }}</span>
                </article>
              </div>
            </section>

            <section class="preview-stat-panel">
              <article class="preview-stat-card">
                <span>正文字符</span>
                <strong>{{ bodyStats.characters }}</strong>
              </article>
              <article class="preview-stat-card">
                <span>正文行数</span>
                <strong>{{ bodyStats.lines }}</strong>
              </article>
              <article class="preview-stat-card">
                <span>段落数量</span>
                <strong>{{ bodyStats.paragraphs }}</strong>
              </article>
            </section>
          </div>
        </aside>
      </div>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">{{ submitButtonText }}</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="externalRefDialogVisible"
      :title="editingExternalRefId ? '编辑外部引用' : '新增外部引用'"
      width="680px"
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
        <el-button @click="externalRefDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitExternalRef">
          {{ editingExternalRefId ? '保存修改' : '创建引用' }}
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="aiSourceDialogVisible"
      title="AI 来源整理"
      width="980px"
      class="ai-source-dialog"
    >
      <div class="editor-guide ai-source-guide">
        <span class="sidebar-kicker">AI Summary Layer</span>
        <h4>人工提供来源，AI Provider 生成导读</h4>
        <p>
          这里不做全网爬虫，也不全文转载原文。你输入原始链接、摘要和关键摘录，后端用
          百炼或 MiMo 生成 AI 导读，确认后创建为草稿。
        </p>
        <div class="ai-source-steps">
          <span :class="{ 'is-active': aiSourceStep === '填写来源' }">1. 填写来源</span>
          <span :class="{ 'is-active': aiSourceStep === '生成中' }">2. 生成导读</span>
          <span :class="{ 'is-active': aiSourceStep === '确认结果' }">3. 确认结果</span>
        </div>
      </div>

      <div class="ai-template-strip">
        <button
          v-for="(template, key) in AI_SOURCE_TEMPLATES"
          :key="key"
          type="button"
          class="ai-template-card"
          :class="{ 'is-active': aiSourceTemplateKey === key }"
          @click="applyAiSourceTemplate(key)"
        >
          <strong>{{ template.label }}</strong>
          <span>{{ template.sourceName }}</span>
        </button>
      </div>

      <div class="ai-provider-strip">
        <span class="ai-provider-label">AI Provider</span>
        <el-radio-group v-model="aiSourceForm.provider" size="small">
          <el-radio-button value="bailian">百炼</el-radio-button>
          <el-radio-button value="mimo">MiMo</el-radio-button>
        </el-radio-group>
        <span class="ai-provider-hint">
          {{ aiSourceForm.provider === 'mimo' ? 'MiMo Token Plan 临时额度，后续可能替换' : '阿里百炼 / DashScope，当前主路径' }}
        </span>
      </div>

      <div class="ai-source-layout">
        <section>
          <el-form
            ref="aiSourceFormRef"
            :model="aiSourceForm"
            :rules="aiSourceRules"
            label-width="112px"
          >
            <div class="form-grid">
              <el-form-item label="来源标题" prop="sourceTitle">
                <el-input v-model="aiSourceForm.sourceTitle" />
              </el-form-item>
              <el-form-item label="来源链接" prop="sourceUrl">
                <el-input v-model="aiSourceForm.sourceUrl" />
              </el-form-item>
              <el-form-item label="来源名称">
                <el-input v-model="aiSourceForm.sourceName" placeholder="例如 OpenAI Blog / Arena / GitHub Trending" />
              </el-form-item>
              <el-form-item label="来源类型">
                <el-select v-model="aiSourceForm.sourceType">
                  <el-option
                    v-for="item in aiSourceTypeOptions"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="内容类型" prop="contentType">
                <el-select v-model="aiSourceForm.contentType">
                  <el-option
                    v-for="item in contentOptions.contentTypes"
                    :key="item.value"
                    :label="item.label"
                    :value="item.value"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="分类" prop="categoryId">
                <el-select v-model="aiSourceForm.categoryId">
                  <el-option
                    v-for="item in contentOptions.categories"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="绑定来源">
                <el-select v-model="aiSourceForm.sourceId" clearable>
                  <el-option
                    v-for="item in contentOptions.sources"
                    :key="item.id"
                    :label="`${item.name} · ${item.sourceType}`"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="图片链接">
                <el-input v-model="aiSourceForm.imageUrl" placeholder="可选，保存为封面图和来源图片" />
              </el-form-item>
              <el-form-item label="标签">
                <el-select v-model="aiSourceForm.tagIds" multiple clearable collapse-tags>
                  <el-option
                    v-for="item in contentOptions.tags"
                    :key="item.id"
                    :label="item.name"
                    :value="item.id"
                  />
                </el-select>
              </el-form-item>
            </div>

            <el-form-item label="原文摘要" prop="originalSummary">
              <el-input
                v-model="aiSourceForm.originalSummary"
                type="textarea"
                :rows="4"
                placeholder="用自己的话记录原网站/原文的主要信息，不需要复制全文"
              />
            </el-form-item>
            <el-form-item label="关键摘录">
              <el-input
                v-model="aiSourceForm.originalExcerpt"
                type="textarea"
                :rows="3"
                placeholder="可选：记录 1-2 句关键摘录或重要数据点"
              />
            </el-form-item>
            <el-form-item label="整理要求">
              <el-input
                v-model="aiSourceForm.instruction"
                type="textarea"
                :rows="2"
                placeholder="例如：偏工程实践、偏科研动态、偏产品观察"
              />
            </el-form-item>
          </el-form>
        </section>

        <aside class="ai-source-result">
          <div class="preview-check-head">
            <div>
              <span class="sidebar-kicker">AI Draft</span>
              <h4>生成结果预览</h4>
            </div>
            <strong>{{ aiSourceGenerated?.importanceScore || '-' }}</strong>
          </div>

          <div v-if="aiSourceGenerated" class="ai-result-card">
            <span>建议标题</span>
            <h4>{{ aiSourceGenerated.suggestedTitle }}</h4>
            <p>{{ aiSourceGenerated.summary }}</p>
            <div class="ai-result-metric">
              <strong>推荐理由</strong>
              <span>{{ aiSourceGenerated.recommendationReason }}</span>
            </div>
            <div class="preview-tag-row">
              <span
                v-for="tag in aiSourceGenerated.tagSuggestions"
                :key="tag"
                class="preview-tag"
              >
                {{ tag }}
              </span>
            </div>
            <div class="ai-tag-match-panel">
              <div>
                <strong>已匹配到标签</strong>
                <div class="preview-tag-row">
                  <span
                    v-for="item in aiMatchedTags"
                    :key="item.tag.id"
                    class="preview-tag"
                  >
                    {{ item.tag.name }}
                  </span>
                  <span v-if="!aiMatchedTags.length" class="preview-tag preview-tag--muted">暂无匹配</span>
                </div>
              </div>
              <div>
                <strong>未匹配建议</strong>
                <div class="preview-tag-row">
                  <span
                    v-for="tag in aiUnmatchedTagSuggestions"
                    :key="tag"
                    class="preview-tag preview-tag--muted"
                  >
                    {{ tag }}
                  </span>
                  <span v-if="!aiUnmatchedTagSuggestions.length" class="preview-tag preview-tag--muted">全部已匹配</span>
                </div>
              </div>
              <div>
                <strong>当前已选标签</strong>
                <div class="preview-tag-row">
                  <span
                    v-for="tag in aiSelectedTags"
                    :key="tag.id"
                    class="preview-tag"
                  >
                    {{ tag.name }}
                  </span>
                  <span v-if="!aiSelectedTags.length" class="preview-tag preview-tag--muted">未选择标签</span>
                </div>
              </div>
            </div>
          </div>
          <div v-else class="ai-result-empty">
            <strong>还没有生成 AI 导读</strong>
            <p>先填写左侧来源信息，然后点击“生成 AI 导读”。如果后端未配置 DASHSCOPE_API_KEY，这里会给出明确提示。</p>
          </div>
        </aside>
      </div>

      <template #footer>
        <el-button @click="aiSourceDialogVisible = false">取消</el-button>
        <el-button :loading="aiSourceGenerating" @click="generateAiSourceSummary">生成 AI 导读</el-button>
        <el-button :disabled="!aiSourceGenerated" @click="fillContentFormFromAiSource">
          回填到编辑表单
        </el-button>
        <el-button type="primary" :disabled="!aiSourceGenerated" @click="submitAiSourceImport">
          创建草稿
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="arxivImportDialogVisible"
      title="arXiv 论文导入"
      width="820px"
      class="github-import-dialog"
    >
      <div class="editor-guide github-import-guide">
        <span class="sidebar-kicker">arXiv API</span>
        <h4>搜索 arXiv 论文，导入为平台内容</h4>
        <p>
          通过 arXiv API 搜索 AI 相关论文，选择候选后回填表单，再导入为草稿内容和外部引用记录。
        </p>
      </div>

      <div class="github-api-panel">
        <strong>搜索 arXiv 论文</strong>
        <div class="github-query-row">
          <el-input v-model="arxivSearchForm.query" placeholder="例如 cat:cs.AI / transformer attention / au:Hinton" />
          <el-input-number v-model="arxivSearchForm.maxResults" :min="1" :max="50" />
          <el-button :loading="arxivSearchLoading" type="primary" @click="searchArxiv">搜索</el-button>
        </div>
      </div>

      <div v-if="arxivSearchResults.length" class="github-candidate-list">
        <article
          v-for="paper in arxivSearchResults"
          :key="paper.arxivId"
          class="github-candidate-card"
        >
          <div>
            <strong>{{ paper.title }}</strong>
            <p>{{ paper.abstractText }}</p>
            <span>{{ paper.arxivId }} · {{ (paper.authors || []).join(', ') }}</span>
          </div>
          <el-button size="small" @click="applyArxivCandidate(paper)">回填</el-button>
        </article>
      </div>

      <el-form
        ref="arxivImportFormRef"
        :model="arxivImportForm"
        label-width="112px"
      >
        <div class="form-grid">
          <el-form-item label="arXiv ID" prop="arxivId" :rules="[{ required: true, message: 'arXiv ID 不能为空' }]">
            <el-input v-model="arxivImportForm.arxivId" placeholder="例如 2501.00001" />
          </el-form-item>
          <el-form-item label="论文标题" prop="title" :rules="[{ required: true, message: '论文标题不能为空' }]">
            <el-input v-model="arxivImportForm.title" />
          </el-form-item>
          <el-form-item label="作者">
            <el-input v-model="arxivImportForm.authors" placeholder="作者名用逗号分隔" />
          </el-form-item>
          <el-form-item label="PDF 链接">
            <el-input v-model="arxivImportForm.pdfUrl" placeholder="https://arxiv.org/pdf/..." />
          </el-form-item>
          <el-form-item label="分类" prop="categoryId" :rules="[{ required: true, message: '请选择分类' }]">
            <el-select v-model="arxivImportForm.categoryId">
              <el-option
                v-for="item in contentOptions.categories"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="来源">
            <el-select v-model="arxivImportForm.sourceId" clearable>
              <el-option
                v-for="item in contentOptions.sources"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标签">
            <el-select v-model="arxivImportForm.tagIds" multiple clearable>
              <el-option
                v-for="item in contentOptions.tags"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="发布状态">
            <el-select v-model="arxivImportForm.publishStatus">
              <el-option
                v-for="item in contentOptions.publishStatuses"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="论文摘要" prop="abstractText" :rules="[{ required: true, message: '论文摘要不能为空' }]">
          <el-input v-model="arxivImportForm.abstractText" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="arxivImportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitArxivImport">导入为内容</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="huggingfaceImportDialogVisible"
      title="HuggingFace 每日论文导入"
      width="820px"
      class="github-import-dialog"
    >
      <div class="editor-guide github-import-guide">
        <span class="sidebar-kicker">HuggingFace Papers</span>
        <h4>浏览 HuggingFace 每日热门论文，导入为平台内容</h4>
        <p>
          从 HuggingFace 每日论文推荐中选择热门论文，导入为平台内容。候选数据包含社区点赞数和评论数。
        </p>
      </div>

      <div v-loading="huggingfaceSearchLoading" class="github-candidate-list">
        <article
          v-for="paper in huggingfaceSearchResults"
          :key="paper.paperId"
          class="github-candidate-card"
        >
          <div>
            <strong>{{ paper.title }}</strong>
            <p>{{ paper.abstractText }}</p>
            <span>{{ paper.paperId }} · Likes {{ paper.likes || 0 }} · Comments {{ paper.comments || 0 }}</span>
          </div>
          <el-button size="small" @click="applyHuggingFaceCandidate(paper)">回填</el-button>
        </article>
      </div>

      <el-form
        ref="huggingfaceImportFormRef"
        :model="huggingfaceImportForm"
        label-width="112px"
      >
        <div class="form-grid">
          <el-form-item label="论文 ID" prop="paperId" :rules="[{ required: true, message: '论文 ID 不能为空' }]">
            <el-input v-model="huggingfaceImportForm.paperId" placeholder="例如 2401.01234" />
          </el-form-item>
          <el-form-item label="论文标题" prop="title" :rules="[{ required: true, message: '论文标题不能为空' }]">
            <el-input v-model="huggingfaceImportForm.title" />
          </el-form-item>
          <el-form-item label="作者">
            <el-input v-model="huggingfaceImportForm.authors" placeholder="作者名用逗号分隔" />
          </el-form-item>
          <el-form-item label="HF 链接">
            <el-input v-model="huggingfaceImportForm.htmlUrl" placeholder="https://huggingface.co/papers/..." />
          </el-form-item>
          <el-form-item label="点赞数">
            <el-input-number v-model="huggingfaceImportForm.likes" :min="0" />
          </el-form-item>
          <el-form-item label="评论数">
            <el-input-number v-model="huggingfaceImportForm.comments" :min="0" />
          </el-form-item>
          <el-form-item label="分类" prop="categoryId" :rules="[{ required: true, message: '请选择分类' }]">
            <el-select v-model="huggingfaceImportForm.categoryId">
              <el-option
                v-for="item in contentOptions.categories"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="来源">
            <el-select v-model="huggingfaceImportForm.sourceId" clearable>
              <el-option
                v-for="item in contentOptions.sources"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标签">
            <el-select v-model="huggingfaceImportForm.tagIds" multiple clearable>
              <el-option
                v-for="item in contentOptions.tags"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="发布状态">
            <el-select v-model="huggingfaceImportForm.publishStatus">
              <el-option
                v-for="item in contentOptions.publishStatuses"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="论文摘要" prop="abstractText" :rules="[{ required: true, message: '论文摘要不能为空' }]">
          <el-input v-model="huggingfaceImportForm.abstractText" type="textarea" :rows="4" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="huggingfaceImportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitHuggingFaceImport">导入为内容</el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="githubImportDialogVisible"
      title="GitHub 项目手动导入"
      width="760px"
      class="github-import-dialog"
    >
      <div class="editor-guide github-import-guide">
        <span class="sidebar-kicker">GitHub REST API</span>
        <h4>查询真实仓库，再转换成平台内容</h4>
        <p>
          现在可以通过 GitHub API 查询公开仓库或搜索候选项目。API 设置页配置 GitHub Token 后额度更高；
          不配置也可以匿名查询公开仓库。
        </p>
      </div>

      <div class="github-api-panel">
        <section class="github-query-card">
          <span class="sidebar-kicker">Repo Lookup</span>
          <h4>按仓库全名查询</h4>
          <div class="github-inline-row">
            <el-input v-model="githubImportForm.repoFullName" placeholder="owner/repo" />
            <el-button :loading="githubQueryLoading" type="primary" @click="queryGitHubRepository">
              查询仓库
            </el-button>
          </div>
        </section>

        <section class="github-query-card">
          <span class="sidebar-kicker">Search</span>
          <h4>关键词搜索候选仓库</h4>
          <div class="github-inline-row">
            <el-input v-model="githubSearchForm.keyword" placeholder="例如 llm agent / rag / inference" />
            <el-select v-model="githubSearchForm.sort">
              <el-option label="按 Star" value="stars" />
              <el-option label="按更新" value="updated" />
              <el-option label="按 Fork" value="forks" />
            </el-select>
            <el-button :loading="githubSearchLoading" @click="searchGitHubRepositories">搜索</el-button>
          </div>
        </section>
      </div>

      <div v-if="githubSearchResults.length" class="github-candidate-list">
        <article
          v-for="repo in githubSearchResults"
          :key="repo.repoFullName"
          class="github-candidate-card"
        >
          <div>
            <span>{{ repo.language || 'Unknown' }} · ★ {{ repo.stars }}</span>
            <strong>{{ repo.repoFullName }}</strong>
            <p>{{ repo.description || '该仓库暂无简介。' }}</p>
          </div>
          <el-button size="small" @click="applyGitHubCandidate(repo)">回填</el-button>
        </article>
      </div>

      <el-form
        ref="githubImportFormRef"
        :model="githubImportForm"
        :rules="githubImportRules"
        label-width="112px"
      >
        <div class="form-grid">
          <el-form-item label="仓库全名" prop="repoFullName">
            <el-input v-model="githubImportForm.repoFullName" placeholder="owner/repo" />
          </el-form-item>
          <el-form-item label="仓库链接" prop="repoUrl">
            <el-input v-model="githubImportForm.repoUrl" placeholder="https://github.com/owner/repo" />
          </el-form-item>
          <el-form-item label="Star 数">
            <el-input-number v-model="githubImportForm.stars" :min="0" />
          </el-form-item>
          <el-form-item label="主要语言">
            <el-input v-model="githubImportForm.language" placeholder="Python / TypeScript / Jupyter Notebook" />
          </el-form-item>
          <el-form-item label="Topics">
            <el-input v-model="githubImportForm.topics" placeholder="ai,llm,rag,agent" />
          </el-form-item>
          <el-form-item label="项目主页">
            <el-input v-model="githubImportForm.homepage" placeholder="可选，例如 https://example.com" />
          </el-form-item>
          <el-form-item label="分类" prop="categoryId">
            <el-select v-model="githubImportForm.categoryId">
              <el-option
                v-for="item in contentOptions.categories"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="来源">
            <el-select v-model="githubImportForm.sourceId" clearable>
              <el-option
                v-for="item in contentOptions.sources"
                :key="item.id"
                :label="`${item.name} · ${item.sourceType}`"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="状态" prop="publishStatus">
            <el-select v-model="githubImportForm.publishStatus">
              <el-option
                v-for="item in contentOptions.publishStatuses"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="标签">
            <el-select v-model="githubImportForm.tagIds" multiple clearable collapse-tags>
              <el-option
                v-for="item in contentOptions.tags"
                :key="item.id"
                :label="item.name"
                :value="item.id"
              />
            </el-select>
          </el-form-item>
        </div>

        <el-form-item label="项目简介" prop="description">
          <el-input
            v-model="githubImportForm.description"
            type="textarea"
            :rows="4"
            placeholder="用 1-2 句话说明这个项目为什么值得关注"
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="githubImportDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitGitHubImport">导入为内容</el-button>
      </template>
    </el-dialog>
  </section>
</template>
