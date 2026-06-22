<template>
  <div class="fonds-mgmt">
    <div class="toolbar">
      <div>
        <h1 class="page-title">全宗管理</h1>
        <p class="page-subtitle">维护全宗号、全宗名称和所属组织。</p>
      </div>
      <div class="actions">
        <el-button type="primary" @click="newFonds">新建全宗</el-button>
      </div>
    </div>

    <section class="grid four" aria-label="全宗概览">
      <div class="metric card"><div class="metric-num">{{ metrics.total }}</div><div class="metric-label">全宗总数</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.active }}</div><div class="metric-label">启用全宗</div><div class="metric-note">可被入库选择</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.archiveSum }}</div><div class="metric-label">归档档案</div></div>
      <div class="metric card"><div class="metric-num">{{ metrics.boxSum }}</div><div class="metric-label">档案盒</div></div>
    </section>

    <div class="fonds-layout">
      <section class="stack">
        <div class="card panel">
          <h2 class="section-title">筛选条件</h2>
          <div class="form-grid">
            <div class="field"><label>关键词</label><input v-model="filters.keyword" placeholder="全宗号、全宗名称、单位" /></div>
            <div class="field">
              <label>所属单位</label>
              <select v-model="filters.orgFilter">
                <option value="">全部单位</option>
                <option v-for="o in orgs" :key="o.id" :value="o.id">{{ o.orgName }}</option>
              </select>
            </div>
            <div class="field">
              <label>关联状态</label>
              <select v-model="filters.relation">
                <option value="">全部</option>
                <option value="linked">有归档数据</option>
                <option value="empty">无关联数据</option>
                <option value="disabled">已停用</option>
              </select>
            </div>
          </div>
          <div class="actions" style="margin-top:12px">
            <el-button type="primary" @click="applyFilter">查询</el-button>
            <el-button @click="resetFilter">重置</el-button>
          </div>
        </div>

        <div class="card panel">
          <div class="toolbar" style="margin-top:0">
            <h2 class="section-title">全宗列表</h2>
            <span class="hint">全宗号创建后不可修改</span>
          </div>
          <div v-if="loading && list.length === 0" class="detail-empty">加载中...</div>
          <div v-else-if="loadError" class="detail-empty">加载失败：<button class="link" @click="loadAll">重试</button></div>
          <div v-else-if="list.length === 0" class="detail-empty">暂无全宗</div>
          <div v-else class="table-wrap">
            <table>
              <thead>
                <tr><th>全宗号</th><th>全宗名称</th><th>所属单位</th><th>归档档案</th><th>档案盒</th><th>状态</th><th>操作</th></tr>
              </thead>
              <tbody>
                <tr v-for="f in list" :key="f.id" :class="['fonds-row', { selected: f.id === selectedId }]" @click="selectRow(f)">
                  <td class="mono">{{ f.fondsNo }}</td>
                  <td>{{ f.fondsName }}</td>
                  <td>{{ f.organizationName || '-' }}</td>
                  <td>{{ f.archiveCount }}</td>
                  <td>{{ f.boxCount }}</td>
                  <td><span class="status" :class="f.status === 'active' ? 'success' : 'warning'">{{ statusLabel(f.status) }}</span></td>
                  <td>
                    <div class="actions" @click.stop>
                      <el-button size="small" @click="selectRow(f)">编辑</el-button>
                      <el-button v-if="f.status === 'active'" size="small" type="warning" @click="removeOrDisable(f)">停用</el-button>
                      <el-button v-else size="small" type="success" @click="enableFonds(f)">启用</el-button>
                      <el-button size="small" type="danger" @click="onDelete(f)">删除</el-button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
            <div v-if="total > 0" style="display:flex;justify-content:flex-end;margin-top:12px">
              <el-pagination
                v-model:current-page="pageNo"
                v-model:page-size="pageSize"
                :total="total"
                :page-sizes="[10, 20, 50, 100]"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="loadAll"
                @current-change="loadAll"
              />
            </div>
          </div>
        </div>

        <div class="grid two">
          <div class="card panel">
            <h2 class="section-title">门类分布</h2>
            <ul v-if="categoryDistribution.length" class="bar-list">
              <li v-for="c in categoryDistribution" :key="c.category" class="bar-item">
                <div class="bar-head"><span>{{ c.category }}</span><span>{{ c.count }} 件</span></div>
                <div class="bar-track"><div class="bar-fill" :style="{ width: barWidth(c.count) + '%' }"></div></div>
              </li>
            </ul>
            <div v-else class="detail-empty">暂无门类数据</div>
          </div>
          <div class="card panel">
            <h2 class="section-title">最近入库记录</h2>
            <ul v-if="recentIntake.length" class="timeline">
              <li v-for="(r, i) in recentIntake" :key="i"><span class="mono">{{ r.date }}</span><strong>{{ r.title }}</strong></li>
            </ul>
            <div v-else class="detail-empty">暂无入库记录</div>
          </div>
        </div>
      </section>

      <aside class="drawer">
        <h2 class="section-title">{{ isCreate ? '新建全宗' : '编辑全宗' }}</h2>
        <div class="detail-kv"><span>当前状态</span><strong>{{ isCreate ? '待保存' : statusLabel(selectedItem?.status) }}</strong></div>
        <div class="detail-kv"><span>关联数据</span><strong>{{ selectedItem?.archiveCount ?? 0 }} 件档案 / {{ selectedItem?.boxCount ?? 0 }} 个档案盒</strong></div>

        <div class="field">
          <label>全宗号</label>
          <input v-model="form.fondsNo" data-testid="fondsNo" :disabled="!isCreate" :placeholder="isCreate ? '留空自动生成（F### 续编）' : ''" />
          <span class="hint">创建后不可修改；新建时留空则由后端按 F### 续编自动生成并校验唯一。</span>
        </div>
        <div class="field"><label>全宗名称</label><input v-model="form.fondsName" /></div>
        <div class="field">
          <label>所属单位</label>
          <select v-model="form.organizationId">
            <option :value="undefined" disabled>请选择所属单位</option>
            <option v-for="o in orgs" :key="o.id" :value="o.id">{{ o.orgName }}</option>
          </select>
        </div>
        <div class="field"><label>全宗说明</label><textarea v-model="form.description" rows="3"></textarea></div>

        <div class="notice warning">
          <strong>审计留痕</strong>
          <div>全宗号创建后不可修改；修改全宗名称、所属单位或说明后，保存会记录审计日志。</div>
        </div>

        <div class="actions">
          <el-button type="primary" :loading="saving" @click="save">{{ isCreate ? '新建全宗' : '保存全宗' }}</el-button>
          <el-button @click="cancelEdit">取消</el-button>
          <el-button v-if="!isCreate && selectedItem && selectedItem.status === 'active'" type="warning" @click="removeOrDisable(selectedItem)">停用全宗</el-button>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createFonds, getFonds, removeFonds, updateFonds } from '@/api/fonds'
