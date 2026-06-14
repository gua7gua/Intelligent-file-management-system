# feat/storage-guo 实施计划：库房与架位 · 档案盘点 · 借阅审批/出库/归还

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重写 `warehouse`/`inventory`/`borrow-approval` 三个占位页面为完整可运行页面，覆盖库房架位/档案盒管理、档案盘点、借阅审批/出库/归还闭环，全部走 mock（库房字段对齐已就绪后端 DTO）。

**Architecture:** per-subdomain 文件组织（warehouse/inventory/borrow-approval 各自 types+api+mock），`VITE_USE_MOCK` 控制 mock/真实切换，warehouse 拆 5 个子组件、inventory/borrow-approval 内联，TDD（先 spec→失败→实现→通过→提交）。

**Tech Stack:** Vue 3.5 `<script setup>` + TypeScript + Element Plus（仅按钮/Dialog/Message/MessageBox/日期选择）+ 原生 HTML 表格/徽章（还原原型）+ vitest + @vue/test-utils。

**先读设计文档**：[frontend/docs/superpowers/specs/2026-06-15-storage-guo-design.md](../specs/2026-06-15-storage-guo-design.md)（字段/API/页面设计/验收口径的权威来源）。

**字段权威来源**：库房对齐后端 `backend/src/main/java/com/archive/dto/{request,response}/*Warehouse*|*Location*|*ArchiveBox*`；盘点/借阅对齐 `doc/接口文档.md` 第 18/12 章 + `doc/数据库设计.md` 10.1/10.2/9.1。

**命令约定**：类型检查 `npx vue-tsc -b`；单文件测试 `npx vitest run <path>`；全量测试 `npm run test:unit -- run`；构建 `npm run build`。

---

## 文件结构

| 文件 | 责任 | 动作 |
|------|------|------|
| `src/types/enums.ts` | 库房/盘点/借阅归还枚举与中文映射 | Modify（末尾追加） |
| `src/types/warehouse.ts` | 库房/架位/档案盒类型 | Create |
| `src/types/inventory.ts` | 盘点任务/明细/统计类型 | Create |
| `src/types/borrow-approval.ts` | 借阅审批查询参数/扩展详情/请求体 | Create |
| `src/mock/modules/warehouse.ts` | 库房/架位/档案盒 mock | Create |
| `src/mock/modules/inventory.ts` | 盘点任务/明细 mock | Create |
| `src/mock/modules/borrow-approval.ts` | 借阅申请（多状态）mock | Create |
| `src/api/warehouse.ts` | §16 九端点 | Create |
| `src/api/inventory.ts` | §18 六端点 | Create |
| `src/api/borrow-approval.ts` | §12 五端点 + 凭证导出 | Create |
| `src/api/warehouse.spec.ts` / `inventory.spec.ts` / `borrow-approval.spec.ts` | mock 分支契约测试 | Create |
| `src/utils/borrowApprovalValidation.ts` (+spec) | 借阅审批/出库/归还校验 | Create |
| `src/views/admin/warehouse/index.vue` | 库房页（组装） | Rewrite |
| `src/views/admin/warehouse/components/RoomList.vue` | 库房列表 | Create |
| `src/views/admin/warehouse/components/RackBoard.vue` | 架位看板 | Create |
| `src/views/admin/warehouse/components/BoxDetailDrawer.vue` | 盒位/盒详情抽屉 | Create |
| `src/views/admin/warehouse/components/CreateRoomDialog.vue` | 添加库房弹窗 | Create |
| `src/views/admin/warehouse/components/RecentBoxesTable.vue` | 最近占用盒位表 | Create |
| `src/views/admin/warehouse/index.spec.ts` | 库房页组件测试 | Create |
| `src/views/admin/inventory/index.vue` | 盘点页 | Rewrite |
| `src/views/admin/inventory/index.spec.ts` | 盘点页组件测试 | Create |
| `src/views/admin/borrow-approval/index.vue` | 借阅审批页 | Rewrite |
| `src/views/admin/borrow-approval/index.spec.ts` | 借阅审批页组件测试 | Create |

路由 `src/router/routes/admin.ts` 已注册三页，**不改路由**。

---

## Task 1: 扩展枚举（`src/types/enums.ts`）

**Files:**
- Modify: `src/types/enums.ts`（在文件末尾 `ApprovalTypeLabel` 之后追加）

- [ ] **Step 1: 在 `ApprovalTypeLabel` 定义之后追加以下枚举与中文映射**

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

/** 档案盒状态（对齐数据库设计 7.3） */
export const BoxStatus = {
  NORMAL: 'normal',
  FULL: 'full',
  MOVED: 'moved',
  DESTROYED: 'destroyed',
} as const

/** 盘点任务状态（对齐数据库设计 10.1） */
export const InventoryTaskStatus = {
  DRAFT: 'draft',
  RUNNING: 'running',
  COMPLETED: 'completed',
} as const

/** 盘点结果（对齐数据库设计 10.2 check_result） */
export const InventoryCheckResult = {
  NORMAL: 'normal',
  MISSING: 'missing',
  MISPLACED: 'misplaced',
  DAMAGED: 'damaged',
  ON_LOAN: 'on_loan',
} as const

/** 归还检查结果（对齐数据库设计 9.1 return_check_result） */
export const ReturnCheckResult = {
  NORMAL: 'normal',
  DAMAGED: 'damaged',
  MISSING_PAGE: 'missing_page',
  OTHER: 'other',
} as const

// 派生值类型
export type WarehouseRoomStatusValue = (typeof WarehouseRoomStatus)[keyof typeof WarehouseRoomStatus]
export type LocationStatusValue = (typeof LocationStatus)[keyof typeof LocationStatus]
export type BoxStatusValue = (typeof BoxStatus)[keyof typeof BoxStatus]
export type InventoryTaskStatusValue = (typeof InventoryTaskStatus)[keyof typeof InventoryTaskStatus]
export type InventoryCheckResultValue = (typeof InventoryCheckResult)[keyof typeof InventoryCheckResult]
export type ReturnCheckResultValue = (typeof ReturnCheckResult)[keyof typeof ReturnCheckResult]

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

- [ ] **Step 2: 类型检查通过**

Run: `npx vue-tsc -b`
Expected: 无错误（新增枚举与既有派生类型模式一致）。

- [ ] **Step 3: 提交**

```bash
git add src/types/enums.ts
git commit -m "feat(storage-guo): 扩展库房盘点借阅归还枚举与中文映射"
```

---

## Task 2: 库房类型（`src/types/warehouse.ts`）

**Files:**
- Create: `src/types/warehouse.ts`

- [ ] **Step 1: 创建 `src/types/warehouse.ts`，内容如下**

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

- [ ] **Step 2: 类型检查通过**

Run: `npx vue-tsc -b`
Expected: 无错误。

- [ ] **Step 3: 提交**

```bash
git add src/types/warehouse.ts
git commit -m "feat(storage-guo): 新增库房架位档案盒类型定义"
```

---

## Task 3: 盘点类型（`src/types/inventory.ts`）

**Files:**
- Create: `src/types/inventory.ts`

- [ ] **Step 1: 创建 `src/types/inventory.ts`，内容如下**

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

- [ ] **Step 2: 类型检查通过**

Run: `npx vue-tsc -b`
Expected: 无错误。

- [ ] **Step 3: 提交**

```bash
git add src/types/inventory.ts
git commit -m "feat(storage-guo): 新增盘点任务与明细类型定义"
```

---

## Task 4: 借阅审批类型（`src/types/borrow-approval.ts`）

**Files:**
- Create: `src/types/borrow-approval.ts`

- [ ] **Step 1: 创建 `src/types/borrow-approval.ts`，内容如下（复用 `types/internal.ts` 的 `BorrowRequestDetail`）**

```typescript
import type { PageData, PageParams } from './api'
import type { BorrowRequestDetail, BorrowStatusValue } from './internal'
import type { ReturnCheckResultValue } from './enums'

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

- [ ] **Step 2: 类型检查通过**

Run: `npx vue-tsc -b`
Expected: 无错误。

- [ ] **Step 3: 提交**

```bash
git add src/types/borrow-approval.ts
git commit -m "feat(storage-guo): 新增借阅审批查询与扩展详情类型定义"
```

## Task 5: 库房 mock + api + 契约测试（§16）

**Files:**
- Create: `src/mock/modules/warehouse.ts`
- Create: `src/api/warehouse.ts`
- Test: `src/api/warehouse.spec.ts`

- [ ] **Step 1: 写失败测试 `src/api/warehouse.spec.ts`**

```typescript
import { describe, expect, it } from 'vitest'
import {
  createArchiveBox, createWarehouseRoom, getArchiveBoxes, getArchiveBoxDetail,
  getStorageLocations, getWarehouseRooms, moveArchiveBox, updateLocationStatus,
  updateWarehouseRoom,
} from './warehouse'

describe('warehouse api mock mode', () => {
  it('lists rooms and flags warning room', async () => {
    const rooms = await getWarehouseRooms()
    expect(rooms.length).toBeGreaterThanOrEqual(3)
    const warned = rooms.find((r) => r.warning)
    expect(warned).toBeTruthy()
    expect(warned?.occupancyRate).toBeGreaterThanOrEqual(warned!.warningThreshold)
  })

  it('filters locations by room and occupied', async () => {
    const all = await getStorageLocations({ roomId: 1, pageSize: 200 })
    expect(all.records.every((l) => l.roomId === 1)).toBe(true)
    const occupied = await getStorageLocations({ roomId: 1, occupied: true, pageSize: 200 })
    expect(occupied.records.every((l) => l.occupied)).toBe(true)
  })

  it('box detail includes items', async () => {
    const detail = await getArchiveBoxDetail(101)
    expect(detail.items.length).toBeGreaterThan(0)
    expect(detail.items[0].archiveNo).toBeTruthy()
  })

  it('create box occupies a free location', async () => {
    const free = (await getStorageLocations({ roomId: 3, occupied: false, pageSize: 200 })).records[0]
    const box = await createArchiveBox({
      locationId: free.id, categoryId: 1, fondsId: 1, yearLabel: '2026', spineText: '测试盒', capacity: 30,
    })
    expect(box.locationId).toBe(free.id)
    const after = await getStorageLocations({ roomId: 3, occupied: true, pageSize: 200 })
    expect(after.records.some((l) => l.id === free.id)).toBe(true)
  })

  it('rejects disabling an occupied location', async () => {
    const occupied = (await getStorageLocations({ occupied: true, pageSize: 200 })).records[0]
    await expect(updateLocationStatus(occupied.id, { status: 'disabled', reason: '维修' })).rejects.toThrow()
  })

  it('move box frees source and occupies target', async () => {
    const box = await getArchiveBoxDetail(101)
    const target = (await getStorageLocations({ roomId: 3, occupied: false, pageSize: 200 })).records[0]
    const moved = await moveArchiveBox(box.id, { targetLocationId: target.id, reason: '整理' })
    expect(moved.locationId).toBe(target.id)
  })

  it('create room generates locations by spec', async () => {
    const room = await createWarehouseRoom({
      roomNo: '404', roomName: '测试库房', rackCount: 2, layersPerRack: 2, boxesPerLayer: 2, warningThreshold: 0.85,
    })
    expect(room.capacity).toBe(8)
    const locs = await getStorageLocations({ roomId: room.id, pageSize: 200 })
    expect(locs.total).toBe(8)
  })
})
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `npx vitest run src/api/warehouse.spec.ts`
Expected: FAIL（`./warehouse` 模块不存在）。

- [ ] **Step 3: 写 `src/mock/modules/warehouse.ts`**

