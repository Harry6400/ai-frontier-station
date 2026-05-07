<script setup>
import { onMounted, ref } from 'vue'
import PortalTopbar from '../components/PortalTopbar.vue'
import ContentCard from '../components/ContentCard.vue'
import { usePortalStore } from '../stores/usePortalStore'
import { getPortalHome } from '../api/portal'

const portalStore = usePortalStore()
const loading = ref(false)
const errorMessage = ref('')
const latestCards = ref([])

async function loadHome() {
  loading.value = true
  errorMessage.value = ''
  try {
    const res = await getPortalHome()
    latestCards.value = res.data.latestContents || []
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
      <section class="section-shell hero-stage hero-stage--compact">
        <div class="hero-copy-block">
          <span class="eyebrow-line">AI intelligence portal · personal workspace</span>
          <h1>把分散的一手信息整理成一个可持续使用的 AI 内容工作台。</h1>
          <p class="lead-copy">
            {{ portalStore.projectName }} 现在先专注三件事：收集可信来源、沉淀摘要与标签、保留原始链接。
            少一点装饰，多一点真正能复用的信息。
          </p>
        </div>

        <div class="hero-actions">
          <RouterLink class="primary-btn" to="/contents">进入内容广场</RouterLink>
          <RouterLink class="secondary-btn" to="/about">查看项目结构</RouterLink>
        </div>
      </section>

      <section class="section-shell">
        <div class="section-head">
          <div>
            <span class="section-kicker">Recent Notes</span>
            <h2>最近整理</h2>
          </div>
          <RouterLink class="card-link" to="/contents">查看全部</RouterLink>
        </div>

        <div v-if="loading || errorMessage" class="status-panel">
          <strong>{{ loading ? '正在加载首页内容' : '首页加载失败' }}</strong>
          <p>{{ loading ? '正在从后端读取最近内容。' : errorMessage }}</p>
        </div>

        <div v-else-if="latestCards.length" class="card-grid card-grid--listing">
          <ContentCard
            v-for="card in latestCards.slice(0, 6)"
            :key="card.id"
            :item="card"
          />
        </div>

        <div v-else class="empty-state-panel">
          <span class="section-kicker">No Content</span>
          <h3>当前还没有公开内容</h3>
          <p>可以先在后台创建草稿，确认后再发布到前台。</p>
        </div>
      </section>
    </main>
  </div>
</template>
