# 前端脚手架设计文档

**日期**：2026-06-11
**负责人**：胡颖（前端牵头）
**分支**：`feat/base-hu`
**状态**：已确认

---

## 1. 总体决策

| 决策项 | 选择 | 说明 |
|--------|------|------|
| 项目结构 | 单 Vue 项目，路由区分四个门户 | 共享登录、API、公共组件，减少重复 |
| 布局风格 | 深色侧边栏 + 浅色顶栏 + 灰色内容区 | 适合 20+ 页面的管理后台，Element Plus 主流风格 |
| 门户切换 | 登录后进默认门户，顶栏可切换 | 支持多角色账号，按角色过滤可切换门户 |
| 菜单权限 | 前端静态路由 + 按角色过滤 | 菜单结构前端定义，登录后根据角色过滤显示；后端接口做真正权限校验 |
| 公共组件 | 配置驱动 ProTable / ProForm | 声明式配置生成标准列表页和表单页，统一 loading/分页/空数据/错误处理 |
| 语言 | TypeScript | 类型约束减少联调字段错误，IDE 补全提高开发效率 |
| 技术栈 | Vue 3 + Element Plus + Vite + Pinia + Vue Router + Axios | 与技术架构文档一致 |

---

## 2. 项目目录结构

```
frontend/
├── public/
├── src/
│   ├── api/                    # API 请求模块
│   │   ├── request.ts          # Axios 实例、拦截器、统一错误处理
│   │   ├── auth.ts             # 登录/登出/刷新 token
│   │   └── modules/            # 按业务模块拆分
│   │       ├── transfer.ts
│   │       ├── archive.ts
│   │       ├── search.ts
│   │       ├── borrow.ts
│   │       ├── warehouse.ts
│   │       ├── appraisal.ts
│   │       ├── approval.ts
│   │       ├── compilation.ts
│   │       ├── statistics.ts
│   │       ├── preservation.ts
│   │       ├── user.ts
│   │       ├── config.ts
│   │       └── dictionary.ts
│   ├── components/             # 全局公共组件
│   │   ├── ProTable/           # 配置驱动表格
│   │   │   └── index.vue
│   │   ├── ProForm/            # 配置驱动表单
│   │   │   └── index.vue
│   │   ├── PortalSwitcher/     # 门户切换下拉组件
│   │   │   └── index.vue
│   │   └── DictTag/            # 字典状态标签组件
│   │       └── index.vue
│   ├── composables/            # Vue 组合式函数
│   │   ├── useDict.ts          # 字典加载与翻译
│   │   └── usePermission.ts    # 角色判断工具
│   ├── layouts/                # 四个门户的布局组件
│   │   ├── AdminLayout.vue     # 管理后台：深色侧边栏 + 顶栏
│   │   ├── TransferLayout.vue  # 移交门户
│   │   ├── PublicLayout.vue    # 公众门户：顶部导航
│   │   └── InternalLayout.vue  # 内部门户
│   ├── router/
│   │   ├── index.ts            # 路由主入口，合并四个门户路由
│   │   ├── guards.ts           # 导航守卫（登录检查、角色权限）
│   │   └── routes/
│   │       ├── admin.ts        # 管理后台 18 个页面路由
│   │       ├── transfer.ts     # 移交门户路由
│   │       ├── public.ts       # 公众门户路由
│   │       └── internal.ts     # 内部门户路由
│   ├── stores/                 # Pinia 状态管理
│   │   ├── auth.ts             # 用户信息、token、角色、默认门户
│   │   └── app.ts              # 全局 UI 状态、字典缓存
│   ├── styles/
│   │   ├── variables.scss      # 颜色、间距等 CSS 变量
│   │   └── index.scss          # 全局样式入口
│   ├── types/
│   │   ├── api.d.ts            # 统一响应 ApiResponse、分页 PageData
│   │   ├── enums.ts            # 业务枚举常量及中文映射
│   │   ├── menu.ts             # 菜单配置类型
│   │   ├── user.ts             # 用户信息类型
│   │   └── components.ts       # ProTable/ProForm 类型定义
│   ├── utils/
│   │   └── index.ts            # 通用工具函数
│   ├── views/
│   │   ├── login/              # 统一登录页
│   │   │   └── index.vue
│   │   ├── admin/              # 管理后台页面（胡颖负责）
│   │   │   ├── overview/
│   │   │   ├── transfer-reception/
│   │   │   ├── collection/
│   │   │   ├── pending-archive/
│   │   │   ├── archive-management/
│   │   │   ├── warehouse/
│   │   │   ├── fonds/
│   │   │   ├── inventory/
│   │   │   ├── borrow-approval/
│   │   │   ├── appraisal/
│   │   │   ├── destruction/
│   │   │   ├── approval/
│   │   │   ├── compilation/
│   │   │   ├── preservation/
│   │   │   ├── statistics/
│   │   │   ├── data-analysis/
│   │   │   ├── user-management/
│   │   │   └── system-settings/
│   │   ├── transfer/           # 移交门户页面（郭一坤负责）
│   │   ├── public/             # 公众门户页面（郭一坤负责）
│   │   └── internal/           # 内部门户页面（郭一坤负责）
│   ├── App.vue
│   └── main.ts
├── index.html
├── vite.config.ts
├── tsconfig.json
├── tsconfig.app.json
├── tsconfig.node.json
├── env.d.ts
├── package.json
└── .env.development            # 开发环境 API 地址等
```

