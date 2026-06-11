import type { FormItemRule } from 'element-plus'

/** ProTable 列定义 */
export interface ProTableColumn {
  prop: string
  label: string
  width?: number | string
  minWidth?: number | string
  dict?: string
  sourceType?: string
  formatter?: (row: any) => string
  slot?: string
  fixed?: 'left' | 'right'
  sortable?: boolean
}

/** ProTable 搜索字段 */
export interface SearchField {
  prop: string
  label: string
  type: 'input' | 'select' | 'dateRange' | 'date' | 'number'
  dict?: string
  defaultValue?: any
}

/** ProForm 表单字段 */
export interface ProFormField {
  prop: string
  label: string
  type: 'input' | 'textarea' | 'number' | 'select' | 'date' | 'dateRange'
      | 'radio' | 'checkbox' | 'switch' | 'upload'
  required?: boolean
  dict?: string
  rules?: FormItemRule[]
  placeholder?: string
  disabled?: boolean
  span?: number
}

/** 字典项 */
export interface DictItem {
  value: string | number
  label: string
  tagType?: '' | 'success' | 'warning' | 'danger' | 'info'
}
