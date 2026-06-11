import { createRouter, createWebHistory } from 'vue-router'
import { adminRoutes } from './routes/admin'
import { transferRoutes } from './routes/transfer'
import { publicRoutes } from './routes/public'
import { internalRoutes } from './routes/internal'
import { setupRouterGuards } from './guards'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/login',
      component: () => import('@/views/login/index.vue'),
      meta: { title: '登录' },
    },
    adminRoutes,
    transferRoutes,
    publicRoutes,
    internalRoutes,
    {
      path: '/',
      redirect: '/public',
    },
  ],
})

setupRouterGuards(router)

export default router
