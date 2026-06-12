// src/mock/modules/reception.ts
import type { BatchDetail, ReceptionBatch, ReceptionItem, StagingFile, UploadResult } from '@/types/reception'

// ── 移交批次 A：4 条目，含匹配异常 ──
const batchAItems: ReceptionItem[] = [
  {
    id: 101, batchId: 1, seqNo: 1,
    title: '2025 年 1 月会计凭证',
    carrierType: '纸质+电子',
    expectedFilename: '2025-01-voucher.pdf',
    paperCheckStatus: 'pending',
    fileMatchStatus: 'none',
    result: 'pending',
    acceptanceNote: '', rejectReason: '',
    createdAt: '2026-06-20T09:00:00+08:00',
    updatedAt: '2026-06-20T09:00:00+08:00',
  },
  {
    id: 102, batchId: 1, seqNo: 2,
    title: '2025 年 2 月会计凭证',
    carrierType: '纸质+电子',
    expectedFilename: '2025-02-voucher.pdf',
    paperCheckStatus: 'pending',
    fileMatchStatus: 'none',
    result: 'pending',
    acceptanceNote: '', rejectReason: '',
    createdAt: '2026-06-20T09:00:00+08:00',
    updatedAt: '2026-06-20T09:00:00+08:00',
  },
  {
    id: 103, batchId: 1, seqNo: 3,
    title: '2025 年 3 月会计凭证',
    carrierType: '纸质+电子',
    expectedFilename: '2025-03-voucher.pdf',
    paperCheckStatus: 'pending',
    fileMatchStatus: 'missing',
    result: 'pending',
    acceptanceNote: '', rejectReason: '',
    createdAt: '2026-06-20T09:00:00+08:00',
    updatedAt: '2026-06-20T09:00:00+08:00',
  },
  {
    id: 104, batchId: 1, seqNo: 4,
    title: '2025 年档案移交说明',
    carrierType: '纯纸质',
    expectedFilename: '',
    paperCheckStatus: 'pending',
    fileMatchStatus: 'none',
    result: 'pending',
    acceptanceNote: '', rejectReason: '',
    createdAt: '2026-06-20T09:00:00+08:00',
    updatedAt: '2026-06-20T09:00:00+08:00',
  },
]

// ── 移交批次 B：2 条目 ──
const batchBItems: ReceptionItem[] = [
  {
    id: 201, batchId: 2, seqNo: 1,
    title: '2025 年 1-6 月工资表',
    carrierType: '纯电子',
    expectedFilename: 'salary-2025-h1.xlsx',
    paperCheckStatus: 'passed',
    fileMatchStatus: 'none',
    result: 'pending',
    acceptanceNote: '', rejectReason: '',
    createdAt: '2026-06-08T14:00:00+08:00',
    updatedAt: '2026-06-08T14:00:00+08:00',
  },
  {
    id: 202, batchId: 2, seqNo: 2,
    title: '2025 年 7-12 月工资表',
    carrierType: '纯电子',
    expectedFilename: 'salary-2025-h2.xlsx',
    paperCheckStatus: 'passed',
    fileMatchStatus: 'none',
    result: 'pending',
    acceptanceNote: '', rejectReason: '',
    createdAt: '2026-06-08T14:00:00+08:00',
    updatedAt: '2026-06-08T14:00:00+08:00',
  },
]

