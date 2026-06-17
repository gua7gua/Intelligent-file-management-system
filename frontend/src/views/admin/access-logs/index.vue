<template>
  <div class="access-logs">
    <section>
      <h1 class="page-title">访问日志</h1>
      <p class="page-subtitle">查询档案查阅、预览、下载记录，支持按用户、档案、访问类型与时间筛选并导出。</p>
    </section>

    <div class="card panel">
      <h2 class="section-title">筛选条件</h2>
      <div class="form-grid">
        <div class="field">
          <label>访问类型</label>
          <select v-model="filters.accessType" data-testid="accessType">
            <option value="">全部</option>
            <option value="view_metadata">查看元数据</option>
            <option value="preview">预览</option>
            <option value="download">下载</option>
          </select>
        </div>
        <div class="field"><label>用户</label><input v-model.number="filters.userId" type="number" placeholder="留空查全部" /></div>
        <div class="field"><label>档案</label><input v-model.number="filters.archiveId" type="number" placeholder="留空查全部" /></div>
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
      <h2 class="section-title">访问记录</h2>
      <div v-if="loading && records.length === 0" class="detail-empty">加载中...</div>
      <div v-else-if="loadError" class="detail-empty">加载失败：<button class="link" @click="load()">重试</button></div>
      <div v-else-if="records.length === 0" class="detail-empty">暂无访问日志</div>
      <div v-else class="table-wrap">
        <table>
          <thead>
            <tr><th>访问时间</th><th>用户</th><th>类型</th><th>档案</th><th>访问类型</th><th>IP</th></tr>
          </thead>
          <tbody>
            <tr v-for="l in records" :key="l.id">
              <td class="mono">{{ l.accessedAt }}</td>
              <td>{{ l.userName || (l.userId ? `用户#${l.userId}` : '匿名') }}</td>
              <td>{{ l.userType }}</td>
              <td>{{ l.archiveNo || `档案#${l.archiveId}` }}</td>
              <td>{{ accessTypeLabel[l.accessType] || l.accessType }}</td>
              <td class="mono">{{ l.ipAddress || '-' }}</td>
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
import { getAccessLogs } from '@/api/access-log'
import { downloadCsv } from '@/utils/csvExport'
import type { AccessLogQuery, AccessType, ArchiveAccessLog } from '@/types/access-log'

const records = ref<ArchiveAccessLog[]>([])
const nextCursor = ref<string | null>(null)
const hasNext = ref(false)
const loading = ref(false)
const loadingMore = ref(false)
const loadError = ref(false)

const filters = reactive({
  accessType: '' as AccessType | '',
  userId: undefined as number | undefined,
  archiveId: undefined as number | undefined,
  startedAt: '',
  endedAt: '',
})

const accessTypeLabel: Record<AccessType, string> = {
  view_metadata: '查看元数据',
  preview: '预览',
  download: '下载',
}

function buildQuery(cursor?: string): AccessLogQuery {
  return {
    accessType: filters.accessType || undefined,
    userId: filters.userId || undefined,
    archiveId: filters.archiveId || undefined,
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
    const res = await getAccessLogs(buildQuery())
    records.value = res.records
    nextCursor.value = res.nextCursor
    hasNext.value = res.hasNext
  } catch {
    loadError.value = true
    ElMessage.error('访问日志加载失败')
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  if (!nextCursor.value) return
  loadingMore.value = true
  try {
    const res = await getAccessLogs(buildQuery(nextCursor.value))
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
  filters.accessType = ''
  filters.userId = undefined
  filters.archiveId = undefined
  filters.startedAt = ''
  filters.endedAt = ''
  load()
}

function exportLogs() {
  const rows = records.value.map((l) => ({
    accessedAt: l.accessedAt,
    user: l.userName || (l.userId ? `用户#${l.userId}` : '匿名'),
    userType: l.userType,
    archive: l.archiveNo || `档案#${l.archiveId}`,
    accessType: accessTypeLabel[l.accessType] || l.accessType,
    ipAddress: l.ipAddress ?? '',
  }))
  downloadCsv('访问日志.csv', rows)
  ElMessage.success('已导出当前已加载的访问日志。')
}

onMounted(() => load())
</script>

<style scoped>
.access-logs { padding: 0; }
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
@media (max-width: 900px) { .form-grid { grid-template-columns: 1fr 1fr; } }
</style>
