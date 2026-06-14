// src/api/appraisal.ts
import request from './request'
import type { PageData, PageParams } from '@/types/api'
import type {
  AppraisalBatch,
  AppraisalBatchCreateData,
  AppraisalBatchDetail,
  AppraisalBatchParams,
  AppraisalItemsSaveData,
} from '@/types/appraisal'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询鉴定批次（§14.1） */
export function getAppraisalBatches(
  params?: AppraisalBatchParams & PageParams,
): Promise<PageData<AppraisalBatch>> {
  if (USE_MOCK) {
    return import('@/mock/modules/appraisal').then((m) => m.mockAppraisalBatches(params))
  }
  return request.get('/admin/appraisal-batches', { params })
}

/** 创建鉴定批次（§14.2） */
export function createAppraisalBatch(data: AppraisalBatchCreateData): Promise<AppraisalBatchDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/appraisal').then((m) => m.mockCreateAppraisalBatch(data))
  }
  return request.post('/admin/appraisal-batches', data)
}

/** 获取鉴定批次详情（§14.3） */
export function getAppraisalBatchDetail(batchId: number): Promise<AppraisalBatchDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/appraisal').then((m) => m.mockAppraisalBatchDetail(batchId))
  }
  return request.get(`/admin/appraisal-batches/${batchId}`)
}

/** 保存鉴定明细（§14.4） */
export function saveAppraisalItems(
  batchId: number,
  data: AppraisalItemsSaveData,
): Promise<AppraisalBatchDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/appraisal').then((m) => m.mockSaveAppraisalItems(batchId, data))
  }
  return request.put(`/admin/appraisal-batches/${batchId}/items`, data)
}

/** 完成鉴定（§14.5） */
export function completeAppraisalBatch(batchId: number): Promise<AppraisalBatchDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/appraisal').then((m) => m.mockCompleteAppraisalBatch(batchId))
  }
  return request.post(`/admin/appraisal-batches/${batchId}/complete`)
}
