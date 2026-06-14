import { describe, expect, it } from 'vitest'
import { getFonds } from './fonds'

describe('fonds api mock mode', () => {
  it('returns fonds list with org reference', async () => {
    const res = await getFonds()
    expect(res.records.length).toBeGreaterThan(0)
    expect(res.records[0].fondsNo).toMatch(/^F\d+$/)
    expect(res.records[0]).toHaveProperty('organizationName')
  })

  it('filters by status', async () => {
    const res = await getFonds({ status: 'active' })
    expect(res.records.every((f) => f.status === 'active')).toBe(true)
  })
})
