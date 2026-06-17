<template>
  <div class="pending-archive">
    <!-- 顶部工具栏 -->
    <div class="toolbar">
      <div>
        <span class="hint">AI 补全：{{ aiStatusLabel }}</span>
      </div>
      <el-button
        type="primary"
        :loading="aiLoading"
        :disabled="!activeBatch || aiLoading || activeBatch?.aiStatus === 'running'"
        @click="handleRunAi"
      >
        AI 整批补全
      </el-button>
    </div>

    <section class="work-layout">
      <!-- 左栏：待处理批次 + 已入库待上架 -->
      <div class="left-col">
        <div class="card panel" style="margin:0">
          <h2 class="section-title">待处理批次</h2>
          <div v-if="batchLoading" class="panel-scroll detail-empty">加载中...</div>
          <div v-else-if="batches.length === 0" class="panel-scroll detail-empty">暂无待入库批次</div>
          <div v-else class="panel-scroll">
            <button
              v-for="b in batches"
              :key="b.id"
              class="batch-card"
              :class="{ active: activeBatch?.id === b.id }"
              type="button"
              @click="selectBatch(b)"
            >
              <strong>{{ b.title }}</strong>
              <span class="batch-no">{{ b.batchNo }}</span>
              <span>接收 {{ b.acceptedCount }} / 回退 {{ b.returnedCount }}</span>
              <span class="hint">AI：{{ aiLabel(b.aiStatus) }}</span>
            </button>
          </div>
        </div>

        <div class="card panel" style="margin:0">
          <h2 class="section-title">已入库待上架</h2>
          <div v-if="shelvableLoading" class="panel-scroll detail-empty">加载中...</div>
          <div v-else-if="shelvableBatches.length === 0" class="panel-scroll detail-empty">暂无待上架批次</div>
          <div v-else class="panel-scroll">
            <div
              v-for="b in shelvableBatches"
              :key="b.id"
              class="batch-card shelvable-card"
            >
              <strong>{{ b.title }}</strong>
              <span class="batch-no">{{ b.batchNo }}</span>
              <span>待上架 {{ b.pendingShelfCount ?? 0 }} 件</span>
              <el-button
                size="small"
                type="primary"
                :loading="shelvingId === b.id"
                @click="handleShelveBatch(b)"
              >
                确认上架
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 中栏：已接收条目 -->
      <div class="card panel" style="margin:0">
        <h2 class="section-title">已接收条目</h2>
        <div v-if="!activeBatch" class="panel-scroll detail-empty">← 选择左侧批次查看条目</div>
        <div v-else-if="items.length === 0" class="panel-scroll detail-empty">该批次暂无已接收条目</div>
        <div v-else class="panel-scroll">
          <button
            v-for="it in items"
            :key="it.id"
            class="item-card"
            :class="{ active: activeItem?.id === it.id }"
            type="button"
            @click="selectItem(it)"
          >
            <span class="item-title">{{ it.inputTitle }}</span>
            <span class="item-meta">{{ carrierLabel(it.carrierStatus) }}</span>
            <span class="status" :class="statusClass(it.itemStatus)">
              {{ statusText(it.itemStatus) }}
            </span>
          </button>
        </div>
      </div>

      <!-- 右栏：条目详情表单 -->
      <div class="card panel" style="margin:0">
        <h2 class="section-title">{{ activeItem ? activeItem.inputTitle : '条目详情' }}</h2>
        <div v-if="!activeItem" class="panel-scroll detail-empty">← 选择左侧条目查看和编辑详情</div>
        <div v-else class="panel-scroll">
          <!-- 受保护字段 -->
          <div class="protect-row">
            <span>密级：{{ securityLabel(activeItem.securityLevel) }}</span>
            <span>保管期限：{{ retentionLabel(activeItem.retentionPeriod) }}</span>
            <span>开放：{{ activeItem.openStatus === 'open' ? '公开' : '不公开' }}</span>
            <span>允许数字化：{{ activeItem.allowDigitization ? '是' : '否' }}</span>
            <span v-if="activeItem.archiveNo">档号：{{ activeItem.archiveNo }}</span>
            <span style="margin-left:auto">
              <span class="status" :class="statusClass(activeItem.itemStatus)">
                {{ statusText(activeItem.itemStatus) }}
              </span>
              {{ activeItem.matchStatus }}
            </span>
          </div>

          <!-- 可编辑字段 -->
          <div class="detail-field">
            <label>正式题名</label>
            <input v-model="form.title" :placeholder="activeItem.inputTitle" />
          </div>
          <div class="detail-field">
            <label>责任者</label>
            <input v-model="form.responsible" />
          </div>
          <div class="detail-field">
            <label>形成日期</label>
            <input v-model="form.formedDate" type="date" />
          </div>
          <div class="detail-field">
            <label>分类</label>
            <select v-model.number="form.categoryId" @change="onCategoryChange">
              <option :value="0">请选择</option>
              <option v-for="cat in categories" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
            </select>
          </div>
          <div class="detail-field">
            <label>标签</label>
            <input v-model="form.tags" />
          </div>
          <div class="detail-field">
            <label>所属全宗</label>
            <select v-model="form.fondsId">
              <option :value="0">请选择</option>
              <option v-for="f in fondsOptions" :key="f.id" :value="f.id">{{ f.fondsNo }} · {{ f.fondsName }}</option>
            </select>
          </div>

          <!-- 纸质档案额外字段 -->
          <template v-if="activeItem.carrierStatus !== 'electronic'">
            <div class="detail-field">
              <label>档案盒</label>
              <select v-model="form.boxId">
                <option :value="undefined">请选择</option>
                <option v-for="b in boxOptions" :key="b.id" :value="b.id">{{ b.boxNo }}（{{ b.locationCode }}）</option>
              </select>
              <div class="hint">盒即架位，选择档案盒即确定其所在架位（括号内为架位号）。</div>
            </div>
          </template>
          <div v-else class="notice" style="margin-top:8px">纯电子档案无需盒号和架位。</div>

          <!-- 已入库提示 -->
          <div v-if="activeItem.archiveNo" class="notice" style="margin-top:12px">
            已入库，档号 {{ activeItem.archiveNo }}。
          </div>

          <!-- 操作按钮 -->
          <div class="detail-actions">
            <el-button
              v-if="canArchive"
              type="primary"
              :loading="archiving"
              @click="handleArchive"
            >
              确认入库
            </el-button>
            <el-button
              v-if="canShelve"
              @click="handleShelve"
            >
              确认上架
            </el-button>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { PendingBatch, PendingItem } from '@/types/archive'
