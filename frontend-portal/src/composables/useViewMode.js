import { ref, watch } from 'vue'

/**
 * 视图模式切换（列表/卡片），带localStorage持久化
 */
export function useViewMode(storageKey, defaultMode = 'list') {
  const saved = typeof window !== 'undefined' ? localStorage.getItem(storageKey) : null
  const viewMode = ref(saved || defaultMode)

  watch(viewMode, (v) => {
    localStorage.setItem(storageKey, v)
  })

  function setViewMode(mode) {
    viewMode.value = mode
  }

  return { viewMode, setViewMode }
}

/**
 * Tab切换
 */
export function useTabs(defaultTab = '') {
  const activeTab = ref(defaultTab)

  function setTab(tab) {
    activeTab.value = tab
  }

  return { activeTab, setTab }
}
