# feat/search-guo 设计文档：内部查阅者检索与借阅申请

**日期**：2026-06-13
**负责人**：郭一坤
**分支**：`feat/search-guo`
**阶段**：06-13 至 06-15
**共同验收点**：入库与利用页面可联动 —— 内部检索、预览、下载、借阅申请主流程可操作

## 1. 背景

本轮从最新 `develop`（37e25be）开始，`develop` 已包含以下前置工作：

- 郭一坤 `feat/base-guo`（PR#9）：登录、角色命名、API request 层、mock 切换、字典 API、四类 Layout。
- 郭一坤 `feat/portal-guo`（PR#13）：移交门户、公众门户页面，公开检索、公众注册/忘记密码，以及 `types/public.ts`、`api/public.ts`、`mock/modules/public.ts`。
- 胡颖 `feat/acceptance-hu`（PR#10）：管理后台验收、电子文件上传、回退回执、征集管理页面。
- 胡颖 `feat/archive-hu`（PR#15）：待入库、AI 建议确认、档案管理、上架页面，以及 `types/archive.ts`、`api/archive.ts`、`mock/modules/archive.ts`。
- 周扬 `feat/transfer-zhou`（PR#8）、`feat/archive-zhou`（PR#16）：清单验收、入库与档案管理后端接口。
- 刘星 `feat/base-liu`（PR#3）、`feat/file-liu`（PR#12）：OpenAPI、AI 客户端、统一异常、电子文件暂存后端。

本轮聚焦郭一坤 06-13 至 06-15 前端任务。后端 `feat/search-liu`（内部检索/AI/下载审计/借阅接口）尚未合并，本轮全部基于 `doc/接口文档.md` 第 11 章「内部查阅者检索与借阅申请」用 mock 开发，后端就绪后将 `VITE_USE_MOCK` 切为 `false` 即可对接，组件代码无需改动。

用户已确认两点关键决策：

1. 纳入「工作台概览 `/internal/overview`」的基础版（避免门户入口空占位，增强版留待 `feat/stats-guo`）。
2. 档案详情以「右侧详情面板」为主（还原原型），同时注册独立路由 `/internal/archives/:id` 复用同一详情组件，供工作台「继续查看」跳转与深链使用。

## 2. 目标与范围

### 2.1 本轮实现

| 页面/组件 | 路由 | 说明 | 原型 |
|-----------|------|------|------|
| 工作台概览 | `/internal/overview` | 重写占位页 | `doc/prototype/internal/overview.html` |
| 档案检索利用 | `/internal/search` | 重写占位页，含 AI 检索、结构化检索、结果表、右侧详情面板 | `doc/prototype/internal/search.html` |
| 档案详情 | `/internal/archives/:id` | 新增独立路由，复用详情面板组件 | 检索原型右侧 detail-panel |
| 我的借阅申请 | `/internal/borrow-requests` | 新增列表 + 详情 + 导出凭证 | 工作台原型 `#borrow` 区块扩展 |
| 详情面板组件 | `views/internal/components/ArchiveDetailPanel.vue` | 元数据 + 电子文件 + 预览/下载 + 借阅申请表单 | 检索原型右侧 detail-panel |

同时更新 `routes/internal.ts`（新增两条子路由）和 `InternalLayout.vue`（导航补「我的借阅申请」）。

### 2.2 不在本轮

- 借阅审批、核验凭证、出库、归还（admin 端 `/admin/borrow-requests`，属 `feat/storage-guo`，06-15 至 06-17）。
- 库房、盘点页面（`feat/storage-guo`）。
- 统计、研判、编研、工作台增强（`feat/stats-guo`，06-17 至 06-18）。
- 胡颖已完成的后台入库/档案管理页面改造。
- 后端检索/借阅接口实现（刘星 `feat/search-liu`）。

## 3. 方案选择

### 3.1 备选方案

| 方案 | 内容 | 优点 | 风险 |
|------|------|------|------|
| 独立 internal 子域文件 + 复用详情组件 | 新建 `types/internal.ts`、`api/internal.ts`、`mock/modules/internal.ts`；详情抽独立组件在检索页和独立路由复用 | 模块边界清晰，三端类型不混淆，符合项目按模块拆分约定 | 文件略多 |
| 检索并入 `archive.ts` | 内部检索复用 admin 档案 API 和类型 | 文件少 | 语义混淆：admin 是档案管理端、internal 是利用端，可见字段（架位）和利用标记（canBorrow）不同 |
| 详情不抽组件 | 检索页和独立详情页各自内联详情 markup | 省一个组件文件 | 详情 markup 和借阅表单逻辑重复，后续维护成本高 |

