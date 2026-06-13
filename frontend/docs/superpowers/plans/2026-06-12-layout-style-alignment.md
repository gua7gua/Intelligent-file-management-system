# Layout 样式对齐原型设计系统 — 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将四个 Layout 组件的视觉风格从 Element Plus 默认深色侧边栏切换为原型的白色侧边栏 + 青色主色设计系统，使导航和页面内容风格统一。

**Architecture:** 将原型 CSS 文件复制到前端 src 目录下作为全局样式引入。Layout 组件模板直接使用原型 shell.css 定义的类名（`.app-shell`、`.sidebar`、`.topbar`、`.content`、`.public-shell`、`.public-nav`）。删除旧的 SCSS 变量和样式定义。

**Tech Stack:** Vue 3 SFC、CSS（原型样式）、SCSS（仅保留必要的尺寸变量）、Vue Router（router-link 导航）、Pinia（角色过滤）

**Spec:** `frontend/docs/superpowers/specs/2026-06-12-layout-style-alignment-design.md`

---

## File Structure

| 文件 | 操作 | 职责 |
|------|------|------|
| `src/styles/prototype/base.css` | 新建（复制） | CSS 自定义属性、重置、排版 |
| `src/styles/prototype/components.css` | 新建（复制） | .card、.button、.grid 等组件类 |
| `src/styles/prototype/shell.css` | 新建（复制） | .app-shell、.sidebar、.topbar、.content、.public-shell、.public-nav |
| `src/main.ts` | 修改 | 引入三个原型 CSS |
| `src/layouts/AdminLayout.vue` | 重写 | 管理后台布局，app-shell + sidebar |
| `src/layouts/PublicLayout.vue` | 重写 | 公众门户布局，public-shell + public-nav |
| `src/layouts/InternalLayout.vue` | 重写 | 内部查阅布局，app-shell + sidebar |
| `src/layouts/TransferLayout.vue` | 重写 | 移交端布局，app-shell + sidebar |
| `src/styles/variables.scss` | 修改 | 删除颜色变量，保留尺寸变量 |
| `src/styles/index.scss` | 修改 | 删除旧 Layout 样式，保留通用样式 |

---

### Task 1: 复制原型 CSS 文件到前端 src 目录

**Files:**
- Create: `src/styles/prototype/base.css`
- Create: `src/styles/prototype/components.css`
- Create: `src/styles/prototype/shell.css`

- [ ] **Step 1: 创建目录并复制文件**

```bash
mkdir -p src/styles/prototype
cp doc/prototype/common/css/base.css src/styles/prototype/base.css
cp doc/prototype/common/css/components.css src/styles/prototype/components.css
cp doc/prototype/common/css/shell.css src/styles/prototype/shell.css
```

- [ ] **Step 2: 在 main.ts 中引入原型 CSS**

将 `src/main.ts` 修改为：

