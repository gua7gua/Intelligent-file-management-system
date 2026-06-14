// src/mock/modules/appraisal.ts
import type {
  AppraisalBatch,
  AppraisalBatchCreateData,
  AppraisalBatchDetail,
  AppraisalBatchParams,
  AppraisalItem,
  AppraisalItemsSaveData,
} from '@/types/appraisal'
import { seedGeneratedList } from './destruction'

const baseItem = (
  archiveId: number,
  archiveNo: string,
  title: string,
  result: AppraisalItem['appraisalResult'],
): AppraisalItem => ({
  archiveId,
  archiveNo,
  title,
  categoryName: '会计档案',
  retentionPeriod: '10y',
  retentionUntil: '2025-12-31',
  currentLifecycleStatus: 'normal',
  appraisalResult: result,
})

// 批次 1：草稿，含未处理 + 已鉴定项
const batch1Items: AppraisalItem[] = [
  {
    ...baseItem(201, 'ARC-000201', '2015 年 1 月会计凭证', 'destroy'),
    opinion: '已满十年且无继续保存价值',
  },
  {
    ...baseItem(202, 'ARC-000202', '2015 年 2 月会计凭证', 'extend'),
    newRetentionPeriod: '30y',
    newRetentionUntil: '2045-12-31',
    opinion: '仍有查考价值，延长保管',
  },
  baseItem(203, 'ARC-000203', '2015 年 3 月会计凭证', ''),
  baseItem(204, 'ARC-000204', '2015 年 4 月会计凭证', ''),
]

// 批次 2：已完成，已生成销毁清册（id=13）
const batch2Items: AppraisalItem[] = [
  { ...baseItem(301, 'ARC-000301', '2014 年基建档案', 'destroy'), opinion: '到期无保存价值' },
  { ...baseItem(302, 'ARC-000302', '2014 年设备档案', 'extend'), newRetentionPeriod: 'permanent', newRetentionUntil: '2099-12-31', opinion: '设备仍在用' },
]

// 批次 3：草稿，部分命中
const batch3Items: AppraisalItem[] = [
  baseItem(401, 'ARC-000401', '2013 年人事档案', 'destroy'),
  baseItem(402, 'ARC-000402', '2013 年会议纪要', ''),
]

const batches: AppraisalBatchDetail[] = [
  {
    id: 1,
    batchNo: 'APP-001',
    batchName: '2015 年会计档案到期鉴定',
    categoryId: 3,
    categoryName: '会计档案',
    formedYearStart: 2015,
    formedYearEnd: 2015,
    status: 'draft',
    hitCount: 4,
    destroyCount: 1,
    extendCount: 1,
    createdAt: '2026-06-15T09:00:00+08:00',
    items: batch1Items,
  },
  {
    id: 2,
    batchNo: 'APP-002',
    batchName: '2014 年基建与设备档案鉴定',
    categoryId: 4,
    categoryName: '基建档案',
    formedYearStart: 2014,
    formedYearEnd: 2014,
    status: 'completed',
    hitCount: 2,
    destroyCount: 1,
    extendCount: 1,
    completedAt: '2026-06-14T16:00:00+08:00',
    createdAt: '2026-06-13T10:00:00+08:00',
    items: batch2Items,
    generatedListId: 13,
    generatedListNo: 'DES-013',
  },
  {
    id: 3,
    batchNo: 'APP-003',
    batchName: '2013 年到期档案鉴定',
    categoryId: 5,
    categoryName: '人事档案',
    formedYearStart: 2013,
    formedYearEnd: 2013,
    status: 'draft',
    hitCount: 2,
    destroyCount: 1,
    extendCount: 0,
    createdAt: '2026-06-15T14:00:00+08:00',
    items: batch3Items,
  },
]

export function mockAppraisalBatches(params?: AppraisalBatchParams): {
  records: AppraisalBatch[]
  pageNo: number
  pageSize: number
  total: number
  hasNext: boolean
} {
  let list = batches.slice()
  if (params?.status) list = list.filter((b) => b.status === params.status)
  const pageNo = params?.pageNo ?? 1
  const pageSize = params?.pageSize ?? 20
  const records = list.map(({ items, generatedListId, generatedListNo, ...rest }) => ({
    ...rest,
    generatedListId,
    generatedListNo,
  }))
  return {
    records,
    pageNo,
    pageSize,
    total: list.length,
    hasNext: false,
  }
}

export function mockAppraisalBatchDetail(id: number): AppraisalBatchDetail {
  const found = batches.find((b) => b.id === id)
  if (!found) throw new Error('鉴定批次不存在')
  return JSON.parse(JSON.stringify(found))
}

let nextBatchNo = 4
export function mockCreateAppraisalBatch(data: AppraisalBatchCreateData): AppraisalBatchDetail {
  const id = nextBatchNo++
  const detail: AppraisalBatchDetail = {
    id,
    batchNo: `APP-00${id}`,
    batchName: data.batchName,
    categoryId: data.categoryId,
    categoryName: data.categoryId === 3 ? '会计档案' : '未分类',
    formedYearStart: data.formedYearStart,
    formedYearEnd: data.formedYearEnd,
    status: 'draft',
    hitCount: 3,
    destroyCount: 0,
    extendCount: 0,
    createdAt: '2026-06-15T15:00:00+08:00',
    items: [
      baseItem(501, 'ARC-000501', `${data.formedYearStart ?? ''} 年待鉴定档案一`, ''),
      baseItem(502, 'ARC-000502', `${data.formedYearStart ?? ''} 年待鉴定档案二`, ''),
      baseItem(503, 'ARC-000503', `${data.formedYearStart ?? ''} 年待鉴定档案三`, ''),
    ],
  }
  return detail
}

export function mockSaveAppraisalItems(id: number, data: AppraisalItemsSaveData): AppraisalBatchDetail {
  const detail = mockAppraisalBatchDetail(id)
  const map = new Map(data.items.map((i) => [i.archiveId, i]))
  detail.items = detail.items.map((it) => {
    const patch = map.get(it.archiveId)
    if (!patch) return it
    return {
      ...it,
      appraisalResult: patch.appraisalResult,
      newRetentionPeriod: patch.newRetentionPeriod ?? it.newRetentionPeriod,
      newRetentionUntil: patch.newRetentionUntil ?? it.newRetentionUntil,
      opinion: patch.opinion ?? it.opinion,
    }
  })
  detail.destroyCount = detail.items.filter((i) => i.appraisalResult === 'destroy').length
  detail.extendCount = detail.items.filter((i) => i.appraisalResult === 'extend').length
  return detail
}

export function mockCompleteAppraisalBatch(id: number): AppraisalBatchDetail {
  const detail = mockAppraisalBatchDetail(id)
  detail.status = 'completed'
  detail.completedAt = '2026-06-15T17:00:00+08:00'
  const destroyItems = detail.items.filter((i) => i.appraisalResult === 'destroy')
  const generated = seedGeneratedList(detail.id, detail.batchNo, destroyItems)
  detail.generatedListId = generated.id
  detail.generatedListNo = generated.listNo
  return detail
}
