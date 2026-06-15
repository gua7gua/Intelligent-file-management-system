import type { CursorParams } from './api'

/** 操作人类型 */
export type ActorType = 'internal' | 'public' | 'system'

/** 审计日志记录（DB §11.4 + §23.1） */
export interface AuditLog {
  id: number
  actorUserId: number
  actorType: ActorType
  /** 富字段：操作人姓名，便于展示 */
  actorName?: string
  moduleName: string
  operationType: string
  businessType?: string
  businessId?: number
  /** JSONB 详情 */
  detail?: Record<string, unknown>
  ipAddress?: string
  operatedAt: string
}

/** 审计日志查询参数（§23.1） */
export interface AuditLogQuery extends CursorParams {
  actorUserId?: number
  actorType?: ActorType | ''
  moduleName?: string
  operationType?: string
  businessType?: string
  businessId?: number
  startedAt?: string
  endedAt?: string
}
