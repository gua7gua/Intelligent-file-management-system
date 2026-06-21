<template>
  <div class="inventory">
    <section class="hero-line">
      <div>
        <h1 class="page-title">档案盘点</h1>
        <p class="page-subtitle">按库房号和分类生成盘点任务，命中范围内档案在盘点期间暂停借阅。</p>
      </div>
      <button class="button" @click="openTaskModal">+ 新建盘点任务</button>
    </section>

    <section class="metric-row">
      <div class="metric card"><div class="metric-num">{{ metrics.running }}</div><div class="metric-label">进行中任务</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.abnormal }}</div><div class="metric-label">异常项</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.completedThisYear }}</div><div class="metric-label">本年完成</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.draft }}</div><div class="metric-label">草稿</div></div>
    </section>

    <div v-if="scopeBanner" class="scope-banner">
      <div class="scope-formula">
        <span>盘点范围</span><span>库房号：{{ scopeBanner.roomNo }}</span><strong>AND</strong><span>分类：{{ scopeBanner.categoryName }}</span>
      </div>
      <span class="status warning">借阅暂停生效</span>
    </div>

    <section class="workspace">
      <aside class="card panel list-col">
        <div class="tabs">
          <button class="tab" :class="{ active: taskFilter === 'running' }" @click="taskFilter = 'running'">进行中</button>
          <button class="tab" :class="{ active: taskFilter === 'draft' }" @click="taskFilter = 'draft'">草稿</button>
          <button class="tab" :class="{ active: taskFilter === 'completed' }" @click="taskFilter = 'completed'">已完成</button>
        </div>
        <div v-if="listLoading" class="empty">加载中...</div>
        <div v-else-if="loadError" class="empty">加载失败：<button class="link" @click="loadTasks">重试</button></div>
        <div v-else-if="filteredTasks.length === 0" class="empty">暂无盘点任务</div>
        <div v-else class="task-list">
          <button v-for="t in filteredTasks" :key="t.id" class="task-item" :class="{ active: selectedId === t.id }" @click="selectTask(t.id)">
            <strong>{{ t.taskName }}</strong>
            <span class="task-meta"><span>范围：{{ t.roomNo }} AND {{ t.categoryName }}</span><span>{{ t.total }} 件</span></span>
            <span class="progress-track"><span class="progress-fill" :style="{ width: progressPercent(t) + '%' }"></span></span>
            <span class="task-meta"><span>已核对 {{ t.checked }} 件</span><span class="status" :class="taskStatusClass(t.status)">{{ InventoryTaskStatusLabel[t.status] }}</span></span>
          </button>
        </div>
        <div v-if="total > 0" style="display:flex;justify-content:flex-end;margin-top:12px">
          <el-pagination
            v-model:current-page="pageNo"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadTasks"
            @current-change="loadTasks"
          />
        </div>
      </aside>

      <section class="card panel">
        <template v-if="!selectedId"><div class="empty">← 点击左侧任务查看盘点明细</div></template>
        <template v-else-if="detailLoading"><div class="empty">加载中...</div></template>
        <template v-else-if="!detail"><div class="empty">明细加载失败：<button class="link" @click="selectTask(selectedId)">重试</button></div></template>
        <template v-else>
          <div class="inventory-toolbar">
            <div>
              <h2 class="section-title">{{ detail.taskName }}</h2>
              <p class="hint">任务号：{{ detail.taskNo }} · 状态：{{ InventoryTaskStatusLabel[detail.status] }}<span v-if="detail.startedAt"> · 开始：{{ detail.startedAt }}</span></p>
            </div>
            <div class="actions">
              <button v-if="detail.status === 'draft'" class="button" :disabled="detail.items.length === 0" @click="onStart">开始盘点</button>
              <button v-if="detail.status === 'draft'" class="button ghost" @click="onDeleteTask">删除任务</button>
              <button v-if="detail.status === 'running'" class="button" @click="onComplete">提交盘点结果</button>
            </div>
          </div>

          <div v-if="detail.status !== 'draft'" class="result-filter">
            <button v-for="opt in resultFilters" :key="opt.value" class="mini-button" :class="{ active: resultFilter === opt.value }" @click="resultFilter = opt.value">
              {{ opt.label }} {{ resultCount(opt.value) }}
            </button>
          </div>

          <div v-if="detail.items.length === 0" class="empty">该任务暂无盘点明细。</div>
          <div v-else class="table-wrap">
            <table>
              <thead><tr><th>档号/题名</th><th>应在架位</th><th>实际架位</th><th>借阅状态</th><th>盘点结果</th><th>说明</th></tr></thead>
              <tbody>
                <tr v-for="it in visibleItems" :key="it.id">
                  <td><span class="mono">{{ it.archiveNo }}</span><br>{{ it.title }}</td>
                  <td class="mono">{{ it.expectedLocationCode }}</td>
                  <td>
                    <input v-if="detail.status === 'running'" v-model="editMap[it.id].actualLocationCode" class="inline-input mono">
                    <template v-else>{{ it.actualLocationCode || '—' }}</template>
                  </td>
                  <td><span class="status" :class="loanClass(it)">{{ loanLabel(it) }}</span></td>
                  <td>
                    <select v-if="detail.status === 'running'" v-model="editMap[it.id].checkResult">
                      <option value="">未核对</option>
                      <option v-for="r in resultOptions" :key="r.value" :value="r.value">{{ r.label }}</option>
                    </select>
                    <span v-else class="status" :class="checkResultClass(it.checkResult)">{{ InventoryCheckResultLabel[it.checkResult] || '—' }}</span>
                  </td>
                  <td>
                    <input v-if="detail.status === 'running'" v-model="editMap[it.id].note" class="inline-input">
                    <template v-else>{{ it.note || '—' }}</template>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </template>
      </section>
    </section>

    <div v-if="taskModalVisible" class="modal-backdrop" @click.self="taskModalVisible = false">
      <div class="modal">
        <header><h2 class="section-title">新建盘点任务</h2><button class="button ghost" @click="taskModalVisible = false">关闭</button></header>
        <div class="body">
          <div class="notice warning">盘点范围必须同时满足库房号和分类：库房号 AND 分类。</div>
          <div class="form-grid">
            <div class="field"><label>库房号</label>
              <select v-model.number="newTask.roomId">
                <option v-for="r in rooms" :key="r.id" :value="r.id">{{ r.roomNo }} {{ r.roomName }}</option>
              </select>
            </div>
            <div class="field"><label>分类</label>
              <select v-model.number="newTask.categoryId">
                <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </div>
            <div class="field"><label>任务名称</label><input v-model="newTask.taskName"></div>
          </div>
        </div>
        <footer>
          <button class="button ghost" @click="taskModalVisible = false">取消</button>
          <button class="button" @click="onCreate">生成清单</button>
        </footer>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { InventoryItem, InventoryTask, InventoryTaskDetail } from '@/types/inventory'
