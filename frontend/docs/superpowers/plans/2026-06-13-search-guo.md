# 内部查阅者检索与借阅申请 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 实现内部查阅者门户的工作台、档案检索利用、档案详情、我的借阅申请四个页面及复用详情面板组件，全部基于接口文档第 11 章用 mock 开发。

**Architecture:** 新建独立 internal 子域（`types/internal.ts` + `api/internal.ts` + `mock/modules/internal.ts`），类型不复用 admin 端 archive 类型（内部不展示架位、需利用标记）；详情面板抽为 `ArchiveDetailPanel.vue` 在检索页右侧和 `/internal/archives/:id` 独立页两处复用；沿用项目 API/mock 切换、全局原型样式与既有页面模式。

**Tech Stack:** Vue 3 `<script setup>` + TypeScript + vue-router + Element Plus + vitest + @vue/test-utils。

**设计文档：** [frontend/docs/superpowers/specs/2026-06-13-search-guo-design.md](../specs/2026-06-13-search-guo-design.md)

**环境约定：** 本计划所有 shell 命令在 `frontend/` 目录下执行。当前分支 `feat/search-guo`（已从最新 `develop` 拉出，并已提交设计文档）。

---

## 文件结构

| 文件 | 动作 | 职责 |
|------|------|------|
| `src/types/internal.ts` | 新建 | 内部查阅全部类型（检索/详情/文件/借阅/工作台/AI） |
| `src/mock/modules/internal.ts` | 新建 | 工作台/检索/详情/AI/借阅 mock 数据与函数 |
| `src/mock/index.ts` | 修改 | 导出新 mock 模块（若存在汇总导出） |
| `src/api/internal.ts` | 新建 | 第 11 章 10 个 `/api/internal/*` 接口封装 |
| `src/utils/borrowValidation.ts` | 新建 | 借阅申请表单校验 |
| `src/utils/borrowValidation.spec.ts` | 新建 | 校验单测 |
| `src/views/internal/components/ArchiveDetailPanel.vue` | 新建 | 详情面板（元数据+文件+预览下载+借阅表单） |
| `src/views/internal/search/index.vue` | 重写 | 档案检索利用（替换占位） |
| `src/views/internal/archives/detail.vue` | 新建 | 详情独立页（包裹详情面板） |
| `src/views/internal/overview/index.vue` | 重写 | 工作台概览（替换占位） |
| `src/views/internal/borrow-requests/index.vue` | 新建 | 我的借阅申请 |
| `src/router/routes/internal.ts` | 修改 | 新增 `archives/:id`、`borrow-requests` 子路由 |
| `src/layouts/InternalLayout.vue` | 修改 | 导航补「我的借阅申请」 |
| `src/api/internal.spec.ts` | 新建 | API mock 分支测试 |
| 页面 spec 文件 | 新建 | 各页面基础渲染/交互测试 |

任务按依赖顺序排列：类型 → mock → API → 校验 → 详情组件 → 路由/导航 → 页面 → 集成验证。

---

## Task 1: 内部查阅者类型定义

**Files:**
- Create: `src/types/internal.ts`

- [ ] **Step 1: 创建类型文件**

写入 `src/types/internal.ts`：

```typescript
import type { PageData, PageParams } from './api'
import type { CarrierStatusValue, OpenStatusValue, RetentionPeriodValue } from './transfer'
import { BorrowStatus, SourceType } from './enums'

export type BorrowStatusValue = (typeof BorrowStatus)[keyof typeof BorrowStatus]
export type SourceTypeValue = (typeof SourceType)[keyof typeof SourceType]

/** 内部档案检索参数（公开检索维度 + 密级/开放/借阅/实体状态等内部维度） */
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

/** 内部档案摘要（检索结果行，不含架位/盒号） */
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

- [ ] **Step 2: 类型检查**

Run: `npx vue-tsc -b`
Expected: 无错误退出（exit code 0）。若报 `Cannot find module './transfer'` 等，确认 `src/types/transfer.ts` 存在且导出 `CarrierStatusValue`/`OpenStatusValue`/`RetentionPeriodValue`（已在 develop 中就绪）。

- [ ] **Step 3: 提交**

```bash
git add src/types/internal.ts
git commit -m "feat(search-guo): 新增内部查阅者类型定义"
```

## Task 2: 内部查阅者 mock 数据

**Files:**
- Create: `src/mock/modules/internal.ts`
- Modify: `src/mock/index.ts`（末尾追加汇总导出）

- [ ] **Step 1: 创建 mock 模块**

写入 `src/mock/modules/internal.ts`：

```typescript
import type {
  BorrowRequest,
  BorrowRequestCreateData,
  BorrowRequestDetail,
  BorrowRequestPage,
  BorrowRequestParams,
  InternalAiQueryRequest,
  InternalAiQueryResult,
  InternalArchive,
  InternalArchiveDetail,
  InternalArchivePage,
  InternalDashboardData,
  InternalSearchParams,
} from '@/types/internal'

// ——————————————————————————————————
// 档案样本（详情；检索结果为其投影）
// ——————————————————————————————————

const archiveSmartCity: InternalArchiveDetail = {
  id: 101,
  archiveNo: 'KJ-2025-0188',
  title: '智慧城市项目年度技术报告',
  categoryId: 3,
  categoryName: '科技档案',
  responsibleText: '技术部',
  formedDate: '2025-12-20',
  securityLevel: 2,
  openStatus: 'closed',
  carrierStatus: 'paper_electronic',
  sourceType: 'transfer',
  tags: ['智慧城市', '年度报告'],
  hasElectronicFile: true,
  canPreview: true,
  canDownload: true,
  canBorrow: true,
  borrowHint: '可申请纸质借阅',
  archivedAt: '2026-01-15T09:00:00+08:00',
  summary: '2025 年度智慧城市建设项目技术总结，含立项、实施、验收全流程技术材料。',
  retentionPeriod: '30y',
  files: [
    { id: 2001, originalFilename: '智慧城市年度技术报告.pdf', fileFormat: 'PDF', fileSize: 8_200_000, fileRole: '原文', fileCheckStatus: 'safe', canPreview: true, canDownload: true },
    { id: 2002, originalFilename: '系统架构扫描件.jpg', fileFormat: 'JPG', fileSize: 3_400_000, fileRole: '扫描件', fileCheckStatus: 'safe', canPreview: true, canDownload: true },
  ],
}

const archiveMeeting: InternalArchiveDetail = {
  id: 102,
  archiveNo: 'WS-2024-0912',
  title: '信息化建设会议纪要',
  categoryId: 1,
  categoryName: '文书档案',
  responsibleText: '办公室',
  formedDate: '2024-09-18',
  securityLevel: 0,
  openStatus: 'open',
  carrierStatus: 'electronic',
  sourceType: 'transfer',
  tags: ['信息化', '会议纪要'],
  hasElectronicFile: true,
  canPreview: true,
  canDownload: true,
  canBorrow: false,
  borrowHint: '纯电子档案不支持纸质借阅',
  archivedAt: '2025-02-10T14:00:00+08:00',
  summary: '信息化建设推进会议纪要，含决议事项与责任分工。',
  retentionPeriod: '10y',
  files: [
    { id: 2003, originalFilename: '信息化建设会议纪要.pdf', fileFormat: 'PDF', fileSize: 1_200_000, fileRole: '原文', fileCheckStatus: 'safe', canPreview: true, canDownload: true },
  ],
}

const archiveUpgrade: InternalArchiveDetail = {
  id: 103,
  archiveNo: 'KJ-2023-0416',
  title: '系统升级实施方案',
  categoryId: 3,
  categoryName: '科技档案',
  responsibleText: '信息中心',
  formedDate: '2023-07-30',
  securityLevel: 2,
  openStatus: 'closed',
  carrierStatus: 'paper',
  sourceType: 'transfer',
  tags: ['系统升级', '实施方案'],
  hasElectronicFile: false,
  canPreview: false,
  canDownload: false,
  canBorrow: true,
  borrowHint: '可申请纸质借阅',
  archivedAt: '2024-03-20T10:00:00+08:00',
  summary: '核心业务系统升级实施方案纸质原件，含手批与签章。',
  retentionPeriod: 'permanent',
  files: [],
}

const archiveBudget: InternalArchiveDetail = {
  id: 104,
  archiveNo: 'WS-2025-0501',
  title: '财政局2025年度预算批复',
  categoryId: 1,
  categoryName: '文书档案',
  responsibleText: '克拉玛依市财政局',
  formedDate: '2025-03-05',
  securityLevel: 1,
  openStatus: 'closed',
  carrierStatus: 'paper_electronic',
  sourceType: 'transfer',
  tags: ['财政预算', '批复'],
  hasElectronicFile: true,
  canPreview: true,
  canDownload: false,
  canBorrow: true,
  borrowHint: '可申请纸质借阅',
  archivedAt: '2025-06-12T11:00:00+08:00',
  summary: '2025 年度财政预算批复，含电子正本与纸质原件。',
  retentionPeriod: '30y',
  files: [
    { id: 2004, originalFilename: '2025预算批复.pdf', fileFormat: 'PDF', fileSize: 2_100_000, fileRole: '原文', fileCheckStatus: 'safe', canPreview: true, canDownload: false },
  ],
}

const archiveDetails: InternalArchiveDetail[] = [
  archiveSmartCity,
  archiveMeeting,
  archiveUpgrade,
  archiveBudget,
]

function toSummary(detail: InternalArchiveDetail): InternalArchive {
  const { summary, retentionPeriod, files, ...rest } = detail
  return rest as InternalArchive
}

// ——————————————————————————————————
// 借阅申请样本（覆盖 7 种状态）
// ——————————————————————————————————

