# feat/base-guo 实施计划：登录、门户布局、API 封装

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 修复角色命名、登录 API 合约、补齐 Mock 基础设施和字典 API，使登录→门户跳转→角色菜单全链路跑通。

**Architecture:** 在 `feat/base-hu` 已有框架基础上，对齐前端类型/命名与接口文档规范，创建 `src/mock/` 基础设施支持 `VITE_USE_MOCK` 切换，实现 auth 和 dictionary 两个核心 API 模块的 mock 数据。

**Tech Stack:** Vue 3 + TypeScript + Pinia + Vue Router + Axios + Element Plus

**设计文档:** `frontend/docs/superpowers/specs/2026-06-12-base-guo-design.md`

---

## Task 1: 创建功能分支

- [ ] **Step 1: 从 develop 创建 feat/base-guo 分支**

```bash
cd /home/guagua/projects/cupk-3s/guoyikun/Intelligent-file-management-system
git checkout develop
git pull origin develop
git checkout -b feat/base-guo
```

Expected: 当前分支切换到 `feat/base-guo`。

---

## Task 2: 更新类型定义（user.ts + enums.ts）

**Files:**
- Modify: `frontend/src/types/user.ts`
- Modify: `frontend/src/types/enums.ts`

- [ ] **Step 1: 更新 user.ts — 对齐接口文档 §3.1 登录请求/响应类型**

将 `frontend/src/types/user.ts` 完整替换为：

```typescript
/** 用户信息（对齐接口文档 §3.1 返回的 user 对象） */
export interface UserInfo {
  id: number
  realName: string
  userType: 'internal' | 'public'
  roles: string[]
  organizationId: number
  maxSecurityLevel: number
  dataScope: string
}

/** 登录请求参数（对齐接口文档 §3.1） */
export interface LoginParams {
  loginName: string
  password: string
  portal: string
}

/** 登录响应数据（对齐接口文档 §3.1） */
export interface LoginResult {
  token: string
  user: UserInfo
  defaultRoute: string
}
```

- [ ] **Step 2: 更新 enums.ts — 角色命名对齐接口文档**

修改 `frontend/src/types/enums.ts`，替换 `RoleKey` 常量为：

```typescript
/** 角色标识（对齐接口文档） */
export const RoleKey = {
  FRONT_ARCHIVIST: 'front_archivist',
  BACK_ARCHIVIST: 'back_archivist',
  TRANSFER_USER: 'transfer_user',
  INTERNAL_READER: 'internal_reader',
  DIRECTOR: 'director',
  SYS_ADMIN: 'sys_admin',
  PUBLIC_USER: 'public_user',
} as const
```

- [ ] **Step 3: 提交**

```bash
cd /home/guagua/projects/cupk-3s/guoyikun/Intelligent-file-management-system
git add frontend/src/types/user.ts frontend/src/types/enums.ts
git commit -m "feat(base-guo): 对齐 user 和 role 类型定义与接口文档"
```

---

## Task 3: 创建 Mock 基础设施

**Files:**
- Create: `frontend/src/mock/index.ts`
- Create: `frontend/src/mock/modules/auth.ts`
- Create: `frontend/src/mock/modules/dictionary.ts`

- [ ] **Step 1: 创建 mock/index.ts 汇总导出**

```typescript
// src/mock/index.ts
// Mock 数据汇总入口，后续按模块扩展
```

- [ ] **Step 2: 创建 mock/modules/auth.ts**

