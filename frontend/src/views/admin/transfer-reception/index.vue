<template>
  <section>
    <h1 class="page-title">移交验收与电子文件上传</h1>
    <p class="page-subtitle">
      调取到馆的移交与征集清单，核对纸质原件、页数、数量和签章；上传 U 盘电子文件后按文件名匹配清单条目，异常必须人工确认或回退。
    </p>
  </section>

  <!-- 概览指标 -->
  <section class="grid four" style="margin-top: 18px;" aria-label="前台待办统计">
    <div class="metric">
      <span class="label">今日到馆清单</span>
      <span class="value">{{ todayCount }}</span>
      <span class="note">按预计移交日期筛选</span>
    </div>
    <div class="metric">
      <span class="label">待验收条目</span>
      <span class="value">{{ pendingItemCount }}</span>
      <span class="note">条目状态：待验收</span>
    </div>
    <div class="metric">
      <span class="label">匹配异常</span>
      <span class="value">{{ abnormalCount }}</span>
      <span class="note">未匹配、重复、缺失或检查失败</span>
    </div>
    <div class="metric">
      <span class="label">待导出回执</span>
      <span class="value">{{ completedNotExportedCount }}</span>
      <span class="note">确认接收后导出 PDF</span>
    </div>
  </section>

  <!-- 筛选工具栏 -->
  <div class="toolbar">
    <div class="tabs" aria-label="待验收清单筛选">
      <button
        v-for="tab in filterTabs"
        :key="tab.key"
        class="tab"
        :class="{ active: activeFilter === tab.key }"
        type="button"
        @click="activeFilter = tab.key"
      >
        {{ tab.label }}
      </button>
    </div>
    <div class="actions">
      <label class="field" style="min-width: 240px;">
        <span class="sr-only">搜索清单</span>
        <input v-model="searchKeyword" type="search" placeholder="搜索清单号、单位、标题">
      </label>
    </div>
  </div>

  <!-- 主布局：左侧列表 + 右侧详情 -->
  <section class="reception-layout">
    <!-- 左侧：待验收清单 -->
    <aside class="card panel batch-pane">
      <h2 class="section-title">待验收清单</h2>
      <div v-if="loading" style="padding: 20px; text-align: center; color: var(--muted);">加载中...</div>
      <div v-else-if="filteredBatches.length === 0" class="empty">当前筛选下没有待验收清单。</div>
      <div v-else class="batch-list">
        <button
          v-for="batch in filteredBatches"
          :key="batch.id"
          class="batch-card"
          :class="{ active: activeBatch?.batch.id === batch.id }"
          type="button"
          @click="selectBatch(batch.id)"
        >
          <strong>{{ batch.title }}</strong>
          <span class="mono">{{ batch.batchNo }}</span>
          <span class="muted">
            <span class="source-tag" :class="`src-${batch.sourceType}`">{{ sourceTypeLabel(batch.sourceType) }}</span>
            {{ batch.organizationName || batch.contactName || '—' }}<template v-if="batch.departmentName"> / {{ batch.departmentName }}</template>
          </span>
          <span>
            <span class="status info">{{ batch.statusText }}</span>
            <span class="muted">{{ batch.itemCount }} 条</span>
          </span>
        </button>
      </div>
    </aside>

    <!-- 右侧：详情面板 -->
    <div v-if="!activeBatch" class="grid detail-pane">
      <div class="card panel" style="text-align: center; padding: 40px; color: var(--muted);">
        请在左侧选择一个待验收清单
      </div>
    </div>

    <div v-else class="grid detail-pane">
      <!-- 清单详情 -->
      <section class="card panel">
        <div class="split-panel">
          <div>
            <div class="detail-head">
              <h2 class="section-title">{{ activeBatch.batch.title }}</h2>
              <p class="muted" style="margin-top: -6px;">
                {{ receptionHeader(activeBatch.batch) }}
              </p>
            </div>
            <div class="summary-row" style="margin-top: 12px;">
              <div class="mini"><span class="muted">批次状态</span><strong>{{ batchStatusLabel }}</strong></div>
              <div class="mini"><span class="muted">签字清单</span><strong>{{ activeBatch.batch.signatureStatus || '—' }}</strong></div>
              <div class="mini"><span class="muted">预计移交</span><strong>{{ activeBatch.batch.expectedTransferDate || '—' }}</strong></div>
            </div>
          </div>
        </div>
      </section>

      <!-- U盘文件上传与匹配 -->
      <details class="card panel collapsible" open>
        <summary>
          <span class="section-title">U 盘电子文件上传与匹配</span>
          <span class="status info">{{ activeBatch.stagingFiles.length > 0 ? '已上传' : '点击展开' }}</span>
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
              <strong>拖拽 U 盘文件或选择文件</strong>
              <p class="muted" style="margin: 6px 0 0;">
                上传后进入暂存路径，系统按清单中的档案文件名匹配，并执行格式、大小、哈希和安全检查。
              </p>
            </div>
            <label class="button secondary" for="filePicker">选择 U 盘文件</label>
            <input id="filePicker" class="sr-only" type="file" multiple @change="handleFilePick">
          </div>
          <div v-if="uploading" style="text-align: center; padding: 12px; color: var(--muted);">文件上传中...</div>
          <div v-if="activeBatch.stagingFiles.length > 0" class="match-grid">
            <div
              v-for="file in activeBatch.stagingFiles"
              :key="file.fileId"
              class="match-card"
            >
              <div>
                <strong>{{ file.originalFilename }}</strong>
                <div class="hint">{{ fileScanHint(file) }}</div>
              </div>
              <span class="status" :class="matchStatusClass(file.matchStatus)">{{ matchStatusLabel(file.matchStatus) }}</span>
            </div>
          </div>
        </div>
      </details>

      <!-- 条目验收 -->
      <section>
        <div class="toolbar" style="margin-top: 0;">
          <h2 class="section-title" style="margin: 0;">条目验收</h2>
          <div class="actions">
            <button class="button secondary" type="button" @click="acceptAllPaper">批量纸质验收通过</button>
            <button class="button" type="button" @click="confirmReceive">确认接收</button>
          </div>
        </div>
        <div class="table-wrap scroll-y">
          <table class="acceptance-table">
            <thead>
              <tr>
                <th>序号</th>
                <th>档案标题</th>
                <th>载体</th>
                <th>清单文件名</th>
                <th>纸质核对</th>
                <th>文件匹配</th>
                <th>验收结论</th>
                <th>说明 / 回退原因</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="item in activeBatch.items" :key="item.id">
                <td>{{ item.seqNo }}</td>
                <td>{{ item.title }}</td>
                <td>{{ carrierStatusLabel(item.carrierStatus) }}</td>
                <td class="mono">{{ item.expectedFilename || '无电子文件' }}</td>
                <td>
                  <span v-if="item.carrierStatus === 'electronic'" class="status success">无需（纯电子）</span>
                  <select v-else v-model="item.paperCheckStatus">
                    <option value="pending">待核对</option>
                    <option value="passed">通过</option>
                    <option value="failed">异常</option>
                  </select>
                </td>
                <td><span class="status" :class="matchStatusClass(item.fileMatchStatus)">{{ matchStatusLabel(item.fileMatchStatus) }}</span></td>
                <td>
                  <select v-model="item.result">
                    <option value="pending">待验收</option>
                    <option value="accepted">接收</option>
                    <option value="rejected">回退</option>
                  </select>
                </td>
                <td><input v-model="item.acceptanceNote" placeholder="验收说明或回退原因"></td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>

      <!-- 接收回执预览 -->
      <section class="card panel">
        <div class="split-panel">
          <div>
            <h2 class="section-title">接收回执预览</h2>
            <div class="receipt-preview">
              <div>
                <span class="status success">已接收 {{ acceptedCount }} 条</span>
                <span class="status" :class="rejectedCount ? 'danger' : ''">
                  {{ rejectedCount ? `回退 ${rejectedCount} 条` : '' }}
                </span>
                <span class="status" :class="pendingItemCountInBatch ? 'info' : 'success'">
                  待处理 {{ pendingItemCountInBatch }} 条
                </span>
              </div>
              <strong>回执内容</strong>
              <span class="muted">已接收条目：{{ acceptedItemsText || '暂无' }}</span>
              <span class="muted">回退条目：{{ rejectedItemsText || '暂无' }}</span>
            </div>
          </div>
          <div class="actions" style="align-content: start;">
            <button
              class="button secondary"
              type="button"
              :disabled="pendingItemCountInBatch > 0"
              @click="handleExportReceipt"
            >
              导出接收回执
            </button>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { ReceptionBatch, BatchDetail, StagingFile } from '@/types/reception'
