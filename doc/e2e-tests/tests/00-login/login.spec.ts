import { test, expect } from '@playwright/test'
import { ACCOUNTS, type AccountKey, type Portal } from '../../support/accounts'
import { LoginPage } from '../../support/pages/LoginPage'

/** portal → 门户中文名 */
const PORTAL_NAME: Record<Portal, string> = {
  admin: '管理后台',
  transfer: '移交单位门户',
  internal: '内部查阅者门户',
  public: '公众门户',
}

function escapeRe(s: string): string {
  return s.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')
}

test.describe('登录与鉴权', () => {
  // 8 个角色逐一 UI 登录：填账号/密码/选角色/提交 → 落到对应门户默认页
  for (const key of Object.keys(ACCOUNTS) as AccountKey[]) {
    const acc = ACCOUNTS[key]
    test(`${acc.realName}（${acc.loginName}）UI 登录 → 进入${PORTAL_NAME[acc.portal]}`, async ({ page }) => {
      const login = new LoginPage(page)
      await login.goto()
      await login.loginAs(key)
      // 落到对应门户默认页（/admin、/transfer、/internal、/public）
      await expect(page).toHaveURL(new RegExp(escapeRe(acc.defaultPath)), { timeout: 20_000 })
      // 登录表单已卸载（确信跳转成功而非停留在登录页）
      await expect(login.accountInput).toBeHidden()
    })
  }

  test('错密码登录失败：显示错误提示且不跳转', async ({ page }) => {
    const login = new LoginPage(page)
    await login.goto()
    await login.fillCredentials('liu.back', 'wrong-password-xxx')
    await login.selectRole('back_archivist')
    await login.submit()
    // 登录页自身展示账号密码错误提示（R3 修复点：非「登录已过期」）
    await expect(login.errorMessage).toContainText('登录失败')
    // 仍停留在登录页
    await expect(page).toHaveURL(/\/login/)
  })

  test('选择角色后实时显示「登录后将进入：XX」', async ({ page }) => {
    const login = new LoginPage(page)
    await login.goto()
    await login.selectRole('transfer_user')
    await expect(login.targetPortalNotice).toContainText(PORTAL_NAME.transfer)
    await login.selectRole('internal_reader')
    await expect(login.targetPortalNotice).toContainText(PORTAL_NAME.internal)
  })
})
