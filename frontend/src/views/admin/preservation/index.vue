<template>
  <div class="preservation">
    <section>
      <h1 class="page-title">档案保存</h1>
      <p class="page-subtitle">管理数据库备份、电子文件备份和四性检测记录，形成可审计的保存管理记录。</p>
    </section>

    <section class="grid four" aria-label="保存概览">
      <div class="metric card"><div class="metric-label">最近数据库备份</div><div class="metric-num">{{ latestDbBackup }}</div><div class="metric-note">数据库元数据备份</div></div>
      <div class="metric card"><div class="metric-label">最近电子文件备份</div><div class="metric-num">{{ latestFileBackup }}</div><div class="metric-note">电子文件备份</div></div>
      <div class="metric card"><div class="metric-num">{{ passRate }}%</div><div class="metric-label">四性检测通过率</div><div class="metric-note">已归档电子文件检测</div></div>
      <div class="metric card"><div class="metric-num">{{ todayCheckCount }}</div><div class="metric-label">今日检测任务</div><div class="metric-note">检测记录</div></div>
    </section>

    <div class="preserve-layout">
      <section class="stack">
        <div class="card panel">
          <h2 class="section-title">备份范围</h2>
          <div v-if="loadError" class="detail-empty">备份任务加载失败：<button class="link" @click="loadAll">重试</button></div>
          <div class="backup-options">
            <label class="option-card" v-for="opt in scopeOptions" :key="opt.value">
              <input type="radio" name="backupScope" :value="opt.value" v-model="selectedScope" />
              <strong>{{ opt.label }}</strong>
              <span class="muted">{{ opt.desc }}</span>
              <span class="status" :class="opt.tagClass">{{ opt.label }}</span>
            </label>
          </div>
          <div class="toolbar" style="margin-top:12px">
            <div class="actions">
              <el-button type="primary" :loading="backupLoading" @click="runBackup">立即备份</el-button>
            </div>
          </div>
        </div>

        <div class="card panel">
          <h2 class="section-title">备份任务</h2>
          <div v-if="backupLoading && backupTasks.length === 0" class="detail-empty">加载中...</div>
          <div v-else-if="backupTasks.length === 0" class="detail-empty">暂无备份任务</div>
          <div v-else class="table-wrap">
            <table>
              <thead>
                <tr><th>任务号</th><th>范围</th><th>状态</th><th>路径</th><th>大小</th><th>完整性校验</th><th>说明</th></tr>
              </thead>
              <tbody>
                <tr v-for="t in backupTasks" :key="t.id">
                  <td class="mono">{{ t.taskNo }}</td>
                  <td>{{ BackupScopeLabel[t.backupScope] }}</td>
                  <td><span class="status" :class="statusClass(t.status)">{{ BackupTaskStatusLabel[t.status] }}</span></td>
                  <td class="mono">{{ t.backupPath }}</td>
                  <td>{{ t.fileSize ? formatSize(t.fileSize) : '-' }}</td>
                  <td>{{ t.sha256 ? '已校验' : '未校验' }}</td>
                  <td>{{ t.message }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="card panel">
          <h2 class="section-title">四性检测</h2>
          <div class="notice">检测结果仅作记录，不影响正式档案状态。</div>

          <div class="form-grid" style="grid-template-columns: 1fr auto; margin-top: 12px">
            <div class="field">
              <label for="checkArchiveNo">档案号</label>
              <input
                id="checkArchiveNo"
                v-model="archiveNoInput"
                placeholder="输入档案号，如 A-2024-0001"
                @keyup.enter="resolveArchive"
              />
            </div>
            <div class="field">
              <label>&nbsp;</label>
              <el-button :loading="resolving" @click="resolveArchive">解析档案</el-button>
            </div>
          </div>
          <div v-if="resolveError" class="notice warning" style="margin-top: 8px">{{ resolveError }}</div>

          <div v-if="resolvedArchive && resolvedArchive.files.length > 0" class="check-section">
            <div class="check-head">
              <strong>电子文件</strong>
              <span class="hint">{{ resolvedArchive.archiveNo }} · 共 {{ resolvedArchive.files.length }} 个，已选 {{ selectedFileIds.length }}</span>
            </div>
            <ul class="check-file-list">
              <li v-for="f in resolvedArchive.files" :key="f.id">
                <label>
                  <input type="checkbox" :checked="selectedFileIds.includes(f.id)" @change="toggleFile(f.id)" />
                  <span class="mono">{{ f.originalFilename }}</span>
                  <span class="muted">{{ f.mimeType }} · {{ formatSize(f.fileSize) }}</span>
                </label>
              </li>
            </ul>
          </div>

          <div class="check-section">
            <div class="check-head"><strong>检测项</strong></div>
            <div class="check-types">
              <label v-for="ct in allCheckTypes" :key="ct" class="check-type">
                <input type="checkbox" :checked="selectedCheckTypes.includes(ct)" @change="toggleCheckType(ct)" />
                <span>{{ CheckTypeLabel[ct] }}</span>
                <span class="muted">{{ ct === 'authenticity' ? '未开启' : checkDesc[ct] }}</span>
              </label>
            </div>
          </div>

          <div class="actions" style="margin-top: 12px">
            <el-button type="primary" :loading="checkLoading" @click="runCheck">执行四性检测</el-button>
          </div>

          <div v-if="checkResultGroups.length > 0" class="check-section" style="margin-top: 14px">
            <div class="check-head"><strong>本次检测结果</strong></div>
            <div v-for="g in checkResultGroups" :key="g.fileId" class="result-group">
              <div class="result-file mono">{{ g.filename }}</div>
              <div class="check-grid">
                <div class="check-card" v-for="r in g.results" :key="r.id">
                  <strong>{{ CheckTypeLabel[r.checkType] }}</strong>
                  <span class="status" :class="checkClass(r.checkResult)">{{ CheckResultLabel[r.checkResult] || r.checkResult }}</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </section>

      <aside class="stack">
        <div class="card panel">
          <h2 class="section-title">存储空间</h2>
          <ul class="timeline">
            <li><span>数据库备份</span><div>数据库备份正常，元数据已纳入例行备份。</div></li>
            <li><span>电子文件备份</span><div>电子文件备份正常，正式电子文件已纳入例行备份。</div></li>
            <li><span>备份目录</span><div>保留最近 6 次例行备份。</div></li>
          </ul>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createBackupTask, getBackupTasks, getFileCheckRecords, triggerFileCheck } from '@/api/preservation'
