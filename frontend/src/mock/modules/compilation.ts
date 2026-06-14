import type { PageData } from '@/types/api'
import type {
  Compilation, CompilationArchiveData, CompilationDetail, CompilationMaterial,
  CompilationParams, CompilationWriteData, MaterialCandidate, MaterialSearchParams,
} from '@/types/compilation'

function mkComp(over: Partial<CompilationDetail> & Pick<CompilationDetail, 'id' | 'compilationNo' | 'title' | 'status'>): CompilationDetail {
  return {
    compilationType: '专题汇编', createdBy: '郭一坤', materialCount: 0,
    createdAt: '2026-06-10T09:00:00+08:00', updatedAt: '2026-06-10T09:00:00+08:00',
    materials: [], ...over,
  }
}

const comps: CompilationDetail[] = [
  mkComp({
    id: 1, compilationNo: 'BY-202606-001', title: '苏州财政改革专题编研', status: 'draft',
    compilationType: '专题汇编', dateRangeText: '2014-2025', keywords: '财政、预算、会计档案',
    summary: '围绕财政改革主题形成的编研成果', contentHtml: '<h3>正文草稿</h3><p>…</p>',
    materialCount: 6, createdAt: '2026-06-14T09:00:00+08:00', updatedAt: '2026-06-16T15:00:00+08:00',
    materials: [
      { id: 11, compilationId: 1, archiveId: 201, archiveNo: 'KJ-2024-0201', title: '采油三厂设备验收报告' },
      { id: 12, compilationId: 1, archiveId: 202, archiveNo: 'KJ-2024-0202', title: '管线改造竣工图' },
    ],
  }),
  mkComp({
    id: 2, compilationNo: 'BY-202605-002', title: '克拉玛依工业园建设大事记', status: 'generated',
    compilationType: '大事记', dateRangeText: '2010-2025', keywords: '工业园、建设',
    summary: '工业园建设历程大事记', contentHtml: '<h3>正文</h3><p>…</p>',
    materialCount: 8, createdAt: '2026-05-20T09:00:00+08:00', updatedAt: '2026-06-12T10:00:00+08:00',
    attachment: { id: 901, fileName: '克拉玛依工业园建设大事记.html', fileUrl: '/files/by-002.html', attachmentType: 'report', generatedAt: '2026-06-12T10:00:00+08:00' },
  }),
  mkComp({
    id: 3, compilationNo: 'BY-202604-003', title: '交通局组织史参考资料', status: 'archived',
    compilationType: '组织史', dateRangeText: '2000-2024', keywords: '组织史、机构沿革',
    summary: '交通局机构沿革与组织史', contentHtml: '<h3>正文</h3><p>…</p>',
    materialCount: 12, createdAt: '2026-04-10T09:00:00+08:00', updatedAt: '2026-04-20T16:00:00+08:00',
    attachment: { id: 902, fileName: '交通局组织史参考资料.pdf', fileUrl: '/files/by-003.pdf', attachmentType: 'report', generatedAt: '2026-04-18T10:00:00+08:00' },
    archiveNo: 'AJ-compile-2026-0001', archiveId: 9001,
  }),
]

const materials: MaterialCandidate[] = [
  { id: 201, archiveNo: 'KJ-2024-0201', title: '采油三厂设备验收报告', categoryName: '科技档案', formedYear: 2024, tags: ['设备', '验收'] },
  { id: 202, archiveNo: 'KJ-2024-0202', title: '管线改造竣工图', categoryName: '科技档案', formedYear: 2024, tags: ['管线', '竣工'] },
  { id: 203, archiveNo: 'KJ-2023-0203', title: '年度审计材料汇编', categoryName: '科技档案', formedYear: 2023, tags: ['审计'] },
  { id: 301, archiveNo: 'WS-2024-0301', title: '党委会议纪要', categoryName: '文书档案', formedYear: 2024, tags: ['会议'] },
  { id: 401, archiveNo: 'KJ-2024-0401', title: '会计凭证汇总', categoryName: '会计档案', formedYear: 2024, tags: ['凭证'] },
  { id: 501, archiveNo: 'KJ-2022-0501', title: '市政管网规划', categoryName: '科技档案', formedYear: 2022, tags: ['规划'] },
  { id: 502, archiveNo: 'KJ-2024-0502', title: '供电改造批复', categoryName: '科技档案', formedYear: 2024, tags: ['批复'] },
  { id: 503, archiveNo: 'KJ-2025-0503', title: '环保验收报告', categoryName: '科技档案', formedYear: 2025, tags: ['环保'] },
]

