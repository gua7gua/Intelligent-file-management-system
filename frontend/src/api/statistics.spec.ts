import { describe, expect, it } from 'vitest'
import { exportStatistics, getStatisticsCategories, getStatisticsOverview } from './statistics'

describe('statistics api mock mode', () => {
  it('overview returns metrics, charts, breakdown, sources', async () => {
    const o = await getStatisticsOverview()
    expect(o.metrics.length).toBe(4)
    expect(o.yearlyIntake.length).toBeGreaterThanOrEqual(6)
    expect(o.categoryDistribution.length).toBeGreaterThanOrEqual(5)
    expect(o.carrierDistribution.length).toBe(3)
    expect(o.businessBreakdown.length).toBe(5)
    expect(o.dataSources.length).toBe(5)
  })

  it('metrics carry target route and source', async () => {
    const o = await getStatisticsOverview()
    for (const m of o.metrics) {
      expect(m.source).toBeTruthy()
      expect(m.targetRoute).toBeTruthy()
    }
  })

  it('filters yearly intake by year range', async () => {
    const all = await getStatisticsOverview()
    const years = all.yearlyIntake.map((b) => b.year)
    const min = Math.min(...years)
    const max = Math.max(...years)
    const o = await getStatisticsOverview({ yearStart: min, yearEnd: min + 1 })
    expect(o.yearlyIntake.every((b) => b.year >= min && b.year <= min + 1)).toBe(true)
    expect(max).toBeGreaterThanOrEqual(min + 1)
  })

  it('categories cover six dimensions', async () => {
    const c = await getStatisticsCategories()
    expect(c.byCategory.length).toBeGreaterThan(0)
    expect(c.byYear.length).toBeGreaterThan(0)
    expect(c.bySourceType.length).toBeGreaterThan(0)
    expect(c.byCarrier.length).toBeGreaterThan(0)
    expect(c.bySecurityLevel.length).toBeGreaterThan(0)
    expect(c.byOpenStatus.length).toBeGreaterThan(0)
  })

  it('export returns feedback message', async () => {
    const r = await exportStatistics({}, 'xlsx')
    expect(r.format).toBe('xlsx')
    expect(r.message).toBeTruthy()
  })
})
