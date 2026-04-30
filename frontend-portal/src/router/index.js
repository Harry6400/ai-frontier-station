import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import ContentListView from '../views/ContentListView.vue'
import ContentDetailView from '../views/ContentDetailView.vue'
import AboutView from '../views/AboutView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', name: 'home', component: HomeView },
    { path: '/contents', name: 'content-list', component: ContentListView },
    { path: '/contents/:id', name: 'content-detail', component: ContentDetailView },
    { path: '/about', name: 'about', component: AboutView }
  ]
})

export default router

