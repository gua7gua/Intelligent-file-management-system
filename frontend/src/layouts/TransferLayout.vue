<template>
  <div class="public-layout">
    <div class="admin-header">
      <div class="admin-header-left">
        <span style="font-weight:700;font-size:16px;">📋 移交单位门户</span>
      </div>
      <div class="admin-header-right">
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
    </div>
    <div class="admin-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import PortalSwitcher from '@/components/PortalSwitcher/index.vue'

const authStore = useAuthStore()
</script>
