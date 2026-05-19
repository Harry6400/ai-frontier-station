<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { Plus } from '@element-plus/icons-vue'
import { createSource, getSources, removeSource, updateSource } from '../api/admin'

const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const sources = ref([])
const formRef = ref()
const form = reactive({
  name: '',
  slug: '',
  sourceType: 'manual',
  websiteUrl: '',
  description: '',
  isEnabled: 1
})

const sourceTypeOptions = [
  { value: 'github', label: 'GitHub', icon: '🔧' },
  { value: 'paper', label: '论文', icon: '📄' },
  { value: 'official_blog', label: '官方博客', icon: '📢' },
  { value: 'leaderboard', label: '榜单', icon: '📊' },
  { value: 'social', label: '社交平台', icon: '💬' },
  { value: 'community', label: '社区', icon: '🌐' },
  { value: 'manual', label: '人工录入', icon: '✏️' }
]

const rules = {
  name: [{ required: true, message: '请输入来源名称', trigger: 'blur' }],
  sourceType: [{ required: true, message: '请选择来源类型', trigger: 'change' }]
}

const sourceTypeMetaMap = computed(() =>
  Object.fromEntries(sourceTypeOptions.map((item) => [item.value, item]))
)

const groupedSources = computed(() => {
  const groups = {}
  sources.value.forEach(source => {
    const type = source.sourceType || 'manual'
    if (!groups[type]) {
      const meta = sourceTypeMetaMap.value[type] || { label: type }
      groups[type] = { label: meta.label, sources: [] }
    }
    groups[type].sources.push(source)
  })
  return Object.entries(groups).map(([type, group]) => ({
    type,
    label: group.label,
    count: group.sources.length,
    sources: group.sources
  }))
})

function resetForm() {
  editingId.value = null
  form.name = ''
  form.slug = ''
  form.sourceType = 'manual'
  form.websiteUrl = ''
  form.description = ''
  form.isEnabled = 1
}

async function loadSources() {
  loading.value = true
  try {
    const res = await getSources()
    sources.value = res.data
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

function openCreate() {
  resetForm()
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.name = row.name
  form.slug = row.slug
  form.sourceType = row.sourceType
  form.websiteUrl = row.websiteUrl || ''
  form.description = row.description || ''
  form.isEnabled = row.isEnabled
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  try {
    const payload = { ...form }
    if (editingId.value) {
      await updateSource(editingId.value, payload)
      ElMessage.success('来源更新成功')
    } else {
      await createSource(payload)
      ElMessage.success('来源创建成功')
    }
    dialogVisible.value = false
    resetForm()
    loadSources()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('删除后将无法恢复，确认继续吗？', '删除来源', { type: 'warning' })
    await removeSource(id)
    ElMessage.success('来源删除成功')
    loadSources()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(loadSources)
</script>

<template>
  <section class="admin-page" v-loading="loading">
    <div class="page-header">
      <h3 class="page-title">来源管理</h3>
      <el-button type="primary" @click="openCreate">
        <el-icon><Plus /></el-icon>
        新建来源
      </el-button>
    </div>

    <div class="source-groups">
      <div v-for="group in groupedSources" :key="group.type" class="source-group">
        <div class="group-header">
          <span class="group-label">{{ group.label }}</span>
          <span class="group-count">{{ group.count }}</span>
        </div>
        <div class="group-list">
          <div v-for="source in group.sources" :key="source.id" class="source-row">
            <div class="source-info">
              <span class="source-name">{{ source.name }}</span>
              <span class="source-slug">{{ source.slug }}</span>
            </div>
            <div class="source-meta">
              <el-tag v-if="!source.isEnabled" type="info" size="small" effect="plain">停用</el-tag>
              <el-button link type="primary" size="small" @click="openEdit(source)">编辑</el-button>
              <el-button link type="danger" size="small" @click="handleDelete(source.id)">删除</el-button>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-if="!loading && groupedSources.length === 0" description="暂无来源数据" />
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑来源' : '新增来源'" width="620px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="来源名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：GitHub Trending" />
        </el-form-item>
        <el-form-item label="来源标识">
          <el-input v-model="form.slug" placeholder="可选，不填自动生成" />
        </el-form-item>
        <el-form-item label="来源类型" prop="sourceType">
          <el-select v-model="form.sourceType" placeholder="请选择来源类型">
            <el-option
              v-for="item in sourceTypeOptions"
              :key="item.value"
              :label="item.label"
              :value="item.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="官网地址">
          <el-input v-model="form.websiteUrl" placeholder="https://example.com" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.isEnabled">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="说明">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-title {
  margin: 0;
  font-size: 20px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.source-groups {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.source-group {
  background: var(--el-bg-color);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: 8px;
  overflow: hidden;
}

.group-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 20px;
  background: var(--el-fill-color-light);
  border-bottom: 1px solid var(--el-border-color-lighter);
}

.group-label {
  font-size: 15px;
  font-weight: 600;
  color: var(--el-text-color-primary);
}

.group-count {
  font-size: 12px;
  font-weight: 600;
  color: var(--el-color-primary);
  background: var(--el-color-primary-light-9);
  padding: 2px 8px;
  border-radius: 10px;
  line-height: 1.4;
}

.group-list {
  display: flex;
  flex-direction: column;
}

.source-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 20px;
  border-bottom: 1px solid var(--el-border-color-extra-light);
  transition: background-color 0.15s;
}

.source-row:last-child {
  border-bottom: none;
}

.source-row:hover {
  background: var(--el-fill-color-lighter);
}

.source-info {
  display: flex;
  align-items: center;
  gap: 12px;
  flex: 1;
  min-width: 0;
}

.source-name {
  font-size: 14px;
  font-weight: 500;
  color: var(--el-text-color-primary);
}

.source-slug {
  font-size: 13px;
  color: var(--el-text-color-secondary);
  font-family: monospace;
  background: var(--el-fill-color-light);
  padding: 2px 8px;
  border-radius: 4px;
}

.source-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-shrink: 0;
}
</style>
