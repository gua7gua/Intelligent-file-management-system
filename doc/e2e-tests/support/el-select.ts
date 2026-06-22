import type { Page, Locator } from '@playwright/test'

/**
 * 选 Element Plus el-select 的某项。
 *
 * 关键坑：el-select 内部的 input 是 readonly，且被上层「选中值/placeholder」元素遮挡，
 * 直接 click 内部 input 会被 pointer-events 拦截而超时。必须点击 .el-select 容器展开下拉，
 * 再点 teleport 到 body 的下拉项 .el-select-dropdown__item。
 *
 * @param page 浏览器页面对象
 * @param container 指向目标 .el-select 容器的 locator（如 page.locator('.el-select:has(#xxx)')）
 * @param optionText 下拉项的可读文本
 */
export async function pickElSelectOption(page: Page, container: Locator, optionText: string): Promise<void> {
  await container.click()
  await page.locator('.el-select-dropdown__item', { hasText: optionText }).first().click()
}
