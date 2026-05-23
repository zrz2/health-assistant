<template>
  <div class="dashboard-page">
    <h3>仪表盘</h3>

    <el-row :gutter="24" class="stat-cards">
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #ecf5ff; color: #409EFF;">
            <el-icon :size="28"><ChatDotRound /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.todayActiveSessions }}</div>
            <div class="stat-label">今日活跃会话</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #f0f9eb; color: #67C23A;">
            <el-icon :size="28"><User /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.totalUsers }}</div>
            <div class="stat-label">总用户数</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #fdf6ec; color: #E6A23C;">
            <el-icon :size="28"><Collection /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.totalKnowledgeItems }}</div>
            <div class="stat-label">知识库条目</div>
          </div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-icon" style="background: #fef0f0; color: #F56C6C;">
            <el-icon :size="28"><Timer /></el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ stats.totalSessions }}</div>
            <div class="stat-label">总会话数</div>
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="24" style="margin-top: 24px;">
      <el-col :span="16">
        <div class="chart-card">
          <div class="chart-title">咨询趋势（近7天）</div>
          <div ref="trendsChart" style="height: 320px;"></div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="chart-card">
          <div class="chart-title">知识来源分布</div>
          <div ref="sourceChart" style="height: 320px;"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboardStats, getDashboardTrends, getSourceDistribution } from '@/api/admin'

const stats = reactive({
  todayActiveSessions: 0,
  totalUsers: 0,
  totalKnowledgeItems: 0,
  totalSessions: 0,
})

const trendsChart = ref<HTMLElement>()
const sourceChart = ref<HTMLElement>()

onMounted(async () => {
  try {
    const res = await getDashboardStats()
    if (res.data) Object.assign(stats, res.data)
  } catch { /* ignore */ }

  try {
    const res = await getDashboardTrends()
    await nextTick()
    if (trendsChart.value && res.data) {
      const chart = echarts.init(trendsChart.value)
      chart.setOption({
        tooltip: { trigger: 'axis' },
        xAxis: { type: 'category', data: res.data.dates || [] },
        yAxis: { type: 'value' },
        series: [{
          name: '咨询量',
          type: 'line',
          smooth: true,
          data: res.data.values || [],
          areaStyle: { opacity: 0.15 },
        }],
        grid: { left: 40, right: 20, top: 20, bottom: 30 },
      })
    }
  } catch { /* ignore */ }

  try {
    const res = await getSourceDistribution()
    await nextTick()
    if (sourceChart.value && res.data) {
      const chart = echarts.init(sourceChart.value)
      chart.setOption({
        tooltip: { trigger: 'item' },
        series: [{
          type: 'pie',
          radius: ['45%', '75%'],
          data: (res.data || []).map((item: any) => ({
            name: item.name,
            value: item.value,
          })),
        }],
      })
    }
  } catch { /* ignore */ }
})
</script>

<style scoped>
.dashboard-page h3 {
  font-size: 20px;
  color: #303133;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 24px;
  display: flex;
  align-items: center;
  gap: 16px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.stat-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-num {
  font-size: 28px;
  font-weight: 700;
  color: #303133;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

.chart-card {
  background: #fff;
  border-radius: 10px;
  padding: 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.chart-title {
  font-size: 16px;
  color: #303133;
  margin-bottom: 16px;
  font-weight: 500;
}
</style>
