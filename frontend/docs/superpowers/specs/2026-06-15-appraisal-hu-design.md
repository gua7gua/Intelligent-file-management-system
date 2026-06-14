# feat/appraisal-hu 设计文档：档案鉴定 · 档案销毁 · 审批工作台

**日期**：2026-06-15
**负责人**：胡颖
**分支**：`feat/appraisal-hu`
**阶段**：06-15 至 06-17
**优先级**：P1
**共同验收点**：库房利用与审批处置页面可联动 —— 鉴定、销毁、审批工作台处置链主流程可操作

## 1. 背景

本轮从最新 `develop`（`b495a7e`）开始，`develop` 已包含以下前置工作：

- 郭一坤 `feat/base-guo`（PR#9）：登录、角色命名、API request 层、mock 切换、字典 API、四类 Layout。
- 郭一坤 `feat/portal-guo`（PR#13）：移交门户、公众门户页面。
- 郭一坤 `feat/search-guo`（已合并）：内部查阅者检索、预览、下载、借阅申请，以及 `types/internal.ts`、`api/internal.ts`、`mock/modules/internal.ts`。
- 胡颖 `feat/acceptance-hu`（PR#10）：管理后台验收、电子文件上传、回退回执、征集管理。
- 胡颖 `feat/archive-hu`（PR#15）：待入库、AI 建议确认、档案管理、上架页面，以及 `types/archive.ts`、`api/archive.ts`、`mock/modules/archive.ts`。**档案管理页已接入密级/开放调整发起入口**（`submitSecurityAdjust`/`submitOpenAdjust`，§10.4/10.5），提交后生成审批单。
- 周扬 `feat/transfer-zhou`（PR#8）、`feat/archive-zhou`（PR#16）：清单验收、入库与档案管理后端接口。
- 刘星 `feat/base-liu`（PR#3）、`feat/file-liu`（PR#12）、`feat/search-liu`（PR#18）：OpenAPI、AI 客户端、统一异常、电子文件暂存、检索/借阅后端。

本轮聚焦胡颖 06-15 至 06-17 前端任务。后端鉴定/销毁/审批接口（周扬 `feat/appraisal-zhou`，计划 06-16 至 06-18）尚未实现，本轮全部基于 `doc/接口文档.md` 第 13、14、15 章用 mock 开发；后端就绪后将 `VITE_USE_MOCK` 切为 `false` 即可对接，组件代码无需改动。

> **教训（来自 archive-hu）**：上一轮 archive-hu 曾有类型错误阻塞 develop 构建，由郭一坤代为修复（commit `ed016d6`）。本轮每个阶段主动执行 `npm run type-check && npm run build`，杜绝再次阻塞集成。

用户已确认两点关键决策：

1. **文件组织**：鉴定、销毁、审批三个独立子域文件（types+api+mock 各自拆分），符合项目 per-subdomain 约定（`reception`/`transfer`/`archive`/`internal` 各自独立）。
2. **跨页联动**：实现关键跨页跳转（鉴定完成→销毁清册；销毁提交审批→审批工作台），同时三页各自能 mock 全状态独立闭环。

## 2. 目标与范围

### 2.1 本轮实现

| 页面/组件 | 路由 | 说明 | 原型 | 角色 |
|-----------|------|------|------|------|
| 档案鉴定 | `/admin/appraisal` | 重写占位页：批次列表 + 批次明细逐件鉴定 + 创建/完成批次 | `doc/prototype/admin/appraisal.html` | back_archivist |
| 档案销毁 | `/admin/destruction` | 重写占位页：清册列表 + 快照详情 + 提交审批 + 确认销毁 | `doc/prototype/admin/destruction.html` | back_archivist |
| 审批工作台 | `/admin/approval` | 重写占位页：审批队列 + 多类型审批详情 + 通过/退回 | `doc/prototype/admin/approval.html` director | director |
| 销毁确认弹窗 | `views/admin/destruction/components/DestroyConfirmDialog.vue` | 销毁方式/双监销人/现场照片/不可逆确认 | 销毁原型销毁确认弹窗 | back_archivist |
| 密级/开放审批详情 | `views/admin/approval/components/ArchiveAdjustDetail.vue` | 目标档案 + 凭证档案双栏 + 前后值 | 审批原型双栏区 | director |
| 销毁审批详情 | `views/admin/approval/components/DestructionApprovalDetail.vue` | 清册快照表 + 鉴定来源 | 审批原型清册区 | director |

