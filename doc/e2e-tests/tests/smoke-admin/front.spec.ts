import { test } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { smokePage } from '../../support/page-smoke'

// 用例文档：测试用例/50-管理后台.md · A-FRONT-*
// 前台角色 chen.front：概览 + 征集 + 移交验收 + 借阅审批，通用冒烟（加载/无枚举/无ISO）。

test.use({ storageState: authFile('chenFront') })

test.describe('前台工作人员（小陈）页面冒烟', () => {
  test('A-FRONT-1 管理概览', async ({ page }) => {
    await smokePage(page, '/admin/overview', '管理概览')
  })

  test('A-FRONT-2 征集管理与接收', async ({ page }) => {
    await smokePage(page, '/admin/collection', '征集管理与接收')
  })

  test('A-FRONT-3 移交验收与电子文件上传', async ({ page }) => {
    await smokePage(page, '/admin/transfer-reception', '移交验收与电子文件上传')
  })

  test('A-FRONT-4 借阅审批', async ({ page }) => {
    await smokePage(page, '/admin/borrow-approval', '借阅审批')
  })
})
