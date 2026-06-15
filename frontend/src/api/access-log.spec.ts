import { describe, expect, it } from 'vitest'
import { getAccessLogs } from './access-log'

describe('access-log api mock mode', () => {
  it('returns cursor-paginated access logs', async () => {
    const res = await getAccessLogs({ limit: 5 })
    expect(res.records.length).toBeLessThanOrEqual(5)
    expect(res.records[0]).toHaveProperty('accessType')
    expect(res).toHaveProperty('hasNext')
  })

  it('filters by accessType', async () => {
    const res = await getAccessLogs({ accessType: 'download' })
    expect(res.records.every((l) => l.accessType === 'download')).toBe(true)
  })

  it('loads next page via cursor without overlap', async () => {
    const first = await getAccessLogs({ limit: 5 })
    expect(first.hasNext).toBe(true)
    const next = await getAccessLogs({ limit: 5, cursor: first.nextCursor! })
    expect(next.records.length).toBeGreaterThan(0)
    const ids = new Set(first.records.map((r) => r.id))
    expect(next.records.every((r) => !ids.has(r.id))).toBe(true)
  })
})
