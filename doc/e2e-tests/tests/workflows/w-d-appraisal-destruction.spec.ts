import { test, expect } from '@playwright/test'
import { authFile } from '../../support/accounts'
import { FIXTURES, uniqueTitle } from '../../support/fixtures'
import { wf } from '../../support/workflow-state'

// 用例文档：测试用例/80-业务闭环.md · W-D 鉴定→销毁（不可逆）
// 跨 3 角色链：
//   小刘(档案管理岗) 建鉴定批次(到期窗口) → 选 1 条「待销毁」→ 完成鉴定自动生成销毁清册 DES
//   → 小刘 destruction 提交审批 → 小方(馆领导) 审批通过
//   → 小刘 填销毁方式/两名监销人/上传现场照片 + 最终确认销毁（不可逆）。
// 幂等：不可逆操作；每次选命中列表中首个非 W-E(ARC-000016)/W-C(ARC-000017) 的档案销毁，
//   随销毁递减；命中档案充足（到期窗口 36500 天覆盖所有非永久档案）。

test.describe.configure({ mode: 'serial' })

// ── W-D-1 小刘鉴定生 DES ──
test.describe('W-D-1 小刘鉴定生DES', () => {
  test.use({ storageState: authFile('liuBack') })

  test('建批次→待销毁→完成生DES', async ({ page }) => {
    test.setTimeout(150_000)
    await page.goto('/admin/appraisal')
    await expect(page.getByRole('button', { name: '创建鉴定批次' })).toBeVisible()
    await page.getByRole('button', { name: '创建鉴定批次' }).click()

    // 建批次弹窗：批次名称 + 到期窗口（36500 天覆盖所有非永久档案）
    const createDialog = page.locator('.el-dialog').filter({ hasText: '创建鉴定批次' })
    await createDialog.locator('input[placeholder*="2015"]').fill(uniqueTitle('鉴定'))
    await createDialog.locator('input[type="number"]').fill('36500')
    await createDialog.getByRole('button', { name: '创建批次' }).click()

    // 建批次后自动选中，等条目表格
    await expect(page.locator('tbody tr').first()).toBeVisible({ timeout: 10_000 })

    // 完成鉴定要求所有条目有结论。销毁目标选档号数值最大者（高号，避开低号基线
    // ARC-000001~019 被 W-F(素材 004)/W-E(016 借阅)/W-C(017 密级) 等依赖），其余延长。
    const rows = page.locator('tbody tr')
    const cnt = await rows.count()
    expect(cnt).toBeGreaterThan(0)
    let targetNo: string | undefined
    let targetNum = 0
    for (let i = 0; i < cnt; i++) {
      const t = (await rows.nth(i).textContent()) ?? ''
      const m = t.match(/ARC-0*(\d+)/)
      const num = m ? parseInt(m[1], 10) : 0
      if (num > targetNum && !t.includes('ARC-000016') && !t.includes('ARC-000017')) {
        targetNum = num
        targetNo = t.match(/ARC-\d+/)?.[0]
      }
    }
    expect(targetNo, '应存在可销毁的高号档案').toBeTruthy()
    wf('W-D').targetNo = targetNo
    for (let i = 0; i < cnt; i++) {
      const row = rows.nth(i)
      const t = (await row.textContent()) ?? ''
      const no = t.match(/ARC-\d+/)?.[0]
      if (no === targetNo) {
        await row.locator('input[type="radio"][value="destroy"]').check()
        await row.locator('textarea').fill('W-D 到期销毁')
      } else {
        await row.locator('input[type="radio"][value="extend"]').check()
        await row.locator('select').selectOption({ index: 1 }) // 新保管期限
        await row.locator('input[type="date"]').fill('2035-12-31') // 新到期日
      }
    }

    // 完成鉴定（内部先保存），监听 /complete 拿生成的销毁清册 id/号
    const completeResp = page.waitForResponse(
      (r) => r.url().includes('/complete') && r.request().method() === 'POST' && r.status() === 200,
    )
    await page.getByRole('button', { name: '完成鉴定' }).click()
    const body = await (await completeResp).json()
    wf('W-D').desId = body.data.generatedListId
    wf('W-D').desNo = body.data.generatedListNo
    expect(wf('W-D').desId).toBeTruthy()
    await expect(page.locator('.el-message').filter({ hasText: /已生成销毁清册|鉴定已完成/ })).toBeVisible({ timeout: 10_000 })
  })
})

