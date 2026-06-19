<template>
  <section>
    <h1 class="page-title">征集管理与接收</h1>
    <p class="page-subtitle">处理公众征集清单的联系判断、到馆接收、在线协议记录校验和回执导出。</p>
  </section>

  <!-- 概览指标 -->
  <section class="grid four" aria-label="征集待办统计">
    <div v-if="!isFrontArchivist" class="metric">
      <span class="label">待联系</span>
      <span class="value">{{ contactCount }}</span>
      <span class="note">后台判断征集方向</span>
    </div>
    <div class="metric">
      <span class="label">待接收</span>
      <span class="value">{{ receiveCount }}</span>
      <span class="note">已约定到馆</span>
    </div>
    <div class="metric">
      <span class="label">今日到馆</span>
      <span class="value">{{ todayArrivalCount }}</span>
      <span class="note">需前台验收</span>
    </div>
    <div class="metric">
      <span class="label">协议记录异常</span>
      <span class="value">{{ agreementErrorCount }}</span>
      <span class="note">提交时在线同意</span>
    </div>
  </section>

  <!-- 主布局 -->
  <section class="work-layout" style="margin-top: 16px;">
    <!-- 左侧：批次列表 -->
    <div class="card panel scroll-pane batch-pane">
      <div class="toolbar">
        <div>
          <h2 class="section-title">征集批次</h2>
          <p class="page-subtitle">清单来源为社会征集，提交后公众端只读。</p>
        </div>
        <div class="tabs" aria-label="批次状态筛选">
          <button
            v-for="tab in collectionTabs"
            :key="tab.key"
            class="tab"
            :class="{ active: activeTab === tab.key }"
            type="button"
            @click="activeTab = tab.key"
          >
            {{ tab.label }}
          </button>
        </div>
      </div>

      <div v-if="loading" style="padding: 20px; text-align: center; color: var(--muted);">加载中...</div>
      <div v-else-if="filteredCollections.length === 0" class="empty" style="padding: 20px; text-align: center; color: var(--muted);">
        当前筛选下没有征集批次。
      </div>
      <ul v-else class="batch-list">
        <li
          v-for="batch in filteredCollections"
          :key="batch.id"
          class="batch-item"
          :class="{ active: selectedBatch?.id === batch.id }"
          @click="selectBatch(batch)"
        >
          <div class="batch-top">
            <h3 class="batch-title">{{ batch.title }}</h3>
            <span class="status" :class="statusClass(batch.status)">{{ batch.statusText }}</span>
          </div>
          <div class="meta-line">
            <span>{{ batch.batchNo }}</span>
            <span>捐赠人：{{ batch.contactName }}</span>
            <span>条目：{{ batch.itemCount }}</span>
            <span v-if="batch.scheduledReceiveAt">到馆：{{ formatDateTime(batch.scheduledReceiveAt) }}</span>
            <span v-else>提交：{{ batch.submittedAt ? batch.submittedAt.slice(0, 10) : '—' }}</span>
          </div>
        </li>
      </ul>
    </div>

    <!-- 右侧：详情区 -->
    <div v-if="!selectedBatch" class="grid scroll-pane detail-pane">
      <div class="card panel" style="text-align: center; padding: 40px; color: var(--muted);">
        请在左侧选择一个征集批次
      </div>
    </div>

    <div v-else class="grid scroll-pane detail-pane">
      <!-- 基本信息 -->
      <div class="card panel">
        <div class="toolbar">
          <div>
            <h2 class="section-title">{{ selectedBatch.title }}</h2>
            <p class="page-subtitle">
              {{ collectionHeader(selectedBatch) }}<template v-if="batchPhaseLabel"> · {{ batchPhaseLabel }}</template>
            </p>
          </div>
          <span class="status" :class="statusClass(selectedBatch.status)">{{ selectedBatch.statusText }}</span>
        </div>
        <div class="detail-grid">
          <div class="info-tile"><span>联系电话</span><strong>{{ selectedBatch.contactPhone }}</strong></div>
          <div class="info-tile"><span>来源标识</span><strong>征集</strong></div>
          <div class="info-tile"><span>目标属性</span><strong>永久 / 非密 / 公开</strong></div>
          <div class="info-tile"><span>公开生效</span><strong>正式入库并满足利用条件后</strong></div>
        </div>
      </div>

      <!-- 后台联系判断 -->
      <div v-if="isBackArchivist && selectedBatch.status === 'pending_contact'" class="card panel">
        <h2 class="section-title">联系判断</h2>
        <div class="form-grid">
          <div class="field">
            <label>约定到馆时间</label>
            <input v-model="scheduleForm.scheduledReceiveAt" type="datetime-local">
          </div>
          <div class="field">
            <label>联系结果</label>
            <select v-model="scheduleForm.contactResult">
              <option value="符合征集方向">符合征集方向</option>
              <option value="需补充说明">需补充说明</option>
              <option value="不符合征集方向">不符合征集方向</option>
            </select>
          </div>
          <div class="field">
            <label>拒绝或备注</label>
            <input v-model="rejectForm.rejectReason" placeholder="拒绝时必填">
          </div>
        </div>
        <div class="actions" style="margin-top: 12px;">
          <button class="button" type="button" @click="handleSchedule">约定到馆</button>
          <button class="button danger" type="button" @click="handleReject">拒绝征集</button>
        </div>
      </div>

      <!-- 到馆接收区 -->
      <div v-if="selectedBatch.status === 'pending_receive' || selectedBatch.status === 'received' || selectedBatch.status === 'partially_received'" class="card panel">
        <h2 class="section-title">到馆接收</h2>
        <div class="form-grid">
          <div class="info-tile">
            <span>在线协议</span>
            <strong v-if="selectedBatch.agreementAcceptedAt">已同意 · {{ formatDateTime(selectedBatch.agreementAcceptedAt) }}</strong>
            <strong v-else style="color: var(--danger);">未同意 — 不可完成接收</strong>
          </div>
          <div class="info-tile">
            <span>协议文案</span>
            <strong>在线捐赠协议</strong>
          </div>
        </div>

        <!-- 前台操作区 -->
        <template v-if="isFrontArchivist && selectedBatch.status === 'pending_receive'">
          <details class="collapsible upload-block" :open="stagingFiles.length > 0">
            <summary>
              <span class="section-title">U 盘电子文件上传与匹配</span>
              <span class="status info">{{ stagingFiles.length > 0 ? '已上传' : '点击展开' }}</span>
            </summary>
            <div class="collapsible-body">
              <div
                class="drop-zone"
                :class="{ dragover: isDragOver }"
                @dragover.prevent="isDragOver = true"
                @dragleave="isDragOver = false"
                @drop.prevent="handleDrop"
              >
                <div>
                  <strong>拖拽捐赠电子文件或选择文件</strong>
                  <p class="muted" style="margin: 6px 0 0;">
                    上传后将自动与清单条目匹配；回退条目会保留记录。
                  </p>
                </div>
                <label class="button secondary" for="collectionFilePicker">选择 U 盘文件</label>
                <input id="collectionFilePicker" class="sr-only" type="file" multiple @change="handleFilePick">
              </div>
              <div v-if="uploading" style="text-align: center; padding: 12px; color: var(--muted);">文件上传中...</div>
              <div v-if="stagingFiles.length > 0" class="match-grid">
                <div v-for="file in stagingFiles" :key="file.fileId" class="match-card">
                  <div>
                    <strong>{{ file.originalFilename }}</strong>
                    <div class="hint">{{ fileScanHint(file) }}</div>
                  </div>
                  <span class="status" :class="matchStatusClass(file.matchStatus)">{{ matchStatusLabel(file.matchStatus) }}</span>
                </div>
              </div>
            </div>
          </details>

          <!-- 条目验收表格 -->
          <div class="table-wrap scroll-y" style="margin-top: 14px;">
            <table class="review-table">
              <thead>
                <tr>
                  <th>条目</th>
                  <th>载体</th>
                  <th>验收结果</th>
                  <th>回退原因</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="item in receptionItems" :key="item.id">
                  <td>{{ item.title }}</td>
                  <td>{{ carrierStatusLabel(item.carrierStatus) }}</td>
                  <td>
                    <select v-model="item.result">
                      <option value="pending">待验收</option>
                      <option value="accepted">已接收</option>
                      <option value="rejected">已回退</option>
                    </select>
                  </td>
                  <td><input v-model="item.acceptanceNote" placeholder="回退时填写"></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div class="actions" style="margin-top: 12px;">
            <button class="button" type="button" @click="handleCompleteReceive">完成接收</button>
            <button class="button secondary" type="button" :disabled="pendingItemsCount > 0" @click="handleExportReceipt">导出回执</button>
          </div>
        </template>

        <!-- 已接收/部分接收后保留导出回执入口（B4-6） -->
        <div
          v-if="isFrontArchivist && (selectedBatch.status === 'received' || selectedBatch.status === 'partially_received')"
          class="actions"
          style="margin-top: 12px;"
        >
          <button class="button secondary" type="button" @click="handleExportReceipt">导出回执</button>
        </div>
      </div>

      <!-- 流程位置 -->
      <div class="card panel">
        <h2 class="section-title">流程位置</h2>
        <ol class="flow-note">
          <li>
            <span class="flow-no">1</span>
            <span><strong>公众提交</strong><br><span class="muted">清单进入待联系，公众端只读。</span></span>
          </li>
          <li>
            <span class="flow-no">2</span>
            <span><strong>后台联系</strong><br><span class="muted">符合征集方向后约定到馆，或填写理由拒绝。</span></span>
          </li>
          <li>
            <span class="flow-no">3</span>
            <span><strong>前台接收</strong><br><span class="muted">核对实物和电子介质，校验在线协议记录并导出回执。</span></span>
          </li>
          <li>
            <span class="flow-no">4</span>
            <span><strong>后续入库</strong><br><span class="muted">已接收条目进入 AI 补全、人工核对、装盒、入库、上架。</span></span>
          </li>
        </ol>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { CollectionBatch, ScheduleData } from '@/types/collection'
