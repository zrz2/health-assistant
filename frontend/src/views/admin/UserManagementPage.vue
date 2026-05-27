<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="page-title">
        <h2>用户管理</h2>
        <p>管理系统注册用户</p>
      </div>
      <el-input
        v-model="keyword"
        placeholder="搜索用户名/昵称"
        style="width: 260px;"
        :prefix-icon="Search"
        clearable
        @change="fetchData"
      />
    </div>

    <div class="table-card">
      <el-table :data="users" v-loading="loading" stripe class="data-table">
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="180" show-overflow-tooltip />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag
              :type="row.userType === 3 ? 'danger' : 'info'"
              size="small"
              round
            >
              {{ row.userType === 3 ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag
              :type="row.status === 1 ? 'success' : 'danger'"
              size="small"
              round
            >
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录" width="180" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button text type="primary" size="small" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button text size="small" @click="changeRole(row)">改角色</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="page"
          v-model:page-size="size"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="fetchData"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, updateUserStatus, updateUserRole, type AdminUser } from '@/api/admin'

const users = ref<AdminUser[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const keyword = ref('')

async function fetchData() {
  loading.value = true
  try {
    const res = await getUsers({ page: page.value - 1, size: size.value, keyword: keyword.value })
    if (res.data) {
      users.value = res.data?.content || []
      total.value = res.data?.totalElements || 0
    }
  } catch { /* ignore */ } finally {
    loading.value = false
  }
}

async function toggleStatus(user: AdminUser) {
  const newStatus = user.status === 1 ? 0 : 1
  try {
    await updateUserStatus(user.id, newStatus)
    user.status = newStatus
    ElMessage.success(newStatus === 1 ? '已启用' : '已禁用')
  } catch { /* ignore */ }
}

async function changeRole(user: AdminUser) {
  try {
    const answer = await ElMessageBox.prompt('请输入新角色 (1=普通用户 3=管理员)', '修改角色', {
      inputValue: String(user.userType),
    })
    const newType = Number(answer.value)
    if (![1, 3].includes(newType)) {
      ElMessage.error('角色值无效')
      return
    }
    await updateUserRole(user.id, newType)
    user.userType = newType
    ElMessage.success('角色已更新')
  } catch { /* cancelled or error */ }
}

onMounted(() => fetchData())
</script>

<style scoped>
.admin-page {
  max-width: 1400px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-title h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  letter-spacing: -0.3px;
}

.page-title p {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 2px;
}

.table-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.data-table {
  --el-table-border-color: var(--border-light);
  --el-table-header-bg-color: #f8fafc;
  --el-table-header-text-color: var(--text-secondary);
  --el-table-row-hover-bg-color: var(--bg-hover);
}

.data-table :deep(th) {
  font-weight: 600;
  font-size: 13px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  padding: 16px 20px;
  border-top: 1px solid var(--border-light);
}
</style>
