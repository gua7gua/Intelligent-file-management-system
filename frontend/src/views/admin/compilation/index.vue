<script setup lang="ts">
import { computed, nextTick, onMounted, ref, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  archiveCompilation, createCompilation, deleteCompilation, generateCompilationBody, getCompilationDetail,
  getCompilations, searchMaterials, updateCompilation,
} from '@/api/compilation'
import { getFonds } from '@/api/fonds'
import type { Compilation, CompilationDetail, MaterialCandidate } from '@/types/compilation'
import type { FondsItem } from '@/types/fonds'
import { CompilationStatusLabel } from '@/types/enums'

const list = ref<Compilation[]>([])
const loading = ref(true)
const errorMsg = ref('')
const filterStatus = ref<'' | 'draft' | 'generated' | 'archived'>('')
const filterKeyword = ref('')

// ── 分页（编研成果列表） ──
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

const currentDetail = ref<CompilationDetail | null>(null)
const title = ref('')
const compilationType = ref('专题汇编')
const dateRangeText = ref('')
const keywords = ref('')
const summary = ref('')
const editorRef = ref<HTMLDivElement | null>(null)
const selectedMaterials = ref<MaterialCandidate[]>([])
const candidates = ref<MaterialCandidate[]>([])
const searchKeyword = ref('')
const saving = ref(false)
const generating = ref(false)

const typeOptions = ['专题汇编', '大事记', '组织史', '目录索引', '参考资料']

// 入库表单：编研成果按纯电子正式档案入库（§19.6）
const archiveForm = reactive({
  fondsId: 0,
  categoryId: 1,
  formedDate: '',
  retentionPeriod: 'permanent',
  openStatus: 'open',
  tagNames: '',
})
const fondsOptions = ref<FondsItem[]>([])
const categoryOptions = [
  { id: 1, name: '文书档案' },
  { id: 2, name: '科技档案' },
  { id: 3, name: '会计档案' },
  { id: 4, name: '音像档案' },
  { id: 5, name: '实物档案' },
]
const retentionOptions = [
  { value: 'permanent', label: '永久' },
  { value: '30y', label: '30 年' },
  { value: '10y', label: '10 年' },
]
const openStatusOptions = [
  { value: 'open', label: '公开' },
  { value: 'closed', label: '不公开' },
]

async function loadFonds() {
  try {
    const page = await getFonds({ pageSize: 100 })
    fondsOptions.value = page.records
  } catch {
    fondsOptions.value = []
  }
}

const filtered = computed(() => list.value.filter((c) => {
  if (filterStatus.value && c.status !== filterStatus.value) return false
  if (filterKeyword.value && !(c.title.includes(filterKeyword.value) || c.compilationNo.includes(filterKeyword.value))) return false
  return true
}))

const metrics = computed(() => ({
  draftCount: list.value.filter((c) => c.status === 'draft').length,
  generatedCount: list.value.filter((c) => c.status === 'generated').length,
  archivedCount: list.value.filter((c) => c.status === 'archived').length,
  monthAdded: list.value.length,
}))

const status = computed(() => currentDetail.value?.status ?? 'draft')
const isReadonly = computed(() => status.value === 'archived')

function syncEditor() {
  nextTick(() => {
    if (editorRef.value) editorRef.value.innerHTML = currentDetail.value?.contentHtml ?? ''
  })
}

function fillForm(d: CompilationDetail | null) {
  currentDetail.value = d
  title.value = d?.title ?? ''
  compilationType.value = d?.compilationType ?? '专题汇编'
  dateRangeText.value = d?.dateRangeText ?? ''
  keywords.value = d?.keywords ?? ''
  summary.value = d?.summary ?? ''
  selectedMaterials.value = d?.materials.map((m) => ({
    id: m.archiveId, archiveNo: m.archiveNo, title: m.title, categoryName: '', formedYear: 0, tags: [],
  })) ?? []
  syncEditor()
}

function collectData() {
  const contentHtml = editorRef.value?.innerHTML ?? ''
  return {
    title: title.value, compilationType: compilationType.value, dateRangeText: dateRangeText.value,
    keywords: keywords.value, summary: summary.value, contentHtml,
    materialArchiveIds: selectedMaterials.value.map((m) => m.id),
  }
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    const page = await getCompilations({ pageNo: pageNo.value, pageSize: pageSize.value })
    list.value = page.records
    total.value = page.total
    if (list.value.length && !currentDetail.value) await openEdit(list.value[0])
  } catch (e) {
    errorMsg.value = (e as Error).message || '编研成果加载失败'
  } finally {
    loading.value = false
  }
}

