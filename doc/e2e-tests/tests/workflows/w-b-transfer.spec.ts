import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { FIXTURES, uniqueTitle } from '../../support/fixtures'
import { wf } from '../../support/workflow-state'

// 用例文档：测试用例/80-业务闭环.md · W-B 移交入库
// 跨 4 角色链：小张(移交端) 编制电子条目清单并提交 → 小陈(前台) 验收上传电子件按名匹配(R3-B2) + 完成接收
//   → 小刘(档案管理岗) 入库（复用 W-A-4：AI 补全为辅助、手动核查兜底）产出 ARC → 小李(内部查阅) 检索并预览电子件。
// 幂等：每次用 uniqueTitle 清单标题 + 新电子条目，产出新 BAT/ARC，不与历史冲突。

test.describe.configure({ mode: 'serial' })

// ── W-B-1 小张编制并提交移交清单 ──
test.describe('W-B-1 小张编制提交移交清单', () => {
  test.use({ storageState: authFile('zhangTransfer') })

  test('编制电子条目并提交产出 BAT', async ({ page }) => {
    await page.goto('/transfer/transfer-list')
    await expect(page.getByRole('heading', { name: '编制移交清单', exact: true })).toBeVisible()

    // 清单基础信息
    await page.locator('#listTitle').fill(uniqueTitle('移交'))
    await page.locator('#department').fill('测试移交部门')
    await page.locator('#contactPhone').fill('13800138000')
    await page.locator('#archiveYear').fill('2024')
    await page.locator('#expectedDate').fill('2026-06-25')

    // 新增空白条目 + 填纯电子条目（expectedFilename 供验收按名匹配）
    await page.getByRole('button', { name: '新增空白条目' }).click()
    const itemRow = page.locator('tbody tr').first()
    await itemRow.locator('input.title-input').fill('测试移交档案')
    await itemRow.locator('select').nth(1).selectOption('electronic') // 载体=纯电子
    await itemRow.locator('input.file-input').fill('02-accounting-voucher.png')
    await itemRow.locator('select').nth(3).selectOption('open') // 开放=公开（密级 0 非密，允许公开）

    // 监听 POST /transfer/batches（create）拿 BAT 号
    const respPromise = page.waitForResponse(
      (r) => r.url().includes('/transfer/batches') && !r.url().includes('/submit') && r.request().method() === 'POST' && r.status() === 200,
    )
    await page.getByRole('button', { name: '提交清单' }).click()
    const resp = await respPromise
    const batchNo: string = (await resp.json()).data.batchNo
    expect(batchNo).toBeTruthy()
    wf('W-B').batchNo = batchNo
  })
})

// ── W-B-2 小陈验收上传电子件 ──
test.describe('W-B-2 小陈验收上传电子件', () => {
  test.use({ storageState: authFile('chenFront') })

  test('上传电子件按名匹配 + 完成接收', async ({ page }) => {
    await page.goto('/admin/transfer-reception')
    await expect(page.getByRole('heading', { name: '移交验收与电子文件上传', exact: true })).toBeVisible()

    const card = page.locator('.batch-card').filter({ hasText: wf('W-B').batchNo! })
    await expect(card).toBeVisible({ timeout: 10_000 })
    await card.click()

    // 上传 U 盘电子件（真实 ClamAV 扫描 + 按清单文件名匹配）
    await page.locator('#filePicker').setInputFiles(FIXTURES.voucher)
    await expect(page.locator('.match-card').filter({ hasText: /匹配成功|已匹配/ })).toBeVisible({ timeout: 30_000 })

    // 逐条验收 = 接收
    await page.locator('.acceptance-table tbody tr').first().locator('select').first().selectOption('accepted')
    await page.getByRole('button', { name: '确认接收' }).click()
    await expect(page.locator('.el-message').filter({ hasText: /接收|完成/ })).toBeVisible({ timeout: 15_000 })
  })
})

// ── W-B-3 小刘入库（复用 W-A-4：AI 补全为辅助，手动核查兜底）──
test.describe('W-B-3 小刘入库', () => {
  test.use({ storageState: authFile('liuBack') })
  test.setTimeout(180_000)

  test('AI 补全/手动核查 + 确认入库产出 ARC', async ({ page }) => {
    await page.goto('/admin/pending-archive')
    await expect(page.getByRole('button', { name: 'AI 整批补全' })).toBeVisible()

    const card = page.locator('button.batch-card').filter({ hasText: wf('W-B').batchNo! })
    await expect(card).toBeVisible({ timeout: 15_000 })
    await card.click()
    // 等详情区加载（入库表单）：纯电子条目文件名不在详情文本，改等「确认入库」按钮可见
    await expect(page.getByRole('button', { name: '确认入库' })).toBeVisible({ timeout: 15_000 })

    // 触发 AI 补全（辅助）。handleRunAi 已修假阳性：真完成弹「完成」、超时弹「仍在进行」。
    await page.getByRole('button', { name: 'AI 整批补全' }).click()
    await expect(page.locator('.el-message').filter({ hasText: 'AI 正在整批补全' })).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('.el-message').filter({ hasText: /AI 整批补全完成|AI 补全仍在进行/ })).toBeVisible({ timeout: 120_000 })

    // AI 对图片条目可能未回填，手动填入库必填（容器 .detail-field）
    const fieldInput = (label: string) => page.locator('.detail-field').filter({ hasText: label }).locator('input')
    await fieldInput('正式题名').fill('测试移交档案')
    await fieldInput('责任者').fill('测试移交者')
    await fieldInput('形成日期').fill('2024-01-01')
    await page.locator('.detail-field').filter({ hasText: '分类' }).locator('select').selectOption('1')

    await page.getByRole('button', { name: '确认入库' }).click()
    const arcLocator = page.locator('.el-message, .notice').filter({ hasText: /ARC-\d+/ })
    await expect(arcLocator).toBeVisible({ timeout: 20_000 })
    const arcText = (await arcLocator.first().textContent()) ?? ''
    const arc = arcText.match(/ARC-\d+/)?.[0]
    expect(arc).toBeTruthy()
    wf('W-B').archiveNo = arc
  })
})

// ── W-B-4 小李检索并预览电子件 ──
test.describe('W-B-4 小李检索并预览', () => {
  test.use({ storageState: authFile('liReader') })

  test('检索到入库档案，详情可见电子件', async ({ page }) => {
    await page.goto('/internal/search')
    await expect(page.getByRole('button', { name: '确认检索' })).toBeVisible()

    await page.getByPlaceholder(/题名、责任者、档号/).fill(wf('W-B').archiveNo!)
    await page.getByRole('button', { name: '确认检索' }).click()
    const row = page.locator('.result-row').filter({ hasText: wf('W-B').archiveNo! })
    await expect(row).toBeVisible({ timeout: 10_000 })

    // 点详情，验证电子件已随入库挂接（已修：列表 record 用 id）
    await row.getByRole('button', { name: '详情' }).click()
    await expect(page.getByText('02-accounting-voucher').first()).toBeVisible({ timeout: 10_000 })
  })
})
