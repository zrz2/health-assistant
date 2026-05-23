<template>
  <div class="admin-page">
    <div class="page-header">
      <h3>敏感词管理</h3>
      <div>
        <el-input v-model="newWord" placeholder="输入新敏感词" style="width: 200px;" @keyup.enter="handleAdd" />
        <el-select v-model="newLevel" style="width: 120px; margin-left: 8px;" placeholder="级别">
          <el-option label="提示" :value="1" />
          <el-option label="拦截" :value="2" />
        </el-select>
        <el-button type="primary" style="margin-left: 8px;" @click="handleAdd">添加</el-button>
      </div>
    </div>

    <el-table :data="words" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="word" label="敏感词" />
      <el-table-column prop="category" label="分类" width="120" />
      <el-table-column label="级别" width="100">
        <template #default="{ row }">
          <el-tag :type="row.level === 2 ? 'danger' : 'warning'" size="small">
            {{ row.level === 2 ? '拦截' : '提示' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled === 1 ? 'success' : 'info'" size="small">
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
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
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
.admin-page h3 {
  font-size: 20px;
  color: #303133;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
</style>
