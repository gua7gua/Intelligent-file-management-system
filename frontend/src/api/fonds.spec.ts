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

  it('createFonds returns new active item with archiveCount 0', async () => {
    const { createFonds } = await import('./fonds')
    const item = await createFonds({ fondsNo: 'F999', fondsName: '测试全宗', organizationId: 1 })
    expect(item.fondsNo).toBe('F999')
    expect(item.status).toBe('active')
    expect(item.archiveCount).toBe(0)
    expect(item.boxCount).toBe(0)
    expect(typeof item.id).toBe('number')
  })

  it('createFonds rejects duplicate fondsNo', async () => {
    const { createFonds, getFonds } = await import('./fonds')
    const first = await getFonds()
    const dup = first.records[0].fondsNo
    await expect(createFonds({ fondsNo: dup, fondsName: '重复', organizationId: 1 })).rejects.toThrow()
  })

  it('updateFonds changes name and status to disabled', async () => {
    const { createFonds, updateFonds } = await import('./fonds')
    const item = await createFonds({ fondsNo: 'F800', fondsName: '原名称', organizationId: 1 })
    const updated = await updateFonds(item.id, { fondsName: '新名称', status: 'disabled' })
    expect(updated.fondsName).toBe('新名称')
    expect(updated.status).toBe('disabled')
    expect(updated.fondsNo).toBe('F800')
  })

  it('removeFonds removes unlinked fonds and rejects linked', async () => {
    const { createFonds, removeFonds, getFonds } = await import('./fonds')
    const empty = await createFonds({ fondsNo: 'F700', fondsName: '空全宗', organizationId: 1 })
    const before = (await getFonds()).records.length
    await removeFonds(empty.id)
    const after = await getFonds()
    expect(after.records.length).toBe(before - 1)
    expect(after.records.find((f) => f.id === empty.id)).toBeUndefined()
    // F018 archiveCount>0 有关联数据，不可删除
    await expect(removeFonds(18)).rejects.toThrow()
  })
})
