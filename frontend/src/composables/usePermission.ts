import { useAuthStore } from '@/stores/auth'

/** 权限 composable：提供角色判断方法 */
export function usePermission() {
  const authStore = useAuthStore()

  function hasRole(role: string): boolean {
    return authStore.hasRole(role)
  }

  function hasAnyRole(...roles: string[]): boolean {
    return authStore.hasAnyRole(...roles)
  }

  function isAdmin(): boolean {
    return authStore.hasAnyRole('front_archivist', 'back_archivist', 'director', 'sys_admin')
  }

  return { hasRole, hasAnyRole, isAdmin }
}
