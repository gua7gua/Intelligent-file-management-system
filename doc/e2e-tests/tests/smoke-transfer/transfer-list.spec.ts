import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { uniqueTitle } from '../../support/fixtures'

// 用例文档：测试用例/40-移交门户.md · T-LIST-*
// 编制移交清单需登录移交经办人（小张）。回归 2.1/2.2 部门电话预填、2.7 页数列。

test.use({ storageState: authFile('zhangTransfer') })

test.describe('编制移交清单 /transfer/transfer-list', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/transfer/transfer-list')
    await expect(page.getByRole('heading', { name: '编制移交清单', exact: true })).toBeVisible()
  })

  test('T-LIST-1 移交部门与联系电话自动预填当前账号（2.1/2.2）', async ({ page }) => {
    const dept = await page.locator('#department').inputValue()
    const phone = await page.locator('#contactPhone').inputValue()
    expect(dept.length).toBeGreaterThan(0)
    expect(phone.length).toBeGreaterThan(0)
  })

  test('T-LIST-2 条目表含「页数」列（2.7）', async ({ page }) => {
    await expect(page.locator('.editable-table thead, .table-wrap thead').first().getByText('页数', { exact: true })).toBeVisible()
  })

  test('T-LIST-3 保存草稿成功（幂等标题）', async ({ page }) => {
    await page.locator('#listTitle').fill(uniqueTitle('移交草稿'))
    await page.locator('#archiveYear').fill('2020')
    await page.getByRole('button', { name: '新增空白条目' }).click()
    // 后端要求条目「档案标题」非空，填入新增条目标题（行内首个 input）
    await page.locator('tbody tr').last().locator('input').first().fill('PW测试条目')
    // 用 UI 点击触发 + 验证后端创建成功（POST 200），比依赖 toast 更稳
    const created = page.waitForResponse(
      (r) => r.url().includes('/transfer/batches') && r.request().method() === 'POST' && r.status() === 200,
    )
    await page.getByRole('button', { name: '保存草稿' }).click()
    await created
  })
})
