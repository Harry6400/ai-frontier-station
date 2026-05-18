import { defineStore } from 'pinia'

export const useAdminStore = defineStore('admin', {
  state: () => ({
    dashboardStats: [
      { label: '内容模块', value: '待接接口' },
      { label: '分类模块', value: '已规划' },
      { label: '标签模块', value: '已规划' },
      { label: '来源模块', value: '已规划' }
    ]
  })
})

