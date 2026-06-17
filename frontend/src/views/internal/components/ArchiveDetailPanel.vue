<template>
  <div class="detail-panel">
    <div v-if="loading" class="notice">加载档案详情…</div>

    <div v-else-if="error" class="notice danger">
      {{ error }}
      <div class="actions" style="margin-top: 8px">
        <button class="button ghost" type="button" @click="loadDetail">重试</button>
      </div>
    </div>

    <div v-else-if="detail" class="drawer">
      <h2 class="section-title">{{ detail.title }}</h2>
      <div class="detail-grid">
        <div class="detail-line"><span class="muted">档号</span><strong>{{ detail.archiveNo }}</strong></div>
        <div class="detail-line"><span class="muted">分类</span><strong>{{ detail.categoryName }}</strong></div>
        <div class="detail-line"><span class="muted">责任者</span><strong>{{ detail.responsibleText }}</strong></div>
        <div class="detail-line"><span class="muted">形成日期</span><strong>{{ detail.formedDate }}</strong></div>
        <div class="detail-line"><span class="muted">密级</span><strong>{{ securityLabel(detail.securityLevel) }}</strong></div>
        <div class="detail-line"><span class="muted">载体状态</span><strong>{{ carrierLabel(detail.carrierStatus) }}</strong></div>
        <div class="detail-line"><span class="muted">纸质借阅</span><strong>{{ canBorrowPaper ? '可申请' : '不支持' }}</strong></div>
      </div>
      <p v-if="detail.summary" class="muted" style="margin-top: 8px">{{ detail.summary }}</p>
      <div class="notice" style="margin-top: 10px">如需纸质原件，请提交借阅申请，由管理员审批并到馆核验。</div>

      <div v-if="detail.files.length > 0" style="margin-top: 12px">
        <h3 class="section-title">电子文件</h3>
        <div v-for="file in detail.files" :key="file.id" class="local-file">
          <div>
            <strong>{{ file.originalFilename }}</strong>
            <div class="hint">{{ file.fileFormat }}，{{ formatSize(file.fileSize) }}，{{ file.fileRole }}</div>
          </div>
          <div class="actions">
            <button class="button secondary" type="button" :disabled="!file.canPreview || previewing" @click="handlePreview(file.id)">
              预览
            </button>
            <button class="button ghost" type="button" :disabled="!file.canDownload || downloading" @click="handleDownload(file)">
              下载
            </button>
          </div>
        </div>
        <div v-if="previewContent" class="notice" style="margin-top: 8px">
          <strong>预览内容</strong>
          <pre style="white-space: pre-wrap; font-size: 13px; margin-top: 4px">{{ previewContent }}</pre>
        </div>
      </div>
      <div v-else class="notice" style="margin-top: 12px">
        {{ detail.carrierStatus === 'paper' ? '纯纸质档案，暂无电子文件可供预览/下载。' : '暂无可预览/下载的电子文件。' }}
      </div>

      <div class="actions" style="margin-top: 12px">
        <button class="button" type="button" :disabled="!canBorrowPaper" :title="borrowHint" @click="toggleBorrowForm">
          {{ showBorrowForm ? '收起申请' : '申请借阅' }}
        </button>
      </div>

      <form v-if="showBorrowForm" class="borrow-form" @submit.prevent="submitBorrow">
        <div class="field">
          <label>借阅理由</label>
          <textarea v-model="borrowForm.reason" placeholder="说明借阅用途和必要性"></textarea>
        </div>
        <div class="form-grid">
          <div class="field">
            <label>借阅天数</label>
            <input v-model.number="borrowForm.expectedDays" type="number" min="1" />
          </div>
          <div class="field">
            <label>到馆时间</label>
            <input v-model="borrowForm.expectedVisitAt" type="datetime-local" />
          </div>
        </div>
        <div class="field">
          <label>联系电话</label>
          <input v-model="borrowForm.contactPhone" placeholder="11 位手机号" />
        </div>
        <ul v-if="borrowErrors.length" class="notice danger">
          <li v-for="msg in borrowErrors" :key="msg">{{ msg }}</li>
        </ul>
        <div class="actions">
          <button class="button" type="submit" :disabled="submitting">{{ submitting ? '提交中…' : '提交申请' }}</button>
          <button class="button ghost" type="button" @click="showBorrowForm = false">取消</button>
        </div>
      </form>
    </div>

    <div v-else class="notice">点击左侧结果行的「详情」查看档案信息。</div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import {
  createBorrowRequest,
  downloadInternalFile,
  getInternalArchiveDetail,
  previewInternalFile,
} from '@/api/internal'
import { validateBorrowRequest } from '@/utils/borrowValidation'
import { CarrierStatusLabel, SecurityLevelLabel } from '@/types/enums'
import type { InternalArchiveDetail, InternalFile } from '@/types/internal'

const props = defineProps<{ archiveId: number | null }>()
const emit = defineEmits<{ borrowed: [] }>()

