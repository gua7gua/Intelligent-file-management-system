import { describe, expect, it } from 'vitest'
import { validateFondsForm } from './fondsValidation'

describe('validateFondsForm', () => {
  const base = { fondsNo: 'F999', fondsName: '测试全宗', organizationId: 1 }

  it('passes with complete create input', () => {
    expect(validateFondsForm(base, ['F001'], true).valid).toBe(true)
  })

  it('rejects empty fondsNo', () => {
    const r = validateFondsForm({ ...base, fondsNo: '  ' }, [], true)
    expect(r.valid).toBe(false)
    expect(r.errors[0]).toContain('全宗号')
  })

  it('rejects duplicate fondsNo on create', () => {
    const r = validateFondsForm({ ...base, fondsNo: 'F001' }, ['F001'], true)
    expect(r.valid).toBe(false)
    expect(r.errors[0]).toContain('已存在')
  })

  it('allows keeping own fondsNo on edit', () => {
    expect(validateFondsForm({ ...base, fondsNo: 'F001' }, ['F001'], false).valid).toBe(true)
  })

  it('rejects empty fondsName', () => {
    const r = validateFondsForm({ ...base, fondsName: '' }, [], true)
    expect(r.valid).toBe(false)
    expect(r.errors[0]).toContain('全宗名称')
  })

  it('rejects missing organizationId', () => {
    const r = validateFondsForm({ ...base, organizationId: undefined as unknown as number }, [], true)
    expect(r.valid).toBe(false)
    expect(r.errors[0]).toContain('所属单位')
  })
})
