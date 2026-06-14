import { describe, expect, it } from 'vitest'
import { batchUpdateSystemConfigs, getSystemConfigs, updateSystemConfig } from './system-settings'

describe('system-settings api mock mode', () => {
  it('returns config list with editable flags', async () => {
    const list = await getSystemConfigs()
    expect(list.length).toBeGreaterThan(0)
    const nonEditable = list.find((c) => !c.editable)
    expect(nonEditable?.configKey).toBe('security.audit_retention')
  })

  it('updates editable config value', async () => {
    const updated = await updateSystemConfig('upload.max_file_size_mb', '1024')
    expect(updated.configValue).toBe('1024')
  })

  it('rejects updating non-editable config', async () => {
    await expect(updateSystemConfig('security.audit_retention', '5y')).rejects.toThrow('不允许修改')
  })

  it('batch updates multiple editable items', async () => {
    const updated = await batchUpdateSystemConfigs({ items: [{ configKey: 'ai.enabled', configValue: 'false' }, { configKey: 'borrow.default_days', configValue: '30' }] })
    expect(updated).toHaveLength(2)
    expect(updated.find((c) => c.configKey === 'ai.enabled')?.configValue).toBe('false')
  })
})
