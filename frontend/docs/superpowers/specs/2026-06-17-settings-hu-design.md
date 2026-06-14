# feat/settings-hu 设计文档：档案保存 · 用户管理 · 系统配置

**日期**：2026-06-17
**负责人**：胡颖
**分支**：`feat/settings-hu`
**阶段**：06-17 至 06-18
**优先级**：P1（用户管理/系统配置）/ P2（保存管理）
**共同验收点**：06-18 完成页面冻结 —— 保存管理、用户配置、系统设置页面可操作，与已合并页面风格一致

## 1. 背景

本轮从最新 `develop`（`4160b1f`）开始，`develop` 已包含以下前置工作：

- 郭一坤 `feat/base-guo`（PR#9）：登录、角色命名、API request 层、mock 切换、字典 API、四类 Layout。
- 郭一坤 `feat/portal-guo`（PR#13）、`feat/search-guo`（PR#17）：门户侧与内部查阅页面。
- 胡颖 `feat/acceptance-hu`（PR#10）、`feat/archive-hu`（PR#15）、`feat/appraisal-hu`（PR#20）：验收、入库、档案管理、上架、鉴定、销毁、审批工作台页面，确立了「顶部指标 + 状态 tab + 原生表格/徽章 + 左列表/右详情或单栏 + Element Plus 仅用于按钮/Message/MessageBox/Dialog」的前端落地风格基线。
- 郭一坤 `feat/storage-guo`（PR#21，已合并）：库房、盘点、借阅审批/出库/归还页面，落地 per-subdomain 文件组织（types+api+mock+utils+views+spec），是本轮最贴近的工程模板。
- 周扬 `feat/storage-zhou`（PR#19）、刘星 `feat/search-liu`（PR#18）、刘星 `feat/borrow-liu`（PR#22）：库房、检索、借阅后端。

本轮聚焦胡颖 06-17 至 06-18 前端任务（项目计划 §3.3 前端任务表第五行）。后端状态：

- **保存管理（§21 备份/四性检测）后端缺失**（项目计划未显式归属，接口冻结节点 06-18），基于 `doc/接口文档.md` 第 21 章 + `doc/数据库设计.md` 11.1（`backup_tasks`）、场景 1/13（`file_check_records`）用 mock 开发。
- **用户/角色（§22.1–22.7）后端缺失**（属周扬 `feat/appraisal-zhou` 06-16 至 06-18 范围，分支未推送），基于第 22 章 + DB 4.3（`users`）、4.4（`roles`/`user_roles`）用 mock 开发。
- **系统配置（§22.8–22.10）后端缺失**（同属周扬 `feat/appraisal-zhou`），基于第 22 章 + DB `system_configs` 用 mock 开发。
- **组织（§17.4）、全宗（§17.1）后端缺失**，本轮仅新增只读列表 api+mock 供用户管理页引用，全宗 CRUD 留待 `/admin/fonds` 页（占位、未归属）后续实现。

> **教训（来自 archive-hu）**：archive-hu 曾有类型错误阻塞 develop 构建，由郭一坤代为修复（commit `ed016d6`）。本轮每个阶段主动执行 `npm run type-check && npm run build`，杜绝再次阻塞集成。

用户已确认两点关键决策（brainstorming 澄清）：

1. **测试深度**：完整测试。每个模块 api/utils/views 配套 `.spec.ts`，覆盖 mock 结构、字段校验、状态门控，与郭一坤 storage-guo / 胡颖 appraisal-hu 约定一致。
2. **组织/全宗数据源**：新增最小只读 `api/organizations.ts`（§17.4）+ `api/fonds.ts`（§17.1）+ mock，供用户管理页只读引用；未来 `/admin/fonds` 页复用扩展，不重复造轮子。

## 2. 目标与范围

### 2.1 本轮实现

| 页面/组件 | 路由 | 说明 | 原型 | 角色 |
|-----------|------|------|------|------|
| 档案保存 | `/admin/preservation` | 重写占位页：保存概览 + 备份范围选择 + 立即备份 + 备份任务表 + 四性检测卡 + 执行四性检测 + 存储空间只读 | `doc/prototype/admin/preservation.html` | back_archivist / sys_admin |
| 用户管理 | `/admin/user-management` | 重写占位页：用户概览 + 账号列表（编辑/重置/禁用）+ 账号编辑表单（含角色勾选）+ 组织/全宗只读引用 + 角色代码 + 权限/审计提示 | `doc/prototype/admin/user-management.html` | sys_admin |
| 系统配置 | `/admin/system-settings` | 重写占位页：配置概览 + 文件上传配置（白名单/大小）+ 能力开关（AI/公开检索）+ 业务默认参数 + 配置项明细表 + 保存/恢复 | `doc/prototype/admin/system-settings.html` | sys_admin |
| 用户管理-账号编辑子组件（按需） | `views/admin/user-management/components/UserEditPanel.vue` | 仅当 index.vue 超 600 行时抽出右栏编辑表单 + 角色勾选 | user-management.html 账号编辑区 | — |

路由 `routes/admin.ts` 与菜单 `adminMenuConfig` 已配置三页（`preservation` 在「编研与保存」组，`user-management`/`system-settings` 在「系统管理」组），本轮**不改路由和菜单**。

### 2.2 不在本轮

