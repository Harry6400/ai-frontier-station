<script setup>
import { computed } from 'vue'
import { formatDate, getContentTypeMeta } from '../utils/content'

const props = defineProps({
  item: {
    type: Object,
    required: true
  },
  variant: {
    type: String,
    default: 'default'
  }
})

const typeMeta = computed(() => getContentTypeMeta(props.item.contentType))
const displayTags = computed(() => (props.item.tags || []).slice(0, 3))
</script>

<template>
  <article class="content-card" :class="`content-card--${variant}`">
    <div class="content-card-top">
      <span class="content-chip">{{ typeMeta.label }}</span>
      <span class="content-date">{{ formatDate(props.item.publishedAt) }}</span>
    </div>

    <div class="content-card-body">
      <div class="content-card-copy">
        <p class="content-eyebrow">{{ typeMeta.english }}</p>
        <h3 class="content-title">{{ props.item.title }}</h3>
        <p class="content-summary">{{ props.item.summary }}</p>
      </div>

      <div class="content-card-meta">
        <span>{{ props.item.categoryName }}</span>
        <span>{{ props.item.sourceName || '人工录入' }}</span>
      </div>
    </div>

    <div class="content-card-footer">
      <div class="tag-cluster" v-if="displayTags.length">
        <span v-for="tag in displayTags" :key="tag.id" class="soft-pill">
          {{ tag.name }}
        </span>
      </div>

      <RouterLink class="card-link" :to="`/contents/${props.item.id}`">
        阅读全文
      </RouterLink>
    </div>
  </article>
</template>
