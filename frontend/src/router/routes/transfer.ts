import type { RouteRecordRaw } from 'vue-router'

export const transferRoutes: RouteRecordRaw = {
  path: '/transfer',
  component: () => import('@/layouts/TransferLayout.vue'),
  meta: { requiresAuth: true },
  children: [
    { path: '', redirect: '/transfer/overview' },
    {
      path: 'overview',
      component: () => import('@/views/transfer/overview/index.vue'),
      meta: { title: '移交工作台' },
    },
    {
      path: 'transfer-list',
      component: () => import('@/views/transfer/transfer-list/index.vue'),
      meta: { title: '编制移交清单' },
    },
  ],
}