```typescript
import type { LoginResult, UserInfo } from '@/types/user'

/** 预设测试账号 */
const mockUsers: Record<string, { user: UserInfo; portal: string }> = {
  admin: {
    portal: 'admin',
    user: {
      id: 1,
      realName: '后台管理员',
      userType: 'internal',
      roles: ['back_archivist'],
      organizationId: 1,
      maxSecurityLevel: 4,
      dataScope: 'all',
    },
  },
  front: {
    portal: 'admin',
    user: {
      id: 2,
      realName: '前台管理员',
      userType: 'internal',
      roles: ['front_archivist'],
      organizationId: 1,
      maxSecurityLevel: 2,
      dataScope: 'all',
    },
  },
  director: {
    portal: 'admin',
    user: {
      id: 3,
      realName: '馆领导',
      userType: 'internal',
      roles: ['director'],
      organizationId: 1,
      maxSecurityLevel: 4,
      dataScope: 'all',
    },
  },
  sys: {
    portal: 'admin',
    user: {
      id: 4,
      realName: '系统管理员',
      userType: 'internal',
      roles: ['sys_admin'],
      organizationId: 1,
      maxSecurityLevel: 4,
      dataScope: 'all',
    },
  },
  transfer: {
    portal: 'transfer',
    user: {
      id: 5,
      realName: '移交经办人',
      userType: 'internal',
      roles: ['transfer_user'],
      organizationId: 2,
      maxSecurityLevel: 0,
      dataScope: 'own_org',
    },
  },
  internal: {
    portal: 'internal',
    user: {
      id: 6,
      realName: '内部查阅者',
      userType: 'internal',
      roles: ['internal_reader'],
      organizationId: 1,
      maxSecurityLevel: 2,
      dataScope: 'own_org',
    },
  },
  public: {
    portal: 'public',
    user: {
      id: 7,
      realName: '公众用户',
      userType: 'public',
      roles: ['public_user'],
      organizationId: 0,
      maxSecurityLevel: 0,
      dataScope: 'none',
    },
  },
}

const defaultRouteMap: Record<string, string> = {
  admin: '/admin',
  transfer: '/transfer',
  internal: '/internal',
  public: '/public',
}

/** 模拟登录响应 */
export function mockLoginResponse(portal: string): LoginResult {
  const entry = Object.values(mockUsers).find((u) => u.portal === portal) || mockUsers.admin
  return {
    token: `mock-token-${entry.user.realName}-${Date.now()}`,
    user: entry.user,
    defaultRoute: defaultRouteMap[portal] || '/admin',
  }
}

/** 模拟获取当前用户信息 */
export function mockUserInfo(): { user: UserInfo } {
  const token = localStorage.getItem('token') || ''
  // token 格式: mock-token-{realName}-{timestamp}
  const match = token.match(/^mock-token-(.+)-\d+$/)
  const realName = match ? match[1] : ''
  const entry = Object.values(mockUsers).find((u) => u.user.realName === realName) || mockUsers.admin
  return { user: entry.user }
}
```

- [ ] **Step 3: 创建 mock/modules/dictionary.ts**

```typescript
import type { DictItem } from '@/types/components'

export const mockDictionaries: Record<string, DictItem[]> = {
  roles: [
    { value: 'front_archivist', label: '前台档案管理员' },
    { value: 'back_archivist', label: '后台档案管理员' },
    { value: 'transfer_user', label: '移交单位经办人' },
    { value: 'internal_reader', label: '内部查阅者' },
    { value: 'director', label: '馆领导' },
    { value: 'sys_admin', label: '系统管理员' },
    { value: 'public_user', label: '社会公众' },
  ],
  categories: [
    { value: 1, label: '文书档案' },
    { value: 2, label: '科技档案' },
    { value: 3, label: '会计档案' },
    { value: 4, label: '音像档案' },
    { value: 5, label: '实物档案' },
  ],
  carrierStatuses: [
    { value: 'electronic', label: '纯电子' },
    { value: 'paper_electronic', label: '纸质+电子' },
    { value: 'paper', label: '纯纸质' },
  ],
  retentionPeriods: [
    { value: '10y', label: '10年' },
    { value: '30y', label: '30年' },
    { value: 'permanent', label: '永久' },
  ],
  securityLevels: [
    { value: 0, label: '非密' },
    { value: 1, label: '内部' },
    { value: 2, label: '秘密' },
    { value: 3, label: '机密' },
    { value: 4, label: '绝密' },
  ],
  openStatuses: [
    { value: 'open', label: '公开' },
    { value: 'closed', label: '不公开' },
  ],
  batchStatuses: [
    { value: 'draft', label: '草稿' },
    { value: 'pending_transfer', label: '待移交' },
    { value: 'pending_contact', label: '待联系' },
    { value: 'pending_receive', label: '待接收' },
    { value: 'received', label: '已接收' },
    { value: 'partially_received', label: '部分接收' },
    { value: 'rejected', label: '已回退' },
    { value: 'archived', label: '已入库' },
    { value: 'shelved', label: '已上架' },
  ],
  itemStatuses: [
    { value: 'draft', label: '草稿' },
    { value: 'pending_acceptance', label: '待验收' },
    { value: 'accepted', label: '已接收' },
    { value: 'rejected', label: '已回退' },
    { value: 'pending_archive', label: '待入库' },
    { value: 'archived', label: '已入库' },
  ],
}
```

