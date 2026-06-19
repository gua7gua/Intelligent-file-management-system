import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { FIXTURES, uniqueTitle } from '../../support/fixtures'

// 用例文档：测试用例/20-公众门户.md · P-COL-*
// 公众征集需登录公众账号

test.use({ storageState: authFile('zhouPublic') })

test.describe('公众征集 /public/collection', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/public/collection')
    // 页面同时有 h1「征集清单」与 h2「我的征集清单」，精确匹配 h1
    await expect(page.getByRole('heading', { name: '征集清单', exact: true })).toBeVisible()
  })

  test('P-COL-1 捐赠表单字段存在', async ({ page }) => {
    for (const id of ['#colTitle', '#contactName', '#contactPhone']) {
      await expect(page.locator(id)).toBeVisible()
    }
  })

  test('P-COL-2 未勾协议时提交禁用，勾选后启用', async ({ page }) => {
    const submit = page.getByRole('button', { name: '提交捐赠意向' })
    await expect(submit).toBeDisabled()
    await page.getByText(/我已阅读并同意/).click()
    await expect(submit).toBeEnabled()
  })

  test('P-COL-3 本地文件只解析名不上传', async ({ page }) => {
    await page.locator('#colFilePicker').setInputFiles(FIXTURES.sealPdf)
    // 选择只解析文件名：local-file 出现该文件名，且标注「未上传」（与 drop-zone 静态提示区分）
    await expect(page.locator('.local-file').filter({ hasText: 'test-seal.pdf' })).toBeVisible({ timeout: 5000 })
    await expect(page.locator('.local-file .status.info').getByText('未上传', { exact: true })).toBeVisible()
  })

  test('P-COL-4 保存草稿成功（幂等标题，后端要求至少 1 条目）', async ({ page }) => {
    const title = uniqueTitle('捐赠草稿')
    await page.locator('#colTitle').fill(title)
    await page.locator('#contactName').fill('测试小周')
    await page.locator('#contactPhone').fill('13800138000')
    await page.locator('#archiveYear').fill('2008')
    // 后端 /public/collections 要求至少一条条目，否则 BAD_REQUEST
    await page.getByRole('button', { name: '新增空白条目' }).click()
    await page.locator('.editable-table tbody tr').last().locator('input').first().fill('PW测试条目')
    await page.getByRole('button', { name: '保存草稿' }).click()
    await expect(page.getByText('草稿已保存')).toBeVisible({ timeout: 15_000 })
  })
})
