# 前端脚手架实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 搭建 Vue 3 + Element Plus 管理后台前端脚手架，包含布局、路由、权限菜单、公共组件和 API 层。

**Architecture:** 单 Vue 项目按路由区分四个门户（admin/transfer/public/internal）。管理后台采用深色侧边栏 + 浅色顶栏布局，菜单按角色过滤。封装 ProTable/ProForm 配置驱动组件统一处理列表页和表单页的标准模式。

**Tech Stack:** Vue 3.5 + TypeScript + Vite 8 + Element Plus 2 + Pinia 3 + Vue Router 5 + Axios + Sass

**Design Doc:** `frontend/docs/superpowers/specs/2026-06-11-frontend-scaffold-design.md`

---

## File Structure

```
frontend/
├── index.html
├── package.json
├── vite.config.ts
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
├── env.d.ts
├── .env.development
├── src/
│   ├── main.ts
│   ├── App.vue
│   ├── api/
│   │   ├── request.ts
│   │   └── auth.ts
│   ├── components/
│   │   ├── ProTable/index.vue
│   │   ├── ProForm/index.vue
│   │   ├── DictTag/index.vue
│   │   └── PortalSwitcher/index.vue
│   ├── composables/
│   │   ├── useDict.ts
│   │   └── usePermission.ts
│   ├── layouts/
│   │   ├── AdminLayout.vue
│   │   ├── TransferLayout.vue
│   │   ├── PublicLayout.vue
│   │   └── InternalLayout.vue
│   ├── router/
│   │   ├── index.ts
│   │   ├── guards.ts
│   │   └── routes/
│   │       ├── admin.ts
│   │       ├── transfer.ts
│   │       ├── public.ts
│   │       └── internal.ts
│   ├── stores/
│   │   ├── auth.ts
│   │   └── app.ts
│   ├── styles/
│   │   ├── variables.scss
│   │   └── index.scss
│   ├── types/
│   │   ├── api.d.ts
│   │   ├── enums.ts
│   │   ├── menu.ts
│   │   ├── user.ts
│   │   └── components.ts
│   ├── utils/
│   │   └── index.ts
│   └── views/
│       ├── login/index.vue
│       └── admin/
│           └── overview/index.vue
```

---

### Task 1: 初始化项目并安装依赖

**Files:**
- Create: `frontend/package.json`
- Create: `frontend/.env.development`
- Create: `frontend/.gitignore`

- [ ] **Step 1: 创建 package.json**

```json
{
  "name": "archive-management-frontend",
  "private": true,
  "version": "0.1.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "vue-tsc -b && vite build",
    "preview": "vite preview"
  }
}
```

- [ ] **Step 2: 安装依赖**

```bash
cd frontend
npm install vue@^3.5.0 vue-router@^5.1.0 pinia@^3.0.0 axios@^1.7.0 element-plus@^2.14.0 @element-plus/icons-vue@^2.3.0
npm install -D vite@^8.0.0 @vitejs/plugin-vue@^6.0.0 typescript@^6.0.0 vue-tsc@^3.3.0 sass@^1.100.0
```

- [ ] **Step 3: 创建 .env.development**

```
VITE_API_BASE_URL=/api
VITE_APP_TITLE=智能档案管理系统
```

- [ ] **Step 4: 创建 .gitignore**

```
node_modules
dist
.env.local
.env.*.local
*.log
.DS_Store
```

- [ ] **Step 5: 提交**

```bash
git add .
git commit -m "chore: 初始化前端项目及依赖"
```

---

### Task 2: 配置 Vite 和 TypeScript

**Files:**
- Create: `frontend/vite.config.ts`
- Create: `frontend/tsconfig.json`
- Create: `frontend/tsconfig.app.json`
- Create: `frontend/tsconfig.node.json`
- Create: `frontend/env.d.ts`
- Create: `frontend/index.html`

- [ ] **Step 1: 创建 vite.config.ts**

```typescript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: {
      '@': resolve(__dirname, 'src'),
    },
  },
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
```

- [ ] **Step 2: 创建 tsconfig.json**

```json
{
  "files": [],
  "references": [
    { "path": "./tsconfig.app.json" },
    { "path": "./tsconfig.node.json" }
  ]
}
```

- [ ] **Step 3: 创建 tsconfig.app.json**

```json
{
  "compilerOptions": {
    "target": "ES2020",
    "useDefineForClassFields": true,
    "module": "ESNext",
    "lib": ["ES2020", "DOM", "DOM.Iterable"],
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "isolatedModules": true,
    "moduleDetection": "force",
    "noEmit": true,
    "jsx": "preserve",
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "noUncheckedSideEffectImports": true,
    "baseUrl": ".",
    "paths": {
      "@/*": ["src/*"]
    }
  },
  "include": ["env.d.ts", "src/**/*.ts", "src/**/*.tsx", "src/**/*.vue"]
}
```

- [ ] **Step 4: 创建 tsconfig.node.json**

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "lib": ["ES2023"],
    "module": "ESNext",
    "skipLibCheck": true,
    "moduleResolution": "bundler",
    "allowImportingTsExtensions": true,
    "isolatedModules": true,
    "moduleDetection": "force",
    "noEmit": true,
    "strict": true,
    "noUnusedLocals": true,
    "noUnusedParameters": true,
    "noFallthroughCasesInSwitch": true,
    "noUncheckedSideEffectImports": true
  },
  "include": ["vite.config.ts"]
}
```

- [ ] **Step 5: 创建 env.d.ts**

```typescript
/// <reference types="vite/client" />