- [ ] **Step 4: 提交**

```bash
cd /home/guagua/projects/cupk-3s/guoyikun/Intelligent-file-management-system
git add frontend/src/mock/
git commit -m "feat(base-guo): 创建 Mock 基础设施，提供 auth 和 dictionary mock 数据"
```

---

## Task 4: 更新 API 层

**Files:**
- Modify: `frontend/src/api/auth.ts`
- Create: `frontend/src/api/dictionary.ts`

- [ ] **Step 1: 更新 auth.ts — 对齐登录 API 合约 + mock 切换**

将 `frontend/src/api/auth.ts` 完整替换为：

```typescript
import request from './request'
import type { LoginParams, LoginResult } from '@/types/user'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 登录 */
export function loginApi(data: LoginParams): Promise<LoginResult> {
  if (USE_MOCK) {
    return import('@/mock/modules/auth').then((m) => m.mockLoginResponse(data.portal))
  }
  return request.post('/auth/login', data)
}

/** 获取当前用户信息（恢复会话） */
export function getUserInfoApi(): Promise<{ user: LoginResult['user'] }> {
  if (USE_MOCK) {
    return import('@/mock/modules/auth').then((m) => m.mockUserInfo())
  }
  return request.get('/auth/me')
}

/** 登出 */
export function logoutApi(): Promise<void> {
  if (USE_MOCK) {
    return Promise.resolve()
  }
  return request.post('/auth/logout')
}
```

- [ ] **Step 2: 创建 dictionary.ts**

```typescript
import request from './request'
import type { DictItem } from '@/types/components'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 获取全量字典 */
export function getDictionariesApi(): Promise<Record<string, DictItem[]>> {
  if (USE_MOCK) {
    return import('@/mock/modules/dictionary').then((m) => m.mockDictionaries)
  }
  return request.get('/dictionaries')
}
```

- [ ] **Step 3: 提交**

```bash
cd /home/guagua/projects/cupk-3s/guoyikun/Intelligent-file-management-system
git add frontend/src/api/auth.ts frontend/src/api/dictionary.ts
git commit -m "feat(base-guo): 更新 auth API 合约，新建 dictionary API，支持 mock 切换"
```

---

## Task 5: 更新 Auth Store

**Files:**
- Modify: `frontend/src/stores/auth.ts`

- [ ] **Step 1: 重写 auth store — 对齐新的 API 合约和角色命名**

将 `frontend/src/stores/auth.ts` 完整替换为：

