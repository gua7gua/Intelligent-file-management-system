<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { getDashboardSummary } from '@/api/dashboard'
import type { DashboardSummary } from '@/types/dashboard'

const router = useRouter()
const summary = ref<DashboardSummary | null>(null)
const loading = ref(true)
const errorMsg = ref('')
const role = ref<'后台管理员' | '前台管理员' | '馆领导' | '系统管理员'>('后台管理员')

const roleNotice = computed(
  () => `当前示例角色：${role.value}。前端入口仅模拟展示，后端接口仍按角色、密级和数据范围鉴权。`,
)

interface MetricCard {
  label: string
  value: string
  note: string
  route: string
  query?: Record<string, string>
  severity?: 'info' | 'warning' | 'danger'
}

const metrics = computed<MetricCard[]>(() => {
  const s = summary.value
  if (!s) return []
  const usagePct = Math.round(s.archiveSummary.storageUsage * 100)
  const usageWarn = s.archiveSummary.storageUsage >= s.archiveSummary.storageWarningThreshold
  return [
    { label: '馆藏总量', value: s.archiveSummary.totalArchives.toLocaleString(), note: '正式档案 archives', route: '/admin/archive-management' },
    { label: '本月新增', value: s.archiveSummary.monthAdded.toLocaleString(), note: '入库完成记录', route: '/admin/archive-management', query: { month: 'current' } },
    { label: '待入库条目', value: String(s.todos.pendingArchive), note: '已接收/部分接收', route: '/admin/pending-archive' },
    { label: '待审批', value: String(s.todos.approvalPending), note: '借阅、开放、密级、销毁', route: '/admin/approval' },
    { label: '待销毁清册', value: String(s.todos.pendingDestruction), note: '馆领导已批准', route: '/admin/destruction' },
    { label: '到期提醒', value: String(s.todos.appraisalDue), note: '进入鉴定范围', route: '/admin/appraisal' },
    { label: '存储使用率', value: `${usagePct}%`, note: usageWarn ? '超过 85% 告警线' : '存储正常', route: '/admin/preservation', severity: usageWarn ? 'danger' : undefined },
    { label: '新借阅申请', value: String(s.todos.newBorrowRequests), note: '待后台审核', route: '/admin/borrow-approval' },
  ]
})

function go(route: string, query?: Record<string, string>) {
  router.push({ path: route, query: query ?? {} })
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    summary.value = await getDashboardSummary()
  } catch (e) {
    errorMsg.value = (e as Error).message || '概览数据加载失败'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <section>
      <h1 class="page-title">管理概览</h1>
      <p class="page-subtitle">按当前角色展示待办和入口。概览页只做提醒与跳转，不直接审批、销毁或批量修改档案。</p>
    </section>

    <div class="role" style="margin-top:16px;">
      <label for="roleSelect">角色</label>
      <select id="roleSelect" v-model="role">
        <option>后台管理员</option>
        <option>前台管理员</option>
        <option>馆领导</option>
        <option>系统管理员</option>
      </select>
    </div>

    <p class="notice" style="margin-top:12px;">{{ roleNotice }}</p>

    <p v-if="loading" class="notice" style="margin-top:16px;">加载中…</p>
    <p v-else-if="errorMsg" class="notice" style="margin-top:16px;">
      {{ errorMsg }} <button class="button" @click="load">重试</button>
    </p>

    <section v-if="summary" class="grid four" style="margin-top:16px;">
      <a
        v-for="m in metrics"
        :key="m.label"
        class="metric"
        :class="m.severity"
        href="javascript:void(0)"
        @click.prevent="go(m.route, m.query)"
      >
        <span class="label">{{ m.label }}</span>
        <span class="value">{{ m.value }}</span>
        <span class="note">{{ m.note }}</span>
      </a>
    </section>

    <section v-if="summary" class="grid two" style="margin-top:16px;">
      <div class="card panel">
        <h2 class="section-title">待办提醒</h2>
        <div class="todo-list">
          <a
            v-for="t in summary.todoEntries"
            :key="t.key"
            class="todo"
            href="javascript:void(0)"
            @click.prevent="go(t.targetRoute, t.targetQuery)"
          >
            <span>
              <strong>{{ t.title }}</strong><br>
              <span class="muted">{{ t.description }}</span>
            </span>
            <span class="status" :class="t.severity">{{ t.count }}</span>
          </a>
        </div>
      </div>
      <div class="card panel">
        <h2 class="section-title">最近操作日志</h2>
        <div
          v-for="log in summary.recentAuditLogs"
          :key="log.id"
          class="log-row"
        >
          <strong>{{ log.operator }}</strong>
          <span>{{ log.module }}</span>
          <span>{{ log.action }}</span>
          <span class="muted">{{ log.operatedAt }}</span>
        </div>
        <p class="hint">日志审计只查询和导出，不允许页面删除或修改日志。</p>
      </div>
    </section>
  </div>
</template>

<style scoped>
.todo-list { display: grid; gap: 10px; }
.todo { display:flex; justify-content:space-between; gap:12px; align-items:center; padding:12px; border:1px solid var(--border); border-radius:var(--radius); background:#fff; cursor:pointer; }
.todo:hover { border-color: var(--primary, #1f6f78); }
.log-row { display:grid; grid-template-columns: 120px 120px minmax(0,1fr) 150px; gap:10px; padding:10px 0; border-bottom:1px solid var(--border); }
.metric.danger { border-color: var(--danger, #c0392b); }
.metric.danger .value { color: var(--danger, #c0392b); }
.metric.warning { border-color: var(--warning, #e6a23c); }
.metric.warning .value { color: var(--warning, #e6a23c); }
.role { display:flex; align-items:center; gap:8px; }
.role select { padding: 4px 8px; }
@media (max-width: 760px) {
  .todo { align-items:flex-start; flex-direction:column; }
  .log-row { grid-template-columns:1fr; }
}
</style>
