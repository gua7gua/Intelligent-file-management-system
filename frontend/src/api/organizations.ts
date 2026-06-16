import request from './request'
import type { PageData } from '@/types/api'
import type { Organization, OrganizationCreate, OrganizationParams, OrganizationUpdate } from '@/types/organization'

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

/** 更新组织（§17.7，部分更新；停用通过 status=disabled，保留历史档案归属） */
export function updateOrganization(organizationId: number, body: OrganizationUpdate): Promise<Organization> {
  if (USE_MOCK) {
    return import('@/mock/modules/organizations').then((m) => m.mockUpdateOrganization(organizationId, body))
  }
  return request.put(`/admin/organizations/${organizationId}`, body)
}

/** 删除组织（§17.8，仅 sys_admin；无关联全宗和用户时方可删除，否则后端返回 409） */
export function deleteOrganization(organizationId: number): Promise<void> {
  if (USE_MOCK) {
    return import('@/mock/modules/organizations').then((m) => m.mockDeleteOrganization(organizationId))
  }
  return request.delete(`/admin/organizations/${organizationId}`)
}
