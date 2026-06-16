// src/types/reception.ts
import type { CarrierStatusValue } from './transfer'

/** 前台验收批次 */
export interface ReceptionBatch {
  id: number
  batchNo: string
  title: string
  sourceType: 'transfer' | 'collection' | 'compilation'
  status: string
  statusText: string
  organizationId: number
  organizationName: string
  departmentName: string
  contactName: string
  contactPhone: string
  expectedTransferDate: string
  submittedAt: string
  itemCount: number
  signatureStatus: string
  acceptanceNote?: string
  acceptedBy?: number
  acceptedAt?: string
  createdAt: string
  updatedAt: string
}

/** 验收条目 */
export interface ReceptionItem {
  id: number
  batchId: number
  seqNo: number
  title: string
  carrierStatus: CarrierStatusValue
  expectedFilename: string
  paperCheckStatus: 'pending' | 'passed' | 'failed'
  fileMatchStatus: 'none' | 'matched' | 'unmatched' | 'duplicate' | 'missing' | 'failed'
  result: 'pending' | 'accepted' | 'rejected'
  acceptanceNote: string
  rejectReason: string
  createdAt: string
  updatedAt: string
}

/** 暂存文件 */
export interface StagingFile {
  fileId: number
  batchId: number
  originalFilename: string
  fileSize: number
  sha256: string
  scanResult: 'safe' | 'unsafe' | 'error'
  matchStatus: 'staging' | 'matched' | 'unmatched' | 'duplicate' | 'deleted'
  matchedItemId?: number
  uploadBatchNo: string
  createdAt: string
}

/** 匹配汇总 */
export interface MatchSummary {
  matched: number
  unmatched: number
  duplicate: number
  failed: number
  missingItems: number[]
}

/** 文件上传结果 */
export interface UploadResult {
  uploadBatchNo: string
  files: StagingFile[]
  matchSummary: MatchSummary
}

/** 批次详情（含条目和暂存文件） */
export interface BatchDetail {
  batch: ReceptionBatch
  items: ReceptionItem[]
  stagingFiles: StagingFile[]
  matchSummary: MatchSummary
}

/** 批次查询参数 */
export interface ReceptionBatchParams {
  sourceType?: 'transfer' | 'collection'
  status?: string
  keyword?: string
  pageNo?: number
  pageSize?: number
}

/** 条目验收请求 */
export interface ItemAcceptanceData {
  result: 'accepted' | 'rejected'
  acceptanceNote?: string
  rejectReason?: string
}

/** 手工匹配请求 */
export interface ManualMatchData {
  itemId: number
  matchStatus: 'matched'
  note: string
}

/** 完成批次验收请求 */
export interface CompleteBatchData {
  acceptanceNote?: string
}
