import type { PageData } from '@/types/api'
import type {
  TransferBatch,
  TransferBatchDetail,
  TransferBatchQuery,
  TransferDashboard,
} from '@/types/transfer'

// ——————————————————————————————————
// 模拟数据
// ——————————————————————————————————

const batch1Detail: TransferBatchDetail = {
  id: 1,
  batchNo: 'TR-2026-0001',
  title: '2026 年度财务处会计档案移交清单',
  sourceType: 'transfer',
  status: 'draft',
  statusText: '草稿',
  organizationName: '克拉玛依市财政局',
  departmentName: '财务处',
  contactName: '张明',
  contactPhone: '13800000001',
  archiveYear: 2026,
  expectedTransferDate: '2026-06-20',
  itemCount: 3,
  acceptedCount: 0,
  rejectedCount: 0,
  items: [
    {
      id: 101,
      batchId: 1,
      seqNo: 1,
      inputTitle: '2026 年第一季度会计凭证',
      pageCount: 120,
      retentionPeriod: '30y',
      carrierStatus: 'paper_electronic',
      securityLevel: 0,
      openStatus: 'open',
      allowDigitization: true,
      electronicFormat: 'PDF',
      expectedFilename: '2026Q1-vouchers.pdf',
      formedDate: '2026-03-31',
      status: 'draft',
    },
    {
      id: 102,
      batchId: 1,
      seqNo: 2,
      inputTitle: '2026 年第一季度会计报表',
      pageCount: 45,
      retentionPeriod: 'permanent',
      carrierStatus: 'paper',
      securityLevel: 0,
      openStatus: 'open',
      allowDigitization: false,
      formedDate: '2026-03-31',
      status: 'draft',
    },
    {
      id: 103,
      batchId: 1,
      seqNo: 3,
      inputTitle: '2026 年第一季度银行对账单',
      pageCount: 30,
      retentionPeriod: '10y',
      carrierStatus: 'electronic',
      securityLevel: 1,
      openStatus: 'closed',
      allowDigitization: true,
      electronicFormat: 'PDF',
      expectedFilename: '2026Q1-bank-statement.pdf',
      formedDate: '2026-03-31',
      status: 'draft',
    },
  ],
}

const batch2Detail: TransferBatchDetail = {
  id: 2,
  batchNo: 'TR-2026-0002',
  title: '2025 年度交通局行政档案移交清单',
  sourceType: 'transfer',
  status: 'pending_transfer',
  statusText: '待移交',
  organizationName: '克拉玛依市交通局',
  departmentName: '办公室',
  contactName: '李华',
  contactPhone: '13800000002',
  archiveYear: 2025,
  expectedTransferDate: '2026-05-15',
  submittedAt: '2026-05-10T09:30:00+08:00',
  itemCount: 5,
  acceptedCount: 0,
  rejectedCount: 0,
  items: [
    {
      id: 201,
      batchId: 2,
      seqNo: 1,
      inputTitle: '2025 年度行政会议纪要',
      retentionPeriod: '30y',
      carrierStatus: 'paper_electronic',
      securityLevel: 1,
      openStatus: 'closed',
      allowDigitization: true,
      electronicFormat: 'PDF',
      expectedFilename: '2025-meeting-minutes.pdf',
      formedDate: '2025-12-20',
      status: 'pending_acceptance',
    },
    {
      id: 202,
      batchId: 2,
      seqNo: 2,
      inputTitle: '2025 年度行政审批文件',
      retentionPeriod: 'permanent',
      carrierStatus: 'paper',
      securityLevel: 0,
      openStatus: 'open',
      allowDigitization: false,
      formedDate: '2025-12-31',
      status: 'pending_acceptance',
    },
    {
      id: 203,
      batchId: 2,
      seqNo: 3,
      inputTitle: '2025 年度公路建设项目报告',
      retentionPeriod: 'permanent',
      carrierStatus: 'paper_electronic',
      securityLevel: 0,
      openStatus: 'open',
      allowDigitization: true,
      electronicFormat: 'DOCX',
      expectedFilename: '2025-road-project-report.docx',
      formedDate: '2025-11-15',
      status: 'pending_acceptance',
    },
    {
      id: 204,
      batchId: 2,
      seqNo: 4,
      inputTitle: '2025 年度交通事故统计',
      retentionPeriod: '30y',
      carrierStatus: 'electronic',
      securityLevel: 0,
      openStatus: 'open',
      allowDigitization: true,
      electronicFormat: 'XLSX',
      expectedFilename: '2025-accident-stats.xlsx',
      formedDate: '2025-12-31',
      status: 'pending_acceptance',
    },
    {
      id: 205,
      batchId: 2,
      seqNo: 5,
      inputTitle: '2025 年度财务决算报表',
      pageCount: 60,
      retentionPeriod: 'permanent',
      carrierStatus: 'paper_electronic',
      securityLevel: 1,
      openStatus: 'closed',
      allowDigitization: true,
      electronicFormat: 'PDF',
      expectedFilename: '2025-financial-report.pdf',
      formedDate: '2025-12-31',
      status: 'pending_acceptance',
    },
  ],
}

