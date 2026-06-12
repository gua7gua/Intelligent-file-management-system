import request from './request'
import type { DictItem } from '@/types/components'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 获取全量字典 */
export function getDictionariesApi(): Promise<Record<string, DictItem[]>> {
  if (USE_MOCK) {
    return import('@/mock/modules/dictionary').then((m) => m.mockDictionaries)
  }
  return request.get('/dictionaries')
}
