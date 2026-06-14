import { describe, expect, it } from 'vitest'
import { validateAppraisalCompletion, validateAppraisalItem } from './appraisalValidation'
import type { AppraisalItem } from '@/types/appraisal'

const mk = (over: Partial<AppraisalItem>): AppraisalItem => ({
  archiveId: 1,
  archiveNo: 'ARC-001',
  title: 't',
  categoryName: 'c',
  retentionPeriod: '10y',
  retentionUntil: '2025-12-31',
  currentLifecycleStatus: 'normal',
  appraisalResult: '',
  ...over,
})

describe('validateAppraisalItem', () => {
  it('requires an appraisal result', () => {
    expect(validateAppraisalItem(mk({ appraisalResult: '' }))).toContain('请选择鉴定结论（延长保存或待销毁）。')
  })

  it('extend requires new retention period and until', () => {
    const errors = validateAppraisalItem(mk({ appraisalResult: 'extend' }))
    expect(errors).toContain('延长保存需填写新保管期限。')
    expect(errors).toContain('延长保存需填写新到期日。')
  })

  it('extend passes with period and until', () => {
    expect(
      validateAppraisalItem(
        mk({ appraisalResult: 'extend', newRetentionPeriod: '30y', newRetentionUntil: '2045-12-31' }),
      ),
    ).toEqual([])
  })

  it('destroy passes without new retention', () => {
    expect(validateAppraisalItem(mk({ appraisalResult: 'destroy' }))).toEqual([])
  })
})

describe('validateAppraisalCompletion', () => {
  it('reports when any item unprocessed', () => {
    const errors = validateAppraisalCompletion([
      mk({ archiveId: 1, appraisalResult: 'destroy' }),
      mk({ archiveId: 2, appraisalResult: '' }),
    ])
    expect(errors.some((e) => /仍有\s+\d+\s+条未处理的鉴定条目/.test(e))).toBe(true)
  })

  it('reports extend item missing new period', () => {
    const errors = validateAppraisalCompletion([
      mk({ archiveId: 1, appraisalResult: 'extend', newRetentionPeriod: '', newRetentionUntil: '' }),
    ])
    expect(errors.some((e) => e.includes('延长保存'))).toBe(true)
  })

  it('passes when all items valid', () => {
    expect(
      validateAppraisalCompletion([
        mk({ archiveId: 1, appraisalResult: 'destroy' }),
        mk({ archiveId: 2, appraisalResult: 'extend', newRetentionPeriod: '30y', newRetentionUntil: '2045-12-31' }),
      ]),
    ).toEqual([])
  })
})
