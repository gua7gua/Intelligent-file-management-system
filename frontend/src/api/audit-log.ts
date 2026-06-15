import request from './request'
import type { CursorData } from '@/types/api'
import type { AuditLog, AuditLogQuery } from '@/types/audit-log'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询审计日志（§23.1，游标分页） */
export function getAuditLogs(query?: AuditLogQuery): Promise<CursorData<AuditLog>> {
  if (USE_MOCK) {
    return import('@/mock/modules/audit-log').then((m) => m.mockAuditLogs(query))
  }
  return request.get('/admin/audit-logs', { params: query })
}
