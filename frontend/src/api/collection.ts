// src/api/collection.ts
import request from './request'
import type { PageData, PageParams } from '@/types/api'
import type {
  CollectionBatch,
  CollectionParams,
  ScheduleData,
  RejectData,
} from '@/types/collection'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询征集批次 */
export function getCollections(
  params?: CollectionParams & PageParams,
): Promise<PageData<CollectionBatch>> {
  if (USE_MOCK) {
    return import('@/mock/modules/collection').then((m) => {
      const all = m.mockCollectionBatches.filter((b) => {
        if (params?.status && b.status !== params.status) return false
        if (params?.keyword) {
          const kw = params.keyword.toLowerCase()
          const text = `${b.batchNo} ${b.title} ${b.donorName} ${b.donorPhone}`.toLowerCase()
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
  return request.get('/admin/collections', { params })
}

/** 约定到馆时间 */
export function scheduleCollection(
  batchId: number,
  data: ScheduleData,
): Promise<CollectionBatch> {
  if (USE_MOCK) {
    return import('@/mock/modules/collection').then((m) => {
      const batch = m.mockCollectionBatches.find((b) => b.id === batchId)
      if (!batch) throw new Error('批次不存在')
      return {
        ...batch,
        status: 'pending_receive',
        statusText: '待接收',
        scheduledReceiveAt: data.scheduledReceiveAt,
        contactNote: data.contactNote,
      }
    })
  }
  return request.post(`/admin/collections/${batchId}/schedule`, data)
}

/** 拒绝征集 */
export function rejectCollection(
  batchId: number,
  data: RejectData,
): Promise<CollectionBatch> {
  if (USE_MOCK) {
    return import('@/mock/modules/collection').then((m) => {
      const batch = m.mockCollectionBatches.find((b) => b.id === batchId)
      if (!batch) throw new Error('批次不存在')
      return {
        ...batch,
        status: 'rejected',
        statusText: '已拒绝',
        rejectReason: data.rejectReason,
      }
    })
  }
  return request.post(`/admin/collections/${batchId}/reject`, data)
}