```typescript
import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import 'element-plus/dist/index.css'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

import App from './App.vue'
import router from './router'

// 原型全局样式（设计系统基准）
import './styles/prototype/base.css'
import './styles/prototype/components.css'
import './styles/prototype/shell.css'

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

- [ ] **Step 3: 验证复制结果**

```bash
diff doc/prototype/common/css/base.css src/styles/prototype/base.css
diff doc/prototype/common/css/components.css src/styles/prototype/components.css
diff doc/prototype/common/css/shell.css src/styles/prototype/shell.css
```

Expected: 无差异输出。

- [ ] **Step 4: 验证构建**

```bash
cd frontend && npx vite build 2>&1 | tail -5
```

Expected: 构建成功，无 CSS 相关报错。

- [ ] **Step 5: 提交**

```bash
git add src/styles/prototype/ src/main.ts
git commit -m "feat(style): 引入原型全局样式作为设计系统基准"
```

---

### Task 2: 清理 variables.scss 和 index.scss

先清理样式基础，避免后续 Layout 改造时新旧样式冲突。

**Files:**
- Modify: `src/styles/variables.scss`
- Modify: `src/styles/index.scss`

- [ ] **Step 1: 清理 variables.scss**

将 `src/styles/variables.scss` 修改为仅保留尺寸变量：

```scss
// Layout 尺寸（原型无折叠，保留用于折叠态）
$sidebar-width: 248px;
$sidebar-collapsed-width: 64px;
```

- [ ] **Step 2: 清理 index.scss**

将 `src/styles/index.scss` 修改为：

```scss
@use './variables' as *;

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
```

删除的内容：全局重置（base.css 已有）、body/html 字体（base.css 已有）、所有 `.admin-*` 和 `.public-*` 布局类（shell.css 已有）、`.collapse-btn` 和 `.user-info`（各 Layout 组件 scoped 内处理）。

- [ ] **Step 3: 验证构建**

```bash
cd frontend && npx vite build 2>&1 | tail -5
```

Expected: 构建成功。

- [ ] **Step 4: 提交**

```bash
git add src/styles/variables.scss src/styles/index.scss
git commit -m "refactor(style): 清理旧 Layout 样式，改用原型 CSS 变量"
```

---

### Task 3: 重写 AdminLayout.vue

**Files:**
- Modify: `src/layouts/AdminLayout.vue`

**参考：** 原型 `doc/prototype/admin/appraisal.html` 的 `.app-shell` 结构；当前 `src/router/routes/admin.ts` 的 `adminMenuConfig` 菜单配置。

- [ ] **Step 1: 重写 AdminLayout.vue**

将 `src/layouts/AdminLayout.vue` 替换为以下完整内容：

```vue
<template>
  <div class="app-shell">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ 'sidebar-collapsed': appStore.sidebarCollapsed }">
      <div class="brand">
        <strong>智能档案管理系统</strong>
        <span>管理后台</span>
      </div>

      <template v-for="section in menuSections" :key="section.title">
        <nav class="nav-section">
          <div class="nav-title">{{ section.title }}</div>
          <router-link
            v-for="item in section.items"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: isActive(item.path) }"
          >
            {{ item.title }}
            <span v-if="item.tag">{{ item.tag }}</span>
          </router-link>
        </nav>
      </template>
    </aside>

    <!-- 主内容区 -->
    <main class="main">
      <header class="topbar">
        <div>
          <strong>{{ route.meta.title || '管理概览' }}</strong>
          <span class="muted"> {{ route.meta.subtitle || '' }}</span>
        </div>
        <div class="role">
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
      </header>

      <section class="content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { adminMenuConfig } from '@/router/routes/admin'
import PortalSwitcher from '@/components/PortalSwitcher/index.vue'
import type { MenuItem } from '@/types/menu'

const route = useRoute()
const authStore = useAuthStore()
const appStore = useAppStore()

/** 侧边栏导航分组（对齐原型 admin 页面结构） */
interface NavItem { path: string; title: string; tag?: string; roles: string[] }
interface NavSection { title: string; items: NavItem[] }

const menuSections = computed<NavSection[]>(() => {
  const roles = authStore.roles

  const sections: NavSection[] = [
    {
      title: '工作台',
      items: [
        { path: '/admin/overview', title: '管理概览', tag: '总览', roles: ['front_admin', 'back_admin', 'leader', 'sys_admin'] },
      ],
    },
    {
      title: '接收入库',
      items: [
        { path: '/admin/transfer-reception', title: '移交验收', tag: '前台', roles: ['front_admin', 'back_admin'] },
        { path: '/admin/collection', title: '征集管理与接收', tag: '征集', roles: ['front_admin', 'back_admin'] },
        { path: '/admin/pending-archive', title: '待入库与上架', tag: '后台', roles: ['back_admin'] },
        { path: '/admin/archive-management', title: '档案管理', tag: '整理', roles: ['back_admin'] },
        { path: '/admin/fonds', title: '全宗管理', tag: '全宗', roles: ['back_admin'] },
      ],
    },
    {
      title: '库房与利用',
      items: [
        { path: '/admin/warehouse', title: '库房管理', tag: '架位', roles: ['back_admin'] },
        { path: '/admin/inventory', title: '档案盘点', tag: '盘点', roles: ['back_admin'] },
        { path: '/admin/borrow-approval', title: '借阅审批', tag: '闭环', roles: ['front_admin', 'back_admin'] },
      ],
    },
    {
      title: '处置与审批',
      items: [
        { path: '/admin/appraisal', title: '档案鉴定', tag: '当前', roles: ['back_admin'] },
        { path: '/admin/destruction', title: '档案销毁', tag: '清册', roles: ['back_admin'] },
        { path: '/admin/approval', title: '审批工作台', tag: '领导', roles: ['leader'] },
      ],
    },
    {
      title: '支撑管理',
      items: [
        { path: '/admin/compilation', title: '档案编研', tag: '编研', roles: ['back_admin'] },
        { path: '/admin/statistics', title: '数据统计', tag: '报表', roles: ['back_admin', 'leader'] },
        { path: '/admin/data-analysis', title: '数据研判', tag: '分析', roles: ['back_admin'] },
        { path: '/admin/preservation', title: '档案保存', tag: '备份', roles: ['back_admin'] },
        { path: '/admin/user-management', title: '用户管理', tag: '系统', roles: ['sys_admin'] },
        { path: '/admin/system-settings', title: '系统配置', tag: '设置', roles: ['sys_admin'] },
      ],
    },
  ]

  // 按角色过滤：过滤掉无权限的项，整个 section 为空则移除
  return sections
    .map((section) => ({
      ...section,
      items: section.items.filter((item) => item.roles.some((r) => roles.includes(r))),
    }))
    .filter((section) => section.items.length > 0)
})

