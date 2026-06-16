// src/api/reception.ts
import request from './request'
import type { PageData, PageParams } from '@/types/api'
import type {
  ReceptionBatch,
  ReceptionBatchParams,
  BatchDetail,
  ReceptionItem,
  StagingFile,
  UploadResult,
  ManualMatchData,
  ItemAcceptanceData,
  CompleteBatchData,
} from '@/types/reception'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/**
 * 后端 GET /admin/reception/batches/{id} 的扁平响应（批次字段 + items[]）。
 * 详情接口未单独返回 stagingFiles / matchSummary（条目匹配状态已落在 item.fileMatchStatus）。
 */
interface RawBatchDetail extends Partial<ReceptionBatch> {
  items?: RawReceptionItem[]
  stagingFiles?: StagingFile[]
  matchSummary?: BatchDetail['matchSummary']
}

interface RawReceptionItem {
  id: number
  batchId: number
  itemNo?: number
  seqNo?: number
  inputTitle?: string
  title?: string
  status?: string
  result?: ReceptionItem['result']
  carrierStatus?: ReceptionItem['carrierStatus']
  expectedFilename?: string
  fileMatchStatus?: ReceptionItem['fileMatchStatus']
  paperCheckStatus?: ReceptionItem['paperCheckStatus']
  acceptanceNote?: string
  rejectReason?: string
  createdAt?: string
  updatedAt?: string
}

/**
 * 将后端扁平 IntakeBatchResponse 适配为前端 BatchDetail（{batch, items, stagingFiles, matchSummary}）。
 * - 后端 items 用 itemNo/inputTitle/status，前端类型用 seqNo/title/result，做映射；
 * - 后端 status: pending_acceptance/accepted/rejected → 前端 result: pending/accepted/rejected；
 * - paperCheckStatus 后端未存，按 fileMatchStatus / carrierStatus 给一个合理初值（pending）；
 * - stagingFiles / matchSummary 后端详情未返回时给空数组 / 零值，UI 展示「点击展开 / 未上传」。
 */
function normalizeBatchDetail(raw: RawBatchDetail): BatchDetail {
  const items: ReceptionItem[] = (raw.items ?? []).map((it) => {
    const result: ReceptionItem['result'] =
      it.result ??
      (it.status === 'accepted' || it.status === 'pending_archive' || it.status === 'archived' || it.status === 'shelved'
        ? 'accepted'
        : it.status === 'rejected'
          ? 'rejected'
          : 'pending')
    return {
      id: it.id,
      batchId: it.batchId,
      seqNo: it.seqNo ?? it.itemNo ?? 0,
      title: it.title ?? it.inputTitle ?? '',
      carrierStatus: (it.carrierStatus ?? 'electronic') as ReceptionItem['carrierStatus'],
      expectedFilename: it.expectedFilename ?? '',
      paperCheckStatus: it.paperCheckStatus ?? 'pending',
      fileMatchStatus: (it.fileMatchStatus ?? 'none') as ReceptionItem['fileMatchStatus'],
      result,
      acceptanceNote: it.acceptanceNote ?? '',
      rejectReason: it.rejectReason ?? '',
      createdAt: it.createdAt ?? '',
      updatedAt: it.updatedAt ?? '',
    }
  })
  const stagingFiles = raw.stagingFiles ?? []
  const matchSummary =
    raw.matchSummary ?? {
      matched: items.filter((i) => i.fileMatchStatus === 'matched').length,
      unmatched: items.filter((i) => i.fileMatchStatus === 'unmatched').length,
      duplicate: items.filter((i) => i.fileMatchStatus === 'duplicate').length,
      failed: items.filter((i) => i.fileMatchStatus === 'failed').length,
      missingItems: items.filter((i) => i.fileMatchStatus === 'missing').map((i) => i.id),
    }
  // 以 batch 字段为主体，剥离 items/stagingFiles/matchSummary 后作为 batch 返回。
  const { items: _items, stagingFiles: _sf, matchSummary: _ms, ...batchFields } = raw
  return {
    batch: batchFields as ReceptionBatch,
    items,
    stagingFiles,
    matchSummary,
  }
}

/** 查询待验收批次 */
export function getReceptionBatches(
  params?: ReceptionBatchParams & PageParams,
): Promise<PageData<ReceptionBatch>> {
  if (USE_MOCK) {
    return import('@/mock/modules/reception').then((m) => {
      const all = m.mockReceptionBatches.filter((b) => {
        if (params?.sourceType && b.sourceType !== params.sourceType) return false
        if (params?.status && b.status !== params.status) return false
        if (params?.keyword) {
          const kw = params.keyword.toLowerCase()
          const text = `${b.batchNo} ${b.title} ${b.organizationName} ${b.contactName}`.toLowerCase()
          if (!text.includes(kw)) return false
        }
        return true
      })
      return {
        records: all,
        pageNo: params?.pageNo ?? 1,
        pageSize: params?.pageSize ?? 20,
        total: all.length,
        hasNext: false,
      }
    })
  }
  return request.get('/admin/reception/batches', { params })
}

/** 获取验收详情 */
export function getReceptionBatchDetail(batchId: number): Promise<BatchDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/reception').then((m) => {
      const detail = m.mockBatchDetail(batchId)
      if (!detail) throw new Error('批次不存在')
      return detail
    })
  }
  return request
    .get<RawBatchDetail>(`/admin/reception/batches/${batchId}`)
    .then((raw) => normalizeBatchDetail(raw))
}

/** 上传暂存电子文件 */
export function uploadStagingFiles(
  batchId: number,
  files: File[],
): Promise<UploadResult> {
  if (USE_MOCK) {
    return import('@/mock/modules/reception').then((m) => m.mockUploadResult())
  }
  const formData = new FormData()
  files.forEach((file) => formData.append('files', file))
  return request.post(`/admin/reception/batches/${batchId}/staging-files`, formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 手工匹配暂存文件 */
export function manualMatchFile(fileId: number, data: ManualMatchData): Promise<unknown> {
  if (USE_MOCK) {
    return Promise.resolve({ success: true })
  }
  return request.put(`/admin/reception/staging-files/${fileId}/match`, data)
}

/** 更新条目验收结果 */
export function updateItemAcceptance(
  itemId: number,
  data: ItemAcceptanceData,
): Promise<unknown> {
  if (USE_MOCK) {
    return Promise.resolve({ success: true })
  }
  return request.put(`/admin/reception/items/${itemId}/acceptance`, data)
}

/** 完成批次验收 */
export function completeBatchAcceptance(
  batchId: number,
  data?: CompleteBatchData,
): Promise<unknown> {
  if (USE_MOCK) {
    return Promise.resolve({ success: true })
  }
  return request.post(`/admin/reception/batches/${batchId}/complete`, data)
}

/** 导出接收回执（PDF blob） */
export function exportReceipt(batchId: number): Promise<Blob> {
  if (USE_MOCK) {
    const blob = new Blob(['Mock 回执 PDF 内容'], { type: 'application/pdf' })
    return Promise.resolve(blob)
  }
  return request.get(`/admin/reception/batches/${batchId}/receipt`, {
    responseType: 'blob',
  }) as Promise<Blob>
}
