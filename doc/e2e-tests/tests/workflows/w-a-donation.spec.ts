import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { FIXTURES, uniqueTitle } from '../../support/fixtures'
import { wf } from '../../support/workflow-state'

// 用例文档：测试用例/80-业务闭环.md · W-A 公众捐赠
// 跨 4 角色链：小周(公众)提交 → 小刘(后台)联系约定 → 小陈(前台)接收上传 → 小刘(后台)入库 → 回查。
// serial：各步骤链式依赖，前步产出 BAT/ARC 经 wf 传给下游。

test.describe.configure({ mode: 'serial' })

/** 从文本提取 BAT-xxxxxx */
function extractBatchNo(text: string): string {
  const m = text.match(/BAT-\d+/)
  if (!m) throw new Error(`未在文本中找到批次号：${text.slice(0, 120)}`)
  return m[0]
}

// ── W-A-1 小周提交捐赠 ──
test.describe('W-A-1 小周提交捐赠', () => {
  test.use({ storageState: authFile('zhouPublic') })

  test('提交捐赠意向产出 BAT', async ({ page }) => {
    await page.goto('/public/collection')
    await expect(page.getByRole('heading', { name: '征集清单', exact: true })).toBeVisible()

    const title = uniqueTitle('捐赠')
    await page.locator('#colTitle').fill(title)
    await page.locator('#contactName').fill('测试小周')
    await page.locator('#contactPhone').fill('13800138000')
    await page.locator('#archiveYear').fill('2003')

    // 拖入 05 老照片（仅解析文件名，不上传）→ 生成一条素材条目
    await page.locator('#colFilePicker').setInputFiles(FIXTURES.oldPhoto)
    await expect(page.locator('.local-file').filter({ hasText: '05-old-city-photo-2003.png' })).toBeVisible()
    // 条目载体选纯电子（拖文件生成的条目载体为空，校验需非空）
    await page.locator('tbody tr').last().locator('select').selectOption('electronic')

    // 勾协议 + 提交
    await page.getByText(/我已阅读并同意/).click()
    await page.getByRole('button', { name: '提交捐赠意向' }).click()

    const notice = page.locator('.notice.success').filter({ hasText: /已提交.*批次号/ })
    await expect(notice).toBeVisible({ timeout: 15_000 })
    wf('W-A').batchNo = extractBatchNo((await notice.textContent()) ?? '')
  })
})

// ── W-A-2 后台联系约定到馆 ──
test.describe('W-A-2 后台联系约定到馆', () => {
  test.use({ storageState: authFile('liuBack') })

  test('约定到馆，批次转待接收', async ({ page }) => {
    await page.goto('/admin/collection')
    await expect(page.getByRole('heading', { name: '征集管理与接收', exact: true })).toBeVisible()

    // 后台见「待联系」tab；定位本次批次卡
    await page.locator('.tabs .tab').filter({ hasText: '待联系' }).click()
    const batchCard = page.locator('.batch-item').filter({ hasText: wf('W-A').batchNo! })
    await expect(batchCard).toBeVisible({ timeout: 10_000 })
    await batchCard.click()

    // 联系判断区：约定到馆时间 + 联系结果 + 约定到馆
    await page.locator('input[type="datetime-local"]').fill('2026-06-20T10:00')
    await page.locator('.field').filter({ hasText: '联系结果' }).locator('select').selectOption({ index: 1 })
    await page.getByRole('button', { name: '约定到馆' }).click()
    await expect(page.locator('.el-message').filter({ hasText: /约定|成功|到馆/ })).toBeVisible({ timeout: 10_000 })
  })
})

// ── W-A-3 前台接收上传电子件 ──
test.describe('W-A-3 前台接收上传', () => {
  test.use({ storageState: authFile('chenFront') })

  test('上传电子件匹配 + 完成接收', async ({ page }) => {
    await page.goto('/admin/collection')
    await expect(page.getByRole('heading', { name: '征集管理与接收', exact: true })).toBeVisible()

    const batchCard = page.locator('.batch-item').filter({ hasText: wf('W-A').batchNo! })
    await expect(batchCard).toBeVisible({ timeout: 10_000 })
    await batchCard.click()

    // 上传 05 老照片（真实 ClamAV 扫描 + 按文件名匹配）
    await page.locator('#collectionFilePicker').setInputFiles(FIXTURES.oldPhoto)
    await expect(page.locator('.match-card').filter({ hasText: '匹配成功' })).toBeVisible({ timeout: 20_000 })

    // 逐条验收 = 已接收 + 完成接收
    await page.locator('.review-table select').first().selectOption('accepted')
    await page.getByRole('button', { name: '完成接收' }).click()
    await expect(page.locator('.el-message').filter({ hasText: /接收|完成/ })).toBeVisible({ timeout: 15_000 })
  })
})

// ── W-A-4 后台入库（AI 补全为辅助，手动核查兜底）──
test.describe('W-A-4 后台入库', () => {
  test.use({ storageState: authFile('liuBack') })
  test.setTimeout(180_000)

  test('确认入库产出 ARC（AI 补全为辅助，手动核查兜底）', async ({ page }) => {
    const opsLog: string[] = []
    page.on('response', async (r) => {
      if (/\/items\/\d+\/(confirmation|archive)$/.test(r.url())) {
        opsLog.push(`${r.url().split('/').slice(-2).join('/')} → ${r.status()}`)
      }
    })

    await page.goto('/admin/pending-archive')
    await expect(page.getByRole('button', { name: 'AI 整批补全' })).toBeVisible()

    const batchCard = page.locator('.batch-card').filter({ hasText: wf('W-A').batchNo! })
    await expect(batchCard).toBeVisible({ timeout: 15_000 })
    await batchCard.click()
    await expect(page.getByText('05-old-city-photo-2003').first()).toBeVisible({ timeout: 10_000 })

    // 触发 AI 补全（辅助）。handleRunAi 已修假阳性：真完成弹「完成」、超时弹「仍在进行」。
    // 等 handleRunAi 结束（二选一），其后 selectBatch 已执行，不再覆盖表单。
    await page.getByRole('button', { name: 'AI 整批补全' }).click()
    await expect(page.locator('.el-message').filter({ hasText: 'AI 正在整批补全' })).toBeVisible({ timeout: 10_000 })
    await expect(page.locator('.el-message').filter({ hasText: /AI 整批补全完成|AI 补全仍在进行/ })).toBeVisible({ timeout: 120_000 })

    // AI 对图片条目可能未回填，手动填全入库必填（题名/责任者/形成日期/分类）。容器 .detail-field。
    const fieldInput = (label: string) => page.locator('.detail-field').filter({ hasText: label }).locator('input')
    await fieldInput('正式题名').fill('测试捐赠档案')
    await fieldInput('责任者').fill('测试捐赠者')
    await fieldInput('形成日期').fill('2003-01-01')
    await page.locator('.detail-field').filter({ hasText: '分类' }).locator('select').selectOption('1')

    await page.getByRole('button', { name: '确认入库' }).click()
    console.log('[W-A-4 confirm/archive]:', opsLog)
    // 入库成功：ElMessage 或详情 notice 含 ARC
    await expect(page.locator('.el-message, .notice').filter({ hasText: /ARC-\d+/ })).toBeVisible({ timeout: 20_000 })
  })
})
