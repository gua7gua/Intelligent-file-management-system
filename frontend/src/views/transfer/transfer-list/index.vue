<template>
  <div>
    <section>
      <h1 class="page-title">编制移交清单</h1>
      <p class="page-subtitle">
        填写清单级信息和条目字段。
      </p>
    </section>

    <div class="edit-layout" style="margin-top: 18px">
      <div class="grid">
        <!-- 清单基础信息 -->
        <section class="card panel">
          <div class="status-line">
            <h2 class="section-title" style="margin: 0">清单基础信息</h2>
            <span :class="['status', submitted ? 'info' : '']">{{ submitted ? '待移交' : '草稿' }}</span>
          </div>
          <div class="form-grid" style="margin-top: 14px">
            <div class="field">
              <label for="listTitle">清单标题</label>
              <input id="listTitle" v-model="batch.title" :readonly="submitted" />
            </div>
            <div class="field">
              <label for="department">移交部门</label>
              <input id="department" v-model="batch.departmentName" :readonly="submitted" />
            </div>
            <div class="field">
              <label for="contactPhone">联系电话</label>
              <input id="contactPhone" v-model="batch.contactPhone" :readonly="submitted" />
            </div>
            <div class="field">
              <label for="archiveYear">档案所属年度</label>
              <input id="archiveYear" v-model.number="batch.archiveYear" type="number" min="1900" max="2099" :readonly="submitted" />
            </div>
            <div class="field">
              <label for="expectedDate">预计到馆移交日期</label>
              <input id="expectedDate" v-model="batch.expectedTransferDate" type="date" :readonly="submitted" />
            </div>
          </div>
        </section>

        <!-- 本地文件解析 -->
        <section class="card panel">
          <h2 class="section-title">本地解析电子文件</h2>
          <div
            :class="['drop-zone', { dragover: isDragOver }]"
            @dragenter.prevent="isDragOver = true"
            @dragover.prevent="isDragOver = true"
            @dragleave.prevent="isDragOver = false"
            @drop.prevent="handleDrop"
          >
            <div>
              <strong>拖拽文件到这里，或选择本地文件</strong>
              <p class="muted" style="margin: 6px 0 0">
                文件不会上传，仅用于辅助填表。
              </p>
            </div>
            <label class="button secondary" for="filePicker">选择文件</label>
            <input id="filePicker" class="sr-only" type="file" multiple @change="handleFileSelect" />
          </div>
          <div v-if="localFiles.length > 0" class="local-file-list" aria-live="polite">
            <div v-for="(file, idx) in localFiles" :key="idx" class="local-file">
              <div>
                <strong>{{ file.expectedFilename }}</strong>
                <div class="hint">{{ file.electronicFormat || '未知格式' }}，{{ formatSize(file.localFileSize) }}</div>
              </div>
              <span class="status info">未上传</span>
            </div>
          </div>
        </section>

        <!-- 清单条目 -->
        <section>
          <div class="toolbar" style="margin-top: 0">
            <h2 class="section-title" style="margin: 0">清单条目</h2>
            <div class="actions">
              <button class="button ghost" type="button" :disabled="submitted" @click="addBlankItem">新增空白条目</button>
              <button class="button secondary" type="button" @click="validate">校验清单</button>
            </div>
          </div>
          <div class="table-wrap scroll-y">
            <table class="editable-table">
              <thead>
                <tr>
                  <th>序号</th>
                  <th>档案标题</th>
                  <th>保管期限</th>
                  <th>载体状态</th>
                  <th>电子格式</th>
                  <th>页数</th>
                  <th>保密级别</th>
                  <th>是否公开</th>
                  <th>档案文件名</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="items.length === 0">
                  <td colspan="10"><div class="empty">暂无条目，请拖拽文件或新增空白条目</div></td>
                </tr>
                <tr v-for="(item, index) in items" :key="index">
                  <td>{{ index + 1 }}</td>
                  <td>
                    <input
                      v-model="item.inputTitle"
                      class="title-input"
                      :readonly="submitted"
                    />
                  </td>
                  <td>
                    <select v-model="item.retentionPeriod" :disabled="submitted">
                      <option value="">请选择</option>
                      <option value="10y">10 年</option>
                      <option value="30y">30 年</option>
                      <option value="permanent">永久</option>
                    </select>
                  </td>
                  <td>
                    <select v-model="item.carrierStatus" :disabled="submitted">
                      <option value="">请选择</option>
                      <option value="electronic">纯电子</option>
                      <option value="paper_electronic">纸质+电子</option>
                      <option value="paper">纯纸质</option>
                    </select>
                  </td>
                  <td>
                    <input v-model="item.electronicFormat" :readonly="submitted" />
                  </td>
                  <td>
                    <input v-model.number="item.pageCount" type="number" min="0" :readonly="submitted" placeholder="页数" />
                  </td>
                  <td>
                    <select v-model.number="item.securityLevel" :disabled="submitted">
                      <option :value="0">非密</option>
                      <option :value="1">内部</option>
                      <option :value="2">秘密</option>
                      <option :value="3">机密</option>
                      <option :value="4">绝密</option>
                    </select>
                  </td>
                  <td>
                    <select v-model="item.openStatus" :disabled="submitted">
                      <option value="open">公开</option>
                      <option value="closed">不公开</option>
                    </select>
                  </td>
                  <td>
                    <input v-model="item.expectedFilename" class="file-input" :readonly="submitted" />
                  </td>
                  <td>
                    <button
                      class="button ghost"
                      type="button"
                      :disabled="submitted"
                      @click="removeItem(index)"
                    >
                      删除
                    </button>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </div>

      <!-- 右侧提交面板 -->
      <aside class="submit-panel">
        <section class="card panel">
          <h2 class="section-title">提交检查</h2>
          <div :class="['notice', validationType]">
            <template v-if="validationErrors.length > 0">
              <strong>发现 {{ validationErrors.length }} 个问题</strong>
              <ul class="validation-list">
                <li v-for="(err, idx) in validationErrors" :key="idx">{{ err }}</li>
              </ul>
            </template>
            <template v-else>
              提交前请确认各项信息填写完整。
            </template>
          </div>
          <div class="actions" style="margin-top: 12px">
            <button class="button ghost" type="button" :disabled="submitted" @click="saveDraft">保存草稿</button>
            <button class="button" type="button" :disabled="submitted" @click="submitList">提交清单</button>
            <button v-if="submitted" class="button secondary" type="button" @click="exportList">导出打印清单</button>
            <button v-if="submitted" class="button" type="button" @click="startNewList">新建清单</button>
          </div>
          <p class="hint">提交后清单将不可再编辑。</p>
        </section>

        <section class="card panel">
          <h2 class="section-title">线下交接提醒</h2>
          <ul class="timeline">
            <li><span class="muted">提交后</span><strong>导出 PDF 清单</strong></li>
            <li><span class="muted">到馆时</span><strong>携带纸质原件、U 盘、签字清单</strong></li>
            <li><span class="muted">前台</span><strong>验收实物并上传 U 盘文件</strong></li>
          </ul>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import { parseLocalFiles } from '@/utils/fileParser'
