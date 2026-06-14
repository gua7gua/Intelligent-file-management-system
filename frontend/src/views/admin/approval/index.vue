<template>
  <div class="approval">
    <section>
      <h1 class="page-title">审批工作台</h1>
      <p class="page-subtitle">
        馆领导集中处理密级调整、开放调整和销毁清册审批。查看目标档案、凭证档案或清册快照，填写审批意见后通过或退回。
      </p>
    </section>

    <section class="metric-row">
      <div class="metric card"><div class="metric-num">{{ countByStatus('pending') }}</div><div class="metric-label">待审批</div></div>
      <div class="metric card"><div class="metric-num">{{ todayProcessed }}</div><div class="metric-label">今日已处理</div></div>
      <div class="metric card"><div class="metric-num">{{ countByType('destruction') }}</div><div class="metric-label">销毁审批</div></div>
      <div class="metric card"><div class="metric-num">{{ countByStatus('rejected') }}</div><div class="metric-label">退回补正</div></div>
    </section>

    <section class="toolbar">
      <div class="tabs">
        <button class="button ghost" :class="{ 'button-active': filterType === '' }" @click="setFilter('')">全部</button>
        <button class="button ghost" :class="{ 'button-active': filterType === 'security_adjust' }" @click="setFilter('security_adjust')">密级调整</button>
        <button class="button ghost" :class="{ 'button-active': filterType === 'open_adjust' }" @click="setFilter('open_adjust')">开放调整</button>
        <button class="button ghost" :class="{ 'button-active': filterType === 'destruction' }" @click="setFilter('destruction')">销毁审批</button>
      </div>
    </section>

    <section class="approval-layout">
      <aside class="card panel queue">
        <div v-if="listLoading" class="detail-empty">加载中...</div>
        <div v-else-if="loadError" class="detail-empty">加载失败：<button class="link" @click="loadApprovals">重试</button></div>
        <div v-else-if="approvals.length === 0" class="detail-empty">暂无审批单</div>
        <div
          v-for="a in approvals"
          :key="a.id"
          class="queue-card"
          :class="{ 'row-active': selectedId === a.id }"
          @click="selectApproval(a.id)"
        >
          <div class="batch-head">
            <span class="tag" :class="typeClass(a.approvalType)">{{ ApprovalTypeLabel[a.approvalType] }}</span>
            <span class="status" :class="a.status === 'approved' ? 'success' : a.status === 'rejected' ? 'danger' : 'warning'">
              {{ statusLabel(a.status) }}
            </span>
          </div>
          <div class="queue-title">{{ a.targetArchiveTitle || a.targetListName || '审批单 #' + a.id }}</div>
          <div class="batch-meta">
            <span>{{ a.submittedByName || '提交人' }}</span>
            <span>{{ a.submittedAt.slice(0, 16).replace('T', ' ') }}</span>
          </div>
        </div>
      </aside>

      <section class="card panel detail">
        <template v-if="!selectedId"><div class="detail-empty">← 点击左侧审批单查看详情</div></template>
        <template v-else-if="detailLoading"><div class="detail-empty">加载中...</div></template>
        <template v-else-if="!detail"><div class="detail-empty">审批详情加载失败：<button class="link" @click="selectApproval(selectedId)">重试</button></div></template>
        <template v-else>
          <h2 class="section-title">{{ ApprovalTypeLabel[detail.approvalType] }} · {{ detail.targetArchiveTitle || detail.targetListName || '#' + detail.id }}</h2>
          <div class="detail-kv"><span>申请理由</span><strong>{{ detail.reason }}</strong></div>
          <div class="detail-kv"><span>提交人/时间</span><strong>{{ detail.submittedByName }} · {{ detail.submittedAt.slice(0, 16).replace('T', ' ') }}</strong></div>
          <div v-if="detail.approvalOpinion && detail.status !== 'pending'" class="detail-kv">
            <span>审批意见</span><strong>{{ detail.approvalOpinion }}（{{ detail.approvedByName }}）</strong>
          </div>

          <!-- 按类型切换详情子组件 -->
          <h3 class="section-title" style="margin-top:12px">审批证据</h3>
          <ArchiveAdjustDetail
            v-if="detail.approvalType === 'security_adjust' || detail.approvalType === 'open_adjust'"
            :detail="detail"
          />
          <DestructionApprovalDetail v-else-if="detail.approvalType === 'destruction'" :detail="detail" />

          <!-- 审批意见 + 操作 -->
          <template v-if="detail.status === 'pending'">
            <h3 class="section-title" style="margin-top:12px">审批意见</h3>
            <textarea v-model="opinion" rows="3" placeholder="填写审批意见（通过或退回均必填）"></textarea>
            <div class="actions" style="margin-top:8px">
              <el-button type="primary" :disabled="!canApprove" @click="handleApprove">审批通过</el-button>
              <el-button type="danger" @click="handleReject">退回补正</el-button>
            </div>
            <p v-if="!canApprove && (detail.approvalType !== 'destruction')" class="hint">凭证异常，不可通过，仅可退回补正。</p>
          </template>
          <div v-else class="notice" :class="detail.status === 'approved' ? 'success' : 'danger'">
            该审批单已{{ detail.status === 'approved' ? '通过' : '退回' }}，目标对象已相应更新。
          </div>
        </template>
      </section>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { ApprovalRequest, ApprovalRequestDetail } from '@/types/approval'
