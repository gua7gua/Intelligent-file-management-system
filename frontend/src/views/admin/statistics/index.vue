<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { exportStatistics, getStatisticsOverview } from '@/api/statistics'
import type { StatisticsOverview, StatisticsParams } from '@/types/statistics'

const router = useRouter()
const overview = ref<StatisticsOverview | null>(null)
const loading = ref(true)
const errorMsg = ref('')
const yearStart = ref<number>(2020)
const yearEnd = ref<number>(2026)
const yearOptions = [2020, 2021, 2022, 2023, 2024, 2025, 2026]

const maxIntake = computed(() => overview.value?.yearlyIntake.reduce((m, b) => Math.max(m, b.count), 1) ?? 1)

function params(): StatisticsParams {
  return { yearStart: yearStart.value, yearEnd: yearEnd.value }
}

async function load() {
  loading.value = true
  errorMsg.value = ''
  try {
    overview.value = await getStatisticsOverview(params())
  } catch (e) {
    errorMsg.value = (e as Error).message || '统计数据加载失败'
  } finally {
    loading.value = false
  }
}

function go(route: string, query?: Record<string, string>) {
  router.push({ path: route, query: query ?? {} })
}

async function doExport(format: 'xlsx' | 'pdf') {
  try {
    const r = await exportStatistics(params(), format)
    ElMessage.success(r.message)
  } catch (e) {
    ElMessage.warning((e as Error).message || '导出失败')
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="toolbar">
      <div>
        <h1 class="page-title">数据统计</h1>
        <p class="page-subtitle">汇总馆藏、移交、征集、借阅、销毁和保存指标，支持按条件跳转查看明细。</p>
      </div>
      <div class="actions">
        <select id="yearStart" v-model="yearStart" aria-label="年度起">
          <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}</option>
        </select>
        <span class="muted">至</span>
        <select id="yearEnd" v-model="yearEnd" aria-label="年度止">
          <option v-for="y in yearOptions" :key="y" :value="y">{{ y }}</option>
        </select>
        <button class="button secondary refresh" @click="load"><span class="icon">R</span>刷新统计</button>
        <button class="button" @click="doExport('xlsx')"><span class="icon">E</span>导出统计表</button>
      </div>
    </div>

    <p v-if="loading" class="notice" style="margin-top:16px;">加载中…</p>
    <p v-else-if="errorMsg" class="notice" style="margin-top:16px;">
      {{ errorMsg }} <button class="button" @click="load">重试</button>
    </p>

    <section v-if="overview" class="grid four" style="margin-top:16px" aria-label="核心统计指标">
      <button
        v-for="m in overview.metrics"
        :key="m.key"
        class="metric clickable-metric"
        @click="go(m.targetRoute!, m.targetQuery)"
      >
        <span class="label">{{ m.label }}</span>
        <span class="value">{{ m.value.toLocaleString() }}</span>
        <span class="note">{{ m.source }}</span>
      </button>
    </section>

    <div v-if="overview" class="stats-layout" style="margin-top:16px">
      <section class="stack">
        <div class="card panel">
          <h2 class="section-title">年度进馆趋势</h2>
          <div class="chart">
            <div class="bar-row" v-for="b in overview.yearlyIntake" :key="b.year">
              <span>{{ b.year }}</span>
              <div class="bar-track"><div class="bar-fill" :style="{ width: (b.count / maxIntake * 100) + '%' }"></div></div>
              <strong>{{ b.count.toLocaleString() }}</strong>
            </div>
          </div>
        </div>

        <div class="grid two">
          <div class="card panel">
            <h2 class="section-title">档案门类分布</h2>
            <div class="chart">
              <div class="bar-row" v-for="d in overview.categoryDistribution" :key="d.label">
                <span>{{ d.label }}</span>
                <div class="bar-track"><div class="bar-fill" :style="{ width: (d.ratio * 100) + '%' }"></div></div>
                <strong>{{ Math.round(d.ratio * 100) }}%</strong>
              </div>
            </div>
          </div>

          <div class="card panel">
            <h2 class="section-title">纸电载体分布</h2>
            <div class="split">
              <div class="segment" v-for="(d, i) in overview.carrierDistribution" :key="d.label">
                <span class="muted">{{ d.label === 'electronic' ? '纯电子' : d.label === 'paper_electronic' ? '纸质+电子' : '纯纸质' }}</span>
                <strong>{{ Math.round(d.ratio * 100) }}%</strong>
                <span class="status" :class="['success', 'info', 'warning'][i]">{{ d.label === 'electronic' ? '纯电子' : d.label === 'paper_electronic' ? '纸质+电子' : '纯纸质' }}</span>
              </div>
            </div>
          </div>
        </div>

        <div class="card panel">
          <h2 class="section-title">业务汇总</h2>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>业务域</th>
                  <th>总数</th>
                  <th>状态明细</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="row in overview.businessBreakdown" :key="row.key">
                  <td>{{ row.domain }}</td>
                  <td>{{ row.total }}</td>
                  <td>
                    <span class="status" v-for="d in row.details" :key="d.label" style="margin-right:6px;">{{ d.label }} {{ d.count }}</span>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </section>

      <aside class="stack">
        <div class="card panel">
          <h2 class="section-title">领导关注指标</h2>
          <div class="timeline">
            <li><span>本月</span><div>馆藏新增 {{ overview.metrics[1]?.value.toLocaleString() ?? '—' }} 件，纸质相关待上架待统计。</div></li>
            <li><span>本季度</span><div>借阅申请 318 件，已归还 291 件，逾期 7 件。</div></li>
            <li><span>年度</span><div>销毁审批 12 册，已销毁 9 册，清册永久留存。</div></li>
          </div>
        </div>
      </aside>
    </div>
  </div>
</template>

<style scoped>
.stats-layout { display: grid; grid-template-columns: minmax(0,1fr) 320px; gap: 16px; }
.stack { display: grid; gap: 16px; }
.chart { display: grid; gap: 10px; }
.bar-row { display: grid; grid-template-columns: 56px minmax(0,1fr) 80px; align-items: center; gap: 10px; }
.bar-track { height: 12px; background: #f0f2f5; border-radius: 6px; overflow: hidden; }
.bar-fill { height: 100%; background: var(--primary, #1f6f78); border-radius: 6px; }
.split { display: grid; gap: 8px; }
.segment { display: grid; grid-template-columns: 80px 60px 1fr; align-items: center; gap: 8px; padding: 8px 0; border-bottom: 1px solid var(--border); }
.segment:last-child { border-bottom: none; }
.source-list, .timeline { list-style: none; margin: 0; padding: 0; display: grid; gap: 8px; }
.source-list li { display: flex; justify-content: space-between; align-items: center; }
.timeline li { display: grid; grid-template-columns: 64px minmax(0,1fr); gap: 8px; padding: 6px 0; border-bottom: 1px dashed var(--border); }
.actions { display: flex; align-items: center; gap: 8px; }
@media (max-width: 960px) { .stats-layout { grid-template-columns: 1fr; } }
</style>
