import request from './request'
import type { PageData } from '@/types/api'
import type {
  ResetPasswordData,
  Role,
  User,
  UserCreateData,
  UserDetail,
  UserParams,
  UserStatusData,
  UserUpdateData,
} from '@/types/user-management'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询用户（§22.1） */
export function getUsers(params?: UserParams): Promise<PageData<User>> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockUsers(params))
  }
  return request.get('/admin/users', { params })
}

/** 创建用户（§22.2） */
export function createUser(data: UserCreateData): Promise<UserDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockCreateUser(data))
  }
  return request.post('/admin/users', data)
}

/** 用户详情（§22.3） */
export function getUserDetail(userId: number): Promise<UserDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockUserDetail(userId))
  }
  return request.get(`/admin/users/${userId}`)
}

/** 更新用户（§22.4） */
export function updateUser(userId: number, data: UserUpdateData): Promise<UserDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockUpdateUser(userId, data))
  }
  return request.put(`/admin/users/${userId}`, data)
}

/** 禁用/启用用户（§22.5） */
export function updateUserStatus(userId: number, data: UserStatusData): Promise<UserDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockUpdateUserStatus(userId, data))
  }
  return request.put(`/admin/users/${userId}/status`, data)
}

/** 重置密码（§22.6） */
export function resetUserPassword(userId: number, data: ResetPasswordData): Promise<boolean> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockResetUserPassword(userId, data))
  }
  return request.post(`/admin/users/${userId}/reset-password`, data)
}

/** 查询角色（§22.7） */
export function getRoles(): Promise<Role[]> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockRoles())
  }
  return request.get('/admin/roles')
}
