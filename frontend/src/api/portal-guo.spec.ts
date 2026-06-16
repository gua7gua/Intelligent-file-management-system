import { describe, expect, it } from 'vitest'
import { getTransferDashboard } from './transfer'
import {
  generatePublicSearchQuery,
  getPublicHome,
  searchPublicArchives,
} from './public'
import { resetPublicPassword, sendPublicSmsCode } from './auth'

describe('portal-guo api mock mode', () => {
  it('returns transfer dashboard summary from API layer', async () => {
    const dashboard = await getTransferDashboard()
    expect(dashboard.summary.pendingTransfer).toBeGreaterThanOrEqual(1)
    expect(dashboard.recentBatches[0].sourceType).toBe('transfer')
  })

  it('returns public home and public search results', async () => {
    const home = await getPublicHome()
    const results = await searchPublicArchives({ keyword: '老城改造' })

    expect(home.stats.openArchiveCount).toBeGreaterThan(0)
    expect(results.records.every((item) => item.openStatus === 'open')).toBe(true)
  })

  it('filters public archives by structured search fields', async () => {
    // 档案门类（categoryId 为整数，4 = 音像档案）
    const byCategory = await searchPublicArchives({ categoryId: 4 })
    expect(byCategory.records.length).toBeGreaterThan(0)
    expect(byCategory.records.every((r) => r.category === '音像档案')).toBe(true)

    // 来源
    const bySource = await searchPublicArchives({ sourceType: 'collection' })
    expect(bySource.records.length).toBeGreaterThan(0)
    expect(bySource.records.every((r) => r.sourceType === 'collection')).toBe(true)

    // 是否有电子文件
    const withFiles = await searchPublicArchives({ hasElectronicFile: true })
    expect(withFiles.records.length).toBeGreaterThan(0)
    expect(withFiles.records.every((r) => r.hasElectronicFile)).toBe(true)

    // 档号模糊
    const byArchiveNo = await searchPublicArchives({ archiveNo: 'A-2024' })
    expect(byArchiveNo.records.length).toBeGreaterThan(0)
    expect(byArchiveNo.records.every((r) => r.archiveNo.includes('A-2024'))).toBe(true)

    // 题名模糊
    const byTitle = await searchPublicArchives({ title: '教育' })
    expect(byTitle.records.every((r) => r.title.includes('教育'))).toBe(true)
  })

  it('generates public AI search query conditions', async () => {
    const query = await generatePublicSearchQuery({ text: '查找 2000 年以后公开的老城改造影像资料' })
    expect(query.ruleType).toBe('publicSearchQuery')
    expect(query.conditions.formedYearStart).toBe(2000)
  })

  it('supports public SMS and reset password mock helpers', async () => {
    await expect(sendPublicSmsCode({ phone: '13800000005', scene: 'forgot_password' })).resolves.toBe(true)
    await expect(
      resetPublicPassword({
        phone: '13800000005',
        smsCode: '135790',
        newPassword: 'Public@2026',
      }),
    ).resolves.toBe(true)
  })
})