import { getOrganizations } from '@/api/organizations'
import { validateFondsForm } from '@/utils/fondsValidation'
import type { FondsItem } from '@/types/fonds'
import type { Organization } from '@/types/organization'

const list = ref<FondsItem[]>([])
const orgs = ref<Organization[]>([])
const loading = ref(false)
const loadError = ref(false)
const saving = ref(false)

// ── 分页 ──
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

const filters = reactive({ keyword: '', orgFilter: '' as number | '', relation: '' as '' | 'linked' | 'empty' | 'disabled' })
const applied = reactive({ keyword: '', orgFilter: '' as number | '', relation: '' as '' | 'linked' | 'empty' | 'disabled' })

const isCreate = ref(true)
const selectedId = ref<number | null>(null)
const form = reactive({ fondsNo: '', fondsName: '', organizationId: undefined as number | undefined, description: '' })

const metrics = computed(() => ({
  total: total.value,
  active: list.value.filter((f) => f.status === 'active').length,
  archiveSum: list.value.reduce((s, f) => s + f.archiveCount, 0),
  boxSum: list.value.reduce((s, f) => s + f.boxCount, 0),
}))
const selectedItem = computed(() => list.value.find((f) => f.id === selectedId.value) ?? null)
const categoryDistribution = computed(() => selectedItem.value?.categoryDistribution ?? [])
const recentIntake = computed(() => selectedItem.value?.recentIntake ?? [])