```typescript
import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/types/user'
import { loginApi, getUserInfoApi } from '@/api/auth'
import router from '@/router'

/** 管理后台相关角色 */
const ADMIN_ROLES = ['front_archivist', 'back_archivist', 'director', 'sys_admin']

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<UserInfo | null>(null)
  const roles = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)

  /** 登录 */
  async function login(loginName: string, password: string, portal: string) {
    const data = await loginApi({ loginName, password, portal })
    token.value = data.token
    user.value = data.user
    roles.value = data.user.roles
    localStorage.setItem('token', data.token)
  }

  /** 登出 */
  function logout() {
    token.value = null
    user.value = null
    roles.value = []
    localStorage.removeItem('token')
    router.push('/login')
  }

  /** 恢复登录状态（页面刷新时调用） */
  async function restoreSession() {
    if (!token.value) return
    try {
      const data = await getUserInfoApi()
      user.value = data.user
      roles.value = data.user.roles
    } catch {
      logout()
    }
  }

  /** 获取默认门户路径 */
  function getDefaultPortal(): string {
    if (roles.value.some((r) => ADMIN_ROLES.includes(r))) return '/admin'
    if (roles.value.includes('transfer_user')) return '/transfer'
    if (roles.value.includes('internal_reader')) return '/internal'
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
    if (roles.value.includes('internal_reader')) {
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

- [ ] **Step 2: 提交**

```bash
cd /home/guagua/projects/cupk-3s/guoyikun/Intelligent-file-management-system
git add frontend/src/stores/auth.ts
git commit -m "feat(base-guo): auth store 对齐新 API 合约和角色命名"
```

---

## Task 6: 更新 App Store（字典加载）

**Files:**
- Modify: `frontend/src/stores/app.ts`

- [ ] **Step 1: 实现 loadDictionaries()，登录后调用字典 API**

将 `frontend/src/stores/app.ts` 完整替换为：

```typescript
import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DictItem } from '@/types/components'
import { getDictionariesApi } from '@/api/dictionary'

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
      const data = await getDictionariesApi()
      dictionaries.value = data
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

- [ ] **Step 2: 提交**

```bash
cd /home/guagua/projects/cupk-3s/guoyikun/Intelligent-file-management-system
git add frontend/src/stores/app.ts
git commit -m "feat(base-guo): 实现 app store 字典加载，接入 dictionary API"
```

---

## Task 7: 更新登录页面

**Files:**
- Modify: `frontend/src/views/login/index.vue`

- [ ] **Step 1: 更新登录页 — 新角色名、portal 映射、loginName 字段**

将 `frontend/src/views/login/index.vue` 完整替换为：

