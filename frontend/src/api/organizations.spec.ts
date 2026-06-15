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

  it('createOrganization returns new active org with id', async () => {
    const { createOrganization } = await import('./organizations')
    const org = await createOrganization({ orgName: '市水利局', orgType: 'government', contactName: '赵主任', contactPhone: '0990-6800009' })
    expect(org.orgName).toBe('市水利局')
    expect(org.orgType).toBe('government')
    expect(org.status).toBe('active')
    expect(typeof org.id).toBe('number')
  })

  it('createOrganization rejects duplicate orgName', async () => {
    const { createOrganization, getOrganizations } = await import('./organizations')
    const first = await getOrganizations()
    const dup = first.records[0].orgName
    await expect(createOrganization({ orgName: dup, orgType: 'government' })).rejects.toThrow()
  })
})
