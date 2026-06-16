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
} from '@/types/public'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

export function getPublicHome(): Promise<PublicHomeData> {
  if (USE_MOCK) return import('@/mock/modules/public').then((m) => m.mockGetPublicHome())
  // 后端暂无专用首页接口，临时用搜索接口替代
  return request.get('/public/archives/search', { params: { pageNo: 1, pageSize: 6 } }).then(() => ({
    stats: { openArchiveCount: 0, electronicFileCount: 0, collectionCount: 0, latestOpenCount: 0 },
    categories: [],
    recentArchives: [],
  }))
}

export function searchPublicArchives(params?: PublicSearchParams): Promise<PublicArchivePage> {
  if (USE_MOCK) return import('@/mock/modules/public').then((m) => m.mockSearchPublicArchives(params))
  return request.get('/public/archives/search', { params })
}

export function getPublicArchiveDetail(archiveId: number): Promise<PublicArchiveDetail> {
  if (USE_MOCK) return import('@/mock/modules/public').then((m) => m.mockGetPublicArchiveDetail(archiveId))
  return request.get(`/public/archives/${archiveId}`)
}

export function generatePublicSearchQuery(data: PublicAiQueryRequest): Promise<PublicAiQueryResult> {
  if (USE_MOCK) return import('@/mock/modules/public').then((m) => m.mockGeneratePublicSearchQuery(data))
  return request.post('/public/archives/ai-query', data)
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
