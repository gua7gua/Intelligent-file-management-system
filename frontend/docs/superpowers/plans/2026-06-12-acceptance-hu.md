# feat/acceptance-hu 实施计划 — 移交验收与征集管理页面

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现管理后台的移交验收与电子文件上传页面和征集管理与接收页面，包含完整的 API 层、Mock 数据和 TypeScript 类型定义。

**Architecture:** 原型直译方式，将已有 HTML 原型搬入 Vue SFC，使用全局引入的原型 CSS 类。Element Plus 仅用于 ElMessage 消息提示和 el-upload 文件上传。两个页面各自独立，征集页面的验收流程复用 reception API。

**Tech Stack:** Vue 3 (Composition API / `<script setup>`)、TypeScript、Element Plus、Axios、原型全局 CSS

**设计文档:** `frontend/docs/superpowers/specs/2026-06-12-acceptance-hu-design.md`

---

## 文件结构

| 操作 | 文件 | 职责 |
|------|------|------|
| 新建 | `src/types/reception.ts` | 验收相关 TypeScript 类型 |
| 新建 | `src/types/collection.ts` | 征集相关 TypeScript 类型 |
| 新建 | `src/mock/modules/reception.ts` | 验收 mock 数据（2 个移交批次 + 暂存文件） |
| 新建 | `src/mock/modules/collection.ts` | 征集 mock 数据（1 个征集批次） |
| 修改 | `src/mock/index.ts` | 汇总导出新增 mock 模块 |
| 新建 | `src/api/reception.ts` | 前台验收 API（7 个函数） |
| 新建 | `src/api/collection.ts` | 征集管理 API（3 个函数） |
| 重写 | `src/views/admin/transfer-reception/index.vue` | 移交验收页面 |
| 重写 | `src/views/admin/collection/index.vue` | 征集管理页面 |

---

### Task 1: TypeScript 类型定义

**Files:**
- Create: `src/types/reception.ts`
- Create: `src/types/collection.ts`

- [ ] **Step 1: 创建 reception 类型文件**

```typescript
// src/types/reception.ts

/** 前台验收批次 */
export interface ReceptionBatch {
  id: number
  batchNo: string
  title: string
  sourceType: 'transfer' | 'collection' | 'compilation'
  status: string
  statusText: string
  organizationId: number
  organizationName: string
  departmentName: string
  contactPerson: string
  contactPhone: string
  expectedTransferDate: string
  submittedAt: string
  itemCount: number
  signatureStatus: string
  acceptanceNote?: string
  acceptedBy?: number
  acceptedAt?: string
  createdAt: string
  updatedAt: string
}

/** 验收条目 */
export interface ReceptionItem {
  id: number
  batchId: number
  seqNo: number
  title: string
  carrierType: string
  expectedFilename: string
  paperCheckStatus: 'pending' | 'passed' | 'failed'
  fileMatchStatus: 'none' | 'matched' | 'unmatched' | 'duplicate' | 'missing' | 'failed'
  result: 'pending' | 'accepted' | 'rejected'
  acceptanceNote: string
  rejectReason: string
  createdAt: string
  updatedAt: string
}

/** 暂存文件 */
export interface StagingFile {
  id: number
  batchId: number
  originalFilename: string
  fileSize: number
  sha256: string
  scanResult: 'safe' | 'unsafe' | 'error'
  matchStatus: 'staging' | 'matched' | 'unmatched' | 'duplicate' | 'deleted'
  matchedItemId?: number
  uploadBatchNo: string
  createdAt: string
}

/** 匹配汇总 */
export interface MatchSummary {
  matched: number
  unmatched: number
  duplicate: number
  failed: number
  missingItems: number[]
}

/** 文件上传结果 */
export interface UploadResult {
  uploadBatchNo: string
  files: StagingFile[]
  matchSummary: MatchSummary
}

/** 批次详情（含条目和暂存文件） */
export interface BatchDetail {
  batch: ReceptionBatch
  items: ReceptionItem[]
  stagingFiles: StagingFile[]
  matchSummary: MatchSummary
}

/** 批次查询参数 */
export interface ReceptionBatchParams {
  sourceType?: 'transfer' | 'collection'
  status?: string
  keyword?: string
  pageNo?: number
  pageSize?: number
}

/** 条目验收请求 */
export interface ItemAcceptanceData {
  result: 'accepted' | 'rejected'
  acceptanceNote?: string
  rejectReason?: string
}

/** 手工匹配请求 */
export interface ManualMatchData {
  itemId: number
  matchStatus: 'matched'
  note: string
}

/** 完成批次验收请求 */
export interface CompleteBatchData {
  acceptanceNote?: string
}
```

- [ ] **Step 2: 创建 collection 类型文件**

```typescript
// src/types/collection.ts
import type { ReceptionBatch } from './reception'

/** 征集批次（扩展验收批次） */
export interface CollectionBatch extends ReceptionBatch {
  sourceType: 'collection'
  donorName: string
  donorPhone: string
  agreementAcceptedAt?: string
  contactNote?: string
  scheduledReceiveAt?: string
  rejectReason?: string
}

/** 征集批次查询参数 */
export interface CollectionParams {
  status?: string
  keyword?: string
  contactPhone?: string
  pageNo?: number
  pageSize?: number
}

/** 约定到馆请求 */
export interface ScheduleData {
  scheduledReceiveAt: string
  contactNote: string
}

/** 拒绝征集请求 */
export interface RejectData {
  rejectReason: string
}
```

- [ ] **Step 3: 类型检查**

Run: `cd frontend && npx vue-tsc --noEmit`
Expected: 无错误

- [ ] **Step 4: 提交**

```bash
git add src/types/reception.ts src/types/collection.ts
git commit -m "feat(acceptance-hu): 添加验收和征集模块 TypeScript 类型定义"
```

---

### Task 2: Mock 数据

**Files:**
- Create: `src/mock/modules/reception.ts`
- Create: `src/mock/modules/collection.ts`
- Modify: `src/mock/index.ts`

- [ ] **Step 1: 创建验收 mock 数据**

