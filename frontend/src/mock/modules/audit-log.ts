import type { AuditLog, AuditLogQuery } from '@/types/audit-log'
import type { CursorData } from '@/types/api'

const auditLogs: AuditLog[] = [
  { id: 1, actorUserId: 1, actorType: 'internal', actorName: '小刘', moduleName: '用户管理', operationType: '禁用账号', businessType: 'user', businessId: 5, detail: { reason: '离职' }, ipAddress: '10.0.0.12', operatedAt: '2026-06-14T09:30:00+08:00' },
  { id: 2, actorUserId: 1, actorType: 'internal', actorName: '小刘', moduleName: '档案管理', operationType: '元数据编辑', businessType: 'archive', businessId: 101, detail: { field: 'title' }, ipAddress: '10.0.0.12', operatedAt: '2026-06-14T10:05:00+08:00' },
  { id: 3, actorUserId: 0, actorType: 'system', moduleName: '系统', operationType: '自动备份', businessType: 'backup', businessId: 7, detail: { scope: 'db' }, ipAddress: '127.0.0.1', operatedAt: '2026-06-14T02:00:00+08:00' },
  { id: 4, actorUserId: 3, actorType: 'internal', actorName: '小张', moduleName: '移交验收', operationType: '接收清单', businessType: 'intake_batch', businessId: 12, detail: {}, ipAddress: '10.0.0.20', operatedAt: '2026-06-13T16:40:00+08:00' },
  { id: 5, actorUserId: 4, actorType: 'public', actorName: '公众用户A', moduleName: '公众查询', operationType: '检索公开档案', businessType: 'archive', businessId: 88, detail: {}, ipAddress: '202.100.1.5', operatedAt: '2026-06-13T11:20:00+08:00' },
  { id: 6, actorUserId: 1, actorType: 'internal', actorName: '小刘', moduleName: '档案鉴定', operationType: '鉴定确认', businessType: 'appraisal', businessId: 3, detail: { result: '销毁' }, ipAddress: '10.0.0.12', operatedAt: '2026-06-12T15:00:00+08:00' },
  { id: 7, actorUserId: 0, actorType: 'system', moduleName: '系统', operationType: '四性检测', businessType: 'file_check', businessId: 9, detail: { result: 'safe' }, ipAddress: '127.0.0.1', operatedAt: '2026-06-12T03:00:00+08:00' },
  { id: 8, actorUserId: 2, actorType: 'internal', actorName: '小李', moduleName: '借阅审批', operationType: '审批通过', businessType: 'borrow_request', businessId: 21, detail: {}, ipAddress: '10.0.0.30', operatedAt: '2026-06-11T14:10:00+08:00' },
  { id: 9, actorUserId: 4, actorType: 'public', actorName: '公众用户B', moduleName: '公众查询', operationType: '下载公开档案', businessType: 'archive', businessId: 90, detail: {}, ipAddress: '202.100.1.9', operatedAt: '2026-06-11T09:45:00+08:00' },
  { id: 10, actorUserId: 1, actorType: 'internal', actorName: '小刘', moduleName: '系统配置', operationType: '更新配置', businessType: 'system_config', businessId: 2, detail: { key: 'ai.enabled' }, ipAddress: '10.0.0.12', operatedAt: '2026-06-10T17:30:00+08:00' },
  { id: 11, actorUserId: 3, actorType: 'internal', actorName: '小张', moduleName: '档案销毁', operationType: '确认销毁', businessType: 'destruction_list', businessId: 5, detail: {}, ipAddress: '10.0.0.20', operatedAt: '2026-06-10T10:00:00+08:00' },
  { id: 12, actorUserId: 0, actorType: 'system', moduleName: '系统', operationType: '登录失败', businessType: 'user', businessId: 9, detail: { reason: '密码错误' }, ipAddress: '203.0.113.7', operatedAt: '2026-06-09T22:15:00+08:00' },
]

export function mockAuditLogs(query?: AuditLogQuery): CursorData<AuditLog> {
  let list = auditLogs
  if (query?.actorType) list = list.filter((l) => l.actorType === query.actorType)
  if (query?.moduleName) list = list.filter((l) => l.moduleName.includes(query.moduleName!.trim()))
  if (query?.operationType) list = list.filter((l) => l.operationType.includes(query.operationType!.trim()))
  if (query?.businessType) list = list.filter((l) => l.businessType === query.businessType)
  if (query?.actorUserId) list = list.filter((l) => l.actorUserId === query.actorUserId)
  if (query?.startedAt) list = list.filter((l) => l.operatedAt >= query.startedAt!)
  if (query?.endedAt) list = list.filter((l) => l.operatedAt <= query.endedAt!)

  const limit = query?.limit ?? 10
  let startIndex = 0
  if (query?.cursor) {
    const idx = list.findIndex((l) => String(l.id) === query.cursor)
    startIndex = idx === -1 ? 0 : idx + 1
  }
  const slice = list.slice(startIndex, startIndex + limit)
  const last = slice[slice.length - 1]
  const hasNext = startIndex + limit < list.length
  return { records: slice.map((l) => ({ ...l })), nextCursor: hasNext && last ? String(last.id) : null, hasNext }
}