import { CarrierStatusLabel, SecurityLevelLabel, RetentionPeriodLabel } from '@/types/enums'
import {
  getPendingBatches,
  getPendingBatchDetail,
  startAiCompletion,
  getAiTask,
  confirmItem,
  archiveItem,
  shelveBatch,
} from '@/api/archive'
import { getFonds } from '@/api/fonds'
import { getArchiveBoxes } from '@/api/warehouse'
import type { FondsItem } from '@/types/fonds'
import type { ArchiveBox } from '@/types/warehouse'

// ── 分类选项 ──
const categories = [
  { id: 1, name: '文书档案' },
  { id: 2, name: '科技档案' },
  { id: 3, name: '会计档案' },
  { id: 4, name: '音像档案' },
  { id: 5, name: '人事档案' },
]

// ── 状态 ──
const batches = ref<PendingBatch[]>([])
const activeBatch = ref<PendingBatch | null>(null)
const items = ref<PendingItem[]>([])
const activeItem = ref<PendingItem | null>(null)
const batchLoading = ref(false)
const aiLoading = ref(false)
const archiving = ref(false)

// 已入库待上架批次（status=archived 且 pendingShelfCount>0）
const shelvableBatches = ref<PendingBatch[]>([])
const shelvableLoading = ref(false)
const shelvingId = ref<number | null>(null)

