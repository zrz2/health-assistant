<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="page-title">
        <h2>敏感词管理</h2>
        <p>管理系统内容过滤规则</p>
      </div>
    </div>

    <div class="add-card">
      <div class="add-title">添加敏感词</div>
      <div class="add-form">
        <el-input
          v-model="newWord"
          placeholder="输入新敏感词"
          style="width: 240px;"
          @keyup.enter="handleAdd"
        />
        <el-select v-model="newLevel" style="width: 130px;" placeholder="拦截级别">
          <el-option label="提示" :value="1" />
          <el-option label="拦截" :value="2" />
        </el-select>
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>
          添加
        </el-button>
      </div>
    </div>

    <div class="table-card">
      <el-table :data="words" v-loading="loading" stripe class="data-table">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="word" label="敏感词" min-width="160" />
        <el-table-column prop="category" label="分类" width="120" />
        <el-table-column label="级别" width="100">
          <template #default="{ row }">
            <el-tag :type="row.level === 2 ? 'danger' : 'warning'" size="small" round>
              {{ row.level === 2 ? '拦截' : '提示' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small" round>
              {{ row.enabled === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{ row }">
            <el-button text type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getSensitiveWords, addSensitiveWord, deleteSensitiveWord, type SensitiveWord } from '@/api/admin'

const words = ref<SensitiveWord[]>([])
const loading = ref(false)
const newWord = ref('')
const newLevel = ref(1)

async function fetchData() {
  loading.value = true
  try {
    const res = await getSensitiveWords()
    if (res.data) words.value = res.data
  } catch { /* ignore */ } finally {
    loading.value = false
  }
}

async function handleAdd() {
  if (!newWord.value.trim()) return
  try {
    await addSensitiveWord({ word: newWord.value.trim(), level: newLevel.value })
    ElMessage.success('添加成功')
    newWord.value = ''
    fetchData()
  } catch { /* ignore */ }
}

async function handleDelete(row: SensitiveWord) {
  try {
    await deleteSensitiveWord(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch { /* ignore */ }
}

onMounted(() => fetchData())
</script>

<style scoped>
.admin-page {
  max-width: 1000px;
}

.page-header {
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

.add-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  padding: 20px 24px;
  margin-bottom: 20px;
  box-shadow: var(--shadow-sm);
}

.add-title {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-secondary);
  margin-bottom: 14px;
}

.add-form {
  display: flex;
  gap: 10px;
  align-items: center;
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
</style>
