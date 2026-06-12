<template>
  <div class="admin-layout">
    <!-- 侧边栏 -->
    <div class="admin-sidebar" :class="{ collapsed: appStore.sidebarCollapsed }">
      <div class="admin-sidebar-logo">
        <span class="logo-icon">📋</span>
        <span v-show="!appStore.sidebarCollapsed" class="logo-text">智能档案管理</span>
      </div>
      <div class="admin-sidebar-menu">
        <el-scrollbar>
          <el-menu
            :default-active="activeMenu"
            :collapse="appStore.sidebarCollapsed"
            :collapse-transition="false"
            background-color="#1d1e2c"
            text-color="#c0c4cc"
            active-text-color="#ffffff"
            router
          >
            <template v-for="item in filteredMenus" :key="item.path">
              <!-- 无子菜单 -->
              <el-menu-item v-if="!item.children" :index="item.path">
                <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
                <template #title>{{ item.title }}</template>
              </el-menu-item>
              <!-- 有子菜单 -->
              <el-sub-menu v-else :index="item.path">
                <template #title>
                  <el-icon v-if="item.icon"><component :is="item.icon" /></el-icon>
                  <span>{{ item.title }}</span>
                </template>
                <el-menu-item
                  v-for="child in item.children"
                  :key="child.path"
                  :index="child.path"
                >
                  {{ child.title }}
                </el-menu-item>
              </el-sub-menu>
            </template>
          </el-menu>
        </el-scrollbar>
      </div>
    </div>

    <!-- 主内容区 -->
    <div class="admin-main">
      <!-- 顶栏 -->
      <div class="admin-header">
        <div class="admin-header-left">
          <el-icon class="collapse-btn" @click="appStore.toggleSidebar()">
            <Fold v-if="!appStore.sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/admin' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">
              {{ route.meta.title }}
            </el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="admin-header-right">
          <PortalSwitcher />
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ authStore.user?.realName || authStore.user?.username || '未登录' }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="authStore.logout()">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </div>

      <!-- 内容区 -->
      <div class="admin-content">
        <router-view />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { Fold, Expand, User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import { adminMenuConfig } from '@/router/routes/admin'
import PortalSwitcher from '@/components/PortalSwitcher/index.vue'
import type { MenuItem } from '@/types/menu'

const route = useRoute()
const authStore = useAuthStore()
const appStore = useAppStore()

/** 当前激活的菜单项 */
const activeMenu = computed(() => route.path)

/** 根据用户角色过滤菜单 */
const filteredMenus = computed(() => {
  const roles = authStore.roles
  return adminMenuConfig.filter((item) => filterMenu(item, roles))
})

function filterMenu(item: MenuItem, roles: string[]): boolean {
  const hasRole = item.roles.some((r) => roles.includes(r))
  if (!hasRole) return false
  if (item.children) {
    const filteredChildren = item.children.filter((child) => filterMenu(child, roles))
    return filteredChildren.length > 0
  }
  return true
}
</script>