const form = reactive({
  title: '',
  responsible: '',
  formedDate: '',
  categoryId: 0,
  tags: '',
  fondsId: 0,
  boxId: undefined as number | undefined,
})

// 入库选项：全宗 / 档案盒（盒即架位，来自库房主数据，提供真实 ID）
const fondsOptions = ref<FondsItem[]>([])
const boxOptions = ref<ArchiveBox[]>([])

async function loadArchiveOptions() {
  try {
    const [fonds, boxes] = await Promise.all([
      getFonds({ pageSize: 100 }),
      getArchiveBoxes({ pageSize: 100 }),
    ])
    fondsOptions.value = fonds.records
    boxOptions.value = boxes.records
  } catch {
    // 选项加载失败不阻塞主流程，选择器留空
  }
}

// ── 标签映射 ──
function aiLabel(s: string): string {
  const map: Record<string, string> = {
    not_started: '未开始',
    running: '处理中',
    partial_completed: '部分完成',
    completed: '已完成',
    failed: '失败',
  }
  return map[s] || s
}

function carrierLabel(c: string): string {
  return CarrierStatusLabel[c] || c
}

function securityLabel(l: number): string {
  return SecurityLevelLabel[l] || '未知'
}

function retentionLabel(r: string): string {
  return RetentionPeriodLabel[r] || r
}

function statusText(s: string): string {
  const map: Record<string, string> = {
    accepted: '已接收',
    suggested: 'AI已建议',
    confirmed: '待入库',
    pending_archive: '待入库',
    archived: '已入库',
    normal: '正常',
  }
  return map[s] || s
}

function statusClass(s: string): string {
  const map: Record<string, string> = {
    accepted: 'info',
    suggested: 'warning',
    confirmed: 'info',
    pending_archive: 'info',
    archived: 'success',
    normal: 'success',
  }
  return map[s] || ''
}

const aiStatusLabel = computed(() => aiLabel(activeBatch.value?.aiStatus || 'not_started'))

const canArchive = computed(() => {
  if (!activeItem.value) return false
  const s = activeItem.value.itemStatus
  return (s === 'accepted' || s === 'suggested' || s === 'confirmed' || s === 'pending_archive') && !activeItem.value.archiveNo
})

const canShelve = computed(() => {
  if (!activeItem.value) return false
  return activeItem.value.lifecycleStatus === 'pending_shelf' && activeItem.value.carrierStatus !== 'electronic'
})

// ── 表单同步 ──
function syncFormFromItem(it: PendingItem) {
  // 已确认字段优先（用户核查后的权威值），其次 AI 建议
  form.title = it.confirmedTitle || it.suggestedTitle || ''
  form.responsible = it.confirmedResponsible || it.suggestedResponsible || ''
  form.formedDate = it.confirmedFormedDate || it.suggestedFormedDate || ''
  form.categoryId = it.confirmedCategoryId || it.suggestedCategoryId || 0
  form.tags = (it.confirmedTags || it.suggestedTags || []).join(',')
  form.fondsId = (it as PendingItem & { fondsId?: number }).fondsId ?? 0
  form.boxId = (it as PendingItem & { boxId?: number }).boxId
}

// ── 数据加载 ──
async function loadBatches() {
  batchLoading.value = true
  try {
    const res = await getPendingBatches()
    batches.value = res.records
    if (batches.value.length > 0) {
      selectBatch(batches.value[0])
    }
  } finally {
    batchLoading.value = false
  }
}

// 加载已入库且含 pending_shelf 档案的批次
async function loadShelvableBatches() {
  shelvableLoading.value = true
  try {
    const res = await getPendingBatches({ status: 'archived', pageNo: 1, pageSize: 100 })
    shelvableBatches.value = res.records.filter((b) => (b.pendingShelfCount ?? 0) > 0)
  } catch {
    shelvableBatches.value = []
  } finally {
    shelvableLoading.value = false
  }
}

