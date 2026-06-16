import type { PageData, PageParams } from '@/types/api'
import type {
  PublicAiQueryRequest,
  PublicAiQueryResult,
  PublicArchiveDetail,
  PublicArchivePage,
  PublicCollectionBatch,
  PublicHomeData,
  PublicOverviewData,
  PublicRegisterRequest,
  PublicResetPasswordRequest,
  PublicSmsCodeRequest,
} from '@/types/public'
import { mockDictionaries } from './dictionary'

// 档案门类 categoryId(整数) -> 门类名 的映射，供公开检索 mock 过滤使用
const categoryLabelById = new Map<number, string>(
  (mockDictionaries.categories ?? []).map((c) => [Number(c.value), String(c.label)]),
)

// ——————————————————————————————————
// 模拟数据
// ——————————————————————————————————

const archive1: PublicArchiveDetail = {
  id: 1,
  archiveNo: 'A-2024-0001',
  title: '2024 年度城市老旧小区改造工程档案',
  responsible: '克拉玛依市住建局',
  category: '科技档案',
  formedYear: 2024,
  carrierStatus: 'paper_electronic',
  sourceType: 'transfer',
  tags: ['城市建设', '老旧小区改造'],
  hasElectronicFile: true,
  canPreview: true,
  canDownload: true,
  formedDate: '2024-12-31',
  retentionPeriod: 'permanent',
  openStatus: 'open',
  summary: '本档案包含 2024 年度克拉玛依市老旧小区综合改造项目的全部工程资料，包括项目立项报告、施工方案、竣工验收文件等。',
  files: [
    {
      id: 1001,
      filename: '2024-renovation-project-full.pdf',
      fileFormat: 'PDF',
      fileSize: 15_480_000,
      canPreview: true,
      canDownload: true,
    },
    {
      id: 1002,
      filename: '2024-renovation-blueprint.dwg',
      fileFormat: 'DWG',
      fileSize: 8_200_000,
      canPreview: false,
      canDownload: true,
    },
  ],
}

const archive2: PublicArchiveDetail = {
  id: 2,
  archiveNo: 'A-2025-0012',
  title: '2025 年度教育工作总结',
  responsible: '克拉玛依市教育局',
  category: '文书档案',
  formedYear: 2025,
  carrierStatus: 'paper_electronic',
  sourceType: 'transfer',
  tags: ['教育', '工作总结'],
  hasElectronicFile: true,
  canPreview: false,
  canDownload: false,
  formedDate: '2025-12-31',
  retentionPeriod: 'permanent',
  openStatus: 'open',
  summary: '2025 年度克拉玛依市教育工作总结，包含各级学校发展情况、师资建设、教学改革等内容。',
  files: [],
}

const archive3: PublicArchiveDetail = {
  id: 3,
  archiveNo: 'A-2025-0008',
  title: '2025 年度招生计划文件',
  responsible: '克拉玛依市教育局',
  category: '文书档案',
  formedYear: 2025,
  carrierStatus: 'paper',
  sourceType: 'transfer',
  tags: ['教育', '招生'],
  hasElectronicFile: false,
  canPreview: false,
  canDownload: false,
  formedDate: '2025-06-15',
  retentionPeriod: '30y',
  openStatus: 'open',
  summary: '2025 年度克拉玛依市各学校招生计划及相关政策文件。仅纸质版，暂无电子文件。',
  files: [],
}

const archive4: PublicArchiveDetail = {
  id: 4,
  archiveNo: 'A-2024-0005',
  title: '克拉玛依老城改造影像资料集',
  responsible: '克拉玛依市档案馆',
  category: '音像档案',
  formedYear: 2023,
  carrierStatus: 'electronic',
  sourceType: 'collection',
  tags: ['老城改造', '影像资料', '历史'],
  hasElectronicFile: true,
  canPreview: true,
  canDownload: true,
  formedDate: '2023-10-15',
  retentionPeriod: 'permanent',
  openStatus: 'open',
  summary: '由市民捐赠的克拉玛依老城区改造前后对比影像资料，包含照片、视频等电子文件。',
  files: [
    {
      id: 4001,
      filename: 'old-city-before-2000.jpg',
      fileFormat: 'JPG',
      fileSize: 3_500_000,
      canPreview: true,
      canDownload: true,
    },
    {
      id: 4002,
      filename: 'old-city-after-2023.jpg',
      fileFormat: 'JPG',
      fileSize: 4_100_000,
      canPreview: true,
      canDownload: true,
    },
    {
      id: 4003,
      filename: 'old-city-renovation-timeline.mp4',
      fileFormat: 'MP4',
      fileSize: 120_000_000,
      canPreview: true,
      canDownload: false,
    },
  ],
}

// ——————————————————————————————————
// 导出
// ——————————————————————————————————

