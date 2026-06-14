import type { PageData, PageParams } from './api'
import type { BackupScopeValue, BackupTaskStatusValue, CheckResultValue, CheckTypeValue } from './enums'

/** 备份任务查询参数（§21.1） */
export interface BackupTaskParams extends PageParams {
  status?: BackupTaskStatusValue | ''
  backupScope?: BackupScopeValue | ''
}

/** 备份任务（对齐 DB 11.1 backup_tasks） */
export interface BackupTask {
  id: number
  taskNo: string
  backupScope: BackupScopeValue
  status: BackupTaskStatusValue
  backupPath: string
  fileSize: number
  sha256?: string
  startedAt: string
  finishedAt?: string
  message?: string
  createdAt: string
}

/** 创建备份任务请求（§21.2） */
export interface BackupTaskCreateData {
  backupScope: BackupScopeValue
}

/** 四性检测记录查询参数（§21.3） */
export interface FileCheckRecordParams extends PageParams {
  targetType?: string
  targetId?: number
  checkType?: CheckTypeValue | ''
  checkResult?: CheckResultValue | ''
}

/** 四性检测记录（对齐 DB file_check_records） */
export interface FileCheckRecord {
  id: number
  targetType: string
  targetId: number
  checkType: CheckTypeValue
  checkResult: CheckResultValue
  message?: string
  checkedAt: string
}

/** 触发检测请求（§21.4） */
export interface TriggerCheckData {
  checkTypes: CheckTypeValue[]
}

export type BackupTaskPage = PageData<BackupTask>
export type FileCheckRecordPage = PageData<FileCheckRecord>
