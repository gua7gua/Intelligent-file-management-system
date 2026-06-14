import { describe, expect, it } from 'vitest'
import {
  confirmDestruction,
  getDestructionListDetail,
  getDestructionLists,
  submitDestructionApproval,
  uploadDestructionPhotos,
} from './destruction'

describe('destruction api mock mode', () => {
  it('lists destruction lists and filters by status', async () => {
    const all = await getDestructionLists()
    const statuses = all.records.map((l) => l.status)
    expect(statuses).toContain('draft')
    expect(statuses).toContain('pending_approval')
    expect(statuses).toContain('pending_destroy')
    expect(statuses).toContain('destroyed')

    const drafts = await getDestructionLists({ status: 'draft' })
    expect(drafts.records.every((l) => l.status === 'draft')).toBe(true)
  })

  it('list detail includes snapshot items with file delete status', async () => {
    const detail = await getDestructionListDetail(10)
    expect(detail.items.length).toBeGreaterThan(0)
    expect(detail.items[0].archiveNoSnapshot).toBeTruthy()
    expect(detail.approval).toBeTruthy()
    expect(detail.approval?.status).toBe('approved')
  })

  it('throws for unknown list id', async () => {
    await expect(getDestructionListDetail(99999)).rejects.toThrow()
  })

  it('submit approval returns a pending destruction approval', async () => {
    const approval = await submitDestructionApproval(13, { reason: '到期销毁' })
    expect(approval.approvalType).toBe('destruction')
    expect(approval.status).toBe('pending')
    expect(approval.targetId).toBe(13)
  })

  it('uploads photos and returns attachment list', async () => {
    const file = new File(['x'], 'scene.jpg', { type: 'image/jpeg' })
    const photos = await uploadDestructionPhotos(10, [file])
    expect(photos.length).toBe(1)
    expect(photos[0].fileName).toBe('scene.jpg')
  })

  it('confirm destruction moves list to destroyed and deletes files', async () => {
    const detail = await confirmDestruction(10, {
      destroyMethod: 'shredding',
      supervisorName1: '刘星',
      supervisorName2: '向加明',
      destroyNote: '现场粉碎',
    })
    expect(detail.status).toBe('destroyed')
    expect(detail.items.every((i) => i.fileDeleteStatus === 'deleted')).toBe(true)
  })
})
