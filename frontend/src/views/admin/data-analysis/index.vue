<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createAnalysisTask, deleteAnalysisItem, deleteAnalysisTask, getAnalysisTaskDetail, getAnalysisTasks, handleAnalysisItem,
} from '@/api/data-analysis'
import type { AnalysisItem, AnalysisTask, AnalysisTaskDetail } from '@/types/data-analysis'
import type { AnalysisTaskTypeValue } from '@/types/enums'
import {
  AnalysisItemStatusLabel, AnalysisProblemTypeLabel, AnalysisTaskStatusLabel, AnalysisTaskTypeLabel,
} from '@/types/enums'

const tasks = ref<AnalysisTask[]>([])
const current = ref<AnalysisTaskDetail | null>(null)
const loading = ref(true)
const errorMsg = ref('')
const selectedItem = ref<AnalysisItem | null>(null)
const newTaskType = ref<AnalysisTaskTypeValue>('mixed')
const newIncludeAi = ref(true)
const filterStatus = ref<'' | 'running' | 'completed' | 'failed'>('')

// ── 分页（任务列表） ──
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

// 扫描范围：档案门类多选 + 形成年度范围。
// 门类 id 与档案管理页 categoryTree 一致（1=文书 2=科技 3=会计 4=音像 5=人事）。
const scanCategories = [
  { id: 1, name: '文书档案' },
  { id: 2, name: '科技档案' },
  { id: 3, name: '会计档案' },
  { id: 4, name: '音像档案' },
  { id: 5, name: '人事档案' },
]
const newCategoryIds = ref<number[]>([1, 2, 3, 4, 5])
const newYearStart = ref<number>(2010)
const newYearEnd = ref<number>(2026)

const filteredTasks = computed(() => filterStatus.value ? tasks.value.filter((t) => t.status === filterStatus.value) : tasks.value)
// 异常建议列表状态筛选：默认只看待处理；采纳/不采纳后项移出当前视图，删除则彻底软删移除
const itemFilterStatus = ref<'all' | 'pending' | 'adopted' | 'rejected'>('pending')
const filteredItems = computed(() => {
  const items = current.value?.items ?? []
  if (itemFilterStatus.value === 'all') return items
  return items.filter((i) => i.status === itemFilterStatus.value)
})

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const page = await getAnalysisTasks({ pageNo: pageNo.value, pageSize: pageSize.value })
    tasks.value = page.records
    total.value = page.total
    if (tasks.value.length) await selectTask(tasks.value[0].id)
  } catch (e) {
    errorMsg.value = (e as Error).message || '研判任务加载失败'
  } finally {
    loading.value = false
  }
}

// running 任务轮询：完成后自动刷新详情并停止（解决「AI 研判完成后不自动刷新」）
let pollTimer: ReturnType<typeof setInterval> | null = null
function stopPolling() {
  if (pollTimer) { clearInterval(pollTimer); pollTimer = null }
}
function startPolling(taskId: number) {
  stopPolling()
  pollTimer = setInterval(async () => {
    if (!current.value) { stopPolling(); return }
    try {
      const fresh = await getAnalysisTaskDetail(taskId)
      current.value = fresh
      if (fresh.status !== 'running') {
        stopPolling()
        ElMessage.success('研判任务已完成，结果已刷新。')
      }
    } catch {
      stopPolling()
    }
  }, 3000)
}

async function selectTask(id: number) {
  try {
    current.value = await getAnalysisTaskDetail(id)
    selectedItem.value = current.value.items[0] ?? null
    if (current.value.status === 'running') startPolling(id)
    else stopPolling()
  } catch (e) {
    ElMessage.warning((e as Error).message)
  }
}

// 删除研判任务（仅 completed/failed 可删，running 不可删）
async function deleteTask(t: AnalysisTask) {
  try {
    await ElMessageBox.confirm(
      '删除该研判任务及全部异常项，不可恢复？',
      '提示',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await deleteAnalysisTask(t.id)
    ElMessage.success('研判任务已删除。')
    if (current.value?.id === t.id) current.value = null
    await load()
  } catch (e) {
    ElMessage.error((e as Error).message || '删除研判任务失败')
  }
}

// 删除单条异常项（R3-C2：走完闭环——可扫描产生/采纳不采纳/删除；软删保留留痕）
async function deleteItem(item: AnalysisItem) {
  try {
    await ElMessageBox.confirm('删除该异常项？删除后列表不再展示（数据软删保留留痕）。', '提示', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
    })
  } catch {
    return
  }
  try {
    await deleteAnalysisItem(item.id)
    ElMessage.success('异常项已删除。')
    if (current.value) {
      await selectTask(current.value.id)
      // 删掉的恰好是当前选中项时，selectTask 会重置为 items[0]
    }
  } catch (e) {
    ElMessage.error((e as Error).message || '删除异常项失败')
  }
}