const borrowRequests: BorrowRequest[] = [
  {
    id: 301, requestNo: 'BR-202606-021', archiveId: 104, archiveNo: 'WS-2025-0501',
    archiveTitle: '财政局2025年度预算批复', status: 'applied', expectedDays: 7,
    expectedVisitAt: '2026-06-14T10:00:00+08:00', appliedAt: '2026-06-07T09:30:00+08:00',
  },
  {
    id: 302, requestNo: 'BR-202606-018', archiveId: 101, archiveNo: 'KJ-2025-0188',
    archiveTitle: '智慧城市项目年度技术报告', status: 'approved', expectedDays: 7,
    expectedVisitAt: '2026-06-13T10:00:00+08:00', appliedAt: '2026-06-04T14:00:00+08:00',
    approvedAt: '2026-06-06T10:00:00+08:00',
  },
  {
    id: 303, requestNo: 'BR-202606-020', archiveId: 103, archiveNo: 'KJ-2023-0416',
    archiveTitle: '系统升级实施方案', status: 'voucher_issued', expectedDays: 5,
    expectedVisitAt: '2026-06-15T09:00:00+08:00', appliedAt: '2026-06-05T11:00:00+08:00',
    approvedAt: '2026-06-07T09:00:00+08:00', voucherNo: 'VCH-000012',
    voucherIssuedAt: '2026-06-08T10:00:00+08:00',
  },
  {
    id: 304, requestNo: 'BR-202606-015', archiveId: 101, archiveNo: 'KJ-2025-0188',
    archiveTitle: '智慧城市项目年度技术报告', status: 'checked_out', expectedDays: 7,
    expectedVisitAt: '2026-06-01T10:00:00+08:00', appliedAt: '2026-05-26T10:00:00+08:00',
    approvedAt: '2026-05-28T09:00:00+08:00', checkedOutAt: '2026-06-01T10:30:00+08:00',
    dueAt: '2026-06-08T10:30:00+08:00', overdue: true,
  },
  {
    id: 305, requestNo: 'BR-202605-033', archiveId: 104, archiveNo: 'WS-2025-0501',
    archiveTitle: '财政局2025年度预算批复', status: 'returned', expectedDays: 3,
    appliedAt: '2026-05-15T10:00:00+08:00', approvedAt: '2026-05-16T09:00:00+08:00',
    checkedOutAt: '2026-05-18T10:00:00+08:00', dueAt: '2026-05-21T10:00:00+08:00',
    returnedAt: '2026-05-21T09:30:00+08:00',
  },
  {
    id: 306, requestNo: 'BR-202605-017', archiveId: 101, archiveNo: 'KJ-2025-0188',
    archiveTitle: '智慧城市项目年度技术报告', status: 'rejected', expectedDays: 5,
    appliedAt: '2026-05-12T11:00:00+08:00',
  },
  {
    id: 307, requestNo: 'BR-202605-009', archiveId: 103, archiveNo: 'KJ-2023-0416',
    archiveTitle: '系统升级实施方案', status: 'abnormal_return', expectedDays: 7,
    appliedAt: '2026-05-03T10:00:00+08:00', approvedAt: '2026-05-04T09:00:00+08:00',
    checkedOutAt: '2026-05-05T10:00:00+08:00', dueAt: '2026-05-12T10:00:00+08:00',
    returnedAt: '2026-05-12T14:00:00+08:00',
  },
]

const borrowRequestDetails: Record<number, BorrowRequestDetail> = {
  301: { ...borrowRequests[0], reason: '财政预算核查需要查阅原件。', contactPhone: '13800000004' },
  302: { ...borrowRequests[1], reason: '项目复核需要查阅纸质原件。', contactPhone: '13800000004', opinion: '同意借阅 7 天。' },
  303: { ...borrowRequests[2], reason: '历史方案参考。', contactPhone: '13800000004', opinion: '同意。' },
  304: { ...borrowRequests[3], reason: '技术报告复核。', contactPhone: '13800000004', opinion: '同意。' },
  305: { ...borrowRequests[4], reason: '预算材料查阅。', contactPhone: '13800000004', opinion: '同意。', returnCheckResult: 'normal', returnNote: '实体完好。' },
  306: { ...borrowRequests[5], reason: '预算复核。', contactPhone: '13800000004', rejectReason: '目标档案处于盘点范围，暂不支持借阅。' },
  307: { ...borrowRequests[6], reason: '方案参考。', contactPhone: '13800000004', opinion: '同意。', returnCheckResult: 'damaged', returnNote: '边缘破损，已登记修复。' },
}

// ——————————————————————————————————
// mock 函数
// ——————————————————————————————————

export function mockInternalDashboard(): Promise<InternalDashboardData> {
  return Promise.resolve({
    stats: {
      recentViewCount: 14,
      pendingApprovalCount: 2,
      approvedPendingPickupCount: 1,
      downloadCount: 23,
    },
    recentViews: [
      { id: 401, archiveId: 101, archiveNo: 'KJ-2025-0188', title: '智慧城市项目年度技术报告', categoryName: '科技档案', securityLevel: 2, viewedAt: '2026-06-07T11:20:00+08:00', accessStatus: 'available' },
      { id: 402, archiveId: 102, archiveNo: 'WS-2024-0912', title: '信息化建设会议纪要', categoryName: '文书档案', securityLevel: 0, viewedAt: '2026-06-05T16:42:00+08:00', accessStatus: 'available' },
      { id: 403, archiveId: 105, archiveNo: 'WS-2019-0440', title: '历史专项资料汇编', categoryName: '文书档案', securityLevel: 2, viewedAt: '2026-05-22T09:10:00+08:00', accessStatus: 'permission_changed' },
    ],
    borrowRequests: [borrowRequests[0], borrowRequests[1], borrowRequests[4], borrowRequests[5]],
    currentLoans: [borrowRequests[3]],
    downloads: [
      { id: 501, archiveId: 101, archiveNo: 'KJ-2025-0188', title: '智慧城市项目年度技术报告', downloadedAt: '2026-06-07T11:25:00+08:00' },
      { id: 502, archiveId: 102, archiveNo: 'WS-2024-0912', title: '信息化建设会议纪要', downloadedAt: '2026-06-03T14:10:00+08:00' },
      { id: 503, archiveId: 104, archiveNo: 'WS-2025-0501', title: '财政局2025年度预算批复', downloadedAt: '2026-05-28T10:00:00+08:00' },
    ],
    permission: {
      role: '内部查阅者',
      organizationName: '技术部',
      maxSecurityLevel: 2,
      dataScope: '本单位授权全宗',
    },
  })
}

export function mockSearchInternalArchives(params?: InternalSearchParams): Promise<InternalArchivePage> {
  let all = archiveDetails.map(toSummary)
  if (params?.keyword) {
    const kw = params.keyword.toLowerCase()
    all = all.filter((a) =>
      `${a.archiveNo} ${a.title} ${a.responsibleText} ${a.tags.join(' ')}`.toLowerCase().includes(kw),
    )
  }
  if (params?.archiveNo) all = all.filter((a) => a.archiveNo.toLowerCase().includes(params.archiveNo!.toLowerCase()))
  if (params?.title) all = all.filter((a) => a.title.toLowerCase().includes(params.title!.toLowerCase()))
  if (params?.responsibleText) all = all.filter((a) => a.responsibleText.toLowerCase().includes(params.responsibleText!.toLowerCase()))
  if (params?.categoryId) all = all.filter((a) => a.categoryId === params.categoryId)
  if (params?.carrierStatus) all = all.filter((a) => a.carrierStatus === params.carrierStatus)
  if (params?.sourceType) all = all.filter((a) => a.sourceType === params.sourceType)
  if (params?.openStatus) all = all.filter((a) => a.openStatus === params.openStatus)
  if (params?.formedYearStart) all = all.filter((a) => Number(a.formedDate.slice(0, 4)) >= params.formedYearStart!)
  if (params?.formedYearEnd) all = all.filter((a) => Number(a.formedDate.slice(0, 4)) <= params.formedYearEnd!)
  if (params?.hasElectronicFile !== undefined) all = all.filter((a) => a.hasElectronicFile === params.hasElectronicFile)
  return Promise.resolve({
    records: all,
    pageNo: params?.pageNo ?? 1,
    pageSize: params?.pageSize ?? 20,
    total: all.length,
    hasNext: false,
  })
}

export function mockInternalArchiveDetail(id: number): Promise<InternalArchiveDetail | null> {
  const detail = archiveDetails.find((d) => d.id === id) ?? null
  return Promise.resolve(detail ? { ...detail } : null)
}

export function mockGenerateInternalAiQuery(data: InternalAiQueryRequest): Promise<InternalAiQueryResult> {
  const conditions: Partial<InternalSearchParams> = {
    keyword: '智慧城市 项目 报告',
    archiveNo: 'KJ',
    title: '智慧城市项目',
    categoryId: 3,
    responsibleText: '技术部',
    formedYearStart: 2024,
    formedYearEnd: 2026,
    securityLevelMax: 2,
    carrierStatus: 'paper_electronic',
    hasElectronicFile: true,
  }
  return Promise.resolve({
    ruleType: 'internalSearchQuery',
    conditions,
    rawJson: { input: data.text, generated: conditions },
  })
}

export function mockPreviewInternalFile(fileId: number): Promise<string> {
  return Promise.resolve(`# 文件 ${fileId} 预览\n\n这是模拟的电子文件预览内容。实际接入后返回预签名 URL 或文件流。`)
}

export function mockDownloadInternalFile(_fileId: number): Promise<Blob> {
  return Promise.resolve(new Blob(['Mock 内部档案文件内容'], { type: 'application/pdf' }))
}

export function mockCreateBorrowRequest(data: BorrowRequestCreateData): Promise<BorrowRequest> {
  const archive = archiveDetails.find((d) => d.id === data.archiveId)
  const now = new Date()
  const seq = String(900 + Math.floor(Math.random() * 100))
  return Promise.resolve({
    id: Number(seq),
    requestNo: `BR-${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}-${seq}`,
    archiveId: data.archiveId,
    archiveNo: archive?.archiveNo ?? '',
    archiveTitle: archive?.title ?? '',
    status: 'applied',
    expectedDays: data.expectedDays,
    expectedVisitAt: data.expectedVisitAt,
    appliedAt: now.toISOString(),
  })
}

export function mockMyBorrowRequests(params?: BorrowRequestParams): Promise<BorrowRequestPage> {
  let all = [...borrowRequests]
  if (params?.status) all = all.filter((r) => r.status === params.status)
  if (params?.keyword) {
    const kw = params.keyword.toLowerCase()
    all = all.filter((r) => `${r.requestNo} ${r.archiveNo} ${r.archiveTitle}`.toLowerCase().includes(kw))
  }
  return Promise.resolve({
    records: all,
    pageNo: params?.pageNo ?? 1,
    pageSize: params?.pageSize ?? 20,
    total: all.length,
    hasNext: false,
  })
}

export function mockBorrowRequestDetail(id: number): Promise<BorrowRequestDetail | null> {
  const detail = borrowRequestDetails[id]
  return Promise.resolve(detail ? { ...detail } : null)
}

export function mockExportBorrowVoucher(_id: number): Promise<Blob> {
  return Promise.resolve(new Blob(['Mock 借阅凭证 PDF 内容'], { type: 'application/pdf' }))
}
```

- [ ] **Step 2: 注册到 mock 汇总入口**

在 `src/mock/index.ts` 末尾追加一行：

```typescript
export * from './modules/internal'
```

完整文件应为：

```typescript
// src/mock/index.ts
// Mock 数据汇总入口