**设计原则**：
- `views/` 按门户分目录，胡颖和郭一坤的工作边界清晰，减少 git 冲突
- `components/` 只放两人共用的公共组件
- `api/modules/` 按业务模块拆分，共享 `request.ts` 基础配置
- `types/` 集中管理类型定义和枚举，保证前后端字段一致

---

## 3. 路由与布局体系

### 3.1 路由顶层结构

```
/login                  → Login.vue（统一登录页，无布局壳）
/public/*               → PublicLayout（顶部导航，无需登录即可访问首页和公开检索）
/transfer/*             → TransferLayout（需登录 + transfer_user 角色）
/internal/*             → InternalLayout（需登录 + internal_user 角色）
/admin/*                → AdminLayout（需登录 + admin 相关角色）
/                       → 重定向到 /public
```

### 3.2 角色与默认门户映射

| 角色关键词 | 默认门户 | 说明 |
|-----------|---------|------|
| `front_admin` / `back_admin` | `/admin` | 前台/后台管理员 |
| `leader` | `/admin` | 馆领导进入管理后台 |
| `sys_admin` | `/admin` | 系统管理员 |
| `transfer_user` | `/transfer` | 移交单位经办人 |
| `internal_user` | `/internal` | 内部查阅者 |
| 无角色（公众） | `/public` | 注册后公众用户 |

顶栏右侧门户切换下拉只展示当前用户角色对应的门户。公众用户无切换入口。

### 3.3 导航守卫逻辑

```
用户访问任意路由
  → 未登录且需要登录？ → 跳转 /login，记录来源路径
  → 已登录？
    → 目标路由需要的角色，当前用户是否具备？
      → 有：放行
      → 无：跳转用户默认门户首页
  → /login 页面且已登录？ → 跳转默认门户首页
```

### 3.4 管理后台路由定义

每个页面路由均使用懒加载（`component: () => import(...)`），meta 中标注 `title` 用于面包屑和侧边栏高亮，`roles` 用于权限过滤。

---

## 4. 管理后台侧边栏菜单

### 4.1 菜单分组

```
管理概览                              (overview)

接收管理
  ├─ 移交验收与电子文件上传            (transfer-reception)
  └─ 征集管理与接收                    (collection)

入库与档案
  ├─ 待入库与上架                      (pending-archive)
  └─ 档案管理                          (archive-management)

库房管理
  ├─ 库房与架位                        (warehouse)
  ├─ 全宗管理                          (fonds)
  └─ 档案盘点                          (inventory)

利用服务
  └─ 借阅审批                          (borrow-approval)

鉴定销毁
  ├─ 档案鉴定                          (appraisal)
  └─ 档案销毁                          (destruction)

审批工作台                            (approval)

编研与保存
  ├─ 档案编研                          (compilation)
  └─ 档案保存                          (preservation)

数据中心
  ├─ 数据统计                          (statistics)
  └─ 数据研判                          (data-analysis)

系统管理
  ├─ 用户管理                          (user-management)
  └─ 系统配置                          (system-settings)
```

