<template>
  <div>
    <section>
      <h1 class="page-title">公众概览</h1>
      <p class="page-subtitle">查看您的征集清单进度和历史下载记录。</p>
    </section>

    <div v-if="loading" class="notice" style="margin-top: 18px">正在加载…</div>
    <div v-else-if="loadError" class="notice danger" style="margin-top: 18px">{{ loadError }}</div>

    <template v-else>
      <!-- 操作入口 -->
      <section class="card panel" style="margin-top: 18px">
        <div class="detail-head">
          <div>
            <h2 class="section-title">{{ authStore.user?.realName ? authStore.user.realName + '，欢迎回来' : '欢迎使用公众档案服务' }}</h2>
            <p v-if="overview.user.phone" class="muted" style="margin: 4px 0 0">
              {{ overview.user.phone }}
              <span :class="['status', userStatusClass(overview.user.status)]" style="margin-left: 10px">{{ userStatusLabel(overview.user.status) }}</span>
            </p>
          </div>
        </div>
        <div class="actions" style="margin-top: 12px">
          <router-link to="/public/search" class="button">公开档案检索</router-link>
          <router-link to="/public/collection" class="button secondary">提交征集清单</router-link>
        </div>
      </section>

      <!-- 公开馆藏统计 -->
      <section class="grid four" style="margin-top: 16px">
        <div class="metric">
          <span class="label">公开档案总量</span>
          <span class="value">{{ overview.stats.openArchiveCount.toLocaleString() }}</span>
        </div>
        <div class="metric">
          <span class="label">电子文件数</span>
          <span class="value">{{ overview.stats.electronicFileCount.toLocaleString() }}</span>
        </div>
        <div class="metric">
          <span class="label">近 30 日新增公开</span>
          <span class="value">{{ overview.stats.latestOpenCount.toLocaleString() }}</span>
        </div>
        <div class="metric">
          <span class="label">全馆征集批次</span>
          <span class="value">{{ overview.stats.collectionCount.toLocaleString() }}</span>
        </div>
      </section>

      <!-- 征集统计 -->
      <section class="grid four" style="margin-top: 16px">
        <div class="metric">
          <span class="label">征集总数</span>
          <span class="value">{{ overview.collectionSummary.total }}</span>
        </div>
        <div class="metric">
          <span class="label">草稿</span>
          <span class="value">{{ overview.collectionSummary.draft }}</span>
        </div>
        <div class="metric">
          <span class="label">进行中</span>
          <span class="value">{{ overview.collectionSummary.inProgress }}</span>
        </div>
        <div class="metric">
          <span class="label">已完成</span>
          <span class="value">{{ overview.collectionSummary.completed }}</span>
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
              <tr v-if="overview.recentCollections.length === 0">
                <td colspan="5"><div class="empty">暂无征集记录</div></td>
              </tr>
              <tr v-for="col in overview.recentCollections" :key="col.id">
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
        <h2 class="section-title">下载记录 <span class="muted" style="font-size: 13px; font-weight: 400">共 {{ overview.stats.myDownloadCount }} 次</span></h2>
        <div class="table-wrap" style="margin-top: 12px">
          <table>
            <thead>
              <tr>
                <th>档号 / 题名</th>
                <th>访问类型</th>
                <th>访问时间</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="overview.downloadLogs.length === 0">
                <td colspan="3"><div class="empty">暂无下载记录</div></td>
              </tr>
              <tr v-for="dl in overview.downloadLogs" :key="dl.id">
                <td>
                  <strong class="mono">{{ dl.archiveNo || `档案 #${dl.archiveId}` }}</strong>
                  <div v-if="dl.title" class="muted">{{ dl.title }}</div>
                </td>
                <td>{{ accessTypeLabel(dl.accessType) }}</td>
                <td>{{ dl.accessedAt }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <p class="muted" style="margin-top: 14px">数据更新于 {{ formatDateTime(overview.summarizedAt) }}</p>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getPublicOverview } from '@/api/public'
import { useAuthStore } from '@/stores/auth'
import type { PublicOverviewData } from '@/types/public'

const authStore = useAuthStore()

const loading = ref(true)
const loadError = ref('')

const defaultOverview: PublicOverviewData = {
  collectionSummary: { total: 0, draft: 0, inProgress: 0, completed: 0 },
  recentCollections: [],
  downloadLogs: [],
  stats: {
    openArchiveCount: 0,
    electronicFileCount: 0,
    collectionCount: 0,
    latestOpenCount: 0,
    myPendingCollections: 0,
    myDownloadCount: 0,
  },
  user: { realName: '', phone: '', status: 'active' },
  summarizedAt: '',
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

function userStatusClass(status: string): string {
  return status === 'disabled' ? 'danger' : 'success'
}

function userStatusLabel(status: string): string {
  return status === 'disabled' ? '已停用' : '正常'
}

function accessTypeLabel(t: string): string {
  const map: Record<string, string> = { download: '下载', preview: '预览' }
  return map[t] || t
}

// 后端 summarizedAt 是带纳秒精度的 OffsetDateTime ISO 串（如 2026-06-16T22:49:08.99102Z），
// 直接展示会让用户看到原始时间戳，统一格式化为本地可读时间。
function formatDateTime(iso?: string | null): string {
  if (!iso) return '-'
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return iso
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
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
