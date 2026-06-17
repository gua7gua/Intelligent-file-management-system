<template>
  <div class="borrow-approval">
    <section>
      <h1 class="page-title">借阅审批</h1>
      <p class="page-subtitle">纸质借阅必须走审批。审批通过后只允许申请人导出凭证；到馆核验并确认出库后，档案才变为借出。</p>
    </section>

    <section class="metric-row">
      <div class="metric card"><div class="metric-num">{{ metrics.pending }}</div><div class="metric-label">待审批</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.approvedNotOut }}</div><div class="metric-label">已批准未出库</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.onLoan }}</div><div class="metric-label">借出中</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.overdueAbnormal }}</div><div class="metric-label">逾期/异常</div></div>
    </section>

    <section class="borrow-layout">
      <aside class="card panel list-col">
        <div class="tabs">
          <button class="tab" :class="{ active: filterStatus === 'applied' }" @click="filterStatus = 'applied'">待审批</button>
          <button class="tab" :class="{ active: filterStatus === 'approved' }" @click="filterStatus = 'approved'">已批准</button>
          <button class="tab" :class="{ active: filterStatus === 'checked_out' }" @click="filterStatus = 'checked_out'">借出中</button>
          <button class="tab" :class="{ active: filterStatus === 'returned' }" @click="filterStatus = 'returned'">归还处理</button>
        </div>
        <div v-if="listLoading" class="empty">加载中...</div>
        <div v-else-if="loadError" class="empty">加载失败：<button class="link" @click="loadList">重试</button></div>
        <div v-else-if="filteredList.length === 0" class="empty">当前状态下没有借阅申请。</div>
        <div v-else class="request-list">
          <button
            v-for="r in filteredList"
            :key="r.id"
            class="request-card"
            :class="{ active: selectedId === r.id }"
            @click="selectRequest(r.id)"
          >
            <strong>{{ r.archiveTitle }}</strong>
            <span class="mono">{{ r.requestNo }}</span>
            <span><span class="status" :class="statusClass(r.status)">{{ BorrowStatusLabel[r.status] }}</span> <span class="muted">{{ r.borrowerName }}</span></span>
          </button>
        </div>
      </aside>

      <section class="detail-area">
        <template v-if="!selectedId"><div class="empty">← 点击左侧申请查看详情与操作</div></template>
        <template v-else-if="detailLoading"><div class="empty">加载中...</div></template>
        <template v-else-if="!detail"><div class="empty">详情加载失败：<button class="link" @click="selectRequest(selectedId)">重试</button></div></template>
        <template v-else>
          <div class="card panel">
            <h2 class="section-title">{{ detail.archiveTitle }}</h2>
            <div class="form-grid">
              <div class="field"><label>申请号</label><input :value="detail.requestNo" disabled></div>
              <div class="field"><label>借阅人</label><input :value="`${detail.borrowerName} / ${detail.borrowerOrg ?? ''}`" disabled></div>
              <div class="field"><label>预计到馆</label><input :value="detail.expectedVisitAt ?? '—'" disabled></div>
              <div class="field"><label>借阅时长</label><input :value="`${detail.expectedDays} 天`" disabled></div>
              <div class="field"><label>档号</label><input :value="detail.archiveNo" disabled></div>
              <div class="field"><label>管理员可见架位</label><input :value="detail.archiveLocationCode ?? '—'" disabled></div>
            </div>
            <div class="field" style="margin-top:12px"><label>借阅理由</label><textarea :value="detail.reason" disabled></textarea></div>
          </div>

          <div class="grid two">
            <div class="card panel">
              <h2 class="section-title">可借检查</h2>
              <div class="check-list">
                <div><span>载体状态</span><strong>{{ detail.checkCarrier ?? '—' }}</strong></div>
                <div><span>生命周期</span><strong>{{ detail.checkLifecycle ?? '—' }}</strong></div>
                <div><span>借阅状态</span><strong>{{ detail.checkLoan ?? '—' }}</strong></div>
                <div><span>盘点范围</span><strong>{{ detail.checkInventory ?? '—' }}</strong></div>
              </div>
              <div class="field" style="margin-top:12px"><label>拒绝/退回原因</label><textarea v-model="rejectReason" placeholder="不可借时填写原因"></textarea></div>
              <div class="actions">
                <button class="button" :disabled="detail.status !== 'applied'" @click="onApprove(true)">审批通过</button>
                <button class="button danger" :disabled="detail.status !== 'applied'" @click="onApprove(false)">审批拒绝</button>
              </div>
            </div>

            <div class="card panel">
              <h2 class="section-title">凭证、出库与归还</h2>
              <div class="field"><label>凭证号</label><input v-model="voucherNo" placeholder="VCH-xxxxxx"></div>
              <div class="field"><label>应还时间</label><input v-model="dueAt" type="datetime-local"></div>
              <div class="field"><label>归还检查结果</label>
                <select v-model="returnCheckResult">
                  <option v-for="opt in returnOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
              </div>
              <div class="field"><label>归还检查说明</label><textarea v-model="returnNote" placeholder="异常归还时必填"></textarea></div>
              <div class="actions">
                <button class="button" :disabled="!canCheckout" @click="onCheckout">确认出库</button>
                <button class="button ghost" :disabled="!canReturn" @click="onReturn">确认归还</button>
              </div>
            </div>
          </div>
        </template>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { BorrowApprovalDetail, BorrowApproveData, BorrowCheckoutData, BorrowReturnData } from '@/types/borrow-approval'
