import { test, expect } from '@playwright/test'

// 用例文档：测试用例/70-异常路径.md · E-PERM-*
// 权限边界：未登录访问受保护页面应被路由守卫重定向到登录页（不渲染受保护内容）。

test.describe('未登录访问受保护页面 → 重定向登录', () => {
  // 这五个路由均带 requiresAuth（见 router/routes/*）
  const protectedUrls = [
    '/admin/overview',
    '/internal/overview',
    '/transfer/overview',
    '/public/overview',
    '/public/collection',
  ]

  for (const url of protectedUrls) {
    test(`E-PERM 未登录访问 ${url} 重定向到 /login`, async ({ page }) => {
      await page.goto(url)
      await expect(page).toHaveURL(/\/login/)
    })
  }
})
