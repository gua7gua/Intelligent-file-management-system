<template>
  <div>
    <section>
      <h1 class="page-title">公开档案检索</h1>
      <p class="page-subtitle">检索已公开的正式档案元数据。</p>
      <div class="notice" style="margin-top: 8px">电子文件预览和下载需登录公众账号。</div>
    </section>

    <!-- AI 自然语言查询 -->
    <section class="card panel" style="margin-top: 18px">
      <h2 class="section-title">AI 智能查询</h2>
      <div class="form-grid" style="grid-template-columns: 1fr auto; margin-top: 12px">
        <div class="field">
          <label for="aiQuery">自然语言描述</label>
          <input id="aiQuery" v-model="aiQueryText" placeholder="例如：查找 2000 年以后公开的老城改造影像资料" />
        </div>
        <div class="field">
          <label>&nbsp;</label>
          <button class="button secondary" type="button" :disabled="aiLoading" @click="handleAiQuery">生成查询条件</button>
        </div>
      </div>
      <div v-if="aiResult" class="notice" style="margin-top: 12px">
        <strong>AI 生成条件</strong>
        <p style="margin: 8px 0 0">{{ aiSummary }}</p>
        <button class="button" type="button" style="margin-top: 8px" @click="applyAiConditions">应用条件并检索</button>
      </div>
    </section>

    <!-- 结构化搜索表单 -->
    <section class="card panel" style="margin-top: 16px">
      <h2 class="section-title">条件检索</h2>

      <!-- 公开元数据 -->
      <div class="filter-block">
        <div class="filter-block-head">
          <strong>公开元数据</strong>
          <span class="hint">仅展示已公开档案。</span>
        </div>
        <div class="form-grid">
          <div class="field">
            <label for="searchKeyword">关键词</label>
            <input id="searchKeyword" v-model="searchParams.keyword" placeholder="题名、责任者、档号、文件名" />
          </div>
          <div class="field">
            <label for="searchArchiveNo">档号</label>
            <input id="searchArchiveNo" v-model="searchParams.archiveNo" placeholder="如 A-2024-0001" />
          </div>
          <div class="field">
            <label for="searchTitle">题名</label>
            <input id="searchTitle" v-model="searchParams.title" placeholder="题名模糊检索" />
          </div>
          <div class="field">
            <label for="searchResponsible">责任者</label>
            <input id="searchResponsible" v-model="searchParams.responsibleText" placeholder="形成单位或人员" />
          </div>
          <div class="field">
            <label for="searchCategory">档案门类</label>
            <select id="searchCategory" v-model="searchParams.categoryId">
              <option :value="undefined">全部</option>
              <option v-for="c in categoryOptions" :key="c.value" :value="c.value">{{ c.label }}</option>
            </select>
          </div>
          <div class="field">
            <label for="searchTags">公开标签</label>
            <input id="searchTags" v-model="searchParams.tagIds" placeholder="多个标签用逗号分隔" />
          </div>
        </div>
      </div>

      <!-- 时间、来源与载体 -->
      <div class="filter-block">
        <div class="filter-block-head">
          <strong>时间、来源与载体</strong>
        </div>
        <div class="form-grid">
          <div class="field">
            <label for="searchYearStart">起始年度</label>
            <input id="searchYearStart" v-model.number="searchParams.formedYearStart" type="number" placeholder="如 2020" />
          </div>
          <div class="field">
            <label for="searchYearEnd">截止年度</label>
            <input id="searchYearEnd" v-model.number="searchParams.formedYearEnd" type="number" placeholder="如 2025" />
          </div>
          <div class="field">
            <label for="searchSource">档案来源</label>
            <select id="searchSource" v-model="searchParams.sourceType">
              <option value="">全部</option>
              <option v-for="s in sourceOptions" :key="s.value" :value="s.value">{{ s.label }}</option>
            </select>
          </div>
          <div class="field">
            <label for="searchCarrier">载体状态</label>
            <select id="searchCarrier" v-model="searchParams.carrierStatus">
              <option value="">不限</option>
              <option value="electronic">纯电子</option>
              <option value="paper_electronic">纸质+电子</option>
              <option value="paper">纯纸质</option>
            </select>
          </div>
        </div>
      </div>

      <!-- 电子文件 -->
      <div class="filter-block">
        <div class="filter-block-head">
          <strong>电子文件</strong>
          <span class="hint">下载需登录公众账号</span>
        </div>
        <div class="form-grid">
          <div class="field">
            <label for="searchFileState">电子文件</label>
            <select id="searchFileState" v-model="fileState">
              <option value="">全部</option>
              <option value="true">有电子文件</option>
              <option value="false">仅公开元数据</option>
            </select>
          </div>
        </div>
      </div>

      <div class="actions" style="margin-top: 12px">
        <button class="button" type="button" @click="handleSearch">检索</button>
        <button class="button ghost" type="button" @click="resetSearch">重置</button>
      </div>
    </section>

    <!-- 搜索结果 -->
    <div v-if="searchLoading" class="notice" style="margin-top: 16px">正在检索…</div>
    <div v-else-if="searchError" class="notice danger" style="margin-top: 16px">{{ searchError }}</div>
    <template v-else>
      <div class="toolbar" style="margin-top: 16px">
        <h2 class="section-title">检索结果（{{ searchResults.total }} 条）</h2>
      </div>
      <div class="table-wrap">
        <table>
          <thead>
            <tr>
              <th>档案号</th>
              <th>标题</th>
              <th>责任者</th>
              <th>分类</th>
              <th>年度</th>
              <th>载体</th>
              <th>来源</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-if="searchResults.records.length === 0">
              <td colspan="8"><div class="empty">没有符合条件的公开档案</div></td>
            </tr>
            <tr v-for="record in searchResults.records" :key="record.id">
              <td class="mono">{{ record.archiveNo }}</td>
              <td>{{ record.title }}</td>
              <td>{{ record.responsible }}</td>
              <td>{{ record.category }}</td>
              <td>{{ record.formedYear }}</td>
              <td>{{ carrierLabel(record.carrierStatus) }}</td>
              <td>{{ sourceLabel(record.sourceType) }}</td>
              <td>
                <button class="button ghost" type="button" @click="showDetail(record.id)">详情</button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <!-- 详情面板 -->
    <section v-if="detailData" class="card panel" style="margin-top: 16px">
      <div class="detail-head">
        <div>
          <h2 class="section-title">{{ detailData.title }}</h2>
          <p class="muted">{{ detailData.archiveNo }}</p>
        </div>
        <span class="status success">公开</span>
      </div>
      <div class="detail-meta" style="margin-top: 12px">
        <div class="meta-item"><span>责任者</span><strong>{{ detailData.responsible }}</strong></div>
        <div class="meta-item"><span>分类</span><strong>{{ detailData.category }}</strong></div>
        <div class="meta-item"><span>形成日期</span><strong>{{ detailData.formedDate }}</strong></div>
        <div class="meta-item"><span>保管期限</span><strong>{{ detailData.retentionPeriod }}</strong></div>
      </div>
      <p style="margin-top: 12px">{{ detailData.summary }}</p>

      <div v-if="detailData.files.length > 0" style="margin-top: 12px">
        <h3 class="section-title">电子文件</h3>
        <div v-for="file in detailData.files" :key="file.id" class="local-file">
          <div>
            <strong>{{ file.filename }}</strong>
            <div class="hint">{{ file.fileFormat }}，{{ (file.fileSize / 1024 / 1024).toFixed(1) }} MB</div>
          </div>
          <template v-if="file.canDownload">
            <button class="button secondary" type="button" @click="handleDownload(file.id, file.filename)">下载</button>
          </template>
          <template v-else>
            <span class="notice">下载需登录公众账号</span>
          </template>
        </div>
      </div>
      <div v-else class="notice" style="margin-top: 12px">
        {{ detailData.carrierStatus === 'paper' ? '纯纸质档案，暂无电子文件可供下载。' : '暂无可下载的电子文件。' }}
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  searchPublicArchives,
  getPublicArchiveDetail,
  generatePublicSearchQuery,
  downloadPublicArchiveFile,
} from '@/api/public'
import { getDictionariesApi } from '@/api/dictionary'
import type { DictItem } from '@/types/components'
import type { PublicSearchParams, PublicAiQueryResult, PublicArchive, PublicArchiveDetail } from '@/types/public'