import {
  getReceptionBatches,
  getReceptionBatchDetail,
  uploadStagingFiles,
  updateItemAcceptance,
  completeBatchAcceptance,
  exportReceipt,
} from '@/api/reception'

// ── 筛选状态 ──
const filterTabs = [
  { key: 'pending', label: '待移交' },
  { key: 'today', label: '今日到馆' },
  { key: 'received', label: '已接收' },
  { key: 'abnormal', label: '存在异常' },
] as const
const activeFilter = ref<string>('pending')
const searchKeyword = ref('')
const loading = ref(false)

// ── 数据 ──
const batchList = ref<ReceptionBatch[]>([])
const activeBatch = ref<BatchDetail | null>(null)
const uploading = ref(false)
const isDragOver = ref(false)

// ── 概览指标 ──
const todayCount = computed(() => {
  const today = new Date().toISOString().slice(0, 10)
  return batchList.value.filter((b) => b.expectedTransferDate === today).length
})
const pendingItemCount = computed(() => {
  if (!activeBatch.value) return batchList.value.reduce((sum, b) => sum + b.itemCount, 0)
  return activeBatch.value.items.filter((i) => i.result === 'pending').length
})
const abnormalCount = computed(() => {
  if (!activeBatch.value) return 0
  const fileAbnormal = activeBatch.value.stagingFiles.filter(
    (f) => f.matchStatus !== 'matched',
  ).length
  const itemAbnormal = activeBatch.value.items.filter(
    (i) => i.fileMatchStatus === 'missing' || i.fileMatchStatus === 'failed',
  ).length
  return fileAbnormal + itemAbnormal
})
const completedNotExportedCount = computed(() => {
  return batchList.value.filter(
    (b) => b.status === 'received' || b.status === 'partially_received',
  ).length
})

