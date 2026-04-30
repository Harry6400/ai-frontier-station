<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import PortalTopbar from '../components/PortalTopbar.vue'
import ContentCard from '../components/ContentCard.vue'
import { getPortalContents, getPortalHome } from '../api/portal'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const errorMessage = ref('')
const contents = ref([])
const categories = ref([])
const sources = ref([])
const tags = ref([])
const pagination = reactive({
  pageNum: 1,
  pageSize: 6,
  total: 0
})
const filters = reactive({
  keyword: '',
  contentType: '',
  categoryId: '',
  sourceId: '',
  tagId: '',
  sortBy: 'latest'
})

const typeOptions = [
  { label: '资讯', value: 'news' },
  { label: '论文', value: 'paper' },
  { label: '项目', value: 'project' },
  { label: '公司动态', value: 'company_update' },
  { label: '技术实践', value: 'practice' }
]

const sortOptions = [
  { label: '最新发布', value: 'latest', note: '按发布时间倒序' },
  { label: '最多浏览', value: 'popular', note: '按浏览量排序' },
  { label: '精选优先', value: 'featured', note: '先看推荐内容' }
]

const typeLabelMap = Object.fromEntries(typeOptions.map((item) => [item.value, item.label]))
const sortLabelMap = Object.fromEntries(sortOptions.map((item) => [item.value, item.label]))

const selectedCategory = computed(() =>
  categories.value.find((item) => String(item.id) === String(filters.categoryId))
)
const selectedSource = computed(() =>
  sources.value.find((item) => String(item.id) === String(filters.sourceId))
)
const selectedTag = computed(() =>
  tags.value.find((item) => String(item.id) === String(filters.tagId))
)
const quickTags = computed(() => tags.value.slice(0, 8))

const activeFilters = computed(() => {
  const chips = []
  const keyword = filters.keyword.trim()

  if (keyword) {
    chips.push({ key: 'keyword', label: `关键词：${keyword}` })
  }
  if (filters.contentType) {
    chips.push({ key: 'contentType', label: `类型：${typeLabelMap[filters.contentType]}` })
  }
  if (selectedCategory.value) {
    chips.push({ key: 'categoryId', label: `分类：${selectedCategory.value.name}` })
  }
  if (selectedSource.value) {
    chips.push({ key: 'sourceId', label: `来源：${selectedSource.value.name}` })
  }
  if (selectedTag.value) {
    chips.push({ key: 'tagId', label: `标签：${selectedTag.value.name}` })
  }

  return chips
})

const hasActiveFilters = computed(() => activeFilters.value.length > 0)
const hasPrev = computed(() => pagination.pageNum > 1)
const hasNext = computed(() => pagination.pageNum * pagination.pageSize < pagination.total)
const totalPages = computed(() => Math.max(1, Math.ceil(pagination.total / pagination.pageSize)))
const currentSortLabel = computed(() => sortLabelMap[filters.sortBy] || sortLabelMap.latest)

const resultSummary = computed(() => {
  if (!pagination.total && hasActiveFilters.value) {
    return `按“${currentSortLabel.value}”排序时，没有找到匹配内容`
  }
  if (!pagination.total) {
    return '当前还没有公开内容'
  }
  if (!hasActiveFilters.value) {
    return `正在展示全部已发布内容，共 ${pagination.total} 条，排序：${currentSortLabel.value}`
  }
  return `命中 ${pagination.total} 条结果，已应用 ${activeFilters.value.length} 个筛选条件，排序：${currentSortLabel.value}`
})

const emptyStateHint = computed(() => {
  if (filters.keyword.trim()) {
    return '可以尝试缩短关键词，或先清空关键词后按类型、标签浏览。'
  }
  if (filters.tagId) {
    return '当前标签下内容较少，可以移除标签或切换到 GitHub、RAG、Agent 等其他标签。'
  }
  if (filters.categoryId || filters.contentType || filters.sourceId) {
    return '可以放宽分类、类型或来源条件，查看更大的内容集合。'
  }
  return '当前还没有公开内容，可以先去后台发布一条内容。'
})