### 3.2 选定方案

采用「独立 internal 子域文件 + 复用详情组件」。

- `types/internal.ts` 定义内部查阅专属类型，**不复用** `types/archive.ts`：admin 端 `ArchiveRecord` 含 `locationCode`/`boxNo` 等架位信息，而内部查阅按 M08 约束「不展示具体库房架位」；内部检索结果还需 `canPreview`/`canDownload`/`canBorrow` 等利用标记。三端（admin/public/internal）可见字段与利用语义不同，独立类型更准确。
- 借阅状态、载体、密级、保管期限、开放状态、来源等枚举值复用 `types/enums.ts`。
- 详情面板抽为 `ArchiveDetailPanel.vue`，在检索页右侧与 `/internal/archives/:id` 两处复用（Q2-A 决策）。

### 3.3 原型还原原则（沿用 `feat/portal-guo`）

- 页面布局、视觉层级、网格比例、卡片组织、表格/详情区关系、流程提示、状态标签和主要文案优先保持原型原貌。
- 实现 Vue 页面时优先搬入原型 `<body>` 内容区结构和页面内样式，再把原型 JavaScript 转为 `<script setup>` 响应式状态和事件。
- 原型已有的 CSS 类名、设计令牌和公共组件类（`.card`、`.toolbar`、`.button`、`.grid`、`.status`、`.metric`、`.table-wrap`、`.notice`、`.filter-group` 等）优先保留；Element Plus 只在表单校验、消息提示、日期选择、对话框/抽屉等能补足交互质量的地方使用。
- 接口文档和业务文档用于补齐原型未细化的字段、API、mock、加载/空数据/错误状态和权限边界，不用于推翻原型布局与交互结构。
- 展示层保留原型业务语言（如「利用状态」「可申请」），数据字段和请求参数按接口文档命名。

## 4. 架构

沿用当前前端架构：

- 页面组件只写内容区，布局继续由 `InternalLayout` 承担。
- API 函数统一放在 `src/api/`，组件只调用 API 函数，不直接 import mock。
- mock 数据放在 `src/mock/modules/`，通过 `VITE_USE_MOCK` 控制 mock/真实请求切换。
- 业务类型放在 `src/types/`，枚举值复用 `types/enums.ts`。
- 页面内部使用 Vue `ref`/`reactive` 管理局部状态，不引入新的全局 store。
- 原型公共样式已全局引入，页面复用 `.card`、`.toolbar`、`.button`、`.grid`、`.status`、`.metric`、`.table-wrap`、`.notice` 等类。
- `request.ts` 响应拦截器已对 `R<T>` 解包，API 函数返回类型为 `data` 字段内容；mock 只模拟 `data` 内容，分页接口模拟完整 `PageData<T>`。

## 5. 文件结构

```
frontend/src/types/
  internal.ts              # 内部查阅：检索/详情/借阅/工作台/AI 类型

frontend/src/api/
  internal.ts              # 第 11 章 10 个 /api/internal/* 接口封装

frontend/src/mock/modules/
  internal.ts              # 内部查阅 mock 数据

frontend/src/utils/
  borrowValidation.ts      # 借阅申请表单校验

frontend/src/views/internal/
  overview/index.vue                    # 工作台概览（重写）
  search/index.vue                      # 档案检索利用（重写）
  archives/detail.vue                   # 档案详情独立页（包裹详情面板）
  borrow-requests/index.vue             # 我的借阅申请
  components/ArchiveDetailPanel.vue     # 详情面板（复用组件）

frontend/src/router/routes/internal.ts  # 新增 archives/:id、borrow-requests
frontend/src/layouts/InternalLayout.vue # 导航补「我的借阅申请」
```

## 6. API 设计

