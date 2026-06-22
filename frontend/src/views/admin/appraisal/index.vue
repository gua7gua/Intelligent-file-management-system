<template>
  <div class="appraisal">
    <section>
      <h1 class="page-title">档案鉴定</h1>
      <p class="page-subtitle">
        处理保管期限到期或即将到期档案，创建鉴定批次，逐件选择「延长保存」或「待销毁」。完成后系统自动生成销毁清册，最终销毁审批在档案销毁页与审批工作台完成。
      </p>
    </section>

    <!-- 指标 -->
    <section class="metric-row">
      <div class="metric card"><div class="metric-num">{{ metrics.expiring }}</div><div class="metric-label">即将到期档案</div></div>
      <div class="metric card"><div class="metric-num">{{ draftCount }}</div><div class="metric-label">待鉴定批次</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.pendingDestroy }}</div><div class="metric-label">待销毁条目</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.generatedLists }}</div><div class="metric-label">已生成清册</div></div>
    </section>

    <section class="toolbar">
      <div class="tabs">
        <button class="button ghost" :class="{ 'button-active': filterStatus === '' }" @click="setFilter('')">全部批次</button>
        <button class="button ghost" :class="{ 'button-active': filterStatus === 'draft' }" @click="setFilter('draft')">待处理</button>
        <button class="button ghost" :class="{ 'button-active': filterStatus === 'completed' }" @click="setFilter('completed')">已完成</button>
      </div>
      <el-button type="primary" @click="openCreate">创建鉴定批次</el-button>
    </section>

    <section class="appraisal-layout">
      <!-- 左栏：批次列表 -->
      <aside class="card panel batch-list">
        <div v-if="listLoading" class="detail-empty">加载中...</div>
        <div v-else-if="loadError" class="detail-empty">加载失败：<button class="link" @click="loadBatches">重试</button></div>
        <div v-else-if="batches.length === 0" class="detail-empty">暂无鉴定批次</div>
        <div
          v-for="b in batches"
          :key="b.id"
          class="batch-card"
          :class="{ 'row-active': selectedBatchId === b.id }"
          @click="selectBatch(b.id)"
        >
          <div class="batch-head">
            <strong>{{ b.batchName }}</strong>
            <span class="status" :class="b.status === 'completed' ? 'success' : 'warning'">
              {{ AppraisalBatchStatusLabel[b.status] }}
            </span>
            <el-button v-if="b.status === 'draft'" size="small" type="danger" plain @click.stop="onDeleteBatch(b)">删除</el-button>
          </div>
          <div class="batch-meta">
            <span>{{ b.batchNo }}</span>
            <span>{{ b.categoryName || '全部分类' }}</span>
            <span>{{ b.dueDays != null ? `到期窗口 ${b.dueDays} 天` : (b.formedYearStart ? `${b.formedYearStart}${b.formedYearEnd && b.formedYearEnd !== b.formedYearStart ? '-' + b.formedYearEnd : ''} 年度` : '—') }}</span>
          </div>
          <div class="batch-meta">
            <span>命中 {{ b.hitCount }} 件</span>
            <span v-if="b.status === 'completed' && b.generatedListNo" class="link" @click.stop="goDestruction(b.generatedListId!)">→ {{ b.generatedListNo }}</span>
          </div>
        </div>
        <div style="display: flex; justify-content: flex-end; margin-top: 12px">
          <el-pagination
            v-model:current-page="pageNo"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadBatches"
            @current-change="loadBatches"
          />
        </div>
      </aside>

      <!-- 右栏：明细 -->
      <section class="card panel detail">
        <template v-if="!selectedBatchId">
          <div class="detail-empty">← 点击左侧批次查看命中档案明细</div>
        </template>
        <template v-else-if="detailLoading">
          <div class="detail-empty">加载中...</div>
        </template>
        <template v-else-if="!batchDetail">
          <div class="detail-empty">批次明细加载失败：<button class="link" @click="selectBatch(selectedBatchId)">重试</button></div>
        </template>
        <template v-else>
          <h2 class="section-title">{{ batchDetail.batchName }}（{{ batchDetail.batchNo }}）</h2>
          <div class="detail-kv"><span>分类范围</span><strong>{{ batchDetail.categoryName || '全部分类' }}</strong></div>
          <div class="detail-kv"><span>鉴定范围</span><strong>{{ batchDetail.dueDays != null ? `保管期限到期 ≤ 今天 + ${batchDetail.dueDays} 天（含已过期）` : (batchDetail.formedYearStart ? `${batchDetail.formedYearStart}–${batchDetail.formedYearEnd ?? batchDetail.formedYearStart} 年度` : '—') }}</strong></div>
          <div class="detail-kv"><span>命中</span><strong>{{ batchDetail.hitCount }} 件（待销毁 {{ batchDetail.destroyCount ?? 0 }} / 延长 {{ batchDetail.extendCount ?? 0 }}）</strong></div>

          <h3 class="section-title" style="margin-top:12px">鉴定明细</h3>
          <div class="table-wrap">
            <table>
              <colgroup>
                <col class="col-archive-no" />
                <col class="col-title" />
                <col class="col-period-orig" />
                <col class="col-until" />
                <col class="col-result" />
                <col class="col-new-period" />
                <col class="col-opinion" />
              </colgroup>
              <thead>
                <tr>
                  <th>档号</th>
                  <th>题名</th>
                  <th>原期限</th>
                  <th>到期日</th>
                  <th>鉴定结论</th>
                  <th>新期限/到期日</th>
                  <th>鉴定意见</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="it in batchDetail.items" :key="it.archiveId">
                  <td>{{ it.archiveNo }}</td>
                  <td>{{ it.title }}</td>
                  <td>{{ RetentionPeriodLabel[it.retentionPeriod] }}</td>
                  <td>{{ it.retentionUntil }}</td>
                  <td>
                    <label class="radio-inline"><input type="radio" :name="'r' + it.archiveId" value="extend" :disabled="batchDetail.status !== 'draft'" v-model="it.appraisalResult" />延长保存</label>
                    <label class="radio-inline"><input type="radio" :name="'r' + it.archiveId" value="destroy" :disabled="batchDetail.status !== 'draft'" v-model="it.appraisalResult" />待销毁</label>
                  </td>
                  <td>
                    <template v-if="it.appraisalResult === 'extend'">
                      <div class="period-inputs">
                        <select v-model="it.newRetentionPeriod" :disabled="batchDetail.status !== 'draft'">
                          <option value="">选择期限</option>
                          <option v-for="(label, val) in RetentionPeriodLabel" :key="val" :value="val">{{ label }}</option>
                        </select>
                        <input type="date" v-model="it.newRetentionUntil" :disabled="batchDetail.status !== 'draft'" />
                      </div>
                    </template>
                    <span v-else-if="it.appraisalResult === 'destroy'" class="status danger">待销毁</span>
                    <span v-else class="hint">—</span>
                  </td>
                  <td>
                    <textarea v-model="it.opinion" rows="1" :disabled="batchDetail.status !== 'draft'" placeholder="鉴定意见"></textarea>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <div v-if="batchDetail.status === 'draft'" class="actions">
            <el-button type="primary" @click="handleSave">保存鉴定明细</el-button>
            <el-button @click="handleComplete">完成鉴定</el-button>
          </div>
          <div v-else class="notice success">
            该批次已完成鉴定，系统已自动生成销毁清册
            <strong v-if="batchDetail.generatedListNo">{{ batchDetail.generatedListNo }}</strong>。
            <button class="link" @click="goDestruction(batchDetail.generatedListId!)">查看销毁清册 →</button>
          </div>
        </template>
      </section>
    </section>

    <!-- 创建批次弹窗 -->
    <el-dialog v-model="createVisible" title="创建鉴定批次" width="480px">
      <div class="field">
        <label>批次名称</label>
        <input v-model="createForm.batchName" placeholder="如 2015 年会计档案鉴定批次" />
      </div>
      <div class="field">
        <label>分类范围</label>
        <select v-model.number="createForm.categoryId">
          <option :value="0">全部分类</option>
          <option v-for="cat in categoryTree.filter(c => c.id > 0)" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
        </select>
      </div>
      <div class="field">
        <label>到期窗口（天）</label>
        <input v-model.number="createForm.dueDays" type="number" min="0" max="3650" placeholder="如 365 表示未来一年内到期" />
        <small class="hint">命中保管期限到期日 ≤ 今天 + 该天数的档案（含已过期未处理）；填 0 表示只圈已过期。</small>
      </div>
      <template #footer>
        <el-button @click="createVisible = false">取消</el-button>
        <el-button type="primary" @click="handleCreate">创建批次</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { AppraisalBatch, AppraisalBatchDetail } from '@/types/appraisal'