import type { WarehouseRoom } from '@/types/warehouse'
import {
  InventoryCheckResult, InventoryCheckResultLabel, InventoryTaskStatusLabel,
} from '@/types/enums'
import type { InventoryCheckResultValue, InventoryTaskStatusValue } from '@/types/enums'
import {
  completeInventoryTask, createInventoryTask, deleteInventoryTask, getInventoryTaskDetail,
  getInventoryTasks, startInventoryTask, updateInventoryItem,
} from '@/api/inventory'
import { getWarehouseRooms } from '@/api/warehouse'

interface ItemEdit { actualLocationCode: string; checkResult: InventoryCheckResultValue | ''; note: string }

const allTasks = ref<InventoryTask[]>([])
const listLoading = ref(false)
const loadError = ref(false)
const taskFilter = ref<InventoryTaskStatusValue>('running')

// ── 分页（任务列表） ──
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

const selectedId = ref<number | null>(null)
const detail = ref<InventoryTaskDetail | null>(null)
const detailLoading = ref(false)

const editMap = reactive<Record<number, ItemEdit>>({})
const resultFilter = ref<'all' | InventoryCheckResultValue>('all')

const rooms = ref<WarehouseRoom[]>([])
const categories = [
  { id: 1, name: '文书档案' }, { id: 2, name: '科技档案' }, { id: 3, name: '会计档案' },
  { id: 4, name: '音像档案' }, { id: 5, name: '人事档案' },
]
const taskModalVisible = ref(false)
const newTask = reactive({ roomId: 1, categoryId: 1, taskName: '' })