`src/api/internal.ts`，严格对应 `doc/接口文档.md` 第 11 章。所有函数走 `USE_MOCK` 分支。

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getInternalDashboard()` | GET | `/internal/dashboard` | 工作台：最近查阅、我的借阅申请、当前借阅、逾期提示、下载记录、权限范围 |
| `searchInternalArchives(params)` | GET | `/internal/archives/search` | 内部档案检索，后端按密级/单位/全宗硬过滤，不返回架位 |
| `getInternalArchiveDetail(archiveId)` | GET | `/internal/archives/{archiveId}` | 内部档案详情，写 `view_metadata` 访问日志 |
| `generateInternalAiQuery(data)` | POST | `/internal/archives/ai-query` | AI 自然语言转内部检索条件 JSON |
| `previewInternalFile(fileId)` | GET | `/internal/archive-files/{fileId}/preview` | 预签名 URL 或文件流，重新鉴权 + 访问日志 |
| `downloadInternalFile(fileId)` | GET | `/internal/archive-files/{fileId}/download` | 文件流（`responseType: 'blob'`），重新鉴权 + 访问日志 |
| `createBorrowRequest(data)` | POST | `/internal/borrow-requests` | 提交纸质借阅申请，生成待审批记录 |
| `getMyBorrowRequests(params)` | GET | `/internal/borrow-requests` | 我的借阅申请列表，支持 status/keyword 分页 |
| `getBorrowRequestDetail(requestId)` | GET | `/internal/borrow-requests/{requestId}` | 借阅申请详情（审批/凭证/出库/归还节点） |
| `exportBorrowVoucher(requestId)` | GET | `/internal/borrow-requests/{requestId}/voucher` | 导出借阅凭证 PDF（`responseType: 'blob'`） |

请求/返回类型见第 7 节。`previewInternalFile` 在 mock 下返回文本预览内容；真实接口返回预签名 URL 时，由 `request` 透传。`downloadInternalFile` 与 `exportBorrowVoucher` 均返回 `Blob`，前端生成下载链接。

## 7. 类型设计

`src/types/internal.ts`

```typescript
import type { PageData, PageParams } from './api'
import type { CarrierStatusValue, OpenStatusValue, RetentionPeriodValue } from './transfer'
import {
  BorrowStatus,
  CarrierStatus,
  OpenStatus,
  RetentionPeriod,
  SecurityLevel,
  SourceType,
} from './enums'

export type BorrowStatusValue = (typeof BorrowStatus)[keyof typeof BorrowStatus]
export type SourceTypeValue = (typeof SourceType)[keyof typeof SourceType]

/** 内部档案检索参数（公开检索参数 + 密级/开放/借阅/实体状态等内部维度） */
export interface InternalSearchParams extends PageParams {
  keyword?: string
  archiveNo?: string
  title?: string
  responsibleText?: string
  categoryId?: number
  fondsId?: number
  organizationName?: string
  tagIds?: string
  formedYearStart?: number
  formedYearEnd?: number
  formedDateStart?: string
  formedDateEnd?: string
  archivedAtStart?: string
  sourceType?: SourceTypeValue
  carrierStatus?: CarrierStatusValue | ''
  retentionPeriod?: RetentionPeriodValue
  securityLevelMax?: number
  openStatus?: OpenStatusValue | ''
  loanStatus?: string
  conditionStatus?: string
  hasElectronicFile?: boolean
  fileExt?: string
  fileRole?: string
  fileCheckStatus?: string
  sortBy?: string
}

/** 内部档案文件 */
export interface InternalFile {
  id: number
  originalFilename: string
  fileFormat: string
  fileSize: number
  fileRole: string
  fileCheckStatus: string
  canPreview: boolean
  canDownload: boolean
}

/** 内部档案摘要（检索结果行，不含架位） */
export interface InternalArchive {
  id: number
  archiveNo: string
  title: string
  categoryId: number
  categoryName: string
  responsibleText: string
  formedDate: string
  securityLevel: number
  openStatus: OpenStatusValue
  carrierStatus: CarrierStatusValue
  sourceType: SourceTypeValue
  tags: string[]
  hasElectronicFile: boolean
  canPreview: boolean
  canDownload: boolean
  canBorrow: boolean
  borrowHint: string
  archivedAt: string
}

/** 内部档案详情 */
export interface InternalArchiveDetail extends InternalArchive {
  summary?: string
  retentionPeriod: RetentionPeriodValue
  files: InternalFile[]
}

/** AI 检索条件生成请求 */
export interface InternalAiQueryRequest {
  text: string
}

/** AI 检索条件生成结果 */
export interface InternalAiQueryResult {
  ruleType: 'internalSearchQuery'
  conditions: Partial<InternalSearchParams>
  rawJson: Record<string, unknown>
}

/** 借阅申请提交请求 */
export interface BorrowRequestCreateData {
  archiveId: number
  reason: string
  expectedDays: number
  expectedVisitAt: string
  contactPhone: string
}

