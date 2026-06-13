import type {
  BorrowRequest,
  BorrowRequestCreateData,
  BorrowRequestDetail,
  BorrowRequestPage,
  BorrowRequestParams,
  InternalAiQueryRequest,
  InternalAiQueryResult,
  InternalArchive,
  InternalArchiveDetail,
  InternalArchivePage,
  InternalDashboardData,
  InternalSearchParams,
} from '@/types/internal'

// ——————————————————————————————————
// 档案样本（详情；检索结果为其投影）
// ——————————————————————————————————

const archiveSmartCity: InternalArchiveDetail = {
  id: 101,
  archiveNo: 'KJ-2025-0188',
  title: '智慧城市项目年度技术报告',
  categoryId: 3,
  categoryName: '科技档案',
  responsibleText: '技术部',
  formedDate: '2025-12-20',
  securityLevel: 2,
  openStatus: 'closed',
  carrierStatus: 'paper_electronic',
  sourceType: 'transfer',
  tags: ['智慧城市', '年度报告'],
  hasElectronicFile: true,
  canPreview: true,
  canDownload: true,
  canBorrow: true,
  borrowHint: '可申请纸质借阅',
  archivedAt: '2026-01-15T09:00:00+08:00',
  summary: '2025 年度智慧城市建设项目技术总结，含立项、实施、验收全流程技术材料。',
  retentionPeriod: '30y',
  files: [
    { id: 2001, originalFilename: '智慧城市年度技术报告.pdf', fileFormat: 'PDF', fileSize: 8_200_000, fileRole: '原文', fileCheckStatus: 'safe', canPreview: true, canDownload: true },
    { id: 2002, originalFilename: '系统架构扫描件.jpg', fileFormat: 'JPG', fileSize: 3_400_000, fileRole: '扫描件', fileCheckStatus: 'safe', canPreview: true, canDownload: true },
  ],
}

const archiveMeeting: InternalArchiveDetail = {
  id: 102,
  archiveNo: 'WS-2024-0912',
  title: '信息化建设会议纪要',
  categoryId: 1,
  categoryName: '文书档案',
  responsibleText: '办公室',
  formedDate: '2024-09-18',
  securityLevel: 0,
  openStatus: 'open',
  carrierStatus: 'electronic',
  sourceType: 'transfer',
  tags: ['信息化', '会议纪要'],
  hasElectronicFile: true,
  canPreview: true,
  canDownload: true,
  canBorrow: false,
  borrowHint: '纯电子档案不支持纸质借阅',
  archivedAt: '2025-02-10T14:00:00+08:00',
  summary: '信息化建设推进会议纪要，含决议事项与责任分工。',
  retentionPeriod: '10y',
  files: [
    { id: 2003, originalFilename: '信息化建设会议纪要.pdf', fileFormat: 'PDF', fileSize: 1_200_000, fileRole: '原文', fileCheckStatus: 'safe', canPreview: true, canDownload: true },
  ],
}

const archiveUpgrade: InternalArchiveDetail = {
  id: 103,
  archiveNo: 'KJ-2023-0416',
  title: '系统升级实施方案',
  categoryId: 3,
  categoryName: '科技档案',
  responsibleText: '信息中心',
  formedDate: '2023-07-30',
  securityLevel: 2,
  openStatus: 'closed',
  carrierStatus: 'paper',
  sourceType: 'transfer',
  tags: ['系统升级', '实施方案'],
  hasElectronicFile: false,
  canPreview: false,
  canDownload: false,
  canBorrow: true,
  borrowHint: '可申请纸质借阅',
  archivedAt: '2024-03-20T10:00:00+08:00',
  summary: '核心业务系统升级实施方案纸质原件，含手批与签章。',
  retentionPeriod: 'permanent',
  files: [],
}

