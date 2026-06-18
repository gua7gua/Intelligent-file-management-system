import request from './request'
import type { PageData } from '@/types/api'
import type {
  Compilation, CompilationArchiveData, CompilationDetail, CompilationParams,
  CompilationWriteData, MaterialCandidate, MaterialSearchParams,
} from '@/types/compilation'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询编研成果（§19.1） */
export function getCompilations(params?: CompilationParams): Promise<PageData<Compilation>> {
  if (USE_MOCK) {
    return import('@/mock/modules/compilation').then((m) => m.mockCompilations(params))
  }
  return request.get('/admin/compilations', { params })
}

/** 创建编研草稿（§19.2） */
export function createCompilation(data: CompilationWriteData): Promise<CompilationDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/compilation').then((m) => m.mockCreateCompilation(data))
  }
  return request.post('/admin/compilations', data)
}

/** 获取编研详情（§19.3） */
export function getCompilationDetail(id: number): Promise<CompilationDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/compilation').then((m) => m.mockCompilationDetail(id))
  }
  return request.get(`/admin/compilations/${id}`)
}

/** 更新编研草稿（§19.4） */
export function updateCompilation(id: number, data: CompilationWriteData): Promise<CompilationDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/compilation').then((m) => m.mockUpdateCompilation(id, data))
  }
  return request.put(`/admin/compilations/${id}`, data)
}

/** 生成编研正文附件（§19.5） */
export function generateCompilationBody(id: number): Promise<CompilationDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/compilation').then((m) => m.mockGenerateCompilationBody(id))
  }
  return request.post(`/admin/compilations/${id}/generate`)
}

/** 编研成果入库（§19.6） */
export function archiveCompilation(id: number, data: CompilationArchiveData): Promise<CompilationDetail> {
  if (USE_MOCK) {
    return import('@/mock/modules/compilation').then((m) => m.mockArchiveCompilation(id, data))
  }
  return request.post(`/admin/compilations/${id}/archive`, data)
}

/** 素材档案候选（复用 §10.1 档案查询） */
export function searchMaterials(params?: MaterialSearchParams): Promise<PageData<MaterialCandidate>> {
  if (USE_MOCK) {
    return import('@/mock/modules/compilation').then((m) => m.mockSearchMaterials(params))
  }
  return request.get('/admin/archives', { params })
}

/** 删除编研草稿（仅 status=draft 可删，已生成/已入库不可删） */
export function deleteCompilation(id: number): Promise<void> {
  if (USE_MOCK) {
    return Promise.resolve()
  }
  return request.delete(`/admin/compilations/${id}`)
}
