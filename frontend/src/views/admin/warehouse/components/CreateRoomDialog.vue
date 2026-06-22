<template>
  <div v-if="modelValue" class="modal-backdrop" @click.self="close">
    <div class="modal">
      <header>
        <h2 class="section-title">{{ editingRoom ? '编辑库房' : '添加库房' }}</h2>
        <button class="button ghost" @click="close">关闭</button>
      </header>
      <div class="body">
        <div v-if="!editingRoom" class="notice">库房提交后生成完整架位结构，编码由库房号、机架号、层号、盒位号组成。</div>
        <div class="form-grid">
          <div class="field"><label>库房号</label><input v-model="form.roomNo" autocomplete="off" :disabled="!!editingRoom"></div>
          <div class="field"><label>库房名称</label><input v-model="form.roomName" autocomplete="off"></div>
          <template v-if="!editingRoom">
            <div class="field"><label>机架数量</label><input v-model.number="form.rackCount" type="number" min="1"></div>
            <div class="field"><label>单机架层数</label><input v-model.number="form.layersPerRack" type="number" min="1"></div>
            <div class="field"><label>每层最大盒数</label><input v-model.number="form.boxesPerLayer" type="number" min="1"></div>
          </template>
          <div class="field"><label>告警阈值</label>
            <select v-model.number="form.warningThreshold">
              <option :value="0.85">85%</option>
              <option :value="0.9">90%</option>
              <option :value="0.8">80%</option>
            </select>
          </div>
        </div>
        <p v-if="!editingRoom" class="hint">预计生成 {{ capacity }} 个盒位，示例编码：{{ form.roomNo || '库房号' }}-01-01-01。</p>
        <p v-else class="hint">编辑模式仅可修改库房名称与告警阈值；库房号与机架/层/盒位结构不可改。</p>
      </div>
      <footer>
        <button class="button ghost" @click="close">取消</button>
        <button class="button" @click="onSubmit">{{ editingRoom ? '保存' : '生成架位' }}</button>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { WarehouseRoom, WarehouseRoomCreateData } from '@/types/warehouse'

const props = defineProps<{ modelValue: boolean; editingRoom?: WarehouseRoom | null }>()
const emit = defineEmits<{
  (e: 'update:modelValue', v: boolean): void
  (e: 'create', data: WarehouseRoomCreateData): void
  (e: 'save', data: { roomName: string; warningThreshold: number }): void
}>()

const defaultForm = (): WarehouseRoomCreateData => ({ roomNo: '', roomName: '', rackCount: 4, layersPerRack: 4, boxesPerLayer: 8, warningThreshold: 0.85 })
const form = ref<WarehouseRoomCreateData>(defaultForm())
const capacity = computed(() => form.value.rackCount * form.value.layersPerRack * form.value.boxesPerLayer)

// 编辑模式：回填当前库房字段（库房号/结构在模板中只读或隐藏）；新建模式：重置默认
watch(() => props.editingRoom, (r) => {
  if (r) {
    form.value = { roomNo: r.roomNo, roomName: r.roomName, rackCount: r.rackCount, layersPerRack: r.layersPerRack, boxesPerLayer: r.boxesPerLayer, warningThreshold: r.warningThreshold }
  } else {
    form.value = defaultForm()
  }
}, { immediate: true })

function close() {
  emit('update:modelValue', false)
}

function onSubmit() {
  if (!form.value.roomName.trim()) {
    ElMessage.warning('请填写库房名称')
    return
  }
  // 编辑模式：仅提交名称与阈值（后端 updateRoom 支持这两个可选字段）
  if (props.editingRoom) {
    emit('save', { roomName: form.value.roomName, warningThreshold: form.value.warningThreshold })
    return
  }
  if (!form.value.roomNo.trim()) {
    ElMessage.warning('请填写库房号')
    return
  }
  if (capacity.value <= 0) {
    ElMessage.warning('容量参数必须大于 0')
    return
  }
  emit('create', { ...form.value })
  form.value = defaultForm()
}
</script>

<style scoped>
.modal-backdrop { position: fixed; inset: 0; z-index: 30; display: flex; align-items: center; justify-content: center; padding: 20px; background: rgba(23, 33, 43, 0.38); }
.modal { width: min(720px, 100%); max-height: calc(100vh - 40px); overflow-y: auto; border-radius: var(--radius); background: #fff; box-shadow: var(--shadow); }
.modal header, .modal footer { display: flex; justify-content: space-between; align-items: center; padding: 14px 18px; border-bottom: 1px solid var(--border); }
.modal footer { border-top: 1px solid var(--border); border-bottom: 0; }
.modal .body { padding: 18px; }
.section-title { font-size: 15px; font-weight: 700; margin: 0; }
.notice { padding: 9px 12px; border-radius: var(--radius-sm); background: var(--primary-soft); font-size: 12px; color: #1f6f78; margin-bottom: 14px; }
.form-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 12px; }
.field { display: grid; gap: 4px; }
.field label { font-size: 12px; color: var(--muted); font-weight: 700; }
.field input, .field select { padding: 6px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 13px; }
.field input:disabled { background: #f5f5f5; color: var(--muted); }
.hint { color: var(--muted); font-size: 12px; margin-top: 10px; }
.button { padding: 6px 14px; border: 1px solid var(--primary); border-radius: var(--radius-sm); background: var(--primary); color: #fff; cursor: pointer; font-size: 13px; }
.button.ghost { background: #fff; color: var(--text); border-color: var(--border); }
</style>