路由 `routes/admin.ts` 与菜单 `adminMenuConfig` 已配置三页（`appraisal`/`destruction` 在「鉴定销毁」组，`approval` 在「审批工作台」单项），本轮**不改路由和菜单**。

### 2.2 不在本轮

- 借阅审批 `/admin/borrow-approval`（属郭一坤 `feat/storage-guo`，06-15 至 06-17）。
- 库房、盘点、统计、研判、编研、保存、用户管理、系统配置页面。
- 密级/开放调整**发起**入口（archive-hu 档案管理页已实现，本轮审批工作台仅作消费方）。
- 后端鉴定/销毁/审批接口实现（周扬 `feat/appraisal-zhou`）。
- 鉴定/销毁/审批的独立详情子路由（`/admin/appraisal/:batchId` 等）：模块说明提到，但 frontend/CLAUDE.md 明确「以实际代码为准」，实际路由采用单页左右布局，不新增子路由。

## 3. 方案选择

### 3.1 文件组织（已确认）

| 方案 | 内容 | 优点 | 缺点 |
|------|------|------|------|
| 拆分三个子域文件（选定） | `types`+`api`+`mock` 各分 `appraisal`/`destruction`/`approval` 三个文件 | 边界清晰，符合 per-subdomain 约定，三页 API 端点组（§13/14/15）天然分离 | 共享类型（如审批单摘要）需跨文件 import |
| 合并为单一 disposal 文件 | 三页共用一个 `appraisal.ts` | 文件少 | 语义混杂，与项目惯例不一致 |

**选定**：拆分三个子域文件。`ApprovalRequest` 实体定义在 `types/approval.ts`；销毁清册详情中引用的审批单摘要从 `types/approval.ts` import `ApprovalRequest` 复用，不重复定义。

### 3.2 审批工作台多类型详情（已确认）

审批详情按 `approvalType` 有两种截然不同的视觉结构：

- 密级/开放调整（`security_adjust`/`open_adjust`）：目标档案 + 凭证档案**双栏并列**。
- 销毁审批（`destruction`）：清册快照表 + 鉴定来源。

| 方案 | 内容 | 优点 | 缺点 |
|------|------|------|------|
| 抽两个子组件按类型切换（选定） | `ArchiveAdjustDetail.vue` + `DestructionApprovalDetail.vue`，主页面 `v-if` 切换 | 每个组件职责单一，主页面不膨胀 | 多两个文件 |
| 单组件内联 v-if 分支 | 审批详情在一个文件内分支渲染 | 文件少 | 单文件过长，两种结构耦合 |

**选定**：抽两个子组件。

### 3.3 原型还原原则（沿用 `feat/portal-guo` / `feat/search-guo`）

- 页面布局、视觉层级、网格比例、卡片组织、列表/详情关系、状态标签、主要文案优先保持原型原貌。
- 实现 Vue 页面时优先搬入原型 `<body>` 内容区结构和页面内样式，原型 JavaScript 转为 `<script setup>` 响应式状态和事件。
- 原型 CSS 类名、设计令牌、公共组件类（`.card`、`.toolbar`、`.button`、`.grid`、`.status`、`.metric`、`.table-wrap`、`.notice`、`.tabs`、`.filter-group` 等）优先保留；Element Plus 只在表单校验、消息提示、日期选择、对话框/抽屉、上传等能补足交互质量处使用。
- 接口文档和业务文档用于补齐原型未细化的字段、API、mock、加载/空/错误状态和权限边界，不推翻原型布局。
- 展示层保留原型业务语言，数据字段和请求参数按接口文档命名。

## 4. 架构

沿用当前前端架构：

- 页面组件只写内容区，布局由 `AdminLayout` 承担。
- API 函数统一放 `src/api/`，组件只调 API 函数，不直接 import mock。
- mock 数据放 `src/mock/modules/`，通过 `VITE_USE_MOCK` 控制 mock/真实切换。
- 业务类型放 `src/types/`，枚举值复用并扩展 `types/enums.ts`。
- 页面内用 Vue `ref`/`reactive` 管理局部状态，不引入新全局 store。
- 原型公共样式已全局引入，页面复用公共类。
- `request.ts` 响应拦截器已对 `R<T>` 解包，API 函数返回类型为 `data` 字段内容；mock 只模拟 `data` 内容，分页接口模拟完整 `PageData<T>`。

## 5. 文件结构