### 4.2 角色可见菜单

| 菜单组 | 前台管理员 | 后台管理员 | 馆领导 | 系统管理员 |
|--------|:---------:|:---------:|:-----:|:---------:|
| 管理概览 | ✅ | ✅ | ✅ | ✅ |
| 接收管理 | ✅ | ✅ | | |
| 入库与档案 | | ✅ | | |
| 库房管理 | | ✅ | | |
| 利用服务 | ✅ | ✅ | | |
| 鉴定销毁 | | ✅ | | |
| 审批工作台 | | | ✅ | |
| 编研与保存 | | ✅ | | |
| 数据中心 | | ✅ | ✅ | |
| 系统管理 | | | | ✅ |

### 4.3 菜单数据结构

```typescript
interface MenuItem {
  path: string
  title: string
  icon?: string
  roles: string[]           // 哪些角色可见此菜单项
  children?: MenuItem[]     // 子菜单
}
```

菜单配置为静态数组定义在 `router/routes/admin.ts` 的 `meta.menuConfig` 中或独立的 `types/menu.ts` 配置文件中，前端根据当前用户角色过滤后渲染侧边栏。没有子菜单的分组（如「管理概览」「审批工作台」）直接显示为一级菜单项。

---

## 5. 公共组件

### 5.1 ProTable — 配置驱动表格

解决的标准模式：筛选 → 请求 → 表格展示 → 分页 → loading / 空数据 / 错误。

**使用示例**：

```vue
<ProTable
  :columns="columns"
  :search-fields="searchFields"
  :fetch-data="transferApi.getBatchList"
>
  <template #toolbar>
    <el-button type="primary">导出清单</el-button>
  </template>
  <template #action="{ row }">
    <el-button link @click="handleAccept(row)">验收</el-button>
    <el-button link @click="handleView(row)">查看</el-button>
  </template>
</ProTable>
```

**内置职责**：

| 职责 | 说明 |
|------|------|
| 筛选表单 | 根据 `searchFields` 自动渲染搜索区，支持展开/收起 |
| 数据请求 | 调用 `fetchData(pageNo, pageSize, searchParams)`，自动管理分页参数 |
| Loading 状态 | 请求中自动显示表格 loading |
| 空数据 | 无数据时显示 Element Plus 空状态 |
| 错误处理 | 请求失败自动 ElMessage 提示 |
| 分页 | 底部分页器，切换页码自动重新请求 |
| 字典翻译 | `dict` 字段自动从全局字典映射中文展示 |
| 操作列 | `#action` 插槽自定义操作按钮 |
| 工具栏 | `#toolbar` 插槽自定义表格上方操作按钮 |

### 5.2 ProForm — 配置驱动表单

解决的标准模式：表单渲染 → 校验 → 提交 → 重置。

**使用示例**：

```vue
<ProForm
  ref="formRef"
  :fields="formFields"
  :model-value="formData"
  :rules="formRules"
  label-width="120px"
/>
```

**内置职责**：

| 职责 | 说明 |
|------|------|
| 字段渲染 | 根据 `type` 自动选择 el-input / el-select / el-date-picker 等 |
| 校验 | `required` 自动生成必填规则，支持自定义 `rules` |
| 字典下拉 | `dict` 字段自动从全局字典加载选项 |
| 布局 | 透传 Element Plus 表单属性（label-width 等） |
| 插槽 | 每个字段支持 `#field-{prop}` 插槽自定义渲染 |

**组件边界**：ProTable / ProForm 不处理业务逻辑、不封装弹窗/抽屉、不封装特定接口调用。弹窗/抽屉由页面组件自行管理，内部放 ProForm。

### 5.3 其他公共组件

