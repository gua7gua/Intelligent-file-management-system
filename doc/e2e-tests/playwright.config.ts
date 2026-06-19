import { defineConfig, devices } from '@playwright/test'
import { BASE_URL } from './support/env'

/**
 * Playwright 配置。
 * - channel:'chrome' 复用系统已装的 google-chrome-stable，免下载浏览器二进制
 * - workers:1 + fullyParallel:false 串行执行，避免多角色并发污染同一后端的业务状态
 * - globalSetup 前置：健康检查 + 为每个角色 API 登录生成 storageState
 */
export default defineConfig({
  testDir: './tests',
  globalSetup: './global-setup.ts',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: process.env.CI ? 2 : 1,
  workers: 1,
  timeout: 60_000,
  expect: { timeout: 10_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: BASE_URL,
    channel: 'chrome',
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    locale: 'zh-CN',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
  ],
})
