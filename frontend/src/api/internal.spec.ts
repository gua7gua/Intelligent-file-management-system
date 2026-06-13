import { describe, expect, it } from 'vitest'
import {
  createBorrowRequest,
  generateInternalAiQuery,
  getBorrowRequestDetail,
  getInternalArchiveDetail,
  getInternalDashboard,
  getMyBorrowRequests,
  searchInternalArchives,
} from './internal'

describe('internal api mock mode', () => {
  it('returns dashboard with metrics, recent views and permission', async () => {
    const data = await getInternalDashboard()
    expect(data.stats.recentViewCount).toBeGreaterThan(0)
    expect(data.recentViews[0].archiveNo).toBeTruthy()
    expect(data.permission.maxSecurityLevel).toBeGreaterThan(0)
  })

  it('searches archives and filters by keyword', async () => {
    const all = await searchInternalArchives()
    expect(all.records.length).toBeGreaterThan(0)
    const filtered = await searchInternalArchives({ keyword: '智慧城市' })
    expect(filtered.records.every((a) => a.title.includes('智慧城市'))).toBe(true)
  })

  it('archive detail includes files and borrow flag, excludes location', async () => {
    const detail = await getInternalArchiveDetail(101)
    expect(detail.title).toContain('智慧城市')
    expect(detail.canBorrow).toBe(true)
    expect(detail.files.length).toBeGreaterThan(0)
    expect((detail as Record<string, unknown>).locationCode).toBeUndefined()
    expect((detail as Record<string, unknown>).boxNo).toBeUndefined()
  })

  it('archive detail throws for unknown id', async () => {
    await expect(getInternalArchiveDetail(99999)).rejects.toThrow()
  })

  it('generates internal AI query conditions', async () => {
    const result = await generateInternalAiQuery({ text: '智慧城市 报告' })
    expect(result.ruleType).toBe('internalSearchQuery')
    expect(result.conditions.keyword).toBeTruthy()
  })

  it('lists my borrow requests covering multiple statuses', async () => {
    const page = await getMyBorrowRequests()
    const statuses = page.records.map((r) => r.status)
    expect(statuses).toContain('applied')
    expect(statuses).toContain('rejected')
  })

  it('filters borrow requests by status', async () => {
    const page = await getMyBorrowRequests({ status: 'rejected' })
    expect(page.records.every((r) => r.status === 'rejected')).toBe(true)
    expect(page.records[0].id).toBe(306)
  })

  it('borrow request detail carries reject reason for rejected one', async () => {
    const detail = await getBorrowRequestDetail(306)
    expect(detail.rejectReason).toBeTruthy()
  })

  it('creates borrow request in applied status with archive info', async () => {
    const created = await createBorrowRequest({
      archiveId: 101,
      reason: '项目复核',
      expectedDays: 5,
      expectedVisitAt: '2026-06-14T10:00:00+08:00',
      contactPhone: '13800000004',
    })
    expect(created.status).toBe('applied')
    expect(created.archiveNo).toBe('KJ-2025-0188')
    expect(created.requestNo).toContain('BR-')
  })
})