declare module '*.vue' {
  import type { DefineComponent } from 'vue'
  const component: DefineComponent<object, object, unknown>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  readonly VITE_APP_TITLE: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
```

- [ ] **Step 6: 创建 index.html**

```html
<!DOCTYPE html>
<html lang="zh-CN">
  <head>
    <meta charset="UTF-8" />
    <link rel="icon" type="image/svg+xml" href="/vite.svg" />
    <meta name="viewport" content="width=device-width, initial-scale=1.0" />
    <title>智能档案管理系统</title>
  </head>
  <body>
    <div id="app"></div>
    <script type="module" src="/src/main.ts"></script>
  </body>
</html>
```

- [ ] **Step 7: 验证配置**

```bash
cd frontend && npx vue-tsc --noEmit
```

Expected: 无报错（此时还没有 src 文件，可能有导入错误，这是正常的）

- [ ] **Step 8: 提交**

```bash
git add .
git commit -m "chore: 配置 Vite 和 TypeScript"
```

---

### Task 3: 定义 TypeScript 类型

**Files:**
- Create: `frontend/src/types/api.d.ts`
- Create: `frontend/src/types/user.ts`
- Create: `frontend/src/types/enums.ts`
- Create: `frontend/src/types/menu.ts`
- Create: `frontend/src/types/components.ts`

- [ ] **Step 1: 创建 types/api.d.ts**

```typescript
/** 统一响应结构 */
export interface ApiResponse<T = any> {
  code: string
  message: string
  data: T
  traceId: string
}

/** 分页响应结构 */
export interface PageData<T = any> {
  records: T[]
  pageNo: number
  pageSize: number
  total: number
  hasNext: boolean
}

/** 分页请求参数 */
export interface PageParams {
  pageNo?: number
  pageSize?: number
  sortBy?: string
  sortOrder?: 'asc' | 'desc'
}
```

- [ ] **Step 2: 创建 types/user.ts**

```typescript
/** 用户基本信息 */
export interface UserInfo {
  id: number
  username: string
  realName: string
  phone: string
  organizationId: number
  organizationName: string
  department: string
  securityLevel: number
  dataScope: string
}

/** 登录请求参数 */
export interface LoginParams {
  username: string
  password: string
}

/** 登录响应数据 */
export interface LoginResult {
  token: string
  user: UserInfo
  roles: string[]
  permissions: string[]
}
```

- [ ] **Step 3: 创建 types/enums.ts**

```typescript
/** 清单批次状态 */
export const BatchStatus = {
  DRAFT: 'draft',
  PENDING_TRANSFER: 'pending_transfer',
  PENDING_CONTACT: 'pending_contact',
  PENDING_RECEIVE: 'pending_receive',
  RECEIVED: 'received',
  PARTIALLY_RECEIVED: 'partially_received',
  REJECTED: 'rejected',
  ARCHIVED: 'archived',
  SHELVED: 'shelved',
} as const

/** 清单条目状态 */
export const ItemStatus = {
  DRAFT: 'draft',
  PENDING_ACCEPTANCE: 'pending_acceptance',
  ACCEPTED: 'accepted',
  REJECTED: 'rejected',
  PENDING_ARCHIVE: 'pending_archive',
  ARCHIVED: 'archived',
} as const

/** 档案生命周期状态 */
export const ArchiveLifecycleStatus = {
  PENDING_SHELF: 'pending_shelf',
  NORMAL: 'normal',
  PENDING_DESTRUCTION: 'pending_destruction',
  DESTROYED: 'destroyed',
} as const

/** 借阅状态 */
export const BorrowStatus = {
  APPLIED: 'applied',
  REJECTED: 'rejected',
  APPROVED: 'approved',
  VOUCHER_ISSUED: 'voucher_issued',
  CHECKED_OUT: 'checked_out',
  RETURNED: 'returned',
  ABNORMAL_RETURN: 'abnormal_return',
} as const

/** 审批状态 */
export const ApprovalStatus = {
  PENDING: 'pending',
  APPROVED: 'approved',
  REJECTED: 'rejected',
} as const

/** 来源类型 */
export const SourceType = {
  TRANSFER: 'transfer',
  COLLECTION: 'collection',
  COMPILATION: 'compilation',
} as const

/** 载体状态 */
export const CarrierStatus = {
  ELECTRONIC: 'electronic',
  PAPER_ELECTRONIC: 'paper_electronic',
  PAPER: 'paper',
} as const

/** 保管期限 */
export const RetentionPeriod = {
  TEN_YEARS: '10y',
  THIRTY_YEARS: '30y',
  PERMANENT: 'permanent',
} as const

/** 密级（数值，0=非密） */
export const SecurityLevel = {
  PUBLIC: 0,
  INTERNAL: 1,
  SECRET: 2,
  CONFIDENTIAL: 3,
  TOP_SECRET: 4,
} as const

/** 开放状态 */
export const OpenStatus = {
  OPEN: 'open',
  CLOSED: 'closed',
} as const

/** 角色标识 */
export const RoleKey = {
  FRONT_ADMIN: 'front_admin',
  BACK_ADMIN: 'back_admin',
  TRANSFER_USER: 'transfer_user',
  INTERNAL_USER: 'internal_user',
  LEADER: 'leader',
  SYS_ADMIN: 'sys_admin',
} as const

/** 清单状态中文映射（需结合 sourceType） */
export const BatchStatusLabel: Record<string, Record<string, string>> = {
  transfer: {
    draft: '草稿',
    pending_transfer: '待移交',
    pending_receive: '待接收',
    received: '已接收',
    partially_received: '部分接收',
    rejected: '已回退',
    archived: '已入库',
    shelved: '已上架',
  },
  collection: {
    draft: '草稿',
    pending_contact: '待联系',
    pending_receive: '待接收',
    received: '已接收',
    partially_received: '部分接收',
    rejected: '已拒绝',
    archived: '已入库',
    shelved: '已上架',
  },
}

/** 条目状态中文 */
export const ItemStatusLabel: Record<string, string> = {
  draft: '草稿',
  pending_acceptance: '待验收',
  accepted: '已接收',
  rejected: '已回退',
  pending_archive: '待入库',
  archived: '已入库',
}

/** 档案生命周期状态中文 */
export const ArchiveStatusLabel: Record<string, string> = {
  pending_shelf: '已入库未上架',
  normal: '正常',
  pending_destruction: '待销毁',
  destroyed: '已销毁',
}

/** 借阅状态中文 */
export const BorrowStatusLabel: Record<string, string> = {
  applied: '待审批',
  rejected: '已拒绝',
  approved: '已批准',
  voucher_issued: '凭证已生成',
  checked_out: '已借出',
  returned: '已归还',
  abnormal_return: '异常归还',
}

/** 载体状态中文 */
export const CarrierStatusLabel: Record<string, string> = {
  electronic: '纯电子',
  paper_electronic: '纸质+电子',
  paper: '纯纸质',
}

/** 保管期限中文 */
export const RetentionPeriodLabel: Record<string, string> = {
  '10y': '10年',
  '30y': '30年',
  permanent: '永久',
}

/** 密级中文 */
export const SecurityLevelLabel: Record<number, string> = {
  0: '非密',
  1: '内部',
  2: '秘密',
  3: '机密',
  4: '绝密',
}

/** 开放状态中文 */
export const OpenStatusLabel: Record<string, string> = {
  open: '公开',
  closed: '不公开',
}
```

- [ ] **Step 4: 创建 types/menu.ts**

```typescript
/** 侧边栏菜单项 */
export interface MenuItem {
  /** 路由路径，作为 el-menu 的 index */
  path: string
  /** 菜单标题 */
  title: string
  /** Element Plus 图标组件名 */
  icon?: string
  /** 可见角色列表 */
  roles: string[]
  /** 子菜单 */
  children?: MenuItem[]
}

/** 门户定义 */
export interface PortalDef {
  key: string
  label: string
  path: string
}
```

- [ ] **Step 5: 创建 types/components.ts**

```typescript
import type { FormItemRule } from 'element-plus'

/** ProTable 列定义 */
export interface ProTableColumn {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  dict?: string
  sourceType?: string
  formatter?: (row: any) => string
  slot?: string
  fixed?: 'left' | 'right'
  sortable?: boolean
}

/** ProTable 搜索字段 */
export interface SearchField {
  prop: string
  label: string
  type: 'input' | 'select' | 'dateRange' | 'date' | 'number'
  dict?: string
  defaultValue?: any
}

/** ProForm 表单字段 */
export interface ProFormField {
  prop: string
  label: string
  type: 'input' | 'textarea' | 'number' | 'select' | 'date' | 'dateRange'
      | 'radio' | 'checkbox' | 'switch' | 'upload'
  required?: boolean
  dict?: string
  rules?: FormItemRule[]
  placeholder?: string
  disabled?: boolean
  span?: number
}

/** 字典项 */
export interface DictItem {
  value: string | number
  label: string
  tagType?: '' | 'success' | 'warning' | 'danger' | 'info'
}
```

- [ ] **Step 6: 提交**

```bash
git add .
git commit -m "feat(types): 定义业务类型、枚举和组件类型"
```

---

### Task 4: 全局样式

**Files:**
- Create: `frontend/src/styles/variables.scss`
- Create: `frontend/src/styles/index.scss`

- [ ] **Step 1: 创建 styles/variables.scss**

```scss
// 侧边栏
$sidebar-width: 220px;
$sidebar-collapsed-width: 64px;
$sidebar-bg: #1d1e2c;
$sidebar-text: #c0c4cc;
$sidebar-active-text: #ffffff;
$sidebar-active-bg: #409eff;

// 顶栏
$header-height: 50px;
$header-bg: #ffffff;
$header-border: #e4e7ed;

// 内容区
$content-bg: #f5f7fa;
$content-padding: 20px;

// 主题色（与 Element Plus 一致）
$primary-color: #409eff;
$success-color: #67c23a;
$warning-color: #e6a23c;
$danger-color: #f56c6c;
$info-color: #909399;
```

- [ ] **Step 2: 创建 styles/index.scss**

```scss
@use './variables' as *;

/* 全局重置 */
*,
*::before,
*::after {
  box-sizing: border-box;
  margin: 0;
  padding: 0;
}

html,
body,
#app {
  width: 100%;
  height: 100%;
  font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Hiragino Sans GB',
    'Microsoft YaHei', '微软雅黑', Arial, sans-serif;
}

