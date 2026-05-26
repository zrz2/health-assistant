<template>
  <div class="admin-page">
    <div class="page-header">
      <div class="page-title">
        <h2>咨询统计</h2>
        <p>用户咨询行为分析</p>
      </div>
    </div>

    <div class="chart-card" style="margin-bottom: 20px;">
      <div class="chart-header">
        <div class="chart-title">近7天咨询量</div>
        <div class="chart-subtitle">每日咨询消息数量趋势</div>
      </div>
      <div ref="dailyChart" class="chart-body"></div>
    </div>

    <div class="table-card">
      <div class="table-header">
        <div class="chart-title">用户最新提问</div>
        <div class="chart-subtitle">最近收到的用户咨询问题</div>
      </div>
      <el-table :data="recentQueries" stripe v-loading="loading" max-height="500" class="data-table">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="content" label="提问内容" min-width="400" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="提问时间" width="180" />
      </el-table>
    </div>
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
  try {
    const res = await getDashboardTrends()
    await nextTick()
    if (dailyChart.value && res.data) {
      const msgTrend = res.data.messages || []
      const chart = echarts.init(dailyChart.value)
      chart.setOption({
        tooltip: { trigger: 'axis', backgroundColor: '#1e293b', borderColor: '#334155', textStyle: { color: '#e2e8f0' } },
        grid: { left: 50, right: 20, top: 20, bottom: 30 },
        xAxis: {
          type: 'category',
          data: msgTrend.map((p: any) => p.date),
          axisLine: { lineStyle: { color: '#e5e7eb' } },
          axisLabel: { color: '#9ca3af', fontSize: 12 },
        },
        yAxis: {
          type: 'value',
          splitLine: { lineStyle: { color: '#f3f4f6' } },
          axisLabel: { color: '#9ca3af', fontSize: 12 },
        },
        series: [{
          name: '咨询量',
          type: 'bar',
          data: msgTrend.map((p: any) => p.value),
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: '#2563eb' },
              { offset: 1, color: '#7c3aed' },
            ]),
            borderRadius: [4, 4, 0, 0],
          },
        }],
      })
    }
  } catch { /* ignore */ }

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
.admin-page {
  max-width: 1400px;
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

.chart-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  padding: 20px 24px;
  box-shadow: var(--shadow-sm);
}

.chart-header, .table-header {
  margin-bottom: 16px;
}

.chart-title {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.chart-subtitle {
  font-size: 12px;
  color: var(--text-muted);
  margin-top: 2px;
}

.chart-body {
  height: 280px;
}

.table-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  overflow: hidden;
  box-shadow: var(--shadow-sm);
}

.table-header {
  padding: 20px 24px 0;
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
