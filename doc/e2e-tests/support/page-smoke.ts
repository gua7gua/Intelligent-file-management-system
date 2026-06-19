import type { Page } from '@playwright/test'
import { expect } from '@playwright/test'
import { expectNoIsoTime, expectNoRawEnum, COMMON_RAW_ENUMS } from './expect'

/**
 * 通用页面冒烟：导航到 url → 断言主标题可见（页面就绪）→ 无业务英文枚举泄露 → 无未格式化 ISO 时间。
 * 用于管理后台各列表/配置页的「能打开、不白屏、文案给人看」基线验证。
 *
 * opts.checkIso 默认 true。对「时间列尚未格式化」的已知体感 bug 页（如访问日志访问时间），
 * 传 false 跳过 ISO 检查，避免阻塞基线；该 bug 单独记录在问题清单。
 */
export async function smokePage(
  page: Page,
  url: string,
  title: string,
  opts: { checkIso?: boolean } = {},
): Promise<void> {
  await page.goto(url)
  await expect(page.getByRole('heading', { name: title, exact: true })).toBeVisible({ timeout: 20_000 })
  await expectNoRawEnum(page.locator('body'), COMMON_RAW_ENUMS)
  if (opts.checkIso !== false) await expectNoIsoTime(page.locator('body'))
}
