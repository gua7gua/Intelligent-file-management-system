<template>
  <div class="destruction">
    <section>
      <h1 class="page-title">档案销毁</h1>
      <p class="page-subtitle">
        检查清册快照后提交馆领导审批，审批通过后填写销毁方式、两名监销人并上传现场照片，最终确认销毁（不可逆）。
      </p>
    </section>

    <section class="metric-row">
      <div class="metric card"><div class="metric-num">{{ countByStatus('draft') }}</div><div class="metric-label">待提交</div></div>
      <div class="metric card"><div class="metric-num">{{ countByStatus('pending_approval') }}</div><div class="metric-label">待审批</div></div>
      <div class="metric card"><div class="metric-num">{{ countByStatus('pending_destroy') }}</div><div class="metric-label">待销毁</div></div>
      <div class="metric card"><div class="metric-num">{{ countByStatus('destroyed') }}</div><div class="metric-label">已销毁</div></div>
    </section>

    <section class="toolbar">
      <div class="tabs">
        <button class="button ghost" :class="{ 'button-active': filterStatus === '' }" @click="setFilter('')">全部</button>
        <button class="button ghost" :class="{ 'button-active': filterStatus === 'draft' }" @click="setFilter('draft')">待提交</button>
        <button class="button ghost" :class="{ 'button-active': filterStatus === 'pending_approval' }" @click="setFilter('pending_approval')">待审批</button>
        <button class="button ghost" :class="{ 'button-active': filterStatus === 'pending_destroy' }" @click="setFilter('pending_destroy')">待销毁</button>
        <button class="button ghost" :class="{ 'button-active': filterStatus === 'destroyed' }" @click="setFilter('destroyed')">已销毁</button>
      </div>
    </section>

    <section class="destruction-layout">
      <aside class="card panel list-col">
        <div v-if="listLoading" class="detail-empty">加载中...</div>
        <div v-else-if="loadError" class="detail-empty">加载失败：<button class="link" @click="loadLists">重试</button></div>
        <div v-else-if="lists.length === 0" class="detail-empty">暂无销毁清册</div>
        <div
          v-for="l in lists"
          :key="l.id"
          class="list-card"
          :class="{ 'row-active': selectedListId === l.id }"
          @click="selectList(l.id)"
        >
          <div class="batch-head">
            <strong>{{ l.listName }}</strong>
            <span class="status" :class="statusClass(l.status)">{{ DestructionListStatusLabel[l.status] }}</span>
          </div>
          <div class="batch-meta">
            <span>{{ l.listNo }}</span>
            <span>来源 {{ l.appraisalBatchNo || '—' }}</span>
            <span>{{ l.itemCount }} 件</span>
          </div>
        </div>
      </aside>

      <section class="card panel detail">
        <template v-if="!selectedListId"><div class="detail-empty">← 点击左侧清册查看快照与操作</div></template>
        <template v-else-if="detailLoading"><div class="detail-empty">加载中...</div></template>
        <template v-else-if="!listDetail"><div class="detail-empty">清册加载失败：<button class="link" @click="selectList(selectedListId)">重试</button></div></template>
        <template v-else>
          <h2 class="section-title">{{ listDetail.listName }}（{{ listDetail.listNo }}）</h2>
          <div class="detail-kv"><span>来源批次</span><strong>{{ listDetail.appraisalBatchNo || '—' }}</strong></div>
          <div class="detail-kv"><span>状态</span><strong>
            <span class="status" :class="statusClass(listDetail.status)">{{ DestructionListStatusLabel[listDetail.status] }}</span>
          </strong></div>

          <h3 class="section-title" style="margin-top:12px">清册快照</h3>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>档号快照</th><th>题名快照</th><th>分类</th><th>保管期限</th><th>密级</th><th>鉴定意见</th><th>物理文件处理</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="it in listDetail.items" :key="it.id">
                  <td>{{ it.archiveNoSnapshot }}</td>
                  <td>{{ it.titleSnapshot }}</td>
                  <td>{{ it.categorySnapshot }}</td>
                  <td>{{ it.retentionSnapshot }}</td>
                  <td>{{ SecurityLevelLabel[it.securityLevelSnapshot] ?? it.securityLevelSnapshot }}</td>
                  <td>{{ it.appraisalOpinionSnapshot }}</td>
                  <td>
                    <span class="status" :class="it.fileDeleteStatus === 'deleted' ? 'info' : it.fileDeleteStatus === 'failed' ? 'danger' : 'warning'">
                      {{ FileDeleteStatusLabel[it.fileDeleteStatus] }}
                    </span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <h3 class="section-title" style="margin-top:12px">证据链（永久保留）</h3>
          <div class="evidence">
            <div class="evidence-node">
              <div class="evidence-title">销毁清册</div>
              <div class="evidence-desc">{{ listDetail.listNo }} · {{ listDetail.itemCount }} 件快照</div>
            </div>
            <div class="evidence-node">
              <div class="evidence-title">审批记录</div>
              <div class="evidence-desc">
                <template v-if="listDetail.approval">
                  {{ ApprovalTypeLabel[listDetail.approval.approvalType] }} ·
                  {{ listDetail.approval.status === 'approved' ? '已通过' : listDetail.approval.status === 'rejected' ? '已退回' : '审批中' }}
                  <span v-if="listDetail.approval.approvalOpinion">（{{ listDetail.approval.approvalOpinion }}）</span>
                </template>
                <template v-else>未提交审批</template>
              </div>
            </div>
            <div class="evidence-node">
              <div class="evidence-title">销毁确认</div>
              <div class="evidence-desc">
                <template v-if="listDetail.status === 'destroyed'">
                  {{ DestroyMethodLabel[listDetail.destroyMethod!] }} · {{ listDetail.supervisorName1 }}/{{ listDetail.supervisorName2 }}
                  · {{ listDetail.photos.length }} 张照片 · {{ listDetail.destroyedAt }}
                </template>
                <template v-else>待确认</template>
              </div>
            </div>
          </div>

          <div class="actions" style="margin-top:12px">
            <el-button v-if="listDetail.status === 'draft'" type="primary" @click="handleSubmit">提交审批</el-button>
            <el-button v-if="listDetail.status === 'pending_destroy'" type="danger" @click="confirmVisible = true">确认销毁</el-button>
            <router-link v-if="listDetail.status === 'pending_approval' && isDirector" to="/admin/approval" class="button ghost">前往审批工作台</router-link>
            <span v-else-if="listDetail.status === 'pending_approval'" class="hint">已提交，等待馆领导审批。</span>
            <span v-if="listDetail.status === 'destroyed'" class="hint">该清册已完成销毁，证据链永久保留。</span>
          </div>
        </template>
      </section>
    </section>

    <DestroyConfirmDialog v-model="confirmVisible" :list-id="selectedListId ?? 0" @success="onDestroyed" />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { DestructionList, DestructionListDetail } from '@/types/destruction'
