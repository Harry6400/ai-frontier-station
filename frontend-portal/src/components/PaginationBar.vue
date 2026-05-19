<script setup>
import { computed } from 'vue'

const props = defineProps({
  total: { type: Number, required: true },
  pageSize: { type: Number, default: 20 },
  currentPage: { type: Number, default: 1 }
})

const emit = defineEmits(['page-change'])

const totalPages = computed(() => Math.ceil(props.total / props.pageSize))

const visiblePages = computed(() => {
  const pages = []
  const tp = totalPages.value
  const cp = props.currentPage
  if (tp <= 7) {
    for (let i = 1; i <= tp; i++) pages.push(i)
  } else {
    pages.push(1)
    if (cp > 3) pages.push('...')
    const start = Math.max(2, cp - 1)
    const end = Math.min(tp - 1, cp + 1)
    for (let i = start; i <= end; i++) pages.push(i)
    if (cp < tp - 2) pages.push('...')
    pages.push(tp)
  }
  return pages
})

function go(p) {
  if (p < 1 || p > totalPages.value || p === props.currentPage) return
  emit('page-change', p)
}
</script>

<template>
  <div v-if="totalPages > 1" class="pagination-bar">
    <button class="pg-btn" :disabled="currentPage <= 1" @click="go(currentPage - 1)">
      ‹ 上一页
    </button>
    <template v-for="(p, i) in visiblePages" :key="i">
      <span v-if="p === '...'" class="pg-ellipsis">…</span>
      <button
        v-else
        :class="['pg-btn pg-num', { active: p === currentPage }]"
        @click="go(p)"
      >{{ p }}</button>
    </template>
    <button class="pg-btn" :disabled="currentPage >= totalPages" @click="go(currentPage + 1)">
      下一页 ›
    </button>
    <span class="pg-info">共 {{ total }} 条</span>
  </div>
</template>

<style scoped>
.pagination-bar {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  margin-top: 28px;
  padding: 16px 0;
}

.pg-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 36px;
  padding: 0 12px;
  border: 1px solid var(--line, #e2e8f0);
  border-radius: 8px;
  background: var(--paper, #fff);
  color: var(--text-secondary, #555);
  font-size: 13px;
  font-weight: 500;
  font-family: inherit;
  cursor: pointer;
  transition: all 0.15s;
  white-space: nowrap;
}

.pg-btn:hover:not(:disabled):not(.active) {
  border-color: var(--accent, #6366f1);
  color: var(--accent, #6366f1);
  background: var(--accent-soft, rgba(99,102,241,0.08));
}

.pg-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.pg-num.active {
  background: var(--accent, #6366f1);
  color: #fff;
  border-color: var(--accent, #6366f1);
  font-weight: 700;
}

.pg-ellipsis {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 36px;
  color: var(--text-tertiary, #999);
  font-size: 14px;
  user-select: none;
}

.pg-info {
  margin-left: 10px;
  font-size: 12px;
  color: var(--text-tertiary, #999);
}
</style>
