import type { Page, Locator } from '@playwright/test'
import { PASSWORD } from '../env'
import { ACCOUNTS, type AccountKey } from '../accounts'
import { pickElSelectOption } from '../el-select'

/** 登录角色 → 登录页「登录角色」下拉显示名（见 views/login/index.vue） */
const ROLE_LABELS: Record<string, string> = {
  front_archivist: '档案管理员（前台）',
  back_archivist: '档案管理员（后台）',
  transfer_user: '移交单位经办人',
  internal_reader: '内部查阅者',
  public_user: '社会公众',
  director: '馆领导',
  sys_admin: '系统管理员',
}

/** 登录页面对象：封装统一登录表单的交互 */
export class LoginPage {
  readonly page: Page
  readonly accountInput: Locator
  readonly passwordInput: Locator
  readonly roleSelect: Locator
  readonly submitButton: Locator
  readonly targetPortalNotice: Locator
  readonly errorMessage: Locator

  constructor(page: Page) {
    this.page = page
    this.accountInput = page.locator('#account')
    this.passwordInput = page.locator('#password')
    // 注意：指向 .el-select 容器而非内部 #role input（后者被遮挡，见 support/el-select）
    this.roleSelect = page.locator('.el-select:has(#role)')
    this.submitButton = page.getByRole('button', { name: '登录', exact: true })
    this.targetPortalNotice = page.locator('.notice', { hasText: '登录后将进入' })
    this.errorMessage = page.locator('.login-status .notice')
  }

  async goto(): Promise<void> {
    await this.page.goto('/login')
    await this.accountInput.waitFor({ state: 'visible' })
  }

  /** 选择登录角色（Element Plus el-select，下拉项 teleport 到 body） */
  async selectRole(role: string): Promise<void> {
    const label = ROLE_LABELS[role]
    if (!label) throw new Error(`未知角色 ${role}，缺少下拉显示名映射`)
    await pickElSelectOption(this.page, this.roleSelect, label)
  }

  /** 填账号/密码（不选角色、不提交） */
  async fillCredentials(loginName: string, password: string): Promise<void> {
    await this.accountInput.fill(loginName)
    await this.passwordInput.fill(password)
  }

  /** 以某账号完整 UI 登录：填账号 + 默认密码 + 选角色 + 提交 */
  async loginAs(key: AccountKey): Promise<void> {
    const acc = ACCOUNTS[key]
    await this.fillCredentials(acc.loginName, PASSWORD)
    await this.selectRole(acc.role)
    await this.submitButton.click()
  }

  async submit(): Promise<void> {
    await this.submitButton.click()
  }
}