import { BorrowStatusLabel, ReturnCheckResult } from '@/types/enums'
import type { ReturnCheckResultValue } from '@/types/enums'
import {
  approveBorrowRequest, checkoutBorrowRequest,
  getBorrowApprovalDetail, getBorrowApprovals, returnBorrowRequest,
} from '@/api/borrow-approval'
import { validateBorrowApprove, validateBorrowCheckout, validateBorrowReturn } from '@/utils/borrowApprovalValidation'

type FilterStatus = 'applied' | 'approved' | 'checked_out' | 'returned'

const allRequests = ref<BorrowApprovalDetail[]>([])
const listLoading = ref(false)
const loadError = ref(false)
const filterStatus = ref<FilterStatus>('applied')

const selectedId = ref<number | null>(null)
const detail = ref<BorrowApprovalDetail | null>(null)
const detailLoading = ref(false)

const rejectReason = ref('')
const voucherNo = ref('')
const dueAt = ref('')
const returnCheckResult = ref<ReturnCheckResultValue>(ReturnCheckResult.NORMAL)
const returnNote = ref('')

const returnOptions: { value: ReturnCheckResultValue; label: string }[] = [
  { value: ReturnCheckResult.NORMAL, label: '正常' },
  { value: ReturnCheckResult.MISSING_PAGE, label: '缺页' },
  { value: ReturnCheckResult.DAMAGED, label: '破损' },
  { value: ReturnCheckResult.OTHER, label: '其他' },
]

/**
 * 将 <input type="datetime-local"> 的本地值（"YYYY-MM-DDTHH:mm"）转为带本地时区偏移的
 * ISO 字符串（"YYYY-MM-DDTHH:mm:00±HH:MM"），供后端 OffsetDateTime 反序列化。
 */
