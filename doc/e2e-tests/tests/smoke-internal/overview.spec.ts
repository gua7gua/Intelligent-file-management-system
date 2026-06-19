import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { expectNoIsoTime } from '../../support/expect'

// 用例文档：测试用例/30-内部门户.md · I-OV-*
// 内部工作台需登录内部查阅者（小李）。回归人#4「一直加载工作台…」。

test.use({ storageState: authFile('liReader') })

test.describe('内部工作台 /internal/overview', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/internal/overview')
    // 副标题唯一，作为加载完成锚点（不再卡在「加载工作台…」）
    await expect(page.getByText('提交申请后可在本页跟踪审批与到馆核验进度。')).toBeVisible({ timeout: 20_000 })
  })

  test('I-OV-1 工作台正常加载（非「加载工作台…」）', async ({ page }) => {
    await expect(page.getByText('加载工作台…')).toHaveCount(0)
  })

  test('I-OV-2 「我的最近查阅」「我的借阅申请」区块存在', async ({ page }) => {
    await expect(page.getByText('我的最近查阅').first()).toBeVisible()
    await expect(page.getByText('我的借阅申请').first()).toBeVisible()
  })

  test('I-OV-3 页面无未格式化的 ISO 时间', async ({ page }) => {
    await expectNoIsoTime(page.locator('body'))
  })
})