const loading = ref(false)
const error = ref('')
const detail = ref<InternalArchiveDetail | null>(null)

const previewing = ref(false)
const previewContent = ref('')
const downloading = ref(false)

const showBorrowForm = ref(false)
const submitting = ref(false)
const borrowErrors = ref<string[]>([])
const borrowForm = reactive({
  reason: '',
  expectedDays: 7,
  expectedVisitAt: '',
  contactPhone: '',
})

function securityLabel(level: number): string {
  return SecurityLevelLabel[level] ?? '未知'
}
// 取浏览器本地时区偏移字符串，如 "+08:00"、"-05:00"。用于把 datetime-local 的无时区值
// 补成后端 OffsetDateTime 能解析的格式。
function getCurrentOffset(): string {
  const offsetMin = -new Date().getTimezoneOffset()
  const sign = offsetMin >= 0 ? '+' : '-'
  const abs = Math.abs(offsetMin)
  const hh = String(Math.floor(abs / 60)).padStart(2, '0')
  const mm = String(abs % 60).padStart(2, '0')
  return `${sign}${hh}:${mm}`
}
function carrierLabel(status: string): string {
  return CarrierStatusLabel[status] ?? status
}
// 纸质借阅资格兜底：后端详情当前不返回 canBorrow/borrowHint（人#6），前端按 §11.7
// 的借阅前置条件近似推断——只有载体含纸质（paper / paper_electronic）的档案可申请纸质借阅，
// 纯电子档案不可借。密级/单位/盘点等强约束仍由后端 11.7 提交时强制校验。
const canBorrowPaper = computed(() => {
  if (!detail.value) return false
  return detail.value.carrierStatus === 'paper' || detail.value.carrierStatus === 'paper_electronic'
})
const borrowHint = computed(() => {
  if (!detail.value) return ''
  if (canBorrowPaper.value) return '可申请纸质借阅，由管理员审批并到馆核验'
  return '纯电子档案不支持纸质借阅'
})
function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

async function loadDetail() {
  if (props.archiveId == null) {
    detail.value = null
    loading.value = false
    return
  }
  loading.value = true
  error.value = ''
  detail.value = null
  previewContent.value = ''
  showBorrowForm.value = false
  try {
    detail.value = await getInternalArchiveDetail(props.archiveId)
  } catch (e: unknown) {
    error.value = e instanceof Error ? e.message : '加载档案详情失败'
  } finally {
    loading.value = false
  }
}

watch(() => props.archiveId, loadDetail, { immediate: true })

function toggleBorrowForm() {
  showBorrowForm.value = !showBorrowForm.value
}

async function handlePreview(fileId: number) {
  previewing.value = true
  previewContent.value = ''
  try {
    previewContent.value = await previewInternalFile(fileId)
  } catch {
    ElMessage.error('预览失败，请稍后重试')
  } finally {
    previewing.value = false
  }
}

async function handleDownload(file: InternalFile) {
  downloading.value = true
  try {
    const blob = await downloadInternalFile(file.id)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = file.originalFilename
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    ElMessage.error('下载失败，请稍后重试')
  } finally {
    downloading.value = false
  }
}

async function submitBorrow() {
  if (!detail.value) return
  // datetime-local 控件产出 "YYYY-MM-DDTHH:mm"（无时区），后端 expectedVisitAt 为 OffsetDateTime，
  // 必须补本地时区偏移（如 +08:00）才能被 Jackson 反序列化。
  // 注意：不能用 new Date(rawVisit)——它把无时区字符串当 UTC 解析，再 toISOString 会偏移 8 小时。
  // 直接在原始字符串末尾拼接本地时区偏移即可。
  const rawVisit = borrowForm.expectedVisitAt
  const expectedVisitAt = rawVisit ? `${rawVisit}${getCurrentOffset()}` : rawVisit
  const data = {
    archiveId: detail.value.archiveId,
    reason: borrowForm.reason,
    expectedDays: borrowForm.expectedDays,
    expectedVisitAt,
    contactPhone: borrowForm.contactPhone,
  }
  const errors = validateBorrowRequest(data)
  borrowErrors.value = errors
  if (errors.length) return
  submitting.value = true
  try {
    const created = await createBorrowRequest(data)
    ElMessage.success(`借阅申请 ${created.requestNo} 已提交，等待审批`)
    showBorrowForm.value = false
    borrowForm.reason = ''
    emit('borrowed')
  } catch {
    ElMessage.error('提交借阅申请失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.detail-panel {
  display: grid;
  gap: 12px;
}
.detail-grid {
  display: grid;
  gap: 10px;
}
.detail-line {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  padding-bottom: 9px;
  border-bottom: 1px solid var(--border);
}
.detail-line:last-child {
  border-bottom: 0;
}
.borrow-form {
  display: grid;
  gap: 10px;
  margin-top: 12px;
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
  margin-top: 8px;
}
</style>
