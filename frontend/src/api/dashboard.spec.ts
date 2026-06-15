import { describe, expect, it } from 'vitest'
import { getDashboardSummary } from './dashboard'

describe('dashboard api mock mode', () => {
  it('returns todos with all count fields', async () => {
    const s = await getDashboardSummary()
    expect(s.todos.pendingTransferReception).toBeGreaterThan(0)
    expect(s.todos.pendingArchive).toBeGreaterThan(0)
    expect(s.todos.borrowApproval).toBeGreaterThan(0)
    expect(s.todos.approvalPending).toBeGreaterThan(0)
    expect(s.todos.pendingDestruction).toBeGreaterThan(0)
  })

  it('exposes todo entries with route and severity', async () => {
    const s = await getDashboardSummary()
    expect(s.todoEntries.length).toBeGreaterThanOrEqual(4)
    for (const e of s.todoEntries) {
      expect(e.targetRoute).toBeTruthy()
      expect(['info', 'warning', 'danger']).toContain(e.severity)
    }
  })

  it('flags storage warning when usage exceeds threshold', async () => {
    const s = await getDashboardSummary()
    expect(s.archiveSummary.storageUsage).toBeGreaterThanOrEqual(s.archiveSummary.storageWarningThreshold)
  })

  it('includes recent audit logs', async () => {
    const s = await getDashboardSummary()
    expect(s.recentAuditLogs.length).toBeGreaterThanOrEqual(4)
    expect(s.recentAuditLogs[0].operator).toBeTruthy()
  })
})
