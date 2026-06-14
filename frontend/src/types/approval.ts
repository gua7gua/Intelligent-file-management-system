import type { PageData, PageParams } from './api'
import type { DestructionItem } from './destruction'
import type {
  ApprovalStatusValue,
  ApprovalTargetTypeValue,
  ApprovalTypeValue,
  SecurityLevelValue,
} from './enums'

/** 审批单查询参数（§13.1） */
export interface ApprovalParams extends PageParams {
  approvalType?: ApprovalTypeValue | ''
  status?: ApprovalStatusValue | ''
  keyword?: string
}

/** 审批关联档案摘要（目标/凭证） */
export interface ApprovalArchiveSummary {
  id: number
  archiveNo: string
  title: string
  categoryName: string
  organizationName?: string
  fondsName?: string
  securityLevel: SecurityLevelValue
  lifecycleStatus: string
}

/** 销毁清册摘要（审批详情内嵌） */
export interface ApprovalDestructionListSummary {
  id: number
  listNo: string
  listName: string
  itemCount: number
  appraisalBatchNo?: string
  items: DestructionItem[]
}

/** 审批单摘要（列表行） */
export interface ApprovalRequest {
  id: number
  approvalType: ApprovalTypeValue
  targetType: ApprovalTargetTypeValue
  targetId: number
  evidenceArchiveId?: number
  oldValue?: string
  newValue?: string
  reason: string
  status: ApprovalStatusValue
  submittedBy: number
  submittedByName?: string
  submittedAt: string
  approvedBy?: number
  approvedByName?: string
  approvedAt?: string
  approvalOpinion?: string
  targetArchiveNo?: string
  targetArchiveTitle?: string
  targetListNo?: string
  targetListName?: string
}

/** 审批单详情（§13.2） */
export interface ApprovalRequestDetail extends ApprovalRequest {
  targetArchive?: ApprovalArchiveSummary
  evidenceArchive?: ApprovalArchiveSummary
  evidenceMatched?: boolean
  destructionList?: ApprovalDestructionListSummary
}

/** 审批意见请求（§13.3 / §13.4） */
export interface ApprovalOpinionData {
  opinion: string
}

export type ApprovalRequestPage = PageData<ApprovalRequest>
