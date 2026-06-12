<template>
  <div>
    <section>
      <h1 class="page-title">移交工作台</h1>
      <p class="page-subtitle">
        跟踪本单位移交清单从编制、前台验收、后台入库到上架的全过程。移交单位侧不上传电子文件本体，电子文件随
        U 盘到馆后由前台统一上传。
      </p>
    </section>

    <!-- 加载状态 -->
    <div v-if="loading" class="notice" style="margin-top: 18px">正在加载移交数据…</div>

    <!-- 接口错误 -->
    <div v-else-if="loadError" class="notice danger" style="margin-top: 18px">
      加载失败：{{ loadError }}。<button class="button ghost" @click="loadData">重试</button>
    </div>

    <template v-else>
      <!-- 状态统计 -->
      <section class="grid four" aria-label="状态统计" style="margin-top: 18px">
        <div class="metric">
          <span class="label">草稿清单</span>
          <span class="value">{{ dashboard.summary.draft }}</span>
          <span class="note">可继续编辑并提交</span>
        </div>
        <div class="metric">
          <span class="label">待移交</span>
          <span class="value">{{ dashboard.summary.pendingTransfer }}</span>
          <span class="note">需携带纸质原件、U 盘和签字清单到馆</span>
        </div>
        <div class="metric">
          <span class="label">部分接收</span>
          <span class="value">{{ dashboard.summary.partiallyReceived }}</span>
          <span class="note">回退条目下次新建清单补交</span>
        </div>
        <div class="metric">
          <span class="label">已上架</span>
          <span class="value">{{ dashboard.summary.shelved }}</span>
          <span class="note">纸质相关档案已进入利用范围</span>
        </div>
      </section>

      <!-- 移交流程 -->
      <section class="flow-strip" aria-label="移交流程">
        <div class="flow-step">
          <strong>1. 编制清单</strong>
          <span class="muted">浏览器本地解析文件名，不上传文件。</span>
        </div>
        <div class="flow-step">
          <strong>2. 前台验收</strong>
          <span class="muted">核对实物，上传 U 盘文件并匹配清单。</span>
        </div>
        <div class="flow-step">
          <strong>3. 后台入库</strong>
          <span class="muted">AI 补全白名单字段，人工确认后生成档号。</span>
        </div>
        <div class="flow-step">
          <strong>4. 确认上架</strong>
          <span class="muted">纸质相关档案上架后开放检索和借阅。</span>
        </div>
      </section>

      <div class="notice warning">
        提醒：提交后的清单只读。若前台回退条目，回退条目不占用正式档号，请在下次移交时新建清单补交。
      </div>

      <!-- 筛选工具栏 -->
      <div class="toolbar">
        <div class="tabs" role="tablist" aria-label="清单状态筛选">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            :class="['tab', { active: activeFilter === tab.value }]"
            type="button"
            @click="activeFilter = tab.value"
          >
            {{ tab.label }}
          </button>
        </div>
        <div class="actions">
          <label class="field" style="min-width: 220px">
            <span class="sr-only">搜索清单</span>
            <input v-model="keyword" type="search" placeholder="搜索清单号、标题、年度" />
          </label>
          <router-link to="/transfer/transfer-list" class="button">新建清单</router-link>
        </div>
      </div>

      <!-- 工作区：清单列表 + 详情 -->
      <section class="workspace">
        <div>
          <h2 class="section-title">我的移交清单</h2>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>清单号</th>
                  <th>标题</th>
                  <th>年度</th>
                  <th>预计移交</th>
                  <th>条目</th>
                  <th>状态</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="filteredBatches.length === 0">
                  <td colspan="7"><div class="empty">没有符合条件的清单</div></td>
                </tr>
                <tr
                  v-for="batch in filteredBatches"
                  :key="batch.id"
                  :class="['batch-row', { active: selectedBatchId === batch.id }]"
                >
                  <td>
                    <button type="button" @click="selectedBatchId = batch.id">
                      <strong class="mono">{{ batch.batchNo }}</strong>
                    </button>
                  </td>
                  <td>{{ batch.title }}</td>
                  <td>{{ batch.archiveYear }}</td>
                  <td>{{ batch.expectedTransferDate }}</td>
                  <td>{{ batch.itemCount }} 件</td>
                  <td>
                    <span :class="['status', statusClass(batch.status)]">{{ batch.statusText }}</span>
                  </td>
                  <td>
                    <router-link
                      v-if="batch.status === 'draft'"
                      :to="`/transfer/transfer-list?batchId=${batch.id}`"
                      class="button ghost"
                      >继续编辑</router-link
                    >
                    <button v-else class="button ghost" type="button" @click="handleExport(batch.id)">导出清单</button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>

        <!-- 详情面板 -->
        <aside v-if="selectedBatch" class="card panel" aria-label="清单详情">
          <div class="detail-head">
            <div>
              <h2 class="section-title">{{ selectedBatch.title }}</h2>
              <p class="muted" style="margin: 0">{{ selectedBatch.batchNo }}</p>
            </div>
            <span :class="['status', statusClass(selectedBatch.status)]">{{ selectedBatch.statusText }}</span>
          </div>

          <div class="detail-meta">
            <div class="meta-item"><span>移交单位</span><strong>{{ selectedBatch.organizationName }}</strong></div>
            <div class="meta-item"><span>移交部门</span><strong>{{ selectedBatch.departmentName }}</strong></div>
            <div class="meta-item">
              <span>经办人</span><strong>{{ selectedBatch.contactPerson }} {{ selectedBatch.contactPhone }}</strong>
            </div>
            <div class="meta-item">
              <span>提交时间</span><strong>{{ selectedBatch.submittedAt || '未提交' }}</strong>
            </div>
            <div class="meta-item">
              <span>接收时间</span><strong>{{ selectedBatch.receivedAt || '未接收' }}</strong>
            </div>
            <div class="meta-item">
              <span>入库 / 上架</span
              ><strong>{{ selectedBatch.archivedAt || '未入库' }} / {{ selectedBatch.shelvedAt || '未上架' }}</strong>
            </div>
          </div>

          <!-- 进度条 -->
          <div class="progress-line">
            <div
              v-for="(step, idx) in progressSteps"
              :key="idx"
              :class="['progress-node', step.cls]"
            >
              <strong>{{ step.label }}</strong>
              <span>{{ step.desc }}</span>
            </div>
          </div>

          <!-- 可执行操作 -->
          <div class="receipt-box">
            <strong>可执行操作</strong>
            <div class="actions">
              <router-link
                v-if="selectedBatch.status === 'draft'"
                :to="`/transfer/transfer-list?batchId=${selectedBatch.id}`"
                class="button"
                >继续编辑</router-link
              >
              <button
                v-else
                class="button"
                type="button"
                @click="handleExport(selectedBatch.id)"
              >
                导出移交清单
              </button>
              <router-link
                v-if="selectedBatch.status === 'partially_received'"
                to="/transfer/transfer-list?intent=resubmit"
                class="button warning"
                >新建补交清单</router-link
              >
            </div>
            <p class="hint">
              {{
                selectedBatch.status === 'draft'
                  ? '草稿清单仍可编辑；提交后将变为只读并生成可打印清单。'
                  : '提交后的清单只读。工作台仅展示本单位进度，不展示后台库房架位。'
              }}
            </p>
          </div>

          <!-- 条目状态 -->
          <template v-if="detailItems.length > 0">
            <h3 class="section-title" style="margin-top: 16px">条目状态</h3>
            <div class="item-list">
              <article v-for="item in detailItems" :key="item.id" class="item-card">
                <div class="line">
                  <strong>{{ item.seqNo }}. {{ item.inputTitle }}</strong>
                  <span :class="['status', itemStatusClass(item.status)]">{{ itemStatusLabel(item.status) }}</span>
                </div>
                <span class="muted">档案文件名：{{ item.expectedFilename || '无' }}</span>
                <span v-if="item.rejectReason" class="status danger">回退原因：{{ item.rejectReason }}</span>
              </article>
            </div>
          </template>
        </aside>
      </section>
    </template>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { getTransferDashboard, getTransferBatches, getTransferBatchDetail, exportTransferBatch } from '@/api/transfer'