/* 滚动条美化 */
::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}

::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 3px;
}

::-webkit-scrollbar-track {
  background: transparent;
}

/* 管理后台布局 */
.admin-layout {
  display: flex;
  height: 100vh;
  overflow: hidden;
}

.admin-sidebar {
  width: $sidebar-width;
  height: 100%;
  background-color: $sidebar-bg;
  transition: width 0.3s;
  overflow: hidden;
  flex-shrink: 0;

  &.collapsed {
    width: $sidebar-collapsed-width;
  }
}

.admin-sidebar-logo {
  height: $header-height;
  display: flex;
  align-items: center;
  padding: 0 16px;
  color: #ffffff;
  font-size: 16px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);

  .logo-icon {
    font-size: 22px;
    flex-shrink: 0;
  }

  .logo-text {
    margin-left: 10px;
    font-size: 14px;
  }
}

.admin-sidebar-menu {
  height: calc(100% - #{$header-height});

  // Element Plus el-menu 深色主题覆盖
  .el-menu {
    border-right: none;
    background-color: transparent;
  }

  .el-menu-item,
  .el-sub-menu__title {
    &:hover {
      background-color: rgba(255, 255, 255, 0.05) !important;
    }
  }

  .el-menu-item.is-active {
    background-color: $sidebar-active-bg !important;
    color: $sidebar-active-text !important;
  }
}

.admin-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.admin-header {
  height: $header-height;
  background-color: $header-bg;
  border-bottom: 1px solid $header-border;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  flex-shrink: 0;
}

.admin-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-header-right {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #606266;
  transition: color 0.2s;

  &:hover {
    color: $primary-color;
  }
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #606266;
  font-size: 14px;
}

.admin-content {
  flex: 1;
  overflow-y: auto;
  padding: $content-padding;
  background-color: $content-bg;
}

/* ProTable 样式 */
.pro-table {
  background-color: #ffffff;
  border-radius: 4px;
  padding: 20px;
}

.pro-table-search {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #ebeef5;
}

.pro-table-toolbar {
  margin-bottom: 16px;
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}

.pro-table-pagination {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}

/* 公众门户布局 */
.public-layout {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}
```

- [ ] **Step 3: 提交**

```bash
git add .
git commit -m "style: 添加全局样式和布局样式"
```

---

### Task 5: API 层

**Files:**
- Create: `frontend/src/api/request.ts`
- Create: `frontend/src/api/auth.ts`

- [ ] **Step 1: 创建 api/request.ts**

```typescript
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import router from '@/router'

const request = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 15000,
})

// 请求拦截器：注入 token
request.interceptors.request.use(
  (config) => {
    const authStore = useAuthStore()
    if (authStore.token) {
      config.headers.Authorization = `Bearer ${authStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error),
)

// 响应拦截器：解包响应、统一错误处理
request.interceptors.response.use(
  (response) => {
    const res = response.data
    // 业务成功
    if (res.code === 'OK') {
      return res.data
    }
    // 业务失败
    const message = res.message || '请求失败'
    ElMessage.error(message)
    return Promise.reject(new Error(message))
  },
  (error) => {
    const { response } = error
    if (response?.status === 401) {
      const authStore = useAuthStore()
      authStore.logout()
      router.push('/login')
      ElMessage.error('登录已过期，请重新登录')
    } else if (response?.data?.message) {
      ElMessage.error(response.data.message)
    } else {
      ElMessage.error('网络异常，请稍后重试')
    }
    return Promise.reject(error)
  },
)

export default request
```

- [ ] **Step 2: 创建 api/auth.ts**

```typescript
import request from './request'
import type { LoginParams, LoginResult } from '@/types/user'

/** 登录 */
export function loginApi(data: LoginParams): Promise<LoginResult> {
  return request.post('/auth/login', data)
}

/** 获取当前用户信息（用于刷新页面时恢复状态） */
export function getUserInfoApi(): Promise<LoginResult> {
  return request.get('/auth/me')
}

/** 登出 */
export function logoutApi(): Promise<void> {
  return request.post('/auth/logout')
}
```

- [ ] **Step 3: 提交**

```bash
git add .
git commit -m "feat(api): 封装 Axios 实例和认证接口"
```

---

### Task 6: Pinia Stores

**Files:**
- Create: `frontend/src/stores/auth.ts`
- Create: `frontend/src/stores/app.ts`

- [ ] **Step 1: 创建 stores/auth.ts**

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/types/user'
import { loginApi, logoutApi, getUserInfoApi } from '@/api/auth'
import router from '@/router'

/** 管理后台相关角色 */
const ADMIN_ROLES = ['front_admin', 'back_admin', 'leader', 'sys_admin']

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<UserInfo | null>(null)
  const roles = ref<string[]>([])
  const permissions = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)

  /** 登录 */
  async function login(username: string, password: string) {
    const data = await loginApi({ username, password })
    token.value = data.token
    user.value = data.user
    roles.value = data.roles
    permissions.value = data.permissions
    localStorage.setItem('token', data.token)
    router.push(getDefaultPortal())
  }

  /** 登出 */
  function logout() {
    token.value = null
    user.value = null
    roles.value = []
    permissions.value = []
    localStorage.removeItem('token')
    router.push('/login')
  }

  /** 恢复登录状态（页面刷新时调用） */
  async function restoreSession() {
    if (!token.value) return
    try {
      const data = await getUserInfoApi()
      user.value = data.user
      roles.value = data.roles
      permissions.value = data.permissions
    } catch {
      logout()
    }
  }

  /** 获取默认门户路径 */
  function getDefaultPortal(): string {
    if (roles.value.some((r) => ADMIN_ROLES.includes(r))) return '/admin'
    if (roles.value.includes('transfer_user')) return '/transfer'
    if (roles.value.includes('internal_user')) return '/internal'
    return '/public'
  }

  /** 获取可切换的门户列表 */
  function getAllowedPortals() {
    const portals: Array<{ key: string; label: string; path: string }> = []
    if (roles.value.some((r) => ADMIN_ROLES.includes(r))) {
      portals.push({ key: 'admin', label: '管理后台', path: '/admin' })
    }
    if (roles.value.includes('transfer_user')) {
      portals.push({ key: 'transfer', label: '移交门户', path: '/transfer' })
    }
    if (roles.value.includes('internal_user')) {
      portals.push({ key: 'internal', label: '内部门户', path: '/internal' })
    }
    portals.push({ key: 'public', label: '公众门户', path: '/public' })
    return portals
  }

  /** 角色判断 */
  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  function hasAnyRole(...checkRoles: string[]): boolean {
    return checkRoles.some((r) => roles.value.includes(r))
  }

  return {
    token,
    user,
    roles,
    permissions,
    isLoggedIn,
    login,
    logout,
    restoreSession,
    getDefaultPortal,
    getAllowedPortals,
    hasRole,
    hasAnyRole,
  }
})
```

- [ ] **Step 2: 创建 stores/app.ts**

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DictItem } from '@/types/components'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const currentPortal = ref('admin')
  const dictionaries = ref<Record<string, DictItem[]>>({})
  const dictionariesLoaded = ref(false)

  /** 切换侧边栏折叠 */
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  /** 加载全局字典（登录后调用一次） */
  async function loadDictionaries() {
    if (dictionariesLoaded.value) return
    try {
      // TODO: 接入后端 GET /api/dictionaries
      // const data = await request.get('/dictionaries')
      // dictionaries.value = data
      dictionariesLoaded.value = true
    } catch {
      // 字典加载失败不阻塞使用
    }
  }

  /** 根据字典 key 获取选项列表 */
  function getDictOptions(dictKey: string): DictItem[] {
    return dictionaries.value[dictKey] || []
  }

  /** 根据字典 key 和值获取中文标签 */
  function getDictLabel(dictKey: string, value: string | number): string {
    const items = dictionaries.value[dictKey]
    if (!items) return String(value)
    const found = items.find((item) => item.value === value)
    return found ? found.label : String(value)
  }

  return {
    sidebarCollapsed,
    currentPortal,
    dictionaries,
    toggleSidebar,
    loadDictionaries,
    getDictOptions,
    getDictLabel,
  }
})
```