```typescript
import type { PageData } from '@/types/api'
import type {
  ArchiveBox, ArchiveBoxCreateData, ArchiveBoxDetail, ArchiveBoxMoveData, ArchiveBoxParams,
  BoxItem, LocationStatusData, StorageLocation, StorageLocationParams,
  WarehouseRoom, WarehouseRoomCreateData, WarehouseRoomParams, WarehouseRoomUpdateData,
} from '@/types/warehouse'

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

/** 库房（对齐 WarehouseRoomResponse） */
const rooms: WarehouseRoom[] = [
  { id: 1, roomNo: '401', roomName: '综合档案库房', rackCount: 8, layersPerRack: 5, boxesPerLayer: 8, capacity: 320, warningThreshold: 0.85, status: 'active', occupiedSlots: 205, occupancyRate: 0.64, warning: false },
  { id: 2, roomNo: '402', roomName: '会计档案库房', rackCount: 6, layersPerRack: 4, boxesPerLayer: 8, capacity: 192, warningThreshold: 0.85, status: 'active', occupiedSlots: 169, occupancyRate: 0.88, warning: true },
  { id: 3, roomNo: '403', roomName: '科技档案库房', rackCount: 4, layersPerRack: 4, boxesPerLayer: 8, capacity: 128, warningThreshold: 0.85, status: 'active', occupiedSlots: 64, occupancyRate: 0.5, warning: false },
]

interface RoomSpec { roomId: number; roomNo: string; racks: number; layers: number; boxes: number }
const specs: RoomSpec[] = [
  { roomId: 1, roomNo: '401', racks: 2, layers: 5, boxes: 8 },
  { roomId: 2, roomNo: '402', racks: 1, layers: 4, boxes: 8 },
  { roomId: 3, roomNo: '403', racks: 1, layers: 4, boxes: 8 },
]

const disabledCodes = new Set(['401-01-01-08', '401-02-03-04', '402-01-02-06'])

function buildLocations(): StorageLocation[] {
  const out: StorageLocation[] = []
  let id = 1000
  for (const s of specs) {
    for (let r = 1; r <= s.racks; r++) {
      for (let l = 1; l <= s.layers; l++) {
        for (let b = 1; b <= s.boxes; b++) {
          const code = `${s.roomNo}-${pad(r)}-${pad(l)}-${pad(b)}`
          out.push({
            id: id++, roomId: s.roomId, rackNo: r, layerNo: l, boxSlotNo: b, locationCode: code,
            status: disabledCodes.has(code) ? 'disabled' : 'active', occupied: false,
          })
        }
      }
    }
  }
  return out
}

const locations: StorageLocation[] = buildLocations()
const locByCode = new Map(locations.map((l) => [l.locationCode, l]))

function mkItems(prefix: string, count: number): BoxItem[] {
  const items: BoxItem[] = []
  for (let i = 1; i <= count; i++) {
    items.push({
      archiveId: Number(`${prefix}${i}`), archiveNo: `KJ-2025-${pad(Number(prefix))}-${pad(i)}`,
      title: `${prefix} 第 ${i} 件档案`, sortNo: i, pageCount: 10 + i, physicalStatus: 'normal',
    })
  }
  return items
}

const boxes: ArchiveBoxDetail[] = [
  { id: 101, boxNo: 'BX-2026-001', locationId: 0, locationCode: '401-01-01-01', roomNo: '401', categoryId: 1, fondsId: 1, yearLabel: '2026', spineText: '克拉玛依工业园扩建批复', capacity: 30, usedCount: 12, status: 'normal', items: mkItems('18', 12) },
  { id: 102, boxNo: 'BX-2026-002', locationId: 0, locationCode: '401-01-01-02', roomNo: '401', categoryId: 1, fondsId: 1, yearLabel: '2026', spineText: '油田道路施工图', capacity: 30, usedCount: 8, status: 'normal', items: mkItems('19', 8) },
  { id: 103, boxNo: 'BX-2026-003', locationId: 0, locationCode: '401-01-01-04', roomNo: '401', categoryId: 1, fondsId: 1, yearLabel: '2026', spineText: '年度项目审批材料', capacity: 30, usedCount: 28, status: 'full', items: mkItems('20', 28) },
  { id: 104, boxNo: 'BX-2024-016', locationId: 0, locationCode: '401-02-01-01', roomNo: '401', categoryId: 2, fondsId: 2, yearLabel: '2024', spineText: '干部任免材料', capacity: 30, usedCount: 6, status: 'normal', items: mkItems('21', 6) },
  { id: 105, boxNo: 'BX-2026-007', locationId: 0, locationCode: '401-02-01-03', roomNo: '401', categoryId: 1, fondsId: 1, yearLabel: '2026', spineText: '工程竣工验收材料', capacity: 30, usedCount: 10, status: 'normal', items: mkItems('22', 10) },
  { id: 106, boxNo: 'BX-2024-032', locationId: 0, locationCode: '402-01-01-01', roomNo: '402', categoryId: 3, fondsId: 3, yearLabel: '2024', spineText: '会计凭证汇总', capacity: 30, usedCount: 30, status: 'full', items: mkItems('23', 30) },
  { id: 107, boxNo: 'BX-2025-019', locationId: 0, locationCode: '403-01-01-01', roomNo: '403', categoryId: 1, fondsId: 1, yearLabel: '2025', spineText: '管线改造竣工图', capacity: 30, usedCount: 6, status: 'normal', items: mkItems('24', 6) },
]

// 回填盒的 locationId 与架位占用
for (const bx of boxes) {
  const loc = locByCode.get(bx.locationCode)
  if (loc) {
    bx.locationId = loc.id
    loc.occupied = true
    loc.currentBoxId = bx.id
    loc.currentBoxNo = bx.boxNo
    loc.boxItemCount = bx.items.length
  }
}

function recomputeRoom(room: WarehouseRoom): void {
  const used = locations.filter((l) => l.roomId === room.id && l.occupied).length
  room.occupiedSlots = used
  room.occupancyRate = room.capacity > 0 ? used / room.capacity : 0
  room.warning = room.occupancyRate >= room.warningThreshold
}

export function mockWarehouseRooms(params?: WarehouseRoomParams): WarehouseRoom[] {
  let list = rooms.slice()
  if (params?.status) list = list.filter((r) => r.status === params.status)
  if (params?.keyword) {
    const kw = params.keyword
    list = list.filter((r) => r.roomNo.includes(kw) || r.roomName.includes(kw))
  }
  return list
}

let nextRoomId = 4
export function mockCreateWarehouseRoom(data: WarehouseRoomCreateData): WarehouseRoom {
  if (rooms.some((r) => r.roomNo === data.roomNo)) throw new Error('库房号已存在')
  const capacity = data.rackCount * data.layersPerRack * data.boxesPerLayer
  if (capacity <= 0) throw new Error('容量参数必须大于 0')
  const room: WarehouseRoom = {
    id: nextRoomId++, roomNo: data.roomNo, roomName: data.roomName, rackCount: data.rackCount,
    layersPerRack: data.layersPerRack, boxesPerLayer: data.boxesPerLayer, capacity,
    warningThreshold: data.warningThreshold, status: 'active', occupiedSlots: 0, occupancyRate: 0, warning: false,
  }
  rooms.push(room)
  let id = 9000 + locations.length
  for (let r = 1; r <= data.rackCount; r++) {
    for (let l = 1; l <= data.layersPerRack; l++) {
      for (let b = 1; b <= data.boxesPerLayer; b++) {
        const code = `${data.roomNo}-${pad(r)}-${pad(l)}-${pad(b)}`
        const loc: StorageLocation = { id: id++, roomId: room.id, rackNo: r, layerNo: l, boxSlotNo: b, locationCode: code, status: 'active', occupied: false }
        locations.push(loc)
        locByCode.set(code, loc)
      }
    }
  }
  return room
}

export function mockUpdateWarehouseRoom(id: number, data: WarehouseRoomUpdateData): WarehouseRoom {
  const room = rooms.find((r) => r.id === id)
  if (!room) throw new Error('库房不存在')
  if (data.roomName !== undefined) room.roomName = data.roomName
  if (data.warningThreshold !== undefined) {
    room.warningThreshold = data.warningThreshold
    recomputeRoom(room)
  }
  if (data.status !== undefined) room.status = data.status
  return room
}

export function mockStorageLocations(params?: StorageLocationParams): PageData<StorageLocation> {
  let list = locations.slice()
  if (params?.roomId) list = list.filter((l) => l.roomId === params.roomId)
  if (params?.rackNo) list = list.filter((l) => l.rackNo === params.rackNo)
  if (params?.status) list = list.filter((l) => l.status === params.status)
  if (params?.occupied !== undefined) list = list.filter((l) => l.occupied === params.occupied)
  const pageNo = params?.pageNo ?? 1
  const pageSize = params?.pageSize ?? 50
  const start = (pageNo - 1) * pageSize
  return {
    records: list.slice(start, start + pageSize), pageNo, pageSize, total: list.length,
    hasNext: start + pageSize < list.length,
  }
}

export function mockUpdateLocationStatus(id: number, data: LocationStatusData): StorageLocation {
  const loc = locations.find((l) => l.id === id)
  if (!loc) throw new Error('架位不存在')
  if (data.status === 'disabled' && loc.occupied) throw new Error('已占用架位不得直接停用')
  loc.status = data.status
  return loc
}

export function mockArchiveBoxes(params?: ArchiveBoxParams): PageData<ArchiveBox> {
  let list = boxes.slice()
  if (params?.boxNo) list = list.filter((b) => b.boxNo.includes(params.boxNo!))
  if (params?.roomId) {
    const room = rooms.find((r) => r.id === params.roomId)
    if (room) list = list.filter((b) => b.roomNo === room.roomNo)
  }
  if (params?.categoryId) list = list.filter((b) => b.categoryId === params.categoryId)
  if (params?.fondsId) list = list.filter((b) => b.fondsId === params.fondsId)
  if (params?.status) list = list.filter((b) => b.status === params.status)
  const pageNo = params?.pageNo ?? 1
  const pageSize = params?.pageSize ?? 20
  const start = (pageNo - 1) * pageSize
  const records = list.slice(start, start + pageSize).map(({ items, ...rest }) => rest)
  return { records, pageNo, pageSize, total: list.length, hasNext: start + pageSize < list.length }
}

export function mockArchiveBoxDetail(id: number): ArchiveBoxDetail {
  const found = boxes.find((b) => b.id === id)
  if (!found) throw new Error('档案盒不存在')
  return JSON.parse(JSON.stringify(found))
}

let nextBoxId = 200
export function mockCreateArchiveBox(data: ArchiveBoxCreateData): ArchiveBox {
  const loc = locations.find((l) => l.id === data.locationId)
  if (!loc) throw new Error('架位不存在')
  if (loc.status !== 'active') throw new Error('架位已停用')
  if (loc.occupied) throw new Error('架位已被占用')
  const room = rooms.find((r) => r.id === loc.roomId)
  const id = nextBoxId++
  const boxNo = `BX-${data.yearLabel}-${String(id).padStart(3, '0')}`
  const box: ArchiveBoxDetail = {
    id, boxNo, locationId: loc.id, locationCode: loc.locationCode, roomNo: room?.roomNo ?? '',
    categoryId: data.categoryId, fondsId: data.fondsId, yearLabel: data.yearLabel, spineText: data.spineText,
    capacity: data.capacity, usedCount: 0, status: 'normal', items: [],
  }
  boxes.push(box)
  loc.occupied = true
  loc.currentBoxId = box.id
  loc.currentBoxNo = box.boxNo
  loc.boxItemCount = 0
  if (room) recomputeRoom(room)
  const { items, ...rest } = box
  return rest
}

export function mockMoveArchiveBox(id: number, data: ArchiveBoxMoveData): ArchiveBox {
  const box = boxes.find((b) => b.id === id)
  if (!box) throw new Error('档案盒不存在')
  const target = locations.find((l) => l.id === data.targetLocationId)
  if (!target) throw new Error('目标架位不存在')
  if (target.status !== 'active') throw new Error('目标架位已停用')
  if (target.occupied) throw new Error('目标架位已被占用')
  const source = locations.find((l) => l.id === box.locationId)
  const sourceRoom = source ? rooms.find((r) => r.id === source.roomId) : undefined
  if (source) {
    source.occupied = false
    source.currentBoxId = undefined
    source.currentBoxNo = undefined
    source.boxItemCount = undefined
  }
  box.locationId = target.id
  box.locationCode = target.locationCode
  box.roomNo = target.locationCode.split('-')[0]
  target.occupied = true
  target.currentBoxId = box.id
  target.currentBoxNo = box.boxNo
  target.boxItemCount = box.items.length
  if (sourceRoom) recomputeRoom(sourceRoom)
  const targetRoom = rooms.find((r) => r.id === target.roomId)
  if (targetRoom && targetRoom !== sourceRoom) recomputeRoom(targetRoom)
  const { items, ...rest } = box
  return rest
}
```

- [ ] **Step 4: 写 `src/api/warehouse.ts`**

```typescript
import request from './request'
import type { PageData } from '@/types/api'
import type {
  ArchiveBox, ArchiveBoxCreateData, ArchiveBoxDetail, ArchiveBoxMoveData, ArchiveBoxParams,
  LocationStatusData, StorageLocation, StorageLocationParams,
  WarehouseRoom, WarehouseRoomCreateData, WarehouseRoomParams, WarehouseRoomUpdateData,
} from '@/types/warehouse'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询库房列表（§16.1） */
export function getWarehouseRooms(params?: WarehouseRoomParams): Promise<WarehouseRoom[]> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockWarehouseRooms(params))
  }
  return request.get('/admin/warehouse/rooms', { params })
}

/** 新增库房（§16.2） */
export function createWarehouseRoom(data: WarehouseRoomCreateData): Promise<WarehouseRoom> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockCreateWarehouseRoom(data))
  }
  return request.post('/admin/warehouse/rooms', data)
}

/** 更新库房（§16.3） */
export function updateWarehouseRoom(roomId: number, data: WarehouseRoomUpdateData): Promise<WarehouseRoom> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockUpdateWarehouseRoom(roomId, data))
  }
  return request.put(`/admin/warehouse/rooms/${roomId}`, data)
}

/** 查询架位（§16.4） */
export function getStorageLocations(params?: StorageLocationParams): Promise<PageData<StorageLocation>> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockStorageLocations(params))
  }
  return request.get('/admin/warehouse/locations', { params })
}

/** 停用/启用架位（§16.5） */
export function updateLocationStatus(locationId: number, data: LocationStatusData): Promise<StorageLocation> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockUpdateLocationStatus(locationId, data))
  }
  return request.put(`/admin/warehouse/locations/${locationId}/status`, data)
}

/** 查询档案盒（§16.6） */
export function getArchiveBoxes(params?: ArchiveBoxParams): Promise<PageData<ArchiveBox>> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockArchiveBoxes(params))
  }
  return request.get('/admin/warehouse/boxes', { params })
}

/** 档案盒详情（§16.7） */
export function getArchiveBoxDetail(boxId: number): Promise<ArchiveBoxDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockArchiveBoxDetail(boxId))
  }
  return request.get(`/admin/warehouse/boxes/${boxId}`)
}

/** 新增档案盒（§16.8） */
export function createArchiveBox(data: ArchiveBoxCreateData): Promise<ArchiveBox> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockCreateArchiveBox(data))
  }
  return request.post('/admin/warehouse/boxes', data)
}

/** 移动档案盒（§16.9） */
export function moveArchiveBox(boxId: number, data: ArchiveBoxMoveData): Promise<ArchiveBox> {
  if (USE_MOCK) {
    return import('@/mock/modules/warehouse').then((m) => m.mockMoveArchiveBox(boxId, data))
  }
  return request.post(`/admin/warehouse/boxes/${boxId}/move`, data)
}
```

- [ ] **Step 5: 运行测试，确认通过**

Run: `npx vitest run src/api/warehouse.spec.ts`
Expected: PASS（7 个用例全过）。

- [ ] **Step 6: 类型检查通过**

Run: `npx vue-tsc -b`
Expected: 无错误。

- [ ] **Step 7: 提交**

```bash
git add src/mock/modules/warehouse.ts src/api/warehouse.ts src/api/warehouse.spec.ts
git commit -m "feat(storage-guo): 实现库房管理 API 封装与 mock 数据"
```

## Task 6: 盘点 mock + api + 契约测试（§18）

**Files:**
- Create: `src/mock/modules/inventory.ts`
- Create: `src/api/inventory.ts`
- Test: `src/api/inventory.spec.ts`

- [ ] **Step 1: 写失败测试 `src/api/inventory.spec.ts`**

```typescript
import { describe, expect, it } from 'vitest'
import {
  completeInventoryTask, createInventoryTask, getInventoryTaskDetail,
  getInventoryTasks, startInventoryTask, updateInventoryItem,
} from './inventory'

describe('inventory api mock mode', () => {
  it('lists tasks across statuses', async () => {
    const all = await getInventoryTasks({ pageSize: 50 })
    const statuses = all.records.map((t) => t.status)
    expect(statuses).toContain('running')
    expect(statuses).toContain('completed')
  })

  it('task detail includes items and stats', async () => {
    const detail = await getInventoryTaskDetail(1)
    expect(detail.items.length).toBeGreaterThan(0)
    expect(detail.stats.total).toBe(detail.items.length)
    expect(detail.stats.checked).toBe(detail.items.filter((i) => i.checkResult !== '').length)
  })

  it('create task generates expected items', async () => {
    const detail = await createInventoryTask({ taskName: '测试盘点', roomId: 1, categoryId: 1 })
    expect(detail.status).toBe('draft')
    expect(detail.items.length).toBeGreaterThan(0)
    expect(detail.items.every((i) => i.checkResult === '')).toBe(true)
  })

  it('start moves draft to running', async () => {
    const created = await createInventoryTask({ taskName: 't', roomId: 2, categoryId: 3 })
    const started = await startInventoryTask(created.id)
    expect(started.status).toBe('running')
    expect(started.startedAt).toBeTruthy()
  })

  it('update item requires running task', async () => {
    const detail = await getInventoryTaskDetail(1)
    const item = detail.items[0]
    const updated = await updateInventoryItem(detail.id, item.id, {
      actualLocationCode: item.expectedLocationCode, checkResult: 'normal', note: '一致',
    })
    expect(updated.checkResult).toBe('normal')
  })

  it('complete moves running to completed with summary', async () => {
    const detail = await completeInventoryTask(1, { summary: '' })
    expect(detail.status).toBe('completed')
    expect(detail.summary).toContain('应盘')
  })
})
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `npx vitest run src/api/inventory.spec.ts`
Expected: FAIL（`./inventory` 模块不存在）。

- [ ] **Step 3: 写 `src/mock/modules/inventory.ts`**

```typescript
import type { PageData } from '@/types/api'
import type {
  InventoryCompleteData, InventoryItem, InventoryItemUpdateData, InventoryTask,
  InventoryTaskCreateData, InventoryTaskDetail, InventoryTaskParams, InventoryTaskStats,
} from '@/types/inventory'

const roomMap: Record<number, string> = { 1: '401', 2: '402', 3: '403' }
const categoryMap: Record<number, string> = { 1: '科技档案', 2: '文书档案', 3: '会计档案', 4: '音像档案', 5: '人事档案' }

function mkItem(over: Partial<InventoryItem>): InventoryItem {
  return { id: 0, taskId: 0, archiveId: 0, archiveNo: '', title: '', expectedLocationCode: '', checkResult: '', loanStatus: 'available', ...over }
}

function statsFromItems(items: InventoryItem[]): InventoryTaskStats {
  return {
    total: items.length,
    checked: items.filter((i) => i.checkResult !== '').length,
    normalCount: items.filter((i) => i.checkResult === 'normal').length,
    missingCount: items.filter((i) => i.checkResult === 'missing').length,
    misplacedCount: items.filter((i) => i.checkResult === 'misplaced').length,
    damagedCount: items.filter((i) => i.checkResult === 'damaged').length,
    onLoanCount: items.filter((i) => i.checkResult === 'on_loan').length,
  }
}

function buildSummary(s: InventoryTaskStats): string {
  return `应盘 ${s.total} 件，正常 ${s.normalCount} 件，缺失 ${s.missingCount} 件，错位 ${s.misplacedCount} 件，损坏 ${s.damagedCount} 件，借出 ${s.onLoanCount} 件`
}

const task1Items: InventoryItem[] = [
  mkItem({ id: 11, archiveId: 101, archiveNo: 'KJ-2025-00018', title: '采油一厂设备验收报告', expectedLocationCode: '401-03-02-05', actualLocationCode: '401-03-02-05', checkResult: 'normal', note: '实物一致' }),
  mkItem({ id: 12, archiveId: 102, archiveNo: 'KJ-2025-00019', title: '管线改造竣工图', expectedLocationCode: '401-03-03-01', actualLocationCode: '401-03-03-02', checkResult: 'misplaced', note: '同机架相邻盒位' }),
  mkItem({ id: 13, archiveId: 103, archiveNo: 'KJ-2025-00020', title: '年度审计材料', expectedLocationCode: '401-02-04-03', checkResult: 'missing', note: '架位未找到' }),
  mkItem({ id: 14, archiveId: 104, archiveNo: 'KJ-2024-00031', title: '油田道路施工图', expectedLocationCode: '401-01-01-02', checkResult: 'on_loan', note: 'BR-202606-017 已出库', loanStatus: 'on_loan' }),
  mkItem({ id: 15, archiveId: 105, archiveNo: 'KJ-2023-00008', title: '设备维修记录汇编', expectedLocationCode: '401-02-04-04', actualLocationCode: '401-02-04-04', checkResult: 'damaged', note: '盒角受潮' }),
  mkItem({ id: 16, archiveId: 106, archiveNo: 'KJ-2025-00021', title: '市政管网规划', expectedLocationCode: '401-02-05-01' }),
  mkItem({ id: 17, archiveId: 107, archiveNo: 'KJ-2025-00022', title: '环保验收报告', expectedLocationCode: '401-02-05-02' }),
  mkItem({ id: 18, archiveId: 108, archiveNo: 'KJ-2025-00023', title: '供电改造批复', expectedLocationCode: '401-02-05-03' }),
]

const task2Items: InventoryItem[] = [
  mkItem({ id: 21, archiveId: 201, archiveNo: 'KJ-2024-00040', title: '2024 会计凭证 A', expectedLocationCode: '402-01-01-01', actualLocationCode: '402-01-01-01', checkResult: 'normal' }),
  mkItem({ id: 22, archiveId: 202, archiveNo: 'KJ-2024-00041', title: '2024 会计凭证 B', expectedLocationCode: '402-01-01-02' }),
  mkItem({ id: 23, archiveId: 203, archiveNo: 'KJ-2024-00042', title: '2024 会计凭证 C', expectedLocationCode: '402-01-01-03' }),
]

const task3Items: InventoryItem[] = [
  mkItem({ id: 31, archiveId: 301, archiveNo: 'WS-2024-001', title: '党委会议纪要', expectedLocationCode: '401-04-01-01', actualLocationCode: '401-04-01-01', checkResult: 'normal' }),
  mkItem({ id: 32, archiveId: 302, archiveNo: 'WS-2024-002', title: '年度工作总结', expectedLocationCode: '401-04-01-02', actualLocationCode: '401-04-01-02', checkResult: 'normal' }),
]

