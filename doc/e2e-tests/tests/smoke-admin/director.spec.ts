import { test } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { smokePage } from '../../support/page-smoke'

// 用例文档：测试用例/50-管理后台.md · A-DIR-*
// 馆领导 fang.director：概览 + 审批工作台 + 数据统计，通用冒烟。

test.use({ storageState: authFile('fangDirector') })

test.describe('馆领导（小方）页面冒烟', () => {
  test('A-DIR-1 管理概览', async ({ page }) => {
    await smokePage(page, '/admin/overview', '管理概览')
  })

  test('A-DIR-2 审批工作台', async ({ page }) => {
    await smokePage(page, '/admin/approval', '审批工作台')
  })

  test('A-DIR-3 数据统计', async ({ page }) => {
    await smokePage(page, '/admin/statistics', '数据统计')
  })
})
