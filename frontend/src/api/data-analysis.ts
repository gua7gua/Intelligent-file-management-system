import request from './request'
import type { PageData } from '@/types/api'
import type {
  AnalysisItem, AnalysisItemHandleData, AnalysisTask, AnalysisTaskCreateData,
  AnalysisTaskDetail, AnalysisTaskParams,
} from '@/types/data-analysis'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询研判任务（§20.4） */
export function getAnalysisTasks(params?: AnalysisTaskParams): Promise<PageData<AnalysisTask>> {
  if (USE_MOCK) {
    return import('@/mock/modules/data-analysis').then((m) => m.mockAnalysisTasks(params))
  }
  return request.get('/admin/analysis-tasks', { params })
}

/** 创建研判任务（§20.5） */
export function createAnalysisTask(data: AnalysisTaskCreateData): Promise<AnalysisTaskDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/data-analysis').then((m) => m.mockCreateAnalysisTask(data))
  }
  return request.post('/admin/analysis-tasks', data)
}

/** 获取研判任务详情（§20.6） */
export function getAnalysisTaskDetail(taskId: number): Promise<AnalysisTaskDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/data-analysis').then((m) => m.mockAnalysisTaskDetail(taskId))
  }
  return request.get(`/admin/analysis-tasks/${taskId}`)
}

/** 处理研判项（§20.7） */
export function handleAnalysisItem(itemId: number, data: AnalysisItemHandleData): Promise<AnalysisItem> {
  if (USE_MOCK) {
    return import('@/mock/modules/data-analysis').then((m) => m.mockHandleAnalysisItem(itemId, data))
  }
  return request.post(`/admin/analysis-items/${itemId}/handle`, data)
}

/** 删除研判任务（仅 completed/failed 可删，running 不可删） */
export function deleteAnalysisTask(taskId: number): Promise<void> {
  if (USE_MOCK) {
    return Promise.resolve()
  }
  return request.delete(`/admin/analysis-tasks/${taskId}`)
}