export { mockReceptionBatches, mockBatchDetail, mockUploadResult } from './modules/reception'
export { mockCollectionBatches } from './modules/collection'
export * from './modules/transfer'
export * from './modules/public'
export * from './modules/archive'
export * from './modules/internal'
```

- [ ] **Step 3: 类型检查**

Run: `npx vue-tsc -b`
Expected: 无错误退出。若报类型不匹配，核对 `InternalSearchParams`/`InternalArchiveDetail` 等字段与 Task 1 一致。

- [ ] **Step 4: 提交**

```bash
git add src/mock/modules/internal.ts src/mock/index.ts
git commit -m "feat(search-guo): 新增内部查阅者 mock 数据"
```

## Task 3: 内部查阅者 API 封装

**Files:**
- Create: `src/api/internal.ts`

- [ ] **Step 1: 创建 API 文件**

写入 `src/api/internal.ts`（10 个函数，严格对应接口文档第 11 章；mock 分支动态 import Task 2 的 mock 函数）：

```typescript
import request from './request'
import type {
  BorrowRequest,
  BorrowRequestCreateData,
  BorrowRequestDetail,
  BorrowRequestPage,
  BorrowRequestParams,
  InternalAiQueryRequest,
  InternalAiQueryResult,
  InternalArchiveDetail,
  InternalArchivePage,
  InternalDashboardData,
  InternalSearchParams,
} from '@/types/internal'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 11.1 内部工作台 */
export function getInternalDashboard(): Promise<InternalDashboardData> {
  if (USE_MOCK) return import('@/mock/modules/internal').then((m) => m.mockInternalDashboard())
  return request.get('/internal/dashboard')
}

/** 11.2 内部档案检索 */
export function searchInternalArchives(params?: InternalSearchParams): Promise<InternalArchivePage> {
  if (USE_MOCK) return import('@/mock/modules/internal').then((m) => m.mockSearchInternalArchives(params))
  return request.get('/internal/archives/search', { params })
}

/** 11.3 内部档案详情 */
export function getInternalArchiveDetail(archiveId: number): Promise<InternalArchiveDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/internal').then(async (m) => {
      const detail = await m.mockInternalArchiveDetail(archiveId)
      if (!detail) throw new Error('档案不存在或当前权限不可见')
      return detail
    })
  }
  return request.get(`/internal/archives/${archiveId}`)
}

/** 11.4 内部 AI 检索 JSON 生成 */
export function generateInternalAiQuery(data: InternalAiQueryRequest): Promise<InternalAiQueryResult> {
  if (USE_MOCK) return import('@/mock/modules/internal').then((m) => m.mockGenerateInternalAiQuery(data))
  return request.post('/internal/archives/ai-query', data)
}

/** 11.5 内部预览电子文件 */
export function previewInternalFile(fileId: number): Promise<string> {
  if (USE_MOCK) return import('@/mock/modules/internal').then((m) => m.mockPreviewInternalFile(fileId))
  return request.get(`/internal/archive-files/${fileId}/preview`)
}

/** 11.6 内部下载电子文件 */
export function downloadInternalFile(fileId: number): Promise<Blob> {
  if (USE_MOCK) return import('@/mock/modules/internal').then((m) => m.mockDownloadInternalFile(fileId))
  return request.get(`/internal/archive-files/${fileId}/download`, { responseType: 'blob' }) as Promise<Blob>
}

/** 11.7 提交借阅申请 */
export function createBorrowRequest(data: BorrowRequestCreateData): Promise<BorrowRequest> {
  if (USE_MOCK) return import('@/mock/modules/internal').then((m) => m.mockCreateBorrowRequest(data))
  return request.post('/internal/borrow-requests', data)
}

/** 11.8 查询我的借阅申请 */
export function getMyBorrowRequests(params?: BorrowRequestParams): Promise<BorrowRequestPage> {
  if (USE_MOCK) return import('@/mock/modules/internal').then((m) => m.mockMyBorrowRequests(params))
  return request.get('/internal/borrow-requests', { params })
}

/** 11.9 借阅申请详情 */
export function getBorrowRequestDetail(requestId: number): Promise<BorrowRequestDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/internal').then(async (m) => {
      const detail = await m.mockBorrowRequestDetail(requestId)
      if (!detail) throw new Error('借阅申请不存在')
      return detail
    })
  }
  return request.get(`/internal/borrow-requests/${requestId}`)
}

/** 11.10 导出借阅凭证 */
export function exportBorrowVoucher(requestId: number): Promise<Blob> {
  if (USE_MOCK) return import('@/mock/modules/internal').then((m) => m.mockExportBorrowVoucher(requestId))
  return request.get(`/internal/borrow-requests/${requestId}/voucher`, { responseType: 'blob' }) as Promise<Blob>
}
```

- [ ] **Step 2: 类型检查**

Run: `npx vue-tsc -b`
Expected: 无错误退出。

- [ ] **Step 3: 提交**

```bash
git add src/api/internal.ts
git commit -m "feat(search-guo): 新增内部查阅者 API 封装"
```

---

## Task 4: 借阅申请表单校验（TDD）

**Files:**
- Create: `src/utils/borrowValidation.spec.ts`
- Create: `src/utils/borrowValidation.ts`

- [ ] **Step 1: 写失败测试**

写入 `src/utils/borrowValidation.spec.ts`：

```typescript
import { describe, expect, it } from 'vitest'
import { validateBorrowRequest } from './borrowValidation'

describe('validateBorrowRequest', () => {
  it('reports missing required fields', () => {
    const errors = validateBorrowRequest({})
    expect(errors).toContain('请填写借阅理由。')
    expect(errors).toContain('借阅天数至少 1 天。')
    expect(errors).toContain('请选择预计到馆时间。')
    expect(errors).toContain('请填写联系电话。')
  })

  it('reports invalid phone format', () => {
    const errors = validateBorrowRequest({
      reason: '项目复核',
      expectedDays: 7,
      expectedVisitAt: '2026-06-14T10:00:00+08:00',
      contactPhone: '12345',
    })
    expect(errors).toEqual(['联系电话格式不正确。'])
  })

  it('rejects zero days', () => {
    const errors = validateBorrowRequest({
      reason: '复核',
      expectedDays: 0,
      expectedVisitAt: '2026-06-14T10:00:00+08:00',
      contactPhone: '13800000004',
    })
    expect(errors).toContain('借阅天数至少 1 天。')
  })

  it('passes for valid input', () => {
    const errors = validateBorrowRequest({
      archiveId: 101,
      reason: '项目复核需要查阅纸质原件。',
      expectedDays: 7,
      expectedVisitAt: '2026-06-14T10:00:00+08:00',
      contactPhone: '13800000004',
    })
    expect(errors).toEqual([])
  })
})
```

- [ ] **Step 2: 运行测试验证失败**

Run: `npx vitest run src/utils/borrowValidation.spec.ts`
Expected: FAIL，错误信息包含 `Failed to resolve import "./borrowValidation"` 或 `validateBorrowRequest is not a function`。

- [ ] **Step 3: 写最小实现**

写入 `src/utils/borrowValidation.ts`：

```typescript
import type { BorrowRequestCreateData } from '@/types/internal'

/** 校验借阅申请表单，返回错误信息数组（空数组表示通过） */
export function validateBorrowRequest(data: Partial<BorrowRequestCreateData>): string[] {
  const errors: string[] = []

  if (!data.reason || !data.reason.trim()) {
    errors.push('请填写借阅理由。')
  }
  if (!data.expectedDays || data.expectedDays < 1) {
    errors.push('借阅天数至少 1 天。')
  }
  if (!data.expectedVisitAt) {
    errors.push('请选择预计到馆时间。')
  }
  const phone = data.contactPhone?.trim() ?? ''
  if (!phone) {
    errors.push('请填写联系电话。')
  } else if (!/^1[3-9]\d{9}$/.test(phone)) {
    errors.push('联系电话格式不正确。')
  }

  return errors
}
```

- [ ] **Step 4: 运行测试验证通过**

Run: `npx vitest run src/utils/borrowValidation.spec.ts`
Expected: PASS，4 个用例全部通过。

- [ ] **Step 5: 提交**

```bash
git add src/utils/borrowValidation.ts src/utils/borrowValidation.spec.ts
git commit -m "feat(search-guo): 新增借阅申请表单校验"
```

## Task 5: 档案详情面板组件（检索页 + 独立页复用）

**Files:**
- Create: `src/views/internal/components/ArchiveDetailPanel.vue`
- Create: `src/views/internal/components/ArchiveDetailPanel.spec.ts`

- [ ] **Step 1: 创建组件**

写入 `src/views/internal/components/ArchiveDetailPanel.vue`（还原 `search.html` 的 `detail-panel` + `drawer` + `borrow-form`；不展示架位；加载/空/错误三态齐全）：

```vue
<template>
  <div class="detail-panel">
    <div v-if="loading" class="notice">加载档案详情…</div>

    <div v-else-if="error" class="notice danger">
      {{ error }}
      <div class="actions" style="margin-top: 8px">
        <button class="button ghost" type="button" @click="loadDetail">重试</button>
      </div>
    </div>

    <div v-else-if="detail" class="drawer">
      <h2 class="section-title">{{ detail.title }}</h2>
      <div class="detail-grid">
        <div class="detail-line"><span class="muted">档号</span><strong>{{ detail.archiveNo }}</strong></div>
        <div class="detail-line"><span class="muted">分类</span><strong>{{ detail.categoryName }}</strong></div>
        <div class="detail-line"><span class="muted">责任者</span><strong>{{ detail.responsibleText }}</strong></div>
        <div class="detail-line"><span class="muted">形成日期</span><strong>{{ detail.formedDate }}</strong></div>
        <div class="detail-line"><span class="muted">密级</span><strong>{{ securityLabel(detail.securityLevel) }}</strong></div>
        <div class="detail-line"><span class="muted">载体状态</span><strong>{{ carrierLabel(detail.carrierStatus) }}</strong></div>
        <div class="detail-line"><span class="muted">纸质借阅</span><strong>{{ detail.canBorrow ? '可申请' : '不支持' }}</strong></div>
      </div>
      <p v-if="detail.summary" class="muted" style="margin-top: 8px">{{ detail.summary }}</p>
      <div class="notice" style="margin-top: 10px">内部查阅者不展示具体库房架位。需要纸质原件时提交借阅申请，由管理员审批和到馆核验。</div>

      <div v-if="detail.files.length > 0" style="margin-top: 12px">
        <h3 class="section-title">电子文件</h3>
        <div v-for="file in detail.files" :key="file.id" class="local-file">
          <div>
            <strong>{{ file.originalFilename }}</strong>
            <div class="hint">{{ file.fileFormat }}，{{ formatSize(file.fileSize) }}，{{ file.fileRole }}</div>
          </div>
          <div class="actions">
            <button class="button secondary" type="button" :disabled="!file.canPreview || previewing" @click="handlePreview(file.id)">
              预览
            </button>
            <button class="button ghost" type="button" :disabled="!file.canDownload || downloading" @click="handleDownload(file)">
              下载
            </button>
          </div>
        </div>
        <div v-if="previewContent" class="notice" style="margin-top: 8px">
          <strong>预览内容</strong>
          <pre style="white-space: pre-wrap; font-size: 13px; margin-top: 4px">{{ previewContent }}</pre>
        </div>
      </div>
      <div v-else class="notice" style="margin-top: 12px">
        {{ detail.carrierStatus === 'paper' ? '纯纸质档案，暂无电子文件可供预览/下载。' : '暂无可预览/下载的电子文件。' }}
      </div>

      <div class="actions" style="margin-top: 12px">
        <button class="button" type="button" :disabled="!detail.canBorrow" :title="detail.borrowHint" @click="toggleBorrowForm">
          {{ showBorrowForm ? '收起申请' : '申请借阅' }}
        </button>
      </div>

      <form v-if="showBorrowForm" class="borrow-form" @submit.prevent="submitBorrow">
        <div class="field">
          <label>借阅理由</label>
          <textarea v-model="borrowForm.reason" placeholder="说明借阅用途和必要性"></textarea>
        </div>
        <div class="form-grid">
          <div class="field">
            <label>借阅天数</label>
            <input v-model.number="borrowForm.expectedDays" type="number" min="1" />
          </div>
          <div class="field">
            <label>到馆时间</label>
            <input v-model="borrowForm.expectedVisitAt" type="datetime-local" />
          </div>
        </div>
        <div class="field">
          <label>联系电话</label>
          <input v-model="borrowForm.contactPhone" placeholder="11 位手机号" />
        </div>
        <ul v-if="borrowErrors.length" class="notice danger">
          <li v-for="msg in borrowErrors" :key="msg">{{ msg }}</li>
        </ul>
        <div class="actions">
          <button class="button" type="submit" :disabled="submitting">{{ submitting ? '提交中…' : '提交申请' }}</button>
          <button class="button ghost" type="button" @click="showBorrowForm = false">取消</button>
        </div>
      </form>
    </div>

    <div v-else class="notice">点击左侧结果行的「详情」查看档案信息。</div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createBorrowRequest,
  downloadInternalFile,
  getInternalArchiveDetail,
  previewInternalFile,
} from '@/api/internal'
import { validateBorrowRequest } from '@/utils/borrowValidation'
import { CarrierStatusLabel, SecurityLevelLabel } from '@/types/enums'
import type { InternalArchiveDetail, InternalFile } from '@/types/internal'

