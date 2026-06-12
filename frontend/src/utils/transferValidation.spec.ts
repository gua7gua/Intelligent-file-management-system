import { describe, expect, it } from 'vitest'
import { validateTransferDraft } from './transferValidation'

describe('validateTransferDraft', () => {
  it('reports missing batch fields and item carrier status', () => {
    const errors = validateTransferDraft({
      title: '',
      departmentName: '',
      contactPhone: '',
      archiveYear: undefined,
      expectedTransferDate: '',
      items: [
        {
          inputTitle: '2025 年第一季度会计凭证',
          retentionPeriod: '30y',
          carrierStatus: '',
          securityLevel: 1,
          openStatus: 'closed',
          allowDigitization: true,
        },
      ],
    })

    expect(errors).toContain('请填写清单标题。')
    expect(errors).toContain('请填写移交部门。')
    expect(errors).toContain('请填写联系电话。')
    expect(errors).toContain('请选择档案所属年度。')
    expect(errors).toContain('请选择预计移交日期。')
    expect(errors).toContain('第 1 条请选择载体状态。')
  })

  it('reports conflict when classified item is marked open', () => {
    const errors = validateTransferDraft({
      title: '2025 年度财政会计档案移交清单',
      departmentName: '财务处',
      contactPhone: '13800000003',
      archiveYear: 2025,
      expectedTransferDate: '2026-05-20',
      items: [
        {
          inputTitle: '涉密会议记录',
          retentionPeriod: '30y',
          carrierStatus: 'paper',
          securityLevel: 2,
          openStatus: 'open',
          allowDigitization: false,
        },
      ],
    })

    expect(errors).toEqual(['第 1 条涉密档案不能设置为公开。'])
  })
})