```typescript
// src/mock/modules/reception.ts
import type { BatchDetail, ReceptionBatch, ReceptionItem, StagingFile, UploadResult } from '@/types/reception'

// ── 移交批次 A：4 条目，含匹配异常 ──
const batchAItems: ReceptionItem[] = [
  {
    id: 101, batchId: 1, seqNo: 1,
    title: '2025 年 1 月会计凭证',
    carrierType: '纸质+电子',
    expectedFilename: '2025-01-voucher.pdf',
    paperCheckStatus: 'pending',
    fileMatchStatus: 'none',
    result: 'pending',
    acceptanceNote: '', rejectReason: '',
    createdAt: '2026-06-20T09:00:00+08:00',
    updatedAt: '2026-06-20T09:00:00+08:00',
  },
  {
    id: 102, batchId: 1, seqNo: 2,
    title: '2025 年 2 月会计凭证',
    carrierType: '纸质+电子',
    expectedFilename: '2025-02-voucher.pdf',
    paperCheckStatus: 'pending',
    fileMatchStatus: 'none',
    result: 'pending',
    acceptanceNote: '', rejectReason: '',
    createdAt: '2026-06-20T09:00:00+08:00',
    updatedAt: '2026-06-20T09:00:00+08:00',
  },
  {
    id: 103, batchId: 1, seqNo: 3,
    title: '2025 年 3 月会计凭证',
    carrierType: '纸质+电子',
    expectedFilename: '2025-03-voucher.pdf',
    paperCheckStatus: 'pending',
    fileMatchStatus: 'missing',
    result: 'pending',
    acceptanceNote: '', rejectReason: '',
    createdAt: '2026-06-20T09:00:00+08:00',
    updatedAt: '2026-06-20T09:00:00+08:00',
  },
  {
    id: 104, batchId: 1, seqNo: 4,
    title: '2025 年档案移交说明',
    carrierType: '纯纸质',
    expectedFilename: '',
    paperCheckStatus: 'pending',
    fileMatchStatus: 'none',
    result: 'pending',
    acceptanceNote: '', rejectReason: '',
    createdAt: '2026-06-20T09:00:00+08:00',
    updatedAt: '2026-06-20T09:00:00+08:00',
  },
]

// ── 移交批次 B：2 条目 ──
const batchBItems: ReceptionItem[] = [
  {
    id: 201, batchId: 2, seqNo: 1,
    title: '2025 年 1-6 月工资表',
    carrierType: '纯电子',
    expectedFilename: 'salary-2025-h1.xlsx',
    paperCheckStatus: 'passed',
    fileMatchStatus: 'none',
    result: 'pending',
    acceptanceNote: '', rejectReason: '',
    createdAt: '2026-06-08T14:00:00+08:00',
    updatedAt: '2026-06-08T14:00:00+08:00',
  },
  {
    id: 202, batchId: 2, seqNo: 2,
    title: '2025 年 7-12 月工资表',
    carrierType: '纯电子',
    expectedFilename: 'salary-2025-h2.xlsx',
    paperCheckStatus: 'passed',
    fileMatchStatus: 'none',
    result: 'pending',
    acceptanceNote: '', rejectReason: '',
    createdAt: '2026-06-08T14:00:00+08:00',
    updatedAt: '2026-06-08T14:00:00+08:00',
  },
]

// ── 模拟上传后的暂存文件（批次 A） ──
const mockStagingFiles: StagingFile[] = [
  {
    id: 301, batchId: 1,
    originalFilename: '2025-01-voucher.pdf',
    fileSize: 524288,
    sha256: 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2',
    scanResult: 'safe',
    matchStatus: 'matched',
    matchedItemId: 101,
    uploadBatchNo: 'UP-20260612-001',
    createdAt: '2026-06-12T10:30:00+08:00',
  },
  {
    id: 302, batchId: 1,
    originalFilename: '2025-02-voucher.pdf',
    fileSize: 483328,
    sha256: 'b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3',
    scanResult: 'safe',
    matchStatus: 'matched',
    matchedItemId: 102,
    uploadBatchNo: 'UP-20260612-001',
    createdAt: '2026-06-12T10:30:00+08:00',
  },
  {
    id: 303, batchId: 1,
    originalFilename: '2025-03-voucher-copy.pdf',
    fileSize: 512000,
    sha256: 'c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4',
    scanResult: 'safe',
    matchStatus: 'unmatched',
    uploadBatchNo: 'UP-20260612-001',
    createdAt: '2026-06-12T10:30:00+08:00',
  },
  {
    id: 304, batchId: 1,
    originalFilename: '2025-02-voucher.pdf',
    fileSize: 483328,
    sha256: 'b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3',
    scanResult: 'safe',
    matchStatus: 'duplicate',
    uploadBatchNo: 'UP-20260612-001',
    createdAt: '2026-06-12T10:30:00+08:00',
  },
]

export const mockReceptionBatches: ReceptionBatch[] = [
  {
    id: 1, batchNo: 'YJ-2026-0008',
    title: '2025 年度会计凭证移交清单',
    sourceType: 'transfer',
    status: 'pending_transfer',
    statusText: '待移交',
    organizationId: 3,
    organizationName: '克拉玛依市财政局',
    departmentName: '财务部',
    contactPerson: '小张',
    contactPhone: '0990-6123456',
    expectedTransferDate: '2026-06-24',
    submittedAt: '2026-06-20T09:00:00+08:00',
    itemCount: 4,
    signatureStatus: '已核对',
    createdAt: '2026-06-20T09:00:00+08:00',
    updatedAt: '2026-06-20T09:00:00+08:00',
  },
  {
    id: 2, batchNo: 'YJ-2026-0007',
    title: '2025 年度工资表电子档案',
    sourceType: 'transfer',
    status: 'pending_transfer',
    statusText: '待移交',
    organizationId: 3,
    organizationName: '克拉玛依市财政局',
    departmentName: '人事财务联合组',
    contactPerson: '小张',
    contactPhone: '0990-6123456',
    expectedTransferDate: '2026-06-09',
    submittedAt: '2026-06-08T14:00:00+08:00',
    itemCount: 2,
    signatureStatus: '已核对',
    createdAt: '2026-06-08T14:00:00+08:00',
    updatedAt: '2026-06-08T14:00:00+08:00',
  },
]

/** 模拟批次详情 */
export function mockBatchDetail(batchId: number): BatchDetail | null {
  const batch = mockReceptionBatches.find((b) => b.id === batchId)
  if (!batch) return null
  const items = batchId === 1 ? batchAItems : batchBItems
  const stagingFiles = batchId === 1 ? mockStagingFiles : []
  return {
    batch,
    items: JSON.parse(JSON.stringify(items)),
    stagingFiles: JSON.parse(JSON.stringify(stagingFiles)),
    matchSummary: {
      matched: stagingFiles.filter((f) => f.matchStatus === 'matched').length,
      unmatched: stagingFiles.filter((f) => f.matchStatus === 'unmatched').length,
      duplicate: stagingFiles.filter((f) => f.matchStatus === 'duplicate').length,
      failed: stagingFiles.filter((f) => f.scanResult === 'unsafe' || f.scanResult === 'error').length,
      missingItems: items.filter((i) => i.fileMatchStatus === 'missing').map((i) => i.id),
    },
  }
}

/** 模拟文件上传结果 */
export function mockUploadResult(): UploadResult {
  return {
    uploadBatchNo: 'UP-20260612-001',
    files: JSON.parse(JSON.stringify(mockStagingFiles)),
    matchSummary: {
      matched: 2,
      unmatched: 1,
      duplicate: 1,
      failed: 0,
      missingItems: [103],
    },
  }
}
```

- [ ] **Step 2: 创建征集 mock 数据**

```typescript
// src/mock/modules/collection.ts
import type { CollectionBatch } from '@/types/collection'

export const mockCollectionBatches: CollectionBatch[] = [
  {
    id: 10, batchNo: 'COL-202606-008',
    title: '城市老照片捐赠意向',
    sourceType: 'collection',
    status: 'pending_contact',
    statusText: '待联系',
    organizationId: 0,
    organizationName: '',
    departmentName: '',
    contactPerson: '周明',
    contactPhone: '138-0000-5526',
    expectedTransferDate: '',
    submittedAt: '2026-06-02T10:00:00+08:00',
    itemCount: 12,
    signatureStatus: '',
    donorName: '周明',
    donorPhone: '138-0000-5526',
    agreementAcceptedAt: '2026-06-02T14:18:00+08:00',
    createdAt: '2026-06-02T10:00:00+08:00',
    updatedAt: '2026-06-02T10:00:00+08:00',
  },
  {
    id: 11, batchNo: 'COL-202605-027',
    title: '社区建设资料捐赠',
    sourceType: 'collection',
    status: 'pending_receive',
    statusText: '待接收',
    organizationId: 0,
    organizationName: '',
    departmentName: '',
    contactPerson: '陈岚',
    contactPhone: '139-0000-7811',
    expectedTransferDate: '',
    submittedAt: '2026-05-15T08:30:00+08:00',
    itemCount: 6,
    signatureStatus: '',
    donorName: '陈岚',
    donorPhone: '139-0000-7811',
    agreementAcceptedAt: '2026-05-15T08:30:00+08:00',
    contactNote: '电话确认可到馆提交材料',
    scheduledReceiveAt: '2026-06-12T09:30:00+08:00',
    createdAt: '2026-05-15T08:30:00+08:00',
    updatedAt: '2026-06-10T16:00:00+08:00',
  },
  {
    id: 12, batchNo: 'COL-202604-014',
    title: '厂区变迁照片资料',
    sourceType: 'collection',
    status: 'pending_receive',
    statusText: '待接收',
    organizationId: 0,
    organizationName: '',
    departmentName: '',
    contactPerson: '林越',
    contactPhone: '137-0000-3490',
    expectedTransferDate: '',
    submittedAt: '2026-04-20T11:00:00+08:00',
    itemCount: 10,
    signatureStatus: '',
    donorName: '林越',
    donorPhone: '137-0000-3490',
    agreementAcceptedAt: '2026-04-20T11:00:00+08:00',
    contactNote: '材料方向符合，已约定到馆',
    scheduledReceiveAt: '2026-06-09T15:00:00+08:00',
    createdAt: '2026-04-20T11:00:00+08:00',
    updatedAt: '2026-06-08T10:00:00+08:00',
  },
]
```

- [ ] **Step 3: 更新 mock/index.ts**

