import type { FondsCreate, FondsItem, FondsParams, FondsUpdate } from '@/types/fonds'
import type { PageData } from '@/types/api'

const fondsList: FondsItem[] = [
  {
    id: 1, fondsNo: 'F001', fondsName: '克拉玛依市档案馆综合全宗', organizationId: 1, organizationName: '克拉玛依市档案馆',
    description: '本馆综合全宗', archiveCount: 312, boxCount: 34, status: 'active',
    categoryDistribution: [{ category: '文书档案', count: 146 }, { category: '科技档案', count: 88 }, { category: '会计档案', count: 54 }, { category: '音像档案', count: 24 }],
    recentIntake: [{ date: '2026-05-28', title: '2025 年度公开工作报告' }, { date: '2026-05-12', title: '内部机构调整文件' }],
    createdAt: '2025-09-12T10:30:00+08:00', updatedAt: '2026-05-28T09:00:00+08:00', createdBy: 1, updatedBy: 1,
  },
  {
    id: 18, fondsNo: 'F018', fondsName: '市财政局财务业务全宗', organizationId: 2, organizationName: '市财政局',
    description: '财务业务档案', archiveCount: 428, boxCount: 47, status: 'active',
    categoryDistribution: [{ category: '会计档案', count: 301 }, { category: '文书档案', count: 92 }, { category: '科技档案', count: 35 }],
    recentIntake: [{ date: '2026-06-02', title: '2025 年度会计凭证' }],
    createdAt: '2025-10-02T14:00:00+08:00', updatedAt: '2026-06-02T11:00:00+08:00', createdBy: 1, updatedBy: 2,
  },
  {
    id: 31, fondsNo: 'F031', fondsName: '市交通局科技项目全宗', organizationId: 3, organizationName: '市交通局',
    description: '科技项目档案', archiveCount: 106, boxCount: 11, status: 'active',
    categoryDistribution: [{ category: '科技档案', count: 64 }, { category: '文书档案', count: 38 }, { category: '音像档案', count: 4 }],
    recentIntake: [{ date: '2026-05-21', title: '老旧小区改造项目资料' }],
    createdAt: '2025-11-15T09:30:00+08:00', updatedAt: '2026-05-21T16:00:00+08:00', createdBy: 1, updatedBy: 3,
  },
  {
    id: 42, fondsNo: 'F042', fondsName: '城投建设集团工程全宗', organizationId: 5, organizationName: '城投建设集团',
    description: '工程项目档案（当前停用）', archiveCount: 0, boxCount: 0, status: 'disabled',
    categoryDistribution: [], recentIntake: [],
    createdAt: '2025-12-01T08:00:00+08:00', updatedAt: '2026-03-10T10:00:00+08:00', createdBy: 1, updatedBy: 1,
  },
]

let seq = 100

export function mockFonds(params?: FondsParams): PageData<FondsItem> {
  let list = fondsList
  if (params?.status) list = list.filter((f) => f.status === params.status)
  if (params?.organizationId) list = list.filter((f) => f.organizationId === params.organizationId)
  if (params?.keyword) {
    const kw = params.keyword.trim().toLowerCase()
    list = list.filter((f) => f.fondsName.toLowerCase().includes(kw) || f.fondsNo.toLowerCase().includes(kw) || (f.organizationName ?? '').toLowerCase().includes(kw))
  }
  if (params?.relation) {
    list = list.filter((f) => {
      const linked = f.archiveCount > 0 || f.boxCount > 0
      if (params.relation === 'linked') return linked
      if (params.relation === 'empty') return !linked
      if (params.relation === 'disabled') return f.status === 'disabled'
      return true
    })
  }
  const pageNo = params?.pageNo ?? 1
  const pageSize = params?.pageSize ?? 50
  const total = list.length
  const start = (pageNo - 1) * pageSize
  return { records: list.slice(start, start + pageSize), pageNo, pageSize, total, hasNext: start + pageSize < total }
}

export function mockCreateFonds(body: FondsCreate): FondsItem {
  if (fondsList.some((f) => f.fondsNo === body.fondsNo.trim())) {
    throw new Error('全宗号已存在：' + body.fondsNo)
  }
  const item: FondsItem = {
    id: ++seq,
    fondsNo: body.fondsNo.trim(),
    fondsName: body.fondsName.trim(),
    organizationId: body.organizationId,
    description: body.description?.trim() ?? '',
    archiveCount: 0,
    boxCount: 0,
    status: 'active',
    categoryDistribution: [],
    recentIntake: [],
    createdAt: '2026-06-15T10:00:00+08:00',
    updatedAt: '2026-06-15T10:00:00+08:00',
    createdBy: 1,
    updatedBy: 1,
  }
  fondsList.push(item)
  return { ...item }
}

export function mockUpdateFonds(id: number, body: FondsUpdate): FondsItem {
  const item = fondsList.find((f) => f.id === id)
  if (!item) throw new Error('全宗不存在：' + id)
  if (body.fondsName !== undefined) item.fondsName = body.fondsName.trim()
  if (body.organizationId !== undefined) item.organizationId = body.organizationId
  if (body.description !== undefined) item.description = body.description.trim()
  if (body.status !== undefined) item.status = body.status
  item.updatedAt = '2026-06-15T10:30:00+08:00'
  return { ...item }
}

export function mockRemoveFonds(id: number): void {
  const idx = fondsList.findIndex((f) => f.id === id)
  if (idx === -1) throw new Error('全宗不存在：' + id)
  const item = fondsList[idx]
  if (item.archiveCount > 0 || item.boxCount > 0) {
    throw new Error('该全宗已有归档档案或档案盒，只能停用，不能删除')
  }
  fondsList.splice(idx, 1)
}