import type { ParsedLocalFile } from '@/utils/fileParser'
import { validateTransferDraft } from '@/utils/transferValidation'
import {
  createTransferBatch,
  updateTransferBatch,
  submitTransferBatch,
  exportTransferBatch,
  getTransferBatchDetail,
} from '@/api/transfer'
import type { TransferItem } from '@/types/transfer'

const route = useRoute()
const authStore = useAuthStore()

const batch = reactive({
  title: '',
  departmentName: '',
  contactPhone: '',
  archiveYear: undefined as number | undefined,
  expectedTransferDate: '',
})

const items = ref<(TransferItem & { localFileSize?: number })[]>([])
const localFiles = ref<ParsedLocalFile[]>([])
const isDragOver = ref(false)
const submitted = ref(false)
const batchId = ref<number | null>(null)
const validationErrors = ref<string[]>([])
const validationType = ref('')

// 继续编辑：从路由 ?batchId= 加载已有草稿回填表单
async function loadDraftForEdit(id: number) {
  try {
    const detail = await getTransferBatchDetail(id)
    batchId.value = detail.id
    batch.title = detail.title
    batch.departmentName = detail.departmentName
    batch.contactPhone = detail.contactPhone
    batch.archiveYear = detail.archiveYear
    batch.expectedTransferDate = detail.expectedTransferDate
    items.value = detail.items.map((it) => ({ ...it }))
    // 非草稿状态（已提交等）只读展示
    submitted.value = detail.status !== 'draft'
  } catch (e) {
    ElMessage.error((e as Error).message || '加载清单草稿失败')
  }
}

