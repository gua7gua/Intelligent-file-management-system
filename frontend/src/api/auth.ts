import request from './request'
import type { LoginParams, LoginResult } from '@/types/user'
import type {
  PublicRegisterRequest,
  PublicResetPasswordRequest,
  PublicSmsCodeRequest,
} from '@/types/public'

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

/** 发送公众短信验证码 */
export function sendPublicSmsCode(data: PublicSmsCodeRequest): Promise<boolean> {
  if (USE_MOCK) {
    return import('@/mock/modules/public').then((m) => m.mockSendPublicSmsCode(data))
  }
  return request.post('/public/auth/sms-code', data)
}

/** 公众注册 */
export function registerPublicUser(data: PublicRegisterRequest): Promise<{ id: number; realName: string; roles: string[] }> {
  if (USE_MOCK) {
    return import('@/mock/modules/public').then((m) => m.mockRegisterPublicUser(data))
  }
  return request.post('/public/auth/register', data)
}

/** 公众重置密码 */
export function resetPublicPassword(data: PublicResetPasswordRequest): Promise<boolean> {
  if (USE_MOCK) {
    return import('@/mock/modules/public').then((m) => m.mockResetPublicPassword(data))
  }
  return request.post('/public/auth/reset-password', data)
}
