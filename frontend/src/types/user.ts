/** 用户基本信息 */
export interface UserInfo {
  id: number
  username: string
  realName: string
  phone: string
  organizationId: number
  organizationName: string
  department: string
  securityLevel: number
  dataScope: string
}

/** 登录请求参数 */
export interface LoginParams {
  username: string
  password: string
}

/** 登录响应数据 */
export interface LoginResult {
  token: string
  user: UserInfo
  roles: string[]
  permissions: string[]
}
