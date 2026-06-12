/** 用户信息（对齐接口文档 §3.1 返回的 user 对象） */
export interface UserInfo {
  id: number
  realName: string
  userType: 'internal' | 'public'
  roles: string[]
  organizationId: number
  maxSecurityLevel: number
  dataScope: string
}

/** 登录请求参数（对齐接口文档 §3.1） */
export interface LoginParams {
  loginName: string
  password: string
  portal: string
}

/** 登录响应数据（对齐接口文档 §3.1） */
export interface LoginResult {
  token: string
  user: UserInfo
  defaultRoute: string
}