const archiveBudget: InternalArchiveDetail = {
  id: 104,
  archiveNo: 'WS-2025-0501',
  title: '财政局2025年度预算批复',
  categoryId: 1,
  categoryName: '文书档案',
  responsibleText: '克拉玛依市财政局',
  formedDate: '2025-03-05',
  securityLevel: 1,
  openStatus: 'closed',
  carrierStatus: 'paper_electronic',
  sourceType: 'transfer',
  tags: ['财政预算', '批复'],
  hasElectronicFile: true,
  canPreview: true,
  canDownload: false,
  canBorrow: true,
  borrowHint: '可申请纸质借阅',
  archivedAt: '2025-06-12T11:00:00+08:00',
  summary: '2025 年度财政预算批复，含电子正本与纸质原件。',
  retentionPeriod: '30y',
  files: [
    { id: 2004, originalFilename: '2025预算批复.pdf', fileFormat: 'PDF', fileSize: 2_100_000, fileRole: '原文', fileCheckStatus: 'safe', canPreview: true, canDownload: false },
  ],
}

const archiveDetails: InternalArchiveDetail[] = [
  archiveSmartCity,
  archiveMeeting,
  archiveUpgrade,
  archiveBudget,
]

function toSummary(detail: InternalArchiveDetail): InternalArchive {
  const { summary, retentionPeriod, files, ...rest } = detail
  return rest as InternalArchive
}

// ——————————————————————————————————
// 借阅申请样本（覆盖 7 种状态）
// ——————————————————————————————————

const borrowRequests: BorrowRequest[] = [
  {
    id: 301, requestNo: 'BR-202606-021', archiveId: 104, archiveNo: 'WS-2025-0501',
    archiveTitle: '财政局2025年度预算批复', status: 'applied', expectedDays: 7,
    expectedVisitAt: '2026-06-14T10:00:00+08:00', appliedAt: '2026-06-07T09:30:00+08:00',
  },
  {
    id: 302, requestNo: 'BR-202606-018', archiveId: 101, archiveNo: 'KJ-2025-0188',
    archiveTitle: '智慧城市项目年度技术报告', status: 'approved', expectedDays: 7,
    expectedVisitAt: '2026-06-13T10:00:00+08:00', appliedAt: '2026-06-04T14:00:00+08:00',
    approvedAt: '2026-06-06T10:00:00+08:00',
  },
  {
    id: 303, requestNo: 'BR-202606-020', archiveId: 103, archiveNo: 'KJ-2023-0416',
    archiveTitle: '系统升级实施方案', status: 'voucher_issued', expectedDays: 5,
    expectedVisitAt: '2026-06-15T09:00:00+08:00', appliedAt: '2026-06-05T11:00:00+08:00',
    approvedAt: '2026-06-07T09:00:00+08:00', voucherNo: 'VCH-000012',
    voucherIssuedAt: '2026-06-08T10:00:00+08:00',
  },
  {
    id: 304, requestNo: 'BR-202606-015', archiveId: 101, archiveNo: 'KJ-2025-0188',
    archiveTitle: '智慧城市项目年度技术报告', status: 'checked_out', expectedDays: 7,
    expectedVisitAt: '2026-06-01T10:00:00+08:00', appliedAt: '2026-05-26T10:00:00+08:00',
    approvedAt: '2026-05-28T09:00:00+08:00', checkedOutAt: '2026-06-01T10:30:00+08:00',
    dueAt: '2026-06-08T10:30:00+08:00', overdue: true,
  },
  {
    id: 305, requestNo: 'BR-202605-033', archiveId: 104, archiveNo: 'WS-2025-0501',
    archiveTitle: '财政局2025年度预算批复', status: 'returned', expectedDays: 3,
    appliedAt: '2026-05-15T10:00:00+08:00', approvedAt: '2026-05-16T09:00:00+08:00',
    checkedOutAt: '2026-05-18T10:00:00+08:00', dueAt: '2026-05-21T10:00:00+08:00',
    returnedAt: '2026-05-21T09:30:00+08:00',
  },
  {
    id: 306, requestNo: 'BR-202605-017', archiveId: 101, archiveNo: 'KJ-2025-0188',
    archiveTitle: '智慧城市项目年度技术报告', status: 'rejected', expectedDays: 5,
    appliedAt: '2026-05-12T11:00:00+08:00',
  },
  {
    id: 307, requestNo: 'BR-202605-009', archiveId: 103, archiveNo: 'KJ-2023-0416',
    archiveTitle: '系统升级实施方案', status: 'abnormal_return', expectedDays: 7,
    appliedAt: '2026-05-03T10:00:00+08:00', approvedAt: '2026-05-04T09:00:00+08:00',
    checkedOutAt: '2026-05-05T10:00:00+08:00', dueAt: '2026-05-12T10:00:00+08:00',
    returnedAt: '2026-05-12T14:00:00+08:00',
  },
]

