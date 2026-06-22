import type { PageData, PageParams } from './api'
import type {
  BatchStatusValue,
  CarrierStatusValue,
  ItemStatusValue,
} from './transfer'

export interface PublicArchive {
  archiveId: number
  archiveNo: string
  title: string
  responsibleText: string
  categoryName: string
  formedYear: number
  carrierStatus: CarrierStatusValue
  sourceType?: 'transfer' | 'collection' | 'compilation'
  tags?: string[]
  hasElectronicFile: boolean
  canPreview?: boolean
  canDownload?: boolean
  fondsName?: string
  organizationName?: string
}

export interface PublicArchiveFile {
  id: number
  filename: string
  fileFormat: string
  fileSize: number
  canPreview: boolean
  canDownload: boolean
}

export interface PublicArchiveDetail extends PublicArchive {
  formedDate: string
  retentionPeriod: '10y' | '30y' | 'permanent'
  openStatus: 'open'
  summary: string
  files: PublicArchiveFile[]
}

export interface PublicCollectionItem {
  id: number
  seqNo: number
  inputTitle: string
  pageCount?: number
  carrierStatus: CarrierStatusValue | ''
  electronicFormat?: string
  expectedFilename?: string
  formedDate?: string
  status: ItemStatusValue
  localFileSize?: number
}

export interface PublicCollectionBatch {
  id: number
  batchNo: string
  title: string
  contactName: string
  contactPhone: string
  archiveYear: number
  status: BatchStatusValue
  statusText: string
  submittedAt?: string
  scheduledReceiveAt?: string
  itemCount: number
  agreementAcceptedAt?: string
  rejectReason?: string
  items: PublicCollectionItem[]
}

export interface PublicHomeStats {
  openArchiveCount: number
  electronicFileCount: number
  collectionCount: number
  latestOpenCount: number
}

export interface PublicHomeData {
  stats: PublicHomeStats
  categories: Array<{ name: string; count: number }>
  recentArchives: PublicArchive[]
}

/** 公开馆藏统计（§5.1.1 GET /api/public/stats，免登录门面数据）。 */
export interface PublicStatsResponse {
  openArchiveCount: number
  electronicFileCount: number
  collectionCount: number
  latestOpenCount: number
  categories: Array<{ name: string; count: number }>
}

export interface PublicSearchParams extends PageParams {
  keyword?: string
  archiveNo?: string
  title?: string
  categoryId?: number
  formedYearStart?: number
  formedYearEnd?: number
  responsibleText?: string
  tagIds?: string
  sourceType?: string
  carrierStatus?: string
  hasElectronicFile?: boolean
  fondsName?: string
  organizationName?: string
  retentionPeriod?: string
  fileExt?: string
  sortBy?: string
}

export interface PublicAiQueryRequest {
  text: string
}

export interface PublicAiQueryResult {
  ruleType: 'publicSearchQuery'
  conditions: PublicSearchParams
  rawJson: Record<string, unknown>
}

export interface PublicCollectionSummary {
  total: number
  draft: number
  inProgress: number
  completed: number
}

export interface PublicDownloadLog {
  id: number
  archiveId: number
  /** 档号（联表 archives 填充，便于直接展示）。 */
  archiveNo?: string
  /** 档案题名（联表 archives 填充）。 */
  title?: string
  accessType: string
  accessedAt: string
}

/** 公众概览（§5.1 dashboard）内嵌的公开馆藏 + 本人统计。 */
export interface PublicDashboardStats {
  openArchiveCount: number
  electronicFileCount: number
  collectionCount: number
  latestOpenCount: number
  myPendingCollections: number
  myDownloadCount: number
}

/** 当前公众用户基础资料。 */
export interface PublicDashboardUser {
  realName: string
  phone: string
  /** active | disabled */
  status: 'active' | 'disabled'
}

export interface PublicOverviewData {
  collectionSummary: PublicCollectionSummary
  recentCollections: PublicCollectionBatch[]
  downloadLogs: PublicDownloadLog[]
  stats: PublicDashboardStats
  user: PublicDashboardUser
  summarizedAt: string
}

export interface PublicSmsCodeRequest {
  phone: string
  scene: 'register' | 'forgot_password'
}

export interface PublicRegisterRequest {
  phone: string
  smsCode: string
  password: string
  realName: string
}

export interface PublicResetPasswordRequest {
  phone: string
  smsCode: string
  newPassword: string
}

export type PublicArchivePage = PageData<PublicArchive & { openStatus: 'open' }>
