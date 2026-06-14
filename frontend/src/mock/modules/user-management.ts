import type {
  ResetPasswordData,
  Role,
  User,
  UserCreateData,
  UserDetail,
  UserParams,
  UserStatusData,
  UserUpdateData,
} from '@/types/user-management'
import type { PageData } from '@/types/api'

let nextUserId = 100

const roles: Role[] = [
  { id: 1, roleCode: 'front_archivist', roleName: '前台档案管理员', description: '负责前台接收验收', enabled: true },
  { id: 2, roleCode: 'back_archivist', roleName: '后台档案管理员', description: '负责入库整理鉴定', enabled: true },
  { id: 3, roleCode: 'transfer_user', roleName: '移交单位经办人', description: '移交单位编制清单', enabled: true },
  { id: 4, roleCode: 'internal_reader', roleName: '内部查阅者', description: '内部检索借阅', enabled: true },
  { id: 5, roleCode: 'director', roleName: '馆领导', description: '审批与密级管理', enabled: true },
  { id: 6, roleCode: 'sys_admin', roleName: '系统管理员', description: '账号与系统配置', enabled: true },
  { id: 7, roleCode: 'public_user', roleName: '社会公众', description: '公众注册账号', enabled: true },
]

const users: User[] = [
  { id: 11, userType: 'internal', loginName: 'liuxiao', realName: '小刘', employeeNo: 'A001', phone: '13800000001', organizationId: 1, organizationName: '克拉玛依市档案馆', departmentName: '业务科', maxSecurityLevel: 2, dataScope: 'all', roleCodes: ['back_archivist'], status: 'active', createdAt: '2025-09-01T09:00:00+08:00' },
  { id: 12, userType: 'internal', loginName: 'fanglead', realName: '小方', employeeNo: 'A010', phone: '13800000010', organizationId: 1, organizationName: '克拉玛依市档案馆', departmentName: '馆领导', maxSecurityLevel: 4, dataScope: 'all', roleCodes: ['director'], status: 'active', createdAt: '2025-09-01T09:00:00+08:00' },
  { id: 13, userType: 'internal', loginName: 'zhangcw', realName: '小张', employeeNo: 'B002', phone: '13800000002', organizationId: 2, organizationName: '市财政局', departmentName: '经办组', maxSecurityLevel: 0, dataScope: 'own_org', roleCodes: ['transfer_user'], status: 'active', createdAt: '2025-10-01T09:00:00+08:00' },
  { id: 14, userType: 'internal', loginName: 'admin01', realName: '系统管理员甲', employeeNo: 'S001', phone: '13800000003', organizationId: 1, organizationName: '克拉玛依市档案馆', departmentName: '信息科', maxSecurityLevel: 0, dataScope: 'all', roleCodes: ['sys_admin'], status: 'active', createdAt: '2025-08-01T09:00:00+08:00' },
  { id: 15, userType: 'internal', loginName: 'admin02', realName: '系统管理员乙', employeeNo: 'S002', phone: '13800000004', organizationId: 1, organizationName: '克拉玛依市档案馆', departmentName: '信息科', maxSecurityLevel: 0, dataScope: 'all', roleCodes: ['sys_admin'], status: 'active', createdAt: '2025-08-01T09:00:00+08:00' },
  { id: 16, userType: 'internal', loginName: 'chenfront', realName: '小陈', employeeNo: 'A003', phone: '13800000005', organizationId: 1, organizationName: '克拉玛依市档案馆', departmentName: '接收服务部', maxSecurityLevel: 1, dataScope: 'own_org', roleCodes: ['front_archivist'], status: 'active', createdAt: '2025-11-01T09:00:00+08:00' },
  { id: 17, userType: 'internal', loginName: 'wangread', realName: '小王', employeeNo: 'A004', phone: '13800000006', organizationId: 3, organizationName: '市交通局', departmentName: '档案室', maxSecurityLevel: 2, dataScope: 'own_fonds', roleCodes: ['internal_reader'], status: 'active', createdAt: '2025-12-01T09:00:00+08:00' },
  { id: 18, userType: 'internal', loginName: 'zhaoleft', realName: '小赵', employeeNo: 'A005', phone: '13800000007', organizationId: 4, organizationName: '市科技研究中心', departmentName: '办公室', maxSecurityLevel: 1, dataScope: 'own_org', roleCodes: ['front_archivist', 'internal_reader'], status: 'disabled', createdAt: '2025-06-01T09:00:00+08:00' },
]