function openItem(item: AnalysisItem) {
  selectedItem.value = item
}

async function onAdopt() {
  await doHandle('adopted')
}
async function onReject() {
  await doHandle('rejected')
}
async function doHandle(action: 'adopted' | 'rejected') {
  if (!selectedItem.value) return
  try {
    await handleAnalysisItem(selectedItem.value.id, { action })
    ElMessage.success(action === 'adopted' ? '已采纳' : '已不采纳，该档案下次扫描将永久跳过')
    if (current.value) await selectTask(current.value.id)
  } catch (e) {
    ElMessage.warning((e as Error).message)
  }
}

async function startScan() {
  if (!newCategoryIds.value.length) {
    ElMessage.warning('请至少选择一个档案门类作为扫描范围')
    return
  }
  if (!newYearStart.value || !newYearEnd.value || newYearStart.value > newYearEnd.value) {
    ElMessage.warning('请填写有效的形成年度范围')
    return
  }
  try {
    const detail = await createAnalysisTask({
      scanMethod: newTaskType.value,
      rule: {
        categoryIds: [...newCategoryIds.value],
        formedYearStart: newYearStart.value,
        formedYearEnd: newYearEnd.value,
        includeAiSuggestion: newIncludeAi.value,
      },
    })
    ElMessage.success('扫描已开始')
    await load()
    await selectTask(detail.id)
  } catch (e) {
    ElMessage.warning((e as Error).message)
  }
}

async function onDelete() {
  if (!selectedItem.value) return
  await deleteItem(selectedItem.value)
}

onMounted(load)
onUnmounted(stopPolling)
</script>

