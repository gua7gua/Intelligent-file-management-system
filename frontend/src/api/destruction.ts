// src/api/destruction.ts
import request from './request'
import type { PageData, PageParams } from '@/types/api'
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

/** 提交销毁审批（§15.3）
 *  后端实现返回更新后的清册详情（含 approvalRequestId），与接口文档「返回审批单」存在差异；
 *  调用方据此刷新清册状态并取得 approvalRequestId 用于跳转审批工作台。
 */
export function submitDestructionApproval(
  listId: number,
  data: DestructionSubmitApprovalData,
): Promise<DestructionListDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/destruction').then((m) => m.mockSubmitDestructionApproval(listId, data))
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
  // 注意：不要手动设 Content-Type=multipart/form-data，否则会覆盖浏览器自动生成的
  // 带 boundary 的 header，导致后端 Tomcat 报 "no multipart boundary was found"（人#13）。
  // 让 axios 检测到 FormData 后自动设带 boundary 的 Content-Type。
  return request.post(`/admin/destruction-lists/${listId}/photos`, form)
}

/** 确认销毁（§15.5） */
export function confirmDestruction(listId: number, data: DestructionConfirmData): Promise<DestructionListDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/destruction').then((m) => m.mockConfirmDestruction(listId, data))
  }
  return request.post(`/admin/destruction-lists/${listId}/destroy`, data)
}
