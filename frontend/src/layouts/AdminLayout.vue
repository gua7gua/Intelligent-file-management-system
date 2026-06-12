<template>
  <div class="app-shell">
    <!-- 侧边栏 -->
    <aside class="sidebar" :class="{ 'sidebar-collapsed': appStore.sidebarCollapsed }">
      <div class="brand">
        <strong>智能档案管理系统</strong>
        <span>管理后台</span>
      </div>

      <template v-for="section in menuSections" :key="section.title">
        <nav class="nav-section">
          <div class="nav-title">{{ section.title }}</div>
          <router-link
            v-for="item in section.items"
            :key="item.path"
            :to="item.path"
            class="nav-link"
            :class="{ active: isActive(item.path) }"
          >
            {{ item.title }}
            <span v-if="item.tag">{{ item.tag }}</span>
          </router-link>
        </nav>
      </template>
    </aside>

    <!-- 主内容区 -->
    <main class="main">
      <header class="topbar">
        <div>
          <strong>{{ route.meta.title || '管理概览' }}</strong>
          <span class="muted"> {{ route.meta.subtitle || '' }}</span>
        </div>
        <div class="role">
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
      </header>

      <section class="content">
        <router-view />
      </section>
    </main>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { User } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { useAppStore } from '@/stores/app'
import PortalSwitcher from '@/components/PortalSwitcher/index.vue'

const route = useRoute()
const authStore = useAuthStore()
const appStore = useAppStore()

/** 导航项 */
interface NavItem { path: string; title: string; tag?: string; roles: string[] }
/** 导航分组 */
interface NavSection { title: string; items: NavItem[] }

/** 侧边栏导航分组（对齐原型 admin 页面结构） */
const menuSections = computed<NavSection[]>(() => {
  const roles = authStore.roles

  const sections: NavSection[] = [
    {
      title: '工作台',
      items: [
        { path: '/admin/overview', title: '管理概览', tag: '总览', roles: ['front_archivist', 'back_archivist', 'director', 'sys_admin'] },
      ],
    },
    {
      title: '接收入库',
      items: [
        { path: '/admin/transfer-reception', title: '移交验收', tag: '前台', roles: ['front_archivist', 'back_archivist'] },
        { path: '/admin/collection', title: '征集管理与接收', tag: '征集', roles: ['front_archivist', 'back_archivist'] },
        { path: '/admin/pending-archive', title: '待入库与上架', tag: '后台', roles: ['back_archivist'] },
        { path: '/admin/archive-management', title: '档案管理', tag: '整理', roles: ['back_archivist'] },
        { path: '/admin/fonds', title: '全宗管理', tag: '全宗', roles: ['back_archivist'] },
      ],
    },
    {
      title: '库房与利用',
      items: [
        { path: '/admin/warehouse', title: '库房管理', tag: '架位', roles: ['back_archivist'] },
        { path: '/admin/inventory', title: '档案盘点', tag: '盘点', roles: ['back_archivist'] },
        { path: '/admin/borrow-approval', title: '借阅审批', tag: '闭环', roles: ['front_archivist', 'back_archivist'] },
      ],
    },
    {
      title: '处置与审批',
      items: [
        { path: '/admin/appraisal', title: '档案鉴定', tag: '当前', roles: ['back_archivist'] },
        { path: '/admin/destruction', title: '档案销毁', tag: '清册', roles: ['back_archivist'] },
        { path: '/admin/approval', title: '审批工作台', tag: '领导', roles: ['director'] },
      ],
    },
    {
      title: '支撑管理',
      items: [
        { path: '/admin/compilation', title: '档案编研', tag: '编研', roles: ['back_archivist'] },
        { path: '/admin/statistics', title: '数据统计', tag: '报表', roles: ['back_archivist', 'director'] },
        { path: '/admin/data-analysis', title: '数据研判', tag: '分析', roles: ['back_archivist'] },
        { path: '/admin/preservation', title: '档案保存', tag: '备份', roles: ['back_archivist'] },
        { path: '/admin/user-management', title: '用户管理', tag: '系统', roles: ['sys_admin'] },
        { path: '/admin/system-settings', title: '系统配置', tag: '设置', roles: ['sys_admin'] },
      ],
    },
  ]

  // 按角色过滤：过滤掉无权限的项，整个 section 为空则移除
  return sections
    .map((section) => ({
      ...section,
      items: section.items.filter((item) => item.roles.some((r) => roles.includes(r))),
    }))
    .filter((section) => section.items.length > 0)
})

function isActive(path: string): boolean {
  return route.path === path
}
</script>

<style scoped lang="scss">
@use '../styles/variables' as *;

/* 折叠态：覆盖 shell.css 的固定宽度 */
.sidebar-collapsed {
  overflow: hidden;
  width: $sidebar-collapsed-width !important;
}

.sidebar-collapsed .brand span,
.sidebar-collapsed .nav-title,
.sidebar-collapsed .nav-link span:last-child {
  font-size: 0;
  line-height: 0;
  overflow: hidden;
}

.sidebar-collapsed .nav-link {
  justify-content: center;
  min-height: 38px;
  padding: 8px;
}

.sidebar-collapsed .brand {
  padding: 12px;
}

.sidebar-collapsed .brand strong {
  font-size: 0;
}

.sidebar-collapsed .brand strong::after {
  content: '📋';
  font-size: 22px;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: var(--muted);
  font-size: 13px;
}
</style>