function normalizeSort(value) {
  return sortOptions.some((item) => item.value === value) ? value : 'latest'
}

function readStateFromQuery() {
  filters.keyword = typeof route.query.keyword === 'string' ? route.query.keyword : ''
  filters.contentType = typeof route.query.contentType === 'string' ? route.query.contentType : ''
  filters.categoryId = typeof route.query.categoryId === 'string' ? route.query.categoryId : ''
  filters.sourceId = typeof route.query.sourceId === 'string' ? route.query.sourceId : ''
  filters.tagId = typeof route.query.tagId === 'string' ? route.query.tagId : ''
  filters.sortBy = normalizeSort(typeof route.query.sortBy === 'string' ? route.query.sortBy : 'latest')
  const pageNum = Number(route.query.pageNum || 1)
  pagination.pageNum = Number.isFinite(pageNum) && pageNum > 0 ? pageNum : 1
}

function buildQuery() {
  const query = {}
  if (filters.keyword.trim()) {
    query.keyword = filters.keyword.trim()
  }
  if (filters.contentType) {
    query.contentType = filters.contentType
  }
  if (filters.categoryId) {
    query.categoryId = filters.categoryId
  }
  if (filters.sourceId) {
    query.sourceId = filters.sourceId
  }
  if (filters.tagId) {
    query.tagId = filters.tagId
  }
  if (filters.sortBy && filters.sortBy !== 'latest') {
    query.sortBy = filters.sortBy
  }
  if (pagination.pageNum > 1) {
    query.pageNum = String(pagination.pageNum)
  }
  return query
}

function syncQuery() {
  router.replace({ path: '/contents', query: buildQuery() })
}

async function loadFilterOptions() {
  const res = await getPortalHome()
  categories.value = res.data.categories || []
  sources.value = res.data.sources || []
  tags.value = res.data.tags || []
}

