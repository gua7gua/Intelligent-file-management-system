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
