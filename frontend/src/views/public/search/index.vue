<template>
  <div>
    <section>
      <h1 class="page-title">公开档案检索</h1>
      <p class="page-subtitle">检索非密、公开、未销毁的正式档案元数据。AI 查询只生成搜索条件，不会自动执行检索。</p>
      <div class="notice" style="margin-top: 8px">电子文件预览和下载需登录公众账号，下载需登录后记录访问日志。</div>
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
        <pre style="margin: 8px 0 0; white-space: pre-wrap; font-size: 13px">{{ JSON.stringify(aiResult.conditions, null, 2) }}</pre>
        <button class="button" type="button" style="margin-top: 8px" @click="applyAiConditions">应用条件并检索</button>
      </div>
    </section>

    <!-- 结构化搜索表单 -->
    <section class="card panel" style="margin-top: 16px">
      <h2 class="section-title">条件检索</h2>
      <div class="form-grid" style="margin-top: 12px">
        <div class="field">
          <label for="searchKeyword">关键词</label>
          <input id="searchKeyword" v-model="searchParams.keyword" placeholder="题名、责任者或档案号" />
        </div>
        <div class="field">
          <label for="searchYearStart">起始年度</label>
          <input id="searchYearStart" v-model.number="searchParams.formedYearStart" type="number" placeholder="如 2020" />
        </div>
        <div class="field">
          <label for="searchYearEnd">截止年度</label>
          <input id="searchYearEnd" v-model.number="searchParams.formedYearEnd" type="number" placeholder="如 2025" />
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
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import {
  searchPublicArchives,
  getPublicArchiveDetail,
  generatePublicSearchQuery,
  downloadPublicArchiveFile,
} from '@/api/public'
import type { PublicSearchParams, PublicAiQueryResult, PublicArchive, PublicArchiveDetail } from '@/types/public'

const route = useRoute()

const aiQueryText = ref('')
const aiLoading = ref(false)
const aiResult = ref<PublicAiQueryResult | null>(null)

const searchParams = reactive<PublicSearchParams>({})
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
    const params: PublicSearchParams = { ...searchParams }
    const result = await searchPublicArchives(params)
    searchResults.value = { records: result.records, total: result.total }
  } catch (e: any) {
    searchError.value = e.message || '检索失败'
  } finally {
    searchLoading.value = false
  }
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

onMounted(() => {
  const keyword = route?.query?.keyword as string
  if (keyword) {
    searchParams.keyword = keyword
    handleSearch()
  }
})
</script>

<style scoped>
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
