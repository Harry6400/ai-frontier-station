<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
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
  {
    value: 'github',
    label: 'GitHub',
    description: '用于 GitHub Trending、仓库榜单、Star 趋势和 README 摘要。'
  },
  {
    value: 'paper',
    label: '论文站点',
    description: '用于 arXiv、论文摘要、作者、会议和项目主页。'
  },
  {
    value: 'official_blog',
    label: '官方博客',
    description: '用于 OpenAI、DeepSeek 等公司官方公告和产品动态。'
  },
  {
    value: 'community',
    label: '技术社区',
    description: '用于工程实践、RAG、Agent、评测等社区经验文章。'
  },
  {
    value: 'manual',
    label: '人工录入',
    description: '用于人工精选、临时记录和自定义来源内容。'
  }
]

const rules = {
  name: [{ required: true, message: '请输入来源名称', trigger: 'blur' }],
  sourceType: [{ required: true, message: '请选择来源类型', trigger: 'change' }]
}

const sourceTypeMetaMap = computed(() =>
  Object.fromEntries(sourceTypeOptions.map((item) => [item.value, item]))
)

const sourceTypeStats = computed(() =>
  sourceTypeOptions.map((item) => ({
    ...item,
    count: sources.value.filter((source) => source.sourceType === item.value).length
  }))
)

function getSourceTypeMeta(type) {
  return sourceTypeMetaMap.value[type] || {
    value: type,
    label: type || '未设置',
    description: '自定义来源类型，可继续维护为新的采集入口。'
  }
}

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
    <div class="page-intro">
      <span class="sidebar-kicker">Sources</span>
      <h3>来源管理</h3>
      <p>来源表已经接入后端接口，这样内容可以明确记录来自 GitHub、论文站点、官方博客或人工录入。</p>
    </div>

    <div class="source-type-board">
      <article v-for="item in sourceTypeStats" :key="item.value" class="source-type-card">
        <span>{{ item.label }}</span>
        <strong>{{ item.count }}</strong>
        <p>{{ item.description }}</p>
      </article>
    </div>

    <div class="card-panel">
      <div class="toolbar-row">
        <el-button type="primary" @click="openCreate">新增来源</el-button>
      </div>

      <el-table :data="sources" stripe>
        <el-table-column prop="name" label="来源名称" min-width="160" />
        <el-table-column prop="slug" label="标识" min-width="160" />
        <el-table-column label="来源类型" width="150">
          <template #default="{ row }">
            <el-tag effect="plain">{{ getSourceTypeMeta(row.sourceType).label }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="websiteUrl" label="官网地址" min-width="220" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled ? 'success' : 'info'">
              {{ row.isEnabled ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
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
