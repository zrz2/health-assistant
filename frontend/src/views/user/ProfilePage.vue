<template>
  <div class="page-container">
    <div class="page-layout">
      <div class="page-sidebar">
        <div class="profile-card">
          <el-avatar :size="72" :src="form.avatarUrl" class="profile-avatar">
            {{ form.nickname?.[0] || 'U' }}
          </el-avatar>
          <div class="profile-name">{{ form.nickname || form.username }}</div>
          <div class="profile-username">@{{ form.username }}</div>
          <div class="profile-meta">
            <div class="meta-item" v-if="form.email">
              <el-icon :size="13"><Message /></el-icon>
              {{ form.email }}
            </div>
            <div class="meta-item" v-if="form.phone">
              <el-icon :size="13"><Phone /></el-icon>
              {{ form.phone }}
            </div>
          </div>
        </div>
      </div>

      <div class="page-main">
        <div class="section-card">
          <div class="section-header">
            <h3>基本信息</h3>
            <p>更新您的个人资料</p>
          </div>
          <el-form ref="formRef" :model="form" label-position="top" class="profile-form">
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="用户名">
                  <el-input :model-value="form.username" disabled>
                    <template #suffix>
                      <el-icon :size="14" color="#9ca3af"><Lock /></el-icon>
                    </template>
                  </el-input>
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="昵称">
                  <el-input v-model="form.nickname" placeholder="请输入昵称" />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="20">
              <el-col :span="12">
                <el-form-item label="邮箱">
                  <el-input v-model="form.email" placeholder="请输入邮箱" />
                </el-form-item>
              </el-col>
              <el-col :span="12">
                <el-form-item label="手机号">
                  <el-input v-model="form.phone" placeholder="请输入手机号" />
                </el-form-item>
              </el-col>
            </el-row>
            <div class="form-actions">
              <el-button type="primary" :loading="saving" @click="handleSave">
                保存修改
              </el-button>
            </div>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { Message, Phone, Lock } from '@element-plus/icons-vue'
import { useAuthStore } from '@/stores/auth'
import { getProfile, updateProfile } from '@/api/user'

const authStore = useAuthStore()
const saving = ref(false)

const form = reactive({
  username: '',
  nickname: '',
  email: '',
  phone: '',
  avatarUrl: '',
})

async function handleSave() {
  saving.value = true
  try {
    await updateProfile({
      nickname: form.nickname,
      email: form.email,
      phone: form.phone,
    })
    if (authStore.userInfo) {
      authStore.userInfo.nickname = form.nickname
      localStorage.setItem('userInfo', JSON.stringify(authStore.userInfo))
    }
    ElMessage.success('保存成功')
  } catch {
    // handled by interceptor
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getProfile()
    if (res.data) {
      Object.assign(form, res.data)
    }
  } catch {
    // ignore
  }
})
</script>

<style scoped>
.page-container {
  height: 100%;
  overflow-y: auto;
  padding: 28px;
  background: var(--bg-page);
}

.page-layout {
  max-width: 900px;
  margin: 0 auto;
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.page-sidebar {
  width: 220px;
  flex-shrink: 0;
}

.profile-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 16px;
  padding: 28px 20px;
  text-align: center;
  box-shadow: var(--shadow-sm);
}

.profile-avatar {
  font-size: 24px;
  margin-bottom: 14px;
}

.profile-name {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.profile-username {
  font-size: 13px;
  color: var(--text-muted);
  margin-bottom: 16px;
}

.profile-meta {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.meta-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--text-muted);
  justify-content: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.page-main {
  flex: 1;
  min-width: 0;
}

.section-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 16px;
  padding: 28px;
  box-shadow: var(--shadow-sm);
}

.section-header {
  margin-bottom: 24px;
}

.section-header h3 {
  font-size: 17px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.section-header p {
  font-size: 13px;
  color: var(--text-muted);
}

.profile-form :deep(.el-form-item__label) {
  font-weight: 500;
  color: var(--text-secondary);
  font-size: 13px;
}

.profile-form :deep(.el-input__wrapper) {
  border-radius: 8px !important;
}

.form-actions {
  margin-top: 8px;
}
</style>
