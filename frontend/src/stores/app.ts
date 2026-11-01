import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useAppStore = defineStore('app', () => {
  const sidebarCollapsed = ref<boolean>(false)
  const currentOrgName = ref<string>('阳光馨园康养中心')

  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value
  }

  function setOrgName(name: string) {
    currentOrgName.value = name
  }

  return {
    sidebarCollapsed,
    currentOrgName,
    toggleSidebar,
    setOrgName
  }
})
