<template>
  <div class="state-loading">
    <div class="spinner"></div>
    <p>{{ message }}</p>
  </div>
</template>
<script setup>
defineProps({ message: { type: String, default: '加载中...' } })
</script>
<style scoped>
.state-loading { text-align: center; padding: 60px 20px; }
.spinner { width: 32px; height: 32px; border: 3px solid var(--line, #e5e7eb); border-top-color: var(--accent, #3b82f6); border-radius: 50%; animation: spin 0.6s linear infinite; margin: 0 auto 16px; }
@keyframes spin { to { transform: rotate(360deg); } }
.state-loading p { color: var(--text-2, #666); font-size: 14px; }
</style>