```vue
<template>
  <main class="auth-shell">
    <!-- 左侧说明区 -->
    <section class="auth-aside" aria-label="系统门户说明">
      <div>
        <p class="status info">统一认证入口</p>
        <h1 class="page-title" style="color: #fff; margin-top: 14px">智能档案管理系统</h1>
        <p style="max-width: 560px; color: rgba(255, 255, 255, 0.82)">
          登录后按角色进入公众门户、内部查阅者门户、移交单位门户或管理后台。菜单展示只用于体验分流，接口权限由后端统一校验。
        </p>
      </div>
      <div class="portal-list">
        <div class="portal-item">
          <strong>移交单位门户</strong>
          <span>编制移交清单、导出打印、查看验收与入库进度。</span>
        </div>
        <div class="portal-item">
          <strong>公众门户</strong>
          <span>检索公开档案、提交征集清单、查看本人下载记录。</span>
        </div>
        <div class="portal-item">
          <strong>内部查阅者门户</strong>
          <span>按密级和数据范围检索档案、预览下载、申请纸质借阅。</span>
        </div>
        <div class="portal-item">
          <strong>管理后台</strong>
          <span>验收、AI 补全入库、上架、借阅审批、鉴定销毁和系统配置。</span>
        </div>
      </div>
    </section>

    <!-- 右侧表单区 -->
    <section class="auth-card" aria-label="登录表单">
      <h1>统一登录</h1>
      <p class="muted">请选择本次登录角色，原型将展示对应门户跳转意图。</p>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <div class="form-field">
          <label for="account">账号 / 手机号 / 工号</label>
          <el-form-item prop="loginName">
            <el-input
              id="account"
              v-model="form.loginName"
              placeholder="请输入账号"
              autocomplete="username"
              size="large"
            />
          </el-form-item>
        </div>
        <div class="form-field">
          <label for="password">密码</label>
          <el-form-item prop="password">
            <el-input
              id="password"
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              autocomplete="current-password"
              show-password
              size="large"
            />
          </el-form-item>
        </div>
        <div class="form-field">
          <label for="role">登录角色</label>
          <el-select id="role" v-model="form.role" placeholder="请选择角色" size="large" style="width: 100%">
            <el-option label="档案管理员（前台）" value="front_archivist" />
            <el-option label="档案管理员（后台）" value="back_archivist" />
            <el-option label="移交单位经办人" value="transfer_user" />
            <el-option label="内部查阅者" value="internal_reader" />
            <el-option label="社会公众" value="public_user" />
            <el-option label="馆领导" value="director" />
            <el-option label="系统管理员" value="sys_admin" />
          </el-select>
          <span class="hint">一人多角色时，后端仍按当前会话角色和数据范围校验。</span>
        </div>

        <div v-if="targetPortal" class="notice">
          登录后将进入：<strong>{{ targetPortal }}</strong>
        </div>

        <div class="actions">
          <el-button type="primary" size="large" :loading="loading" @click="handleLogin">
            登录
          </el-button>
          <router-link to="/public" class="button ghost">公众首页</router-link>
        </div>
      </el-form>

      <div class="login-footer">
        <router-link to="/public/register" class="button secondary">公众注册</router-link>
        <router-link to="/public/forgot-password" class="button ghost">忘记密码</router-link>
      </div>

      <div v-if="loginMessage" class="login-status" aria-live="polite">
        <div :class="['notice', loginMessageType]">{{ loginMessage }}</div>
      </div>

      <p class="hint">公众注册与找回密码只服务社会公众账号；内部账号、角色、状态由系统管理员在用户管理中维护。</p>
    </section>
  </main>
</template>

<script setup lang="ts">
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import type { FormInstance, FormItemRule } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const formRef = ref<FormInstance>()
const loading = ref(false)
const loginMessage = ref('')
const loginMessageType = ref('')

const form = reactive({
  loginName: '',
  password: '',
  role: 'back_archivist',
})

const rules: Record<string, FormItemRule[]> = {
  loginName: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

/** 角色到 portal 的映射 */
const roleToPortal: Record<string, string> = {
  front_archivist: 'admin',
  back_archivist: 'admin',
  director: 'admin',
  sys_admin: 'admin',
  transfer_user: 'transfer',
  internal_reader: 'internal',
  public_user: 'public',
}

/** 角色对应的门户名称 */
const rolePortalNames: Record<string, string> = {
  front_archivist: '管理后台',
  back_archivist: '管理后台',
  director: '管理后台',
  sys_admin: '管理后台',
  transfer_user: '移交单位门户',
  internal_reader: '内部查阅者门户',
  public_user: '公众门户',
}

/** 当前角色对应的门户名称 */
const targetPortal = computed(() => rolePortalNames[form.role] || '')

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  loginMessage.value = ''
  try {
    const portal = roleToPortal[form.role] || 'admin'
    await authStore.login(form.loginName, form.password, portal)
    const redirect = (route.query.redirect as string) || authStore.getDefaultPortal()
    router.push(redirect)
  } catch {
    loginMessage.value = '登录失败，请检查账号和密码。'
    loginMessageType.value = 'danger'
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.portal-list {
  display: grid;
  gap: 12px;
  margin-top: 8px;
}

.portal-item {
  padding: 12px;
  border: 1px solid rgba(255, 255, 255, 0.24);
  border-radius: var(--radius);
  background: rgba(255, 255, 255, 0.1);
  color: rgba(255, 255, 255, 0.88);
}

.portal-item strong {
  display: block;
  margin-bottom: 4px;
  color: #ffffff;
}

.auth-card h1 {
  margin: 0 0 8px;
  font-size: 25px;
}

.login-form {
  display: grid;
  gap: 14px;
  margin-top: 20px;
}

.form-field {
  display: grid;
  gap: 6px;
}

.form-field label {
  color: #34414d;
  font-size: 13px;
  font-weight: 700;
}

.login-form :deep(.el-form-item) {
  margin-bottom: 0;
}

.login-form :deep(.el-input__wrapper),
.login-form :deep(.el-select .el-input__wrapper) {
  border-radius: var(--radius-sm);
}

.login-status {
  min-height: 44px;
  margin-top: 14px;
}

.login-footer {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 18px;
}

.login-footer .button {
  text-decoration: none;
}
</style>
```