import {
  DestructionListStatusLabel,
  DestroyMethodLabel,
  FileDeleteStatusLabel,
  SecurityLevelLabel,
  ApprovalTypeLabel,
} from '@/types/enums'
import type { DestructionListStatusValue } from '@/types/enums'
import { getDestructionListDetail, getDestructionLists, submitDestructionApproval } from '@/api/destruction'
import DestroyConfirmDialog from './components/DestroyConfirmDialog.vue'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
/** 是否馆领导：审批工作台仅馆领导可进入，避免非馆领导越权跳转 */
const isDirector = computed(() => authStore.roles.includes('director'))

const lists = ref<DestructionList[]>([])
const allLists = ref<DestructionList[]>([])
const filterStatus = ref<'' | DestructionListStatusValue>('')
const listLoading = ref(false)
const loadError = ref(false)

const selectedListId = ref<number | null>(null)
const listDetail = ref<DestructionListDetail | null>(null)
const detailLoading = ref(false)
const confirmVisible = ref(false)

const countByStatus = (s: DestructionListStatusValue) => allLists.value.filter((l) => l.status === s).length

function statusClass(s: string): string {
  const map: Record<string, string> = { draft: 'warning', pending_approval: 'info', pending_destroy: 'danger', destroyed: 'success' }
  return map[s] || ''
}

async function loadLists() {
  listLoading.value = true
  loadError.value = false
  try {
    const all = await getDestructionLists()
    allLists.value = all.records
    lists.value = filterStatus.value ? all.records.filter((l) => l.status === filterStatus.value) : all.records
  } catch {
    loadError.value = true
  } finally {
    listLoading.value = false
  }
}

function setFilter(s: '' | DestructionListStatusValue) {
  filterStatus.value = s
  lists.value = s ? allLists.value.filter((l) => l.status === s) : allLists.value
}

