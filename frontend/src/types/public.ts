import type { PageData, PageParams } from './api'
import type {
  BatchStatusValue,
  CarrierStatusValue,
  ItemStatusValue,
} from './transfer'

export interface PublicArchive {
  id: number
  archiveNo: string
  title: string
  responsible: string
  category: string
  formedYear: number
  carrierStatus: CarrierStatusValue
  sourceType: 'transfer' | 'collection' | 'compilation'
  tags: string[]
  hasElectronicFile: boolean
  canPreview: boolean
  canDownload: boolean
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
  accessType: string
  accessedAt: string
}

export interface PublicOverviewData {
  collectionSummary: PublicCollectionSummary
  recentCollections: PublicCollectionBatch[]
  downloadLogs: PublicDownloadLog[]
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