import type { BatchDetail, StagingFile } from '@/types/reception'
import { usePermission } from '@/composables/usePermission'
import { getCollections, scheduleCollection, rejectCollection } from '@/api/collection'
import {
  getReceptionBatchDetail,
  uploadStagingFiles,
  updateItemAcceptance,
  completeBatchAcceptance,
  exportReceipt,
} from '@/api/reception'

const { hasRole } = usePermission()
const isBackArchivist = computed(() => hasRole('back_archivist'))
const isFrontArchivist = computed(() => hasRole('front_archivist'))

// ── 筛选 ──
// R3-D4：前台无 pending_contact 处理权（filteredCollections 已过滤该状态），隐藏「待联系」tab 避免点进去空白
const collectionTabs = computed(() =>
  isFrontArchivist.value
    ? [
        { key: 'all', label: '全部' },
        { key: 'pending_receive', label: '待接收' },
      ]
    : [
        { key: 'all', label: '全部' },
        { key: 'pending_contact', label: '待联系' },
        { key: 'pending_receive', label: '待接收' },
      ],
)
const activeTab = ref<string>('all')
const loading = ref(false)

// ── 数据 ──
const batchList = ref<CollectionBatch[]>([])
const selectedBatch = ref<CollectionBatch | null>(null)
const batchDetail = ref<BatchDetail | null>(null)