onMounted(() => {
  const id = Number(route.query.batchId)
  if (id > 0) {
    loadDraftForEdit(id)
  } else {
    // 2.1/2.2：新建清单时预填当前账号的部门与电话，减少手工录入
    const u = authStore.user
    if (u) {
      if (!batch.departmentName && u.departmentName) batch.departmentName = u.departmentName
      if (!batch.contactPhone && u.phone) batch.contactPhone = u.phone
    }
  }
})

function addBlankItem() {
  if (submitted.value) return
  items.value.push({
    id: 0,
    batchId: 0,
    seqNo: items.value.length + 1,
    inputTitle: '',
    retentionPeriod: '10y',
    carrierStatus: 'paper',
    securityLevel: 0,
    openStatus: 'closed',
    allowDigitization: true,
    status: 'draft',
  })
}

function removeItem(index: number) {
  if (submitted.value) return
  items.value.splice(index, 1)
}

function handleFileSelect(event: Event) {
  const input = event.target as HTMLInputElement
  if (input.files) appendFiles(input.files)
}

function handleDrop(event: DragEvent) {
  isDragOver.value = false
  if (event.dataTransfer?.files) appendFiles(event.dataTransfer.files)
}

function appendFiles(files: FileList) {
  const parsed = parseLocalFiles(Array.from(files))
  localFiles.value.push(...parsed)
  parsed.forEach((p) => {
    items.value.push({
      id: 0,
      batchId: 0,
      seqNo: items.value.length + 1,
      inputTitle: p.inputTitle,
      retentionPeriod: '',
      carrierStatus: '',
      securityLevel: 0,
      openStatus: 'closed' as const,
      allowDigitization: true,
      electronicFormat: p.electronicFormat,
      expectedFilename: p.expectedFilename,
      status: 'draft' as const,
      localFileSize: p.localFileSize,
    })
  })
}

function formatSize(bytes: number): string {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let size = bytes
  let unit = 0
  while (size >= 1024 && unit < units.length - 1) {
    size /= 1024
    unit++
  }
  return `${size.toFixed(unit === 0 ? 0 : 1)} ${units[unit]}`
}

function validate() {
  const errors = validateTransferDraft({
    title: batch.title,
    departmentName: batch.departmentName,
    contactPhone: batch.contactPhone,
    archiveYear: batch.archiveYear,
    expectedTransferDate: batch.expectedTransferDate,
    items: items.value.map((item) => ({
      inputTitle: item.inputTitle,
      retentionPeriod: item.retentionPeriod,
      carrierStatus: item.carrierStatus,
      securityLevel: item.securityLevel,
      openStatus: item.openStatus,
      allowDigitization: item.allowDigitization,
    })),
  })
  validationErrors.value = errors
  validationType.value = errors.length > 0 ? 'danger' : ''
  return errors.length === 0
}

async function saveDraft() {
  try {
    const payload = {
      ...batch,
      archiveYear: batch.archiveYear ?? 0,
    }
    if (batchId.value) {
      await updateTransferBatch(batchId.value, {
        ...payload,
        id: batchId.value,
        batchNo: '',
        sourceType: 'transfer' as const,
        status: 'draft' as const,
        statusText: '草稿',
        organizationName: '',
        contactName: '',
        itemCount: items.value.length,
        acceptedCount: 0,
        rejectedCount: 0,
        items: items.value,
      })
    } else {
      const result = await createTransferBatch({
        ...payload,
        id: 0,
        batchNo: '',
        sourceType: 'transfer' as const,
        status: 'draft' as const,
        statusText: '草稿',
        organizationName: '',
        contactName: '',
        itemCount: items.value.length,
        acceptedCount: 0,
        rejectedCount: 0,
        items: items.value,
      })
      batchId.value = result.id
    }
    ElMessage.success('草稿已保存')
  } catch (e) {
    ElMessage.error((e as Error).message || '保存草稿失败')
  }
}

