<template>
  <el-dropdown v-if="portals.length > 1" trigger="click" @command="handleSwitch">
    <el-button text size="small">
      <el-icon><Switch /></el-icon>
      切换门户
    </el-button>
    <template #dropdown>
      <el-dropdown-menu>
        <el-dropdown-item
          v-for="portal in portals"
          :key="portal.key"
          :command="portal.path"
          :disabled="currentPath === portal.path"
        >
          {{ portal.label }}
        </el-dropdown-item>
      </el-dropdown-menu>
    </template>
  </el-dropdown>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Switch } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const portals = computed(() => authStore.getAllowedPortals())
const currentPath = computed(() => route.path)

function handleSwitch(path: string) {
  router.push(path)
}
</script>
