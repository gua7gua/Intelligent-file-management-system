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
  // 后端 ArchiveSummaryResponse 返回 archiveId/tagNames/hasElectronicFile 等，
  // 前端 InternalArchive 期望 id/tags/canPreview/canDownload，做一次字段映射 + 派生利用状态。
  return request.get('/internal/archives/search', { params }).then((page) => {
    if (page && Array.isArray(page.records)) {
      page.records = page.records.map(normalizeInternalArchive)
    }
    return page
  })
}

/** 后端检索摘要 → 前端 InternalArchive。
 *  tagNames→tags；canPreview/canDownload 由 hasElectronicFile 派生（内部在密级+数据范围权限内可访问电子件，
 *  已由 internalSearch 过滤保证）；后端摘要不含 loanStatus，canBorrow 暂置 false（利用状态只显预览/下载）。 */
function normalizeInternalArchive(raw: Record<string, unknown>): InternalArchive {
  const hasFile = raw.hasElectronicFile === true
  const tagNames = (raw.tagNames as string[] | undefined) ?? (raw.tags as string[] | undefined)
  return {
    id: (raw.archiveId as number) ?? (raw.id as number) ?? 0,
    archiveNo: (raw.archiveNo as string) ?? '',
    title: (raw.title as string) ?? '',
    categoryId: (raw.categoryId as number) ?? 0,
    categoryName: (raw.categoryName as string) ?? '',
    responsibleText: (raw.responsibleText as string) ?? '',
    formedDate: (raw.formedDate as string) ?? '',
    formedYear: (raw.formedYear as number) ?? null,
    fondsName: (raw.fondsName as string) ?? '',
    organizationName: (raw.organizationName as string) ?? '',
    securityLevel: (raw.securityLevel as number) ?? 0,
    openStatus: (raw.openStatus as InternalArchive['openStatus']) ?? 'open',
    carrierStatus: (raw.carrierStatus as InternalArchive['carrierStatus']) ?? 'paper',
    sourceType: (raw.sourceType as InternalArchive['sourceType']) ?? 'transfer',
    tags: Array.isArray(tagNames) ? tagNames : [],
    hasElectronicFile: hasFile,
    canPreview: (raw.canPreview as boolean) ?? hasFile,
    canDownload: (raw.canDownload as boolean) ?? hasFile,
    canBorrow: (raw.canBorrow as boolean) ?? false,
    borrowHint: (raw.borrowHint as string) ?? '',
    archivedAt: (raw.archivedAt as string) ?? '',
  }
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
  return request.post('/internal/archives/ai-query', data, { timeout: 60000 })
}

/** 11.5 内部预览电子文件（返回 Blob，供 FilePreview 组件渲染） */
export function previewInternalFile(fileId: number): Promise<Blob> {
  if (USE_MOCK) return import('@/mock/modules/internal').then((m) => m.mockPreviewInternalFile(fileId))
  return request.get(`/internal/archive-files/${fileId}/preview`, { responseType: 'blob' }) as Promise<Blob>
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
  // 后端返回 archive 嵌套对象（{archiveId, archiveNo, title,...}）+ createdAt，
  // 前端 BorrowRequest 类型期望扁平字段 archiveId/archiveNo/archiveTitle/appliedAt，
  // 在 API 层做一次归一化，避免每个调用方重复适配。
  return request.get('/internal/borrow-requests', { params }).then(normalizeBorrowPage)
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
  return request.get(`/internal/borrow-requests/${requestId}`).then(normalizeBorrowDetail)
}

// 后端 BorrowRequestResponse：archive 为嵌套对象、申请时间为 createdAt。
// 前端类型 BorrowRequest：扁平 archiveId/archiveNo/archiveTitle、申请时间为 appliedAt。
// 保留后端原始字段（如 createdAt）以兼容历史代码，同时补齐前端期望的扁平字段。
function normalizeBorrowItem<T extends Record<string, unknown>>(raw: T): T {
  if (!raw) return raw
  const archive = raw.archive as { archiveId?: number; archiveNo?: string; title?: string } | undefined
  const out: Record<string, unknown> = { ...raw }
  if (archive) {
    if (archive.archiveId != null && out.archiveId == null) out.archiveId = archive.archiveId
    if (archive.archiveNo && !out.archiveNo) out.archiveNo = archive.archiveNo
    if (archive.title && !out.archiveTitle) out.archiveTitle = archive.title
  }
  // appliedAt ← createdAt（后端实体审计字段）
  if (raw.createdAt && !out.appliedAt) out.appliedAt = raw.createdAt
  return out as T
}

function normalizeBorrowPage(page: BorrowRequestPage): BorrowRequestPage {
  if (!page || !Array.isArray(page.records)) return page
  return { ...page, records: page.records.map(normalizeBorrowItem) }
}

function normalizeBorrowDetail(detail: BorrowRequestDetail): BorrowRequestDetail {
  return normalizeBorrowItem(detail)
}

/** 11.10 导出借阅凭证 */
export function exportBorrowVoucher(requestId: number): Promise<Blob> {
  if (USE_MOCK) return import('@/mock/modules/internal').then((m) => m.mockExportBorrowVoucher(requestId))
  return request.get(`/internal/borrow-requests/${requestId}/voucher`, { responseType: 'blob' }) as Promise<Blob>
}

/** 11.11 撤回借阅申请（仅本人 + applied 状态，撤回即删除） */
export function withdrawBorrowRequest(requestId: number): Promise<void> {
  if (USE_MOCK) return Promise.resolve()
  return request.post(`/internal/borrow-requests/${requestId}/withdraw`)
}