import { getArchives, getArchiveDetail } from '@/api/archive'
import type { BackupTask, FileCheckRecord } from '@/types/preservation'
import type { ArchiveDetail } from '@/types/archive'
import type { BackupScopeValue, BackupTaskStatusValue, CheckResultValue, CheckTypeValue } from '@/types/enums'
import { BackupScopeLabel, BackupTaskStatusLabel, CheckResultLabel, CheckTypeLabel } from '@/types/enums'

const backupTasks = ref<BackupTask[]>([])
const checkRecords = ref<FileCheckRecord[]>([])
const selectedScope = ref<BackupScopeValue>('database')
const backupLoading = ref(false)
const checkLoading = ref(false)
const loadError = ref(false)

// 四性检测：选检测对象（档号 → 电子文件）
const archiveNoInput = ref('')
const resolving = ref(false)
const resolveError = ref('')
const resolvedArchive = ref<ArchiveDetail | null>(null)
const selectedFileIds = ref<number[]>([])
const selectedCheckTypes = ref<CheckTypeValue[]>(['integrity', 'usability', 'security'])
const checkResultGroups = ref<Array<{ fileId: number; filename: string; results: FileCheckRecord[] }>>([])

const scopeOptions = [
  { value: 'database' as BackupScopeValue, label: '数据库备份', desc: 'PostgreSQL 元数据、清单、审批、日志', tagClass: '' },
  { value: 'files' as BackupScopeValue, label: '电子文件备份', desc: 'MinIO 正式电子文件、扫描件、附件', tagClass: 'info' },
  { value: 'both' as BackupScopeValue, label: '同时备份', desc: '数据库与电子文件任务统一记录', tagClass: 'success' },
]
const allCheckTypes: CheckTypeValue[] = ['integrity', 'usability', 'authenticity', 'security']
const checkDesc: Record<string, string> = {
  integrity: '完整性校验',
  usability: '可用性检测',
  authenticity: '真实性核验',
  security: '病毒扫描和格式限制',
}

