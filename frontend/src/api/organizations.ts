import request from './request'
import type { PageData } from '@/types/api'
import type { Organization, OrganizationCreate, OrganizationParams } from '@/types/organization'

const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false'

/** 查询组织（§17.4，只读引用） */
export function getOrganizations(params?: OrganizationParams): Promise<PageData<Organization>> {
  if (USE_MOCK) {
    return import('@/mock/modules/organizations').then((m) => m.mockOrganizations(params))
  }
  return request.get('/admin/organizations', { params })
}

/** 新增组织（§17.5，组织名称唯一 §17.6） */
export function createOrganization(body: OrganizationCreate): Promise<Organization> {
  if (USE_MOCK) {
    return import('@/mock/modules/organizations').then((m) => m.mockCreateOrganization(body))
  }
  return request.post('/admin/organizations', body)
}