function buildTask(over: Partial<InventoryTaskDetail> & Pick<InventoryTaskDetail, 'id' | 'taskNo' | 'taskName' | 'roomId' | 'categoryId' | 'status' | 'items'>): InventoryTaskDetail {
  const stats = statsFromItems(over.items)
  const abnormalCount = stats.missingCount + stats.misplacedCount + stats.damagedCount
  const base: InventoryTask = {
    id: over.id, taskNo: over.taskNo, taskName: over.taskName, roomId: over.roomId, roomNo: roomMap[over.roomId] ?? '',
    categoryId: over.categoryId, categoryName: categoryMap[over.categoryId] ?? '', status: over.status,
    total: stats.total, checked: stats.checked, abnormalCount, createdAt: over.createdAt ?? '2026-06-08T09:00:00+08:00',
    startedAt: over.startedAt, completedAt: over.completedAt, summary: over.summary,
  }
  return { ...base, items: over.items, stats }
}

const tasks: InventoryTaskDetail[] = [
  buildTask({ id: 1, taskNo: 'PD-202606-001', taskName: '2026 年度 401 库房科技档案盘点', roomId: 1, categoryId: 1, status: 'running', items: task1Items, startedAt: '2026-06-08T09:30:00+08:00', createdAt: '2026-06-08T09:00:00+08:00' }),
  buildTask({ id: 2, taskNo: 'PD-202606-002', taskName: '2026 年度 402 库房会计档案盘点', roomId: 2, categoryId: 3, status: 'running', items: task2Items, startedAt: '2026-06-10T10:00:00+08:00', createdAt: '2026-06-10T09:30:00+08:00' }),
  buildTask({ id: 3, taskNo: 'PD-202605-003', taskName: '2026 春季 401 库房文书档案复盘', roomId: 1, categoryId: 2, status: 'completed', items: task3Items, startedAt: '2026-05-15T09:00:00+08:00', completedAt: '2026-05-21T16:00:00+08:00', summary: '应盘 2 件，正常 2 件', createdAt: '2026-05-15T08:30:00+08:00' }),
  buildTask({ id: 4, taskNo: 'PD-202606-004', taskName: '2026 年度 403 库房音像档案盘点', roomId: 3, categoryId: 4, status: 'draft', items: [], createdAt: '2026-06-14T14:00:00+08:00' }),
]

export function mockInventoryTasks(params?: InventoryTaskParams): PageData<InventoryTask> {
  let list = tasks.slice()
  if (params?.status) list = list.filter((t) => t.status === params.status)
  if (params?.roomId) list = list.filter((t) => t.roomId === params.roomId)
  if (params?.categoryId) list = list.filter((t) => t.categoryId === params.categoryId)
  const pageNo = params?.pageNo ?? 1
  const pageSize = params?.pageSize ?? 20
  const start = (pageNo - 1) * pageSize
  const records = list.slice(start, start + pageSize).map(({ items, stats, ...rest }) => rest)
  return { records, pageNo, pageSize, total: list.length, hasNext: start + pageSize < list.length }
}

export function mockInventoryTaskDetail(id: number): InventoryTaskDetail {
  const found = tasks.find((t) => t.id === id)
  if (!found) throw new Error('盘点任务不存在')
  return JSON.parse(JSON.stringify(found))
}

let nextTaskId = 5
export function mockCreateInventoryTask(data: InventoryTaskCreateData): InventoryTaskDetail {
  const id = nextTaskId++
  const items: InventoryItem[] = Array.from({ length: 4 }, (_, i) => mkItem({
    id: id * 100 + i, taskId: id, archiveId: 500 + i, archiveNo: `KJ-2026-${String(500 + i).padStart(5, '0')}`,
    title: `范围内档案 ${i + 1}`, expectedLocationCode: `${roomMap[data.roomId] ?? '000'}-01-01-${String(i + 1).padStart(2, '0')}`,
  }))
  const detail = buildTask({
    id, taskNo: `PD-202606-${String(id).padStart(3, '0')}`, taskName: data.taskName,
    roomId: data.roomId, categoryId: data.categoryId, status: 'draft', items, createdAt: '2026-06-15T10:00:00+08:00',
  })
  tasks.push(detail)
  return JSON.parse(JSON.stringify(detail))
}

export function mockStartInventoryTask(id: number): InventoryTaskDetail {
  const task = tasks.find((t) => t.id === id)
  if (!task) throw new Error('盘点任务不存在')
  if (task.items.length === 0) throw new Error('任务无盘点明细，不可开始')
  task.status = 'running'
  task.startedAt = '2026-06-15T10:30:00+08:00'
  return JSON.parse(JSON.stringify(task))
}

export function mockUpdateInventoryItem(taskId: number, itemId: number, data: InventoryItemUpdateData): InventoryItem {
  const task = tasks.find((t) => t.id === taskId)
  if (!task) throw new Error('盘点任务不存在')
  if (task.status !== 'running') throw new Error('任务非进行中，不可更新明细')
  const item = task.items.find((i) => i.id === itemId)
  if (!item) throw new Error('盘点明细不存在')
  item.actualLocationCode = data.actualLocationCode
  item.checkResult = data.checkResult
  item.note = data.note
  task.stats = statsFromItems(task.items)
  task.checked = task.stats.checked
  task.abnormalCount = task.stats.missingCount + task.stats.misplacedCount + task.stats.damagedCount
  return { ...item }
}

export function mockCompleteInventoryTask(id: number, data: InventoryCompleteData): InventoryTaskDetail {
  const task = tasks.find((t) => t.id === id)
  if (!task) throw new Error('盘点任务不存在')
  if (task.status !== 'running') throw new Error('任务非进行中，不可完成')
  task.status = 'completed'
  task.completedAt = '2026-06-15T17:00:00+08:00'
  task.summary = data.summary.trim() || buildSummary(task.stats)
  return JSON.parse(JSON.stringify(task))
}
```

- [ ] **Step 4: 写 `src/api/inventory.ts`**

```typescript
import request from './request'
import type { PageData } from '@/types/api'
import type {
  InventoryCompleteData, InventoryItem, InventoryItemUpdateData, InventoryTask,
  InventoryTaskCreateData, InventoryTaskDetail, InventoryTaskParams,
} from '@/types/inventory'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询盘点任务（§18.1） */
export function getInventoryTasks(params?: InventoryTaskParams): Promise<PageData<InventoryTask>> {
  if (USE_MOCK) {
    return import('@/mock/modules/inventory').then((m) => m.mockInventoryTasks(params))
  }
  return request.get('/admin/inventory-tasks', { params })
}

/** 创建盘点任务（§18.2） */
export function createInventoryTask(data: InventoryTaskCreateData): Promise<InventoryTaskDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/inventory').then((m) => m.mockCreateInventoryTask(data))
  }
  return request.post('/admin/inventory-tasks', data)
}

/** 开始盘点（§18.3） */
export function startInventoryTask(taskId: number): Promise<InventoryTaskDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/inventory').then((m) => m.mockStartInventoryTask(taskId))
  }
  return request.post(`/admin/inventory-tasks/${taskId}/start`)
}

/** 获取盘点详情（§18.4） */
export function getInventoryTaskDetail(taskId: number): Promise<InventoryTaskDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/inventory').then((m) => m.mockInventoryTaskDetail(taskId))
  }
  return request.get(`/admin/inventory-tasks/${taskId}`)
}

/** 更新盘点明细（§18.5） */
export function updateInventoryItem(taskId: number, itemId: number, data: InventoryItemUpdateData): Promise<InventoryItem> {
  if (USE_MOCK) {
    return import('@/mock/modules/inventory').then((m) => m.mockUpdateInventoryItem(taskId, itemId, data))
  }
  return request.put(`/admin/inventory-tasks/${taskId}/items/${itemId}`, data)
}

/** 完成盘点（§18.6） */
export function completeInventoryTask(taskId: number, data: InventoryCompleteData): Promise<InventoryTaskDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/inventory').then((m) => m.mockCompleteInventoryTask(taskId, data))
  }
  return request.post(`/admin/inventory-tasks/${taskId}/complete`, data)
}
```

- [ ] **Step 5: 运行测试，确认通过**

Run: `npx vitest run src/api/inventory.spec.ts`
Expected: PASS（6 个用例全过）。

- [ ] **Step 6: 类型检查通过**

Run: `npx vue-tsc -b`
Expected: 无错误。

- [ ] **Step 7: 提交**

```bash
git add src/mock/modules/inventory.ts src/api/inventory.ts src/api/inventory.spec.ts
git commit -m "feat(storage-guo): 实现盘点任务 API 封装与 mock 数据"
```

## Task 7: 借阅审批 mock + api + 契约测试（§12）

**Files:**
- Create: `src/mock/modules/borrow-approval.ts`
- Create: `src/api/borrow-approval.ts`
- Test: `src/api/borrow-approval.spec.ts`

- [ ] **Step 1: 写失败测试 `src/api/borrow-approval.spec.ts`**

```typescript
import { describe, expect, it } from 'vitest'
import {
  approveBorrowRequest, checkoutBorrowRequest, exportBorrowVoucher,
  getBorrowApprovalDetail, getBorrowApprovals, returnBorrowRequest,
} from './borrow-approval'

describe('borrow-approval api mock mode', () => {
  it('lists requests across statuses with overdue', async () => {
    const all = await getBorrowApprovals({ pageSize: 50 })
    const statuses = all.records.map((r) => r.status)
    expect(statuses).toContain('applied')
    expect(statuses).toContain('checked_out')
    expect(statuses).toContain('returned')

    const overdue = await getBorrowApprovals({ overdue: true, pageSize: 50 })
    expect(overdue.records.every((r) => r.overdue)).toBe(true)
  })

  it('inventory hit blocks approval', async () => {
    await expect(approveBorrowRequest(2, { approved: true, opinion: '同意' })).rejects.toThrow()
  })

  it('reject requires reason', async () => {
    await expect(approveBorrowRequest(1, { approved: false })).rejects.toThrow()
    const rejected = await approveBorrowRequest(1, { approved: false, rejectReason: '材料不全' })
    expect(rejected.status).toBe('rejected')
    expect(rejected.rejectReason).toBe('材料不全')
  })

  it('checkout moves approved to checked_out', async () => {
    const result = await checkoutBorrowRequest(3, { voucherNo: 'VCH-2026-0013', dueAt: '2026-06-22T09:00:00+08:00' })
    expect(result.status).toBe('checked_out')
    expect(result.dueAt).toBe('2026-06-22T09:00:00+08:00')
  })

  it('abnormal return requires note', async () => {
    await expect(returnBorrowRequest(6, { returnCheckResult: 'damaged' })).rejects.toThrow()
    const returned = await returnBorrowRequest(6, { returnCheckResult: 'damaged', returnNote: '边角破损' })
    expect(returned.status).toBe('abnormal_return')
  })

  it('normal return moves to returned', async () => {
    const returned = await returnBorrowRequest(5, { returnCheckResult: 'normal', returnNote: '完好' })
    expect(returned.status).toBe('returned')
  })

  it('export voucher issues first time and reuses', async () => {
    const first = await exportBorrowVoucher(4)
    expect(first.firstIssued).toBe(false)
    expect(first.voucherNo).toBe('VCH-2026-0010')
    const fresh = await exportBorrowVoucher(3)
    expect(fresh.firstIssued).toBe(true)
    expect(fresh.voucherNo).toMatch(/^VCH-2026-/)
  })
})
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `npx vitest run src/api/borrow-approval.spec.ts`
Expected: FAIL（`./borrow-approval` 模块不存在）。

- [ ] **Step 3: 写 `src/mock/modules/borrow-approval.ts`**

```typescript
import type { PageData } from '@/types/api'
import type {
  BorrowApprovalDetail, BorrowApprovalParams, BorrowApproveData,
  BorrowCheckoutData, BorrowReturnData, BorrowVoucherResult,
} from '@/types/borrow-approval'

function mkReq(over: Partial<BorrowApprovalDetail>): BorrowApprovalDetail {
  return {
    id: 0, requestNo: '', archiveId: 0, archiveNo: '', archiveTitle: '', status: 'applied',
    expectedDays: 7, appliedAt: '2026-06-12T09:00:00+08:00', reason: '', contactPhone: '',
    borrowerName: '', archiveLocationCode: '', carrierStatus: 'paper_electronic',
    checkCarrier: '纸质+电子，可申请纸质借阅', checkLifecycle: '正常', checkLoan: '可借',
    checkInventory: '未命中进行中盘点', inventoryHit: false, ...over,
  }
}

const requests: BorrowApprovalDetail[] = [
  mkReq({ id: 1, requestNo: 'JY-2026-0017', archiveId: 96, archiveNo: 'KJ-2024-0096', archiveTitle: '智慧城市平台建设报告', status: 'applied', expectedDays: 7, expectedVisitAt: '2026-06-16T09:30:00+08:00', appliedAt: '2026-06-12T09:00:00+08:00', reason: '用于本单位智慧城市项目复盘，需核对纸质原件签章。', contactPhone: '13800000001', borrowerName: '小李', borrowerOrg: '技术部', archiveLocationCode: '401-03-02-05' }),
  mkReq({ id: 2, requestNo: 'JY-2026-0018', archiveId: 120, archiveNo: 'KJ-2024-0120', archiveTitle: '401 库房科技档案盘点范围内材料', status: 'applied', expectedDays: 5, expectedVisitAt: '2026-06-16T10:00:00+08:00', appliedAt: '2026-06-12T10:00:00+08:00', reason: '工程复盘查阅。', contactPhone: '13800000002', borrowerName: '小赵', borrowerOrg: '工程部', archiveLocationCode: '401-04-01-02', inventoryHit: true, checkInventory: '命中进行中盘点范围', checkLoan: '盘点暂停借阅' }),
  mkReq({ id: 3, requestNo: 'JY-2026-0013', archiveId: 320, archiveNo: 'KJ-2024-0320', archiveTitle: '工程建设验收材料', status: 'approved', expectedDays: 7, expectedVisitAt: '2026-06-15T09:00:00+08:00', appliedAt: '2026-06-11T09:00:00+08:00', approvedAt: '2026-06-12T14:00:00+08:00', opinion: '同意借阅 7 天', reason: '验收核对。', contactPhone: '13800000003', borrowerName: '王磊', borrowerOrg: '工程部', archiveLocationCode: '402-02-03-07' }),
  mkReq({ id: 4, requestNo: 'JY-2026-0010', archiveId: 55, archiveNo: 'KJ-2024-0055', archiveTitle: '年度规划文本', status: 'voucher_issued', expectedDays: 10, expectedVisitAt: '2026-06-14T09:00:00+08:00', appliedAt: '2026-06-10T09:00:00+08:00', approvedAt: '2026-06-11T10:00:00+08:00', opinion: '同意', voucherNo: 'VCH-2026-0010', voucherIssuedAt: '2026-06-13T09:00:00+08:00', reason: '规划研讨。', contactPhone: '13800000004', borrowerName: '周敏', borrowerOrg: '规划部', archiveLocationCode: '401-02-02-05' }),
  mkReq({ id: 5, requestNo: 'JY-2026-0009', archiveId: 8, archiveNo: 'KJ-2023-0008', archiveTitle: '设备维修记录汇编', status: 'checked_out', expectedDays: 7, expectedVisitAt: '2026-06-05T09:00:00+08:00', appliedAt: '2026-06-04T09:00:00+08:00', approvedAt: '2026-06-04T15:00:00+08:00', voucherNo: 'VCH-2026-0009', voucherIssuedAt: '2026-06-05T08:30:00+08:00', checkedOutAt: '2026-06-05T09:30:00+08:00', dueAt: '2026-06-12T09:30:00+08:00', overdue: true, reason: '设备维护参考。', contactPhone: '13800000005', borrowerName: '陈欣', borrowerOrg: '设备科', archiveLocationCode: '401-02-04-03' }),
  mkReq({ id: 6, requestNo: 'JY-2026-0007', archiveId: 40, archiveNo: 'KJ-2024-0040', archiveTitle: '2024 会计凭证汇编', status: 'checked_out', expectedDays: 5, appliedAt: '2026-06-10T09:00:00+08:00', approvedAt: '2026-06-10T14:00:00+08:00', voucherNo: 'VCH-2026-0007', checkedOutAt: '2026-06-11T09:00:00+08:00', dueAt: '2026-06-25T09:00:00+08:00', overdue: false, reason: '审计查阅。', contactPhone: '13800000006', borrowerName: '林涛', borrowerOrg: '财务部', archiveLocationCode: '402-01-01-02' }),
  mkReq({ id: 7, requestNo: 'JY-2026-0005', archiveId: 22, archiveNo: 'KJ-2023-0022', archiveTitle: '2023 人事任免卷', status: 'returned', expectedDays: 3, appliedAt: '2026-06-01T09:00:00+08:00', approvedAt: '2026-06-01T14:00:00+08:00', checkedOutAt: '2026-06-02T09:00:00+08:00', dueAt: '2026-06-05T09:00:00+08:00', returnedAt: '2026-06-05T08:50:00+08:00', returnCheckResult: 'normal', returnNote: '实体完好', reason: '人事核对。', contactPhone: '13800000007', borrowerName: '吴芳', borrowerOrg: '人事部', archiveLocationCode: '401-05-01-01' }),
  mkReq({ id: 8, requestNo: 'JY-2026-0003', archiveId: 15, archiveNo: 'KJ-2023-0015', archiveTitle: '老旧基建图纸', status: 'abnormal_return', expectedDays: 5, appliedAt: '2026-05-28T09:00:00+08:00', approvedAt: '2026-05-28T14:00:00+08:00', checkedOutAt: '2026-05-29T09:00:00+08:00', dueAt: '2026-06-03T09:00:00+08:00', returnedAt: '2026-06-03T10:00:00+08:00', returnCheckResult: 'damaged', returnNote: '边角破损', reason: '基建复查。', contactPhone: '13800000008', borrowerName: '郑刚', borrowerOrg: '基建部', archiveLocationCode: '401-06-02-03' }),
  mkReq({ id: 9, requestNo: 'JY-2026-0006', archiveId: 60, archiveNo: 'KJ-2024-0060', archiveTitle: '涉密技术报告', status: 'rejected', expectedDays: 7, appliedAt: '2026-06-08T09:00:00+08:00', approvedAt: '2026-06-09T10:00:00+08:00', rejectReason: '密级过高，不予纸质借阅，请走电子查阅。', reason: '技术评估。', contactPhone: '13800000009', borrowerName: '孙宇', borrowerOrg: '技术部', archiveLocationCode: '401-03-01-04', checkLoan: '密级限制' }),
]

export function mockBorrowApprovals(params?: BorrowApprovalParams): PageData<BorrowApprovalDetail> {
  let list = requests.slice()
  if (params?.status) list = list.filter((r) => r.status === params.status)
  if (params?.borrowerKeyword) {
    const kw = params.borrowerKeyword
    list = list.filter((r) => r.borrowerName.includes(kw) || r.requestNo.includes(kw))
  }
  if (params?.archiveKeyword) {
    const kw = params.archiveKeyword
    list = list.filter((r) => r.archiveNo.includes(kw) || r.archiveTitle.includes(kw))
  }
  if (params?.overdue) list = list.filter((r) => r.overdue === true)
  const pageNo = params?.pageNo ?? 1
  const pageSize = params?.pageSize ?? 20
  const start = (pageNo - 1) * pageSize
  return { records: list.slice(start, start + pageSize), pageNo, pageSize, total: list.length, hasNext: start + pageSize < list.length }
}

export function mockBorrowApprovalDetail(id: number): BorrowApprovalDetail {
  const found = requests.find((r) => r.id === id)
  if (!found) throw new Error('借阅申请不存在')
  return JSON.parse(JSON.stringify(found))
}

export function mockApproveBorrowRequest(id: number, data: BorrowApproveData): BorrowApprovalDetail {
  const req = requests.find((r) => r.id === id)
  if (!req) throw new Error('借阅申请不存在')
  if (req.status !== 'applied') throw new Error('当前状态不可审批')
  if (data.approved && req.inventoryHit) throw new Error('目标档案命中进行中盘点，不可审批通过')
  if (data.approved) {
    req.status = 'approved'
    req.approvedAt = '2026-06-15T11:00:00+08:00'
    req.opinion = data.opinion || '同意'
  } else {
    const reason = data.rejectReason?.trim() || data.opinion?.trim()
    if (!reason) throw new Error('审批拒绝必须填写原因')
    req.status = 'rejected'
    req.approvedAt = '2026-06-15T11:00:00+08:00'
    req.rejectReason = reason
  }
  return JSON.parse(JSON.stringify(req))
}

export function mockCheckoutBorrowRequest(id: number, data: BorrowCheckoutData): BorrowApprovalDetail {
  const req = requests.find((r) => r.id === id)
  if (!req) throw new Error('借阅申请不存在')
  if (req.status !== 'approved' && req.status !== 'voucher_issued') throw new Error('仅已批准申请可出库')
  if (!data.voucherNo.trim()) throw new Error('凭证号必填')
  req.status = 'checked_out'
  req.voucherNo = data.voucherNo
  req.checkedOutAt = '2026-06-15T14:00:00+08:00'
  req.dueAt = data.dueAt
  req.overdue = false
  return JSON.parse(JSON.stringify(req))
}

export function mockReturnBorrowRequest(id: number, data: BorrowReturnData): BorrowApprovalDetail {
  const req = requests.find((r) => r.id === id)
  if (!req) throw new Error('借阅申请不存在')
  if (req.status !== 'checked_out') throw new Error('仅已出库申请可归还')
  if (data.returnCheckResult !== 'normal' && !data.returnNote?.trim()) throw new Error('异常归还必须填写检查说明')
  req.returnCheckResult = data.returnCheckResult
  req.returnNote = data.returnNote
  req.returnedAt = '2026-06-15T16:00:00+08:00'
  req.status = data.returnCheckResult === 'normal' ? 'returned' : 'abnormal_return'
  return JSON.parse(JSON.stringify(req))
}

let voucherSeq = 100
export function mockExportBorrowVoucher(id: number): BorrowVoucherResult {
  const req = requests.find((r) => r.id === id)
  if (!req) throw new Error('借阅申请不存在')
  if (req.status !== 'approved' && req.status !== 'voucher_issued') throw new Error('仅审批通过后可导出凭证')
  const firstIssued = !req.voucherNo
  if (firstIssued) {
    req.voucherNo = `VCH-2026-${String(voucherSeq++).padStart(4, '0')}`
    req.voucherIssuedAt = '2026-06-15T12:00:00+08:00'
    req.status = 'voucher_issued'
  }
  return { voucherNo: req.voucherNo!, voucherIssuedAt: req.voucherIssuedAt!, firstIssued }
}
```

