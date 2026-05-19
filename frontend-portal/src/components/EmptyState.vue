<template>
  <div class="state-empty">
    <p>{{ message }}</p>
  </div>
</template>
<script setup>
defineProps({ message: { type: String, default: '暂无数据' } })
</script>
<style scoped>
.state-empty { text-align: center; padding: 60px 20px; }
.state-empty p { color: var(--text-3, #999); font-size: 14px; }
</style>