<script setup>
import ContentFilterBar from './ContentFilterBar.vue'

defineProps({
  contentList: { type: Array, default: () => [] },
  loading: { type: Boolean, default: false },
  total: { type: Number, default: 0 },
  pageNum: { type: Number, default: 1 },
  pageSize: { type: Number, default: 10 },
  activeType: { type: String, default: '' },
  activeSubCategory: { type: String, default: '' },
  keyword: { type: String, default: '' },
  contentTypes: { type: Array, default: () => [] }
})

const emit = defineEmits([
  'page-change',
  'type-change',
  'sub-category-change',
  'edit',
  'delete',
  'search',
  'clear',
  'update:keyword'
])

function formatDateTime(value) {
  if (!value) {
    return '保存后自动生成'
  }
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return value
  }
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    hour12: false
  }).format(date)
}

function onPageChange(page) {
  emit('page-change', page)
}

function onTypeChange(type) {
  emit('type-change', type)
}

function onSubCategoryChange(subCat) {
  emit('sub-category-change', subCat)
}

function onSearch() {
  emit('search')
}

function onClear() {
  emit('clear')
}

function onEdit(item) {
  emit('edit', item)
}

function onDelete(id) {
  emit('delete', id)
}

function onKeywordUpdate(value) {
  emit('update:keyword', value)
}
</script>

<template>
  <div class="card-panel" v-loading="loading">
    <ContentFilterBar
      :active-type="activeType"
      :active-sub-category="activeSubCategory"
      :keyword="keyword"
      :content-types="contentTypes"
      @type-change="onTypeChange"
      @sub-category-change="onSubCategoryChange"
      @search="onSearch"
      @clear="onClear"
      @update:keyword="onKeywordUpdate"
    />

    <div class="content-list">
      <div v-for="item in contentList" :key="item.id" class="content-row" @click="onEdit(item)">
        <div class="row-title">{{ item.title }}</div>
        <div class="row-cat">{{ item.categoryName || '未分类' }}</div>
        <div class="row-date">{{ formatDateTime(item.publishedAt) }}</div>
        <div class="row-ops">
          <el-button link type="danger" size="small" @click.stop="onDelete(item.id)">删除</el-button>
        </div>
      </div>
    </div>

    <div class="pagination-row">
      <el-pagination
        background
        layout="prev, pager, next, total"
        :current-page="pageNum"
        :page-size="pageSize"
        :total="total"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>
