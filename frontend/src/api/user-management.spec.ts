import { describe, expect, it } from 'vitest'
import { createUser, getRoles, getUserDetail, getUsers, updateUserStatus } from './user-management'

describe('user-management api mock mode', () => {
  it('lists users and filters by role', async () => {
    const all = await getUsers()
    expect(all.records.length).toBeGreaterThan(0)
    const admins = await getUsers({ roleCode: 'sys_admin' })
    expect(admins.records.every((u) => u.roleCodes.includes('sys_admin'))).toBe(true)
  })

  it('user detail includes recent actions', async () => {
    const d = await getUserDetail(11)
    expect(d.realName).toBe('小刘')
    expect(d.recentActions?.length).toBeGreaterThan(0)
  })

  it('throws on duplicate login name', async () => {
    await expect(createUser({ userType: 'internal', loginName: 'liuxiao', realName: '重复', maxSecurityLevel: 0, dataScope: 'all', roleCodes: ['back_archivist'], initialPassword: '123456' })).rejects.toThrow('登录名已存在')
  })

  it('creates user with new login name', async () => {
    const u = await createUser({ userType: 'internal', loginName: 'newperson', realName: '新人', maxSecurityLevel: 1, dataScope: 'own_org', roleCodes: ['internal_reader'], initialPassword: '123456' })
    expect(u.status).toBe('active')
  })

  it('rejects disabling the last remaining active sys_admin', async () => {
    const admins = await getUsers({ roleCode: 'sys_admin', status: 'active' })
    const a01 = admins.records.find((u) => u.loginName === 'admin01')!
    const a02 = admins.records.find((u) => u.loginName === 'admin02')!
    // 先禁用 admin01（仍剩 admin02，应放行），使 admin02 成为最后一个活跃 sys_admin
    await updateUserStatus(a01.id, { status: 'disabled', reason: '测试' })
    // 再禁用 admin02 应被拦截
    await expect(updateUserStatus(a02.id, { status: 'disabled', reason: '测试' })).rejects.toThrow('最后一个系统管理员')
  })

  it('returns enabled roles', async () => {
    const roles = await getRoles()
    expect(roles.length).toBeGreaterThan(0)
    expect(roles.every((r) => r.enabled)).toBe(true)
  })
})

