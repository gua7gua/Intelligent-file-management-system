import { defineStore } from 'pinia'
import { ref } from 'vue'
import type { DictItem } from '@/types/components'
import { getDictionariesApi } from '@/api/dictionary'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref(false)
  const currentPortal = ref('admin')
  const dictionaries = ref<Record<string, DictItem[]>>({})
  const dictionariesLoaded = ref(false)

  /** 切换侧边栏折叠 */
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  /** 加载全局字典（登录后调用一次） */
  async function loadDictionaries() {
    if (dictionariesLoaded.value) return
    try {
      const data = await getDictionariesApi()
      dictionaries.value = data
      dictionariesLoaded.value = true
    } catch {
      // 字典加载失败不阻塞使用
    }
  }

  /** 根据字典 key 获取选项列表 */
  function getDictOptions(dictKey: string): DictItem[] {
    return dictionaries.value[dictKey] || []
  }

  /** 根据字典 key 和值获取中文标签 */
  function getDictLabel(dictKey: string, value: string | number): string {
    const items = dictionaries.value[dictKey]
    if (!items) return String(value)
    const found = items.find((item) => item.value === value)
    return found ? found.label : String(value)
  }

  return {
    sidebarCollapsed,
    currentPortal,
    dictionaries,
    toggleSidebar,
    loadDictionaries,
    getDictOptions,
    getDictLabel,
  }
})
