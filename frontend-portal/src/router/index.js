import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ContentDetailView from '../views/ContentDetailView.vue'
import PapersView from '../views/PapersView.vue'
import GithubView from '../views/GithubView.vue'
import NewsView from '../views/NewsView.vue'
import CompanyView from '../views/CompanyView.vue'
import ArenaView from '../views/ArenaView.vue'
import ToolsView from '../views/ToolsView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/contents/:id', name: 'content-detail', component: ContentDetailView },
    { path: '/papers', name: 'papers', component: PapersView },
    { path: '/github', name: 'github', component: GithubView },
    { path: '/news', name: 'news', component: NewsView },
    { path: '/company', name: 'company', component: CompanyView },
    { path: '/arena', name: 'arena', component: ArenaView },
    { path: '/tools', name: 'tools', component: ToolsView },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/NotFoundView.vue')
    }
  ],
  scrollBehavior() {
    return { top: 0 }
  }
})

export default router
