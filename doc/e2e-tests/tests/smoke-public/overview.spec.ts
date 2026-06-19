import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { expectNoIsoTime } from '../../support/expect'

// 用例文档：测试用例/20-公众门户.md · P-OV-*
// 公众概览需登录公众账号

test.use({ storageState: authFile('zhouPublic') })

test.describe('公众概览 /public/overview', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/public/overview')
    await expect(page.getByRole('heading', { name: '公众概览' })).toBeVisible()
  })

  test('P-OV-1 欢迎语含当前用户姓名（小周）', async ({ page }) => {
    await expect(page.getByText(/小周.*欢迎回来|欢迎回来.*小周/)).toBeVisible()
  })

  test('P-OV-2 公开馆藏统计区块渲染', async ({ page }) => {
    await expect(page.getByText('公开档案总量')).toBeVisible()
  })

  test('P-OV-3 征集清单与下载记录区块存在', async ({ page }) => {
    await expect(page.getByText('征集清单').first()).toBeVisible()
    await expect(page.getByText('下载记录')).toBeVisible()
  })

  test('P-OV-4 页面无未格式化的 ISO 时间', async ({ page }) => {
    await expectNoIsoTime(page.locator('body'))
  })
})
