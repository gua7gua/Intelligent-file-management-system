import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { smokePage } from '../../support/page-smoke'

// 用例文档：测试用例/50-管理后台.md · A-OV/A-ARCH/A-APP-*
// 后台核心角色 liu.back：概览 + 档案管理（R3-D2 全宗/R3-C1 标签/人#11 年度）+ 鉴定（R3-C5 真统计）。

test.use({ storageState: authFile('liuBack') })

test.describe('管理概览 /admin/overview（小刘）', () => {
  test('A-OV-1 概览加载，无英文枚举/ISO 时间', async ({ page }) => {
    await smokePage(page, '/admin/overview', '管理概览')
  })
})

test.describe('档案管理 /admin/archive-management', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/admin/archive-management')
    await expect(page.getByRole('heading', { name: '档案管理', exact: true })).toBeVisible()
  })

  test('A-ARCH-1 列表含「所属全宗」「标签」「年度」列（R3-D2/R3-C1/人#11）', async ({ page }) => {
    const thead = page.locator('.table-wrap thead').first()
    await expect(thead.getByText('所属全宗', { exact: true })).toBeVisible()
    await expect(thead.getByText('标签', { exact: true })).toBeVisible()
    await expect(thead.getByText('年度', { exact: true })).toBeVisible()
  })

  test('A-ARCH-2 分页器存在', async ({ page }) => {
    await expect(page.locator('.el-pagination')).toBeVisible()
  })
})

test.describe('档案鉴定 /admin/appraisal', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/admin/appraisal')
    await expect(page.getByRole('heading', { name: '档案鉴定', exact: true })).toBeVisible()
  })

  test('A-APP-1 四个指标卡可见（R3-C5 真统计）', async ({ page }) => {
    for (const label of ['即将到期档案', '待鉴定批次', '待销毁条目', '已生成清册']) {
      await expect(page.getByText(label, { exact: true }).first()).toBeVisible()
    }
  })

  test('A-APP-2 「创建鉴定批次」入口存在', async ({ page }) => {
    await expect(page.getByRole('button', { name: '创建鉴定批次', exact: true })).toBeVisible()
  })
})