/** 借阅申请摘要 */
export interface BorrowRequest {
  id: number
  requestNo: string
  archiveId: number
  archiveNo: string
  archiveTitle: string
  status: BorrowStatusValue
  expectedDays: number
  expectedVisitAt?: string
  appliedAt: string
  approvedAt?: string
  checkedOutAt?: string
  dueAt?: string
  returnedAt?: string
  overdue?: boolean
}

/** 借阅申请详情 */
export interface BorrowRequestDetail extends BorrowRequest {
  reason: string
  contactPhone: string
  opinion?: string
  rejectReason?: string
  voucherNo?: string
  voucherIssuedAt?: string
  returnCheckResult?: string
  returnNote?: string
}

/** 我的借阅申请查询参数 */
export interface BorrowRequestParams extends PageParams {
  status?: BorrowStatusValue | ''
  keyword?: string
}

/** 工作台指标 */
export interface InternalDashboardStats {
  recentViewCount: number
  pendingApprovalCount: number
  approvedPendingPickupCount: number
  downloadCount: number
}

/** 最近查阅记录 */
export interface RecentViewRecord {
  id: number
  archiveId: number
  archiveNo: string
  title: string
  categoryName: string
  securityLevel: number
  viewedAt: string
  accessStatus: 'available' | 'permission_changed'
}

/** 工作台下载记录 */
export interface DownloadRecord {
  id: number
  archiveId: number
  archiveNo: string
  title: string
  downloadedAt: string
}

/** 当前用户权限范围 */
export interface InternalPermission {
  role: string
  organizationName: string
  maxSecurityLevel: number
  dataScope: string
}

/** 工作台聚合数据 */
export interface InternalDashboardData {
  stats: InternalDashboardStats
  recentViews: RecentViewRecord[]
  borrowRequests: BorrowRequest[]
  currentLoans: BorrowRequest[]
  downloads: DownloadRecord[]
  permission: InternalPermission
}

