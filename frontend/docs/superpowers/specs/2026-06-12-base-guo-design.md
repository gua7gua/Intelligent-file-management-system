# feat/base-guo 设计文档：登录、门户布局、API 封装

**日期**：2026-06-12
**负责人**：郭一坤
**分支**：`feat/base-guo`
**阶段**：06-10 ~ 06-11
**验收标准**：所有门户可进入，菜单按角色展示

## 1. 背景

`feat/base-hu`（胡颖，已合并到 develop）已搭建前端基础框架：四个 Layout、路由体系、登录页、Auth/App Store、API request 层、所有视图存根页面。

`feat/base-guo` 在此基础上修复角色命名不一致、登录 API 合约偏差、补齐 Mock 基础设施和字典 API，使登录→门户跳转→角色菜单全链路可跑通。

## 2. 角色命名统一

### 2.1 映射关系

| 现有前端命名 | 接口文档命名 | 说明 |
|-------------|-------------|------|
| `front_admin` | `front_archivist` | 前台档案管理员 |
| `back_admin` | `back_archivist` | 后台档案管理员 |
| `leader` | `director` | 馆领导 |
| `internal_user` | `internal_reader` | 内部查阅者 |
| `transfer_user` | `transfer_user` | 不变 |
| `sys_admin` | `sys_admin` | 不变 |
| `public_user` | `public_user` | 不变 |

### 2.2 涉及文件

| 文件 | 改动 |
|------|------|
| `src/types/enums.ts` | `RoleKey` 常量值更新 |
| `src/stores/auth.ts` | `ADMIN_ROLES` 数组、`getDefaultPortal()`、`getAllowedPortals()` 中的角色引用 |
| `src/layouts/AdminLayout.vue` | 侧边栏菜单项 `roles` 过滤条件 |
| `src/router/routes/admin.ts` | `adminMenuConfig` 中每个菜单项的 `roles` |
| `src/views/login/index.vue` | 角色下拉选项 `value` 值 |
| `src/composables/usePermission.ts` | `isAdmin()` 中的角色判断 |

## 3. 登录 API 合约对齐

### 3.1 类型变更

`src/types/user.ts`：

```typescript
// 请求参数（对齐接口文档 §3.1）
interface LoginParams {
  loginName: string   // 登录名、工号或手机号
  password: string
  portal: string      // public | internal | transfer | admin | approval
}

// 响应数据
interface LoginResult {
  token: string
  user: UserInfo
  defaultRoute: string
}

// 用户信息（对齐接口文档 §3.1 返回的 user 对象）
interface UserInfo {
  id: number
  realName: string
  userType: 'internal' | 'public'
  roles: string[]
  organizationId: number
  maxSecurityLevel: number
  dataScope: string
}
```

### 3.2 关联修改

- **`src/api/auth.ts`**：`loginApi` 发送 `{ loginName, password, portal }`
- **`src/stores/auth.ts`**：
  - `login()` 参数改为 `(loginName, password, portal)`
  - 从 `data.user.roles` 提取角色列表
  - 用 `data.defaultRoute` 做跳转
  - 去掉独立的 `permissions` ref（后端不返回独立权限列表）
- **`src/views/login/index.vue`**：
  - 角色选择器 `value` 映射到 `portal` 字段
  - 提交时传 `loginName` 而非 `username`

### 3.3 角色到 portal 映射

```typescript
const roleToPortal: Record<string, string> = {
  front_archivist: 'admin',
  back_archivist:  'admin',
  director:        'admin',
  sys_admin:       'admin',
  transfer_user:   'transfer',
  internal_reader: 'internal',
  public_user:     'public',
}
```

## 4. Mock 基础设施

### 4.1 目录结构

```
src/mock/
  index.ts              # 汇总导出（预留）
  modules/
    auth.ts             # 登录、获取用户信息 mock
    dictionary.ts       # 字典 mock
```

### 4.2 auth mock

提供预设测试账号，按 `portal` 返回对应角色和默认路由：

| 测试 loginName | portal | 返回 roles | defaultRoute |
|----------------|--------|-----------|--------------|
| `admin` | `admin` | `['back_archivist']` | `/admin` |
| `front` | `admin` | `['front_archivist']` | `/admin` |
| `director` | `admin` | `['director']` | `/admin` |
| `sys` | `admin` | `['sys_admin']` | `/admin` |
| `transfer` | `transfer` | `['transfer_user']` | `/transfer` |
| `internal` | `internal` | `['internal_reader']` | `/internal` |
| `public` | `public` | `['public_user']` | `/public` |

函数签名：
- `mockLoginResponse(portal: string): Promise<LoginResult>` — 模拟登录响应
- `mockUserInfo(portal: string): Promise<{ user: UserInfo }>` — 模拟 `/auth/me` 响应

### 4.3 dictionary mock

`mockDictionaries`：按接口文档 §2.1 格式返回完整字典数据，包含：
- `roles`：全部 7 个角色
- `categories`：文书、会计、音像、实物等门类
- `carrierStatuses`：electronic / paper_electronic / paper
- `retentionPeriods`：10y / 30y / permanent
- `securityLevels`：0~4
- `openStatuses`：open / closed
- `batchStatuses`：全部 8 个状态
- `itemStatuses`：全部 6 个状态
- `systemConfigs`：上传白名单和大小限制

### 4.4 API 层对接

遵循 `frontend/CLAUDE.md` 的 mock 切换约定：

```typescript
const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

export function loginApi(data: LoginParams): Promise<LoginResult> {
  if (USE_MOCK) {
    return import('@/mock/modules/auth').then(m => m.mockLoginResponse(data.portal))
  }
  return request.post('/auth/login', data)
}
```

### 4.5 环境配置

`.env.development` 添加：

```env
VITE_USE_MOCK=true
```

## 5. 字典 API + App Store 补完

### 5.1 新建 `src/api/dictionary.ts`

```typescript
export function getDictionariesApi(): Promise<Record<string, DictItem[]>>
```

支持 mock 切换，mock 时返回 `mockDictionaries`。

### 5.2 修改 `src/stores/app.ts`

`loadDictionaries()` 实现：
- 调用 `getDictionariesApi()`
- 将返回的字典对象转换为 `DictItem[]` 格式
- 存入 `dictionaries` ref
- 失败不阻塞

### 5.3 修改 `src/stores/auth.ts`

- `login()` 成功后调用 `appStore.loadDictionaries()`
- `restoreSession()` 成功后调用 `appStore.loadDictionaries()`

## 6. 改动范围总结

| 模块 | 文件 | 新建/修改 |
|------|------|-----------|
| 角色命名统一 | enums.ts, auth store, AdminLayout, admin.ts, login/index.vue, usePermission.ts | 修改 6 文件 |
| 登录合约对齐 | user.ts, auth store, auth API, login/index.vue | 修改 4 文件（auth store 与模块 1 共享） |
| Mock 基础设施 | src/mock/index.ts, src/mock/modules/auth.ts, src/mock/modules/dictionary.ts | 新建 3 文件 |
| 字典 API 补完 | src/api/dictionary.ts, app store | 新建 1 + 修改 1 文件 |
| 环境配置 | .env.development | 修改 1 文件 |

**不在范围内**：具体业务页面的 API 封装和 mock（如 transfer batches、public search 等），留给后续 `feat/portal-guo`、`feat/search-guo` 等分支按需创建。
