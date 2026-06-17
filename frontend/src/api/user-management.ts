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

/**
 * 后端 UserInfoResponse 适配为前端 User 类型：
 * - 后端字段 `roles`（List<String>）→ 前端 `roleCodes`
 * - 后端 userType 因 `public` 在 Java 是关键字，DB 存 `public_` → 前端统一 `public`
 * 仅做字段名映射，不改语义。
 */
function adaptUser<T extends Record<string, unknown>>(raw: T): T {
  if (!raw || typeof raw !== 'object') return raw
  const out: Record<string, unknown> = { ...raw }
  if (out.roleCodes === undefined && Array.isArray(out.roles)) {
    out.roleCodes = out.roles
    delete out.roles
  }
  if (out.userType === 'public_') {
    out.userType = 'public'
  }
  return out as T
}

/** 查询用户（§22.1） */
export async function getUsers(params?: UserParams): Promise<PageData<User>> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockUsers(params))
  }
  const res = await request.get<{ records: User[]; pageNo: number; pageSize: number; total: number; hasNext: boolean }>('/admin/users', { params })
  return { ...res, records: (res.records || []).map((u) => adaptUser(u as unknown as Record<string, unknown>)) as User[] }
}

/** 创建用户（§22.2） */
export function createUser(data: UserCreateData): Promise<UserDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockCreateUser(data))
  }
  return request.post('/admin/users', data).then((r) => adaptUser(r as Record<string, unknown>)) as Promise<UserDetail>
}

/** 用户详情（§22.3） */
export function getUserDetail(userId: number): Promise<UserDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockUserDetail(userId))
  }
  return request.get(`/admin/users/${userId}`).then((r) => adaptUser(r as Record<string, unknown>)) as Promise<UserDetail>
}

/** 更新用户（§22.4） */
export function updateUser(userId: number, data: UserUpdateData): Promise<UserDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockUpdateUser(userId, data))
  }
  return request.put(`/admin/users/${userId}`, data).then((r) => adaptUser(r as Record<string, unknown>)) as Promise<UserDetail>
}

/** 禁用/启用用户（§22.5） */
export function updateUserStatus(userId: number, data: UserStatusData): Promise<UserDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/user-management').then((m) => m.mockUpdateUserStatus(userId, data))
  }
  return request.put(`/admin/users/${userId}/status`, data).then((r) => adaptUser(r as Record<string, unknown>)) as Promise<UserDetail>
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
