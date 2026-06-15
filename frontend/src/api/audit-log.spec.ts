import { describe, expect, it } from 'vitest'
import { getAuditLogs } from './audit-log'

describe('audit-log api mock mode', () => {
  it('returns cursor-paginated audit logs', async () => {
    const res = await getAuditLogs({ limit: 5 })
    expect(res.records.length).toBeLessThanOrEqual(5)
    expect(res.records[0]).toHaveProperty('operationType')
    expect(res).toHaveProperty('nextCursor')
    expect(res).toHaveProperty('hasNext')
  })

  it('filters by actorType', async () => {
    const res = await getAuditLogs({ actorType: 'system' })
    expect(res.records.every((l) => l.actorType === 'system')).toBe(true)
  })

  it('loads next page via cursor without overlap', async () => {
    const first = await getAuditLogs({ limit: 5 })
    expect(first.hasNext).toBe(true)
    const next = await getAuditLogs({ limit: 5, cursor: first.nextCursor! })
    expect(next.records.length).toBeGreaterThan(0)
    const ids = new Set(first.records.map((r) => r.id))
    expect(next.records.every((r) => !ids.has(r.id))).toBe(true)
  })
})
