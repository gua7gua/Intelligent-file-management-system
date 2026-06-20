import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { wf } from '../../support/workflow-state'

// 用例文档：测试用例/80-业务闭环.md · W-E 借阅
// 跨 4 角色链（状态机 applied→approved→voucher_issued→checked_out→returned）：
//   小李(内部查阅) 对纯纸质档案申请借阅 → 小刘(档案管理岗) 审批通过
//   → 小李 导出借阅凭证（生成凭证号 voucherNo，转 voucher_issued；后端出库要求凭证号匹配）
//   → 小陈(前台) 核验出库 + 归还（前台无审批权 = 5.4 门控；凭证号由前端回填 detail.voucherNo）
//   → 小李 归还后补打借阅凭证（R3-D1）
// serial：requestNo 经 wf 传下游；闭环含归还→档案恢复可借→可重复申请（幂等）。

test.describe.configure({ mode: 'serial' })

// 目标档案：纯纸质（paper）、公开（securityLevel=0/open），载体含纸质 → 前端 canBorrowPaper 即允许申请。
// 须避开「盘点中」的门类（会计档案当前在盘点、暂停借阅），故选文书档案 ARC-000016。
const ARCHIVE_NO = 'ARC-000016'

// ── W-E-1 小李申请借阅 ──
test.describe('W-E-1 小李申请借阅', () => {
  test.use({ storageState: authFile('liReader') })

  test('提交借阅申请产出申请号', async ({ page }) => {
    await page.goto('/internal/search')
    await expect(page.getByRole('button', { name: '确认检索' })).toBeVisible()

    // 筛选纯纸质 + 检索（onMounted 只加载字典，需主动点「确认检索」）
    await page.locator('.field').filter({ hasText: '载体状态' }).locator('select').selectOption('paper')
    await page.getByRole('button', { name: '确认检索' }).click()
    const row = page.locator('.result-row').filter({ hasText: ARCHIVE_NO })
    await expect(row).toBeVisible({ timeout: 10_000 })

    // 点「详情」弹档案详情面板（已修：列表 record 用 id 而非 archiveId）
    await row.getByRole('button', { name: '详情' }).click()
    await expect(page.getByRole('button', { name: '申请借阅' })).toBeVisible({ timeout: 10_000 })

    // 展开借阅申请表单
    await page.getByRole('button', { name: '申请借阅' }).click()
    await expect(page.locator('.borrow-form')).toBeVisible()

    // 监听提交响应拿申请号（page 拿原始 R<T>，取 .data.requestNo）
    const respPromise = page.waitForResponse(
      (r) => r.url().includes('/internal/borrow-requests') && r.request().method() === 'POST' && r.status() === 200,
    )
    await page.locator('.borrow-form textarea').fill('W-E 闭环借阅测试')
    await page.locator('.borrow-form input[type="number"]').fill('7')
    await page.locator('.borrow-form input[type="datetime-local"]').fill('2026-06-25T10:00')
    await page.locator('.borrow-form input[placeholder*="手机号"]').fill('13800138000')
    await page.getByRole('button', { name: '提交申请' }).click()

    const resp = await respPromise
    const requestNo: string = (await resp.json()).data.requestNo
    expect(requestNo).toBeTruthy()
    wf('W-E').requestNo = requestNo

    await expect(page.locator('.el-message').filter({ hasText: /已提交.*等待审批/ })).toBeVisible({ timeout: 10_000 })
  })
})

// ── W-E-2 小刘审批通过 ──
test.describe('W-E-2 小刘审批通过', () => {
  test.use({ storageState: authFile('liuBack') })

  test('审批通过，申请转已批准', async ({ page }) => {
    await page.goto('/admin/borrow-approval')
    await expect(page.getByRole('heading', { name: '借阅审批', exact: true })).toBeVisible()

    // 默认「待审批」tab；定位本次申请卡
    const card = page.locator('.request-card').filter({ hasText: wf('W-E').requestNo! })
    await expect(card).toBeVisible({ timeout: 10_000 })
    await card.click()

    // 详情加载后「审批通过」可点（status=applied；小刘 back_archivist 有审批权）
    await expect(page.getByRole('button', { name: '审批通过' })).toBeEnabled({ timeout: 10_000 })
    await page.getByRole('button', { name: '审批通过' }).click()
    await expect(page.locator('.el-message').filter({ hasText: /审批已通过/ })).toBeVisible({ timeout: 10_000 })
  })
})