const route = useRoute()

const aiQueryText = ref('')
const aiLoading = ref(false)
const aiResult = ref<PublicAiQueryResult | null>(null)

// 档案门类字典（categoryId 为整数），来源于全量字典
const categoryOptions = ref<DictItem[]>([])
const sourceOptions = [
  { value: 'transfer', label: '移交' },
  { value: 'collection', label: '征集' },
  { value: 'compilation', label: '编研' },
]

const searchParams = reactive<PublicSearchParams>({})

// 将 AI 生成的检索条件渲染为人类可读摘要
const aiSummary = computed(() => {
  const c = aiResult.value?.conditions as Record<string, unknown> | undefined
  if (!c) return '已生成检索条件，点击下方按钮执行检索。'
  const parts: string[] = []
  if (c.keyword) parts.push(`关键词：${c.keyword}`)
  if (c.title) parts.push(`题名：${c.title}`)
  if (c.responsibleText) parts.push(`责任者：${c.responsibleText}`)
  if (c.archiveNo) parts.push(`档号：${c.archiveNo}`)
  if (c.formedYearStart || c.formedYearEnd) {
    const start = c.formedYearStart ?? '不限'
    const end = c.formedYearEnd ?? '至今'
    parts.push(`年度：${start}-${end}`)
  }
  if (c.sourceType) parts.push(`来源：${sourceLabel(String(c.sourceType))}`)
  if (parts.length === 0) return '已生成检索条件，点击下方按钮执行检索。'
  return parts.join('　')
})
// hasElectronicFile 为布尔值，原生 select 以字符串代理写入
const fileState = computed<string>({
  get: () =>
    searchParams.hasElectronicFile === true ? 'true' : searchParams.hasElectronicFile === false ? 'false' : '',
  set: (v) => {
    searchParams.hasElectronicFile = v === 'true' ? true : v === 'false' ? false : undefined
  },
})
const searchLoading = ref(false)
const searchError = ref('')
const searchResults = ref<{ records: (PublicArchive & { openStatus: 'open' })[]; total: number }>({ records: [], total: 0 })
const detailData = ref<PublicArchiveDetail | null>(null)

