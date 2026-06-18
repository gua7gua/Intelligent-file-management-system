<template>
  <aside class="drawer">
    <template v-if="!location"><p class="hint">点击架位查看详情与操作</p></template>
    <template v-else>
      <h2 class="section-title">盒位详情</h2>
      <ul class="detail-list">
        <li><span>位置编码</span><strong class="mono">{{ location.locationCode }}</strong></li>
        <li><span>架位状态</span><span>{{ location.status === 'active' ? '启用' : '停用' }}</span></li>
        <li><span>当前盒号</span><span class="mono">{{ location.occupied ? (location.currentBoxNo ?? '—') : '无' }}</span></li>
        <li><span>盒内件数</span><span>{{ location.boxItemCount ?? 0 }} 件</span></li>
      </ul>

      <template v-if="!location.occupied && location.status === 'active'">
        <h3 class="section-title">新增档案盒到此位</h3>
        <div class="field"><label>档案分类</label>
          <select v-model.number="form.categoryId">
            <option v-for="c in categories" :key="c.id" :value="c.id">{{ c.name }}</option>
          </select>
        </div>
        <div class="field"><label>全宗</label>
          <select v-model.number="form.fondsId">
            <option v-for="f in fondsList" :key="f.id" :value="f.id">{{ f.fondsNo }} · {{ f.fondsName }}</option>
          </select>
        </div>
        <div class="field"><label>年度</label><input v-model="form.yearLabel"></div>
        <div class="field"><label>盒脊信息 <small style="color:var(--text-muted);font-weight:normal">档案盒侧面标签文字，用于库房实物识别</small></label><input v-model="form.spineText" placeholder="如：2025 会计凭证 01"></div>
        <div class="field"><label>容量</label><input v-model.number="form.capacity" type="number"></div>
        <div class="actions">
          <button class="button" @click="onCreateBox">新增档案盒</button>
          <button class="button ghost" @click="onDisable">停用架位</button>
        </div>
      </template>

      <template v-else-if="location.occupied">
        <div v-if="boxLoading" class="hint">盒详情加载中...</div>
        <template v-else-if="boxDetail">
          <h3 class="section-title">盒内档案（{{ boxDetail.items.length }} 件）</h3>
          <div v-if="boxDetail.items.length === 0" class="hint">盒内暂无档案条目</div>
          <ul v-else class="box-items">
            <li v-for="it in boxDetail.items" :key="it.archiveId">
              <span class="mono">{{ it.archiveNo }}</span> {{ it.title }}
              <span class="status" :class="physicalClass(it.physicalStatus)">{{ physicalLabel(it.physicalStatus) }}</span>
            </li>
          </ul>
        </template>
        <h3 class="section-title" style="margin-top:12px">移动档案盒</h3>
        <div class="field"><label>目标架位</label>
          <select v-model.number="moveTargetId">
            <option :value="0">请选择空闲架位</option>
            <option v-for="loc in freeLocations" :key="loc.id" :value="loc.id">{{ loc.locationCode }}</option>
          </select>
        </div>
        <div class="field"><label>移动原因</label><input v-model="moveReason"></div>
        <div class="actions">
          <button class="button" :disabled="!moveTargetId" @click="onMove">移动档案盒</button>
          <button class="button ghost" disabled title="已占用架位需先迁出档案盒">停用架位</button>
        </div>
        <p class="hint">已占用架位需迁出档案盒后才能停用。</p>
      </template>

      <template v-else>
        <p class="hint">该架位已停用。</p>
        <div class="actions"><button class="button ghost" @click="onEnable">启用架位</button></div>
      </template>
    </template>
  </aside>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { ArchiveBoxDetail, ArchiveBoxCreateData, StorageLocation } from '@/types/warehouse'
import type { FondsItem } from '@/types/fonds'
import { createArchiveBox, getArchiveBoxDetail, moveArchiveBox, updateLocationStatus } from '@/api/warehouse'
import { getFonds } from '@/api/fonds'

const props = defineProps<{ location: StorageLocation | null; freeLocations: StorageLocation[] }>()
const emit = defineEmits<{ (e: 'refresh'): void }>()

// 五大固定门类（categories 初始化字典，对齐后端种子：1 文书 2 科技 3 会计 4 音像 5 人事）
const categories = [
  { id: 1, name: '文书档案' },
  { id: 2, name: '科技档案' },
  { id: 3, name: '会计档案' },
  { id: 4, name: '音像档案' },
  { id: 5, name: '人事档案' },
]
const fondsList = ref<FondsItem[]>([])

