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
  /** 应还时间：留空由后端按借阅时长（expectedDays）自动计算 */
  dueAt?: string
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
