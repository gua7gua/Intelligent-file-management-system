import type {
  BackupTask,
  BackupTaskCreateData,
  BackupTaskParams,
  FileCheckRecord,
  FileCheckRecordParams,
  TriggerCheckData,
} from '@/types/preservation'
import type { PageData } from '@/types/api'

let nextTaskId = 191
const tasks: BackupTask[] = [
  { id: 188, taskNo: 'BAK-188', backupScope: 'database', status: 'success', backupPath: '/backup/db/20260609.dump', fileSize: 9019431320, sha256: '9f2ac7e1d4', startedAt: '2026-06-09T02:00:00+08:00', finishedAt: '2026-06-09T02:10:00+08:00', message: '月度例行备份', createdAt: '2026-06-09T02:00:00+08:00' },
  { id: 189, taskNo: 'BAK-189', backupScope: 'files', status: 'success', backupPath: 'minio://backup/archive-files/20260609', fileSize: 689153961984, sha256: '71be0a3f09', startedAt: '2026-06-09T01:30:00+08:00', finishedAt: '2026-06-09T01:40:00+08:00', message: '正式电子文件备份', createdAt: '2026-06-09T01:30:00+08:00' },
  { id: 190, taskNo: 'BAK-190', backupScope: 'files', status: 'failed', backupPath: 'minio://backup/archive-files/20260608', fileSize: 0, sha256: '', startedAt: '2026-06-08T01:30:00+08:00', finishedAt: '2026-06-08T01:34:00+08:00', message: '对象存储连接超时，可重试', createdAt: '2026-06-08T01:30:00+08:00' },
]

// 最近一轮四性检测（target 为最近一份正式电子文件，id=5001）
const records: FileCheckRecord[] = [
  { id: 9101, targetType: 'archive_file', targetId: 5001, checkType: 'integrity', checkResult: 'passed', message: 'SHA-256 校验一致', checkedAt: '2026-06-15T09:00:00+08:00' },
  { id: 9102, targetType: 'archive_file', targetId: 5001, checkType: 'usability', checkResult: 'passed', message: '格式白名单内可打开', checkedAt: '2026-06-15T09:00:00+08:00' },
  { id: 9103, targetType: 'archive_file', targetId: 5001, checkType: 'authenticity', checkResult: 'not_configured', message: '外部签名体系未接入', checkedAt: '2026-06-15T09:00:00+08:00' },
  { id: 9104, targetType: 'archive_file', targetId: 5001, checkType: 'security', checkResult: 'passed', message: '病毒扫描通过', checkedAt: '2026-06-15T09:00:00+08:00' },
]

function paginate<T>(list: T[], pageNo = 1, pageSize = 20): PageData<T> {
  const total = list.length
  const start = (pageNo - 1) * pageSize
  return { records: list.slice(start, start + pageSize), pageNo, pageSize, total, hasNext: start + pageSize < total }
}

export function mockBackupTasks(params?: BackupTaskParams): PageData<BackupTask> {
  let list = [...tasks].sort((a, b) => b.id - a.id)
  if (params?.status) list = list.filter((t) => t.status === params.status)
  if (params?.backupScope) list = list.filter((t) => t.backupScope === params.backupScope)
  return paginate(list, params?.pageNo, params?.pageSize)
}

export function mockCreateBackupTask(data: BackupTaskCreateData): BackupTask {
  const running = tasks.find((t) => t.status === 'running')
  if (running) {
    throw new Error('当前已有运行中的备份任务，请等待其完成')
  }
  const id = nextTaskId++
  const now = '2026-06-15T10:00:00+08:00'
  const task: BackupTask = {
    id,
    taskNo: `BAK-${id}`,
    backupScope: data.backupScope,
    status: 'running',
    backupPath: '等待生成',
    fileSize: 0,
    sha256: '',
    startedAt: now,
    message: '手动发起备份任务',
    createdAt: now,
  }
  tasks.unshift(task)
  // mock 模拟异步完成：1.2s 后置成功
  setTimeout(() => {
    task.status = 'success'
    task.backupPath = data.backupScope === 'database' ? `/backup/db/manual-${id}.dump` : `minio://backup/manual-${id}`
    task.fileSize = data.backupScope === 'database' ? 8500000000 : 680000000000
    task.sha256 = Math.random().toString(16).slice(2, 12)
    task.finishedAt = '2026-06-15T10:01:12+08:00'
  }, 1200)
  return task
}

export function mockFileCheckRecords(params?: FileCheckRecordParams): PageData<FileCheckRecord> {
  let list = [...records].sort((a, b) => b.id - a.id)
  if (params?.targetType) list = list.filter((r) => r.targetType === params.targetType)
  if (params?.targetId) list = list.filter((r) => r.targetId === params.targetId)
  if (params?.checkType) list = list.filter((r) => r.checkType === params.checkType)
  if (params?.checkResult) list = list.filter((r) => r.checkResult === params.checkResult)
  return paginate(list, params?.pageNo, params?.pageSize)
}

export function mockTriggerFileCheck(fileId: number, data: TriggerCheckData): FileCheckRecord[] {
  const now = '2026-06-15T10:05:00+08:00'
  const resultMap: Record<string, FileCheckRecord['checkResult']> = {
    integrity: 'passed',
    usability: 'passed',
    authenticity: 'not_configured',
    security: 'passed',
  }
  // 移除该 target 旧记录，写入新记录
  const filtered = records.filter((r) => !(r.targetType === 'archive_file' && r.targetId === fileId))
  records.length = 0
  records.push(...filtered)
  const created: FileCheckRecord[] = []
  for (const ct of data.checkTypes) {
    const rec: FileCheckRecord = {
      id: Math.floor(Math.random() * 100000) + 9200,
      targetType: 'archive_file',
      targetId: fileId,
      checkType: ct,
      checkResult: resultMap[ct] ?? 'passed',
      message: ct === 'authenticity' ? '外部签名体系未接入' : '检测通过',
      checkedAt: now,
    }
    records.push(rec)
    created.push(rec)
  }
  return created
}