const borrowRequestDetails: Record<number, BorrowRequestDetail> = {
  301: { ...borrowRequests[0], reason: '财政预算核查需要查阅原件。', contactPhone: '13800000004' },
  302: { ...borrowRequests[1], reason: '项目复核需要查阅纸质原件。', contactPhone: '13800000004', opinion: '同意借阅 7 天。' },
  303: { ...borrowRequests[2], reason: '历史方案参考。', contactPhone: '13800000004', opinion: '同意。' },
  304: { ...borrowRequests[3], reason: '技术报告复核。', contactPhone: '13800000004', opinion: '同意。' },
  305: { ...borrowRequests[4], reason: '预算材料查阅。', contactPhone: '13800000004', opinion: '同意。', returnCheckResult: 'normal', returnNote: '实体完好。' },
  306: { ...borrowRequests[5], reason: '预算复核。', contactPhone: '13800000004', rejectReason: '目标档案处于盘点范围，暂不支持借阅。' },
  307: { ...borrowRequests[6], reason: '方案参考。', contactPhone: '13800000004', opinion: '同意。', returnCheckResult: 'damaged', returnNote: '边缘破损，已登记修复。' },
}

// ——————————————————————————————————
// mock 函数
// ——————————————————————————————————

export function mockInternalDashboard(): Promise<InternalDashboardData> {
  return Promise.resolve({
    stats: {
      recentViewCount: 14,
      pendingApprovalCount: 2,
      approvedPendingPickupCount: 1,
      downloadCount: 23,
    },
    recentViews: [
      { id: 401, archiveId: 101, archiveNo: 'KJ-2025-0188', title: '智慧城市项目年度技术报告', categoryName: '科技档案', securityLevel: 2, viewedAt: '2026-06-07T11:20:00+08:00', accessStatus: 'available' },
      { id: 402, archiveId: 102, archiveNo: 'WS-2024-0912', title: '信息化建设会议纪要', categoryName: '文书档案', securityLevel: 0, viewedAt: '2026-06-05T16:42:00+08:00', accessStatus: 'available' },
      { id: 403, archiveId: 105, archiveNo: 'WS-2019-0440', title: '历史专项资料汇编', categoryName: '文书档案', securityLevel: 2, viewedAt: '2026-05-22T09:10:00+08:00', accessStatus: 'permission_changed' },
    ],
    borrowRequests: [borrowRequests[0], borrowRequests[1], borrowRequests[4], borrowRequests[5]],
    currentLoans: [borrowRequests[3]],
    downloads: [
      { id: 501, archiveId: 101, archiveNo: 'KJ-2025-0188', title: '智慧城市项目年度技术报告', downloadedAt: '2026-06-07T11:25:00+08:00' },
      { id: 502, archiveId: 102, archiveNo: 'WS-2024-0912', title: '信息化建设会议纪要', downloadedAt: '2026-06-03T14:10:00+08:00' },
      { id: 503, archiveId: 104, archiveNo: 'WS-2025-0501', title: '财政局2025年度预算批复', downloadedAt: '2026-05-28T10:00:00+08:00' },
    ],
    permission: {
      role: '内部查阅者',
      organizationName: '技术部',
      maxSecurityLevel: 2,
      dataScope: '本单位授权全宗',
    },
  })
}

