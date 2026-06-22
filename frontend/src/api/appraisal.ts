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

/** 鉴定工作台顶部统计（真实聚合，替代前端硬编码假值） */
export interface AppraisalStats {
  expiringCount: number
  pendingDestructionCount: number
  generatedListCount: number
}
export function getAppraisalStats(): Promise<AppraisalStats> {
  if (USE_MOCK) {
    return Promise.resolve({ expiringCount: 0, pendingDestructionCount: 0, generatedListCount: 0 })
  }
  return request.get('/admin/appraisal-batches/stats')
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

/** 删除未完成的鉴定批次（§14.3.1，仅 draft 可删） */
export function deleteAppraisalBatch(batchId: number): Promise<void> {
  if (USE_MOCK) return Promise.resolve()
  return request.delete(`/admin/appraisal-batches/${batchId}`)
}
