<template>
  <div>
    <div class="rule-strip">
      <div>
        <strong>架位编码：库房号-机架号-层号-盒位号</strong>
        <div class="hint">示例：<span class="mono">401-03-02-05</span>。纯电子档案不占架位，纸质+电子和纯纸质必须记录盒号与架位。</div>
      </div>
      <span class="status info">管理员可见</span>
    </div>
    <div v-if="locations.length === 0" class="empty">该库房暂无架位数据</div>
    <div v-else class="rack-board">
      <div v-for="group in rackGroups" :key="group.rackNo" class="rack-row">
        <div class="rack-label">机架 {{ pad(group.rackNo) }}</div>
        <div class="slot-grid">
          <button
            v-for="loc in group.items"
            :key="loc.id"
            class="slot"
            :class="[slotClass(loc), { active: modelValue === loc.id }]"
            @click="emit('update:modelValue', loc.id)"
          >
            <strong>{{ loc.locationCode }}</strong>
            <span>{{ slotText(loc) }}</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { StorageLocation } from '@/types/warehouse'

const props = defineProps<{ locations: StorageLocation[]; modelValue: number | null }>()
const emit = defineEmits<{ (e: 'update:modelValue', id: number): void }>()

function pad(n: number): string {
  return String(n).padStart(2, '0')
}

const rackGroups = computed(() => {
  const map = new Map<number, StorageLocation[]>()
  for (const l of props.locations) {
    if (!map.has(l.rackNo)) map.set(l.rackNo, [])
    map.get(l.rackNo)!.push(l)
  }
  return [...map.entries()]
    .sort((a, b) => a[0] - b[0])
    .map(([rackNo, items]) => ({
      rackNo,
      items: items.sort((a, b) => a.layerNo - b.layerNo || a.boxSlotNo - b.boxSlotNo),
    }))
})

function slotClass(loc: StorageLocation): string {
  if (loc.status === 'disabled') return 'disabled'
  if (loc.occupied) return 'occupied'
  return ''
}
function slotText(loc: StorageLocation): string {
  if (loc.status === 'disabled') return '停用'
  if (loc.occupied) return loc.currentBoxNo ?? ''
  return '空闲'
}
</script>

<style scoped>
.rule-strip { display: flex; justify-content: space-between; align-items: center; gap: 12px; margin-bottom: 14px; padding: 12px 14px; border: 1px solid #b9d7d9; border-radius: var(--radius); background: var(--primary-soft); }
.hint { color: var(--muted); font-size: 12px; }
.mono { font-family: monospace; }
.rack-board { display: grid; gap: 12px; max-height: calc(100vh - 320px); overflow-y: auto; padding-right: 4px; }
.rack-row { display: grid; grid-template-columns: 74px minmax(0, 1fr); gap: 10px; align-items: stretch; }
.rack-label { display: grid; place-items: center; min-height: 96px; border: 1px solid var(--border); border-radius: var(--radius); color: #34414d; background: #f7fafc; font-weight: 750; }
.slot-grid { display: grid; grid-template-columns: repeat(5, minmax(70px, 1fr)); gap: 8px; }
.slot { display: grid; min-height: 58px; align-content: center; gap: 3px; padding: 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); color: #34414d; background: #fff; text-align: left; cursor: pointer; }
.slot strong { font-size: 12px; }
.slot span { color: var(--muted); font-size: 11px; }
.slot:hover, .slot.active { border-color: #8abcbf; box-shadow: 0 4px 14px rgba(23, 33, 43, 0.08); }
.slot.occupied { border-color: #a9c9cc; background: #f2fbfb; }
.slot.disabled { color: var(--muted); background: #eef1f4; }
.empty { color: var(--muted); font-size: 13px; padding: 12px; }
.status { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.status.info { background: #e6f7ff; color: #1890ff; }
@media (max-width: 760px) { .slot-grid { grid-template-columns: repeat(2, 1fr); } .rack-row { grid-template-columns: 1fr; } }
</style>
