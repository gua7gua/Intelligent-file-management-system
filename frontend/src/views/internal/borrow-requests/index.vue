<template>
  <section>
    <div class="toolbar" style="margin: 0 0 12px">
      <div>
        <h1 class="page-title">我的借阅申请</h1>
        <p class="page-subtitle">查阅本人纸质借阅申请的审批、凭证与出库归还进度。</p>
      </div>
      <router-link to="/internal/search" class="button">发起新申请</router-link>
    </div>

    <div class="card panel">
      <div class="form-grid">
        <div class="field">
          <label>状态</label>
          <select v-model="filter.status" @change="loadList">
            <option value="">全部</option>
            <option value="applied">待审批</option>
            <option value="approved">已批准</option>
            <option value="voucher_issued">凭证已生成</option>
            <option value="checked_out">已借出</option>
            <option value="returned">已归还</option>
            <option value="abnormal_return">异常归还</option>
            <option value="rejected">已拒绝</option>
          </select>
        </div>
        <div class="field">
          <label>关键词</label>
          <input v-model="filter.keyword" placeholder="申请号、档号、题名" @keyup.enter="loadList">
        </div>
        <div class="field">
          <label>&nbsp;</label>
          <button class="button" type="button" @click="loadList">查询</button>
        </div>
      </div>
    </div>

    <div class="card panel" style="margin-top: 16px">
      <div v-if="loading" class="notice">加载中…</div>
      <div v-else-if="error" class="notice danger">{{ error }}</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr><th>申请号</th><th>档案题名</th><th>申请时间</th><th>状态</th><th>操作</th></tr>
          </thead>
          <tbody>
            <tr v-if="records.length === 0"><td colspan="5"><div class="empty">没有符合条件的申请</div></td></tr>
            <tr v-for="req in records" :key="req.id">
              <td class="mono">{{ req.requestNo }}</td>
              <td>{{ req.archiveTitle }}</td>
              <td>{{ formatDate(req.appliedAt) }}</td>
              <td>
                <span :class="['status', statusClass(req.status)]">{{ statusLabel(req.status) }}</span>
                <span v-if="req.overdue" class="status danger" style="margin-left: 4px">已逾期</span>
              </td>
              <td>
                <button class="button ghost" type="button" @click="openDetail(req.id)">查看详情</button>
                <button v-if="canExportVoucher(req.status)" class="button secondary" type="button" @click="handleExport(req.id)">导出凭证</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <el-drawer v-model="drawerVisible" size="420px" :title="detailTitle" direction="rtl">
      <div v-if="detailLoading" class="notice">加载详情…</div>
      <div v-else-if="detail">
        <div class="detail-grid">
          <div class="detail-line"><span class="muted">申请号</span><strong>{{ detail.requestNo }}</strong></div>
          <div class="detail-line"><span class="muted">档案</span><strong>{{ detail.archiveTitle }}（{{ detail.archiveNo }}）</strong></div>
          <div class="detail-line"><span class="muted">状态</span><strong>{{ statusLabel(detail.status) }}</strong></div>
          <div class="detail-line"><span class="muted">借阅天数</span><strong>{{ detail.expectedDays }} 天</strong></div>
          <div class="detail-line"><span class="muted">预计到馆</span><strong>{{ detail.expectedVisitAt ? formatDate(detail.expectedVisitAt) : '-' }}</strong></div>
          <div class="detail-line"><span class="muted">申请时间</span><strong>{{ formatDate(detail.appliedAt) }}</strong></div>
          <div v-if="detail.approvedAt" class="detail-line"><span class="muted">批准时间</span><strong>{{ formatDate(detail.approvedAt) }}</strong></div>
          <div v-if="detail.checkedOutAt" class="detail-line"><span class="muted">出库时间</span><strong>{{ formatDate(detail.checkedOutAt) }}</strong></div>
          <div v-if="detail.dueAt" class="detail-line"><span class="muted">应还时间</span><strong>{{ formatDate(detail.dueAt) }}</strong></div>
          <div v-if="detail.returnedAt" class="detail-line"><span class="muted">归还时间</span><strong>{{ formatDate(detail.returnedAt) }}</strong></div>
          <div v-if="detail.voucherNo" class="detail-line"><span class="muted">凭证号</span><strong>{{ detail.voucherNo }}</strong></div>
          <div class="detail-line"><span class="muted">联系电话</span><strong>{{ detail.contactPhone }}</strong></div>
          <div class="detail-line"><span class="muted">借阅理由</span><strong>{{ detail.reason }}</strong></div>
          <div v-if="detail.opinion" class="detail-line"><span class="muted">审批意见</span><strong>{{ detail.opinion }}</strong></div>
          <div v-if="detail.rejectReason" class="detail-line"><span class="muted">拒绝原因</span><strong>{{ detail.rejectReason }}</strong></div>
          <div v-if="detail.returnNote" class="detail-line"><span class="muted">归还备注</span><strong>{{ detail.returnNote }}</strong></div>
        </div>
        <div v-if="canExportVoucher(detail.status)" class="actions" style="margin-top: 12px">
          <button class="button secondary" type="button" @click="handleExport(detail.id)">导出借阅凭证</button>
        </div>
        <div class="notice" style="margin-top: 12px">凭证需单位盖章，到馆核验后才可确认出库。</div>
      </div>
    </el-drawer>
  </section>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { exportBorrowVoucher, getBorrowRequestDetail, getMyBorrowRequests } from '@/api/internal'
import { BorrowStatusLabel } from '@/types/enums'
import type { BorrowRequest, BorrowRequestDetail } from '@/types/internal'

const route = useRoute()

const filter = reactive<{ status: string; keyword: string }>({ status: '', keyword: '' })
const loading = ref(false)
const error = ref('')
const records = ref<BorrowRequest[]>([])

const drawerVisible = ref(false)
const detailLoading = ref(false)
const detail = ref<BorrowRequestDetail | null>(null)
const detailTitle = computed(() => detail.value?.requestNo ?? '申请详情')

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

async function loadList() {
  loading.value = true
  error.value = ''
  try {
    const page = await getMyBorrowRequests({
      status: (filter.status || undefined) as BorrowRequest['status'] | undefined,
      keyword: filter.keyword || undefined,
    })
    records.value = page.records
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '加载借阅申请失败'
  } finally {
    loading.value = false
  }
}

async function openDetail(id: number) {
  drawerVisible.value = true
  detailLoading.value = true
  detail.value = null
  try {
    detail.value = await getBorrowRequestDetail(id)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '加载详情失败')
    drawerVisible.value = false
  } finally {
    detailLoading.value = false
  }
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
    ElMessage.success('借阅凭证已导出')
  } catch {
    ElMessage.error('导出凭证失败')
  }
}

onMounted(async () => {
  await loadList()
  const focusId = route.query.focus
  if (focusId) {
    openDetail(Number(focusId))
  }
})
</script>

<style scoped>
.detail-grid { display: grid; gap: 10px; }
.detail-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 9px;
  border-bottom: 1px solid var(--border);
}
.detail-line:last-child { border-bottom: 0; }
</style>
