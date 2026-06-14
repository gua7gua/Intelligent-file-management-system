import type { PageData, PageParams } from './api'
import type {
  AppraisalBatchStatusValue,
  AppraisalResultValue,
  ArchiveLifecycleStatusValue,
} from './enums'
import type { RetentionPeriodValue } from './transfer'

/** 鉴定批次查询参数（§14.1） */
export interface AppraisalBatchParams extends PageParams {
  status?: AppraisalBatchStatusValue | ''
  categoryId?: number
  formedYearStart?: number
  formedYearEnd?: number
}

/** 创建鉴定批次（§14.2） */
export interface AppraisalBatchCreateData {
  batchName: string
  categoryId?: number
  formedYearStart?: number
  formedYearEnd?: number
}

/** 鉴定明细（命中档案 + 鉴定结论） */
export interface AppraisalItem {
  id?: number
  archiveId: number
  archiveNo: string
  title: string
  categoryName: string
  retentionPeriod: RetentionPeriodValue
  retentionUntil: string
  currentLifecycleStatus: ArchiveLifecycleStatusValue
  appraisalResult: AppraisalResultValue | ''
  newRetentionPeriod?: RetentionPeriodValue | ''
  newRetentionUntil?: string
  opinion?: string
  appraisedBy?: number
  appraisedAt?: string
}

/** 鉴定批次摘要（列表行） */
export interface AppraisalBatch {
  id: number
  batchNo: string
  batchName: string
  categoryId?: number
  categoryName?: string
  formedYearStart?: number
  formedYearEnd?: number
  status: AppraisalBatchStatusValue
  completedAt?: string
  hitCount: number
  destroyCount?: number
  extendCount?: number
  createdAt: string
  generatedListId?: number
  generatedListNo?: string
}

/** 鉴定批次详情（§14.3） */
export interface AppraisalBatchDetail extends AppraisalBatch {
  items: AppraisalItem[]
}

/** 保存鉴定明细请求（§14.4） */
export interface AppraisalItemsSaveData {
  items: Array<{
    archiveId: number
    appraisalResult: AppraisalResultValue
    newRetentionPeriod?: RetentionPeriodValue | null
    newRetentionUntil?: string | null
    opinion?: string
  }>
}

export type AppraisalBatchPage = PageData<AppraisalBatch>
