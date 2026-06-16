// src/types/collection.ts
import type { ReceptionBatch } from './reception'

/** 征集批次（扩展验收批次，联系人字段继承自 ReceptionBatch：contactName/contactPhone） */
export interface CollectionBatch extends ReceptionBatch {
  sourceType: 'collection'
  agreementAcceptedAt?: string
  contactNote?: string
  scheduledReceiveAt?: string
  rejectReason?: string
}

/** 征集批次查询参数 */
export interface CollectionParams {
  status?: string
  keyword?: string
  contactPhone?: string
  pageNo?: number
  pageSize?: number
}

/** 约定到馆请求 */
export interface ScheduleData {
  scheduledReceiveAt: string
  contactNote: string
}

/** 拒绝征集请求 */
export interface RejectData {
  rejectReason: string
}
