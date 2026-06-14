import type { PageData, PageParams } from './api'
import type { DataScopeValue, UserStatusValue, UserTypeValue } from './enums'

export interface UserParams extends PageParams {
  userType?: UserTypeValue | ''
  roleCode?: string | ''
  organizationId?: number
  status?: UserStatusValue | ''
  keyword?: string
}

export interface Role {
  id: number
  roleCode: string
  roleName: string
  description?: string
  enabled: boolean
}

export interface User {
  id: number
  userType: UserTypeValue
  loginName: string
  employeeNo?: string
  phone?: string
  realName: string
  organizationId?: number
  organizationName?: string
  departmentName?: string
  maxSecurityLevel: number
  dataScope: DataScopeValue
  /** 角色代码集合（user_roles） */
  roleCodes: string[]
  status: UserStatusValue
  createdAt: string
  updatedAt?: string
}

export interface UserDetail extends User {
  recentActions?: Array<{ operationType: string; moduleName: string; operatedAt: string }>
}

export interface UserCreateData {
  userType: UserTypeValue
  loginName: string
  employeeNo?: string
  phone?: string
  realName: string
  organizationId?: number
  departmentName?: string
  maxSecurityLevel: number
  dataScope: DataScopeValue
  roleCodes: string[]
  initialPassword: string
}

export interface UserUpdateData {
  realName?: string
  employeeNo?: string
  phone?: string
  organizationId?: number
  departmentName?: string
  maxSecurityLevel?: number
  dataScope?: DataScopeValue
  roleCodes?: string[]
}

export interface UserStatusData {
  status: UserStatusValue
  reason?: string
}

export interface ResetPasswordData {
  newPassword: string
}

export type UserPage = PageData<User>
