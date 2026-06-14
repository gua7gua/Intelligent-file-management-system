import { describe, expect, it } from 'vitest'
import {
  completeInventoryTask, createInventoryTask, getInventoryTaskDetail,
  getInventoryTasks, startInventoryTask, updateInventoryItem,
} from './inventory'

describe('inventory api mock mode', () => {
  it('lists tasks across statuses', async () => {
    const all = await getInventoryTasks({ pageSize: 50 })
    const statuses = all.records.map((t) => t.status)
    expect(statuses).toContain('running')
    expect(statuses).toContain('completed')
  })

  it('task detail includes items and stats', async () => {
    const detail = await getInventoryTaskDetail(1)
    expect(detail.items.length).toBeGreaterThan(0)
    expect(detail.stats.total).toBe(detail.items.length)
    expect(detail.stats.checked).toBe(detail.items.filter((i) => i.checkResult !== '').length)
  })

  it('create task generates expected items', async () => {
    const detail = await createInventoryTask({ taskName: '测试盘点', roomId: 1, categoryId: 1 })
    expect(detail.status).toBe('draft')
    expect(detail.items.length).toBeGreaterThan(0)
    expect(detail.items.every((i) => i.checkResult === '')).toBe(true)
  })

  it('start moves draft to running', async () => {
    const created = await createInventoryTask({ taskName: 't', roomId: 2, categoryId: 3 })
    const started = await startInventoryTask(created.id)
    expect(started.status).toBe('running')
    expect(started.startedAt).toBeTruthy()
  })

  it('update item requires running task', async () => {
    const detail = await getInventoryTaskDetail(1)
    const item = detail.items[0]
    const updated = await updateInventoryItem(detail.id, item.id, {
      actualLocationCode: item.expectedLocationCode, checkResult: 'normal', note: '一致',
    })
    expect(updated.checkResult).toBe('normal')
  })

  it('complete moves running to completed with summary', async () => {
    const detail = await completeInventoryTask(1, { summary: '' })
    expect(detail.status).toBe('completed')
    expect(detail.summary).toContain('应盘')
  })
})
