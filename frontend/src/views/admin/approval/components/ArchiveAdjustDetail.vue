<template>
  <div class="adjust-detail">
    <div v-if="!detail.evidenceMatched" class="notice danger">
      凭证档案来源关系不匹配（单位/全宗不一致），不可通过，只能退回补正。
    </div>
    <div v-else-if="!detail.evidenceArchive" class="notice warning">凭证档案缺失，不可通过。</div>

    <div class="dual">
      <div class="card panel">
        <h3 class="section-title">目标档案</h3>
        <div class="detail-kv"><span>档号</span><strong>{{ detail.targetArchive?.archiveNo || '—' }}</strong></div>
        <div class="detail-kv"><span>题名</span><strong>{{ detail.targetArchive?.title || '—' }}</strong></div>
        <div class="detail-kv"><span>分类</span><strong>{{ detail.targetArchive?.categoryName || '—' }}</strong></div>
        <div class="detail-kv"><span>单位</span><strong>{{ detail.targetArchive?.organizationName || '—' }}</strong></div>
        <div class="detail-kv"><span>全宗</span><strong>{{ detail.targetArchive?.fondsName || '—' }}</strong></div>
        <div class="detail-kv"><span>当前密级</span><strong>{{ SecurityLevelLabel[detail.targetArchive?.securityLevel ?? 0] }}</strong></div>
      </div>
      <div class="card panel">
        <h3 class="section-title">凭证档案</h3>
        <div class="detail-kv"><span>档号</span><strong>{{ detail.evidenceArchive?.archiveNo || '—' }}</strong></div>
        <div class="detail-kv"><span>题名</span><strong>{{ detail.evidenceArchive?.title || '—' }}</strong></div>
        <div class="detail-kv"><span>分类</span><strong>{{ detail.evidenceArchive?.categoryName || '—' }}</strong></div>
        <div class="detail-kv"><span>单位</span><strong>{{ detail.evidenceArchive?.organizationName || '—' }}</strong></div>
        <div class="detail-kv"><span>全宗</span><strong>{{ detail.evidenceArchive?.fondsName || '—' }}</strong></div>
        <div class="detail-kv"><span>密级</span><strong>{{ SecurityLevelLabel[detail.evidenceArchive?.securityLevel ?? 0] }}</strong></div>
      </div>
    </div>

    <div class="value-change">
      <span>调整前：<strong>{{ formatValue(detail.oldValue) }}</strong></span>
      <span class="arrow">→</span>
      <span>调整后：<strong>{{ formatValue(detail.newValue) }}</strong></span>
    </div>
  </div>
</template>

<script setup lang="ts">
import type { ApprovalRequestDetail } from '@/types/approval'
import { SecurityLevelLabel, OpenStatusLabel } from '@/types/enums'

const props = defineProps<{ detail: ApprovalRequestDetail }>()

function formatValue(v?: string): string {
  if (v === undefined || v === '') return '—'
  if (props.detail.approvalType === 'security_adjust') return SecurityLevelLabel[Number(v)] ?? v
  return OpenStatusLabel[v] ?? v
}
</script>

<style scoped>
.adjust-detail { display: grid; gap: 12px; }
.notice.danger { padding: 8px 10px; border-radius: var(--radius-sm); background: #fff1f0; color: #a8071a; font-size: 12px; }
.notice.warning { padding: 8px 10px; border-radius: var(--radius-sm); background: #fff7e6; color: #ad6800; font-size: 12px; }
.dual { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; }
.panel { padding: 12px; }
.section-title { font-size: 14px; font-weight: 700; margin: 0 0 6px; }
.detail-kv { display: grid; grid-template-columns: 80px minmax(0,1fr); gap: 6px; padding: 4px 0; border-bottom: 1px solid var(--border); font-size: 13px; }
.detail-kv span { color: var(--muted); font-weight: 700; }
.value-change { display: flex; gap: 12px; align-items: center; padding: 10px; background: var(--bg); border-radius: var(--radius-sm); font-size: 14px; }
.value-change .arrow { color: var(--primary); font-weight: 700; }
@media (max-width: 760px) { .dual { grid-template-columns: 1fr; } }
</style>
