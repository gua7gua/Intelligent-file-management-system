import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { uniqueTitle } from '../../support/fixtures'

// 用例文档：测试用例/50-管理后台.md（补充：全宗 / 库房 / 保存 / 盘点 深度操作）
// 后台档案管理员 liu.back 的「管理类」深度 CRUD（区别于 back-core 的列表冒烟）。
// 每个用例自包含、幂等：全宗/库房用本次运行唯一编号，停用只作用于本次新建实体，不污染基线（F001-3 / 401-402）。

test.use({ storageState: authFile('liuBack') })

// ── 全宗管理 ──
test.describe('全宗管理 /admin/fonds', () => {
  test('A-FONDS 全宗 新建→编辑→停用 全生命周期', async ({ page }) => {
    await page.goto('/admin/fonds')
    await expect(page.getByRole('heading', { name: '全宗管理', exact: true })).toBeVisible()

    const fondsNo = uniqueTitle('FONS') // 唯一全宗号（后端仅校验唯一+非空，无格式约束）
    const drawer = page.locator('aside.drawer')

    // ── 新建 ──
    await page.getByRole('button', { name: '新建全宗', exact: true }).first().click()
    await expect(drawer.getByRole('heading', { name: '新建全宗', exact: true })).toBeVisible()
    await drawer.locator('input[data-testid="fondsNo"]').fill(fondsNo)
    await drawer.locator('.field').filter({ hasText: '全宗名称' }).locator('input').fill(`E2E全宗${fondsNo}`)
    // 所属单位默认 orgs[0]，不改动
    await drawer.getByRole('button', { name: '新建全宗', exact: true }).click()
    await expect(page.locator('.el-message').filter({ hasText: '全宗已新建' })).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('.fonds-row').filter({ hasText: fondsNo })).toBeVisible()

    // ── 编辑（改全宗名称；全宗号创建后不可改）──
    await page.locator('.fonds-row').filter({ hasText: fondsNo }).click()
    await expect(drawer.getByRole('heading', { name: '编辑全宗', exact: true })).toBeVisible()
    await expect(drawer.locator('input[data-testid="fondsNo"]')).toBeDisabled()
    const nameInput = drawer.locator('.field').filter({ hasText: '全宗名称' }).locator('input')
    await nameInput.fill(`已改名${fondsNo}`)
    await drawer.getByRole('button', { name: '保存全宗', exact: true }).click()
    await expect(page.locator('.el-message').filter({ hasText: '全宗已保存' })).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('.fonds-row').filter({ hasText: `已改名${fondsNo}` })).toBeVisible()

    // ── 停用（无关联数据时直接 status=disabled，无确认弹窗）──
    await page.locator('.fonds-row').filter({ hasText: `已改名${fondsNo}` }).getByRole('button', { name: '停用', exact: true }).click()
    await expect(page.locator('.el-message').filter({ hasText: '已停用' })).toBeVisible({ timeout: 10_000 })
    // 状态置灰 + 操作按钮变「已停用」且禁用
    const row = page.locator('.fonds-row').filter({ hasText: `已改名${fondsNo}` })
    await expect(row.getByRole('button', { name: '已停用' })).toBeDisabled()
  })
})

// ── 库房与架位 ──
test.describe('库房管理 /admin/warehouse', () => {
  test('A-WAREHOUSE 创建库房房间并自动生成架位', async ({ page }) => {
    await page.goto('/admin/warehouse')
    await expect(page.getByRole('heading', { name: '库房管理', exact: true })).toBeVisible()

    // 库房号取运行戳数字部分（基线 401/402 为纯数字，沿用数字格式规避后端格式校验）
    const roomNo = uniqueTitle('R').replace(/\D/g, '')
    const beforeActive = Number((await page.locator('.metric-row .metric').first().locator('.metric-num').textContent()) ?? '0')

    await page.getByRole('button', { name: /添加库房/ }).click()
    const modal = page.locator('.modal')
    await expect(modal.getByRole('heading', { name: '添加库房', exact: true })).toBeVisible()

    await modal.locator('.field').filter({ hasText: '库房号' }).locator('input').fill(roomNo)
    await modal.locator('.field').filter({ hasText: '库房名称' }).locator('input').fill(`E2E库房${roomNo}`)
    // 机架/层/盒位用默认 4×4×8 = 128 盒位，不改
    await modal.getByRole('button', { name: '生成架位', exact: true }).click()

    // 成功提示含架位数，证明后端已生成完整架位结构
    await expect(page.locator('.el-message').filter({ hasText: /已创建.*个架位/ })).toBeVisible({ timeout: 15_000 })
    // 启用库房指标 +1
    await expect(page.locator('.metric-row .metric').first().locator('.metric-num')).toHaveText(String(beforeActive + 1))
    // 新库房出现在房间列表（room-item strong 文本含 roomNo）
    await expect(page.locator('.room-item').filter({ hasText: roomNo })).toBeVisible()
  })
})

