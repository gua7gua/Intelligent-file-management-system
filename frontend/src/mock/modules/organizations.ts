import type { Organization, OrganizationCreate, OrganizationParams, OrganizationUpdate } from '@/types/organization'
import type { PageData } from '@/types/api'

const organizations: Organization[] = [
  { id: 1, orgName: '克拉玛依市档案馆', orgType: 'archive_org', contactName: '李馆长', contactPhone: '0990-6800001', status: 'active', fondsCount: 3, userCount: 5 },
  { id: 2, orgName: '市财政局', orgType: 'government', contactName: '王主任', contactPhone: '0990-6800002', status: 'active', fondsCount: 1, userCount: 2 },
  { id: 3, orgName: '市交通局', orgType: 'government', contactName: '陈科长', contactPhone: '0990-6800003', status: 'active', fondsCount: 0, userCount: 0 },
  { id: 4, orgName: '市科技研究中心', orgType: 'public_institution', contactName: '王工', contactPhone: '0990-6800004', status: 'active', fondsCount: 0, userCount: 1 },
  { id: 5, orgName: '城投建设集团', orgType: 'enterprise', contactName: '刘经理', contactPhone: '0990-6800005', status: 'disabled', fondsCount: 0, userCount: 0 },
]

let orgSeq = 100

export function mockOrganizations(params?: OrganizationParams): PageData<Organization> {
  let list = organizations
  if (params?.status) list = list.filter((o) => o.status === params.status)
  if (params?.orgType) list = list.filter((o) => o.orgType === params.orgType)
  if (params?.keyword) {
    const kw = params.keyword.trim().toLowerCase()
    list = list.filter((o) => o.orgName.toLowerCase().includes(kw))
  }
  const pageNo = params?.pageNo ?? 1
  const pageSize = params?.pageSize ?? 50
  const total = list.length
  const start = (pageNo - 1) * pageSize
  return { records: list.slice(start, start + pageSize), pageNo, pageSize, total, hasNext: start + pageSize < total }
}

export function mockCreateOrganization(body: OrganizationCreate): Organization {
  if (organizations.some((o) => o.orgName === body.orgName.trim())) {
    throw new Error('组织名称已存在：' + body.orgName)
  }
  const org: Organization = {
    id: ++orgSeq,
    orgName: body.orgName.trim(),
    orgType: body.orgType,
    contactName: body.contactName?.trim() || undefined,
    contactPhone: body.contactPhone?.trim() || undefined,
    status: 'active',
    fondsCount: 0,
    userCount: 0,
  }
  organizations.push(org)
  return { ...org }
}

export function mockDeleteOrganization(id: number): void {
  const idx = organizations.findIndex((o) => o.id === id)
  if (idx < 0) throw new Error('组织不存在：' + id)
  const org = organizations[idx]
  if ((org.fondsCount ?? 0) > 0 || (org.userCount ?? 0) > 0) {
    throw new Error('存在关联全宗或用户，无法删除，请改用停用')
  }
  organizations.splice(idx, 1)
}

export function mockUpdateOrganization(id: number, body: OrganizationUpdate): Organization {
  const org = organizations.find((o) => o.id === id)
  if (!org) throw new Error('组织不存在：' + id)
  if (body.orgName !== undefined) {
    const name = body.orgName.trim()
    if (organizations.some((o) => o.id !== id && o.orgName === name)) {
      throw new Error('组织名称已存在：' + body.orgName)
    }
    org.orgName = name
  }
  if (body.orgType !== undefined) org.orgType = body.orgType
  if (body.contactName !== undefined) org.contactName = body.contactName?.trim() || undefined
  if (body.contactPhone !== undefined) org.contactPhone = body.contactPhone?.trim() || undefined
  if (body.status !== undefined) org.status = body.status
  return { ...org }
}