import { AppraisalBatchStatusLabel, RetentionPeriodLabel } from '@/types/enums'
import {
  completeAppraisalBatch,
  createAppraisalBatch,
  deleteAppraisalBatch,
  getAppraisalBatchDetail,
  getAppraisalBatches,
  getAppraisalStats,
  saveAppraisalItems,
} from '@/api/appraisal'
import { validateAppraisalCompletion } from '@/utils/appraisalValidation'

const router = useRouter()

const categoryTree = [
  { id: 1, name: '文书档案' },
  { id: 2, name: '科技档案' },
  { id: 3, name: '会计档案' },
  { id: 4, name: '音像档案' },
  { id: 5, name: '人事档案' },
]

// R3-C5：顶部指标接真实统计接口（/admin/appraisal-batches/stats），不再硬编码假值
const metrics = reactive({ expiring: 0, pendingDestroy: 0, generatedLists: 0 })

const batches = ref<AppraisalBatch[]>([])
const filterStatus = ref<'' | 'draft' | 'completed'>('')
const listLoading = ref(false)
const loadError = ref(false)
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

const selectedBatchId = ref<number | null>(null)
const batchDetail = ref<AppraisalBatchDetail | null>(null)
const detailLoading = ref(false)

const draftCount = computed(() => batches.value.filter((b) => b.status === 'draft').length)

