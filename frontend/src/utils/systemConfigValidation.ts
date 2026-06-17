import type { SystemConfig } from '@/types/system-settings'

export interface ConfigValidationResult {
  valid: boolean
  errors: string[]
}

/** 数值范围校验规则（key → [min, max]） */
const numberRanges: Record<string, [number, number]> = {
  'upload.max_file_size_mb': [1, 2048],
  'warehouse.usage_warning_threshold': [0, 1],
  'borrow.default_days': [1, 90],
}

/** 校验待保存的配置项集合（仅 editable 项） */
export function validateConfigs(items: SystemConfig[]): ConfigValidationResult {
  const errors: string[] = []
  for (const item of items) {
    if (!item.editable) continue
    const range = numberRanges[item.configKey]
    if (range) {
      const n = Number(item.configValue)
      if (Number.isNaN(n) || n < range[0] || n > range[1]) {
        errors.push(`${item.configKey} 必须在 ${range[0]} 到 ${range[1]} 之间`)
      }
    }
    if (item.valueType === 'boolean' && !['true', 'false'].includes(item.configValue)) {
      errors.push(`${item.configKey} 必须为 true 或 false`)
    }
  }
  return { valid: errors.length === 0, errors }
}

/** 白名单字符串规范化：去点、小写、去重 */
export function normalizeExtensions(raw: string[]): string[] {
  const seen = new Set<string>()
  const result: string[] = []
  for (const ext of raw) {
    const clean = ext.trim().replace(/\./g, '').toLowerCase()
    if (clean && !seen.has(clean)) {
      seen.add(clean)
      result.push(clean)
    }
  }
  return result
}
