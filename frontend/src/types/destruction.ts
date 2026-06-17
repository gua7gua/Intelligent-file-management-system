import type { PageData, PageParams } from './api'
import type { ApprovalRequest } from './approval'
import type {
  DestructionListStatusValue,
  DestroyMethodValue,
  FileDeleteStatusValue,
  SecurityLevelValue,
} from './enums'

/** 销毁清册查询参数（§15.1） */
export interface DestructionListParams extends PageParams {
  status?: DestructionListStatusValue | ''
  keyword?: string
}

/** 销毁清册明细快照（数据库 8.5） */
export interface DestructionItem {
  id: number
  archiveId: number
  archiveNoSnapshot: string
  titleSnapshot: string
  categorySnapshot: string
  pageCountSnapshot?: number
  retentionSnapshot: string
  securityLevelSnapshot: SecurityLevelValue
  appraisalOpinionSnapshot: string
  fileDeleteStatus: FileDeleteStatusValue
  fileDeletedAt?: string
}

/** 销毁清册附件（现场照片）
 *  字段名与后端 DestructionListDetailResponse.Photo / 其他附件响应保持一致（originalFilename）。 */
export interface DestructionPhoto {
  id: number
  originalFilename: string
  mimeType: string
  fileSize: number
  sha256: string
}

/** 销毁清册摘要（列表行） */
export interface DestructionList {
  id: number
  listNo: string
  listName: string
  appraisalBatchId?: number
  appraisalBatchNo?: string
  status: DestructionListStatusValue
  approvalRequestId?: number
  destroyedAt?: string
  destroyMethod?: DestroyMethodValue
  supervisorName1?: string
  supervisorName2?: string
  destroyNote?: string
  itemCount: number
  createdAt: string
}

/** 销毁清册详情（§15.2） */
export interface DestructionListDetail extends DestructionList {
  items: DestructionItem[]
  approval?: ApprovalRequest
  photos: DestructionPhoto[]
}

/** 提交销毁审批请求（§15.3） */
export interface DestructionSubmitApprovalData {
  reason: string
}

/** 确认销毁请求（§15.5） */
export interface DestructionConfirmData {
  destroyMethod: DestroyMethodValue
  supervisorName1: string
  supervisorName2: string
  destroyNote: string
  photoIds?: number[]
}

export type DestructionListPage = PageData<DestructionList>
