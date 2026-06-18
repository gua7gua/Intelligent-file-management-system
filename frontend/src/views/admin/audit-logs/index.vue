<template>
  <div class="audit-logs">
    <section>
      <h1 class="page-title">审计日志</h1>
      <p class="page-subtitle">查询系统操作审计记录，支持按操作人、模块、类型与时间筛选并导出。</p>
    </section>

    <div class="card panel">
      <h2 class="section-title">筛选条件</h2>
      <div class="form-grid">
        <div class="field">
          <label>操作人类型</label>
          <select v-model="filters.actorType" data-testid="actorType">
            <option value="">全部</option>
            <option value="internal">内部用户</option>
            <option value="public">公众</option>
            <option value="system">系统</option>
          </select>
        </div>
        <div class="field"><label>模块</label><input v-model="filters.moduleName" placeholder="如 用户管理" /></div>
        <div class="field"><label>操作类型</label><input v-model="filters.operationType" placeholder="如 禁用账号" /></div>
        <div class="field"><label>业务类型</label><input v-model="filters.businessType" placeholder="如 用户" /></div>
        <div class="field"><label>开始时间</label><input type="date" v-model="filters.startedAt" /></div>
        <div class="field"><label>结束时间</label><input type="date" v-model="filters.endedAt" /></div>
      </div>
      <div class="actions" style="margin-top:12px">
        <el-button type="primary" @click="load()">查询</el-button>
        <el-button @click="reset">重置</el-button>
        <el-button :disabled="records.length === 0" @click="exportLogs">导出</el-button>
      </div>
    </div>

    <div class="card panel">
      <h2 class="section-title">审计记录</h2>
      <div v-if="loading && records.length === 0" class="detail-empty">加载中...</div>
      <div v-else-if="loadError" class="detail-empty">加载失败：<button class="link" @click="load()">重试</button></div>
      <div v-else-if="records.length === 0" class="detail-empty">暂无审计日志</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr><th>操作时间</th><th>操作人</th><th>类型</th><th>模块</th><th>操作类型</th><th>业务类型</th><th>关联对象</th><th>IP</th><th>详情</th></tr>
          </thead>
          <tbody>
            <tr v-for="l in records" :key="l.id">
              <td class="mono">{{ l.operatedAt }}</td>
              <td>{{ l.actorName || `用户#${l.actorUserId}` }}</td>
              <td>{{ actorTypeLabel[l.actorType] || l.actorType }}</td>
              <td>{{ l.moduleLabel || l.moduleName }}</td>
              <td>{{ operationTypeLabel[l.operationType] || l.operationType }}</td>
              <td>{{ businessTypeLabel[l.businessType || ''] || l.businessType || '-' }}</td>
              <td>{{ l.archiveNo || (l.businessId != null ? `#${l.businessId}` : '-') }}</td>
              <td class="mono">{{ l.ipAddress || '-' }}</td>
              <td>
                <details v-if="l.detail">
                  <summary class="link">{{ detailSummary(l.detail) }}</summary>
                  <pre class="detail-pre">{{ l.detail }}</pre>
                </details>
                <span v-else>-</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      <div v-if="hasNext" class="more-wrap">
        <el-button :loading="loadingMore" @click="loadMore">加载更多</el-button>
      </div>
      <div v-else-if="records.length > 0" class="detail-empty">已加载全部</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAuditLogs } from '@/api/audit-log'
import { downloadCsv } from '@/utils/csvExport'
import type { ActorType, AuditLog, AuditLogQuery } from '@/types/audit-log'

const records = ref<AuditLog[]>([])
const nextCursor = ref<string | null>(null)
const hasNext = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const loadError = ref(false)

const filters = reactive({
  actorType: '' as ActorType | '',
  moduleName: '',
  operationType: '',
  businessType: '',
  startedAt: '',
  endedAt: '',
})

const actorTypeLabel: Record<ActorType, string> = {
  internal: '内部用户',
  public: '公众',
  system: '系统',
}

const operationTypeLabel: Record<string, string> = {
  issue_voucher: '发放凭证',
  generate_compilation: '生成编研',
  upload: '上传',
  destroy: '销毁',
  upload_destruction_photo: '上传销毁照片',
}

