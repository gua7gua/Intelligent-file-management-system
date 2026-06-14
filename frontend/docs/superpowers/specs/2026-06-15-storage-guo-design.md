# feat/storage-guo 设计文档：库房与架位 · 档案盘点 · 借阅审批/出库/归还

**日期**：2026-06-15
**负责人**：郭一坤
**分支**：`feat/storage-guo`
**阶段**：06-15 至 06-17
**优先级**：P1
**共同验收点**：库房利用与审批处置页面可联动 —— 库房、盘点、借阅审批/出库/归还页面可操作，纸质借阅闭环可走通

## 1. 背景

本轮从最新 `develop`（`4c9ad36`）开始，`develop` 已包含以下前置工作：

- 郭一坤 `feat/base-guo`（PR#9）：登录、角色命名、API request 层、mock 切换、字典 API、四类 Layout。
- 郭一坤 `feat/portal-guo`（PR#13）：移交门户、公众门户页面。
- 郭一坤 `feat/search-guo`（已合并）：内部查阅者检索、预览、下载、借阅申请；已落地 `types/internal.ts`、`api/internal.ts`、`mock/modules/internal.ts`，其中 `BorrowRequest`/`BorrowRequestDetail`/`BorrowStatus`/`BorrowStatusLabel`/`validateBorrowRequest` 为本轮借阅审批复用基础。
- 胡颖 `feat/acceptance-hu`（PR#10）、`feat/archive-hu`（PR#15）、`feat/appraisal-hu`（PR#20）：验收、入库、档案管理、上架、鉴定、销毁、审批工作台页面，确立了「左列表 + 右详情 + 顶部指标 + 状态 tab + 原生表格/徽章 + Element Plus 仅用于按钮/Message/MessageBox/Dialog」的前端落地风格基线。
- 周扬 `feat/storage-zhou`（PR#19，已合并）：**库房后端 `WarehouseController` 9 个端点已就绪**（§16.1–16.9），DTO 字段为本轮库房 mock 的权威来源。
- 刘星 `feat/search-liu`（PR#18，已合并）：内部检索/借阅申请后端、下载审计。

本轮聚焦郭一坤 06-15 至 06-17 前端任务。后端状态：

- **库房（§16）已就绪**（`WarehouseController`）。本轮库房页面仍走 `VITE_USE_MOCK=true`（与项目统一开关一致，且 mock 字段严格对齐后端 DTO，后端联调时切 `false` 即无缝对接）。
- **盘点（§18）后端缺失**（计划属刘星 `feat/stats-liu`），基于 `doc/接口文档.md` 第 18 章 + `doc/数据库设计.md` 10.1/10.2 用 mock 开发。
- **借阅审批/出库/归还（§12）后端缺失**（属刘星 `feat/borrow-liu`，未合并），基于第 12 章 + DB 9.1 用 mock 开发。

> **教训（来自 archive-hu）**：archive-hu 曾有类型错误阻塞 develop 构建，由郭一坤代为修复（commit `ed016d6`）。本轮每个阶段主动执行 `npm run type-check && npm run build`，杜绝再次阻塞集成。

用户已确认两点关键决策：

1. **产出范围**：本轮产出 design spec + implementation plan，并**继续编码实现**三页（spec→plan→编码一气呵成）。
2. **档案盒 CRUD 落点**：第 16.6–16.9 的档案盒查询/详情/新增/移动**不新增独立大区**，在架位看板的盒位抽屉与空闲架位上提供入口，底部「最近占用盒位」表格覆盖 §16.6 查询。

## 2. 目标与范围

### 2.1 本轮实现

| 页面/组件 | 路由 | 说明 | 原型 | 角色 |
|-----------|------|------|------|------|
| 库房与架位 | `/admin/warehouse` | 重写占位页：库房列表 + 架位看板 + 盒位详情抽屉 + 档案盒新增/移动 + 最近占用盒位 + 添加库房 | `doc/prototype/admin/warehouse.html` | back_archivist |
| 档案盘点 | `/admin/inventory` | 重写占位页：盘点任务列表 + 盘点明细逐件核对 + 新建/开始/完成盘点 | `doc/prototype/admin/inventory.html` | back_archivist |
| 借阅审批 | `/admin/borrow-approval` | 重写占位页：借阅申请列表 + 审批 + 凭证导出 + 确认出库 + 确认归还 | `doc/prototype/admin/borrow-approval.html` | back_archivist / front_archivist |
| 库房-库房列表子组件 | `views/admin/warehouse/components/RoomList.vue` | 库房卡片列表 + 占用率进度条 + 告警 | warehouse.html 库房列表区 | — |
| 库房-架位看板子组件 | `views/admin/warehouse/components/RackBoard.vue` | 机架×层×盒位 slot 网格 + 状态四态 | warehouse.html 架位看板 | — |
| 库房-盒位详情抽屉子组件 | `views/admin/warehouse/components/BoxDetailDrawer.vue` | 盒位/盒详情 + 新增盒/移动盒/查看盒内档案/停用架位 | warehouse.html 盒位详情抽屉 | — |
| 库房-添加库房弹窗子组件 | `views/admin/warehouse/components/CreateRoomDialog.vue` | 库房号/名称/机架/层/盒位/阈值 + 容量实时计算 | warehouse.html 添加库房弹窗 | — |
| 库房-最近占用盒位表格子组件 | `views/admin/warehouse/components/RecentBoxesTable.vue` | 盒位列表（§16.6 查询） + 状态徽章 | warehouse.html 最近占用盒位表 | — |

路由 `routes/admin.ts` 与菜单 `adminMenuConfig` 已配置三页（`warehouse`/`inventory` 在「库房管理」组，`borrow-approval` 在「利用服务」组），本轮**不改路由和菜单**。

### 2.2 不在本轮