- [ ] **Step 4: 写 `src/api/borrow-approval.ts`**

```typescript
import request from './request'
import type { PageData } from '@/types/api'
import type {
  BorrowApprovalDetail, BorrowApprovalParams, BorrowApproveData,
  BorrowCheckoutData, BorrowReturnData, BorrowVoucherResult,
} from '@/types/borrow-approval'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询借阅申请（§12.1） */
export function getBorrowApprovals(params?: BorrowApprovalParams): Promise<PageData<BorrowApprovalDetail>> {
  if (USE_MOCK) {
    return import('@/mock/modules/borrow-approval').then((m) => m.mockBorrowApprovals(params))
  }
  return request.get('/admin/borrow-requests', { params })
}

/** 获取借阅申请详情（§12.2） */
export function getBorrowApprovalDetail(requestId: number): Promise<BorrowApprovalDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/borrow-approval').then((m) => m.mockBorrowApprovalDetail(requestId))
  }
  return request.get(`/admin/borrow-requests/${requestId}`)
}

/** 审批借阅申请（§12.3） */
export function approveBorrowRequest(requestId: number, data: BorrowApproveData): Promise<BorrowApprovalDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/borrow-approval').then((m) => m.mockApproveBorrowRequest(requestId, data))
  }
  return request.post(`/admin/borrow-requests/${requestId}/approve`, data)
}

/** 核验凭证并确认出库（§12.4） */
export function checkoutBorrowRequest(requestId: number, data: BorrowCheckoutData): Promise<BorrowApprovalDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/borrow-approval').then((m) => m.mockCheckoutBorrowRequest(requestId, data))
  }
  return request.post(`/admin/borrow-requests/${requestId}/checkout`, data)
}

/** 确认归还（§12.5） */
export function returnBorrowRequest(requestId: number, data: BorrowReturnData): Promise<BorrowApprovalDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/borrow-approval').then((m) => m.mockReturnBorrowRequest(requestId, data))
  }
  return request.post(`/admin/borrow-requests/${requestId}/return`, data)
}

/** 导出/生成借阅凭证（api 层预留，与 §11.10 对称的管理端点） */
export function exportBorrowVoucher(requestId: number): Promise<BorrowVoucherResult> {
  if (USE_MOCK) {
    return import('@/mock/modules/borrow-approval').then((m) => m.mockExportBorrowVoucher(requestId))
  }
  return request.post(`/admin/borrow-requests/${requestId}/voucher`)
}
```

- [ ] **Step 5: 运行测试，确认通过**

Run: `npx vitest run src/api/borrow-approval.spec.ts`
Expected: PASS（7 个用例全过）。

- [ ] **Step 6: 类型检查通过**

Run: `npx vue-tsc -b`
Expected: 无错误。

- [ ] **Step 7: 提交**

```bash
git add src/mock/modules/borrow-approval.ts src/api/borrow-approval.ts src/api/borrow-approval.spec.ts
git commit -m "feat(storage-guo): 实现借阅审批出库归还 API 封装与 mock 数据"
```

## Task 8: 借阅审批校验（`src/utils/borrowApprovalValidation.ts`）

**Files:**
- Create: `src/utils/borrowApprovalValidation.ts`
- Test: `src/utils/borrowApprovalValidation.spec.ts`

> 说明：mock 层（Task 7）已做同样的服务端校验并抛错；本任务是前端纯函数校验，供页面在调用 API 前给用户即时反馈（双保险，对齐 `borrowValidation.ts` / `appraisalValidation.ts` 模式）。

- [ ] **Step 1: 写失败测试 `src/utils/borrowApprovalValidation.spec.ts`**

```typescript
import { describe, expect, it } from 'vitest'
import { validateBorrowApprove, validateBorrowCheckout, validateBorrowReturn } from './borrowApprovalValidation'
import type { BorrowApproveData, BorrowCheckoutData, BorrowReturnData } from '@/types/borrow-approval'

describe('validateBorrowApprove', () => {
  it('blocks approval when inventory hit', () => {
    const errors = validateBorrowApprove({ status: 'applied', inventoryHit: true }, { approved: true, opinion: '同意' })
    expect(errors).toContain('目标档案命中进行中盘点，不可审批通过。')
  })

  it('requires reason on reject', () => {
    const errors = validateBorrowApprove({ status: 'applied', inventoryHit: false }, { approved: false })
    expect(errors).toContain('审批拒绝必须填写原因。')
  })

  it('passes approve when not inventory hit', () => {
    expect(validateBorrowApprove({ status: 'applied', inventoryHit: false }, { approved: true, opinion: '同意' })).toEqual([])
  })

  it('rejects non-applied status', () => {
    const errors = validateBorrowApprove({ status: 'approved', inventoryHit: false }, { approved: true })
    expect(errors).toContain('仅待审批申请可审批。')
  })
})

describe('validateBorrowCheckout', () => {
  const ok: BorrowCheckoutData = { voucherNo: 'VCH-001', dueAt: '2026-06-22T09:00:00+08:00' }

  it('requires voucher and dueAt', () => {
    const errors = validateBorrowCheckout({ status: 'approved' }, { voucherNo: '', dueAt: '' })
    expect(errors).toContain('凭证号必填。')
    expect(errors).toContain('应还时间必填。')
  })

  it('rejects non-approved status', () => {
    const errors = validateBorrowCheckout({ status: 'applied' }, ok)
    expect(errors).toContain('仅已批准申请可确认出库。')
  })

  it('passes for approved with voucher and dueAt', () => {
    expect(validateBorrowCheckout({ status: 'approved' }, ok)).toEqual([])
  })
})

describe('validateBorrowReturn', () => {
  it('requires note for abnormal return', () => {
    const errors = validateBorrowReturn({ status: 'checked_out' }, { returnCheckResult: 'damaged' })
    expect(errors).toContain('异常归还必须填写检查说明。')
  })

  it('passes normal return without note', () => {
    expect(validateBorrowReturn({ status: 'checked_out' }, { returnCheckResult: 'normal' })).toEqual([])
  })

  it('rejects non-checked-out status', () => {
    const errors = validateBorrowReturn({ status: 'approved' }, { returnCheckResult: 'normal' })
    expect(errors).toContain('仅已出库申请可确认归还。')
  })
})
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `npx vitest run src/utils/borrowApprovalValidation.spec.ts`
Expected: FAIL（模块不存在）。

- [ ] **Step 3: 写 `src/utils/borrowApprovalValidation.ts`**

```typescript
import type { BorrowApprovalDetail, BorrowApproveData, BorrowCheckoutData, BorrowReturnData } from '@/types/borrow-approval'

/** 审批校验：返回错误数组（空数组表示通过） */
export function validateBorrowApprove(
  detail: Pick<BorrowApprovalDetail, 'status' | 'inventoryHit'>,
  data: BorrowApproveData,
): string[] {
  const errors: string[] = []
  if (detail.status !== 'applied') errors.push('仅待审批申请可审批。')
  if (data.approved && detail.inventoryHit) errors.push('目标档案命中进行中盘点，不可审批通过。')
  if (!data.approved) {
    const reason = (data.rejectReason?.trim() || data.opinion?.trim()) ?? ''
    if (!reason) errors.push('审批拒绝必须填写原因。')
  }
  return errors
}

/** 出库校验 */
export function validateBorrowCheckout(
  detail: Pick<BorrowApprovalDetail, 'status'>,
  data: BorrowCheckoutData,
): string[] {
  const errors: string[] = []
  if (detail.status !== 'approved' && detail.status !== 'voucher_issued') errors.push('仅已批准申请可确认出库。')
  if (!data.voucherNo.trim()) errors.push('凭证号必填。')
  if (!data.dueAt) errors.push('应还时间必填。')
  return errors
}

/** 归还校验 */
export function validateBorrowReturn(
  detail: Pick<BorrowApprovalDetail, 'status'>,
  data: BorrowReturnData,
): string[] {
  const errors: string[] = []
  if (detail.status !== 'checked_out') errors.push('仅已出库申请可确认归还。')
  if (!data.returnCheckResult) errors.push('请选择归还检查结果。')
  if (data.returnCheckResult && data.returnCheckResult !== 'normal' && !data.returnNote?.trim()) {
    errors.push('异常归还必须填写检查说明。')
  }
  return errors
}
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `npx vitest run src/utils/borrowApprovalValidation.spec.ts`
Expected: PASS（9 个用例全过）。

- [ ] **Step 5: 类型检查通过**

Run: `npx vue-tsc -b`
Expected: 无错误。

- [ ] **Step 6: 提交**

```bash
git add src/utils/borrowApprovalValidation.ts src/utils/borrowApprovalValidation.spec.ts
git commit -m "feat(storage-guo): 新增借阅审批出库归还前端校验"
```

## Task 9: 借阅审批页面（`src/views/admin/borrow-approval/index.vue`）

**Files:**
- Rewrite: `src/views/admin/borrow-approval/index.vue`
- Test: `src/views/admin/borrow-approval/index.spec.ts`

- [ ] **Step 1: 写失败测试 `src/views/admin/borrow-approval/index.spec.ts`**

