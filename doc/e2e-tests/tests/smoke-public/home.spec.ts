import { test, expect } from '@playwright/test'

// 用例文档：测试用例/20-公众门户.md · P-HOME-*
// 公众首页免登录

test.describe('公众首页 /public/index', () => {
  test('P-HOME-1 首页加载：标题与公开档案统计可见', async ({ page }) => {
    await page.goto('/public/index')
    await expect(page.locator('#home-title')).toBeVisible()
    await expect(page.getByText('公开档案总量')).toBeVisible()
  })

  test('P-HOME-2 快速检索跳转检索页并携带关键词', async ({ page }) => {
    await page.goto('/public/index')
    await page.locator('#quickKeyword').fill('财政')
    await page.getByRole('button', { name: '检索', exact: true }).click()
    await expect(page).toHaveURL(/\/public\/search/)
    await expect(page).toHaveURL(/keyword=/)
  })
})