async function loadContents() {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getPortalContents({
      pageNum: pagination.pageNum,
      pageSize: pagination.pageSize,
      keyword: filters.keyword.trim() || undefined,
      contentType: filters.contentType || undefined,
      categoryId: filters.categoryId || undefined,
      sourceId: filters.sourceId || undefined,
      tagId: filters.tagId || undefined,
      sortBy: filters.sortBy || 'latest'
    })
    contents.value = res.data.records || []
    pagination.total = res.data.total || 0
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

function search() {
  pagination.pageNum = 1
  syncQuery()
}

function clearFilters() {
  filters.keyword = ''
  filters.contentType = ''
  filters.categoryId = ''
  filters.sourceId = ''
  filters.tagId = ''
  filters.sortBy = 'latest'
  pagination.pageNum = 1
  syncQuery()
}

function removeFilter(key) {
  filters[key] = ''
  pagination.pageNum = 1
  syncQuery()
}

function selectTag(tagId) {
  filters.tagId = String(tagId)
  pagination.pageNum = 1
  syncQuery()
}

function changeSort() {
  pagination.pageNum = 1
  syncQuery()
}

function prevPage() {
  if (!hasPrev.value) {
    return
  }
  pagination.pageNum -= 1
  syncQuery()
}

function nextPage() {
  if (!hasNext.value) {
    return
  }
  pagination.pageNum += 1
  syncQuery()
}

async function initialize() {
  try {
    readStateFromQuery()
    await loadFilterOptions()
    await loadContents()
  } catch (error) {
    errorMessage.value = error.message
  }
}

watch(
  () => route.query,
  async () => {
    readStateFromQuery()
    await loadContents()
  }
)

onMounted(initialize)
</script>

<template>
  <div class="portal-shell">
    <PortalTopbar context-label="Result Set" :context-value="`${pagination.total} 条内容`" />

    <main class="page-stack">
      <section class="filter-stage">
        <section class="section-shell filter-hero">
          <span class="eyebrow-line">Search & Browse</span>
          <h1>内容广场</h1>
          <p class="lead-copy">
            这里已经接入真实已发布内容，可以按标题、类型、分类、来源和标签筛选。排序和筛选条件会同步到地址栏，
            刷新页面后仍能保持当前检索状态。
          </p>
        </section>

        <section class="section-shell filter-rack-panel">
          <div class="section-head section-head--tight">
            <div>
              <span class="section-kicker">Filters</span>
              <h2>筛选条件</h2>
            </div>
            <span class="micro-note">
              {{ hasActiveFilters ? `已启用 ${activeFilters.length} 个筛选条件` : '前台检索入口' }}
            </span>
          </div>

          <div class="filter-rack">
            <input
              v-model="filters.keyword"
              class="filter-input"
              placeholder="搜索标题关键词"
              @keyup.enter="search"
            />

            <div class="filter-field-row filter-field-row--four">
              <select v-model="filters.contentType" class="filter-select">
                <option value="">全部类型</option>
                <option v-for="item in typeOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
              <select v-model="filters.categoryId" class="filter-select">
                <option value="">全部分类</option>
                <option v-for="item in categories" :key="item.id" :value="item.id">
                  {{ item.name }}
                </option>
              </select>
              <select v-model="filters.sourceId" class="filter-select">
                <option value="">全部来源</option>
                <option v-for="item in sources" :key="item.id" :value="item.id">
                  {{ item.name }}
                </option>
              </select>
              <select v-model="filters.sortBy" class="filter-select" @change="changeSort">
                <option v-for="item in sortOptions" :key="item.value" :value="item.value">
                  {{ item.label }}
                </option>
              </select>
            </div>

            <div v-if="quickTags.length" class="quick-tag-panel">
              <span class="micro-note">热门标签</span>
              <button
                v-for="tag in quickTags"
                :key="tag.id"
                type="button"
                class="quick-tag-chip"
                :class="{ 'is-active': String(filters.tagId) === String(tag.id) }"
                @click="selectTag(tag.id)"
              >
                {{ tag.name }}
              </button>
            </div>

            <div class="filter-action-row">
              <button class="primary-btn" @click="search">查询</button>
              <button class="secondary-btn" @click="clearFilters">清空</button>
              <span class="micro-note">当前排序：{{ currentSortLabel }}</span>
            </div>
          </div>

          <div v-if="hasActiveFilters" class="active-filter-bar">
            <span class="micro-note">当前条件</span>
            <button
              v-for="item in activeFilters"
              :key="item.key"
              type="button"
              class="active-filter-chip"
              @click="removeFilter(item.key)"
            >
              {{ item.label }} ×
            </button>
          </div>
        </section>
      </section>

      <section v-if="loading || errorMessage" class="section-shell">
        <div class="status-panel">
          <strong>{{ loading ? '正在加载内容列表' : '列表加载失败' }}</strong>
          <p>{{ loading ? '后端内容接口正在返回真实数据。' : errorMessage }}</p>
        </div>
      </section>

      <section class="section-shell">
        <div class="results-strip">
          <div>
            <span class="section-kicker">Result View</span>
            <h2>当前检索结果</h2>
          </div>
          <div class="results-summary">
            <strong>{{ resultSummary }}</strong>
            <p class="micro-note">
              第 {{ pagination.pageNum }} / {{ totalPages }} 页
            </p>
          </div>
        </div>

        <div v-if="contents.length" class="card-grid card-grid--listing">
          <ContentCard v-for="item in contents" :key="item.id" :item="item" />
        </div>

        <div v-else-if="!loading && !errorMessage" class="empty-state-panel">
          <span class="section-kicker">No Match</span>
          <h3>这一组条件下还没有检索到内容</h3>
          <p>{{ emptyStateHint }}</p>
          <button class="secondary-btn" @click="clearFilters">重置筛选</button>
        </div>

        <div class="pagination-bar">
          <button class="secondary-btn" :disabled="!hasPrev" @click="prevPage">上一页</button>
          <span>第 {{ pagination.pageNum }} 页</span>
          <button class="secondary-btn" :disabled="!hasNext" @click="nextPage">下一页</button>
        </div>
      </section>
    </main>
  </div>
</template>
