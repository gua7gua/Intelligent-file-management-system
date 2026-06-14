# 档案鉴定·销毁·审批工作台 实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现管理后台「档案鉴定 / 档案销毁 / 审批工作台」三页，覆盖鉴定→销毁→审批处置链，全程 mock 开发，后端就绪后切 `VITE_USE_MOCK=false` 即对接。

**Architecture:** 三个独立子域（appraisal / destruction / approval）各自 types+api+mock；视图沿用 AdminLayout 单页左右布局；审批工作台按 approvalType 拆 ArchiveAdjustDetail / DestructionApprovalDetail 两个子组件；销毁确认抽 DestroyConfirmDialog。字段严格对齐 `doc/接口文档.md` §13/14/15 与 `doc/数据库设计.md` §8.1–8.5。

**Tech Stack:** Vue 3 `<script setup>` + TypeScript + vue-router + Element Plus + Vitest + @vue/test-utils，mock 经 `VITE_USE_MOCK` 切换。

**对应设计文档：** `frontend/docs/superpowers/specs/2026-06-15-appraisal-hu-design.md`

**验证脚本（项目无独立 type-check，build 即含 vue-tsc 类型检查）：**
- `npm run build` = `vue-tsc -b && vite build`（类型检查 + 生产构建）
- `npm run test:unit` = `vitest`（单元测试）

---

## 文件结构

| 文件 | 职责 | 动作 |
|------|------|------|
| `frontend/src/types/enums.ts` | 扩展鉴定/销毁/审批枚举与中文映射 + 派生值类型 | 修改（追加） |
| `frontend/src/types/appraisal.ts` | 鉴定批次/明细/创建/查询类型 | 新建 |
| `frontend/src/types/destruction.ts` | 销毁清册/明细快照/确认/查询类型 | 新建 |
| `frontend/src/types/approval.ts` | 审批单/详情/查询类型 | 新建 |
| `frontend/src/mock/modules/appraisal.ts` | 鉴定 mock 数据 | 新建 |
| `frontend/src/mock/modules/destruction.ts` | 销毁 mock 数据 | 新建 |
| `frontend/src/mock/modules/approval.ts` | 审批 mock 数据 | 新建 |
| `frontend/src/api/appraisal.ts` | §14 五端点 | 新建 |
| `frontend/src/api/appraisal.spec.ts` | 鉴定 API mock 契约测试 | 新建 |
| `frontend/src/api/destruction.ts` | §15 五端点 | 新建 |
| `frontend/src/api/destruction.spec.ts` | 销毁 API mock 契约测试 | 新建 |
| `frontend/src/api/approval.ts` | §13 四端点 | 新建 |
| `frontend/src/api/approval.spec.ts` | 审批 API mock 契约测试 | 新建 |
| `frontend/src/utils/appraisalValidation.ts` | 鉴定明细校验 | 新建 |
| `frontend/src/utils/appraisalValidation.spec.ts` | 鉴定校验单测 | 新建 |
| `frontend/src/utils/destructionValidation.ts` | 销毁确认校验 | 新建 |
| `frontend/src/utils/destructionValidation.spec.ts` | 销毁校验单测 | 新建 |
| `frontend/src/views/admin/appraisal/index.vue` | 档案鉴定页（重写占位） | 修改 |
| `frontend/src/views/admin/destruction/index.vue` | 档案销毁页（重写占位） | 修改 |
| `frontend/src/views/admin/destruction/components/DestroyConfirmDialog.vue` | 销毁确认弹窗 | 新建 |
| `frontend/src/views/admin/approval/index.vue` | 审批工作台（重写占位） | 修改 |
| `frontend/src/views/admin/approval/components/ArchiveAdjustDetail.vue` | 密级/开放审批详情 | 新建 |
| `frontend/src/views/admin/approval/components/DestructionApprovalDetail.vue` | 销毁审批详情 | 新建 |

依赖顺序：枚举/类型（Task 1–4）→ mock（5–7）→ api+spec（8–10）→ 校验+spec（11–12）→ 视图（13–16）→ 全量验证（17）。

---

## Task 1: 扩展枚举（types/enums.ts）

**Files:**
- Modify: `frontend/src/types/enums.ts`（在文件末尾追加）

- [ ] **Step 1: 追加枚举常量、派生值类型与中文映射**

在 `frontend/src/types/enums.ts` 末尾追加：

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

// 派生值类型
export type AppraisalResultValue = (typeof AppraisalResult)[keyof typeof AppraisalResult]
export type AppraisalBatchStatusValue = (typeof AppraisalBatchStatus)[keyof typeof AppraisalBatchStatus]
export type DestructionListStatusValue = (typeof DestructionListStatus)[keyof typeof DestructionListStatus]
export type DestroyMethodValue = (typeof DestroyMethod)[keyof typeof DestroyMethod]
export type FileDeleteStatusValue = (typeof FileDeleteStatus)[keyof typeof FileDeleteStatus]
export type ApprovalTypeValue = (typeof ApprovalType)[keyof typeof ApprovalType]
export type ApprovalTargetTypeValue = (typeof ApprovalTargetType)[keyof typeof ApprovalTargetType]
export type ApprovalStatusValue = (typeof ApprovalStatus)[keyof typeof ApprovalStatus]
export type SecurityLevelValue = number
export type RetentionPeriodValue = (typeof RetentionPeriod)[keyof typeof RetentionPeriod]
export type ArchiveLifecycleStatusValue = (typeof ArchiveLifecycleStatus)[keyof typeof ArchiveLifecycleStatus]

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

- [ ] **Step 2: 类型检查通过**

Run: `npm run build`
Expected: 构建成功（vue-tsc 无错误）。

- [ ] **Step 3: 提交**

```bash
git add frontend/src/types/enums.ts
git commit -m "feat(appraisal-hu): 扩展鉴定销毁审批枚举与中文映射"
```

---

## Task 2: 鉴定类型（types/appraisal.ts）

**Files:**
- Create: `frontend/src/types/appraisal.ts`

- [ ] **Step 1: 创建类型文件**

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

- [ ] **Step 2: 提交**

```bash
git add frontend/src/types/appraisal.ts
git commit -m "feat(appraisal-hu): 新增鉴定批次类型定义"
```

---

## Task 3: 审批类型（types/approval.ts）

> 先建 approval.ts，因其被 destruction.ts 引用（destruction 详情内嵌审批单摘要）。两个文件相互 `import type`（仅类型，编译期擦除，无运行时循环依赖问题）。

**Files:**
- Create: `frontend/src/types/approval.ts`

- [ ] **Step 1: 创建类型文件**

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
  targetArchiveNo?: string
  targetArchiveTitle?: string
  targetListNo?: string
  targetListName?: string
}

/** 审批单详情（§13.2） */
export interface ApprovalRequestDetail extends ApprovalRequest {
  targetArchive?: ApprovalArchiveSummary
  evidenceArchive?: ApprovalArchiveSummary
  evidenceMatched?: boolean
  destructionList?: ApprovalDestructionListSummary
}

/** 审批意见请求（§13.3 / §13.4） */
export interface ApprovalOpinionData {
  opinion: string
}

export type ApprovalRequestPage = PageData<ApprovalRequest>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/types/approval.ts
git commit -m "feat(appraisal-hu): 新增审批工作台类型定义"
```

---

## Task 4: 销毁类型（types/destruction.ts）

**Files:**
- Create: `frontend/src/types/destruction.ts`

- [ ] **Step 1: 创建类型文件**

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

/** 销毁清册附件（现场照片） */
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
  photoIds?: number[]
}

export type DestructionListPage = PageData<DestructionList>
```

- [ ] **Step 2: 类型检查**

Run: `npm run build`
Expected: 成功（types 互引为类型，编译期擦除）。

- [ ] **Step 3: 提交**

```bash
git add frontend/src/types/destruction.ts
git commit -m "feat(appraisal-hu): 新增销毁清册类型定义"
```

---

## Task 5: 鉴定 mock（mock/modules/appraisal.ts）

**Files:**
- Create: `frontend/src/mock/modules/appraisal.ts`

- [ ] **Step 1: 创建 mock 数据与函数**

```typescript
// src/mock/modules/appraisal.ts
import type {
  AppraisalBatch,
  AppraisalBatchCreateData,
  AppraisalBatchDetail,
  AppraisalBatchParams,
  AppraisalItem,
  AppraisalItemsSaveData,
} from '@/types/appraisal'

const baseItem = (
  archiveId: number,
  archiveNo: string,
  title: string,
  result: AppraisalItem['appraisalResult'],
): AppraisalItem => ({
  archiveId,
  archiveNo,
  title,
  categoryName: '会计档案',
  retentionPeriod: '10y',
  retentionUntil: '2025-12-31',
  currentLifecycleStatus: 'normal',
  appraisalResult: result,
})

// 批次 1：草稿，含未处理 + 已鉴定项
const batch1Items: AppraisalItem[] = [
  {
    ...baseItem(201, 'ARC-000201', '2015 年 1 月会计凭证', 'destroy'),
    opinion: '已满十年且无继续保存价值',
  },
  {
    ...baseItem(202, 'ARC-000202', '2015 年 2 月会计凭证', 'extend'),
    newRetentionPeriod: '30y',
    newRetentionUntil: '2045-12-31',
    opinion: '仍有查考价值，延长保管',
  },
  baseItem(203, 'ARC-000203', '2015 年 3 月会计凭证', ''),
  baseItem(204, 'ARC-000204', '2015 年 4 月会计凭证', ''),
]

// 批次 2：已完成，已生成销毁清册（id=13）
const batch2Items: AppraisalItem[] = [
  { ...baseItem(301, 'ARC-000301', '2014 年基建档案', 'destroy'), opinion: '到期无保存价值' },
  { ...baseItem(302, 'ARC-000302', '2014 年设备档案', 'extend'), newRetentionPeriod: 'permanent', newRetentionUntil: '2099-12-31', opinion: '设备仍在用' },
]

// 批次 3：草稿，部分命中
const batch3Items: AppraisalItem[] = [
  baseItem(401, 'ARC-000401', '2013 年人事档案', 'destroy'),
  baseItem(402, 'ARC-000402', '2013 年会议纪要', ''),
]

const batches: AppraisalBatchDetail[] = [
  {
    id: 1,
    batchNo: 'APP-001',
    batchName: '2015 年会计档案到期鉴定',
    categoryId: 3,
    categoryName: '会计档案',
    formedYearStart: 2015,
    formedYearEnd: 2015,
    status: 'draft',
    hitCount: 4,
    destroyCount: 1,
    extendCount: 1,
    createdAt: '2026-06-15T09:00:00+08:00',
    items: batch1Items,
  },
  {
    id: 2,
    batchNo: 'APP-002',
    batchName: '2014 年基建与设备档案鉴定',
    categoryId: 4,
    categoryName: '基建档案',
    formedYearStart: 2014,
    formedYearEnd: 2014,
    status: 'completed',
    hitCount: 2,
    destroyCount: 1,
    extendCount: 1,
    completedAt: '2026-06-14T16:00:00+08:00',
    createdAt: '2026-06-13T10:00:00+08:00',
    items: batch2Items,
    generatedListId: 13,
    generatedListNo: 'DES-013',
  },
  {
    id: 3,
    batchNo: 'APP-003',
    batchName: '2013 年到期档案鉴定',
    categoryId: 5,
    categoryName: '人事档案',
    formedYearStart: 2013,
    formedYearEnd: 2013,
    status: 'draft',
    hitCount: 2,
    destroyCount: 1,
    extendCount: 0,
    createdAt: '2026-06-15T14:00:00+08:00',
    items: batch3Items,
  },
]

export function mockAppraisalBatches(params?: AppraisalBatchParams): {
  records: AppraisalBatch[]
  pageNo: number
  pageSize: number
  total: number
  hasNext: boolean
} {
  let list = batches.slice()
  if (params?.status) list = list.filter((b) => b.status === params.status)
  const pageNo = params?.pageNo ?? 1
  const pageSize = params?.pageSize ?? 20
  const records = list.map(({ items, generatedListId, generatedListNo, ...rest }) => ({
    ...rest,
    generatedListId,
    generatedListNo,
  }))
  return {
    records,
    pageNo,
    pageSize,
    total: list.length,
    hasNext: false,
  }
}

export function mockAppraisalBatchDetail(id: number): AppraisalBatchDetail {
  const found = batches.find((b) => b.id === id)
  if (!found) throw new Error('鉴定批次不存在')
  return JSON.parse(JSON.stringify(found))
}

let nextBatchNo = 4
export function mockCreateAppraisalBatch(data: AppraisalBatchCreateData): AppraisalBatchDetail {
  const id = nextBatchNo++
  const detail: AppraisalBatchDetail = {
    id,
    batchNo: `APP-00${id}`,
    batchName: data.batchName,
    categoryId: data.categoryId,
    categoryName: data.categoryId === 3 ? '会计档案' : '未分类',
    formedYearStart: data.formedYearStart,
    formedYearEnd: data.formedYearEnd,
    status: 'draft',
    hitCount: 3,
    destroyCount: 0,
    extendCount: 0,
    createdAt: '2026-06-15T15:00:00+08:00',
    items: [
      baseItem(501, 'ARC-000501', `${data.formedYearStart ?? ''} 年待鉴定档案一`, ''),
      baseItem(502, 'ARC-000502', `${data.formedYearStart ?? ''} 年待鉴定档案二`, ''),
      baseItem(503, 'ARC-000503', `${data.formedYearStart ?? ''} 年待鉴定档案三`, ''),
    ],
  }
  return detail
}

export function mockSaveAppraisalItems(id: number, data: AppraisalItemsSaveData): AppraisalBatchDetail {
  const detail = mockAppraisalBatchDetail(id)
  const map = new Map(data.items.map((i) => [i.archiveId, i]))
  detail.items = detail.items.map((it) => {
    const patch = map.get(it.archiveId)
    if (!patch) return it
    return {
      ...it,
      appraisalResult: patch.appraisalResult,
      newRetentionPeriod: patch.newRetentionPeriod ?? it.newRetentionPeriod,
      newRetentionUntil: patch.newRetentionUntil ?? it.newRetentionUntil,
      opinion: patch.opinion ?? it.opinion,
    }
  })
  detail.destroyCount = detail.items.filter((i) => i.appraisalResult === 'destroy').length
  detail.extendCount = detail.items.filter((i) => i.appraisalResult === 'extend').length
  return detail
}

let nextListNo = 14
export function mockCompleteAppraisalBatch(id: number): AppraisalBatchDetail {
  const detail = mockAppraisalBatchDetail(id)
  detail.status = 'completed'
  detail.completedAt = '2026-06-15T17:00:00+08:00'
  detail.generatedListId = nextListNo
  detail.generatedListNo = `DES-0${nextListNo}`
  nextListNo++
  return detail
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/mock/modules/appraisal.ts
git commit -m "feat(appraisal-hu): 新增鉴定 mock 数据"
```