const props = defineProps<{ archiveId: number | null }>()
const emit = defineEmits<{ borrowed: [] }>()

const loading = ref(false)
const error = ref('')
const detail = ref<InternalArchiveDetail | null>(null)

const previewing = ref(false)
const previewContent = ref('')
const downloading = ref(false)

const showBorrowForm = ref(false)
const submitting = ref(false)
const borrowErrors = ref<string[]>([])
const borrowForm = reactive({
  reason: '',
  expectedDays: 7,
  expectedVisitAt: '',
  contactPhone: '',
})

function securityLabel(level: number): string {
  return SecurityLevelLabel[level] ?? '未知'
}
function carrierLabel(status: string): string {
  return CarrierStatusLabel[status] ?? status
}
function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

async function loadDetail() {
  if (props.archiveId == null) {
    detail.value = null
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  detail.value = null
  previewContent.value = ''
  showBorrowForm.value = false
  try {
    detail.value = await getInternalArchiveDetail(props.archiveId)
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '加载档案详情失败'
  } finally {
    loading.value = false
  }
}

watch(() => props.archiveId, loadDetail, { immediate: true })

function toggleBorrowForm() {
  showBorrowForm.value = !showBorrowForm.value
}

async function handlePreview(fileId: number) {
  previewing.value = true
  previewContent.value = ''
  try {
    previewContent.value = await previewInternalFile(fileId)
  } catch {
    ElMessage.error('预览失败，请稍后重试')
  } finally {
    previewing.value = false
  }
}

async function handleDownload(file: InternalFile) {
  downloading.value = true
  try {
    const blob = await downloadInternalFile(file.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = file.originalFilename
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载失败，请稍后重试')
  } finally {
    downloading.value = false
  }
}

async function submitBorrow() {
  if (!detail.value) return
  const data = {
    archiveId: detail.value.id,
    reason: borrowForm.reason,
    expectedDays: borrowForm.expectedDays,
    expectedVisitAt: borrowForm.expectedVisitAt,
    contactPhone: borrowForm.contactPhone,
  }
  const errors = validateBorrowRequest(data)
  borrowErrors.value = errors
  if (errors.length) return
  submitting.value = true
  try {
    const created = await createBorrowRequest(data)
    ElMessage.success(`借阅申请 ${created.requestNo} 已提交，等待审批`)
    showBorrowForm.value = false
    borrowForm.reason = ''
    emit('borrowed')
  } catch {
    ElMessage.error('提交借阅申请失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.detail-panel {
  display: grid;
  gap: 12px;
}
.detail-grid {
  display: grid;
  gap: 10px;
}
.detail-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 9px;
  border-bottom: 1px solid var(--border);
}
.detail-line:last-child {
  border-bottom: 0;
}
.borrow-form {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}
.local-file {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  padding: 9px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
  margin-top: 8px;
}
</style>
```

- [ ] **Step 2: 写组件测试**

写入 `src/views/internal/components/ArchiveDetailPanel.spec.ts`：

```typescript
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import ArchiveDetailPanel from './ArchiveDetailPanel.vue'

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

describe('ArchiveDetailPanel', () => {
  it('loads and renders archive detail from mock API', async () => {
    const wrapper = mount(ArchiveDetailPanel, { props: { archiveId: 101 } })
    await waitForAsyncData()

    expect(wrapper.text()).toContain('智慧城市项目年度技术报告')
    expect(wrapper.text()).toContain('KJ-2025-0188')
    expect(wrapper.text()).toContain('不展示具体库房架位')
  })

  it('disables borrow button for electronic-only archive', async () => {
    const wrapper = mount(ArchiveDetailPanel, { props: { archiveId: 102 } })
    await waitForAsyncData()

    expect(wrapper.text()).toContain('纯电子档案不支持纸质借阅')
    const borrowBtn = wrapper.findAll('button').find((b) => b.text().includes('申请借阅'))
    expect(borrowBtn?.attributes('disabled')).toBeDefined()
  })

  it('shows placeholder when archiveId is null', async () => {
    const wrapper = mount(ArchiveDetailPanel, { props: { archiveId: null } })
    await waitForAsyncData()

    expect(wrapper.text()).toContain('点击左侧结果行')
  })
})
```

- [ ] **Step 3: 运行测试验证通过**

Run: `npx vitest run src/views/internal/components/ArchiveDetailPanel.spec.ts`
Expected: PASS，3 个用例通过。

- [ ] **Step 4: 类型检查**

Run: `npx vue-tsc -b`
Expected: 无错误退出。

- [ ] **Step 5: 提交**

```bash
git add src/views/internal/components/ArchiveDetailPanel.vue src/views/internal/components/ArchiveDetailPanel.spec.ts
git commit -m "feat(search-guo): 新增档案详情面板组件"
```

## Task 6: 档案检索利用页面

**Files:**
- Modify(重写): `src/views/internal/search/index.vue`
- Create: `src/views/internal/search/index.spec.ts`

> 路由 `/internal/search` 已存在（指向本文件），本 task 只重写内容。

- [ ] **Step 1: 重写检索页**

将 `src/views/internal/search/index.vue` 完整替换为（还原 `search.html` 双栏布局：左 AI+结构化条件+结果表，右 `ArchiveDetailPanel`）：

```vue
<template>
  <section class="search-layout">
    <div class="grid">
      <!-- AI 检索条件生成 -->
      <details class="card panel ai-panel">
        <summary>
          <span class="section-title">AI 检索条件生成</span>
          <span class="status info">点击展开</span>
        </summary>
        <div class="collapsible-body">
          <div class="field">
            <label>自然语言检索描述</label>
            <textarea v-model="aiText" placeholder="例如：查找财政局 2025 年预算相关档案"></textarea>
            <span class="hint">AI 不查库、不读取正文，只生成结构化查询条件。</span>
          </div>
          <div class="actions">
            <button class="button" type="button" :disabled="aiLoading" @click="handleAiQuery">
              {{ aiLoading ? '生成中…' : '生成 JSON' }}
            </button>
            <button class="button ghost" type="button" @click="clearAi">清空</button>
          </div>
          <div v-if="aiError" class="notice danger">{{ aiError }}</div>
          <pre v-if="aiResult" class="ai-box">{{ JSON.stringify(aiResult.conditions, null, 2) }}</pre>
          <div v-if="aiResult" class="json-actions">
            <button class="button secondary" type="button" @click="applyAiConditions">填充表单</button>
            <button class="button ghost" type="button" @click="copyJson">复制 JSON</button>
          </div>
        </div>
      </details>

      <!-- 结构化检索条件 -->
      <div class="card panel">
        <div class="toolbar" style="margin: 0">
          <h2 class="section-title" style="margin: 0">结构化检索条件</h2>
          <span class="status info">后端追加密级、单位、全宗范围过滤</span>
        </div>
        <div class="filter-sections">
          <section class="filter-group">
            <div class="filter-group-head"><strong>核心元数据</strong></div>
            <div class="filter-grid">
              <div class="field"><label>关键词</label><input v-model="params.keyword" placeholder="题名、责任者、档号、文件名"></div>
              <div class="field"><label>档号</label><input v-model="params.archiveNo"></div>
              <div class="field"><label>题名</label><input v-model="params.title"></div>
              <div class="field"><label>责任者</label><input v-model="params.responsibleText"></div>
              <div class="field"><label>所属全宗</label><input v-model="fondsInput" placeholder="全宗名称"></div>
              <div class="field">
                <label>档案门类</label>
                <select v-model.number="params.categoryId">
                  <option :value="undefined">全部</option>
                  <option :value="1">文书档案</option>
                  <option :value="3">科技档案</option>
                </select>
              </div>
              <div class="field"><label>形成/移交单位</label><input v-model="params.organizationName"></div>
              <div class="field"><label>标签</label><input v-model="params.tagIds" placeholder="逗号分隔"></div>
            </div>
          </section>

          <section class="filter-group">
            <div class="filter-group-head"><strong>时间与业务属性</strong></div>
            <div class="filter-grid">
              <div class="field"><label>形成年度起</label><input v-model.number="params.formedYearStart" type="number"></div>
              <div class="field"><label>形成年度止</label><input v-model.number="params.formedYearEnd" type="number"></div>
              <div class="field">
                <label>档案来源</label>
                <select v-model="params.sourceType">
                  <option value="">全部</option>
                  <option value="transfer">移交</option>
                  <option value="collection">征集</option>
                  <option value="compilation">编研</option>
                </select>
              </div>
              <div class="field">
                <label>载体状态</label>
                <select v-model="params.carrierStatus">
                  <option value="">全部</option>
                  <option value="electronic">纯电子</option>
                  <option value="paper_electronic">纸质+电子</option>
                  <option value="paper">纯纸质</option>
                </select>
              </div>
              <div class="field">
                <label>保管期限</label>
                <select v-model="params.retentionPeriod">
                  <option value="">全部</option>
                  <option value="permanent">永久</option>
                  <option value="30y">30年</option>
                  <option value="10y">10年</option>
                </select>
              </div>
              <div class="field">
                <label>密级范围</label>
                <select v-model.number="params.securityLevelMax">
                  <option :value="undefined">当前权限内全部</option>
                  <option :value="0">非密</option>
                  <option :value="1">内部</option>
                  <option :value="2">秘密</option>
                </select>
              </div>
              <div class="field">
                <label>开放状态</label>
                <select v-model="params.openStatus">
                  <option value="">全部</option>
                  <option value="open">公开</option>
                  <option value="closed">不公开</option>
                </select>
              </div>
              <div class="field">
                <label>借阅状态</label>
                <select v-model="params.loanStatus">
                  <option value="">全部</option>
                  <option value="available">可借阅</option>
                  <option value="on_loan">借出中</option>
                  <option value="inventory_paused">盘点暂停</option>
                </select>
              </div>
            </div>
          </section>

          <section class="filter-group">
            <div class="filter-group-head"><strong>电子文件与排序</strong></div>
            <div class="filter-grid">
              <div class="field">
                <label>电子文件</label>
                <select v-model="hasFileSelect">
                  <option value="">全部</option>
                  <option value="yes">有电子文件</option>
                  <option value="no">无电子文件</option>
                </select>
              </div>
              <div class="field"><label>文件格式</label><input v-model="params.fileExt" placeholder="PDF / JPG..."></div>
              <div class="field">
                <label>排序</label>
                <select v-model="params.sortBy">
                  <option value="relevance">相关度优先</option>
                  <option value="formed_desc">形成日期倒序</option>
                  <option value="archived_desc">入库时间倒序</option>
                  <option value="archiveNo_asc">档号升序</option>
                </select>
              </div>
            </div>
          </section>
        </div>
        <div class="actions" style="margin-top: 12px">
          <button class="button" type="button" :disabled="searching" @click="handleSearch">
            {{ searching ? '检索中…' : '确认检索' }}
          </button>
          <button class="button ghost" type="button" @click="resetSearch">重置</button>
        </div>
      </div>

      <!-- 检索结果 -->
      <div class="card panel">
        <div class="toolbar">
          <div>
            <h2 class="section-title">检索结果</h2>
            <p class="page-subtitle">结果已按当前用户权限过滤；不会展示高于密级上限或超出范围的档案。</p>
          </div>
          <span v-if="!searching && searched" class="status info">权限过滤后 {{ results.total }} 条</span>
        </div>
        <div v-if="searching" class="notice">正在检索…</div>
        <div v-else-if="searchError" class="notice danger">{{ searchError }}</div>
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>档号</th><th>题名</th><th>分类</th><th>密级</th><th>载体</th><th>利用状态</th><th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="searched && results.records.length === 0">
                <td colspan="7"><div class="empty">没有符合条件的档案</div></td>
              </tr>
              <tr v-for="record in results.records" :key="record.id" class="result-row" @click="selectArchive(record.id)">
                <td class="mono">{{ record.archiveNo }}</td>
                <td>{{ record.title }}</td>
                <td>{{ record.categoryName }}</td>
                <td><span :class="['status', record.securityLevel > 0 ? 'warning' : 'success']">{{ securityLabel(record.securityLevel) }}</span></td>
                <td>{{ carrierLabel(record.carrierStatus) }}</td>
                <td><span class="status success">{{ usageHint(record) }}</span></td>
                <td><button class="button ghost" type="button" @click.stop="selectArchive(record.id)">详情</button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 右栏详情面板 -->
    <aside class="detail-panel-wrapper">
      <ArchiveDetailPanel :archive-id="selectedArchiveId" />
      <div class="card panel">
        <h2 class="section-title">安全边界</h2>
        <div class="notice">AI 只输出查询条件 JSON；正式检索、预览、下载、借阅申请都走后端业务接口和权限校验。</div>
      </div>
    </aside>
  </section>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import ArchiveDetailPanel from '@/views/internal/components/ArchiveDetailPanel.vue'
import { generateInternalAiQuery, searchInternalArchives } from '@/api/internal'
import { CarrierStatusLabel, SecurityLevelLabel } from '@/types/enums'
import type { InternalArchive, InternalAiQueryResult, InternalSearchParams } from '@/types/internal'

const aiText = ref('')
const aiLoading = ref(false)
const aiError = ref('')
const aiResult = ref<InternalAiQueryResult | null>(null)

const params = reactive<InternalSearchParams>({})
const fondsInput = ref('')
const hasFileSelect = ref<'' | 'yes' | 'no'>('')

const searching = ref(false)
const searchError = ref('')
const searched = ref(false)
const results = ref<{ records: InternalArchive[]; total: number }>({ records: [], total: 0 })
const selectedArchiveId = ref<number | null>(null)

function securityLabel(level: number): string {
  return SecurityLevelLabel[level] ?? '未知'
}
function carrierLabel(status: string): string {
  return CarrierStatusLabel[status] ?? status
}
function usageHint(record: InternalArchive): string {
  const hints: string[] = []
  if (record.canPreview) hints.push('可预览')
  if (record.canDownload) hints.push('可下载')
  if (record.canBorrow) hints.push('可借阅')
  return hints.length ? hints.join(' / ') : '仅元数据'
}

async function handleAiQuery() {
  if (!aiText.value.trim()) {
    ElMessage.warning('请输入自然语言描述')
    return
  }
  aiLoading.value = true
  aiError.value = ''
  try {
    aiResult.value = await generateInternalAiQuery({ text: aiText.value })
    ElMessage.success('AI 已生成查询条件，尚未执行检索')
  } catch (e: unknown) {
    aiError.value = e instanceof Error ? e.message : 'AI 生成失败，请改用结构化检索'
    aiResult.value = null
  } finally {
    aiLoading.value = false
  }
}

function applyAiConditions() {
  if (!aiResult.value) return
  Object.assign(params, aiResult.value.conditions)
  ElMessage.success('条件已填充到表单，请确认后检索')
}

function clearAi() {
  aiText.value = ''
  aiResult.value = null
  aiError.value = ''
}

async function copyJson() {
  if (!aiResult.value) return
  try {
    await navigator.clipboard.writeText(JSON.stringify(aiResult.value.conditions, null, 2))
    ElMessage.success('JSON 已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择')
  }
}

async function handleSearch() {
  searching.value = true
  searchError.value = ''
  selectedArchiveId.value = null
  try {
    const query: InternalSearchParams = { ...params }
    if (hasFileSelect.value === 'yes') query.hasElectronicFile = true
    else if (hasFileSelect.value === 'no') query.hasElectronicFile = false
    const page = await searchInternalArchives(query)
    results.value = { records: page.records, total: page.total }
    searched.value = true
  } catch (e: unknown) {
    searchError.value = e instanceof Error ? e.message : '检索失败'
  } finally {
    searching.value = false
  }
}

function selectArchive(id: number) {
  selectedArchiveId.value = id
}

function resetSearch() {
  Object.keys(params).forEach((k) => delete (params as Record<string, unknown>)[k])
  fondsInput.value = ''
  hasFileSelect.value = ''
  results.value = { records: [], total: 0 }
  searched.value = false
  searchError.value = ''
  selectedArchiveId.value = null
  aiResult.value = null
  aiText.value = ''
}
</script>

<style scoped>
.search-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 400px;
  gap: 16px;
  align-items: start;
}
.filter-sections { display: grid; gap: 12px; margin-top: 14px; }
.filter-group {
  display: grid; gap: 12px; padding: 12px;
  border: 1px solid var(--border); border-radius: var(--radius); background: #f8fbfc;
}
.filter-group-head { display: flex; gap: 8px; align-items: center; justify-content: space-between; }
.filter-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.ai-panel { display: grid; gap: 12px; }
.ai-panel summary {
  display: flex; min-height: 34px; cursor: pointer; list-style: none;
  align-items: center; justify-content: space-between; gap: 12px;
}
.ai-panel summary::-webkit-details-marker { display: none; }
.collapsible-body {
  display: grid; gap: 12px; margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--border);
}
.json-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.result-row { cursor: pointer; transition: background 0.16s ease; }
.result-row:hover { background: #f8fbfc; }
.detail-panel-wrapper { display: grid; gap: 12px; position: sticky; top: 84px; }
@media (max-width: 1120px) {
  .search-layout { grid-template-columns: 1fr; }
  .detail-panel-wrapper { position: static; }
}
@media (max-width: 960px) {
  .filter-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
```

- [ ] **Step 2: 写页面测试**

写入 `src/views/internal/search/index.spec.ts`：

```typescript
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import InternalSearch from './index.vue'

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

describe('InternalSearch', () => {
  it('renders AI panel, structured filters, and result area', async () => {
    const wrapper = mount(InternalSearch)
    await waitForAsyncData()

    expect(wrapper.text()).toContain('AI 检索条件生成')
    expect(wrapper.text()).toContain('结构化检索条件')
    expect(wrapper.text()).toContain('检索结果')
  })

  it('loads results on search and opens detail panel', async () => {
    const wrapper = mount(InternalSearch)
    await waitForAsyncData()

    const searchBtn = wrapper.findAll('button').find((b) => b.text().includes('确认检索'))
    await searchBtn?.trigger('click')
    await waitForAsyncData()

    expect(wrapper.text()).toContain('智慧城市项目年度技术报告')

    const detailBtn = wrapper.findAll('button').find((b) => b.text() === '详情')
    await detailBtn?.trigger('click')
    await waitForAsyncData()

    expect(wrapper.text()).toContain('KJ-2025-0188')
  })
})
```

- [ ] **Step 3: 运行测试验证通过**

Run: `npx vitest run src/views/internal/search/index.spec.ts`
Expected: PASS，2 个用例通过。

- [ ] **Step 4: 类型检查**

Run: `npx vue-tsc -b`
Expected: 无错误退出。

- [ ] **Step 5: 提交**

```bash
git add src/views/internal/search/index.vue src/views/internal/search/index.spec.ts
git commit -m "feat(search-guo): 实现内部档案检索利用页面"
```

## Task 7: 档案详情独立页

**Files:**
- Create: `src/views/internal/archives/detail.vue`
- Create: `src/views/internal/archives/detail.spec.ts`

> 路由 `/internal/archives/:id` 在 Task 10 注册；本 task 仅创建页面组件，复用 `ArchiveDetailPanel`。

- [ ] **Step 1: 创建详情独立页**

写入 `src/views/internal/archives/detail.vue`（满屏包裹详情面板，读路由 `:id`，带返回入口）：

```vue
<template>
  <section>
    <div class="toolbar" style="margin: 0 0 12px">
      <div>
        <h1 class="page-title">档案详情</h1>
        <p class="page-subtitle">打开详情时后端重新鉴权；权限不足仅保留历史记录提示。</p>
      </div>
      <router-link to="/internal/search" class="button ghost">返回检索</router-link>
    </div>

    <div class="card panel detail-card">
      <ArchiveDetailPanel :archive-id="archiveId" />
    </div>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import ArchiveDetailPanel from '@/views/internal/components/ArchiveDetailPanel.vue'

const route = useRoute()
const archiveId = computed(() => {
  const id = Number(route.params.id)
  return Number.isFinite(id) ? id : null
})
</script>

<style scoped>
.detail-card {
  max-width: 760px;
}
</style>
```

- [ ] **Step 2: 写页面测试**

写入 `src/views/internal/archives/detail.spec.ts`（用 `createMemoryHistory` 注入路由参数）：

```typescript
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import { createMemoryHistory, createRouter } from 'vue-router'
import InternalArchiveDetail from './detail.vue'

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

describe('InternalArchiveDetail', () => {
  it('reads archiveId from route and loads detail', async () => {
    const router = createRouter({
      history: createMemoryHistory(),
      routes: [{ path: '/internal/archives/:id', component: InternalArchiveDetail }],
    })
    await router.push('/internal/archives/101')
    const wrapper = mount(InternalArchiveDetail, { global: { plugins: [router] } })
    await waitForAsyncData()

    expect(wrapper.text()).toContain('智慧城市项目年度技术报告')
    expect(wrapper.text()).toContain('返回检索')
  })
})
```

- [ ] **Step 3: 运行测试验证通过**

Run: `npx vitest run src/views/internal/archives/detail.spec.ts`
Expected: PASS。

- [ ] **Step 4: 提交**

```bash
git add src/views/internal/archives/detail.vue src/views/internal/archives/detail.spec.ts
git commit -m "feat(search-guo): 实现档案详情独立页"
```

---

## Task 8: 工作台概览页面

**Files:**
- Modify(重写): `src/views/internal/overview/index.vue`
- Create: `src/views/internal/overview/index.spec.ts`

- [ ] **Step 1: 重写工作台**

将 `src/views/internal/overview/index.vue` 完整替换为（还原 `overview.html`：4 指标卡 + 最近查阅 + 借阅申请摘要 + 权限/下载/边界）：

```vue
<template>
  <section>
    <div v-if="loading" class="notice">加载工作台…</div>
    <div v-else-if="error" class="notice danger">{{ error }}</div>
    <template v-else-if="data">
      <section class="grid four">
        <div class="metric"><span class="label">最近查阅</span><span class="value">{{ data.stats.recentViewCount }}</span><span class="note">近 30 日元数据访问</span></div>
        <div class="metric"><span class="label">待审批申请</span><span class="value">{{ data.stats.pendingApprovalCount }}</span><span class="note">等待管理员审核</span></div>
        <div class="metric"><span class="label">已批准待取件</span><span class="value">{{ data.stats.approvedPendingPickupCount }}</span><span class="note">可导出借阅凭证</span></div>
        <div class="metric"><span class="label">下载记录</span><span class="value">{{ data.stats.downloadCount }}</span><span class="note">每次下载均记录日志</span></div>
      </section>

      <section class="dashboard-layout">
        <div class="grid">
          <div class="card panel">
            <div class="toolbar">
              <div>
                <h2 class="section-title">我的最近查阅</h2>
                <p class="page-subtitle">再次打开详情时，后端仍会按当前权限重新过滤。</p>
              </div>
              <router-link to="/internal/search" class="button">进入检索</router-link>
            </div>
            <ul class="record-list">
              <li v-for="item in data.recentViews" :key="item.id" class="record-card">
                <div>
                  <h3>{{ item.title }}</h3>
                  <div class="meta-line">
                    <span>档号：{{ item.archiveNo }}</span>
                    <span>分类：{{ item.categoryName }}</span>
                    <span>密级：{{ securityLabel(item.securityLevel) }}</span>
                    <span>{{ formatDate(item.viewedAt) }}</span>
                  </div>
                </div>
                <div class="actions">
                  <span :class="['status', item.accessStatus === 'available' ? 'success' : 'warning']">
                    {{ item.accessStatus === 'available' ? '可重新鉴权' : '仅保留记录' }}
                  </span>
                  <router-link :to="`/internal/archives/${item.archiveId}`" class="button ghost">继续查看</router-link>
                </div>
              </li>
            </ul>
          </div>

          <div class="card panel">
            <div class="toolbar">
              <div>
                <h2 class="section-title">我的借阅申请</h2>
                <p class="page-subtitle">借阅凭证不是出库记录，到馆核验并确认出库后档案才变为借出。</p>
              </div>
              <router-link to="/internal/borrow-requests" class="button ghost">查看全部</router-link>
            </div>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr><th>申请号</th><th>档案题名</th><th>申请时间</th><th>状态</th><th>操作</th></tr>
                </thead>
                <tbody>
                  <tr v-for="req in data.borrowRequests" :key="req.id">
                    <td class="mono">{{ req.requestNo }}</td>
                    <td>{{ req.archiveTitle }}</td>
                    <td>{{ formatDate(req.appliedAt) }}</td>
                    <td><span :class="['status', statusClass(req.status)]">{{ statusLabel(req.status) }}</span></td>
                    <td>
                      <button v-if="canExportVoucher(req.status)" class="button secondary" type="button" @click="handleExport(req.id)">导出凭证</button>
                      <router-link v-else :to="`/internal/borrow-requests?focus=${req.id}`" class="button ghost">查看申请</router-link>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <aside class="grid">
          <div class="card panel">
            <h2 class="section-title">我的权限范围</h2>
            <div class="authority-grid">
              <div class="authority-line"><span class="muted">角色</span><strong>{{ data.permission.role }}</strong></div>
              <div class="authority-line"><span class="muted">所属单位</span><strong>{{ data.permission.organizationName }}</strong></div>
              <div class="authority-line"><span class="muted">密级上限</span><strong>{{ securityLabel(data.permission.maxSecurityLevel) }}</strong></div>
              <div class="authority-line"><span class="muted">可见范围</span><strong>{{ data.permission.dataScope }}</strong></div>
            </div>
          </div>

          <div class="card panel">
            <h2 class="section-title">我的下载记录</h2>
            <ol class="timeline">
              <li v-for="d in data.downloads" :key="d.id">
                <span class="muted">{{ formatDate(d.downloadedAt) }}</span>
                <span>{{ d.title }}</span>
              </li>
            </ol>
          </div>

          <div class="card panel">
            <h2 class="section-title">利用边界</h2>
            <div class="notice">内部查阅不展示具体库房架位。电子预览和下载复用同一套后端鉴权，纸质原件统一走借阅申请。</div>
          </div>
        </aside>
      </section>
    </template>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { exportBorrowVoucher, getInternalDashboard } from '@/api/internal'
import { BorrowStatusLabel, SecurityLevelLabel } from '@/types/enums'
import type { InternalDashboardData } from '@/types/internal'

const loading = ref(true)
const error = ref('')
const data = ref<InternalDashboardData | null>(null)

function securityLabel(level: number): string {
  return SecurityLevelLabel[level] ?? '未知'
}
function statusLabel(status: string): string {
  return BorrowStatusLabel[status] ?? status
}
function statusClass(status: string): string {
  const map: Record<string, string> = {
    applied: 'warning',
    approved: 'success',
    voucher_issued: 'success',
    checked_out: 'info',
    returned: 'info',
    abnormal_return: 'danger',
    rejected: 'danger',
  }
  return map[status] ?? 'info'
}
function canExportVoucher(status: string): boolean {
  return status === 'approved' || status === 'voucher_issued'
}
function formatDate(iso: string): string {
  return iso.slice(0, 10)
}

async function handleExport(id: number) {
  try {
    const blob = await exportBorrowVoucher(id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `voucher-${id}.pdf`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('借阅凭证已生成，需到馆核验后才可确认出库')
  } catch {
    ElMessage.error('导出凭证失败')
  }
}

onMounted(async () => {
  loading.value = true
  try {
    data.value = await getInternalDashboard()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '加载工作台失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 16px;
  align-items: start;
  margin-top: 16px;
}
.record-list { display: grid; gap: 10px; margin: 0; padding: 0; list-style: none; }
.record-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #fff;
}
.record-card h3 { margin: 0 0 4px; font-size: 15px; }
.meta-line { display: flex; flex-wrap: wrap; gap: 8px; color: var(--muted); font-size: 13px; }
.authority-grid { display: grid; gap: 10px; }
.authority-line { display: flex; justify-content: space-between; gap: 12px; padding-bottom: 10px; border-bottom: 1px solid var(--border); }
.authority-line:last-child { border-bottom: 0; }
.timeline { margin: 0; padding-left: 18px; display: grid; gap: 8px; }
.timeline li { display: flex; gap: 10px; }
@media (max-width: 1080px) { .dashboard-layout { grid-template-columns: 1fr; } }
@media (max-width: 640px) { .record-card { grid-template-columns: 1fr; } }
</style>
```

- [ ] **Step 2: 写页面测试**

写入 `src/views/internal/overview/index.spec.ts`：

```typescript
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import InternalOverview from './index.vue'

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

describe('InternalOverview', () => {
  it('renders metrics, recent views and borrow requests from mock', async () => {
    const wrapper = mount(InternalOverview)
    await waitForAsyncData()

    expect(wrapper.text()).toContain('最近查阅')
    expect(wrapper.text()).toContain('待审批申请')
    expect(wrapper.text()).toContain('智慧城市项目年度技术报告')
    expect(wrapper.text()).toContain('BR-202606-021')
  })

  it('shows export voucher for approved and view link for others', async () => {
    const wrapper = mount(InternalOverview)
    await waitForAsyncData()

    expect(wrapper.text()).toContain('导出凭证')
    expect(wrapper.text()).toContain('查看申请')
  })
})
```

- [ ] **Step 3: 运行测试验证通过**

Run: `npx vitest run src/views/internal/overview/index.spec.ts`
Expected: PASS，2 个用例通过。

- [ ] **Step 4: 类型检查**

Run: `npx vue-tsc -b`
Expected: 无错误退出。

- [ ] **Step 5: 提交**

```bash
git add src/views/internal/overview/index.vue src/views/internal/overview/index.spec.ts
git commit -m "feat(search-guo): 实现内部工作台概览页面"
```

## Task 9: 我的借阅申请页面

**Files:**
- Create: `src/views/internal/borrow-requests/index.vue`
- Create: `src/views/internal/borrow-requests/index.spec.ts`

> 路由 `/internal/borrow-requests` 在 Task 10 注册。

- [ ] **Step 1: 创建借阅申请页**

写入 `src/views/internal/borrow-requests/index.vue`（列表筛选 + 详情 `el-drawer` + 导出凭证，承接工作台 `?focus={id}` 自动打开详情）：

```vue
<template>
  <section>
    <div class="toolbar" style="margin: 0 0 12px">
      <div>
        <h1 class="page-title">我的借阅申请</h1>
        <p class="page-subtitle">查阅本人纸质借阅申请的审批、凭证与出库归还进度。</p>
      </div>
      <router-link to="/internal/search" class="button">发起新申请</router-link>
    </div>

    <div class="card panel">
      <div class="form-grid">
        <div class="field">
          <label>状态</label>
          <select v-model="filter.status" @change="loadList">
            <option value="">全部</option>
            <option value="applied">待审批</option>
            <option value="approved">已批准</option>
            <option value="voucher_issued">凭证已生成</option>
            <option value="checked_out">已借出</option>
            <option value="returned">已归还</option>
            <option value="abnormal_return">异常归还</option>
            <option value="rejected">已拒绝</option>
          </select>
        </div>
        <div class="field">
          <label>关键词</label>
          <input v-model="filter.keyword" placeholder="申请号、档号、题名" @keyup.enter="loadList">
        </div>
        <div class="field">
          <label>&nbsp;</label>
          <button class="button" type="button" @click="loadList">查询</button>
        </div>
      </div>
    </div>

    <div class="card panel" style="margin-top: 16px">
      <div v-if="loading" class="notice">加载中…</div>
      <div v-else-if="error" class="notice danger">{{ error }}</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr><th>申请号</th><th>档案题名</th><th>申请时间</th><th>状态</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-if="records.length === 0"><td colspan="5"><div class="empty">没有符合条件的申请</div></td></tr>
            <tr v-for="req in records" :key="req.id">
              <td class="mono">{{ req.requestNo }}</td>
              <td>{{ req.archiveTitle }}</td>
              <td>{{ formatDate(req.appliedAt) }}</td>
              <td>
                <span :class="['status', statusClass(req.status)]">{{ statusLabel(req.status) }}</span>
                <span v-if="req.overdue" class="status danger" style="margin-left: 4px">已逾期</span>
              </td>
              <td>
                <button class="button ghost" type="button" @click="openDetail(req.id)">查看详情</button>
                <button v-if="canExportVoucher(req.status)" class="button secondary" type="button" @click="handleExport(req.id)">导出凭证</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <el-drawer v-model="drawerVisible" size="420px" :title="detailTitle" direction="rtl">
      <div v-if="detailLoading" class="notice">加载详情…</div>
      <div v-else-if="detail">
        <div class="detail-grid">
          <div class="detail-line"><span class="muted">申请号</span><strong>{{ detail.requestNo }}</strong></div>
          <div class="detail-line"><span class="muted">档案</span><strong>{{ detail.archiveTitle }}（{{ detail.archiveNo }}）</strong></div>
          <div class="detail-line"><span class="muted">状态</span><strong>{{ statusLabel(detail.status) }}</strong></div>
          <div class="detail-line"><span class="muted">借阅天数</span><strong>{{ detail.expectedDays }} 天</strong></div>
          <div class="detail-line"><span class="muted">预计到馆</span><strong>{{ detail.expectedVisitAt ? formatDate(detail.expectedVisitAt) : '-' }}</strong></div>
          <div class="detail-line"><span class="muted">申请时间</span><strong>{{ formatDate(detail.appliedAt) }}</strong></div>
          <div v-if="detail.approvedAt" class="detail-line"><span class="muted">批准时间</span><strong>{{ formatDate(detail.approvedAt) }}</strong></div>
          <div v-if="detail.checkedOutAt" class="detail-line"><span class="muted">出库时间</span><strong>{{ formatDate(detail.checkedOutAt) }}</strong></div>
          <div v-if="detail.dueAt" class="detail-line"><span class="muted">应还时间</span><strong>{{ formatDate(detail.dueAt) }}</strong></div>
          <div v-if="detail.returnedAt" class="detail-line"><span class="muted">归还时间</span><strong>{{ formatDate(detail.returnedAt) }}</strong></div>
          <div v-if="detail.voucherNo" class="detail-line"><span class="muted">凭证号</span><strong>{{ detail.voucherNo }}</strong></div>
          <div class="detail-line"><span class="muted">联系电话</span><strong>{{ detail.contactPhone }}</strong></div>
          <div class="detail-line"><span class="muted">借阅理由</span><strong>{{ detail.reason }}</strong></div>
          <div v-if="detail.opinion" class="detail-line"><span class="muted">审批意见</span><strong>{{ detail.opinion }}</strong></div>
          <div v-if="detail.rejectReason" class="detail-line"><span class="muted">拒绝原因</span><strong>{{ detail.rejectReason }}</strong></div>
          <div v-if="detail.returnNote" class="detail-line"><span class="muted">归还备注</span><strong>{{ detail.returnNote }}</strong></div>
        </div>
        <div v-if="canExportVoucher(detail.status)" class="actions" style="margin-top: 12px">
          <button class="button secondary" type="button" @click="handleExport(detail.id)">导出借阅凭证</button>
        </div>
        <div class="notice" style="margin-top: 12px">凭证需单位盖章，到馆核验后才可确认出库。</div>
      </div>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { exportBorrowVoucher, getBorrowRequestDetail, getMyBorrowRequests } from '@/api/internal'
import { BorrowStatusLabel } from '@/types/enums'
import type { BorrowRequest, BorrowRequestDetail } from '@/types/internal'

const route = useRoute()

const filter = reactive<{ status: string; keyword: string }>({ status: '', keyword: '' })
const loading = ref(false)
const error = ref('')
const records = ref<BorrowRequest[]>([])

const drawerVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<BorrowRequestDetail | null>(null)
const detailTitle = computed(() => detail.value?.requestNo ?? '申请详情')

function statusLabel(status: string): string {
  return BorrowStatusLabel[status] ?? status
}
function statusClass(status: string): string {
  const map: Record<string, string> = {
    applied: 'warning',
    approved: 'success',
    voucher_issued: 'success',
    checked_out: 'info',
    returned: 'info',
    abnormal_return: 'danger',
    rejected: 'danger',
  }
  return map[status] ?? 'info'
}
function canExportVoucher(status: string): boolean {
  return status === 'approved' || status === 'voucher_issued'
}
function formatDate(iso: string): string {
  return iso.slice(0, 10)
}

async function loadList() {
  loading.value = true
  error.value = ''
  try {
    const page = await getMyBorrowRequests({
      status: (filter.status || undefined) as BorrowRequest['status'] | undefined,
      keyword: filter.keyword || undefined,
    })
    records.value = page.records
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '加载借阅申请失败'
  } finally {
    loading.value = false
  }
}

async function openDetail(id: number) {
  drawerVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getBorrowRequestDetail(id)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '加载详情失败')
    drawerVisible.value = false
  } finally {
    detailLoading.value = false
  }
}

async function handleExport(id: number) {
  try {
    const blob = await exportBorrowVoucher(id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `voucher-${id}.pdf`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('借阅凭证已导出')
  } catch {
    ElMessage.error('导出凭证失败')
  }
}

onMounted(async () => {
  await loadList()
  const focusId = route.query.focus
  if (focusId) {
    openDetail(Number(focusId))
  }
})
</script>

<style scoped>
.detail-grid { display: grid; gap: 10px; }
.detail-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 9px;
  border-bottom: 1px solid var(--border);
}
.detail-line:last-child { border-bottom: 0; }
</style>
```

- [ ] **Step 2: 写页面测试**

写入 `src/views/internal/borrow-requests/index.spec.ts`（stub `ElDrawer`，因测试环境未全局注册 element-plus）：

```typescript
import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import MyBorrowRequests from './index.vue'

const stubs = {
  ElDrawer: { template: '<div class="el-drawer-stub" />' },
}

async function waitForAsyncData() {
  await new Promise((r) => setTimeout(r, 50))
  await flushPromises()
}

describe('MyBorrowRequests', () => {
  it('renders list covering multiple statuses', async () => {
    const wrapper = mount(MyBorrowRequests, { global: { stubs } })
    await waitForAsyncData()

    expect(wrapper.text()).toContain('BR-202606-021')
    expect(wrapper.text()).toContain('待审批')
    expect(wrapper.text()).toContain('已拒绝')
  })

  it('shows export voucher button for approved and voucher_issued', async () => {
    const wrapper = mount(MyBorrowRequests, { global: { stubs } })
    await waitForAsyncData()

    const exportButtons = wrapper.findAll('button').filter((b) => b.text().includes('导出凭证'))
    expect(exportButtons.length).toBe(2)
  })

  it('filters list by status', async () => {
    const wrapper = mount(MyBorrowRequests, { global: { stubs } })
    await waitForAsyncData()

    await wrapper.find('select').setValue('rejected')
    const queryBtn = wrapper.findAll('button').find((b) => b.text().includes('查询'))
    await queryBtn?.trigger('click')
    await waitForAsyncData()

    expect(wrapper.text()).toContain('BR-202605-017')
    expect(wrapper.text()).not.toContain('BR-202606-021')
  })
})
```

- [ ] **Step 3: 运行测试验证通过**

Run: `npx vitest run src/views/internal/borrow-requests/index.spec.ts`
Expected: PASS，3 个用例通过。

- [ ] **Step 4: 提交**

```bash
git add src/views/internal/borrow-requests/index.vue src/views/internal/borrow-requests/index.spec.ts
git commit -m "feat(search-guo): 实现我的借阅申请页面"
```

## Task 10: 路由与导航更新

**Files:**
- Modify: `src/router/routes/internal.ts`
- Modify: `src/layouts/InternalLayout.vue`

> 此时所有页面（Task 6-9）已创建，新增路由指向的文件均存在，`vue-tsc` 不会因缺失文件报错。

- [ ] **Step 1: 更新内部路由**

将 `src/router/routes/internal.ts` 完整替换为：

```typescript
import type { RouteRecordRaw } from 'vue-router'

export const internalRoutes: RouteRecordRaw = {
  path: '/internal',
  component: () => import('@/layouts/InternalLayout.vue'),
  meta: { requiresAuth: true },
  children: [
    { path: '', redirect: '/internal/overview' },
    {
      path: 'overview',
      component: () => import('@/views/internal/overview/index.vue'),
      meta: { title: '工作台概览' },
    },
    {
      path: 'search',
      component: () => import('@/views/internal/search/index.vue'),
      meta: { title: '档案检索利用' },
    },
    {
      path: 'archives/:id',
      component: () => import('@/views/internal/archives/detail.vue'),
      meta: { title: '档案详情' },
    },
    {
      path: 'borrow-requests',
      component: () => import('@/views/internal/borrow-requests/index.vue'),
      meta: { title: '我的借阅申请' },
    },
  ],
}
```

- [ ] **Step 2: 更新侧边导航**

打开 `src/layouts/InternalLayout.vue`，找到 `<nav class="nav-section">` 块，将整段 `<nav>` 替换为（新增「我的借阅申请」链接）：

```html
      <nav class="nav-section" aria-label="内部门户导航">
        <div class="nav-title">个人工作台</div>
        <router-link to="/internal/overview" class="nav-link" :class="{ active: route.path === '/internal/overview' }">
          工作台概览
        </router-link>
        <router-link to="/internal/search" class="nav-link" :class="{ active: route.path === '/internal/search' }">
          档案检索利用
        </router-link>
        <router-link to="/internal/borrow-requests" class="nav-link" :class="{ active: route.path === '/internal/borrow-requests' }">
          我的借阅申请
        </router-link>
      </nav>
```

- [ ] **Step 3: 类型检查**

Run: `npx vue-tsc -b`
Expected: 无错误退出。

- [ ] **Step 4: 提交**

```bash
git add src/router/routes/internal.ts src/layouts/InternalLayout.vue
git commit -m "feat(search-guo): 补充内部查阅路由与导航"
```

---

## Task 11: 内部查阅者 API mock 测试

**Files:**
- Create: `src/api/internal.spec.ts`

- [ ] **Step 1: 写 API 测试**

写入 `src/api/internal.spec.ts`（覆盖 10 个函数的 mock 返回结构、检索/借阅过滤、详情不含架位字段）：

```typescript
import { describe, expect, it } from 'vitest'
import {
  createBorrowRequest,
  generateInternalAiQuery,
  getBorrowRequestDetail,
  getInternalArchiveDetail,
  getInternalDashboard,
  getMyBorrowRequests,
  searchInternalArchives,
} from './internal'

describe('internal api mock mode', () => {
  it('returns dashboard with metrics, recent views and permission', async () => {
    const data = await getInternalDashboard()
    expect(data.stats.recentViewCount).toBeGreaterThan(0)
    expect(data.recentViews[0].archiveNo).toBeTruthy()
    expect(data.permission.maxSecurityLevel).toBeGreaterThan(0)
  })

  it('searches archives and filters by keyword', async () => {
    const all = await searchInternalArchives()
    expect(all.records.length).toBeGreaterThan(0)
    const filtered = await searchInternalArchives({ keyword: '智慧城市' })
    expect(filtered.records.every((a) => a.title.includes('智慧城市'))).toBe(true)
  })

  it('archive detail includes files and borrow flag, excludes location', async () => {
    const detail = await getInternalArchiveDetail(101)
    expect(detail.title).toContain('智慧城市')
    expect(detail.canBorrow).toBe(true)
    expect(detail.files.length).toBeGreaterThan(0)
    expect((detail as Record<string, unknown>).locationCode).toBeUndefined()
    expect((detail as Record<string, unknown>).boxNo).toBeUndefined()
  })

  it('archive detail throws for unknown id', async () => {
    await expect(getInternalArchiveDetail(99999)).rejects.toThrow()
  })

  it('generates internal AI query conditions', async () => {
    const result = await generateInternalAiQuery({ text: '智慧城市 报告' })
    expect(result.ruleType).toBe('internalSearchQuery')
    expect(result.conditions.keyword).toBeTruthy()
  })

  it('lists my borrow requests covering multiple statuses', async () => {
    const page = await getMyBorrowRequests()
    const statuses = page.records.map((r) => r.status)
    expect(statuses).toContain('applied')
    expect(statuses).toContain('rejected')
  })

  it('filters borrow requests by status', async () => {
    const page = await getMyBorrowRequests({ status: 'rejected' })
    expect(page.records.every((r) => r.status === 'rejected')).toBe(true)
    expect(page.records[0].id).toBe(306)
  })

  it('borrow request detail carries reject reason for rejected one', async () => {
    const detail = await getBorrowRequestDetail(306)
    expect(detail.rejectReason).toBeTruthy()
  })

  it('creates borrow request in applied status with archive info', async () => {
    const created = await createBorrowRequest({
      archiveId: 101,
      reason: '项目复核',
      expectedDays: 5,
      expectedVisitAt: '2026-06-14T10:00:00+08:00',
      contactPhone: '13800000004',
    })
    expect(created.status).toBe('applied')
    expect(created.archiveNo).toBe('KJ-2025-0188')
    expect(created.requestNo).toContain('BR-')
  })
})
```

- [ ] **Step 2: 运行测试验证通过**

Run: `npx vitest run src/api/internal.spec.ts`
Expected: PASS，9 个用例通过。

- [ ] **Step 3: 提交**

```bash
git add src/api/internal.spec.ts
git commit -m "test(search-guo): 补充内部查阅 API mock 测试"
```

---

## Task 12: 集成验证

**Files:** 无（仅验证）

- [ ] **Step 1: 全量类型检查**

Run: `npx vue-tsc -b`
Expected: 无错误退出。

- [ ] **Step 2: 生产构建**

Run: `npm run build`
Expected: 构建成功，输出 `dist/`，无 TypeScript 错误。

- [ ] **Step 3: 全量单元测试**

Run: `npm run test:unit -- --run`
Expected: 全部测试通过（含本轮新增 spec 与既有 spec）。若既有 spec 因本轮改动失败，定位是本轮代码问题还是测试本身问题并修复。

- [ ] **Step 4: 手动核验要点（可选，mock 模式下）**

启动 `npm run dev`，登录后切到内部门户，依次核验：

- `/internal/overview`：4 指标卡有数、最近查阅与借阅申请可见、「进入检索」可跳转、已批准申请「导出凭证」可点。
- `/internal/search`：AI 生成 JSON → 填充表单 → 确认检索出 4 条结果 → 点「详情」右侧加载详情面板 → 纯电子档案（会议纪要）借阅按钮禁用并提示 → 纸质+电子（智慧城市）可展开借阅表单并提交。
- `/internal/archives/101`：独立页加载详情，「返回检索」可回检索页。
- `/internal/borrow-requests`：7 种状态齐全，「查看详情」打开抽屉，「导出凭证」仅 approved/voucher_issued 可见，逾期记录有「已逾期」标记，状态筛选生效。

- [ ] **Step 5: 推送分支并发起 PR**

```bash
git push -u origin feat/search-guo
```

随后用 `gh pr create` 向 `develop` 发起 PR（标题 `feat(search-guo): 内部查阅者检索与借阅申请`），等待方江苏 review 合并。

---

## Self-Review

**1. Spec 覆盖**

| 设计文档章节 | 对应 Task |
|--------------|-----------|
| 2.1 页面清单（overview/search/archives/borrow-requests + 详情组件） | Task 5（详情组件）、6（检索）、7（详情页）、8（工作台）、9（借阅申请） |
| 5 文件结构 | Task 1-10 逐文件覆盖 |
| 6 API 设计（10 函数） | Task 3 |
| 7 类型设计 | Task 1 |
| 8 页面设计 | Task 5-9 |
| 9 错误处理与边界（三态/架位隐藏/AI 不查库/纯电子禁借/凭证门控） | 各页面 Task 内 + Task 11 断言不含架位 |
| 10 Mock 数据（工作台/检索/借阅 7 态） | Task 2 |
| 11 测试与验证 | 各 Task spec + Task 11 + Task 12 |
| 路由/导航补充 | Task 10 |

无遗漏章节。

**2. 占位符扫描**

全计划无 TBD/TODO/「类似上方」/「省略」。每个代码步骤均为完整可运行代码。

**3. 类型一致性**

- API 函数名：`getInternalDashboard`/`searchInternalArchives`/`getInternalArchiveDetail`/`generateInternalAiQuery`/`previewInternalFile`/`downloadInternalFile`/`createBorrowRequest`/`getMyBorrowRequests`/`getBorrowRequestDetail`/`exportBorrowVoucher`，在 Task 3 定义，Task 5-9、11 引用，名称一致。
- mock 函数名：`mockInternalDashboard` 等，Task 2 定义，Task 3 import 调用，一致。
- 类型名：`InternalSearchParams`/`InternalArchive`/`InternalArchiveDetail`/`InternalFile`/`InternalAiQueryResult`/`BorrowRequestCreateData`/`BorrowRequest`/`BorrowRequestDetail`/`InternalDashboardData`，Task 1 定义，后续 Task 引用，一致。
- `ArchiveDetailPanel` props `archiveId: number | null` + emit `borrowed`，Task 5 定义，Task 6（传 `selectedArchiveId`）、Task 7（传 `archiveId` computed）引用，类型一致。
- `validateBorrowRequest(data: Partial<BorrowRequestCreateData>)`，Task 4 定义，Task 5 调用，签名一致。
- 枚举复用 `BorrowStatusLabel`/`CarrierStatusLabel`/`SecurityLevelLabel`（来自 `enums.ts`），跨 Task 一致。

**4. 任务顺序合理性**

类型(Task1) → mock(Task2) → API(Task3) → 校验(Task4) → 详情组件(Task5) → 页面(Task6-9) → 路由(Task10) → API 测试(Task11) → 集成(Task12)。路由放在页面之后，避免 `vue-tsc` 对未创建文件的动态 import 报错。每个 Task 产出独立可提交。

---

## Execution Handoff

计划已写入 `frontend/docs/superpowers/plans/2026-06-13-search-guo.md`。两种执行方式：

1. **Subagent-Driven（推荐）** — 每个 Task 派发独立 subagent，任务间 review，快速迭代。
2. **Inline Execution** — 当前会话用 executing-plans 批量执行，带检查点。

请选择执行方式。