- [ ] **Step 2: 提交**

```bash
cd /home/guagua/projects/cupk-3s/guoyikun/Intelligent-file-management-system
git add frontend/src/views/login/index.vue
git commit -m "feat(base-guo): 登录页对齐新角色命名和 API 合约"
```

---

## Task 8: 更新 AdminLayout + 路由 + composables

**Files:**
- Modify: `frontend/src/layouts/AdminLayout.vue`
- Modify: `frontend/src/router/routes/admin.ts`
- Modify: `frontend/src/composables/usePermission.ts`

- [ ] **Step 1: 更新 AdminLayout.vue — 菜单角色过滤**

修改 `frontend/src/layouts/AdminLayout.vue` 中 `menuSections` computed 内所有 `roles` 数组：

```typescript
const menuSections = computed<NavSection[]>(() => {
  const roles = authStore.roles

  const sections: NavSection[] = [
    {
      title: '工作台',
      items: [
        { path: '/admin/overview', title: '管理概览', tag: '总览', roles: ['front_archivist', 'back_archivist', 'director', 'sys_admin'] },
      ],
    },
    {
      title: '接收入库',
      items: [
        { path: '/admin/transfer-reception', title: '移交验收', tag: '前台', roles: ['front_archivist', 'back_archivist'] },
        { path: '/admin/collection', title: '征集管理与接收', tag: '征集', roles: ['front_archivist', 'back_archivist'] },
        { path: '/admin/pending-archive', title: '待入库与上架', tag: '后台', roles: ['back_archivist'] },
        { path: '/admin/archive-management', title: '档案管理', tag: '整理', roles: ['back_archivist'] },
        { path: '/admin/fonds', title: '全宗管理', tag: '全宗', roles: ['back_archivist'] },
      ],
    },
    {
      title: '库房与利用',
      items: [
        { path: '/admin/warehouse', title: '库房管理', tag: '架位', roles: ['back_archivist'] },
        { path: '/admin/inventory', title: '档案盘点', tag: '盘点', roles: ['back_archivist'] },
        { path: '/admin/borrow-approval', title: '借阅审批', tag: '闭环', roles: ['front_archivist', 'back_archivist'] },
      ],
    },
    {
      title: '处置与审批',
      items: [
        { path: '/admin/appraisal', title: '档案鉴定', tag: '当前', roles: ['back_archivist'] },
        { path: '/admin/destruction', title: '档案销毁', tag: '清册', roles: ['back_archivist'] },
        { path: '/admin/approval', title: '审批工作台', tag: '领导', roles: ['director'] },
      ],
    },
    {
      title: '支撑管理',
      items: [
        { path: '/admin/compilation', title: '档案编研', tag: '编研', roles: ['back_archivist'] },
        { path: '/admin/statistics', title: '数据统计', tag: '报表', roles: ['back_archivist', 'director'] },
        { path: '/admin/data-analysis', title: '数据研判', tag: '分析', roles: ['back_archivist'] },
        { path: '/admin/preservation', title: '档案保存', tag: '备份', roles: ['back_archivist'] },
        { path: '/admin/user-management', title: '用户管理', tag: '系统', roles: ['sys_admin'] },
        { path: '/admin/system-settings', title: '系统配置', tag: '设置', roles: ['sys_admin'] },
      ],
    },
  ]

  return sections
    .map((section) => ({
      ...section,
      items: section.items.filter((item) => item.roles.some((r) => roles.includes(r))),
    }))
    .filter((section) => section.items.length > 0)
})
```

注意：只修改 `menuSections` computed 内的 `roles` 数组值，文件其余部分不变。

- [ ] **Step 2: 更新 admin.ts — 菜单配置角色名**

修改 `frontend/src/router/routes/admin.ts` 中 `adminMenuConfig` 内所有 `roles` 数组：