```typescript
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import BorrowApproval from './index.vue'

const stubs = {
  ElMessageBox: { template: '<div />' },
}

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

async function mountComponent() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/admin/borrow-approval', component: BorrowApproval }],
  })
  await router.push('/admin/borrow-approval')
  await router.isReady()
  return mount(BorrowApproval, { global: { plugins: [router], stubs } })
}

describe('BorrowApproval', () => {
  it('renders list covering multiple statuses', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    expect(wrapper.text()).toContain('JY-2026-0017')
    expect(wrapper.text()).toContain('待审批')
  })

  it('filters by status tab', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    const tabs = wrapper.findAll('.tab')
    const loanTab = tabs.find((t) => t.text().includes('借出中'))
    await loanTab?.trigger('click')
    await waitForAsyncData()
    expect(wrapper.text()).toContain('JY-2026-0009')
  })

  it('disables approve buttons for non-applied request', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    const tabs = wrapper.findAll('.tab')
    const loanTab = tabs.find((t) => t.text().includes('借出中'))
    await loanTab?.trigger('click')
    await waitForAsyncData()
    const firstCard = wrapper.find('.request-card')
    await firstCard.trigger('click')
    await waitForAsyncData()
    const approveBtn = wrapper.findAll('button').find((b) => b.text().includes('审批通过'))
    expect((approveBtn?.element as HTMLButtonElement).disabled).toBe(true)
  })

  it('shows detail fields after selecting', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    await wrapper.find('.request-card').trigger('click')
    await waitForAsyncData()
    expect(wrapper.text()).toContain('可借检查')
    expect(wrapper.text()).toContain('凭证、出库与归还')
  })
})
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `npx vitest run src/views/admin/borrow-approval/index.spec.ts`
Expected: FAIL（页面仍是占位）。

- [ ] **Step 3: 重写 `src/views/admin/borrow-approval/index.vue`**

```vue
<template>
  <div class="borrow-approval">
    <section>
      <h1 class="page-title">借阅审批</h1>
      <p class="page-subtitle">纸质借阅必须走审批。审批通过后只允许申请人导出凭证；到馆核验并确认出库后，档案才变为借出。</p>
    </section>

    <section class="metric-row">
      <div class="metric card"><div class="metric-num">{{ metrics.pending }}</div><div class="metric-label">待审批</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.approvedNotOut }}</div><div class="metric-label">已批准未出库</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.onLoan }}</div><div class="metric-label">借出中</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.overdueAbnormal }}</div><div class="metric-label">逾期/异常</div></div>
    </section>

    <section class="steps">
      <div class="step"><strong>1. 提交申请</strong><br><span class="muted">内部查阅者填写理由、时长、到馆时间。</span></div>
      <div class="step"><strong>2. 后台审批</strong><br><span class="muted">检查档案状态、盘点范围和是否借出。</span></div>
      <div class="step"><strong>3. 凭证核验</strong><br><span class="muted">单位盖章凭证到馆核验。</span></div>
      <div class="step"><strong>4. 出库归还</strong><br><span class="muted">确认出库后借出，归还检查后恢复正常。</span></div>
    </section>

    <section class="borrow-layout">
      <aside class="card panel list-col">
        <div class="tabs">
          <button class="tab" :class="{ active: filterStatus === 'applied' }" @click="filterStatus = 'applied'">待审批</button>
          <button class="tab" :class="{ active: filterStatus === 'approved' }" @click="filterStatus = 'approved'">已批准</button>
          <button class="tab" :class="{ active: filterStatus === 'checked_out' }" @click="filterStatus = 'checked_out'">借出中</button>
          <button class="tab" :class="{ active: filterStatus === 'returned' }" @click="filterStatus = 'returned'">归还处理</button>
        </div>
        <div v-if="listLoading" class="empty">加载中...</div>
        <div v-else-if="loadError" class="empty">加载失败：<button class="link" @click="loadList">重试</button></div>
        <div v-else-if="filteredList.length === 0" class="empty">当前状态下没有借阅申请。</div>
        <div v-else class="request-list">
          <button
            v-for="r in filteredList"
            :key="r.id"
            class="request-card"
            :class="{ active: selectedId === r.id }"
            @click="selectRequest(r.id)"
          >
            <strong>{{ r.archiveTitle }}</strong>
            <span class="mono">{{ r.requestNo }}</span>
            <span><span class="status" :class="statusClass(r.status)">{{ BorrowStatusLabel[r.status] }}</span> <span class="muted">{{ r.borrowerName }}</span></span>
          </button>
        </div>
      </aside>

      <section class="detail-area">
        <template v-if="!selectedId"><div class="empty">← 点击左侧申请查看详情与操作</div></template>
        <template v-else-if="detailLoading"><div class="empty">加载中...</div></template>
        <template v-else-if="!detail"><div class="empty">详情加载失败：<button class="link" @click="selectRequest(selectedId)">重试</button></div></template>
        <template v-else>
          <div class="card panel">
            <h2 class="section-title">{{ detail.archiveTitle }}</h2>
            <div class="form-grid">
              <div class="field"><label>申请号</label><input :value="detail.requestNo" disabled></div>
              <div class="field"><label>借阅人</label><input :value="`${detail.borrowerName} / ${detail.borrowerOrg ?? ''}`" disabled></div>
              <div class="field"><label>预计到馆</label><input :value="detail.expectedVisitAt ?? '—'" disabled></div>
              <div class="field"><label>借阅时长</label><input :value="`${detail.expectedDays} 天`" disabled></div>
              <div class="field"><label>档号</label><input :value="detail.archiveNo" disabled></div>
              <div class="field"><label>管理员可见架位</label><input :value="detail.archiveLocationCode ?? '—'" disabled></div>
            </div>
            <div class="field" style="margin-top:12px"><label>借阅理由</label><textarea :value="detail.reason" disabled></textarea></div>
          </div>

          <div class="grid two">
            <div class="card panel">
              <h2 class="section-title">可借检查</h2>
              <div class="check-list">
                <div><span>载体状态</span><strong>{{ detail.checkCarrier ?? '—' }}</strong></div>
                <div><span>生命周期</span><strong>{{ detail.checkLifecycle ?? '—' }}</strong></div>
                <div><span>借阅状态</span><strong>{{ detail.checkLoan ?? '—' }}</strong></div>
                <div><span>盘点范围</span><strong>{{ detail.checkInventory ?? '—' }}</strong></div>
              </div>
              <div class="field" style="margin-top:12px"><label>拒绝/退回原因</label><textarea v-model="rejectReason" placeholder="不可借时填写原因"></textarea></div>
              <div class="actions">
                <button class="button" :disabled="detail.status !== 'applied'" @click="onApprove(true)">审批通过</button>
                <button class="button danger" :disabled="detail.status !== 'applied'" @click="onApprove(false)">审批拒绝</button>
              </div>
            </div>

            <div class="card panel">
              <h2 class="section-title">凭证、出库与归还</h2>
              <div class="notice warning">借阅凭证包含单位意见盖章区和已归还盖章区，不等于出库记录。</div>
              <div class="field"><label>凭证号</label><input v-model="voucherNo" placeholder="VCH-xxxxxx"></div>
              <div class="field"><label>应还时间</label><input v-model="dueAt" type="datetime-local"></div>
              <div class="field"><label>归还检查结果</label>
                <select v-model="returnCheckResult">
                  <option v-for="opt in returnOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </div>
              <div class="field"><label>归还检查说明</label><textarea v-model="returnNote" placeholder="异常归还时必填"></textarea></div>
              <div class="actions">
                <button class="button secondary" :disabled="!canExport" @click="onExport">导出凭证</button>
                <button class="button" :disabled="!canCheckout" @click="onCheckout">确认出库</button>
                <button class="button ghost" :disabled="!canReturn" @click="onReturn">确认归还</button>
              </div>
            </div>
          </div>
        </template>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { BorrowApprovalDetail, BorrowApproveData, BorrowCheckoutData, BorrowReturnData } from '@/types/borrow-approval'
import { BorrowStatusLabel, ReturnCheckResult } from '@/types/enums'
import type { ReturnCheckResultValue } from '@/types/enums'
import {
  approveBorrowRequest, checkoutBorrowRequest, exportBorrowVoucher,
  getBorrowApprovalDetail, getBorrowApprovals, returnBorrowRequest,
} from '@/api/borrow-approval'
import { validateBorrowApprove, validateBorrowCheckout, validateBorrowReturn } from '@/utils/borrowApprovalValidation'

type FilterStatus = 'applied' | 'approved' | 'checked_out' | 'returned'

const allRequests = ref<BorrowApprovalDetail[]>([])
const listLoading = ref(false)
const loadError = ref(false)
const filterStatus = ref<FilterStatus>('applied')

const selectedId = ref<number | null>(null)
const detail = ref<BorrowApprovalDetail | null>(null)
const detailLoading = ref(false)

const rejectReason = ref('')
const voucherNo = ref('')
const dueAt = ref('')
const returnCheckResult = ref<ReturnCheckResultValue>(ReturnCheckResult.NORMAL)
const returnNote = ref('')

const returnOptions: { value: ReturnCheckResultValue; label: string }[] = [
  { value: ReturnCheckResult.NORMAL, label: '正常' },
  { value: ReturnCheckResult.MISSING_PAGE, label: '缺页' },
  { value: ReturnCheckResult.DAMAGED, label: '破损' },
  { value: ReturnCheckResult.OTHER, label: '其他' },
]

const metrics = computed(() => {
  const all = allRequests.value
  return {
    pending: all.filter((r) => r.status === 'applied').length,
    approvedNotOut: all.filter((r) => r.status === 'approved' || r.status === 'voucher_issued').length,
    onLoan: all.filter((r) => r.status === 'checked_out').length,
    overdueAbnormal: all.filter((r) => r.overdue || r.status === 'abnormal_return').length,
  }
})

const filteredList = computed(() => {
  const s = filterStatus.value
  if (s === 'returned') return allRequests.value.filter((r) => ['returned', 'abnormal_return', 'rejected'].includes(r.status))
  if (s === 'approved') return allRequests.value.filter((r) => ['approved', 'voucher_issued'].includes(r.status))
  return allRequests.value.filter((r) => r.status === s)
})

const canExport = computed(() => !!detail.value && ['approved', 'voucher_issued'].includes(detail.value.status))
const canCheckout = computed(() => !!detail.value && ['approved', 'voucher_issued'].includes(detail.value.status))
const canReturn = computed(() => detail.value?.status === 'checked_out')

function statusClass(s: string): string {
  const map: Record<string, string> = {
    applied: 'warning', approved: 'info', voucher_issued: 'info', checked_out: 'warning',
    returned: 'success', abnormal_return: 'danger', rejected: 'danger',
  }
  return map[s] ?? ''
}

async function loadList() {
  listLoading.value = true
  loadError.value = false
  try {
    const page = await getBorrowApprovals({ pageSize: 100 })
    allRequests.value = page.records
  } catch {
    loadError.value = true
  } finally {
    listLoading.value = false
  }
}

