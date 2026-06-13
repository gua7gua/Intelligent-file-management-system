// src/mock/modules/archive.ts
import type {
  PendingBatch,
  PendingBatchDetail,
  PendingItem,
  AiTask,
  ArchiveRecord,
  ArchiveDetail,
} from '@/types/archive'

// ── 待入库 mock 数据 ──

const batch1Items: PendingItem[] = [
  {
    id: 1001, batchId: 1, seqNo: 1,
    inputTitle: '2025 年 1 月会计凭证',
    expectedFilename: '2025-01-voucher.pdf',
    carrierStatus: 'paper_electronic',
    itemStatus: 'accepted',
    matchStatus: '匹配成功',
    securityLevel: 0,
    retentionPeriod: '30y',
    openStatus: 'closed',
    allowDigitization: true,
    createdAt: '2026-06-12T10:00:00+08:00',
    updatedAt: '2026-06-12T10:00:00+08:00',
  },
  {
    id: 1002, batchId: 1, seqNo: 2,
    inputTitle: '2025 年 2 月会计凭证',
    expectedFilename: '2025-02-voucher.pdf',
    carrierStatus: 'paper_electronic',
    itemStatus: 'confirmed',
    matchStatus: '匹配成功',
    securityLevel: 0,
    retentionPeriod: '30y',
    openStatus: 'closed',
    allowDigitization: true,
    confirmedTitle: '2025 年 2 月会计凭证',
    confirmedResponsible: '克拉玛依市财政局财务部',
    confirmedFormedDate: '2025-02-28',
    confirmedCategoryId: 3,
    confirmedTags: ['会计凭证', '2025'],
    boxNo: 'BOX-128',
    spine: '2025 会计凭证 02',
    locationCode: '401-03-02-05',
    createdAt: '2026-06-12T10:00:00+08:00',
    updatedAt: '2026-06-12T14:00:00+08:00',
  },
  {
    id: 1003, batchId: 1, seqNo: 3,
    inputTitle: '2025 年 3 月会计凭证',
    expectedFilename: '2025-03-voucher.pdf',
    carrierStatus: 'paper_electronic',
    itemStatus: 'suggested',
    matchStatus: '匹配成功',
    securityLevel: 0,
    retentionPeriod: '30y',
    openStatus: 'closed',
    allowDigitization: true,
    suggestedTitle: '2025 年 3 月会计凭证',
    suggestedResponsible: '克拉玛依市财政局财务部',
    suggestedFormedDate: '2025-03-31',
    suggestedCategoryId: 3,
    suggestedTags: ['会计凭证', '2025'],
    createdAt: '2026-06-12T10:00:00+08:00',
    updatedAt: '2026-06-12T15:30:00+08:00',
  },
  {
    id: 1004, batchId: 1, seqNo: 4,
    inputTitle: '2025 年档案移交说明',
    expectedFilename: '',
    carrierStatus: 'paper',
    itemStatus: 'archived',
    matchStatus: '无电子文件',
    securityLevel: 0,
    retentionPeriod: '10y',
    openStatus: 'closed',
    allowDigitization: true,
    confirmedTitle: '2025 年档案移交说明',
    confirmedResponsible: '克拉玛依市财政局财务部',
    confirmedFormedDate: '2025-06-01',
    confirmedCategoryId: 1,
    confirmedTags: ['移交说明'],
    archiveId: 10,
    archiveNo: 'ARC-000188',
    lifecycleStatus: 'normal',
    boxNo: 'BOX-132',
    spine: '2025 移交说明',
    locationCode: '401-02-01-08',
    createdAt: '2026-06-12T10:00:00+08:00',
    updatedAt: '2026-06-12T16:00:00+08:00',
  },
]

export const mockPendingBatches: PendingBatch[] = [
  {
    id: 1,
    batchNo: 'BAT-0008',
    title: '2025 年度会计凭证移交清单',
    sourceType: 'transfer',
    status: 'partially_received',
    itemCount: 4,
    acceptedCount: 4,
    returnedCount: 0,
    aiStatus: 'not_started',
    createdAt: '2026-06-12T09:00:00+08:00',
    updatedAt: '2026-06-12T09:00:00+08:00',
  },
  {
    id: 2,
    batchNo: 'BAT-0009',
    title: '2025 年度工资表电子档案',
    sourceType: 'transfer',
    status: 'received',
    itemCount: 2,
    acceptedCount: 2,
    returnedCount: 0,
    aiStatus: 'completed',
    createdAt: '2026-06-11T14:00:00+08:00',
    updatedAt: '2026-06-12T11:00:00+08:00',
  },
]

