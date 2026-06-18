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
