import { describe, expect, it } from 'vitest'
import {
  completeAppraisalBatch,
  createAppraisalBatch,
  getAppraisalBatchDetail,
  getAppraisalBatches,
  saveAppraisalItems,
} from './appraisal'

describe('appraisal api mock mode', () => {
  it('lists batches and filters by status', async () => {
    const all = await getAppraisalBatches()
    expect(all.records.length).toBeGreaterThan(0)
    const drafts = await getAppraisalBatches({ status: 'draft' })
    expect(drafts.records.every((b) => b.status === 'draft')).toBe(true)
  })

  it('batch detail includes items with appraisal results', async () => {
    const detail = await getAppraisalBatchDetail(1)
    expect(detail.items.length).toBeGreaterThan(0)
    const results = detail.items.map((i) => i.appraisalResult)
    expect(results).toContain('extend')
    expect(results).toContain('destroy')
    expect(results).toContain('')
  })

  it('throws for unknown batch id', async () => {
    await expect(getAppraisalBatchDetail(99999)).rejects.toThrow()
  })

  it('completed batch (id=2) carries generated list info', async () => {
    const detail = await getAppraisalBatchDetail(2)
    expect(detail.status).toBe('completed')
    expect(detail.generatedListId).toBe(13)
    expect(detail.generatedListNo).toBe('DES-013')
  })

  it('creates a draft batch with hit items', async () => {
    const created = await createAppraisalBatch({
      batchName: '测试批次',
      categoryId: 3,
      formedYearStart: 2016,
      formedYearEnd: 2016,
    })
    expect(created.status).toBe('draft')
    expect(created.items.length).toBeGreaterThan(0)
    expect(created.batchNo).toContain('APP-')
  })

  it('saves appraisal items and updates counts', async () => {
    const detail = await saveAppraisalItems(1, {
      items: [
        { archiveId: 203, appraisalResult: 'destroy', opinion: '无价值' },
        { archiveId: 204, appraisalResult: 'extend', newRetentionPeriod: '30y', newRetentionUntil: '2045-12-31' },
      ],
    })
    const it203 = detail.items.find((i) => i.archiveId === 203)!
    expect(it203.appraisalResult).toBe('destroy')
    expect(detail.destroyCount).toBeGreaterThanOrEqual(2)
  })

  it('completes batch and returns generated list no', async () => {
    const detail = await completeAppraisalBatch(1)
    expect(detail.status).toBe('completed')
    expect(detail.generatedListNo).toContain('DES-')
  })
})