- 统计、研判、编研、保存、工作台补齐（属郭一坤 `feat/stats-guo`，06-17 至 06-18）。
- 库房容量告警看板接口对接（概览页消费，本轮仅在库房页内展示）。
- 后端盘点接口（刘星 `feat/stats-liu`）、借阅审批/出库/归还接口（刘星 `feat/borrow-liu`）实现。
- 密集架动态重排、库房内复杂迁移路径规划（DB 7.1 明确本期不做）。
- 借阅凭证 PDF 落库到 `business_attachments`（DB 9.1 可选，本轮前端走打印预览，不落库）。

## 3. 方案选择

### 3.1 文件组织（per-subdomain，沿用项目约定）

| 方案 | 内容 | 优点 | 缺点 |
|------|------|------|------|
| 拆分三个子域文件（选定） | `types`+`api`+`mock` 各分 `warehouse`/`inventory`/`borrow-approval` 三个文件 | 边界清晰，符合 `reception`/`transfer`/`archive`/`internal`/`appraisal` 既有 per-subdomain 约定；三组 API 端点（§16/18/12）天然分离 | 借阅实体跨 `internal.ts` 与 `borrow-approval.ts` |
| 合并为单一 storage 文件 | 三页共用一个文件 | 文件少 | 语义混杂，与项目惯例不一致 |

**选定**：拆分三个子域文件。借阅审批复用 `types/internal.ts` 的 `BorrowRequest`/`BorrowRequestDetail`，扩展字段与查询参数放 `types/borrow-approval.ts`，不重复定义借阅实体（沿用胡颖 commit `584eedd` 去重原则）。

### 3.2 warehouse 组件拆分（已确认）

`warehouse.html` 原型 722 行 + 9 个接口 + 5 类交互（库房列表/架位看板/盒位抽屉/添加库房弹窗/最近占用表），单文件必破 600 行（`frontend/CLAUDE.md` 8.1）。

| 方案 | 内容 | 优点 | 缺点 |
|------|------|------|------|
| 抽 5 个子组件（选定） | RoomList/RackBoard/BoxDetailDrawer/CreateRoomDialog/RecentBoxesTable | 主页面只做状态编排与数据加载，每子组件职责单一可独立理解 | 多 5 个文件 |
| 单文件内联 | 全部塞 index.vue | 文件少 | 单文件 800+ 行，状态/交互耦合 |

**选定**：抽 5 个子组件。`inventory`、`borrow-approval` 原型结构相对聚焦（左列表 + 右单面板），以内联为主，必要时抽弹窗子组件。

### 3.3 原型还原原则（沿用 `feat/portal-guo` / `feat/search-guo`，参照 `feat/appraisal-hu`）

- 页面布局、视觉层级、网格比例、卡片组织、列表/详情关系、状态标签、主要文案优先保持原型原貌。
- 实现 Vue 页面时优先搬入原型 `<body>` 内容区结构和页面内样式，原型 JavaScript 转为 `<script setup>` 响应式状态和事件。
- 原型 CSS 类名、设计令牌、公共组件类（`.card`、`.toolbar`、`.button`、`.grid`、`.status`、`.metric`、`.table-wrap`、`.notice`、`.tabs`、`.slot`、`.rack-row`、`.usage-track` 等）优先保留；Element Plus 只在对话框、消息提示、日期选择、表单校验、抽屉等能补足交互质量处使用。
- 接口文档和数据库设计用于补齐原型未细化的字段、API、mock、加载/空/错误状态和权限边界，**不推翻原型布局**。
- 展示层保留原型业务语言，数据字段和请求参数按接口文档/后端 DTO/数据库设计命名（字段冲突时以接口文档 + 后端 DTO + DB 设计为准，原型示意字段作参考）。

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
  enums.ts                       # 扩展：库房/盘点/借阅归还检查相关枚举与中文映射
  warehouse.ts                   # 库房/架位/档案盒类型（对齐后端 WarehouseController DTO）
  inventory.ts                   # 盘点任务/明细/统计/创建/查询类型
  borrow-approval.ts             # 借阅审批查询参数 + 扩展详情字段 + 审批/出库/归还请求体

frontend/src/api/
  warehouse.ts                   # 第 16 章 9 个 /api/admin/warehouse/* 接口
  inventory.ts                   # 第 18 章 6 个 /api/admin/inventory-tasks/* 接口
  borrow-approval.ts             # 第 12 章 5 个 /api/admin/borrow-requests/* 接口 + 凭证导出

frontend/src/mock/modules/
  warehouse.ts                   # 库房/架位/档案盒 mock 数据
  inventory.ts                   # 盘点任务/明细 mock 数据
  borrow-approval.ts             # 借阅申请（多状态）mock 数据

frontend/src/utils/
  borrowApprovalValidation.ts    # 借阅审批/出库/归还校验（区别于已存在的 borrowValidation.ts）

frontend/src/views/admin/
  warehouse/index.vue                                   # 库房与架位（重写）
  warehouse/components/RoomList.vue                     # 库房列表
  warehouse/components/RackBoard.vue                    # 架位看板
  warehouse/components/BoxDetailDrawer.vue              # 盒位/盒详情抽屉
  warehouse/components/CreateRoomDialog.vue             # 添加库房弹窗
  warehouse/components/RecentBoxesTable.vue             # 最近占用盒位表
  inventory/index.vue                                   # 档案盘点（重写）
  borrow-approval/index.vue                             # 借阅审批（重写）
```

测试文件（`.spec.ts`）与各 `api/`、`utils/`、`views/` 源文件同目录，对齐既有约定。

## 6. 枚举扩展（`types/enums.ts`）

新增常量与中文映射，复用既有 `BorrowStatus`/`BorrowStatusLabel`（借阅审批直接复用，不重复定义）。

```typescript
/** 库房状态 */
export const WarehouseRoomStatus = {
  ACTIVE: 'active',
  DISABLED: 'disabled',
} as const

/** 架位状态 */
export const LocationStatus = {
  ACTIVE: 'active',
  DISABLED: 'disabled',
} as const

