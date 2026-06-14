import { describe, expect, it } from 'vitest'
import { normalizeExtensions, validateConfigs } from './systemConfigValidation'
import type { SystemConfig } from '@/types/system-settings'

const cfg = (key: string, value: string, editable = true, valueType: SystemConfig['valueType'] = 'number'): SystemConfig => ({ configKey: key, configValue: value, valueType, editable })

describe('validateConfigs', () => {
  it('passes in-range number config', () => {
    const r = validateConfigs([cfg('upload.max_file_size_mb', '512')])
    expect(r.valid).toBe(true)
  })
  it('rejects out-of-range max size', () => {
    const r = validateConfigs([cfg('upload.max_file_size_mb', '99999')])
    expect(r.valid).toBe(false)
    expect(r.errors[0]).toContain('1 到 2048')
  })
  it('rejects out-of-range borrow days', () => {
    const r = validateConfigs([cfg('borrow.default_days', '0')])
    expect(r.valid).toBe(false)
  })
  it('ignores non-editable items', () => {
    const r = validateConfigs([cfg('security.audit_retention', 'permanent', false, 'string')])
    expect(r.valid).toBe(true)
  })
  it('rejects bad boolean value', () => {
    const r = validateConfigs([cfg('ai.enabled', 'yes', true, 'boolean')])
    expect(r.valid).toBe(false)
  })
})

describe('normalizeExtensions', () => {
  it('strips dots lowercases and dedups', () => {
    expect(normalizeExtensions(['PDF', '.docx', 'pdf', 'JPG'])).toEqual(['pdf', 'docx', 'jpg'])
  })
  it('drops empty entries', () => {
    expect(normalizeExtensions(['', '  ', 'png'])).toEqual(['png'])
  })
})