// ── 筛选 ──
const filteredBatches = computed(() => {
  return batchList.value.filter((b) => {
    if (searchKeyword.value) {
      const kw = searchKeyword.value.toLowerCase()
      const text = `${b.batchNo} ${b.title} ${b.organizationName}`.toLowerCase()
      if (!text.includes(kw)) return false
    }
    if (activeFilter.value === 'today') {
      const today = new Date().toISOString().slice(0, 10)
      return b.expectedTransferDate === today
    }
    if (activeFilter.value === 'received') {
      // 已接收（未入库）：received 与 partially_received 合并，便于补导出回执
      return b.status === 'received' || b.status === 'partially_received'
    }
    if (activeFilter.value === 'abnormal') {
      return true
    }
    // pending（默认）：待移交/待接收，排除已接收，避免接收后批次仍停留在「待移交」
    return b.status !== 'received' && b.status !== 'partially_received'
  })
})

// ── 批次详情计算属性 ──
const batchStatusLabel = computed(() => {
  if (!activeBatch.value) return ''
  const pending = activeBatch.value.items.filter((i) => i.result === 'pending').length
  const rejected = activeBatch.value.items.filter((i) => i.result === 'rejected').length
  if (pending > 0) return '待移交'
  return rejected > 0 ? '部分接收' : '已接收'
})

// 详情头部：批次号 / 组织名 / 联系人 电话，组织名为空时跳过该项以避免孤立斜杠
function receptionHeader(batch: { batchNo: string; organizationName?: string | null; contactName?: string | null; contactPhone?: string | null }): string {
  const parts: string[] = [batch.batchNo]
  if (batch.organizationName) parts.push(batch.organizationName)
  const contact = [batch.contactName, batch.contactPhone].filter(Boolean).join(' ')
  if (contact) parts.push(contact)
  return parts.join(' / ')
}
const acceptedCount = computed(() =>
  activeBatch.value ? activeBatch.value.items.filter((i) => i.result === 'accepted').length : 0,
)
const rejectedCount = computed(() =>
  activeBatch.value ? activeBatch.value.items.filter((i) => i.result === 'rejected').length : 0,
)
const pendingItemCountInBatch = computed(() =>
  activeBatch.value ? activeBatch.value.items.filter((i) => i.result === 'pending').length : 0,
)
const acceptedItemsText = computed(() => {
  if (!activeBatch.value) return ''
  return activeBatch.value.items
    .filter((i) => i.result === 'accepted')
    .map((i) => String(i.seqNo).padStart(2, '0'))
    .join('、')
})
const rejectedItemsText = computed(() => {
  if (!activeBatch.value) return ''
  return activeBatch.value.items
    .filter((i) => i.result === 'rejected')
    .map((i) => `${String(i.seqNo).padStart(2, '0')}（${i.acceptanceNote || '未填写原因'}）`)
    .join('；')
})

