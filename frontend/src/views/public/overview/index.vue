<template>
  <div>
    <section>
      <h1 class="page-title">公众概览</h1>
      <p class="page-subtitle">查看您的账号状态、征集清单进度和历史下载记录。</p>
    </section>

    <div v-if="loading" class="notice" style="margin-top: 18px">正在加载…</div>
    <div v-else-if="loadError" class="notice danger" style="margin-top: 18px">{{ loadError }}</div>

    <template v-else>
      <!-- 欢迎区 -->
      <section class="card panel" style="margin-top: 18px">
        <div class="detail-head">
          <div>
            <h2 class="section-title">{{ overview.user.realName }}，欢迎回来</h2>
            <p class="muted">手机号：{{ overview.user.phone }}</p>
          </div>
          <span :class="['status', overview.user.status === 'active' ? 'success' : 'danger']">
            {{ overview.user.status === 'active' ? '正常' : '已停用' }}
          </span>
        </div>
        <div class="actions" style="margin-top: 12px">
          <router-link to="/public/search" class="button">公开档案检索</router-link>
          <router-link to="/public/collection" class="button secondary">提交征集清单</router-link>
        </div>
      </section>

      <!-- 统计卡片 -->
      <section class="grid four" style="margin-top: 16px">
        <div class="metric">
          <span class="label">公开档案</span>
          <span class="value">{{ overview.stats.openArchiveCount }}</span>
        </div>
        <div class="metric">
          <span class="label">电子文件</span>
          <span class="value">{{ overview.stats.electronicFileCount }}</span>
        </div>
        <div class="metric">
          <span class="label">我的征集</span>
          <span class="value">{{ overview.stats.myPendingCollections }}</span>
        </div>
        <div class="metric">
          <span class="label">下载次数</span>
          <span class="value">{{ overview.stats.myDownloadCount }}</span>
        </div>
      </section>

      <!-- 征集清单 -->
      <section style="margin-top: 18px">
        <h2 class="section-title">征集清单</h2>
        <div class="table-wrap" style="margin-top: 12px">
          <table>
            <thead>
              <tr>
                <th>清单号</th>
                <th>标题</th>
                <th>条目</th>
                <th>状态</th>
                <th>提交时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="overview.collections.length === 0">
                <td colspan="5"><div class="empty">暂无征集记录</div></td>
              </tr>
              <tr v-for="col in overview.collections" :key="col.id">
                <td class="mono">{{ col.batchNo }}</td>
                <td>{{ col.title }}</td>
                <td>{{ col.itemCount }} 件</td>
                <td><span :class="['status', collectionStatusClass(col.status)]">{{ col.statusText }}</span></td>
                <td>{{ col.submittedAt || '未提交' }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- 下载记录 -->
      <section style="margin-top: 18px">
        <h2 class="section-title">下载记录</h2>
        <div class="table-wrap" style="margin-top: 12px">
          <table>
            <thead>
              <tr>
                <th>档案号</th>
                <th>标题</th>
                <th>下载时间</th>
                <th>访问状态</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="overview.downloads.length === 0">
                <td colspan="4"><div class="empty">暂无下载记录</div></td>
              </tr>
              <tr v-for="dl in overview.downloads" :key="dl.id">
                <td class="mono">{{ dl.archiveNo }}</td>
                <td>{{ dl.title }}</td>
                <td>{{ dl.downloadedAt }}</td>
                <td>
                  <span v-if="dl.accessStatus === 'available'" class="status success">可访问</span>
                  <span v-else class="status danger">权限已变更，请重新鉴权</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPublicOverview } from '@/api/public'
import type { PublicOverviewData } from '@/types/public'

const loading = ref(true)
const loadError = ref('')

const defaultOverview: PublicOverviewData = {
  user: { realName: '', phone: '', status: 'active' },
  stats: {
    openArchiveCount: 0,
    electronicFileCount: 0,
    collectionCount: 0,
    latestOpenCount: 0,
    myPendingCollections: 0,
    myDownloadCount: 0,
  },
  collections: [],
  downloads: [],
}

const overview = ref<PublicOverviewData>(defaultOverview)

function collectionStatusClass(status: string): string {
  const map: Record<string, string> = {
    draft: '',
    pending_contact: 'info',
    pending_receive: 'info',
    received: 'success',
    partially_received: 'warning',
    rejected: 'danger',
  }
  return map[status] || ''
}

onMounted(async () => {
  try {
    overview.value = await getPublicOverview()
  } catch (e: any) {
    loadError.value = e.message || '加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.detail-head {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: flex-start;
  justify-content: space-between;
}
</style>