```
frontend/src/types/
  enums.ts                  # 扩展：鉴定/销毁/审批相关枚举与中文映射
  appraisal.ts              # 鉴定批次/明细/创建/查询类型
  destruction.ts            # 销毁清册/明细快照/确认/查询类型（import approval 复用审批单）
  approval.ts               # 审批单/详情/查询类型

frontend/src/api/
  appraisal.ts              # 第 14 章 5 个 /api/admin/appraisal-batches 接口
  destruction.ts            # 第 15 章 5 个 /api/admin/destruction-lists 接口
  approval.ts               # 第 13 章 4 个 /api/admin/approvals 接口

frontend/src/mock/modules/
  appraisal.ts              # 鉴定 mock 数据
  destruction.ts            # 销毁 mock 数据
  approval.ts               # 审批 mock 数据

frontend/src/utils/
  appraisalValidation.ts    # 鉴定明细校验
  destructionValidation.ts  # 销毁确认校验

frontend/src/views/admin/
  appraisal/index.vue                              # 档案鉴定（重写）
  destruction/index.vue                            # 档案销毁（重写）
  destruction/components/DestroyConfirmDialog.vue  # 销毁确认弹窗
  approval/index.vue                               # 审批工作台（重写）
  approval/components/ArchiveAdjustDetail.vue      # 密级/开放审批详情
  approval/components/DestructionApprovalDetail.vue# 销毁审批详情
```

## 6. 枚举扩展（`types/enums.ts`）

新增常量与中文映射，复用既有 `ApprovalStatus`、`ArchiveLifecycleStatus`、`RetentionPeriod`、`SecurityLevel`、`OpenStatus`、`CarrierStatus`。

```typescript
/** 鉴定结论 */
export const AppraisalResult = {
  EXTEND: 'extend',
  DESTROY: 'destroy',
} as const

/** 鉴定批次状态 */
export const AppraisalBatchStatus = {
  DRAFT: 'draft',
  COMPLETED: 'completed',
} as const

/** 销毁清册状态 */
export const DestructionListStatus = {
  DRAFT: 'draft',
  PENDING_APPROVAL: 'pending_approval',
  PENDING_DESTROY: 'pending_destroy',
  DESTROYED: 'destroyed',
} as const

/** 销毁方式 */
export const DestroyMethod = {
  SHREDDING: 'shredding',
  BURNING: 'burning',
  ENTRUSTED: 'entrusted',
} as const

/** 电子文件删除状态 */
export const FileDeleteStatus = {
  NOT_STARTED: 'not_started',
  DELETED: 'deleted',
  FAILED: 'failed',
} as const

/** 审批类型 */
export const ApprovalType = {
  SECURITY_ADJUST: 'security_adjust',
  OPEN_ADJUST: 'open_adjust',
  DESTRUCTION: 'destruction',
} as const

/** 审批目标类型 */
export const ApprovalTargetType = {
  ARCHIVE: 'archive',
  DESTRUCTION_LIST: 'destruction_list',
} as const

// 中文映射
export const AppraisalResultLabel: Record<string, string> = {
  extend: '延长保存',
  destroy: '待销毁',
}
export const AppraisalBatchStatusLabel: Record<string, string> = {
  draft: '草稿',
  completed: '已完成',
}
export const DestructionListStatusLabel: Record<string, string> = {
  draft: '待提交',
  pending_approval: '待审批',
  pending_destroy: '待销毁',
  destroyed: '已销毁',
}
export const DestroyMethodLabel: Record<string, string> = {
  shredding: '粉碎',
  burning: '焚毁',
  entrusted: '委托销毁',
}
export const FileDeleteStatusLabel: Record<string, string> = {
  not_started: '未开始',
  deleted: '已删除',
  failed: '删除失败',
}
export const ApprovalTypeLabel: Record<string, string> = {
  security_adjust: '密级调整',
  open_adjust: '开放调整',
  destruction: '销毁审批',
}
```

派生值类型沿用既有 `enums.ts` 模式（`export type XxxValue = (typeof Xxx)[keyof typeof Xxx]`）：`AppraisalResultValue`、`AppraisalBatchStatusValue`、`DestructionListStatusValue`、`DestroyMethodValue`、`FileDeleteStatusValue`、`ApprovalTypeValue`、`ApprovalTargetTypeValue`，供 §7 类型引用。

## 7. 类型设计

字段名严格对齐 `doc/接口文档.md` 与 `doc/数据库设计.md` 第 8.1–8.5 节。