function page<T>(records: T[], pageNo: number, pageSize: number): PageData<T> {
  const start = (pageNo - 1) * pageSize
  return { records: records.slice(start, start + pageSize), pageNo, pageSize, total: records.length, hasNext: start + pageSize < records.length }
}

export function mockCompilations(params?: CompilationParams): PageData<Compilation> {
  let list = comps.slice()
  if (params?.status) list = list.filter((c) => c.status === params.status)
  if (params?.keyword) {
    const kw = params.keyword
    list = list.filter((c) => c.title.includes(kw) || c.compilationNo.includes(kw))
  }
  const pageNo = params?.pageNo ?? 1
  const pageSize = params?.pageSize ?? 20
  const records = list.map(({ materials, ...rest }) => rest)
  return page(records, pageNo, pageSize)
}

export function mockCompilationDetail(id: number): CompilationDetail {
  const found = comps.find((c) => c.id === id)
  if (!found) throw new Error('编研成果不存在')
  return JSON.parse(JSON.stringify(found))
}

let nextId = 4
export function mockCreateCompilation(data: CompilationWriteData): CompilationDetail {
  const id = nextId++
  const detail = mkComp({
    id, compilationNo: `BY-202606-${String(id).padStart(3, '0')}`, title: data.title, status: 'draft',
    compilationType: data.compilationType, dateRangeText: data.dateRangeText, keywords: data.keywords,
    summary: data.summary, contentHtml: data.contentHtml ?? '', materialCount: data.materialArchiveIds.length,
    createdAt: '2026-06-17T09:00:00+08:00', updatedAt: '2026-06-17T09:00:00+08:00',
    materials: data.materialArchiveIds.map((aid, i) => ({ id: id * 100 + i, compilationId: id, archiveId: aid, archiveNo: `KJ-2024-${String(aid).padStart(4, '0')}`, title: `素材 ${aid}` } as CompilationMaterial)),
  })
  comps.unshift(detail)
  return JSON.parse(JSON.stringify(detail))
}

export function mockUpdateCompilation(id: number, data: CompilationWriteData): CompilationDetail {
  const c = comps.find((x) => x.id === id)
  if (!c) throw new Error('编研成果不存在')
  if (c.status === 'archived') throw new Error('已入库成果只读，不可编辑')
  c.title = data.title
  c.compilationType = data.compilationType
  c.dateRangeText = data.dateRangeText
  c.keywords = data.keywords
  c.summary = data.summary
  c.contentHtml = data.contentHtml ?? c.contentHtml
  c.materialCount = data.materialArchiveIds.length
  c.updatedAt = '2026-06-17T10:00:00+08:00'
  return JSON.parse(JSON.stringify(c))
}

export function mockGenerateCompilationBody(id: number): CompilationDetail {
  const c = comps.find((x) => x.id === id)
  if (!c) throw new Error('编研成果不存在')
  if (c.status === 'archived') throw new Error('已入库成果不可重新生成正文')
  c.status = 'generated'
  c.attachment = { id: 900 + id, fileName: `${c.title}.html`, fileUrl: `/files/by-${id}.html`, attachmentType: 'report', generatedAt: '2026-06-17T11:00:00+08:00' }
  c.updatedAt = '2026-06-17T11:00:00+08:00'
  return JSON.parse(JSON.stringify(c))
}

let archiveSeq = 2
export function mockArchiveCompilation(id: number, _data: CompilationArchiveData): CompilationDetail {
  const c = comps.find((x) => x.id === id)
  if (!c) throw new Error('编研成果不存在')
  if (c.status !== 'generated') throw new Error('须先生成正文文件再入库')
  if (!c.attachment) throw new Error('正文文件未生成，不可入库')
  if (!c.title.trim()) throw new Error('标题必填')
  if (!c.summary?.trim()) throw new Error('摘要必填')
  if (c.materialCount === 0) throw new Error('请添加至少一个素材引用')
  c.status = 'archived'
  c.archiveNo = `AJ-compile-2026-${String(archiveSeq++).padStart(4, '0')}`
  c.archiveId = 9000 + id
  c.updatedAt = '2026-06-17T14:00:00+08:00'
  return JSON.parse(JSON.stringify(c))
}

export function mockSearchMaterials(params?: MaterialSearchParams): PageData<MaterialCandidate> {
  let list = materials.slice()
  if (params?.keyword) {
    const kw = params.keyword
    list = list.filter((m) => m.archiveNo.includes(kw) || m.title.includes(kw))
  }
  if (params?.year) list = list.filter((m) => m.formedYear === params.year)
  if (params?.tag) list = list.filter((m) => m.tags.includes(params.tag!))
  return page(list, params?.pageNo ?? 1, params?.pageSize ?? 20)
}
