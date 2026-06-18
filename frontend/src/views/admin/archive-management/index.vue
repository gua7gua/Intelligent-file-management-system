<template>
  <div class="archive-management">
    <section>
      <h1 class="page-title">档案管理</h1>
      <p class="page-subtitle">
        正式档案查询、元数据整理和密级/开放调整发起。
      </p>
    </section>

    <section class="manage-layout">
      <!-- 左栏：分类树 -->
      <aside class="card panel tree">
        <h2 class="section-title">固定档案门类</h2>
        <button
          v-for="cat in categoryTree"
          :key="cat.id"
          class="button ghost"
          :class="{ 'button-active': selectedCategoryId === cat.id }"
          type="button"
          @click="selectCategory(cat.id)"
        >
          {{ cat.name }}
        </button>
      </aside>

      <!-- 中栏：筛选 + 列表 -->
      <section class="grid">
        <div class="card panel">
          <h2 class="section-title">筛选条件</h2>
          <div class="form-grid">
            <div class="field">
              <label>关键词</label>
              <input v-model="query.keyword" placeholder="题名、档号、责任者" />
            </div>
            <div class="field">
              <label>年度</label>
              <input v-model.number="query.year" type="number" placeholder="2025" />
            </div>
            <div class="field">
              <label>密级</label>
              <select v-model="query.securityLevel">
                <option value="">全部</option>
                <option v-for="(label, val) in SecurityLevelLabel" :key="val" :value="Number(val)">{{ label }}</option>
              </select>
            </div>
            <div class="field">
              <label>开放状态</label>
              <select v-model="query.openStatus">
                <option value="">全部</option>
                <option value="open">公开</option>
                <option value="closed">不公开</option>
              </select>
            </div>
            <div class="field">
              <label>载体状态</label>
              <select v-model="query.carrierStatus">
                <option value="">全部</option>
                <option v-for="(label, val) in CarrierStatusLabel" :key="val" :value="val">{{ label }}</option>
              </select>
            </div>
          </div>
          <div class="actions" style="margin-top:12px">
            <el-button type="primary" @click="handleSearch">查询</el-button>
            <el-button @click="handleReset">重置</el-button>
          </div>
        </div>

        <div class="card panel">
          <h2 class="section-title">档案列表</h2>
          <div v-if="listLoading" class="detail-empty">加载中...</div>
          <div v-else-if="archives.length === 0" class="detail-empty">暂无匹配的正式档案</div>
          <div v-else class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>档号</th>
                  <th>题名</th>
                  <th>年度</th>
                  <th>分类</th>
                  <th>所属全宗</th>
                  <th>密级</th>
                  <th>开放</th>
                  <th>载体</th>
                  <th>状态</th>
                  <th>标签</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="a in archives"
                  :key="a.id"
                  :class="{ 'row-active': selectedArchive?.id === a.id }"
                  style="cursor:pointer"
                  @click="selectArchive(a)"
                >
                  <td>{{ a.archiveNo }}</td>
                  <td>{{ a.title }}</td>
                  <td>{{ a.formedYear ?? '—' }}</td>
                  <td>{{ a.categoryName }}</td>
                  <td>{{ a.fondsName || a.organizationName || '—' }}</td>
                  <td>{{ SecurityLevelLabel[a.securityLevel] || '未知' }}</td>
                  <td>{{ a.openStatus === 'open' ? '公开' : '不公开' }}</td>
                  <td>{{ CarrierStatusLabel[a.carrierStatus] || a.carrierStatus }}</td>
                  <td>
                    <span class="status" :class="lifeClass(a.lifecycleStatus)">
                      {{ ArchiveStatusLabel[a.lifecycleStatus] || a.lifecycleStatus }}
                    </span>
                  </td>
                  <td>{{ a.tags?.join('、') || '—' }}</td>
                </tr>
              </tbody>
            </table>
            <div style="display:flex;justify-content:flex-end;margin-top:12px">
              <el-pagination
                v-model:current-page="pageNo"
                v-model:page-size="pageSize"
                :total="total"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="loadArchives"
                @current-change="loadArchives"
              />
            </div>
          </div>
        </div>
      </section>

      <!-- 详情弹窗：居中展示，便于查看与复制档号（原右侧抽屉易遮挡、点档号误触） -->
      <el-dialog
        v-model="detailDrawerVisible"
        title="档案详情"
        width="720px"
        :append-to-body="true"
        align-center
        destroy-on-close
      >
        <div v-if="detailLoading" class="detail-empty">加载中...</div>
        <div v-else-if="selectedArchive" class="drawer-body">
          <h2 class="section-title">{{ detail?.title || selectedArchive.title }}</h2>
          <div class="detail-kv"><span>档号</span><strong>{{ detail?.archiveNo || selectedArchive.archiveNo }}</strong></div>
          <div class="detail-kv"><span>生命周期</span><strong>{{ ArchiveStatusLabel[detail?.lifecycleStatus || 'normal'] }}</strong></div>
          <div class="detail-kv"><span>架位</span><strong>{{ detail?.locationCode || '纯电子无架位' }}</strong></div>
          <div class="detail-kv"><span>所属组织</span><strong>{{ detail?.organizationName || selectedArchive?.organizationName || '—' }}</strong></div>
          <div class="detail-kv"><span>所属全宗</span><strong>{{ detail?.fondsName || selectedArchive?.fondsName || '—' }}</strong></div>

          <!-- 可编辑元数据 -->
          <h3 class="section-title" style="margin-top:12px">可编辑元数据</h3>
          <div class="field">
            <label>题名</label>
            <input v-model="editForm.title" />
          </div>
          <div class="field">
            <label>责任者</label>
            <input v-model="editForm.responsibleText" />
          </div>
          <div class="split">
            <div class="field">
              <label>形成日期</label>
              <input v-model="editForm.formedDate" type="date" />
            </div>
            <div class="field">
              <label>分类</label>
              <select v-model="editForm.categoryId">
                <option v-for="cat in categoryTree.filter(c => c.id > 0)" :key="cat.id" :value="cat.id">{{ cat.name }}</option>
              </select>
            </div>
          </div>
          <div class="field">
            <label>标签</label>
            <input v-model="editForm.tagsStr" />
          </div>
          <div class="actions">
            <el-button type="primary" @click="handleSaveMeta">保存元数据</el-button>
            <el-button @click="handlePreview">预览</el-button>
          </div>

          <!-- 受保护字段（只读展示） -->
          <h3 class="section-title" style="margin-top:12px">受保护字段</h3>
          <div class="split">
            <div class="field">
              <label>当前密级</label>
              <input :value="SecurityLevelLabel[detail?.securityLevel ?? 0]" disabled />
            </div>
            <div class="field">
              <label>当前开放</label>
              <input :value="detail?.openStatus === 'open' ? '公开' : '不公开'" disabled />
            </div>
          </div>

          <!-- 发起审批 -->
          <h3 class="section-title" style="margin-top:12px">发起审批</h3>
          <div class="field">
            <label>凭证档号</label>
            <input v-model="approvalForm.evidenceArchiveNo" placeholder="如 ARC-000007" />
            <p style="margin:4px 0 0;font-size:12px;color:var(--muted)">凭证须为与该档案<strong>同组织/同全宗</strong>的凭证类档案（如档案处置授权书），否则将被「组织/全宗不匹配」拒绝。</p>
          </div>
          <div class="split">
            <div class="field">
              <label>调整后密级</label>
              <select v-model.number="approvalForm.newSecurityLevel">
                <option v-for="(label, val) in SecurityLevelLabel" :key="val" :value="Number(val)">{{ label }}</option>
              </select>
            </div>
            <div class="field">
              <label>调整后开放</label>
              <select v-model="approvalForm.newOpenStatus">
                <option value="open">公开</option>
                <option value="closed">不公开</option>
              </select>
            </div>
          </div>
          <div class="field">
            <label>申请理由</label>
            <textarea v-model="approvalForm.reason" placeholder="填写调整依据和理由" rows="3" />
          </div>
          <div class="actions">
            <el-button @click="handleSecurityAdjust">密级调整</el-button>
            <el-button @click="handleOpenAdjust">开放调整</el-button>
            <router-link to="/admin/approval" class="button ghost">审批工作台</router-link>
          </div>
        </div>
      </el-dialog>
      <FilePreview
        v-model:visible="previewVisible"
        :file-id="previewFile?.id ?? null"
        :file-name="previewFile?.name"
        :mime="previewFile?.mime"
        :fetcher="previewArchiveFile"
      />
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { ArchiveRecord, ArchiveDetail } from '@/types/archive'
import { ArchiveStatusLabel, CarrierStatusLabel, SecurityLevelLabel } from '@/types/enums'
import FilePreview from '@/components/FilePreview/index.vue'
import { getArchives, getArchiveDetail, updateArchive, submitSecurityAdjust, submitOpenAdjust, previewArchiveFile } from '@/api/archive'

