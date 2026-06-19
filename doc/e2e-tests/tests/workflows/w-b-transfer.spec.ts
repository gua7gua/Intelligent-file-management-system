import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { FIXTURES, uniqueTitle } from '../../support/fixtures'
import { wf } from '../../support/workflow-state'

// 用例文档：测试用例/80-业务闭环.md · W-B 移交入库（纸质+电子）
// 跨 4 角色链：小张(移交端) 编制【纸质+电子】条目清单并提交 → 小陈(前台) 验收上传电子件按名匹配(R3-B2) + 完成接收
//   → 小刘(档案管理岗) 入库（AI 补全为辅助、手动核查兜底；纸质部分选档案盒预占架位）产出 ARC → 小李(内部查阅) 检索并预览电子件
//   → 小刘 确认上架（纸质部分进架位，进入正常利用）。
// 一个闭环同时覆盖电子件验收上传（W-B-2）与纸质档案上架（W-B-5），载体=纸质+电子。
// 幂等：每次用 uniqueTitle 清单标题 + 新条目，产出新 BAT/ARC，不与历史冲突。

test.describe.configure({ mode: 'serial' })

// ── W-B-1 小张编制并提交移交清单 ──
test.describe('W-B-1 小张编制提交移交清单', () => {
  test.use({ storageState: authFile('zhangTransfer') })

  test('编制纸质+电子条目并提交产出 BAT', async ({ page }) => {
    await page.goto('/transfer/transfer-list')
    await expect(page.getByRole('heading', { name: '编制移交清单', exact: true })).toBeVisible()

    // 清单基础信息
    await page.locator('#listTitle').fill(uniqueTitle('移交'))
    await page.locator('#department').fill('测试移交部门')
    await page.locator('#contactPhone').fill('13800138000')
    await page.locator('#archiveYear').fill('2024')
    await page.locator('#expectedDate').fill('2026-06-25')

    // 新增空白条目 + 填【纸质+电子】条目（expectedFilename 供验收按名匹配；纸质部分触发后续上架）
    await page.getByRole('button', { name: '新增空白条目' }).click()
    const itemRow = page.locator('tbody tr').first()
    await itemRow.locator('input.title-input').fill('测试移交档案')
    await itemRow.locator('select').nth(1).selectOption('paper_electronic') // 载体=纸质+电子
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

    // 纸质+电子载体多了「纸质核对」列：用「批量纸质验收通过」一键置 纸质核对=通过 + 验收结论=接收（纯电子亦兼容）
    await page.getByRole('button', { name: '批量纸质验收通过' }).click()
    await page.getByRole('button', { name: '确认接收' }).click()
    await expect(page.locator('.el-message').filter({ hasText: /接收|完成/ })).toBeVisible({ timeout: 15_000 })
  })
})

// ── W-B-3 小刘入库（复用 W-A-4：AI 补全为辅助，手动核查兜底）──
test.describe('W-B-3 小刘入库', () => {
  test.use({ storageState: authFile('liuBack') })
  test.setTimeout(180_000)

  test('AI 补全/手动核查 + 选档案盒 + 确认入库产出 ARC', async ({ page }) => {
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
    // 分类 select 用 .first()：纸质+电子多出的「档案盒」字段 hint 含「分类」二字会多匹配一个
    await page.locator('.detail-field').filter({ hasText: '分类' }).locator('select').first().selectOption('1')

    // 纸质+电子载体：入库必选档案盒（盒即架位，按分类过滤为 cat1 文书）；选第一个盒（index 1 跳过「请选择」）
    await page.locator('.detail-field').filter({ hasText: '档案盒' }).locator('select').selectOption({ index: 1 })

    await page.getByRole('button', { name: '确认入库' }).click()
    const arcLocator = page.locator('.el-message, .notice').filter({ hasText: /ARC-\d+/ })
    await expect(arcLocator).toBeVisible({ timeout: 20_000 })
    const arcText = (await arcLocator.first().textContent()) ?? ''
    const arc = arcText.match(/ARC-\d+/)?.[0]
    expect(arc).toBeTruthy()
    wf('W-B').archiveNo = arc
  })
})

// ── W-B-4 小刘确认上架（纸质部分进架位；未上架前 lifecycle=pending_shelf，内部检索不可见）──
test.describe('W-B-4 小刘确认上架', () => {
  test.use({ storageState: authFile('liuBack') })

  test('已入库批次确认上架进入正常利用', async ({ page }) => {
    await page.goto('/admin/pending-archive')
    // 左下「已入库待上架」分区定位本次批次（纸质部分 pendingShelfCount>0）
    const shelvable = page.locator('.shelvable-card').filter({ hasText: wf('W-B').batchNo! })
    await expect(shelvable).toBeVisible({ timeout: 15_000 })
    await shelvable.getByRole('button', { name: '确认上架' }).click()
    await expect(page.locator('.el-message').filter({ hasText: /已确认上架/ })).toBeVisible({ timeout: 15_000 })
    // 上架后该批次从「已入库待上架」分区移除
    await expect(page.locator('.shelvable-card').filter({ hasText: wf('W-B').batchNo! })).toHaveCount(0)
  })
})

// ── W-B-5 小李检索并预览电子件（上架后进入正常利用，方可被内部检索）──
test.describe('W-B-5 小李检索并预览', () => {
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
