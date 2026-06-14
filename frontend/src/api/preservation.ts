import request from './request'
import type { PageData } from '@/types/api'
import type {
  BackupTask,
  BackupTaskCreateData,
  BackupTaskParams,
  FileCheckRecord,
  FileCheckRecordParams,
  TriggerCheckData,
} from '@/types/preservation'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询备份任务（§21.1） */
export function getBackupTasks(params?: BackupTaskParams): Promise<PageData<BackupTask>> {
  if (USE_MOCK) {
    return import('@/mock/modules/preservation').then((m) => m.mockBackupTasks(params))
  }
  return request.get('/admin/backup-tasks', { params })
}

/** 创建备份任务（§21.2） */
export function createBackupTask(data: BackupTaskCreateData): Promise<BackupTask> {
  if (USE_MOCK) {
    return import('@/mock/modules/preservation').then((m) => m.mockCreateBackupTask(data))
  }
  return request.post('/admin/backup-tasks', data)
}

/** 查询四性检测记录（§21.3） */
export function getFileCheckRecords(params?: FileCheckRecordParams): Promise<PageData<FileCheckRecord>> {
  if (USE_MOCK) {
    return import('@/mock/modules/preservation').then((m) => m.mockFileCheckRecords(params))
  }
  return request.get('/admin/file-check-records', { params })
}

/** 触发正式文件检测（§21.4） */
export function triggerFileCheck(fileId: number, data: TriggerCheckData): Promise<FileCheckRecord[]> {
  if (USE_MOCK) {
    return import('@/mock/modules/preservation').then((m) => m.mockTriggerFileCheck(fileId, data))
  }
  return request.post(`/admin/archive-files/${fileId}/checks`, data)
}
