// src/api/approval.ts
import request from './request'
import type { PageData, PageParams } from '@/types/api'
import type {
  ApprovalOpinionData,
  ApprovalParams,
  ApprovalRequest,
  ApprovalRequestDetail,
} from '@/types/approval'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询审批单（§13.1） */
export function getApprovals(params?: ApprovalParams & PageParams): Promise<PageData<ApprovalRequest>> {
  if (USE_MOCK) {
    return import('@/mock/modules/approval').then((m) => m.mockApprovals(params))
  }
  return request.get('/admin/approvals', { params })
}

/** 获取审批详情（§13.2） */
export function getApprovalDetail(approvalId: number): Promise<ApprovalRequestDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/approval').then((m) => m.mockApprovalDetail(approvalId))
  }
  return request.get(`/admin/approvals/${approvalId}`)
}

/** 审批通过（§13.3） */
export function approveApproval(approvalId: number, data: ApprovalOpinionData): Promise<ApprovalRequestDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/approval').then((m) => m.mockApproveApproval(approvalId, data))
  }
  return request.post(`/admin/approvals/${approvalId}/approve`, data)
}

/** 审批退回（§13.4） */
export function rejectApproval(approvalId: number, data: ApprovalOpinionData): Promise<ApprovalRequestDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/approval').then((m) => m.mockRejectApproval(approvalId, data))
  }
  return request.post(`/admin/approvals/${approvalId}/reject`, data)
}
