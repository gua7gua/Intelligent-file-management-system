import request from './request'
import type { CursorData } from '@/types/api'
import type { AccessLogQuery, ArchiveAccessLog } from '@/types/access-log'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询档案访问日志（§23.2，游标分页） */
export function getAccessLogs(query?: AccessLogQuery): Promise<CursorData<ArchiveAccessLog>> {
  if (USE_MOCK) {
    return import('@/mock/modules/access-log').then((m) => m.mockAccessLogs(query))
  }
  return request.get('/admin/archive-access-logs', { params: query })
}
