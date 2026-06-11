<template>
  <el-tag :type="tagType" size="small">{{ label }}</el-tag>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useAppStore } from '@/stores/app'
import {
  BatchStatusLabel,
  ItemStatusLabel,
  ArchiveStatusLabel,
  BorrowStatusLabel,
  CarrierStatusLabel,
  RetentionPeriodLabel,
  SecurityLevelLabel,
  OpenStatusLabel,
} from '@/types/enums'

const props = defineProps<{
  dict: string
  value: string | number
  sourceType?: string
}>()

const inlineDicts: Record<string, Record<string, Record<string, string>>> = {
  batchStatus: BatchStatusLabel,
}

const staticDicts: Record<string, Record<string, string>> = {
  itemStatus: ItemStatusLabel,
  archiveLifecycleStatus: ArchiveStatusLabel,
  borrowStatus: BorrowStatusLabel,
  carrierStatus: CarrierStatusLabel,
  retentionPeriod: RetentionPeriodLabel,
  openStatus: OpenStatusLabel,
}

const tagTypeMap: Record<string, Record<string, string>> = {
  borrowStatus: {
    applied: 'warning',
    rejected: 'danger',
    approved: 'success',
    checked_out: '',
    returned: 'info',
    abnormal_return: 'danger',
  },
  archiveLifecycleStatus: {
    pending_shelf: 'warning',
    normal: 'success',
    pending_destruction: 'danger',
    destroyed: 'info',
  },
}

const appStore = useAppStore()

const label = computed(() => {
  if (props.sourceType && inlineDicts[props.dict]) {
    const sourceMap = inlineDicts[props.dict][props.sourceType]
    if (sourceMap && sourceMap[String(props.value)]) {
      return sourceMap[String(props.value)]
    }
  }
  if (inlineDicts[props.dict]) {
    for (const sourceMap of Object.values(inlineDicts[props.dict])) {
      if (sourceMap[String(props.value)]) {
        return sourceMap[String(props.value)]
      }
    }
  }
  if (staticDicts[props.dict]?.[String(props.value)]) {
    return staticDicts[props.dict][String(props.value)]
  }
  if (props.dict === 'securityLevel') {
    return SecurityLevelLabel[Number(props.value)] ?? String(props.value)
  }
  return appStore.getDictLabel(props.dict, props.value)
})

const tagType = computed(() => {
  const map = tagTypeMap[props.dict]
  if (map) return (map[String(props.value)] || '') as '' | 'success' | 'warning' | 'danger' | 'info'
  return ''
})
</script>
