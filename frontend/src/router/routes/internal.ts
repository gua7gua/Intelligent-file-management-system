import type { RouteRecordRaw } from 'vue-router'

export const internalRoutes: RouteRecordRaw = {
  path: '/internal',
  component: () => import('@/layouts/InternalLayout.vue'),
  meta: { requiresAuth: true },
  children: [
    { path: '', redirect: '/internal/overview' },
    {
      path: 'overview',
      component: () => import('@/views/internal/overview/index.vue'),
      meta: { title: '工作台概览' },
    },
    {
      path: 'search',
      component: () => import('@/views/internal/search/index.vue'),
      meta: { title: '档案检索利用' },
    },
  ],
}