```typescript
// src/mock/index.ts
// Mock 数据汇总入口，后续按模块扩展

// 按需导入的 mock 模块（由 API 层动态 import，此处仅做汇总标记）
export { mockReceptionBatches, mockBatchDetail, mockUploadResult } from './modules/reception'
export { mockCollectionBatches } from './modules/collection'
```

- [ ] **Step 4: 类型检查**

Run: `cd frontend && npx vue-tsc --noEmit`
Expected: 无错误

- [ ] **Step 5: 提交**

```bash
git add src/mock/modules/reception.ts src/mock/modules/collection.ts src/mock/index.ts
git commit -m "feat(acceptance-hu): 添加验收和征集模块 mock 数据"
```

---

### Task 3: API 函数

**Files:**
- Create: `src/api/reception.ts`
- Create: `src/api/collection.ts`

- [ ] **Step 1: 创建 reception API**

```typescript
// src/api/reception.ts
import request from './request'
import type { PageData, PageParams } from '@/types/api'
import type {
  ReceptionBatch,
  ReceptionBatchParams,
  BatchDetail,
  UploadResult,
  ManualMatchData,
  ItemAcceptanceData,
  CompleteBatchData,
} from '@/types/reception'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询待验收批次 */
export function getReceptionBatches(
  params?: ReceptionBatchParams & PageParams,
): Promise<PageData<ReceptionBatch>> {
  if (USE_MOCK) {
    return import('@/mock/modules/reception').then((m) => {
      const all = m.mockReceptionBatches.filter((b) => {
        if (params?.sourceType && b.sourceType !== params.sourceType) return false
        if (params?.status && b.status !== params.status) return false
        if (params?.keyword) {
          const kw = params.keyword.toLowerCase()
          const text = `${b.batchNo} ${b.title} ${b.organizationName} ${b.contactPerson}`.toLowerCase()
          if (!text.includes(kw)) return false
        }
        return true
      })
      return {
        records: all,
        pageNo: params?.pageNo ?? 1,
        pageSize: params?.pageSize ?? 20,
        total: all.length,
        hasNext: false,
      }
    })
  }
  return request.get('/admin/reception/batches', { params })
}

/** 获取验收详情 */
export function getReceptionBatchDetail(batchId: number): Promise<BatchDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/reception').then((m) => {
      const detail = m.mockBatchDetail(batchId)
      if (!detail) throw new Error('批次不存在')
      return detail
    })
  }
  return request.get(`/admin/reception/batches/${batchId}`)
}

/** 上传暂存电子文件 */
export function uploadStagingFiles(
  batchId: number,
  files: File[],
): Promise<UploadResult> {
  if (USE_MOCK) {
    return import('@/mock/modules/reception').then((m) => m.mockUploadResult())
  }
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))
  return request.post(`/admin/reception/batches/${batchId}/staging-files`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 手工匹配暂存文件 */
export function manualMatchFile(fileId: number, data: ManualMatchData): Promise<unknown> {
  if (USE_MOCK) {
    return Promise.resolve({ success: true })
  }
  return request.put(`/admin/reception/staging-files/${fileId}/match`, data)
}

/** 更新条目验收结果 */
export function updateItemAcceptance(
  itemId: number,
  data: ItemAcceptanceData,
): Promise<unknown> {
  if (USE_MOCK) {
    return Promise.resolve({ success: true })
  }
  return request.put(`/admin/reception/items/${itemId}/acceptance`, data)
}

/** 完成批次验收 */
export function completeBatchAcceptance(
  batchId: number,
  data?: CompleteBatchData,
): Promise<unknown> {
  if (USE_MOCK) {
    return Promise.resolve({ success: true })
  }
  return request.post(`/admin/reception/batches/${batchId}/complete`, data)
}

/** 导出接收回执（PDF blob） */
export function exportReceipt(batchId: number): Promise<Blob> {
  if (USE_MOCK) {
    const blob = new Blob(['Mock 回执 PDF 内容'], { type: 'application/pdf' })
    return Promise.resolve(blob)
  }
  return request.get(`/admin/reception/batches/${batchId}/receipt`, {
    responseType: 'blob',
  }) as Promise<Blob>
}
```

- [ ] **Step 2: 创建 collection API**

```typescript
// src/api/collection.ts
import request from './request'
import type { PageData, PageParams } from '@/types/api'
import type {
  CollectionBatch,
  CollectionParams,
  ScheduleData,
  RejectData,
} from '@/types/collection'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询征集批次 */
export function getCollections(
  params?: CollectionParams & PageParams,
): Promise<PageData<CollectionBatch>> {
  if (USE_MOCK) {
    return import('@/mock/modules/collection').then((m) => {
      const all = m.mockCollectionBatches.filter((b) => {
        if (params?.status && b.status !== params.status) return false
        if (params?.keyword) {
          const kw = params.keyword.toLowerCase()
          const text = `${b.batchNo} ${b.title} ${b.donorName} ${b.donorPhone}`.toLowerCase()
          if (!text.includes(kw)) return false
        }
        return true
      })
      return {
        records: all,
        pageNo: params?.pageNo ?? 1,
        pageSize: params?.pageSize ?? 20,
        total: all.length,
        hasNext: false,
      }
    })
  }
  return request.get('/admin/collections', { params })
}

/** 约定到馆时间 */
export function scheduleCollection(
  batchId: number,
  data: ScheduleData,
): Promise<CollectionBatch> {
  if (USE_MOCK) {
    return import('@/mock/modules/collection').then((m) => {
      const batch = m.mockCollectionBatches.find((b) => b.id === batchId)
      if (!batch) throw new Error('批次不存在')
      return {
        ...batch,
        status: 'pending_receive',
        statusText: '待接收',
        scheduledReceiveAt: data.scheduledReceiveAt,
        contactNote: data.contactNote,
      }
    })
  }
  return request.post(`/admin/collections/${batchId}/schedule`, data)
}

/** 拒绝征集 */
export function rejectCollection(
  batchId: number,
  data: RejectData,
): Promise<CollectionBatch> {
  if (USE_MOCK) {
    return import('@/mock/modules/collection').then((m) => {
      const batch = m.mockCollectionBatches.find((b) => b.id === batchId)
      if (!batch) throw new Error('批次不存在')
      return {
        ...batch,
        status: 'rejected',
        statusText: '已拒绝',
        rejectReason: data.rejectReason,
      }
    })
  }
  return request.post(`/admin/collections/${batchId}/reject`, data)
}
```

- [ ] **Step 3: 类型检查**

Run: `cd frontend && npx vue-tsc --noEmit`
Expected: 无错误

- [ ] **Step 4: 提交**

```bash
git add src/api/reception.ts src/api/collection.ts
git commit -m "feat(acceptance-hu): 添加验收和征集模块 API 函数"
```

---

### Task 4: 移交验收页面

**Files:**
- Rewrite: `src/views/admin/transfer-reception/index.vue`

本页面直接翻译原型 `doc/prototype/admin/transfer-reception.html`，使用原型全局 CSS 类。

- [ ] **Step 1: 创建页面骨架 + script setup 数据层**

将完整 Vue SFC 写入 `src/views/admin/transfer-reception/index.vue`。由于文件较长（约 450 行），分 template / script / style 三段。

