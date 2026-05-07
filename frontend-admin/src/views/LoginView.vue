<script setup>
import { computed, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus/es/components/message/index'
import { useAuthStore } from '../stores/useAuthStore'
import { useAdminThemeStore } from '../stores/useAdminThemeStore'

const router = useRouter()
const authStore = useAuthStore()
const themeStore = useAdminThemeStore()
const loading = ref(false)
const formRef = ref()

const form = reactive({
  username: '',
  password: ''
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

const nextThemeLabel = computed(() => (themeStore.isDark ? '切换浅色' : '切换深色'))

async function handleLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    await authStore.login(form.username, form.password)
    ElMessage.success('登录成功')
    router.push('/')
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-container">
    <div class="login-card">
      <div class="login-header">
        <span class="login-kicker">Admin Console</span>
        <h1>AI 前沿情报站</h1>
        <p>管理后台登录</p>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="0"
        @submit.prevent="handleLogin"
      >
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            size="large"
          />
        </el-form-item>

        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <el-form-item>
          <el-button
            type="primary"
            size="large"
            :loading="loading"
            style="width: 100%"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <div class="login-footer">
        <p>请使用你本地初始化的管理员账号登录。</p>
      </div>
    </div>

    <button class="theme-toggle-float" type="button" @click="themeStore.toggleTheme()">
      {{ nextThemeLabel }}
    </button>
  </div>
</template>

<style scoped>
.login-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--admin-body);
  position: relative;
}

.login-card {
  width: 400px;
  padding: 40px;
  background: var(--admin-surface);
  border: 1px solid var(--admin-line);
  border-radius: var(--admin-radius-lg);
  box-shadow: var(--admin-shadow-md);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}

.login-kicker {
  display: inline-block;
  padding: 4px 12px;
  background: var(--admin-muted);
  border-radius: var(--admin-radius-sm);
  font-size: 11px;
  font-weight: 600;
  color: var(--color-text-tertiary);
  margin-bottom: 16px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.login-header h1 {
  margin: 0 0 8px;
  font-size: 24px;
  font-weight: 600;
  color: var(--color-text-primary);
  font-family: var(--font-display);
}

.login-header p {
  margin: 0;
  color: var(--color-text-secondary);
  font-size: 14px;
}

.login-footer {
  text-align: center;
  margin-top: 24px;
  padding-top: 16px;
  border-top: 1px solid var(--admin-line);
}

.login-footer p {
  margin: 0;
  color: var(--color-text-tertiary);
  font-size: 12px;
}

.theme-toggle-float {
  position: fixed;
  bottom: 24px;
  right: 24px;
  padding: 8px 16px;
  background: var(--admin-surface);
  border: 1px solid var(--admin-line);
  border-radius: var(--admin-radius-md);
  color: var(--color-text-secondary);
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s ease;
}

.theme-toggle-float:hover {
  background: var(--admin-muted);
  color: var(--color-text-primary);
}

:deep(.el-input__wrapper) {
  border-radius: var(--admin-radius-md);
  background: var(--admin-surface);
  box-shadow: 0 0 0 1px var(--admin-line);
}

:deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--color-text-tertiary);
}

:deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px var(--admin-brand);
}

:deep(.el-button--primary) {
  background: var(--admin-brand);
  border-color: var(--admin-brand);
  border-radius: var(--admin-radius-md);
}

:deep(.el-button--primary:hover) {
  background: var(--admin-brand-hover);
  border-color: var(--admin-brand-hover);
}
</style>