const batch3Detail: TransferBatchDetail = {
  id: 3,
  batchNo: 'TR-2026-0003',
  title: '2025 年度教育局文书档案移交清单',
  sourceType: 'transfer',
  status: 'partially_received',
  statusText: '部分接收',
  organizationName: '克拉玛依市教育局',
  departmentName: '综合档案室',
  contactName: '王芳',
  contactPhone: '13800000003',
  archiveYear: 2025,
  expectedTransferDate: '2026-04-01',
  submittedAt: '2026-03-25T14:00:00+08:00',
  receivedAt: '2026-04-02T10:20:00+08:00',
  itemCount: 4,
  acceptedCount: 3,
  rejectedCount: 1,
  items: [
    {
      id: 301,
      batchId: 3,
      seqNo: 1,
      inputTitle: '2025 年度教育工作总结',
      retentionPeriod: 'permanent',
      carrierStatus: 'paper_electronic',
      securityLevel: 0,
      openStatus: 'open',
      allowDigitization: true,
      electronicFormat: 'PDF',
      expectedFilename: '2025-education-summary.pdf',
      formedDate: '2025-12-31',
      status: 'accepted',
    },
    {
      id: 302,
      batchId: 3,
      seqNo: 2,
      inputTitle: '2025 年度招生计划文件',
      retentionPeriod: '30y',
      carrierStatus: 'paper',
      securityLevel: 0,
      openStatus: 'open',
      allowDigitization: false,
      formedDate: '2025-06-15',
      status: 'accepted',
    },
    {
      id: 303,
      batchId: 3,
      seqNo: 3,
      inputTitle: '2025 年度教师培训记录',
      retentionPeriod: '10y',
      carrierStatus: 'electronic',
      securityLevel: 0,
      openStatus: 'open',
      allowDigitization: true,
      electronicFormat: 'DOCX',
      expectedFilename: '2025-teacher-training.docx',
      formedDate: '2025-09-01',
      status: 'accepted',
    },
    {
      id: 304,
      batchId: 3,
      seqNo: 4,
      inputTitle: '涉密教学评估材料',
      pageCount: 20,
      retentionPeriod: '30y',
      carrierStatus: 'paper',
      securityLevel: 2,
      openStatus: 'closed',
      allowDigitization: false,
      formedDate: '2025-07-20',
      status: 'rejected',
      rejectReason: '文件缺页，第 12-15 页缺失，请补充后重新提交。',
    },
  ],
}

const batch4Detail: TransferBatchDetail = {
  id: 4,
  batchNo: 'TR-2026-0004',
  title: '2024 年度住建局工程档案移交清单',
  sourceType: 'transfer',
  status: 'shelved',
  statusText: '已上架',
  organizationName: '克拉玛依市住建局',
  departmentName: '档案管理科',
  contactName: '赵刚',
  contactPhone: '13800000004',
  archiveYear: 2024,
  expectedTransferDate: '2025-03-01',
  submittedAt: '2025-02-20T11:00:00+08:00',
  receivedAt: '2025-03-05T09:00:00+08:00',
  archivedAt: '2025-03-10T16:30:00+08:00',
  shelvedAt: '2025-03-12T14:00:00+08:00',
  itemCount: 2,
  acceptedCount: 2,
  rejectedCount: 0,
  items: [
    {
      id: 401,
      batchId: 4,
      seqNo: 1,
      inputTitle: '2024 年度城市老旧小区改造工程档案',
      retentionPeriod: 'permanent',
      carrierStatus: 'paper_electronic',
      securityLevel: 0,
      openStatus: 'open',
      allowDigitization: true,
      electronicFormat: 'PDF',
      expectedFilename: '2024-renovation-project.pdf',
      formedDate: '2024-12-31',
      status: 'archived',
    },
    {
      id: 402,
      batchId: 4,
      seqNo: 2,
      inputTitle: '2024 年度市政道路建设审批文件',
      retentionPeriod: '30y',
      carrierStatus: 'paper_electronic',
      securityLevel: 1,
      openStatus: 'closed',
      allowDigitization: true,
      electronicFormat: 'PDF',
      expectedFilename: '2024-road-approval.pdf',
      formedDate: '2024-11-15',
      status: 'archived',
    },
  ],
}