// ── 表单 ──
const scheduleForm = ref<ScheduleData & { contactResult: string }>({
  scheduledReceiveAt: '',
  contactNote: '',
  contactResult: '符合征集方向',
})
const rejectForm = ref({ rejectReason: '' })

// ── 文件上传 ──
const stagingFiles = ref<StagingFile[]>([])
const uploading = ref(false)
const isDragOver = ref(false)

// ── 条目 ──
const receptionItems = computed(() => batchDetail.value?.items ?? [])
const pendingItemsCount = computed(() => receptionItems.value.filter((i) => i.result === 'pending').length)

// ── 概览指标 ──
const contactCount = computed(() => batchList.value.filter((b) => b.status === 'pending_contact').length)
const receiveCount = computed(() => batchList.value.filter((b) => b.status === 'pending_receive').length)
const todayArrivalCount = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return batchList.value.filter((b) => {
    if (b.status !== 'pending_receive' || !b.scheduledReceiveAt) return false
    return b.scheduledReceiveAt.startsWith(today)
  }).length
})
const agreementErrorCount = computed(() => 0)

// ── 筛选列表 ──
const filteredCollections = computed(() => {
  return batchList.value.filter((b) => {
    if (isFrontArchivist.value && (b.status === 'pending_contact' || b.status === 'rejected')) return false
    if (activeTab.value !== 'all' && b.status !== activeTab.value) return false
    return true
  })
})