// ── 分类树 ──
const categoryTree = [
  { id: 0, name: '全部门类' },
  { id: 1, name: '文书档案' },
  { id: 2, name: '科技档案' },
  { id: 3, name: '会计档案' },
  { id: 4, name: '音像档案' },
  { id: 5, name: '人事档案' },
]
const selectedCategoryId = ref(0)

// ── 查询状态 ──
const query = reactive({
  keyword: '',
  year: undefined as number | undefined,
  securityLevel: '' as string | number,
  openStatus: '',
  carrierStatus: '',
})
const archives = ref<ArchiveRecord[]>([])
const selectedArchive = ref<ArchiveRecord | null>(null)
const detail = ref<ArchiveDetail | null>(null)
const listLoading = ref(false)
const detailLoading = ref(false)

// ── 预览 ──
const previewVisible = ref(false)
const previewFile = ref<{ id: number; name?: string; mime?: string } | null>(null)

// ── 分页 ──
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

/** 详情抽屉可见性：选中档案即展开，关闭即清空选中，避免查看详情时整页跟随滑动 */
const detailDrawerVisible = computed<boolean>({
  get: () => !!selectedArchive.value,
  set: (val) => {
    if (!val) {
      selectedArchive.value = null
      detail.value = null
    }
  },
})

// ── 编辑表单 ──
const editForm = reactive({
  title: '',
  responsibleText: '',
  formedDate: '',
  categoryId: 1,
  tagsStr: '',
})

