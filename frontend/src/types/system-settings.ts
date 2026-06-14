import type { ConfigValueTypeValue } from './enums'

export interface SystemConfig {
  configKey: string
  configValue: string
  valueType: ConfigValueTypeValue
  editable: boolean
  description?: string
  updatedAt?: string
}

export interface ConfigUpdateItem {
  configKey: string
  configValue: string
}

export interface ConfigBatchUpdateData {
  items: ConfigUpdateItem[]
}