- `/admin/fonds` 全宗管理 CRUD 页（占位、未归属，本轮只新增其只读列表 api+mock）。
- `/admin/compilation` 档案编研、统计/研判（属郭一坤 `feat/stats-guo`，06-17 至 06-18）。
- 后端保存/用户/系统配置接口实现（属周扬 `feat/appraisal-zhou` 等）。
- 审计日志独立查询页（§23）：本轮仅在用户管理/系统配置页内以提示形式说明「写入 audit_logs」，不做独立审计日志页。
- 跨机房容灾编排、自动恢复（DB 明确本期不做）。
- 用户逐按钮自定义授权（§22.7 明确本期不做，只维护预设角色勾选）。

## 3. 方案选择

### 3.1 文件组织（per-subdomain，沿用项目约定）

| 方案 | 内容 | 优点 | 缺点 |
|------|------|------|------|
| 拆分子域文件（选定） | `types`+`api`+`mock` 各分 `preservation`/`user-management`/`system-settings`（+只读 `organizations`/`fonds`） | 边界清晰，符合 `reception`/`transfer`/`archive`/`appraisal`/`warehouse` 既有 per-subdomain 约定；三组端点（§21/§22/§17）天然分离 | 文件数较多 |
| 合并为单一 settings 文件 | 三页共用一个文件 | 文件少 | 语义混杂，与项目惯例不一致，难以独立测试 |

**选定**：拆分子域文件。用户实体在既有 `types/user.ts`（`UserInfo`/`LoginResult`，登录侧）基础上，将管理侧 `User`/`UserCreateData` 等放 `types/user-management.ts`，不污染登录类型；角色复用 `types/enums.ts` 的 `RoleKey` + `mock/modules/dictionary.ts` 的 `roles`。

### 3.2 组件粒度

三页原型 HTML 行数：preservation 320、user-management 447、system-settings 409。Vue 化后单文件预计 380–560 行。

| 方案 | 内容 | 优点 | 缺点 |
|------|------|------|------|
| 单 index.vue 为主，按需抽子组件（选定） | preservation/system-settings 单文件；user-management 若超 600 行抽 `UserEditPanel.vue` | 多数页面聚焦单文件可读；避免过早拆分 | 需在实现时判断阈值 |
| 全部拆子组件 | 每页都拆表单/表格子组件 | 主页面更薄 | 三页结构相对扁平，过度拆分增加导航成本 |

**选定**：单文件为主，user-management 右栏编辑表单（含角色勾选 + 多字段）较重，若 index.vue 超 600 行（`frontend/CLAUDE.md` 8.1）则抽 `components/UserEditPanel.vue`。

### 3.3 原型还原原则（沿用 `feat/appraisal-hu` / `feat/storage-guo`）

- 页面布局、视觉层级、网格比例、卡片组织、状态标签、主要文案优先保持原型原貌。
- 实现 Vue 页面时优先搬入原型 `<body>` 内容区结构和页面内样式，原型 JavaScript 转为 `<script setup>` 响应式状态和事件。
- 原型 CSS 类名、设计令牌、公共组件类（`.card`、`.toolbar`、`.button`、`.grid`、`.status`、`.metric`、`.table-wrap`、`.notice`、`.tabs`、`.form-grid`、`.field`、`.mini-list`、`.role-grid`、`.role-check`、`.role-tags`、`.config-row`、`.check-grid`、`.check-card`、`.option-card`、`.backup-options`、`.format-tag`、`.tag-list`、`.switch-line`、`.timeline` 等）优先保留；Element Plus 只在按钮、消息提示、二次确认、日期选择等能补足交互质量处使用。
- 接口文档和数据库设计用于补齐原型未细化的字段、API、mock、加载/空/错误状态和权限边界，**不推翻原型布局**。
- 展示层保留原型业务语言，数据字段和请求参数按接口文档/DB 设计命名（字段冲突时以接口文档 + DB 设计为准，原型示意字段作参考）。

## 4. 架构

沿用当前前端架构：

- 页面组件只写内容区，布局由 `AdminLayout` 承担。
- API 函数统一放 `src/api/`，组件只调 API 函数，不直接 import mock。
- mock 数据放 `src/mock/modules/`，通过 `VITE_USE_MOCK` 控制 mock/真实切换。
- 业务类型放 `src/types/`，枚举值复用并扩展 `types/enums.ts`。
- 页面内用 Vue `ref`/`reactive` 管理局部状态，不引入新全局 store。
- 原型公共样式已全局引入，页面复用公共类。
- `request.ts` 响应拦截器已对 `R<T>` 解包，API 函数返回类型为 `data` 字段内容；mock 只模拟 `data` 内容，分页接口模拟完整 `PageData<T>`。
- 审计字段遵循 `BaseEntity` 约定（`id` number、`createdAt`/`updatedAt` ISO 8601 字符串）。

## 5. 文件结构

