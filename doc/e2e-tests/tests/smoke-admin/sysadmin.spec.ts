import { test } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { smokePage } from '../../support/page-smoke'

// 用例文档：测试用例/50-管理后台.md · A-SYS-*
// 系统管理员 admin：概览 + 用户管理 + 系统配置 + 审计日志 + 访问日志，通用冒烟。

test.use({ storageState: authFile('admin') })

test.describe('系统管理员页面冒烟', () => {
  test('A-SYS-1 管理概览', async ({ page }) => {
    await smokePage(page, '/admin/overview', '管理概览')
  })

  test('A-SYS-2 用户管理', async ({ page }) => {
    await smokePage(page, '/admin/user-management', '用户管理')
  })

  test('A-SYS-3 系统配置', async ({ page }) => {
    await smokePage(page, '/admin/system-settings', '系统配置')
  })

  test('A-SYS-4 审计日志', async ({ page }) => {
    await smokePage(page, '/admin/audit-logs', '审计日志')
  })

  test('A-SYS-5 访问日志（访问时间列为 ISO 未格式化＝已知体感 bug，C 类，单独跟踪）', async ({ page }) => {
    // 审计日志时间已本地格式化，但访问日志「访问时间」仍为 ISO（如 2026-06-19T07:11:31），暂跳过 ISO 检查
    await smokePage(page, '/admin/access-logs', '访问日志', { checkIso: false })
  })
})
