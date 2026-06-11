import type { RouteRecordRaw } from 'vue-router'

export const publicRoutes: RouteRecordRaw = {
  path: '/public',
  component: () => import('@/layouts/PublicLayout.vue'),
  children: [
    { path: '', redirect: '/public/index' },
    {
      path: 'index',
      component: () => import('@/views/public/index/index.vue'),
      meta: { title: '公众首页' },
    },
    {
      path: 'search',
      component: () => import('@/views/public/search/index.vue'),
      meta: { title: '公开档案检索' },
    },
    {
      path: 'collection',
      component: () => import('@/views/public/collection/index.vue'),
      meta: { title: '征集清单' },
    },
    {
      path: 'register',
      component: () => import('@/views/public/register/index.vue'),
      meta: { title: '公众注册' },
    },
    {
      path: 'overview',
      component: () => import('@/views/public/overview/index.vue'),
      meta: { title: '公众概览', requiresAuth: true },
    },
  ],
}
