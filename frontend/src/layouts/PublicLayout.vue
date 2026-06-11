<template>
  <div class="public-layout">
    <div class="admin-header">
      <div class="admin-header-left">
        <span style="font-weight:700;font-size:16px;">📋 克拉玛依市档案馆</span>
      </div>
      <div class="admin-header-right">
        <PortalSwitcher v-if="authStore.isLoggedIn" />
        <template v-else>
          <el-button text @click="$router.push('/login')">登录</el-button>
          <el-button text @click="$router.push('/public/register')">注册</el-button>
        </template>
      </div>
    </div>
    <div class="admin-content">
      <router-view />
    </div>
  </div>
</template>

<script setup lang="ts">
import { useAuthStore } from '@/stores/auth'
import PortalSwitcher from '@/components/PortalSwitcher/index.vue'

const authStore = useAuthStore()
</script>
