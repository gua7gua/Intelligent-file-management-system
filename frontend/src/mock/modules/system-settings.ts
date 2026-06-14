import type { ConfigBatchUpdateData, SystemConfig } from '@/types/system-settings'

const configs: SystemConfig[] = [
  { configKey: 'upload.allowed_extensions', configValue: 'pdf,docx,jpg,png,mp4', valueType: 'json', editable: true, description: '文件上传格式白名单', updatedAt: '2026-06-14T09:22:00+08:00' },
  { configKey: 'upload.max_file_size_mb', configValue: '512', valueType: 'number', editable: true, description: '最大上传大小', updatedAt: '2026-06-14T09:22:00+08:00' },
  { configKey: 'ai.enabled', configValue: 'true', valueType: 'boolean', editable: true, description: 'AI 功能统一开关', updatedAt: '2026-06-14T09:22:00+08:00' },
  { configKey: 'public_search.enabled', configValue: 'true', valueType: 'boolean', editable: true, description: '公众公开检索入口', updatedAt: '2026-06-14T09:22:00+08:00' },
  { configKey: 'warehouse.usage_warning_threshold', configValue: '85', valueType: 'number', editable: true, description: '库房占用告警阈值', updatedAt: '2026-06-14T09:22:00+08:00' },
  { configKey: 'borrow.default_days', configValue: '14', valueType: 'number', editable: true, description: '默认借阅天数', updatedAt: '2026-06-14T09:22:00+08:00' },
  { configKey: 'collection.agreement_text', configValue: '捐赠人声明材料来源合法、权属清晰，同意无偿捐赠，并允许档案馆按规定整理、保存和公开利用。', valueType: 'string', editable: true, description: '公众提交征集清单前展示并勾选同意', updatedAt: '2026-06-14T09:22:00+08:00' },
  { configKey: 'security.audit_retention', configValue: 'permanent', valueType: 'string', editable: false, description: '审计日志保留策略，不允许页面修改', updatedAt: '2026-06-01T00:00:00+08:00' },
]

export function mockSystemConfigs(): SystemConfig[] {
  return configs.map((c) => ({ ...c }))
}

export function mockUpdateSystemConfig(key: string, value: string): SystemConfig {
  const c = configs.find((x) => x.configKey === key)
  if (!c) throw new Error('配置项不存在：' + key)
  if (!c.editable) throw new Error('该配置项不允许修改：' + key)
  c.configValue = value
  c.updatedAt = '2026-06-15T10:00:00+08:00'
  return { ...c }
}

export function mockBatchUpdateSystemConfigs(data: ConfigBatchUpdateData): SystemConfig[] {
  const updated: SystemConfig[] = []
  for (const item of data.items) {
    updated.push(mockUpdateSystemConfig(item.configKey, item.configValue))
  }
  return updated
}