// ── 审批表单 ──
const approvalForm = reactive({
  evidenceArchiveNo: '',
  newSecurityLevel: 0,
  newOpenStatus: 'open' as 'open' | 'closed',
  reason: '',
})

// ── 辅助函数 ──
function lifeClass(s: string): string {
  const map: Record<string, string> = { normal: 'success', pending_shelf: 'warning', pending_destruction: 'danger', destroyed: 'info' }
  return map[s] || ''
}

function syncEditForm(d: ArchiveDetail) {
  editForm.title = d.title
  editForm.responsibleText = d.responsibleText
  editForm.formedDate = d.formedDate
  editForm.categoryId = d.categoryId
  editForm.tagsStr = d.tags.join(',')
}

// ── 数据加载 ──
async function loadArchives() {
  listLoading.value = true
  try {
    const params: Record<string, unknown> = { pageNo: pageNo.value, pageSize: pageSize.value }
    if (query.keyword) params.keyword = query.keyword
    if (query.year) { params.formedYearStart = query.year; params.formedYearEnd = query.year }
    if (query.securityLevel !== '') params.securityLevel = Number(query.securityLevel)
    if (query.openStatus) params.openStatus = query.openStatus
    if (query.carrierStatus) params.carrierStatus = query.carrierStatus
    if (selectedCategoryId.value > 0) params.categoryId = selectedCategoryId.value
    const res = await getArchives(params)
    archives.value = res.records
    total.value = res.total
  } finally {
    listLoading.value = false
  }
}

async function selectArchive(a: ArchiveRecord) {
  selectedArchive.value = a
  detailLoading.value = true
  try {
    const d = await getArchiveDetail(a.id)
    detail.value = d
    syncEditForm(d)
  } finally {
    detailLoading.value = false
  }
}

function selectCategory(id: number) {
  selectedCategoryId.value = id
  pageNo.value = 1
  loadArchives()
}

function handleSearch() {
  pageNo.value = 1
  loadArchives()
}

function handleReset() {
  query.keyword = ''
  query.year = undefined
  query.securityLevel = ''
  query.openStatus = ''
  query.carrierStatus = ''
  pageNo.value = 1
  loadArchives()
}

// ── 保存元数据 ──
async function handleSaveMeta() {
  if (!detail.value) return
  try {
    const updated = await updateArchive(detail.value.id, {
      title: editForm.title,
      responsibleText: editForm.responsibleText,
      formedDate: editForm.formedDate,
      categoryId: editForm.categoryId,
      tagNames: editForm.tagsStr.split(',').map((t) => t.trim()).filter(Boolean),
      changeReason: 'manual_edit',
    })
    detail.value = updated
    ElMessage.success('元数据已保存')
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '保存失败'
    ElMessage.error(msg)
  }
}

function handlePreview() {
  const files = detail.value?.files ?? []
  if (!files.length) {
    ElMessage.info('该档案暂无可预览的电子文件')
    return
  }
  const f = files[0]
  previewFile.value = { id: f.id, name: f.originalFilename, mime: f.mimeType }
  previewVisible.value = true
}

