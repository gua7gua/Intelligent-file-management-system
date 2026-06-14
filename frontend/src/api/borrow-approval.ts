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
