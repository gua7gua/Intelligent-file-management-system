import type { CursorParams } from './api'

/** 访问者类型 */
export type AccessUserType = 'internal' | 'public' | 'anonymous'

/** 访问类型 */
export type AccessType = 'view_metadata' | 'preview' | 'download'

/** 档案访问日志记录（DB §9.2 + §23.2） */
export interface ArchiveAccessLog {
  id: number
  userId?: number
  userType: AccessUserType
  /** 富字段：用户姓名 */
  userName?: string
  archiveId: number
  /** 富字段：档号，便于展示 */
  archiveNo?: string
  archiveFileId?: number
  accessType: AccessType
  ipAddress?: string
  accessedAt: string
}

/** 访问日志查询参数（§23.2） */
export interface AccessLogQuery extends CursorParams {
  userId?: number
  archiveId?: number
  accessType?: AccessType | ''
  startedAt?: string
  endedAt?: string
}