// ── 匹配状态工具函数 ──
function matchStatusLabel(status: string): string {
  const map: Record<string, string> = {
    none: '未上传', matched: '匹配成功', missing: '清单缺文件',
    unmatched: '多余文件', duplicate: '重复匹配', failed: '检查失败', staging: '暂存中',
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
function sourceTypeLabel(sourceType: string): string {
  const map: Record<string, string> = {
    transfer: '移交', collection: '征集', compilation: '编研',
  }
  return map[sourceType] || sourceType
}
function fileScanHint(file: StagingFile): string {
  if (file.matchStatus === 'matched') return '格式检查通过、安全检查通过'
  if (file.matchStatus === 'unmatched') return '文件名与清单不一致，需人工确认或回退'
  if (file.matchStatus === 'duplicate') return '同名重复，需前台人工选择对应条目'
  return ''
}

// ── 加载数据 ──
async function loadBatches() {
  loading.value = true
  try {
    const res = await getReceptionBatches()
    batchList.value = res.records
  } catch {
    ElMessage.error('加载待验收清单失败')
  } finally {
    loading.value = false
  }
}

async function selectBatch(batchId: number) {
  try {
    activeBatch.value = await getReceptionBatchDetail(batchId)
    // 5.1：纯电子条目无纸质件，纸质核对固定为「通过」，避免误导并解除核对阻塞
    activeBatch.value.items.forEach((item) => {
      if (item.carrierStatus === 'electronic') {
        item.paperCheckStatus = 'passed'
      }
    })
  } catch {
    ElMessage.error('加载批次详情失败')
  }
}

// ── 文件上传 ──
async function handleFileUpload(files: File[]) {
  if (!activeBatch.value || files.length === 0) return
  uploading.value = true
  try {
    const result = await uploadStagingFiles(activeBatch.value.batch.id, files)
    activeBatch.value.stagingFiles = result.files
    activeBatch.value.matchSummary = result.matchSummary
    result.files.forEach((sf) => {
      if (sf.matchedItemId) {
        const item = activeBatch.value!.items.find((i) => i.id === sf.matchedItemId)
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

// ── 批量纸质验收通过 ──
function acceptAllPaper() {
  if (!activeBatch.value) return
  activeBatch.value.items.forEach((item) => {
    item.paperCheckStatus = 'passed'
    if (item.carrierStatus === 'paper' || item.fileMatchStatus === 'matched') {
      item.result = 'accepted'
      if (!item.acceptanceNote) item.acceptanceNote = '纸质核对通过'
    }
  })
  ElMessage.success('已批量标记纸质验收通过，电子匹配异常仍需人工处理。')
}

// ── 确认接收 ──
async function confirmReceive() {
  if (!activeBatch.value) return
  const items = activeBatch.value.items
  const missingReason = items.find((i) => i.result === 'rejected' && !i.acceptanceNote?.trim())
  if (missingReason) {
    ElMessage.error(`第 ${String(missingReason.seqNo).padStart(2, '0')} 条回退前必须填写回退原因。`)
    return
  }
  const pending = items.filter((i) => i.result === 'pending')
  if (pending.length > 0) {
    ElMessage.error(`仍有 ${pending.length} 条待验收，不能确认接收。`)
    return
  }
  // R3-B2：电子件文件名未匹配/重复/检查异常时阻断接收，否则这些暂存文件不会挂到任何条目，确认接收后永久丢失
  const problematicFiles = activeBatch.value.stagingFiles.filter(
    (f) => f.matchStatus === 'unmatched' || f.matchStatus === 'duplicate' || f.matchStatus === 'failed',
  )
  if (problematicFiles.length > 0) {
    ElMessage.error(
      `有 ${problematicFiles.length} 个电子件未匹配或检查异常，请先人工确认/处理后确认接收，否则电子件将丢失。`,
    )
    return
  }
  try {
    // 逐条提交验收结论（§7.5 PUT /admin/reception/items/{itemId}/acceptance）
    for (const i of items) {
      await updateItemAcceptance(i.id, {
        result: i.result as 'accepted' | 'rejected',
        acceptanceNote: i.acceptanceNote || undefined,
        rejectReason: i.rejectReason || undefined,
      })
    }
    await completeBatchAcceptance(activeBatch.value.batch.id, { acceptanceNote: '现场清点完成' })
    const rejected = items.filter((i) => i.result === 'rejected').length
    ElMessage.success(
      rejected ? '已确认部分接收，回退条目将写入回执。' : '已确认全部接收，条目进入后台待入库。',
    )
    // 刷新左栏批次列表（接收后批次从待验收变成已接收/部分接收，列表卡片状态需同步），
    // 同时刷新当前 activeBatch 详情避免本地状态滞后。
    await loadBatches()
    // 接收完成后切到「已接收」tab，直观看到成果并支持补导出回执
    activeFilter.value = 'received'
    if (activeBatch.value) await selectBatch(activeBatch.value.batch.id)
  } catch {
    ElMessage.error('确认接收失败')
  }
}

// ── 导出回执 ──
async function handleExportReceipt() {
  if (!activeBatch.value) return
  try {
    const blob = await exportReceipt(activeBatch.value.batch.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `回执-${activeBatch.value.batch.batchNo}.pdf`
    a.click()
    URL.revokeObjectURL(url)
    ElMessage.success('接收回执已生成')
  } catch {
    ElMessage.error('导出回执失败')
  }
}

onMounted(() => {
  loadBatches()
})
</script>

<style scoped>
.reception-layout {
  display: grid;
  grid-template-columns: 360px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
  height: calc(100vh - 286px);
  min-height: 620px;
}

.batch-pane,
.detail-pane {
  min-width: 0;
  min-height: 0;
  overflow-y: auto;
}

.batch-pane {
  max-height: 100%;
}

.detail-pane > .card,
.detail-pane > details,
.detail-pane > section {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.batch-card {
  display: grid;
  gap: 8px;
  width: 100%;
  padding: 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius);
  background: #ffffff;
  text-align: left;
  transition: border-color 0.16s ease, background 0.16s ease;
}

.batch-card:hover,
.batch-card.active {
  border-color: #8abcbf;
  background: #f2f8f8;
}

.batch-list {
  display: grid;
  gap: 10px;
  padding-right: 4px;
}

.source-tag {
  display: inline-block;
  padding: 1px 7px;
  margin-right: 6px;
  font-size: 12px;
  line-height: 18px;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border);
  background: #f0f4f5;
  color: #23494f;
}
.source-tag.src-collection {
  background: #fff7e6;
  border-color: #ffd591;
  color: #874d00;
}
.source-tag.src-transfer {
  background: #e6f7f6;
  border-color: #87e8de;
  color: #006d75;
}
.source-tag.src-compilation {
  background: #f6ffed;
  border-color: #b7eb8f;
  color: #389e0d;
}

.split-panel {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  align-items: start;
  min-width: 0;
}

.drop-zone {
  display: grid;
  min-height: 146px;
  place-items: center;
  padding: 20px;
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

.match-grid {
  display: grid;
  gap: 10px;
  margin-top: 12px;
}

.match-card {
  display: grid;
  grid-template-columns: 1fr auto;
  gap: 8px;
  align-items: center;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
}

.acceptance-table {
  min-width: 980px;
}

.acceptance-table input,
.acceptance-table select {
  width: 100%;
  min-height: 34px;
  padding: 6px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
}

.receipt-preview {
  display: grid;
  gap: 10px;
  padding: 12px;
  border: 1px dashed #b8c5ce;
  border-radius: var(--radius);
  background: #f7fafc;
}

.summary-row {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 10px;
}

.summary-row .mini {
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
}

.summary-row strong {
  display: block;
  font-size: 21px;
}

@media (max-width: 1220px) {
  .reception-layout,
  .split-panel {
    grid-template-columns: 1fr;
  }

  .reception-layout {
    height: auto;
    min-height: 0;
  }

  .batch-pane,
  .detail-pane {
    max-height: none;
  }
}
</style>
