import request from './request'
import type { DashboardSummary } from '@/types/dashboard'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 管理概览统计（§6.1） */
export function getDashboardSummary(): Promise<DashboardSummary> {
  if (USE_MOCK) {
    return import('@/mock/modules/dashboard').then((m) => m.mockDashboardSummary())
  }
  return request.get('/admin/dashboard')
}
