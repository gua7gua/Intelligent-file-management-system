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
            <span class="hint">AI 不查库、不读取正文，只生成结构化查询条件。</span>
          </div>
          <div class="actions">
            <button class="button" type="button" :disabled="aiLoading" @click="handleAiQuery">
              {{ aiLoading ? '生成中…' : '生成 JSON' }}
            </button>
            <button class="button ghost" type="button" @click="clearAi">清空</button>
          </div>
          <div v-if="aiError" class="notice danger">{{ aiError }}</div>
          <pre v-if="aiResult" class="ai-box">{{ JSON.stringify(aiResult.conditions, null, 2) }}</pre>
          <div v-if="aiResult" class="json-actions">
            <button class="button secondary" type="button" @click="applyAiConditions">填充表单</button>
            <button class="button ghost" type="button" @click="copyJson">复制 JSON</button>
          </div>
        </div>
      </details>

      <!-- 结构化检索条件 -->
      <div class="card panel">
        <div class="toolbar" style="margin: 0">
          <h2 class="section-title" style="margin: 0">结构化检索条件</h2>
          <span class="status info">后端追加密级、单位、全宗范围过滤</span>
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
                  <option :value="1">文书档案</option>
                  <option :value="3">科技档案</option>
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
          <button class="button" type="button" :disabled="searching" @click="handleSearch">
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
            <p class="page-subtitle">结果已按当前用户权限过滤；不会展示高于密级上限或超出范围的档案。</p>
          </div>
          <span v-if="!searching && searched" class="status info">权限过滤后 {{ results.total }} 条</span>
        </div>
        <div v-if="searching" class="notice">正在检索…</div>
        <div v-else-if="searchError" class="notice danger">{{ searchError }}</div>
        <div v-else class="table-wrap">
          <table>
            <thead>
              <tr>
                <th>档号</th><th>题名</th><th>分类</th><th>密级</th><th>载体</th><th>利用状态</th><th>操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-if="searched && results.records.length === 0">
                <td colspan="7"><div class="empty">没有符合条件的档案</div></td>
              </tr>
              <tr v-for="record in results.records" :key="record.id" class="result-row" @click="selectArchive(record.id)">
                <td class="mono">{{ record.archiveNo }}</td>
                <td>{{ record.title }}</td>
                <td>{{ record.categoryName }}</td>
                <td><span :class="['status', record.securityLevel > 0 ? 'warning' : 'success']">{{ securityLabel(record.securityLevel) }}</span></td>
                <td>{{ carrierLabel(record.carrierStatus) }}</td>
                <td><span class="status success">{{ usageHint(record) }}</span></td>
                <td><button class="button ghost" type="button" @click.stop="selectArchive(record.id)">详情</button></td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </div>

    <!-- 右栏详情面板 -->
    <aside class="detail-panel-wrapper">
      <ArchiveDetailPanel :archive-id="selectedArchiveId" />
      <div class="card panel">
        <h2 class="section-title">安全边界</h2>
        <div class="notice">AI 只输出查询条件 JSON；正式检索、预览、下载、借阅申请都走后端业务接口和权限校验。</div>
      </div>
    </aside>
  </section>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import ArchiveDetailPanel from '@/views/internal/components/ArchiveDetailPanel.vue'
import { generateInternalAiQuery, searchInternalArchives } from '@/api/internal'
import { CarrierStatusLabel, SecurityLevelLabel } from '@/types/enums'
import type { InternalArchive, InternalAiQueryResult, InternalSearchParams } from '@/types/internal'

const aiText = ref('')
const aiLoading = ref(false)
const aiError = ref('')
const aiResult = ref<InternalAiQueryResult | null>(null)

const params = reactive<InternalSearchParams>({})
const fondsInput = ref('')
const hasFileSelect = ref<'' | 'yes' | 'no'>('')

const searching = ref(false)
const searchError = ref('')
const searched = ref(false)
const results = ref<{ records: InternalArchive[]; total: number }>({ records: [], total: 0 })
const selectedArchiveId = ref<number | null>(null)

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

function applyAiConditions() {
  if (!aiResult.value) return
  Object.assign(params, aiResult.value.conditions)
  ElMessage.success('条件已填充到表单，请确认后检索')
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

async function handleSearch() {
  searching.value = true
  searchError.value = ''
  selectedArchiveId.value = null
  try {
    const query: InternalSearchParams = { ...params }
    if (hasFileSelect.value === 'yes') query.hasElectronicFile = true
    else if (hasFileSelect.value === 'no') query.hasElectronicFile = false
    const page = await searchInternalArchives(query)
    results.value = { records: page.records, total: page.total }
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
}
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
.detail-panel-wrapper { display: grid; gap: 12px; position: sticky; top: 84px; }
@media (max-width: 1120px) {
  .search-layout { grid-template-columns: 1fr; }
  .detail-panel-wrapper { position: static; }
}
@media (max-width: 960px) {
  .filter-grid { grid-template-columns: repeat(2, minmax(0, 1fr)); }
}
</style>
