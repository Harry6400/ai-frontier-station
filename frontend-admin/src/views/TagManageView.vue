<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { createTag, getTags, removeTag, updateTag } from '../api/admin'

const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const tags = ref([])
const formRef = ref()
const form = reactive({
  name: '',
  slug: '',
  color: '#0f7df2',
  description: ''
})

const rules = {
  name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }]
}

function resetForm() {
  editingId.value = null
  form.name = ''
  form.slug = ''
  form.color = '#0f7df2'
  form.description = ''
}

async function loadTags() {
  loading.value = true
  try {
    const res = await getTags()
    tags.value = res.data
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
  form.color = row.color || '#0f7df2'
  form.description = row.description || ''
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  try {
    const payload = { ...form }
    if (editingId.value) {
      await updateTag(editingId.value, payload)
      ElMessage.success('标签更新成功')
    } else {
      await createTag(payload)
      ElMessage.success('标签创建成功')
    }
    dialogVisible.value = false
    resetForm()
    loadTags()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('删除后将无法恢复，确认继续吗？', '删除标签', { type: 'warning' })
    await removeTag(id)
    ElMessage.success('标签删除成功')
    loadTags()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(loadTags)
</script>

<template>
  <section class="admin-page" v-loading="loading">
    <div class="page-intro">
      <span class="sidebar-kicker">Tags</span>
      <h3>标签管理</h3>
      <p>标签表已接入真实接口，后续前台搜索、筛选、推荐和专题都可以复用这套标签体系。</p>
    </div>

    <div class="card-panel">
      <div class="toolbar-row">
        <el-button type="primary" @click="openCreate">新增标签</el-button>
      </div>

      <el-table :data="tags" stripe>
        <el-table-column prop="name" label="标签名称" min-width="140" />
        <el-table-column prop="slug" label="标识" min-width="160" />
        <el-table-column label="颜色" width="120">
          <template #default="{ row }">
            <div class="color-chip">
              <span class="color-dot" :style="{ background: row.color || '#dfe6ee' }"></span>
              <span>{{ row.color || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" min-width="220" show-overflow-tooltip />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑标签' : '新增标签'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：RAG" />
        </el-form-item>
        <el-form-item label="标签标识">
          <el-input v-model="form.slug" placeholder="可选，不填自动生成" />
        </el-form-item>
        <el-form-item label="展示色">
          <el-input v-model="form.color" placeholder="#0f7df2" />
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
