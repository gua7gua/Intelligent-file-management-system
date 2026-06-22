import request from './request'
import type { PageData, PageParams } from '@/types/api'
import type {
  PublicAiQueryRequest,
  PublicAiQueryResult,
  PublicArchiveDetail,
  PublicArchivePage,
  PublicCollectionBatch,
  PublicHomeData,
  PublicOverviewData,
  PublicSearchParams,
  PublicStatsResponse,
} from '@/types/public'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

export function getPublicStats(): Promise<PublicStatsResponse> {
  if (USE_MOCK) return import('@/mock/modules/public').then((m) => m.mockGetPublicStats())
  return request.get('/public/stats')
}

export function getPublicHome(): Promise<PublicHomeData> {
  if (USE_MOCK) return import('@/mock/modules/public').then((m) => m.mockGetPublicHome())
  // 公开馆藏统计来自 /public/stats，最近公开档案来自公开检索
  return Promise.all([getPublicStats(), searchPublicArchives({ pageNo: 1, pageSize: 6 })]).then(
    ([stats, page]) => ({
      stats: {
        openArchiveCount: stats.openArchiveCount,
        electronicFileCount: stats.electronicFileCount,
        collectionCount: stats.collectionCount,
        latestOpenCount: stats.latestOpenCount,
      },
      categories: stats.categories,
      recentArchives: page.records,
    }),
  )
}

export function searchPublicArchives(params?: PublicSearchParams): Promise<PublicArchivePage> {
  if (USE_MOCK) return import('@/mock/modules/public').then((m) => m.mockSearchPublicArchives(params))
  // 后端 ArchiveSummaryResponse 返回 tagNames，前端 PublicArchive.tags，做一次映射（与档案管理 F5 一致）
  return request.get('/public/archives/search', { params }).then((page) => {
    if (page && Array.isArray(page.records)) {
      page.records = page.records.map((r) => {
        const raw = r as PublicArchive & { tagNames?: string[] }
        return { ...r, tags: raw.tagNames ?? r.tags ?? [] }
      })
    }
    return page
  })
}

export function getPublicArchiveDetail(archiveId: number): Promise<PublicArchiveDetail> {
  if (USE_MOCK) return import('@/mock/modules/public').then((m) => m.mockGetPublicArchiveDetail(archiveId))
  // 后端公开详情的电子文件字段为 fileId/originalFilename/fileExt/fileSize/mimeType/fileRole，
  // 前端类型为 id/filename/fileFormat/fileSize，这里做一次字段映射，避免详情面板电子文件名缺失。
  return request.get<unknown, PublicArchiveDetail>(`/public/archives/${archiveId}`).then((raw) => {
    const r = raw as PublicArchiveDetail & {
      files?: Array<{
        fileId?: number
        originalFilename?: string
        fileExt?: string
        fileSize?: number
        mimeType?: string
        fileRole?: string
      }>
    }
    if (Array.isArray(r.files)) {
      r.files = r.files.map((f) => ({
        id: f.id ?? f.fileId ?? 0,
        filename: f.filename ?? f.originalFilename ?? '',
        fileFormat: f.fileFormat ?? f.fileExt ?? '',
        fileSize: f.fileSize ?? 0,
        canPreview: f.canPreview ?? false,
        canDownload: f.canDownload ?? true,
      }))
    }
    return r
  })
}

export function generatePublicSearchQuery(data: PublicAiQueryRequest): Promise<PublicAiQueryResult> {
  if (USE_MOCK) return import('@/mock/modules/public').then((m) => m.mockGeneratePublicSearchQuery(data))
  return request.post('/public/archives/ai-query', data, { timeout: 60000 })
}

/** 公众端电子文件预览（返回 Blob，供 FilePreview 组件渲染） */
export function previewPublicArchiveFile(fileId: number): Promise<Blob> {
  if (USE_MOCK) return Promise.resolve(new Blob(['公众预览内容'], { type: 'application/pdf' }))
  return request.get(`/public/archive-files/${fileId}/preview`, { responseType: 'blob' }) as Promise<Blob>
}

export function downloadPublicArchiveFile(fileId: number): Promise<Blob> {
  if (USE_MOCK) return import('@/mock/modules/public').then((m) => m.mockDownloadPublicArchiveFile(fileId))
  return request.get(`/public/archive-files/${fileId}/download`, { responseType: 'blob' })
}

export function getPublicOverview(): Promise<PublicOverviewData> {
  if (USE_MOCK) return import('@/mock/modules/public').then((m) => m.mockGetPublicOverview())
  return request.get('/public/dashboard')
}

export function getMyCollections(params?: PageParams): Promise<PageData<PublicCollectionBatch>> {
  if (USE_MOCK) return import('@/mock/modules/public').then((m) => m.mockGetMyCollections(params))
  return request.get('/public/collections', { params })
}

export function createCollectionDraft(data: PublicCollectionBatch): Promise<PublicCollectionBatch> {
  if (USE_MOCK) return import('@/mock/modules/public').then((m) => m.mockCreateCollectionDraft(data))
  return request.post('/public/collections', data)
}

export function updateCollectionDraft(batchId: number, data: PublicCollectionBatch): Promise<PublicCollectionBatch> {
  if (USE_MOCK) return import('@/mock/modules/public').then((m) => m.mockUpdateCollectionDraft(batchId, data))
  return request.put(`/public/collections/${batchId}`, data)
}

export function submitCollectionBatch(batchId: number): Promise<PublicCollectionBatch> {
  if (USE_MOCK) return import('@/mock/modules/public').then((m) => m.mockSubmitCollectionBatch(batchId))
  return request.post(`/public/collections/${batchId}/submit`, { agreementAccepted: true })
}
