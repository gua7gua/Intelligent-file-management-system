import request from './request'
import type { PageData } from '@/types/api'
import type {
  BorrowApprovalDetail, BorrowApprovalParams, BorrowApproveData,
  BorrowCheckoutData, BorrowReturnData, BorrowVoucherResult,
} from '@/types/borrow-approval'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/**
 * 后端 §12 返回嵌套结构（archive/borrower/location 对象 + checkCarrier 等可借检查字段），
 * 本页组件按扁平字段（borrowerName/archiveNo/archiveTitle/archiveLocationCode/carrierStatus）消费。
 * 这里做一次适配，把后端真实返回映射到 BorrowApprovalDetail 扁平类型。
 */
interface BorrowApprovalRaw {
  id: number
  requestNo: string
  status: string
  reason?: string
  expectedDays?: number
  expectedVisitAt?: string | null
  contactPhone?: string | null
  dueAt?: string | null
  overdue?: boolean
  rejectReason?: string | null
  voucherNo?: string | null
  voucherIssuedAt?: string | null
  approvedAt?: string | null
  approvedBy?: number | null
  checkedOutAt?: string | null
  returnedAt?: string | null
  returnCheckResult?: string | null
  returnNote?: string | null
  createdAt?: string
  checkCarrier?: string | null
  checkLifecycle?: string | null
  checkLoan?: string | null
  checkInventory?: string | null
  archive?: { archiveId?: number; archiveNo?: string; title?: string; carrierStatus?: string } | null
  borrower?: { borrowerId?: number; realName?: string; employeeNo?: string; departmentName?: string; organizationName?: string } | null
  location?: { boxNo?: string; locationCode?: string } | null
}

function adaptDetail(raw: BorrowApprovalRaw): BorrowApprovalDetail {
  const archive = raw.archive ?? {}
  const borrower = raw.borrower ?? {}
  const location = raw.location ?? null
  // BorrowApprovalDetail 继承自 BorrowRequestDetail/BorrowRequest，扁平字段来自适配
  return {
    id: raw.id,
    requestNo: raw.requestNo,
    archiveId: archive.archiveId ?? 0,
    archiveNo: archive.archiveNo ?? '',
    archiveTitle: archive.title ?? '',
    status: raw.status as BorrowApprovalDetail['status'],
    expectedDays: raw.expectedDays ?? 0,
    expectedVisitAt: raw.expectedVisitAt ?? undefined,
    appliedAt: raw.createdAt ?? '',
    approvedAt: raw.approvedAt ?? undefined,
    checkedOutAt: raw.checkedOutAt ?? undefined,
    dueAt: raw.dueAt ?? undefined,
    returnedAt: raw.returnedAt ?? undefined,
    overdue: raw.overdue,
    voucherNo: raw.voucherNo ?? undefined,
    voucherIssuedAt: raw.voucherIssuedAt ?? undefined,
    reason: raw.reason ?? '',
    contactPhone: raw.contactPhone ?? '',
    rejectReason: raw.rejectReason ?? undefined,
    returnCheckResult: raw.returnCheckResult ?? undefined,
    returnNote: raw.returnNote ?? undefined,
    borrowerName: borrower.realName ?? '',
    borrowerOrg: borrower.organizationName ?? borrower.departmentName,
    carrierStatus: archive.carrierStatus as BorrowApprovalDetail['carrierStatus'],
    archiveLocationCode: location?.locationCode ?? undefined,
    checkCarrier: raw.checkCarrier ?? undefined,
    checkLifecycle: raw.checkLifecycle ?? undefined,
    checkLoan: raw.checkLoan ?? undefined,
    checkInventory: raw.checkInventory ?? undefined,
  } as BorrowApprovalDetail
}

/** 查询借阅申请（§12.1） */
export async function getBorrowApprovals(params?: BorrowApprovalParams): Promise<PageData<BorrowApprovalDetail>> {
  if (USE_MOCK) {
    return import('@/mock/modules/borrow-approval').then((m) => m.mockBorrowApprovals(params))
  }
  const page = await request.get('/admin/borrow-requests', { params }) as unknown as PageData<BorrowApprovalRaw>
  return { ...page, records: (page.records ?? []).map(adaptDetail) }
}

/** 获取借阅申请详情（§12.2） */
export async function getBorrowApprovalDetail(requestId: number): Promise<BorrowApprovalDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/borrow-approval').then((m) => m.mockBorrowApprovalDetail(requestId))
  }
  const raw = await request.get(`/admin/borrow-requests/${requestId}`) as unknown as BorrowApprovalRaw
  return adaptDetail(raw)
}

/** 审批借阅申请（§12.3） */
export async function approveBorrowRequest(requestId: number, data: BorrowApproveData): Promise<BorrowApprovalDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/borrow-approval').then((m) => m.mockApproveBorrowRequest(requestId, data))
  }
  const raw = await request.post(`/admin/borrow-requests/${requestId}/approve`, data) as unknown as BorrowApprovalRaw
  return adaptDetail(raw)
}

/** 核验凭证并确认出库（§12.4） */
export async function checkoutBorrowRequest(requestId: number, data: BorrowCheckoutData): Promise<BorrowApprovalDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/borrow-approval').then((m) => m.mockCheckoutBorrowRequest(requestId, data))
  }
  const raw = await request.post(`/admin/borrow-requests/${requestId}/checkout`, data) as unknown as BorrowApprovalRaw
  return adaptDetail(raw)
}

/** 确认归还（§12.5） */
export async function returnBorrowRequest(requestId: number, data: BorrowReturnData): Promise<BorrowApprovalDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/borrow-approval').then((m) => m.mockReturnBorrowRequest(requestId, data))
  }
  const raw = await request.post(`/admin/borrow-requests/${requestId}/return`, data) as unknown as BorrowApprovalRaw
  return adaptDetail(raw)
}

/** 导出/生成借阅凭证（api 层预留，与 §11.10 对称的管理端点） */
export function exportBorrowVoucher(requestId: number): Promise<BorrowVoucherResult> {
  if (USE_MOCK) {
    return import('@/mock/modules/borrow-approval').then((m) => m.mockExportBorrowVoucher(requestId))
  }
  return request.post(`/admin/borrow-requests/${requestId}/voucher`)
}
