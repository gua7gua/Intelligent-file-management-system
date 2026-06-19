import { test, expect } from '@playwright/test'

// 用例文档：测试用例/20-公众门户.md · P-SEARCH-*
// 公众检索免登录（下载才需登录）。覆盖第三轮 R3-C1 标签列、分页、详情弹窗。

test.describe('公众检索 /public/search', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/public/search')
    await expect(page.getByRole('heading', { name: '公开档案检索' })).toBeVisible()
    // onMounted 自动检索：等结果区出现
    await expect(page.getByText(/检索结果（\d+ 条）/)).toBeVisible({ timeout: 20_000 })
  })

  test('P-SEARCH-1 默认检索出结果：结果表有数据行', async ({ page }) => {
    const rowCount = await page.locator('.table-wrap tbody tr').count()
    expect(rowCount).toBeGreaterThan(0)
  })

  test('P-SEARCH-2 标签列存在（R3-C1）', async ({ page }) => {
    await expect(page.locator('.table-wrap thead').getByText('标签', { exact: true })).toBeVisible()
  })

  test('P-SEARCH-3 分页器存在', async ({ page }) => {
    await expect(page.locator('.el-pagination')).toBeVisible()
  })

  test('P-SEARCH-4 关键词检索更新结果', async ({ page }) => {
    await page.locator('#searchKeyword').fill('财政')
    await page.getByRole('button', { name: '检索', exact: true }).click()
    await expect(page.getByText(/检索结果（\d+ 条）/)).toBeVisible()
  })

  test('P-SEARCH-5 无结果时显示空提示', async ({ page }) => {
    await page.locator('#searchKeyword').fill('ZZZ不存在的关键词XYZ123')
    await page.getByRole('button', { name: '检索', exact: true }).click()
    await expect(page.getByText('没有符合条件的公开档案')).toBeVisible()
  })

  test('P-SEARCH-6 点击详情打开弹窗并标记「公开」', async ({ page }) => {
    await page.locator('.table-wrap tbody tr').first().getByRole('button', { name: '详情' }).click()
    const dialog = page.locator('.el-dialog').filter({ hasText: '档案详情' })
    await expect(dialog).toBeVisible()
    await expect(dialog.locator('.status').getByText('公开', { exact: true })).toBeVisible()
  })
})
