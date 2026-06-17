import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import type { UserInfo } from '@/types/user'
import { loginApi, getUserInfoApi } from '@/api/auth'
import router from '@/router'

/** 管理后台相关角色 */
const ADMIN_ROLES = ['front_archivist', 'back_archivist', 'director', 'sys_admin']

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string | null>(localStorage.getItem('token'))
  const user = ref<UserInfo | null>(null)
  const roles = ref<string[]>([])

  const isLoggedIn = computed(() => !!token.value)

  /** 登录 */
  async function login(loginName: string, password: string, portal: string) {
    const data = await loginApi({ loginName, password, portal })
    token.value = data.token
    user.value = data.user
    roles.value = data.user.roles
    localStorage.setItem('token', data.token)
  }

  /** 清除登录态（不跳转） */
  function clearAuth() {
    token.value = null
    user.value = null
    roles.value = []
    localStorage.removeItem('token')
  }

  /** 登出 */
  function logout() {
    clearAuth()
    router.push('/login')
  }

  /** 恢复登录状态（页面刷新时调用） */
  async function restoreSession() {
    if (!token.value) return
    try {
      const data = await getUserInfoApi()
      user.value = data
      roles.value = data.roles
    } catch {
      // token 失效：仅清除登录态，不主动跳转，由路由守卫按目标页决定（匿名页可继续访问）
      clearAuth()
    }
  }

  /** 获取默认门户路径 */
  function getDefaultPortal(): string {
    if (roles.value.some((r) => ADMIN_ROLES.includes(r))) return '/admin'
    if (roles.value.includes('transfer_user')) return '/transfer'
    if (roles.value.includes('internal_reader')) return '/internal'
    return '/public'
  }

  /** 获取可切换的门户列表 */
  function getAllowedPortals() {
    const portals: Array<{ key: string; label: string; path: string }> = []
    if (roles.value.some((r) => ADMIN_ROLES.includes(r))) {
      portals.push({ key: 'admin', label: '管理后台', path: '/admin' })
    }
    if (roles.value.includes('transfer_user')) {
      portals.push({ key: 'transfer', label: '移交门户', path: '/transfer' })
    }
    if (roles.value.includes('internal_reader')) {
      portals.push({ key: 'internal', label: '内部门户', path: '/internal' })
    }
    portals.push({ key: 'public', label: '公众门户', path: '/public' })
    return portals
  }

  /** 角色判断 */
  function hasRole(role: string): boolean {
    return roles.value.includes(role)
  }

  function hasAnyRole(...checkRoles: string[]): boolean {
    return checkRoles.some((r) => roles.value.includes(r))
  }

  return {
    token,
    user,
    roles,
    isLoggedIn,
    login,
    logout,
    restoreSession,
    getDefaultPortal,
    getAllowedPortals,
    hasRole,
    hasAnyRole,
  }
})
