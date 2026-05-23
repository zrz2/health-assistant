<template>
  <div class="page-container">
    <div class="page-card">
      <h3>个人设置</h3>
      <el-form ref="formRef" :model="form" label-width="80px">
        <el-form-item label="头像">
          <el-avatar :size="64" :src="form.avatarUrl">
            {{ form.nickname?.[0] || 'U' }}
          </el-avatar>
        </el-form-item>
        <el-form-item label="用户名">
          <el-input :model-value="form.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="form.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="handleSave">保存修改</el-button>
        </el-form-item>
      </el-form>
    </div>
  </div>
</template>

<script setup lang="ts">
import { reactive, onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
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
  padding: 24px;
}

.page-card {
  max-width: 600px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  padding: 32px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.page-card h3 {
  font-size: 20px;
  color: #303133;
  margin-bottom: 24px;
}
</style>