export const mockPublicHome: PublicHomeData = {
  stats: {
    openArchiveCount: 128,
    electronicFileCount: 56,
    collectionCount: 12,
    latestOpenCount: 8,
  },
  categories: [
    { name: '城市建设', count: 35 },
    { name: '教育', count: 28 },
    { name: '历史文化', count: 22 },
    { name: '交通', count: 18 },
    { name: '财政', count: 15 },
    { name: '其他', count: 10 },
  ],
  recentArchives: [archive4, archive1, archive2],
}

export const mockPublicArchivePage: PublicArchivePage = {
  records: [archive4, archive1, archive2, archive3].map((a) => ({
    ...a,
    openStatus: 'open' as const,
  })),
  pageNo: 1,
  pageSize: 20,
  total: 4,
  hasNext: false,
}

export const mockPublicArchiveDetails: Record<number, PublicArchiveDetail> = {
  1: archive1,
  2: archive2,
  3: archive3,
  4: archive4,
}

export const mockPublicOverview: PublicOverviewData = {
  collectionSummary: {
    total: 1,
    draft: 0,
    inProgress: 1,
    completed: 0,
  },
  recentCollections: [
    {
      id: 1,
      batchNo: 'CL-2026-0001',
      title: '家族老照片捐赠',
      contactName: '张三',
      contactPhone: '13800000005',
      archiveYear: 1980,
      status: 'pending_contact',
      statusText: '待联系',
      submittedAt: '2026-06-01T10:00:00+08:00',
      scheduledReceiveAt: '2026-06-15T14:00:00+08:00',
      itemCount: 3,
      agreementAcceptedAt: '2026-06-01T10:00:00+08:00',
      items: [
        {
          id: 1,
          seqNo: 1,
          inputTitle: '1980 年代克拉玛依市中心全景',
          carrierStatus: 'electronic',
          electronicFormat: 'JPG',
          expectedFilename: '1980-panorama.jpg',
          status: 'draft',
          localFileSize: 2500000,
        },
        {
          id: 2,
          seqNo: 2,
          inputTitle: '1995 年家属院合影',
          carrierStatus: 'paper',
          status: 'draft',
        },
        {
          id: 3,
          seqNo: 3,
          inputTitle: '2000 年市区街景录像',
          carrierStatus: 'electronic',
          electronicFormat: 'MP4',
          expectedFilename: '2000-street-video.mp4',
          status: 'draft',
          localFileSize: 50000000,
        },
      ],
    },
  ],
  downloadLogs: [
    { id: 1, archiveId: 1, accessType: 'download', accessedAt: '2026-05-20T09:30:00+08:00' },
    { id: 2, archiveId: 4, accessType: 'download', accessedAt: '2026-05-18T14:00:00+08:00' },
    { id: 3, archiveId: 2, accessType: 'download', accessedAt: '2026-04-10T11:00:00+08:00' },
  ],
  summarizedAt: '2026-06-16T08:30:00+08:00',
}

export const mockMyCollections: PageData<PublicCollectionBatch> = {
  records: mockPublicOverview.recentCollections,
  pageNo: 1,
  pageSize: 20,
  total: mockPublicOverview.recentCollections.length,
  hasNext: false,
}

// ——————————————————————————————————
// Mock 函数
// ——————————————————————————————————

export function mockGetPublicHome(): Promise<PublicHomeData> {
  return Promise.resolve(mockPublicHome)
}

export function mockSearchPublicArchives(params?: import('@/types/public').PublicSearchParams): Promise<PublicArchivePage> {
  let records = mockPublicArchivePage.records

  if (params?.keyword) {
    const kw = params.keyword.toLowerCase()
    records = records.filter(
      (r) =>
        r.title.toLowerCase().includes(kw) ||
        r.archiveNo.toLowerCase().includes(kw) ||
        r.responsible.toLowerCase().includes(kw) ||
        r.tags.some((t) => t.toLowerCase().includes(kw)),
    )
  }
  if (params?.archiveNo) {
    const v = params.archiveNo.toLowerCase()
    records = records.filter((r) => r.archiveNo.toLowerCase().includes(v))
  }
  if (params?.title) {
    const v = params.title.toLowerCase()
    records = records.filter((r) => r.title.toLowerCase().includes(v))
  }
  if (params?.responsibleText) {
    const v = params.responsibleText.toLowerCase()
    records = records.filter((r) => r.responsible.toLowerCase().includes(v))
  }
  if (params?.tagIds) {
    const tags = params.tagIds
      .split(',')
      .map((t) => t.trim().toLowerCase())
      .filter(Boolean)
    if (tags.length > 0) {
      records = records.filter((r) => tags.some((t) => r.tags.some((rt) => rt.toLowerCase().includes(t))))
    }
  }
  if (params?.categoryId !== undefined && params.categoryId !== null) {
    // 前端按门类名匹配（mock 档案以门类名作为 category 字段）
    const label = categoryLabelById.get(params.categoryId)
    records = label ? records.filter((r) => r.category === label) : records
  }
  if (params?.formedYearStart) {
    records = records.filter((r) => r.formedYear >= params.formedYearStart!)
  }
  if (params?.formedYearEnd) {
    records = records.filter((r) => r.formedYear <= params.formedYearEnd!)
  }
  if (params?.carrierStatus) {
    records = records.filter((r) => r.carrierStatus === params.carrierStatus)
  }
  if (params?.sourceType) {
    records = records.filter((r) => r.sourceType === params.sourceType)
  }
  if (params?.hasElectronicFile !== undefined) {
    records = records.filter((r) => r.hasElectronicFile === params.hasElectronicFile)
  }

  return Promise.resolve({
    records,
    pageNo: params?.pageNo ?? 1,
    pageSize: params?.pageSize ?? 20,
    total: records.length,
    hasNext: false,
  })
}

