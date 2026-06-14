<template>
  <div class="settings">
    <section>
      <h1 class="page-title">系统配置</h1>
      <p class="page-subtitle">维护上传白名单、上传大小、AI 开关、公开检索开关和业务默认参数；配置变更记录审计日志。</p>
    </section>

    <section class="grid four" aria-label="配置概览">
      <div class="metric card"><div class="metric-num">{{ editableCount }}</div><div class="metric-label">可编辑配置</div><div class="metric-note">editable = true</div></div>
      <div class="metric card"><div class="metric-num">{{ aiEnabled ? '开' : '关' }}</div><div class="metric-label">AI 功能</div><div class="metric-note">ai.enabled</div></div>
      <div class="metric card"><div class="metric-num">{{ publicEnabled ? '开' : '关' }}</div><div class="metric-label">公开检索</div><div class="metric-note">public_search.enabled</div></div>
      <div class="metric card"><div class="metric-num">{{ savedAt }}</div><div class="metric-label">最近保存</div><div class="metric-note">写入 audit_logs</div></div>
    </section>

    <div class="toolbar">
      <div></div>
      <div class="actions">
        <el-button :loading="loading" @click="resetConfig">恢复本页示例</el-button>
        <el-button type="primary" :loading="saving" @click="saveConfig">保存配置</el-button>
      </div>
    </div>

    <div class="settings-layout">
      <section class="stack">
        <div v-if="loading && configs.length === 0" class="detail-empty">加载中...</div>
        <div v-else-if="loadError" class="detail-empty">加载失败：<button class="link" @click="loadAll">重试</button></div>
        <template v-else>
          <div class="card panel">
            <h2 class="section-title">文件上传配置</h2>
            <div class="config-row format-row">
              <div class="config-key"><strong>上传格式白名单</strong><span class="mono">upload.allowed_extensions</span></div>
              <div class="format-content">
                <div class="tag-list">
                  <span class="format-tag" v-for="ext in extensions" :key="ext">{{ ext }}</span>
                </div>
                <div class="add-format-inline">
                  <input v-model="newExt" placeholder="新增格式，如 tif" @keyup.enter="addFormat" />
                  <el-button @click="addFormat">添加格式</el-button>
                </div>
              </div>
            </div>
            <div class="config-row">
              <div class="config-key"><strong>最大上传大小</strong><span class="mono">upload.max_file_size_mb</span></div>
              <div class="field"><label>大小限制 MB</label><input type="number" min="1" max="2048" v-model.number="maxSize" /></div>
              <span class="status">number</span>
            </div>
          </div>

          <div class="card panel">
            <h2 class="section-title">能力开关</h2>
            <div class="config-row">
              <div class="config-key"><strong>AI 功能开关</strong><span class="mono">ai.enabled</span></div>
              <label class="switch-line"><input type="checkbox" v-model="aiEnabled" @change="onAiToggle" /><span>启用 AI 补全、检索 JSON 和数据研判建议</span></label>
              <span class="status info">boolean</span>
            </div>
            <div class="config-row">
              <div class="config-key"><strong>公开检索开关</strong><span class="mono">public_search.enabled</span></div>
              <label class="switch-line"><input type="checkbox" v-model="publicEnabled" @change="onPublicToggle" /><span>启用公众门户公开档案检索入口</span></label>
              <span class="status info">boolean</span>
            </div>
            <div class="notice" style="margin-top:12px">AI 关闭时业务流程回退为人工填写和手工筛选；公开检索关闭只影响入口，不改变正式档案的公开字段。</div>
          </div>

          <div class="card panel">
            <h2 class="section-title">业务默认参数</h2>
            <div class="config-row">
              <div class="config-key"><strong>库房占用告警阈值</strong><span class="mono">warehouse.usage_warning_threshold</span></div>
              <div class="field"><label>阈值百分比</label><input type="number" min="50" max="100" v-model.number="warehouseThreshold" /></div>
              <span class="status">number</span>
            </div>
            <div class="config-row">
              <div class="config-key"><strong>默认借阅天数</strong><span class="mono">borrow.default_days</span></div>
              <div class="field"><label>天数</label><input type="number" min="1" max="90" v-model.number="borrowDays" /></div>
              <span class="status">number</span>
            </div>
            <div class="config-row">
              <div class="config-key"><strong>征集捐赠协议文案</strong><span class="mono">collection.agreement_text</span></div>
              <div class="field"><label>协议文案</label><textarea v-model="agreementText" rows="3"></textarea></div>
              <span class="status">string</span>
            </div>
          </div>

          <div class="card panel">
            <h2 class="section-title">配置项明细</h2>
            <div class="table-wrap">
              <table>
                <thead><tr><th>配置键</th><th>当前值</th><th>类型</th><th>可编辑</th><th>说明</th></tr></thead>
                <tbody>
                  <tr v-for="c in configs" :key="c.configKey">
                    <td class="mono">{{ c.configKey }}</td>
                    <td>{{ displayValue(c) }}</td>
                    <td>{{ c.valueType }}</td>
                    <td><span class="status" :class="c.editable ? 'success' : ''">{{ c.editable ? '是' : '否' }}</span></td>
                    <td>{{ c.description }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </template>
      </section>

      <aside class="stack">
        <div class="card panel">
          <h2 class="section-title">配置边界</h2>
          <ul class="timeline">
            <li><span>上传</span><div>只维护格式白名单和大小限制。</div></li>
            <li><span>AI</span><div>统一启停补全、检索 JSON 和数据研判建议。</div></li>
            <li><span>公开检索</span><div>只控制公众入口，不直接改档案公开状态。</div></li>
            <li><span>征集协议</span><div>公众提交时在线展示并记录同意时间。</div></li>
          </ul>
        </div>
        <div class="notice warning"><strong>审批边界</strong><div>配置页不办理密级调整、开放调整或销毁审批；这些业务必须进入审批工作台并保留审批记录。</div></div>
        <div class="notice"><strong>审计要求</strong><div>保存配置写入 <span class="mono">audit_logs</span>，不可在页面删除审计记录。</div></div>
      </aside>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { batchUpdateSystemConfigs, getSystemConfigs } from '@/api/system-settings'
import type { SystemConfig } from '@/types/system-settings'
import { normalizeExtensions, validateConfigs } from '@/utils/systemConfigValidation'

const configs = ref<SystemConfig[]>([])
const loading = ref(false)
const loadError = ref(false)
const saving = ref(false)
const savedAt = ref('-')

const extensions = ref<string[]>([])
const newExt = ref('')
const maxSize = ref(512)
const aiEnabled = ref(true)
const publicEnabled = ref(true)
const warehouseThreshold = ref(85)
const borrowDays = ref(14)
const agreementText = ref('')

const editableCount = computed(() => configs.value.filter((c) => c.editable).length)

function displayValue(c: SystemConfig): string {
  if (c.configKey === 'upload.allowed_extensions') return extensions.value.join(', ')
  if (c.configKey === 'upload.max_file_size_mb') return String(maxSize.value)
  if (c.configKey === 'ai.enabled') return String(aiEnabled.value)
  if (c.configKey === 'public_search.enabled') return String(publicEnabled.value)
  if (c.configKey === 'warehouse.usage_warning_threshold') return String(warehouseThreshold.value)
  if (c.configKey === 'borrow.default_days') return String(borrowDays.value)
  if (c.configKey === 'collection.agreement_text') return agreementText.value ? '捐赠协议文案' : '-'
  return c.configValue
}

function hydrateFromConfigs() {
  for (const c of configs.value) {
    if (c.configKey === 'upload.allowed_extensions') extensions.value = c.configValue.split(',').map((s) => s.trim()).filter(Boolean)
    else if (c.configKey === 'upload.max_file_size_mb') maxSize.value = Number(c.configValue)
    else if (c.configKey === 'ai.enabled') aiEnabled.value = c.configValue === 'true'
    else if (c.configKey === 'public_search.enabled') publicEnabled.value = c.configValue === 'true'
    else if (c.configKey === 'warehouse.usage_warning_threshold') warehouseThreshold.value = Number(c.configValue)
    else if (c.configKey === 'borrow.default_days') borrowDays.value = Number(c.configValue)
    else if (c.configKey === 'collection.agreement_text') agreementText.value = c.configValue
  }
}

async function loadAll() {
  loading.value = true
  loadError.value = false
  try {
    configs.value = await getSystemConfigs()
    hydrateFromConfigs()
    const latest = configs.value.filter((c) => c.updatedAt).sort((a, b) => (b.updatedAt! > a.updatedAt! ? 1 : -1))[0]
    savedAt.value = latest?.updatedAt?.slice(11, 16) ?? '-'
  } catch {
    loadError.value = true
    ElMessage.error('系统配置加载失败')
  } finally {
    loading.value = false
  }
}

function addFormat() {
  const normalized = normalizeExtensions([newExt.value])
  if (normalized.length === 0) {
    ElMessage.warning('请输入要加入白名单的扩展名。')
    return
  }
  for (const ext of normalized) {
    if (!extensions.value.includes(ext)) extensions.value.push(ext)
  }
  newExt.value = ''
  ElMessage.success('格式已加入白名单，保存后写入 system_configs。')
}

function onAiToggle() {
  ElMessage.success(aiEnabled.value ? 'AI 功能已启用。' : 'AI 已关闭，业务流程回退为人工处理。')
}
function onPublicToggle() {
  ElMessage.success(publicEnabled.value ? '公开检索入口已启用。' : '公开检索入口已关闭，档案公开字段不变化。')
}

function resetConfig() {
  hydrateFromConfigs()
  ElMessage.success('已恢复本页示例值。')
}

async function saveConfig() {
  const items: SystemConfig[] = [
    { configKey: 'upload.allowed_extensions', configValue: extensions.value.join(','), valueType: 'json', editable: true },
    { configKey: 'upload.max_file_size_mb', configValue: String(maxSize.value), valueType: 'number', editable: true },
    { configKey: 'ai.enabled', configValue: String(aiEnabled.value), valueType: 'boolean', editable: true },
    { configKey: 'public_search.enabled', configValue: String(publicEnabled.value), valueType: 'boolean', editable: true },
    { configKey: 'warehouse.usage_warning_threshold', configValue: String(warehouseThreshold.value), valueType: 'number', editable: true },
    { configKey: 'borrow.default_days', configValue: String(borrowDays.value), valueType: 'number', editable: true },
    { configKey: 'collection.agreement_text', configValue: agreementText.value, valueType: 'string', editable: true },
  ]
  const result = validateConfigs(items)
  if (!result.valid) {
    ElMessage.error(result.errors[0])
    return
  }
  saving.value = true
  try {
    await batchUpdateSystemConfigs({ items: items.map((i) => ({ configKey: i.configKey, configValue: i.configValue })) })
    configs.value = await getSystemConfigs()
    savedAt.value = '刚刚'
    ElMessage.success('配置已保存，变更写入 audit_logs。')
  } catch (e) {
    ElMessage.error((e as Error).message || '保存配置失败')
  } finally {
    saving.value = false
  }
}

onMounted(loadAll)
</script>

<style scoped>
.settings { padding: 0; }
.page-title { font-size: 20px; font-weight: 700; }
.page-subtitle { color: #909399; margin-top: 6px; }
.grid.four { display: grid; grid-template-columns: repeat(4, 1fr); gap: 12px; margin-top: 16px; }
.metric.card { padding: 14px; }
.metric-num { font-size: 22px; font-weight: 800; color: var(--primary, #1f6f78); }
.metric-label { font-size: 13px; color: #606266; }
.metric-note { font-size: 11px; color: #909399; margin-top: 4px; }
.toolbar { display: flex; justify-content: space-between; align-items: center; margin: 16px 0; }
.actions { display: flex; gap: 8px; }
.settings-layout { display: grid; grid-template-columns: minmax(0, 1fr) 380px; gap: 16px; align-items: start; }
.stack { display: grid; gap: 16px; }
.config-row { display: grid; grid-template-columns: 220px minmax(0, 1fr) 130px; gap: 12px; align-items: center; padding: 13px 0; border-bottom: 1px solid var(--border, #e4e7ed); }
.config-key { display: grid; gap: 3px; min-width: 0; overflow-wrap: anywhere; }
.switch-line { display: flex; flex-wrap: wrap; gap: 10px; align-items: center; }
.switch-line input { width: 18px; height: 18px; }
.tag-list { display: flex; flex-wrap: wrap; gap: 7px; }
.format-tag { display: inline-flex; align-items: center; gap: 6px; padding: 5px 9px; border-radius: 999px; color: #23494f; background: rgba(31, 111, 120, 0.12); font-size: 12px; font-weight: 800; }
.field { display: grid; gap: 4px; }
.field label { font-size: 12px; color: #909399; }
.field input, .field textarea, .field select { padding: 6px 8px; border: 1px solid var(--border, #e4e7ed); border-radius: 6px; font-family: inherit; }
.config-row.format-row { grid-template-columns: 220px minmax(0, 1fr); }
.format-content { display: grid; gap: 10px; }
.add-format-inline { display: flex; gap: 8px; align-items: center; }
.add-format-inline input { flex: 1; min-width: 0; padding: 6px 8px; border: 1px solid var(--border, #e4e7ed); border-radius: 6px; }
.add-format-inline :deep(.el-button) { flex-shrink: 0; }
.detail-empty { color: #909399; padding: 16px; text-align: center; }
.link { background: none; border: none; color: var(--primary, #1f6f78); cursor: pointer; }
.mono { font-family: ui-monospace, SFMono-Regular, Menlo, monospace; font-size: 12px; }
.timeline { list-style: none; margin: 0; padding: 0; display: grid; gap: 10px; }
.timeline li { padding: 8px 0; border-bottom: 1px solid var(--border, #e4e7ed); }
.timeline li span { font-weight: 700; display: block; }
.section-title { font-size: 15px; font-weight: 700; margin: 0 0 10px; }
@media (max-width: 1120px) { .settings-layout { grid-template-columns: 1fr; } }
</style>