const batch2Items: PendingItem[] = [
  {
    id: 2001, batchId: 2, seqNo: 1,
    inputTitle: '2025 年 1-6 月工资表',
    expectedFilename: 'salary-2025-h1.xlsx',
    carrierStatus: 'electronic',
    itemStatus: 'suggested',
    matchStatus: '匹配成功',
    securityLevel: 0,
    retentionPeriod: '30y',
    openStatus: 'closed',
    allowDigitization: true,
    suggestedTitle: '2025 年上半年工资表',
    suggestedResponsible: '克拉玛依市财政局人事部',
    suggestedFormedDate: '2025-06-30',
    suggestedCategoryId: 3,
    suggestedTags: ['工资', '2025'],
    createdAt: '2026-06-11T14:00:00+08:00',
    updatedAt: '2026-06-12T11:00:00+08:00',
  },
  {
    id: 2002, batchId: 2, seqNo: 2,
    inputTitle: '2025 年 7-12 月工资表',
    expectedFilename: 'salary-2025-h2.xlsx',
    carrierStatus: 'electronic',
    itemStatus: 'archived',
    matchStatus: '匹配成功',
    securityLevel: 0,
    retentionPeriod: '30y',
    openStatus: 'open',
    allowDigitization: true,
    confirmedTitle: '2025 年下半年工资表',
    confirmedResponsible: '克拉玛依市财政局人事部',
    confirmedFormedDate: '2025-12-31',
    confirmedCategoryId: 3,
    confirmedTags: ['工资', '2025'],
    archiveId: 11,
    archiveNo: 'ARC-000189',
    lifecycleStatus: 'normal',
    createdAt: '2026-06-11T14:00:00+08:00',
    updatedAt: '2026-06-12T11:30:00+08:00',
  },
]

/** 模拟待入库批次详情 */
export function mockPendingBatchDetail(batchId: number): PendingBatchDetail | null {
  const batch = mockPendingBatches.find((b) => b.id === batchId)
  if (!batch) return null
  const items = batchId === 1 ? batch1Items : batch2Items
  return {
    ...batch,
    items: JSON.parse(JSON.stringify(items)),
  }
}

/** 模拟 AI 任务 */
export let mockAiTask: AiTask = {
  id: 1,
  taskNo: 'AIT-000001',
  status: 'completed',
  batchSize: 50,
  totalBatches: 1,
  completedCount: 4,
  failedCount: 0,
}

/** 模拟启动 AI 补全（1.2 秒后完成） */
export function mockStartAiCompletion(batchId: number): Promise<AiTask> {
  const batch = mockPendingBatches.find((b) => b.id === batchId)
  if (batch) batch.aiStatus = 'running'

  return new Promise((resolve) => {
    setTimeout(() => {
      const items = batchId === 1 ? batch1Items : batch2Items
      items.forEach((it) => {
        if (it.itemStatus === 'accepted') {
          it.suggestedTitle = it.inputTitle
          it.suggestedResponsible = '克拉玛依市财政局财务部'
          it.suggestedFormedDate = '2025-01-31'
          it.suggestedCategoryId = it.inputTitle.includes('会计') ? 3 : 1
          it.suggestedTags = ['移交', '2025']
          it.itemStatus = 'suggested'
        }
      })
      if (batch) batch.aiStatus = 'completed'
      mockAiTask = {
        ...mockAiTask,
        id: Date.now(),
        status: 'completed',
        completedCount: items.filter((i) => i.itemStatus === 'suggested').length,
      }
      resolve({ ...mockAiTask })
    }, 1200)
  })
}

/** 模拟确认条目字段 */
export function mockConfirmItem(itemId: number): PendingItem | null {
  const allItems = [...batch1Items, ...batch2Items]
  const item = allItems.find((i) => i.id === itemId)
  if (!item) return null
  item.itemStatus = 'pending_archive'
  item.confirmedTitle = item.suggestedTitle || item.inputTitle
  item.confirmedResponsible = item.suggestedResponsible || ''
  item.confirmedFormedDate = item.suggestedFormedDate || ''
  item.confirmedCategoryId = item.suggestedCategoryId || 1
  item.confirmedTags = item.suggestedTags || []
  return { ...item }
}

/** 模拟确认入库 */
export function mockArchiveItem(itemId: number): { archiveId: number; archiveNo: string; lifecycleStatus: string } | null {
  const allItems = [...batch1Items, ...batch2Items]
  const item = allItems.find((i) => i.id === itemId)
  if (!item) return null
  const archiveId = Date.now()
  const archiveNo = `ARC-${String(archiveId).slice(-6)}`
  const isElectronic = item.carrierStatus === 'electronic'
  item.archiveId = archiveId
  item.archiveNo = archiveNo
  item.lifecycleStatus = isElectronic ? 'normal' : 'pending_shelf'
  item.itemStatus = 'archived'
  return { archiveId, archiveNo, lifecycleStatus: item.lifecycleStatus }
}

