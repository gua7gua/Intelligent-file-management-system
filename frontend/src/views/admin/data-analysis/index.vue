<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createAnalysisTask, getAnalysisTaskDetail, getAnalysisTasks, handleAnalysisItem,
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
const adoptedQueue = computed(() => current.value?.items.filter((i) => i.status === 'adopted') ?? [])

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const page = await getAnalysisTasks({ pageSize: 50 })
    tasks.value = page.records
    if (tasks.value.length) await selectTask(tasks.value[0].id)
  } catch (e) {
    errorMsg.value = (e as Error).message || '研判任务加载失败'
  } finally {
    loading.value = false
  }
}

async function selectTask(id: number) {
  try {
    current.value = await getAnalysisTaskDetail(id)
    selectedItem.value = current.value.items[0] ?? null
  } catch (e) {
    ElMessage.warning((e as Error).message)
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
    ElMessage.success(action === 'adopted' ? '已采纳，加入待处理队列' : '已不采纳')
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

function generateList() {
  if (!adoptedQueue.value.length) {
    ElMessage.warning('请先采纳建议，再生成待补充清单')
    return
  }
  ElMessage.success(`已生成待补充清单（${adoptedQueue.value.length} 条），请前往档案管理页人工处理`)
}

onMounted(load)
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
        <button class="button" id="makeQueue" @click="generateList"><span class="icon">Q</span>生成待补充清单</button>
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
            </li>
          </ul>
        </div>

        <div class="card panel">
          <h2 class="section-title">异常建议列表</h2>
          <div class="table-wrap">
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
                  v-for="i in current?.items ?? []"
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
                <tr v-if="!current || !current.items.length">
                  <td colspan="6" class="muted" style="text-align:center;padding:16px;">暂无异常建议。</td>
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
            <div class="actions" style="margin-top:12px">
              <button class="button secondary" :disabled="selectedItem.status !== 'pending'" @click="onAdopt">采纳</button>
              <button class="button ghost" :disabled="selectedItem.status !== 'pending'" @click="onReject">不采纳</button>
              <span class="status">{{ AnalysisItemStatusLabel[selectedItem.status] }}</span>
            </div>
          </template>
          <p v-else class="muted">请从左侧列表选择一条异常建议。</p>
        </div>

        <div class="card panel">
          <h2 class="section-title">待处理队列</h2>
          <ul class="queue">
            <li v-for="q in adoptedQueue" :key="q.id"><span class="mono">{{ q.archiveNo }}</span> {{ q.title }}</li>
            <li v-if="!adoptedQueue.length" class="muted">暂无已采纳建议。</li>
          </ul>
        </div>

        <div class="notice">
          <strong>处理边界</strong>
          <div>采纳或不采纳只更新建议状态；正式档案字段需在档案管理页人工确认。</div>
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
.field select { padding: 6px 8px; }
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
.task-item { display: grid; grid-template-columns: 120px minmax(0,1fr) 70px 70px 60px; gap: 8px; align-items: center; padding: 8px 10px; border: 1px solid var(--border); border-radius: 6px; cursor: pointer; font-size: 13px; }
.task-item:hover { border-color: var(--primary, #1f6f78); }
.task-item.active { border-color: var(--primary, #1f6f78); background: rgba(31,111,120,0.06); }
.detail-line { display: grid; grid-template-columns: 56px minmax(0,1fr); gap: 8px; padding: 6px 0; border-bottom: 1px solid var(--border); }
.queue { list-style: none; margin: 0; padding: 0; display: grid; gap: 6px; }
.actions { display: flex; align-items: center; gap: 8px; }
.tabs { display: flex; gap: 4px; }
@media (max-width: 1100px) { .analysis-grid { grid-template-columns: 1fr; } .form-grid { grid-template-columns: 1fr; } }
</style>