// ——————————————————————————————————
// 导出
// ——————————————————————————————————

export const mockTransferDashboard: TransferDashboard = {
  summary: {
    draft: 1,
    pendingTransfer: 1,
    partiallyReceived: 1,
    received: 0,
    archived: 0,
    shelved: 1,
    rejected: 0,
  },
  recentBatches: [batch4Detail, batch3Detail, batch2Detail, batch1Detail],
}

export const mockTransferBatches: PageData<TransferBatch> = {
  records: [batch4Detail, batch3Detail, batch2Detail, batch1Detail],
  pageNo: 1,
  pageSize: 20,
  total: 4,
  hasNext: false,
}

export const mockTransferBatchDetails: Record<number, TransferBatchDetail> = {
  1: batch1Detail,
  2: batch2Detail,
  3: batch3Detail,
  4: batch4Detail,
}

// ——————————————————————————————————
// Mock 函数
// ——————————————————————————————————

export function mockGetTransferDashboard(): Promise<TransferDashboard> {
  return Promise.resolve(mockTransferDashboard)
}

export function mockGetTransferBatches(params?: TransferBatchQuery): Promise<PageData<TransferBatch>> {
  let records = [...mockTransferBatches.records]

  if (params?.status) {
    records = records.filter((r) => r.status === params.status)
  }
  if (params?.keyword) {
    const kw = params.keyword.toLowerCase()
    records = records.filter(
      (r) =>
        r.title.toLowerCase().includes(kw) ||
        r.batchNo.toLowerCase().includes(kw) ||
        r.departmentName.toLowerCase().includes(kw),
    )
  }
  if (params?.archiveYear) {
    records = records.filter((r) => r.archiveYear === params.archiveYear)
  }

  return Promise.resolve({
    records,
    pageNo: params?.pageNo ?? 1,
    pageSize: params?.pageSize ?? 20,
    total: records.length,
    hasNext: false,
  })
}

export function mockGetTransferBatchDetail(batchId: number): Promise<TransferBatchDetail> {
  const detail = mockTransferBatchDetails[batchId]
  if (!detail) return Promise.reject(new Error(`批次 ${batchId} 不存在`))
  return Promise.resolve({ ...detail, items: [...detail.items] })
}

let nextBatchId = 10

export function mockCreateTransferBatch(data: TransferBatchDetail): Promise<TransferBatchDetail> {
  const id = nextBatchId++
  const created: TransferBatchDetail = {
    ...data,
    id,
    batchNo: `TR-2026-${String(id).padStart(4, '0')}`,
    sourceType: 'transfer',
    status: 'draft',
    statusText: '草稿',
    itemCount: data.items.length,
    acceptedCount: 0,
    rejectedCount: 0,
    items: data.items.map((item, i) => ({ ...item, id: id * 100 + i, batchId: id })),
  }
  mockTransferBatchDetails[id] = created
  return Promise.resolve(created)
}

export function mockUpdateTransferBatch(batchId: number, data: TransferBatchDetail): Promise<TransferBatchDetail> {
  const existing = mockTransferBatchDetails[batchId]
  if (!existing) return Promise.reject(new Error(`批次 ${batchId} 不存在`))
  const updated: TransferBatchDetail = {
    ...existing,
    ...data,
    id: batchId,
    batchNo: existing.batchNo,
    status: existing.status,
    statusText: existing.statusText,
    itemCount: data.items.length,
  }
  mockTransferBatchDetails[batchId] = updated
  return Promise.resolve(updated)
}

export function mockSubmitTransferBatch(batchId: number): Promise<TransferBatchDetail> {
  const existing = mockTransferBatchDetails[batchId]
  if (!existing) return Promise.reject(new Error(`批次 ${batchId} 不存在`))
  const submitted: TransferBatchDetail = {
    ...existing,
    status: 'pending_transfer',
    statusText: '待移交',
    submittedAt: new Date().toISOString(),
  }
  mockTransferBatchDetails[batchId] = submitted
  return Promise.resolve(submitted)
}

export function mockExportTransferBatch(_batchId: number): Promise<Blob> {
  return Promise.resolve(new Blob(['批次导出内容'], { type: 'application/octet-stream' }))
}
