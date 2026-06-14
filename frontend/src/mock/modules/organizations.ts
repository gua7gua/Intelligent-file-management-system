import type { Organization, OrganizationParams } from '@/types/organization'
import type { PageData } from '@/types/api'

const organizations: Organization[] = [
  { id: 1, orgName: '克拉玛依市档案馆', orgType: 'archive_org', contactName: '李馆长', contactPhone: '0990-6800001', status: 'active' },
  { id: 2, orgName: '市财政局', orgType: 'government', contactName: '王主任', contactPhone: '0990-6800002', status: 'active' },
  { id: 3, orgName: '市交通局', orgType: 'government', contactName: '陈科长', contactPhone: '0990-6800003', status: 'active' },
  { id: 4, orgName: '市科技研究中心', orgType: 'public_institution', contactName: '王工', contactPhone: '0990-6800004', status: 'active' },
  { id: 5, orgName: '城投建设集团', orgType: 'enterprise', contactName: '刘经理', contactPhone: '0990-6800005', status: 'disabled' },
]

export function mockOrganizations(params?: OrganizationParams): PageData<Organization> {
  let list = organizations
  if (params?.status) list = list.filter((o) => o.status === params.status)
  if (params?.keyword) {
    const kw = params.keyword.trim().toLowerCase()
    list = list.filter((o) => o.orgName.toLowerCase().includes(kw))
  }
  const pageNo = params?.pageNo ?? 1
  const pageSize = params?.pageSize ?? 50
  const total = list.length
  const start = (pageNo - 1) * pageSize
  return {
    records: list.slice(start, start + pageSize),
    pageNo,
    pageSize,
    total,
    hasNext: start + pageSize < total,
  }
}
