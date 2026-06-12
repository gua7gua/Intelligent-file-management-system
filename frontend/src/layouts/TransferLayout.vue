<template>
  <div class="app-shell">
    <aside class="sidebar">
      <div class="brand">
        <strong>智能档案管理系统</strong>
        <span>移交单位门户</span>
      </div>
      <nav class="nav-section" aria-label="移交单位门户导航">
        <div class="nav-title">移交业务</div>
        <router-link to="/transfer/overview" class="nav-link" :class="{ active: route.path === '/transfer/overview' }">
          移交工作台 <span>当前</span>
        </router-link>
        <router-link to="/transfer/transfer-list" class="nav-link" :class="{ active: route.path === '/transfer/transfer-list' }">
          编制移交清单 <span>新建</span>
        </router-link>
      </nav>
    </aside>

    <main class="main">
      <header class="topbar">
        <div>
          <strong>{{ route.meta.title || '移交工作台' }}</strong>
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
