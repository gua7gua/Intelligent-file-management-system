import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { pollUntil } from '../../support/ai'

// 用例文档：测试用例/80-业务闭环.md · W-G 数据研判
// 单角色（liu.back）闭环：开始扫描（真实 AI）→ 轮询至已完成 → 异常建议列表 → 候选四要素 → 删除异常项（R3-C2）。
// 幂等：每次运行新建一个 ANA 任务，异常项软删留痕，不冲突。

test.use({ storageState: authFile('liuBack') })

test.describe('W-G 数据研判闭环', () => {
  // 真实 AI 扫描 30-60s + 轮询，给足超时
  test.setTimeout(180_000)

  test('扫描完成 → 候选四要素 → 删除异常项', async ({ page }) => {
    await page.goto('/admin/data-analysis')
    await expect(page.locator('#startScan')).toBeVisible()

    // 默认扫描范围（全门类 + 含 AI 候选），点开始扫描
    await page.locator('#startScan').click()
    await expect(page.getByText('扫描已开始')).toBeVisible()

    // 轮询「任务状态」卡的状态至「已完成」。前端 startScan 后不自动轮询，
    // 需主动点当前任务项触发 selectTask 重刷 current.status。
    const statusCard = page.locator('.card.panel').filter({ hasText: /^任务状态/ })
    await pollUntil(
      async () => {
        await page.locator('.task-item.active').click().catch(() => {})
        const txt = (await statusCard.locator('span.status').first().textContent()) ?? ''
        if (!txt.includes('已完成')) throw new Error(`status=${txt}`)
      },
      { timeout: 150_000, interval: 4_000, message: '研判扫描未在超时内完成' },
    )

    // 异常建议列表
    const table = page.locator('.card.panel').filter({ hasText: '异常建议列表' }).locator('table')
    await expect(table).toBeVisible()
    const rowCount = await table.locator('tbody tr').count()
    test.skip(rowCount === 0, '本次扫描未产生异常项，跳过候选/删除验证')

    // 点首行 → 右侧条目详情
    await table.locator('tbody tr').first().click()
    await expect(page.locator('.card.panel').filter({ hasText: '条目详情' })).toBeVisible()

    // R3-C3 候选四要素：若该异常项有 AI 候选，断言「字段/原值/建议值/置信度」齐全
    const candTitle = page.getByText(/AI 候选明细/)
    if (await candTitle.isVisible().catch(() => false)) {
      for (const label of ['字段', '原值', '建议值', '置信度']) {
        await expect(page.locator('.candidates').getByText(label, { exact: true })).toBeVisible()
      }
    }

    // R3-C2 删除首行异常项：行内删除 → ElMessageBox 确认
    const beforeCount = await table.locator('tbody tr').count()
    await table.locator('tbody tr').first().getByRole('button', { name: '删除' }).click()
    await page.locator('.el-message-box').getByRole('button', { name: '删除' }).click()
    await expect(page.locator('.el-message').filter({ hasText: '异常项已删除' })).toBeVisible({ timeout: 10_000 })
    // 删除后列表行数减少（软删后 selectTask 重刷过滤）
    await expect(table.locator('tbody tr')).toHaveCount(beforeCount - 1, { timeout: 10_000 })
  })
})
