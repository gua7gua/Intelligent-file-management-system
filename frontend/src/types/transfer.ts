import {
  BatchStatus,
  CarrierStatus,
  ItemStatus,
  OpenStatus,
  RetentionPeriod,
} from './enums'

export type BatchStatusValue = (typeof BatchStatus)[keyof typeof BatchStatus]
export type ItemStatusValue = (typeof ItemStatus)[keyof typeof ItemStatus]
export type CarrierStatusValue = (typeof CarrierStatus)[keyof typeof CarrierStatus]
export type RetentionPeriodValue = (typeof RetentionPeriod)[keyof typeof RetentionPeriod]
export type OpenStatusValue = (typeof OpenStatus)[keyof typeof OpenStatus]

export interface TransferDashboardSummary {
  draft: number
  pendingTransfer: number
  partiallyReceived: number
  received: number
  archived: number
  shelved: number
  rejected: number
}

export interface TransferBatch {
  id: number
  batchNo: string
  title: string
  sourceType: 'transfer'
  status: BatchStatusValue
  statusText: string
  organizationName: string
  departmentName: string
  contactPerson: string
  contactPhone: string
  archiveYear: number
  expectedTransferDate: string
  submittedAt?: string
  receivedAt?: string
  archivedAt?: string
  shelvedAt?: string
  itemCount: number
  acceptedCount: number
  rejectedCount: number
  receiptAttachmentId?: number
}

export interface TransferItem {
  id: number
  batchId: number
  seqNo: number
  inputTitle: string
  pageCount?: number
  retentionPeriod: RetentionPeriodValue | ''
  carrierStatus: CarrierStatusValue | ''
  securityLevel: number
  openStatus: OpenStatusValue
  allowDigitization: boolean
  electronicFormat?: string
  expectedFilename?: string
  formedDate?: string
  status: ItemStatusValue
  rejectReason?: string
  localFileSize?: number
}

export interface TransferBatchDetail extends TransferBatch {
  items: TransferItem[]
}

export interface TransferDashboard {
  summary: TransferDashboardSummary
  recentBatches: TransferBatch[]
}

export interface TransferBatchQuery {
  status?: string
  keyword?: string
  archiveYear?: number
  pageNo?: number
  pageSize?: number
}
