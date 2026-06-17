<template>
  <section>
    <div v-if="loading" class="notice">加载工作台…</div>
    <div v-else-if="error" class="notice danger">{{ error }}</div>
    <template v-else-if="data">
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
            <ul v-if="data.recentViews.length" class="record-list">
              <li v-for="item in data.recentViews" :key="item.archiveId" class="record-card">
                <div>
                  <h3>{{ item.title }}</h3>
                  <div class="meta-line">
                    <span>档号：{{ item.archiveNo }}</span>
                    <span v-if="item.accessedAt">{{ formatDate(item.accessedAt) }}</span>
                    <span v-else class="muted">查阅时间未记录</span>
                  </div>
                </div>
                <div class="actions">
                  <router-link :to="`/internal/archives/${item.archiveId}`" class="button ghost">继续查看</router-link>
                </div>
              </li>
            </ul>
            <p v-else class="muted empty-tip">暂无最近查阅记录。</p>
          </div>

          <div class="card panel">
            <div class="toolbar">
              <div>
                <h2 class="section-title">我的借阅申请</h2>
                <p class="page-subtitle">提交申请后可在本页跟踪审批与到馆核验进度。</p>
              </div>
              <router-link to="/internal/borrow-requests" class="button ghost">查看全部</router-link>
            </div>
            <div v-if="data.myBorrowRequests.length" class="table-wrap">
              <table>
                <thead>
                  <tr><th>申请号</th><th>档案题名</th><th>申请时间</th><th>状态</th><th>操作</th></tr>
                </thead>
                <tbody>
                  <tr v-for="req in data.myBorrowRequests" :key="req.requestNo">
                    <td class="mono">{{ req.requestNo }}</td>
                    <td>{{ req.title }}</td>
                    <td>{{ formatDate(req.appliedAt) }}</td>
                    <td><span :class="['status', statusClass(req.status)]">{{ statusLabel(req.status) }}</span></td>
                    <td>
                      <button v-if="canExportVoucher(req.status)" class="button secondary" type="button" @click="handleExportByRequestNo(req.requestNo)">导出凭证</button>
                      <router-link v-else :to="`/internal/borrow-requests?focus=${req.requestNo}`" class="button ghost">查看申请</router-link>
                    </td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p v-else class="muted empty-tip">暂无借阅申请。</p>
          </div>

          <div v-if="data.currentBorrows.length" class="card panel">
            <div class="toolbar">
              <div>
                <h2 class="section-title">当前借阅</h2>
                <p class="page-subtitle">已出库待归还的纸质档案。</p>
              </div>
            </div>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr><th>申请号</th><th>档案题名</th><th>状态</th><th>到期时间</th></tr>
                </thead>
                <tbody>
                  <tr v-for="req in data.currentBorrows" :key="req.requestNo">
                    <td class="mono">{{ req.requestNo }}</td>
                    <td>{{ req.title }}</td>
                    <td><span :class="['status', statusClass(req.status)]">{{ statusLabel(req.status) }}</span></td>
                    <td>{{ req.dueAt ? formatDate(req.dueAt) : '—' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>

          <div v-if="data.overdueReminders.length" class="card panel">
            <div class="toolbar">
              <div>
                <h2 class="section-title">逾期提示</h2>
                <p class="page-subtitle">以下借阅已超期，请尽快归还。</p>
              </div>
            </div>
            <div class="table-wrap">
              <table>
                <thead>
                  <tr><th>申请号</th><th>档案题名</th><th>到期时间</th></tr>
                </thead>
                <tbody>
                  <tr v-for="req in data.overdueReminders" :key="req.requestNo">
                    <td class="mono">{{ req.requestNo }}</td>
                    <td>{{ req.title }}</td>
                    <td><span class="status danger">{{ req.dueAt ? formatDate(req.dueAt) : '—' }}</span></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
      </section>
    </template>
  </section>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { exportBorrowVoucher, getInternalDashboard, getMyBorrowRequests } from '@/api/internal'
import { BorrowStatusLabel } from '@/types/enums'
import type { InternalDashboardData } from '@/types/internal'

const loading = ref(true)
const error = ref('')
const data = ref<InternalDashboardData | null>(null)

function statusLabel(status: string): string {
  return BorrowStatusLabel[status as keyof typeof BorrowStatusLabel] ?? status
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
function formatDate(iso?: string): string {
  return iso ? iso.slice(0, 10) : '—'
}

async function handleExportByRequestNo(requestNo: string) {
  try {
    // 后端凭证接口按 requestId 路径参数，工作台摘要只有 requestNo，先从我的借阅列表查回 id。
    const page = await getMyBorrowRequests({ pageNo: 1, pageSize: 50, keyword: requestNo })
    const target = page.records.find((r) => r.requestNo === requestNo)
    if (!target) {
      ElMessage.error('未找到该借阅申请，无法导出凭证')
      return
    }
    const blob = await exportBorrowVoucher(target.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `voucher-${requestNo}.pdf`
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
  grid-template-columns: minmax(0, 1fr);
  gap: 16px;
  align-items: start;
  margin-top: 16px;
}
.dashboard-layout .grid { display: grid; gap: 16px; }
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
.empty-tip { padding: 12px 0; font-size: 13px; }
@media (max-width: 640px) { .record-card { grid-template-columns: 1fr; } }
</style>