### 7.1 `types/appraisal.ts`

```typescript
import type { PageData, PageParams } from './api'
import type {
  AppraisalBatchStatusValue,
  AppraisalResultValue,
  ArchiveLifecycleStatusValue,
  RetentionPeriodValue,
} from './enums'

/** 鉴定批次查询参数（§14.1） */
export interface AppraisalBatchParams extends PageParams {
  status?: AppraisalBatchStatusValue | ''
  categoryId?: number
  formedYearStart?: number
  formedYearEnd?: number
}

/** 创建鉴定批次（§14.2） */
export interface AppraisalBatchCreateData {
  batchName: string
  categoryId?: number
  formedYearStart?: number
  formedYearEnd?: number
}

/** 鉴定明细（命中档案 + 鉴定结论） */
export interface AppraisalItem {
  id?: number
  archiveId: number
  archiveNo: string
  title: string
  categoryName: string
  retentionPeriod: RetentionPeriodValue
  retentionUntil: string
  currentLifecycleStatus: ArchiveLifecycleStatusValue
  appraisalResult: AppraisalResultValue | ''
  newRetentionPeriod?: RetentionPeriodValue | ''
  newRetentionUntil?: string
  opinion?: string
  appraisedBy?: number
  appraisedAt?: string
}

/** 鉴定批次摘要（列表行） */
export interface AppraisalBatch {
  id: number
  batchNo: string
  batchName: string
  categoryId?: number
  categoryName?: string
  formedYearStart?: number
  formedYearEnd?: number
  status: AppraisalBatchStatusValue
  completedAt?: string
  hitCount: number
  destroyCount?: number
  extendCount?: number
  createdAt: string
}

/** 鉴定批次详情（§14.3） */
export interface AppraisalBatchDetail extends AppraisalBatch {
  items: AppraisalItem[]
  generatedListId?: number
  generatedListNo?: string
}

/** 保存鉴定明细请求（§14.4） */
export interface AppraisalItemsSaveData {
  items: Array<{
    archiveId: number
    appraisalResult: AppraisalResultValue
    newRetentionPeriod?: RetentionPeriodValue | null
    newRetentionUntil?: string | null
    opinion?: string
  }>
}

export type AppraisalBatchPage = PageData<AppraisalBatch>
```

### 7.2 `types/destruction.ts`

```typescript
import type { PageData, PageParams } from './api'
import type { ApprovalRequest } from './approval'
import type {
  DestructionListStatusValue,
  DestroyMethodValue,
  FileDeleteStatusValue,
  SecurityLevelValue,
} from './enums'

/** 销毁清册查询参数（§15.1） */
export interface DestructionListParams extends PageParams {
  status?: DestructionListStatusValue | ''
  keyword?: string
}

/** 销毁清册明细快照（数据库 8.5） */
export interface DestructionItem {
  id: number
  archiveId: number
  archiveNoSnapshot: string
  titleSnapshot: string
  categorySnapshot: string
  pageCountSnapshot?: number
  retentionSnapshot: string
  securityLevelSnapshot: SecurityLevelValue
  appraisalOpinionSnapshot: string
  fileDeleteStatus: FileDeleteStatusValue
  fileDeletedAt?: string
}

/** 销毁清册附件（现场照片，business_attachments 摘要） */
export interface DestructionPhoto {
  id: number
  fileName: string
  fileSize: number
  uploadedAt: string
}

/** 销毁清册摘要（列表行） */
export interface DestructionList {
  id: number
  listNo: string
  listName: string
  appraisalBatchId?: number
  appraisalBatchNo?: string
  status: DestructionListStatusValue
  approvalRequestId?: number
  destroyedAt?: string
  destroyMethod?: DestroyMethodValue
  supervisorName1?: string
  supervisorName2?: string
  destroyNote?: string
  itemCount: number
  createdAt: string
}

/** 销毁清册详情（§15.2） */
export interface DestructionListDetail extends DestructionList {
  items: DestructionItem[]
  approval?: ApprovalRequest
  photos: DestructionPhoto[]
}

/** 提交销毁审批请求（§15.3） */
export interface DestructionSubmitApprovalData {
  reason: string
}

/** 确认销毁请求（§15.5） */
export interface DestructionConfirmData {
  destroyMethod: DestroyMethodValue
  supervisorName1: string
  supervisorName2: string
  destroyNote: string
  // 照片通过 §15.4 单独上传，确认时只传元信息；此处 photos 仅记录已上传附件 id
  photoIds?: number[]
}

export type DestructionListPage = PageData<DestructionList>
```