<template>
  <div>
    <div class="toolbar">
      <div>
        <h1 class="page-title">数据研判</h1>
        <p class="page-subtitle">扫描正式档案元数据，生成缺失字段、分类冲突和标签建议；采纳后进入待处理清单，不在本页直接改库。</p>
      </div>
      <div class="actions">
        <button class="button secondary scan-start" id="startScan" @click="startScan"><span class="icon">S</span>开始扫描</button>
      </div>
    </div>

    <p v-if="loading" class="notice" style="margin-top:16px;">加载中…</p>
    <p v-else-if="errorMsg" class="notice" style="margin-top:16px;">
      {{ errorMsg }} <button class="button" @click="load">重试</button>
    </p>

    <div v-if="!loading && !errorMsg" class="analysis-grid" style="margin-top:16px">
      <section class="stack">
        <div class="card panel">
          <h2 class="section-title">扫描控制</h2>
          <div class="form-grid">
            <div class="field">
              <label>扫描范围（档案门类）</label>
              <div class="scope-checks">
                <label v-for="cat in scanCategories" :key="cat.id" class="check">
                  <input
                    type="checkbox"
                    :value="cat.id"
                    v-model="newCategoryIds"
                  />
                  {{ cat.name }}
                </label>
              </div>
            </div>
            <div class="field">
              <label>形成年度范围</label>
              <div class="row">
                <input v-model.number="newYearStart" type="number" placeholder="如 2010" />
                <span>—</span>
                <input v-model.number="newYearEnd" type="number" placeholder="如 2026" />
              </div>
            </div>
            <div class="field">
              <label>任务类型</label>
              <select v-model="newTaskType">
                <option value="mixed">{{ AnalysisTaskTypeLabel.mixed }}</option>
                <option value="rule">{{ AnalysisTaskTypeLabel.rule }}</option>
                <option value="ai">{{ AnalysisTaskTypeLabel.ai }}</option>
              </select>
            </div>
            <div class="field">
              <label>AI 建议</label>
              <label class="check"><input type="checkbox" v-model="newIncludeAi" /> 包含 AI 候选建议</label>
            </div>
          </div>
        </div>

        <div class="card panel">
          <div class="toolbar" style="margin-top:0">
            <h2 class="section-title">任务状态</h2>
            <span class="status" v-if="current">{{ AnalysisTaskStatusLabel[current.status] }}</span>
            <span class="status" v-else>未开始</span>
            <div class="tabs" style="margin-left:auto">
              <button class="tab" :class="{ active: filterStatus === '' }" @click="filterStatus = ''">全部</button>
              <button class="tab" :class="{ active: filterStatus === 'running' }" @click="filterStatus = 'running'">执行中</button>
              <button class="tab" :class="{ active: filterStatus === 'completed' }" @click="filterStatus = 'completed'">已完成</button>
              <button class="tab" :class="{ active: filterStatus === 'failed' }" @click="filterStatus = 'failed'">失败</button>
            </div>
          </div>
          <div class="task-strip" v-if="current">
            <div class="task-cell"><span>任务号</span><strong class="mono">{{ current.taskNo }}</strong></div>
            <div class="task-cell"><span>扫描档案</span><strong>{{ current.scannedCount }}</strong></div>
            <div class="task-cell"><span>候选建议</span><strong>{{ current.abnormalCount }}</strong></div>
            <div class="task-cell"><span>已采纳</span><strong>{{ current.adoptedCount }}</strong></div>
          </div>
          <div class="progress" v-if="current" aria-label="扫描进度"><span :style="{ width: (current.progress * 100) + '%' }"></span></div>
          <ul class="task-list" v-if="filteredTasks.length">
            <li
              v-for="t in filteredTasks"
              :key="t.id"
              class="task-item"
              :class="{ active: current?.id === t.id }"
              @click="selectTask(t.id)"
            >
              <strong class="mono">{{ t.taskNo }}</strong>
              <span class="muted">{{ t.scopeText }}</span>
              <span class="muted">{{ AnalysisTaskTypeLabel[t.scanMethod ?? 'mixed'] }}</span>
              <span class="status">{{ AnalysisTaskStatusLabel[t.status] }}</span>
              <span class="muted">{{ t.abnormalCount }} 项</span>
              <span class="task-action">
                <el-button
                  v-if="t.status === 'completed' || t.status === 'failed'"
                  size="small"
                  type="danger"
                  @click.stop="deleteTask(t)"
                >删除</el-button>
              </span>
            </li>
          </ul>
          <div v-if="total > 0" style="display:flex;justify-content:flex-end;margin-top:12px">
            <el-pagination
              v-model:current-page="pageNo"
              v-model:page-size="pageSize"
              :total="total"
              :page-sizes="[10, 20, 50, 100]"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="load"
              @current-change="load"
            />
          </div>
        </div>

        <div class="card panel">
          <div class="toolbar" style="margin-top:0">
            <h2 class="section-title">异常建议列表</h2>
            <div class="tabs" style="margin-left:auto">
              <button class="tab" :class="{ active: itemFilterStatus === 'pending' }" @click="itemFilterStatus = 'pending'">待处理</button>
              <button class="tab" :class="{ active: itemFilterStatus === 'adopted' }" @click="itemFilterStatus = 'adopted'">已采纳</button>
              <button class="tab" :class="{ active: itemFilterStatus === 'rejected' }" @click="itemFilterStatus = 'rejected'">已不采纳</button>
              <button class="tab" :class="{ active: itemFilterStatus === 'all' }" @click="itemFilterStatus = 'all'">全部</button>
            </div>
          </div>
          <div class="table-wrap item-table-wrap">
            <table>
              <thead>
                <tr>
                  <th>档号</th>
                  <th>题名</th>
                  <th>问题类型</th>
                  <th>问题描述</th>
                  <th>建议动作</th>
                  <th>状态</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="i in filteredItems"
                  :key="i.id"
                  :class="{ active: selectedItem?.id === i.id }"
                  style="cursor:pointer"
                  @click="openItem(i)"
                >
                  <td class="mono">{{ i.archiveNo }}</td>
                  <td>{{ i.title }}</td>
                  <td><span class="status info">{{ AnalysisProblemTypeLabel[i.problemType] }}</span></td>
                  <td>{{ i.problemDesc }}</td>
                  <td>{{ i.suggestedAction }}</td>
                  <td><span class="status">{{ AnalysisItemStatusLabel[i.status] }}</span></td>
                </tr>
                <tr v-if="!filteredItems.length">
                  <td colspan="6" class="muted" style="text-align:center;padding:16px;">当前筛选下暂无异常建议。</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <aside class="stack">
        <div class="card panel">
          <h2 class="section-title">条目详情</h2>
          <template v-if="selectedItem">
            <div class="detail-line"><span>档号</span><strong class="mono">{{ selectedItem.archiveNo }}</strong></div>
            <div class="detail-line"><span>题名</span><strong>{{ selectedItem.title }}</strong></div>
            <div class="detail-line"><span>问题</span><strong>{{ selectedItem.problemDesc }}</strong></div>
            <div class="detail-line"><span>建议</span><strong>{{ selectedItem.suggestedAction }}</strong></div>
            <div v-if="selectedItem.suggestion?.candidates?.length" class="candidates">
              <div class="candidates-title">AI 候选明细（字段 / 原值 / 建议值 / 置信度）</div>
              <div v-for="(c, idx) in selectedItem.suggestion.candidates" :key="idx" class="candidate">
                <div class="cand-row"><span>字段</span><strong>{{ c.field }}</strong></div>
                <div class="cand-row"><span>原值</span><strong>{{ c.currentValue || '（空）' }}</strong></div>
                <div class="cand-row"><span>建议值</span><strong>{{ (c.suggestedValue || []).join('、') || '—' }}</strong></div>
                <div class="cand-row"><span>置信度</span><strong>{{ Math.round((c.confidence || 0) * 100) }}%</strong></div>
              </div>
            </div>
            <div class="actions" style="margin-top:12px">
              <button class="button secondary" :disabled="selectedItem.status !== 'pending'" @click="onAdopt">采纳</button>
              <button class="button ghost" :disabled="selectedItem.status !== 'pending'" @click="onReject">不采纳</button>
              <el-button size="small" type="danger" @click="onDelete">删除</el-button>
              <span class="status">{{ AnalysisItemStatusLabel[selectedItem.status] }}</span>
            </div>
          </template>
          <p v-else class="muted">请从左侧列表选择一条异常建议。</p>
        </div>

        <div class="notice">
          <strong>处理说明</strong>
          <div>采纳：进入档案管理页人工处理；不采纳：认定该档案无问题，下次扫描永久跳过；删除：本次建议有误但可能仍有问题，软删本次建议，下次扫描仍会检查。</div>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.analysis-grid { display: grid; grid-template-columns: minmax(0,1fr) 360px; gap: 16px; }
