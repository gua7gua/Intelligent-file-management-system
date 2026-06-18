<template>
  <div>
    <section>
      <h1 class="page-title">征集清单</h1>
      <p class="page-subtitle">填写捐赠信息并登记相关电子文件。</p>
    </section>

    <!-- 我的征集清单：回查已提交批次号与进度 -->
    <section v-if="myCollections.length > 0" class="card panel" style="margin-top: 18px">
      <div class="toolbar" style="margin-top: 0">
        <h2 class="section-title" style="margin: 0">我的征集清单</h2>
        <span class="muted" style="font-size: 13px">共 {{ myCollections.length }} 条</span>
      </div>
      <div class="table-wrap" style="margin-top: 12px">
        <table>
          <thead>
            <tr>
              <th>清单号</th>
              <th>标题</th>
              <th>条目</th>
              <th>状态</th>
              <th>提交时间</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="c in myCollections" :key="c.id">
              <td class="mono">{{ c.batchNo }}</td>
              <td>{{ c.title }}</td>
              <td>{{ c.itemCount }} 件</td>
              <td><span :class="['status', myColStatusClass(c.status)]">{{ c.statusText || c.status }}</span></td>
              <td>{{ formatMyDateTime(c.submittedAt) }}</td>
            </tr>
          </tbody>
        </table>
      </div>
    </section>

    <div class="edit-layout" style="margin-top: 18px">
      <div class="grid">
        <!-- 捐赠信息 -->
        <section class="card panel">
          <h2 class="section-title">捐赠信息</h2>
          <div class="form-grid" style="margin-top: 14px">
            <div class="field">
              <label for="colTitle">清单标题</label>
              <input id="colTitle" v-model="draft.title" :readonly="submitted" />
            </div>
            <div class="field">
              <label for="contactName">联系人</label>
              <input id="contactName" v-model="draft.contactName" :readonly="submitted" />
            </div>
            <div class="field">
              <label for="contactPhone">联系电话</label>
              <input id="contactPhone" v-model="draft.contactPhone" :readonly="submitted" />
            </div>
            <div class="field">
              <label for="archiveYear">档案所属年度</label>
              <input id="archiveYear" v-model.number="draft.archiveYear" type="number" min="1900" max="2099" placeholder="如 1980" :readonly="submitted" />
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
              <p class="muted" style="margin: 6px 0 0">文件不会上传，仅用于辅助填写。</p>
            </div>
            <label class="button secondary" for="colFilePicker">选择文件</label>
            <input id="colFilePicker" class="sr-only" type="file" multiple @change="handleFileSelect" />
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

        <!-- 条目列表 -->
        <section>
          <div class="toolbar" style="margin-top: 0">
            <h2 class="section-title" style="margin: 0">征集条目</h2>
            <div class="actions">
              <button class="button ghost" type="button" :disabled="submitted" @click="addBlankItem">新增空白条目</button>
              <button class="button secondary" type="button" @click="validateDraft">校验清单</button>
            </div>
          </div>
          <div class="table-wrap scroll-y">
            <table class="editable-table">
              <thead>
                <tr>
                  <th>序号</th>
                  <th>档案标题</th>
                  <th>载体状态</th>
                  <th>电子格式</th>
                  <th>档案文件名</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="items.length === 0">
                  <td colspan="6"><div class="empty">暂无条目，请拖拽文件或新增空白条目</div></td>
                </tr>
                <tr v-for="(item, index) in items" :key="index">
                  <td>{{ index + 1 }}</td>
                  <td><input v-model="item.inputTitle" :readonly="submitted" /></td>
                  <td>
                    <select v-model="item.carrierStatus" :disabled="submitted">
                      <option value="">请选择</option>
                      <option value="electronic">纯电子</option>
                      <option value="paper_electronic">纸质+电子</option>
                      <option value="paper">纯纸质</option>
                    </select>
                  </td>
                  <td><input v-model="item.electronicFormat" :readonly="submitted" /></td>
                  <td><input v-model="item.expectedFilename" :readonly="submitted" /></td>
                  <td>
                    <button class="button ghost" type="button" :disabled="submitted" @click="removeItem(index)">删除</button>
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
            <template v-else>已准备校验。提交前请确认载体状态等信息。</template>
          </div>
          <label style="display:flex;gap:8px;align-items:flex-start;margin-top:12px;font-size:14px;cursor:pointer">
            <input type="checkbox" v-model="agreed" :disabled="submitted" style="margin-top:3px" />
            <span>我已阅读并同意在线捐赠协议，知悉提交后清单转为只读、由档案馆联系约定到馆时间</span>
          </label>
          <div class="actions" style="margin-top: 12px">
            <button class="button ghost" type="button" :disabled="submitted" @click="saveDraft">保存草稿</button>
            <button class="button" type="button" :disabled="submitted || !agreed" @click="submitCollection">提交捐赠意向</button>
          </div>
          <p class="hint">提交后清单变为只读，后台管理员将联系您约定到馆时间。</p>
          <div v-if="submitted && submittedBatchNo" class="notice success" style="margin-top: 12px">
            <strong>✓ 已提交，批次号 {{ submittedBatchNo }}</strong>
            <p style="margin: 4px 0 0; font-size: 13px">请记下批次号以便后续查询；进度可在上方「我的征集清单」或「公众概览」查看。</p>
          </div>
        </section>

        <section class="notice warning">
          <p style="margin: 0">提交后工作人员将与您联系。</p>
        </section>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { parseLocalFiles } from '@/utils/fileParser'
