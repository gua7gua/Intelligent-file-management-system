<template>
  <section class="search-layout">
    <div class="grid">
      <!-- AI 检索条件生成 -->
      <details class="card panel ai-panel">
        <summary>
          <span class="section-title">AI 检索条件生成</span>
          <span class="status info">点击展开</span>
        </summary>
        <div class="collapsible-body">
          <div class="field">
            <label>自然语言检索描述</label>
            <textarea v-model="aiText" placeholder="例如：查找财政局 2025 年预算相关档案"></textarea>
            <span class="hint">用自然语言描述检索需求，AI 帮您生成检索条件。</span>
          </div>
          <div class="actions">
            <button class="button" type="button" :disabled="aiLoading" @click="handleAiQuery">
              {{ aiLoading ? '生成中…' : '生成查询条件' }}
            </button>
            <button class="button ghost" type="button" @click="clearAi">清空</button>
          </div>
          <div v-if="aiError" class="notice danger">{{ aiError }}</div>
          <pre v-if="aiResult" class="ai-box">{{ JSON.stringify(aiResult.conditions, null, 2) }}</pre>
          <div v-if="aiResult" class="json-actions">
            <button class="button secondary" type="button" @click="applyAiConditions">应用条件并检索</button>
            <button class="button ghost" type="button" @click="copyJson">复制 JSON</button>
          </div>
        </div>
      </details>

      <!-- 结构化检索条件 -->
      <div class="card panel">
        <div class="toolbar" style="margin: 0">
          <h2 class="section-title" style="margin: 0">结构化检索条件</h2>
          <span class="status info">系统将自动限定在您的权限范围内</span>
        </div>
        <div class="filter-sections">
          <section class="filter-group">
            <div class="filter-group-head"><strong>核心元数据</strong></div>
            <div class="filter-grid">
              <div class="field"><label>关键词</label><input v-model="params.keyword" placeholder="题名、责任者、档号、文件名"></div>
              <div class="field"><label>档号</label><input v-model="params.archiveNo"></div>
              <div class="field"><label>题名</label><input v-model="params.title"></div>
              <div class="field"><label>责任者</label><input v-model="params.responsibleText"></div>
              <div class="field"><label>所属全宗</label><input v-model="fondsInput" placeholder="全宗名称"></div>
              <div class="field">
                <label>档案门类</label>
                <select v-model.number="params.categoryId">
                  <option :value="undefined">全部</option>
                  <option v-for="c in categoryOptions" :key="c.categoryId" :value="c.categoryId">{{ c.categoryName }}</option>
                </select>
              </div>
              <div class="field"><label>形成/移交单位</label><input v-model="params.organizationName"></div>
              <div class="field"><label>标签</label><input v-model="params.tagIds" placeholder="逗号分隔"></div>
            </div>
          </section>

          <section class="filter-group">
            <div class="filter-group-head"><strong>时间与业务属性</strong></div>
            <div class="filter-grid">
              <div class="field"><label>形成年度起</label><input v-model.number="params.formedYearStart" type="number"></div>
              <div class="field"><label>形成年度止</label><input v-model.number="params.formedYearEnd" type="number"></div>
              <div class="field">
                <label>档案来源</label>
                <select v-model="params.sourceType">
                  <option value="">全部</option>
                  <option value="transfer">移交</option>
                  <option value="collection">征集</option>
                  <option value="compilation">编研</option>
                </select>
              </div>
              <div class="field">
                <label>载体状态</label>
                <select v-model="params.carrierStatus">
                  <option value="">全部</option>
                  <option value="electronic">纯电子</option>
                  <option value="paper_electronic">纸质+电子</option>
                  <option value="paper">纯纸质</option>
                </select>
              </div>
              <div class="field">
                <label>保管期限</label>
                <select v-model="params.retentionPeriod">
                  <option value="">全部</option>
                  <option value="permanent">永久</option>
                  <option value="30y">30年</option>
                  <option value="10y">10年</option>
                </select>
              </div>
              <div class="field">
                <label>密级范围</label>
                <select v-model.number="params.securityLevelMax">
                  <option :value="undefined">当前权限内全部</option>
                  <option :value="0">非密</option>
                  <option :value="1">内部</option>
                  <option :value="2">秘密</option>
                </select>
              </div>
              <div class="field">
                <label>开放状态</label>
                <select v-model="params.openStatus">
                  <option value="">全部</option>
                  <option value="open">公开</option>
                  <option value="closed">不公开</option>
                </select>
              </div>
              <div class="field">
                <label>借阅状态</label>
                <select v-model="params.loanStatus">
                  <option value="">全部</option>
                  <option value="available">可借阅</option>
                  <option value="on_loan">借出中</option>
                  <option value="inventory_paused">盘点暂停</option>
                </select>
              </div>
            </div>
          </section>

          <section class="filter-group">
            <div class="filter-group-head"><strong>电子文件与排序</strong></div>
            <div class="filter-grid">
              <div class="field">
                <label>电子文件</label>
                <select v-model="hasFileSelect">
                  <option value="">全部</option>
                  <option value="yes">有电子文件</option>
                  <option value="no">无电子文件</option>
                </select>
              </div>
              <div class="field"><label>文件格式</label><input v-model="params.fileExt" placeholder="PDF / JPG..."></div>
              <div class="field">
                <label>排序</label>
                <select v-model="params.sortBy">
                  <option value="relevance">相关度优先</option>
                  <option value="formed_desc">形成日期倒序</option>
                  <option value="archived_desc">入库时间倒序</option>
                  <option value="archiveNo_asc">档号升序</option>
                </select>
              </div>
            </div>
          </section>
        </div>
        <div class="actions" style="margin-top: 12px">
          <button class="button" type="button" :disabled="searching" @click="searchFromFirstPage">
            {{ searching ? '检索中…' : '确认检索' }}
          </button>
          <button class="button ghost" type="button" @click="resetSearch">重置</button>
        </div>
      </div>

      <!-- 检索结果 -->
      <div class="card panel">
        <div class="toolbar">
          <div>
            <h2 class="section-title">检索结果</h2>
            <p class="page-subtitle">展示您权限范围内可查阅的档案。</p>
          </div>
          <span v-if="!searching && searched" class="status info">共 {{ results.total }} 条</span>
        </div>
        <div v-if="searching" class="notice">正在检索…</div>
        <div v-else-if="searchError" class="notice danger">{{ searchError }}</div>
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>档号</th><th>题名</th><th>分类</th><th>密级</th><th>载体</th><th>利用状态</th><th>标签</th><th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="searched && results.records.length === 0">
                <td colspan="8"><div class="empty">没有符合条件的档案</div></td>
              </tr>
              <tr v-for="record in results.records" :key="record.archiveId" class="result-row" @click="selectArchive(record.archiveId)">
                <td class="mono">{{ record.archiveNo }}</td>
                <td>{{ record.title }}</td>
                <td>{{ record.categoryName }}</td>
                <td><span :class="['status', record.securityLevel > 0 ? 'warning' : 'success']">{{ securityLabel(record.securityLevel) }}</span></td>
                <td>{{ carrierLabel(record.carrierStatus) }}</td>
                <td><span class="status success">{{ usageHint(record) }}</span></td>
                <td>{{ record.tags?.join('、') || '—' }}</td>
                <td><button class="button ghost" type="button" @click.stop="selectArchive(record.archiveId)">详情</button></td>
              </tr>
            </tbody>
          </table>
        </div>
        <div v-if="!searching && !searchError && searched" style="display: flex; justify-content: flex-end; margin-top: 12px">
          <el-pagination
            v-model:current-page="pageNo"
            v-model:page-size="pageSize"
            :total="total"
            :page-sizes="[10, 20, 50, 100]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSearch"
            @current-change="handleSearch"
          />
        </div>
      </div>
    </div>

    <!-- 右栏详情面板 -->
    <aside class="detail-panel-wrapper">
      <el-dialog v-model="detailDialogVisible" title="档案详情" width="720px" align-center destroy-on-close>
        <ArchiveDetailPanel :archive-id="selectedArchiveId" />
      </el-dialog>
    </aside>
  </section>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import ArchiveDetailPanel from '@/views/internal/components/ArchiveDetailPanel.vue'
