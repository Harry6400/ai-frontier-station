<script setup>
import { computed, onMounted, ref } from 'vue'
import PortalTopbar from '../components/PortalTopbar.vue'
import ContentCard from '../components/ContentCard.vue'
import { usePortalStore } from '../stores/usePortalStore'
import { getPortalHome } from '../api/portal'
import { formatDateTime, getContentTypeMeta } from '../utils/content'

const portalStore = usePortalStore()
const loading = ref(false)
const errorMessage = ref('')
const featuredCards = ref([])
const latestCards = ref([])
const categories = ref([])
const sources = ref([])

const metrics = computed(() => [
  { label: '精选内容', value: featuredCards.value.length, note: '由后台控制精选级别' },
  { label: '当前上线', value: latestCards.value.length, note: '门户已发布内容' },
  { label: '栏目骨架', value: categories.value.length, note: '为后续扩展预留' }
])

const pulseItems = computed(() => latestCards.value.slice(0, 4))
const heroLeadItem = computed(() => featuredCards.value[0] || latestCards.value[0] || null)
const heroSecondaryItems = computed(() => {
  const bucket = [...featuredCards.value.slice(1), ...latestCards.value]
  const unique = []
  const seen = new Set()
  for (const item of bucket) {
    if (!item || seen.has(item.id) || item.id === heroLeadItem.value?.id) {
      continue
    }
    seen.add(item.id)
    unique.push(item)
    if (unique.length === 2) {
      break
    }
  }
  return unique
})
const sourceConstellation = computed(() => sources.value.slice(0, 5))

const architecturePoints = [
  {
    title: '内容模型统一',
    description: '项目、论文、公司动态、技术实践统一进入内容中心，避免未来扩展时拆散前台展示逻辑。'
  },
  {
    title: '来源与标签解耦',
    description: '来源表、标签表独立存在，后续可以继续接采集、推荐、专题与统计模块。'
  },
  {
    title: '前后台双端分离',
    description: '前台承担品牌和阅读体验，后台承担内容运营，两边都更容易讲清楚。'
  }
]

async function loadHome() {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getPortalHome()
    featuredCards.value = res.data.featuredContents || []
    latestCards.value = res.data.latestContents || []
    categories.value = res.data.categories || []
    sources.value = res.data.sources || []
  } catch (error) {
    errorMessage.value = error.message
  } finally {
    loading.value = false
  }
}

onMounted(loadHome)
</script>

