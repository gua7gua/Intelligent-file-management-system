import { describe, expect, it } from 'vitest'
import { canDisableUser, validateUserForm } from './userValidation'
import type { User } from '@/types/user-management'

const baseUser: User = {
  id: 1, userType: 'internal', loginName: 'a', realName: '甲', maxSecurityLevel: 0,
  dataScope: 'all', roleCodes: ['sys_admin'], status: 'active', createdAt: '2026-01-01T00:00:00+08:00',
}

describe('validateUserForm', () => {
  it('rejects empty login name', () => {
    const r = validateUserForm({ loginName: '', realName: '甲', roleCodes: ['back_archivist'], maxSecurityLevel: 0, dataScope: 'all', initialPassword: '123456' }, [], true)
    expect(r.valid).toBe(false)
    expect(r.errors).toContain('登录名不能为空')
  })
  it('rejects duplicate login name on create', () => {
    const r = validateUserForm({ loginName: 'liuxiao', realName: '甲', roleCodes: ['back_archivist'], maxSecurityLevel: 0, dataScope: 'all', initialPassword: '123456' }, ['liuxiao'], true)
    expect(r.valid).toBe(false)
  })
  it('rejects zero roles', () => {
    const r = validateUserForm({ loginName: 'new', realName: '甲', roleCodes: [], maxSecurityLevel: 0, dataScope: 'all', initialPassword: '123456' }, [], true)
    expect(r.errors).toContain('请至少选择一个预设角色')
  })
  it('rejects bad security level', () => {
    const r = validateUserForm({ loginName: 'new', realName: '甲', roleCodes: ['back_archivist'], maxSecurityLevel: 9, dataScope: 'all', initialPassword: '123456' }, [], true)
    expect(r.valid).toBe(false)
  })
  it('passes a valid create form', () => {
    const r = validateUserForm({ loginName: 'newuser', realName: '甲', roleCodes: ['back_archivist'], maxSecurityLevel: 2, dataScope: 'all', initialPassword: '123456' }, [], true)
    expect(r.valid).toBe(true)
  })
})

describe('canDisableUser', () => {
  it('blocks disabling last sys_admin', () => {
    const users: User[] = [{ ...baseUser, id: 1, loginName: 'only' }]
    const r = canDisableUser(users[0], users, { status: 'disabled' })
    expect(r.allowed).toBe(false)
  })
  it('allows disabling when another active admin exists', () => {
    const users: User[] = [{ ...baseUser, id: 1 }, { ...baseUser, id: 2, loginName: 'b' }]
    const r = canDisableUser(users[0], users, { status: 'disabled' })
    expect(r.allowed).toBe(true)
  })
  it('allows disabling non-admin', () => {
    const r = canDisableUser({ ...baseUser, roleCodes: ['back_archivist'] }, [baseUser], { status: 'disabled' })
    expect(r.allowed).toBe(true)
  })
})