```
frontend/src/types/
  enums.ts                       # 扩展：BackupScope/BackupTaskStatus/CheckType/CheckResult/UserType/DataScope/UserStatus/ConfigValueType + 中文映射
  preservation.ts                # BackupTask / FileCheckRecord / 触发检测请求 / 查询参数
  user-management.ts             # User / Role / UserCreateData / UserUpdateData / UserStatusData / UserParams（管理侧）
  system-settings.ts             # SystemConfig / ConfigUpdateItem
  organization.ts                # Organization（只读引用）
  fonds.ts                       # FondsReference（只读引用，仅用户管理页所需字段）

frontend/src/api/
  preservation.ts                # 第 21 章 4 个 /api/admin/backup-tasks、/api/admin/file-check-records、archive-files/{id}/checks 接口
  user-management.ts             # 第 22.1–22.7 共 7 个 /api/admin/users、/api/admin/roles 接口
  system-settings.ts             # 第 22.8–22.10 共 3 个 /api/admin/system-configs 接口
  organizations.ts               # 第 17.4 GET /api/admin/organizations 只读列表
  fonds.ts                       # 第 17.1 GET /api/admin/fonds 只读列表

frontend/src/mock/modules/
  preservation.ts                # 备份任务 + 四性检测记录 mock
  user-management.ts             # 用户 + 角色 mock
  system-settings.ts             # 系统配置项 mock
  organizations.ts               # 组织只读 mock
  fonds.ts                       # 全宗只读 mock

frontend/src/utils/
  userValidation.ts              # 登录名/工号/手机号格式、至少 1 角色、sys_admin 保护、密级/数据范围校验
  systemConfigValidation.ts      # 数值范围（maxSize 1–2048、borrowDays 1–90、阈值 50–100）、editable 门控、白名单去重

frontend/src/views/admin/
  preservation/index.vue                                # 档案保存（重写）
  user-management/index.vue                            # 用户管理（重写）
  user-management/components/UserEditPanel.vue         # 账号编辑表单（仅当 index.vue 超 600 行）
  system-settings/index.vue                            # 系统配置（重写）
```

测试文件（`.spec.ts`）与各 `api/`、`utils/`、`views/` 源文件同目录，对齐既有约定。

## 6. 枚举扩展（`types/enums.ts`）

新增常量与中文映射，复用既有 `RoleKey`（enums.ts:87）与 dictionary `roles`。

```typescript
/** 备份范围（对齐 DB 11.1 backup_scope） */
export const BackupScope = {
  DATABASE: 'database',
  FILES: 'files',
  BOTH: 'both',
} as const

/** 备份任务状态（对齐 DB 11.1 status） */
export const BackupTaskStatus = {
  RUNNING: 'running',
  SUCCESS: 'success',
  FAILED: 'failed',
} as const

/** 四性检测类型（对齐接口文档 §21.4 checkTypes） */
export const CheckType = {
  INTEGRITY: 'integrity',
  USABILITY: 'usability',
  AUTHENTICITY: 'authenticity',
  SECURITY: 'security',
} as const

/** 四性检测结果（对齐场景 1/13，真实性未配置记 not_configured） */
export const CheckResult = {
  PASSED: 'passed',
  FAILED: 'failed',
  NOT_CONFIGURED: 'not_configured',
} as const

/** 用户类型（对齐 DB 4.3 user_type） */
export const UserType = {
  INTERNAL: 'internal',
  PUBLIC: 'public',
} as const

/** 数据范围（对齐 DB 4.3 data_scope） */
export const DataScope = {
  OWN_ORG: 'own_org',
  OWN_FONDS: 'own_fonds',
  ALL: 'all',
} as const

/** 用户状态（对齐 DB 4.3 status） */
export const UserStatus = {
  ACTIVE: 'active',
  DISABLED: 'disabled',
} as const

/** 配置项值类型（对齐接口文档 §22.9 valueType） */
export const ConfigValueType = {
  STRING: 'string',
  NUMBER: 'number',
  BOOLEAN: 'boolean',
  JSON: 'json',
} as const

// 中文映射
export const BackupScopeLabel: Record<string, string> = {
  database: '数据库',
  files: '电子文件',
  both: '同时',
}
export const BackupTaskStatusLabel: Record<string, string> = {
  running: '执行中',
  success: '成功',
  failed: '失败',
}
export const CheckTypeLabel: Record<string, string> = {
  integrity: '完整性',
  usability: '可用性',
  authenticity: '真实性',
  security: '安全性',
}
export const CheckResultLabel: Record<string, string> = {
  passed: 'passed',
  failed: 'failed',
  not_configured: 'not_configured',
}
export const UserTypeLabel: Record<string, string> = {
  internal: '内部',
  public: '公众',
}
export const DataScopeLabel: Record<string, string> = {
  own_org: '本单位',
  own_fonds: '本全宗',
  all: '全部',
}
export const UserStatusLabel: Record<string, string> = {
  active: '启用',
  disabled: '禁用',
}
export const ConfigValueTypeLabel: Record<string, string> = {
  string: 'string',
  number: 'number',
  boolean: 'boolean',
  json: 'json',
}
```

派生值类型沿用既有 `enums.ts` 模式（`export type XxxValue = (typeof Xxx)[keyof typeof Xxx]`）：`BackupScopeValue`、`BackupTaskStatusValue`、`CheckTypeValue`、`CheckResultValue`、`UserTypeValue`、`DataScopeValue`、`UserStatusValue`、`ConfigValueTypeValue`。

> **密级上限**：`maxSecurityLevel` 为 0–4 数字（0 非密 / 1 内部 / 2 秘密 / 3 机密 / 4 绝密），复用 dictionary `securityLevels`，不新增枚举。

## 7. 类型设计

字段名严格对齐 `doc/接口文档.md`（§21/§22/§17）、`doc/数据库设计.md`（4.1–4.4/11.1/`system_configs`）。

### 7.1 `types/preservation.ts`