- [ ] **Step 3: 提交**

```bash
git add .
git commit -m "feat(store): 添加 auth 和 app 状态管理"
```

---

### Task 7: 路由与导航守卫

**Files:**
- Create: `frontend/src/router/routes/admin.ts`
- Create: `frontend/src/router/routes/transfer.ts`
- Create: `frontend/src/router/routes/public.ts`
- Create: `frontend/src/router/routes/internal.ts`
- Create: `frontend/src/router/guards.ts`
- Create: `frontend/src/router/index.ts`

- [ ] **Step 1: 创建 router/routes/admin.ts — 管理后台路由和菜单配置**

```typescript
import type { RouteRecordRaw } from 'vue-router'
import type { MenuItem } from '@/types/menu'

/** 管理后台侧边栏菜单配置 */
export const adminMenuConfig: MenuItem[] = [
  {
    path: '/admin/overview',
    title: '管理概览',
    icon: 'Odometer',
    roles: ['front_admin', 'back_admin', 'leader', 'sys_admin'],
  },
  {
    path: '/admin/receive',
    title: '接收管理',
    icon: 'Download',
    roles: ['front_admin', 'back_admin'],
    children: [
      { path: '/admin/transfer-reception', title: '移交验收与电子文件上传', roles: ['front_admin', 'back_admin'] },
      { path: '/admin/collection', title: '征集管理与接收', roles: ['front_admin', 'back_admin'] },
    ],
  },
  {
    path: '/admin/archive',
    title: '入库与档案',
    icon: 'Box',
    roles: ['back_admin'],
    children: [
      { path: '/admin/pending-archive', title: '待入库与上架', roles: ['back_admin'] },
      { path: '/admin/archive-management', title: '档案管理', roles: ['back_admin'] },
    ],
  },
  {
    path: '/admin/warehouse-group',
    title: '库房管理',
    icon: 'OfficeBuilding',
    roles: ['back_admin'],
    children: [
      { path: '/admin/warehouse', title: '库房与架位', roles: ['back_admin'] },
      { path: '/admin/fonds', title: '全宗管理', roles: ['back_admin'] },
      { path: '/admin/inventory', title: '档案盘点', roles: ['back_admin'] },
    ],
  },
  {
    path: '/admin/borrow',
    title: '利用服务',
    icon: 'Reading',
    roles: ['front_admin', 'back_admin'],
    children: [
      { path: '/admin/borrow-approval', title: '借阅审批', roles: ['front_admin', 'back_admin'] },
    ],
  },
  {
    path: '/admin/appraisal-group',
    title: '鉴定销毁',
    icon: 'Stamp',
    roles: ['back_admin'],
    children: [
      { path: '/admin/appraisal', title: '档案鉴定', roles: ['back_admin'] },
      { path: '/admin/destruction', title: '档案销毁', roles: ['back_admin'] },
    ],
  },
  {
    path: '/admin/approval',
    title: '审批工作台',
    icon: 'Checked',
    roles: ['leader'],
  },
  {
    path: '/admin/research-group',
    title: '编研与保存',
    icon: 'Notebook',
    roles: ['back_admin'],
    children: [
      { path: '/admin/compilation', title: '档案编研', roles: ['back_admin'] },
      { path: '/admin/preservation', title: '档案保存', roles: ['back_admin'] },
    ],
  },
  {
    path: '/admin/data-group',
    title: '数据中心',
    icon: 'DataAnalysis',
    roles: ['back_admin', 'leader'],
    children: [
      { path: '/admin/statistics', title: '数据统计', roles: ['back_admin', 'leader'] },
      { path: '/admin/data-analysis', title: '数据研判', roles: ['back_admin'] },
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
    {
      path: 'overview',
      component: () => import('@/views/admin/overview/index.vue'),
      meta: { title: '管理概览' },
    },
    {
      path: 'transfer-reception',
      component: () => import('@/views/admin/transfer-reception/index.vue'),
      meta: { title: '移交验收与电子文件上传' },
    },
    {
      path: 'collection',
      component: () => import('@/views/admin/collection/index.vue'),
      meta: { title: '征集管理与接收' },
    },
    {
      path: 'pending-archive',
      component: () => import('@/views/admin/pending-archive/index.vue'),
      meta: { title: '待入库与上架' },
    },
    {
      path: 'archive-management',
      component: () => import('@/views/admin/archive-management/index.vue'),
      meta: { title: '档案管理' },
    },
    {
      path: 'warehouse',
      component: () => import('@/views/admin/warehouse/index.vue'),
      meta: { title: '库房与架位' },
    },
    {
      path: 'fonds',
      component: () => import('@/views/admin/fonds/index.vue'),
      meta: { title: '全宗管理' },
    },
    {
      path: 'inventory',
      component: () => import('@/views/admin/inventory/index.vue'),
      meta: { title: '档案盘点' },
    },
    {
      path: 'borrow-approval',
      component: () => import('@/views/admin/borrow-approval/index.vue'),
      meta: { title: '借阅审批' },
    },
    {
      path: 'appraisal',
      component: () => import('@/views/admin/appraisal/index.vue'),
      meta: { title: '档案鉴定' },
    },
    {
      path: 'destruction',
      component: () => import('@/views/admin/destruction/index.vue'),
      meta: { title: '档案销毁' },
    },
    {
      path: 'approval',
      component: () => import('@/views/admin/approval/index.vue'),
      meta: { title: '审批工作台' },
    },
    {
      path: 'compilation',
      component: () => import('@/views/admin/compilation/index.vue'),
      meta: { title: '档案编研' },
    },
    {
      path: 'preservation',
      component: () => import('@/views/admin/preservation/index.vue'),
      meta: { title: '档案保存' },
    },
    {
      path: 'statistics',
      component: () => import('@/views/admin/statistics/index.vue'),
      meta: { title: '数据统计' },
    },
    {
      path: 'data-analysis',
      component: () => import('@/views/admin/data-analysis/index.vue'),
      meta: { title: '数据研判' },
    },
    {
      path: 'user-management',
      component: () => import('@/views/admin/user-management/index.vue'),
      meta: { title: '用户管理' },
    },
    {
      path: 'system-settings',
      component: () => import('@/views/admin/system-settings/index.vue'),
      meta: { title: '系统配置' },
    },
  ],
}
```

