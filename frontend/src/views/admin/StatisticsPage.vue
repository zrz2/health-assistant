<template>
  <div class="admin-page">
    <div class="page-header">
      <h3>咨询统计</h3>
    </div>

    <el-row :gutter="24">
      <el-col :span="24">
        <div class="chart-card">
          <div class="chart-title">近7天咨询量</div>
          <div ref="dailyChart" style="height: 300px;"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="24" style="margin-top: 24px;">
      <el-col :span="24">
        <div class="chart-card">
          <div class="chart-title">用户最新提问</div>
          <el-table :data="recentQueries" stripe v-loading="loading" max-height="500">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="content" label="提问内容" min-width="400" show-overflow-tooltip />
            <el-table-column prop="createdAt" label="提问时间" width="180" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboardTrends, getRecentQueries } from '@/api/admin'

const dailyChart = ref<HTMLElement>()
const recentQueries = ref<any[]>([])
const loading = ref(false)

onMounted(async () => {
  // Daily consultation volume chart
  try {
    const res = await getDashboardTrends()
    await nextTick()
    if (dailyChart.value && res.data) {
      const msgTrend = res.data.messages || []
      const chart = echarts.init(dailyChart.value)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: msgTrend.map((p: any) => p.date) },
        yAxis: { type: 'value' },
        series: [{
          name: '咨询量',
          type: 'line',
          smooth: true,
          data: msgTrend.map((p: any) => p.value),
          areaStyle: { opacity: 0.15 },
        }],
        grid: { left: 50, right: 20, top: 20, bottom: 30 },
      })
    }
  } catch { /* ignore */ }

  // Recent user queries
  loading.value = true
  try {
    const res = await getRecentQueries()
    if (res.data) recentQueries.value = res.data
  } catch { /* ignore */ } finally {
    loading.value = false
  }
})
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

.chart-card {
  background: #fff;
  border-radius: 10px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}

.chart-title {
  font-size: 16px;
  color: #303133;
  margin-bottom: 16px;
  font-weight: 500;
}
</style>