<template>
  <div class="portal-shell">
    <PortalTopbar context-label="Personal Workspace" context-value="AI 信息发现与精选" />

    <main class="page-stack">
      <section class="hero-grid">
        <section class="section-shell hero-stage">
          <div class="hero-copy-block">
            <span class="eyebrow-line">AI intelligence portal · personal workspace</span>
            <h1>把分散的一手信息整理成一个可持续使用的 AI 内容工作台。</h1>
            <p class="lead-copy">
              {{ portalStore.projectName }} 当前已经跑通真实数据流，前台负责信息发现、
              内容编排和阅读体验，后续可以继续向 GitHub、论文、公司动态与技术实践聚合平台扩展。
            </p>
          </div>

          <div class="hero-actions">
            <RouterLink class="primary-btn" to="/contents">进入内容广场</RouterLink>
            <RouterLink class="secondary-btn" to="/about">查看项目结构</RouterLink>
          </div>

          <div class="hero-stage-bottom">
            <div class="metric-ribbon">
              <article v-for="item in metrics" :key="item.label" class="metric-cell">
                <span>{{ item.label }}</span>
                <strong>{{ item.value }}</strong>
                <p>{{ item.note }}</p>
              </article>
            </div>

            <article class="hero-brief">
              <span class="section-kicker">Portal Thesis</span>
              <p>
                这一版首页不只是在展示数据，而是在前台直接呈现“内容平台如何组织信息、如何表达信号价值”。
              </p>
            </article>
          </div>
        </section>

        <aside class="section-shell signal-stage">
          <div class="section-head section-head--tight">
            <div>
              <span class="section-kicker">Signal Deck</span>
              <h2>最新情报的前台编排</h2>
            </div>
            <span class="micro-note">借鉴现代产品首页的信号面板思路</span>
          </div>

          <div v-if="loading || errorMessage" class="status-panel">
            <strong>{{ loading ? '正在加载首页内容' : '首页加载失败' }}</strong>
            <p>{{ loading ? '正在从后端获取精选内容与栏目结构。' : errorMessage }}</p>
          </div>

          <div v-else class="signal-stage-body">
            <article v-if="heroLeadItem" class="signal-card signal-card--focus">
              <div class="signal-card-top">
                <span class="content-chip">{{ getContentTypeMeta(heroLeadItem.contentType).label }}</span>
                <span class="micro-note">{{ formatDateTime(heroLeadItem.publishedAt) }}</span>
              </div>
              <h3>{{ heroLeadItem.title }}</h3>
              <p>{{ heroLeadItem.summary }}</p>
              <RouterLink class="card-link" :to="`/contents/${heroLeadItem.id}`">
                查看这条信号
              </RouterLink>
            </article>

            <div class="signal-subgrid">
              <RouterLink
                v-for="item in heroSecondaryItems"
                :key="item.id"
                class="signal-card signal-card--compact"
                :to="`/contents/${item.id}`"
              >
                <span class="micro-note">
                  {{ getContentTypeMeta(item.contentType).label }} · {{ formatDateTime(item.publishedAt) }}
                </span>
                <strong>{{ item.title }}</strong>
              </RouterLink>
            </div>

            <div class="source-constellation">
              <span class="section-kicker">Trusted Sources</span>
              <div class="source-constellation-grid">
                <span
                  v-for="item in sourceConstellation"
                  :key="item.id"
                  class="source-pill"
                >
                  {{ item.name }}
                </span>
              </div>
            </div>

            <div class="pulse-list">
              <RouterLink
                v-for="(item, index) in pulseItems"
                :key="item.id"
                class="pulse-item"
                :to="`/contents/${item.id}`"
              >
                <span class="pulse-index">{{ String(index + 1).padStart(2, '0') }}</span>
                <div class="pulse-copy">
                  <strong>{{ item.title }}</strong>
                  <p>{{ getContentTypeMeta(item.contentType).label }} · {{ formatDateTime(item.publishedAt) }}</p>
                </div>
              </RouterLink>
            </div>
          </div>
        </aside>
      </section>

      <section class="section-shell">
        <div class="section-head">
          <div>
            <span class="section-kicker">Featured Selection</span>
            <h2>精选内容卡片</h2>
          </div>
          <p>这一组内容直接来自后台的精选级别设置，前台负责把它们编排成更像产品首页的内容序列。</p>
        </div>

        <div class="card-grid card-grid--featured">
          <ContentCard
            v-for="card in featuredCards"
            :key="card.id"
            :item="card"
            variant="featured"
          />
        </div>
      </section>

      <section class="editorial-grid">
        <section class="section-shell">
          <div class="section-head">
            <div>
              <span class="section-kicker">Category Map</span>
              <h2>栏目地图</h2>
            </div>
            <p>当前首页不只展示内容，也清楚呈现这个平台未来会如何扩展。</p>
          </div>

          <div class="category-board">
            <article v-for="(item, index) in categories" :key="item.id" class="category-tile">
              <span class="category-index">{{ String(index + 1).padStart(2, '0') }}</span>
              <div>
                <h3>{{ item.name }}</h3>
                <p>{{ item.description }}</p>
              </div>
            </article>
          </div>
        </section>

        <section class="section-shell">
          <div class="section-head">
            <div>
              <span class="section-kicker">System Logic</span>
              <h2>这版首页为什么更像系统</h2>
            </div>
            <p>不仅是视觉更完整，也在前台层面把内容平台的组织方式说清楚。</p>
          </div>

          <div class="architecture-list">
            <article v-for="item in architecturePoints" :key="item.title" class="architecture-item">
              <h3>{{ item.title }}</h3>
              <p>{{ item.description }}</p>
            </article>
          </div>

          <div class="soft-panel">
            <span class="section-kicker">Planned Types</span>
            <div class="soft-panel-tags">
              <span v-for="item in portalStore.contentTypes" :key="item" class="soft-pill">
                {{ item }}
              </span>
            </div>
          </div>
        </section>
      </section>
    </main>
  </div>
</template>
