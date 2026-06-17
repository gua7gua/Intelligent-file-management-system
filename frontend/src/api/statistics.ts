import request from './request'
import type {
  StatisticsCategories, StatisticsExportResult, StatisticsOverview, StatisticsParams,
} from '@/types/statistics'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 统计总览（§20.1） */
export function getStatisticsOverview(params?: StatisticsParams): Promise<StatisticsOverview> {
  if (USE_MOCK) {
    return import('@/mock/modules/statistics').then((m) => m.mockStatisticsOverview(params))
  }
  return request.get('/admin/statistics/overview', { params })
}

/** 分类统计（§20.2） */
export function getStatisticsCategories(): Promise<StatisticsCategories> {
  if (USE_MOCK) {
    return import('@/mock/modules/statistics').then((m) => m.mockStatisticsCategories())
  }
  return request.get('/admin/statistics/categories')
}

/** 导出统计报表（§20.3，真实接口返回 xlsx/pdf 文件流，mock 仅反馈） */
export async function exportStatistics(params: StatisticsParams, format: 'xlsx' | 'pdf'): Promise<StatisticsExportResult> {
  if (USE_MOCK) {
    return import('@/mock/modules/statistics').then((m) => m.mockExportStatistics(params, format))
  }
  const blob = await request.get('/admin/statistics/export', {
    params: { ...params, format },
    responseType: 'blob' as 'json',
  }) as unknown as Blob
  // 触发浏览器下载
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = `档案统计报表.${format}`
  document.body.appendChild(a)
  a.click()
  document.body.removeChild(a)
  URL.revokeObjectURL(url)
  return { format, message: `已导出 ${format.toUpperCase()} 报表` }
}