import { ApprovalTypeLabel } from '@/types/enums'
import type { ApprovalStatusValue, ApprovalTypeValue } from '@/types/enums'
import { approveApproval, getApprovalDetail, getApprovals, rejectApproval } from '@/api/approval'
import ArchiveAdjustDetail from './components/ArchiveAdjustDetail.vue'
import DestructionApprovalDetail from './components/DestructionApprovalDetail.vue'

const route = useRoute()

const approvals = ref<ApprovalRequest[]>([])
const allApprovals = ref<ApprovalRequest[]>([])
const filterType = ref<'' | ApprovalTypeValue>('')
const listLoading = ref(false)
const loadError = ref(false)
const todayProcessed = ref(3)

const selectedId = ref<number | null>(null)
const detail = ref<ApprovalRequestDetail | null>(null)
const detailLoading = ref(false)
const opinion = ref('')

const countByStatus = (s: ApprovalStatusValue) => allApprovals.value.filter((a) => a.status === s).length
const countByType = (t: ApprovalTypeValue) => allApprovals.value.filter((a) => a.approvalType === t).length

const canApprove = computed(() => {
  if (!detail.value || detail.value.status !== 'pending') return false
  if (detail.value.approvalType === 'destruction') return true
  return !!detail.value.evidenceArchive && !!detail.value.evidenceMatched
})

function statusLabel(s: ApprovalStatusValue): string {
  return s === 'approved' ? '已通过' : s === 'rejected' ? '已退回' : '待审批'
}
function typeClass(t: ApprovalTypeValue): string {
  return t === 'destruction' ? 'danger' : t === 'open_adjust' ? 'info' : 'warning'
}

async function loadApprovals() {
  listLoading.value = true
  loadError.value = false
  try {
    const all = await getApprovals()
    allApprovals.value = all.records
    approvals.value = filterType.value ? all.records.filter((a) => a.approvalType === filterType.value) : all.records
  } catch {
    loadError.value = true
  } finally {
    listLoading.value = false
  }
}

function setFilter(t: '' | ApprovalTypeValue) {
  filterType.value = t
  approvals.value = t ? allApprovals.value.filter((a) => a.approvalType === t) : allApprovals.value
}

