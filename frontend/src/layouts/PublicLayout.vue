<template>
  <div class="public-shell">
    <nav class="public-nav" aria-label="公众门户导航">
      <router-link to="/public/index" class="nav-brand">智能档案馆</router-link>
      <div class="links">
        <router-link to="/public/search">公开检索</router-link>
        <router-link to="/public/collection">征集清单</router-link>
        <router-link v-if="authStore.isLoggedIn" to="/public/overview">公众概览</router-link>
        <template v-if="!authStore.isLoggedIn">
          <router-link to="/login">登录</router-link>
          <router-link to="/public/register">注册</router-link>
        </template>
        <template v-else>
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
        </template>
      </div>
    </nav>

    <main class="public-content">
      <router-view />
    </main>
  </div>
</template>

<script setup lang="ts">
import { User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import PortalSwitcher from '@/components/PortalSwitcher/index.vue'

const authStore = useAuthStore()
</script>

<style scoped>
.nav-brand {
  font-weight: 700;
  color: var(--text);
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--muted);
  font-size: 13px;
  padding: 8px 10px;
}
</style>