function statusLabel(status?: 'active' | 'disabled'): string {
  return status === 'active' ? '启用' : status === 'disabled' ? '停用' : '-'
}
function isLinked(f?: FondsItem | null): boolean {
  return !!f && (f.archiveCount > 0 || f.boxCount > 0)
}
function barWidth(count: number): number {
  const max = Math.max(...categoryDistribution.value.map((c) => c.count), 1)
  return Math.max((count / max) * 100, count ? 8 : 0)
}

async function loadAll() {
  loading.value = true
  loadError.value = false
  try {
    const [f, o] = await Promise.all([
      getFonds({ pageNo: pageNo.value, pageSize: pageSize.value, keyword: applied.keyword || undefined, organizationId: applied.orgFilter || undefined, relation: applied.relation || undefined }),
      getOrganizations(),
    ])
    list.value = f.records
    total.value = f.total
    orgs.value = o.records
    if (!selectedId.value && list.value.length) selectRow(list.value[0])
  } catch {
    loadError.value = true
    ElMessage.error('全宗数据加载失败')
  } finally {
    loading.value = false
  }
}

function applyFilter() {
  applied.keyword = filters.keyword
  applied.orgFilter = filters.orgFilter
  applied.relation = filters.relation
  pageNo.value = 1
  loadAll()
}
function resetFilter() {
  filters.keyword = ''
  filters.orgFilter = ''
  filters.relation = ''
  applied.keyword = ''
  applied.orgFilter = ''
  applied.relation = ''
  pageNo.value = 1
  loadAll()
}

function newFonds() {
  isCreate.value = true
  selectedId.value = null
  form.fondsNo = ''
  form.fondsName = ''
  form.organizationId = orgs.value[0]?.id
  form.description = ''
  ElMessage.success('已进入新建全宗状态。')
}

function selectRow(f: FondsItem) {
  isCreate.value = false
  selectedId.value = f.id
  form.fondsNo = f.fondsNo
  form.fondsName = f.fondsName
  form.organizationId = f.organizationId
  form.description = f.description ?? ''
}

function cancelEdit() {
  if (selectedItem.value) selectRow(selectedItem.value)
  else newFonds()
  ElMessage.info('已取消本次编辑。')
}

async function save() {
  const existing = list.value.filter((f) => f.id !== selectedId.value).map((f) => f.fondsNo)
  const result = validateFondsForm({ fondsNo: form.fondsNo, fondsName: form.fondsName, organizationId: form.organizationId }, existing, isCreate.value)
  if (!result.valid) {
    ElMessage.error(result.errors[0])
    return
  }
  saving.value = true
  try {
    if (isCreate.value) {
      const created = await createFonds({ fondsNo: form.fondsNo, fondsName: form.fondsName, organizationId: form.organizationId!, description: form.description })
      ElMessage.success('全宗已新建，可在待入库与上架页面选择该全宗。')
      await loadAll()
      selectRow(created)
    } else {
      await updateFonds(selectedId.value!, { fondsName: form.fondsName, organizationId: form.organizationId, description: form.description })
      ElMessage.success('全宗已保存')
      await loadAll()
    }
  } catch (e) {
    ElMessage.error((e as Error).message || '保存全宗失败')
  } finally {
    saving.value = false
  }
}

async function removeOrDisable(f: FondsItem) {
  try {
    // 停用：仅置 status=disabled，保留全宗与历史归属；如需彻底清理误建全宗，使用「删除」（onDelete，关联 fonds_id 置空）
    await updateFonds(f.id, { status: 'disabled' })
    ElMessage.success(isLinked(f) ? '该全宗已有归档档案或档案盒，已停用。' : '无关联数据的全宗已停用。')
    await loadAll()
  } catch (e) {
    ElMessage.error((e as Error).message || '操作失败')
  }
}

