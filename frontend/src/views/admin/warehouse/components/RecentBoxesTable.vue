<template>
  <section class="card panel">
    <div class="toolbar" style="margin-top: 0">
      <h2 class="section-title">最近占用盒位</h2>
    </div>
    <div v-if="boxes.length === 0" class="empty">暂无档案盒</div>
    <div v-else class="table-wrap">
      <table>
        <thead>
          <tr><th>库房号</th><th>位置编码</th><th>盒号</th><th>年度</th><th>盒内件数</th><th>状态</th></tr>
        </thead>
        <tbody>
          <tr v-for="b in boxes" :key="b.id">
            <td>{{ b.roomNo }}</td>
            <td class="mono">{{ b.locationCode }}</td>
            <td>{{ b.boxNo }}</td>
            <td>{{ b.yearLabel }}</td>
            <td>{{ b.usedCount }} / {{ b.capacity }}</td>
            <td><span class="status" :class="boxStatusClass(b.status)">{{ BoxStatusLabel[b.status] }}</span></td>
          </tr>
        </tbody>
      </table>
    </div>
  </section>
</template>

<script setup lang="ts">
import type { ArchiveBox } from '@/types/warehouse'
import { BoxStatusLabel } from '@/types/enums'

defineProps<{ boxes: ArchiveBox[] }>()

function boxStatusClass(s: string): string {
  return ({ normal: 'success', full: 'warning', moved: 'info', destroyed: 'danger' } as Record<string, string>)[s] ?? ''
}
</script>

<style scoped>
.panel { padding: 14px; }
.toolbar { margin-bottom: 10px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0; }
.table-wrap { overflow-x: auto; }
.table-wrap table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table-wrap th, .table-wrap td { padding: 8px 10px; text-align: left; border-bottom: 1px solid var(--border); }
.table-wrap th { font-weight: 700; color: var(--muted); background: var(--bg); }
.mono { font-family: monospace; font-size: 12px; }
.empty { color: var(--muted); font-size: 13px; padding: 12px; }
.status { display: inline-block; padding: 2px 8px; border-radius: var(--radius-sm); font-size: 12px; font-weight: 600; }
.status.success { background: #f6ffed; color: #52c41a; }
.status.warning { background: #fff7e6; color: #fa8c16; }
.status.danger { background: #fff1f0; color: #f5222d; }
.status.info { background: #e6f7ff; color: #1890ff; }
</style>
