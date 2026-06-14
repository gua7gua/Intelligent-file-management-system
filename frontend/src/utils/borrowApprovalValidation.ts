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
  detail: Pick<BorrowApprovalDetail, 'status' | 'voucherNo'>,
  data: BorrowCheckoutData,
): string[] {
  const errors: string[] = []
  if (detail.status !== 'approved' && detail.status !== 'voucher_issued') errors.push('仅已批准申请可确认出库。')
  if (!data.voucherNo.trim()) errors.push('凭证号必填。')
  if (!data.dueAt) errors.push('应还时间必填。')
  // 已导出凭证（voucher_issued）时，出库录入的凭证号必须与已导出凭证一致
  if (detail.voucherNo && data.voucherNo !== detail.voucherNo) {
    errors.push('凭证号与已导出凭证不匹配。')
  }
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
