import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'

// 用例文档：测试用例/50-管理后台.md · A-RES/A-COMP-*
// liu.back：数据研判（R3-C2 删除按钮/R3-C3 候选四要素入口）+ 档案编研（D3 一键入库/D6/D7 去关键词摘要）。

test.use({ storageState: authFile('liuBack') })

test.describe('数据研判 /admin/data-analysis', () => {
  test('A-RES-1 页面加载，「开始扫描」入口存在', async ({ page }) => {
    await page.goto('/admin/data-analysis')
    await expect(page.getByRole('heading', { name: '数据研判', exact: true })).toBeVisible()
    await expect(page.locator('#startScan')).toBeVisible()
  })
})

test.describe('档案编研 /admin/compilation', () => {
  test('A-COMP-1 「生成正文并入库」一键按钮存在（D3）', async ({ page }) => {
    await page.goto('/admin/compilation')
    await expect(page.getByRole('heading', { name: '档案编研', exact: true })).toBeVisible()
    // 按钮内含 icon「A」，accessible name 为「A生成正文并入库」，用正则 contains 匹配
    await expect(page.getByRole('button', { name: /生成正文并入库/ })).toBeVisible()
  })

  test('A-COMP-2 编辑区已去掉「关键词」「摘要」字段（D6/D7）', async ({ page }) => {
    await page.goto('/admin/compilation')
    await expect(page.getByText('关键词', { exact: true })).toHaveCount(0)
    await expect(page.getByText('摘要', { exact: true })).toHaveCount(0)
  })
})
