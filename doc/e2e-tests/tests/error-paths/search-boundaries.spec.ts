import { test, expect } from '@playwright/test'

// 用例文档：测试用例/70-异常路径.md · E-SEARCH-*
// 公众检索权限边界：匿名公众只能检索「非密 + 公开」档案，涉密/非公开档案不应出现（安全：不泄露存在性）。

test.describe('公众检索权限边界', () => {
  test('E-SEARCH-1 涉密档案（ARC-000001，内部级）公众检索不到', async ({ page }) => {
    await page.goto('/public/search')
    await expect(page.getByRole('heading', { name: '公开档案检索' })).toBeVisible()
    // ARC-000001 为内部密级档案，公众端按密级+开放状态硬过滤，不应命中
    await page.locator('#searchArchiveNo').fill('ARC-000001')
    await page.getByRole('button', { name: '检索', exact: true }).click()
    await expect(page.getByText('没有符合条件的公开档案')).toBeVisible({ timeout: 15_000 })
  })
})
