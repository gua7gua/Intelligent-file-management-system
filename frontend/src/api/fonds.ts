import request from './request'
import type { PageData } from '@/types/api'
import type { FondsCreate, FondsItem, FondsParams, FondsUpdate } from '@/types/fonds'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询全宗（§17.1，含归档数量） */
export function getFonds(params?: FondsParams): Promise<PageData<FondsItem>> {
  if (USE_MOCK) {
    return import('@/mock/modules/fonds').then((m) => m.mockFonds(params))
  }
  return request.get('/admin/fonds', { params })
}

/** 新增全宗（§17.2，全宗号唯一） */
export function createFonds(body: FondsCreate): Promise<FondsItem> {
  if (USE_MOCK) {
    return import('@/mock/modules/fonds').then((m) => m.mockCreateFonds(body))
  }
  return request.post('/admin/fonds', body)
}

/** 更新全宗（§17.3，fondsNo 不可改；停用经 status） */
export function updateFonds(id: number, body: FondsUpdate): Promise<FondsItem> {
  if (USE_MOCK) {
    return import('@/mock/modules/fonds').then((m) => m.mockUpdateFonds(id, body))
  }
  return request.put(`/admin/fonds/${id}`, body)
}

/** 删除全宗（仅无关联数据时允许；§17 未定义 DELETE，真实后端联调时确认端点） */
export function removeFonds(id: number): Promise<void> {
  if (USE_MOCK) {
    return import('@/mock/modules/fonds').then((m) => m.mockRemoveFonds(id))
  }
  return request.delete(`/admin/fonds/${id}`)
}
