<template>
  <div class="pro-table">
    <!-- 搜索区 -->
    <div v-if="searchFields && searchFields.length > 0" class="pro-table-search">
      <el-form :model="searchForm" inline>
        <template v-for="field in searchFields" :key="field.prop">
          <el-form-item :label="field.label">
            <el-input
              v-if="field.type === 'input'"
              v-model="searchForm[field.prop]"
              :placeholder="`请输入${field.label}`"
              clearable
              style="width: 200px"
            />
            <el-select
              v-else-if="field.type === 'select'"
              v-model="searchForm[field.prop]"
              :placeholder="`请选择${field.label}`"
              clearable
              style="width: 200px"
            >
              <el-option
                v-for="opt in appStore.getDictOptions(field.dict ?? '')"
                :key="opt.value"
                :label="opt.label"
                :value="opt.value"
              />
            </el-select>
            <el-date-picker
              v-else-if="field.type === 'dateRange'"
              v-model="searchForm[field.prop]"
              type="daterange"
              range-separator="至"
              start-placeholder="开始日期"
              end-placeholder="结束日期"
              value-format="YYYY-MM-DD"
              style="width: 260px"
            />
            <el-date-picker
              v-else-if="field.type === 'date'"
              v-model="searchForm[field.prop]"
              :placeholder="`请选择${field.label}`"
              value-format="YYYY-MM-DD"
              style="width: 200px"
            />
            <el-input-number
              v-else-if="field.type === 'number'"
              v-model="searchForm[field.prop]"
              :placeholder="`请输入${field.label}`"
              style="width: 200px"
            />
          </el-form-item>
        </template>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="handleReset">
            <el-icon><Refresh /></el-icon>重置
          </el-button>
        </el-form-item>
      </el-form>
    </div>

    <!-- 工具栏 -->
    <div v-if="$slots.toolbar" class="pro-table-toolbar">
      <slot name="toolbar" />
    </div>

    <!-- 表格 -->
    <el-table v-loading="loading" :data="tableData" border stripe>
      <template v-for="col in columns" :key="col.prop">
        <el-table-column
          :prop="col.prop"
          :label="col.label"
          :width="col.width"
          :min-width="col.minWidth"
          :fixed="col.fixed"
          :sortable="col.sortable"
        >
          <template #default="{ row }">
            <slot v-if="col.slot" :name="col.slot" :row="row" />
            <DictTag
              v-else-if="col.dict"
              :dict="col.dict"
              :value="row[col.prop]"
              :source-type="col.sourceType"
            />
            <span v-else-if="col.formatter">{{ col.formatter(row) }}</span>
            <span v-else>{{ row[col.prop] }}</span>
          </template>
        </el-table-column>
      </template>
      <el-table-column v-if="$slots.action" label="操作" fixed="right" :width="actionWidth">
        <template #default="{ row }">
          <slot name="action" :row="row" />
        </template>
      </el-table-column>
    </el-table>

    <!-- 分页 -->
    <div class="pro-table-pagination">
      <el-pagination
        v-model:current-page="pageNo"
        v-model:page-size="pageSize"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { Search, Refresh } from '@element-plus/icons-vue'
import { useAppStore } from '@/stores/app'
import DictTag from '@/components/DictTag/index.vue'
import type { ProTableColumn, SearchField } from '@/types/components'
import type { PageData } from '@/types/api'

const props = withDefaults(
  defineProps<{
    columns: ProTableColumn[]
    searchFields?: SearchField[]
    fetchData: (pageNo: number, pageSize: number, params: Record<string, any>) => Promise<PageData>
    actionWidth?: number | string
  }>(),
  {
    actionWidth: 200,
  },
)

const appStore = useAppStore()
const loading = ref(false)
const tableData = ref<any[]>([])
const pageNo = ref(1)
const pageSize = ref(20)
const total = ref(0)

const searchForm = reactive<Record<string, any>>({})

function initSearchForm() {
  if (props.searchFields) {
    for (const field of props.searchFields) {
      searchForm[field.prop] = field.defaultValue ?? undefined
    }
  }
}

function buildParams(): Record<string, any> {
  const params: Record<string, any> = {}
  for (const [key, value] of Object.entries(searchForm)) {
    if (value !== undefined && value !== null && value !== '') {
      if (Array.isArray(value) && value.length === 2) {
        params[`${key}Start`] = value[0]
        params[`${key}End`] = value[1]
      } else {
        params[key] = value
      }
    }
  }
  return params
}

async function loadData() {
  loading.value = true
  try {
    const params = buildParams()
    const result = await props.fetchData(pageNo.value, pageSize.value, params)
    tableData.value = result.records
    total.value = result.total
  } catch {
    tableData.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  pageNo.value = 1
  loadData()
}

function handleReset() {
  initSearchForm()
  pageNo.value = 1
  loadData()
}

function refresh() {
  loadData()
}

initSearchForm()

onMounted(() => {
  loadData()
})

defineExpose({ refresh, loadData })
</script>
