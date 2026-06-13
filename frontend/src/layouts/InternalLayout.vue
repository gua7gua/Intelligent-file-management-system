<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <strong>智能档案管理系统</strong>
        <span>内部查阅者门户</span>
      </div>
      <nav class="nav-section" aria-label="内部门户导航">
        <div class="nav-title">个人工作台</div>
        <router-link to="/internal/overview" class="nav-link" :class="{ active: route.path === '/internal/overview' }">
          工作台概览
        </router-link>
        <router-link to="/internal/search" class="nav-link" :class="{ active: route.path === '/internal/search' }">
          档案检索利用
        </router-link>
        <router-link to="/internal/borrow-requests" class="nav-link" :class="{ active: route.path === '/internal/borrow-requests' }">
          我的借阅申请
        </router-link>
      </nav>
    </aside>

    <main class="main">
      <header class="topbar">
        <div>
          <strong>{{ route.meta.title || '工作台概览' }}</strong>
        </div>
        <div class="role">
          <PortalSwitcher />
          <el-dropdown trigger="click">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ authStore.user?.realName || '未登录' }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item @click="authStore.logout()">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </header>

      <section class="content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { User } from '@element-plus/icons-vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import PortalSwitcher from '@/components/PortalSwitcher/index.vue'

const route = useRoute()
const authStore = useAuthStore()
</script>

<style scoped>
.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--muted);
  font-size: 13px;
}
</style>
