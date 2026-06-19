import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'

// 用例文档：测试用例/30-内部门户.md · I-BR-*
// 我的借阅申请列表（小李）。回归 B7-5 字段扁平化。

test.use({ storageState: authFile('liReader') })

test.describe('我的借阅申请 /internal/borrow-requests', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/internal/borrow-requests')
    await expect(page.getByRole('heading', { name: '我的借阅申请', exact: true })).toBeVisible()
  })

  test('I-BR-1 列表页加载，表头齐全', async ({ page }) => {
    const thead = page.locator('.table-wrap thead').first()
    for (const col of ['申请号', '档案题名', '申请时间', '状态', '操作']) {
      await expect(thead.getByText(col, { exact: true })).toBeVisible()
    }
  })

  test('I-BR-2 「发起新申请」入口存在且指向检索页', async ({ page }) => {
    const link = page.getByRole('link', { name: '发起新申请' })
    await expect(link).toBeVisible()
    await link.click()
    await expect(page).toHaveURL(/\/internal\/search/)
  })
})