import type { TransferDashboard, TransferBatch, TransferBatchDetail } from '@/types/transfer'

const tabs = [
  { label: '全部', value: 'all' },
  { label: '草稿', value: 'draft' },
  { label: '待移交', value: 'pending_transfer' },
  { label: '部分接收', value: 'partially_received' },
  { label: '已入库', value: 'archived' },
  { label: '已上架', value: 'shelved' },
]

const loading = ref(true)
const loadError = ref('')
const dashboard = ref<TransferDashboard>({
  summary: { draft: 0, pendingTransfer: 0, partiallyReceived: 0, received: 0, archived: 0, shelved: 0, rejected: 0 },
  recentBatches: [],
})
const batches = ref<TransferBatch[]>([])
const selectedBatchId = ref<number | null>(null)
const detailData = ref<TransferBatchDetail | null>(null)

const keyword = ref('')
const activeFilter = ref('all')

const filteredBatches = computed(() => {
  let result = batches.value
  if (activeFilter.value !== 'all') {
    result = result.filter((b) => b.status === activeFilter.value)
  }
  const q = keyword.value.trim().toLowerCase()
  if (q) {
    result = result.filter(
      (b) =>
        b.batchNo.toLowerCase().includes(q) ||
        b.title.toLowerCase().includes(q) ||
        String(b.archiveYear).includes(q),
    )
  }
  return result
})