// 批次级确认上架（来自左侧「已入库待上架」分区）
async function handleShelveBatch(b: PendingBatch) {
  shelvingId.value = b.id
  try {
    await shelveBatch(b.id, { note: '纸质档案已放入预占架位' })
    ElMessage.success(`${b.batchNo} 已确认上架，进入正常利用范围。`)
    await Promise.all([loadShelvableBatches(), loadBatches()])
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '上架失败'
    ElMessage.error(msg)
  } finally {
    shelvingId.value = null
  }
}

async function selectBatch(b: PendingBatch) {
  activeBatch.value = b
  try {
    const detail = await getPendingBatchDetail(b.id)
    items.value = detail.items
    activeItem.value = items.value.length > 0 ? items.value[0] : null
    if (activeItem.value) syncFormFromItem(activeItem.value)
  } catch {
    items.value = []
    activeItem.value = null
  }
}

function selectItem(it: PendingItem) {
  activeItem.value = it
  syncFormFromItem(it)
}

// ── AI 补全 ──
async function handleRunAi() {
  if (!activeBatch.value) return
  aiLoading.value = true
  ElMessage.info('AI 正在整批补全…')
  try {
    const task = await startAiCompletion(activeBatch.value.id)
    if (task.status === 'running') {
      for (let i = 0; i < 30; i++) {
        await new Promise((r) => setTimeout(r, 1000))
        const t = await getAiTask(task.aiTaskId)
        if (t.status !== 'running') break
      }
    }
    await selectBatch(activeBatch.value)
    ElMessage.success('AI 整批补全完成，请逐条核查后入库。')
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : 'AI 补全失败'
    ElMessage.error(msg)
  } finally {
    aiLoading.value = false
  }
}

// ── 确认入库 ──
async function handleArchive() {
  if (!activeItem.value) return
  const required: Record<string, string> = {
    title: '正式题名',
    responsible: '责任者',
    formedDate: '形成日期',
  }
  for (const [key, label] of Object.entries(required)) {
    if (!form[key as keyof typeof form]) {
      ElMessage.warning(`请补全${label}后再入库。`)
      return
    }
  }
  if (form.categoryId === 0) {
    ElMessage.warning('请选择分类后再入库。')
    return
  }
  if (form.fondsId === 0) {
    ElMessage.warning('请选择所属全宗后再入库。')
    return
  }
  if (activeItem.value.carrierStatus !== 'electronic') {
    if (!form.boxId) {
      ElMessage.warning('纸质档案入库前必须选择档案盒。')
      return
    }
  }

  archiving.value = true
  try {
    // 仅在「已接收」态需要先确认字段；suggested/confirmed/pending_archive 已确认过，直接入库
    // 注意：前端 activeItem.itemStatus 可能滞后于后端（archive 失败后未刷新），
    // 导致误判为 accepted 仍走 confirmItem，后端返回 409 BUSINESS_CONFLICT（已确认过）。
    // 此时静默跳过 confirmation，直接走 archive 即可。
    if (activeItem.value.itemStatus === 'accepted') {
      try {
        await confirmItem(activeItem.value.id, {
          confirmedTitle: form.title,
          confirmedResponsibleText: form.responsible,
          confirmedFormedDate: form.formedDate,
          confirmedCategoryId: form.categoryId,
          confirmedTags: form.tags.split(',').map((t) => t.trim()).filter(Boolean),
        })
      } catch (e) {
        // 已确认过（409 BUSINESS_CONFLICT）属预期，继续入库
        const msg = e instanceof Error ? e.message : ''
        if (!/不是已接收|BUSINESS_CONFLICT|已确认/.test(msg)) throw e
      }
    }
    const result = await archiveItem(activeItem.value.id, {
      fondsId: form.fondsId,
      boxId: form.boxId,
    })
    ElMessage.success(`${activeItem.value.inputTitle} 已入库，档号 ${result.archiveNo}。`)
    if (activeBatch.value) await selectBatch(activeBatch.value)
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '入库失败'
    ElMessage.error(msg)
  } finally {
    archiving.value = false
  }
}

