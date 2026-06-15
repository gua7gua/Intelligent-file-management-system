import type { RouteRecordRaw } from 'vue-router'
import type { MenuItem } from '@/types/menu'

/** 管理后台侧边栏菜单配置 */
export const adminMenuConfig: MenuItem[] = [
  {
    path: '/admin/overview',
    title: '管理概览',
    icon: 'Odometer',
    roles: ['front_archivist', 'back_archivist', 'director', 'sys_admin'],
  },
  {
    path: '/admin/receive',
    title: '接收管理',
    icon: 'Download',
    roles: ['front_archivist', 'back_archivist'],
    children: [
      { path: '/admin/transfer-reception', title: '移交验收与电子文件上传', roles: ['front_archivist', 'back_archivist'] },
      { path: '/admin/collection', title: '征集管理与接收', roles: ['front_archivist', 'back_archivist'] },
    ],
  },
  {
    path: '/admin/archive',
    title: '入库与档案',
    icon: 'Box',
    roles: ['back_archivist'],
    children: [
      { path: '/admin/pending-archive', title: '待入库与上架', roles: ['back_archivist'] },
      { path: '/admin/archive-management', title: '档案管理', roles: ['back_archivist'] },
    ],
  },
  {
    path: '/admin/warehouse-group',
    title: '库房管理',
    icon: 'OfficeBuilding',
    roles: ['back_archivist'],
    children: [
      { path: '/admin/warehouse', title: '库房与架位', roles: ['back_archivist'] },
      { path: '/admin/fonds', title: '全宗管理', roles: ['back_archivist'] },
      { path: '/admin/inventory', title: '档案盘点', roles: ['back_archivist'] },
    ],
  },
  {
    path: '/admin/borrow',
    title: '利用服务',
    icon: 'Reading',
    roles: ['front_archivist', 'back_archivist'],
    children: [
      { path: '/admin/borrow-approval', title: '借阅审批', roles: ['front_archivist', 'back_archivist'] },
    ],
  },
  {
    path: '/admin/appraisal-group',
    title: '鉴定销毁',
    icon: 'Stamp',
    roles: ['back_archivist'],
    children: [
      { path: '/admin/appraisal', title: '档案鉴定', roles: ['back_archivist'] },
      { path: '/admin/destruction', title: '档案销毁', roles: ['back_archivist'] },
    ],
  },
  {
    path: '/admin/approval',
    title: '审批工作台',
    icon: 'Checked',
    roles: ['director'],
  },
  {
    path: '/admin/research-group',
    title: '编研与保存',
    icon: 'Notebook',
    roles: ['back_archivist'],
    children: [
      { path: '/admin/compilation', title: '档案编研', roles: ['back_archivist'] },
      { path: '/admin/preservation', title: '档案保存', roles: ['back_archivist'] },
    ],
  },
  {
    path: '/admin/data-group',
    title: '数据中心',
    icon: 'DataAnalysis',
    roles: ['back_archivist', 'director'],
    children: [
      { path: '/admin/statistics', title: '数据统计', roles: ['back_archivist', 'director'] },
      { path: '/admin/data-analysis', title: '数据研判', roles: ['back_archivist'] },
    ],
  },
  {
    path: '/admin/system-group',
    title: '系统管理',
    icon: 'Setting',
    roles: ['sys_admin'],
    children: [
      { path: '/admin/user-management', title: '用户管理', roles: ['sys_admin'] },
      { path: '/admin/system-settings', title: '系统配置', roles: ['sys_admin'] },
      { path: '/admin/audit-logs', title: '审计日志', roles: ['sys_admin'] },
      { path: '/admin/access-logs', title: '访问日志', roles: ['sys_admin'] },
    ],
  },
]

/** 管理后台路由定义 */
export const adminRoutes: RouteRecordRaw = {
  path: '/admin',
  component: () => import('@/layouts/AdminLayout.vue'),
  meta: { requiresAuth: true },
  children: [
    { path: '', redirect: '/admin/overview' },
    { path: 'overview', component: () => import('@/views/admin/overview/index.vue'), meta: { title: '管理概览' } },
    { path: 'transfer-reception', component: () => import('@/views/admin/transfer-reception/index.vue'), meta: { title: '移交验收与电子文件上传' } },
    { path: 'collection', component: () => import('@/views/admin/collection/index.vue'), meta: { title: '征集管理与接收' } },
    { path: 'pending-archive', component: () => import('@/views/admin/pending-archive/index.vue'), meta: { title: '待入库与上架' } },
    { path: 'archive-management', component: () => import('@/views/admin/archive-management/index.vue'), meta: { title: '档案管理' } },
    { path: 'warehouse', component: () => import('@/views/admin/warehouse/index.vue'), meta: { title: '库房与架位' } },
    { path: 'fonds', component: () => import('@/views/admin/fonds/index.vue'), meta: { title: '全宗管理' } },
    { path: 'inventory', component: () => import('@/views/admin/inventory/index.vue'), meta: { title: '档案盘点' } },
    { path: 'borrow-approval', component: () => import('@/views/admin/borrow-approval/index.vue'), meta: { title: '借阅审批' } },
    { path: 'appraisal', component: () => import('@/views/admin/appraisal/index.vue'), meta: { title: '档案鉴定' } },
    { path: 'destruction', component: () => import('@/views/admin/destruction/index.vue'), meta: { title: '档案销毁' } },
    { path: 'approval', component: () => import('@/views/admin/approval/index.vue'), meta: { title: '审批工作台' } },
    { path: 'compilation', component: () => import('@/views/admin/compilation/index.vue'), meta: { title: '档案编研' } },
    { path: 'preservation', component: () => import('@/views/admin/preservation/index.vue'), meta: { title: '档案保存' } },
    { path: 'statistics', component: () => import('@/views/admin/statistics/index.vue'), meta: { title: '数据统计' } },
    { path: 'data-analysis', component: () => import('@/views/admin/data-analysis/index.vue'), meta: { title: '数据研判' } },
    { path: 'user-management', component: () => import('@/views/admin/user-management/index.vue'), meta: { title: '用户管理' } },
    { path: 'system-settings', component: () => import('@/views/admin/system-settings/index.vue'), meta: { title: '系统配置' } },
    { path: 'audit-logs', component: () => import('@/views/admin/audit-logs/index.vue'), meta: { title: '审计日志' } },
    { path: 'access-logs', component: () => import('@/views/admin/access-logs/index.vue'), meta: { title: '访问日志' } },
  ],
}