function isActive(path: string): boolean {
  return route.path === path
}
</script>

<style scoped>
/* 折叠态：覆盖 shell.css 的固定宽度 */
.sidebar-collapsed {
  overflow: hidden;
  width: $sidebar-collapsed-width !important;
}

.sidebar-collapsed .brand span,
.sidebar-collapsed .nav-title,
.sidebar-collapsed .nav-link span,
.sidebar-collapsed .nav-link {
  /* 折叠时隐藏文字，只保留结构 */
  font-size: 0;
  line-height: 0;
  overflow: hidden;
}

.sidebar-collapsed .nav-link {
  justify-content: center;
  min-height: 38px;
  padding: 8px;
}

.sidebar-collapsed .brand {
  padding: 12px;
}

.sidebar-collapsed .brand strong {
  font-size: 0;
}

.sidebar-collapsed .brand strong::after {
  content: '📋';
  font-size: 22px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--muted);
  font-size: 13px;
}
</style>
```

**关键说明：**
- 模板使用 shell.css 的类名（`.app-shell`、`.sidebar`、`.main`、`.topbar`、`.content`、`.brand`、`.nav-section`、`.nav-title`、`.nav-link`）
- 导航使用 `<router-link>` + `.active` 类（替代 el-menu 的 router 模式）
- 导航分组硬编码（来自原型 admin 页面），分组内各项通过角色过滤
- 折叠功能用 `.sidebar-collapsed` CSS 类覆盖 shell.css 的固定宽度
- `.role` 类由 shell.css 定义（`display: flex; gap: 8px; align-items: center`）
- scoped 样式只处理折叠态和 `.user-info`

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npx vite build 2>&1 | tail -5
```

Expected: 构建成功。

- [ ] **Step 3: 提交**

```bash
git add src/layouts/AdminLayout.vue
git commit -m "feat(layout): AdminLayout 对齐原型白色侧边栏设计系统"
```

---

### Task 4: 重写 PublicLayout.vue

**Files:**
- Modify: `src/layouts/PublicLayout.vue`

**参考：** 原型 `doc/prototype/public/index.html` 的 `.public-shell` 结构；当前 `src/router/routes/public.ts` 路由。

- [ ] **Step 1: 重写 PublicLayout.vue**

将 `src/layouts/PublicLayout.vue` 替换为以下完整内容：

```vue
<template>
  <div class="public-shell">
    <nav class="public-nav" aria-label="公众门户导航">
      <router-link to="/public/index" class="nav-brand">智能档案馆</router-link>
      <div class="links">
        <router-link to="/public/search">公开检索</router-link>
        <router-link to="/public/collection">征集清单</router-link>
        <router-link to="/public/overview" v-if="authStore.isLoggedIn">公众概览</router-link>
        <template v-if="!authStore.isLoggedIn">
          <router-link to="/login">登录</router-link>
          <router-link to="/public/register">注册</router-link>
        </template>
        <template v-else>
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
        </template>
      </div>
    </nav>

    <main class="public-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import PortalSwitcher from '@/components/PortalSwitcher/index.vue'

const authStore = useAuthStore()
</script>

<style scoped>
.nav-brand {
  font-weight: 700;
  color: var(--text);
}

/* 覆盖 public-nav a 的默认样式，给 router-link 添加适当间距 */
.public-nav .links .user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--muted);
  font-size: 13px;
  padding: 8px 10px;
}
</style>
```

