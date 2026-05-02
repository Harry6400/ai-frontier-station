import { createRouter, createWebHistory } from 'vue-router'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
    { path: '/', name: 'dashboard', component: () => import('../views/DashboardView.vue') },
    { path: '/contents', name: 'contents', component: () => import('../views/ContentManageView.vue') },
    { path: '/categories', name: 'categories', component: () => import('../views/CategoryManageView.vue') },
    { path: '/tags', name: 'tags', component: () => import('../views/TagManageView.vue') },
    { path: '/sources', name: 'sources', component: () => import('../views/SourceManageView.vue') },
    { path: '/api-settings', name: 'api-settings', component: () => import('../views/ApiSettingsView.vue') }
  ]
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('admin_token')
  if (to.name !== 'login' && !token) {
    next({ name: 'login' })
  } else if (to.name === 'login' && token) {
    next({ name: 'dashboard' })
  } else {
    next()
  }
})

export default router
