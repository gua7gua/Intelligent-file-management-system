import { describe, expect, it } from 'vitest'
import { validateBorrowApprove, validateBorrowCheckout, validateBorrowReturn } from './borrowApprovalValidation'
import type { BorrowCheckoutData } from '@/types/borrow-approval'

describe('validateBorrowApprove', () => {
  it('blocks approval when inventory hit', () => {
    const errors = validateBorrowApprove({ status: 'applied', inventoryHit: true }, { approved: true, opinion: '同意' })
    expect(errors).toContain('目标档案命中进行中盘点，不可审批通过。')
  })

  it('requires reason on reject', () => {
    const errors = validateBorrowApprove({ status: 'applied', inventoryHit: false }, { approved: false })
    expect(errors).toContain('审批拒绝必须填写原因。')
  })

  it('passes approve when not inventory hit', () => {
    expect(validateBorrowApprove({ status: 'applied', inventoryHit: false }, { approved: true, opinion: '同意' })).toEqual([])
  })

  it('rejects non-applied status', () => {
    const errors = validateBorrowApprove({ status: 'approved', inventoryHit: false }, { approved: true })
    expect(errors).toContain('仅待审批申请可审批。')
  })
})

describe('validateBorrowCheckout', () => {
  const ok: BorrowCheckoutData = { voucherNo: 'VCH-001', dueAt: '2026-06-22T09:00:00+08:00' }

  it('requires voucher and dueAt', () => {
    const errors = validateBorrowCheckout({ status: 'approved' }, { voucherNo: '', dueAt: '' })
    expect(errors).toContain('凭证号必填。')
    expect(errors).toContain('应还时间必填。')
  })

  it('rejects non-approved status', () => {
    const errors = validateBorrowCheckout({ status: 'applied' }, ok)
    expect(errors).toContain('仅已批准申请可确认出库。')
  })

  it('passes for approved with voucher and dueAt', () => {
    expect(validateBorrowCheckout({ status: 'approved' }, ok)).toEqual([])
  })
})

describe('validateBorrowReturn', () => {
  it('requires note for abnormal return', () => {
    const errors = validateBorrowReturn({ status: 'checked_out' }, { returnCheckResult: 'damaged' })
    expect(errors).toContain('异常归还必须填写检查说明。')
  })

  it('passes normal return without note', () => {
    expect(validateBorrowReturn({ status: 'checked_out' }, { returnCheckResult: 'normal' })).toEqual([])
  })

  it('rejects non-checked-out status', () => {
    const errors = validateBorrowReturn({ status: 'approved' }, { returnCheckResult: 'normal' })
    expect(errors).toContain('仅已出库申请可确认归还。')
  })
})
