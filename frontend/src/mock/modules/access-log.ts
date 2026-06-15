import type { AccessLogQuery, ArchiveAccessLog } from '@/types/access-log'
import type { CursorData } from '@/types/api'

const accessLogs: ArchiveAccessLog[] = [
  { id: 1, userId: 2, userType: 'internal', userName: '小李', archiveId: 101, archiveNo: 'ARC-001', accessType: 'view_metadata', ipAddress: '10.0.0.30', accessedAt: '2026-06-14T10:00:00+08:00' },
  { id: 2, userId: 2, userType: 'internal', userName: '小李', archiveId: 101, archiveNo: 'ARC-001', archiveFileId: 201, accessType: 'download', ipAddress: '10.0.0.30', accessedAt: '2026-06-14T10:02:00+08:00' },
  { id: 3, userId: undefined, userType: 'anonymous', archiveId: 88, archiveNo: 'ARC-088', accessType: 'view_metadata', ipAddress: '202.100.1.5', accessedAt: '2026-06-13T11:20:00+08:00' },
  { id: 4, userId: 4, userType: 'public', userName: '公众用户A', archiveId: 88, archiveNo: 'ARC-088', accessType: 'download', ipAddress: '202.100.1.5', accessedAt: '2026-06-13T11:25:00+08:00' },
  { id: 5, userId: 3, userType: 'internal', userName: '小张', archiveId: 105, archiveNo: 'ARC-005', accessType: 'preview', ipAddress: '10.0.0.20', accessedAt: '2026-06-13T09:10:00+08:00' },
  { id: 6, userId: 2, userType: 'internal', userName: '小李', archiveId: 110, archiveNo: 'ARC-010', accessType: 'view_metadata', ipAddress: '10.0.0.30', accessedAt: '2026-06-12T16:30:00+08:00' },
  { id: 7, userId: undefined, userType: 'anonymous', archiveId: 90, archiveNo: 'ARC-090', accessType: 'view_metadata', ipAddress: '202.100.1.9', accessedAt: '2026-06-12T14:00:00+08:00' },
  { id: 8, userId: 4, userType: 'public', userName: '公众用户B', archiveId: 90, archiveNo: 'ARC-090', accessType: 'download', ipAddress: '202.100.1.9', accessedAt: '2026-06-11T09:45:00+08:00' },
  { id: 9, userId: 3, userType: 'internal', userName: '小张', archiveId: 105, archiveNo: 'ARC-005', accessType: 'download', ipAddress: '10.0.0.20', accessedAt: '2026-06-11T08:20:00+08:00' },
  { id: 10, userId: 1, userType: 'internal', userName: '小刘', archiveId: 120, archiveNo: 'ARC-020', accessType: 'view_metadata', ipAddress: '10.0.0.12', accessedAt: '2026-06-10T17:00:00+08:00' },
  { id: 11, userId: undefined, userType: 'anonymous', archiveId: 88, archiveNo: 'ARC-088', accessType: 'preview', ipAddress: '203.0.113.2', accessedAt: '2026-06-10T12:00:00+08:00' },
  { id: 12, userId: 2, userType: 'internal', userName: '小李', archiveId: 110, archiveNo: 'ARC-010', accessType: 'preview', ipAddress: '10.0.0.30', accessedAt: '2026-06-09T15:40:00+08:00' },
]

export function mockAccessLogs(query?: AccessLogQuery): CursorData<ArchiveAccessLog> {
  let list = accessLogs
  if (query?.accessType) list = list.filter((l) => l.accessType === query.accessType)
  if (query?.userId) list = list.filter((l) => l.userId === query.userId)
  if (query?.archiveId) list = list.filter((l) => l.archiveId === query.archiveId)
  if (query?.startedAt) list = list.filter((l) => l.accessedAt >= query.startedAt!)
  if (query?.endedAt) list = list.filter((l) => l.accessedAt <= query.endedAt!)

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
