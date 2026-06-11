import { useAppStore } from '@/stores/app'

/** 字典 composable：提供字典选项和标签翻译 */
export function useDict() {
  const appStore = useAppStore()

  function getDictOptions(dictKey: string) {
    return appStore.getDictOptions(dictKey)
  }

  function getDictLabel(dictKey: string, value: string | number): string {
    return appStore.getDictLabel(dictKey, value)
  }

  return { getDictOptions, getDictLabel }
}
