import { describe, expect, it } from 'vitest'
import { createAnalysisTask, getAnalysisTaskDetail, getAnalysisTasks, handleAnalysisItem } from './data-analysis'

describe('data-analysis api mock mode', () => {
  it('lists tasks across statuses', async () => {
    const all = await getAnalysisTasks({ pageSize: 50 })
    const statuses = all.records.map((t) => t.status)
    expect(statuses).toContain('completed')
  })

  it('create task generates candidate items', async () => {
    const detail = await createAnalysisTask({
      taskType: 'mixed',
      rule: { categoryIds: [1], formedYearStart: 2020, formedYearEnd: 2026, includeAiSuggestion: true },
    })
    expect(detail.status).toBe('running')
    expect(detail.items.length).toBeGreaterThan(0)
    expect(detail.scannedCount).toBeGreaterThan(0)
    expect(detail.abnormalCount).toBe(detail.items.length)
  })

  it('task detail items carry problem type and status', async () => {
    const detail = await getAnalysisTaskDetail(1)
    expect(detail.items.length).toBeGreaterThan(0)
    expect(detail.items[0].problemType).toBeTruthy()
    expect(detail.items[0].status).toBe('pending')
  })

  it('handle moves pending to adopted and rejects double handling', async () => {
    const detail = await getAnalysisTaskDetail(1)
    const itemId = detail.items[0].id
    const adopted = await handleAnalysisItem(itemId, { action: 'adopted', note: '采纳' })
    expect(adopted.status).toBe('adopted')
    await expect(handleAnalysisItem(itemId, { action: 'rejected' })).rejects.toThrow()
  })
})