// ── W-E-3 小李导出借阅凭证（approved→voucher_issued，生成凭证号）──
test.describe('W-E-3 小李导出借阅凭证', () => {
  test.use({ storageState: authFile('liReader') })

  test('导出凭证生成凭证号', async ({ page }) => {
    await page.goto('/internal/borrow-requests')
    await expect(page.getByRole('heading', { name: '我的借阅申请', exact: true })).toBeVisible()

    const row = page.locator('tbody tr').filter({ hasText: wf('W-E').requestNo! })
    await expect(row).toBeVisible({ timeout: 10_000 })

    // 首次导出凭证（后端生成 voucherNo + 转 voucher_issued）；监听 GET voucher 200
    const voucherResp = page.waitForResponse(
      (r) => r.url().includes('/internal/borrow-requests/') && r.url().includes('/voucher') && r.status() === 200,
    )
    await row.getByRole('button', { name: '导出凭证' }).click()
    const resp = await voucherResp
    expect(resp.ok()).toBeTruthy()
    await expect(page.locator('.el-message').filter({ hasText: /已导出/ })).toBeVisible({ timeout: 10_000 })
  })
})

// ── W-E-4 小陈出库与归还（含 5.4 前台门控）──
test.describe('W-E-4 小陈出库与归还', () => {
  test.use({ storageState: authFile('chenFront') })

  test('前台无审批权(5.4) → 核验出库 → 归还', async ({ page }) => {
    await page.goto('/admin/borrow-approval')
    await expect(page.getByRole('heading', { name: '借阅审批', exact: true })).toBeVisible()

    // 切「已批准」tab（导出凭证后状态=voucher_issued，属已批准 tab）
    await page.locator('.tabs .tab').filter({ hasText: '已批准' }).click()
    const card = page.locator('.request-card').filter({ hasText: wf('W-E').requestNo! })
    await expect(card).toBeVisible({ timeout: 10_000 })
    await card.click()

    // 5.4 门控：前台 front_archivist 无审批按钮，应见提示
    await expect(page.locator('.notice').filter({ hasText: '审批由档案管理岗操作' })).toBeVisible({ timeout: 10_000 })

    // W-E-1 重构：凭证号审批通过时由后端自动生成，此处只读展示（disabled input），不手动覆盖
    const voucherInput = page.locator('.field').filter({ hasText: '凭证号' }).locator('input')
    await expect(voucherInput).not.toHaveValue('', { timeout: 10_000 })

    // 应还时间由后端按 expectedDays 在确认出库时自动计算，前端不再有可编辑应还时间 input；
    // approved/voucher_issued 阶段仅展示「确认出库」按钮，直接点击 + 确认弹窗。
    await expect(page.getByRole('button', { name: '确认出库' })).toBeEnabled({ timeout: 10_000 })
    await page.getByRole('button', { name: '确认出库' }).click()
    await expect(page.locator('.el-message-box').filter({ hasText: '确认出库' })).toBeVisible()
    await page.locator('.el-message-box').getByRole('button', { name: '确定' }).click()
    await expect(page.locator('.el-message').filter({ hasText: /已确认出库/ })).toBeVisible({ timeout: 10_000 })

    // 出库后 detail=checked_out，canReturn=true，直接归还（默认 normal，无需说明）
    await page.getByRole('button', { name: '确认归还' }).click()
    await expect(page.locator('.el-message-box').filter({ hasText: '确认归还' })).toBeVisible()
    await page.locator('.el-message-box').getByRole('button', { name: '确定' }).click()
    await expect(page.locator('.el-message').filter({ hasText: /恢复可借/ })).toBeVisible({ timeout: 10_000 })
  })
})

// ── W-E-5 小李归还后补打凭证（R3-D1）──
test.describe('W-E-5 小李归还后补打凭证', () => {
  test.use({ storageState: authFile('liReader') })

  test('归还(returned)后可补打借阅凭证(R3-D1)', async ({ page }) => {
    await page.goto('/internal/borrow-requests')
    await expect(page.getByRole('heading', { name: '我的借阅申请', exact: true })).toBeVisible()

    const row = page.locator('tbody tr').filter({ hasText: wf('W-E').requestNo! })
    await expect(row).toBeVisible({ timeout: 10_000 })

    // R3-D1：returned 后应可补打凭证。canExportVoucher 已含 returned（与后端一致）→ 按钮可见
    await expect(row.getByRole('button', { name: '导出凭证' })).toBeVisible({ timeout: 10_000 })

    const voucherResp = page.waitForResponse(
      (r) => r.url().includes('/internal/borrow-requests/') && r.url().includes('/voucher') && r.status() === 200,
    )
    await row.getByRole('button', { name: '导出凭证' }).click()
    const resp = await voucherResp
    expect(resp.ok()).toBeTruthy()
  })
})