const createVisible = ref(false)
const createForm = reactive({ batchName: '', categoryId: 0, dueDays: 365 as number })

async function loadBatches() {
  listLoading.value = true
  loadError.value = false
  try {
    const res = await getAppraisalBatches({
      ...(filterStatus.value ? { status: filterStatus.value } : {}),
      pageNo: pageNo.value,
      pageSize: pageSize.value,
    })
    batches.value = res.records
    total.value = res.total
  } catch {
    loadError.value = true
  } finally {
    listLoading.value = false
  }
}

function setFilter(s: '' | 'draft' | 'completed') {
  filterStatus.value = s
  pageNo.value = 1
  loadBatches()
}

async function selectBatch(id: number) {
  selectedBatchId.value = id
  batchDetail.value = null
  detailLoading.value = true
  try {
    batchDetail.value = await getAppraisalBatchDetail(id)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '批次详情加载失败')
  } finally {
    detailLoading.value = false
  }
}

function openCreate() {
  createForm.batchName = ''
  createForm.categoryId = 0
  createForm.dueDays = 365
  createVisible.value = true
}

async function handleCreate() {
  if (!createForm.batchName.trim()) {
    ElMessage.warning('请填写批次名称。')
    return
  }
  if (createForm.dueDays == null || createForm.dueDays < 0) {
    ElMessage.warning('请填写有效的到期窗口天数（0 或正整数）。')
    return
  }
  try {
    const detail = await createAppraisalBatch({
      batchName: createForm.batchName.trim(),
      categoryId: createForm.categoryId || undefined,
      dueDays: createForm.dueDays,
    })
    createVisible.value = false
    ElMessage.success('鉴定批次已创建，已拉入命中档案。')
    await loadBatches()
    await selectBatch(detail.id)
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  }
}

async function handleSave() {
  if (!batchDetail.value) return
  try {
    const updated = await saveAppraisalItems(batchDetail.value.id, {
      items: batchDetail.value.items
        .filter((i) => i.appraisalResult === 'extend' || i.appraisalResult === 'destroy')
        .map((i) => ({
          archiveId: i.archiveId,
          appraisalResult: i.appraisalResult as 'extend' | 'destroy',
          newRetentionPeriod: i.appraisalResult === 'extend' ? (i.newRetentionPeriod || null) : null,
          newRetentionUntil: i.appraisalResult === 'extend' ? (i.newRetentionUntil || null) : null,
          opinion: i.opinion,
        })),
    })
    batchDetail.value = updated
    ElMessage.success('鉴定明细已保存。')
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '保存失败')
  }
}

async function handleComplete() {
  if (!batchDetail.value) return
  const errors = validateAppraisalCompletion(batchDetail.value.items)
  if (errors.length > 0) {
    ElMessage.warning(errors[0])
    return
  }
  await handleSave()
  if (!batchDetail.value) return
  try {
    const completed = await completeAppraisalBatch(batchDetail.value.id)
    batchDetail.value = completed
    ElMessage.success(`鉴定已完成，已生成销毁清册 ${completed.generatedListNo ?? ''}。`)
    await loadBatches()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '完成鉴定失败')
  }
}