```vue
<!-- src/views/admin/transfer-reception/index.vue -->
<template>
  <section>
    <h1 class="page-title">移交验收与电子文件上传</h1>
    <p class="page-subtitle">
      调取待移交清单，核对纸质原件、页数、数量和签章；上传 U 盘电子文件后按文件名匹配清单条目，异常必须人工确认或回退。
    </p>
  </section>

  <!-- 概览指标 -->
  <section class="grid four" style="margin-top: 18px;" aria-label="前台待办统计">
    <div class="metric">
      <span class="label">今日到馆清单</span>
      <span class="value">{{ todayCount }}</span>
      <span class="note">按预计移交日期筛选</span>
    </div>
    <div class="metric">
      <span class="label">待验收条目</span>
      <span class="value">{{ pendingItemCount }}</span>
      <span class="note">条目状态：待验收</span>
    </div>
    <div class="metric">
      <span class="label">匹配异常</span>
      <span class="value">{{ abnormalCount }}</span>
      <span class="note">未匹配、重复、缺失或检查失败</span>
    </div>
    <div class="metric">
      <span class="label">待导出回执</span>
      <span class="value">{{ completedNotExportedCount }}</span>
      <span class="note">确认接收后导出 PDF</span>
    </div>
  </section>

  <!-- 筛选工具栏 -->
  <div class="toolbar">
    <div class="tabs" aria-label="待验收清单筛选">
      <button
        v-for="tab in filterTabs"
        :key="tab.key"
        class="tab"
        :class="{ active: activeFilter === tab.key }"
        type="button"
        @click="activeFilter = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>
    <div class="actions">
      <label class="field" style="min-width: 240px;">
        <span class="sr-only">搜索清单</span>
        <input v-model="searchKeyword" type="search" placeholder="搜索清单号、单位、标题">
      </label>
    </div>
  </div>

  <!-- 主布局：左侧列表 + 右侧详情 -->
  <section class="reception-layout">
    <!-- 左侧：待验收清单 -->
    <aside class="card panel batch-pane">
      <h2 class="section-title">待验收清单</h2>
      <div v-if="loading" style="padding: 20px; text-align: center; color: var(--muted);">加载中...</div>
      <div v-else-if="filteredBatches.length === 0" class="empty">当前筛选下没有待验收清单。</div>
      <div v-else class="batch-list">
        <button
          v-for="batch in filteredBatches"
          :key="batch.id"
          class="batch-card"
          :class="{ active: activeBatch?.batch.id === batch.id }"
          type="button"
          @click="selectBatch(batch.id)"
        >
          <strong>{{ batch.title }}</strong>
          <span class="mono">{{ batch.batchNo }}</span>
          <span class="muted">{{ batch.organizationName }} / {{ batch.departmentName }}</span>
          <span>
            <span class="status info">{{ batch.statusText }}</span>
            <span class="muted">{{ batch.itemCount }} 条</span>
          </span>
        </button>
      </div>
    </aside>

    <!-- 右侧：详情面板 -->
    <div v-if="!activeBatch" class="grid detail-pane">
      <div class="card panel" style="text-align: center; padding: 40px; color: var(--muted);">
        请在左侧选择一个待验收清单
      </div>
    </div>

    <div v-else class="grid detail-pane">
      <!-- 清单详情 -->
      <section class="card panel">
        <div class="split-panel">
          <div>
            <div class="detail-head">
              <h2 class="section-title">{{ activeBatch.batch.title }}</h2>
              <p class="muted" style="margin-top: -6px;">
                {{ activeBatch.batch.batchNo }} / {{ activeBatch.batch.organizationName }} / {{ activeBatch.batch.contactPerson }} {{ activeBatch.batch.contactPhone }}
              </p>
            </div>
            <div class="summary-row" style="margin-top: 12px;">
              <div class="mini"><span class="muted">批次状态</span><strong>{{ batchStatusLabel }}</strong></div>
              <div class="mini"><span class="muted">签字清单</span><strong>{{ activeBatch.batch.signatureStatus || '—' }}</strong></div>
              <div class="mini"><span class="muted">预计移交</span><strong>{{ activeBatch.batch.expectedTransferDate || '—' }}</strong></div>
            </div>
          </div>
          <div class="notice warning">
            前台只记录验收、暂存文件和回执。AI 补全、正式档号、盒号和架位由后台入库页面处理。
          </div>
        </div>
      </section>

      <!-- U盘文件上传与匹配 -->
      <details class="card panel collapsible" :open="activeBatch.stagingFiles.length > 0">
        <summary>
          <span class="section-title">U 盘电子文件上传与匹配</span>
          <span class="status info">{{ activeBatch.stagingFiles.length > 0 ? '已上传' : '点击展开' }}</span>
        </summary>
        <div class="collapsible-body">
          <div
            class="drop-zone"
            :class="{ dragover: isDragOver }"
            @dragover.prevent="isDragOver = true"
            @dragleave="isDragOver = false"
            @drop.prevent="handleDrop"
          >
            <div>
              <strong>拖拽 U 盘文件或选择文件</strong>
              <p class="muted" style="margin: 6px 0 0;">
                上传后进入暂存路径，系统按清单中的档案文件名匹配，并执行格式、大小、哈希和安全检查。
              </p>
            </div>
            <label class="button secondary" for="filePicker">选择 U 盘文件</label>
            <input id="filePicker" class="sr-only" type="file" multiple @change="handleFilePick">
          </div>
          <div v-if="uploading" style="text-align: center; padding: 12px; color: var(--muted);">文件上传中...</div>
          <div v-if="activeBatch.stagingFiles.length > 0" class="match-grid">
            <div
              v-for="file in activeBatch.stagingFiles"
              :key="file.id"
              class="match-card"
            >
              <div>
                <strong>{{ file.originalFilename }}</strong>
                <div class="hint">{{ fileScanHint(file) }}</div>
              </div>
              <span class="status" :class="matchStatusClass(file.matchStatus)">{{ matchStatusLabel(file.matchStatus) }}</span>
            </div>
          </div>
        </div>
      </details>

      <!-- 条目验收 -->
      <section>
        <div class="toolbar" style="margin-top: 0;">
          <h2 class="section-title" style="margin: 0;">条目验收</h2>
          <div class="actions">
            <button class="button secondary" type="button" @click="acceptAllPaper">批量纸质验收通过</button>
            <button class="button" type="button" @click="confirmReceive">确认接收</button>
          </div>
        </div>
        <div class="table-wrap scroll-y">
          <table class="acceptance-table">
            <thead>
              <tr>
                <th>序号</th>
                <th>档案标题</th>
                <th>载体</th>
                <th>清单文件名</th>
                <th>纸质核对</th>
                <th>文件匹配</th>
                <th>验收结论</th>
                <th>说明 / 回退原因</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in activeBatch.items" :key="item.id">
                <td>{{ item.seqNo }}</td>
                <td>{{ item.title }}</td>
                <td>{{ item.carrierType }}</td>
                <td class="mono">{{ item.expectedFilename || '无电子文件' }}</td>
                <td>
                  <select v-model="item.paperCheckStatus">
                    <option value="pending">待核对</option>
                    <option value="passed">通过</option>
                    <option value="failed">异常</option>
                  </select>
                </td>
                <td><span class="status" :class="matchStatusClass(item.fileMatchStatus)">{{ matchStatusLabel(item.fileMatchStatus) }}</span></td>
                <td>
                  <select v-model="item.result">
                    <option value="pending">待验收</option>
                    <option value="accepted">接收</option>
                    <option value="rejected">回退</option>
                  </select>
                </td>
                <td><input v-model="item.acceptanceNote" placeholder="验收说明或回退原因"></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- 接收回执预览 -->
      <section class="card panel">
        <div class="split-panel">
          <div>
            <h2 class="section-title">接收回执预览</h2>
            <div class="receipt-preview">
              <div>
                <span class="status success">已接收 {{ acceptedCount }} 条</span>
                <span class="status" :class="rejectedCount ? 'danger' : ''">
                  {{ rejectedCount ? `回退 ${rejectedCount} 条` : '' }}
                </span>
                <span class="status" :class="pendingItemCountInBatch ? 'info' : 'success'">
                  待处理 {{ pendingItemCountInBatch }} 条
                </span>
              </div>
              <strong>回执内容</strong>
              <span class="muted">已接收条目：{{ acceptedItemsText || '暂无' }}</span>
              <span class="muted">回退条目：{{ rejectedItemsText || '暂无' }}</span>
            </div>
          </div>
          <div class="actions" style="align-content: start;">
            <button
              class="button secondary"
              type="button"
              :disabled="pendingItemCountInBatch > 0"
              @click="handleExportReceipt"
            >
              导出接收回执
            </button>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

- [ ] **Step 2: 添加 script setup 逻辑**

紧接在 `</template>` 之后、`<style>` 之前：

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { ReceptionBatch, BatchDetail, StagingFile } from '@/types/reception'
import {
  getReceptionBatches,
  getReceptionBatchDetail,
  uploadStagingFiles,
  completeBatchAcceptance,
  exportReceipt,
} from '@/api/reception'

// ── 筛选状态 ──
const filterTabs = [
  { key: 'pending', label: '待移交' },
  { key: 'today', label: '今日到馆' },
  { key: 'abnormal', label: '存在异常' },
] as const
const activeFilter = ref<string>('pending')
const searchKeyword = ref('')
const loading = ref(false)

// ── 数据 ──
const batchList = ref<ReceptionBatch[]>([])
const activeBatch = ref<BatchDetail | null>(null)
const uploading = ref(false)
const isDragOver = ref(false)

// ── 概览指标 ──
const todayCount = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return batchList.value.filter((b) => b.expectedTransferDate === today).length
})
const pendingItemCount = computed(() => {
  if (!activeBatch.value) return 0
  return activeBatch.value.items.filter((i) => i.result === 'pending').length
})
const abnormalCount = computed(() => {
  if (!activeBatch.value) return 0
  const fileAbnormal = activeBatch.value.stagingFiles.filter(
    (f) => f.matchStatus !== 'matched',
  ).length
  const itemAbnormal = activeBatch.value.items.filter(
    (i) => i.fileMatchStatus === 'missing' || i.fileMatchStatus === 'failed',
  ).length
  return fileAbnormal + itemAbnormal
})
const completedNotExportedCount = computed(() => {
  // 简化：已接收或部分接收但未导出的批次
  return batchList.value.filter(
    (b) => b.status === 'received' || b.status === 'partially_received',
  ).length
})

// ── 筛选 ──
const filteredBatches = computed(() => {
  return batchList.value.filter((b) => {
    // 关键字
    if (searchKeyword.value) {
      const kw = searchKeyword.value.toLowerCase()
      const text = `${b.batchNo} ${b.title} ${b.organizationName}`.toLowerCase()
      if (!text.includes(kw)) return false
    }
    // 标签筛选
    if (activeFilter.value === 'today') {
      const today = new Date().toISOString().slice(0, 10)
      return b.expectedTransferDate === today
    }
    if (activeFilter.value === 'abnormal') {
      // 简化：标记为异常的批次（实际应看条目文件匹配状态）
      return true
    }
    return true
  })
})

// ── 批次详情计算属性 ──
const batchStatusLabel = computed(() => {
  if (!activeBatch.value) return ''
  const pending = activeBatch.value.items.filter((i) => i.result === 'pending').length
  const rejected = activeBatch.value.items.filter((i) => i.result === 'rejected').length
  if (pending > 0) return '待移交'
  return rejected > 0 ? '部分接收' : '已接收'
})
const acceptedCount = computed(() =>
  activeBatch.value ? activeBatch.value.items.filter((i) => i.result === 'accepted').length : 0,
)
const rejectedCount = computed(() =>
  activeBatch.value ? activeBatch.value.items.filter((i) => i.result === 'rejected').length : 0,
)
const pendingItemCountInBatch = computed(() =>
  activeBatch.value ? activeBatch.value.items.filter((i) => i.result === 'pending').length : 0,
)
const acceptedItemsText = computed(() => {
  if (!activeBatch.value) return ''
  return activeBatch.value.items
    .filter((i) => i.result === 'accepted')
    .map((i) => String(i.seqNo).padStart(2, '0'))
    .join('、')
})
const rejectedItemsText = computed(() => {
  if (!activeBatch.value) return ''
  return activeBatch.value.items
    .filter((i) => i.result === 'rejected')
    .map((i) => `${String(i.seqNo).padStart(2, '0')}（${i.acceptanceNote || '未填写原因'}）`)
    .join('；')
})

// ── 匹配状态工具函数 ──
function matchStatusLabel(status: string): string {
  const map: Record<string, string> = {
    none: '未上传',
    matched: '匹配成功',
    missing: '清单缺文件',
    unmatched: '多余文件',
    duplicate: '重复匹配',
    failed: '检查失败',
    staging: '暂存中',
  }
  return map[status] || status
}
function matchStatusClass(status: string): string {
  const map: Record<string, string> = {
    none: '',
    matched: 'success',
    missing: 'warning',
    unmatched: 'warning',
    duplicate: 'warning',
    failed: 'danger',
    staging: 'info',
  }
  return map[status] || ''
}
function fileScanHint(file: StagingFile): string {
  const parts: string[] = []
  if (file.matchStatus === 'matched') {
    parts.push('格式检查通过、哈希已记录、安全检查通过')
  } else if (file.matchStatus === 'unmatched') {
    parts.push('文件名与清单不一致，需人工确认或回退')
  } else if (file.matchStatus === 'duplicate') {
    parts.push('同名重复，需前台人工选择对应条目')
  }
  return parts.join('；')
}

// ── 加载数据 ──
async function loadBatches() {
  loading.value = true
  try {
    const res = await getReceptionBatches({ sourceType: 'transfer' })
    batchList.value = res.records
  } catch {
    ElMessage.error('加载待验收清单失败')
  } finally {
    loading.value = false
  }
}

async function selectBatch(batchId: number) {
  try {
    activeBatch.value = await getReceptionBatchDetail(batchId)
  } catch {
    ElMessage.error('加载批次详情失败')
  }
}

// ── 文件上传 ──
async function handleFileUpload(files: File[]) {
  if (!activeBatch.value || files.length === 0) return
  uploading.value = true
  try {
    const result = await uploadStagingFiles(activeBatch.value.batch.id, files)
    activeBatch.value.stagingFiles = result.files
    activeBatch.value.matchSummary = result.matchSummary
    // 更新条目匹配状态
    result.files.forEach((sf) => {
      if (sf.matchedItemId) {
        const item = activeBatch.value!.items.find((i) => i.id === sf.matchedItemId)
        if (item) item.fileMatchStatus = 'matched'
      }
    })
    ElMessage.success(`上传完成，匹配成功 ${result.matchSummary.matched} 个文件`)
  } catch {
    ElMessage.error('文件上传失败')
  } finally {
    uploading.value = false
  }
}
function handleDrop(e: DragEvent) {
  isDragOver.value = false
  const files = Array.from(e.dataTransfer?.files ?? [])
  handleFileUpload(files)
}
function handleFilePick(e: Event) {
  const input = e.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  handleFileUpload(files)
  input.value = ''
}

// ── 批量纸质验收通过 ──
function acceptAllPaper() {
  if (!activeBatch.value) return
  activeBatch.value.items.forEach((item) => {
    item.paperCheckStatus = 'passed'
    if (item.carrierType === '纯纸质' || item.fileMatchStatus === 'matched') {
      item.result = 'accepted'
      if (!item.acceptanceNote) item.acceptanceNote = '纸质核对通过'
    }
  })
  ElMessage.success('已批量标记纸质验收通过，电子匹配异常仍需人工处理。')
}

// ── 确认接收 ──
async function confirmReceive() {
  if (!activeBatch.value) return
  const items = activeBatch.value.items
  const missingReason = items.find((i) => i.result === 'rejected' && !i.acceptanceNote?.trim())
  if (missingReason) {
    ElMessage.error(`第 ${String(missingReason.seqNo).padStart(2, '0')} 条回退前必须填写回退原因。`)
    return
  }
  const pending = items.filter((i) => i.result === 'pending')
  if (pending.length > 0) {
    ElMessage.error(`仍有 ${pending.length} 条待验收，不能确认接收。`)
    return
  }
  try {
    await completeBatchAcceptance(activeBatch.value.batch.id, { acceptanceNote: '现场清点完成' })
    const rejected = items.filter((i) => i.result === 'rejected').length
    ElMessage.success(
      rejected ? '已确认部分接收，回退条目将写入回执。' : '已确认全部接收，条目进入后台待入库。',
    )
  } catch {
    ElMessage.error('确认接收失败')
  }
}

// ── 导出回执 ──
async function handleExportReceipt() {
  if (!activeBatch.value) return
  try {
    const blob = await exportReceipt(activeBatch.value.batch.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `回执-${activeBatch.value.batch.batchNo}.pdf`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('接收回执已生成')
  } catch {
    ElMessage.error('导出回执失败')
  }
}

onMounted(() => {
  loadBatches()
})
</script>

- [ ] **Step 3: 添加 scoped style**

紧接在 `</script>` 之后：

```vue
<style scoped>
.reception-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
  height: calc(100vh - 286px);
  min-height: 620px;
}