async function enableFonds(f: FondsItem) {
  try {
    await updateFonds(f.id, { status: 'active' })
    ElMessage.success('全宗已启用')
    await loadAll()
  } catch (e) {
    ElMessage.error((e as Error).message || '启用失败')
  }
}

async function onDelete(f: FondsItem) {
  const linked = isLinked(f)
  const tip = linked
    ? `该全宗关联 ${f.archiveCount} 件档案 / ${f.boxCount} 个档案盒，删除后这些记录的全宗字段将被清空，且不可恢复。确认删除？`
    : '删除全宗不可恢复，确认删除？'
  try {
    await ElMessageBox.confirm(tip, '删除全宗', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消',
    })
  } catch {
    return // 用户取消
  }
  try {
    await removeFonds(f.id)
    ElMessage.success('全宗已删除')
    if (selectedId.value === f.id) selectedId.value = null
    await loadAll()
  } catch (e) {
    ElMessage.error((e as Error).message || '删除失败')
  }
}

onMounted(loadAll)
</script>

<style scoped>
.fonds-mgmt { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; }
.page-subtitle { color: #909399; margin-top: 6px; }
.toolbar { display: flex; justify-content: space-between; align-items: flex-start; gap: 8px; }
.actions { display: flex; gap: 8px; align-items: center; }
.grid.four { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 16px; }
.grid.two { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
.metric.card { padding: 14px; }
.metric-num { font-size: 22px; font-weight: 800; color: var(--primary, #1f6f78); }
.metric-label { font-size: 13px; color: #606266; }
.metric-note { font-size: 11px; color: #909399; margin-top: 4px; }
.fonds-layout { display: grid; grid-template-columns: minmax(0, 1fr) 390px; gap: 16px; align-items: start; margin-top: 16px; }
.stack { display: grid; gap: 16px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 10px; }
.form-grid { display: grid; grid-template-columns: 1fr 1fr 1fr; gap: 10px; }
.field { display: grid; gap: 4px; }
.field label { font-size: 12px; color: #909399; }
.field input, .field select, .field textarea { width: 100%; padding: 6px 8px; border: 1px solid var(--border, #e4e7ed); border-radius: 6px; font: inherit; box-sizing: border-box; }
.field textarea { resize: vertical; }
.hint { font-size: 12px; color: #909399; }
.table-wrap { overflow-x: auto; }
.fonds-row { cursor: pointer; }
.fonds-row.selected td { background: #f7fcfc; }
.fonds-mgmt .table-wrap td .actions { flex-wrap: nowrap; gap: 6px; }
.fonds-mgmt :deep(.table-wrap .el-button + .el-button) { margin-left: 0; }
.bar-list { display: grid; gap: 10px; margin: 0; padding: 0; list-style: none; }
.bar-item { display: grid; gap: 6px; }
.bar-head { display: flex; justify-content: space-between; gap: 10px; font-size: 13px; font-weight: 750; }
.bar-track { height: 8px; overflow: hidden; border-radius: 999px; background: var(--surface-muted, #f0f2f5); }
.bar-fill { height: 100%; border-radius: inherit; background: var(--primary, #1f6f78); }
.timeline { display: grid; gap: 10px; margin: 0; padding: 0; list-style: none; }
.timeline li { display: grid; gap: 4px; padding: 8px 0; border-bottom: 1px solid var(--border, #e4e7ed); }
.detail-kv { display: grid; grid-template-columns: 96px 1fr; gap: 8px; padding: 9px 0; border-bottom: 1px solid var(--border, #e4e7ed); }
.detail-kv span { color: #909399; font-weight: 700; }
.detail-empty { color: #909399; padding: 16px; text-align: center; }
.link { background: none; border: none; color: var(--primary, #1f6f78); cursor: pointer; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }
@media (max-width: 1120px) { .fonds-layout { grid-template-columns: 1fr; } }
</style>