| 组件 | 用途 |
|------|------|
| `PortalSwitcher` | 顶栏右侧门户切换下拉 |
| `DictTag` | 根据 dict key 和 value 渲染带颜色的状态标签 |

### 5.4 类型定义

```typescript
interface ProTableColumn {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  searchable?: boolean
  dict?: string
  sourceType?: string
  formatter?: (row: any) => string
  slot?: string
  fixed?: 'left' | 'right'
  sortable?: boolean
}

interface SearchField {
  prop: string
  label: string
  type: 'input' | 'select' | 'dateRange' | 'date' | 'number'
  dict?: string
  defaultValue?: any
}

interface ProFormField {
  prop: string
  label: string
  type: 'input' | 'textarea' | 'number' | 'select' | 'date' | 'dateRange'
      | 'radio' | 'checkbox' | 'switch' | 'upload'
  required?: boolean
  dict?: string
  rules?: FormItemRule[]
  placeholder?: string
  disabled?: boolean
}
```

---

## 6. API 层与状态管理

### 6.1 Axios 封装（`api/request.ts`）

**统一响应类型**：

```typescript
interface ApiResponse<T = any> {
  code: string
  message: string
  data: T
  traceId: string
}

interface PageData<T = any> {
  records: T[]
  pageNo: number
  pageSize: number
  total: number
  hasNext: boolean
}
```

**拦截器职责**：

| 拦截器 | 职责 |
|--------|------|
| 请求拦截 | 自动注入 `Authorization: Bearer <token>` |
| 响应拦截-成功 | 解包 `response.data`，提取 `data` 字段返回给调用方 |
| 响应拦截-401 | token 过期 → 清除登录状态 → 跳转 `/login` |
| 响应拦截-业务错误 | `code !== 'OK'` → `ElMessage.error(message)` → rejected Promise |
| 响应拦截-网络错误 | 超时/断网 → `ElMessage.error('网络异常')` |

### 6.2 Auth Store（`stores/auth.ts`）

```typescript
interface AuthState {
  token: string | null
  user: UserInfo | null
  roles: string[]
  permissions: string[]
}

interface UserInfo {
  id: number
  username: string
  realName: string
  phone: string
  organizationId: number
  department: string
  securityLevel: number
  dataScope: string
}
```

**核心方法**：
- `login(username, password)` → 调登录接口 → 存 token 和用户信息 → 跳转默认门户
- `logout()` → 清除状态 → 跳转 `/login`
- `getDefaultPortal()` → 根据角色列表返回默认门户路径
- `getAllowedPortals()` → 返回当前用户可切换的门户列表
- `hasRole(role)` / `hasAnyRole(...roles)` → 判断角色

### 6.3 App Store（`stores/app.ts`）

```typescript
interface AppState {
  sidebarCollapsed: boolean
  currentPortal: string
  dictionaries: Record<string, DictItem[]>
}
```

- `toggleSidebar()` — 切换侧边栏折叠
- `loadDictionaries()` — 登录后一次性加载所有字典并缓存
- `getDictLabel(dictKey, value)` — 根据字典 key 和枚举值返回中文文本

### 6.4 枚举常量（`types/enums.ts`）

与接口文档 1.5 节对齐，定义所有业务枚举及中文映射。清单状态中文展示必须结合 `sourceType` 映射（如 `rejected` 在移交侧展示「已回退」，在征集侧展示「已拒绝」）。

---

## 7. 开发分工

| 负责人 | 本阶段任务 | 涉及目录 |
|--------|-----------|---------|
| 胡颖 | 项目初始化、AdminLayout、侧边栏菜单、路由守卫、ProTable/ProForm、API 封装、Auth/App Store、枚举类型、登录页 | `src/` 全局基础设施 + `views/admin/` + `views/login/` |
| 郭一坤 | TransferLayout / PublicLayout / InternalLayout、三个门户的路由和页面 | `layouts/` 三个非管理布局 + `views/transfer/` + `views/public/` + `views/internal/` |

郭一坤的开发基于胡颖搭建的公共组件和 API 层，使用 ProTable / ProForm 编写各门户页面。
