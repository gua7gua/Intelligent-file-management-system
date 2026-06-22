<template>
  <el-dialog
    :model-value="visible"
    :title="fileName || '文件预览'"
    width="80%"
    top="5vh"
    append-to-body
    @update:model-value="onToggle"
    @closed="revoke"
  >
    <div v-loading="loading" element-loading-text="正在加载文件…" style="min-height: 60vh">
      <div v-if="error" class="file-preview-error">
        <span>{{ error }}</span>
      </div>
      <img v-else-if="objectUrl && isImage" :src="objectUrl" :alt="fileName" class="file-preview-img" />
      <iframe v-else-if="objectUrl" :src="objectUrl" :title="fileName" class="file-preview-frame"></iframe>
    </div>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed, watch } from 'vue'

/**
 * 电子文件预览组件（三端复用）。
 * 调用方传入返回 Blob 的 fetcher（各端各自的 preview API），组件负责
 * fetch → Blob → objectURL → 按 MIME 用 img（图片）或 iframe（PDF 等）渲染。
 * 不依赖额外 PDF 库；PDF 用浏览器原生渲染（iframe）。
 */
const props = defineProps<{
  visible: boolean
  fileId: number | null
  fileName?: string
  /** 文件 MIME（blob.type 缺失时兜底判断图片/文档） */
  mime?: string
  /** 返回文件 Blob 的获取函数，传入各端 preview API（已配置 responseType:'blob'） */
  fetcher: (fileId: number) => Promise<Blob>
}>()
const emit = defineEmits<{ 'update:visible': [val: boolean] }>()

const loading = ref(false)
const error = ref('')
const objectUrl = ref('')
const actualType = ref('')

const isImage = computed(() => actualType.value.startsWith('image/'))

watch(
  () => [props.visible, props.fileId] as const,
  async ([vis, id]) => {
    if (vis && id != null) {
      await load(id)
    } else if (!vis) {
      revoke()
    }
  },
  { immediate: true },
)

async function load(id: number) {
  loading.value = true
  error.value = ''
  revoke()
  try {
    const blob = await props.fetcher(id)
    // 空 Blob（0 字节）视为无效，避免 iframe 显示空白
    if (!blob || blob.size === 0) {
      error.value = '文件内容为空，无法预览'
      return
    }
    actualType.value = blob.type || props.mime || ''
    objectUrl.value = URL.createObjectURL(blob)
  } catch (e) {
    error.value = e instanceof Error ? e.message : '预览失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

function revoke() {
  if (objectUrl.value) {
    URL.revokeObjectURL(objectUrl.value)
    objectUrl.value = ''
  }
}

function onToggle(val: boolean) {
  if (!val) revoke()
  emit('update:visible', val)
}
</script>

<style scoped>
.file-preview-error {
  color: var(--el-color-danger);
  padding: 32px;
  text-align: center;
}
.file-preview-img {
  max-width: 100%;
  display: block;
  margin: 0 auto;
}
.file-preview-frame {
  width: 100%;
  height: 72vh;
  border: 0;
}
</style>
