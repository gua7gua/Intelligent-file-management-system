import type {
  BusinessBreakdownRow, DataSourceStatus, DistributionItem, StatisticsCategories,
  StatisticsExportResult, StatisticsOverview, StatisticsParams,
} from '@/types/statistics'

const baseYearly = [
  { year: 2020, count: 1820 },
  { year: 2021, count: 2104 },
  { year: 2022, count: 1985 },
  { year: 2023, count: 2360 },
  { year: 2024, count: 2710 },
  { year: 2025, count: 3018 },
  { year: 2026, count: 1286 },
]

function inRange(year: number, p?: StatisticsParams): boolean {
  if (p?.yearStart !== undefined && year < p.yearStart) return false
  if (p?.yearEnd !== undefined && year > p.yearEnd) return false
  return true
}

const categoryDistribution: DistributionItem[] = [
  { label: '科技档案', value: 78230, ratio: 0.314 },
  { label: '文书档案', value: 65100, ratio: 0.262 },
  { label: '会计档案', value: 52860, ratio: 0.212 },
  { label: '音像档案', value: 24890, ratio: 0.1 },
  { label: '人事档案', value: 19840, ratio: 0.08 },
  { label: '其他', value: 8000, ratio: 0.032 },
]

const carrierDistribution: DistributionItem[] = [
  { label: 'electronic', value: 142300, ratio: 0.572 },
  { label: 'paper_electronic', value: 82620, ratio: 0.332 },
  { label: 'paper', value: 24000, ratio: 0.096 },
]

const businessBreakdown: BusinessBreakdownRow[] = [
  {
    key: 'transfer', domain: '移交', total: 412,
    details: [{ label: '已接收', count: 360 }, { label: '部分接收', count: 40 }, { label: '待验收', count: 12 }],
    sourceTable: 'intake_batches (transfer)',
  },
  {
    key: 'collection', domain: '征集', total: 86,
    details: [{ label: '已到馆', count: 70 }, { label: '联系中', count: 12 }, { label: '已拒绝', count: 4 }],
    sourceTable: 'intake_batches (collection)',
  },
  {
    key: 'borrow', domain: '借阅', total: 238,
    details: [{ label: '已归还', count: 180 }, { label: '借出中', count: 42 }, { label: '逾期', count: 16 }],
    sourceTable: 'borrow_requests',
  },
  {
    key: 'destruction', domain: '销毁', total: 540,
    details: [{ label: '已销毁', count: 530 }, { label: '待审批', count: 8 }, { label: '草稿', count: 2 }],
    sourceTable: 'destruction_lists',
  },
  {
    key: 'preservation', domain: '保存检测', total: 1280,
    details: [{ label: '正常', count: 1274 }, { label: '失败', count: 6 }],
    sourceTable: 'backup_tasks / file_check_records',
  },
]

const dataSources: DataSourceStatus[] = [
  { table: 'archives', label: '正式档案主表', healthy: true, lastSyncedAt: '08:30' },
  { table: 'intake_batches', label: '移交/征集批次', healthy: true, lastSyncedAt: '08:30' },
  { table: 'borrow_requests', label: '借阅申请', healthy: true, lastSyncedAt: '08:30' },
  { table: 'destruction_lists', label: '销毁清册', healthy: true, lastSyncedAt: '08:30' },
  { table: 'backup_tasks', label: '备份与检测', healthy: false, lastSyncedAt: '09:10' },
]

function buildMetrics(): StatisticsOverview['metrics'] {
  return [
    { key: 'total', label: '馆藏总量', value: 248920, source: 'archives', targetRoute: '/admin/archive-management', targetQuery: { lifecycle_status: 'normal' } },
    { key: 'month_added', label: '本月新增', value: 1286, source: 'archives (archived_at=this_month)', targetRoute: '/admin/archive-management', targetQuery: { archived_at: 'this_month' } },
    { key: 'pending_archive', label: '待入库 / 待上架', value: 37, source: 'intake_batches', targetRoute: '/admin/pending-archive', targetQuery: { status: 'pending_archive' } },
    { key: 'pending_destruction', label: '待审批销毁清册', value: 8, source: 'destruction_lists', targetRoute: '/admin/destruction', targetQuery: { status: 'pending_approval' } },
  ]
}

export function mockStatisticsOverview(params?: StatisticsParams): StatisticsOverview {
  const yearlyIntake = baseYearly.filter((b) => inRange(b.year, params))
  return {
    metrics: buildMetrics(),
    yearlyIntake,
    categoryDistribution,
    carrierDistribution,
    businessBreakdown,
    dataSources,
    summarizedAt: '2026-06-17T08:30:00+08:00',
  }
}

export function mockStatisticsCategories(): StatisticsCategories {
  return {
    byCategory: categoryDistribution,
    byYear: baseYearly,
    bySourceType: [
      { label: 'transfer', value: 158200, ratio: 0.636 },
      { label: 'collection', value: 28600, ratio: 0.115 },
      { label: 'compilation', value: 62120, ratio: 0.249 },
    ],
    byCarrier: carrierDistribution,
    bySecurityLevel: [
      { label: '非密', value: 218400, ratio: 0.878 },
      { label: '秘密', value: 21500, ratio: 0.086 },
      { label: '机密', value: 9020, ratio: 0.036 },
    ],
    byOpenStatus: [
      { label: 'open', value: 196300, ratio: 0.789 },
      { label: 'restricted', value: 42600, ratio: 0.171 },
      { label: 'closed', value: 10020, ratio: 0.04 },
    ],
  }
}

export function mockExportStatistics(_params: StatisticsParams, format: 'xlsx' | 'pdf'): StatisticsExportResult {
  return {
    format,
    message: `已生成当前口径统计表（${format.toUpperCase()}，模拟导出）`,
  }
}