import { generateInternalAiQuery, searchInternalArchives } from '@/api/internal'
import { getDictionariesApi } from '@/api/dictionary'
import { CarrierStatusLabel, SecurityLevelLabel } from '@/types/enums'
import type { InternalArchive, InternalAiQueryResult, InternalSearchParams } from '@/types/internal'

const aiText = ref('')
const aiLoading = ref(false)
const aiError = ref('')
const aiResult = ref<InternalAiQueryResult | null>(null)

// 档案门类字典（含 categoryId/categoryCode/categoryName），来源于全量字典
interface CategoryOption {
  categoryId: number
  categoryCode: string
  categoryName: string
}
const categoryOptions = ref<CategoryOption[]>([])

const params = reactive<InternalSearchParams>({})
const fondsInput = ref('')
const hasFileSelect = ref<'' | 'yes' | 'no'>('')

const searching = ref(false)
const searchError = ref('')
const searched = ref(false)
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)
const results = ref<{ records: InternalArchive[]; total: number }>({ records: [], total: 0 })
const selectedArchiveId = ref<number | null>(null)
const detailDialogVisible = computed({
  get: () => selectedArchiveId.value !== null,
  set: (v: boolean) => {
    if (!v) selectedArchiveId.value = null
  },
})

function securityLabel(level: number): string {
  return SecurityLevelLabel[level] ?? '未知'
}
function carrierLabel(status: string): string {
  return CarrierStatusLabel[status] ?? status
}
function usageHint(record: InternalArchive): string {
  const hints: string[] = []
  if (record.canPreview) hints.push('可预览')
  if (record.canDownload) hints.push('可下载')
  if (record.canBorrow) hints.push('可借阅')
  return hints.length ? hints.join(' / ') : '仅元数据'
}