.stack { display: grid; gap: 16px; }
.form-grid { display: grid; grid-template-columns: repeat(3, minmax(0,1fr)); gap: 12px; }
.field { display: grid; gap: 4px; }
.field select { padding: 6px 8px; height: 32px; box-sizing: border-box; line-height: 1.4; }
.check { display: flex; align-items: center; gap: 6px; }
.check input[type="checkbox"] { width: 15px; height: 15px; cursor: pointer; }
.scope-checks { display: flex; flex-wrap: wrap; gap: 6px 16px; }
.scope-checks .check { margin: 0; }
.row { display: flex; align-items: center; gap: 8px; }
.row input[type="number"] { width: 96px; padding: 6px 8px; }
.task-strip { display: grid; grid-template-columns: repeat(4, minmax(0,1fr)); gap: 12px; margin-top: 12px; }
.task-cell { display: grid; gap: 4px; padding: 8px; background: #f7f9fc; border-radius: 6px; }
.task-cell span { font-size: 12px; color: #909399; }
.progress { height: 8px; background: #f0f2f5; border-radius: 4px; overflow: hidden; }
.progress span { display: block; height: 100%; background: var(--primary, #1f6f78); transition: width .3s; }
.task-list { list-style: none; margin: 12px 0 0; padding: 0; display: grid; gap: 6px; max-height: 180px; overflow: auto; }
.task-item { display: grid; grid-template-columns: 120px minmax(0,1fr) 70px 70px 60px 64px; gap: 8px; align-items: center; padding: 8px 10px; border: 1px solid var(--border); border-radius: 6px; cursor: pointer; font-size: 13px; }
.task-action { display: flex; justify-content: flex-end; }
.task-action :deep(.el-button) { margin-left: 0; }
.task-item:hover { border-color: var(--primary, #1f6f78); }
.task-item.active { border-color: var(--primary, #1f6f78); background: rgba(31,111,120,0.06); }
.detail-line { display: grid; grid-template-columns: 56px minmax(0,1fr); gap: 8px; padding: 6px 0; border-bottom: 1px solid var(--border); }
.candidates { margin-top: 10px; padding-top: 10px; border-top: 1px dashed var(--border); display: grid; gap: 8px; max-height: 220px; overflow-y: auto; }
.item-table-wrap { max-height: 480px; overflow-y: auto; }
.candidates-title { font-size: 12px; color: var(--muted, #909399); font-weight: 700; }
.candidate { padding: 8px 10px; border: 1px solid var(--border, #ebeef5); border-radius: 6px; background: #f7f9fc; display: grid; gap: 4px; }
.cand-row { display: grid; grid-template-columns: 56px minmax(0,1fr); gap: 8px; font-size: 13px; }
.cand-row span { color: #909399; }
.queue { list-style: none; margin: 0; padding: 0; display: grid; gap: 6px; }
.actions { display: flex; align-items: center; gap: 8px; }
.tabs { display: flex; gap: 4px; }
@media (max-width: 1100px) { .analysis-grid { grid-template-columns: 1fr; } .form-grid { grid-template-columns: 1fr; } }
</style>