async function onDeleteBatch(b: { id: number; batchName: string }) {
  try {
    await ElMessageBox.confirm(`删除批次「${b.batchName}」？仅未完成（draft）批次可删，是否继续？`, '删除鉴定批次', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteAppraisalBatch(b.id)
    ElMessage.success('鉴定批次已删除。')
    if (selectedBatchId.value === b.id) {
      selectedBatchId.value = null
      batchDetail.value = null
    }
    await loadBatches()
  } catch (e: unknown) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}

function goDestruction(listId: number) {
  router.push({ path: '/admin/destruction', query: { focus: String(listId) } })
}

async function loadStats() {
  try {
    const s = await getAppraisalStats()
    metrics.expiring = s.expiringCount
    metrics.pendingDestroy = s.pendingDestructionCount
    metrics.generatedLists = s.generatedListCount
  } catch {
    // 静默：统计加载失败不阻断主流程，保留 0
  }
}

onMounted(() => {
  loadBatches()
  loadStats()
})
</script>

<style scoped>
.appraisal { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { color: var(--muted); font-size: 13px; margin: 0 0 16px; }

.metric-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }
.metric { padding: 16px; text-align: center; }
.metric-num { font-size: 26px; font-weight: 700; color: var(--primary); }
.metric-label { font-size: 13px; color: var(--muted); margin-top: 4px; }

.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.tabs { display: flex; gap: 6px; }
.button.ghost { padding: 6px 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: #fff; cursor: pointer; font-size: 14px; }
.button-active { border-color: #8abcbf !important; background: #f2f8f8 !important; }

.appraisal-layout { display: grid; grid-template-columns: 340px minmax(0, 1fr); gap: 16px; align-items: start; }
.batch-list { padding: 10px; max-height: 640px; overflow-y: auto; }
.detail { max-height: calc(100vh - 200px); overflow-y: auto; }
.batch-card { padding: 10px 12px; border-bottom: 1px solid var(--border); cursor: pointer; }
.batch-card:hover { background: #fafafa; }
.batch-head { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.batch-meta { display: flex; gap: 12px; color: var(--muted); font-size: 12px; margin-top: 4px; }

.section-title { font-size: 15px; font-weight: 700; margin: 0 0 8px; }
.detail-kv { display: grid; grid-template-columns: 96px minmax(0,1fr); gap: 8px; padding: 6px 0; border-bottom: 1px solid var(--border); font-size: 13px; }
.detail-kv span { color: var(--muted); font-weight: 700; }

.table-wrap { overflow-x: auto; margin-top: 8px; }
.table-wrap table { width: 100%; border-collapse: collapse; font-size: 13px; table-layout: fixed; }
.col-archive-no { width: 12%; }
.col-title { width: 20%; }
.col-period-orig { width: 8%; }
.col-until { width: 10%; }
.col-result { width: 16%; }
.col-new-period { width: 22%; }
.col-opinion { width: 12%; }
.table-wrap th, .table-wrap td { padding: 8px 10px; text-align: left; border-bottom: 1px solid var(--border); vertical-align: middle; word-break: break-word; }
.table-wrap th { font-weight: 700; color: var(--muted); background: var(--bg); }
.table-wrap input, .table-wrap select, .table-wrap textarea { width: 100%; min-height: 30px; padding: 4px 6px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 13px; }
.radio-inline { display: inline-flex; align-items: center; gap: 4px; margin-right: 12px; font-size: 13px; cursor: pointer; }
.radio-inline input[type="radio"] { width: auto; min-height: auto; margin: 0; }
.period-inputs { display: flex; gap: 6px; }
.period-inputs select, .period-inputs input { flex: 1; min-width: 0; width: auto; }

.detail-empty { display: flex; align-items: center; justify-content: center; height: 220px; color: var(--muted); font-size: 14px; }
.row-active { background: #f2f8f8; }
.actions { display: flex; gap: 8px; margin-top: 12px; flex-wrap: wrap; }
.link { background: none; border: none; color: var(--primary); cursor: pointer; font-size: 13px; text-decoration: underline; padding: 0; }
.hint { color: var(--muted); font-size: 12px; }
.status { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.status.success { background: #f6ffed; color: #52c41a; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.danger { background: #fff1f0; color: #f5222d; }
.notice.success { padding: 10px 12px; border-radius: var(--radius-sm); background: #f6ffed; color: #389e0d; font-size: 13px; margin-top: 12px; }

.field { margin-bottom: 10px; }
.field label { display: block; font-size: 13px; font-weight: 700; color: var(--muted); margin-bottom: 4px; }
.field input, .field select { width: 100%; min-height: 34px; padding: 6px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 14px; }
.split { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }

@media (max-width: 1100px) {
  .metric-row { grid-template-columns: repeat(2, 1fr); }
  .appraisal-layout { grid-template-columns: 1fr; }
}
</style>