/** 档案盒状态（对齐 DB 7.3） */
export const BoxStatus = {
  NORMAL: 'normal',
  FULL: 'full',
  MOVED: 'moved',
  DESTROYED: 'destroyed',
} as const

/** 盘点任务状态（对齐 DB 10.1） */
export const InventoryTaskStatus = {
  DRAFT: 'draft',
  RUNNING: 'running',
  COMPLETED: 'completed',
} as const

/** 盘点结果（对齐 DB 10.2 check_result） */
export const InventoryCheckResult = {
  NORMAL: 'normal',
  MISSING: 'missing',
  MISPLACED: 'misplaced',
  DAMAGED: 'damaged',
  ON_LOAN: 'on_loan',
} as const

/** 归还检查结果（对齐 DB 9.1 return_check_result） */
export const ReturnCheckResult = {
  NORMAL: 'normal',
  DAMAGED: 'damaged',
  MISSING_PAGE: 'missing_page',
  OTHER: 'other',
} as const

// 中文映射
export const WarehouseRoomStatusLabel: Record<string, string> = {
  active: '启用',
  disabled: '停用',
}
export const LocationStatusLabel: Record<string, string> = {
  active: '启用',
  disabled: '停用',
}
export const BoxStatusLabel: Record<string, string> = {
  normal: '正常',
  full: '满盒',
  moved: '已移动',
  destroyed: '已销毁',
}
export const InventoryTaskStatusLabel: Record<string, string> = {
  draft: '草稿',
  running: '进行中',
  completed: '已完成',
}
export const InventoryCheckResultLabel: Record<string, string> = {
  normal: '正常',
  missing: '缺失',
  misplaced: '错位',
  damaged: '损坏',
  on_loan: '借出中',
}
export const ReturnCheckResultLabel: Record<string, string> = {
  normal: '正常',
  damaged: '破损',
  missing_page: '缺页',
  other: '其他',
}
```

派生值类型沿用既有 `enums.ts` 模式（`export type XxxValue = (typeof Xxx)[keyof typeof Xxx]`）：`WarehouseRoomStatusValue`、`LocationStatusValue`、`BoxStatusValue`、`InventoryTaskStatusValue`、`InventoryCheckResultValue`、`ReturnCheckResultValue`。

## 7. 类型设计

字段名严格对齐 `doc/接口文档.md`（§12/16/18）、后端 `WarehouseController` DTO、`doc/数据库设计.md`（7.1–7.4/9.1/10.1–10.2）。

### 7.1 `types/warehouse.ts`

```typescript
import type { PageData, PageParams } from './api'
import type { BoxStatusValue, LocationStatusValue, WarehouseRoomStatusValue } from './enums'

/** 库房列表查询参数（§16.1） */
export interface WarehouseRoomParams {
  status?: WarehouseRoomStatusValue | ''
  keyword?: string
}

/** 库房（对齐 WarehouseRoomResponse） */
export interface WarehouseRoom {
  id: number
  roomNo: string
  roomName: string
  rackCount: number
  layersPerRack: number
  boxesPerLayer: number
  /** 总盒位 = rackCount * layersPerRack * boxesPerLayer */
  capacity: number
  /** 告警阈值，0~1，默认 0.85 */
  warningThreshold: number
  status: WarehouseRoomStatusValue
  /** 已占用盒位 */
  occupiedSlots: number
  /** 占用率，0~1 */
  occupancyRate: number
  /** 是否达到告警阈值 */
  warning: boolean
}

/** 新增库房请求（§16.2 / WarehouseRoomCreateRequest） */
export interface WarehouseRoomCreateData {
  roomNo: string
  roomName: string
  rackCount: number
  layersPerRack: number
  boxesPerLayer: number
  warningThreshold: number
}

/** 更新库房请求（§16.3 / WarehouseRoomUpdateRequest） */
export interface WarehouseRoomUpdateData {
  roomName?: string
  warningThreshold?: number
  status?: WarehouseRoomStatusValue
}

/** 架位查询参数（§16.4） */
export interface StorageLocationParams extends PageParams {
  roomId?: number
  rackNo?: number
  status?: LocationStatusValue | ''
  occupied?: boolean
}

/** 架位（对齐 StorageLocationResponse） */
export interface StorageLocation {
  id: number
  roomId: number
  rackNo: number
  layerNo: number
  boxSlotNo: number
  /** 如 401-03-02-05 */
  locationCode: string
  status: LocationStatusValue
  /** 是否被档案盒占用 */
  occupied: boolean
  currentBoxId?: number
  currentBoxNo?: string
  /** 当前盒内件数 */
  boxItemCount?: number
}

/** 架位状态变更请求（§16.5 / LocationStatusRequest） */
export interface LocationStatusData {
  status: LocationStatusValue
  reason?: string
}

/** 档案盒查询参数（§16.6） */
export interface ArchiveBoxParams extends PageParams {
  boxNo?: string
  roomId?: number
  categoryId?: number
  fondsId?: number
  status?: BoxStatusValue | ''
}

/** 档案盒列表项（对齐 ArchiveBoxResponse） */
export interface ArchiveBox {
  id: number
  boxNo: string
  locationId: number
  locationCode: string
  roomNo: string
  categoryId: number
  fondsId: number
  yearLabel: string
  spineText: string
  capacity: number
  usedCount: number
  status: BoxStatusValue
}

/** 盒内档案条目（ArchiveBoxDetailResponse.BoxItemView） */
export interface BoxItem {
  archiveId: number
  archiveNo: string
  title: string
  sortNo: number
  pageCount: number
  /** normal / damaged / lost */
  physicalStatus: string
}

/** 档案盒详情（§16.7 / ArchiveBoxDetailResponse） */
export interface ArchiveBoxDetail extends ArchiveBox {
  items: BoxItem[]
}

