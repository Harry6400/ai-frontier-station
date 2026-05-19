<template>
  <div class="state-error">
    <p>{{ message }}</p>
    <button v-if="retryable" @click="$emit('retry')">重试</button>
  </div>
</template>
<script setup>
defineProps({
  message: { type: String, default: '加载失败' },
  retryable: { type: Boolean, default: true }
})
defineEmits(['retry'])
</script>
<style scoped>
.state-error { text-align: center; padding: 60px 20px; }
.state-error p { color: var(--text-2, #666); margin-bottom: 16px; }
.state-error button { padding: 8px 20px; border: 1px solid var(--accent, #3b82f6); background: transparent; color: var(--accent, #3b82f6); border-radius: 6px; cursor: pointer; }
.state-error button:hover { background: var(--accent, #3b82f6); color: #fff; }
</style>