const businessTypeLabel: Record<string, string> = {
  borrow_request: '借阅',
  compilation: '编研',
  staging_file: '暂存文件',
  destruction_list: '销毁清册',
}

// 将详情对象转为简短摘要，避免直接暴露原始 JSON
function detailSummary(detail: unknown): string {
  if (!detail) return '-'
  if (typeof detail === 'string') return detail
  try {
    const obj = detail as Record<string, unknown>
    const summary = obj.summary ?? obj.message ?? obj.operation ?? obj.action
    if (typeof summary === 'string') return summary
    const entries = Object.entries(obj)
    if (entries.length === 0) return '-'
    return entries.slice(0, 2).map(([k, v]) => `${k}: ${typeof v === 'object' ? '…' : String(v)}`).join('；')
  } catch {
    return '详情'
  }
}

function buildQuery(cursor?: string): AuditLogQuery {
  return {
    actorType: filters.actorType || undefined,
    moduleName: filters.moduleName.trim() || undefined,
    operationType: filters.operationType.trim() || undefined,
    businessType: filters.businessType.trim() || undefined,
    startedAt: filters.startedAt ? filters.startedAt + 'T00:00:00+08:00' : undefined,
    endedAt: filters.endedAt ? filters.endedAt + 'T23:59:59+08:00' : undefined,
    cursor,
    limit: 5,
  }
}

async function load() {
  if (filters.startedAt && filters.endedAt && filters.startedAt > filters.endedAt) {
    ElMessage.error('开始时间不能晚于结束时间')
    return
  }
  loading.value = true
  loadError.value = false
  try {
    const res = await getAuditLogs(buildQuery())
    records.value = res.records
    nextCursor.value = res.nextCursor
    hasNext.value = res.hasNext
  } catch {
    loadError.value = true
    ElMessage.error('审计日志加载失败')
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (!nextCursor.value) return
  loadingMore.value = true
  try {
    const res = await getAuditLogs(buildQuery(nextCursor.value))
    records.value = [...records.value, ...res.records]
    nextCursor.value = res.nextCursor
    hasNext.value = res.hasNext
  } catch {
    ElMessage.error('加载更多失败')
  } finally {
    loadingMore.value = false
  }
}

function reset() {
  filters.actorType = ''
  filters.moduleName = ''
  filters.operationType = ''
  filters.businessType = ''
  filters.startedAt = ''
  filters.endedAt = ''
  load()
}

function exportLogs() {
  const rows = records.value.map((l) => ({
    operatedAt: l.operatedAt,
    actor: l.actorName || `用户#${l.actorUserId}`,
    actorType: actorTypeLabel[l.actorType] || l.actorType,
    moduleName: l.moduleLabel || l.moduleName,
    operationType: l.operationType,
    businessType: l.businessType ?? '',
    businessRef: l.archiveNo || (l.businessId != null ? `#${l.businessId}` : ''),
    ipAddress: l.ipAddress ?? '',
    detail: detailSummary(l.detail),
  }))
  downloadCsv('审计日志.csv', rows)
  ElMessage.success('已导出当前已加载的审计日志。')
}

onMounted(() => load())
</script>

<style scoped>
.audit-logs { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; }
.page-subtitle { color: #909399; margin-top: 6px; }
.card.panel { margin-top: 16px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 10px; }
.form-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 10px; }
.field { display: grid; gap: 4px; }
.field label { font-size: 12px; color: #909399; }
.field input, .field select { height: 34px; padding: 0 8px; border: 1px solid var(--border, #e4e7ed); border-radius: 6px; }
.actions { display: flex; gap: 8px; align-items: center; }
.table-wrap { overflow-x: auto; }
.more-wrap { text-align: center; padding: 12px 0; }
.detail-empty { color: #909399; padding: 16px; text-align: center; }
.link { background: none; border: none; color: var(--primary, #1f6f78); cursor: pointer; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }
details > summary { list-style: none; cursor: pointer; }
details > summary::-webkit-details-marker { display: none; }
.detail-pre { margin: 6px 0 0; padding: 8px; max-width: 320px; max-height: 160px; overflow: auto; background: #f6f8f8; border: 1px solid var(--border, #e4e7ed); border-radius: 6px; font-size: 12px; white-space: pre-wrap; word-break: break-all; }
@media (max-width: 900px) { .form-grid { grid-template-columns: 1fr 1fr; } }
</style>
