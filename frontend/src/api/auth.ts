import request from './request'
import type { LoginParams, LoginResult } from '@/types/user'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 登录 */
export function loginApi(data: LoginParams): Promise<LoginResult> {
  if (USE_MOCK) {
    return import('@/mock/modules/auth').then((m) => m.mockLoginResponse(data.loginName, data.portal))
  }
  return request.post('/auth/login', data)
}

/** 获取当前用户信息（恢复会话） */
export function getUserInfoApi(): Promise<{ user: LoginResult['user'] }> {
  if (USE_MOCK) {
    return import('@/mock/modules/auth').then((m) => m.mockUserInfo())
  }
  return request.get('/auth/me')
}

/** 登出 */
export function logoutApi(): Promise<void> {
  if (USE_MOCK) {
    return Promise.resolve()
  }
  return request.post('/auth/logout')
}