async function openEdit(c: Compilation) {
  try {
    const d = await getCompilationDetail(c.id)
    fillForm(d)
  } catch (e) {
    ElMessage.warning((e as Error).message)
  }
}

// 删除编研草稿（仅 status=draft 可删，已生成/已入库不显示删除按钮）
async function deleteItem(c: Compilation) {
  try {
    await ElMessageBox.confirm(
      '删除该草稿编研，不可恢复？',
      '提示',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' },
    )
  } catch {
    return
  }
  try {
    await deleteCompilation(c.id)
    ElMessage.success('草稿编研已删除。')
    if (currentDetail.value?.id === c.id) fillForm(null)
    await load()
  } catch (e) {
    ElMessage.error((e as Error).message || '删除编研失败')
  }
}

function openNew() {
  fillForm(null)
}

async function saveDraft() {
  if (!title.value.trim()) { ElMessage.warning('请填写标题'); return }
  saving.value = true
  try {
    if (currentDetail.value) {
      const d = await updateCompilation(currentDetail.value.id, collectData())
      fillForm(d)
    } else {
      const d = await createCompilation(collectData())
      fillForm(d)
      await load()
    }
    ElMessage.success('草稿已保存')
  } catch (e) {
    ElMessage.warning((e as Error).message)
  } finally {
    saving.value = false
  }
}

// D3：合并「生成正文」+「确认纯电子入库」为一键操作：尚未生成正文则先生成，再入库。
async function generateAndArchive() {
  if (!currentDetail.value) { ElMessage.warning('请先保存草稿'); return }
  if (selectedMaterials.value.length === 0) { ElMessage.warning('请添加至少一个素材后再入库'); return }
  if (!archiveForm.formedDate) { ElMessage.warning('请填写形成日期后再入库'); return }
  try {
    await ElMessageBox.confirm('将生成正文文件并确认纯电子入库，入库后生成正式档号且不可再编辑。', '生成正文并入库', { type: 'warning' })
  } catch {
    return
  }
  generating.value = true
  try {
    // 尚未生成正文时先生成（status=draft），生成后 status 变为 generated
    if (status.value !== 'generated') {
      const g = await generateCompilationBody(currentDetail.value.id)
      fillForm(g)
    }
    const d = await archiveCompilation(currentDetail.value.id, {
      fondsId: archiveForm.fondsId || null,
      categoryId: archiveForm.categoryId,
      formedDate: archiveForm.formedDate,
      retentionPeriod: archiveForm.retentionPeriod,
      openStatus: archiveForm.openStatus,
      tagNames: archiveForm.tagNames.split(',').map((t) => t.trim()).filter(Boolean),
    })
    fillForm(d)
    await load()
    ElMessage.success(`已生成正文并入库，正式档号 ${d.archiveNo}`)
  } catch (e) {
    ElMessage.warning((e as Error).message)
  } finally {
    generating.value = false
  }
}

async function search() {
  // 6.14：检索框为空时不返回全部档号，提示并清空候选
  const kw = searchKeyword.value.trim()
  if (!kw) {
    candidates.value = []
    ElMessage.warning('请输入题名或档号后再查询。')
    return
  }
  const page = await searchMaterials({ keyword: kw, pageSize: 50 })
  candidates.value = page.records
}

function addMaterial(m: MaterialCandidate) {
  if (!selectedMaterials.value.some((x) => x.id === m.id)) selectedMaterials.value.push(m)
}
function removeMaterial(id: number) {
  selectedMaterials.value = selectedMaterials.value.filter((m) => m.id !== id)
}

onMounted(() => {
  load()
  loadFonds()
})
</script>

