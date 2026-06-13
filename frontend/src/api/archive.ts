// src/api/archive.ts
import request from './request'
import type { PageData, PageParams } from '@/types/api'
import type {
  PendingBatch,
  PendingBatchDetail,
  PendingBatchParams,
  PendingItem,
  AiTask,
  ConfirmItemData,
  ArchiveItemData,
  ShelveBatchData,
  ArchiveRecord,
  ArchiveDetail,
  ArchiveQueryParams,
  ArchiveEditData,
  SecurityAdjustData,
  OpenAdjustData,
} from '@/types/archive'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

// ── 待入库相关 ──

/** 查询待入库批次 */
export function getPendingBatches(
  params?: PendingBatchParams & PageParams,
): Promise<PageData<PendingBatch>> {
  if (USE_MOCK) {
    return import('@/mock/modules/archive').then((m) => {
      let all = [...m.mockPendingBatches]
      if (params?.sourceType) all = all.filter((b) => b.sourceType === params.sourceType)
      if (params?.aiStatus) all = all.filter((b) => b.aiStatus === params.aiStatus)
      if (params?.keyword) {
        const kw = params.keyword.toLowerCase()
        all = all.filter((b) => `${b.batchNo} ${b.title}`.toLowerCase().includes(kw))
      }
      return {
        records: all,
        pageNo: params?.pageNo ?? 1,
        pageSize: params?.pageSize ?? 20,
        total: all.length,
        hasNext: false,
      }
    })
  }
  return request.get('/admin/pending-archive/batches', { params })
}

/** 获取入库批次详情 */
export function getPendingBatchDetail(batchId: number): Promise<PendingBatchDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/archive').then((m) => {
      const detail = m.mockPendingBatchDetail(batchId)
      if (!detail) throw new Error('批次不存在')
      return detail
    })
  }
  return request.get(`/admin/pending-archive/batches/${batchId}`)
}

/** 启动 AI 补全 */
export function startAiCompletion(batchId: number): Promise<AiTask> {
  if (USE_MOCK) {
    return import('@/mock/modules/archive').then((m) => m.mockStartAiCompletion(batchId))
  }
  return request.post(`/admin/pending-archive/batches/${batchId}/ai-completion`)
}

/** 查询 AI 补全任务 */
export function getAiTask(taskId: number): Promise<AiTask> {
  if (USE_MOCK) {
    return import('@/mock/modules/archive').then((m) => ({ ...m.mockAiTask }))
  }
  return request.get(`/admin/ai-tasks/${taskId}`)
}

/** 重试失败 AI 批次 */
export function retryAiTask(taskId: number): Promise<AiTask> {
  if (USE_MOCK) {
    return import('@/mock/modules/archive').then((m) => ({ ...m.mockAiTask }))
  }
  return request.post(`/admin/ai-tasks/${taskId}/retry-failed`)
}

/** 确认条目入库字段 */
export function confirmItem(itemId: number, data: ConfirmItemData): Promise<PendingItem> {
  if (USE_MOCK) {
    return import('@/mock/modules/archive').then((m) => {
      const result = m.mockConfirmItem(itemId)
      if (!result) throw new Error('条目不存在')
      return result
    })
  }
  return request.put(`/admin/pending-archive/items/${itemId}/confirmation`, data)
}

/** 确认入库 */
export function archiveItem(
  itemId: number,
  data: ArchiveItemData,
): Promise<{ archiveId: number; archiveNo: string; lifecycleStatus: string }> {
  if (USE_MOCK) {
    return import('@/mock/modules/archive').then((m) => {
      const result = m.mockArchiveItem(itemId)
      if (!result) throw new Error('条目不存在')
      return result
    })
  }
  return request.post(`/admin/pending-archive/items/${itemId}/archive`, data)
}

/** 批次确认上架 */
export function shelveBatch(
  batchId: number,
  data?: ShelveBatchData,
): Promise<PendingBatchDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/archive').then((m) => {
      const result = m.mockShelveBatch(batchId)
      if (!result) throw new Error('批次不存在')
      return result
    })
  }
  return request.post(`/admin/pending-archive/batches/${batchId}/shelve`, data)
}

// ── 档案管理相关 ──

/** 管理端查询档案 */
export function getArchives(
  params?: ArchiveQueryParams & PageParams,
): Promise<PageData<ArchiveRecord>> {
  if (USE_MOCK) {
    return import('@/mock/modules/archive').then((m) => {
      let all = [...m.mockArchiveRecords]
      if (params?.keyword) {
        const kw = params.keyword.toLowerCase()
        all = all.filter(
          (a) =>
            `${a.archiveNo} ${a.title} ${a.responsibleText} ${a.tags.join(' ')}`.toLowerCase().includes(kw),
        )
      }
      if (params?.categoryId) all = all.filter((a) => a.categoryId === params.categoryId)
      if (params?.securityLevel !== undefined && params?.securityLevel !== null) {
        all = all.filter((a) => a.securityLevel === params.securityLevel)
      }
      if (params?.openStatus) all = all.filter((a) => a.openStatus === params.openStatus)
      if (params?.carrierStatus) all = all.filter((a) => a.carrierStatus === params.carrierStatus)
      if (params?.lifecycleStatus) all = all.filter((a) => a.lifecycleStatus === params.lifecycleStatus)
      return {
        records: all,
        pageNo: params?.pageNo ?? 1,
        pageSize: params?.pageSize ?? 20,
        total: all.length,
        hasNext: false,
      }
    })
  }
  return request.get('/admin/archives', { params })
}

/** 获取档案详情 */
export function getArchiveDetail(archiveId: number): Promise<ArchiveDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/archive').then((m) => {
      const detail = m.mockArchiveDetail(archiveId)
      if (!detail) throw new Error('档案不存在')
      return detail
    })
  }
  return request.get(`/admin/archives/${archiveId}`)
}

/** 编辑非受保护元数据 */
export function updateArchive(
  archiveId: number,
  data: ArchiveEditData,
): Promise<ArchiveDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/archive').then((m) => {
      const detail = m.mockArchiveDetail(archiveId)
      if (!detail) throw new Error('档案不存在')
      return { ...detail, ...data, tagNames: undefined, tags: data.tagNames } as unknown as ArchiveDetail
    })
  }
  return request.put(`/admin/archives/${archiveId}`, data)
}

/** 发起密级调整审批 */
export function submitSecurityAdjust(
  archiveId: number,
  data: SecurityAdjustData,
): Promise<unknown> {
  if (USE_MOCK) {
    return Promise.resolve({ success: true })
  }
  return request.post(`/admin/archives/${archiveId}/security-adjustments`, data)
}

/** 发起开放调整审批 */
export function submitOpenAdjust(
  archiveId: number,
  data: OpenAdjustData,
): Promise<unknown> {
  if (USE_MOCK) {
    return Promise.resolve({ success: true })
  }
  return request.post(`/admin/archives/${archiveId}/open-adjustments`, data)
}

/** 档案文件预览 */
export function previewArchiveFile(fileId: number): Promise<string> {
  if (USE_MOCK) {
    return Promise.resolve('# 预览内容\n\n这是模拟的文件预览。')
  }
  return request.get(`/admin/archive-files/${fileId}/preview`)
}

/** 档案文件下载 */
export function downloadArchiveFile(fileId: number): Promise<Blob> {
  if (USE_MOCK) {
    const blob = new Blob(['Mock 档案文件内容'], { type: 'application/pdf' })
    return Promise.resolve(blob)
  }
  return request.get(`/admin/archive-files/${fileId}/download`, {
    responseType: 'blob',
  }) as Promise<Blob>
}