const latestDbBackup = computed(() => {
  const t = backupTasks.value.find((x) => x.backupScope !== 'files' && x.status === 'success')
  return t ? t.finishedAt?.slice(11, 16) ?? '-' : '-'
})
const latestFileBackup = computed(() => {
  const t = backupTasks.value.find((x) => x.backupScope !== 'database' && x.status === 'success')
  return t ? t.finishedAt?.slice(11, 16) ?? '-' : '-'
})
const passRate = computed(() => {
  if (checkRecords.value.length === 0) return '-'
  const passed = checkRecords.value.filter((r) => r.checkResult === 'passed').length
  return ((passed / checkRecords.value.length) * 100).toFixed(1)
})
const todayCheckCount = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return checkRecords.value.filter((r) => r.checkedAt.slice(0, 10) === today).length
})

function statusClass(s: BackupTaskStatusValue): string {
  return s === 'success' ? 'success' : s === 'failed' ? 'danger' : 'warning'
}
function checkClass(r: CheckResultValue | ''): string {
  if (!r) return ''
  return r === 'passed' ? 'success' : r === 'failed' ? 'danger' : 'warning'
}
function formatSize(bytes: number): string {
  if (bytes >= 1e9) return (bytes / 1e9).toFixed(1) + ' GB'
  if (bytes >= 1e6) return (bytes / 1e6).toFixed(1) + ' MB'
  return bytes + ' B'
}

async function loadAll() {
  loadError.value = false
  backupLoading.value = true
  checkLoading.value = true
  try {
    const [tasks, recs] = await Promise.all([getBackupTasks(), getFileCheckRecords()])
    backupTasks.value = tasks.records
    checkRecords.value = recs.records
  } catch {
    loadError.value = true
    ElMessage.error('保存数据加载失败')
  } finally {
    backupLoading.value = false
    checkLoading.value = false
  }
}

async function runBackup() {
  backupLoading.value = true
  try {
    await createBackupTask({ backupScope: selectedScope.value })
    ElMessage.success('已创建备份任务，范围：' + BackupScopeLabel[selectedScope.value])
    const tasks = await getBackupTasks()
    backupTasks.value = tasks.records
    setTimeout(async () => {
      const refreshed = await getBackupTasks()
      backupTasks.value = refreshed.records
    }, 1400)
  } catch (e) {
    ElMessage.error((e as Error).message || '创建备份任务失败')
  } finally {
    backupLoading.value = false
  }
}

// 按档案号解析档案及其电子文件
async function resolveArchive() {
  resolveError.value = ''
  resolvedArchive.value = null
  selectedFileIds.value = []
  checkResultGroups.value = []
  const no = archiveNoInput.value.trim()
  if (!no) {
    resolveError.value = '请输入档案号'
    return
  }
  resolving.value = true
  try {
    const page = await getArchives({ keyword: no })
    const matched = page.records.find((r) => r.archiveNo === no) ?? page.records[0]
    if (!matched) {
      resolveError.value = `未找到档号 ${no} 的档案`
      return
    }
    const detail = await getArchiveDetail(matched.id)
    resolvedArchive.value = detail
    if (!detail.files || detail.files.length === 0) {
      resolveError.value = '该档案无电子文件，无需四性检测'
      return
    }
    // 默认全选电子文件
    selectedFileIds.value = detail.files.map((f) => f.id)
  } catch (e) {
    resolveError.value = (e as Error).message || '解析档案失败'
  } finally {
    resolving.value = false
  }
}

function toggleFile(id: number) {
  const i = selectedFileIds.value.indexOf(id)
  if (i >= 0) selectedFileIds.value.splice(i, 1)
  else selectedFileIds.value.push(id)
}