/** 新增档案盒请求（§16.8 / ArchiveBoxCreateRequest） */
export interface ArchiveBoxCreateData {
  locationId: number
  categoryId: number
  fondsId: number
  yearLabel: string
  spineText: string
  capacity: number
}

/** 移动档案盒请求（§16.9 / ArchiveBoxMoveRequest） */
export interface ArchiveBoxMoveData {
  targetLocationId: number
  reason: string
}

export type StorageLocationPage = PageData<StorageLocation>
export type ArchiveBoxPage = PageData<ArchiveBox>
```

### 7.2 `types/inventory.ts`

```typescript
import type { PageData, PageParams } from './api'
import type { InventoryCheckResultValue, InventoryTaskStatusValue } from './enums'

/** 盘点任务查询参数（§18.1） */
export interface InventoryTaskParams extends PageParams {
  status?: InventoryTaskStatusValue | ''
  roomId?: number
  categoryId?: number
}

/** 创建盘点任务请求（§18.2） */
export interface InventoryTaskCreateData {
  taskName: string
  roomId: number
  categoryId: number
}

/** 盘点任务统计（§18.4 返回的统计部分） */
export interface InventoryTaskStats {
  total: number
  checked: number
  normalCount: number
  missingCount: number
  misplacedCount: number
  damagedCount: number
  onLoanCount: number
}

/** 盘点任务摘要（列表行） */
export interface InventoryTask {
  id: number
  taskNo: string
  taskName: string
  roomId: number
  roomNo: string
  categoryId: number
  categoryName: string
  status: InventoryTaskStatusValue
  total: number
  checked: number
  abnormalCount: number
  startedAt?: string
  completedAt?: string
  summary?: string
  createdAt: string
}

/** 盘点明细项（§18.4 / DB 10.2 + 展示字段） */
export interface InventoryItem {
  id: number
  taskId: number
  archiveId: number
  archiveNo: string
  title: string
  /** 应在架位编码（由 expected_location_id 解析） */
  expectedLocationCode: string
  /** 实际架位编码 */
  actualLocationCode?: string
  checkResult: InventoryCheckResultValue | ''
  note?: string
  /** 关联档案当前借阅状态，用于展示「暂停借阅/借出中」 */
  loanStatus?: string
}

/** 盘点任务详情（§18.4） */
export interface InventoryTaskDetail extends InventoryTask {
  items: InventoryItem[]
  stats: InventoryTaskStats
}

/** 更新盘点明细请求（§18.5） */
export interface InventoryItemUpdateData {
  actualLocationCode?: string
  checkResult: InventoryCheckResultValue
  note?: string
}

/** 完成盘点请求（§18.6） */
export interface InventoryCompleteData {
  summary: string
}

export type InventoryTaskPage = PageData<InventoryTask>
```

### 7.3 `types/borrow-approval.ts`

复用 `types/internal.ts` 的 `BorrowRequest`/`BorrowRequestDetail`/`BorrowStatusValue`，扩展管理侧查询参数与展示字段。

```typescript
import type { PageData, PageParams } from './api'
import type { BorrowRequestDetail } from './internal'
import type { BorrowStatusValue, ReturnCheckResultValue } from './enums'

/** 管理侧借阅申请查询参数（§12.1） */
export interface BorrowApprovalParams extends PageParams {
  status?: BorrowStatusValue | ''
  /** 借阅人关键字 */
  borrowerKeyword?: string
  /** 档案关键字（档号/题名） */
  archiveKeyword?: string
  /** 是否逾期 */
  overdue?: boolean
}

/**
 * 管理侧借阅申请详情（§12.2），在 BorrowRequestDetail 基础上补：
 * 借阅人信息、档案盒号架位、载体、可借检查结果、命中盘点。
 */
export interface BorrowApprovalDetail extends BorrowRequestDetail {
  /** 借阅人姓名 */
  borrowerName: string
  /** 借阅人单位/部门 */
  borrowerOrg?: string
  /** 档案题名（列表/详情展示） */
  archiveTitle: string
  /** 档案所在盒号架位（管理员可见） */
  archiveLocationCode?: string
  /** 档案载体状态 */
  carrierStatus?: string
  /** 可借检查结果（载体/生命周期/借阅/盘点四项） */
  checkCarrier?: string
  checkLifecycle?: string
  checkLoan?: string
  checkInventory?: string
  /** 是否命中运行中盘点（true 时前端拦截审批通过） */
  inventoryHit?: boolean
}

/** 审批借阅请求（§12.3） */
export interface BorrowApproveData {
  approved: boolean
  opinion?: string
  /** 拒绝时与 opinion 二选一必填 */
  rejectReason?: string
}

/** 确认出库请求（§12.4） */
export interface BorrowCheckoutData {
  voucherNo: string
  dueAt: string
  note?: string
}

/** 确认归还请求（§12.5） */
export interface BorrowReturnData {
  returnCheckResult: ReturnCheckResultValue
  returnNote?: string
}

/** 凭证导出结果 */
export interface BorrowVoucherResult {
  voucherNo: string
  voucherIssuedAt: string
  /** 是否首次生成（重复导出复用原凭证号） */
  firstIssued: boolean
}

export type BorrowApprovalPage = PageData<BorrowApprovalDetail>
```

## 8. API 设计

所有函数内部通过 `USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'` 控制 mock/真实请求。真实端点严格对齐 `WarehouseController`（库房已就绪）与接口文档（盘点/借阅）。