- [ ] **Step 2: 创建 router/routes/transfer.ts**

```typescript
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
```

- [ ] **Step 3: 创建 router/routes/public.ts**

```typescript
import type { RouteRecordRaw } from 'vue-router'

export const publicRoutes: RouteRecordRaw = {
  path: '/public',
  component: () => import('@/layouts/PublicLayout.vue'),
  children: [
    { path: '', redirect: '/public/index' },
    {
      path: 'index',
      component: () => import('@/views/public/index.vue'),
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
```

- [ ] **Step 4: 创建 router/routes/internal.ts**

```typescript
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
```

- [ ] **Step 5: 创建 router/guards.ts**

```typescript
import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/** 管理后台相关角色 */
const ADMIN_ROLES = ['front_admin', 'back_admin', 'leader', 'sys_admin']

/** 需要登录才能访问的路由 meta 标识 */
function requiresAuth(to: any): boolean {
  return to.matched.some((record: any) => record.meta.requiresAuth)
}

/** 检查路由是否需要特定角色 */
function getRequiredRoles(to: any): string[] | null {
  const roles: string[] = []
  to.matched.forEach((record: any) => {
    if (record.meta.roles) {
      roles.push(...record.meta.roles)
    }
  })
  return roles.length > 0 ? roles : null
}

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to, _from) => {
    const authStore = useAuthStore()

    // 已登录但用户信息未恢复（页面刷新）
    if (authStore.isLoggedIn && !authStore.user) {
      await authStore.restoreSession()
    }

    // 不需要登录的页面，直接放行
    if (!requiresAuth(to)) {
      // 已登录访问登录页，跳转默认门户
      if (to.path === '/login' && authStore.isLoggedIn) {
        return authStore.getDefaultPortal()
      }
      return true
    }

    // 需要登录但未登录
    if (!authStore.isLoggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    // 检查角色权限
    const requiredRoles = getRequiredRoles(to)
    if (requiredRoles) {
      const hasPermission = requiredRoles.some((r) => authStore.hasRole(r))
      if (!hasPermission) {
        return authStore.getDefaultPortal()
      }
    }

    return true
  })
}
```

- [ ] **Step 6: 创建 router/index.ts**

```typescript
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
```

- [ ] **Step 7: 提交**

```bash
git add .
git commit -m "feat(router): 添加四个门户路由和导航守卫"
```

---

### Task 8: Layout 布局组件

**Files:**
- Create: `frontend/src/layouts/AdminLayout.vue`
- Create: `frontend/src/layouts/TransferLayout.vue`
- Create: `frontend/src/layouts/PublicLayout.vue`
- Create: `frontend/src/layouts/InternalLayout.vue`

- [ ] **Step 1: 创建 layouts/AdminLayout.vue — 管理后台主布局**

```vue
<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <div class="admin-sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
      <div class="admin-sidebar-logo">
        <span class="logo-icon">📋</span>
        <span v-show="!appStore.sidebarCollapsed" class="logo-text">智能档案管理</span>
      </div>
      <div class="admin-sidebar-menu">
        <el-scrollbar>
          <el-menu
            :default-active="activeMenu"
            :collapse="appStore.sidebarCollapsed"
            :collapse-transition="false"
            background-color="#1d1e2c"
            text-color="#c0c4cc"
            active-text-color="#ffffff"
            router
          >
            <template v-for="item in filteredMenus" :key="item.path">
              <!-- 无子菜单 -->
              <el-menu-item v-if="!item.children" :index="item.path">
                <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
                <template #title>{{ item.title }}</template>
              </el-menu-item>
              <!-- 有子菜单 -->
              <el-sub-menu v-else :index="item.path">
                <template #title>
                  <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
                  <span>{{ item.title }}</span>
                </template>
                <el-menu-item
                  v-for="child in item.children"
                  :key="child.path"
                  :index="child.path"
                >
                  {{ child.title }}
                </el-menu-item>
              </el-sub-menu>
            </template>
          </el-menu>
        </el-scrollbar>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="admin-main">
      <!-- 顶栏 -->
      <div class="admin-header">
        <div class="admin-header-left">
          <el-icon class="collapse-btn" @click="appStore.toggleSidebar()">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">
              {{ route.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="admin-header-right">
          <PortalSwitcher />
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ authStore.user?.realName || authStore.user?.username || '未登录' }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="authStore.logout()">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 内容区 -->
      <div class="admin-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Fold, Expand, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { adminMenuConfig } from '@/router/routes/admin'
import PortalSwitcher from '@/components/PortalSwitcher/index.vue'
import type { MenuItem } from '@/types/menu'

const route = useRoute()
const authStore = useAuthStore()
const appStore = useAppStore()

/** 当前激活的菜单项 */
const activeMenu = computed(() => route.path)

/** 根据用户角色过滤菜单 */
const filteredMenus = computed(() => {
  const roles = authStore.roles
  return adminMenuConfig.filter((item) => filterMenu(item, roles))
})

function filterMenu(item: MenuItem, roles: string[]): boolean {
  const hasRole = item.roles.some((r) => roles.includes(r))
  if (!hasRole) return false
  if (item.children) {
    item = { ...item, children: item.children.filter((child) => filterMenu(child, roles)) }
    return (item.children?.length ?? 0) > 0
  }
  return true
}
</script>
```

- [ ] **Step 2: 创建 layouts/TransferLayout.vue — 移交门户占位布局**

```vue
<template>
  <div class="public-layout">
    <div class="admin-header">
      <div class="admin-header-left">
        <span style="font-weight:700;font-size:16px;">📋 移交单位门户</span>
      </div>
      <div class="admin-header-right">
        <PortalSwitcher />
        <el-dropdown trigger="click">
          <span class="user-info">
            <el-icon><User /></el-icon>
            {{ authStore.user?.realName || '未登录' }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="authStore.logout()">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
    <div class="admin-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import PortalSwitcher from '@/components/PortalSwitcher/index.vue'

const authStore = useAuthStore()
</script>
```

- [ ] **Step 3: 创建 layouts/PublicLayout.vue — 公众门户布局**

```vue
<template>
  <div class="public-layout">
    <div class="admin-header">
      <div class="admin-header-left">
        <span style="font-weight:700;font-size:16px;">📋 克拉玛依市档案馆</span>
      </div>
      <div class="admin-header-right">
        <PortalSwitcher v-if="authStore.isLoggedIn" />
        <template v-else>
          <el-button text @click="$router.push('/login')">登录</el-button>
          <el-button text @click="$router.push('/public/register')">注册</el-button>
        </template>
      </div>
    </div>
    <div class="admin-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import PortalSwitcher from '@/components/PortalSwitcher/index.vue'

const authStore = useAuthStore()
</script>
```

