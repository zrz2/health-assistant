<template>
  <div class="admin-page">
    <div class="page-header">
      <h3>操作日志</h3>
      <div class="filters">
        <el-input v-model="keyword" placeholder="搜索操作人/描述" style="width: 200px;" :prefix-icon="Search" clearable @change="fetchData" />
        <el-date-picker
          v-model="dateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          value-format="YYYY-MM-DD"
          style="margin-left: 8px;"
        />
      </div>
    </div>

    <el-table :data="logs" v-loading="loading" stripe>
      <el-table-column prop="id" label="ID" width="80" />
      <el-table-column prop="username" label="操作人" width="120" />
      <el-table-column prop="operationType" label="操作类型" width="120" />
      <el-table-column prop="operationDesc" label="操作描述" min-width="200" show-overflow-tooltip />
      <el-table-column prop="requestUrl" label="请求URL" width="200" show-overflow-tooltip />
      <el-table-column prop="ipAddress" label="IP地址" width="140" />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
            {{ row.status === 1 ? '成功' : '失败' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="时间" width="180" />
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
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { Search } from '@element-plus/icons-vue'
import { getOperationLogs, type OperationLog } from '@/api/admin'

const logs = ref<OperationLog[]>([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const keyword = ref('')
const dateRange = ref('')

async function fetchData() {
  loading.value = true
  try {
    const res = await getOperationLogs({ page: page.value - 1, size: size.value, keyword: keyword.value })
    if (res.data) {
      logs.value = res.data?.content || []
      total.value = res.data?.totalElements || 0
    }
  } catch { /* ignore */ } finally {
    loading.value = false
  }
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

.filters {
  display: flex;
  gap: 8px;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
