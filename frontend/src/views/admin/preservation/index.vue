<template>
  <div class="preservation">
    <section>
      <h1 class="page-title">档案保存</h1>
      <p class="page-subtitle">管理数据库备份、电子文件备份和四性检测记录，形成可审计的保存管理记录。</p>
    </section>

    <section class="grid four" aria-label="保存概览">
      <div class="metric card"><div class="metric-label">最近数据库备份</div><div class="metric-num">{{ latestDbBackup }}</div><div class="metric-note">backup_scope = database</div></div>
      <div class="metric card"><div class="metric-label">最近电子文件备份</div><div class="metric-num">{{ latestFileBackup }}</div><div class="metric-note">backup_scope = files</div></div>
      <div class="metric card"><div class="metric-num">{{ passRate }}%</div><div class="metric-label">四性检测通过率</div><div class="metric-note">archive_file 检测记录</div></div>
      <div class="metric card"><div class="metric-num">{{ todayCheckCount }}</div><div class="metric-label">今日检测任务</div><div class="metric-note">file_check_records</div></div>
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
              <span class="status" :class="opt.tagClass">{{ opt.value }}</span>
            </label>
          </div>
          <div class="toolbar" style="margin-top:12px">
            <div class="actions">
              <el-button type="primary" :loading="backupLoading" @click="runBackup">立即备份</el-button>
              <el-button :loading="checkLoading" @click="runCheck">执行四性检测</el-button>
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
                <tr><th>任务号</th><th>范围</th><th>状态</th><th>路径</th><th>大小</th><th>校验哈希</th><th>说明</th></tr>
              </thead>
              <tbody>
                <tr v-for="t in backupTasks" :key="t.id">
                  <td class="mono">{{ t.taskNo }}</td>
                  <td>{{ BackupScopeLabel[t.backupScope] }}</td>
                  <td><span class="status" :class="statusClass(t.status)">{{ BackupTaskStatusLabel[t.status] }}</span></td>
                  <td class="mono">{{ t.backupPath }}</td>
                  <td>{{ t.fileSize ? formatSize(t.fileSize) : '-' }}</td>
                  <td class="mono">{{ t.sha256 ? t.sha256.slice(0, 4) + '...' + t.sha256.slice(-4) : '-' }}</td>
                  <td>{{ t.message }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <div class="card panel">
          <h2 class="section-title">四性检测记录</h2>
          <div v-if="checkLoading && checkRecords.length === 0" class="detail-empty">加载中...</div>
          <div v-else-if="checkRecords.length === 0" class="detail-empty">暂无检测记录</div>
          <div v-else class="check-grid">
            <div class="check-card" v-for="ct in checkTypes" :key="ct">
              <strong>{{ CheckTypeLabel[ct] }}</strong>
              <span class="muted">{{ checkDesc[ct] }}</span>
              <span class="status" :class="checkClass(latestResult(ct))">{{ latestResult(ct) || '-' }}</span>
            </div>
          </div>
          <div class="notice" style="margin-top:12px">
            四性检测写入 <span class="mono">file_check_records</span>，检测失败只形成记录和提示，不直接修改正式档案状态。
          </div>
        </div>
      </section>

      <aside class="stack">
        <div class="card panel">
          <h2 class="section-title">存储空间</h2>
          <ul class="timeline">
            <li><span>MinIO</span><div>已用 742 GB / 1 TB，正式文件和备份分桶存储。</div></li>
            <li><span>PostgreSQL</span><div>元数据 18.6 GB，审计日志 4.2 GB。</div></li>
            <li><span>备份目录</span><div>保留最近 6 次例行备份，路径与哈希入库。</div></li>
          </ul>
        </div>
        <div class="notice warning">
          <strong>保存边界</strong>
          <div>本页面不做跨机房容灾编排，不自动恢复档案，也不替代鉴定、销毁或审批流程。</div>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createBackupTask, getBackupTasks, getFileCheckRecords, triggerFileCheck } from '@/api/preservation'
import type { BackupTask, FileCheckRecord } from '@/types/preservation'
import type { BackupScopeValue, BackupTaskStatusValue, CheckResultValue, CheckTypeValue } from '@/types/enums'
import { BackupScopeLabel, BackupTaskStatusLabel, CheckTypeLabel } from '@/types/enums'

const backupTasks = ref<BackupTask[]>([])
const checkRecords = ref<FileCheckRecord[]>([])
const selectedScope = ref<BackupScopeValue>('database')
const backupLoading = ref(false)
const checkLoading = ref(false)
const loadError = ref(false)

const scopeOptions = [
  { value: 'database' as BackupScopeValue, label: '数据库备份', desc: 'PostgreSQL 元数据、清单、审批、日志', tagClass: '' },
  { value: 'files' as BackupScopeValue, label: '电子文件备份', desc: 'MinIO 正式电子文件、扫描件、附件', tagClass: 'info' },
  { value: 'both' as BackupScopeValue, label: '同时备份', desc: '数据库与电子文件任务统一记录', tagClass: 'success' },
]
const checkTypes: CheckTypeValue[] = ['integrity', 'usability', 'authenticity', 'security']
const checkDesc: Record<string, string> = {
  integrity: '校验 SHA-256 哈希',
  usability: '格式白名单与基础打开检测',
  authenticity: '外部签名体系验签记录',
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
  const today = '2026-06-15'
  return checkRecords.value.filter((r) => r.checkedAt.slice(0, 10) === today).length
})

function latestResult(ct: CheckTypeValue): CheckResultValue | '' {
  const rec = checkRecords.value.find((r) => r.checkType === ct)
  return rec ? rec.checkResult : ''
}
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

async function runCheck() {
  checkLoading.value = true
  try {
    const created = await triggerFileCheck(5001, { checkTypes: checkTypes })
    checkRecords.value = created
    ElMessage.success('四性检测记录已生成；真实性未配置时记录为 not_configured。')
  } catch {
    ElMessage.error('四性检测执行失败')
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