- [ ] **Step 4: 创建 layouts/InternalLayout.vue — 内部门户占位布局**

```vue
<template>
  <div class="public-layout">
    <div class="admin-header">
      <div class="admin-header-left">
        <span style="font-weight:700;font-size:16px;">📋 内部查阅门户</span>
      </div>
      <div class="admin-header-right">
        <PortalSwitcher />
        <el-dropdown trigger="click">
          <span class="user-info">
            <el-icon><User /></el-icon>
            {{ authStore.user?.realName || '未登录' }}
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="authStore.logout()">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </div>
    <div class="admin-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import PortalSwitcher from '@/components/PortalSwitcher/index.vue'

const authStore = useAuthStore()
</script>
```

- [ ] **Step 5: 提交**

```bash
git add .
git commit -m "feat(layout): 添加管理后台、移交、公众、内部门户布局"
```

---

### Task 9: 共享组件和 Composables

**Files:**
- Create: `frontend/src/components/DictTag/index.vue`
- Create: `frontend/src/components/PortalSwitcher/index.vue`
- Create: `frontend/src/composables/useDict.ts`
- Create: `frontend/src/composables/usePermission.ts`

- [ ] **Step 1: 创建 components/DictTag/index.vue — 字典状态标签**

```vue
<template>
  <el-tag :type="tagType" size="small">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '@/stores/app'
import {
  BatchStatusLabel,
  ItemStatusLabel,
  ArchiveStatusLabel,
  BorrowStatusLabel,
  CarrierStatusLabel,
  RetentionPeriodLabel,
  SecurityLevelLabel,
  OpenStatusLabel,
} from '@/types/enums'

const props = defineProps<{
  dict: string
  value: string | number
  sourceType?: string
}>()

/** 内联字典映射（不依赖后端接口） */
const inlineDicts: Record<string, Record<string, Record<string, string>>> = {
  batchStatus: BatchStatusLabel,
}

const staticDicts: Record<string, Record<string, string>> = {
  itemStatus: ItemStatusLabel,
  archiveLifecycleStatus: ArchiveStatusLabel,
  borrowStatus: BorrowStatusLabel,
  carrierStatus: CarrierStatusLabel,
  retentionPeriod: RetentionPeriodLabel,
  openStatus: OpenStatusLabel,
}

const tagTypeMap: Record<string, Record<string, string>> = {
  borrowStatus: {
    applied: 'warning',
    rejected: 'danger',
    approved: 'success',
    checked_out: '',
    returned: 'info',
    abnormal_return: 'danger',
  },
  archiveLifecycleStatus: {
    pending_shelf: 'warning',
    normal: 'success',
    pending_destruction: 'danger',
    destroyed: 'info',
  },
}

const appStore = useAppStore()

const label = computed(() => {
  // 带来源类型的字典（清单状态）
  if (props.sourceType && inlineDicts[props.dict]) {
    const sourceMap = inlineDicts[props.dict][props.sourceType]
    if (sourceMap && sourceMap[String(props.value)]) {
      return sourceMap[String(props.value)]
    }
  }
  // 内联字典（不带来源类型）
  if (inlineDicts[props.dict]) {
    for (const sourceMap of Object.values(inlineDicts[props.dict])) {
      if (sourceMap[String(props.value)]) {
        return sourceMap[String(props.value)]
      }
    }
  }
  // 静态字典
  if (staticDicts[props.dict]?.[String(props.value)]) {
    return staticDicts[props.dict][String(props.value)]
  }
  // 密级（数值型）
  if (props.dict === 'securityLevel') {
    return SecurityLevelLabel[Number(props.value)] ?? String(props.value)
  }
  // 从 appStore 字典中查找
  return appStore.getDictLabel(props.dict, props.value)
})

const tagType = computed(() => {
  const map = tagTypeMap[props.dict]
  if (map) return (map[String(props.value)] || '') as '' | 'success' | 'warning' | 'danger' | 'info'
  return ''
})
</script>
```

- [ ] **Step 2: 创建 components/PortalSwitcher/index.vue — 门户切换下拉**

```vue
<template>
  <el-dropdown v-if="portals.length > 1" trigger="click" @command="handleSwitch">
    <el-button text size="small">
      <el-icon><Switch /></el-icon>
      切换门户
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="portal in portals"
          :key="portal.key"
          :command="portal.path"
          :disabled="currentPath === portal.path"
        >
          {{ portal.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Switch } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const portals = computed(() => authStore.getAllowedPortals())
const currentPath = computed(() => route.path)

function handleSwitch(path: string) {
  router.push(path)
}
</script>
```

- [ ] **Step 3: 创建 composables/useDict.ts**

```typescript
import { useAppStore } from '@/stores/app'

/** 字典 composable：提供字典选项和标签翻译 */
export function useDict() {
  const appStore = useAppStore()

  /** 获取字典选项列表（用于 el-select） */
  function getDictOptions(dictKey: string) {
    return appStore.getDictOptions(dictKey)
  }

  /** 获取字典标签文本 */
  function getDictLabel(dictKey: string, value: string | number): string {
    return appStore.getDictLabel(dictKey, value)
  }

  return { getDictOptions, getDictLabel }
}
```

- [ ] **Step 4: 创建 composables/usePermission.ts**

```typescript
import { useAuthStore } from '@/stores/auth'

/** 权限 composable：提供角色判断方法 */
export function usePermission() {
  const authStore = useAuthStore()

  function hasRole(role: string): boolean {
    return authStore.hasRole(role)
  }

  function hasAnyRole(...roles: string[]): boolean {
    return authStore.hasAnyRole(...roles)
  }

  function isAdmin(): boolean {
    return authStore.hasAnyRole('front_admin', 'back_admin', 'leader', 'sys_admin')
  }

  return { hasRole, hasAnyRole, isAdmin }
}
```

- [ ] **Step 5: 提交**

```bash
git add .
git commit -m "feat(components): 添加 DictTag、PortalSwitcher 组件和 composables"
```

---

### Task 10: ProTable 组件

**Files:**
- Create: `frontend/src/components/ProTable/index.vue`

- [ ] **Step 1: 创建 components/ProTable/index.vue**

