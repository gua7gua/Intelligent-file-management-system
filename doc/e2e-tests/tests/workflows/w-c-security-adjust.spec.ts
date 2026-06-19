import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { wf } from '../../support/workflow-state'

// 用例文档：测试用例/80-业务闭环.md · W-C 密级/开放调整
// 2 角色链：小刘(档案管理岗 back_archivist) 发起密级调整（凭证档号须与目标同组织/同全宗）
//   → 小方(馆领导 director) 审批工作台审批通过（canApprove 须 evidenceMatched）
//   → 小刘 回查档案密级字段已生效。
// 幂等：目标 ARC-000017、凭证 ARC-000018 均为文书档案（同 organizationId=2 / fondsId=1）；
//   密级值读当前后取反（0↔1）避免「无变化」被拒；审批通过清 pending，可重复发起。

test.describe.configure({ mode: 'serial' })

const TARGET_NO = 'ARC-000017'
const EVIDENCE_NO = 'ARC-000018'
const KEYWORD_INPUT = 'input[placeholder="题名、档号、责任者"]'

// ── W-C-1 小刘发起密级调整 ──
test.describe('W-C-1 小刘发起密级调整', () => {
  test.use({ storageState: authFile('liuBack') })

  test('发起密级调整生成审批单', async ({ page }) => {
    await page.goto('/admin/archive-management')
    await expect(page.getByRole('heading', { name: '档案管理', exact: true })).toBeVisible()

    // 检索目标档案
    await page.locator(KEYWORD_INPUT).fill(TARGET_NO)
    await page.getByRole('button', { name: '查询' }).click()
    const row = page.locator('tbody tr').filter({ hasText: TARGET_NO })
    await expect(row).toBeVisible({ timeout: 10_000 })

    // 读当前密级（第 6 列=密级），取反作为调整目标（0↔1），保证幂等且「有变化」
    const currentLabel = (await row.locator('td').nth(5).textContent()) ?? ''
    const newLevel = currentLabel.includes('非密') ? 1 : 0
    wf('W-C').newLevel = newLevel

    // 点行进详情弹窗
    await row.click()
    await expect(page.locator('.el-dialog')).toBeVisible({ timeout: 10_000 })

    // 发起审批：凭证档号（同全宗）+ 调整后密级 + 理由；监听 POST 拿审批单 id
    const respPromise = page.waitForResponse(
      (r) => r.url().includes('/security-adjustments') && r.request().method() === 'POST' && r.status() === 200,
    )
    await page.locator('input[placeholder="如 ARC-000007"]').fill(EVIDENCE_NO)
    await page.locator('.field').filter({ hasText: '调整后密级' }).locator('select').selectOption({ value: String(newLevel) })
    await page.locator('textarea[placeholder*="调整依据"]').fill('W-C 闭环密级调整测试')
    await page.getByRole('button', { name: '密级调整' }).click()

    const resp = await respPromise
    const approvalId = (await resp.json()).data.id
    expect(approvalId).toBeTruthy()
    wf('W-C').approvalId = approvalId

    await expect(page.locator('.el-message').filter({ hasText: /密级调整申请已生成/ })).toBeVisible({ timeout: 10_000 })
  })
})

// ── W-C-2 小方审批通过 ──
test.describe('W-C-2 小方审批通过', () => {
  test.use({ storageState: authFile('fangDirector') })

  test('审批通过密级调整单（凭证匹配）', async ({ page }) => {
    // 用 focus 精准定位本次审批单（approval 页 watch route.query.focus）
    await page.goto(`/admin/approval?focus=${wf('W-C').approvalId}`)
    await expect(page.getByRole('heading', { name: '审批工作台', exact: true })).toBeVisible()

    // 详情加载后「审批通过」应可点（evidenceMatched=true → canApprove=true）
    await expect(page.getByRole('button', { name: '审批通过' })).toBeEnabled({ timeout: 10_000 })
    await page.locator('textarea[placeholder*="审批意见"]').fill('W-C 同意密级调整')
    await page.getByRole('button', { name: '审批通过' }).click()
    await expect(page.locator('.el-message').filter({ hasText: /审批已通过/ })).toBeVisible({ timeout: 10_000 })
  })
})

// ── W-C-3 小刘回查密级已生效 ──
test.describe('W-C-3 小刘回查密级生效', () => {
  test.use({ storageState: authFile('liuBack') })

  test('档案密级字段已更新为目标值', async ({ page }) => {
    await page.goto('/admin/archive-management')
    await expect(page.getByRole('heading', { name: '档案管理', exact: true })).toBeVisible()

    await page.locator(KEYWORD_INPUT).fill(TARGET_NO)
    await page.getByRole('button', { name: '查询' }).click()
    const row = page.locator('tbody tr').filter({ hasText: TARGET_NO })
    await expect(row).toBeVisible({ timeout: 10_000 })

    const levelLabel = (await row.locator('td').nth(5).textContent()) ?? ''
    const expected = wf('W-C').newLevel === 0 ? '非密' : '内部'
    expect(levelLabel).toContain(expected)
  })
})