async function submitList() {
  // 提交前校验：涉密档案（securityLevel > 0）不允许设置为公开，
  // 由 validateTransferDraft 给出明确报错，不再静默改写 openStatus。
  if (!validate()) return
  try {
    const payload = {
      ...batch,
      archiveYear: batch.archiveYear ?? 0,
    }
    if (!batchId.value) {
      const created = await createTransferBatch({
        ...payload,
        id: 0,
        batchNo: '',
        sourceType: 'transfer' as const,
        status: 'draft' as const,
        statusText: '草稿',
        organizationName: '',
        contactName: '',
        itemCount: items.value.length,
        acceptedCount: 0,
        rejectedCount: 0,
        items: items.value,
      })
      batchId.value = created.id
    } else {
      // 草稿已存在时，先把最新条目（含密级/公开等修改）同步到后端，
      // 否则 submit 仅触发状态机，后端用的是旧草稿数据，校验会失真。
      await updateTransferBatch(batchId.value, {
        ...payload,
        id: batchId.value,
        batchNo: '',
        sourceType: 'transfer' as const,
        status: 'draft' as const,
        statusText: '草稿',
        organizationName: '',
        contactName: '',
        itemCount: items.value.length,
        acceptedCount: 0,
        rejectedCount: 0,
        items: items.value,
      })
    }
    await submitTransferBatch(batchId.value!)
    submitted.value = true
    validationErrors.value = []
    validationType.value = ''
    ElMessage.success('移交清单已提交')
  } catch (e) {
    ElMessage.error((e as Error).message || '提交失败，请稍后重试')
  }
}

async function exportList() {
  if (!batchId.value) return
  try {
    const blob = await exportTransferBatch(batchId.value)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = `transfer-batch-${batchId.value}.pdf`
    a.click()
    URL.revokeObjectURL(url)
  } catch (e) {
    ElMessage.error((e as Error).message || '导出失败')
  }
}

// 2.6：提交后清空表单开始编制新清单，无需刷新页面
function startNewList() {
  batchId.value = null
  submitted.value = false
  batch.title = ''
  batch.archiveYear = undefined
  batch.expectedTransferDate = ''
  items.value = []
  localFiles.value = []
  validationErrors.value = []
  // 复用 2.1/2.2 预填：部门/电话沿用当前账号
  const u = authStore.user
  batch.departmentName = u?.departmentName ?? ''
  batch.contactPhone = u?.phone ?? ''
  ElMessage.success('已开始新清单，请继续编制。')
}
</script>

<style scoped>
.edit-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  align-items: start;
}

.edit-layout > .grid,
.edit-layout > .submit-panel,
.edit-layout > .grid > .card,
.edit-layout > .grid > section {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.drop-zone {
  display: grid;
  min-height: 176px;
  place-items: center;
  gap: 12px;
  padding: 22px;
  border: 2px dashed #9bbdc0;
  border-radius: var(--radius);
  color: #23494f;
  background: #f7fcfc;
  text-align: center;
  transition: border-color 0.16s ease, background 0.16s ease;
}

.drop-zone.dragover {
  border-color: var(--primary);
  background: var(--primary-soft);
}

.drop-zone strong {
  display: block;
  font-size: 17px;
}

.local-file-list {
  display: grid;
  gap: 8px;
  max-height: 220px;
  margin-top: 12px;
  overflow-y: auto;
}

.local-file {
  display: flex;
  gap: 10px;
  align-items: center;
  justify-content: space-between;
  padding: 9px 10px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
}

.editable-table {
  min-width: 900px;
}

.editable-table input,
.editable-table select {
  width: 100%;
  min-width: 80px;
  min-height: 34px;
  padding: 6px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #ffffff;
}

.editable-table .title-input {
  min-width: 160px;
}

.editable-table .file-input {
  min-width: 140px;
}

.validation-list {
  display: grid;
  gap: 8px;
  margin: 0;
  padding-left: 18px;
}

.submit-panel {
  position: sticky;
  top: 86px;
  display: grid;
  gap: 12px;
}

.status-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

@media (max-width: 1160px) {
  .edit-layout {
    grid-template-columns: 1fr;
  }

  .submit-panel {
    position: static;
  }
}
</style>
