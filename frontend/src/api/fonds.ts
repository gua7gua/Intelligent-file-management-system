import request from './request'
import type { PageData } from '@/types/api'
import type { FondsParams, FondsReference } from '@/types/fonds'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询全宗（§17.1，只读引用） */
export function getFonds(params?: FondsParams): Promise<PageData<FondsReference>> {
  if (USE_MOCK) {
    return import('@/mock/modules/fonds').then((m) => m.mockFonds(params))
  }
  return request.get('/admin/fonds', { params })
}
