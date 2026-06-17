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
          const text = `${b.batchNo} ${b.title} ${b.contactName} ${b.contactPhone}`.toLowerCase()
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
  return request.post(`/admin/collections/${batchId}/schedule`, {
    ...data,
    // datetime-local 控件产出 "YYYY-MM-DDTHH:mm"，后端 OffsetDateTime 需要带时区的 ISO 8601。
    // 这里补齐秒与本机时区偏移（如 +08:00），避免反序列化 400。
    scheduledReceiveAt: toOffsetDateTime(data.scheduledReceiveAt),
  })
}

/**
 * 把 datetime-local 的 "YYYY-MM-DDTHH:mm" 或已带时区的字符串规范成
 * 后端 OffsetDateTime 可解析的 "YYYY-MM-DDTHH:mm:ss±HH:mm"。
 * 入参已是合法 ISO（带 Z/offset）时直接返回原值。
 */
function toOffsetDateTime(value: string | undefined): string | undefined {
  if (!value) return value
  // 已含时区（Z 或 ±HH:MM）：交给后端，不动。
  if (/[zZ]|[+-]\d{2}:\d{2}$/.test(value)) return value
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return value
  const pad = (n: number) => String(n).padStart(2, '0')
  const offset = -d.getTimezoneOffset() // 分钟，东八区为 +480
  const sign = offset >= 0 ? '+' : '-'
  const abs = Math.abs(offset)
  return (
    `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}` +
    `T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}` +
    `${sign}${pad(Math.floor(abs / 60))}:${pad(abs % 60)}`
  )
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
