<template>
  <section class="card panel">
    <div class="toolbar" style="margin-top: 0">
      <h2 class="section-title">库房列表</h2>
      <span class="status info">告警阈值 85%</span>
    </div>
    <div class="room-list">
      <div v-if="rooms.length === 0" class="empty">暂无库房</div>
      <div
        v-for="r in rooms"
        :key="r.id"
        class="room-item"
        role="button"
        tabindex="0"
        :class="{ active: modelValue === r.id }"
        @click="emit('update:modelValue', r.id)"
      >
        <span class="room-head">
          <span>
            <strong>{{ r.roomNo }} {{ r.roomName }}</strong><br>
            <span class="muted">{{ r.rackCount }} 个机架 · {{ r.layersPerRack }} 层 · 每层 {{ r.boxesPerLayer }} 盒位</span>
          </span>
          <span class="room-head-right">
            <span class="status" :class="r.warning ? 'warning' : 'success'">{{ r.warning ? '容量告警' : '启用' }}</span>
            <el-button size="small" @click.stop="emit('edit', r)">编辑</el-button>
            <el-button size="small" type="danger" plain @click.stop="emit('delete', r.id)">删除</el-button>
          </span>
        </span>
        <span class="usage">
          <span class="usage-track"><span class="usage-fill" :class="{ warning: r.warning }" :style="{ width: Math.round(r.occupancyRate * 100) + '%' }"></span></span>
          <span class="usage-text"><span>已用 {{ r.occupiedSlots }} / {{ r.capacity }}</span><span>{{ Math.round(r.occupancyRate * 100) }}%</span></span>
        </span>
      </div>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { WarehouseRoom } from '@/types/warehouse'

defineProps<{ rooms: WarehouseRoom[]; modelValue: number | null }>()
const emit = defineEmits<{
  (e: 'update:modelValue', id: number): void
  (e: 'edit', room: WarehouseRoom): void
  (e: 'delete', id: number): void
}>()
</script>

<style scoped>
.panel { padding: 14px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin-bottom: 10px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0; }
.room-list { display: grid; gap: 12px; }
.room-item { display: grid; gap: 10px; padding: 14px; border: 1px solid var(--border); border-radius: var(--radius); background: #fff; text-align: left; cursor: pointer; }
.room-item:hover { background: #fafafa; }
.room-item.active { border-color: #8abcbf; box-shadow: 0 0 0 3px rgba(31, 111, 120, 0.1); }
.room-head { display: flex; justify-content: space-between; align-items: flex-start; gap: 10px; }
.room-head-right { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.usage { display: grid; gap: 6px; }
.usage-track { height: 9px; border-radius: 999px; background: #e5edf2; overflow: hidden; }
.usage-fill { display: block; height: 100%; background: var(--primary); }
.usage-fill.warning { background: var(--warning); }
.usage-text { display: flex; justify-content: space-between; color: var(--muted); font-size: 12px; font-weight: 650; }
.muted { color: var(--muted); font-size: 12px; }
.empty { color: var(--muted); font-size: 13px; padding: 12px; }
.status { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.status.success { background: #f6ffed; color: #52c41a; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.info { background: #e6f7ff; color: #1890ff; }
</style>
