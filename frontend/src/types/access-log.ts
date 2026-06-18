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
  /** 富字段：访问人姓名（后端联 users.real_name 填充，与 userName 同义，后端实际返回字段名） */
  actorName?: string
  archiveId: number
  /** 富字段：档号，便于展示 */
  archiveNo?: string
  /** 富字段：档案题名 */
  title?: string
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
