import { describe, expect, it } from 'vitest'
import { approveApproval, getApprovalDetail, getApprovals, rejectApproval } from './approval'

describe('approval api mock mode', () => {
  it('lists approvals covering three types and statuses', async () => {
    const all = await getApprovals()
    const types = all.records.map((a) => a.approvalType)
    expect(types).toContain('security_adjust')
    expect(types).toContain('open_adjust')
    expect(types).toContain('destruction')
    const statuses = all.records.map((a) => a.status)
    expect(statuses).toContain('pending')
    expect(statuses).toContain('approved')
    expect(statuses).toContain('rejected')
  })

  it('filters by approval type', async () => {
    const page = await getApprovals({ approvalType: 'destruction' })
    expect(page.records.every((a) => a.approvalType === 'destruction')).toBe(true)
  })

  it('security adjust detail includes target and evidence archives', async () => {
    const detail = await getApprovalDetail(20)
    expect(detail.approvalType).toBe('security_adjust')
    expect(detail.targetArchive).toBeTruthy()
    expect(detail.evidenceArchive).toBeTruthy()
    expect(detail.evidenceMatched).toBe(true)
  })

  it('open adjust with unmatched evidence is flagged', async () => {
    const detail = await getApprovalDetail(21)
    expect(detail.evidenceMatched).toBe(false)
  })

  it('destruction approval detail includes list snapshot', async () => {
    const detail = await getApprovalDetail(22)
    expect(detail.destructionList).toBeTruthy()
    expect(detail.destructionList!.items.length).toBeGreaterThan(0)
  })

  it('throws for unknown approval id', async () => {
    await expect(getApprovalDetail(99999)).rejects.toThrow()
  })

  it('approve sets status approved with opinion', async () => {
    const detail = await approveApproval(20, { opinion: '同意' })
    expect(detail.status).toBe('approved')
    expect(detail.approvalOpinion).toBe('同意')
  })

  it('reject sets status rejected with opinion', async () => {
    const detail = await rejectApproval(21, { opinion: '凭证不匹配，退回' })
    expect(detail.status).toBe('rejected')
    expect(detail.approvalOpinion).toBe('凭证不匹配，退回')
  })
})