```typescript
import type { PageData, PageParams } from './api'
import type { BackupScopeValue, BackupTaskStatusValue, CheckResultValue, CheckTypeValue } from './enums'

/** 备份任务查询参数（§21.1） */
export interface BackupTaskParams extends PageParams {
  status?: BackupTaskStatusValue | ''
  backupScope?: BackupScopeValue | ''
}

/** 备份任务（对齐 DB 11.1 backup_tasks） */
export interface BackupTask {
  id: number
  taskNo: string
  backupScope: BackupScopeValue
  status: BackupTaskStatusValue
  backupPath: string
  /** 备份文件大小（字节），失败时为 0 */
  fileSize: number
  /** 校验哈希，失败/执行中时为空 */
  sha256?: string
  startedAt: string
  finishedAt?: string
  /** 失败原因或摘要 */
  message?: string
  createdBy?: number
  createdAt: string
}

/** 创建备份任务请求（§21.2） */
export interface BackupTaskCreateData {
  backupScope: BackupScopeValue
}

/** 四性检测记录查询参数（§21.3） */
export interface FileCheckRecordParams extends PageParams {
  targetType?: string
  targetId?: number
  checkType?: CheckTypeValue | ''
  checkResult?: CheckResultValue | ''
}

/** 四性检测记录（对齐 DB file_check_records） */
export interface FileCheckRecord {
  id: number
  targetType: string
  targetId: number
  checkType: CheckTypeValue
  checkResult: CheckResultValue
  message?: string
  checkedAt: string
}

/** 触发正式文件检测请求（§21.4） */
export interface TriggerCheckData {
  checkTypes: CheckTypeValue[]
}

export type BackupTaskPage = PageData<BackupTask>
export type FileCheckRecordPage = PageData<FileCheckRecord>
```

### 7.2 `types/user-management.ts`

管理侧用户类型，独立于登录侧 `types/user.ts` 的 `UserInfo`，避免污染登录/鉴权语义。

```typescript
import type { PageData, PageParams } from './api'
import type { DataScopeValue, UserStatusValue, UserTypeValue } from './enums'

/** 用户查询参数（§22.1） */
export interface UserParams extends PageParams {
  userType?: UserTypeValue | ''
  roleCode?: string | ''
  organizationId?: number
  status?: UserStatusValue | ''
  keyword?: string
}

/** 预设角色（§22.7 / DB 4.4 roles） */
export interface Role {
  id: number
  roleCode: string
  roleName: string
  description?: string
  enabled: boolean
}

/** 用户列表项 / 详情（§22.1 / §22.3，对齐 DB 4.3 users） */
export interface User {
  id: number
  userType: UserTypeValue
  loginName: string
  employeeNo?: string
  phone?: string
  realName: string
  organizationId?: number
  organizationName?: string
  departmentName?: string
  maxSecurityLevel: number
  dataScope: DataScopeValue
  /** 角色代码集合（user_roles） */
  roleCodes: string[]
  status: UserStatusValue
  createdAt: string
  updatedAt?: string
}

/** 用户详情（§22.3，含最近操作摘要） */
export interface UserDetail extends User {
  recentActions?: Array<{ operationType: string; moduleName: string; operatedAt: string }>
}

/** 创建用户请求（§22.2） */
export interface UserCreateData {
  userType: UserTypeValue
  loginName: string
  employeeNo?: string
  phone?: string
  realName: string
  organizationId?: number
  departmentName?: string
  maxSecurityLevel: number
  dataScope: DataScopeValue
  roleCodes: string[]
  initialPassword: string
}

/** 更新用户请求（§22.4，除密码外字段 + roleCodes） */
export interface UserUpdateData {
  realName?: string
  employeeNo?: string
  phone?: string
  organizationId?: number
  departmentName?: string
  maxSecurityLevel?: number
  dataScope?: DataScopeValue
  roleCodes?: string[]
}

/** 禁用/启用用户请求（§22.5） */
export interface UserStatusData {
  status: UserStatusValue
  reason?: string
}

/** 重置密码请求（§22.6） */
export interface ResetPasswordData {
  newPassword: string
}

export type UserPage = PageData<User>
```

### 7.3 `types/system-settings.ts`

```typescript
import type { ConfigValueTypeValue } from './enums'

/** 系统配置项（§22.8 / DB system_configs） */
export interface SystemConfig {
  configKey: string
  /** 配置值统一以字符串存储，前端按 valueType 解析展示 */
  configValue: string
  valueType: ConfigValueTypeValue
  /** 是否允许页面修改（security.audit_retention 等 editable=false） */
  editable: boolean
  description?: string
  updatedAt?: string
}

/** 单项更新（§22.9） */
export interface ConfigUpdateItem {
  configKey: string
  configValue: string
}

/** 批量更新请求（§22.10） */
export interface ConfigBatchUpdateData {
  items: ConfigUpdateItem[]
}
```

### 7.4 `types/organization.ts`

```typescript
import type { PageParams } from './api'

/** 组织查询参数（§17.4，用户管理页只读，默认全量） */
export interface OrganizationParams extends PageParams {
  status?: 'active' | 'disabled' | ''
  keyword?: string
}

/** 组织（对齐 DB 4.1 organizations） */
export interface Organization {
  id: number
  orgName: string
  /** archive_org / government / enterprise / public_institution */
  orgType: string
  contactName?: string
  contactPhone?: string
  status: 'active' | 'disabled'
}
```

### 7.5 `types/fonds.ts`

仅含用户管理页只读引用所需字段（全宗 CRUD 留待 `/admin/fonds` 页扩展）。

