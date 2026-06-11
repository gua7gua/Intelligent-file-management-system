<template>
  <el-form ref="formRef" :model="modelValue" :rules="computedRules" v-bind="$attrs">
    <el-row :gutter="20">
      <template v-for="field in fields" :key="field.prop">
        <el-col :span="field.span ?? 24">
          <el-form-item :label="field.label" :prop="field.prop">
            <slot :name="`field-${field.prop}`" :model="modelValue">
              <el-input
                v-if="field.type === 'input'"
                :model-value="modelValue[field.prop]"
                :placeholder="field.placeholder || `请输入${field.label}`"
                :disabled="field.disabled"
                @update:model-value="(val: string) => updateField(field.prop, val)"
              />
              <el-input
                v-else-if="field.type === 'textarea'"
                type="textarea"
                :rows="3"
                :model-value="modelValue[field.prop]"
                :placeholder="field.placeholder || `请输入${field.label}`"
                :disabled="field.disabled"
                @update:model-value="(val: string) => updateField(field.prop, val)"
              />
              <el-input-number
                v-else-if="field.type === 'number'"
                :model-value="modelValue[field.prop]"
                :placeholder="field.placeholder || `请输入${field.label}`"
                :disabled="field.disabled"
                style="width: 100%"
                @update:model-value="(val: number) => updateField(field.prop, val)"
              />
              <el-select
                v-else-if="field.type === 'select'"
                :model-value="modelValue[field.prop]"
                :placeholder="field.placeholder || `请选择${field.label}`"
                :disabled="field.disabled"
                style="width: 100%"
                @update:model-value="(val: any) => updateField(field.prop, val)"
              >
                <el-option
                  v-for="opt in getOptions(field)"
                  :key="opt.value"
                  :label="opt.label"
                  :value="opt.value"
                />
              </el-select>
              <el-date-picker
                v-else-if="field.type === 'date'"
                :model-value="modelValue[field.prop]"
                :placeholder="field.placeholder || `请选择${field.label}`"
                :disabled="field.disabled"
                value-format="YYYY-MM-DD"
                style="width: 100%"
                @update:model-value="(val: string) => updateField(field.prop, val)"
              />
              <el-date-picker
                v-else-if="field.type === 'dateRange'"
                :model-value="modelValue[field.prop]"
                type="daterange"
                range-separator="至"
                start-placeholder="开始日期"
                end-placeholder="结束日期"
                value-format="YYYY-MM-DD"
                :disabled="field.disabled"
                style="width: 100%"
                @update:model-value="(val: string[]) => updateField(field.prop, val)"
              />
              <el-switch
                v-else-if="field.type === 'switch'"
                :model-value="modelValue[field.prop]"
                :disabled="field.disabled"
                @update:model-value="(val: boolean) => updateField(field.prop, val)"
              />
              <el-input
                v-else
                :model-value="modelValue[field.prop]"
                :placeholder="field.placeholder || `请输入${field.label}`"
                :disabled="field.disabled"
                @update:model-value="(val: string) => updateField(field.prop, val)"
              />
            </slot>
          </el-form-item>
        </el-col>
      </template>
    </el-row>
  </el-form>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import type { FormInstance, FormItemRule } from 'element-plus'
import { useAppStore } from '@/stores/app'
import type { ProFormField } from '@/types/components'

const props = defineProps<{
  fields: ProFormField[]
  modelValue: Record<string, any>
  rules?: Record<string, FormItemRule[]>
}>()

const emit = defineEmits<{
  'update:modelValue': [value: Record<string, any>]
}>()

const appStore = useAppStore()
const formRef = ref<FormInstance>()

const computedRules = computed(() => {
  const result: Record<string, FormItemRule[]> = {}
  for (const field of props.fields) {
    const rules: FormItemRule[] = []
    if (field.required) {
      rules.push({ required: true, message: `${field.label}不能为空`, trigger: 'blur' })
    }
    if (field.rules) {
      rules.push(...field.rules)
    }
    if (rules.length > 0) {
      result[field.prop] = rules
    }
  }
  if (props.rules) {
    Object.assign(result, props.rules)
  }
  return result
})

function getOptions(field: ProFormField) {
  if (field.dict) {
    return appStore.getDictOptions(field.dict)
  }
  return []
}

function updateField(prop: string, value: any) {
  const newModel = { ...props.modelValue, [prop]: value }
  emit('update:modelValue', newModel)
}

async function validate(): Promise<boolean> {
  if (!formRef.value) return false
  try {
    await formRef.value.validate()
    return true
  } catch {
    return false
  }
}

function resetFields() {
  formRef.value?.resetFields()
}

function clearValidate(props?: string | string[]) {
  formRef.value?.clearValidate(props)
}

defineExpose({ validate, resetFields, clearValidate, formRef })
</script>
