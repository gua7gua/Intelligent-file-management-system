import { describe, expect, it } from 'vitest'
import { validateDestroyConfirm } from './destructionValidation'

const valid = {
  destroyMethod: 'shredding' as const,
  supervisorName1: '刘星',
  supervisorName2: '向加明',
  destroyNote: '现场粉碎销毁',
  irrevocableConfirm: true,
  photoCount: 1,
}

describe('validateDestroyConfirm', () => {
  it('requires destroy method', () => {
    expect(validateDestroyConfirm({ ...valid, destroyMethod: '' as never })).toContain('请选择销毁方式。')
  })

  it('requires two distinct supervisors', () => {
    expect(validateDestroyConfirm({ ...valid, supervisorName1: '', supervisorName2: '' })).toContain('请填写两名监销人。')
    expect(validateDestroyConfirm({ ...valid, supervisorName1: '刘星', supervisorName2: '刘星' })).toContain(
      '两名监销人不能为同一人。',
    )
  })

  it('requires destroy note', () => {
    expect(validateDestroyConfirm({ ...valid, destroyNote: '' })).toContain('请填写销毁说明。')
  })

  it('requires irrevocable confirm checked', () => {
    expect(validateDestroyConfirm({ ...valid, irrevocableConfirm: false })).toContain(
      '请确认销毁后档案状态不可恢复。',
    )
  })

  it('requires at least one photo', () => {
    expect(validateDestroyConfirm({ ...valid, photoCount: 0 })).toContain('请至少上传一张现场照片。')
  })

  it('passes for valid input', () => {
    expect(validateDestroyConfirm(valid)).toEqual([])
  })
})