### 7.3 `types/approval.ts`

```typescript
import type { PageData, PageParams } from './api'
import type { DestructionItem } from './destruction'
import type {
  ApprovalStatusValue,
  ApprovalTargetTypeValue,
  ApprovalTypeValue,
  SecurityLevelValue,
} from './enums'

/** 审批单查询参数（§13.1） */
export interface ApprovalParams extends PageParams {
  approvalType?: ApprovalTypeValue | ''
  status?: ApprovalStatusValue | ''
  keyword?: string
}

/** 审批关联档案摘要（目标/凭证） */
export interface ApprovalArchiveSummary {
  id: number
  archiveNo: string
  title: string
  categoryName: string
  organizationName?: string
  fondsName?: string
  securityLevel: SecurityLevelValue
  lifecycleStatus: string
}

/** 销毁清册摘要（审批详情内嵌） */
export interface ApprovalDestructionListSummary {
  id: number
  listNo: string
  listName: string
  itemCount: number
  appraisalBatchNo?: string
  items: DestructionItem[]
}

/** 审批单摘要（列表行） */
export interface ApprovalRequest {
  id: number
  approvalType: ApprovalTypeValue
  targetType: ApprovalTargetTypeValue
  targetId: number
  evidenceArchiveId?: number
  oldValue?: string
  newValue?: string
  reason: string
  status: ApprovalStatusValue
  submittedBy: number
  submittedByName?: string
  submittedAt: string
  approvedBy?: number
  approvedByName?: string
  approvedAt?: string
  approvalOpinion?: string
  // 列表摘要字段（按类型填充）
  targetArchiveNo?: string
  targetArchiveTitle?: string
  targetListNo?: string
  targetListName?: string
}

/** 审批单详情（§13.2） */
export interface ApprovalRequestDetail extends ApprovalRequest {
  targetArchive?: ApprovalArchiveSummary
  evidenceArchive?: ApprovalArchiveSummary
  destructionList?: ApprovalDestructionListSummary
}

/** 审批意见请求（§13.3 / §13.4） */
export interface ApprovalOpinionData {
  opinion: string
}

export type ApprovalRequestPage = PageData<ApprovalRequest>
```

## 8. API 设计

所有函数内部通过 `USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'` 控制 mock/真实请求。

### 8.1 `api/appraisal.ts`（§14）

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getAppraisalBatches(params)` | GET | `/admin/appraisal-batches` | 鉴定批次分页（status/categoryId/formedYear） |
| `createAppraisalBatch(data)` | POST | `/admin/appraisal-batches` | 创建批次，返回批次及命中明细 |
| `getAppraisalBatchDetail(batchId)` | GET | `/admin/appraisal-batches/{batchId}` | 批次详情 + 命中档案 + 明细 |
| `saveAppraisalItems(batchId, data)` | PUT | `/admin/appraisal-batches/{batchId}/items` | 保存逐件鉴定结论（批次须 draft） |
| `completeAppraisalBatch(batchId)` | POST | `/admin/appraisal-batches/{batchId}/complete` | 完成鉴定，返回批次 + 生成的销毁清册 |

### 8.2 `api/destruction.ts`（§15）

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getDestructionLists(params)` | GET | `/admin/destruction-lists` | 销毁清册分页（status/keyword） |
| `getDestructionListDetail(listId)` | GET | `/admin/destruction-lists/{listId}` | 清册 + 明细快照 + 审批单 + 照片 |
| `submitDestructionApproval(listId, data)` | POST | `/admin/destruction-lists/{listId}/submit-approval` | 提交审批，返回审批单，清册→pending_approval |
| `uploadDestructionPhotos(listId, files)` | POST | `/admin/destruction-lists/{listId}/photos` | multipart/form-data 上传现场照片，返回附件列表 |
| `confirmDestruction(listId, data)` | POST | `/admin/destruction-lists/{listId}/destroy` | 确认销毁（方式/双监销人/说明），清册→destroyed |

`uploadDestructionPhotos` 真实请求用 `FormData`（字段名 `files`）；mock 下校验文件类型/大小后返回模拟附件列表。