const boxDetail = ref<ArchiveBoxDetail | null>(null)
const boxLoading = ref(false)
const form = ref<ArchiveBoxCreateData>({ locationId: 0, categoryId: 1, fondsId: 1, yearLabel: '2026', spineText: '', capacity: 30 })
const moveTargetId = ref(0)
const moveReason = ref('')

watch(
  () => props.location,
  async (loc) => {
    boxDetail.value = null
    if (loc && loc.occupied && loc.currentBoxId) {
      boxLoading.value = true
      try {
        boxDetail.value = await getArchiveBoxDetail(loc.currentBoxId)
      } catch (e) {
        ElMessage.error(e instanceof Error ? e.message : '盒详情加载失败')
      } finally {
        boxLoading.value = false
      }
    }
    if (loc) {
      form.value.locationId = loc.id
      moveTargetId.value = 0
      moveReason.value = ''
    }
  },
)

onMounted(async () => {
  try {
    const page = await getFonds({ pageSize: 100 })
    fondsList.value = page.records
    if (fondsList.value.length) form.value.fondsId = fondsList.value[0].id
  } catch {
    fondsList.value = []
  }
})

async function onCreateBox() {
  if (!props.location) return
  if (!form.value.spineText.trim()) {
    ElMessage.warning('请填写盒脊信息')
    return
  }
  try {
    await createArchiveBox({ ...form.value, locationId: props.location.id })
    ElMessage.success('档案盒已新增，架位已占用。')
    emit('refresh')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '新增失败')
  }
}
async function onMove() {
  if (!props.location?.currentBoxId || !moveTargetId.value) return
  if (!moveReason.value.trim()) {
    ElMessage.warning('请填写移动原因')
    return
  }
  try {
    await moveArchiveBox(props.location.currentBoxId, { targetLocationId: moveTargetId.value, reason: moveReason.value })
    ElMessage.success('档案盒已移动到目标架位。')
    emit('refresh')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '移动失败')
  }
}
async function onDisable() {
  if (!props.location) return
  try {
    await ElMessageBox.confirm('停用后该架位不可上架档案，是否继续？', '停用架位', { type: 'warning' })
  } catch {
    return
  }
  try {
    await updateLocationStatus(props.location.id, { status: 'disabled', reason: '维护停用' })
    ElMessage.success('架位已停用。')
    emit('refresh')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '停用失败')
  }
}
async function onEnable() {
  if (!props.location) return
  try {
    await updateLocationStatus(props.location.id, { status: 'active' })
    ElMessage.success('架位已启用。')
    emit('refresh')
  } catch (e) {
    ElMessage.error(e instanceof Error ? e.message : '启用失败')
  }
}
function physicalLabel(s: string): string {
  return ({ normal: '正常', damaged: '破损', lost: '遗失' } as Record<string, string>)[s] ?? s
}
function physicalClass(s: string): string {
  return ({ normal: 'success', damaged: 'warning', lost: 'danger' } as Record<string, string>)[s] ?? ''
}
</script>

<style scoped>
.drawer { padding: 14px; background: #fff; border: 1px solid var(--border); border-radius: var(--radius); }
.section-title { font-size: 14px; font-weight: 700; margin: 0 0 8px; }
.hint { color: var(--muted); font-size: 12px; }
.detail-list { list-style: none; margin: 0 0 12px; padding: 0; display: grid; gap: 8px; }
.detail-list li { display: grid; grid-template-columns: 80px 1fr; gap: 8px; padding-bottom: 8px; border-bottom: 1px solid var(--border); font-size: 13px; }
.detail-list span:first-child { color: var(--muted); font-weight: 700; }
.field { display: grid; gap: 4px; margin-bottom: 8px; }
.field label { font-size: 12px; color: var(--muted); font-weight: 700; }
.field input, .field select { padding: 6px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 13px; }
.box-items { list-style: none; margin: 0 0 12px; padding: 0; display: grid; gap: 6px; max-height: 200px; overflow-y: auto; }
.box-items li { font-size: 12px; padding: 6px; border: 1px solid var(--border); border-radius: var(--radius-sm); }
.actions { display: flex; gap: 8px; flex-wrap: wrap; margin-top: 8px; }
.button { padding: 6px 12px; border: 1px solid var(--primary); border-radius: var(--radius-sm); background: var(--primary); color: #fff; cursor: pointer; font-size: 12px; }
.button:disabled { opacity: 0.5; cursor: not-allowed; }
.button.ghost { background: #fff; color: var(--text); border-color: var(--border); }
.mono { font-family: monospace; font-size: 12px; }
.status { display: inline-block; padding: 1px 6px; border-radius: var(--radius-sm); font-size: 11px; font-weight: 600; }
.status.success { background: #f6ffed; color: #52c41a; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.danger { background: #fff1f0; color: #f5222d; }
</style>
