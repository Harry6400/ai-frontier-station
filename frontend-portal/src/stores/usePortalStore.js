import { defineStore } from 'pinia'

export const usePortalStore = defineStore('portal', {
  state: () => ({
    projectName: 'AI前沿情报站',
    positioning: 'AI 内容精选与发布系统第一版',
    contentTypes: ['资讯', '论文', '项目', '公司动态', '技术实践']
  })
})