// ── 模拟上传后的暂存文件（批次 A） ──
const mockStagingFiles: StagingFile[] = [
  {
    id: 301, batchId: 1,
    originalFilename: '2025-01-voucher.pdf',
    fileSize: 524288,
    sha256: 'a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2',
    scanResult: 'safe',
    matchStatus: 'matched',
    matchedItemId: 101,
    uploadBatchNo: 'UP-20260612-001',
    createdAt: '2026-06-12T10:30:00+08:00',
  },
  {
    id: 302, batchId: 1,
    originalFilename: '2025-02-voucher.pdf',
    fileSize: 483328,
    sha256: 'b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3',
    scanResult: 'safe',
    matchStatus: 'matched',
    matchedItemId: 102,
    uploadBatchNo: 'UP-20260612-001',
    createdAt: '2026-06-12T10:30:00+08:00',
  },
  {
    id: 303, batchId: 1,
    originalFilename: '2025-03-voucher-copy.pdf',
    fileSize: 512000,
    sha256: 'c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4',
    scanResult: 'safe',
    matchStatus: 'unmatched',
    uploadBatchNo: 'UP-20260612-001',
    createdAt: '2026-06-12T10:30:00+08:00',
  },
  {
    id: 304, batchId: 1,
    originalFilename: '2025-02-voucher.pdf',
    fileSize: 483328,
    sha256: 'b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3d4e5f6a1b2c3',
    scanResult: 'safe',
    matchStatus: 'duplicate',
    uploadBatchNo: 'UP-20260612-001',
    createdAt: '2026-06-12T10:30:00+08:00',
  },
]

export const mockReceptionBatches: ReceptionBatch[] = [
  {
    id: 1, batchNo: 'YJ-2026-0008',
    title: '2025 年度会计凭证移交清单',
    sourceType: 'transfer',
    status: 'pending_transfer',
    statusText: '待移交',
    organizationId: 3,
    organizationName: '克拉玛依市财政局',
    departmentName: '财务部',
    contactPerson: '小张',
    contactPhone: '0990-6123456',
    expectedTransferDate: '2026-06-24',
    submittedAt: '2026-06-20T09:00:00+08:00',
    itemCount: 4,
    signatureStatus: '已核对',
    createdAt: '2026-06-20T09:00:00+08:00',
    updatedAt: '2026-06-20T09:00:00+08:00',
  },
  {
    id: 2, batchNo: 'YJ-2026-0007',
    title: '2025 年度工资表电子档案',
    sourceType: 'transfer',
    status: 'pending_transfer',
    statusText: '待移交',
    organizationId: 3,
    organizationName: '克拉玛依市财政局',
    departmentName: '人事财务联合组',
    contactPerson: '小张',
    contactPhone: '0990-6123456',
    expectedTransferDate: '2026-06-09',
    submittedAt: '2026-06-08T14:00:00+08:00',
    itemCount: 2,
    signatureStatus: '已核对',
    createdAt: '2026-06-08T14:00:00+08:00',
    updatedAt: '2026-06-08T14:00:00+08:00',
  },
]

/** 模拟批次详情 */
export function mockBatchDetail(batchId: number): BatchDetail | null {
  const batch = mockReceptionBatches.find((b) => b.id === batchId)
  if (!batch) return null
  const items = batchId === 1 ? batchAItems : batchBItems
  const stagingFiles = batchId === 1 ? mockStagingFiles : []
  return {
    batch,
    items: JSON.parse(JSON.stringify(items)),
    stagingFiles: JSON.parse(JSON.stringify(stagingFiles)),
    matchSummary: {
      matched: stagingFiles.filter((f) => f.matchStatus === 'matched').length,
      unmatched: stagingFiles.filter((f) => f.matchStatus === 'unmatched').length,
      duplicate: stagingFiles.filter((f) => f.matchStatus === 'duplicate').length,
      failed: stagingFiles.filter((f) => f.scanResult === 'unsafe' || f.scanResult === 'error').length,
      missingItems: items.filter((i) => i.fileMatchStatus === 'missing').map((i) => i.id),
    },
  }
}

/** 模拟文件上传结果 */
export function mockUploadResult(): UploadResult {
  return {
    uploadBatchNo: 'UP-20260612-001',
    files: JSON.parse(JSON.stringify(mockStagingFiles)),
    matchSummary: {
      matched: 2,
      unmatched: 1,
      duplicate: 1,
      failed: 0,
      missingItems: [103],
    },
  }
}