export function mockGetPublicArchiveDetail(archiveId: number): Promise<PublicArchiveDetail> {
  const detail = mockPublicArchiveDetails[archiveId]
  if (!detail) return Promise.reject(new Error(`档案 ${archiveId} 不存在`))
  return Promise.resolve({ ...detail, files: [...detail.files] })
}

export function mockGeneratePublicSearchQuery(data: PublicAiQueryRequest): Promise<PublicAiQueryResult> {
  const text = data.text
  const conditions: import('@/types/public').PublicSearchParams = {}

  // 简易关键字提取
  const yearMatch = text.match(/(\d{4})\s*年\s*以后/)
  if (yearMatch) {
    conditions.formedYearStart = parseInt(yearMatch[1], 10)
  }

  const keywordMatch = text.match(/(?:公开的|查找|搜索)\s*(.+?)(?:影像|资料|档案|文件|记录|$)/)
  if (keywordMatch) {
    conditions.keyword = keywordMatch[1].trim()
  } else if (text.includes('老城改造')) {
    conditions.keyword = '老城改造'
  }

  // 来源识别
  if (text.includes('征集') || text.includes('捐赠')) {
    conditions.sourceType = 'collection'
  } else if (text.includes('编研') || text.includes('编撰')) {
    conditions.sourceType = 'compilation'
  } else if (text.includes('移交')) {
    conditions.sourceType = 'transfer'
  }

  // 电子文件属性识别
  if (text.includes('纸质') && !text.includes('电子')) {
    conditions.hasElectronicFile = false
  } else if (text.includes('电子') || text.includes('照片') || text.includes('影像') || text.includes('视频')) {
    conditions.hasElectronicFile = true
  }

  return Promise.resolve({
    ruleType: 'publicSearchQuery',
    conditions,
    rawJson: { text, parsed: true },
  })
}

export function mockDownloadPublicArchiveFile(fileId: number): Promise<Blob> {
  return Promise.resolve(new Blob([`模拟文件内容 file-${fileId}`], { type: 'application/octet-stream' }))
}

export function mockGetPublicOverview(): Promise<PublicOverviewData> {
  return Promise.resolve(mockPublicOverview)
}

export function mockGetMyCollections(params?: PageParams): Promise<PageData<PublicCollectionBatch>> {
  return Promise.resolve({
    ...mockMyCollections,
    pageNo: params?.pageNo ?? 1,
    pageSize: params?.pageSize ?? 20,
  })
}

let nextCollectionId = 20

export function mockCreateCollectionDraft(data: PublicCollectionBatch): Promise<PublicCollectionBatch> {
  const id = nextCollectionId++
  const created: PublicCollectionBatch = {
    ...data,
    id,
    batchNo: `CL-2026-${String(id).padStart(4, '0')}`,
    status: 'draft',
    statusText: '草稿',
    itemCount: data.items.length,
    items: data.items.map((item, i) => ({ ...item, id: id * 100 + i })),
  }
  return Promise.resolve(created)
}

export function mockUpdateCollectionDraft(batchId: number, data: PublicCollectionBatch): Promise<PublicCollectionBatch> {
  return Promise.resolve({
    ...data,
    id: batchId,
    itemCount: data.items.length,
  })
}

export function mockSubmitCollectionBatch(batchId: number): Promise<PublicCollectionBatch> {
  return Promise.resolve({
    id: batchId,
    batchNo: `CL-2026-${String(batchId).padStart(4, '0')}`,
    title: '已提交征集清单',
    contactName: '张三',
    contactPhone: '13800000005',
    archiveYear: 0,
    status: 'pending_contact',
    statusText: '待联系',
    submittedAt: new Date().toISOString(),
    scheduledReceiveAt: undefined,
    itemCount: 1,
    agreementAcceptedAt: new Date().toISOString(),
    items: [],
  })
}

export function mockSendPublicSmsCode(_data: PublicSmsCodeRequest): Promise<boolean> {
  return Promise.resolve(true)
}

export function mockRegisterPublicUser(data: PublicRegisterRequest): Promise<{ id: number; realName: string; roles: string[] }> {
  return Promise.resolve({
    id: 100,
    realName: data.realName,
    roles: ['public_user'],
  })
}

export function mockResetPublicPassword(_data: PublicResetPasswordRequest): Promise<boolean> {
  return Promise.resolve(true)
}