async function handleAiQuery() {
  if (!aiText.value.trim()) {
    ElMessage.warning('请输入自然语言描述')
    return
  }
  aiLoading.value = true
  aiError.value = ''
  try {
    aiResult.value = await generateInternalAiQuery({ text: aiText.value })
    ElMessage.success('AI 已生成查询条件，尚未执行检索')
  } catch (e: unknown) {
    aiError.value = e instanceof Error ? e.message : 'AI 生成失败，请改用结构化检索'
    aiResult.value = null
  } finally {
    aiLoading.value = false
  }
}

// 将 AI 返回的检索条件映射为后端 /internal/archives/search 接受的查询参数。
// 后端 InternalSearchQuery 期望：keyword(String 单数)、categoryId(Integer)、
// formedYearStart/End(Integer)、responsibleText、carrierStatus、securityLevelMax、openStatus。
// AI 输出 schema 与之不同（keywords 数组 / category code / formedDateRange 日期数组），
// 在此层做适配，避免后端 DTO 与 AI 契约耦合。
function applyAiConditions() {
  if (!aiResult.value) return
  const c = aiResult.value.conditions as Record<string, unknown> | undefined
  if (!c) return
  // 先清空旧条件，避免上一次的手动筛选混入
  Object.keys(params).forEach((key) => delete (params as Record<string, unknown>)[key])

  // keywords 数组 → 取首个填入关键词框（满足"应用条件后表单填充"的视觉反馈）。
  // 与公众端策略一致：首次检索不下发 keyword，后端 keyword 是单字段 SQL like 连续子串匹配，
  // AI 抽取的"克拉玛依"在数据中可能写作"克拉玛依市"，强制过滤会漏掉本应命中的档案。
  // 结构化条件（门类/年度/责任者/密级/开放状态）更可靠，由它们兜底命中；关键词保留在表单框
  // 供用户看见 AI 的抽词结果，用户若想用关键词缩小范围可自行点"检索"重新过滤。
  const kws = Array.isArray(c.keywords) ? (c.keywords as unknown[]).filter((x): x is string => typeof x === 'string' && x.trim() !== '') : []
  if (kws.length > 0) {
    params.keyword = kws[0]
  }
  // category code → categoryId（通过门类字典反查；查不到则忽略，避免下发后端无法识别的字符串）
  if (typeof c.category === 'string' && c.category) {
    const matched = categoryOptions.value.find((o) => o.categoryCode === c.category)
    if (matched) params.categoryId = matched.categoryId
  }
  // formedDateRange [start, end] → formedYearStart/End（取年份整数）
  if (Array.isArray(c.formedDateRange)) {
    const [s, e] = c.formedDateRange as [unknown, unknown]
    const sy = s ? parseInt(String(s).slice(0, 4), 10) : NaN
    const ey = e ? parseInt(String(e).slice(0, 4), 10) : NaN
    if (!Number.isNaN(sy)) params.formedYearStart = sy
    if (!Number.isNaN(ey)) params.formedYearEnd = ey
  }
  if (typeof c.responsible === 'string' && c.responsible.trim()) {
    params.responsibleText = c.responsible
  }
  if (typeof c.carrierStatus === 'string' && c.carrierStatus) {
    params.carrierStatus = c.carrierStatus as InternalSearchParams['carrierStatus']
  }
  if (typeof c.securityLevelMax === 'number' && Number.isFinite(c.securityLevelMax)) {
    params.securityLevelMax = c.securityLevelMax
  }
  if (typeof c.openStatus === 'string' && c.openStatus) {
    params.openStatus = c.openStatus as InternalSearchParams['openStatus']
  }
  // 首次检索暂不下发 keyword（见上方注释），临时摘出后调 handleSearch，完成后再放回表单框显示。
  const keywordToShow = params.keyword
  delete params.keyword
  pageNo.value = 1
  handleSearch().finally(() => {
    if (keywordToShow) params.keyword = keywordToShow
  })
}

