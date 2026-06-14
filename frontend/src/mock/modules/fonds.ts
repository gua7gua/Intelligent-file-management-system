import type { FondsParams, FondsReference } from '@/types/fonds'
import type { PageData } from '@/types/api'

const fondsList: FondsReference[] = [
  { id: 1, fondsNo: 'F001', fondsName: '克拉玛依市档案馆综合全宗', organizationId: 1, organizationName: '克拉玛依市档案馆', description: '本馆综合全宗', status: 'active' },
  { id: 18, fondsNo: 'F018', fondsName: '市财政局财务业务全宗', organizationId: 2, organizationName: '市财政局', description: '财务业务档案', status: 'active' },
  { id: 31, fondsNo: 'F031', fondsName: '市交通局科技项目全宗', organizationId: 3, organizationName: '市交通局', description: '科技项目档案', status: 'active' },
  { id: 42, fondsNo: 'F042', fondsName: '城投建设集团工程全宗', organizationId: 5, organizationName: '城投建设集团', description: '工程项目档案', status: 'disabled' },
]

export function mockFonds(params?: FondsParams): PageData<FondsReference> {
  let list = fondsList
  if (params?.status) list = list.filter((f) => f.status === params.status)
  if (params?.keyword) {
    const kw = params.keyword.trim().toLowerCase()
    list = list.filter((f) => f.fondsName.toLowerCase().includes(kw) || f.fondsNo.toLowerCase().includes(kw))
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
