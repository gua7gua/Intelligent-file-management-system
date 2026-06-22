import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { expectNoIsoTime } from '../../support/expect'

// 用例文档：测试用例/40-移交门户.md · T-OV-*
// 移交工作台需登录移交经办人（小张·财政局）。回归 2.3/2.4 账号展示、移#2 时间汉化、人#3 详情切换。

test.use({ storageState: authFile('zhangTransfer') })

test.describe('移交工作台 /transfer/overview', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/transfer/overview')
    await expect(page.getByRole('heading', { name: '移交工作台', exact: true })).toBeVisible({ timeout: 20_000 })
  })

  test('T-OV-1 顶部展示当前账号（小张）', async ({ page }) => {
    await expect(page.getByText(/当前账号/)).toBeVisible()
    await expect(page.locator('body')).toContainText('小张')
  })

  test('T-OV-2 状态统计 4 个指标可见', async ({ page }) => {
    for (const label of ['草稿清单', '待移交', '部分接收', '已上架']) {
      await expect(page.getByText(label, { exact: true }).first()).toBeVisible()
    }
  })

  test('T-OV-3 点击非首行清单，右侧详情随之切换（人#3）', async ({ page }) => {
    const rows = page.locator('tr.batch-row')
    await rows.first().waitFor({ state: 'visible' })
    const count = await rows.count()
    test.skip(count < 2, '当前账号清单不足 2 条，跳过切换验证')

    // 取第二行清单号，点击后断言详情区出现同一清单号
    const secondBatchNo = (await rows.nth(1).locator('.mono').first().textContent())?.trim() ?? ''
    expect(secondBatchNo.length).toBeGreaterThan(0)
    await rows.nth(1).click()
    const detail = page.locator('aside[aria-label="清单详情"]')
    await expect(detail).toContainText(secondBatchNo, { timeout: 10_000 })
  })

  test('T-OV-4 页面无未格式化的 ISO 时间（移#2）', async ({ page }) => {
    await expectNoIsoTime(page.locator('body'))
  })
})
