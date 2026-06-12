import request from './request'
import type { LoginParams, LoginResult } from '@/types/user'

/** 登录 */
export function loginApi(data: LoginParams): Promise<LoginResult> {
  return request.post('/auth/login', data)
}

/** 获取当前用户信息（用于刷新页面时恢复状态） */
export function getUserInfoApi(): Promise<LoginResult> {
  return request.get('/auth/me')
}

/** 登出 */
export function logoutApi(): Promise<void> {
  return request.post('/auth/logout')
}