---

## Task 6: 审批 mock（mock/modules/approval.ts）

> 先建 approval mock，因 destruction mock 详情内嵌审批单摘要会引用审批数据；两者通过模块内独立数据保持联动一致。

**Files:**
- Create: `frontend/src/mock/modules/approval.ts`

- [ ] **Step 1: 创建 mock 数据与函数**

```typescript
// src/mock/modules/approval.ts
import type {
  ApprovalDestructionListSummary,
  ApprovalOpinionData,
  ApprovalParams,
  ApprovalRequest,
  ApprovalRequestDetail,
} from '@/types/approval'
import type { DestructionItem } from '@/types/destruction'

const destructionItems: DestructionItem[] = [
  {
    id: 9001,
    archiveId: 301,
    archiveNoSnapshot: 'ARC-000301',
    titleSnapshot: '2014 年基建档案',
    categorySnapshot: '基建档案',
    pageCountSnapshot: 120,
    retentionSnapshot: '10y',
    securityLevelSnapshot: 0,
    appraisalOpinionSnapshot: '到期无保存价值',
    fileDeleteStatus: 'not_started',
  },
]

const approvals: ApprovalRequestDetail[] = [
  {
    id: 20,
    approvalType: 'security_adjust',
    targetType: 'archive',
    targetId: 101,
    evidenceArchiveId: 102,
    oldValue: '0',
    newValue: '2',
    reason: '凭证档案为机密，目标档案应同步调整',
    status: 'pending',
    submittedBy: 2,
    submittedByName: '周扬',
    submittedAt: '2026-06-15T10:00:00+08:00',
    targetArchiveNo: 'ARC-000101',
    targetArchiveTitle: '2024 年城市规划文本',
    targetArchive: {
      id: 101,
      archiveNo: 'ARC-000101',
      title: '2024 年城市规划文本',
      categoryName: '文书档案',
      organizationName: '克拉玛依市自然资源局',
      fondsName: '自然资源局全宗',
      securityLevel: 0,
      lifecycleStatus: 'normal',
    },
    evidenceArchive: {
      id: 102,
      archiveNo: 'ARC-000102',
      title: '涉密区域规划底图',
      categoryName: '文书档案',
      organizationName: '克拉玛依市自然资源局',
      fondsName: '自然资源局全宗',
      securityLevel: 2,
      lifecycleStatus: 'normal',
    },
    evidenceMatched: true,
  },
  {
    id: 21,
    approvalType: 'open_adjust',
    targetType: 'archive',
    targetId: 103,
    evidenceArchiveId: 104,
    oldValue: 'open',
    newValue: 'closed',
    reason: '公开范围复核，调整为不公开',
    status: 'pending',
    submittedBy: 2,
    submittedByName: '周扬',
    submittedAt: '2026-06-15T10:30:00+08:00',
    targetArchiveNo: 'ARC-000103',
    targetArchiveTitle: '2023 年信访处理记录',
    targetArchive: {
      id: 103,
      archiveNo: 'ARC-000103',
      title: '2023 年信访处理记录',
      categoryName: '文书档案',
      organizationName: '克拉玛依市信访局',
      securityLevel: 0,
      lifecycleStatus: 'normal',
    },
    evidenceArchive: {
      id: 104,
      archiveNo: 'ARC-000104',
      title: '其他单位人事档案',
      categoryName: '人事档案',
      organizationName: '克拉玛依市人社局',
      securityLevel: 0,
      lifecycleStatus: 'normal',
    },
    // 来源关系不匹配（不同单位、不同全宗）——演示凭证门控
    evidenceMatched: false,
  },
  {
    id: 22,
    approvalType: 'destruction',
    targetType: 'destruction_list',
    targetId: 11,
    reason: '到期鉴定后按制度提交销毁',
    status: 'pending',
    submittedBy: 2,
    submittedByName: '周扬',
    submittedAt: '2026-06-15T11:00:00+08:00',
    targetListNo: 'DES-011',
    targetListName: '2014 年基建档案销毁清册',
    destructionList: {
      id: 11,
      listNo: 'DES-011',
      listName: '2014 年基建档案销毁清册',
      itemCount: 1,
      appraisalBatchNo: 'APP-002',
      items: destructionItems,
    },
  },
  {
    id: 23,
    approvalType: 'security_adjust',
    targetType: 'archive',
    targetId: 105,
    oldValue: '1',
    newValue: '0',
    reason: '密级下调',
    status: 'approved',
    submittedBy: 2,
    submittedByName: '周扬',
    submittedAt: '2026-06-14T09:00:00+08:00',
    approvedBy: 4,
    approvedByName: '方江苏',
    approvedAt: '2026-06-14T15:00:00+08:00',
    approvalOpinion: '同意下调',
    targetArchiveNo: 'ARC-000105',
    targetArchiveTitle: '2022 年会议纪要',
  },
  {
    id: 24,
    approvalType: 'open_adjust',
    targetType: 'archive',
    targetId: 106,
    oldValue: 'closed',
    newValue: 'open',
    reason: '申请公开',
    status: 'rejected',
    submittedBy: 2,
    submittedByName: '周扬',
    submittedAt: '2026-06-14T09:30:00+08:00',
    approvedBy: 4,
    approvedByName: '方江苏',
    approvedAt: '2026-06-14T15:30:00+08:00',
    approvalOpinion: '依据不足，退回补充说明',
    targetArchiveNo: 'ARC-000106',
    targetArchiveTitle: '2021 年财务决算',
  },
  {
    id: 25,
    approvalType: 'destruction',
    targetType: 'destruction_list',
    targetId: 10,
    reason: '到期销毁',
    status: 'approved',
    submittedBy: 2,
    submittedByName: '周扬',
    submittedAt: '2026-06-13T11:00:00+08:00',
    approvedBy: 4,
    approvedByName: '方江苏',
    approvedAt: '2026-06-13T16:00:00+08:00',
    approvalOpinion: '同意销毁',
    targetListNo: 'DES-010',
    targetListName: '2014 年设备档案销毁清册',
  },
]

export function mockApprovals(params?: ApprovalParams): {
  records: ApprovalRequest[]
  pageNo: number
  pageSize: number
  total: number
  hasNext: boolean
} {
  let list = approvals.slice()
  if (params?.approvalType) list = list.filter((a) => a.approvalType === params.approvalType)
  if (params?.status) list = list.filter((a) => a.status === params.status)
  if (params?.keyword) {
    const kw = params.keyword
    list = list.filter(
      (a) =>
        a.targetArchiveTitle?.includes(kw) ||
        a.targetListName?.includes(kw) ||
        a.reason.includes(kw),
    )
  }
  return {
    records: list.map(({ targetArchive, evidenceArchive, destructionList, evidenceMatched, ...rest }) => rest),
    pageNo: params?.pageNo ?? 1,
    pageSize: params?.pageSize ?? 20,
    total: list.length,
    hasNext: false,
  }
}

export function mockApprovalDetail(id: number): ApprovalRequestDetail {
  const found = approvals.find((a) => a.id === id)
  if (!found) throw new Error('审批单不存在')
  return JSON.parse(JSON.stringify(found))
}

export function mockApproveApproval(id: number, data: ApprovalOpinionData): ApprovalRequestDetail {
  const detail = mockApprovalDetail(id)
  detail.status = 'approved'
  detail.approvalOpinion = data.opinion
  detail.approvedAt = '2026-06-15T18:00:00+08:00'
  return detail
}

export function mockRejectApproval(id: number, data: ApprovalOpinionData): ApprovalRequestDetail {
  const detail = mockApprovalDetail(id)
  detail.status = 'rejected'
  detail.approvalOpinion = data.opinion
  detail.approvedAt = '2026-06-15T18:00:00+08:00'
  return detail
}

/** 销毁审批通过时联动更新清册状态（供 destruction mock 引用） */
export function resolveDestructionListSummary(id: number): ApprovalDestructionListSummary | undefined {
  const a = approvals.find((x) => x.approvalType === 'destruction' && x.targetId === id)
  return a?.destructionList
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/mock/modules/approval.ts
git commit -m "feat(appraisal-hu): 新增审批工作台 mock 数据"
```

---

## Task 7: 销毁 mock（mock/modules/destruction.ts）

**Files:**
- Create: `frontend/src/mock/modules/destruction.ts`

- [ ] **Step 1: 创建 mock 数据与函数**

```typescript
// src/mock/modules/destruction.ts
import type {
  DestructionConfirmData,
  DestructionList,
  DestructionListDetail,
  DestructionListParams,
  DestructionPhoto,
} from '@/types/destruction'
import type { ApprovalRequest } from '@/types/approval'
import { mockApprovalDetail, resolveDestructionListSummary } from './approval'

// 清册 10：待销毁（已审批通过，approval 25）
const list10Items = [
  {
    id: 8001,
    archiveId: 302,
    archiveNoSnapshot: 'ARC-000302',
    titleSnapshot: '2014 年设备档案',
    categorySnapshot: '设备档案',
    pageCountSnapshot: 80,
    retentionSnapshot: '10y',
    securityLevelSnapshot: 0,
    appraisalOpinionSnapshot: '到期无保存价值',
    fileDeleteStatus: 'not_started' as const,
  },
]

const lists: DestructionListDetail[] = [
  {
    id: 10,
    listNo: 'DES-010',
    listName: '2014 年设备档案销毁清册',
    appraisalBatchId: 2,
    appraisalBatchNo: 'APP-002',
    status: 'pending_destroy',
    approvalRequestId: 25,
    itemCount: 1,
    createdAt: '2026-06-13T12:00:00+08:00',
    items: list10Items,
    approval: mockApprovalDetail(25) as ApprovalRequest,
    photos: [
      { id: 7001, fileName: 'scene-1.jpg', fileSize: 204800, uploadedAt: '2026-06-15T16:00:00+08:00' },
    ],
  },
  {
    id: 11,
    listNo: 'DES-011',
    listName: '2014 年基建档案销毁清册',
    appraisalBatchId: 2,
    appraisalBatchNo: 'APP-002',
    status: 'pending_approval',
    approvalRequestId: 22,
    itemCount: 1,
    createdAt: '2026-06-15T11:00:00+08:00',
    items: resolveDestructionListSummary(11)!.items,
    approval: mockApprovalDetail(22) as ApprovalRequest,
    photos: [],
  },
  {
    id: 12,
    listNo: 'DES-012',
    listName: '2013 年人事档案销毁清册',
    appraisalBatchId: 3,
    appraisalBatchNo: 'APP-003',
    status: 'destroyed',
    destroyedAt: '2026-06-12T17:00:00+08:00',
    destroyMethod: 'shredding',
    supervisorName1: '刘星',
    supervisorName2: '向加明',
    destroyNote: '现场粉碎销毁，照片已上传',
    itemCount: 1,
    createdAt: '2026-06-11T10:00:00+08:00',
    items: [
      {
        id: 8101,
        archiveId: 401,
        archiveNoSnapshot: 'ARC-000401',
        titleSnapshot: '2013 年人事档案',
        categorySnapshot: '人事档案',
        pageCountSnapshot: 60,
        retentionSnapshot: '10y',
        securityLevelSnapshot: 1,
        appraisalOpinionSnapshot: '到期销毁',
        fileDeleteStatus: 'deleted',
        fileDeletedAt: '2026-06-12T17:00:00+08:00',
      },
    ],
    approval: undefined,
    photos: [
      { id: 7101, fileName: 'destroyed-1.jpg', fileSize: 256000, uploadedAt: '2026-06-12T16:30:00+08:00' },
    ],
  },
  {
    id: 13,
    listNo: 'DES-013',
    listName: '2014 年基建与设备档案销毁清册',
    appraisalBatchId: 2,
    appraisalBatchNo: 'APP-002',
    status: 'draft',
    itemCount: 1,
    createdAt: '2026-06-14T16:00:00+08:00',
    items: list10Items,
    approval: undefined,
    photos: [],
  },
]

export function mockDestructionLists(params?: DestructionListParams): {
  records: DestructionList[]
  pageNo: number
  pageSize: number
  total: number
  hasNext: boolean
} {
  let list = lists.slice()
  if (params?.status) list = list.filter((l) => l.status === params.status)
  if (params?.keyword) {
    const kw = params.keyword
    list = list.filter((l) => l.listName.includes(kw) || l.listNo.includes(kw))
  }
  return {
    records: list.map(({ items, approval, photos, ...rest }) => rest),
    pageNo: params?.pageNo ?? 1,
    pageSize: params?.pageSize ?? 20,
    total: list.length,
    hasNext: false,
  }
}

export function mockDestructionListDetail(id: number): DestructionListDetail {
  const found = lists.find((l) => l.id === id)
  if (!found) throw new Error('销毁清册不存在')
  return JSON.parse(JSON.stringify(found))
}

let nextApprovalId = 26
export function mockSubmitDestructionApproval(id: number): ApprovalRequest {
  const detail = mockDestructionListDetail(id)
  detail.status = 'pending_approval'
  detail.approvalRequestId = nextApprovalId
  const approval: ApprovalRequest = {
    id: nextApprovalId,
    approvalType: 'destruction',
    targetType: 'destruction_list',
    targetId: id,
    reason: '到期鉴定后按制度提交销毁',
    status: 'pending',
    submittedBy: 2,
    submittedByName: '胡颖',
    submittedAt: '2026-06-15T18:00:00+08:00',
    targetListNo: detail.listNo,
    targetListName: detail.listName,
  }
  nextApprovalId++
  return approval
}

let nextPhotoId = 8000
export function mockUploadDestructionPhotos(id: number, files: File[]): DestructionPhoto[] {
  const detail = mockDestructionListDetail(id)
  const photos: DestructionPhoto[] = files.map((f) => ({
    id: nextPhotoId++,
    fileName: f.name,
    fileSize: f.size,
    uploadedAt: '2026-06-15T18:30:00+08:00',
  }))
  detail.photos.push(...photos)
  return photos
}

export function mockConfirmDestruction(id: number, data: DestructionConfirmData): DestructionListDetail {
  const detail = mockDestructionListDetail(id)
  detail.status = 'destroyed'
  detail.destroyMethod = data.destroyMethod
  detail.supervisorName1 = data.supervisorName1
  detail.supervisorName2 = data.supervisorName2
  detail.destroyNote = data.destroyNote
  detail.destroyedAt = '2026-06-15T19:00:00+08:00'
  detail.items = detail.items.map((it) => ({
    ...it,
    fileDeleteStatus: 'deleted' as const,
    fileDeletedAt: '2026-06-15T19:00:00+08:00',
  }))
  return detail
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/mock/modules/destruction.ts
git commit -m "feat(appraisal-hu): 新增销毁清册 mock 数据"
```

