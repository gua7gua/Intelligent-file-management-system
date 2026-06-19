import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { uniqueTitle } from '../../support/fixtures'

// 用例文档：测试用例/80-业务闭环.md · W-F 档案编研
// 单角色（liu.back）闭环：新建编研 → 加素材 → 一键「生成正文并入库」（D3）→ 产出正式档号 ARC-xxx。
// 幂等：每次用运行戳标题新建编研，不冲突。

test.use({ storageState: authFile('liuBack') })

test.describe('W-F 档案编研闭环', () => {
  test.setTimeout(90_000)

  test('新建编研 → 加素材 → 一键入库（D3）产出档号', async ({ page }) => {
    await page.goto('/admin/compilation')
    await expect(page.locator('#newCompilation')).toBeVisible()

    // 新建 + 填标题 + 保存草稿
    await page.locator('#newCompilation').click()
    const title = uniqueTitle('编研')
    await page.locator('#title').fill(title)
    await page.locator('#saveDraft').click()
    await expect(page.locator('.el-message').filter({ hasText: '草稿已保存' })).toBeVisible({ timeout: 15_000 })

    // 加素材：检索种子档号 ARC-000001 → 添加（6.10 物理分区：可添加/已引用）
    await page.getByPlaceholder('题名/档号').fill('ARC-000001')
    await page.locator('.row').getByRole('button', { name: /查询/ }).click()
    await expect(page.locator('.candidate-list li').first()).toBeVisible({ timeout: 10_000 })
    await page.locator('.candidate-list li').first().getByRole('button', { name: '添加' }).click()
    await expect(page.locator('.material-list li').filter({ hasText: 'ARC-000001' })).toBeVisible()

    // 入库表单：所属全宗（选第一个）+ 形成日期（D3 要求全宗+日期）
    await page.locator('.field').filter({ hasText: '所属全宗' }).locator('select').selectOption({ index: 1 })
    await page.locator('.field').filter({ hasText: '形成日期' }).locator('input[type="date"]').fill('2026-06-19')

    // D3 一键「生成正文并入库」→ 确认 → 产出档号
    await page.getByRole('button', { name: /生成正文并入库/ }).click()
    await page.locator('.el-message-box').getByRole('button', { name: '确定' }).click()
    await expect(page.locator('.el-message').filter({ hasText: /已生成正文并入库.*ARC-/ })).toBeVisible({ timeout: 60_000 })
  })
})
