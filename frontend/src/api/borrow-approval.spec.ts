import { beforeEach, describe, expect, it } from 'vitest'
import {
  approveBorrowRequest, checkoutBorrowRequest, exportBorrowVoucher,
  getBorrowApprovals, returnBorrowRequest,
} from './borrow-approval'
import { __resetBorrowApprovalMock } from '@/mock/modules/borrow-approval'

describe('borrow-approval api mock mode', () => {
  beforeEach(() => {
    __resetBorrowApprovalMock()
  })
  it('lists requests across statuses with overdue', async () => {
    const all = await getBorrowApprovals({ pageSize: 50 })
    const statuses = all.records.map((r) => r.status)
    expect(statuses).toContain('applied')
    expect(statuses).toContain('checked_out')
    expect(statuses).toContain('returned')

    const overdue = await getBorrowApprovals({ overdue: true, pageSize: 50 })
    expect(overdue.records.every((r) => r.overdue)).toBe(true)
  })

  it('inventory hit blocks approval', async () => {
    await expect(approveBorrowRequest(2, { approved: true, opinion: '同意' })).rejects.toThrow()
  })

  it('reject requires reason', async () => {
    await expect(approveBorrowRequest(1, { approved: false })).rejects.toThrow()
    const rejected = await approveBorrowRequest(1, { approved: false, rejectReason: '材料不全' })
    expect(rejected.status).toBe('rejected')
    expect(rejected.rejectReason).toBe('材料不全')
  })

  it('checkout moves approved to checked_out', async () => {
    const result = await checkoutBorrowRequest(3, { voucherNo: 'VCH-2026-0013', dueAt: '2026-06-22T09:00:00+08:00' })
    expect(result.status).toBe('checked_out')
    expect(result.dueAt).toBe('2026-06-22T09:00:00+08:00')
  })

  it('abnormal return requires note', async () => {
    await expect(returnBorrowRequest(6, { returnCheckResult: 'damaged' })).rejects.toThrow()
    const returned = await returnBorrowRequest(6, { returnCheckResult: 'damaged', returnNote: '边角破损' })
    expect(returned.status).toBe('abnormal_return')
  })

  it('normal return moves to returned', async () => {
    const returned = await returnBorrowRequest(5, { returnCheckResult: 'normal', returnNote: '完好' })
    expect(returned.status).toBe('returned')
  })

  it('export voucher issues first time and reuses', async () => {
    const first = await exportBorrowVoucher(4)
    expect(first.firstIssued).toBe(false)
    expect(first.voucherNo).toBe('VCH-2026-0010')
    const fresh = await exportBorrowVoucher(3)
    expect(fresh.firstIssued).toBe(true)
    expect(fresh.voucherNo).toMatch(/^VCH-2026-/)
  })
})