// ── 档案保存 ──
test.describe('档案保存 /admin/preservation', () => {
  test('A-PRESERVE 立即数据库备份，任务列表新增', async ({ page }) => {
    await page.goto('/admin/preservation')
    await expect(page.getByRole('heading', { name: '档案保存', exact: true })).toBeVisible()
    // 默认选中「数据库备份」
    await expect(page.locator('input[name="backupScope"][value="database"]')).toBeChecked()
    // 等备份任务表加载稳定后记录基线行数
    await expect(page.getByRole('button', { name: '立即备份', exact: true })).toBeVisible()
    const before = await page.locator('.table-wrap tbody tr').count()

    await page.getByRole('button', { name: '立即备份', exact: true }).click()
    await expect(page.locator('.el-message').filter({ hasText: '已创建备份任务' })).toBeVisible({ timeout: 15_000 })
    // 创建后立即刷新任务列表，新任务行出现
    await expect(page.locator('.table-wrap tbody tr')).toHaveCount(before + 1, { timeout: 15_000 })
  })

  test('A-CHECK 四性检测：解析档案→执行→出结果', async ({ page }) => {
    await page.goto('/admin/preservation')
    await expect(page.getByRole('heading', { name: '档案保存', exact: true })).toBeVisible()

    // ARC-000040（会计）含 1 个电子件 02-accounting-voucher.png
    await page.locator('#checkArchiveNo').fill('ARC-000040')
    await page.getByRole('button', { name: '解析档案', exact: true }).click()
    await expect(page.locator('.check-file-list')).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('.check-file-list').getByText(/accounting-voucher/)).toBeVisible()

    // 默认全选电子件 + 默认检测项（完整性/可用性/安全性）
    await page.getByRole('button', { name: '执行四性检测', exact: true }).click()
    await expect(page.locator('.el-message').filter({ hasText: /四性检测/ })).toBeVisible({ timeout: 30_000 })
    // 检测结果分组出现
    await expect(page.locator('.result-group')).toBeVisible({ timeout: 15_000 })
  })
})

// ── 档案盘点 ──
test.describe('档案盘点 /admin/inventory', () => {
  test('A-INVENTORY 建盘点任务→开始→填正常→提交', async ({ page }) => {
    await page.goto('/admin/inventory')
    await expect(page.getByRole('heading', { name: '档案盘点', exact: true })).toBeVisible()

    // 新建盘点任务：库房 401(id=1) × 会计(cat=3) —— 该范围纸质档案最多（12 件），命中稳定
    await page.getByRole('button', { name: /新建盘点任务/ }).click()
    const modal = page.locator('.modal')
    await expect(modal.getByRole('heading', { name: '新建盘点任务' })).toBeVisible()
    await modal.locator('select').nth(0).selectOption('1') // 库房 401
    await modal.locator('select').nth(1).selectOption('3') // 会计
    const taskName = `E2E盘点${uniqueTitle('INV')}`
    await modal.locator('.field').filter({ hasText: '任务名称' }).locator('input').fill(taskName)
    await modal.getByRole('button', { name: '生成清单', exact: true }).click()
    await expect(page.locator('.el-message').filter({ hasText: /已生成盘点清单/ })).toBeVisible({ timeout: 15_000 })

    // 草稿 tab → 选中新任务 → 开始盘点（命中 >0 才可点）
    await page.getByRole('button', { name: '草稿', exact: true }).click()
    const taskItem = page.locator('.task-item').filter({ hasText: taskName })
    await expect(taskItem).toBeVisible({ timeout: 10_000 })
    await taskItem.click()
    await expect(page.getByRole('button', { name: '开始盘点', exact: true })).toBeEnabled({ timeout: 10_000 })
    await page.getByRole('button', { name: '开始盘点', exact: true }).click()
    await expect(page.locator('.el-message').filter({ hasText: '盘点已开始' })).toBeVisible()

    // running 态：每行盘点结果选「正常」
    const resultSelects = page.locator('.table-wrap tbody tr select')
    await expect(resultSelects.first()).toBeVisible({ timeout: 10_000 })
    const rowCount = await resultSelects.count()
    expect(rowCount).toBeGreaterThan(0)
    for (let i = 0; i < rowCount; i++) {
      await resultSelects.nth(i).selectOption('normal')
    }

    // 提交盘点结果 → ElMessageBox 确认
    await page.getByRole('button', { name: '提交盘点结果', exact: true }).click()
    await expect(page.locator('.el-message-box').filter({ hasText: '提交盘点结果' })).toBeVisible({ timeout: 15_000 })
    await page.locator('.el-message-box').getByRole('button', { name: '确定' }).click()
    await expect(page.locator('.el-message').filter({ hasText: '盘点结果已提交' })).toBeVisible({ timeout: 15_000 })
  })
})