export function mockSearchInternalArchives(params?: InternalSearchParams): Promise<InternalArchivePage> {
  let all = archiveDetails.map(toSummary)
  if (params?.keyword) {
    const kw = params.keyword.toLowerCase()
    all = all.filter((a) =>
      `${a.archiveNo} ${a.title} ${a.responsibleText} ${a.tags.join(' ')}`.toLowerCase().includes(kw),
    )
  }
  if (params?.archiveNo) all = all.filter((a) => a.archiveNo.toLowerCase().includes(params.archiveNo!.toLowerCase()))
  if (params?.title) all = all.filter((a) => a.title.toLowerCase().includes(params.title!.toLowerCase()))
  if (params?.responsibleText) all = all.filter((a) => a.responsibleText.toLowerCase().includes(params.responsibleText!.toLowerCase()))
  if (params?.categoryId) all = all.filter((a) => a.categoryId === params.categoryId)
  if (params?.carrierStatus) all = all.filter((a) => a.carrierStatus === params.carrierStatus)
  if (params?.sourceType) all = all.filter((a) => a.sourceType === params.sourceType)
  if (params?.openStatus) all = all.filter((a) => a.openStatus === params.openStatus)
  if (params?.formedYearStart) all = all.filter((a) => Number(a.formedDate.slice(0, 4)) >= params.formedYearStart!)
  if (params?.formedYearEnd) all = all.filter((a) => Number(a.formedDate.slice(0, 4)) <= params.formedYearEnd!)
  if (params?.hasElectronicFile !== undefined) all = all.filter((a) => a.hasElectronicFile === params.hasElectronicFile)
  return Promise.resolve({
    records: all,
    pageNo: params?.pageNo ?? 1,
    pageSize: params?.pageSize ?? 20,
    total: all.length,
    hasNext: false,
  })
}

export function mockInternalArchiveDetail(id: number): Promise<InternalArchiveDetail | null> {
  const detail = archiveDetails.find((d) => d.id === id) ?? null
  return Promise.resolve(detail ? { ...detail } : null)
}

export function mockGenerateInternalAiQuery(data: InternalAiQueryRequest): Promise<InternalAiQueryResult> {
  const conditions: Partial<InternalSearchParams> = {
    keyword: '智慧城市 项目 报告',
    archiveNo: 'KJ',
    title: '智慧城市项目',
    categoryId: 3,
    responsibleText: '技术部',
    formedYearStart: 2024,
    formedYearEnd: 2026,
    securityLevelMax: 2,
    carrierStatus: 'paper_electronic',
    hasElectronicFile: true,
  }
  return Promise.resolve({
    ruleType: 'internalSearchQuery',
    conditions,
    rawJson: { input: data.text, generated: conditions },
  })
}

export function mockPreviewInternalFile(fileId: number): Promise<string> {
  return Promise.resolve(`# 文件 ${fileId} 预览\n\n这是模拟的电子文件预览内容。实际接入后返回预签名 URL 或文件流。`)
}

export function mockDownloadInternalFile(_fileId: number): Promise<Blob> {
  return Promise.resolve(new Blob(['Mock 内部档案文件内容'], { type: 'application/pdf' }))
}

export function mockCreateBorrowRequest(data: BorrowRequestCreateData): Promise<BorrowRequest> {
  const archive = archiveDetails.find((d) => d.id === data.archiveId)
  const now = new Date()
  const seq = String(900 + Math.floor(Math.random() * 100))
  return Promise.resolve({
    id: Number(seq),
    requestNo: `BR-${now.getFullYear()}${String(now.getMonth() + 1).padStart(2, '0')}-${seq}`,
    archiveId: data.archiveId,
    archiveNo: archive?.archiveNo ?? '',
    archiveTitle: archive?.title ?? '',
    status: 'applied',
    expectedDays: data.expectedDays,
    expectedVisitAt: data.expectedVisitAt,
    appliedAt: now.toISOString(),
  })
}

export function mockMyBorrowRequests(params?: BorrowRequestParams): Promise<BorrowRequestPage> {
  let all = [...borrowRequests]
  if (params?.status) all = all.filter((r) => r.status === params.status)
  if (params?.keyword) {
    const kw = params.keyword.toLowerCase()
    all = all.filter((r) => `${r.requestNo} ${r.archiveNo} ${r.archiveTitle}`.toLowerCase().includes(kw))
  }
  return Promise.resolve({
    records: all,
    pageNo: params?.pageNo ?? 1,
    pageSize: params?.pageSize ?? 20,
    total: all.length,
    hasNext: false,
  })
}

export function mockBorrowRequestDetail(id: number): Promise<BorrowRequestDetail | null> {
  const detail = borrowRequestDetails[id]
  return Promise.resolve(detail ? { ...detail } : null)
}

export function mockExportBorrowVoucher(_id: number): Promise<Blob> {
  return Promise.resolve(new Blob(['Mock 借阅凭证 PDF 内容'], { type: 'application/pdf' }))
}