```typescript
import type { PageParams } from './api'

/** 全宗查询参数（§17.1，只读引用） */
export interface FondsParams extends PageParams {
  status?: 'active' | 'disabled' | ''
  keyword?: string
}

/** 全宗引用（对齐 DB 4.2 fonds 子集） */
export interface FondsReference {
  id: number
  fondsNo: string
  fondsName: string
  organizationId: number
  organizationName?: string
  description?: string
  status: 'active' | 'disabled'
}
```

## 8. API 设计

所有函数内部通过 `USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'` 控制 mock/真实请求。真实端点严格对齐接口文档 §21/§22/§17。

### 8.1 `api/preservation.ts`（§21）

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getBackupTasks(params?)` | GET | `/admin/backup-tasks` | 备份任务分页（status/backupScope 过滤） |
| `createBackupTask(data)` | POST | `/admin/backup-tasks` | 创建备份任务（database/files/both） |
| `getFileCheckRecords(params?)` | GET | `/admin/file-check-records` | 四性检测记录分页（targetType/checkType/checkResult 过滤） |
| `triggerFileCheck(fileId, data)` | POST | `/admin/archive-files/{fileId}/checks` | 触发正式文件四性检测，返回检测记录列表 |

> **执行四性检测交互说明**：§21.4 需指定 `fileId`。原型 `preservation.html` 的「执行四性检测」按钮为整页四性状态展示。本轮 mock 下：按钮触发 `triggerFileCheck`，mock 内部以「最近一份正式电子文件」为 `targetId` 派生 4 条检测记录（完整性/可用性/安全性 `passed`，真实性 `not_configured`），并刷新页面四性检测卡与「今日检测任务」指标；真实端点由后端补齐。

### 8.2 `api/user-management.ts`（§22.1–22.7）

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getUsers(params?)` | GET | `/admin/users` | 用户分页（userType/roleCode/organizationId/status/keyword） |
| `createUser(data)` | POST | `/admin/users` | 创建用户（登录名唯一、角色须启用） |
| `getUserDetail(userId)` | GET | `/admin/users/{userId}` | 用户详情 + 最近操作摘要 |
| `updateUser(userId, data)` | PUT | `/admin/users/{userId}` | 更新用户（除密码外字段 + roleCodes） |
| `updateUserStatus(userId, data)` | PUT | `/admin/users/{userId}/status` | 禁用/启用（不可禁用最后一个 sys_admin） |
| `resetUserPassword(userId, data)` | POST | `/admin/users/{userId}/reset-password` | 重置内部用户密码 |
| `getRoles()` | GET | `/admin/roles` | 预设角色列表（本期不做按钮级授权） |

### 8.3 `api/system-settings.ts`（§22.8–22.10）

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getSystemConfigs()` | GET | `/admin/system-configs` | 配置项列表（不返回敏感密钥） |
| `updateSystemConfig(configKey, data)` | PUT | `/admin/system-configs/{configKey}` | 单项更新（须 editable=true） |
| `batchUpdateSystemConfigs(data)` | PUT | `/admin/system-configs` | 批量更新（保存配置主入口） |

### 8.4 `api/organizations.ts`（§17.4，只读）

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getOrganizations(params?)` | GET | `/admin/organizations` | 组织列表（用户管理页只读引用 + 编辑表单所属单位下拉） |