### 8.3 `api/approval.ts`（§13）

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getApprovals(params)` | GET | `/admin/approvals` | 审批单分页（approvalType/status/keyword） |
| `getApprovalDetail(approvalId)` | GET | `/admin/approvals/{approvalId}` | 审批单 + 目标/凭证档案或清册快照 |
| `approveApproval(approvalId, data)` | POST | `/admin/approvals/{approvalId}/approve` | 审批通过（opinion 必填） |
| `rejectApproval(approvalId, data)` | POST | `/admin/approvals/{approvalId}/reject` | 审批退回（opinion 必填） |

## 9. 页面设计

> **顶部指标卡取数**：接口文档（§13/14/15）未定义鉴定/销毁/审批的看板聚合接口，三页顶部 `metric` 卡为页面快照数值（与原型静态示意一致）。mock 下用常量定义（如「待鉴定批次」可由本页批次列表 draft 数量派生，「已生成清册」由销毁 mock 清册总数派生，跨域计数如「即将到期档案」用快照常量），后端若后续补看板接口再改为独立请求；当前不新增 API 函数。

### 9.1 档案鉴定 `/admin/appraisal`

**目标**：处理到期档案，创建鉴定批次，逐件「延长保存/待销毁」，完成后系统生成销毁清册。

**布局**（还原 `appraisal.html` 左右双栏）：

- 顶部 4 个 `metric` 卡：即将到期档案、待鉴定批次（draft）、待销毁条目、已生成清册。
- 「创建鉴定批次」按钮 → 弹窗：批次名称、分类（字典）、形成年度起止；创建后刷新列表并选中。
- 主体 `dashboard-layout` 双栏（左批次列表 `1fr` + 右明细 `2fr`）：
  - 左列：批次状态标签（待处理/已完成）过滤 + 批次卡片列表（批次号、名称、分类、年度、命中数、状态）。
  - 右列：选中批次的明细表 —— 档号、题名、分类、原保管期限、到期日、当前状态、鉴定结果（单选延长保存/待销毁）、新期限、新到期日、鉴定意见。

**行为**：

- 首次加载 `getAppraisalBatches()`；顶部指标按 §9 开头说明取数。
- 选中批次 → `getAppraisalBatchDetail(id)` 加载明细。
- 明细行「延长保存」选中后展开新保管期限（下拉）+ 新到期日（日期）；「待销毁」互斥，仅展示状态标签。
- 「保存鉴定明细」`saveAppraisalItems(id, data)`：用 `appraisalValidation` 校验延长保存项必填新期限+新到期日；批次须 draft。
- 「完成鉴定」`completeAppraisalBatch(id)`：校验所有明细有结论；成功后提示生成的清册号 + 「查看销毁清册」按钮跳 `/admin/destruction?focus={generatedListId}`。
- 三态：批次加载中、无批次、接口错误；明细加载中、明细为空、加载失败。
- 延长保存未填新期限时禁用「完成鉴定」并提示。

### 9.2 档案销毁 `/admin/destruction`

**目标**：查看鉴定生成的销毁清册，检查快照，提交审批，审批通过后确认销毁，保留三类证据链。

**布局**（还原 `destruction.html` 左右双栏）：

- 顶部 4 个 `metric` 卡：待提交、待审批、待销毁、已销毁。
- 主体双栏（左清册列表 + 右详情）：
  - 左列：清册状态标签（待提交/待审批/待销毁/已销毁）过滤 + 清册卡片列表（清册号、名称、来源批次、数量、状态）。
  - 右列：清册基本信息 + 明细快照表（档号快照、题名快照、分类、保管期限、密级、鉴定意见、文件删除状态）+ 证据链区（销毁清册/审批记录/销毁确认三类证据节点）+ 操作区。
- `DestroyConfirmDialog` 弹窗：销毁方式、监销人 1、监销人 2、销毁说明、现场照片上传（≥1 张）、不可逆确认勾选。

**行为**：

- 首次加载 `getDestructionLists()`；状态标签过滤。
- 选中清册 → `getDestructionListDetail(id)` 加载快照+审批+照片。
- 路由 query `focus={listId}` 时自动选中并展开对应清册（承接鉴定页跳转）。
- `draft` →「提交审批」`submitDestructionApproval(id, {reason})`（reason 必填）→ 成功提示审批单号 +「前往审批工作台」跳 `/admin/approval?focus={approvalId}`，清册→pending_approval。
- `pending_destroy` →「确认销毁」打开弹窗：
  - `destructionValidation` 校验方式/双监销人（不同）/说明/不可逆勾选。
  - 照片先 `uploadDestructionPhotos(id, files)` 上传（复用 `utils/fileParser` 校验格式/大小），再 `confirmDestruction(id, data)`。
  - 成功后清册→destroyed，提示「档案状态已更新为已销毁，证据链永久保留」。
- `pending_approval` → 展示审批中状态 + 退回意见（如有），可「修改说明重新提交」。
- 三态 + 不可逆二次确认；照片格式（jpg/png）/大小校验。

### 9.3 审批工作台 `/admin/approval`

**目标**：馆领导集中处理密级/开放/销毁审批，查看证据，通过或退回。

**布局**（还原 `approval.html` 左右双栏）：

- 顶部 4 个 `metric` 卡：待审批、今日已处理、销毁审批、退回补正。
- 主体双栏（左审批队列 + 右详情）：
  - 左列：类型标签（全部/密级/开放/销毁）过滤 + 审批队列卡片（类型、目标摘要、提交人、提交时间、状态）。
  - 右列：审批单基础信息（类型、目标、申请理由、调整前后值、风险提示）+ 按 `approvalType` 切换的详情子组件 + 审批意见输入 + 通过/退回按钮。

**子组件**：

- `ArchiveAdjustDetail.vue`（密级/开放，`security_adjust`/`open_adjust`）：
  - 双栏展示目标档案 `targetArchive` 与凭证档案 `evidenceArchive`（档号、题名、分类、单位/全宗、密级、生命周期状态）。
  - 调整前后值（`oldValue`/`newValue`，密级用 `SecurityLevelLabel`，开放用 `OpenStatusLabel`）。
  - 凭证校验提示：凭证缺失/已销毁/来源关系不匹配时禁用「通过」，仅允许退回。
- `DestructionApprovalDetail.vue`（销毁，`destruction`）：
  - 清册快照表（档号/题名/分类/保管期限/密级/鉴定意见快照）+ 来源鉴定批次号。
  - 馆领导审批即批准凭证提示。

**行为**：

- 首次加载 `getApprovals()`；类型标签过滤。
- 选中审批单 → `getApprovalDetail(id)` 加载详情。
- 路由 query `focus={approvalId}` 时自动选中（承接销毁页跳转）。
- 「审批通过」`approveApproval(id, {opinion})`：opinion 必填；通过后提示业务结果（密级/开放字段生效 / 销毁清册进入待销毁）。
- 「退回补正」`rejectApproval(id, {opinion})`：opinion 必填；退回后提示目标不变。
- 三态；通过前按凭证/快照完整性门控。

## 10. 跨页联动

- 鉴定完成 → `/admin/destruction?focus={generatedListId}`：销毁页自动选中该清册。
- 销毁提交审批 → `/admin/approval?focus={approvalId}`：审批工作台自动选中该审批单。
- 联动通过 `vue-router` 的 `route.query.focus` 实现，目标页 `watch` query 变化后定位选中项；无 focus 时正常首次加载。
- 三页各自 mock 数据自闭环（不依赖跳转才能工作），跳转仅提升处置链连续性。

## 11. 错误处理与边界

- 每页/组件必须处理加载中、空数据、接口失败、表单校验失败（`frontend/CLAUDE.md` AI 约束）。
- 捕获 API 异常后展示页面级 `notice` 或 Element Plus 消息，避免静默失败。
- 鉴定：延长保存必填新期限+新到期日；未处理全部条目不能完成；批次须 draft 才能保存/完成。
- 销毁：确认销毁需双监销人（不同人）、销毁方式、不可逆勾选、≥1 张现场照片；只审批通过（pending_destroy）的清册可确认。
- 审批：通过/退回意见必填；凭证档案缺失/已销毁/来源不匹配或销毁清册快照不全时禁用通过。
- 确认销毁前二次提醒不可逆；馆领导审批即本馆批准凭证，不引入第三方节点。
- mock 下文件上传用 `utils/fileParser` 做前端格式/大小校验，模拟附件返回。
- 枚举值与状态标签复用 `enums.ts`，三页状态展示一致。
- 本轮不改胡颖已合并的档案管理页；共享语义（审批单）只通过 `types/approval.ts` 保持一致。

## 12. Mock 数据

`src/mock/modules/`，结构对齐接口 `data` 字段内容（不包 `R<T>`），分页模拟完整 `PageData<T>`。

### 12.1 `mock/modules/appraisal.ts`

- `mockAppraisalBatches(params)`：≥3 个批次（1 个 draft 含未处理明细、1 个 completed 已生成清册、1 个 draft 空命中），支持 status 过滤。
- `mockAppraisalBatchDetail(id)`：明细覆盖延长保存项、待销毁项、未处理项；completed 批次含 `generatedListId/No`。
- `mockCreateAppraisalBatch(data)`：返回新批次（`batchNo=APP-x`）+ 命中档案明细。
- `mockSaveAppraisalItems(id, data)`：返回更新后的批次详情。
- `mockCompleteAppraisalBatch(id)`：返回批次（completed）+ 生成的销毁清册（`listNo=DES-x`，`generatedListId`）。

### 12.2 `mock/modules/destruction.ts`

- `mockDestructionLists(params)`：≥4 个清册覆盖四态（draft/pending_approval/pending_destroy/destroyed）各一，支持 status/keyword 过滤。
- `mockDestructionListDetail(id)`：明细快照（含 fileDeleteStatus 三态）、审批单摘要（pending/已通过）、现场照片附件。
- `mockSubmitDestructionApproval(id, data)`：返回审批单（pending），清册→pending_approval。
- `mockUploadDestructionPhotos(id, files)`：校验后返回附件列表。
- `mockConfirmDestruction(id, data)`：清册→destroyed，明细 fileDeleteStatus→deleted。

### 12.3 `mock/modules/approval.ts`

- `mockApprovals(params)`：≥6 个审批单，覆盖三种类型 ×（pending/approved/rejected），支持 approvalType/status/keyword 过滤。
- `mockApprovalDetail(id)`：
  - 密级调整：`targetArchive` + `evidenceArchive`（含匹配/不匹配各一条用于门控演示）+ 前后值。
  - 开放调整：同结构，`newValue` 为 open/closed。
  - 销毁审批：`destructionList` 摘要 + 明细快照 + 来源批次。
- `mockApproveApproval(id, data)` / `mockRejectApproval(id, data)`：返回更新后审批单。销毁审批退回时，联动将对应销毁清册 mock 状态回退到 `draft`（§13.4）；通过时联动置 `pending_destroy`。

mock 数据需保证联动一致：鉴定 completed 批次的 `generatedListId` 对应销毁 mock 中某清册；销毁 pending_approval 清册的 `approvalRequestId` 对应审批 mock 中某销毁审批单。

## 13. 测试与验证

### 13.1 单元测试

- `api/appraisal.spec.ts`、`api/destruction.spec.ts`、`api/approval.spec.ts`：mock 分支返回结构与类型正确性。
- `utils/appraisalValidation.spec.ts`：延长保存需新期限/新到期日；未处理项阻止完成。
- `utils/destructionValidation.spec.ts`：双监销人不同/方式/不可逆/照片数量校验。

### 13.2 组件交互测试

- `/admin/appraisal`：创建批次→逐件鉴定→保存→完成→提示清册号；延长保存缺新期限时禁用完成。
- `/admin/destruction`：状态过滤、快照渲染、提交审批门控、确认销毁弹窗校验与不可逆门控。
- `/admin/approval`：类型切换、凭证门控（不匹配禁用通过）、通过/退回意见必填。
- 跨页联动：`?focus=` query 选中目标项。

### 13.3 构建验证

```bash
npm run type-check
npm run build
npm run test:unit
```

TypeScript 检查、生产构建、单元测试均需通过（杜绝 archive-hu 式类型错误阻塞集成）。

## 14. 验收口径

- `/admin/appraisal`、`/admin/destruction`、`/admin/approval` 无「待实现」占位。
- 页面布局、视觉层级、文案、交互结构、状态标签还原对应 HTML 原型；字段补充不改变原型组织方式。
- 鉴定：延长/待销毁互斥，延长需新期限+新到期日；完成生成清册可跳销毁页。
- 销毁：四态过滤、快照展示、提交审批可跳审批工作台、确认销毁不可逆双确认 + 双监销人 + 现场照片 + 证据链。
- 审批：三类型切换正确，目标/凭证/清册快照证据可见，凭证异常禁用通过，通过/退回意见必填。
- 跨页联动（鉴定→销毁→审批）`?focus=` 跳转可用。
- 三页覆盖加载中、空数据、接口错误三态。
- API 函数全部支持 mock/真实切换；mock 数据对齐接口 `data` 字段内容且联动一致。
- TypeScript 检查、生产构建、单元测试通过。