function toggleCheckType(ct: CheckTypeValue) {
  const i = selectedCheckTypes.value.indexOf(ct)
  if (i >= 0) selectedCheckTypes.value.splice(i, 1)
  else selectedCheckTypes.value.push(ct)
}

async function runCheck() {
  if (!resolvedArchive.value || selectedFileIds.value.length === 0) {
    ElMessage.warning('请先解析档案并选择至少一个电子文件')
    return
  }
  if (selectedCheckTypes.value.length === 0) {
    ElMessage.warning('请至少选择一个检测项')
    return
  }
  checkLoading.value = true
  try {
    const groups: Array<{ fileId: number; filename: string; results: FileCheckRecord[] }> = []
    for (const fid of selectedFileIds.value) {
      const created = await triggerFileCheck(fid, { checkTypes: selectedCheckTypes.value })
      const file = resolvedArchive.value.files.find((f) => f.id === fid)
      groups.push({ fileId: fid, filename: file?.originalFilename ?? `file-${fid}`, results: created })
    }
    checkResultGroups.value = groups
    // 刷新历史检测记录，更新通过率/今日检测数
    const recs = await getFileCheckRecords()
    checkRecords.value = recs.records
    ElMessage.success(`已完成 ${selectedFileIds.value.length} 个文件的四性检测`)
  } catch (e) {
    ElMessage.error((e as Error).message || '四性检测执行失败')
  } finally {
    checkLoading.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.preservation { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; }
.page-subtitle { color: #909399; margin-top: 6px; }
.grid.four { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 16px; }
.metric.card { padding: 14px; }
.metric-num { font-size: 22px; font-weight: 800; color: var(--primary, #1f6f78); }
.metric-label { font-size: 13px; color: #606266; }
.metric-note { font-size: 11px; color: #909399; margin-top: 4px; }
.preserve-layout { display: grid; grid-template-columns: minmax(0, 1fr) 380px; gap: 16px; align-items: start; margin-top: 16px; }
.stack { display: grid; gap: 16px; }
.backup-options { display: grid; grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
.option-card { position: relative; display: grid; gap: 8px; min-height: 106px; padding: 12px; border: 1px solid var(--border, #e4e7ed); border-radius: 8px; background: #fff; }
.option-card input { position: absolute; top: 12px; right: 12px; width: 18px; height: 18px; }
.check-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; }
.check-card { display: grid; gap: 8px; padding: 12px; border: 1px solid var(--border, #e4e7ed); border-radius: 8px; background: #fff; }
.check-section { margin-top: 14px; }
.check-head { display: flex; flex-wrap: wrap; gap: 8px; align-items: center; justify-content: space-between; margin-bottom: 8px; }
.check-file-list { list-style: none; margin: 0; padding: 0; display: grid; gap: 6px; }
.check-file-list label { display: flex; gap: 8px; align-items: center; padding: 8px 10px; border: 1px solid var(--border, #e4e7ed); border-radius: 6px; background: #fff; }
.check-file-list .muted { margin-left: auto; }
.check-types { display: flex; flex-wrap: wrap; gap: 8px; }
.check-type { display: flex; gap: 6px; align-items: center; padding: 6px 10px; border: 1px solid var(--border, #e4e7ed); border-radius: 6px; background: #f8fbfc; font-size: 13px; }
.check-type .muted { font-size: 12px; }
.result-group { padding: 10px; border: 1px solid var(--border, #e4e7ed); border-radius: 6px; margin-top: 8px; }
.result-file { font-weight: 700; margin-bottom: 8px; }
.detail-empty { color: #909399; padding: 16px; text-align: center; }
.link { background: none; border: none; color: var(--primary, #1f6f78); cursor: pointer; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }
.muted { color: #909399; font-size: 12px; }
.timeline { list-style: none; margin: 0; padding: 0; display: grid; gap: 10px; }
.timeline li { padding: 8px 0; border-bottom: 1px solid var(--border, #e4e7ed); }
.timeline li span { font-weight: 700; display: block; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 10px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; gap: 8px; }
.actions { display: flex; gap: 8px; }
@media (max-width: 1120px) { .preserve-layout { grid-template-columns: 1fr; } }
</style>
