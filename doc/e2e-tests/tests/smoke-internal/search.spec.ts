import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'

// 用例文档：测试用例/30-内部门户.md · I-SEARCH-*
// 内部检索需登录。回归 R3-C1 标签列、利用状态列、详情弹窗。
// 注意：本页 onMounted 不自动检索，需点「确认检索」。

test.use({ storageState: authFile('liReader') })

test.describe('内部检索 /internal/search', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/internal/search')
    await expect(page.getByText('结构化检索条件')).toBeVisible()
  })

  test('I-SEARCH-1 点确认检索后出结果', async ({ page }) => {
    await page.getByRole('button', { name: '确认检索' }).click()
    // 「共 N 条」同时出现在结果计数与分页器 .el-pagination__total，取首个（结果计数）
    await expect(page.getByText(/共 \d+ 条/).first()).toBeVisible({ timeout: 20_000 })
  })

  test('I-SEARCH-2 结果表含「标签」「利用状态」列（R3-C1）', async ({ page }) => {
    await page.getByRole('button', { name: '确认检索' }).click()
    // 「共 N 条」同时出现在结果计数与分页器 .el-pagination__total，取首个（结果计数）
    await expect(page.getByText(/共 \d+ 条/).first()).toBeVisible({ timeout: 20_000 })
    const thead = page.locator('.table-wrap thead').first()
    await expect(thead.getByText('标签', { exact: true })).toBeVisible()
    await expect(thead.getByText('利用状态', { exact: true })).toBeVisible()
  })

  test('I-SEARCH-3 分页器存在', async ({ page }) => {
    await page.getByRole('button', { name: '确认检索' }).click()
    // 「共 N 条」同时出现在结果计数与分页器 .el-pagination__total，取首个（结果计数）
    await expect(page.getByText(/共 \d+ 条/).first()).toBeVisible({ timeout: 20_000 })
    await expect(page.locator('.el-pagination')).toBeVisible()
  })

  test('I-SEARCH-4 点击详情打开弹窗', async ({ page }) => {
    await page.getByRole('button', { name: '确认检索' }).click()
    // 「共 N 条」同时出现在结果计数与分页器 .el-pagination__total，取首个（结果计数）
    await expect(page.getByText(/共 \d+ 条/).first()).toBeVisible({ timeout: 20_000 })
    await page.locator('.table-wrap tbody tr').first().getByRole('button', { name: '详情' }).click()
    await expect(page.locator('.el-dialog').filter({ hasText: '档案详情' })).toBeVisible()
  })
})
