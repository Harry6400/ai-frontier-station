<script setup>
import PortalTopbar from '../components/PortalTopbar.vue'

const props = defineProps({
  title: { type: String, default: '' },
  subtitle: { type: String, default: '' },
  emoji: { type: String, default: '' },
  description: { type: String, default: '' }
})
</script>

<template>
  <div class="portal-shell">
    <PortalTopbar :context-label="title" :context-value="subtitle" />

    <main class="page-stack">
      <section class="section-shell section-placeholder">
        <div class="section-placeholder-icon">{{ emoji }}</div>
        <h1>{{ title }}</h1>
        <p class="section-placeholder-subtitle">{{ subtitle }}</p>
        <p class="section-placeholder-desc">{{ description }}</p>
        <div class="section-placeholder-coming">
          <span>页面设计中，敬请期待</span>
        </div>
      </section>
    </main>
  </div>
</template>

<style scoped>
.section-placeholder {
  text-align: center;
  padding: 80px 30px;
  max-width: 600px;
  margin: 40px auto;
}

.section-placeholder-icon {
  font-size: 4rem;
  margin-bottom: 20px;
}

.section-placeholder h1 {
  font-size: 2rem;
  margin: 0 0 8px;
  letter-spacing: -0.04em;
}

.section-placeholder-subtitle {
  color: var(--text-secondary);
  font-size: 1.1rem;
  margin: 0 0 20px;
}

.section-placeholder-desc {
  color: var(--text-secondary);
  line-height: 1.75;
  margin: 0 0 32px;
}

.section-placeholder-coming {
  display: inline-flex;
  padding: 10px 24px;
  border-radius: 999px;
  border: 1px solid var(--line);
  background: var(--surface);
  color: var(--text-secondary);
  font-size: 0.9rem;
}
</style>