### 8.1 `api/warehouse.ts`（§16）

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getWarehouseRooms(params?)` | GET | `/admin/warehouse/rooms` | 库房列表（含占用率/告警），返回 `WarehouseRoom[]` |
| `createWarehouseRoom(data)` | POST | `/admin/warehouse/rooms` | 新增库房，按规格批量生成架位 |
| `updateWarehouseRoom(roomId, data)` | PUT | `/admin/warehouse/rooms/{roomId}` | 维护名称/阈值/状态 |
| `getStorageLocations(params)` | GET | `/admin/warehouse/locations` | 架位分页（roomId/rackNo/status/occupied） |
| `updateLocationStatus(locationId, data)` | PUT | `/admin/warehouse/locations/{locationId}/status` | 停用/启用架位（已占用不可停用） |
| `getArchiveBoxes(params)` | GET | `/admin/warehouse/boxes` | 档案盒分页 |
| `getArchiveBoxDetail(boxId)` | GET | `/admin/warehouse/boxes/{boxId}` | 盒详情 + 盒内档案条目 |
| `createArchiveBox(data)` | POST | `/admin/warehouse/boxes` | 新增档案盒到空闲架位 |
| `moveArchiveBox(boxId, data)` | POST | `/admin/warehouse/boxes/{boxId}/move` | 移动档案盒到目标空闲架位 |

### 8.2 `api/inventory.ts`（§18）

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getInventoryTasks(params)` | GET | `/admin/inventory-tasks` | 盘点任务分页 |
| `createInventoryTask(data)` | POST | `/admin/inventory-tasks` | 创建任务 + 系统生成应盘明细 |
| `startInventoryTask(taskId)` | POST | `/admin/inventory-tasks/{taskId}/start` | draft→running |
| `getInventoryTaskDetail(taskId)` | GET | `/admin/inventory-tasks/{taskId}` | 任务 + 明细 + 统计 |
| `updateInventoryItem(taskId, itemId, data)` | PUT | `/admin/inventory-tasks/{taskId}/items/{itemId}` | 更新明细（须 running） |
| `completeInventoryTask(taskId, data)` | POST | `/admin/inventory-tasks/{taskId}/complete` | running→completed |

### 8.3 `api/borrow-approval.ts`（§12）

| 函数 | 方法 | 端点 | 说明 |
|------|------|------|------|
| `getBorrowApprovals(params)` | GET | `/admin/borrow-requests` | 借阅申请分页（status/borrowerKeyword/archiveKeyword/overdue） |
| `getBorrowApprovalDetail(requestId)` | GET | `/admin/borrow-requests/{requestId}` | 申请详情 + 档案摘要 + 盒号架位 + 节点 |
| `approveBorrowRequest(requestId, data)` | POST | `/admin/borrow-requests/{requestId}/approve` | 通过/拒绝（拒绝 opinion/rejectReason 必填） |
| `checkoutBorrowRequest(requestId, data)` | POST | `/admin/borrow-requests/{requestId}/checkout` | 核验凭证并确认出库，档案→on_loan |
| `returnBorrowRequest(requestId, data)` | POST | `/admin/borrow-requests/{requestId}/return` | 确认归还（异常必填 returnNote） |
| `exportBorrowVoucher(requestId)` | POST | `/admin/borrow-requests/{requestId}/voucher` | 导出/生成凭证号，approved→voucher_issued |

> **凭证导出端点说明**：接口文档第 12 章未显式列出管理员侧凭证导出端点（11.10 为内部查阅者侧）。本轮在 api 层**预留** `POST /admin/borrow-requests/{requestId}/voucher`（与 11.10 对称），mock 实现生成 `VCH-xxxxxx` 并将状态置为 `voucher_issued`；真实端点由 `feat/borrow-liu` 补齐。前端导出交互走「生成凭证号 + 打印预览」，不落库 PDF（DB 9.1 可选）。

## 9. 页面设计

> **顶部指标卡取数**：接口文档（§12/16/18）未定义库房/盘点/借阅的看板聚合接口，三页顶部 `metric` 卡为页面快照数值（与原型静态示意一致）。mock 下用常量或本页列表派生（如「待审批」由借阅申请 applied 数量派生，「容量告警」由库房 `warning=true` 数量派生），后端若后续补看板接口再改为独立请求；当前不新增 API 函数。

### 9.1 库房与架位 `/admin/warehouse`

**目标**：维护库房房间、机架、层、盒位结构，可视化架位占用，管理档案盒上架/移动，监控容量告警。

**布局**（还原 `warehouse.html`）：

- 顶部 4 个 `metric` 卡：启用库房、总盒位、已占用盒位（占用率）、容量告警数。
- 「添加库房」按钮 → `CreateRoomDialog`。
- `workspace` 双栏（左库房列表 `360px` + 右架位区 `1fr`）：
  - 左列 `RoomList`：库房卡片（库房号+名称、机架×层×盒位规格、状态徽章、占用率进度条 `usage-track`/`usage-fill`，达阈值告警色 `warning`）。点击切换当前库房。
  - 右列：`rule-strip`（架位编码规则提示 `401-03-02-05`）+ 筛选区（库房号/机架/架位状态）+ `location-layout`（`RackBoard` 架位看板 + `BoxDetailDrawer` 盒位详情抽屉 `300px`）。
- 底部 `RecentBoxesTable`：最近占用盒位表（库房号/位置编码/盒号/分类/全宗/盒内件数/状态）。

**`RackBoard` 架位看板**：按机架分组（`rack-row` = 机架标签 + `slot-grid`），每个 `slot` 四态：
- 空闲（`slot`）：无盒。
- 已占用（`slot occupied`）：显示盒号。
- 停用（`slot disabled`）：`status=disabled`。
- 告警/跨类（`slot alert`）：占用且盒 `status=full` 或跨类待调整。
点击 slot → 选中（`active`）→ `BoxDetailDrawer` 展示该架位/盒详情。

**`BoxDetailDrawer` 盒位详情抽屉**（档案盒 CRUD 入口，承载 §16.5/16.7/16.8/16.9）：
- 通用信息：位置编码、架位状态、当前盒号、盒脊信息、盒内件数。
- 空闲架位：操作「新增档案盒到此位」→ 内联表单（分类/全宗/年度/盒脊/容量）→ `createArchiveBox`；「停用架位」→ `updateLocationStatus(disabled)`。
- 已占用架位：操作「查看盒内档案」→ `getArchiveBoxDetail` 展示盒内条目表；「移动档案盒」→ 选目标空闲架位 → `moveArchiveBox`；提示「已占用架位需迁出档案盒后才能停用」。
- `BoxStatus`/`LocationStatus` 徽章用 `enums.ts` 中文映射 + `.status` 色系。