// ── 确认上架 ──
async function handleShelve() {
  if (!activeBatch.value) return
  try {
    await shelveBatch(activeBatch.value.id, { note: '纸质档案已放入预占架位' })
    ElMessage.success(`${activeItem.value?.inputTitle} 已确认上架，进入正常利用范围。`)
    if (activeBatch.value) await selectBatch(activeBatch.value)
    // 刷新左栏「已入库待上架」分区，使已上架批次从该分区移除
    await loadShelvableBatches()
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '上架失败'
    ElMessage.error(msg)
  }
}

// 分类下拉兜底：v-model.number 已能处理大部分场景，但自动化填表
// （如 fill_form 仅修改 select.value）可能不触发 Vue 的双向绑定，
// 这里在 change 事件里显式同步，保证 form.categoryId 始终拿到数字值。
function onCategoryChange(e: Event) {
  const raw = (e.target as HTMLSelectElement).value
  const num = Number(raw)
  form.categoryId = Number.isNaN(num) ? 0 : num
}

onMounted(() => {
  loadBatches()
  loadShelvableBatches()
  loadArchiveOptions()
})
</script>

<style scoped>
.pending-archive {
  padding: 0;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.work-layout {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr) 420px;
  gap: 14px;
  align-items: start;
  height: calc(100vh - 240px);
  min-height: 560px;
}

.left-col {
  display: flex;
  flex-direction: column;
  gap: 14px;
  align-self: start;
}

.panel-scroll {
  overflow-y: auto;
  height: 100%;
  max-height: calc(100vh - 320px);
  padding-right: 4px;
}

.batch-card {
  display: grid;
  gap: 6px;
  width: 100%;
  padding: 11px 12px;
  margin-bottom: 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.14s, background 0.14s;
  font-size: 14px;
}
.batch-card:hover,
.batch-card.active {
  border-color: #8abcbf;
  background: #f2f8f8;
}
.batch-no {
  font-family: monospace;
  font-size: 12px;
  color: var(--muted);
}

.shelvable-card {
  border-left: 3px solid #8abcbf;
}

.item-card {
  display: flex;
  gap: 10px;
  align-items: center;
  width: 100%;
  padding: 10px 12px;
  margin-bottom: 6px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #fff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.14s, background 0.14s;
  font-size: 14px;
}
.item-card:hover,
.item-card.active {
  border-color: #8abcbf;
  background: #f2f8f8;
}
.item-title { flex: 1; }
.item-meta { font-size: 12px; color: var(--muted); }

.detail-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: var(--muted);
  font-size: 14px;
}

.protect-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  background: var(--warning-soft);
  font-size: 12px;
  color: #6f440c;
  margin-bottom: 12px;
}
.protect-row span { white-space: nowrap; }

.detail-field {
  display: grid;
  grid-template-columns: 90px minmax(0, 1fr);
  gap: 6px;
  align-items: center;
  padding: 7px 0;
  border-bottom: 1px solid var(--border);
}
.detail-field label {
  font-size: 13px;
  font-weight: 700;
  color: var(--muted);
}
.detail-field input,
.detail-field select {
  width: 100%;
  min-height: 34px;
  padding: 6px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #fff;
  font-size: 14px;
}

.detail-actions {
  display: flex;
  gap: 8px;
  margin-top: 14px;
  flex-wrap: wrap;
}

.status {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}
.status.info { background: #e6f7ff; color: #1890ff; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.success { background: #f6ffed; color: #52c41a; }

@media (max-width: 1200px) {
  .work-layout { grid-template-columns: 1fr; height: auto; }
}
</style>