```vue
<template>
  <div class="pro-table">
    <!-- 搜索区 -->
    <div v-if="searchFields && searchFields.length > 0" class="pro-table-search">
      <el-form :model="searchForm" inline>
        <template v-for="field in searchFields" :key="field.prop">
          <el-form-item :label="field.label">
            <el-input
              v-if="field.type === 'input'"
              v-model="searchForm[field.prop]"
              :placeholder="`请输入${field.label}`"
              clearable
              style="width: 200px"
            />
            <el-select
              v-else-if="field.type === 'select'"
              v-model="searchForm[field.prop]"
              :placeholder="`请选择${field.label}`"
              clearable
              style="width: 200px"
            >
              <el-option
                v-for="opt in appStore.getDictOptions(field.dict ?? '')"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <el-date-picker
              v-else-if="field.type === 'dateRange'"
              v-model="searchForm[field.prop]"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 260px"
            />
            <el-date-picker
              v-else-if="field.type === 'date'"
              v-model="searchForm[field.prop]"
              :placeholder="`请选择${field.label}`"
              value-format="YYYY-MM-DD"
              style="width: 200px"
            />
            <el-input-number
              v-else-if="field.type === 'number'"
              v-model="searchForm[field.prop]"
              :placeholder="`请输入${field.label}`"
              style="width: 200px"
            />
          </el-form-item>
        </template>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 工具栏 -->
    <div v-if="$slots.toolbar" class="pro-table-toolbar">
      <slot name="toolbar" />
    </div>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="tableData" border stripe>
      <template v-for="col in columns" :key="col.prop">
        <el-table-column
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          :fixed="col.fixed"
          :sortable="col.sortable"
        >
          <template #default="{ row }">
            <slot v-if="col.slot" :name="col.slot" :row="row" />
            <DictTag
              v-else-if="col.dict"
              :dict="col.dict"
              :value="row[col.prop]"
              :source-type="col.sourceType"
            />
            <span v-else-if="col.formatter">{{ col.formatter(row) }}</span>
            <span v-else>{{ row[col.prop] }}</span>
          </template>
        </el-table-column>
      </template>
      <!-- 操作列 -->
      <el-table-column v-if="$slots.action" label="操作" fixed="right" :width="actionWidth">
        <template #default="{ row }">
          <slot name="action" :row="row" />
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pro-table-pagination">
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import DictTag from '@/components/DictTag/index.vue'
import type { ProTableColumn, SearchField } from '@/types/components'
import type { PageData } from '@/types/api'

const props = withDefaults(
  defineProps<{
    columns: ProTableColumn[]
    searchFields?: SearchField[]
    fetchData: (pageNo: number, pageSize: number, params: Record<string, any>) => Promise<PageData>
    actionWidth?: number | string
  }>(),
  {
    actionWidth: 200,
  },
)

const appStore = useAppStore()
const loading = ref(false)
const tableData = ref<any[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

/** 搜索表单数据 */
const searchForm = reactive<Record<string, any>>({})

/** 初始化搜索表单默认值 */
function initSearchForm() {
  if (props.searchFields) {
    for (const field of props.searchFields) {
      searchForm[field.prop] = field.defaultValue ?? undefined
    }
  }
}

/** 构建请求参数（过滤掉空值） */
function buildParams(): Record<string, any> {
  const params: Record<string, any> = {}
  for (const [key, value] of Object.entries(searchForm)) {
    if (value !== undefined && value !== null && value !== '') {
      // 日期范围特殊处理
      if (Array.isArray(value) && value.length === 2) {
        params[`${key}Start`] = value[0]
        params[`${key}End`] = value[1]
      } else {
        params[key] = value
      }
    }
  }
  return params
}

/** 加载数据 */
async function loadData() {
  loading.value = true
  try {
    const params = buildParams()
    const result = await props.fetchData(pageNo.value, pageSize.value, params)
    tableData.value = result.records
    total.value = result.total
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

/** 搜索 */
function handleSearch() {
  pageNo.value = 1
  loadData()
}

/** 重置 */
function handleReset() {
  initSearchForm()
  pageNo.value = 1
  loadData()
}

/** 刷新当前页 */
function refresh() {
  loadData()
}

initSearchForm()

onMounted(() => {
  loadData()
})

defineExpose({ refresh, loadData })
</script>
```

- [ ] **Step 2: 提交**

```bash
git add .
git commit -m "feat(components): 添加 ProTable 配置驱动表格组件"
```

---

### Task 11: ProForm 组件

**Files:**
- Create: `frontend/src/components/ProForm/index.vue`

- [ ] **Step 1: 创建 components/ProForm/index.vue**

```vue
<template>
  <el-form ref="formRef" :model="modelValue" :rules="computedRules" v-bind="$attrs">
    <el-row :gutter="20">
      <template v-for="field in fields" :key="field.prop">
        <el-col :span="field.span ?? 24">
          <el-form-item :label="field.label" :prop="field.prop">
            <!-- 自定义插槽 -->
            <slot :name="`field-${field.prop}`" :model="modelValue">
              <el-input
                v-if="field.type === 'input'"
                :model-value="modelValue[field.prop]"
                :placeholder="field.placeholder || `请输入${field.label}`"
                :disabled="field.disabled"
                @update:model-value="(val: string) => updateField(field.prop, val)"
              />
              <el-input
                v-else-if="field.type === 'textarea'"
                type="textarea"
                :rows="3"
                :model-value="modelValue[field.prop]"
                :placeholder="field.placeholder || `请输入${field.label}`"
                :disabled="field.disabled"
                @update:model-value="(val: string) => updateField(field.prop, val)"
              />
              <el-input-number
                v-else-if="field.type === 'number'"
                :model-value="modelValue[field.prop]"
                :placeholder="field.placeholder || `请输入${field.label}`"
                :disabled="field.disabled"
                style="width: 100%"
                @update:model-value="(val: number) => updateField(field.prop, val)"
              />
              <el-select
                v-else-if="field.type === 'select'"
                :model-value="modelValue[field.prop]"
                :placeholder="field.placeholder || `请选择${field.label}`"
                :disabled="field.disabled"
                style="width: 100%"
                @update:model-value="(val: any) => updateField(field.prop, val)"
              >
                <el-option
                  v-for="opt in getOptions(field)"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <el-date-picker
                v-else-if="field.type === 'date'"
                :model-value="modelValue[field.prop]"
                :placeholder="field.placeholder || `请选择${field.label}`"
                :disabled="field.disabled"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                @update:model-value="(val: string) => updateField(field.prop, val)"
              />
              <el-date-picker
                v-else-if="field.type === 'dateRange'"
                :model-value="modelValue[field.prop]"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                :disabled="field.disabled"
                style="width: 100%"
                @update:model-value="(val: string[]) => updateField(field.prop, val)"
              />
              <el-switch
                v-else-if="field.type === 'switch'"
                :model-value="modelValue[field.prop]"
                :disabled="field.disabled"
                @update:model-value="(val: boolean) => updateField(field.prop, val)"
              />
              <el-input
                v-else
                :model-value="modelValue[field.prop]"
                :placeholder="field.placeholder || `请输入${field.label}`"
                :disabled="field.disabled"
                @update:model-value="(val: string) => updateField(field.prop, val)"
              />
            </slot>
          </el-form-item>
        </el-col>
      </template>
    </el-row>
  </el-form>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { FormInstance, FormItemRule } from 'element-plus'
import { useAppStore } from '@/stores/app'
import type { ProFormField } from '@/types/components'

const props = defineProps<{
  fields: ProFormField[]
  modelValue: Record<string, any>
  rules?: Record<string, FormItemRule[]>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, any>]
}>()

const appStore = useAppStore()
const formRef = ref<FormInstance>()

/** 自动生成校验规则：required 字段自动添加必填规则 */
const computedRules = computed(() => {
  const result: Record<string, FormItemRule[]> = {}
  for (const field of props.fields) {
    const rules: FormItemRule[] = []
    if (field.required) {
      rules.push({ required: true, message: `${field.label}不能为空`, trigger: 'blur' })
    }
    if (field.rules) {
      rules.push(...field.rules)
    }
    if (rules.length > 0) {
      result[field.prop] = rules
    }
  }
  // 合并外部传入的 rules
  if (props.rules) {
    Object.assign(result, props.rules)
  }
  return result
})

/** 获取 select 类型的选项列表 */
function getOptions(field: ProFormField) {
  if (field.dict) {
    return appStore.getDictOptions(field.dict)
  }
  return []
}

/** 更新字段值 */
function updateField(prop: string, value: any) {
  const newModel = { ...props.modelValue, [prop]: value }
  emit('update:modelValue', newModel)
}

/** 表单校验 */
async function validate(): Promise<boolean> {
  if (!formRef.value) return false
  try {
    await formRef.value.validate()
    return true
  } catch {
    return false
  }
}

/** 重置表单 */
function resetFields() {
  formRef.value?.resetFields()
}

/** 清除校验状态 */
function clearValidate(props?: string | string[]) {
  formRef.value?.clearValidate(props)
}

defineExpose({ validate, resetFields, clearValidate, formRef })
</script>
```