<template>
  <div>
    <div class="toolbar">
      <div>
        <h1 class="page-title">档案编研</h1>
        <p class="page-subtitle">编研成果以草稿、正文文件、入库三个阶段流转，入库后生成独立正式档案。</p>
      </div>
      <div class="actions">
        <button class="button secondary new-compilation" id="newCompilation" @click="openNew"><span class="icon">+</span>新建编研成果</button>
      </div>
    </div>

    <section class="grid four" style="margin-top:16px" aria-label="编研统计">
      <div class="metric"><span class="label">草稿成果</span><span class="value">{{ metrics.draftCount }}</span><span class="note">仅创建人和后台管理员可见</span></div>
      <div class="metric"><span class="label">已生成正文</span><span class="value">{{ metrics.generatedCount }}</span><span class="note">等待入库确认</span></div>
      <div class="metric"><span class="label">已入库成果</span><span class="value">{{ metrics.archivedCount }}</span><span class="note">已生成正式档案</span></div>
      <div class="metric"><span class="label">本月新增</span><span class="value">{{ metrics.monthAdded }}</span><span class="note">本月新增编研成果</span></div>
    </section>

    <p v-if="loading" class="notice" style="margin-top:16px;">加载中…</p>
    <p v-else-if="errorMsg" class="notice" style="margin-top:16px;">{{ errorMsg }} <button class="button" @click="load">重试</button></p>

    <div v-if="!loading && !errorMsg" class="workbench" style="margin-top:16px">
      <section class="stack">
        <div class="card panel">
          <div class="toolbar" style="margin-top:0">
            <div>
              <h2 class="section-title">编研成果列表</h2>
            </div>
            <div class="actions">
              <select v-model="filterStatus" aria-label="成果状态筛选">
                <option value="">全部状态</option>
                <option value="draft">草稿</option>
                <option value="generated">已生成</option>
                <option value="archived">已入库</option>
              </select>
              <input v-model="filterKeyword" placeholder="编号/题名" />
            </div>
          </div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>编研编号</th>
                  <th>题名</th>
                  <th>类型</th>
                  <th>状态</th>
                  <th>素材</th>
                  <th>正文文件</th>
                  <th>入库档号</th>
                  <th>操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="c in filtered" :key="c.id">
                  <td class="mono">{{ c.compilationNo }}</td>
                  <td>{{ c.title }}</td>
                  <td>{{ c.compilationType }}</td>
                  <td><span class="status" :class="c.status === 'archived' ? 'success' : c.status === 'generated' ? 'info' : 'warning'">{{ CompilationStatusLabel[c.status] }}</span></td>
                  <td>{{ c.materialCount }} 条引用</td>
                  <td><span class="status" :class="c.attachment ? 'success' : ''">{{ c.attachment ? c.attachment.fileName : '未生成' }}</span></td>
                  <td class="mono">{{ c.archiveNo || '—' }}</td>
                  <td>
                    <div class="actions">
                      <button class="button ghost" @click="openEdit(c)">编辑/查看</button>
                      <button v-if="c.status === 'draft'" class="button danger" @click="deleteItem(c)">删除</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="!filtered.length"><td colspan="8" class="muted" style="text-align:center;padding:16px;">暂无编研成果。</td></tr>
              </tbody>
            </table>
            <div v-if="total > 0" style="display:flex;justify-content:flex-end;margin-top:12px">
              <el-pagination
                v-model:current-page="pageNo"
                v-model:page-size="pageSize"
                :total="total"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="load"
                @current-change="load"
              />
            </div>
          </div>
        </div>

        <div class="card panel">
          <h2 class="section-title">成果编辑 <span class="status" :class="isReadonly ? 'success' : 'warning'">{{ CompilationStatusLabel[status] }}</span></h2>
          <div class="form-grid">
            <div class="field">
              <label>编研标题</label>
              <input id="title" v-model="title" :disabled="isReadonly" />
            </div>
            <div class="field">
              <label>编研类型</label>
              <select v-model="compilationType" :disabled="isReadonly">
                <option v-for="t in typeOptions" :key="t">{{ t }}</option>
              </select>
            </div>
            <div class="field">
              <label>时间范围</label>
              <input v-model="dateRangeText" :disabled="isReadonly" placeholder="如 2014-2025" />
            </div>
          </div>
          <div ref="editorRef" class="editor-surface" :contenteditable="isReadonly ? 'false' : 'true'" style="margin-top:12px"></div>
        </div>
      </section>

      <aside class="stack">
        <div class="notice">
          <strong>入库口径</strong>
          <div>确认入库后生成独立正式档案，载体为纯电子；素材仅保留引用关系。</div>
        </div>

        <div class="card panel">
          <div class="toolbar" style="margin-top:0">
            <h2 class="section-title">素材引用</h2>
            <span class="status">{{ selectedMaterials.length }} 条</span>
          </div>
          <div class="form-grid" style="grid-template-columns:1fr">
            <div class="field">
              <label>素材检索</label>
              <div class="row">
                <input v-model="searchKeyword" placeholder="题名/档号" @keyup.enter="search" />
                <button class="button secondary" @click="search"><span class="icon">+</span>查询</button>
              </div>
              <span class="hint">系统按档号校验档案存在、未销毁且当前管理员有权查看，校验通过后加入素材列表。</span>
            </div>
          </div>
          <div v-if="candidates.length" class="material-group">
            <h3 class="group-title">检索结果（可添加）</h3>
            <ul class="candidate-list">
              <li v-for="c in candidates" :key="c.id">
                <span class="mono">{{ c.archiveNo }}</span> {{ c.title }}
                <button class="button ghost" @click="addMaterial(c)">添加</button>
              </li>
            </ul>
          </div>
          <div class="material-group" style="margin-top:14px">
            <h3 class="group-title">已引用素材</h3>
            <ul class="material-list">
              <li v-for="m in selectedMaterials" :key="m.id">
                <strong>档号 {{ m.archiveNo }} · {{ m.title }}</strong>
                <button class="button ghost" :disabled="isReadonly" @click="removeMaterial(m.id)">移除</button>
              </li>
              <li v-if="!selectedMaterials.length" class="muted">暂无素材引用。</li>
            </ul>
          </div>
        </div>

        <div class="card panel">
          <h2 class="section-title">正文文件与入库确认</h2>
          <div class="check-row">
            <div class="check-item"><span>正文文件</span><span class="status" :class="currentDetail?.attachment ? 'success' : ''">{{ currentDetail?.attachment ? currentDetail.attachment.fileName : '未生成' }}</span></div>
            <div class="check-item"><span>载体状态</span><span class="status success">纯电子</span></div>
          </div>
          <div class="form-grid" style="margin-top:14px">
            <div class="field">
              <label>所属全宗</label>
              <select v-model="archiveForm.fondsId" :disabled="isReadonly">
                <option :value="0">暂不归属（可留空）</option>
                <option v-for="f in fondsOptions" :key="f.id" :value="f.id">{{ f.fondsNo }} · {{ f.fondsName }}</option>
              </select>
            </div>
            <div class="field">
              <label>档案门类</label>
              <select v-model="archiveForm.categoryId" :disabled="isReadonly">
                <option v-for="c in categoryOptions" :key="c.id" :value="c.id">{{ c.name }}</option>
              </select>
            </div>
            <div class="field">
              <label>形成日期</label>
              <input v-model="archiveForm.formedDate" type="date" :disabled="isReadonly" />
            </div>
            <div class="field">
              <label>保管期限</label>
              <select v-model="archiveForm.retentionPeriod" :disabled="isReadonly">
                <option v-for="r in retentionOptions" :key="r.value" :value="r.value">{{ r.label }}</option>
              </select>
            </div>
            <div class="field">
              <label>是否公开</label>
              <select v-model="archiveForm.openStatus" :disabled="isReadonly">
                <option v-for="o in openStatusOptions" :key="o.value" :value="o.value">{{ o.label }}</option>
              </select>
            </div>
            <div class="field">
              <label>标签</label>
              <input v-model="archiveForm.tagNames" placeholder="多个标签用逗号分隔" :disabled="isReadonly" />
            </div>
          </div>
          <div class="actions" style="margin-top:14px; justify-content:space-between">
            <button class="button" id="saveDraft" :disabled="isReadonly || saving" @click="saveDraft"><span class="icon">S</span>保存草稿</button>
            <button class="button" :disabled="isReadonly || generating || !currentDetail" @click="generateAndArchive"><span class="icon">A</span>生成正文并入库</button>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.workbench { display: grid; grid-template-columns: minmax(0,1fr) 360px; gap: 16px; }
