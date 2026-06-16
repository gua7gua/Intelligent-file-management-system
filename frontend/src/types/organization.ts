import type { PageParams } from './api'

/** 组织查询参数（§17.4，用户管理页只读 + 编辑表单下拉） */
export interface OrganizationParams extends PageParams {
  status?: 'active' | 'disabled' | ''
  orgType?: string
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
  /** 关联全宗数（未删除），用于判断删除或停用 */
  fondsCount: number
  /** 关联用户数（未删除） */
  userCount: number
}

/** 新增组织请求体（§17.5） */
export interface OrganizationCreate {
  orgName: string
  orgType: 'archive_org' | 'government' | 'enterprise' | 'public_institution'
  contactName?: string
  contactPhone?: string
}

/** 更新组织请求体（§17.7，部分更新；停用通过 status=disabled） */
export interface OrganizationUpdate {
  orgName?: string
  orgType?: 'archive_org' | 'government' | 'enterprise' | 'public_institution'
  contactName?: string
  contactPhone?: string
  status?: 'active' | 'disabled'
}
