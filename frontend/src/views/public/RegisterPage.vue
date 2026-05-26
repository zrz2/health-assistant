<template>
  <div class="register-page">
    <div class="register-left">
      <div class="left-content">
        <div class="brand">
          <div class="brand-icon">
            <el-icon :size="22" color="#fff"><FirstAidKit /></el-icon>
          </div>
          <span class="brand-name">医疗健康智能助手</span>
        </div>
        <h2 class="left-title">开启您的<br />健康管理之旅</h2>
        <p class="left-desc">注册账号，建立个人健康档案，获取更精准的个性化健康建议</p>
        <div class="left-steps">
          <div class="left-step" v-for="(s, i) in steps" :key="i">
            <div class="step-num">{{ i + 1 }}</div>
            <div>
              <div class="step-title">{{ s.title }}</div>
              <div class="step-desc">{{ s.desc }}</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="register-right">
      <div class="form-card">
        <div class="form-header">
          <h2>创建账号</h2>
          <p>填写以下信息，立即开始使用</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          label-position="top"
          class="register-form"
        >
          <el-form-item label="用户名" prop="username">
            <el-input
              v-model="form.username"
              placeholder="请输入用户名"
              :prefix-icon="User"
              size="large"
              @keyup.enter="handleRegister"
            />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input
              v-model="form.nickname"
              placeholder="给自己取个名字"
              :prefix-icon="UserFilled"
              size="large"
              @keyup.enter="handleRegister"
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码（至少6位）"
              :prefix-icon="Lock"
              show-password
              size="large"
              @keyup.enter="handleRegister"
            />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input
              v-model="form.confirmPassword"
              type="password"
              placeholder="请确认密码"
              :prefix-icon="Lock"
              show-password
              size="large"
              @keyup.enter="handleRegister"
            />
          </el-form-item>
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="submit-btn"
              :loading="loading"
              native-type="button"
              @click="handleRegister"
            >
              {{ loading ? '注册中...' : '立即注册' }}
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-footer">
          已有账号？
          <router-link to="/login" class="link">立即登录</router-link>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { User, Lock, UserFilled, FirstAidKit } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'

const authStore = useAuthStore()
const formRef = ref<FormInstance>()
const loading = ref(false)

const steps = [
  { title: '注册账号', desc: '填写基本信息完成注册' },
  { title: '完善健康档案', desc: '记录您的健康状况' },
  { title: '开始智能咨询', desc: '获取个性化健康建议' },
]

const form = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
})

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== form.password) {
    callback(new Error('两次密码输入不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

async function handleRegister() {
  if (loading.value) return
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  loading.value = true
  try {
    await authStore.register({
      username: form.username,
      password: form.password,
      nickname: form.nickname,
    })
    ElMessage.success('注册成功，请登录')
  } catch (e: any) {
    ElMessage.error(e?.message || '注册失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.register-page {
  flex: 1;
  display: flex;
  min-height: calc(100vh - 64px - 73px);
}

.register-left {
  flex: 1;
  background: linear-gradient(135deg, #0f172a 0%, #1e1b4b 60%, #0f172a 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 60px 48px;
  position: relative;
  overflow: hidden;
}

.register-left::before {
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
  margin-bottom: 40px;
}

.left-steps {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.left-step {
  display: flex;
  align-items: flex-start;
  gap: 14px;
}

.step-num {
  width: 28px;
  height: 28px;
  background: rgba(37, 99, 235, 0.3);
  border: 1px solid rgba(37, 99, 235, 0.5);
  color: #93c5fd;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 700;
  flex-shrink: 0;
  margin-top: 2px;
}

.step-title {
  font-size: 14px;
  font-weight: 600;
  color: #e2e8f0;
  margin-bottom: 2px;
}

.step-desc {
  font-size: 13px;
  color: #64748b;
}

.register-right {
  width: 480px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 40px;
  background: var(--bg-page);
  overflow-y: auto;
}

.form-card {
  width: 100%;
  max-width: 380px;
}

.form-header {
  margin-bottom: 32px;
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

.register-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text-secondary);
  font-size: 14px;
}

.register-form :deep(.el-input__wrapper) {
  border-radius: 10px !important;
  box-shadow: 0 0 0 1px var(--border-medium) !important;
  transition: box-shadow 0.2s;
}

.register-form :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px var(--primary) !important;
}

.register-form :deep(.el-input__wrapper.is-focus) {
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