/** 模拟确认上架 */
export function mockShelveBatch(batchId: number): PendingBatchDetail | null {
  const items = batchId === 1 ? batch1Items : batch2Items
  items.forEach((it) => {
    if (it.lifecycleStatus === 'pending_shelf') {
      it.lifecycleStatus = 'normal'
    }
  })
  const batch = mockPendingBatches.find((b) => b.id === batchId)
  if (batch) batch.status = 'shelved'
  return mockPendingBatchDetail(batchId)
}

// ── 档案管理 mock 数据 ──

export const mockArchiveRecords: ArchiveRecord[] = [
  {
    id: 1,
    archiveNo: 'ARC-000031',
    title: '2025 年度公开工作报告',
    categoryId: 1,
    categoryName: '文书档案',
    securityLevel: 0,
    openStatus: 'open',
    carrierStatus: 'electronic',
    lifecycleStatus: 'normal',
    responsibleText: '办公室',
    formedDate: '2025-12-30',
    tags: ['公开', '年度报告'],
    fondsId: 1,
    fondsName: '克拉玛依市交通局全宗',
    createdAt: '2026-06-10T10:00:00+08:00',
    updatedAt: '2026-06-10T10:00:00+08:00',
  },
  {
    id: 2,
    archiveNo: 'ARC-000096',
    title: '智慧城市平台建设报告',
    categoryId: 2,
    categoryName: '科技档案',
    securityLevel: 2,
    openStatus: 'closed',
    carrierStatus: 'paper_electronic',
    lifecycleStatus: 'normal',
    responsibleText: '技术部',
    formedDate: '2024-09-18',
    tags: ['智慧城市', '平台'],
    fondsId: 1,
    locationCode: '401-03-02-05',
    boxNo: 'BOX-128',
    createdAt: '2026-06-08T14:00:00+08:00',
    updatedAt: '2026-06-08T14:00:00+08:00',
  },
  {
    id: 3,
    archiveNo: 'ARC-000102',
    title: '设备采购验收材料',
    categoryId: 3,
    categoryName: '会计档案',
    securityLevel: 0,
    openStatus: 'closed',
    carrierStatus: 'paper',
    lifecycleStatus: 'pending_shelf',
    responsibleText: '财务部',
    formedDate: '2025-06-01',
    tags: ['采购', '验收'],
    fondsId: 1,
    locationCode: '401-02-01-08',
    boxNo: 'BOX-132',
    createdAt: '2026-06-05T09:00:00+08:00',
    updatedAt: '2026-06-05T09:00:00+08:00',
  },
  {
    id: 4,
    archiveNo: 'ARC-000045',
    title: '2024 年度人事调动记录',
    categoryId: 5,
    categoryName: '人事档案',
    securityLevel: 1,
    openStatus: 'closed',
    carrierStatus: 'paper',
    lifecycleStatus: 'normal',
    responsibleText: '人事处',
    formedDate: '2024-12-31',
    tags: ['人事', '调动'],
    fondsId: 1,
    locationCode: '401-01-03-02',
    boxNo: 'BOX-056',
    createdAt: '2026-05-20T11:00:00+08:00',
    updatedAt: '2026-05-20T11:00:00+08:00',
  },
  {
    id: 5,
    archiveNo: 'ARC-000078',
    title: '2025 年城市宣传片素材',
    categoryId: 4,
    categoryName: '音像档案',
    securityLevel: 0,
    openStatus: 'open',
    carrierStatus: 'electronic',
    lifecycleStatus: 'normal',
    responsibleText: '宣传部',
    formedDate: '2025-08-15',
    tags: ['宣传片', '城市'],
    fondsId: 1,
    createdAt: '2026-06-01T16:00:00+08:00',
    updatedAt: '2026-06-01T16:00:00+08:00',
  },
]

/** 模拟档案详情 */
export function mockArchiveDetail(archiveId: number): ArchiveDetail | null {
  const record = mockArchiveRecords.find((a) => a.id === archiveId)
  if (!record) return null
  return {
    ...record,
    summary: `${record.title}的详细摘要信息。`,
    allowDigitization: true,
    retentionPeriod: record.categoryId === 5 ? 'permanent' : '30y',
    loanStatus: 'available',
    conditionStatus: 'normal',
    files: record.carrierStatus !== 'paper'
      ? [{
          id: archiveId * 100 + 1,
          originalFilename: `${record.archiveNo}.pdf`,
          fileSize: 524288,
          fileStatus: 'normal',
          mimeType: 'application/pdf',
          createdAt: record.createdAt,
        }]
      : [],
    changeLogs: [],
    pendingApprovals: [],
  }
}
