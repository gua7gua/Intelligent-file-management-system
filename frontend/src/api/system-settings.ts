import request from './request'
import type { ConfigBatchUpdateData, SystemConfig } from '@/types/system-settings'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询系统配置（§22.8） */
export function getSystemConfigs(): Promise<SystemConfig[]> {
  if (USE_MOCK) {
    return import('@/mock/modules/system-settings').then((m) => m.mockSystemConfigs())
  }
  return request.get('/admin/system-configs')
}

/** 更新单项配置（§22.9） */
export function updateSystemConfig(configKey: string, configValue: string): Promise<SystemConfig> {
  if (USE_MOCK) {
    return import('@/mock/modules/system-settings').then((m) => m.mockUpdateSystemConfig(configKey, configValue))
  }
  return request.put(`/admin/system-configs/${configKey}`, { configValue })
}

/** 批量更新配置（§22.10） */
export function batchUpdateSystemConfigs(data: ConfigBatchUpdateData): Promise<SystemConfig[]> {
  if (USE_MOCK) {
    return import('@/mock/modules/system-settings').then((m) => m.mockBatchUpdateSystemConfigs(data))
  }
  return request.put('/admin/system-configs', data)
}