const resultOptions: { value: InventoryCheckResultValue; label: string }[] = [
  { value: InventoryCheckResult.NORMAL, label: '正常' },
  { value: InventoryCheckResult.MISSING, label: '缺失' },
  { value: InventoryCheckResult.MISPLACED, label: '错位' },
  { value: InventoryCheckResult.DAMAGED, label: '损坏' },
  { value: InventoryCheckResult.ON_LOAN, label: '借出中' },
]
const resultFilters: { value: 'all' | InventoryCheckResultValue; label: string }[] = [
  { value: 'all', label: '全部' },
  { value: InventoryCheckResult.NORMAL, label: '正常' },
  { value: InventoryCheckResult.MISSING, label: '缺失' },
  { value: InventoryCheckResult.MISPLACED, label: '错位' },
  { value: InventoryCheckResult.DAMAGED, label: '损坏' },
  { value: InventoryCheckResult.ON_LOAN, label: '借出中' },
]

const metrics = computed(() => ({
  running: allTasks.value.filter((t) => t.status === 'running').length,
  draft: allTasks.value.filter((t) => t.status === 'draft').length,
  completedThisYear: allTasks.value.filter((t) => t.status === 'completed').length,
  abnormal: allTasks.value.reduce((s, t) => s + t.abnormalCount, 0),
}))

const filteredTasks = computed(() => allTasks.value.filter((t) => t.status === taskFilter.value))

const scopeBanner = computed(() => {
  if (!detail.value || detail.value.status === 'draft') return null
  return { roomNo: detail.value.roomNo, categoryName: detail.value.categoryName }
})

const visibleItems = computed(() => {
  if (!detail.value) return []
  if (resultFilter.value === 'all') return detail.value.items
  return detail.value.items.filter((it) => it.checkResult === resultFilter.value)
})

function progressPercent(t: InventoryTask): number {
  return t.total > 0 ? Math.round((t.checked / t.total) * 100) : 0
}
function taskStatusClass(s: string): string {
  return ({ draft: 'info', running: 'warning', completed: 'success' } as Record<string, string>)[s] ?? ''
}
function loanLabel(it: InventoryItem): string {
  if (it.loanStatus === 'on_loan') return '借出中'
  return detail.value?.status === 'running' ? '暂停借阅' : '可借'
}
function loanClass(it: InventoryItem): string {
  if (it.loanStatus === 'on_loan') return 'info'
  return detail.value?.status === 'running' ? 'warning' : 'success'
}
function checkResultClass(s: string): string {
  return ({ normal: 'success', missing: 'danger', misplaced: 'warning', damaged: 'warning', on_loan: 'info' } as Record<string, string>)[s] ?? ''
}
function resultCount(v: string): number {
  if (!detail.value) return 0
  if (v === 'all') return detail.value.items.length
  return detail.value.items.filter((it) => it.checkResult === v).length
}

async function loadTasks() {
  listLoading.value = true
  loadError.value = false
  try {
    const page = await getInventoryTasks({ pageNo: pageNo.value, pageSize: pageSize.value })
    allTasks.value = page.records
    total.value = page.total
  } catch {
    loadError.value = true
  } finally {
    listLoading.value = false
  }
}