function paginate(list: User[], pageNo = 1, pageSize = 20): PageData<User> {
  const total = list.length
  const start = (pageNo - 1) * pageSize
  return { records: list.slice(start, start + pageSize), pageNo, pageSize, total, hasNext: start + pageSize < total }
}

export function mockUsers(params?: UserParams): PageData<User> {
  let list = [...users]
  if (params?.userType) list = list.filter((u) => u.userType === params.userType)
  if (params?.roleCode) list = list.filter((u) => u.roleCodes.includes(params.roleCode!))
  if (params?.organizationId) list = list.filter((u) => u.organizationId === params.organizationId)
  if (params?.status) list = list.filter((u) => u.status === params.status)
  if (params?.keyword) {
    const kw = params.keyword.trim().toLowerCase()
    list = list.filter((u) => u.loginName.toLowerCase().includes(kw) || u.realName.includes(params.keyword!.trim()))
  }
  return paginate(list, params?.pageNo, params?.pageSize)
}

export function mockUserDetail(id: number): UserDetail {
  const u = users.find((x) => x.id === id)
  if (!u) throw new Error('用户不存在')
  return { ...u, recentActions: [{ operationType: 'login', moduleName: 'auth', operatedAt: '2026-06-15T08:30:00+08:00' }] }
}

export function mockCreateUser(data: UserCreateData): UserDetail {
  if (users.some((u) => u.loginName === data.loginName)) {
    throw new Error('登录名已存在')
  }
  const id = nextUserId++
  const orgNameMap: Record<number, string> = { 1: '克拉玛依市档案馆', 2: '市财政局', 3: '市交通局', 4: '市科技研究中心' }
  const u: User = {
    id,
    userType: data.userType,
    loginName: data.loginName,
    employeeNo: data.employeeNo,
    phone: data.phone,
    realName: data.realName,
    organizationId: data.organizationId,
    organizationName: data.organizationId ? orgNameMap[data.organizationId] : undefined,
    departmentName: data.departmentName,
    maxSecurityLevel: data.maxSecurityLevel,
    dataScope: data.dataScope,
    roleCodes: data.roleCodes,
    status: 'active',
    createdAt: '2026-06-15T10:00:00+08:00',
  }
  users.unshift(u)
  return { ...u }
}

export function mockUpdateUser(id: number, data: UserUpdateData): UserDetail {
  const u = users.find((x) => x.id === id)
  if (!u) throw new Error('用户不存在')
  Object.assign(u, data)
  u.updatedAt = '2026-06-15T10:00:00+08:00'
  return { ...u }
}

export function mockUpdateUserStatus(id: number, data: UserStatusData): UserDetail {
  const u = users.find((x) => x.id === id)
  if (!u) throw new Error('用户不存在')
  if (data.status === 'disabled' && u.roleCodes.includes('sys_admin')) {
    const activeAdmins = users.filter((x) => x.roleCodes.includes('sys_admin') && x.status === 'active')
    if (activeAdmins.length <= 1) {
      throw new Error('不能禁用最后一个系统管理员')
    }
  }
  u.status = data.status
  return { ...u }
}

export function mockResetUserPassword(_id: number, _data: ResetPasswordData): boolean {
  return true
}

export function mockRoles(): Role[] {
  return roles.filter((r) => r.enabled)
}
