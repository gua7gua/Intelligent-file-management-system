<template>
  <el-dialog
    :model-value="modelValue"
    title="确认销毁（不可逆）"
    width="520px"
    @update:model-value="(v: boolean) => emit('update:modelValue', v)"
  >
    <div class="notice danger">
      确认销毁后档案状态将变为「已销毁」且不可恢复。销毁清册、审批记录、销毁确认与操作日志永久保留。
    </div>

    <div class="field">
      <label>销毁方式</label>
      <select v-model="form.destroyMethod">
        <option value="">请选择</option>
        <option v-for="(label, val) in DestroyMethodLabel" :key="val" :value="val">{{ label }}</option>
      </select>
    </div>
    <div class="split">
      <div class="field">
        <label>监销人 1</label>
        <input v-model="form.supervisorName1" />
      </div>
      <div class="field">
        <label>监销人 2</label>
        <input v-model="form.supervisorName2" />
      </div>
    </div>
    <div class="field">
      <label>销毁说明</label>
      <textarea v-model="form.destroyNote" rows="3" placeholder="说明销毁执行情况"></textarea>
    </div>
    <div class="field">
      <label>现场照片（至少 1 张，jpg/png，≤10MB）</label>
      <input type="file" accept="image/jpeg,image/png" multiple @change="handleFileChange" />
      <div v-if="photos.length > 0" class="photo-list">
        <span v-for="p in photos" :key="p.id" class="photo-chip">{{ p.originalFilename }}</span>
      </div>
    </div>
    <div class="field check">
      <label><input type="checkbox" v-model="form.irrevocableConfirm" /> 我已确认销毁后档案状态不可恢复</label>
    </div>

    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="danger" :loading="submitting" :disabled="uploading" @click="handleConfirm">最终确认销毁</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { DestructionPhoto } from '@/types/destruction'
import { DestroyMethodLabel } from '@/types/enums'
import type { DestroyMethodValue } from '@/types/enums'
import { uploadDestructionPhotos, confirmDestruction } from '@/api/destruction'
import { validateDestroyConfirm } from '@/utils/destructionValidation'

const props = defineProps<{ modelValue: boolean; listId: number }>()
const emit = defineEmits<{ (e: 'update:modelValue', v: boolean): void; (e: 'success'): void }>()

const form = reactive({
  destroyMethod: '' as DestroyMethodValue | '',
  supervisorName1: '',
  supervisorName2: '',
  destroyNote: '',
  irrevocableConfirm: false,
})
const photos = ref<DestructionPhoto[]>([])
const uploading = ref(false)
const submitting = ref(false)

watch(
  () => props.modelValue,
  (v) => {
    if (v) {
      form.destroyMethod = ''
      form.supervisorName1 = ''
      form.supervisorName2 = ''
      form.destroyNote = ''
      form.irrevocableConfirm = false
      photos.value = []
    }
  },
)

async function handleFileChange(e: Event) {
  const input = e.target as HTMLInputElement
  if (!input.files || input.files.length === 0) return
  const files = Array.from(input.files)
  const invalid = files.find((f) => !['image/jpeg', 'image/png'].includes(f.type) || f.size > 10 * 1024 * 1024)
  if (invalid) {
    ElMessage.warning(`${invalid.name} 不是 jpg/png 或超过 10MB。`)
    input.value = ''
    return
  }
  uploading.value = true
  try {
    const uploaded = await uploadDestructionPhotos(props.listId, files)
    photos.value.push(...uploaded)
    ElMessage.success(`已上传 ${uploaded.length} 张现场照片。`)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '照片上传失败')
  } finally {
    uploading.value = false
    input.value = ''
  }
}

async function handleConfirm() {
  const errors = validateDestroyConfirm({
    destroyMethod: form.destroyMethod,
    supervisorName1: form.supervisorName1,
    supervisorName2: form.supervisorName2,
    destroyNote: form.destroyNote,
    irrevocableConfirm: form.irrevocableConfirm,
    photoCount: photos.value.length,
  })
  if (errors.length > 0) {
    ElMessage.warning(errors[0])
    return
  }
  submitting.value = true
  try {
    await confirmDestruction(props.listId, {
      destroyMethod: form.destroyMethod as DestroyMethodValue,
      supervisorName1: form.supervisorName1.trim(),
      supervisorName2: form.supervisorName2.trim(),
      destroyNote: form.destroyNote.trim(),
      photoIds: photos.value.map((p) => p.id),
    })
    ElMessage.success('档案状态已更新为已销毁，证据链永久保留。')
    emit('success')
    emit('update:modelValue', false)
  } catch (err: unknown) {
    ElMessage.error(err instanceof Error ? err.message : '确认销毁失败')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.notice.danger { padding: 8px 10px; border-radius: var(--radius-sm); background: #fff1f0; color: #a8071a; font-size: 12px; margin-bottom: 12px; }
.field { margin-bottom: 10px; }
.field label { display: block; font-size: 13px; font-weight: 700; color: var(--muted); margin-bottom: 4px; }
.field input, .field select, .field textarea { width: 100%; min-height: 34px; padding: 6px 8px; border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 14px; }
.field.check label { display: flex; align-items: center; gap: 6px; }
.field.check input { width: auto; min-height: auto; }
.split { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.photo-list { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 6px; }
.photo-chip { padding: 2px 8px; background: var(--bg); border-radius: var(--radius-sm); font-size: 12px; }
</style>
