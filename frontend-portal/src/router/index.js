import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ContentDetailView from '../views/ContentDetailView.vue'
import SectionPlaceholderView from '../views/SectionPlaceholderView.vue'

const sections = [
  {
    path: '/papers',
    name: 'papers',
    title: '论文速递',
    subtitle: '最新 AI 论文与研究动态',
    emoji: '📄',
    description: '追踪 arXiv、HuggingFace Daily Papers 等来源的最新论文，聚焦 CT 去噪、医学影像、扩散模型等研究方向。'
  },
  {
    path: '/github',
    name: 'github',
    title: 'GitHub 开源项目',
    subtitle: '热门 AI 开源项目追踪',
    emoji: '🐙',
    description: '聚合 GitHub Trending 上的 AI 相关项目，关注 Agent、RAG、推理优化等方向的工程实践。'
  },
  {
    path: '/news',
    name: 'news',
    title: 'AI 资讯',
    subtitle: '全球 AI 行业动态',
    emoji: '📰',
    description: '来自 OpenAI、Google、DeepMind 等公司的官方动态，以及 AI 领域的重要新闻。'
  },
  {
    path: '/company',
    name: 'company',
    title: 'AI 公司动态',
    subtitle: '大模型公司与产品更新',
    emoji: '🏢',
    description: '追踪 OpenAI、Anthropic、DeepSeek、Google 等公司的产品发布、融资和战略动向。'
  },
  {
    path: '/tools',
    name: 'tools',
    title: '工具与最佳实践',
    subtitle: 'AI 工具与编码实践',
    emoji: '🔧',
    description: '来自 X、Reddit、Hacker News 等平台分享的 AI 工具、编码工具和工程最佳实践。'
  },
  {
    path: '/arena',
    name: 'arena',
    title: 'Arena 模型评测',
    subtitle: '大模型评测与排行榜',
    emoji: '🏆',
    description: '整合 LMSYS Arena、OpenCompass 等评测平台数据，追踪主流模型的能力变化。'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/contents/:id', name: 'content-detail', component: ContentDetailView },
    ...sections.map(s => ({
      path: s.path,
      name: s.name,
      component: SectionPlaceholderView,
      props: {
        title: s.title,
        subtitle: s.subtitle,
        emoji: s.emoji,
        description: s.description
      }
    }))
  ]
})

export { sections }
export default router