function carrierLabel(status: string): string {
  const map: Record<string, string> = { electronic: '纯电子', paper_electronic: '纸质+电子', paper: '纯纸质' }
  return map[status] || status
}

function sourceLabel(sourceType: string): string {
  const map: Record<string, string> = { transfer: '移交', collection: '征集', compilation: '编撰' }
  return map[sourceType] || sourceType
}

async function handleAiQuery() {
  if (!aiQueryText.value.trim()) return
  aiLoading.value = true
  try {
    aiResult.value = await generatePublicSearchQuery({ text: aiQueryText.value })
  } catch {
    aiResult.value = null
  } finally {
    aiLoading.value = false
  }
}

function applyAiConditions() {
  if (!aiResult.value) return
  Object.assign(searchParams, aiResult.value.conditions)
  handleSearch()
}

async function handleSearch() {
  searchLoading.value = true
  searchError.value = ''
  detailData.value = null
  try {
    const params = cleanParams(searchParams)
    const result = await searchPublicArchives(params)
    searchResults.value = { records: result.records, total: result.total }
  } catch (e: any) {
    searchError.value = e.message || '检索失败'
  } finally {
    searchLoading.value = false
  }
}

// 剔除空值，避免把空字符串/undefined 作为查询参数下发
function cleanParams(src: PublicSearchParams): PublicSearchParams {
  const out: PublicSearchParams = {}
  for (const [k, v] of Object.entries(src)) {
    if (v === undefined || v === null || v === '') continue
    ;(out as Record<string, unknown>)[k] = v
  }
  return out
}

function resetSearch() {
  Object.keys(searchParams).forEach((key) => delete (searchParams as any)[key])
  searchResults.value = { records: [], total: 0 }
  detailData.value = null
  aiResult.value = null
}

async function showDetail(archiveId: number) {
  try {
    detailData.value = await getPublicArchiveDetail(archiveId)
  } catch {
    detailData.value = null
  }
}

async function handleDownload(fileId: number, filename: string) {
  try {
    const blob = await downloadPublicArchiveFile(fileId)
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = filename
    a.click()
    URL.revokeObjectURL(url)
  } catch {
    // 静默处理
  }
}

onMounted(async () => {
  // 加载门类字典，供档案门类下拉使用
  try {
    const dict = await getDictionariesApi()
    categoryOptions.value = dict.categories ?? []
  } catch {
    categoryOptions.value = []
  }

  const keyword = route?.query?.keyword as string
  if (keyword) {
    searchParams.keyword = keyword
    handleSearch()
  }
})
</script>

<style scoped>
.filter-block {
  margin-top: 14px;
}

.filter-block:first-of-type {
  margin-top: 12px;
}

.filter-block-head {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
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