**`CreateRoomDialog` 添加库房弹窗**：库房号、库房名称、机架数、单机架层数、每层盒数、告警阈值（85%/90%/80% 下拉）；实时计算「预计生成 N 个盒位，示例编码 `{roomNo}-01-01-01`」；提交 `createWarehouseRoom` → 刷新库房列表并选中新库房。

**行为**：

- 首次加载 `getWarehouseRooms()` → 顶部指标按 §9 开头说明取数；默认选中第一个库房。
- 选中库房 → `getStorageLocations({ roomId, rackNo, status, occupied })` 加载架位（分页或按机架分组）。
- 选中 slot → 若占用，`getArchiveBoxDetail(currentBoxId)` 加载盒详情供抽屉展示。
- 新增库房 → 刷新库房列表；mock 下按规格生成架位并回填新库房。
- 停用已占用架位 → 前端拦截 + 提示（对齐 §16.5 校验「已占用架位不得直接停用」）。
- 移动档案盒 → 目标架位须空闲且启用（对齐 §16.9 校验）；成功后刷新源/目标架位。
- 三态：库房加载中、无库房、库房加载失败+重试；架位加载中、空架位、架位加载失败；盒详情加载失败。

### 9.2 档案盘点 `/admin/inventory`

**目标**：按「库房号 AND 分类」生成盘点任务，逐件核对实物与架位，提交结果后范围内档案恢复可借；命中盘点的档案暂停借阅。

**布局**（还原 `inventory.html`）：

- 顶部 4 个 `metric` 卡：进行中任务、暂停借阅档案、异常项、本年完成。
- `scope-banner`：盘点范围横幅（`库房号：401` AND `分类：科技档案`）+「借阅暂停生效」徽章。
- `workspace` 双栏（左任务列表 `330px` + 右明细 `1fr`）：
  - 左列：任务状态 `tabs`（进行中/草稿/已完成）+ `task-list`（任务名、范围、件数、进度条 `progress-track`/`progress-fill`、已核对数、状态徽章）。
  - 右列：`inventory-toolbar`（任务标题+任务号+状态+开始时间 +「提交盘点结果」按钮）+ `result-filter`（mini-button：全部/正常/缺失/错位/损坏/借出中，带计数）+ 明细表。
- 明细表列：档号+题名、应在架位（`mono`）、实际架位（可编辑 `inline-input`）、借阅状态（暂停借阅/借出中徽章）、盘点结果（`result-option` 按钮组：正常/错位/缺失/损坏，借出中档案追加「借出中」选项）、说明（可编辑）。
- 借阅状态列映射规则（消除歧义）：`loanStatus === 'on_loan'` → 「借出中」（info 徽章）；否则任务 `running` 时 → 「暂停借阅」（warning 徽章，表示命中盘点范围、借阅被暂停）；`completed` 且可借 → 「可借」（success）。即「暂停借阅」是 `loanStatus=available` 且任务进行中的派生展示，「借出中」直接取 `loanStatus`。
- 底部 `borrow-impact` 三卡：命中范围暂停、已借出不强制召回、完成后恢复可借。

**新建盘点任务弹窗**：库房号（下拉，来自库房列表）、分类（下拉）、任务名称（按 `{年度} {库房} 库房{分类}盘点` 自动生成可改）；提交 `createInventoryTask` → 刷新列表并选中。

**行为**：

- 首次加载 `getInventoryTasks()` → 顶部指标派生（进行中=running 数、异常=abnormal 汇总、本年完成=completed 本年数）。
- 选中任务 → `getInventoryTaskDetail(id)` 加载明细 + 统计；范围横幅按任务 roomNo/categoryName 渲染。
- `draft` 任务：「开始盘点」`startInventoryTask(id)`（draft→running）；无明细时禁用。
- `running` 任务：逐行编辑实际架位/盘点结果/说明，`updateInventoryItem(taskId, itemId, data)`（行内保存，须 running）；`result-filter` 按结果筛选明细。
- 「提交盘点结果」`completeInventoryTask(id, { summary })`：summary 由统计自动生成（应盘 N 件，正常 X，错位 Y，缺失 Z，借出 W）；running→completed。
- `completed` 任务：明细只读展示。
- 三态：任务加载中、无任务、任务加载失败；明细加载中、明细为空、明细加载失败；草稿无明细时禁用开始。

### 9.3 借阅审批 `/admin/borrow-approval`

**目标**：集中处理纸质借阅申请——审批通过后申请人导出凭证，到馆核验并确认出库后档案才借出，归还检查后恢复，闭环留痕。

**布局**（还原 `borrow-approval.html`）：

- 顶部 4 个 `metric` 卡：待审批、已批准未出库、借出中、逾期/异常。
- `steps` 四步流程：提交申请→后台审批→凭证核验→出库归还。
- `borrow-layout` 双栏（左申请列表 `360px` + 右三面板 `1fr`）：
  - 左列 `panel`：状态 `tabs`（待审批 applied / 已批准 approved+voucher_issued / 借出中 checked_out / 归还处理 returned+abnormal_return+rejected）+ `request-card` 列表（题名、申请号、状态徽章、借阅人）。
  - 右列 `panel`×3：
    1. 申请详情：申请号、借阅人、预计到馆、借阅时长、档号、管理员可见架位、借阅理由。
    2. 可借检查 + 审批：`check-list`（载体状态/生命周期/借阅状态/盘点范围）+ 拒绝/退回原因 textarea + 「审批通过」「审批拒绝」按钮。
    3. 凭证、出库与归还：凭证号输入、归还检查结果下拉（正常/缺页/破损/其他）、归还检查说明 textarea + 「导出凭证」「确认出库」「确认归还」按钮。