**关键说明：**
- 模板使用 shell.css 的类名（`.public-shell`、`.public-nav`、`.links`、`.public-content`）
- 公众概览只在登录后显示（`requiresAuth` 路由）
- 未登录时显示"登录"和"注册"链接，登录后显示 PortalSwitcher + 用户下拉

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npx vite build 2>&1 | tail -5
```

Expected: 构建成功。

- [ ] **Step 3: 提交**

```bash
git add src/layouts/PublicLayout.vue
git commit -m "feat(layout): PublicLayout 对齐原型 public-shell 设计系统"
```

---

### Task 5: 重写 InternalLayout.vue

**Files:**
- Modify: `src/layouts/InternalLayout.vue`

**参考：** 原型 `doc/prototype/internal/overview.html` 的 `.app-shell` 结构；当前 `src/router/routes/internal.ts` 路由。

- [ ] **Step 1: 重写 InternalLayout.vue**

将 `src/layouts/InternalLayout.vue` 替换为以下完整内容：

```vue
<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <strong>智能档案管理系统</strong>
        <span>内部查阅者门户</span>
      </div>
      <nav class="nav-section" aria-label="内部门户导航">
        <div class="nav-title">个人工作台</div>
        <router-link to="/internal/overview" class="nav-link" :class="{ active: route.path === '/internal/overview' }">
          工作台概览
        </router-link>
        <router-link to="/internal/search" class="nav-link" :class="{ active: route.path === '/internal/search' }">
          档案检索利用
        </router-link>
      </nav>
    </aside>

    <main class="main">
      <header class="topbar">
        <div>
          <strong>{{ route.meta.title || '工作台概览' }}</strong>
        </div>
        <div class="role">
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
      </header>

      <section class="content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { User } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import PortalSwitcher from '@/components/PortalSwitcher/index.vue'

const route = useRoute()
const authStore = useAuthStore()
</script>

<style scoped>
.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--muted);
  font-size: 13px;
}
</style>
```

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npx vite build 2>&1 | tail -5
```

Expected: 构建成功。

- [ ] **Step 3: 提交**

```bash
git add src/layouts/InternalLayout.vue
git commit -m "feat(layout): InternalLayout 对齐原型 app-shell 设计系统"
```

---

### Task 6: 重写 TransferLayout.vue

**Files:**
- Modify: `src/layouts/TransferLayout.vue`

**参考：** 原型 `doc/prototype/transfer/overview.html` 的 `.app-shell` 结构；当前 `src/router/routes/transfer.ts` 路由。

- [ ] **Step 1: 重写 TransferLayout.vue**

将 `src/layouts/TransferLayout.vue` 替换为以下完整内容：

```vue
<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <strong>智能档案管理系统</strong>
        <span>移交单位门户</span>
      </div>
      <nav class="nav-section" aria-label="移交单位门户导航">
        <div class="nav-title">移交业务</div>
        <router-link to="/transfer/overview" class="nav-link" :class="{ active: route.path === '/transfer/overview' }">
          移交工作台 <span>当前</span>
        </router-link>
        <router-link to="/transfer/transfer-list" class="nav-link" :class="{ active: route.path === '/transfer/transfer-list' }">
          编制移交清单 <span>新建</span>
        </router-link>
      </nav>
    </aside>

    <main class="main">
      <header class="topbar">
        <div>
          <strong>{{ route.meta.title || '移交工作台' }}</strong>
        </div>
        <div class="role">
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
      </header>

      <section class="content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { User } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import PortalSwitcher from '@/components/PortalSwitcher/index.vue'

const route = useRoute()
const authStore = useAuthStore()
</script>

<style scoped>
.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--muted);
  font-size: 13px;
}
</style>
```

- [ ] **Step 2: 验证构建**

```bash
cd frontend && npx vite build 2>&1 | tail -5
```

Expected: 构建成功。

- [ ] **Step 3: 提交**

```bash
git add src/layouts/TransferLayout.vue
git commit -m "feat(layout): TransferLayout 对齐原型 app-shell 设计系统"
```

---

### Task 7: 全量构建验证

- [ ] **Step 1: 运行完整构建**

```bash
cd frontend && npx vite build 2>&1
```

Expected: 构建成功，无警告或错误。

- [ ] **Step 2: 启动开发服务器，逐门户检查渲染**

```bash
cd frontend && npx vite --host 0.0.0.0 --port 5173
```

检查点：
1. 访问 `/login` — 登录页面不受影响（独立 Login 页面）
2. 登录后访问 `/admin/overview` — 白色侧边栏、青色导航、毛玻璃顶栏
3. 访问 `/public/index` — 公众导航栏、毛玻璃顶栏
4. 访问 `/internal/overview` — 白色侧边栏、内部查阅者标签
5. 访问 `/transfer/overview` — 白色侧边栏、移交单位标签
6. 侧边栏导航点击可正常跳转
7. 角色切换（PortalSwitcher）可正常工作

- [ ] **Step 3: 提交所有改动（如有遗漏修复）**

```bash
git add -A
git commit -m "fix(layout): 修复样式对齐后的遗漏问题"
```
