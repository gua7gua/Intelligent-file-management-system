import { describe, expect, it } from 'vitest'
import { getOrganizations } from './organizations'

describe('organizations api mock mode', () => {
  it('returns organization list', async () => {
    const res = await getOrganizations()
    expect(res.records.length).toBeGreaterThan(0)
    expect(res.records[0]).toHaveProperty('orgName')
    expect(res.records[0]).toHaveProperty('orgType')
  })

  it('filters by status', async () => {
    const res = await getOrganizations({ status: 'active' })
    expect(res.records.every((o) => o.status === 'active')).toBe(true)
  })

  it('filters by keyword', async () => {
    const res = await getOrganizations({ keyword: '财政' })
    expect(res.records.every((o) => o.orgName.includes('财政'))).toBe(true)
  })
})