// ── 审批 ──
function validateApproval(): boolean {
  if (!approvalForm.evidenceArchiveNo.trim() || !approvalForm.reason.trim()) {
    ElMessage.warning('请填写凭证档号和申请理由。')
    return false
  }
  return true
}

async function handleSecurityAdjust() {
  if (!detail.value || !validateApproval()) return
  try {
    await submitSecurityAdjust(detail.value.id, {
      newSecurityLevel: approvalForm.newSecurityLevel,
      evidenceArchiveNo: approvalForm.evidenceArchiveNo,
      reason: approvalForm.reason,
    })
    ElMessage.success('密级调整申请已生成审批单，目标字段在馆领导审批通过前保持不变。')
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '提交失败'
    ElMessage.error(msg)
  }
}

async function handleOpenAdjust() {
  if (!detail.value || !validateApproval()) return
  try {
    await submitOpenAdjust(detail.value.id, {
      newOpenStatus: approvalForm.newOpenStatus,
      evidenceArchiveNo: approvalForm.evidenceArchiveNo,
      reason: approvalForm.reason,
    })
    ElMessage.success('开放调整申请已生成审批单，目标字段在馆领导审批通过前保持不变。')
  } catch (e: unknown) {
    const msg = e instanceof Error ? e.message : '提交失败'
    ElMessage.error(msg)
  }
}

onMounted(loadArchives)
</script>

<style scoped>
.archive-management {
  padding: 0;
}

.page-title {
  font-size: 20px;
  font-weight: 700;
  margin: 0 0 4px;
}
.page-subtitle {
  color: var(--muted);
  font-size: 13px;
  margin: 0 0 16px;
}

.manage-layout {
  display: grid;
  grid-template-columns: 220px minmax(0, 1fr);
  gap: 16px;
  align-items: start;
}

.tree button {
  width: 100%;
  margin-bottom: 8px;
  justify-content: flex-start;
}
.button-active {
  border-color: #8abcbf !important;
  background: #f2f8f8 !important;
}

.grid {
  display: grid;
  gap: 14px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.field label {
  display: block;
  font-size: 13px;
  font-weight: 700;
  color: var(--muted);
  margin-bottom: 4px;
}
.field input,
.field select,
.field textarea {
  width: 100%;
  min-height: 34px;
  padding: 6px 8px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #fff;
  font-size: 14px;
}
.field textarea { min-height: 60px; resize: vertical; }

.detail-kv {
  display: grid;
  grid-template-columns: 118px minmax(0, 1fr);
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border);
}
.detail-kv span { color: var(--muted); font-weight: 700; font-size: 13px; }

.split {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.detail-empty {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 200px;
  color: var(--muted);
  font-size: 14px;
}

.table-wrap {
  overflow-x: auto;
}
.table-wrap table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}
.table-wrap th,
.table-wrap td {
  padding: 8px 10px;
  text-align: left;
  border-bottom: 1px solid var(--border);
}
.table-wrap th {
  font-weight: 700;
  color: var(--muted);
  background: var(--bg);
}
tr.row-active {
  background: #f2f8f8;
}

.status {
  display: inline-block;
  padding: 2px 8px;
  border-radius: var(--radius-sm);
  font-size: 12px;
  font-weight: 600;
}
.status.info { background: #e6f7ff; color: #1890ff; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.success { background: #f6ffed; color: #52c41a; }
.status.danger { background: #fff1f0; color: #f5222d; }

.actions {
  display: flex;
  gap: 8px;
  margin-top: 8px;
  flex-wrap: wrap;
}

.notice.warning {
  padding: 8px 10px;
  border-radius: var(--radius-sm);
  background: var(--warning-soft);
  font-size: 12px;
  color: #6f440c;
  margin-bottom: 8px;
}

.button.ghost {
  display: inline-block;
  padding: 6px 12px;
  border: 1px solid var(--border);
  border-radius: var(--radius-sm);
  background: #fff;
  color: var(--text);
  font-size: 14px;
  cursor: pointer;
  text-decoration: none;
  transition: border-color 0.14s;
}
.button.ghost:hover {
  border-color: #8abcbf;
}

@media (max-width: 1220px) {
  .manage-layout { grid-template-columns: 1fr; }
}
@media (max-width: 760px) {
  .split { grid-template-columns: 1fr; }
  .form-grid { grid-template-columns: 1fr; }
}
</style>