async function selectRequest(id: number) {
  selectedId.value = id
  detail.value = null
  detailLoading.value = true
  try {
    detail.value = await getBorrowApprovalDetail(id)
    resetForms()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

function resetForms() {
  rejectReason.value = ''
  voucherNo.value = detail.value?.voucherNo ?? ''
  dueAt.value = detail.value?.dueAt ?? ''
  returnCheckResult.value = ReturnCheckResult.NORMAL
  returnNote.value = ''
}

async function onApprove(approved: boolean) {
  if (!detail.value) return
  const data: BorrowApproveData = approved ? { approved: true } : { approved: false, rejectReason: rejectReason.value }
  const errors = validateBorrowApprove(detail.value, data)
  if (errors.length) {
    errors.forEach((e) => ElMessage.warning(e))
    return
  }
  try {
    const updated = await approveBorrowRequest(detail.value.id, data)
    detail.value = updated
    ElMessage.success(approved ? '审批已通过，申请人可导出借阅凭证。' : '已拒绝借阅申请并保留原因。')
    await loadList()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onExport() {
  if (!detail.value) return
  try {
    const result = await exportBorrowVoucher(detail.value.id)
    voucherNo.value = result.voucherNo
    detail.value = await getBorrowApprovalDetail(detail.value.id)
    ElMessage.success(result.firstIssued ? `借阅凭证已生成：${result.voucherNo}` : `凭证号：${result.voucherNo}（复用）`)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '导出失败')
  }
}

async function onCheckout() {
  if (!detail.value) return
  const data: BorrowCheckoutData = { voucherNo: voucherNo.value, dueAt: dueAt.value }
  const errors = validateBorrowCheckout(detail.value, data)
  if (errors.length) {
    errors.forEach((e) => ElMessage.warning(e))
    return
  }
  try {
    await ElMessageBox.confirm('确认出库后档案状态将变为借出，是否继续？', '确认出库', { type: 'warning' })
  } catch {
    return
  }
  try {
    const updated = await checkoutBorrowRequest(detail.value.id, data)
    detail.value = updated
    ElMessage.success('已确认出库，档案状态变为借出。')
    await loadList()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onReturn() {
  if (!detail.value) return
  const data: BorrowReturnData = { returnCheckResult: returnCheckResult.value, returnNote: returnNote.value }
  const errors = validateBorrowReturn(detail.value, data)
  if (errors.length) {
    errors.forEach((e) => ElMessage.warning(e))
    return
  }
  try {
    const updated = await returnBorrowRequest(detail.value.id, data)
    detail.value = updated
    ElMessage.success('归还检查已记录，档案借阅状态恢复可借。')
    await loadList()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

onMounted(loadList)
</script>

<style scoped>
.borrow-approval { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { color: var(--muted); font-size: 13px; margin: 0 0 16px; }
.metric-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }
.metric { padding: 16px; text-align: center; }
.metric-num { font-size: 26px; font-weight: 700; color: var(--primary); }
.metric-label { font-size: 13px; color: var(--muted); margin-top: 4px; }
.steps { display: grid; grid-template-columns: repeat(4, 1fr); gap: 10px; margin-bottom: 16px; }
.step { padding: 12px; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; font-size: 13px; }
.borrow-layout { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 16px; align-items: start; }
.list-col { padding: 10px; max-height: 680px; overflow-y: auto; }
.tabs { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 10px; }
.tab { padding: 6px 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: #fff; cursor: pointer; font-size: 13px; }
.tab.active { color: var(--primary-strong); border-color: #8abcbf; background: var(--primary-soft); }
.request-list { display: grid; gap: 8px; }
.request-card { display: grid; gap: 5px; width: 100%; padding: 12px; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; text-align: left; cursor: pointer; }
.request-card:hover { background: #fafafa; }
.request-card.active { border-color: #8abcbf; background: #f2f8f8; }
.detail-area { display: grid; gap: 16px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 8px; }
.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.field { display: grid; gap: 4px; }
.field label { font-size: 12px; color: var(--muted); font-weight: 700; }
.field input, .field textarea, .field select { padding: 6px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 13px; }
.field input:disabled, .field textarea:disabled { background: var(--bg); color: var(--muted); }
.check-list { display: grid; gap: 8px; }
.check-list div { display: flex; justify-content: space-between; gap: 12px; padding: 9px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: #fff; font-size: 13px; }
.check-list span { color: var(--muted); }
.grid.two { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.actions { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 12px; }
.button { padding: 6px 14px; border: 1px solid var(--primary); border-radius: var(--radius-sm); background: var(--primary); color: #fff; cursor: pointer; font-size: 13px; }
.button:disabled { opacity: 0.5; cursor: not-allowed; }
.button.secondary { background: #fff; color: var(--primary); }
.button.ghost { background: #fff; color: var(--text); border-color: var(--border); }
.button.danger { background: var(--danger); border-color: var(--danger); }
.notice { padding: 9px 12px; border-radius: var(--radius-sm); font-size: 12px; margin-bottom: 8px; }
.notice.warning { background: var(--warning-soft); color: #7a4c12; }
.empty { display: flex; align-items: center; justify-content: center; height: 200px; color: var(--muted); font-size: 14px; }
.link { background: none; border: none; color: var(--primary); cursor: pointer; font-size: 13px; text-decoration: underline; padding: 0; }
.muted { color: var(--muted); font-size: 12px; }
.mono { font-family: monospace; font-size: 12px; color: var(--muted); }
.status { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.status.success { background: #f6ffed; color: #52c41a; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.danger { background: #fff1f0; color: #f5222d; }
.status.info { background: #e6f7ff; color: #1890ff; }
@media (max-width: 1080px) { .metric-row, .steps, .grid.two { grid-template-columns: 1fr; } .borrow-layout { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `npx vitest run src/views/admin/borrow-approval/index.spec.ts`
Expected: PASS（4 个用例全过）。

- [ ] **Step 5: 类型检查通过**

Run: `npx vue-tsc -b`
Expected: 无错误。

- [ ] **Step 6: 提交**

```bash
git add src/views/admin/borrow-approval/index.vue src/views/admin/borrow-approval/index.spec.ts
git commit -m "feat(storage-guo): 实现借阅审批出库归还页面"
```

## Task 10: 盘点页面（`src/views/admin/inventory/index.vue`）

**Files:**
- Rewrite: `src/views/admin/inventory/index.vue`
- Test: `src/views/admin/inventory/index.spec.ts`

- [ ] **Step 1: 写失败测试 `src/views/admin/inventory/index.spec.ts`**

```typescript
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import Inventory from './index.vue'

const stubs = { ElMessageBox: { template: '<div />' } }

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

async function mountComponent() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/admin/inventory', component: Inventory }],
  })
  await router.push('/admin/inventory')
  await router.isReady()
  return mount(Inventory, { global: { plugins: [router], stubs } })
}

describe('Inventory', () => {
  it('renders running tasks', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    expect(wrapper.text()).toContain('2026 年度 401 库房科技档案盘点')
    expect(wrapper.text()).toContain('进行中')
  })

  it('shows detail items after selecting a running task', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    await wrapper.find('.task-item').trigger('click')
    await waitForAsyncData()
    expect(wrapper.text()).toContain('采油一厂设备验收报告')
    expect(wrapper.text()).toContain('盘点结果')
  })

  it('opens create task modal', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    const btn = wrapper.findAll('button').find((b) => b.text().includes('新建盘点任务'))
    await btn?.trigger('click')
    expect(wrapper.text()).toContain('新建盘点任务')
    expect(wrapper.find('.modal').exists()).toBe(true)
  })

  it('filter tasks by status tab', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    const tabs = wrapper.findAll('.tab')
    const draftTab = tabs.find((t) => t.text().includes('草稿'))
    await draftTab?.trigger('click')
    await waitForAsyncData()
    expect(wrapper.text()).toContain('2026 年度 403 库房音像档案盘点')
  })
})
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `npx vitest run src/views/admin/inventory/index.spec.ts`
Expected: FAIL（页面仍是占位）。

- [ ] **Step 3: 重写 `src/views/admin/inventory/index.vue`**

```vue
<template>
  <div class="inventory">
    <section class="hero-line">
      <div>
        <h1 class="page-title">档案盘点</h1>
        <p class="page-subtitle">按库房号和分类生成盘点任务，命中范围内档案在盘点期间暂停借阅。</p>
      </div>
      <button class="button" @click="openTaskModal">+ 新建盘点任务</button>
    </section>

    <section class="metric-row">
      <div class="metric card"><div class="metric-num">{{ metrics.running }}</div><div class="metric-label">进行中任务</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.abnormal }}</div><div class="metric-label">异常项</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.completedThisYear }}</div><div class="metric-label">本年完成</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.draft }}</div><div class="metric-label">草稿</div></div>
    </section>

    <div v-if="scopeBanner" class="scope-banner">
      <div class="scope-formula">
        <span>盘点范围</span><span>库房号：{{ scopeBanner.roomNo }}</span><strong>AND</strong><span>分类：{{ scopeBanner.categoryName }}</span>
      </div>
      <span class="status warning">借阅暂停生效</span>
    </div>

    <section class="workspace">
      <aside class="card panel list-col">
        <div class="tabs">
          <button class="tab" :class="{ active: taskFilter === 'running' }" @click="taskFilter = 'running'">进行中</button>
          <button class="tab" :class="{ active: taskFilter === 'draft' }" @click="taskFilter = 'draft'">草稿</button>
          <button class="tab" :class="{ active: taskFilter === 'completed' }" @click="taskFilter = 'completed'">已完成</button>
        </div>
        <div v-if="listLoading" class="empty">加载中...</div>
        <div v-else-if="loadError" class="empty">加载失败：<button class="link" @click="loadTasks">重试</button></div>
        <div v-else-if="filteredTasks.length === 0" class="empty">暂无盘点任务</div>
        <div v-else class="task-list">
          <button v-for="t in filteredTasks" :key="t.id" class="task-item" :class="{ active: selectedId === t.id }" @click="selectTask(t.id)">
            <strong>{{ t.taskName }}</strong>
            <span class="task-meta"><span>范围：{{ t.roomNo }} AND {{ t.categoryName }}</span><span>{{ t.total }} 件</span></span>
            <span class="progress-track"><span class="progress-fill" :style="{ width: progressPercent(t) + '%' }"></span></span>
            <span class="task-meta"><span>已核对 {{ t.checked }} 件</span><span class="status" :class="taskStatusClass(t.status)">{{ InventoryTaskStatusLabel[t.status] }}</span></span>
          </button>
        </div>
      </aside>

      <section class="card panel">
        <template v-if="!selectedId"><div class="empty">← 点击左侧任务查看盘点明细</div></template>
        <template v-else-if="detailLoading"><div class="empty">加载中...</div></template>
        <template v-else-if="!detail"><div class="empty">明细加载失败：<button class="link" @click="selectTask(selectedId)">重试</button></div></template>
        <template v-else>
          <div class="inventory-toolbar">
            <div>
              <h2 class="section-title">{{ detail.taskName }}</h2>
              <p class="hint">任务号：{{ detail.taskNo }} · 状态：{{ InventoryTaskStatusLabel[detail.status] }}<span v-if="detail.startedAt"> · 开始：{{ detail.startedAt }}</span></p>
            </div>
            <div class="actions">
              <button v-if="detail.status === 'draft'" class="button" @click="onStart">开始盘点</button>
              <button v-if="detail.status === 'running'" class="button" @click="onComplete">提交盘点结果</button>
            </div>
          </div>

          <div v-if="detail.status !== 'draft'" class="result-filter">
            <button v-for="opt in resultFilters" :key="opt.value" class="mini-button" :class="{ active: resultFilter === opt.value }" @click="resultFilter = opt.value">
              {{ opt.label }} {{ resultCount(opt.value) }}
            </button>
          </div>

          <div v-if="detail.items.length === 0" class="empty">该任务暂无盘点明细。</div>
          <div v-else class="table-wrap">
            <table>
              <thead><tr><th>档号/题名</th><th>应在架位</th><th>实际架位</th><th>借阅状态</th><th>盘点结果</th><th>说明</th></tr></thead>
              <tbody>
                <tr v-for="it in visibleItems" :key="it.id">
                  <td><span class="mono">{{ it.archiveNo }}</span><br>{{ it.title }}</td>
                  <td class="mono">{{ it.expectedLocationCode }}</td>
                  <td>
                    <input v-if="detail.status === 'running'" v-model="editMap[it.id].actualLocationCode" class="inline-input mono">
                    <template v-else>{{ it.actualLocationCode || '—' }}</template>
                  </td>
                  <td><span class="status" :class="loanClass(it)">{{ loanLabel(it) }}</span></td>
                  <td>
                    <select v-if="detail.status === 'running'" v-model="editMap[it.id].checkResult">
                      <option value="">未核对</option>
                      <option v-for="r in resultOptions" :key="r.value" :value="r.value">{{ r.label }}</option>
                    </select>
                    <span v-else class="status" :class="checkResultClass(it.checkResult)">{{ InventoryCheckResultLabel[it.checkResult] || '—' }}</span>
                  </td>
                  <td>
                    <input v-if="detail.status === 'running'" v-model="editMap[it.id].note" class="inline-input">
                    <template v-else>{{ it.note || '—' }}</template>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <section class="borrow-impact">
            <div class="impact-item"><span class="status warning">借阅审批校验</span><h3 class="section-title">命中范围暂停</h3><p class="hint">盘点范围内新的纸质借阅申请显示"盘点暂停借阅"。</p></div>
            <div class="impact-item"><span class="status info">已借出档案</span><h3 class="section-title">不强制召回</h3><p class="hint">盘点清单记录为"借出中"，归还后可补核实物状态。</p></div>
            <div class="impact-item"><span class="status success">完成后恢复</span><h3 class="section-title">正常档案可借</h3><p class="hint">提交盘点结果后，正常项恢复可借。</p></div>
          </section>
        </template>
      </section>
    </section>

    <div v-if="taskModalVisible" class="modal-backdrop" @click.self="taskModalVisible = false">
      <div class="modal">
        <header><h2 class="section-title">新建盘点任务</h2><button class="button ghost" @click="taskModalVisible = false">关闭</button></header>
        <div class="body">
          <div class="notice warning">盘点范围必须同时满足库房号和分类：库房号 AND 分类。</div>
          <div class="form-grid">
            <div class="field"><label>库房号</label>
              <select v-model.number="newTask.roomId">
                <option v-for="r in rooms" :key="r.id" :value="r.id">{{ r.roomNo }} {{ r.roomName }}</option>
              </select>
            </div>
            <div class="field"><label>分类</label>
              <select v-model.number="newTask.categoryId">
                <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </div>
            <div class="field"><label>任务名称</label><input v-model="newTask.taskName"></div>
          </div>
        </div>
        <footer>
          <button class="button ghost" @click="taskModalVisible = false">取消</button>
          <button class="button" @click="onCreate">生成清单</button>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { InventoryItem, InventoryTask, InventoryTaskDetail } from '@/types/inventory'
import type { WarehouseRoom } from '@/types/warehouse'
import {
  InventoryCheckResult, InventoryCheckResultLabel, InventoryTaskStatusLabel,
} from '@/types/enums'
import type { InventoryCheckResultValue, InventoryTaskStatusValue } from '@/types/enums'
import {
  completeInventoryTask, createInventoryTask, getInventoryTaskDetail,
  getInventoryTasks, startInventoryTask, updateInventoryItem,
} from '@/api/inventory'
import { getWarehouseRooms } from '@/api/warehouse'

interface ItemEdit { actualLocationCode: string; checkResult: InventoryCheckResultValue | ''; note: string }

const allTasks = ref<InventoryTask[]>([])
const listLoading = ref(false)
const loadError = ref(false)
const taskFilter = ref<InventoryTaskStatusValue>('running')

const selectedId = ref<number | null>(null)
const detail = ref<InventoryTaskDetail | null>(null)
const detailLoading = ref(false)

const editMap = reactive<Record<number, ItemEdit>>({})
const resultFilter = ref<'all' | InventoryCheckResultValue>('all')

const rooms = ref<WarehouseRoom[]>([])
const categories = [
  { id: 1, name: '科技档案' }, { id: 2, name: '文书档案' }, { id: 3, name: '会计档案' },
  { id: 4, name: '音像档案' }, { id: 5, name: '人事档案' },
]
const taskModalVisible = ref(false)
const newTask = reactive({ roomId: 1, categoryId: 1, taskName: '' })

const resultOptions: { value: InventoryCheckResultValue; label: string }[] = [
  { value: InventoryCheckResult.NORMAL, label: '正常' },
  { value: InventoryCheckResult.MISSING, label: '缺失' },
  { value: InventoryCheckResult.MISPLACED, label: '错位' },
  { value: InventoryCheckResult.DAMAGED, label: '损坏' },
  { value: InventoryCheckResult.ON_LOAN, label: '借出中' },
]
const resultFilters: { value: 'all' | InventoryCheckResultValue; label: string }[] = [
  { value: 'all', label: '全部' },
  { value: InventoryCheckResult.NORMAL, label: '正常' },
  { value: InventoryCheckResult.MISSING, label: '缺失' },
  { value: InventoryCheckResult.MISPLACED, label: '错位' },
  { value: InventoryCheckResult.DAMAGED, label: '损坏' },
  { value: InventoryCheckResult.ON_LOAN, label: '借出中' },
]

const metrics = computed(() => ({
  running: allTasks.value.filter((t) => t.status === 'running').length,
  draft: allTasks.value.filter((t) => t.status === 'draft').length,
  completedThisYear: allTasks.value.filter((t) => t.status === 'completed').length,
  abnormal: allTasks.value.reduce((s, t) => s + t.abnormalCount, 0),
}))

const filteredTasks = computed(() => allTasks.value.filter((t) => t.status === taskFilter.value))

const scopeBanner = computed(() => {
  if (!detail.value || detail.value.status === 'draft') return null
  return { roomNo: detail.value.roomNo, categoryName: detail.value.categoryName }
})

const visibleItems = computed(() => {
  if (!detail.value) return []
  if (resultFilter.value === 'all') return detail.value.items
  return detail.value.items.filter((it) => it.checkResult === resultFilter.value)
})

function progressPercent(t: InventoryTask): number {
  return t.total > 0 ? Math.round((t.checked / t.total) * 100) : 0
}
function taskStatusClass(s: string): string {
  return ({ draft: 'info', running: 'warning', completed: 'success' } as Record<string, string>)[s] ?? ''
}
function loanLabel(it: InventoryItem): string {
  if (it.loanStatus === 'on_loan') return '借出中'
  return detail.value?.status === 'running' ? '暂停借阅' : '可借'
}
function loanClass(it: InventoryItem): string {
  if (it.loanStatus === 'on_loan') return 'info'
  return detail.value?.status === 'running' ? 'warning' : 'success'
}
function checkResultClass(s: string): string {
  return ({ normal: 'success', missing: 'danger', misplaced: 'warning', damaged: 'warning', on_loan: 'info' } as Record<string, string>)[s] ?? ''
}
function resultCount(v: string): number {
  if (!detail.value) return 0
  if (v === 'all') return detail.value.items.length
  return detail.value.items.filter((it) => it.checkResult === v).length
}

async function loadTasks() {
  listLoading.value = true
  loadError.value = false
  try {
    const page = await getInventoryTasks({ pageSize: 100 })
    allTasks.value = page.records
  } catch {
    loadError.value = true
  } finally {
    listLoading.value = false
  }
}

async function selectTask(id: number) {
  selectedId.value = id
  detail.value = null
  detailLoading.value = true
  resultFilter.value = 'all'
  try {
    detail.value = await getInventoryTaskDetail(id)
    Object.keys(editMap).forEach((k) => { delete editMap[Number(k)] })
    detail.value.items.forEach((it) => {
      editMap[it.id] = { actualLocationCode: it.actualLocationCode ?? '', checkResult: it.checkResult, note: it.note ?? '' }
    })
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '明细加载失败')
  } finally {
    detailLoading.value = false
  }
}

function openTaskModal() {
  const r = rooms.value[0]
  newTask.roomId = r?.id ?? 1
  newTask.categoryId = 1
  newTask.taskName = `2026 年度 ${r?.roomNo ?? ''} 库房${categories[0].name}盘点`
  taskModalVisible.value = true
}

async function onCreate() {
  if (!newTask.taskName.trim()) {
    ElMessage.warning('请填写任务名称')
    return
  }
  try {
    const created = await createInventoryTask({ taskName: newTask.taskName, roomId: newTask.roomId, categoryId: newTask.categoryId })
    taskModalVisible.value = false
    ElMessage.success(`已生成盘点清单：${created.taskNo}，命中 ${created.items.length} 件`)
    await loadTasks()
    await selectTask(created.id)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  }
}

async function onStart() {
  if (!detail.value) return
  try {
    const started = await startInventoryTask(detail.value.id)
    detail.value = started
    ElMessage.success('盘点已开始，范围内档案借阅暂停。')
    await loadTasks()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onComplete() {
  if (!detail.value) return
  try {
    for (const it of detail.value.items) {
      const edit = editMap[it.id]
      if (!edit) continue
      const changed = edit.actualLocationCode !== (it.actualLocationCode ?? '') || edit.checkResult !== it.checkResult || edit.note !== (it.note ?? '')
      if (changed) {
        if (!edit.checkResult) {
          ElMessage.warning(`档案 ${it.archiveNo} 未选择盘点结果`)
          return
        }
        await updateInventoryItem(detail.value.id, it.id, { actualLocationCode: edit.actualLocationCode, checkResult: edit.checkResult, note: edit.note })
      }
    }
    await ElMessageBox.confirm('提交后任务变为已完成，正常档案恢复可借，是否继续？', '提交盘点结果', { type: 'warning' })
  } catch {
    return
  }
  try {
    const completed = await completeInventoryTask(detail.value.id, { summary: '' })
    detail.value = completed
    ElMessage.success('盘点结果已提交，任务已完成。')
    await loadTasks()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

onMounted(async () => {
  await loadTasks()
  try {
    rooms.value = await getWarehouseRooms()
  } catch {
    rooms.value = []
  }
})
</script>

<style scoped>
.inventory { padding: 0; }
.hero-line { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { color: var(--muted); font-size: 13px; margin: 0; }
.metric-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }
.metric { padding: 16px; text-align: center; }
.metric-num { font-size: 26px; font-weight: 700; color: var(--primary); }
.metric-label { font-size: 13px; color: var(--muted); margin-top: 4px; }
.scope-banner { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 12px 14px; border: 1px solid #efd39d; border-radius: var(--radius); background: var(--warning-soft); color: #6f440c; margin-bottom: 16px; }
.scope-formula { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; font-weight: 700; }
.scope-formula span { display: inline-flex; padding: 3px 9px; border-radius: 999px; background: #fff; }
.workspace { display: grid; grid-template-columns: 320px minmax(0, 1fr); gap: 16px; align-items: start; }
.list-col { padding: 10px; max-height: 700px; overflow-y: auto; }
.tabs { display: flex; gap: 6px; margin-bottom: 10px; }
.tab { padding: 6px 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: #fff; cursor: pointer; font-size: 13px; }
.tab.active { color: var(--primary-strong); border-color: #8abcbf; background: var(--primary-soft); }
.task-list { display: grid; gap: 10px; }
.task-item { display: grid; gap: 6px; padding: 12px; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; text-align: left; cursor: pointer; }
.task-item:hover { background: #fafafa; }
.task-item.active { border-color: #8abcbf; background: #f2f8f8; }
.task-meta { display: flex; gap: 12px; color: var(--muted); font-size: 12px; }
.progress-track { height: 8px; border-radius: 999px; background: #e5edf2; overflow: hidden; }
.progress-fill { display: block; height: 100%; background: var(--primary); }
.inventory-toolbar { display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; margin-bottom: 12px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 4px; }
.hint { color: var(--muted); font-size: 12px; }
.actions { display: flex; gap: 8px; }
.result-filter { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px; }
.mini-button { padding: 5px 10px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: #fff; cursor: pointer; font-size: 12px; font-weight: 700; }
.mini-button.active { color: var(--primary-strong); border-color: #b9d7d9; background: var(--primary-soft); }
.table-wrap { overflow-x: auto; }
.table-wrap table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table-wrap th, .table-wrap td { padding: 8px 10px; text-align: left; border-bottom: 1px solid var(--border); }
.table-wrap th { font-weight: 700; color: var(--muted); background: var(--bg); }
.inline-input { width: min(160px, 100%); padding: 4px 6px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 12px; }
.table-wrap select { padding: 4px 6px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 12px; }
.borrow-impact { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-top: 16px; }
.impact-item { padding: 14px; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; }
.modal-backdrop { position: fixed; inset: 0; z-index: 30; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(23, 33, 43, 0.38); }
.modal { width: min(640px, 100%); border-radius: var(--radius); background: #fff; box-shadow: var(--shadow); }
.modal header, .modal footer { display: flex; justify-content: space-between; align-items: center; padding: 14px 18px; border-bottom: 1px solid var(--border); }
.modal footer { border-top: 1px solid var(--border); border-bottom: 0; }
.modal .body { padding: 18px; }
.notice { padding: 9px 12px; border-radius: var(--radius-sm); font-size: 12px; margin-bottom: 10px; }
.notice.warning { background: var(--warning-soft); color: #7a4c12; }
.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.field { display: grid; gap: 4px; }
.field label { font-size: 12px; color: var(--muted); font-weight: 700; }
.field input, .field select { padding: 6px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 13px; }
.button { padding: 6px 14px; border: 1px solid var(--primary); border-radius: var(--radius-sm); background: var(--primary); color: #fff; cursor: pointer; font-size: 13px; }
.button.ghost { background: #fff; color: var(--text); border-color: var(--border); }
.empty { display: flex; align-items: center; justify-content: center; height: 200px; color: var(--muted); font-size: 14px; }
.link { background: none; border: none; color: var(--primary); cursor: pointer; font-size: 13px; text-decoration: underline; padding: 0; }
.muted { color: var(--muted); font-size: 12px; }
.mono { font-family: monospace; font-size: 12px; }
.status { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.status.success { background: #f6ffed; color: #52c41a; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.danger { background: #fff1f0; color: #f5222d; }
.status.info { background: #e6f7ff; color: #1890ff; }
@media (max-width: 1080px) { .metric-row, .workspace, .borrow-impact, .form-grid { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `npx vitest run src/views/admin/inventory/index.spec.ts`
Expected: PASS（4 个用例全过）。

- [ ] **Step 5: 类型检查通过**

Run: `npx vue-tsc -b`
Expected: 无错误。

- [ ] **Step 6: 提交**

```bash
git add src/views/admin/inventory/index.vue src/views/admin/inventory/index.spec.ts
git commit -m "feat(storage-guo): 实现档案盘点页面"
```

## Task 11a: 库房展示型子组件（RoomList / RackBoard / RecentBoxesTable）

**Files:**
- Create: `src/views/admin/warehouse/components/RoomList.vue`
- Create: `src/views/admin/warehouse/components/RackBoard.vue`
- Create: `src/views/admin/warehouse/components/RecentBoxesTable.vue`

> 这三个是纯展示组件（无 API 调用），由 Task 12 的 `warehouse/index.spec.ts` 间接覆盖；本步只创建组件 + 类型检查 + 提交。

- [ ] **Step 1: 创建 `src/views/admin/warehouse/components/RoomList.vue`**

```vue
<template>
  <section class="card panel">
    <div class="toolbar" style="margin-top: 0">
      <h2 class="section-title">库房列表</h2>
      <span class="status info">告警阈值 85%</span>
    </div>
    <div class="room-list">
      <div v-if="rooms.length === 0" class="empty">暂无库房</div>
      <button
        v-for="r in rooms"
        :key="r.id"
        class="room-item"
        :class="{ active: modelValue === r.id }"
        @click="emit('update:modelValue', r.id)"
      >
        <span class="room-head">
          <span>
            <strong>{{ r.roomNo }} {{ r.roomName }}</strong><br>
            <span class="muted">{{ r.rackCount }} 个机架 · {{ r.layersPerRack }} 层 · 每层 {{ r.boxesPerLayer }} 盒位</span>
          </span>
          <span class="status" :class="r.warning ? 'warning' : 'success'">{{ r.warning ? '容量告警' : '启用' }}</span>
        </span>
        <span class="usage">
          <span class="usage-track"><span class="usage-fill" :class="{ warning: r.warning }" :style="{ width: Math.round(r.occupancyRate * 100) + '%' }"></span></span>
          <span class="usage-text"><span>已用 {{ r.occupiedSlots }} / {{ r.capacity }}</span><span>{{ Math.round(r.occupancyRate * 100) }}%</span></span>
        </span>
      </button>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { WarehouseRoom } from '@/types/warehouse'

defineProps<{ rooms: WarehouseRoom[]; modelValue: number | null }>()
const emit = defineEmits<{ (e: 'update:modelValue', id: number): void }>()
</script>

<style scoped>
.panel { padding: 14px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0; }
.room-list { display: grid; gap: 12px; }
.room-item { display: grid; gap: 10px; padding: 14px; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; text-align: left; cursor: pointer; }
.room-item:hover { background: #fafafa; }
.room-item.active { border-color: #8abcbf; box-shadow: 0 0 0 3px rgba(31, 111, 120, 0.1); }
.room-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; }
.usage { display: grid; gap: 6px; }
.usage-track { height: 9px; border-radius: 999px; background: #e5edf2; overflow: hidden; }
.usage-fill { display: block; height: 100%; background: var(--primary); }
.usage-fill.warning { background: var(--warning); }
.usage-text { display: flex; justify-content: space-between; color: var(--muted); font-size: 12px; font-weight: 650; }
.muted { color: var(--muted); font-size: 12px; }
.empty { color: var(--muted); font-size: 13px; padding: 12px; }
.status { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.status.success { background: #f6ffed; color: #52c41a; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.info { background: #e6f7ff; color: #1890ff; }
</style>
```

- [ ] **Step 2: 创建 `src/views/admin/warehouse/components/RackBoard.vue`**

```vue
<template>
  <div>
    <div class="rule-strip">
      <div>
        <strong>架位编码：库房号-机架号-层号-盒位号</strong>
        <div class="hint">示例：<span class="mono">401-03-02-05</span>。纯电子档案不占架位，纸质+电子和纯纸质必须记录盒号与架位。</div>
      </div>
      <span class="status info">管理员可见</span>
    </div>
    <div v-if="locations.length === 0" class="empty">该库房暂无架位数据</div>
    <div v-else class="rack-board">
      <div v-for="group in rackGroups" :key="group.rackNo" class="rack-row">
        <div class="rack-label">机架 {{ pad(group.rackNo) }}</div>
        <div class="slot-grid">
          <button
            v-for="loc in group.items"
            :key="loc.id"
            class="slot"
            :class="[slotClass(loc), { active: modelValue === loc.id }]"
            @click="emit('update:modelValue', loc.id)"
          >
            <strong>{{ loc.locationCode }}</strong>
            <span>{{ slotText(loc) }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { StorageLocation } from '@/types/warehouse'

const props = defineProps<{ locations: StorageLocation[]; modelValue: number | null }>()
const emit = defineEmits<{ (e: 'update:modelValue', id: number): void }>()

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

const rackGroups = computed(() => {
  const map = new Map<number, StorageLocation[]>()
  for (const l of props.locations) {
    if (!map.has(l.rackNo)) map.set(l.rackNo, [])
    map.get(l.rackNo)!.push(l)
  }
  return [...map.entries()]
    .sort((a, b) => a[0] - b[0])
    .map(([rackNo, items]) => ({
      rackNo,
      items: items.sort((a, b) => a.layerNo - b.layerNo || a.boxSlotNo - b.boxSlotNo),
    }))
})

function slotClass(loc: StorageLocation): string {
  if (loc.status === 'disabled') return 'disabled'
  if (loc.occupied) return 'occupied'
  return ''
}
function slotText(loc: StorageLocation): string {
  if (loc.status === 'disabled') return '停用'
  if (loc.occupied) return loc.currentBoxNo ?? ''
  return '空闲'
}
</script>

<style scoped>
.rule-strip { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 14px; padding: 12px 14px; border: 1px solid #b9d7d9; border-radius: var(--radius); background: var(--primary-soft); }
.hint { color: var(--muted); font-size: 12px; }
.mono { font-family: monospace; }
.rack-board { display: grid; gap: 12px; }
.rack-row { display: grid; grid-template-columns: 74px minmax(0, 1fr); gap: 10px; align-items: stretch; }
.rack-label { display: grid; place-items: center; min-height: 96px; border: 1px solid var(--border); border-radius: var(--radius); color: #34414d; background: #f7fafc; font-weight: 750; }
.slot-grid { display: grid; grid-template-columns: repeat(5, minmax(70px, 1fr)); gap: 8px; }
.slot { display: grid; min-height: 58px; align-content: center; gap: 3px; padding: 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); color: #34414d; background: #fff; text-align: left; cursor: pointer; }
.slot strong { font-size: 12px; }
.slot span { color: var(--muted); font-size: 11px; }
.slot:hover, .slot.active { border-color: #8abcbf; box-shadow: 0 4px 14px rgba(23, 33, 43, 0.08); }
.slot.occupied { border-color: #a9c9cc; background: #f2fbfb; }
.slot.disabled { color: var(--muted); background: #eef1f4; }
.empty { color: var(--muted); font-size: 13px; padding: 12px; }
.status { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.status.info { background: #e6f7ff; color: #1890ff; }
@media (max-width: 760px) { .slot-grid { grid-template-columns: repeat(2, 1fr); } .rack-row { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 3: 创建 `src/views/admin/warehouse/components/RecentBoxesTable.vue`**

```vue
<template>
  <section class="card panel">
    <div class="toolbar" style="margin-top: 0">
      <h2 class="section-title">最近占用盒位</h2>
    </div>
    <div v-if="boxes.length === 0" class="empty">暂无档案盒</div>
    <div v-else class="table-wrap">
      <table>
        <thead>
          <tr><th>库房号</th><th>位置编码</th><th>盒号</th><th>年度</th><th>盒内件数</th><th>状态</th></tr>
        </thead>
        <tbody>
          <tr v-for="b in boxes" :key="b.id">
            <td>{{ b.roomNo }}</td>
            <td class="mono">{{ b.locationCode }}</td>
            <td>{{ b.boxNo }}</td>
            <td>{{ b.yearLabel }}</td>
            <td>{{ b.usedCount }} / {{ b.capacity }}</td>
            <td><span class="status" :class="boxStatusClass(b.status)">{{ BoxStatusLabel[b.status] }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ArchiveBox } from '@/types/warehouse'
import { BoxStatusLabel } from '@/types/enums'

defineProps<{ boxes: ArchiveBox[] }>()

function boxStatusClass(s: string): string {
  return ({ normal: 'success', full: 'warning', moved: 'info', destroyed: 'danger' } as Record<string, string>)[s] ?? ''
}
</script>

<style scoped>
.panel { padding: 14px; }
.toolbar { margin-bottom: 10px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0; }
.table-wrap { overflow-x: auto; }
.table-wrap table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table-wrap th, .table-wrap td { padding: 8px 10px; text-align: left; border-bottom: 1px solid var(--border); }
.table-wrap th { font-weight: 700; color: var(--muted); background: var(--bg); }
.mono { font-family: monospace; font-size: 12px; }
.empty { color: var(--muted); font-size: 13px; padding: 12px; }
.status { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.status.success { background: #f6ffed; color: #52c41a; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.danger { background: #fff1f0; color: #f5222d; }
.status.info { background: #e6f7ff; color: #1890ff; }
</style>
```

- [ ] **Step 4: 类型检查通过**

Run: `npx vue-tsc -b`
Expected: 无错误（三个组件独立编译通过）。

- [ ] **Step 5: 提交**

```bash
git add src/views/admin/warehouse/components/RoomList.vue src/views/admin/warehouse/components/RackBoard.vue src/views/admin/warehouse/components/RecentBoxesTable.vue
git commit -m "feat(storage-guo): 新增库房列表架位看板盒位表展示组件"
```

## Task 11b: 库房交互型子组件（BoxDetailDrawer / CreateRoomDialog）

**Files:**
- Create: `src/views/admin/warehouse/components/BoxDetailDrawer.vue`
- Create: `src/views/admin/warehouse/components/CreateRoomDialog.vue`

> BoxDetailDrawer 自包含（内部调用 §16 API 处理新增盒/移动盒/停用），由 Task 12 的 `index.spec.ts` 间接覆盖。CreateRoomDialog 为纯表单弹窗。

- [ ] **Step 1: 创建 `src/views/admin/warehouse/components/BoxDetailDrawer.vue`**

```vue
<template>
  <aside class="drawer">
    <template v-if="!location"><p class="hint">点击架位查看详情与操作</p></template>
    <template v-else>
      <h2 class="section-title">盒位详情</h2>
      <ul class="detail-list">
        <li><span>位置编码</span><strong class="mono">{{ location.locationCode }}</strong></li>
        <li><span>架位状态</span><span>{{ location.status === 'active' ? '启用' : '停用' }}</span></li>
        <li><span>当前盒号</span><span class="mono">{{ location.occupied ? (location.currentBoxNo ?? '—') : '无' }}</span></li>
        <li><span>盒内件数</span><span>{{ location.boxItemCount ?? 0 }} 件</span></li>
      </ul>

      <template v-if="!location.occupied && location.status === 'active'">
        <h3 class="section-title">新增档案盒到此位</h3>
        <div class="field"><label>分类 ID</label><input v-model.number="form.categoryId" type="number"></div>
        <div class="field"><label>全宗 ID</label><input v-model.number="form.fondsId" type="number"></div>
        <div class="field"><label>年度</label><input v-model="form.yearLabel"></div>
        <div class="field"><label>盒脊信息</label><input v-model="form.spineText"></div>
        <div class="field"><label>容量</label><input v-model.number="form.capacity" type="number"></div>
        <div class="actions">
          <button class="button" @click="onCreateBox">新增档案盒</button>
          <button class="button ghost" @click="onDisable">停用架位</button>
        </div>
      </template>

      <template v-else-if="location.occupied">
        <div v-if="boxLoading" class="hint">盒详情加载中...</div>
        <template v-else-if="boxDetail">
          <h3 class="section-title">盒内档案（{{ boxDetail.items.length }} 件）</h3>
          <div v-if="boxDetail.items.length === 0" class="hint">盒内暂无档案条目</div>
          <ul v-else class="box-items">
            <li v-for="it in boxDetail.items" :key="it.archiveId">
              <span class="mono">{{ it.archiveNo }}</span> {{ it.title }}
              <span class="status" :class="physicalClass(it.physicalStatus)">{{ physicalLabel(it.physicalStatus) }}</span>
            </li>
          </ul>
        </template>
        <h3 class="section-title" style="margin-top:12px">移动档案盒</h3>
        <div class="field"><label>目标架位</label>
          <select v-model.number="moveTargetId">
            <option :value="0">请选择空闲架位</option>
            <option v-for="loc in freeLocations" :key="loc.id" :value="loc.id">{{ loc.locationCode }}</option>
          </select>
        </div>
        <div class="field"><label>移动原因</label><input v-model="moveReason"></div>
        <div class="actions">
          <button class="button" :disabled="!moveTargetId" @click="onMove">移动档案盒</button>
          <button class="button ghost" disabled title="已占用架位需先迁出档案盒">停用架位</button>
        </div>
        <p class="hint">已占用架位需迁出档案盒后才能停用。</p>
      </template>

      <template v-else>
        <p class="hint">该架位已停用。</p>
        <div class="actions"><button class="button ghost" @click="onEnable">启用架位</button></div>
      </template>
    </template>
  </aside>
</template>

<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { ArchiveBoxDetail, ArchiveBoxCreateData, StorageLocation } from '@/types/warehouse'
import { createArchiveBox, getArchiveBoxDetail, moveArchiveBox, updateLocationStatus } from '@/api/warehouse'

const props = defineProps<{ location: StorageLocation | null; freeLocations: StorageLocation[] }>()
const emit = defineEmits<{ (e: 'refresh'): void }>()

const boxDetail = ref<ArchiveBoxDetail | null>(null)
const boxLoading = ref(false)
const form = ref<ArchiveBoxCreateData>({ locationId: 0, categoryId: 1, fondsId: 1, yearLabel: '2026', spineText: '', capacity: 30 })
const moveTargetId = ref(0)
const moveReason = ref('')

watch(
  () => props.location,
  async (loc) => {
    boxDetail.value = null
    if (loc && loc.occupied && loc.currentBoxId) {
      boxLoading.value = true
      try {
        boxDetail.value = await getArchiveBoxDetail(loc.currentBoxId)
      } catch (e) {
        ElMessage.error(e instanceof Error ? e.message : '盒详情加载失败')
      } finally {
        boxLoading.value = false
      }
    }
    if (loc) {
      form.value.locationId = loc.id
      moveTargetId.value = 0
      moveReason.value = ''
    }
  },
)

async function onCreateBox() {
  if (!props.location) return
  if (!form.value.spineText.trim()) {
    ElMessage.warning('请填写盒脊信息')
    return
  }
  try {
    await createArchiveBox({ ...form.value, locationId: props.location.id })
    ElMessage.success('档案盒已新增，架位已占用。')
    emit('refresh')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '新增失败')
  }
}
async function onMove() {
  if (!props.location?.currentBoxId || !moveTargetId.value) return
  if (!moveReason.value.trim()) {
    ElMessage.warning('请填写移动原因')
    return
  }
  try {
    await moveArchiveBox(props.location.currentBoxId, { targetLocationId: moveTargetId.value, reason: moveReason.value })
    ElMessage.success('档案盒已移动到目标架位。')
    emit('refresh')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '移动失败')
  }
}
async function onDisable() {
  if (!props.location) return
  try {
    await updateLocationStatus(props.location.id, { status: 'disabled', reason: '维护停用' })
    ElMessage.success('架位已停用。')
    emit('refresh')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '停用失败')
  }
}
async function onEnable() {
  if (!props.location) return
  try {
    await updateLocationStatus(props.location.id, { status: 'active' })
    ElMessage.success('架位已启用。')
    emit('refresh')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '启用失败')
  }
}
function physicalLabel(s: string): string {
  return ({ normal: '正常', damaged: '破损', lost: '遗失' } as Record<string, string>)[s] ?? s
}
function physicalClass(s: string): string {
  return ({ normal: 'success', damaged: 'warning', lost: 'danger' } as Record<string, string>)[s] ?? ''
}
</script>

<style scoped>
.drawer { padding: 14px; background: #fff; border: 1px solid var(--border); border-radius: var(--radius); }
.section-title { font-size: 14px; font-weight: 700; margin: 0 0 8px; }
.hint { color: var(--muted); font-size: 12px; }
.detail-list { list-style: none; margin: 0 0 12px; padding: 0; display: grid; gap: 8px; }
.detail-list li { display: grid; grid-template-columns: 80px 1fr; gap: 8px; padding-bottom: 8px; border-bottom: 1px solid var(--border); font-size: 13px; }
.detail-list span:first-child { color: var(--muted); font-weight: 700; }
.field { display: grid; gap: 4px; margin-bottom: 8px; }
.field label { font-size: 12px; color: var(--muted); font-weight: 700; }
.field input, .field select { padding: 6px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 13px; }
.box-items { list-style: none; margin: 0 0 12px; padding: 0; display: grid; gap: 6px; max-height: 200px; overflow-y: auto; }
.box-items li { font-size: 12px; padding: 6px; border: 1px solid var(--border); border-radius: var(--radius-sm); }
.actions { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 8px; }
.button { padding: 6px 12px; border: 1px solid var(--primary); border-radius: var(--radius-sm); background: var(--primary); color: #fff; cursor: pointer; font-size: 12px; }
.button:disabled { opacity: 0.5; cursor: not-allowed; }
.button.ghost { background: #fff; color: var(--text); border-color: var(--border); }
.mono { font-family: monospace; font-size: 12px; }
.status { display: inline-block; padding: 1px 6px; border-radius: var(--radius-sm); font-size: 11px; font-weight: 600; }
.status.success { background: #f6ffed; color: #52c41a; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.danger { background: #fff1f0; color: #f5222d; }
</style>
```

- [ ] **Step 2: 创建 `src/views/admin/warehouse/components/CreateRoomDialog.vue`**

```vue
<template>
  <div v-if="modelValue" class="modal-backdrop" @click.self="emit('update:modelValue', false)">
    <div class="modal">
      <header>
        <h2 class="section-title">添加库房</h2>
        <button class="button ghost" @click="emit('update:modelValue', false)">关闭</button>
      </header>
      <div class="body">
        <div class="notice">库房提交后生成完整架位结构，编码由库房号、机架号、层号、盒位号组成。</div>
        <div class="form-grid">
          <div class="field"><label>库房号</label><input v-model="form.roomNo" autocomplete="off"></div>
          <div class="field"><label>库房名称</label><input v-model="form.roomName" autocomplete="off"></div>
          <div class="field"><label>机架数量</label><input v-model.number="form.rackCount" type="number" min="1"></div>
          <div class="field"><label>单机架层数</label><input v-model.number="form.layersPerRack" type="number" min="1"></div>
          <div class="field"><label>每层最大盒数</label><input v-model.number="form.boxesPerLayer" type="number" min="1"></div>
          <div class="field"><label>告警阈值</label>
            <select v-model.number="form.warningThreshold">
              <option :value="0.85">85%</option>
              <option :value="0.9">90%</option>
              <option :value="0.8">80%</option>
            </select>
          </div>
        </div>
        <p class="hint">预计生成 {{ capacity }} 个盒位，示例编码：{{ form.roomNo || '库房号' }}-01-01-01。</p>
      </div>
      <footer>
        <button class="button ghost" @click="emit('update:modelValue', false)">取消</button>
        <button class="button" @click="onSubmit">生成架位</button>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { WarehouseRoomCreateData } from '@/types/warehouse'

defineProps<{ modelValue: boolean }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void; (e: 'create', data: WarehouseRoomCreateData): void }>()

const form = ref<WarehouseRoomCreateData>({ roomNo: '', roomName: '', rackCount: 4, layersPerRack: 4, boxesPerLayer: 8, warningThreshold: 0.85 })
const capacity = computed(() => form.value.rackCount * form.value.layersPerRack * form.value.boxesPerLayer)

function onSubmit() {
  if (!form.value.roomNo.trim()) {
    ElMessage.warning('请填写库房号')
    return
  }
  if (!form.value.roomName.trim()) {
    ElMessage.warning('请填写库房名称')
    return
  }
  if (capacity.value <= 0) {
    ElMessage.warning('容量参数必须大于 0')
    return
  }
  emit('create', { ...form.value })
  form.value = { roomNo: '', roomName: '', rackCount: 4, layersPerRack: 4, boxesPerLayer: 8, warningThreshold: 0.85 }
}
</script>

<style scoped>
.modal-backdrop { position: fixed; inset: 0; z-index: 30; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(23, 33, 43, 0.38); }
.modal { width: min(720px, 100%); max-height: calc(100vh - 40px); overflow-y: auto; border-radius: var(--radius); background: #fff; box-shadow: var(--shadow); }
.modal header, .modal footer { display: flex; justify-content: space-between; align-items: center; padding: 14px 18px; border-bottom: 1px solid var(--border); }
.modal footer { border-top: 1px solid var(--border); border-bottom: 0; }
.modal .body { padding: 18px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0; }
.notice { padding: 9px 12px; border-radius: var(--radius-sm); background: var(--primary-soft); font-size: 12px; color: #1f6f78; margin-bottom: 14px; }
.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.field { display: grid; gap: 4px; }
.field label { font-size: 12px; color: var(--muted); font-weight: 700; }
.field input, .field select { padding: 6px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 13px; }
.hint { color: var(--muted); font-size: 12px; margin-top: 10px; }
.button { padding: 6px 14px; border: 1px solid var(--primary); border-radius: var(--radius-sm); background: var(--primary); color: #fff; cursor: pointer; font-size: 13px; }
.button.ghost { background: #fff; color: var(--text); border-color: var(--border); }
</style>
```

- [ ] **Step 3: 类型检查通过**

Run: `npx vue-tsc -b`
Expected: 无错误。

- [ ] **Step 4: 提交**

```bash
git add src/views/admin/warehouse/components/BoxDetailDrawer.vue src/views/admin/warehouse/components/CreateRoomDialog.vue
git commit -m "feat(storage-guo): 新增盒位详情抽屉与添加库房弹窗组件"
```

## Task 12: 库房页面组装（`src/views/admin/warehouse/index.vue`）

**Files:**
- Rewrite: `src/views/admin/warehouse/index.vue`
- Test: `src/views/admin/warehouse/index.spec.ts`

- [ ] **Step 1: 写失败测试 `src/views/admin/warehouse/index.spec.ts`**

```typescript
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import Warehouse from './index.vue'

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 80))
  await flushPromises()
}

async function mountComponent() {
  const router = createRouter({
    history: createMemoryHistory(),
    routes: [{ path: '/admin/warehouse', component: Warehouse }],
  })
  await router.push('/admin/warehouse')
  await router.isReady()
  return mount(Warehouse, { global: { plugins: [router] } })
}

describe('Warehouse', () => {
  it('renders room list with warning room', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    expect(wrapper.text()).toContain('综合档案库房')
    expect(wrapper.text()).toContain('401')
    expect(wrapper.text()).toContain('容量告警')
  })

  it('loads locations and renders slots after room select', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    await waitForAsyncData()
    expect(wrapper.text()).toContain('架位编码')
    expect(wrapper.findAll('.slot').length).toBeGreaterThan(0)
  })

  it('opens create room dialog', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    const btn = wrapper.findAll('button').find((b) => b.text().includes('添加库房'))
    await btn?.trigger('click')
    expect(wrapper.text()).toContain('添加库房')
    expect(wrapper.find('.modal').exists()).toBe(true)
  })

  it('renders recent boxes table', async () => {
    const wrapper = await mountComponent()
    await waitForAsyncData()
    expect(wrapper.text()).toContain('最近占用盒位')
    expect(wrapper.text()).toContain('BX-2026-001')
  })
})
```

- [ ] **Step 2: 运行测试，确认失败**

Run: `npx vitest run src/views/admin/warehouse/index.spec.ts`
Expected: FAIL（页面仍是占位）。

- [ ] **Step 3: 重写 `src/views/admin/warehouse/index.vue`**

```vue
<template>
  <div class="warehouse">
    <section class="hero-line">
      <div>
        <h1 class="page-title">库房管理</h1>
        <p class="page-subtitle">维护库房房间、机架、层、盒位和档案盒占用状态，管理员可见完整架位编码。</p>
      </div>
      <button class="button" @click="roomDialogVisible = true">+ 添加库房</button>
    </section>

    <section class="metric-row">
      <div class="metric card"><div class="metric-num">{{ metrics.activeRooms }}</div><div class="metric-label">启用库房</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.totalCapacity }}</div><div class="metric-label">总盒位</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.totalOccupied }}</div><div class="metric-label">已占用盒位</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.warningRooms }}</div><div class="metric-label">容量告警</div></div>
    </section>

    <section class="workspace">
      <RoomList :rooms="rooms" v-model="selectedRoomId" />
      <section class="rack-area">
        <div class="filters">
          <div class="field"><label>机架</label>
            <select v-model.number="rackFilter">
              <option :value="0">全部机架</option>
              <option v-for="r in rackOptions" :key="r" :value="r">机架 {{ pad(r) }}</option>
            </select>
          </div>
          <div class="field"><label>架位状态</label>
            <select v-model="statusFilter">
              <option value="">全部状态</option>
              <option value="free">空闲</option>
              <option value="occupied">已占用</option>
              <option value="disabled">停用</option>
            </select>
          </div>
        </div>

        <div v-if="locLoading" class="empty">架位加载中...</div>
        <div v-else-if="locError" class="empty">架位加载失败：<button class="link" @click="loadLocations">重试</button></div>
        <div v-else class="location-layout">
          <RackBoard :locations="filteredLocations" v-model="selectedLocationId" />
          <BoxDetailDrawer :location="selectedLocation" :free-locations="freeLocations" @refresh="onRefresh" />
        </div>
      </section>
    </section>

    <div style="margin-top: 16px">
      <RecentBoxesTable :boxes="recentBoxes" />
    </div>

    <CreateRoomDialog v-model="roomDialogVisible" @create="onCreateRoom" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { ArchiveBox, StorageLocation, WarehouseRoom, WarehouseRoomCreateData } from '@/types/warehouse'
import { createWarehouseRoom, getArchiveBoxes, getStorageLocations, getWarehouseRooms } from '@/api/warehouse'
import RoomList from './components/RoomList.vue'
import RackBoard from './components/RackBoard.vue'
import BoxDetailDrawer from './components/BoxDetailDrawer.vue'
import CreateRoomDialog from './components/CreateRoomDialog.vue'
import RecentBoxesTable from './components/RecentBoxesTable.vue'

const rooms = ref<WarehouseRoom[]>([])
const selectedRoomId = ref<number | null>(null)
const locations = ref<StorageLocation[]>([])
const locLoading = ref(false)
const locError = ref(false)
const selectedLocationId = ref<number | null>(null)
const recentBoxes = ref<ArchiveBox[]>([])
const roomDialogVisible = ref(false)
const rackFilter = ref(0)
const statusFilter = ref<'' | 'free' | 'occupied' | 'disabled'>('')

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

const metrics = computed(() => {
  const active = rooms.value.filter((r) => r.status === 'active')
  return {
    activeRooms: active.length,
    totalCapacity: active.reduce((s, r) => s + r.capacity, 0),
    totalOccupied: active.reduce((s, r) => s + r.occupiedSlots, 0),
    warningRooms: active.filter((r) => r.warning).length,
  }
})

const rackOptions = computed(() => [...new Set(locations.value.map((l) => l.rackNo))].sort((a, b) => a - b))

const filteredLocations = computed(() => {
  let list = locations.value
  if (rackFilter.value) list = list.filter((l) => l.rackNo === rackFilter.value)
  if (statusFilter.value === 'free') list = list.filter((l) => !l.occupied && l.status === 'active')
  else if (statusFilter.value === 'occupied') list = list.filter((l) => l.occupied)
  else if (statusFilter.value === 'disabled') list = list.filter((l) => l.status === 'disabled')
  return list
})

const selectedLocation = computed(() => locations.value.find((l) => l.id === selectedLocationId.value) ?? null)
const freeLocations = computed(() => filteredLocations.value.filter((l) => !l.occupied && l.status === 'active'))

async function loadRooms() {
  try {
    rooms.value = await getWarehouseRooms()
    if (!selectedRoomId.value && rooms.value.length) selectedRoomId.value = rooms.value[0].id
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '库房加载失败')
  }
}

async function loadLocations() {
  if (!selectedRoomId.value) return
  locLoading.value = true
  locError.value = false
  try {
    const page = await getStorageLocations({ roomId: selectedRoomId.value, pageSize: 500 })
    locations.value = page.records
    selectedLocationId.value = null
  } catch {
    locError.value = true
  } finally {
    locLoading.value = false
  }
}

async function loadRecentBoxes() {
  try {
    const page = await getArchiveBoxes({ pageSize: 10 })
    recentBoxes.value = page.records
  } catch {
    recentBoxes.value = []
  }
}

async function onCreateRoom(data: WarehouseRoomCreateData) {
  try {
    const room = await createWarehouseRoom(data)
    roomDialogVisible.value = false
    ElMessage.success(`库房 ${room.roomNo} 已创建，生成 ${room.capacity} 个架位。`)
    await loadRooms()
    selectedRoomId.value = room.id
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  }
}

async function onRefresh() {
  await loadLocations()
  await loadRooms()
  await loadRecentBoxes()
}

watch(selectedRoomId, () => {
  loadLocations()
})

onMounted(async () => {
  await loadRooms()
  await loadRecentBoxes()
})
</script>

<style scoped>
.warehouse { padding: 0; }
.hero-line { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { color: var(--muted); font-size: 13px; margin: 0; }
.metric-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }
.metric { padding: 16px; text-align: center; }
.metric-num { font-size: 26px; font-weight: 700; color: var(--primary); }
.metric-label { font-size: 13px; color: var(--muted); margin-top: 4px; }
.workspace { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 16px; align-items: start; }
.rack-area { display: grid; gap: 12px; }
.filters { display: flex; gap: 12px; flex-wrap: wrap; }
.field { display: grid; gap: 4px; }
.field label { font-size: 12px; color: var(--muted); font-weight: 700; }
.field select { padding: 6px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 13px; min-width: 140px; }
.location-layout { display: grid; grid-template-columns: minmax(0, 1fr) 300px; gap: 16px; align-items: start; }
.empty { display: flex; align-items: center; justify-content: center; height: 200px; color: var(--muted); font-size: 14px; }
.link { background: none; border: none; color: var(--primary); cursor: pointer; font-size: 13px; text-decoration: underline; padding: 0; }
.button { padding: 6px 14px; border: 1px solid var(--primary); border-radius: var(--radius-sm); background: var(--primary); color: #fff; cursor: pointer; font-size: 13px; }
@media (max-width: 1180px) { .metric-row, .workspace, .location-layout { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 4: 运行测试，确认通过**

Run: `npx vitest run src/views/admin/warehouse/index.spec.ts`
Expected: PASS（4 个用例全过）。

- [ ] **Step 5: 类型检查通过**

Run: `npx vue-tsc -b`
Expected: 无错误。

- [ ] **Step 6: 提交**

```bash
git add src/views/admin/warehouse/index.vue src/views/admin/warehouse/index.spec.ts
git commit -m "feat(storage-guo): 实现库房与架位管理页面"
```

## Task 13: 全量验证与交付

**Files:** 无新增（验证既有改动）

- [ ] **Step 1: 全量类型检查**

Run: `npx vue-tsc -b`
Expected: 无错误（杜绝 archive-hu 式类型错误阻塞集成）。

- [ ] **Step 2: 全量单元测试**

Run: `npm run test:unit -- run`
Expected: 全部通过（本轮新增 4 个 api spec + 1 个 utils spec + 3 个页面 spec，既有测试不回归）。

- [ ] **Step 3: 生产构建**

Run: `npm run build`
Expected: 构建成功（`vue-tsc -b && vite build` 均通过）。

- [ ] **Step 4: 启动 dev 手动抽检（可选但推荐）**

Run: `npm run dev`，浏览器依次进入 `/admin/warehouse`、`/admin/inventory`、`/admin/borrow-approval`，验证：
- 库房页：库房列表 + 架位看板四态 + 空闲架位新增盒 + 占用架位移动盒 + 已占用架位停用被拦截 + 添加库房。
- 盘点页：任务列表 + 选中明细 + 新建任务 + 进行中逐行核对 + 提交盘点。
- 借阅页：申请列表 + 命中盘点禁用通过 + 拒绝必填 + 出库/归还状态流转 + 导出凭证。

- [ ] **Step 5: 推送分支并发起 PR**

```bash
git push -u origin feat/storage-guo
```

随后用 `gh pr create`（郭一坤身份）发起 PR，标题 `feat(storage-guo): 库房盘点借阅审批页面实现`，base `develop`，等待方江苏 review。PR body 引用 spec 与本 plan。

> 服务生命周期提醒：dev server 验证完毕后关闭，不留僵尸进程。

---

## 自查（Self-Review）

### Spec 覆盖

| Spec 章节 | 对应 Task |
|-----------|-----------|
| §6 枚举扩展 | Task 1 |
| §7.1 warehouse 类型 | Task 2 |
| §7.2 inventory 类型 | Task 3 |
| §7.3 borrow-approval 类型 | Task 4 |
| §8.1 warehouse API（§16） | Task 5 |
| §8.2 inventory API（§18） | Task 6 |
| §8.3 borrow-approval API（§12） | Task 7 |
| §9.3 borrow-approval 页面 | Task 9 |
| §9.2 inventory 页面 | Task 10 |
| §9.1 warehouse 页面（5 子组件 + 组装） | Task 11a / 11b / 12 |
| §11 借阅审批校验 | Task 8 |
| §12 mock 数据 | Task 5 / 6 / 7 |
| §13 测试与验证 | 各 Task 内 spec + Task 13 |
| §10 跨页联动（盘点命中→借阅拦截） | Task 7 mock `inventoryHit` + Task 9 前端拦截 |

无遗漏章节。

### 占位扫描

全文无 `TODO` / `TBD` / `...省略` / "类似上方"。每个 Task 的代码块均为完整可运行实现（types / mock / api / utils / vue / spec）。组件 `style` 块均为完整 scoped 样式。

### 类型一致性

- 枚举值跨文件一致：`WarehouseRoomStatus`/`LocationStatus`（active/disabled）、`BoxStatus`（normal/full/moved/destroyed）、`InventoryTaskStatus`（draft/running/completed）、`InventoryCheckResult`（normal/missing/misplaced/damaged/on_loan）、`ReturnCheckResult`（normal/damaged/missing_page/other）在 enums.ts 定义后被 types/api/mock/vue 统一引用。
- API 函数名跨 Task 一致：`getWarehouseRooms`/`createArchiveBox`/`moveArchiveBox`/`getInventoryTaskDetail`/`approveBorrowRequest`/`checkoutBorrowRequest`/`returnBorrowRequest`/`exportBorrowVoucher` 在 api 定义后被页面一致调用。
- mock 函数名与 api 内 `import('@/mock/modules/xxx').then(m => m.mockXxx)` 一一对应。
- `BorrowApprovalDetail extends BorrowRequestDetail`（Task 4）在 mock（Task 7）与页面（Task 9）中字段使用一致；`inventoryHit`/`checkInventory` 等扩展字段定义与消费一致。

### 风险与备注

- **库房后端已就绪但本轮走 mock**：`VITE_USE_MOCK=true` 下 mock 字段对齐后端 DTO；联调时切 `false` 即对接 `WarehouseController`，组件不改。
- **盘点/借阅后端待补**：`feat/stats-liu`（盘点）、`feat/borrow-liu`（借阅审批/出库/归还）合并前走 mock；`exportBorrowVoucher` 真实端点需 borrow-liu 补 `POST /admin/borrow-requests/{id}/voucher`。
- **盘点明细全量加载**：mock 下每库房架位 ≤ 500，`getStorageLocations({ pageSize: 500 })` 一次拉取；真实环境若库房架位远超 500，需改为分页或后端补聚合接口（本期不处理）。
- **组件测试异步等待**：页面 spec 用 `waitForAsyncData`（80ms + flushPromises），mock 下足够；若 CI 慢机 flaky，调大等待。

