/** 统计查询参数（§20.1/20.3） */
export interface StatisticsParams {
  yearStart?: number
  yearEnd?: number
  organizationId?: number
  fondsId?: number
}

/** 统计指标卡项 */
export interface StatisticsMetric {
  key: string
  label: string
  value: number
  unit?: string
  source: string
  targetRoute?: string
  targetQuery?: Record<string, string>
}

/** 年度进馆柱状项（原型 bar-chart） */
export interface YearlyIntakeBar {
  year: number
  count: number
}

/** 分布项（门类/载体/密级/公开等通用） */
export interface DistributionItem {
  label: string
  value: number
  ratio: number
}

/** 业务明细行（移交/征集/借阅/销毁/保存检测） */
export interface BusinessBreakdownRow {
  key: string
  domain: string
  total: number
  details: { label: string; count: number }[]
  sourceTable: string
}

/** 数据源状态项 */
export interface DataSourceStatus {
  table: string
  label: string
  healthy: boolean
  lastSyncedAt: string
}

/** 统计总览响应（§20.1） */
export interface StatisticsOverview {
  metrics: StatisticsMetric[]
  yearlyIntake: YearlyIntakeBar[]
  categoryDistribution: DistributionItem[]
  carrierDistribution: DistributionItem[]
  businessBreakdown: BusinessBreakdownRow[]
  dataSources: DataSourceStatus[]
  summarizedAt: string
}

/** 分类统计响应（§20.2） */
export interface StatisticsCategories {
  byCategory: DistributionItem[]
  byYear: YearlyIntakeBar[]
  bySourceType: DistributionItem[]
  byCarrier: DistributionItem[]
  bySecurityLevel: DistributionItem[]
  byOpenStatus: DistributionItem[]
}

/** 导出结果（§20.3，mock 用反馈） */
export interface StatisticsExportResult {
  format: 'xlsx' | 'pdf'
  message: string
}
