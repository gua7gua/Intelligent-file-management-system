import type { PageParams } from './api'

/** 组织查询参数（§17.4，用户管理页只读 + 编辑表单下拉） */
export interface OrganizationParams extends PageParams {
  status?: 'active' | 'disabled' | ''
  keyword?: string
}

/** 组织（对齐 DB 4.1 organizations） */
export interface Organization {
  id: number
  orgName: string
  /** archive_org / government / enterprise / public_institution */
  orgType: string
  contactName?: string
  contactPhone?: string
  status: 'active' | 'disabled'
}