async function selectApproval(id: number) {
  selectedId.value = id
  detail.value = null
  opinion.value = ''
  detailLoading.value = true
  try {
    detail.value = await getApprovalDetail(id)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '审批详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function handleApprove() {
  if (!detail.value || !opinion.value.trim()) {
    ElMessage.warning('请填写审批意见。')
    return
  }
  try {
    const updated = await approveApproval(detail.value.id, { opinion: opinion.value.trim() })
    detail.value = updated
    ElMessage.success('审批已通过。密级/开放调整字段已生效，销毁清册已进入待销毁。')
    await loadApprovals()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '审批失败')
  }
}

async function handleReject() {
  if (!detail.value || !opinion.value.trim()) {
    ElMessage.warning('请填写退回意见。')
    return
  }
  try {
    const updated = await rejectApproval(detail.value.id, { opinion: opinion.value.trim() })
    detail.value = updated
    ElMessage.success('审批已退回，目标对象保持不变。')
    await loadApprovals()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '退回失败')
  }
}

watch(
  () => route.query.focus,
  async (focus) => {
    if (focus) {
      const id = Number(focus)
      if (!Number.isNaN(id)) {
        await loadApprovals()
        await selectApproval(id)
      }
    }
  },
  { immediate: true },
)

onMounted(async () => {
  await loadApprovals()
})
</script>

<style scoped>
.approval { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { color: var(--muted); font-size: 13px; margin: 0 0 16px; }
.metric-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }
.metric { padding: 16px; text-align: center; }
.metric-num { font-size: 26px; font-weight: 700; color: var(--primary); }
.metric-label { font-size: 13px; color: var(--muted); margin-top: 4px; }
.toolbar { margin-bottom: 12px; }
.tabs { display: flex; gap: 6px; flex-wrap: wrap; }
.button.ghost { padding: 6px 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: #fff; cursor: pointer; font-size: 14px; }
.button-active { border-color: #8abcbf !important; background: #f2f8f8 !important; }
.approval-layout { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 16px; align-items: start; }
.queue { padding: 10px; max-height: 640px; overflow-y: auto; }
.queue-card { padding: 10px 12px; border-bottom: 1px solid var(--border); cursor: pointer; }
.queue-card:hover { background: #fafafa; }
.batch-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; margin-bottom: 4px; }
.queue-title { font-weight: 700; font-size: 13px; }
.batch-meta { display: flex; gap: 12px; color: var(--muted); font-size: 12px; margin-top: 4px; }
.tag { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.tag.warning { background: #fff7e6; color: #fa8c16; }
.tag.info { background: #e6f7ff; color: #1890ff; }
.tag.danger { background: #fff1f0; color: #f5222d; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 8px; }
.detail-kv { display: grid; grid-template-columns: 96px minmax(0,1fr); gap: 8px; padding: 6px 0; border-bottom: 1px solid var(--border); font-size: 13px; }
.detail-kv span { color: var(--muted); font-weight: 700; }
textarea { width: 100%; min-height: 60px; padding: 6px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 14px; resize: vertical; }
.detail-empty { display: flex; align-items: center; justify-content: center; height: 220px; color: var(--muted); font-size: 14px; }
.row-active { background: #f2f8f8; }
.actions { display: flex; gap: 8px; flex-wrap: wrap; }
.link { background: none; border: none; color: var(--primary); cursor: pointer; font-size: 13px; text-decoration: underline; padding: 0; }
.hint { color: var(--muted); font-size: 12px; margin-top: 6px; }
.status { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.status.success { background: #f6ffed; color: #52c41a; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.danger { background: #fff1f0; color: #f5222d; }
.notice { padding: 10px 12px; border-radius: var(--radius-sm); font-size: 13px; margin-top: 12px; }
.notice.success { background: #f6ffed; color: #389e0d; }
.notice.danger { background: #fff1f0; color: #a8071a; }
@media (max-width: 1100px) { .metric-row { grid-template-columns: repeat(2, 1fr); } .approval-layout { grid-template-columns: 1fr; } }
</style>