.batch-pane,
.detail-pane {
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
}

.batch-pane {
  max-height: 100%;
}

.detail-pane > .card,
.detail-pane > details,
.detail-pane > section {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.batch-card {
  display: grid;
  gap: 8px;
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: #ffffff;
  text-align: left;
  transition: border-color 0.16s ease, background 0.16s ease;
}

.batch-card:hover,
.batch-card.active {
  border-color: #8abcbf;
  background: #f2f8f8;
}

.batch-list {
  display: grid;
  gap: 10px;
  padding-right: 4px;
}

.split-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  align-items: start;
  min-width: 0;
}

.drop-zone {
  display: grid;
  min-height: 146px;
  place-items: center;
  padding: 20px;
  border: 2px dashed #9bbdc0;
  border-radius: var(--radius);
  color: #23494f;
  background: #f7fcfc;
  text-align: center;
}

.drop-zone.dragover {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.collapsible summary {
  display: flex;
  min-height: 34px;
  cursor: pointer;
  list-style: none;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.collapsible summary::-webkit-details-marker {
  display: none;
}

.collapsible-body {
  display: grid;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.match-grid {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.match-card {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  align-items: center;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
}

.acceptance-table {
  min-width: 980px;
}

.acceptance-table input,
.acceptance-table select {
  width: 100%;
  min-height: 34px;
  padding: 6px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
}

.receipt-preview {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px dashed #b8c5ce;
  border-radius: var(--radius);
  background: #f7fafc;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.summary-row .mini {
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
}

.summary-row strong {
  display: block;
  font-size: 21px;
}

@media (max-width: 1220px) {
  .reception-layout,
  .split-panel {
    grid-template-columns: 1fr;
  }

  .reception-layout {
    height: auto;
    min-height: 0;
  }

  .batch-pane,
  .detail-pane {
    max-height: none;
  }
}
</style>
```

- [ ] **Step 4: 类型检查 + 构建**

Run: `cd frontend && npx vue-tsc --noEmit && npx vite build`
Expected: 无错误

- [ ] **Step 5: 提交**

```bash
git add src/views/admin/transfer-reception/index.vue
git commit -m "feat(acceptance-hu): 实现移交验收与电子文件上传页面"
```

---

### Task 5: 征集管理页面

**Files:**
- Rewrite: `src/views/admin/collection/index.vue`

本页面翻译原型 `doc/prototype/admin/collection.html`，按角色条件渲染不同操作区。

- [ ] **Step 1: 创建页面 template**

```vue
<!-- src/views/admin/collection/index.vue -->
<template>
  <section>
    <h1 class="page-title">征集管理与接收</h1>
    <p class="page-subtitle">处理公众征集清单的联系判断、到馆接收、在线协议记录校验和回执导出。</p>
  </section>

  <!-- 概览指标 -->
  <section class="grid four" aria-label="征集待办统计">
    <div class="metric">
      <span class="label">待联系</span>
      <span class="value">{{ contactCount }}</span>
      <span class="note">后台判断征集方向</span>
    </div>
    <div class="metric">
      <span class="label">待接收</span>
      <span class="value">{{ receiveCount }}</span>
      <span class="note">已约定到馆</span>
    </div>
    <div class="metric">
      <span class="label">今日到馆</span>
      <span class="value">{{ todayArrivalCount }}</span>
      <span class="note">需前台验收</span>
    </div>
    <div class="metric">
      <span class="label">协议记录异常</span>
      <span class="value">{{ agreementErrorCount }}</span>
      <span class="note">提交时在线同意</span>
    </div>
  </section>

  <!-- 主布局 -->
  <section class="work-layout" style="margin-top: 16px;">
    <!-- 左侧：批次列表 -->
    <div class="card panel scroll-pane batch-pane">
      <div class="toolbar">
        <div>
          <h2 class="section-title">征集批次</h2>
          <p class="page-subtitle">清单来源为社会征集，提交后公众端只读。</p>
        </div>
        <div class="tabs" aria-label="批次状态筛选">
          <button
            v-for="tab in collectionTabs"
            :key="tab.key"
            class="tab"
            :class="{ active: activeTab === tab.key }"
            type="button"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>

      <div v-if="loading" style="padding: 20px; text-align: center; color: var(--muted);">加载中...</div>
      <div v-else-if="filteredCollections.length === 0" class="empty" style="padding: 20px; text-align: center; color: var(--muted);">
        当前筛选下没有征集批次。
      </div>
      <ul v-else class="batch-list">
        <li
          v-for="batch in filteredCollections"
          :key="batch.id"
          class="batch-item"
          :class="{ active: selectedBatch?.id === batch.id }"
          @click="selectBatch(batch)"
        >
          <div class="batch-top">
            <h3 class="batch-title">{{ batch.title }}</h3>
            <span class="status" :class="statusClass(batch.status)">{{ batch.statusText }}</span>
          </div>
          <div class="meta-line">
            <span>{{ batch.batchNo }}</span>
            <span>捐赠人：{{ batch.donorName }}</span>
            <span>条目：{{ batch.itemCount }}</span>
            <span v-if="batch.scheduledReceiveAt">到馆：{{ formatDateTime(batch.scheduledReceiveAt) }}</span>
            <span v-else>提交：{{ batch.submittedAt.slice(0, 10) }}</span>
          </div>
        </li>
      </ul>
    </div>

    <!-- 右侧：详情区 -->
    <div v-if="!selectedBatch" class="grid scroll-pane detail-pane">
      <div class="card panel" style="text-align: center; padding: 40px; color: var(--muted);">
        请在左侧选择一个征集批次
      </div>
    </div>

    <div v-else class="grid scroll-pane detail-pane">
      <!-- 基本信息 -->
      <div class="card panel">
        <div class="toolbar">
          <div>
            <h2 class="section-title">{{ selectedBatch.title }}</h2>
            <p class="page-subtitle">
              {{ selectedBatch.batchNo }} · {{ selectedBatch.donorName }} · {{ batchPhaseLabel }}
            </p>
          </div>
          <span class="status" :class="statusClass(selectedBatch.status)">{{ selectedBatch.statusText }}</span>
        </div>
        <div class="detail-grid">
          <div class="info-tile"><span>联系电话</span><strong>{{ selectedBatch.donorPhone }}</strong></div>
          <div class="info-tile"><span>来源标识</span><strong>征集</strong></div>
          <div class="info-tile"><span>目标属性</span><strong>永久 / 非密 / 公开</strong></div>
          <div class="info-tile"><span>公开生效</span><strong>正式入库并满足利用条件后</strong></div>
        </div>
      </div>

      <!-- 后台联系判断（仅 back_archivist + pending_contact 状态） -->
      <div v-if="isBackArchivist && selectedBatch.status === 'pending_contact'" class="card panel">
        <h2 class="section-title">联系判断</h2>
        <div class="form-grid">
          <div class="field">
            <label>约定到馆时间</label>
            <input v-model="scheduleForm.scheduledReceiveAt" type="datetime-local">
          </div>
          <div class="field">
            <label>联系结果</label>
            <select v-model="scheduleForm.contactResult">
              <option value="符合征集方向">符合征集方向</option>
              <option value="需补充说明">需补充说明</option>
              <option value="不符合征集方向">不符合征集方向</option>
            </select>
          </div>
          <div class="field">
            <label>拒绝或备注</label>
            <input v-model="rejectForm.rejectReason" placeholder="拒绝时必填">
          </div>
        </div>
        <div class="actions" style="margin-top: 12px;">
          <button class="button" type="button" @click="handleSchedule">约定到馆</button>
          <button class="button danger" type="button" @click="handleReject">拒绝征集</button>
        </div>
      </div>

      <!-- 前台到馆接收（仅 front_archivist + pending_receive 状态） -->
      <div v-if="selectedBatch.status === 'pending_receive' || selectedBatch.status === 'received' || selectedBatch.status === 'partially_received'" class="card panel">
        <h2 class="section-title">到馆接收</h2>
        <div class="form-grid">
          <div class="info-tile">
            <span>在线协议</span>
            <strong v-if="selectedBatch.agreementAcceptedAt">已同意 · {{ formatDateTime(selectedBatch.agreementAcceptedAt) }}</strong>
            <strong v-else style="color: var(--danger);">未同意 — 不可完成接收</strong>
          </div>
          <div class="info-tile">
            <span>协议文案</span>
            <strong>collection.agreement_text</strong>
          </div>
        </div>

        <!-- 文件上传（仅前台 + 待接收） -->
        <template v-if="isFrontArchivist && selectedBatch.status === 'pending_receive'">
          <details class="collapsible upload-block" :open="stagingFiles.length > 0">
            <summary>
              <span class="section-title">U 盘电子文件上传与匹配</span>
              <span class="status info">{{ stagingFiles.length > 0 ? '已上传' : '点击展开' }}</span>
            </summary>
            <div class="collapsible-body">
              <div
                class="drop-zone"
                :class="{ dragover: isDragOver }"
                @dragover.prevent="isDragOver = true"
                @dragleave="isDragOver = false"
                @drop.prevent="handleDrop"
              >
                <div>
                  <strong>拖拽捐赠电子文件或选择文件</strong>
                  <p class="muted" style="margin: 6px 0 0;">
                    上传后写入暂存路径，按清单文件名匹配；回退条目会删除暂存对象并保留记录。
                  </p>
                </div>
                <label class="button secondary" for="collectionFilePicker">选择 U 盘文件</label>
                <input id="collectionFilePicker" class="sr-only" type="file" multiple @change="handleFilePick">
              </div>
              <div v-if="uploading" style="text-align: center; padding: 12px; color: var(--muted);">文件上传中...</div>
              <div v-if="stagingFiles.length > 0" class="match-grid">
                <div v-for="file in stagingFiles" :key="file.id" class="match-card">
                  <div>
                    <strong>{{ file.originalFilename }}</strong>
                    <div class="hint">{{ fileScanHint(file) }}</div>
                  </div>
                  <span class="status" :class="matchStatusClass(file.matchStatus)">{{ matchStatusLabel(file.matchStatus) }}</span>
                </div>
              </div>
            </div>
          </details>

          <!-- 条目验收表格 -->
          <div class="table-wrap scroll-y" style="margin-top: 14px;">
            <table class="review-table">
              <thead>
                <tr>
                  <th>条目</th>
                  <th>载体</th>
                  <th>验收结果</th>
                  <th>回退原因</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in receptionItems" :key="item.id">
                  <td>{{ item.title }}</td>
                  <td>{{ item.carrierType }}</td>
                  <td>
                    <select v-model="item.result">
                      <option value="pending">待验收</option>
                      <option value="accepted">已接收</option>
                      <option value="rejected">已回退</option>
                    </select>
                  </td>
                  <td><input v-model="item.acceptanceNote" placeholder="回退时填写"></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="actions" style="margin-top: 12px;">
            <button class="button" type="button" @click="handleCompleteReceive">完成接收</button>
            <button class="button secondary" type="button" :disabled="pendingItemsCount > 0" @click="handleExportReceipt">导出回执</button>
          </div>
        </template>
      </div>

      <!-- 流程位置 -->
      <div class="card panel">
        <h2 class="section-title">流程位置</h2>
        <ol class="flow-note">
          <li>
            <span class="flow-no">1</span>
            <span><strong>公众提交</strong><br><span class="muted">清单进入待联系，公众端只读。</span></span>
          </li>
          <li>
            <span class="flow-no">2</span>
            <span><strong>后台联系</strong><br><span class="muted">符合征集方向后约定到馆，或填写理由拒绝。</span></span>
          </li>
          <li>
            <span class="flow-no">3</span>
            <span><strong>前台接收</strong><br><span class="muted">核对实物和电子介质，校验在线协议记录并导出回执。</span></span>
          </li>
          <li>
            <span class="flow-no">4</span>
            <span><strong>后续入库</strong><br><span class="muted">已接收条目进入 AI 补全、人工核对、装盒、入库、上架。</span></span>
          </li>
        </ol>
      </div>
    </div>
  </section>
</template>

- [ ] **Step 2: 添加 script setup 逻辑**

```vue
<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { CollectionBatch, ScheduleData } from '@/types/collection'
import type { BatchDetail, ReceptionItem, StagingFile } from '@/types/reception'
import { usePermission } from '@/composables/usePermission'
import { getCollections, scheduleCollection, rejectCollection } from '@/api/collection'
import {
  getReceptionBatchDetail,
  uploadStagingFiles,
  completeBatchAcceptance,
  exportReceipt,
} from '@/api/reception'

const { hasRole } = usePermission()
const isBackArchivist = computed(() => hasRole('back_archivist'))
const isFrontArchivist = computed(() => hasRole('front_archivist'))

// ── 筛选 ──
const collectionTabs = [
  { key: 'all', label: '全部' },
  { key: 'pending_contact', label: '待联系' },
  { key: 'pending_receive', label: '待接收' },
] as const
const activeTab = ref<string>('all')
const loading = ref(false)

// ── 数据 ──
const batchList = ref<CollectionBatch[]>([])
const selectedBatch = ref<CollectionBatch | null>(null)
const batchDetail = ref<BatchDetail | null>(null)

// ── 表单 ──
const scheduleForm = ref<ScheduleData & { contactResult: string }>({
  scheduledReceiveAt: '',
  contactNote: '',
  contactResult: '符合征集方向',
})
const rejectForm = ref({ rejectReason: '' })

// ── 文件上传 ──
const stagingFiles = ref<StagingFile[]>([])
const uploading = ref(false)
const isDragOver = ref(false)

// ── 条目（从 batchDetail 中取） ──
const receptionItems = computed(() => batchDetail.value?.items ?? [])
const pendingItemsCount = computed(() => receptionItems.value.filter((i) => i.result === 'pending').length)

// ── 概览指标 ──
const contactCount = computed(() => batchList.value.filter((b) => b.status === 'pending_contact').length)
const receiveCount = computed(() => batchList.value.filter((b) => b.status === 'pending_receive').length)
const todayArrivalCount = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return batchList.value.filter((b) => {
    if (b.status !== 'pending_receive' || !b.scheduledReceiveAt) return false
    return b.scheduledReceiveAt.startsWith(today)
  }).length
})
const agreementErrorCount = computed(() => 0)

// ── 筛选列表 ──
const filteredCollections = computed(() => {
  return batchList.value.filter((b) => {
    // 前台不可见 pending_contact 和 rejected
    if (isFrontArchivist.value && (b.status === 'pending_contact' || b.status === 'rejected')) return false
    // 标签筛选
    if (activeTab.value !== 'all' && b.status !== activeTab.value) return false
    return true
  })
})

const batchPhaseLabel = computed(() => {
  if (!selectedBatch.value) return ''
  const map: Record<string, string> = {
    pending_contact: '当前待后台联系判断',
    pending_receive: '当前待前台到馆接收',
    received: '已接收完成',
    partially_received: '部分接收完成',
    rejected: '已拒绝',
  }
  return map[selectedBatch.value.status] || ''
})

// ── 工具函数 ──
function statusClass(status: string): string {
  const map: Record<string, string> = {
    pending_contact: 'warning',
    pending_receive: 'info',
    received: 'success',
    partially_received: 'info',
    rejected: 'danger',
  }
  return map[status] || ''
}

function formatDateTime(iso: string): string {
  if (!iso) return ''
  const d = new Date(iso)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function matchStatusLabel(status: string): string {
  const map: Record<string, string> = {
    none: '未上传', matched: '匹配成功', missing: '清单缺文件',
    unmatched: '多余文件', duplicate: '重复提示', failed: '检查失败', staging: '暂存中',
  }
  return map[status] || status
}

function matchStatusClass(status: string): string {
  const map: Record<string, string> = {
    none: '', matched: 'success', missing: 'warning',
    unmatched: 'warning', duplicate: 'warning', failed: 'danger', staging: 'info',
  }
  return map[status] || ''
}

function fileScanHint(file: StagingFile): string {
  if (file.matchStatus === 'matched') return '格式和安全检查通过'
  if (file.matchStatus === 'unmatched') return '文件名与清单不一致，需人工确认'
  if (file.matchStatus === 'duplicate') return '与已入库影像疑似重复'
  return ''
}

// ── 加载数据 ──
async function loadCollections() {
  loading.value = true
  try {
    const res = await getCollections()
    batchList.value = res.records
  } catch {
    ElMessage.error('加载征集批次失败')
  } finally {
    loading.value = false
  }
}

async function selectBatch(batch: CollectionBatch) {
  selectedBatch.value = batch
  batchDetail.value = null
  stagingFiles.value = []
  if (batch.status === 'pending_receive' || batch.status === 'received' || batch.status === 'partially_received') {
    try {
      batchDetail.value = await getReceptionBatchDetail(batch.id)
      stagingFiles.value = batchDetail.value.stagingFiles
    } catch {
      ElMessage.error('加载批次详情失败')
    }
  }
}

// ── 后台操作 ──
async function handleSchedule() {
  if (!selectedBatch.value) return
  if (!scheduleForm.value.scheduledReceiveAt) {
    ElMessage.error('请选择约定到馆时间')
    return
  }
  try {
    const updated = await scheduleCollection(selectedBatch.value.id, {
      scheduledReceiveAt: scheduleForm.value.scheduledReceiveAt,
      contactNote: scheduleForm.value.contactResult,
    })
    Object.assign(selectedBatch.value, updated)
    ElMessage.success('已约定到馆时间，批次状态变更为待接收')
  } catch {
    ElMessage.error('约定到馆失败')
  }
}

async function handleReject() {
  if (!selectedBatch.value) return
  if (!rejectForm.value.rejectReason.trim()) {
    ElMessage.error('拒绝征集必须填写拒绝理由')
    return
  }
  try {
    const updated = await rejectCollection(selectedBatch.value.id, {
      rejectReason: rejectForm.value.rejectReason,
    })
    Object.assign(selectedBatch.value, updated)
    ElMessage.success('已拒绝该征集意向')
  } catch {
    ElMessage.error('拒绝操作失败')
  }
}

// ── 前台文件上传 ──
async function handleFileUpload(files: File[]) {
  if (!selectedBatch.value || files.length === 0) return
  uploading.value = true
  try {
    const result = await uploadStagingFiles(selectedBatch.value.id, files)
    stagingFiles.value = result.files
    result.files.forEach((sf) => {
      if (sf.matchedItemId) {
        const item = batchDetail.value?.items.find((i) => i.id === sf.matchedItemId)
        if (item) item.fileMatchStatus = 'matched'
      }
    })
    ElMessage.success(`上传完成，匹配成功 ${result.matchSummary.matched} 个文件`)
  } catch {
    ElMessage.error('文件上传失败')
  } finally {
    uploading.value = false
  }
}

function handleDrop(e: DragEvent) {
  isDragOver.value = false
  const files = Array.from(e.dataTransfer?.files ?? [])
  handleFileUpload(files)
}

function handleFilePick(e: Event) {
  const input = e.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  handleFileUpload(files)
  input.value = ''
}

// ── 前台完成接收 ──
async function handleCompleteReceive() {
  if (!selectedBatch.value || !batchDetail.value) return
  const items = batchDetail.value.items
  const missingReason = items.find((i) => i.result === 'rejected' && !i.acceptanceNote?.trim())
  if (missingReason) {
    ElMessage.error(`条目"${missingReason.title}"回退前必须填写原因`)
    return
  }
  const pending = items.filter((i) => i.result === 'pending')
  if (pending.length > 0) {
    ElMessage.error(`仍有 ${pending.length} 条待验收，不能完成接收`)
    return
  }
  // 检查在线协议
  if (!selectedBatch.value.agreementAcceptedAt) {
    ElMessage.error('缺少在线协议同意记录，不可完成接收')
    return
  }
  try {
    await completeBatchAcceptance(selectedBatch.value.id, { acceptanceNote: '征集到馆验收完成' })
    ElMessage.success('已确认接收，条目进入后续入库流程')
  } catch {
    ElMessage.error('完成接收失败')
  }
}

// ── 导出回执 ──
async function handleExportReceipt() {
  if (!selectedBatch.value) return
  try {
    const blob = await exportReceipt(selectedBatch.value.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `征集回执-${selectedBatch.value.batchNo}.pdf`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('回执已生成')
  } catch {
    ElMessage.error('导出回执失败')
  }
}

onMounted(() => {
  loadCollections()
})
</script>

- [ ] **Step 3: 添加 scoped style**

```vue
<style scoped>
.work-layout {
  display: grid;
  grid-template-columns: minmax(420px, 0.95fr) minmax(0, 1.05fr);
  gap: 16px;
  align-items: stretch;
  height: calc(100vh - 256px);
  min-height: 560px;
}

.scroll-pane {
  min-height: 0;
  overflow-y: auto;
}

.detail-pane {
  padding-right: 4px;
}

.batch-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.batch-item {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
  cursor: pointer;
}

.batch-item.active {
  border-color: #9cc7ca;
  background: #f6fbfb;
  box-shadow: 0 0 0 3px rgba(31, 111, 120, 0.08);
}

.batch-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.batch-title {
  margin: 0;
  font-size: 15px;
}

.meta-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--muted);
  font-size: 13px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.info-tile {
  padding: 11px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
}

.info-tile span {
  display: block;
  color: var(--muted);
  font-size: 12px;
  font-weight: 700;
}

.info-tile strong {
  display: block;
  margin-top: 4px;
  overflow-wrap: anywhere;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.form-grid .field {
  display: grid;
  gap: 4px;
}

.form-grid .field label {
  font-size: 13px;
  color: var(--muted);
  font-weight: 600;
}

.form-grid .field input,
.form-grid .field select {
  min-height: 36px;
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
}

.review-table select,
.review-table input {
  width: 100%;
  min-width: 120px;
  min-height: 34px;
  padding: 7px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
}

.drop-zone {
  display: grid;
  min-height: 132px;
  place-items: center;
  padding: 18px;
  border: 2px dashed #9bbdc0;
  border-radius: var(--radius);
  color: #23494f;
  background: #f7fcfc;
  text-align: center;
}

.drop-zone.dragover {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.match-grid {
  display: grid;
  gap: 10px;
}

.match-card {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
}

.collapsible summary {
  display: flex;
  min-height: 34px;
  cursor: pointer;
  list-style: none;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.collapsible summary::-webkit-details-marker {
  display: none;
}

.collapsible-body {
  display: grid;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.upload-block {
  margin-top: 14px;
  padding: 12px 0 0;
  border-top: 1px solid var(--border);
}

.flow-note {
  display: grid;
  gap: 9px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.flow-note li {
  display: grid;
  grid-template-columns: 34px 1fr;
  gap: 10px;
  align-items: start;
}

.flow-no {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  background: var(--primary);
  font-size: 13px;
  font-weight: 800;
}

@media (max-width: 1120px) {
  .work-layout,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .work-layout {
    height: auto;
    min-height: 0;
  }

  .scroll-pane {
    overflow: visible;
  }
}
</style>
```

- [ ] **Step 4: 类型检查 + 构建**

Run: `cd frontend && npx vue-tsc --noEmit && npx vite build`
Expected: 无错误

- [ ] **Step 5: 提交**

```bash
git add src/views/admin/collection/index.vue
git commit -m "feat(acceptance-hu): 实现征集管理与接收页面"
```

---

### Task 6: 整体验证与提交

**Files:** 无新文件，验证所有已修改文件

- [ ] **Step 1: 完整类型检查**

Run: `cd frontend && npx vue-tsc --noEmit`
Expected: 无错误

- [ ] **Step 2: 生产构建**

Run: `cd frontend && npx vite build`
Expected: 构建成功，无警告

- [ ] **Step 3: 启动开发服务器并验证页面**

Run: `cd frontend && npx vite --port 5173`

在浏览器中验证：

1. 使用 `front` 账号（前台管理员）登录管理后台
2. 访问 `/admin/transfer-reception`：
   - 左侧显示 2 个待验收清单
   - 点击清单后右侧显示详情、文件上传、条目验收、回执预览
   - 筛选标签切换正常
   - 拖拽/选择文件上传后显示匹配结果
   - 批量纸质验收通过按钮正常
   - 确认接收校验正常（未处理完不可接收）
   - 导出回执按钮在全部处理完后启用
3. 访问 `/admin/collection`：
   - 前台角色看不到"待联系"批次，只看到"待接收"批次
   - 待接收批次展开验收流程
   - 流程位置说明正确展示
4. 使用 `admin` 账号（后台管理员）登录
5. 访问 `/admin/collection`：
   - 后台角色可看到"待联系"批次
   - 可约定到馆时间、拒绝征集
   - 待接收批次只读查看

- [ ] **Step 4: 停止开发服务器**

Run: `kill %1` 或 Ctrl+C

- [ ] **Step 5: 最终提交（如有遗漏修正）**

```bash
git add -A
git commit -m "feat(acceptance-hu): 完成移交验收与征集管理前端页面实现"
```

- [ ] **Step 6: 推送到远端并创建 PR**

```bash
git push origin feat/acceptance-hu
# 然后通过 GitHub CLI 或网页创建 PR 合并到 develop
```
