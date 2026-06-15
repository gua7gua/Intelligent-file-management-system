import { describe, expect, it } from 'vitest'
import {
  archiveCompilation, createCompilation, generateCompilationBody, getCompilations,
  searchMaterials, updateCompilation,
} from './compilation'

describe('compilation api mock mode', () => {
  it('lists compilations across statuses', async () => {
    const all = await getCompilations({ pageSize: 50 })
    const statuses = all.records.map((c) => c.status)
    expect(statuses).toContain('draft')
    expect(statuses).toContain('generated')
    expect(statuses).toContain('archived')
  })

  it('create draft and update', async () => {
    const created = await createCompilation({
      title: '测试编研', compilationType: '专题汇编', summary: '摘要',
      contentHtml: '<p>正文</p>', materialArchiveIds: [1, 2],
    })
    expect(created.status).toBe('draft')
    expect(created.materialCount).toBe(2)
    const updated = await updateCompilation(created.id, { title: '测试编研改', compilationType: '专题汇编', materialArchiveIds: [1, 2, 3] })
    expect(updated.title).toBe('测试编研改')
    expect(updated.materialCount).toBe(3)
  })

  it('generate body moves draft to generated', async () => {
    const created = await createCompilation({ title: 't', compilationType: '专题汇编', materialArchiveIds: [1] })
    const generated = await generateCompilationBody(created.id)
    expect(generated.status).toBe('generated')
    expect(generated.attachment?.attachmentType).toBe('report')
  })

  it('archive validates body and material then generates archiveNo', async () => {
    const created = await createCompilation({ title: 't', compilationType: '专题汇编', summary: 's', materialArchiveIds: [1] })
    await expect(archiveCompilation(created.id, { fondsId: 1, categoryId: 1, formedDate: '2026-06-17', retentionPeriod: 'permanent', openStatus: 'open' })).rejects.toThrow()
    await generateCompilationBody(created.id)
    const archived = await archiveCompilation(created.id, { fondsId: 1, categoryId: 1, formedDate: '2026-06-17', retentionPeriod: 'permanent', openStatus: 'open' })
    expect(archived.status).toBe('archived')
    expect(archived.archiveNo).toBeTruthy()
  })

  it('archived is readonly', async () => {
    await expect(updateCompilation(3, { title: 'x', compilationType: '专题汇编', materialArchiveIds: [] })).rejects.toThrow()
  })

  it('search materials returns candidates', async () => {
    const page = await searchMaterials({ pageSize: 50 })
    expect(page.records.length).toBeGreaterThan(0)
    expect(page.records[0].archiveNo).toBeTruthy()
  })
})
