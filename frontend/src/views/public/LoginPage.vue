<template>
  <div class="login-page">
    <div class="login-left">
      <div class="left-content">
        <div class="brand">
          <div class="brand-icon">
            <el-icon :size="22" color="#fff"><FirstAidKit /></el-icon>
          </div>
          <span class="brand-name">医疗健康智能助手</span>
        </div>
        <h2 class="left-title">专业健康咨询<br />触手可及</h2>
        <p class="left-desc">基于循证医学知识库与 AI 大模型，为您提供科学可靠的健康建议</p>
        <div class="left-features">
          <div class="left-feature" v-for="f in leftFeatures" :key="f">
            <el-icon :size="16" color="#60a5fa"><CircleCheck /></el-icon>
            <span>{{ f }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="login-right">
      <div class="form-card">
        <div class="form-header">
          <h2>欢迎回来</h2>
          <p>登录您的账号，继续健康咨询</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="login-form"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              size="large"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              :prefix-icon="Lock"
              show-password
              size="large"
              @keyup.enter="handleLogin"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="submit-btn"
              :loading="loading"
              native-type="button"
              @click="handleLogin"
            >
              {{ loading ? '登录中...' : '登录' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          还没有账号？
          <router-link to="/register" class="link">立即注册</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { User, Lock, CircleCheck, FirstAidKit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const leftFeatures = [
  '40+ 权威医疗知识来源',
  '实时流式 AI 对话体验',
  '个性化健康档案管理',
  '证据等级透明可溯源',
]

const form = reactive({
  username: '',
  password: '',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin() {
  if (loading.value) return
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await authStore.login({ username: form.username, password: form.password })
  } catch (e: any) {
    ElMessage.error(e?.message || '登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  flex: 1;
  display: flex;
  min-height: calc(100vh - 64px - 73px);
}

.login-left {
  flex: 1;
  background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 60%, #0f172a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 48px;
  position: relative;
  overflow: hidden;
}

.login-left::before {
  content: '';
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.03) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.03) 1px, transparent 1px);
  background-size: 40px 40px;
}

.left-content {
  position: relative;
  z-index: 1;
  max-width: 400px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 40px;
}

.brand-icon {
  width: 40px;
  height: 40px;
  background: linear-gradient(135deg, var(--primary) 0%, #7c3aed 100%);
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.brand-name {
  font-size: 16px;
  font-weight: 600;
  color: #e2e8f0;
}

.left-title {
  font-size: 40px;
  font-weight: 800;
  color: #fff;
  line-height: 1.2;
  margin-bottom: 16px;
  letter-spacing: -0.5px;
}

.left-desc {
  font-size: 15px;
  color: #94a3b8;
  line-height: 1.7;
  margin-bottom: 36px;
}

.left-features {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.left-feature {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 14px;
  color: #cbd5e1;
}

.login-right {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  background: var(--bg-page);
}

.form-card {
  width: 100%;
  max-width: 380px;
}

.form-header {
  margin-bottom: 36px;
}

.form-header h2 {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
  margin-bottom: 8px;
  letter-spacing: -0.5px;
}

.form-header p {
  font-size: 14px;
  color: var(--text-muted);
}

.login-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text-secondary);
  font-size: 14px;
}

.login-form :deep(.el-input__wrapper) {
  border-radius: 10px !important;
  box-shadow: 0 0 0 1px var(--border-medium) !important;
  transition: box-shadow 0.2s;
}

.login-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--primary) !important;
}

.login-form :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 2px rgba(37, 99, 235, 0.2), 0 0 0 1px var(--primary) !important;
}

.submit-btn {
  width: 100%;
  height: 48px !important;
  font-size: 15px !important;
  font-weight: 600 !important;
  border-radius: 10px !important;
  background: linear-gradient(135deg, var(--primary) 0%, #7c3aed 100%) !important;
  border: none !important;
  box-shadow: 0 4px 16px rgba(37, 99, 235, 0.35) !important;
  transition: all 0.2s !important;
}

.submit-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 6px 20px rgba(37, 99, 235, 0.45) !important;
}

.form-footer {
  text-align: center;
  font-size: 14px;
  color: var(--text-muted);
  margin-top: 4px;
}

.link {
  color: var(--primary);
  font-weight: 500;
  text-decoration: none;
}

.link:hover {
  text-decoration: underline;
}
</style>