const selectedBatch = computed(() => {
  if (!selectedBatchId.value) return batches.value[0] || null
  return batches.value.find((b) => b.id === selectedBatchId.value) || batches.value[0] || null
})

const detailItems = computed(() => detailData.value?.items || [])

const progressSteps = computed(() => {
  const status = selectedBatch.value?.status || 'draft'
  const steps = [
    { label: '编制清单', desc: '提交后生成清单号' },
    { label: '前台验收', desc: '实物核对和电子文件匹配' },
    { label: '后台入库', desc: '生成正式档号' },
    { label: '确认上架', desc: '纸质相关档案开放利用' },
  ]
  const indexMap: Record<string, number> = {
    draft: 0,
    pending_transfer: 1,
    received: 2,
    partially_received: 2,
    archived: 3,
    shelved: 4,
    rejected: 1,
  }
  const reached = indexMap[status] ?? 0
  return steps.map((step, idx) => ({
    ...step,
    cls: idx < reached ? 'done' : idx === reached ? 'current' : '',
  }))
})

function statusClass(status: string): string {
  const map: Record<string, string> = {
    draft: '',
    pending_transfer: 'info',
    received: 'success',
    partially_received: 'warning',
    archived: 'success',
    shelved: 'success',
    rejected: 'danger',
  }
  return map[status] || ''
}

function itemStatusClass(status: string): string {
  const map: Record<string, string> = {
    draft: '',
    pending_acceptance: 'info',
    accepted: 'success',
    rejected: 'danger',
    archived: 'success',
  }
  return map[status] || ''
}

function itemStatusLabel(status: string): string {
  const map: Record<string, string> = {
    draft: '草稿',
    pending_acceptance: '待验收',
    accepted: '已接收',
    rejected: '已回退',
    archived: '已入库',
  }
  return map[status] || status
}

async function loadDetail() {
  const batch = selectedBatch.value
  if (!batch) return
  try {
    detailData.value = await getTransferBatchDetail(batch.id)
  } catch {
    detailData.value = null
  }
}

async function handleExport(batchId: number) {
  try {
    const blob = await exportTransferBatch(batchId)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `transfer-batch-${batchId}.pdf`
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    // 静默处理
  }
}

async function loadData() {
  loading.value = true
  loadError.value = ''
  try {
    const [dash, page] = await Promise.all([getTransferDashboard(), getTransferBatches()])
    dashboard.value = dash
    batches.value = page.records
    // 默认选中第一个批次
    if (batches.value.length > 0 && !selectedBatchId.value) {
      selectedBatchId.value = batches.value[0].id
    }
  } catch (e: any) {
    loadError.value = e.message || '未知错误'
  } finally {
    loading.value = false
  }
}

watch(selectedBatch, () => {
  loadDetail()
})

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.workspace {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(340px, 0.9fr);
  gap: 16px;
  align-items: start;
  min-width: 0;
}

.workspace > * {
  min-width: 0;
  max-width: 100%;
  box-sizing: border-box;
}

.batch-row {
  transition: background 0.16s ease;
}

.batch-row:hover,
.batch-row.active {
  background: #f2f8f8;
}

.batch-row button {
  width: 100%;
  padding: 0;
  border: 0;
  color: inherit;
  background: transparent;
  text-align: left;
}

.detail-head {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: flex-start;
  justify-content: space-between;
}

.detail-meta {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 8px;
  margin-top: 12px;
}

.meta-item {
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #f8fafb;
}

.meta-item span {
  display: block;
  color: var(--muted);
  font-size: 12px;
  font-weight: 700;
}

.meta-item strong {
  display: block;
  margin-top: 2px;
  font-size: 14px;
}

.progress-line {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 8px;
  margin: 14px 0;
}

.progress-node {
  min-height: 66px;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
}

.progress-node.done {
  border-color: #a8d8be;
  background: var(--success-soft);
}

.progress-node.current {
  border-color: #8abcbf;
  background: var(--primary-soft);
}

.progress-node strong {
  display: block;
  font-size: 13px;
}

.progress-node span {
  color: var(--muted);
  font-size: 12px;
}

.item-list {
  display: grid;
  gap: 8px;
  margin-top: 12px;
}

.item-card {
  display: grid;
  gap: 6px;
  padding: 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
}

.item-card .line {
  display: flex;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
}

.receipt-box {
  display: grid;
  gap: 10px;
  margin-top: 12px;
  padding: 12px;
  border: 1px dashed #b8c5ce;
  border-radius: var(--radius);
  background: #f7fafc;
}

@media (max-width: 1120px) {
  .workspace {
    grid-template-columns: 1fr;
  }
}
</style>
