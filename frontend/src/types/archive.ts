// src/types/archive.ts

// ── 待入库相关 ──

/** 待入库批次摘要 */
export interface PendingBatch {
  id: number
  batchNo: string
  title: string
  sourceType: 'transfer' | 'collection'
  status: string
  itemCount: number
  acceptedCount: number
  returnedCount: number
  aiStatus: 'not_started' | 'running' | 'partial_completed' | 'completed' | 'failed'
  createdAt: string
  updatedAt: string
}

/** 待入库条目 */
export interface PendingItem {
  id: number
  batchId: number
  seqNo: number
  inputTitle: string
  expectedFilename: string
  carrierStatus: 'electronic' | 'paper_electronic' | 'paper'
  itemStatus: 'accepted' | 'suggested' | 'confirmed' | 'pending_archive' | 'archived'
  matchStatus: string
  // 受保护字段
  securityLevel: number
  retentionPeriod: string
  openStatus: 'open' | 'closed'
  allowDigitization: boolean
  // AI 候选字段
  suggestedTitle?: string
  suggestedResponsible?: string
  suggestedFormedDate?: string
  suggestedCategoryId?: number
  suggestedTags?: string[]
  // 确认字段
  confirmedTitle?: string
  confirmedResponsible?: string
  confirmedFormedDate?: string
  confirmedCategoryId?: number
  confirmedTags?: string[]
  // 入库结果
  archiveId?: number
  archiveNo?: string
  lifecycleStatus?: string
  boxNo?: string
  spine?: string
  locationCode?: string
  createdAt: string
  updatedAt: string
}

/** 待入库批次详情（含条目） */
export interface PendingBatchDetail extends PendingBatch {
  items: PendingItem[]
}

/** AI 补全任务 */
export interface AiTask {
  id: number
  taskNo: string
  status: 'running' | 'partial_completed' | 'completed' | 'failed'
  batchSize: number
  totalBatches: number
  completedCount: number
  failedCount: number
  errorMessage?: string
}

/** 确认条目字段请求 */
export interface ConfirmItemData {
  confirmedTitle: string
  confirmedResponsibleText: string
  confirmedFormedDate: string
  confirmedCategoryId: number
  confirmedTags: string[]
}

/** 入库请求 */
export interface ArchiveItemData {
  fondsId: number
  boxId?: number
  locationId?: number
  sortNo?: number
  pageCount?: number
}

/** 上架请求 */
export interface ShelveBatchData {
  note?: string
}

/** 待入库批次查询参数 */
export interface PendingBatchParams {
  sourceType?: 'transfer' | 'collection'
  aiStatus?: string
  keyword?: string
  pageNo?: number
  pageSize?: number
}

// ── 档案管理相关 ──

/** 正式档案摘要 */
export interface ArchiveRecord {
  id: number
  archiveNo: string
  title: string
  categoryId: number
  categoryName: string
  securityLevel: number
  openStatus: 'open' | 'closed'
  carrierStatus: string
  lifecycleStatus: string
  responsibleText: string
  formedDate: string
  tags: string[]
  fondsId: number
  fondsName?: string
  locationCode?: string
  boxNo?: string
  createdAt: string
  updatedAt: string
}

/** 档案文件 */
export interface ArchiveFile {
  id: number
  originalFilename: string
  fileSize: number
  fileStatus: string
  mimeType: string
  createdAt: string
}

/** 变更日志 */
export interface ArchiveChangeLog {
  id: number
  field: string
  oldValue: string
  newValue: string
  changeSource: string
  changedAt: string
  changedBy: string
}

/** 待审批记录 */
export interface PendingApproval {
  id: number
  type: 'security_adjustment' | 'open_adjustment'
  status: 'pending' | 'approved' | 'rejected'
  newSecurityLevel?: number
  newOpenStatus?: string
  reason: string
  createdAt: string
}

/** 档案详情 */
export interface ArchiveDetail extends ArchiveRecord {
  summary?: string
  allowDigitization: boolean
  retentionPeriod: string
  loanStatus: string
  conditionStatus: string
  files: ArchiveFile[]
  changeLogs: ArchiveChangeLog[]
  pendingApprovals: PendingApproval[]
}

/** 档案编辑请求 */
export interface ArchiveEditData {
  title: string
  responsibleText: string
  formedDate: string
  categoryId: number
  fondsId?: number
  tagNames: string[]
  changeReason?: string
}

/** 密级调整请求 */
export interface SecurityAdjustData {
  newSecurityLevel: number
  evidenceArchiveNo: string
  reason: string
}

/** 开放调整请求 */
export interface OpenAdjustData {
  newOpenStatus: 'open' | 'closed'
  evidenceArchiveNo: string
  reason: string
}

/** 档案查询参数 */
export interface ArchiveQueryParams {
  keyword?: string
  archiveNo?: string
  categoryId?: number
  formedYearStart?: number
  formedYearEnd?: number
  organizationId?: number
  fondsId?: number
  securityLevel?: number
  openStatus?: string
  carrierStatus?: string
  lifecycleStatus?: string
  loanStatus?: string
  conditionStatus?: string
  pageNo?: number
  pageSize?: number
}
