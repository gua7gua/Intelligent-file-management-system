import type { LoginResult, UserInfo } from '@/types/user'

/** 预设测试账号 */
const mockUsers: Record<string, { user: UserInfo; portal: string }> = {
  admin: {
    portal: 'admin',
    user: {
      id: 1,
      realName: '后台管理员',
      userType: 'internal',
      roles: ['back_archivist'],
      organizationId: 1,
      maxSecurityLevel: 4,
      dataScope: 'all',
    },
  },
  front: {
    portal: 'admin',
    user: {
      id: 2,
      realName: '前台管理员',
      userType: 'internal',
      roles: ['front_archivist'],
      organizationId: 1,
      maxSecurityLevel: 2,
      dataScope: 'all',
    },
  },
  director: {
    portal: 'admin',
    user: {
      id: 3,
      realName: '馆领导',
      userType: 'internal',
      roles: ['director'],
      organizationId: 1,
      maxSecurityLevel: 4,
      dataScope: 'all',
    },
  },
  sys: {
    portal: 'admin',
    user: {
      id: 4,
      realName: '系统管理员',
      userType: 'internal',
      roles: ['sys_admin'],
      organizationId: 1,
      maxSecurityLevel: 4,
      dataScope: 'all',
    },
  },
  transfer: {
    portal: 'transfer',
    user: {
      id: 5,
      realName: '移交经办人',
      userType: 'internal',
      roles: ['transfer_user'],
      organizationId: 2,
      maxSecurityLevel: 0,
      dataScope: 'own_org',
    },
  },
  internal: {
    portal: 'internal',
    user: {
      id: 6,
      realName: '内部查阅者',
      userType: 'internal',
      roles: ['internal_reader'],
      organizationId: 1,
      maxSecurityLevel: 2,
      dataScope: 'own_org',
    },
  },
  public: {
    portal: 'public',
    user: {
      id: 7,
      realName: '公众用户',
      userType: 'public',
      roles: ['public_user'],
      organizationId: 0,
      maxSecurityLevel: 0,
      dataScope: 'none',
    },
  },
}

const defaultRouteMap: Record<string, string> = {
  admin: '/admin',
  transfer: '/transfer',
  internal: '/internal',
  public: '/public',
}

/** 模拟登录响应（按 loginName 查找用户） */
export function mockLoginResponse(loginName: string, portal: string): LoginResult {
  // 优先按 loginName 精确匹配测试账号
  let entry = mockUsers[loginName]
  if (!entry) {
    // loginName 不匹配预设账号时，按 portal 找第一个匹配的
    entry = Object.values(mockUsers).find((u) => u.portal === portal) || mockUsers.admin
  }
  return {
    token: `mock-token-${entry.user.realName}-${Date.now()}`,
    user: entry.user,
    defaultRoute: defaultRouteMap[portal] || '/admin',
  }
}

/** 模拟获取当前用户信息（/auth/me 的 data 直接是 user 对象，与真实后端一致） */
export function mockUserInfo(): UserInfo {
  const token = localStorage.getItem('token') || ''
  // token 格式: mock-token-{realName}-{timestamp}
  const match = token.match(/^mock-token-(.+)-\d+$/)
  const realName = match ? match[1] : ''
  const entry = Object.values(mockUsers).find((u) => u.user.realName === realName) || mockUsers.admin
  return entry.user
}
