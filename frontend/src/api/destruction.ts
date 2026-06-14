// src/api/destruction.ts
import request from './request'
import type { PageData, PageParams } from '@/types/api'
import type { ApprovalRequest } from '@/types/approval'
import type {
  DestructionConfirmData,
  DestructionList,
  DestructionListDetail,
  DestructionListParams,
  DestructionPhoto,
  DestructionSubmitApprovalData,
} from '@/types/destruction'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询销毁清册（§15.1） */
export function getDestructionLists(
  params?: DestructionListParams & PageParams,
): Promise<PageData<DestructionList>> {
  if (USE_MOCK) {
    return import('@/mock/modules/destruction').then((m) => m.mockDestructionLists(params))
  }
  return request.get('/admin/destruction-lists', { params })
}

/** 获取销毁清册详情（§15.2） */
export function getDestructionListDetail(listId: number): Promise<DestructionListDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/destruction').then((m) => m.mockDestructionListDetail(listId))
  }
  return request.get(`/admin/destruction-lists/${listId}`)
}

/** 提交销毁审批（§15.3） */
export function submitDestructionApproval(
  listId: number,
  data: DestructionSubmitApprovalData,
): Promise<ApprovalRequest> {
  if (USE_MOCK) {
    return import('@/mock/modules/destruction').then((m) => m.mockSubmitDestructionApproval(listId))
  }
  return request.post(`/admin/destruction-lists/${listId}/submit-approval`, data)
}

/** 上传销毁现场照片（§15.4） */
export function uploadDestructionPhotos(listId: number, files: File[]): Promise<DestructionPhoto[]> {
  if (USE_MOCK) {
    return import('@/mock/modules/destruction').then((m) => m.mockUploadDestructionPhotos(listId, files))
  }
  const form = new FormData()
  files.forEach((f) => form.append('files', f))
  return request.post(`/admin/destruction-lists/${listId}/photos`, form, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}

/** 确认销毁（§15.5） */
export function confirmDestruction(listId: number, data: DestructionConfirmData): Promise<DestructionListDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/destruction').then((m) => m.mockConfirmDestruction(listId, data))
  }
  return request.post(`/admin/destruction-lists/${listId}/destroy`, data)
}
