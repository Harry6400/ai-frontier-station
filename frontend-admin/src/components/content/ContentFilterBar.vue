<script setup>
const props = defineProps({
  activeType: { type: String, default: '' },
  activeSubCategory: { type: String, default: '' },
  keyword: { type: String, default: '' },
  contentTypes: { type: Array, default: () => [] }
})

const emit = defineEmits(['type-change', 'sub-category-change', 'search', 'clear', 'update:keyword'])

const subCategoryOptions = [
  { value: '', label: '全部论文' },
  { value: '3d_ct_denoising', label: '3D CT 去噪' },
  { value: 'medical_imaging', label: '医学影像' },
  { value: 'large_model', label: '大模型' }
]

function onTypeChange(value) {
  emit('type-change', value)
}

function onSubCategoryChange(value) {
  emit('sub-category-change', value)
}

function onSearch() {
  emit('search')
}

function onClear() {
  emit('clear')
}

function onKeywordInput(value) {
  emit('update:keyword', value)
}
</script>

<template>
  <div>
    <div class="toolbar-row toolbar-row--wrap">
      <el-input
        :model-value="keyword"
        placeholder="搜索标题"
        class="toolbar-input"
        clearable
        @update:model-value="onKeywordInput"
        @keyup.enter="onSearch"
      />
      <el-button type="primary" @click="onSearch">查询</el-button>
      <el-button @click="onClear">清空筛选</el-button>
    </div>
    <div class="type-tab-strip">
      <button
        v-for="item in contentTypes"
        :key="item.value"
        :class="['type-tab', { active: activeType === item.value }]"
        @click="onTypeChange(item.value)"
      >{{ item.label }}</button>
    </div>
    <div v-if="activeType === 'paper'" class="sub-cat-strip">
      <button
        v-for="cat in subCategoryOptions"
        :key="cat.value"
        :class="['sub-cat', { active: activeSubCategory === cat.value }]"
        @click="onSubCategoryChange(cat.value)"
      >{{ cat.label }}</button>
    </div>
  </div>
</template>

<style scoped>
.type-tab-strip {
  display: flex; gap: 2px; background: var(--admin-surface-muted, #f5f5f5);
  border-radius: 10px; padding: 3px; margin-bottom: 12px;
}
.type-tab {
  padding: 7px 16px; border-radius: 8px; border: 0; background: transparent;
  font-size: 13px; font-weight: 600; color: var(--admin-text-secondary, #666);
  cursor: pointer; transition: all 0.15s; white-space: nowrap;
}
.type-tab:hover { color: var(--admin-text, #333); }
.type-tab.active {
  background: var(--admin-surface-elevated, #fff);
  color: var(--admin-text, #333);
  box-shadow: 0 1px 3px rgba(0,0,0,0.08);
}

.sub-cat-strip {
  display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 12px;
}
.sub-cat {
  padding: 5px 12px; border-radius: 6px;
  border: 1px solid var(--admin-line-soft, #e0e0e0);
  background: transparent; font-size: 12px; font-weight: 600;
  color: var(--admin-text-secondary, #666); cursor: pointer; transition: all 0.12s;
}
.sub-cat:hover { border-color: var(--admin-brand, #2563EB); color: var(--admin-brand, #2563EB); }
.sub-cat.active {
  background: var(--admin-brand, #2563EB); color: #fff;
  border-color: var(--admin-brand, #2563EB);
}
</style>