function toOffsetIso(localValue: string): string {
  if (!localValue) return localValue
  const d = new Date(localValue)
  if (Number.isNaN(d.getTime())) return localValue
  const pad = (n: number) => String(n).padStart(2, '0')
  const offset = -d.getTimezoneOffset()
  const sign = offset >= 0 ? '+' : '-'
  const abs = Math.abs(offset)
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}${sign}${pad(Math.floor(abs / 60))}:${pad(abs % 60)}`
}

/**
 * 将后端返回的 ISO 偏移字符串（如 "2026-07-01T10:00:00+08:00"）转换为
 * <input type="datetime-local"> 所需的本地格式 "YYYY-MM-DDTHH:mm"。
 * 直接把带时区的 ISO 字符串塞给 datetime-local 不会被浏览器识别，导致应还时间不回填。
 */
function fromOffsetIso(iso: string | null | undefined): string {
  if (!iso) return ''
  const d = new Date(iso)
  if (Number.isNaN(d.getTime())) return ''
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`
}

const metrics = computed(() => {
  const all = allRequests.value
  return {
    pending: all.filter((r) => r.status === 'applied').length,
    approvedNotOut: all.filter((r) => r.status === 'approved' || r.status === 'voucher_issued').length,
    onLoan: all.filter((r) => r.status === 'checked_out').length,
    overdueAbnormal: all.filter((r) => r.overdue || r.status === 'abnormal_return').length,
  }
})

const filteredList = computed(() => {
  const s = filterStatus.value
  if (s === 'returned') return allRequests.value.filter((r) => ['returned', 'abnormal_return', 'rejected'].includes(r.status))
  if (s === 'approved') return allRequests.value.filter((r) => ['approved', 'voucher_issued'].includes(r.status))
  return allRequests.value.filter((r) => r.status === s)
})

const canCheckout = computed(() => !!detail.value && ['approved', 'voucher_issued'].includes(detail.value.status))
const canReturn = computed(() => detail.value?.status === 'checked_out')

function statusClass(s: string): string {
  const map: Record<string, string> = {
    applied: 'warning', approved: 'info', voucher_issued: 'info', checked_out: 'warning',
    returned: 'success', abnormal_return: 'danger', rejected: 'danger',
  }
  return map[s] ?? ''
}

async function loadList() {
  listLoading.value = true
  loadError.value = false
  try {
    const page = await getBorrowApprovals({ pageSize: 100 })
    allRequests.value = page.records
  } catch {
    loadError.value = true
  } finally {
    listLoading.value = false
  }
}