```typescript
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
    ],
  },
]
```

注意：只修改 `adminMenuConfig` 内的 `roles` 数组值，路由定义部分不变。

- [ ] **Step 3: 更新 usePermission.ts — 角色判断**

将 `frontend/src/composables/usePermission.ts` 中 `isAdmin()` 的角色数组更新：

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
    return authStore.hasAnyRole('front_archivist', 'back_archivist', 'director', 'sys_admin')
  }

  return { hasRole, hasAnyRole, isAdmin }
}
```

- [ ] **Step 4: 提交**

```bash
cd /home/guagua/projects/cupk-3s/guoyikun/Intelligent-file-management-system
git add frontend/src/layouts/AdminLayout.vue frontend/src/router/routes/admin.ts frontend/src/composables/usePermission.ts
git commit -m "feat(base-guo): AdminLayout、路由菜单、usePermission 角色命名对齐接口文档"
```

---

## Task 9: 环境配置

**Files:**
- Modify: `frontend/.env.development`

- [ ] **Step 1: 添加 VITE_USE_MOCK**

在 `frontend/.env.development` 中追加：

```env
VITE_USE_MOCK=true
```

完整文件内容应为：

```env
VITE_API_BASE_URL=/api
VITE_APP_TITLE=智能档案管理系统
VITE_USE_MOCK=true
```

- [ ] **Step 2: 提交**

```bash
cd /home/guagua/projects/cupk-3s/guoyikun/Intelligent-file-management-system
git add frontend/.env.development
git commit -m "feat(base-guo): 添加 VITE_USE_MOCK 环境变量"
```

---

## Task 10: 构建验证

- [ ] **Step 1: 安装依赖**

```bash
cd /home/guagua/projects/cupk-3s/guoyikun/Intelligent-file-management-system/frontend
npm install
```

Expected: 无报错，依赖安装成功。

- [ ] **Step 2: TypeScript 类型检查**

```bash
cd /home/guagua/projects/cupk-3s/guoyikun/Intelligent-file-management-system/frontend
npx vue-tsc --noEmit
```

Expected: 无类型错误。如有错误，根据报错信息修复类型引用。

- [ ] **Step 3: Vite 构建验证**

```bash
cd /home/guagua/projects/cupk-3s/guoyikun/Intelligent-file-management-system/frontend
npx vite build
```

Expected: 构建成功，无编译错误。

- [ ] **Step 4: 启动开发服务器并手动验证**

```bash
cd /home/guagua/projects/cupk-3s/guoyikun/Intelligent-file-management-system/frontend
npx vite --host
```

验证以下流程：

1. 访问 `/login` → 登录页正常显示，角色下拉显示新命名
2. 选择"档案管理员（后台）" → 显示"登录后将进入：管理后台"
3. 输入任意账号密码，点击登录 → 跳转到 `/admin/overview`
4. 管理后台侧边栏菜单正常显示
5. 刷新页面 → 会话恢复正常（mock 模式）
6. 退出登录 → 回到登录页
7. 选择"移交单位经办人"登录 → 跳转到 `/transfer/overview`
8. 选择"内部查阅者"登录 → 跳转到 `/internal/overview`
9. 选择"社会公众"登录 → 跳转到 `/public/index`
10. 门户切换器正常工作

- [ ] **Step 5: 修复发现的问题（如有）并提交**

```bash
cd /home/guagua/projects/cupk-3s/guoyikun/Intelligent-file-management-system
git add -A
git commit -m "fix(base-guo): 修复构建或运行时发现的问题"
```

---

## Task 11: 推送到远端

- [ ] **Step 1: 推送 feat/base-guo 分支**

```bash
cd /home/guagua/projects/cupk-3s/guoyikun/Intelligent-file-management-system
git push origin feat/base-guo
```

Expected: 推送成功。

- [ ] **Step 2: 验证 git log**

```bash
git log --oneline feat/base-guo --not develop
```

Expected: 显示约 6-8 个提交，均为 `feat(base-guo):` 前缀。