const batchPhaseLabel = computed(() => {
  if (!selectedBatch.value) return ''
  const map: Record<string, string> = {
    pending_contact: '当前待后台联系判断',
    pending_receive: '当前待前台到馆接收',
    received: '已接收完成',
    partially_received: '部分接收完成',
    rejected: '已拒绝',
  }
  return map[selectedBatch.value.status] || ''
})

// 详情头部：批次号 / 组织名 / 联系人 电话，组织名为空时跳过该项以避免孤立斜杠
function collectionHeader(batch: { batchNo: string; organizationName?: string | null; contactName?: string | null; contactPhone?: string | null }): string {
  const parts: string[] = [batch.batchNo]
  if (batch.organizationName) parts.push(batch.organizationName)
  const contact = [batch.contactName, batch.contactPhone].filter(Boolean).join(' ')
  if (contact) parts.push(contact)
  return parts.join(' / ')
}

// ── 工具函数 ──
function statusClass(status: string): string {
  const map: Record<string, string> = {
    pending_contact: 'warning',
    pending_receive: 'info',
    received: 'success',
    partially_received: 'info',
    rejected: 'danger',
  }
  return map[status] || ''
}

function formatDateTime(iso: string): string {
  if (!iso) return ''
  const d = new Date(iso)
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

function matchStatusLabel(status: string): string {
  const map: Record<string, string> = {
    none: '未上传', matched: '匹配成功', missing: '清单缺文件',
    unmatched: '多余文件', duplicate: '重复提示', failed: '检查失败', staging: '暂存中',
  }
  return map[status] || status
}

function matchStatusClass(status: string): string {
  const map: Record<string, string> = {
    none: '', matched: 'success', missing: 'warning',
    unmatched: 'warning', duplicate: 'warning', failed: 'danger', staging: 'info',
  }
  return map[status] || ''
}
function carrierStatusLabel(status: string): string {
  const map: Record<string, string> = {
    electronic: '纯电子', paper_electronic: '纸质+电子', paper: '纯纸质',
  }
  return map[status] || status
}

function fileScanHint(file: StagingFile): string {
  if (file.matchStatus === 'matched') return '格式和安全检查通过'
  if (file.matchStatus === 'unmatched') return '文件名与清单不一致，需人工确认'
  if (file.matchStatus === 'duplicate') return '与已入库影像疑似重复'
  return ''
}

// ── 加载数据 ──
async function loadCollections() {
  loading.value = true
  try {
    // 征集管理页一次性拉取较多批次：后端默认 pageSize=20，批次累积时会截断，
    // 导致 tab 筛选（前端过滤）漏掉分页之外的新批次（如新提交的待联系批次）。
    // 后端 pageSize 上限 100，取 100 容纳常规累积（完整分页/tab 后端筛选为后续优化）。
    const res = await getCollections({ pageSize: 100 })
    batchList.value = res.records
  } catch {
    ElMessage.error('加载征集批次失败')
  } finally {
    loading.value = false
  }
}

async function selectBatch(batch: CollectionBatch) {
  selectedBatch.value = batch
  batchDetail.value = null
  stagingFiles.value = []
  if (batch.status === 'pending_receive' || batch.status === 'received' || batch.status === 'partially_received') {
    try {
      batchDetail.value = await getReceptionBatchDetail(batch.id)
      stagingFiles.value = batchDetail.value.stagingFiles
    } catch {
      ElMessage.error('加载批次详情失败')
    }
  }
}

// ── 后台操作 ──
async function handleSchedule() {
  if (!selectedBatch.value) return
  if (!scheduleForm.value.scheduledReceiveAt) {
    ElMessage.error('请选择约定到馆时间')
    return
  }
  try {
    const updated = await scheduleCollection(selectedBatch.value.id, {
      scheduledReceiveAt: scheduleForm.value.scheduledReceiveAt,
      contactNote: scheduleForm.value.contactResult,
    })
    Object.assign(selectedBatch.value, updated)
    ElMessage.success('已约定到馆时间，批次状态变更为待接收')
  } catch {
    ElMessage.error('约定到馆失败')
  }
}

async function handleReject() {
  if (!selectedBatch.value) return
  if (!rejectForm.value.rejectReason.trim()) {
    ElMessage.error('拒绝征集必须填写拒绝理由')
    return
  }
  try {
    const updated = await rejectCollection(selectedBatch.value.id, {
      rejectReason: rejectForm.value.rejectReason,
    })
    Object.assign(selectedBatch.value, updated)
    ElMessage.success('已拒绝该征集意向')
  } catch {
    ElMessage.error('拒绝操作失败')
  }
}

// ── 前台文件上传 ──
async function handleFileUpload(files: File[]) {
  if (!selectedBatch.value || files.length === 0) return
  uploading.value = true
  try {
    const result = await uploadStagingFiles(selectedBatch.value.id, files)
    stagingFiles.value = result.files
    result.files.forEach((sf) => {
      if (sf.matchedItemId) {
        const item = batchDetail.value?.items.find((i) => i.id === sf.matchedItemId)
        if (item) item.fileMatchStatus = 'matched'
      }
    })
    ElMessage.success(`上传完成，匹配成功 ${result.matchSummary.matched} 个文件`)
  } catch {
    ElMessage.error('文件上传失败')
  } finally {
    uploading.value = false
  }
}

function handleDrop(e: DragEvent) {
  isDragOver.value = false
  const files = Array.from(e.dataTransfer?.files ?? [])
  handleFileUpload(files)
}

function handleFilePick(e: Event) {
  const input = e.target as HTMLInputElement
  const files = Array.from(input.files ?? [])
  handleFileUpload(files)
  input.value = ''
}

// ── 前台完成接收 ──
async function handleCompleteReceive() {
  if (!selectedBatch.value || !batchDetail.value) return
  const items = batchDetail.value.items
  const missingReason = items.find((i) => i.result === 'rejected' && !i.acceptanceNote?.trim())
  if (missingReason) {
    ElMessage.error(`条目"${missingReason.title}"回退前必须填写原因`)
    return
  }
  const pending = items.filter((i) => i.result === 'pending')
  if (pending.length > 0) {
    ElMessage.error(`仍有 ${pending.length} 条待验收，不能完成接收`)
    return
  }
  if (!selectedBatch.value.agreementAcceptedAt) {
    ElMessage.error('缺少在线协议同意记录，不可完成接收')
    return
  }
  try {
    // 逐条提交征集验收结论
    for (const i of items) {
      await updateItemAcceptance(i.id, {
        result: i.result as 'accepted' | 'rejected',
        acceptanceNote: i.acceptanceNote || undefined,
        rejectReason: i.rejectReason || undefined,
      })
    }
    await completeBatchAcceptance(selectedBatch.value.id, { acceptanceNote: '征集到馆验收完成' })
    ElMessage.success('已确认接收，条目进入后续入库流程')
    // 刷新批次列表与详情，避免完成后卡片仍显示原状态（与 B12-1 同类问题）
    const completedId = selectedBatch.value.id
    await loadCollections()
    // 关键：从刷新后的 batchList 取最新对象再 selectBatch，
    // 否则 selectedBatch 仍指向旧对象（status=pending_receive），头部状态不会更新
    const refreshed = batchList.value.find((b) => b.id === completedId)
    if (refreshed) {
      await selectBatch(refreshed)
    }
  } catch {
    ElMessage.error('完成接收失败')
  }
}

// ── 导出回执 ──
async function handleExportReceipt() {
  if (!selectedBatch.value) return
  try {
    const blob = await exportReceipt(selectedBatch.value.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `征集回执-${selectedBatch.value.batchNo}.pdf`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('回执已生成')
  } catch {
    ElMessage.error('导出回执失败')
  }
}

onMounted(() => {
  loadCollections()
})
</script>

<style scoped>
.work-layout {
  display: grid;
  grid-template-columns: minmax(420px, 0.95fr) minmax(0, 1.05fr);
  gap: 16px;
  align-items: stretch;
  height: calc(100vh - 256px);
  min-height: 560px;
}

.scroll-pane {
  min-height: 0;
  overflow-y: auto;
}

.detail-pane {
  padding-right: 4px;
}

.batch-list {
  display: grid;
  gap: 10px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.batch-item {
  display: grid;
  gap: 8px;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
  cursor: pointer;
}

.batch-item.active {
  border-color: #9cc7ca;
  background: #f6fbfb;
  box-shadow: 0 0 0 3px rgba(31, 111, 120, 0.08);
}

.batch-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  align-items: center;
}

.batch-title {
  margin: 0;
  font-size: 15px;
}

.meta-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  color: var(--muted);
  font-size: 13px;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.info-tile {
  padding: 11px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
}

.info-tile span {
  display: block;
  color: var(--muted);
  font-size: 12px;
  font-weight: 700;
}

.info-tile strong {
  display: block;
  margin-top: 4px;
  overflow-wrap: anywhere;
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
  margin-top: 12px;
}

.form-grid .field {
  display: grid;
  gap: 4px;
}

.form-grid .field label {
  font-size: 13px;
  color: var(--muted);
  font-weight: 600;
}

.form-grid .field input,
.form-grid .field select {
  min-height: 36px;
  padding: 6px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
}

.review-table select,
.review-table input {
  width: 100%;
  min-width: 120px;
  min-height: 34px;
  padding: 7px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
}

.drop-zone {
  display: grid;
  min-height: 132px;
  place-items: center;
  padding: 18px;
  border: 2px dashed #9bbdc0;
  border-radius: var(--radius);
  color: #23494f;
  background: #f7fcfc;
  text-align: center;
}

.drop-zone.dragover {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.match-grid {
  display: grid;
  gap: 10px;
}

.match-card {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 10px;
  align-items: center;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
}

.collapsible summary {
  display: flex;
  min-height: 34px;
  cursor: pointer;
  list-style: none;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.collapsible summary::-webkit-details-marker {
  display: none;
}

.collapsible-body {
  display: grid;
  gap: 12px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid var(--border);
}

.upload-block {
  margin-top: 14px;
  padding: 12px 0 0;
  border-top: 1px solid var(--border);
}

.flow-note {
  display: grid;
  gap: 9px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.flow-note li {
  display: grid;
  grid-template-columns: 34px 1fr;
  gap: 10px;
  align-items: start;
}

.flow-no {
  display: grid;
  width: 28px;
  height: 28px;
  place-items: center;
  border-radius: 50%;
  color: #ffffff;
  background: var(--primary);
  font-size: 13px;
  font-weight: 800;
}

@media (max-width: 1120px) {
  .work-layout,
  .detail-grid {
    grid-template-columns: 1fr;
  }

  .work-layout {
    height: auto;
    min-height: 0;
  }

  .scroll-pane {
    overflow: visible;
  }
}
</style>