- [ ] **Step 2: 提交**

```bash
git add .
git commit -m "feat(components): 添加 ProForm 配置驱动表单组件"
```

---

### Task 12: 登录页

**Files:**
- Create: `frontend/src/views/login/index.vue`

- [ ] **Step 1: 创建 views/login/index.vue**

```vue
<template>
  <div class="login-page">
    <div class="login-card">
      <h2 class="login-title">📋 智能档案管理系统</h2>
      <p class="login-subtitle">克拉玛依市档案馆</p>
      <el-form ref="formRef" :model="form" :rules="rules" @keyup.enter="handleLogin">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="请输入账号"
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            :prefix-icon="Lock"
            size="large"
            show-password
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登录
          </el-button>
        </el-form-item>
      </el-form>
      <div class="login-footer">
        <el-button text @click="$router.push('/public')">返回公众首页</el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormItemRule } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: '',
  password: '',
})

const rules: Record<string, FormItemRule[]> = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    const redirect = (route.query.redirect as string) || authStore.getDefaultPortal()
    router.push(redirect)
  } catch {
    // 错误已在拦截器中处理
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  width: 100%;
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
}

.login-card {
  width: 400px;
  padding: 40px;
  background: #ffffff;
  border-radius: 12px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.15);
}

.login-title {
  text-align: center;
  font-size: 24px;
  color: #303133;
  margin-bottom: 8px;
}

.login-subtitle {
  text-align: center;
  font-size: 14px;
  color: #909399;
  margin-bottom: 32px;
}

.login-footer {
  text-align: center;
  margin-top: 16px;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add .
git commit -m "feat(auth): 添加统一登录页面"
```

---

### Task 13: App.vue、main.ts 和管理概览占位页

**Files:**
- Create: `frontend/src/App.vue`
- Create: `frontend/src/main.ts`
- Create: `frontend/src/utils/index.ts`
- Create: `frontend/src/views/admin/overview/index.vue`
- Create: 所有 admin 子目录占位 `index.vue`（使用相同模板）

- [ ] **Step 1: 创建 utils/index.ts**

```typescript
/** 将对象中 undefined/null/空字符串的键移除 */
export function cleanParams(params: Record<string, any>): Record<string, any> {
  const result: Record<string, any> = {}
  for (const [key, value] of Object.entries(params)) {
    if (value !== undefined && value !== null && value !== '') {
      result[key] = value
    }
  }
  return result
}
```

- [ ] **Step 2: 创建 App.vue**

```vue
<template>
  <router-view />
</template>
```

- [ ] **Step 3: 创建 main.ts**

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'
import './styles/index.scss'

const app = createApp(App)

// 注册 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

app.mount('#app')
```

- [ ] **Step 4: 创建 views/admin/overview/index.vue — 管理概览占位页**

```vue
<template>
  <div>
    <h2>管理概览</h2>
    <p style="color: #909399; margin-top: 8px;">待实现：馆藏总量、本月新增、待办提醒等统计卡片</p>
    <el-row :gutter="20" style="margin-top: 24px;">
      <el-col :span="6">
        <el-card shadow="hover">
          <p style="color: #909399; font-size: 14px;">馆藏总量</p>
          <p style="font-size: 28px; font-weight: 700; color: #303133;">—</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <p style="color: #909399; font-size: 14px;">本月新增</p>
          <p style="font-size: 28px; font-weight: 700; color: #409eff;">—</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <p style="color: #909399; font-size: 14px;">待入库</p>
          <p style="font-size: 28px; font-weight: 700; color: #e6a23c;">—</p>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card shadow="hover">
          <p style="color: #909399; font-size: 14px;">待审批借阅</p>
          <p style="font-size: 28px; font-weight: 700; color: #f56c6c;">—</p>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>
```

- [ ] **Step 5: 创建所有 admin 子目录占位页**

对以下路径创建占位 `index.vue`，内容为相同的模板（仅标题不同）：

```bash
# 占位页模板（替换 TITLE 为对应页面名称）
```

```vue
<template>
  <div>
    <h2>TITLE</h2>
    <p style="color: #909399; margin-top: 8px;">待实现</p>
  </div>
</template>
```

需要创建的占位页列表：
- `views/admin/transfer-reception/index.vue` — 移交验收与电子文件上传
- `views/admin/collection/index.vue` — 征集管理与接收
- `views/admin/pending-archive/index.vue` — 待入库与上架
- `views/admin/archive-management/index.vue` — 档案管理
- `views/admin/warehouse/index.vue` — 库房与架位
- `views/admin/fonds/index.vue` — 全宗管理
- `views/admin/inventory/index.vue` — 档案盘点
- `views/admin/borrow-approval/index.vue` — 借阅审批
- `views/admin/appraisal/index.vue` — 档案鉴定
- `views/admin/destruction/index.vue` — 档案销毁
- `views/admin/approval/index.vue` — 审批工作台
- `views/admin/compilation/index.vue` — 档案编研
- `views/admin/preservation/index.vue` — 档案保存
- `views/admin/statistics/index.vue` — 数据统计
- `views/admin/data-analysis/index.vue` — 数据研判
- `views/admin/user-management/index.vue` — 用户管理
- `views/admin/system-settings/index.vue` — 系统配置

以及非管理门户的占位页：
- `views/transfer/overview/index.vue` — 移交工作台
- `views/transfer/transfer-list/index.vue` — 编制移交清单
- `views/public/index.vue` — 公众首页
- `views/public/search/index.vue` — 公开档案检索
- `views/public/collection/index.vue` — 征集清单
- `views/public/register/index.vue` — 公众注册
- `views/public/overview/index.vue` — 公众概览
- `views/internal/overview/index.vue` — 工作台概览
- `views/internal/search/index.vue` — 档案检索利用

- [ ] **Step 6: 提交**

```bash
git add .
git commit -m "feat: 添加 App.vue、main.ts 和所有页面占位组件"
```

---

### Task 14: 验证

- [ ] **Step 1: 启动开发服务器**

```bash
cd frontend && npm run dev
```

Expected: Vite dev server 在 `http://localhost:3000` 启动成功

- [ ] **Step 2: 验证登录页**

浏览器访问 `http://localhost:3000/login`，确认：
- 登录页正常渲染（渐变背景、登录卡片、表单）
- 页面无控制台报错

- [ ] **Step 3: 验证路由跳转**

浏览器访问 `http://localhost:3000`，确认：
- 自动重定向到 `/public`
- 公众门户布局正常渲染

- [ ] **Step 4: 验证 TypeScript 编译**

```bash
cd frontend && npx vue-tsc --noEmit
```

Expected: 无报错（或仅有已知的类型兼容性警告）

- [ ] **Step 5: 最终提交**

如果有任何修复，提交：

```bash
git add .
git commit -m "fix: 修复脚手架验证中发现的问题"
```
