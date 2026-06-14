import type { User, UserStatusData } from '@/types/user-management'

/** 校验结果：valid 为 true 时 errors 为空 */
export interface ValidationResult {
  valid: boolean
  errors: string[]
}

/** 校验新建/编辑用户表单 */
export function validateUserForm(
  data: { loginName: string; realName: string; roleCodes: string[]; maxSecurityLevel: number; dataScope: string; initialPassword?: string },
  existingLoginNames: string[],
  isCreate: boolean,
): ValidationResult {
  const errors: string[] = []
  if (!data.loginName.trim()) errors.push('登录名不能为空')
  else if (isCreate && existingLoginNames.includes(data.loginName.trim())) errors.push('登录名已存在')
  if (!data.realName.trim()) errors.push('姓名不能为空')
  if (!data.roleCodes || data.roleCodes.length === 0) errors.push('请至少选择一个预设角色')
  if (data.maxSecurityLevel < 0 || data.maxSecurityLevel > 4) errors.push('密级上限必须在 0–4 之间')
  if (!['own_org', 'own_fonds', 'all'].includes(data.dataScope)) errors.push('数据范围不合法')
  if (isCreate && (!data.initialPassword || data.initialPassword.length < 6)) errors.push('初始密码至少 6 位')
  return { valid: errors.length === 0, errors }
}

/** 判断禁用某用户是否会移除最后一个 sys_admin */
export function canDisableUser(target: User, allUsers: User[], data: UserStatusData): { allowed: boolean; reason?: string } {
  if (data.status !== 'disabled') return { allowed: true }
  if (!target.roleCodes.includes('sys_admin')) return { allowed: true }
  const activeAdmins = allUsers.filter((u) => u.roleCodes.includes('sys_admin') && u.status === 'active')
  if (activeAdmins.length <= 1) {
    return { allowed: false, reason: '不能禁用最后一个系统管理员' }
  }
  return { allowed: true }
}

/** 手机号格式（宽松：11 位数字） */
export function isValidPhone(phone: string): boolean {
  return /^\d{11}$/.test(phone.trim())
}