function clearAi() {
  aiText.value = ''
  aiResult.value = null
  aiError.value = ''
}

async function copyJson() {
  if (!aiResult.value) return
  try {
    await navigator.clipboard.writeText(JSON.stringify(aiResult.value.conditions, null, 2))
    ElMessage.success('JSON 已复制')
  } catch {
    ElMessage.warning('复制失败，请手动选择')
  }
}

// 用户主动点「确认检索」/应用 AI 条件时回到第一页；分页器切换页码时直接走 handleSearch 保留当前页
function searchFromFirstPage() {
  pageNo.value = 1
  return handleSearch()
}

async function handleSearch() {
  searching.value = true
  searchError.value = ''
  selectedArchiveId.value = null
  try {
    const query: InternalSearchParams = { ...params, pageNo: pageNo.value, pageSize: pageSize.value }
    if (hasFileSelect.value === 'yes') query.hasElectronicFile = true
    else if (hasFileSelect.value === 'no') query.hasElectronicFile = false
    const page = await searchInternalArchives(query)
    results.value = { records: page.records, total: page.total }
    total.value = page.total
    searched.value = true
  } catch (e: unknown) {
    searchError.value = e instanceof Error ? e.message : '检索失败'
  } finally {
    searching.value = false
  }
}

function selectArchive(id: number) {
  selectedArchiveId.value = id
}

function resetSearch() {
  Object.keys(params).forEach((k) => delete (params as Record<string, unknown>)[k])
  fondsInput.value = ''
  hasFileSelect.value = ''
  results.value = { records: [], total: 0 }
  searched.value = false
  searchError.value = ''
  selectedArchiveId.value = null
  aiResult.value = null
  aiText.value = ''
  pageNo.value = 1
  total.value = 0
}

onMounted(async () => {
  // 加载门类字典，供档案门类下拉和 AI 条件映射使用
  // 后端返回 categories: [{categoryId, categoryCode, categoryName, enabled}]
  try {
    const dict = await getDictionariesApi()
    const raw = (dict.categories ?? []) as Array<Record<string, unknown>>
    categoryOptions.value = raw
      .filter((c) => c.enabled !== false && c.categoryId != null)
      .map((c) => ({
        categoryId: Number(c.categoryId),
        categoryCode: String(c.categoryCode ?? ''),
        categoryName: String(c.categoryName ?? ''),
      }))
  } catch {
    categoryOptions.value = []
  }
})
</script>

<style scoped>
.search-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 400px;
  gap: 16px;
  align-items: start;
}
.filter-sections { display: grid; gap: 12px; margin-top: 14px; }
.filter-group {
  display: grid; gap: 12px; padding: 12px;
  border: 1px solid var(--border); border-radius: var(--radius); background: #f8fbfc;
}
.filter-group-head { display: flex; gap: 8px; align-items: center; justify-content: space-between; }
.filter-grid { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 12px; }
.ai-panel { display: grid; gap: 12px; }
.ai-panel summary {
  display: flex; min-height: 34px; cursor: pointer; list-style: none;
  align-items: center; justify-content: space-between; gap: 12px;
}
.ai-panel summary::-webkit-details-marker { display: none; }
.collapsible-body {
  display: grid; gap: 12px; margin-top: 12px; padding-top: 12px; border-top: 1px solid var(--border);
}
.json-actions { display: flex; flex-wrap: wrap; gap: 8px; }
.result-row { cursor: pointer; transition: background 0.16s ease; }
.result-row:hover { background: #f8fbfc; }
.detail-panel-wrapper { display: grid; gap: 12px; position: sticky; top: 84px; max-height: calc(100vh - 100px); overflow-y: auto; }
@media (max-width: 1120px) {
  .search-layout { grid-template-columns: 1fr; }
  .detail-panel-wrapper { position: static; }
}
@media (max-width: 960px) {
  .filter-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
