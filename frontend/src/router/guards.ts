import type { Router } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

/** 需要登录才能访问的路由 meta 标识 */
function requiresAuth(to: any): boolean {
  return to.matched.some((record: any) => record.meta?.requiresAuth)
}

export function setupRouterGuards(router: Router) {
  router.beforeEach(async (to, _from) => {
    const authStore = useAuthStore()

    // 已登录但用户信息未恢复（页面刷新）
    if (authStore.isLoggedIn && !authStore.user) {
      await authStore.restoreSession()
    }

    // 不需要登录的页面，直接放行
    if (!requiresAuth(to)) {
      // 已登录访问登录页，跳转默认门户
      if (to.path === '/login' && authStore.isLoggedIn) {
        return authStore.getDefaultPortal()
      }
      return true
    }

    // 需要登录但未登录
    if (!authStore.isLoggedIn) {
      return { path: '/login', query: { redirect: to.fullPath } }
    }

    return true
  })
}
