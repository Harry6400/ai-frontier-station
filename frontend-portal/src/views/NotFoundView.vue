<template>
  <div class="not-found">
    <h1>404</h1>
    <p>页面不存在</p>
    <router-link to="/">返回首页</router-link>
  </div>
</template>
<style scoped>
.not-found { text-align: center; padding: 120px 20px; }
.not-found h1 { font-size: 72px; color: var(--accent, #3b82f6); margin-bottom: 16px; }
.not-found p { font-size: 18px; color: var(--text-2, #666); margin-bottom: 24px; }
.not-found a { color: var(--accent, #3b82f6); text-decoration: underline; }
</style>