async function selectRequest(id: number) {
  selectedId.value = id
  detail.value = null
  detailLoading.value = true
  try {
    detail.value = await getBorrowApprovalDetail(id)
    resetForms()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

function resetForms() {
  rejectReason.value = ''
  voucherNo.value = detail.value?.voucherNo ?? ''
  dueAt.value = fromOffsetIso(detail.value?.dueAt)
  returnCheckResult.value = ReturnCheckResult.NORMAL
  returnNote.value = ''
}

async function onApprove(approved: boolean) {
  if (!detail.value) return
  const data: BorrowApproveData = approved ? { approved: true } : { approved: false, rejectReason: rejectReason.value }
  const errors = validateBorrowApprove(detail.value, data)
  if (errors.length) {
    errors.forEach((e) => ElMessage.warning(e))
    return
  }
  try {
    const updated = await approveBorrowRequest(detail.value.id, data)
    detail.value = updated
    ElMessage.success(approved ? '审批已通过，申请人可导出借阅凭证。' : '已拒绝借阅申请并保留原因。')
    await loadList()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onCheckout() {
  if (!detail.value) return
  // datetime-local 值为 "YYYY-MM-DDTHH:mm"，后端 OffsetDateTime 需要带时区偏移的 ISO，补秒与本地偏移
  const data: BorrowCheckoutData = { voucherNo: voucherNo.value, dueAt: toOffsetIso(dueAt.value) }
  const errors = validateBorrowCheckout(detail.value, data)
  if (errors.length) {
    errors.forEach((e) => ElMessage.warning(e))
    return
  }
  try {
    await ElMessageBox.confirm('确认出库后档案状态将变为借出，是否继续？', '确认出库', { type: 'warning' })
  } catch {
    return
  }
  try {
    const updated = await checkoutBorrowRequest(detail.value.id, data)
    detail.value = updated
    ElMessage.success('已确认出库，档案状态变为借出。')
    await loadList()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onReturn() {
  if (!detail.value) return
  const data: BorrowReturnData = { returnCheckResult: returnCheckResult.value, returnNote: returnNote.value }
  const errors = validateBorrowReturn(detail.value, data)
  if (errors.length) {
    errors.forEach((e) => ElMessage.warning(e))
    return
  }
  try {
    await ElMessageBox.confirm('确认归还后档案借阅状态恢复可借，是否继续？', '确认归还', { type: 'warning' })
  } catch {
    return
  }
  try {
    const updated = await returnBorrowRequest(detail.value.id, data)
    detail.value = updated
    ElMessage.success('归还检查已记录，档案借阅状态恢复可借。')
    await loadList()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

onMounted(loadList)
</script>

<style scoped>
.borrow-approval { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { color: var(--muted); font-size: 13px; margin: 0 0 16px; }
.metric-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }
.metric { padding: 16px; text-align: center; }
.metric-num { font-size: 26px; font-weight: 700; color: var(--primary); }
.metric-label { font-size: 13px; color: var(--muted); margin-top: 4px; }
.borrow-layout { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 16px; align-items: start; }
.list-col { padding: 10px; max-height: 680px; overflow-y: auto; }
.tabs { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 10px; }
.tab { padding: 6px 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: #fff; cursor: pointer; font-size: 13px; }
.tab.active { color: var(--primary-strong); border-color: #8abcbf; background: var(--primary-soft); }
.request-list { display: grid; gap: 8px; }
.request-card { display: grid; gap: 5px; width: 100%; padding: 12px; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; text-align: left; cursor: pointer; }
.request-card:hover { background: #fafafa; }
.request-card.active { border-color: #8abcbf; background: #f2f8f8; }
.detail-area { display: grid; gap: 16px; max-height: calc(100vh - 200px); overflow-y: auto; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 8px; }
.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 10px; }
.field { display: grid; gap: 4px; }
.field label { font-size: 12px; color: var(--muted); font-weight: 700; }
.field input, .field textarea, .field select { padding: 6px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 13px; }
.field input:disabled, .field textarea:disabled { background: var(--bg); color: var(--muted); }
.check-list { display: grid; gap: 8px; }
.check-list div { display: flex; justify-content: space-between; gap: 12px; padding: 9px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: #fff; font-size: 13px; }
.check-list span { color: var(--muted); }
.grid.two { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.actions { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 12px; }
.button { padding: 6px 14px; border: 1px solid var(--primary); border-radius: var(--radius-sm); background: var(--primary); color: #fff; cursor: pointer; font-size: 13px; }
.button:disabled { opacity: 0.5; cursor: not-allowed; }
.button.secondary { background: #fff; color: var(--primary); }
.button.ghost { background: #fff; color: var(--text); border-color: var(--border); }
.button.danger { background: var(--danger); border-color: var(--danger); }
.notice { padding: 9px 12px; border-radius: var(--radius-sm); font-size: 12px; margin-bottom: 8px; }
.notice.warning { background: var(--warning-soft); color: #7a4c12; }
.empty { display: flex; align-items: center; justify-content: center; height: 200px; color: var(--muted); font-size: 14px; }
.link { background: none; border: none; color: var(--primary); cursor: pointer; font-size: 13px; text-decoration: underline; padding: 0; }
.muted { color: var(--muted); font-size: 12px; }
.mono { font-family: monospace; font-size: 12px; color: var(--muted); }
.status { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.status.success { background: #f6ffed; color: #52c41a; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.danger { background: #fff1f0; color: #f5222d; }
.status.info { background: #e6f7ff; color: #1890ff; }
@media (max-width: 1080px) { .metric-row, .grid.two { grid-template-columns: 1fr; } .borrow-layout { grid-template-columns: 1fr; } }
</style>
