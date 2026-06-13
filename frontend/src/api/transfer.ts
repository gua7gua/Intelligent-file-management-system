import request from './request'
import type { PageData } from '@/types/api'
import type {
  TransferBatch,
  TransferBatchDetail,
  TransferBatchQuery,
  TransferDashboard,
} from '@/types/transfer'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

export function getTransferDashboard(): Promise<TransferDashboard> {
  if (USE_MOCK) return import('@/mock/modules/transfer').then((m) => m.mockGetTransferDashboard())
  return request.get('/transfer/dashboard')
}

export function getTransferBatches(params?: TransferBatchQuery): Promise<PageData<TransferBatch>> {
  if (USE_MOCK) return import('@/mock/modules/transfer').then((m) => m.mockGetTransferBatches(params))
  return request.get('/transfer/batches', { params })
}

export function getTransferBatchDetail(batchId: number): Promise<TransferBatchDetail> {
  if (USE_MOCK) return import('@/mock/modules/transfer').then((m) => m.mockGetTransferBatchDetail(batchId))
  return request.get(`/transfer/batches/${batchId}`)
}

export function createTransferBatch(data: TransferBatchDetail): Promise<TransferBatchDetail> {
  if (USE_MOCK) return import('@/mock/modules/transfer').then((m) => m.mockCreateTransferBatch(data))
  return request.post('/transfer/batches', data)
}

export function updateTransferBatch(batchId: number, data: TransferBatchDetail): Promise<TransferBatchDetail> {
  if (USE_MOCK) return import('@/mock/modules/transfer').then((m) => m.mockUpdateTransferBatch(batchId, data))
  return request.put(`/transfer/batches/${batchId}`, data)
}

export function submitTransferBatch(batchId: number): Promise<TransferBatchDetail> {
  if (USE_MOCK) return import('@/mock/modules/transfer').then((m) => m.mockSubmitTransferBatch(batchId))
  return request.post(`/transfer/batches/${batchId}/submit`)
}

export function exportTransferBatch(batchId: number): Promise<Blob> {
  if (USE_MOCK) return import('@/mock/modules/transfer').then((m) => m.mockExportTransferBatch(batchId))
  return request.get(`/transfer/batches/${batchId}/export`, { responseType: 'blob' })
}