async function selectTask(id: number) {
  selectedId.value = id
  detail.value = null
  detailLoading.value = true
  resultFilter.value = 'all'
  try {
    detail.value = await getInventoryTaskDetail(id)
    Object.keys(editMap).forEach((k) => { delete editMap[Number(k)] })
    detail.value.items.forEach((it) => {
      editMap[it.id] = { actualLocationCode: it.actualLocationCode ?? '', checkResult: it.checkResult, note: it.note ?? '' }
    })
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '明细加载失败')
  } finally {
    detailLoading.value = false
  }
}

function openTaskModal() {
  const r = rooms.value[0]
  newTask.roomId = r?.id ?? 1
  newTask.categoryId = 1
  newTask.taskName = `2026 年度 ${r?.roomNo ?? ''} 库房${categories[0].name}盘点`
  taskModalVisible.value = true
}

async function onCreate() {
  if (!newTask.taskName.trim()) {
    ElMessage.warning('请填写任务名称')
    return
  }
  try {
    const created = await createInventoryTask({ taskName: newTask.taskName, roomId: newTask.roomId, categoryId: newTask.categoryId })
    taskModalVisible.value = false
    ElMessage.success(`已生成盘点清单：${created.taskNo}，命中 ${created.items.length} 件`)
    await loadTasks()
    await selectTask(created.id)
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '创建失败')
  }
}

async function onStart() {
  if (!detail.value) return
  try {
    const started = await startInventoryTask(detail.value.id)
    detail.value = started
    ElMessage.success('盘点已开始，范围内档案借阅暂停。')
    await loadTasks()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onComplete() {
  if (!detail.value) return
  try {
    for (const it of detail.value.items) {
      const edit = editMap[it.id]
      if (!edit) continue
      const changed = edit.actualLocationCode !== (it.actualLocationCode ?? '') || edit.checkResult !== it.checkResult || edit.note !== (it.note ?? '')
      if (changed) {
        if (!edit.checkResult) {
          ElMessage.warning(`档案 ${it.archiveNo} 未选择盘点结果`)
          return
        }
        await updateInventoryItem(detail.value.id, it.id, { actualLocationCode: edit.actualLocationCode, checkResult: edit.checkResult, note: edit.note })
      }
    }
    await ElMessageBox.confirm('提交后任务变为已完成，正常档案恢复可借，是否继续？', '提交盘点结果', { type: 'warning' })
  } catch {
    return
  }
  try {
    const completed = await completeInventoryTask(detail.value.id, { summary: '' })
    detail.value = completed
    ElMessage.success('盘点结果已提交，任务已完成。')
    await loadTasks()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '操作失败')
  }
}

async function onDeleteTask() {
  if (!detail.value) return
  try {
    await ElMessageBox.confirm('删除后不可恢复，仅草稿任务可删，是否继续？', '删除盘点任务', { type: 'warning' })
  } catch {
    return
  }
  try {
    await deleteInventoryTask(detail.value.id)
    ElMessage.success('盘点任务已删除。')
    selectedId.value = null
    detail.value = null
    await loadTasks()
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '删除失败')
  }
}

onMounted(async () => {
  await loadTasks()
  try {
    rooms.value = await getWarehouseRooms()
  } catch {
    rooms.value = []
  }
})
</script>