### 8.5 `api/fonds.ts`（§17.1，只读）

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getFonds(params?)` | GET | `/admin/fonds` | 全宗列表（用户管理页只读引用） |

## 9. 页面设计

> **顶部指标卡取数**：接口文档未定义保存/用户/配置的看板聚合接口，三页顶部 `metric` 卡为页面快照数值（与原型静态示意一致）。mock 下用常量或本页列表派生（如「禁用账号」由用户 `status=disabled` 数量派生，「可编辑配置」由 `editable=true` 数量派生，「最近保存」记录最近一次保存时间），后端若补看板接口再改为独立请求；当前不新增 API 函数。

### 9.1 档案保存 `/admin/preservation`

**目标**：管理数据库备份、电子文件备份和四性检测记录，形成可审计的保存管理记录；只记录，不改变正式档案生命周期。

**布局**（还原 `preservation.html`）：

- 顶部 4 个 `metric` 卡：最近数据库备份时间、最近电子文件备份时间、四性检测通过率、今日检测任务数。
- 工具栏「立即备份」「执行四性检测」按钮。
- `preserve-layout` 双栏（左主区 `1fr` + 右 `380px`）：
  - 左列 `stack`：
    - 备份范围卡：`backup-options` 三 `option-card` radio（数据库 database / 电子文件 files / 同时 both）。
    - 备份任务卡：`table-wrap` 表（任务号、范围、状态、路径、大小、校验哈希、说明 7 列），状态徽章用 `BackupTaskStatusLabel` + `.status` 色（success/warning/danger）。
    - 四性检测卡：`check-grid` 四 `check-card`（完整性/可用性/真实性/安全性，状态用 `CheckResultLabel` + `.status` 色，真实性默认 `not_configured` warning）+ `notice`（检测写入 file_check_records，不直接改档案状态）。
  - 右列 `stack`：存储空间卡（`timeline`：MinIO / PostgreSQL / 备份目录，只读）+ 保存边界 `notice warning`（不做容灾编排/自动恢复/不替代鉴定销毁审批）。

**行为**：

- 首次加载 `getBackupTasks()` + `getFileCheckRecords()` → 顶部指标派生（最近库/文件备份取最新对应 scope 任务的时间或耗时，通过率取检测记录 passed 占比，今日检测取 checkedAt 为今天的记录数）；四性卡取最新一轮检测记录。
- 「立即备份」：读备份范围 radio → `createBackupTask({ backupScope })` → 刷新备份任务表（新任务 `running`，mock 延迟置 `success`/按规则 `failed`）→ toast「已创建备份任务，范围：X」。
- 「执行四性检测」：`triggerFileCheck(latestFileId, { checkTypes: [全部四项] })` → 刷新四性卡（真实性 `not_configured`，其余 `passed`，演示 1 项 `failed` 可选）+ 今日检测 +1 → toast。
- 三态：备份任务加载中、无任务、加载失败+重试；四性检测加载中、无记录、加载失败；备份创建中按钮禁用。

### 9.2 用户管理 `/admin/user-management`

**目标**：维护组织、全宗、账号和预设角色；系统管理员负责账号配置，不默认拥有档案正文查看权限。

**布局**（还原 `user-management.html`）：

- 顶部 4 个 `metric` 卡：内部账号、公众账号、禁用账号、系统管理员数（由用户列表派生）。
- 工具栏「新建用户」「保存用户」按钮。
- `user-layout` 双栏（左主区 `1fr` + 右 `390px`）：
  - 左列 `stack`：
    - 账号列表卡：工具栏（角色筛选下拉 + 筛选按钮）+ `table-wrap` 表（登录名、姓名、单位/部门、角色、数据范围、密级上限、状态、操作 8 列）；操作列「编辑」「重置」「禁用」按钮；角色/状态徽章用对应 Label + `.status` 色。
    - `grid two`：组织卡（`mini-list` 只读，orgName + org_type/状态/联系人）+ 全宗卡（`mini-list` 只读，fondsNo · fondsName + 关联组织）。
  - 右列 `stack`：
    - 账号编辑卡（`UserEditPanel` 若抽出）：`form-grid`（登录名、姓名、用户类型 internal/public、联系电话、所属单位下拉来自 organizations、所属部门、数据范围 all/own_org/own_fonds、密级上限 0–4）+ 「预设角色」`role-grid`（6 个内部角色勾选，来自 `getRoles()`）。
    - 角色代码卡：`role-tags`（7 个预设角色代码只读标签）。
    - 权限边界 `notice warning`（不默认查看档案正文，密级/开放/销毁审批由审批工作台）+ 审计留痕 `notice`（禁用/重置/角色变更写 audit_logs）。

**行为**：

- 首次加载 `getUsers()` + `getRoles()` + `getOrganizations()` + `getFonds()` → 顶部指标派生（内部/公众/禁用/sys_admin 数）；角色勾选项来自 `getRoles()`（enabled=true）。
- 「新建用户」：清空编辑表单，默认 `internal_reader` 角色，toast「已进入新建用户状态」。
- 「保存用户」：校验（登录名非空且唯一、至少 1 角色）→ 新建走 `createUser`（含 initialPassword），编辑走 `updateUser` → 刷新列表 + 指标 → toast「用户已保存，角色关系写入 user_roles」。
- 「编辑」：`getUserDetail(id)` 载入编辑表单。
- 「重置」：`ElMessageBox.confirm` 二次确认 → `resetUserPassword(id, { newPassword })` → toast「已生成密码重置记录」。
- 「禁用/启用」：禁用最后一个 sys_admin 前端拦截 + 提示；否则 `ElMessageBox.confirm`（带原因）→ `updateUserStatus(id, { status, reason })` → 刷新状态 + 禁用指标 → toast。
- 角色筛选：前端按 roleCode 过滤列表（mock 下 `getUsers` 亦支持 roleCode 参数）。
- 三态：列表加载中、无用户、加载失败+重试；编辑表单加载中；操作 toast 反馈。

### 9.3 系统配置 `/admin/system-settings`

**目标**：维护上传白名单、上传大小、AI 开关、公开检索开关和业务默认参数；配置变更记录审计日志。

**布局**（还原 `system-settings.html`）：

- 顶部 4 个 `metric` 卡：可编辑配置数（editable=true）、AI 功能（开/关）、公开检索（开/关）、最近保存时间。
- 工具栏「恢复本页示例」「保存配置」按钮。
- `settings-layout` 双栏（左主区 `1fr` + 右 `380px`）：
  - 左列 `stack`：
    - 文件上传配置卡：`config-row`×2 —— 上传格式白名单 `tag-list`（format-tag + 新增格式输入 + 「添加格式」按钮，对应 `upload.allowed_extensions` valueType=json）+ 最大上传大小 number（`upload.max_file_size_mb`，1–2048）。
    - 能力开关卡：`config-row`×2 —— AI 功能开关 checkbox（`ai.enabled`）+ 公开检索开关 checkbox（`public_search.enabled`）+ `notice`（AI 关闭回退人工、公开检索关闭只影响入口不改档案公开字段）。
    - 业务默认参数卡：`config-row`×3 —— 库房占用告警阈值 number（`warehouse.usage_warning_threshold`，50–100）+ 默认借阅天数 number（`borrow.default_days`，1–90）+ 征集捐赠协议文案 textarea（`collection.agreement_text`）。
    - 配置项明细卡：`table-wrap` 表（配置键、当前值、类型、可编辑、说明 5 列），含 `security.audit_retention` 等 `editable=false` 行（展示「否」徽章，不参与编辑）。
  - 右列 `stack`：配置边界卡（`timeline`：上传/AI/公开检索/征集协议边界）+ 审批边界 `notice warning`（不办密级/开放/销毁审批）+ 审计要求 `notice`（保存写 audit_logs）。

**行为**：

- 首次加载 `getSystemConfigs()` → 渲染各配置项当前值（json 类型解析为数组渲染 tag-list、boolean 渲染 checkbox、number/string 渲染输入）+ 顶部指标派生（可编辑数、AI/公开检索开关态、最近保存取最近 updatedAt）。
- 添加格式：输入扩展名 → 去重去点小写 → 加入 tag-list（保存前不落库）→ toast「格式已加入白名单，保存后写入 system_configs」。
- AI/公开检索开关：`change` 即时同步顶部指标 + toast（切换语义提示，不立即落库，随「保存配置」批量提交）。
- 「保存配置」：校验数值范围（maxSize 1–2048、borrowDays 1–90、阈值 50–100）→ 收集所有 editable=true 项的当前值为 `ConfigUpdateItem[]` → `batchUpdateSystemConfigs({ items })` → 刷新明细表 + 最近保存时间 → toast「配置已保存，变更写入 audit_logs」。
- 「恢复本页示例」：还原加载时的原始值（重置本地编辑态）→ toast「已恢复本页示例值」。
- 三态：配置加载中、加载失败+重试、保存中按钮禁用；校验失败 toast 具体原因。

## 10. 跨页联动

- **系统配置 → 库房**：`warehouse.usage_warning_threshold` 为库房页占用告警阈值的配置源；本轮系统配置页维护该值，库房页（已合并）mock 内固定阈值，联调阶段由后端统一读取 system_configs，前端无需改动。
- **系统配置 → 征集**：`collection.agreement_text` 为公众征集提交前展示的捐赠协议文案；本轮系统配置页维护，征集页（已合并）mock 内固定文案，联调阶段后端统一读取。
- **系统配置 → AI/公开检索**：`ai.enabled`/`public_search.enabled` 为全局能力开关，本轮仅在配置页维护，不改动已合并页面的 AI/检索入口（联调阶段后端按开关降级）。
- **用户管理 → 登录/鉴权**：用户/角色数据为登录、菜单、后端鉴权、密级上限、数据范围的数据源；本轮用户管理页维护账号，已合并页面 mock 用户不变。
- 三页各自 mock 数据自闭环（不依赖跳转才能工作），联动仅提升配置一致性，不在前端跨页实时生效（以联调阶段后端为准）。

## 11. 错误处理与边界

- 每页必须处理加载中、空数据、接口失败、表单校验失败（`frontend/CLAUDE.md` AI 约束）。
- 捕获 API 异常后展示页面级 `notice` 或 Element Plus 消息，避免静默失败。
- **保存管理**：同一时间只允许一个运行中备份任务（创建前校验已有 running）；备份范围三选一；四性检测只生成记录不改变档案状态；真实性未配置记 `not_configured`，不伪造 passed。
- **用户管理**：登录名唯一（创建/更新前校验）；至少 1 个角色；不可禁用最后一个 sys_admin；角色须启用；公众用户一般由注册接口创建（编辑表单 userType=public 时提示）；maxSecurityLevel 0–4；重置密码写审计。
- **系统配置**：仅 editable=true 可改（明细表 editable=false 行只读）；数值范围保存前校验；白名单去重去点小写；boolean/json 类型按 valueType 正确序列化为字符串存储；保存写 audit_logs。
- 不可逆/敏感操作（禁用账号、重置密码、保存系统配置）二次确认（`ElMessageBox.confirm`）。
- mock 下备份/检测用前端实现，不落库真实文件。
- 枚举值与状态标签复用 `enums.ts`，三页状态展示一致。
- 本轮不改已合并页面；共享语义（角色）只通过 `enums.ts` `RoleKey` + dictionary `roles` 保持一致。

## 12. Mock 数据

`src/mock/modules/`，结构对齐接口 `data` 字段内容（不包 `R<T>`），分页模拟完整 `PageData<T>`。字段严格对齐 DB 设计。

### 12.1 `mock/modules/preservation.ts`

- `mockBackupTasks(params)`：≥4 个任务覆盖 database/files/both 三范围与 running/success/failed 三状态（≥1 failed 含 message「对象存储连接超时」）；含 fileSize/sha256/startedAt/finishedAt；支持 status/backupScope 过滤 + 分页。
- `mockCreateBackupTask(data)`：若已有 running 任务抛错（演示校验）；否则生成 `taskNo`（`BAK-{自增}`），初始 `running` → 延迟置 `success`（含 backupPath/fileSize/sha256），返回新任务。
- `mockFileCheckRecords(params)`：≥6 条记录覆盖 4 类 checkType 与 passed/failed/not_configured 三结果（真实性至少 1 条 not_configured）；支持 targetType/checkType/checkResult 过滤 + 分页。
- `mockTriggerFileCheck(fileId, data)`：按 checkTypes 生成检测记录（完整性/可用性/安全性 passed、真实性 not_configured），写 checkedAt 为「当前时间」，返回记录列表。
- 顶部指标由上述数据派生（最近库/文件备份取最新对应 scope 任务的 finishedAt；通过率 = passed / 总数；今日检测 = checkedAt 为今天的记录数）。

### 12.2 `mock/modules/user-management.ts`

- `mockUsers(params)`：≥8 个用户覆盖 internal/public、active/disabled、7 种角色（含 2 个 sys_admin，用于「不可禁用最后一个」校验演示）；含 organizationName/departmentName/maxSecurityLevel/dataScope/roleCodes；支持 userType/roleCode/organizationId/status/keyword 过滤 + 分页。
- `mockCreateUser(data)`：loginName 唯一校验（重复抛错）；否则生成新用户（status=active），返回详情。
- `mockUserDetail(id)`：用户 + `recentActions[]`（操作类型/模块/时间）。
- `mockUpdateUser(id, data)`：更新除密码外字段 + roleCodes。
- `mockUpdateUserStatus(id, data)`：禁用时若为最后一个 active 的 sys_admin 抛错；否则切换 active/disabled，写 reason。
- `mockResetUserPassword(id, data)`：返回 true（mock 仅记录）。
- `mockRoles()`：7 个预设角色（对齐 dictionary roles 的 roleCode/roleName + enabled/description）。

### 12.3 `mock/modules/system-settings.ts`

- `mockSystemConfigs()`：≥7 项覆盖 json/boolean/number/string 四类型 + ≥1 项 editable=false（`security.audit_retention`）。关键项：`upload.allowed_extensions`（json：pdf/docx/jpg/png/mp4）、`upload.max_file_size_mb`（number：512）、`ai.enabled`（boolean：true）、`public_search.enabled`（boolean：true）、`warehouse.usage_warning_threshold`（number：85）、`borrow.default_days`（number：14）、`collection.agreement_text`（string：捐赠协议文案）。
- `mockUpdateSystemConfig(key, data)`：editable=false 抛错；否则更新 configValue + updatedAt。
- `mockBatchUpdateSystemConfigs(data)`：逐项 editable 校验 + 更新，返回更新后列表。

### 12.4 `mock/modules/organizations.ts`

- `mockOrganizations(params)`：≥4 个组织覆盖 archive_org/government/enterprise/public_institution 四类型与 active/disabled；含 contactName/contactPhone；支持 status/keyword 过滤 + 分页。用户管理页编辑表单「所属单位」下拉与组织只读 mini-list 共用此数据。

### 12.5 `mock/modules/fonds.ts`

- `mockFonds(params)`：≥3 个全宗（F001 综合全宗/档案馆本馆、F018 财务业务全宗/财务部、F031 科技项目全宗/市科技中心，编码与 collection/archive 页 mock 口径一致）；含 organizationName；支持 status/keyword 过滤 + 分页。

mock 数据需保证口径一致：用户管理页组织/全宗 mini-list 与编辑表单所属单位下拉同源；全宗 fondsNo 与 collection 页一致；角色 roleCode 与 dictionary roles、enums `RoleKey` 一致。

## 13. 测试与验证

### 13.1 单元测试

- `api/preservation.spec.ts`、`api/user-management.spec.ts`、`api/system-settings.spec.ts`、`api/organizations.spec.ts`、`api/fonds.spec.ts`：mock 分支返回结构与类型正确性、过滤逻辑、分页结构。
- `utils/userValidation.spec.ts`：登录名唯一、至少 1 角色、maxSecurityLevel 0–4、dataScope 合法、不可禁用最后一个 sys_admin。
- `utils/systemConfigValidation.spec.ts`：maxSize 1–2048、borrowDays 1–90、阈值 50–100、editable 门控、白名单去重去点小写、boolean/json 序列化为字符串。

### 13.2 组件交互测试

- `/admin/preservation`：备份范围选择；立即备份创建任务行；执行四性检测刷新四性卡（真实性 not_configured）；failed 任务展示原因；指标派生。
- `/admin/user-management`：新建/保存用户（登录名重复拦截、至少 1 角色）；编辑载入；禁用最后一个 sys_admin 被拦截；重置密码二次确认；角色筛选。
- `/admin/system-settings`：加载渲染各类型配置；添加格式去重；AI/公开检索开关同步指标；保存校验数值范围（越界拦截）；恢复示例；editable=false 行只读。

### 13.3 构建验证

```bash
npm run type-check
npm run build
npm run test:unit
```

TypeScript 检查、生产构建、单元测试均需通过（杜绝 archive-hu 式类型错误阻塞集成）。

## 14. 验收口径

- `/admin/preservation`、`/admin/user-management`、`/admin/system-settings` 无「待实现」占位。
- 页面布局、视觉层级、文案、交互结构、状态标签还原对应 HTML 原型；字段补充不改变原型组织方式。
- 档案保存：备份范围三选一；立即备份创建任务行；备份任务表 7 列含 failed 原因；四性检测四卡（真实性 not_configured）+ 执行检测刷新；存储空间只读；保存边界提示。
- 用户管理：4 指标派生；账号列表 8 列 + 编辑/重置/禁用；账号编辑表单（含 6 角色勾选、所属单位下拉）；组织/全宗只读 mini-list；登录名唯一、至少 1 角色、不可禁用最后一个 sys_admin；重置密码二次确认。
- 系统配置：4 指标；上传白名单增删 + 大小；AI/公开检索开关；业务默认三项（阈值/借阅天数/协议文案）；配置明细表含 editable=false 行；保存校验数值范围 + 批量更新；恢复示例。
- 三页覆盖加载中、空数据、接口错误三态；敏感操作二次确认。
- API 函数全部支持 mock/真实切换；mock 字段对齐接口文档/DB 设计；mock 口径一致（角色/组织/全宗）。
- TypeScript 检查、生产构建、单元测试通过。