export type InternalArchivePage = PageData<InternalArchive>
export type BorrowRequestPage = PageData<BorrowRequest>
```

## 8. 页面设计

### 8.1 工作台概览 `/internal/overview`

目标：内部查阅者查看个人利用指标、最近查阅、借阅申请、下载记录和权限范围。

布局（还原 `overview.html`）：

- 顶部 4 个 `metric` 卡：最近查阅、待审批申请、已批准待取件、下载记录。
- 主体 `dashboard-layout` 两列（左 `1fr` + 右 `360px`）：
  - 左列：
    - 「我的最近查阅」record-card 列表（档号、分类、密级、时间、「继续查看」按钮）。
    - 「我的借阅申请」摘要表（申请号、档案题名、申请时间、状态、操作）。
  - 右列：
    - 「我的权限范围」（角色、所属单位、密级上限、可见范围）。
    - 「我的下载记录」timeline。
    - 「利用边界」说明卡。

行为：

- 首次加载调用 `getInternalDashboard()`。
- 「继续查看」跳转 `/internal/archives/{archiveId}`（重新鉴权由后端 11.3 完成，前端直接跳转；`accessStatus='permission_changed'` 时按钮文案变「仅保留记录」，跳转后由详情接口返回错误态承接）。
- 「进入检索」跳转 `/internal/search`。
- 借阅申请摘要「查看申请」跳转 `/internal/borrow-requests?focus={id}`。
- 「导出凭证」仅 `approved`/`voucher_issued` 状态可用，调用 `exportBorrowVoucher(id)` 下载 PDF。
- 覆盖加载中、空数据、接口错误三态。

### 8.2 档案检索利用 `/internal/search`

目标：内部查阅者通过 AI 自然语言或结构化条件检索权限范围内档案，查看详情、预览/下载电子文件、发起纸质借阅申请。

布局（还原 `search.html` 的 `search-layout` 双栏）：

- 左栏（`1fr`）：
  - AI 检索条件生成（`<details>` 折叠）：textarea、生成 JSON、清空、JSON 预览、填充表单、复制 JSON。
  - 结构化检索条件（3 个 `filter-group`，每个内嵌 `filter-grid`）：
    - 核心元数据：关键词、档号、题名、责任者、所属全宗、档案门类、形成/移交单位、标签。
    - 时间与业务属性：形成年度起止、形成日期起止、档案来源、载体状态、保管期限、密级范围、开放状态、借阅状态、实体状态、入库时间。
    - 电子文件与排序：电子文件、文件格式、文件角色、文件检查、排序。
  - 检索结果表（档号、题名、分类、密级、载体、利用状态、操作）。
- 右栏（`400px` sticky）：`<ArchiveDetailPanel :archive-id="selectedArchiveId" />` + 安全边界说明卡。

行为：

- `generateInternalAiQuery()` 只生成查询条件 JSON，**不自动检索**（M04 约束）。
- 「填充表单」将 JSON 条件写入结构化表单字段。
- 「确认检索」调用 `searchInternalArchives()`，结果按权限过滤。
- 点击结果行「详情」设置 `selectedArchiveId`，右侧面板加载 `getInternalArchiveDetail()`。
- 右侧面板内完成预览、下载、借阅申请（见 8.4）。
- 「重置」清空全部结构化条件与结果。
- 覆盖 AI 生成中、检索中、检索失败、无结果、详情加载失败等状态。

### 8.3 档案详情独立页 `/internal/archives/:id`

目标：为工作台「继续查看」和深链提供独立入口，复用详情面板。

布局：

- 满屏容器包裹 `<ArchiveDetailPanel :archive-id="Number(route.params.id)" />`，顶部带返回检索入口。
- 鉴权失败（后端返回权限不足）时，面板内展示「当前权限不再覆盖该档案」提示，并提供返回工作台链接。

### 8.4 详情面板组件 `ArchiveDetailPanel.vue`

目标：展示档案元数据、电子文件、预览/下载、借阅申请表单，在检索页右侧与独立详情页复用。

布局（还原 `search.html` 的 `detail-panel` + `drawer` + `borrow-form`）：

- 元数据 `detail-grid`：档号、分类、责任者、形成日期、密级、载体状态、纸质借阅（可申请/不支持）。**不展示架位、盒号。**
- 安全提示 `notice`：内部查阅者不展示库房架位，纸质原件走借阅申请。
- 电子文件列表：文件名、格式、大小、角色、检查状态，每行带预览/下载按钮。
- 操作按钮：在线预览、下载、申请借阅。
- 借阅申请表单（点击「申请借阅」展开）：借阅理由（textarea）、借阅天数（number）、到馆时间（datetime-local）、联系电话、提交/取消。

行为：

- props：`archiveId: number`。`watch(archiveId)` 变化时重新加载。
- 加载调用 `getInternalArchiveDetail(archiveId)`；加载中显示骨架，失败显示错误重试，无数据提示空态。
- 「在线预览」对 `canPreview` 的文件调用 `previewInternalFile(fileId)`，展示预览内容（mock 文本或真实 URL）；权限不足或不可预览时禁用并提示。
- 「下载」对 `canDownload` 的文件调用 `downloadInternalFile(fileId)`，生成 Blob 下载；不可下载禁用。
- 「申请借阅」：
  - `canBorrow=false`（纯电子或不可借状态）时禁用并提示「纯电子档案不支持纸质借阅」或具体 `borrowHint`。
  - 展开表单后用 `borrowValidation` 校验理由、天数（≥1）、到馆时间、手机号。
  - 提交调用 `createBorrowRequest(data)`，成功后提示申请号与「待审批」状态，收起表单。
- 借阅表单的联系电话优先回填当前用户手机号。

### 8.5 我的借阅申请 `/internal/borrow-requests`

目标：查阅者管理本人借阅申请，查看详情和导出凭证。

布局：

- 顶部筛选：状态（全部/待审批/已批准/凭证已生成/已借出/已归还/异常归还/已拒绝）、关键词。
- 列表表：申请号、档案题名、申请时间、状态（带逾期标记）、操作（查看详情/导出凭证）。
- 详情用 `el-drawer`：展示申请信息、审批意见、凭证状态、出库归还节点。
- 操作按钮区：`approved`/`voucher_issued` 显示「导出凭证」。

行为：

- 首次加载调用 `getMyBorrowRequests(params)`；筛选变更重新查询。
- 「查看详情」打开抽屉，调用 `getBorrowRequestDetail(id)`。
- 「导出凭证」调用 `exportBorrowVoucher(id)` 下载 PDF；非可导出状态禁用并 tooltip 说明。
- `overdue=true` 的记录在状态列追加「已逾期」标记。
- 覆盖加载、空、错误三态。
- 路由 query `focus={id}` 时自动打开对应详情抽屉（承接工作台跳转）。

## 9. 错误处理与边界

- 每个页面/组件必须处理加载中、空数据、接口失败、表单校验失败，不得假定数据永远正常返回（`frontend/CLAUDE.md` AI 约束）。
- 组件捕获 API 异常后展示页面级 `notice` 或 Element Plus 消息，避免静默失败。
- AI 只输出查询条件 JSON，**不查库、不读正文、不绕权限**（M04）。AI 失败时回退为手工填写条件。
- 内部检索结果与详情**不展示库房架位、盒号**（M08）。
- 纯电子档案不可借阅：前端按 `canBorrow` 禁用，并展示 `borrowHint`；最终校验由后端 11.7 完成。
- 导出凭证仅 `approved`/`voucher_issued` 可用，凭证 ≠ 出库记录（原型与 11.10 约束）。
- 工作台「继续查看」前端直接跳详情，重新鉴权由后端 11.3 完成；`permission_changed` 记录在详情接口报错时由面板承接为权限提示。
- 预览/下载复用同一套后端鉴权（11.5/11.6），前端只按 `canPreview`/`canDownload` 控制可用性。
- 借阅表单联系电话、到馆时间、理由为必填，天数 ≥ 1。
- 本轮不修改胡颖的后台页面；共享语义只通过 `enums.ts` 保持一致。

## 10. Mock 数据

`src/mock/modules/internal.ts`，结构对齐接口 `data` 字段内容（不包裹 `R<T>`），分页接口模拟完整 `PageData<T>`。

### 10.1 工作台 mock

`mockInternalDashboard` 覆盖：

- 4 项指标数值。
- 最近查阅：含可重新鉴权记录与 `permission_changed` 记录。
- 借阅申请摘要：覆盖待审批、已批准、已归还、已拒绝。
- 当前借阅：含一条逾期（`overdue=true`）。
- 下载记录 timeline。
- 权限范围：内部查阅者、技术部、密级上限秘密、本单位授权全宗。

### 10.2 检索 mock

`mockSearchInternalArchives(params)` 覆盖：

- 纸质+电子、可预览/可借阅档案。
- 纯电子、可预览/可下载、不可借阅档案。
- 纯纸质、可借阅、无电子文件档案。
- 支持关键词、档号、分类、载体、密级、年度等前端过滤。

`mockInternalArchiveDetail(id)` 覆盖：元数据、多文件（原文/扫描件）、可借阅判断。

`mockGenerateInternalAiQuery(data)` 返回条件 JSON（含 keyword、archiveNo、formedYear 范围、category、tags、securityLevelMax 等）。

### 10.3 借阅 mock

`mockBorrowRequests` 覆盖 7 种状态各至少一条，含一条逾期。

`mockBorrowRequestDetail(id)` 覆盖审批意见、凭证号、出库归还节点。

`mockCreateBorrowRequest(data)` 返回新生成的 `applied` 申请（含 `requestNo`、`appliedAt`）。

`mockExportBorrowVoucher(id)` 返回 PDF Blob。

## 11. 测试与验证

### 11.1 单元测试

- `api/internal.spec.ts`：mock 分支返回结构与类型正确性。
- `utils/borrowValidation.spec.ts`：理由/天数/到馆时间/手机号校验、纯电子禁用。
- 路由 spec：`/internal/archives/:id`、`/internal/borrow-requests` 可解析。

### 11.2 组件交互测试

- `/internal/overview`：指标渲染、「继续查看」跳转、「导出凭证」状态门控。
- `/internal/search`：生成 JSON → 填充表单 → 检索 → 选中结果加载详情。
- `ArchiveDetailPanel`：加载/空/错误三态、预览/下载按钮可用性、借阅表单展开/校验/提交。
- `/internal/borrow-requests`：筛选、详情抽屉、导出凭证门控、逾期标记。

### 11.3 构建验证

```bash
npm run type-check
npm run build
npm run test:unit
```

TypeScript 检查、生产构建、单元测试均需通过。

## 12. 验收口径

- `/internal/overview`、`/internal/search` 无「待实现」占位；`/internal/archives/:id`、`/internal/borrow-requests` 可访问。
- 页面布局、视觉层级、文案、交互结构和状态反馈还原对应 HTML 原型；字段补充不改变原型组织方式。
- AI 检索只生成条件、不自动查库；检索结果与详情不展示架位。
- 详情面板在检索页右侧与独立详情页两处复用，行为一致。
- 纯电子档案不可借阅（前端禁用 + 提示），纸质+电子/纯纸质可发起借阅申请。
- 借阅申请列表覆盖 7 种状态，导出凭证按状态门控，逾期标记正确。
- 工作台「继续查看」跳转独立详情页，权限变化由详情态承接。
- 所有页面覆盖加载中、空数据、接口错误三态。
- API 函数全部支持 mock/真实切换；mock 数据对齐接口 `data` 字段内容。
- TypeScript 检查、生产构建、单元测试通过。