**按状态启用操作**：

- `applied`：可审批通过/拒绝。
- `approved` / `voucher_issued`：可导出凭证、可确认出库。
- `checked_out`：可确认归还。
- `returned` / `abnormal_return` / `rejected`：终态，按钮区展示结果说明，无可操作按钮。

**行为**：

- 首次加载 `getBorrowApprovals()` → 顶部指标按状态派生；默认 tab=待审批，选中第一条。
- 选中申请 → `getBorrowApprovalDetail(id)` 加载详情（含可借检查结果、命中盘点）。
- 「审批通过」`approveBorrowRequest(id, { approved: true, opinion })`：命中盘点（`inventoryHit=true`）时前端拦截 + 提示「目标档案命中进行中盘点，不可审批通过」；通过 applied→approved。
- 「审批拒绝」`approveBorrowRequest(id, { approved: false, rejectReason })`：`rejectReason` 必填；applied→rejected。
- 「导出凭证」`exportBorrowVoucher(id)`：仅 approved/voucher_issued 可用；生成/复用凭证号，approved→voucher_issued；前端提供打印预览。
- 「确认出库」`checkoutBorrowRequest(id, { voucherNo, dueAt, note })`：`voucherNo` 必填、`dueAt` 必填（日期选择器）；approved/voucher_issued→checked_out，档案 on_loan。
- 「确认归还」`returnBorrowRequest(id, { returnCheckResult, returnNote })`：`returnCheckResult !== 'normal'` 时 `returnNote` 必填；checked_out→returned（normal）或 abnormal_return（异常），档案恢复 available。
- 三态：列表加载中、无申请、列表加载失败；详情加载中、详情加载失败；操作 toast 反馈（沿用原型 `notice toast`）。

## 10. 跨页联动

- **盘点 → 借阅审批**：盘点任务 `running` 期间，范围内（`roomId AND categoryId`）档案的借阅申请在借阅审批页可借检查显示「命中进行中盘点」，审批通过被前端拦截（对齐 DB 10.1 业务规则：盘点不直接改 `loan_status`，由借阅接口动态校验）。mock 下 `borrow-approval` mock 数据中部分申请的 `inventoryHit=true` 演示该联动。
- **库房 → 借阅审批**：借阅申请详情的「管理员可见架位」字段（`archiveLocationCode`）来源于库房架位/档案盒数据；mock 下借用库房 mock 的 `locationCode` 示意值（如 `401-03-02-05`），保持编码规则一致。
- **库房 → 盘点**：盘点新建任务弹窗的库房号下拉，数据源为库房列表（`getWarehouseRooms`）；mock 下复用同一批库房编号（401/402/403）保证口径一致。
- 三页各自 mock 数据自闭环（不依赖跳转才能工作），联动仅提升处置链连续性。

## 11. 错误处理与边界

- 每页/组件必须处理加载中、空数据、接口失败、表单校验失败（`frontend/CLAUDE.md` AI 约束）。
- 捕获 API 异常后展示页面级 `notice` 或 Element Plus 消息，避免静默失败。
- **库房**：新增库房库房号唯一、容量参数 >0；更新库房本期只维护名称/阈值/状态（不改结构数量）；已占用架位不可停用；新增档案盒架位须空闲且启用；移动档案盒目标架位须空闲且启用；混盒校验（同盒 categoryId/fondsId 一致，对齐 DB 7.4）。
- **盘点**：范围必须同时给 roomId + categoryId；任务须有明细才能开始；明细更新须 running；完成盘点须 running；盘点期间范围档案借阅被拦截。
- **借阅**：命中盘点不可审批通过；拒绝 opinion/rejectReason 必填；凭证号必须匹配（出库校验）；仅 approved/voucher_issued 可出库；仅 checked_out 可归还；异常归还 returnNote 必填。
- 不可逆/敏感操作（确认出库、确认归还、停用架位）二次确认（`ElMessageBox.confirm`）。
- mock 下文件上传/凭证导出用前端实现，不落库。
- 枚举值与状态标签复用 `enums.ts`，三页状态展示一致。
- 本轮不改胡颖已合并页面与 search-guo 借阅申请页；共享语义（借阅实体）只通过 `types/internal.ts` 保持一致，扩展字段放 `types/borrow-approval.ts`。

## 12. Mock 数据

`src/mock/modules/`，结构对齐接口 `data` 字段内容（不包 `R<T>`），分页模拟完整 `PageData<T>`。字段严格对齐后端 DTO（库房）/ DB 设计（盘点/借阅）。

### 12.1 `mock/modules/warehouse.ts`

- `mockWarehouseRooms(params?)`：≥3 个库房（401 综合/402 会计达阈值告警/403 科技），含 `capacity`/`occupiedSlots`/`occupancyRate`/`warning`；支持 status/keyword 过滤；返回 `WarehouseRoom[]`（库房列表非分页，对齐后端 `listRooms` 返回 `List`）。
- 内部 `rooms` 与 `locations`/`boxes` 联动：新增库房按规格生成架位；架位 `occupied`/`currentBoxNo` 由档案盒反查。
- `mockCreateWarehouseRoom(data)`：返回新库房（`roomNo` 唯一校验）+ 按规格生成架位。
- `mockUpdateWarehouseRoom(id, data)`：维护名称/阈值/状态。
- `mockStorageLocations(params)`：按 roomId/rackNo/status/occupied 过滤 + 分页；返回 `StorageLocation[]` 含 `locationCode`/`occupied`/`currentBoxNo`/`boxItemCount`。
- `mockUpdateLocationStatus(id, data)`：已占用抛错（演示校验），否则切换 active/disabled。
- `mockArchiveBoxes(params)`：≥6 个档案盒覆盖 normal/full/moved/destroyed；支持 boxNo/roomId/categoryId/fondsId/status 过滤 + 分页。
- `mockArchiveBoxDetail(id)`：盒信息 + `items[]`（archiveNo/title/sortNo/pageCount/physicalStatus 覆盖 normal/damaged/lost）。
- `mockCreateArchiveBox(data)`：架位空闲且启用校验；生成 `boxNo`（`BX-xxxxx`），占用架位。
- `mockMoveArchiveBox(id, data)`：目标架位空闲校验；释放源架位、占用目标架位。
- 提供 `getWarehouseRoomsForDropdown()`（或直接复用 `mockWarehouseRooms()`）供盘点任务弹窗库房下拉使用。