<style scoped>
.inventory { padding: 0; }
.hero-line { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; justify-content: space-between; margin-bottom: 16px; }
.page-title { font-size: 20px; font-weight: 700; margin: 0 0 4px; }
.page-subtitle { color: var(--muted); font-size: 13px; margin: 0; }
.metric-row { display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px; margin-bottom: 16px; }
.metric { padding: 16px; text-align: center; }
.metric-num { font-size: 26px; font-weight: 700; color: var(--primary); }
.metric-label { font-size: 13px; color: var(--muted); margin-top: 4px; }
.scope-banner { display: flex; justify-content: space-between; align-items: center; gap: 12px; padding: 12px 14px; border: 1px solid #efd39d; border-radius: var(--radius); background: var(--warning-soft); color: #6f440c; margin-bottom: 16px; }
.scope-formula { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; font-weight: 700; }
.scope-formula span { display: inline-flex; padding: 3px 9px; border-radius: 999px; background: #fff; }
.workspace { display: grid; grid-template-columns: 320px minmax(0, 1fr); gap: 16px; align-items: start; }
.list-col { padding: 10px; max-height: 700px; overflow-y: auto; }
.tabs { display: flex; gap: 6px; margin-bottom: 10px; }
.tab { padding: 6px 12px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: #fff; cursor: pointer; font-size: 13px; }
.tab.active { color: var(--primary-strong); border-color: #8abcbf; background: var(--primary-soft); }
.task-list { display: grid; gap: 10px; }
.task-item { display: grid; gap: 6px; padding: 12px; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; text-align: left; cursor: pointer; }
.task-item:hover { background: #fafafa; }
.task-item.active { border-color: #8abcbf; background: #f2f8f8; }
.task-meta { display: flex; gap: 12px; color: var(--muted); font-size: 12px; }
.progress-track { height: 8px; border-radius: 999px; background: #e5edf2; overflow: hidden; }
.progress-fill { display: block; height: 100%; background: var(--primary); }
.inventory-toolbar { display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; margin-bottom: 12px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 4px; }
.hint { color: var(--muted); font-size: 12px; }
.actions { display: flex; gap: 8px; }
.result-filter { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px; }
.mini-button { padding: 5px 10px; border: 1px solid var(--border); border-radius: var(--radius-sm); background: #fff; cursor: pointer; font-size: 12px; font-weight: 700; }
.mini-button.active { color: var(--primary-strong); border-color: #b9d7d9; background: var(--primary-soft); }
.table-wrap { overflow-x: auto; }
.table-wrap table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table-wrap th, .table-wrap td { padding: 8px 10px; text-align: left; border-bottom: 1px solid var(--border); }
.table-wrap th { font-weight: 700; color: var(--muted); background: var(--bg); }
.inline-input { width: min(160px, 100%); padding: 4px 6px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 12px; }
.table-wrap select { padding: 4px 6px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 12px; }
.modal-backdrop { position: fixed; inset: 0; z-index: 30; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(23, 33, 43, 0.38); }
.modal { width: min(640px, 100%); border-radius: var(--radius); background: #fff; box-shadow: var(--shadow); }
.modal header, .modal footer { display: flex; justify-content: space-between; align-items: center; padding: 14px 18px; border-bottom: 1px solid var(--border); }
.modal footer { border-top: 1px solid var(--border); border-bottom: 0; }
.modal .body { padding: 18px; }
.notice { padding: 9px 12px; border-radius: var(--radius-sm); font-size: 12px; margin-bottom: 10px; }
.notice.warning { background: var(--warning-soft); color: #7a4c12; }
.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.field { display: grid; gap: 4px; }
.field label { font-size: 12px; color: var(--muted); font-weight: 700; }
.field input, .field select { padding: 6px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 13px; }
.button { padding: 6px 14px; border: 1px solid var(--primary); border-radius: var(--radius-sm); background: var(--primary); color: #fff; cursor: pointer; font-size: 13px; }
.button.ghost { background: #fff; color: var(--text); border-color: var(--border); }
.empty { display: flex; align-items: center; justify-content: center; height: 200px; color: var(--muted); font-size: 14px; }
.link { background: none; border: none; color: var(--primary); cursor: pointer; font-size: 13px; text-decoration: underline; padding: 0; }
.muted { color: var(--muted); font-size: 12px; }
.mono { font-family: monospace; font-size: 12px; }
.status { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.status.success { background: #f6ffed; color: #52c41a; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.danger { background: #fff1f0; color: #f5222d; }
.status.info { background: #e6f7ff; color: #1890ff; }
@media (max-width: 1080px) { .metric-row, .workspace, .form-grid { grid-template-columns: 1fr; } }
</style>
