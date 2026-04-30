<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { createCategory, getCategories, removeCategory, updateCategory } from '../api/admin'

const loading = ref(false)
const dialogVisible = ref(false)
const editingId = ref(null)
const categories = ref([])
const formRef = ref()
const form = reactive({
  name: '',
  slug: '',
  description: '',
  sortOrder: 1,
  isEnabled: 1
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
  sortOrder: [{ required: true, message: '请输入排序值', trigger: 'change' }]
}

function resetForm() {
  editingId.value = null
  form.name = ''
  form.slug = ''
  form.description = ''
  form.sortOrder = 1
  form.isEnabled = 1
}

async function loadCategories() {
  loading.value = true
  try {
    const res = await getCategories()
    categories.value = res.data
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
  form.description = row.description || ''
  form.sortOrder = row.sortOrder
  form.isEnabled = row.isEnabled
  dialogVisible.value = true
}

async function submit() {
  await formRef.value.validate()
  try {
    const payload = { ...form }
    if (editingId.value) {
      await updateCategory(editingId.value, payload)
      ElMessage.success('分类更新成功')
    } else {
      await createCategory(payload)
      ElMessage.success('分类创建成功')
    }
    dialogVisible.value = false
    resetForm()
    loadCategories()
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('删除后将无法恢复，确认继续吗？', '删除分类', { type: 'warning' })
    await removeCategory(id)
    ElMessage.success('分类删除成功')
    loadCategories()
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '删除失败')
    }
  }
}

onMounted(loadCategories)
</script>

<template>
  <section class="admin-page" v-loading="loading">
    <div class="page-intro">
      <span class="sidebar-kicker">Categories</span>
      <h3>分类管理</h3>
      <p>这里已经接入真实分类表，可以维护 AI资讯、论文速递、热门项目、公司动态、技术实践等栏目。</p>
    </div>

    <div class="card-panel">
      <div class="toolbar-row">
        <el-button type="primary" @click="openCreate">新增分类</el-button>
      </div>

      <el-table :data="categories" stripe>
        <el-table-column prop="name" label="分类名称" min-width="160" />
        <el-table-column prop="slug" label="标识" min-width="160" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.isEnabled ? 'success' : 'info'">
              {{ row.isEnabled ? '启用' : '停用' }}
            </el-tag>
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

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑分类' : '新增分类'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="例如：热门项目" />
        </el-form-item>
        <el-form-item label="分类标识">
          <el-input v-model="form.slug" placeholder="可选，不填自动生成" />
        </el-form-item>
        <el-form-item label="排序值" prop="sortOrder">
          <el-input-number v-model="form.sortOrder" :min="1" />
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
