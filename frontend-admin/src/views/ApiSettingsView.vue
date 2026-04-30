<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus/es/components/message/index'
import { ElMessageBox } from 'element-plus/es/components/message-box/index'
import { clearApiCredential, getApiSettingsStatus, saveApiCredential } from '../api/admin'

const loading = ref(false)
const status = ref({
  bailian: { enabled: false, maskedKey: '', source: 'none', updatedAt: null },
  github: { enabled: false, maskedKey: '', source: 'none', updatedAt: null }
})

const forms = reactive({
  bailian: { apiKey: '', remark: '用于 AI 来源整理和 AI 导读生成' },
  github: { apiKey: '', remark: '用于 GitHub 仓库查询和搜索' }
})

const providers = computed(() => [
  {
    key: 'bailian',
    label: '阿里百炼',
    name: 'Bailian / DashScope',
    capability: 'AI 导读、分类建议、标签建议、推荐理由生成',
    placeholder: '请输入百炼 API Key，例如 sk-...',
    status: status.value.bailian
  },
  {
    key: 'github',
    label: 'GitHub',
    name: 'GitHub REST API',
    capability: '真实仓库查询、关键词搜索、项目导入数据回填',
    placeholder: '请输入 GitHub Token，可选；不填也能匿名查询公开仓库',
    status: status.value.github
  }
])

function formatDate(value) {
  if (!value) {
    return '本次运行尚未配置'
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

async function loadStatus() {
  loading.value = true
  try {
    const res = await getApiSettingsStatus()
    status.value = res.data
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

async function save(provider) {
  const form = forms[provider]
  if (!form.apiKey.trim()) {
    ElMessage.warning('请先输入密钥')
    return
  }
  try {
    await saveApiCredential(provider, {
      apiKey: form.apiKey,
      remark: form.remark
    })
    form.apiKey = ''
    await loadStatus()
    ElMessage.success('API 能力已启用')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

async function clear(provider) {
  try {
    await ElMessageBox.confirm('清除后本次后端运行内将不再使用该密钥，确认继续吗？', '清除 API 配置', { type: 'warning' })
    await clearApiCredential(provider)
    await loadStatus()
    ElMessage.success('API 配置已清除')
  } catch (error) {
    if (error !== 'cancel') {
      ElMessage.error(error.message || '清除失败')
    }
  }
}

onMounted(loadStatus)
</script>

<template>
  <section class="admin-page" v-loading="loading">
    <div class="page-intro">
      <span class="sidebar-kicker">Runtime Settings</span>
      <h3>API 设置</h3>
      <p>
        这里用于课堂演示阶段临时启用外部能力。密钥只保存在 Spring Boot 运行内存里，
        不写入数据库、不回显完整内容，后端重启后需要重新配置。
      </p>
    </div>

    <div class="api-settings-grid">
      <article
        v-for="item in providers"
        :key="item.key"
        class="api-setting-card"
      >
        <div class="api-setting-head">
          <div>
            <span class="sidebar-kicker">{{ item.name }}</span>
            <h4>{{ item.label }}</h4>
          </div>
          <span class="api-status-pill" :class="{ 'is-enabled': item.status.enabled }">
            {{ item.status.enabled ? '已启用' : '未启用' }}
          </span>
        </div>

        <p>{{ item.capability }}</p>

        <div class="api-status-board">
          <div>
            <span>密钥状态</span>
            <strong>{{ item.status.enabled ? item.status.maskedKey : '未配置' }}</strong>
          </div>
          <div>
            <span>保存位置</span>
            <strong>
              {{ item.status.source === 'runtime' ? '后端内存' : item.status.source === 'environment' ? '环境变量' : '未启用' }}
            </strong>
          </div>
          <div>
            <span>更新时间</span>
            <strong>{{ formatDate(item.status.updatedAt) }}</strong>
          </div>
        </div>

        <el-input
          v-model="forms[item.key].apiKey"
          type="password"
          show-password
          :placeholder="item.placeholder"
        />
        <el-input
          v-model="forms[item.key].remark"
          placeholder="备注，可选"
        />

        <div class="api-setting-actions">
          <el-button type="primary" @click="save(item.key)">保存并启用</el-button>
          <el-button :disabled="!item.status.enabled" @click="clear(item.key)">清除</el-button>
        </div>
      </article>
    </div>

    <div class="card-panel api-setting-note">
      <span class="sidebar-kicker">Defense Note</span>
      <h4>答辩时可以怎么讲</h4>
      <p>
        这不是完整的生产级密钥管理，而是课程项目第一版的运行时配置方案。
        它比把 Key 写死在代码里更安全，也比直接落库更适合演示。后续如果接登录权限，
        可以继续扩展为管理员加密配置表。
      </p>
    </div>
  </section>
</template>
