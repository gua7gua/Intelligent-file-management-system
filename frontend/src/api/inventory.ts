import request from './request'
import type { PageData } from '@/types/api'
import type {
  InventoryCompleteData, InventoryItem, InventoryItemUpdateData, InventoryTask,
  InventoryTaskCreateData, InventoryTaskDetail, InventoryTaskParams,
} from '@/types/inventory'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询盘点任务（§18.1） */
export function getInventoryTasks(params?: InventoryTaskParams): Promise<PageData<InventoryTask>> {
  if (USE_MOCK) {
    return import('@/mock/modules/inventory').then((m) => m.mockInventoryTasks(params))
  }
  return request.get('/admin/inventory-tasks', { params })
}

/** 创建盘点任务（§18.2） */
export function createInventoryTask(data: InventoryTaskCreateData): Promise<InventoryTaskDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/inventory').then((m) => m.mockCreateInventoryTask(data))
  }
  return request.post('/admin/inventory-tasks', data)
}

/** 开始盘点（§18.3） */
export function startInventoryTask(taskId: number): Promise<InventoryTaskDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/inventory').then((m) => m.mockStartInventoryTask(taskId))
  }
  return request.post(`/admin/inventory-tasks/${taskId}/start`)
}

/** 获取盘点详情（§18.4） */
export function getInventoryTaskDetail(taskId: number): Promise<InventoryTaskDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/inventory').then((m) => m.mockInventoryTaskDetail(taskId))
  }
  return request.get(`/admin/inventory-tasks/${taskId}`)
}

/** 更新盘点明细（§18.5） */
export function updateInventoryItem(taskId: number, itemId: number, data: InventoryItemUpdateData): Promise<InventoryItem> {
  if (USE_MOCK) {
    return import('@/mock/modules/inventory').then((m) => m.mockUpdateInventoryItem(taskId, itemId, data))
  }
  return request.put(`/admin/inventory-tasks/${taskId}/items/${itemId}`, data)
}

/** 完成盘点（§18.6） */
export function completeInventoryTask(taskId: number, data: InventoryCompleteData): Promise<InventoryTaskDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/inventory').then((m) => m.mockCompleteInventoryTask(taskId, data))
  }
  return request.post(`/admin/inventory-tasks/${taskId}/complete`, data)
}