---

## Task 8: 鉴定 API + 契约测试（api/appraisal.ts）

**Files:**
- Create: `frontend/src/api/appraisal.spec.ts`
- Create: `frontend/src/api/appraisal.ts`

- [ ] **Step 1: 写失败测试**

`frontend/src/api/appraisal.spec.ts`：

```typescript
import { describe, expect, it } from 'vitest'
import {
  completeAppraisalBatch,
  createAppraisalBatch,
  getAppraisalBatchDetail,
  getAppraisalBatches,
  saveAppraisalItems,
} from './appraisal'

describe('appraisal api mock mode', () => {
  it('lists batches and filters by status', async () => {
    const all = await getAppraisalBatches()
    expect(all.records.length).toBeGreaterThan(0)
    const drafts = await getAppraisalBatches({ status: 'draft' })
    expect(drafts.records.every((b) => b.status === 'draft')).toBe(true)
  })

  it('batch detail includes items with appraisal results', async () => {
    const detail = await getAppraisalBatchDetail(1)
    expect(detail.items.length).toBeGreaterThan(0)
    const results = detail.items.map((i) => i.appraisalResult)
    expect(results).toContain('extend')
    expect(results).toContain('destroy')
    expect(results).toContain('')
  })

  it('throws for unknown batch id', async () => {
    await expect(getAppraisalBatchDetail(99999)).rejects.toThrow()
  })

  it('completed batch (id=2) carries generated list info', async () => {
    const detail = await getAppraisalBatchDetail(2)
    expect(detail.status).toBe('completed')
    expect(detail.generatedListId).toBe(13)
    expect(detail.generatedListNo).toBe('DES-013')
  })

  it('creates a draft batch with hit items', async () => {
    const created = await createAppraisalBatch({
      batchName: '测试批次',
      categoryId: 3,
      formedYearStart: 2016,
      formedYearEnd: 2016,
    })
    expect(created.status).toBe('draft')
    expect(created.items.length).toBeGreaterThan(0)
    expect(created.batchNo).toContain('APP-')
  })

  it('saves appraisal items and updates counts', async () => {
    const detail = await saveAppraisalItems(1, {
      items: [
        { archiveId: 203, appraisalResult: 'destroy', opinion: '无价值' },
        { archiveId: 204, appraisalResult: 'extend', newRetentionPeriod: '30y', newRetentionUntil: '2045-12-31' },
      ],
    })
    const it203 = detail.items.find((i) => i.archiveId === 203)!
    expect(it203.appraisalResult).toBe('destroy')
    expect(detail.destroyCount).toBeGreaterThanOrEqual(2)
  })

  it('completes batch and returns generated list no', async () => {
    const detail = await completeAppraisalBatch(1)
    expect(detail.status).toBe('completed')
    expect(detail.generatedListNo).toContain('DES-')
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm run test:unit -- appraisal`
Expected: FAIL（`./appraisal` 模块不存在）。

- [ ] **Step 3: 实现 API 文件**

`frontend/src/api/appraisal.ts`：

```typescript
// src/api/appraisal.ts
import request from './request'
import type { PageData, PageParams } from '@/types/api'
import type {
  AppraisalBatch,
  AppraisalBatchCreateData,
  AppraisalBatchDetail,
  AppraisalBatchParams,
  AppraisalItemsSaveData,
} from '@/types/appraisal'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询鉴定批次（§14.1） */
export function getAppraisalBatches(
  params?: AppraisalBatchParams & PageParams,
): Promise<PageData<AppraisalBatch>> {
  if (USE_MOCK) {
    return import('@/mock/modules/appraisal').then((m) => m.mockAppraisalBatches(params))
  }
  return request.get('/admin/appraisal-batches', { params })
}

/** 创建鉴定批次（§14.2） */
export function createAppraisalBatch(data: AppraisalBatchCreateData): Promise<AppraisalBatchDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/appraisal').then((m) => m.mockCreateAppraisalBatch(data))
  }
  return request.post('/admin/appraisal-batches', data)
}

/** 获取鉴定批次详情（§14.3） */
export function getAppraisalBatchDetail(batchId: number): Promise<AppraisalBatchDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/appraisal').then((m) => m.mockAppraisalBatchDetail(batchId))
  }
  return request.get(`/admin/appraisal-batches/${batchId}`)
}

/** 保存鉴定明细（§14.4） */
export function saveAppraisalItems(
  batchId: number,
  data: AppraisalItemsSaveData,
): Promise<AppraisalBatchDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/appraisal').then((m) => m.mockSaveAppraisalItems(batchId, data))
  }
  return request.put(`/admin/appraisal-batches/${batchId}/items`, data)
}

/** 完成鉴定（§14.5） */
export function completeAppraisalBatch(batchId: number): Promise<AppraisalBatchDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/appraisal').then((m) => m.mockCompleteAppraisalBatch(batchId))
  }
  return request.post(`/admin/appraisal-batches/${batchId}/complete`)
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `npm run test:unit -- appraisal`
Expected: PASS（全部用例）。

- [ ] **Step 5: 提交**

```bash
git add frontend/src/api/appraisal.ts frontend/src/api/appraisal.spec.ts
git commit -m "feat(appraisal-hu): 新增鉴定 API 封装与契约测试"
```

---

## Task 9: 销毁 API + 契约测试（api/destruction.ts）

**Files:**
- Create: `frontend/src/api/destruction.spec.ts`
- Create: `frontend/src/api/destruction.ts`

- [ ] **Step 1: 写失败测试**

`frontend/src/api/destruction.spec.ts`：

```typescript
import { describe, expect, it } from 'vitest'
import {
  confirmDestruction,
  getDestructionListDetail,
  getDestructionLists,
  submitDestructionApproval,
  uploadDestructionPhotos,
} from './destruction'

