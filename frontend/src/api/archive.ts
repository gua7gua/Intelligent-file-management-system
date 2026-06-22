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
  return request.get('/admin/pending-archive/batches', { params }).then((res) => {
    // 后端列表字段为 latestAiTaskStatus，前端模型使用 aiStatus；统一归一化
    const records = (res.records ?? []).map((b: PendingBatch & { latestAiTaskStatus?: string }) => ({
      ...b,
      aiStatus: (b.aiStatus || b.latestAiTaskStatus || 'not_started') as PendingBatch['aiStatus'],
    }))
    return { ...res, records }
  })
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
  return request.get(`/admin/pending-archive/batches/${batchId}`).then((res) => normalizePendingBatchDetail(res, batchId))
}

/**
 * 把后端 PendingBatchDetailResponse 字段映射到前端 PendingBatchDetail 类型。
 * 后端条目字段 status/itemNo/fileMatchStatus/confirmedResponsibleText，
 * 前端模型用 itemStatus/seqNo/matchStatus/confirmedResponsible。
 */
function normalizePendingBatchDetail(raw: Record<string, unknown>, batchId: number): PendingBatchDetail {
  const rawItems = (raw.items ?? []) as Record<string, unknown>[]
  const items: PendingItem[] = rawItems.map((it) => ({
    id: it.id as number,
    batchId,
    seqNo: (it.itemNo as number) ?? 0,
    inputTitle: (it.inputTitle as string) ?? '',
    expectedFilename: '',
    carrierStatus: (it.carrierStatus as PendingItem['carrierStatus']) ?? 'electronic',
    itemStatus: (it.status as PendingItem['itemStatus']) ?? 'accepted',
    matchStatus: (it.fileMatchStatus as string) ?? 'none',
    securityLevel: (it.securityLevel as number) ?? 0,
    retentionPeriod: (it.retentionPeriod as string) ?? '',
    openStatus: (it.openStatus as PendingItem['openStatus']) ?? 'closed',
    allowDigitization: (it.allowDigitization as boolean) ?? false,
    suggestedTitle: it.suggestedTitle as string | undefined,
    suggestedResponsible: it.suggestedResponsible as string | undefined,
    suggestedFormedDate: it.suggestedFormedDate as string | undefined,
    suggestedCategoryId: it.suggestedCategoryId as number | undefined,
    suggestedTags: it.suggestedTags as string[] | undefined,
    confirmedTitle: it.confirmedTitle as string | undefined,
    confirmedResponsible: (it.confirmedResponsibleText as string) ?? (it.confirmedResponsible as string | undefined),
    confirmedFormedDate: it.confirmedFormedDate as string | undefined,
    confirmedCategoryId: it.confirmedCategoryId as number | undefined,
    confirmedTags: it.confirmedTags as string[] | undefined,
    archiveId: (it.generatedArchiveId as number) ?? (it.archiveId as number | undefined),
    archiveNo: it.archiveNo as string | undefined,
    lifecycleStatus: it.lifecycleStatus as string | undefined,
    archiveFondsId: (it.archiveFondsId as number | undefined) ?? undefined,
    archiveBoxId: (it.archiveBoxId as number | undefined) ?? undefined,
    createdAt: (it.createdAt as string) ?? '',
    updatedAt: (it.updatedAt as string) ?? '',
  }))
  // 详情接口现已回填 latestAiTaskStatus（与列表口径一致）；统一归一化为 aiStatus，
  // 避免 activeBatch.aiStatus 为 undefined 导致详情视图 AI 标签回退为「未开始」。
  const latestAiTaskStatus = (raw.latestAiTaskStatus as string | undefined) ?? (raw.aiStatus as string | undefined)
  return {
    ...(raw as object),
    id: batchId,
    items,
    aiStatus: (latestAiTaskStatus ?? 'not_started') as PendingBatch['aiStatus'],
  } as PendingBatchDetail
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
  return request.get('/admin/archives', { params }).then((res: PageData<ArchiveRecord & { tagNames?: string[] }>) => {
    // 后端 ArchiveResponse 返回 tagNames，前端列表模板绑定 tags —— 对齐字段，与详情映射一致
    if (res?.records) {
      res.records = res.records.map((r) => ({ ...r, tags: r.tagNames ?? r.tags ?? [] }))
    }
    return res
  })
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

/** 档案换盒（P0-1，改所在档案盒，目标盒须同分类） */
export function placeArchive(archiveId: number, boxId: number): Promise<void> {
  if (USE_MOCK) return Promise.resolve()
  return request.put(`/admin/archives/${archiveId}/placement`, { boxId })
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

/** 档案文件预览（返回 Blob，供 FilePreview 组件渲染） */
export function previewArchiveFile(fileId: number): Promise<Blob> {
  if (USE_MOCK) {
    return Promise.resolve(new Blob(['# 预览内容\n\n这是模拟的文件预览。'], { type: 'application/pdf' }))
  }
  return request.get(`/admin/archive-files/${fileId}/preview`, { responseType: 'blob' }) as Promise<Blob>
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
