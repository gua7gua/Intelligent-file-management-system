<template>
  <div class="destroy-approval">
    <div class="notice info">
      审批通过后清册将进入待销毁流程，由档案管理员现场确认销毁。
    </div>
    <div class="card panel">
      <div class="detail-kv"><span>清册号</span><strong>{{ list.listNo }}</strong></div>
      <div class="detail-kv"><span>清册名称</span><strong>{{ list.listName }}</strong></div>
      <div class="detail-kv"><span>件数</span><strong>{{ list.itemCount }} 件</strong></div>
      <div class="detail-kv"><span>来源鉴定批次</span><strong>{{ list.appraisalBatchNo || '—' }}</strong></div>
    </div>
    <h3 class="section-title">清册快照</h3>
    <div class="table-wrap">
      <table>
        <thead>
          <tr><th>档号快照</th><th>题名快照</th><th>分类</th><th>保管期限</th><th>密级</th><th>鉴定意见</th></tr>
        </thead>
        <tbody>
          <tr v-for="it in list.items" :key="it.id">
            <td>{{ it.archiveNoSnapshot }}</td>
            <td>{{ it.titleSnapshot }}</td>
            <td>{{ it.categorySnapshot }}</td>
            <td>{{ it.retentionSnapshot }}</td>
            <td>{{ SecurityLevelLabel[it.securityLevelSnapshot] ?? it.securityLevelSnapshot }}</td>
            <td>{{ it.appraisalOpinionSnapshot }}</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { ApprovalRequestDetail } from '@/types/approval'
import { SecurityLevelLabel } from '@/types/enums'

const props = defineProps<{ detail: ApprovalRequestDetail }>()
const list = computed(() => props.detail.destructionList!)
</script>

<style scoped>
.destroy-approval { display: grid; gap: 12px; }
.notice.info { padding: 8px 10px; border-radius: var(--radius-sm); background: #e6f7ff; color: #096dd9; font-size: 12px; }
.panel { padding: 12px; }
.detail-kv { display: grid; grid-template-columns: 120px minmax(0,1fr); gap: 6px; padding: 4px 0; border-bottom: 1px solid var(--border); font-size: 13px; }
.detail-kv span { color: var(--muted); font-weight: 700; }
.section-title { font-size: 14px; font-weight: 700; margin: 0; }
.table-wrap { overflow-x: auto; }
.table-wrap table { width: 100%; border-collapse: collapse; font-size: 13px; }
.table-wrap th, .table-wrap td { padding: 8px 10px; text-align: left; border-bottom: 1px solid var(--border); }
.table-wrap th { font-weight: 700; color: var(--muted); background: var(--bg); }
</style>