describe('destruction api mock mode', () => {
  it('lists destruction lists and filters by status', async () => {
    const all = await getDestructionLists()
    const statuses = all.records.map((l) => l.status)
    expect(statuses).toContain('draft')
    expect(statuses).toContain('pending_approval')
    expect(statuses).toContain('pending_destroy')
    expect(statuses).toContain('destroyed')

    const drafts = await getDestructionLists({ status: 'draft' })
    expect(drafts.records.every((l) => l.status === 'draft')).toBe(true)
  })

  it('list detail includes snapshot items with file delete status', async () => {
    const detail = await getDestructionListDetail(10)
    expect(detail.items.length).toBeGreaterThan(0)
    expect(detail.items[0].archiveNoSnapshot).toBeTruthy()
    expect(detail.approval).toBeTruthy()
    expect(detail.approval?.status).toBe('approved')
  })

  it('throws for unknown list id', async () => {
    await expect(getDestructionListDetail(99999)).rejects.toThrow()
  })

  it('submit approval returns a pending destruction approval', async () => {
    const approval = await submitDestructionApproval(13, { reason: '到期销毁' })
    expect(approval.approvalType).toBe('destruction')
    expect(approval.status).toBe('pending')
    expect(approval.targetId).toBe(13)
  })

  it('uploads photos and returns attachment list', async () => {
    const file = new File(['x'], 'scene.jpg', { type: 'image/jpeg' })
    const photos = await uploadDestructionPhotos(10, [file])
    expect(photos.length).toBe(1)
    expect(photos[0].fileName).toBe('scene.jpg')
  })

  it('confirm destruction moves list to destroyed and deletes files', async () => {
    const detail = await confirmDestruction(10, {
      destroyMethod: 'shredding',
      supervisorName1: '刘星',
      supervisorName2: '向加明',
      destroyNote: '现场粉碎',
    })
    expect(detail.status).toBe('destroyed')
    expect(detail.items.every((i) => i.fileDeleteStatus === 'deleted')).toBe(true)
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm run test:unit -- destruction`
Expected: FAIL（`./destruction` 不存在）。

- [ ] **Step 3: 实现 API 文件**

`frontend/src/api/destruction.ts`：

```typescript
// src/api/destruction.ts
import request from './request'
import type { PageData, PageParams } from '@/types/api'
import type { ApprovalRequest } from '@/types/approval'
import type {
  DestructionConfirmData,
  DestructionList,
  DestructionListDetail,
  DestructionListParams,
  DestructionPhoto,
  DestructionSubmitApprovalData,
} from '@/types/destruction'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询销毁清册（§15.1） */
export function getDestructionLists(
  params?: DestructionListParams & PageParams,
): Promise<PageData<DestructionList>> {
  if (USE_MOCK) {
    return import('@/mock/modules/destruction').then((m) => m.mockDestructionLists(params))
  }
  return request.get('/admin/destruction-lists', { params })
}

/** 获取销毁清册详情（§15.2） */
export function getDestructionListDetail(listId: number): Promise<DestructionListDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/destruction').then((m) => m.mockDestructionListDetail(listId))
  }
  return request.get(`/admin/destruction-lists/${listId}`)
}

/** 提交销毁审批（§15.3） */
export function submitDestructionApproval(
  listId: number,
  data: DestructionSubmitApprovalData,
): Promise<ApprovalRequest> {
  if (USE_MOCK) {
    return import('@/mock/modules/destruction').then((m) => m.mockSubmitDestructionApproval(listId))
  }
  return request.post(`/admin/destruction-lists/${listId}/submit-approval`, data)
}

/** 上传销毁现场照片（§15.4） */
export function uploadDestructionPhotos(listId: number, files: File[]): Promise<DestructionPhoto[]> {
  if (USE_MOCK) {
    return import('@/mock/modules/destruction').then((m) => m.mockUploadDestructionPhotos(listId, files))
  }
  const form = new FormData()
  files.forEach((f) => form.append('files', f))
  return request.post(`/admin/destruction-lists/${listId}/photos`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 确认销毁（§15.5） */
export function confirmDestruction(listId: number, data: DestructionConfirmData): Promise<DestructionListDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/destruction').then((m) => m.mockConfirmDestruction(listId, data))
  }
  return request.post(`/admin/destruction-lists/${listId}/destroy`, data)
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `npm run test:unit -- destruction`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add frontend/src/api/destruction.ts frontend/src/api/destruction.spec.ts
git commit -m "feat(appraisal-hu): 新增销毁清册 API 封装与契约测试"
```

---

## Task 10: 审批 API + 契约测试（api/approval.ts）

**Files:**
- Create: `frontend/src/api/approval.spec.ts`
- Create: `frontend/src/api/approval.ts`

- [ ] **Step 1: 写失败测试**

`frontend/src/api/approval.spec.ts`：

```typescript
import { describe, expect, it } from 'vitest'
import { approveApproval, getApprovalDetail, getApprovals, rejectApproval } from './approval'

describe('approval api mock mode', () => {
  it('lists approvals covering three types and statuses', async () => {
    const all = await getApprovals()
    const types = all.records.map((a) => a.approvalType)
    expect(types).toContain('security_adjust')
    expect(types).toContain('open_adjust')
    expect(types).toContain('destruction')
    const statuses = all.records.map((a) => a.status)
    expect(statuses).toContain('pending')
    expect(statuses).toContain('approved')
    expect(statuses).toContain('rejected')
  })

  it('filters by approval type', async () => {
    const page = await getApprovals({ approvalType: 'destruction' })
    expect(page.records.every((a) => a.approvalType === 'destruction')).toBe(true)
  })

  it('security adjust detail includes target and evidence archives', async () => {
    const detail = await getApprovalDetail(20)
    expect(detail.approvalType).toBe('security_adjust')
    expect(detail.targetArchive).toBeTruthy()
    expect(detail.evidenceArchive).toBeTruthy()
    expect(detail.evidenceMatched).toBe(true)
  })

  it('open adjust with unmatched evidence is flagged', async () => {
    const detail = await getApprovalDetail(21)
    expect(detail.evidenceMatched).toBe(false)
  })

  it('destruction approval detail includes list snapshot', async () => {
    const detail = await getApprovalDetail(22)
    expect(detail.destructionList).toBeTruthy()
    expect(detail.destructionList!.items.length).toBeGreaterThan(0)
  })

  it('throws for unknown approval id', async () => {
    await expect(getApprovalDetail(99999)).rejects.toThrow()
  })

  it('approve sets status approved with opinion', async () => {
    const detail = await approveApproval(20, { opinion: '同意' })
    expect(detail.status).toBe('approved')
    expect(detail.approvalOpinion).toBe('同意')
  })

  it('reject sets status rejected with opinion', async () => {
    const detail = await rejectApproval(21, { opinion: '凭证不匹配，退回' })
    expect(detail.status).toBe('rejected')
    expect(detail.approvalOpinion).toBe('凭证不匹配，退回')
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm run test:unit -- approval`
Expected: FAIL（`./approval` 不存在）。

- [ ] **Step 3: 实现 API 文件**

`frontend/src/api/approval.ts`：

```typescript
// src/api/approval.ts
import request from './request'
import type { PageData, PageParams } from '@/types/api'
import type {
  ApprovalOpinionData,
  ApprovalParams,
  ApprovalRequest,
  ApprovalRequestDetail,
} from '@/types/approval'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询审批单（§13.1） */
export function getApprovals(params?: ApprovalParams & PageParams): Promise<PageData<ApprovalRequest>> {
  if (USE_MOCK) {
    return import('@/mock/modules/approval').then((m) => m.mockApprovals(params))
  }
  return request.get('/admin/approvals', { params })
}

/** 获取审批详情（§13.2） */
export function getApprovalDetail(approvalId: number): Promise<ApprovalRequestDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/approval').then((m) => m.mockApprovalDetail(approvalId))
  }
  return request.get(`/admin/approvals/${approvalId}`)
}

/** 审批通过（§13.3） */
export function approveApproval(approvalId: number, data: ApprovalOpinionData): Promise<ApprovalRequestDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/approval').then((m) => m.mockApproveApproval(approvalId, data))
  }
  return request.post(`/admin/approvals/${approvalId}/approve`, data)
}

/** 审批退回（§13.4） */
export function rejectApproval(approvalId: number, data: ApprovalOpinionData): Promise<ApprovalRequestDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/approval').then((m) => m.mockRejectApproval(approvalId, data))
  }
  return request.post(`/admin/approvals/${approvalId}/reject`, data)
}
```

> 注：`approveApproval`/`rejectApproval` 真实路径以接口文档 §13.3/13.4 为准（`/api/admin/approvals/{id}/approve`）。`request` 已配置 baseURL，mock 分支不触发请求。若 `request` 的 baseURL 已含 `/api`，则真实路径去掉前缀 `/api`，与项目其他 api 文件保持一致（其他文件用 `/admin/...`）。

- [ ] **Step 4: 运行测试确认通过**

Run: `npm run test:unit -- approval`
Expected: PASS。

- [ ] **Step 5: 全量构建验证**

Run: `npm run build`
Expected: 成功（三个 api 文件 + 类型 + mock 全部编译通过）。

- [ ] **Step 6: 提交**

```bash
git add frontend/src/api/approval.ts frontend/src/api/approval.spec.ts
git commit -m "feat(appraisal-hu): 新增审批工作台 API 封装与契约测试"
```

---

## Task 11: 鉴定校验工具（utils/appraisalValidation.ts）

**Files:**
- Create: `frontend/src/utils/appraisalValidation.spec.ts`
- Create: `frontend/src/utils/appraisalValidation.ts`

- [ ] **Step 1: 写失败测试**

`frontend/src/utils/appraisalValidation.spec.ts`：

```typescript
import { describe, expect, it } from 'vitest'
import { validateAppraisalCompletion, validateAppraisalItem } from './appraisalValidation'
import type { AppraisalItem } from '@/types/appraisal'

const mk = (over: Partial<AppraisalItem>): AppraisalItem => ({
  archiveId: 1,
  archiveNo: 'ARC-001',
  title: 't',
  categoryName: 'c',
  retentionPeriod: '10y',
  retentionUntil: '2025-12-31',
  currentLifecycleStatus: 'normal',
  appraisalResult: '',
  ...over,
})

describe('validateAppraisalItem', () => {
  it('requires an appraisal result', () => {
    expect(validateAppraisalItem(mk({ appraisalResult: '' }))).toContain('请选择鉴定结论（延长保存或待销毁）。')
  })

  it('extend requires new retention period and until', () => {
    const errors = validateAppraisalItem(mk({ appraisalResult: 'extend' }))
    expect(errors).toContain('延长保存需填写新保管期限。')
    expect(errors).toContain('延长保存需填写新到期日。')
  })

  it('extend passes with period and until', () => {
    expect(
      validateAppraisalItem(
        mk({ appraisalResult: 'extend', newRetentionPeriod: '30y', newRetentionUntil: '2045-12-31' }),
      ),
    ).toEqual([])
  })

  it('destroy passes without new retention', () => {
    expect(validateAppraisalItem(mk({ appraisalResult: 'destroy' }))).toEqual([])
  })
})

describe('validateAppraisalCompletion', () => {
  it('reports when any item unprocessed', () => {
    const errors = validateAppraisalCompletion([
      mk({ archiveId: 1, appraisalResult: 'destroy' }),
      mk({ archiveId: 2, appraisalResult: '' }),
    ])
    expect(errors.some((e) => e.includes('仍有未处理的鉴定条目'))).toBe(true)
  })

  it('reports extend item missing new period', () => {
    const errors = validateAppraisalCompletion([
      mk({ archiveId: 1, appraisalResult: 'extend', newRetentionPeriod: '', newRetentionUntil: '' }),
    ])
    expect(errors.some((e) => e.includes('延长保存'))).toBe(true)
  })

  it('passes when all items valid', () => {
    expect(
      validateAppraisalCompletion([
        mk({ archiveId: 1, appraisalResult: 'destroy' }),
        mk({ archiveId: 2, appraisalResult: 'extend', newRetentionPeriod: '30y', newRetentionUntil: '2045-12-31' }),
      ]),
    ).toEqual([])
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm run test:unit -- appraisalValidation`
Expected: FAIL（模块不存在）。

- [ ] **Step 3: 实现校验**

`frontend/src/utils/appraisalValidation.ts`：

```typescript
// src/utils/appraisalValidation.ts
import type { AppraisalItem } from '@/types/appraisal'

/** 校验单条鉴定结论，返回错误信息数组（空表示通过） */
export function validateAppraisalItem(item: AppraisalItem): string[] {
  const errors: string[] = []
  if (!item.appraisalResult) {
    errors.push('请选择鉴定结论（延长保存或待销毁）。')
    return errors
  }
  if (item.appraisalResult === 'extend') {
    if (!item.newRetentionPeriod) errors.push('延长保存需填写新保管期限。')
    if (!item.newRetentionUntil) errors.push('延长保存需填写新到期日。')
  }
  return errors
}

/** 校验整批鉴定是否可完成，返回错误信息数组（空表示可完成） */
export function validateAppraisalCompletion(items: AppraisalItem[]): string[] {
  const errors: string[] = []
  const unprocessed = items.filter((i) => !i.appraisalResult)
  if (unprocessed.length > 0) {
    errors.push(`仍有 ${unprocessed.length} 条未处理的鉴定条目，请逐条给出结论后再完成。`)
  }
  items.forEach((item) => {
    const itemErrors = validateAppraisalItem(item)
    itemErrors.forEach((msg) => {
      if (!errors.includes(msg)) errors.push(msg)
    })
  })
  return errors
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `npm run test:unit -- appraisalValidation`
Expected: PASS。

- [ ] **Step 5: 提交**

```bash
git add frontend/src/utils/appraisalValidation.ts frontend/src/utils/appraisalValidation.spec.ts
git commit -m "feat(appraisal-hu): 新增鉴定明细完成校验"
```

---

## Task 12: 销毁确认校验工具（utils/destructionValidation.ts）

**Files:**
- Create: `frontend/src/utils/destructionValidation.spec.ts`
- Create: `frontend/src/utils/destructionValidation.ts`

- [ ] **Step 1: 写失败测试**

`frontend/src/utils/destructionValidation.spec.ts`：

```typescript
import { describe, expect, it } from 'vitest'
import { validateDestroyConfirm } from './destructionValidation'

const valid = {
  destroyMethod: 'shredding' as const,
  supervisorName1: '刘星',
  supervisorName2: '向加明',
  destroyNote: '现场粉碎销毁',
  irrevocableConfirm: true,
  photoCount: 1,
}

describe('validateDestroyConfirm', () => {
  it('requires destroy method', () => {
    expect(validateDestroyConfirm({ ...valid, destroyMethod: '' as never })).toContain('请选择销毁方式。')
  })

  it('requires two distinct supervisors', () => {
    expect(validateDestroyConfirm({ ...valid, supervisorName1: '', supervisorName2: '' })).toContain('请填写两名监销人。')
    expect(validateDestroyConfirm({ ...valid, supervisorName1: '刘星', supervisorName2: '刘星' })).toContain(
      '两名监销人不能为同一人。',
    )
  })

  it('requires destroy note', () => {
    expect(validateDestroyConfirm({ ...valid, destroyNote: '' })).toContain('请填写销毁说明。')
  })

  it('requires irrevocable confirm checked', () => {
    expect(validateDestroyConfirm({ ...valid, irrevocableConfirm: false })).toContain(
      '请确认销毁后档案状态不可恢复。',
    )
  })

  it('requires at least one photo', () => {
    expect(validateDestroyConfirm({ ...valid, photoCount: 0 })).toContain('请至少上传一张现场照片。')
  })

  it('passes for valid input', () => {
    expect(validateDestroyConfirm(valid)).toEqual([])
  })
})
```

- [ ] **Step 2: 运行测试确认失败**

Run: `npm run test:unit -- destructionValidation`
Expected: FAIL（模块不存在）。

- [ ] **Step 3: 实现校验**

`frontend/src/utils/destructionValidation.ts`：

```typescript
// src/utils/destructionValidation.ts
import type { DestroyMethodValue } from '@/types/enums'

/** 销毁确认表单输入（含 UI 专用字段 irrevocableConfirm / photoCount） */
export interface DestroyConfirmInput {
  destroyMethod: DestroyMethodValue | ''
  supervisorName1: string
  supervisorName2: string
  destroyNote: string
  irrevocableConfirm: boolean
  photoCount: number
}

/** 校验销毁确认表单，返回错误信息数组（空表示通过） */
export function validateDestroyConfirm(input: DestroyConfirmInput): string[] {
  const errors: string[] = []
  if (!input.destroyMethod) errors.push('请选择销毁方式。')
  if (!input.supervisorName1.trim() || !input.supervisorName2.trim()) {
    errors.push('请填写两名监销人。')
  } else if (input.supervisorName1.trim() === input.supervisorName2.trim()) {
    errors.push('两名监销人不能为同一人。')
  }
  if (!input.destroyNote.trim()) errors.push('请填写销毁说明。')
  if (!input.irrevocableConfirm) errors.push('请确认销毁后档案状态不可恢复。')
  if (input.photoCount < 1) errors.push('请至少上传一张现场照片。')
  return errors
}
```

- [ ] **Step 4: 运行测试确认通过**

Run: `npm run test:unit -- destructionValidation`
Expected: PASS。

- [ ] **Step 5: 全量测试**

Run: `npm run test:unit`
Expected: 全部新增用例 + 既有用例通过。

- [ ] **Step 6: 提交**

```bash
git add frontend/src/utils/destructionValidation.ts frontend/src/utils/destructionValidation.spec.ts
git commit -m "feat(appraisal-hu): 新增销毁确认不可逆校验"
```

---

## Task 13: 档案鉴定页（views/admin/appraisal/index.vue）

**Files:**
- Modify: `frontend/src/views/admin/appraisal/index.vue`（重写占位）

- [ ] **Step 1: 重写鉴定页 SFC**

完整替换 `frontend/src/views/admin/appraisal/index.vue`：

```vue
<template>
  <div class="appraisal">
    <section>
      <h1 class="page-title">档案鉴定</h1>
      <p class="page-subtitle">
        处理保管期限到期或即将到期档案，创建鉴定批次，逐件选择「延长保存」或「待销毁」。完成后系统自动生成销毁清册，最终销毁审批在档案销毁页与审批工作台完成。
      </p>
    </section>

    <!-- 指标 -->
    <section class="metric-row">
      <div class="metric card"><div class="metric-num">{{ metrics.expiring }}</div><div class="metric-label">即将到期档案</div></div>
      <div class="metric card"><div class="metric-num">{{ draftCount }}</div><div class="metric-label">待鉴定批次</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.pendingDestroy }}</div><div class="metric-label">待销毁条目</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.generatedLists }}</div><div class="metric-label">已生成清册</div></div>
    </section>

    <section class="toolbar">
      <div class="tabs">
        <button class="button ghost" :class="{ 'button-active': filterStatus === '' }" @click="setFilter('')">全部批次</button>
        <button class="button ghost" :class="{ 'button-active': filterStatus === 'draft' }" @click="setFilter('draft')">待处理</button>
        <button class="button ghost" :class="{ 'button-active': filterStatus === 'completed' }" @click="setFilter('completed')">已完成</button>
      </div>
      <el-button type="primary" @click="openCreate">创建鉴定批次</el-button>
    </section>

    <section class="appraisal-layout">
      <!-- 左栏：批次列表 -->
      <aside class="card panel batch-list">
        <div v-if="listLoading" class="detail-empty">加载中...</div>
        <div v-else-if="loadError" class="detail-empty">加载失败：<button class="link" @click="loadBatches">重试</button></div>
        <div v-else-if="batches.length === 0" class="detail-empty">暂无鉴定批次</div>
        <div
          v-for="b in batches"
          :key="b.id"
          class="batch-card"
          :class="{ 'row-active': selectedBatchId === b.id }"
          @click="selectBatch(b.id)"
        >
          <div class="batch-head">
            <strong>{{ b.batchName }}</strong>
            <span class="status" :class="b.status === 'completed' ? 'success' : 'warning'">
              {{ AppraisalBatchStatusLabel[b.status] }}
            </span>
          </div>
          <div class="batch-meta">
            <span>{{ b.batchNo }}</span>
            <span>{{ b.categoryName || '全部分类' }}</span>
            <span>{{ b.formedYearStart }}{{ b.formedYearEnd && b.formedYearEnd !== b.formedYearStart ? '-' + b.formedYearEnd : '' }} 年度</span>
          </div>
          <div class="batch-meta">
            <span>命中 {{ b.hitCount }} 件</span>
            <span v-if="b.status === 'completed' && b.generatedListNo" class="link" @click.stop="goDestruction(b.generatedListId!)">→ {{ b.generatedListNo }}</span>
          </div>
        </div>
      </aside>

      <!-- 右栏：明细 -->
      <section class="card panel detail">
        <template v-if="!selectedBatchId">
          <div class="detail-empty">← 点击左侧批次查看命中档案明细</div>
        </template>
        <template v-else-if="detailLoading">
          <div class="detail-empty">加载中...</div>
        </template>
        <template v-else-if="!batchDetail">
          <div class="detail-empty">批次明细加载失败：<button class="link" @click="selectBatch(selectedBatchId)">重试</button></div>
        </template>
        <template v-else>
          <h2 class="section-title">{{ batchDetail.batchName }}（{{ batchDetail.batchNo }}）</h2>
          <div class="detail-kv"><span>分类范围</span><strong>{{ batchDetail.categoryName || '全部分类' }}</strong></div>
          <div class="detail-kv"><span>年度范围</span><strong>{{ batchDetail.formedYearStart }}–{{ batchDetail.formedYearEnd }}</strong></div>
          <div class="detail-kv"><span>命中</span><strong>{{ batchDetail.hitCount }} 件（待销毁 {{ batchDetail.destroyCount ?? 0 }} / 延长 {{ batchDetail.extendCount ?? 0 }}）</strong></div>

          <h3 class="section-title" style="margin-top:12px">鉴定明细</h3>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>档号</th>
                  <th>题名</th>
                  <th>原期限</th>
                  <th>到期日</th>
                  <th>鉴定结论</th>
                  <th>新期限/到期日</th>
                  <th>鉴定意见</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="it in batchDetail.items" :key="it.archiveId">
                  <td>{{ it.archiveNo }}</td>
                  <td>{{ it.title }}</td>
                  <td>{{ RetentionPeriodLabel[it.retentionPeriod] }}</td>
                  <td>{{ it.retentionUntil }}</td>
                  <td>
                    <label class="radio-inline"><input type="radio" :name="'r' + it.archiveId" value="extend" :disabled="batchDetail.status !== 'draft'" v-model="it.appraisalResult" />延长保存</label>
                    <label class="radio-inline"><input type="radio" :name="'r' + it.archiveId" value="destroy" :disabled="batchDetail.status !== 'draft'" v-model="it.appraisalResult" />待销毁</label>
                  </td>
                  <td>
                    <template v-if="it.appraisalResult === 'extend'">
                      <select v-model="it.newRetentionPeriod" :disabled="batchDetail.status !== 'draft'">
                        <option value="">选择期限</option>
                        <option v-for="(label, val) in RetentionPeriodLabel" :key="val" :value="val">{{ label }}</option>
                      </select>
                      <input type="date" v-model="it.newRetentionUntil" :disabled="batchDetail.status !== 'draft'" />
                    </template>
                    <span v-else-if="it.appraisalResult === 'destroy'" class="status danger">待销毁</span>
                    <span v-else class="hint">—</span>
                  </td>
                  <td>
                    <textarea v-model="it.opinion" rows="1" :disabled="batchDetail.status !== 'draft'" placeholder="鉴定意见"></textarea>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="batchDetail.status === 'draft'" class="actions">
            <el-button type="primary" @click="handleSave">保存鉴定明细</el-button>
            <el-button @click="handleComplete">完成鉴定</el-button>
          </div>
          <div v-else class="notice success">
            该批次已完成鉴定，系统已自动生成销毁清册
            <strong v-if="batchDetail.generatedListNo">{{ batchDetail.generatedListNo }}</strong>。
            <button class="link" @click="goDestruction(batchDetail.generatedListId!)">查看销毁清册 →</button>
          </div>
        </template>
      </section>
    </section>

    <!-- 创建批次弹窗 -->
    <el-dialog v-model="createVisible" title="创建鉴定批次" width="480px">
      <div class="field">
        <label>批次名称</label>
        <input v-model="createForm.batchName" placeholder="如 2015 年会计档案鉴定批次" />
      </div>
      <div class="field">
        <label>分类范围</label>
        <select v-model.number="createForm.categoryId">
          <option :value="0">全部分类</option>
          <option v-for="cat in categoryTree.filter(c => c.id > 0)" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
        </select>
      </div>
      <div class="split">
        <div class="field">
          <label>形成年度起</label>
          <input v-model.number="createForm.formedYearStart" type="number" placeholder="2014" />
        </div>
        <div class="field">
          <label>形成年度止</label>
          <input v-model.number="createForm.formedYearEnd" type="number" placeholder="2014" />
        </div>
      </div>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建批次</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { AppraisalBatch, AppraisalBatchDetail } from '@/types/appraisal'
import { AppraisalBatchStatusLabel, RetentionPeriodLabel } from '@/types/enums'
import {
  completeAppraisalBatch,
  createAppraisalBatch,
  getAppraisalBatchDetail,
  getAppraisalBatches,
  saveAppraisalItems,
} from '@/api/appraisal'
import { validateAppraisalCompletion } from '@/utils/appraisalValidation'

const router = useRouter()

const categoryTree = [
  { id: 1, name: '文书档案' },
  { id: 2, name: '科技档案' },
  { id: 3, name: '会计档案' },
  { id: 4, name: '音像档案' },
  { id: 5, name: '人事档案' },
]

const metrics = reactive({ expiring: 28, pendingDestroy: 6, generatedLists: 4 })

const batches = ref<AppraisalBatch[]>([])
const filterStatus = ref<'' | 'draft' | 'completed'>('')
const listLoading = ref(false)
const loadError = ref(false)

const selectedBatchId = ref<number | null>(null)
const batchDetail = ref<AppraisalBatchDetail | null>(null)
const detailLoading = ref(false)

const draftCount = computed(() => batches.value.filter((b) => b.status === 'draft').length)

const createVisible = ref(false)
const createForm = reactive({ batchName: '', categoryId: 0, formedYearStart: undefined as number | undefined, formedYearEnd: undefined as number | undefined })

async function loadBatches() {
  listLoading.value = true
  loadError.value = false
  try {
    const res = await getAppraisalBatches(filterStatus.value ? { status: filterStatus.value } : undefined)
    batches.value = res.records
  } catch {
    loadError.value = true
  } finally {
    listLoading.value = false
  }
}

function setFilter(s: '' | 'draft' | 'completed') {
  filterStatus.value = s
  loadBatches()
}

async function selectBatch(id: number) {
  selectedBatchId.value = id
  batchDetail.value = null
  detailLoading.value = true
  try {
    batchDetail.value = await getAppraisalBatchDetail(id)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '批次详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

function openCreate() {
  createForm.batchName = ''
  createForm.categoryId = 0
  createForm.formedYearStart = undefined
  createForm.formedYearEnd = undefined
  createVisible.value = true
}

async function handleCreate() {
  if (!createForm.batchName.trim()) {
    ElMessage.warning('请填写批次名称。')
    return
  }
  try {
    const detail = await createAppraisalBatch({
      batchName: createForm.batchName.trim(),
      categoryId: createForm.categoryId || undefined,
      formedYearStart: createForm.formedYearStart,
      formedYearEnd: createForm.formedYearEnd,
    })
    createVisible.value = false
    ElMessage.success('鉴定批次已创建，已拉入命中档案。')
    await loadBatches()
    await selectBatch(detail.id)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  }
}

async function handleSave() {
  if (!batchDetail.value) return
  try {
    const updated = await saveAppraisalItems(batchDetail.value.id, {
      items: batchDetail.value.items
        .filter((i) => i.appraisalResult === 'extend' || i.appraisalResult === 'destroy')
        .map((i) => ({
          archiveId: i.archiveId,
          appraisalResult: i.appraisalResult,
          newRetentionPeriod: i.appraisalResult === 'extend' ? (i.newRetentionPeriod || null) : null,
          newRetentionUntil: i.appraisalResult === 'extend' ? (i.newRetentionUntil || null) : null,
          opinion: i.opinion,
        })),
    })
    batchDetail.value = updated
    ElMessage.success('鉴定明细已保存。')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function handleComplete() {
  if (!batchDetail.value) return
  const errors = validateAppraisalCompletion(batchDetail.value.items)
  if (errors.length > 0) {
    ElMessage.warning(errors[0])
    return
  }
  await handleSave()
  if (!batchDetail.value) return
  try {
    const completed = await completeAppraisalBatch(batchDetail.value.id)
    batchDetail.value = completed
    ElMessage.success(`鉴定已完成，已生成销毁清册 ${completed.generatedListNo ?? ''}。`)
    await loadBatches()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '完成鉴定失败')
  }
}

function goDestruction(listId: number) {
  router.push({ path: '/admin/destruction', query: { focus: String(listId) } })
}

onMounted(loadBatches)
</script>

<style scoped>
.appraisal { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { color: var(--muted); font-size: 13px; margin: 0 0 16px; }

.metric-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }
.metric { padding: 16px; text-align: center; }
.metric-num { font-size: 26px; font-weight: 700; color: var(--primary); }
.metric-label { font-size: 13px; color: var(--muted); margin-top: 4px; }

.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.tabs { display: flex; gap: 6px; }
.button.ghost { padding: 6px 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: #fff; cursor: pointer; font-size: 14px; }
.button-active { border-color: #8abcbf !important; background: #f2f8f8 !important; }

.appraisal-layout { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 16px; align-items: start; }
.batch-list { padding: 10px; max-height: 640px; overflow-y: auto; }
.batch-card { padding: 10px 12px; border-bottom: 1px solid var(--border); cursor: pointer; }
.batch-card:hover { background: #fafafa; }
.batch-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.batch-meta { display: flex; gap: 12px; color: var(--muted); font-size: 12px; margin-top: 4px; }

.section-title { font-size: 15px; font-weight: 700; margin: 0 0 8px; }
.detail-kv { display: grid; grid-template-columns: 96px minmax(0,1fr); gap: 8px; padding: 6px 0; border-bottom: 1px solid var(--border); font-size: 13px; }
.detail-kv span { color: var(--muted); font-weight: 700; }

.table-wrap { overflow-x: auto; margin-top: 8px; }
.table-wrap table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table-wrap th, .table-wrap td { padding: 8px 10px; text-align: left; border-bottom: 1px solid var(--border); vertical-align: middle; }
.table-wrap th { font-weight: 700; color: var(--muted); background: var(--bg); }
.table-wrap input, .table-wrap select, .table-wrap textarea { width: 100%; min-height: 30px; padding: 4px 6px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 13px; }
.radio-inline { margin-right: 10px; font-size: 13px; }

.detail-empty { display: flex; align-items: center; justify-content: center; height: 220px; color: var(--muted); font-size: 14px; }
.row-active { background: #f2f8f8; }
.actions { display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap; }
.link { background: none; border: none; color: var(--primary); cursor: pointer; font-size: 13px; text-decoration: underline; padding: 0; }
.hint { color: var(--muted); font-size: 12px; }
.status { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.status.success { background: #f6ffed; color: #52c41a; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.danger { background: #fff1f0; color: #f5222d; }
.notice.success { padding: 10px 12px; border-radius: var(--radius-sm); background: #f6ffed; color: #389e0d; font-size: 13px; margin-top: 12px; }

.field { margin-bottom: 10px; }
.field label { display: block; font-size: 13px; font-weight: 700; color: var(--muted); margin-bottom: 4px; }
.field input, .field select { width: 100%; min-height: 34px; padding: 6px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 14px; }
.split { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }

@media (max-width: 1100px) {
  .metric-row { grid-template-columns: repeat(2, 1fr); }
  .appraisal-layout { grid-template-columns: 1fr; }
}
</style>
```

- [ ] **Step 2: 类型检查 + 构建**

Run: `npm run build`
Expected: 成功（鉴定页编译通过）。

- [ ] **Step 3: 提交**

```bash
git add frontend/src/views/admin/appraisal/index.vue
git commit -m "feat(appraisal-hu): 实现档案鉴定页面"
```

---

## Task 14: 销毁确认弹窗组件（DestroyConfirmDialog.vue）

**Files:**
- Create: `frontend/src/views/admin/destruction/components/DestroyConfirmDialog.vue`

> 照片校验为内联实现（MIME `image/jpeg|png` + ≤10MB）。`utils/fileParser` 仅解析文件名/大小，不做图片校验，故不复用。

- [ ] **Step 1: 创建弹窗组件**

```vue
<template>
  <el-dialog
    :model-value="modelValue"
    title="确认销毁（不可逆）"
    width="520px"
    @update:model-value="(v) => emit('update:modelValue', v)"
  >
    <div class="notice danger">
      确认销毁后档案状态将变为「已销毁」且不可恢复。销毁清册、审批记录、销毁确认与操作日志永久保留。
    </div>

    <div class="field">
      <label>销毁方式</label>
      <select v-model="form.destroyMethod">
        <option value="">请选择</option>
        <option v-for="(label, val) in DestroyMethodLabel" :key="val" :value="val">{{ label }}</option>
      </select>
    </div>
    <div class="split">
      <div class="field">
        <label>监销人 1</label>
        <input v-model="form.supervisorName1" />
      </div>
      <div class="field">
        <label>监销人 2</label>
        <input v-model="form.supervisorName2" />
      </div>
    </div>
    <div class="field">
      <label>销毁说明</label>
      <textarea v-model="form.destroyNote" rows="3" placeholder="说明销毁执行情况"></textarea>
    </div>
    <div class="field">
      <label>现场照片（至少 1 张，jpg/png，≤10MB）</label>
      <input type="file" accept="image/jpeg,image/png" multiple @change="handleFileChange" />
      <div v-if="photos.length > 0" class="photo-list">
        <span v-for="p in photos" :key="p.id" class="photo-chip">{{ p.fileName }}</span>
      </div>
    </div>
    <div class="field check">
      <label><input type="checkbox" v-model="form.irrevocableConfirm" /> 我已确认销毁后档案状态不可恢复</label>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="danger" :loading="submitting" @click="handleConfirm">最终确认销毁</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { DestructionPhoto } from '@/types/destruction'
import { DestroyMethodLabel } from '@/types/enums'
import type { DestroyMethodValue } from '@/types/enums'
import { uploadDestructionPhotos, confirmDestruction } from '@/api/destruction'
import { validateDestroyConfirm } from '@/utils/destructionValidation'

const props = defineProps<{ modelValue: boolean; listId: number }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void; (e: 'success'): void }>()

const form = reactive({
  destroyMethod: '' as DestroyMethodValue | '',
  supervisorName1: '',
  supervisorName2: '',
  destroyNote: '',
  irrevocableConfirm: false,
})
const photos = ref<DestructionPhoto[]>([])
const uploading = ref(false)
const submitting = ref(false)

watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      form.destroyMethod = ''
      form.supervisorName1 = ''
      form.supervisorName2 = ''
      form.destroyNote = ''
      form.irrevocableConfirm = false
      photos.value = []
    }
  },
)

async function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files || input.files.length === 0) return
  const files = Array.from(input.files)
  const invalid = files.find((f) => !['image/jpeg', 'image/png'].includes(f.type) || f.size > 10 * 1024 * 1024)
  if (invalid) {
    ElMessage.warning(`${invalid.name} 不是 jpg/png 或超过 10MB。`)
    input.value = ''
    return
  }
  uploading.value = true
  try {
    const uploaded = await uploadDestructionPhotos(props.listId, files)
    photos.value.push(...uploaded)
    ElMessage.success(`已上传 ${uploaded.length} 张现场照片。`)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '照片上传失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function handleConfirm() {
  const errors = validateDestroyConfirm({
    destroyMethod: form.destroyMethod,
    supervisorName1: form.supervisorName1,
    supervisorName2: form.supervisorName2,
    destroyNote: form.destroyNote,
    irrevocableConfirm: form.irrevocableConfirm,
    photoCount: photos.value.length,
  })
  if (errors.length > 0) {
    ElMessage.warning(errors[0])
    return
  }
  submitting.value = true
  try {
    await confirmDestruction(props.listId, {
      destroyMethod: form.destroyMethod as DestroyMethodValue,
      supervisorName1: form.supervisorName1.trim(),
      supervisorName2: form.supervisorName2.trim(),
      destroyNote: form.destroyNote.trim(),
      photoIds: photos.value.map((p) => p.id),
    })
    ElMessage.success('档案状态已更新为已销毁，证据链永久保留。')
    emit('success')
    emit('update:modelValue', false)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '确认销毁失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.notice.danger { padding: 8px 10px; border-radius: var(--radius-sm); background: #fff1f0; color: #a8071a; font-size: 12px; margin-bottom: 12px; }
.field { margin-bottom: 10px; }
.field label { display: block; font-size: 13px; font-weight: 700; color: var(--muted); margin-bottom: 4px; }
.field input, .field select, .field textarea { width: 100%; min-height: 34px; padding: 6px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 14px; }
.field.check label { display: flex; align-items: center; gap: 6px; }
.field.check input { width: auto; min-height: auto; }
.split { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.photo-list { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px; }
.photo-chip { padding: 2px 8px; background: var(--bg); border-radius: var(--radius-sm); font-size: 12px; }
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/views/admin/destruction/components/DestroyConfirmDialog.vue
git commit -m "feat(appraisal-hu): 新增销毁确认不可逆弹窗组件"
```

---

## Task 15: 档案销毁页（views/admin/destruction/index.vue）

**Files:**
- Modify: `frontend/src/views/admin/destruction/index.vue`（重写占位）

- [ ] **Step 1: 重写销毁页 SFC**

完整替换 `frontend/src/views/admin/destruction/index.vue`：

```vue
<template>
  <div class="destruction">
    <section>
      <h1 class="page-title">档案销毁</h1>
      <p class="page-subtitle">
        销毁清册由鉴定批次自动生成。检查清册快照后提交馆领导审批；审批通过后填写销毁方式、两名监销人并上传现场照片，最终确认销毁（不可逆）。
      </p>
    </section>

    <section class="metric-row">
      <div class="metric card"><div class="metric-num">{{ countByStatus('draft') }}</div><div class="metric-label">待提交</div></div>
      <div class="metric card"><div class="metric-num">{{ countByStatus('pending_approval') }}</div><div class="metric-label">待审批</div></div>
      <div class="metric card"><div class="metric-num">{{ countByStatus('pending_destroy') }}</div><div class="metric-label">待销毁</div></div>
      <div class="metric card"><div class="metric-num">{{ countByStatus('destroyed') }}</div><div class="metric-label">已销毁</div></div>
    </section>

    <section class="toolbar">
      <div class="tabs">
        <button class="button ghost" :class="{ 'button-active': filterStatus === '' }" @click="setFilter('')">全部</button>
        <button class="button ghost" :class="{ 'button-active': filterStatus === 'draft' }" @click="setFilter('draft')">待提交</button>
        <button class="button ghost" :class="{ 'button-active': filterStatus === 'pending_approval' }" @click="setFilter('pending_approval')">待审批</button>
        <button class="button ghost" :class="{ 'button-active': filterStatus === 'pending_destroy' }" @click="setFilter('pending_destroy')">待销毁</button>
        <button class="button ghost" :class="{ 'button-active': filterStatus === 'destroyed' }" @click="setFilter('destroyed')">已销毁</button>
      </div>
    </section>

    <section class="destruction-layout">
      <aside class="card panel list-col">
        <div v-if="listLoading" class="detail-empty">加载中...</div>
        <div v-else-if="loadError" class="detail-empty">加载失败：<button class="link" @click="loadLists">重试</button></div>
        <div v-else-if="lists.length === 0" class="detail-empty">暂无销毁清册</div>
        <div
          v-for="l in lists"
          :key="l.id"
          class="list-card"
          :class="{ 'row-active': selectedListId === l.id }"
          @click="selectList(l.id)"
        >
          <div class="batch-head">
            <strong>{{ l.listName }}</strong>
            <span class="status" :class="statusClass(l.status)">{{ DestructionListStatusLabel[l.status] }}</span>
          </div>
          <div class="batch-meta">
            <span>{{ l.listNo }}</span>
            <span>来源 {{ l.appraisalBatchNo || '—' }}</span>
            <span>{{ l.itemCount }} 件</span>
          </div>
        </div>
      </aside>

      <section class="card panel detail">
        <template v-if="!selectedListId"><div class="detail-empty">← 点击左侧清册查看快照与操作</div></template>
        <template v-else-if="detailLoading"><div class="detail-empty">加载中...</div></template>
        <template v-else-if="!listDetail"><div class="detail-empty">清册加载失败：<button class="link" @click="selectList(selectedListId)">重试</button></div></template>
        <template v-else>
          <h2 class="section-title">{{ listDetail.listName }}（{{ listDetail.listNo }}）</h2>
          <div class="detail-kv"><span>来源批次</span><strong>{{ listDetail.appraisalBatchNo || '—' }}</strong></div>
          <div class="detail-kv"><span>状态</span><strong>
            <span class="status" :class="statusClass(listDetail.status)">{{ DestructionListStatusLabel[listDetail.status] }}</span>
          </strong></div>

          <h3 class="section-title" style="margin-top:12px">清册快照</h3>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>档号快照</th><th>题名快照</th><th>分类</th><th>保管期限</th><th>密级</th><th>鉴定意见</th><th>文件删除</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="it in listDetail.items" :key="it.id">
                  <td>{{ it.archiveNoSnapshot }}</td>
                  <td>{{ it.titleSnapshot }}</td>
                  <td>{{ it.categorySnapshot }}</td>
                  <td>{{ it.retentionSnapshot }}</td>
                  <td>{{ SecurityLevelLabel[it.securityLevelSnapshot] ?? it.securityLevelSnapshot }}</td>
                  <td>{{ it.appraisalOpinionSnapshot }}</td>
                  <td>
                    <span class="status" :class="it.fileDeleteStatus === 'deleted' ? 'info' : it.fileDeleteStatus === 'failed' ? 'danger' : 'warning'">
                      {{ FileDeleteStatusLabel[it.fileDeleteStatus] }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 证据链 -->
          <h3 class="section-title" style="margin-top:12px">证据链（永久保留）</h3>
          <div class="evidence">
            <div class="evidence-node">
              <div class="evidence-title">销毁清册</div>
              <div class="evidence-desc">{{ listDetail.listNo }} · {{ listDetail.itemCount }} 件快照</div>
            </div>
            <div class="evidence-node">
              <div class="evidence-title">审批记录</div>
              <div class="evidence-desc">
                <template v-if="listDetail.approval">
                  {{ ApprovalTypeLabel[listDetail.approval.approvalType] }} ·
                  {{ listDetail.approval.status === 'approved' ? '已通过' : listDetail.approval.status === 'rejected' ? '已退回' : '审批中' }}
                  <span v-if="listDetail.approval.approvalOpinion">（{{ listDetail.approval.approvalOpinion }}）</span>
                </template>
                <template v-else>未提交审批</template>
              </div>
            </div>
            <div class="evidence-node">
              <div class="evidence-title">销毁确认</div>
              <div class="evidence-desc">
                <template v-if="listDetail.status === 'destroyed'">
                  {{ DestroyMethodLabel[listDetail.destroyMethod!] }} · {{ listDetail.supervisorName1 }}/{{ listDetail.supervisorName2 }}
                  · {{ listDetail.photos.length }} 张照片 · {{ listDetail.destroyedAt }}
                </template>
                <template v-else>待确认</template>
              </div>
            </div>
          </div>

          <!-- 操作 -->
          <div class="actions" style="margin-top:12px">
            <el-button v-if="listDetail.status === 'draft'" type="primary" @click="handleSubmit">提交审批</el-button>
            <el-button v-if="listDetail.status === 'pending_destroy'" type="danger" @click="confirmVisible = true">确认销毁</el-button>
            <router-link v-if="listDetail.status === 'pending_approval'" to="/admin/approval" class="button ghost">前往审批工作台</router-link>
            <span v-if="listDetail.status === 'pending_approval'" class="hint">馆领导审批即本馆批准凭证，不引入第三方节点。</span>
            <span v-if="listDetail.status === 'destroyed'" class="hint">该清册已完成销毁，证据链永久保留。</span>
          </div>
        </template>
      </section>
    </section>

    <DestroyConfirmDialog v-model="confirmVisible" :list-id="selectedListId ?? 0" @success="onDestroyed" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { DestructionList, DestructionListDetail } from '@/types/destruction'
import {
  DestructionListStatusLabel,
  DestroyMethodLabel,
  FileDeleteStatusLabel,
  SecurityLevelLabel,
  ApprovalTypeLabel,
} from '@/types/enums'
import type { DestructionListStatusValue } from '@/types/enums'
import { getDestructionListDetail, getDestructionLists, submitDestructionApproval } from '@/api/destruction'
import DestroyConfirmDialog from './components/DestroyConfirmDialog.vue'

const route = useRoute()
const router = useRouter()

const lists = ref<DestructionList[]>([])
const allLists = ref<DestructionList[]>([])
const filterStatus = ref<'' | DestructionListStatusValue>('')
const listLoading = ref(false)
const loadError = ref(false)

const selectedListId = ref<number | null>(null)
const listDetail = ref<DestructionListDetail | null>(null)
const detailLoading = ref(false)
const confirmVisible = ref(false)

const countByStatus = (s: DestructionListStatusValue) => allLists.value.filter((l) => l.status === s).length

function statusClass(s: string): string {
  const map: Record<string, string> = { draft: 'warning', pending_approval: 'info', pending_destroy: 'danger', destroyed: 'success' }
  return map[s] || ''
}

async function loadLists() {
  listLoading.value = true
  loadError.value = false
  try {
    const all = await getDestructionLists()
    allLists.value = all.records
    lists.value = filterStatus.value ? all.records.filter((l) => l.status === filterStatus.value) : all.records
  } catch {
    loadError.value = true
  } finally {
    listLoading.value = false
  }
}

function setFilter(s: '' | DestructionListStatusValue) {
  filterStatus.value = s
  lists.value = s ? allLists.value.filter((l) => l.status === s) : allLists.value
}

async function selectList(id: number) {
  selectedListId.value = id
  listDetail.value = null
  detailLoading.value = true
  try {
    listDetail.value = await getDestructionListDetail(id)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '清册详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function handleSubmit() {
  if (!listDetail.value) return
  try {
    const { value } = await ElMessageBox.prompt('请填写提交审批的说明', '提交销毁审批', {
      inputPattern: /.+/,
      inputErrorMessage: '请填写说明',
      inputValue: '到期鉴定后按制度提交销毁',
    })
    const approval = await submitDestructionApproval(listDetail.value.id, { reason: value })
    ElMessage.success(`已生成审批单 ${approval.targetListNo ?? ''}，清册进入待审批。`)
    router.push({ path: '/admin/approval', query: { focus: String(approval.id) } })
  } catch (e: unknown) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e instanceof Error ? e.message : '提交审批失败')
    }
  }
}

async function onDestroyed() {
  await loadLists()
  if (selectedListId.value) await selectList(selectedListId.value)
}

// 跨页联动：鉴定完成跳转 focus
watch(
  () => route.query.focus,
  async (focus) => {
    if (focus) {
      const id = Number(focus)
      if (!Number.isNaN(id)) {
        await loadLists()
        await selectList(id)
      }
    }
  },
  { immediate: true },
)

onMounted(async () => {
  await loadLists()
  if (!route.query.focus && lists.value.length > 0) {
    // 默认不自动选中，等待用户点击
  }
})
</script>

<style scoped>
.destruction { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { color: var(--muted); font-size: 13px; margin: 0 0 16px; }
.metric-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }
.metric { padding: 16px; text-align: center; }
.metric-num { font-size: 26px; font-weight: 700; color: var(--primary); }
.metric-label { font-size: 13px; color: var(--muted); margin-top: 4px; }
.toolbar { margin-bottom: 12px; }
.tabs { display: flex; gap: 6px; flex-wrap: wrap; }
.button.ghost { padding: 6px 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: #fff; cursor: pointer; font-size: 14px; text-decoration: none; color: var(--text); display: inline-block; }
.button-active { border-color: #8abcbf !important; background: #f2f8f8 !important; }
.destruction-layout { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 16px; align-items: start; }
.list-col { padding: 10px; max-height: 640px; overflow-y: auto; }
.list-card { padding: 10px 12px; border-bottom: 1px solid var(--border); cursor: pointer; }
.list-card:hover { background: #fafafa; }
.batch-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.batch-meta { display: flex; gap: 12px; color: var(--muted); font-size: 12px; margin-top: 4px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 8px; }
.detail-kv { display: grid; grid-template-columns: 96px minmax(0,1fr); gap: 8px; padding: 6px 0; border-bottom: 1px solid var(--border); font-size: 13px; }
.detail-kv span { color: var(--muted); font-weight: 700; }
.table-wrap { overflow-x: auto; margin-top: 8px; }
.table-wrap table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table-wrap th, .table-wrap td { padding: 8px 10px; text-align: left; border-bottom: 1px solid var(--border); }
.table-wrap th { font-weight: 700; color: var(--muted); background: var(--bg); }
.evidence { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.evidence-node { padding: 10px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: #fafafa; }
.evidence-title { font-weight: 700; font-size: 13px; margin-bottom: 4px; }
.evidence-desc { font-size: 12px; color: var(--muted); }
.detail-empty { display: flex; align-items: center; justify-content: center; height: 220px; color: var(--muted); font-size: 14px; }
.row-active { background: #f2f8f8; }
.actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.link { background: none; border: none; color: var(--primary); cursor: pointer; font-size: 13px; text-decoration: underline; padding: 0; }
.hint { color: var(--muted); font-size: 12px; }
.status { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.status.success { background: #f6ffed; color: #52c41a; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.danger { background: #fff1f0; color: #f5222d; }
.status.info { background: #e6f7ff; color: #1890ff; }
@media (max-width: 1100px) { .metric-row { grid-template-columns: repeat(2, 1fr); } .destruction-layout { grid-template-columns: 1fr; } .evidence { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 2: 类型检查 + 构建**

Run: `npm run build`
Expected: 成功。

- [ ] **Step 3: 提交**

```bash
git add frontend/src/views/admin/destruction/index.vue
git commit -m "feat(appraisal-hu): 实现档案销毁页面"
```

---

## Task 16: 审批详情子组件（ArchiveAdjustDetail / DestructionApprovalDetail）

**Files:**
- Create: `frontend/src/views/admin/approval/components/ArchiveAdjustDetail.vue`
- Create: `frontend/src/views/admin/approval/components/DestructionApprovalDetail.vue`

- [ ] **Step 1: 创建密级/开放审批详情组件**

`frontend/src/views/admin/approval/components/ArchiveAdjustDetail.vue`：

```vue
<template>
  <div class="adjust-detail">
    <div v-if="!detail.evidenceMatched" class="notice danger">
      凭证档案来源关系不匹配（单位/全宗不一致），不可通过，只能退回补正。
    </div>
    <div v-else-if="!detail.evidenceArchive" class="notice warning">凭证档案缺失，不可通过。</div>

    <div class="dual">
      <div class="card panel">
        <h3 class="section-title">目标档案</h3>
        <div class="detail-kv"><span>档号</span><strong>{{ detail.targetArchive?.archiveNo || '—' }}</strong></div>
        <div class="detail-kv"><span>题名</span><strong>{{ detail.targetArchive?.title || '—' }}</strong></div>
        <div class="detail-kv"><span>分类</span><strong>{{ detail.targetArchive?.categoryName || '—' }}</strong></div>
        <div class="detail-kv"><span>单位</span><strong>{{ detail.targetArchive?.organizationName || '—' }}</strong></div>
        <div class="detail-kv"><span>全宗</span><strong>{{ detail.targetArchive?.fondsName || '—' }}</strong></div>
        <div class="detail-kv"><span>当前密级</span><strong>{{ SecurityLevelLabel[detail.targetArchive?.securityLevel ?? 0] }}</strong></div>
      </div>
      <div class="card panel">
        <h3 class="section-title">凭证档案</h3>
        <div class="detail-kv"><span>档号</span><strong>{{ detail.evidenceArchive?.archiveNo || '—' }}</strong></div>
        <div class="detail-kv"><span>题名</span><strong>{{ detail.evidenceArchive?.title || '—' }}</strong></div>
        <div class="detail-kv"><span>分类</span><strong>{{ detail.evidenceArchive?.categoryName || '—' }}</strong></div>
        <div class="detail-kv"><span>单位</span><strong>{{ detail.evidenceArchive?.organizationName || '—' }}</strong></div>
        <div class="detail-kv"><span>全宗</span><strong>{{ detail.evidenceArchive?.fondsName || '—' }}</strong></div>
        <div class="detail-kv"><span>密级</span><strong>{{ SecurityLevelLabel[detail.evidenceArchive?.securityLevel ?? 0] }}</strong></div>
      </div>
    </div>

    <div class="value-change">
      <span>调整前：<strong>{{ formatValue(detail.oldValue) }}</strong></span>
      <span class="arrow">→</span>
      <span>调整后：<strong>{{ formatValue(detail.newValue) }}</strong></span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ApprovalRequestDetail } from '@/types/approval'
import { SecurityLevelLabel, OpenStatusLabel } from '@/types/enums'

const props = defineProps<{ detail: ApprovalRequestDetail }>()

function formatValue(v?: string): string {
  if (v === undefined || v === '') return '—'
  if (props.detail.approvalType === 'security_adjust') return SecurityLevelLabel[Number(v)] ?? v
  return OpenStatusLabel[v] ?? v
}
</script>

<style scoped>
.adjust-detail { display: grid; gap: 12px; }
.notice.danger { padding: 8px 10px; border-radius: var(--radius-sm); background: #fff1f0; color: #a8071a; font-size: 12px; }
.notice.warning { padding: 8px 10px; border-radius: var(--radius-sm); background: #fff7e6; color: #ad6800; font-size: 12px; }
.dual { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.panel { padding: 12px; }
.section-title { font-size: 14px; font-weight: 700; margin: 0 0 6px; }
.detail-kv { display: grid; grid-template-columns: 80px minmax(0,1fr); gap: 6px; padding: 4px 0; border-bottom: 1px solid var(--border); font-size: 13px; }
.detail-kv span { color: var(--muted); font-weight: 700; }
.value-change { display: flex; gap: 12px; align-items: center; padding: 10px; background: var(--bg); border-radius: var(--radius-sm); font-size: 14px; }
.value-change .arrow { color: var(--primary); font-weight: 700; }
@media (max-width: 760px) { .dual { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 2: 创建销毁审批详情组件**

`frontend/src/views/admin/approval/components/DestructionApprovalDetail.vue`：

```vue
<template>
  <div class="destroy-approval">
    <div class="notice info">
      馆领导审批即本馆批准凭证，不引入第三方审批节点。审批通过后清册进入待销毁，由档案管理员现场确认销毁。
    </div>
    <div class="card panel">
      <div class="detail-kv"><span>清册号</span><strong>{{ list.listNo }}</strong></div>
      <div class="detail-kv"><span>清册名称</span><strong>{{ list.listName }}</strong></div>
      <div class="detail-kv"><span>件数</span><strong>{{ list.itemCount }} 件</strong></div>
      <div class="detail-kv"><span>来源鉴定批次</span><strong>{{ list.appraisalBatchNo || '—' }}</strong></div>
    </div>
    <h3 class="section-title">清册快照</h3>
    <div class="table-wrap">
      <table>
        <thead>
          <tr><th>档号快照</th><th>题名快照</th><th>分类</th><th>保管期限</th><th>密级</th><th>鉴定意见</th></tr>
        </thead>
        <tbody>
          <tr v-for="it in list.items" :key="it.id">
            <td>{{ it.archiveNoSnapshot }}</td>
            <td>{{ it.titleSnapshot }}</td>
            <td>{{ it.categorySnapshot }}</td>
            <td>{{ it.retentionSnapshot }}</td>
            <td>{{ SecurityLevelLabel[it.securityLevelSnapshot] ?? it.securityLevelSnapshot }}</td>
            <td>{{ it.appraisalOpinionSnapshot }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ApprovalRequestDetail } from '@/types/approval'
import { SecurityLevelLabel } from '@/types/enums'

const props = defineProps<{ detail: ApprovalRequestDetail }>()
const list = computed(() => props.detail.destructionList!)
</script>

<style scoped>
.destroy-approval { display: grid; gap: 12px; }
.notice.info { padding: 8px 10px; border-radius: var(--radius-sm); background: #e6f7ff; color: #096dd9; font-size: 12px; }
.panel { padding: 12px; }
.detail-kv { display: grid; grid-template-columns: 120px minmax(0,1fr); gap: 6px; padding: 4px 0; border-bottom: 1px solid var(--border); font-size: 13px; }
.detail-kv span { color: var(--muted); font-weight: 700; }
.section-title { font-size: 14px; font-weight: 700; margin: 0; }
.table-wrap { overflow-x: auto; }
.table-wrap table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table-wrap th, .table-wrap td { padding: 8px 10px; text-align: left; border-bottom: 1px solid var(--border); }
.table-wrap th { font-weight: 700; color: var(--muted); background: var(--bg); }
</style>
```

- [ ] **Step 3: 提交**

```bash
git add frontend/src/views/admin/approval/components/ArchiveAdjustDetail.vue frontend/src/views/admin/approval/components/DestructionApprovalDetail.vue
git commit -m "feat(appraisal-hu): 新增审批详情子组件"
```

---

## Task 17: 审批工作台页（views/admin/approval/index.vue）

**Files:**
- Modify: `frontend/src/views/admin/approval/index.vue`（重写占位）

- [ ] **Step 1: 重写审批工作台 SFC**

完整替换 `frontend/src/views/admin/approval/index.vue`：

```vue
<template>
  <div class="approval">
    <section>
      <h1 class="page-title">审批工作台</h1>
      <p class="page-subtitle">
        馆领导集中处理密级调整、开放调整和销毁清册审批。查看目标档案、凭证档案或清册快照，填写审批意见后通过或退回。
      </p>
    </section>

    <section class="metric-row">
      <div class="metric card"><div class="metric-num">{{ countByStatus('pending') }}</div><div class="metric-label">待审批</div></div>
      <div class="metric card"><div class="metric-num">{{ todayProcessed }}</div><div class="metric-label">今日已处理</div></div>
      <div class="metric card"><div class="metric-num">{{ countByType('destruction') }}</div><div class="metric-label">销毁审批</div></div>
      <div class="metric card"><div class="metric-num">{{ countByStatus('rejected') }}</div><div class="metric-label">退回补正</div></div>
    </section>

    <section class="toolbar">
      <div class="tabs">
        <button class="button ghost" :class="{ 'button-active': filterType === '' }" @click="setFilter('')">全部</button>
        <button class="button ghost" :class="{ 'button-active': filterType === 'security_adjust' }" @click="setFilter('security_adjust')">密级调整</button>
        <button class="button ghost" :class="{ 'button-active': filterType === 'open_adjust' }" @click="setFilter('open_adjust')">开放调整</button>
        <button class="button ghost" :class="{ 'button-active': filterType === 'destruction' }" @click="setFilter('destruction')">销毁审批</button>
      </div>
    </section>

    <section class="approval-layout">
      <aside class="card panel queue">
        <div v-if="listLoading" class="detail-empty">加载中...</div>
        <div v-else-if="loadError" class="detail-empty">加载失败：<button class="link" @click="loadApprovals">重试</button></div>
        <div v-else-if="approvals.length === 0" class="detail-empty">暂无审批单</div>
        <div
          v-for="a in approvals"
          :key="a.id"
          class="queue-card"
          :class="{ 'row-active': selectedId === a.id }"
          @click="selectApproval(a.id)"
        >
          <div class="batch-head">
            <span class="tag" :class="typeClass(a.approvalType)">{{ ApprovalTypeLabel[a.approvalType] }}</span>
            <span class="status" :class="a.status === 'approved' ? 'success' : a.status === 'rejected' ? 'danger' : 'warning'">
              {{ statusLabel(a.status) }}
            </span>
          </div>
          <div class="queue-title">{{ a.targetArchiveTitle || a.targetListName || '审批单 #' + a.id }}</div>
          <div class="batch-meta">
            <span>{{ a.submittedByName || '提交人' }}</span>
            <span>{{ a.submittedAt.slice(0, 16).replace('T', ' ') }}</span>
          </div>
        </div>
      </aside>

      <section class="card panel detail">
        <template v-if="!selectedId"><div class="detail-empty">← 点击左侧审批单查看详情</div></template>
        <template v-else-if="detailLoading"><div class="detail-empty">加载中...</div></template>
        <template v-else-if="!detail"><div class="detail-empty">审批详情加载失败：<button class="link" @click="selectApproval(selectedId)">重试</button></div></template>
        <template v-else>
          <h2 class="section-title">{{ ApprovalTypeLabel[detail.approvalType] }} · {{ detail.targetArchiveTitle || detail.targetListName || '#' + detail.id }}</h2>
          <div class="detail-kv"><span>申请理由</span><strong>{{ detail.reason }}</strong></div>
          <div class="detail-kv"><span>提交人/时间</span><strong>{{ detail.submittedByName }} · {{ detail.submittedAt.slice(0, 16).replace('T', ' ') }}</strong></div>
          <div v-if="detail.approvalOpinion && detail.status !== 'pending'" class="detail-kv">
            <span>审批意见</span><strong>{{ detail.approvalOpinion }}（{{ detail.approvedByName }}）</strong>
          </div>

          <!-- 按类型切换详情子组件 -->
          <h3 class="section-title" style="margin-top:12px">审批证据</h3>
          <ArchiveAdjustDetail
            v-if="detail.approvalType === 'security_adjust' || detail.approvalType === 'open_adjust'"
            :detail="detail"
          />
          <DestructionApprovalDetail v-else-if="detail.approvalType === 'destruction'" :detail="detail" />

          <!-- 审批意见 + 操作 -->
          <template v-if="detail.status === 'pending'">
            <h3 class="section-title" style="margin-top:12px">审批意见</h3>
            <textarea v-model="opinion" rows="3" placeholder="填写审批意见（通过或退回均必填）"></textarea>
            <div class="actions" style="margin-top:8px">
              <el-button type="primary" :disabled="!canApprove" @click="handleApprove">审批通过</el-button>
              <el-button type="danger" @click="handleReject">退回补正</el-button>
            </div>
            <p v-if="!canApprove && (detail.approvalType !== 'destruction')" class="hint">凭证异常，不可通过，仅可退回补正。</p>
          </template>
          <div v-else class="notice" :class="detail.status === 'approved' ? 'success' : 'danger'">
            该审批单已{{ detail.status === 'approved' ? '通过' : '退回' }}，目标对象已相应更新。
          </div>
        </template>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { ApprovalRequest, ApprovalRequestDetail } from '@/types/approval'
import { ApprovalTypeLabel } from '@/types/enums'
import type { ApprovalStatusValue, ApprovalTypeValue } from '@/types/enums'
import { approveApproval, getApprovalDetail, getApprovals, rejectApproval } from '@/api/approval'
import ArchiveAdjustDetail from './components/ArchiveAdjustDetail.vue'
import DestructionApprovalDetail from './components/DestructionApprovalDetail.vue'

const route = useRoute()

const approvals = ref<ApprovalRequest[]>([])
const allApprovals = ref<ApprovalRequest[]>([])
const filterType = ref<'' | ApprovalTypeValue>('')
const listLoading = ref(false)
const loadError = ref(false)
const todayProcessed = ref(3)

const selectedId = ref<number | null>(null)
const detail = ref<ApprovalRequestDetail | null>(null)
const detailLoading = ref(false)
const opinion = ref('')

const countByStatus = (s: ApprovalStatusValue) => allApprovals.value.filter((a) => a.status === s).length
const countByType = (t: ApprovalTypeValue) => allApprovals.value.filter((a) => a.approvalType === t).length

const canApprove = computed(() => {
  if (!detail.value || detail.value.status !== 'pending') return false
  // 销毁审批默认可通过；密级/开放需凭证匹配
  if (detail.value.approvalType === 'destruction') return true
  return !!detail.value.evidenceArchive && !!detail.value.evidenceMatched
})

function statusLabel(s: ApprovalStatusValue): string {
  return s === 'approved' ? '已通过' : s === 'rejected' ? '已退回' : '待审批'
}
function typeClass(t: ApprovalTypeValue): string {
  return t === 'destruction' ? 'danger' : t === 'open_adjust' ? 'info' : 'warning'
}

async function loadApprovals() {
  listLoading.value = true
  loadError.value = false
  try {
    const all = await getApprovals()
    allApprovals.value = all.records
    approvals.value = filterType.value ? all.records.filter((a) => a.approvalType === filterType.value) : all.records
  } catch {
    loadError.value = true
  } finally {
    listLoading.value = false
  }
}

function setFilter(t: '' | ApprovalTypeValue) {
  filterType.value = t
  approvals.value = t ? allApprovals.value.filter((a) => a.approvalType === t) : allApprovals.value
}

async function selectApproval(id: number) {
  selectedId.value = id
  detail.value = null
  opinion.value = ''
  detailLoading.value = true
  try {
    detail.value = await getApprovalDetail(id)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '审批详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function handleApprove() {
  if (!detail.value || !opinion.value.trim()) {
    ElMessage.warning('请填写审批意见。')
    return
  }
  try {
    const updated = await approveApproval(detail.value.id, { opinion: opinion.value.trim() })
    detail.value = updated
    ElMessage.success('审批已通过。密级/开放调整字段已生效，销毁清册已进入待销毁。')
    await loadApprovals()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '审批失败')
  }
}

async function handleReject() {
  if (!detail.value || !opinion.value.trim()) {
    ElMessage.warning('请填写退回意见。')
    return
  }
  try {
    const updated = await rejectApproval(detail.value.id, { opinion: opinion.value.trim() })
    detail.value = updated
    ElMessage.success('审批已退回，目标对象保持不变。')
    await loadApprovals()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '退回失败')
  }
}

watch(
  () => route.query.focus,
  async (focus) => {
    if (focus) {
      const id = Number(focus)
      if (!Number.isNaN(id)) {
        await loadApprovals()
        await selectApproval(id)
      }
    }
  },
  { immediate: true },
)

onMounted(async () => {
  await loadApprovals()
})
</script>

<style scoped>
.approval { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { color: var(--muted); font-size: 13px; margin: 0 0 16px; }
.metric-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }
.metric { padding: 16px; text-align: center; }
.metric-num { font-size: 26px; font-weight: 700; color: var(--primary); }
.metric-label { font-size: 13px; color: var(--muted); margin-top: 4px; }
.toolbar { margin-bottom: 12px; }
.tabs { display: flex; gap: 6px; flex-wrap: wrap; }
.button.ghost { padding: 6px 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: #fff; cursor: pointer; font-size: 14px; }
.button-active { border-color: #8abcbf !important; background: #f2f8f8 !important; }
.approval-layout { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 16px; align-items: start; }
.queue { padding: 10px; max-height: 640px; overflow-y: auto; }
.queue-card { padding: 10px 12px; border-bottom: 1px solid var(--border); cursor: pointer; }
.queue-card:hover { background: #fafafa; }
.batch-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-bottom: 4px; }
.queue-title { font-weight: 700; font-size: 13px; }
.batch-meta { display: flex; gap: 12px; color: var(--muted); font-size: 12px; margin-top: 4px; }
.tag { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.tag.warning { background: #fff7e6; color: #fa8c16; }
.tag.info { background: #e6f7ff; color: #1890ff; }
.tag.danger { background: #fff1f0; color: #f5222d; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 8px; }
.detail-kv { display: grid; grid-template-columns: 96px minmax(0,1fr); gap: 8px; padding: 6px 0; border-bottom: 1px solid var(--border); font-size: 13px; }
.detail-kv span { color: var(--muted); font-weight: 700; }
textarea { width: 100%; min-height: 60px; padding: 6px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 14px; resize: vertical; }
.detail-empty { display: flex; align-items: center; justify-content: center; height: 220px; color: var(--muted); font-size: 14px; }
.row-active { background: #f2f8f8; }
.actions { display: flex; gap: 8px; flex-wrap: wrap; }
.link { background: none; border: none; color: var(--primary); cursor: pointer; font-size: 13px; text-decoration: underline; padding: 0; }
.hint { color: var(--muted); font-size: 12px; margin-top: 6px; }
.status { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.status.success { background: #f6ffed; color: #52c41a; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.danger { background: #fff1f0; color: #f5222d; }
.notice { padding: 10px 12px; border-radius: var(--radius-sm); font-size: 13px; margin-top: 12px; }
.notice.success { background: #f6ffed; color: #389e0d; }
.notice.danger { background: #fff1f0; color: #a8071a; }
@media (max-width: 1100px) { .metric-row { grid-template-columns: repeat(2, 1fr); } .approval-layout { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 2: 类型检查 + 构建**

Run: `npm run build`
Expected: 成功（三页 + 子组件全部编译通过）。

- [ ] **Step 3: 提交**

```bash
git add frontend/src/views/admin/approval/index.vue
git commit -m "feat(appraisal-hu): 实现审批工作台页面"
```

---

## Task 18: 全量验证与提交计划文档

**Files:**
- 验证：整个 `frontend/`
- 本计划文档已在本文件

- [ ] **Step 1: 全量类型检查 + 生产构建**

Run: `npm run build`
Expected: `vue-tsc -b` 与 `vite build` 均成功，无类型错误、无未使用导入（杜绝 archive-hu 式类型错误阻塞集成）。

- [ ] **Step 2: 全量单元测试**

Run: `npm run test:unit`
Expected: 全部新增用例（appraisal/destruction/approval api spec、appraisalValidation/destructionValidation spec）+ 既有用例全部通过。

- [ ] **Step 3: 运行时冒烟（建议）**

Run（后台）: `npm run dev`，浏览器访问三页核对：

- `/admin/appraisal`：创建批次→逐件鉴定→保存→完成→显示清册号→「查看销毁清册」跳转；延长保存未填新期限时完成被拦截。
- `/admin/destruction`：四态过滤；draft 清册提交审批→跳审批工作台；pending_destroy 清册确认销毁弹窗（双监销人/不可逆/照片）→成功后状态变 destroyed。
- `/admin/approval`：三类型切换；密级/开放双栏（凭证不匹配时通过按钮禁用）；销毁审批清册快照；通过/退回意见必填。

> mock 下无需后端。运行时验证为可选项；若环境受限，至少保证 Step 1–2 通过。验证完成后 `pkill -f vite` 或 Ctrl+C 停止 dev 服务。

- [ ] **Step 4: 提交本实施计划文档**

```bash
git add frontend/docs/superpowers/plans/2026-06-15-appraisal-hu.md
git commit -m "docs(appraisal-hu): 档案鉴定销毁审批工作台实施计划"
```

- [ ] **Step 5: 推送分支并开启 PR（交付给项目经理方江苏 review）**

```bash
git push -u origin feat/appraisal-hu
gh pr create --base develop --head feat/appraisal-hu \
  --title "feat(appraisal-hu): 档案鉴定·销毁·审批工作台" \
  --body "P1 任务 06-15~06-17：档案鉴定、档案销毁、审批工作台三页（mock 开发）。含设计文档、实施计划、类型/mock/api/校验/视图与测试。共同验收点：库房利用与审批处置页面可联动。"
```

> PR 标题与 body 按团队约定；`gh` 使用 huying 身份（仓库目录已配 `GH_CONFIG_DIR`）。若 PR 模板不同，以仓库实际为准。

---

## 自查记录（计划作者）

本计划在撰写过程中已完成以下自查与修正：

1. **路径一致性**：Task 10 `approveApproval` 真实路径曾误写 `/api/admin/...`，已统一为 `/admin/approvals/{id}/approve`（与项目其他 api 文件一致，`request` 已含 `/api` baseURL）。
2. **照片校验来源**：设计文档原写「复用 `utils/fileParser` 校验」，但 `fileParser` 仅解析文件名/大小、不做图片类型/大小校验。已修正为弹窗内联校验（MIME `image/jpeg|png` + ≤10MB）。
3. **保存语义**：Task 13 `handleSave` 原用 `appraisalResult || 'destroy'` 兜底，会把未处理项误存为待销毁。已修正为只保存已决策项（`filter` + 直接用类型值），并通过 `validateAppraisalCompletion` 在完成前拦截未处理项。
4. **跨域 mock 联动**：鉴定 completed 批次 `generatedListId=13` 对应销毁 mock 清册 13；销毁 `pending_approval`/`pending_destroy` 清册的 `approvalRequestId` 对应审批 mock 中销毁类型审批单，保证跳转与详情一致。
5. **类型循环引用**：`types/destruction.ts` 与 `types/approval.ts` 互引（销毁详情内嵌审批单、审批销毁详情内嵌清册明细），均为 `import type`，编译期擦除，无运行时循环依赖。
6. **验证脚本**：项目无独立 `type-check`，`npm run build`（`vue-tsc -b && vite build`）即类型检查 + 构建，计划全部使用真实脚本。

## 验收对照（设计文档 §14）

| 设计要求 | 对应任务 |
|----------|----------|
| 三页无占位、还原原型布局 | Task 13 / 15 / 17 |
| 字段对齐接口文档 §13/14/15、数据库 §8.1–8.5 | Task 2–4（类型）、5–7（mock） |
| 鉴定延长/待销毁互斥、延长需新期限、完成生成清册可跳转 | Task 11（校验）、13（页） |
| 销毁四态过滤、快照、提交审批跳审批台、确认销毁不可逆+双监销人+照片+证据链 | Task 12（校验）、14（弹窗）、15（页） |
| 审批三类型切换、证据可见、凭证门控、意见必填 | Task 16（子组件）、17（页） |
| 跨页联动 `?focus=` | Task 15 / 17（watch query） |
| 三态（加载/空/错误）覆盖 | Task 13 / 15 / 17（模板 `v-if` 分支） |
| mock/真实切换组件不改 | Task 8–10（USE_MOCK 分支） |
| 枚举复用与扩展一致 | Task 1 |
| type-check/build/test 全过 | Task 18 |

---

**计划完成。** 共 18 个任务，依赖顺序：枚举/类型（1–4）→ mock（5–7）→ api+spec（8–10）→ 校验+spec（11–12）→ 视图（13–17）→ 全量验证与交付（18）。