async function selectList(id: number) {
  selectedListId.value = id
  listDetail.value = null
  detailLoading.value = true
  try {
    listDetail.value = await getDestructionListDetail(id)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '清册详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

async function handleSubmit() {
  if (!listDetail.value) return
  try {
    const { value } = await ElMessageBox.prompt('请填写提交审批的说明', '提交销毁审批', {
      inputPattern: /.+/,
      inputErrorMessage: '请填写说明',
      inputValue: '到期鉴定后按制度提交销毁',
    })
    const updated = await submitDestructionApproval(listDetail.value.id, { reason: value })
    // 后端返回的是更新后的清册详情（含 approvalRequestId），与 api 类型签名不一致；这里取审批单号做提示
    const approvalNo = updated?.listNo ? `#${updated.approvalRequestId ?? ''}` : ''
    ElMessage.success(`已生成审批单 ${approvalNo}，清册进入待审批。`)
    await loadLists()
    if (selectedListId.value) await selectList(selectedListId.value)
    if (isDirector.value && updated?.approvalRequestId) {
      router.push({ path: '/admin/approval', query: { focus: String(updated.approvalRequestId) } })
    }
  } catch (e: unknown) {
    if (e !== 'cancel' && e !== 'close') {
      ElMessage.error(e instanceof Error ? e.message : '提交审批失败')
    }
  }
}

async function onDestroyed() {
  await loadLists()
  if (selectedListId.value) await selectList(selectedListId.value)
}

watch(
  () => route.query.focus,
  async (focus) => {
    if (focus) {
      const id = Number(focus)
      if (!Number.isNaN(id)) {
        await loadLists()
        await selectList(id)
      }
    }
  },
  { immediate: true },
)

onMounted(async () => {
  await loadLists()
})
</script>

<style scoped>
.destruction { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { color: var(--muted); font-size: 13px; margin: 0 0 16px; }
.metric-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }
.metric { padding: 16px; text-align: center; }
.metric-num { font-size: 26px; font-weight: 700; color: var(--primary); }
.metric-label { font-size: 13px; color: var(--muted); margin-top: 4px; }
.toolbar { margin-bottom: 12px; }
.tabs { display: flex; gap: 6px; flex-wrap: wrap; }
.button.ghost { padding: 6px 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: #fff; cursor: pointer; font-size: 14px; text-decoration: none; color: var(--text); display: inline-block; }
.button-active { border-color: #8abcbf !important; background: #f2f8f8 !important; }
.destruction-layout { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 16px; align-items: start; }
.list-col { padding: 10px; max-height: 640px; overflow-y: auto; }
.list-card { padding: 10px 12px; border-bottom: 1px solid var(--border); cursor: pointer; }
.list-card:hover { background: #fafafa; }
.batch-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.batch-meta { display: flex; gap: 12px; color: var(--muted); font-size: 12px; margin-top: 4px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 8px; }
.detail-kv { display: grid; grid-template-columns: 96px minmax(0,1fr); gap: 8px; padding: 6px 0; border-bottom: 1px solid var(--border); font-size: 13px; }
.detail-kv span { color: var(--muted); font-weight: 700; }
.table-wrap { overflow-x: auto; margin-top: 8px; }
.table-wrap table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table-wrap th, .table-wrap td { padding: 8px 10px; text-align: left; border-bottom: 1px solid var(--border); }
.table-wrap th { font-weight: 700; color: var(--muted); background: var(--bg); }
.evidence { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.evidence-node { padding: 10px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: #fafafa; }
.evidence-title { font-weight: 700; font-size: 13px; margin-bottom: 4px; }
.evidence-desc { font-size: 12px; color: var(--muted); }
.detail-empty { display: flex; align-items: center; justify-content: center; height: 220px; color: var(--muted); font-size: 14px; }
.row-active { background: #f2f8f8; }
.actions { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
.link { background: none; border: none; color: var(--primary); cursor: pointer; font-size: 13px; text-decoration: underline; padding: 0; }
.hint { color: var(--muted); font-size: 12px; }
.status { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.status.success { background: #f6ffed; color: #52c41a; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.danger { background: #fff1f0; color: #f5222d; }
.status.info { background: #e6f7ff; color: #1890ff; }
.detail { max-height: calc(100vh - 200px); overflow-y: auto; }
@media (max-width: 1100px) { .metric-row { grid-template-columns: repeat(2, 1fr); } .destruction-layout { grid-template-columns: 1fr; } .evidence { grid-template-columns: 1fr; } }
</style>
