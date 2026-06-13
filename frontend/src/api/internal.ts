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