import type { ParsedLocalFile } from '@/utils/fileParser'
import { validateCollectionDraft } from '@/utils/publicValidation'
import { createCollectionDraft, updateCollectionDraft, submitCollectionBatch, getMyCollections } from '@/api/public'
import type { PublicCollectionItem, PublicCollectionBatch } from '@/types/public'

interface DraftItem extends PublicCollectionItem {
  localFileSize?: number
}

const draft = reactive({
  title: '',
  contactName: '',
  contactPhone: '',
  archiveYear: undefined as number | undefined,
  agreementAccepted: false,
})

const items = ref<DraftItem[]>([])
const localFiles = ref<ParsedLocalFile[]>([])
const isDragOver = ref(false)
const submitted = ref(false)
const agreed = ref(false)
const batchId = ref<number | null>(null)
const validationErrors = ref<string[]>([])
const validationType = ref('')
// 我的征集清单（回查）+ 提交后回显批次号
const myCollections = ref<PublicCollectionBatch[]>([])
const submittedBatchNo = ref('')

function addBlankItem() {
  if (submitted.value) return
  items.value.push({
    id: 0,
    seqNo: items.value.length + 1,
    inputTitle: '',
    carrierStatus: 'paper',
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
      seqNo: items.value.length + 1,
      inputTitle: p.inputTitle,
      carrierStatus: '',
      electronicFormat: p.electronicFormat,
      expectedFilename: p.expectedFilename,
      status: 'draft',
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

function validateDraft() {
  const errors = validateCollectionDraft({
    ...draft,
    items: items.value,
  })
  validationErrors.value = errors
  validationType.value = errors.length > 0 ? 'danger' : ''
  return errors.length === 0
}

function buildBatchPayload(): PublicCollectionBatch {
  return {
    id: batchId.value ?? 0,
    batchNo: '',
    title: draft.title,
    contactName: draft.contactName,
    contactPhone: draft.contactPhone,
    archiveYear: draft.archiveYear ?? 0,
    status: 'draft',
    statusText: '草稿',
    itemCount: items.value.length,
    items: items.value,
  }
}

async function saveDraft() {
  try {
    const payload = buildBatchPayload()
    if (batchId.value) {
      await updateCollectionDraft(batchId.value, payload)
    } else {
      const result = await createCollectionDraft(payload)
      batchId.value = result.id
    }
    ElMessage.success('草稿已保存')
  } catch (e) {
    ElMessage.error((e as Error).message || '保存草稿失败')
  }
}

async function submitCollection() {
  if (!agreed.value) {
    ElMessage.warning('请先勾选同意在线捐赠协议')
    return
  }
  draft.agreementAccepted = true
  if (!validateDraft()) return
  try {
    if (!batchId.value) {
      const created = await createCollectionDraft(buildBatchPayload())
      batchId.value = created.id
    }
    const result = await submitCollectionBatch(batchId.value!)
    submitted.value = true
    submittedBatchNo.value = result?.batchNo || ''
    validationErrors.value = []
    validationType.value = ''
    await loadMyCollections()
    ElMessage.success(submittedBatchNo.value ? `征集清单已提交，批次号 ${submittedBatchNo.value}` : '征集清单已提交')
  } catch (e) {
    ElMessage.error((e as Error).message || '提交失败，请稍后重试')
  }
}

// 我的征集清单：进入页面加载 + 提交后刷新，让公众在征集页即可回查批次号与状态
async function loadMyCollections() {
  try {
    const page = await getMyCollections({ pageNo: 1, pageSize: 20 })
    myCollections.value = page.records
  } catch {
    // 静默失败：回查列表不应阻断提交主流程
  }
}

function myColStatusClass(status: string): string {
  const map: Record<string, string> = {
    draft: '',
    pending_contact: 'info',
    pending_receive: 'info',
    received: 'success',
    partially_received: 'warning',
    shelved: 'success',
    rejected: 'danger',
  }
  return map[status] || ''
}

function formatMyDateTime(iso?: string): string {
  if (!iso) return '未提交'
  try {
    return new Date(iso).toLocaleString('zh-CN')
  } catch {
    return iso
  }
}

onMounted(() => {
  loadMyCollections()
})
</script>

<style scoped>
.edit-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 340px;
  gap: 16px;
  align-items: start;
}

.edit-layout > .grid,
.edit-layout > .submit-panel {
  width: 100%;
  max-width: 100%;
  min-width: 0;
  box-sizing: border-box;
}

.drop-zone {
  display: grid;
  min-height: 160px;
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
  min-width: 700px;
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

@media (max-width: 1160px) {
  .edit-layout {
    grid-template-columns: 1fr;
  }

  .submit-panel {
    position: static;
  }
}
</style>
