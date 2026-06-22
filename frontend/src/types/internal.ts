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
  /** 所属全宗名称（模糊匹配，后端转 id） */
  fondsName?: string
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
  /** 排序：relevance / formed_desc / archived_desc / archiveNo_asc */
  sortBy?: string
}

/** 内部档案文件 */
export interface InternalFile {
  fileId: number
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
  fondsName?: string
  organizationName?: string
}

/** 内部档案详情：兼容旧 mock 数据与后端 ArchiveSearchDetailResponse 真实返回 */
export interface InternalArchiveDetail extends InternalArchive {
  /** 后端详情响应字段为 archiveId（与列表的 id 同义），保留兼容 */
  archiveId?: number
  summary?: string
  retentionPeriod?: RetentionPeriodValue
  files: InternalFile[]
}

/** 后端详情响应字段（ArchiveSearchDetailResponse），独立于检索摘要 */
export interface InternalArchiveDetailResponse {
  archiveId: number
  archiveNo: string
  title: string
  responsibleText: string
  formedYear?: number
  formedDate?: string
  categoryName: string
  carrierStatus: CarrierStatusValue
  securityLevel?: number
  openStatus?: OpenStatusValue
  retentionPeriod?: RetentionPeriodValue
  files: InternalFileSummary[]
}

/** 后端详情中的电子文件摘要（FileSummary） */
export interface InternalFileSummary {
  fileId: number
  originalFilename: string
  fileExt?: string
  fileSize?: number
  mimeType?: string
  fileRole?: string
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
  voucherNo?: string
  voucherIssuedAt?: string
}

/** 借阅申请详情 */
export interface BorrowRequestDetail extends BorrowRequest {
  reason: string
  contactPhone: string
  opinion?: string
  rejectReason?: string
  returnCheckResult?: string
  returnNote?: string
}

/** 我的借阅申请查询参数 */
export interface BorrowRequestParams extends PageParams {
  status?: BorrowStatusValue | ''
  keyword?: string
}

/** 最近查阅记录（11.1 后端返回；只含元数据访问摘要，无分类/密级等冗余字段） */
export interface RecentViewRecord {
  archiveId: number
  archiveNo: string
  title: string
  accessedAt?: string
}

/** 借阅摘要（11.1 我的申请/当前借阅/逾期提示共用结构） */
export interface BorrowSummaryItem {
  requestNo: string
  archiveId: number
  archiveNo: string
  title: string
  status: BorrowStatusValue
  dueAt?: string
  appliedAt: string
}

/** 工作台聚合数据：对齐后端 §11.1 真实返回 */
export interface InternalDashboardData {
  recentViews: RecentViewRecord[]
  myBorrowRequests: BorrowSummaryItem[]
  currentBorrows: BorrowSummaryItem[]
  overdueReminders: BorrowSummaryItem[]
}

export type InternalArchivePage = PageData<InternalArchive>
export type BorrowRequestPage = PageData<BorrowRequest>