.stack { display: grid; gap: 16px; }
.status-line { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px; }
.form-grid { display: grid; grid-template-columns: repeat(2, minmax(0,1fr)); gap: 12px; }
.field { display: grid; gap: 4px; }
.field input, .field select, .field textarea { padding: 6px 8px; }
.editor-surface { min-height: 140px; border: 1px solid var(--border); border-radius: 6px; padding: 10px; background: #fff; }
.editor-surface:focus { border-color: var(--primary, #1f6f78); outline: none; }
.editor-surface :deep(h3) { margin: 0 0 6px; }
.editor-surface :deep(p) { margin: 4px 0; }
.row { display: flex; gap: 8px; }
.candidate-list, .material-list { list-style: none; margin: 8px 0 0; padding: 0; display: grid; gap: 6px; }
.candidate-list li, .material-list li { display: flex; justify-content: space-between; align-items: center; gap: 8px; padding: 6px 0; border-bottom: 1px solid var(--border); }
/* 6.10：可添加与已引用素材物理分区，避免误点对方分区的按钮 */
.material-group { padding: 10px 12px; border: 1px solid var(--border); border-radius: 8px; background: #fafbfc; }
.material-group + .material-group { margin-top: 14px; }
.group-title { font-size: 13px; font-weight: 700; margin: 0 0 4px; color: #303133; }
.check-row { display: grid; gap: 8px; }
.check-item { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; border-bottom: 1px solid var(--border); }
.actions { display: flex; align-items: center; gap: 8px; }
@media (max-width: 1100px) { .workbench { grid-template-columns: 1fr; } .form-grid { grid-template-columns: 1fr; } }
</style>