// ── W-D-2 小刘提交销毁审批 ──
test.describe('W-D-2 小刘提交销毁审批', () => {
  test.use({ storageState: authFile('liuBack') })

  test('提交审批生成审批单', async ({ page }) => {
    await page.goto(`/admin/destruction?focus=${wf('W-D').desId}`)
    await expect(page.getByRole('button', { name: '提交审批' })).toBeVisible({ timeout: 10_000 })

    const submitResp = page.waitForResponse(
      (r) => r.url().includes('/submit-approval') && r.status() === 200,
    )
    await page.getByRole('button', { name: '提交审批' }).click()
    // ElMessageBox.prompt 填写提交说明
    await page.locator('.el-message-box__input input').fill('W-D 到期销毁审批说明')
    await page.locator('.el-message-box').getByRole('button', { name: '确定' }).click()

    const body = await (await submitResp).json()
    wf('W-D').approvalId = body.data.approvalRequestId
    expect(wf('W-D').approvalId).toBeTruthy()
    await expect(page.locator('.el-message').filter({ hasText: /审批单|待审批/ })).toBeVisible({ timeout: 10_000 })
  })
})

// ── W-D-3 小方审批通过 ──
test.describe('W-D-3 小方审批通过', () => {
  test.use({ storageState: authFile('fangDirector') })

  test('审批通过销毁清册', async ({ page }) => {
    await page.goto(`/admin/approval?focus=${wf('W-D').approvalId}`)
    // 销毁审批 canApprove 恒真（不依赖凭证匹配）
    await expect(page.getByRole('button', { name: '审批通过' })).toBeEnabled({ timeout: 10_000 })
    await page.locator('textarea[placeholder*="审批意见"]').fill('W-D 同意销毁')
    await page.getByRole('button', { name: '审批通过' }).click()
    await expect(page.locator('.el-message').filter({ hasText: /审批已通过/ })).toBeVisible({ timeout: 10_000 })
  })
})

// ── W-D-4 小刘确认销毁（不可逆）──
test.describe('W-D-4 小刘确认销毁', () => {
  test.use({ storageState: authFile('liuBack') })

  test('填方式/监销人/照片 + 最终确认销毁', async ({ page }) => {
    await page.goto(`/admin/destruction?focus=${wf('W-D').desId}`)
    await expect(page.getByRole('button', { name: '确认销毁' })).toBeVisible({ timeout: 10_000 })
    await page.getByRole('button', { name: '确认销毁' }).click()

    // DestroyConfirmDialog：销毁方式 + 两名监销人 + 说明 + 现场照片 + 不可逆确认
    const dialog = page.locator('.el-dialog').filter({ hasText: '确认销毁（不可逆）' })
    await expect(dialog).toBeVisible()
    await dialog.locator('select').selectOption({ index: 1 }) // 销毁方式（首个真实选项）
    await dialog.locator('.split input').nth(0).fill('监销人甲')
    await dialog.locator('.split input').nth(1).fill('监销人乙')
    await dialog.locator('textarea').fill('W-D 销毁执行情况')
    await dialog.locator('input[type="file"]').setInputFiles(FIXTURES.destroyScene)
    await dialog.locator('input[type="checkbox"]').check()
    await dialog.getByRole('button', { name: '最终确认销毁' }).click()

    await expect(page.locator('.el-message').filter({ hasText: /已销毁|已更新为已销毁/ })).toBeVisible({ timeout: 15_000 })
  })
})
