import type { PageData, PageParams } from './api'
import type { CarrierStatusValue, OpenStatusValue, RetentionPeriodValue } from './transfer'
import { BorrowStatus, SourceType } from './enums'

export type BorrowStatusValue = (typeof BorrowStatus)[keyof typeof BorrowStatus]
export type SourceTypeValue = (typeof SourceType)[keyof typeof SourceType]

/** 内部档案检索参数（公开检索维度 + 密级/开放/借阅/实体状态等内部维度） */
export interface InternalSearchParams extends PageParams {
  keyword?: string
  archiveNo?: string
  title?: string
  responsibleText?: string
  categoryId?: number
  fondsId?: number
  organizationName?: string
  tagIds?: string
  formedYearStart?: number
  formedYearEnd?: number
  formedDateStart?: string
  formedDateEnd?: string
  archivedAtStart?: string
  sourceType?: SourceTypeValue
  carrierStatus?: CarrierStatusValue | ''
  retentionPeriod?: RetentionPeriodValue
  securityLevelMax?: number
  openStatus?: OpenStatusValue | ''
  loanStatus?: string
  conditionStatus?: string
  hasElectronicFile?: boolean
  fileExt?: string
  fileRole?: string
  fileCheckStatus?: string
}

/** 内部档案文件 */
export interface InternalFile {
  id: number
  originalFilename: string
  fileFormat: string
  fileSize: number
  fileRole: string
  fileCheckStatus: string
  canPreview: boolean
  canDownload: boolean
}

/** 内部档案摘要（检索结果行，不含架位/盒号） */
export interface InternalArchive {
  id: number
  archiveNo: string
  title: string
  categoryId: number
  categoryName: string
  responsibleText: string
  formedDate: string
  securityLevel: number
  openStatus: OpenStatusValue
  carrierStatus: CarrierStatusValue
  sourceType: SourceTypeValue
  tags: string[]
  hasElectronicFile: boolean
  canPreview: boolean
  canDownload: boolean
  canBorrow: boolean
  borrowHint: string
  archivedAt: string
}

/** 内部档案详情 */
export interface InternalArchiveDetail extends InternalArchive {
  summary?: string
  retentionPeriod: RetentionPeriodValue
  files: InternalFile[]
}

/** AI 检索条件生成请求 */
export interface InternalAiQueryRequest {
  text: string
}

/** AI 检索条件生成结果 */
export interface InternalAiQueryResult {
  ruleType: 'internalSearchQuery'
  conditions: Partial<InternalSearchParams>
  rawJson: Record<string, unknown>
}

/** 借阅申请提交请求 */
export interface BorrowRequestCreateData {
  archiveId: number
  reason: string
  expectedDays: number
  expectedVisitAt: string
  contactPhone: string
}

/** 借阅申请摘要 */
export interface BorrowRequest {
  id: number
  requestNo: string
  archiveId: number
  archiveNo: string
  archiveTitle: string
  status: BorrowStatusValue
  expectedDays: number
  expectedVisitAt?: string
  appliedAt: string
  approvedAt?: string
  checkedOutAt?: string
  dueAt?: string
  returnedAt?: string
  overdue?: boolean
}

/** 借阅申请详情 */
export interface BorrowRequestDetail extends BorrowRequest {
  reason: string
  contactPhone: string
  opinion?: string
  rejectReason?: string
  voucherNo?: string
  voucherIssuedAt?: string
  returnCheckResult?: string
  returnNote?: string
}

/** 我的借阅申请查询参数 */
export interface BorrowRequestParams extends PageParams {
  status?: BorrowStatusValue | ''
  keyword?: string
}

/** 工作台指标 */
export interface InternalDashboardStats {
  recentViewCount: number
  pendingApprovalCount: number
  approvedPendingPickupCount: number
  downloadCount: number
}

/** 最近查阅记录 */
export interface RecentViewRecord {
  id: number
  archiveId: number
  archiveNo: string
  title: string
  categoryName: string
  securityLevel: number
  viewedAt: string
  accessStatus: 'available' | 'permission_changed'
}

/** 工作台下载记录 */
export interface DownloadRecord {
  id: number
  archiveId: number
  archiveNo: string
  title: string
  downloadedAt: string
}

/** 当前用户权限范围 */
export interface InternalPermission {
  role: string
  organizationName: string
  maxSecurityLevel: number
  dataScope: string
}

/** 工作台聚合数据 */
export interface InternalDashboardData {
  stats: InternalDashboardStats
  recentViews: RecentViewRecord[]
  borrowRequests: BorrowRequest[]
  currentLoans: BorrowRequest[]
  downloads: DownloadRecord[]
  permission: InternalPermission
}

export type InternalArchivePage = PageData<InternalArchive>
export type BorrowRequestPage = PageData<BorrowRequest>