### 12.2 `mock/modules/inventory.ts`

- `mockInventoryTasks(params)`：≥4 个任务覆盖 draft/running/completed；含 total/checked/abnormalCount/进度；支持 status/roomId/categoryId 过滤 + 分页。
- `mockInventoryTaskDetail(id)`：`items[]`（覆盖 normal/missing/misplaced/damaged/on_loan 五种 checkResult，借出中项 loanStatus=on_loan）+ `stats`（按 items 统计）。
- `mockCreateInventoryTask(data)`：生成 `taskNo`（`PD-yyyyMM-xxx`）+ 系统生成应盘明细（基于 roomId+categoryId 范围的若干档案，初始 checkResult=''）。
- `mockStartInventoryTask(id)`：draft→running，写 startedAt。
- `mockUpdateInventoryItem(taskId, itemId, data)`：running 校验；更新 actualLocationCode/checkResult/note；同步 stats。
- `mockCompleteInventoryTask(id, data)`：running→completed，写 completedAt/summary（summary 缺省时按 stats 自动生成）。

### 12.3 `mock/modules/borrow-approval.ts`

- `mockBorrowApprovals(params)`：≥6 个申请覆盖 applied/approved/voucher_issued/checked_out/returned/abnormal_return/rejected 各态；含 `inventoryHit`（≥1 条 true 演示盘点拦截）、`overdue`（≥1 条 checked_out 且 dueAt 过期）；支持 status/borrowerKeyword/archiveKeyword/overdue 过滤 + 分页。
- `mockBorrowApprovalDetail(id)`：申请详情 + 借阅人/档案题名/盒号架位/载体 + 可借检查四项 + 命中盘点。
- `mockApproveBorrowRequest(id, data)`：inventoryHit 且 approved=true 抛错；否则 applied→approved（通过）/applied→rejected（拒绝，写 rejectReason）。
- `mockCheckoutBorrowRequest(id, data)`：approved/voucher_issued→checked_out，写 checkedOutAt/dueAt/voucherNo。
- `mockReturnBorrowRequest(id, data)`：checked_out→returned（normal）/abnormal_return（其他），写 returnedAt/returnCheckResult/returnNote。
- `mockExportBorrowVoucher(id)`：approved→voucher_issued，生成 `VCH-xxxxxx`（复用原号）；返回 `BorrowVoucherResult`。

mock 数据需保证联动一致：借阅申请的 `archiveLocationCode` 与库房 mock 的 `locationCode` 编码规则一致（`roomNo-rack-layer-slot`）；盘点任务的 `roomNo`/`categoryName` 与库房 mock 库房、字典分类口径一致。

## 13. 测试与验证

### 13.1 单元测试

- `api/warehouse.spec.ts`、`api/inventory.spec.ts`、`api/borrow-approval.spec.ts`：mock 分支返回结构与类型正确性、过滤逻辑、分页结构。
- `utils/borrowApprovalValidation.spec.ts`：命中盘点拦截审批通过；拒绝必填 rejectReason；出库 voucherNo/dueAt 必填；异常归还必填 returnNote；状态门控（仅 approved 可出库、仅 checked_out 可归还）。

### 13.2 组件交互测试

- `/admin/warehouse`：库房切换加载架位；slot 四态渲染；空闲 slot 新增盒；占用 slot 查看盒内/移动；已占用架位停用被拦截；添加库房容量计算。
- `/admin/inventory`：任务状态过滤；新建任务生成明细；开始盘点门控；逐行更新结果；提交盘点统计汇总。
- `/admin/borrow-approval`：状态 tab 过滤；命中盘点禁用通过；拒绝/出库/归还校验与门控；导出凭证状态流转。

### 13.3 构建验证

```bash
npm run type-check
npm run build
npm run test:unit
```

TypeScript 检查、生产构建、单元测试均需通过（杜绝 archive-hu 式类型错误阻塞集成）。

## 14. 验收口径

- `/admin/warehouse`、`/admin/inventory`、`/admin/borrow-approval` 无「待实现」占位。
- 页面布局、视觉层级、文案、交互结构、状态标签还原对应 HTML 原型；字段补充不改变原型组织方式。
- 库房：库房列表 + 占用率/告警；架位看板四态；空闲架位新增盒、占用架位移动盒/查看盒内档案；已占用架位停用被拦截；添加库房按规格生成架位；最近占用盒位表。
- 盘点：范围「库房 AND 分类」；任务三态过滤；新建生成明细；逐件核对（正常/缺失/错位/损坏/借出中）；提交结果统计汇总；草稿无明细禁用开始。
- 借阅：状态 tab 过滤；命中盘点禁用通过；拒绝必填原因；导出凭证状态流转；出库（凭证号+应还时间）→借出；归还（正常/异常，异常必填说明）→恢复；仅对应状态可执行对应操作。
- 跨页联动：盘点命中→借阅审批拦截；库房→盘点库房下拉；库房→借阅架位编码口径一致。
- 三页覆盖加载中、空数据、接口错误三态；敏感操作二次确认。
- API 函数全部支持 mock/真实切换；库房 mock 字段对齐后端 DTO，盘点/借阅 mock 字段对齐 DB 设计；mock 联动一致。
- TypeScript 检查、生产构建、单元测试通过。
