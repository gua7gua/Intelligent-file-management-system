<template>
  <section>
    <div v-if="loading" class="notice">加载工作台…</div>
    <div v-else-if="error" class="notice danger">{{ error }}</div>
    <template v-else-if="data">
      <section class="grid four">
        <div class="metric"><span class="label">最近查阅</span><span class="value">{{ data.stats.recentViewCount }}</span><span class="note">近 30 日元数据访问</span></div>
        <div class="metric"><span class="label">待审批申请</span><span class="value">{{ data.stats.pendingApprovalCount }}</span><span class="note">等待管理员审核</span></div>
        <div class="metric"><span class="label">已批准待取件</span><span class="value">{{ data.stats.approvedPendingPickupCount }}</span><span class="note">可导出借阅凭证</span></div>
        <div class="metric"><span class="label">下载记录</span><span class="value">{{ data.stats.downloadCount }}</span><span class="note">近期电子文件下载</span></div>
      </section>

      <section class="dashboard-layout">
        <div class="grid">
          <div class="card panel">
            <div class="toolbar">
              <div>
                <h2 class="section-title">我的最近查阅</h2>
                <p class="page-subtitle">您近期查阅过的档案。</p>
              </div>
              <router-link to="/internal/search" class="button">进入检索</router-link>
            </div>
            <ul class="record-list">
              <li v-for="item in data.recentViews" :key="item.id" class="record-card">
                <div>
                  <h3>{{ item.title }}</h3>
                  <div class="meta-line">
                    <span>档号：{{ item.archiveNo }}</span>
                    <span>分类：{{ item.categoryName }}</span>
                    <span>密级：{{ securityLabel(item.securityLevel) }}</span>
                    <span>{{ formatDate(item.viewedAt) }}</span>
                  </div>
                </div>
                <div class="actions">
                  <span :class="['status', item.accessStatus === 'available' ? 'success' : 'warning']">
                    {{ item.accessStatus === 'available' ? '可继续查阅' : '仅保留历史记录' }}
                  </span>
                  <router-link :to="`/internal/archives/${item.archiveId}`" class="button ghost">继续查看</router-link>
                </div>
              </li>
            </ul>
          </div>

          <div class="card panel">
            <div class="toolbar">
              <div>
                <h2 class="section-title">我的借阅申请</h2>
                <p class="page-subtitle">提交申请后可在本页跟踪审批与到馆核验进度。</p>
              </div>
              <router-link to="/internal/borrow-requests" class="button ghost">查看全部</router-link>
            </div>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr><th>申请号</th><th>档案题名</th><th>申请时间</th><th>状态</th><th>操作</th></tr>
                </thead>
                <tbody>
                  <tr v-for="req in data.borrowRequests" :key="req.id">
                    <td class="mono">{{ req.requestNo }}</td>
                    <td>{{ req.archiveTitle }}</td>
                    <td>{{ formatDate(req.appliedAt) }}</td>
                    <td><span :class="['status', statusClass(req.status)]">{{ statusLabel(req.status) }}</span></td>
                    <td>
                      <button v-if="canExportVoucher(req.status)" class="button secondary" type="button" @click="handleExport(req.id)">导出凭证</button>
                      <router-link v-else :to="`/internal/borrow-requests?focus=${req.id}`" class="button ghost">查看申请</router-link>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <aside class="grid">
          <div class="card panel">
            <h2 class="section-title">我的权限范围</h2>
            <div class="authority-grid">
              <div class="authority-line"><span class="muted">角色</span><strong>{{ data.permission.role }}</strong></div>
              <div class="authority-line"><span class="muted">所属单位</span><strong>{{ data.permission.organizationName }}</strong></div>
              <div class="authority-line"><span class="muted">密级上限</span><strong>{{ securityLabel(data.permission.maxSecurityLevel) }}</strong></div>
              <div class="authority-line"><span class="muted">可见范围</span><strong>{{ data.permission.dataScope }}</strong></div>
            </div>
          </div>

          <div class="card panel">
            <h2 class="section-title">我的下载记录</h2>
            <ol class="timeline">
              <li v-for="d in data.downloads" :key="d.id">
                <span class="muted">{{ formatDate(d.downloadedAt) }}</span>
                <span>{{ d.title }}</span>
              </li>
            </ol>
          </div>

          <div class="card panel">
            <h2 class="section-title">利用边界</h2>
            <div class="notice">纸质原件请通过借阅申请获取。</div>
          </div>
        </aside>
      </section>
    </template>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { exportBorrowVoucher, getInternalDashboard } from '@/api/internal'
import { BorrowStatusLabel, SecurityLevelLabel } from '@/types/enums'
import type { InternalDashboardData } from '@/types/internal'

const loading = ref(true)
const error = ref('')
const data = ref<InternalDashboardData | null>(null)

function securityLabel(level: number): string {
  return SecurityLevelLabel[level] ?? '未知'
}
function statusLabel(status: string): string {
  return BorrowStatusLabel[status] ?? status
}
function statusClass(status: string): string {
  const map: Record<string, string> = {
    applied: 'warning',
    approved: 'success',
    voucher_issued: 'success',
    checked_out: 'info',
    returned: 'info',
    abnormal_return: 'danger',
    rejected: 'danger',
  }
  return map[status] ?? 'info'
}
function canExportVoucher(status: string): boolean {
  return status === 'approved' || status === 'voucher_issued'
}
function formatDate(iso: string): string {
  return iso.slice(0, 10)
}

async function handleExport(id: number) {
  try {
    const blob = await exportBorrowVoucher(id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `voucher-${id}.pdf`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('借阅凭证已生成，需到馆核验后才可确认出库')
  } catch {
    ElMessage.error('导出凭证失败')
  }
}

onMounted(async () => {
  loading.value = true
  try {
    data.value = await getInternalDashboard()
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '加载工作台失败'
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.dashboard-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  gap: 16px;
  align-items: start;
  margin-top: 16px;
}
.record-list { display: grid; gap: 10px; margin: 0; padding: 0; list-style: none; }
.record-card {
  display: grid;
  grid-template-columns: minmax(0, 1fr) auto;
  gap: 12px;
  align-items: center;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #fff;
}
.record-card h3 { margin: 0 0 4px; font-size: 15px; }
.meta-line { display: flex; flex-wrap: wrap; gap: 8px; color: var(--muted); font-size: 13px; }
.authority-grid { display: grid; gap: 10px; }
.authority-line { display: flex; justify-content: space-between; gap: 12px; padding-bottom: 10px; border-bottom: 1px solid var(--border); }
.authority-line:last-child { border-bottom: 0; }
.timeline { margin: 0; padding-left: 18px; display: grid; gap: 8px; }
.timeline li { display: flex; gap: 10px; }
@media (max-width: 1080px) { .dashboard-layout { grid-template-columns: 1fr; } }
@media (max-width: 640px) { .record-card { grid-template-columns: 1fr; } }
</style>
