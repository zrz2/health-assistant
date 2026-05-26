<template>
  <div class="dashboard-page">
    <div class="page-title">
      <h2>仪表盘</h2>
      <p>系统运行概览</p>
    </div>

    <el-row :gutter="20" class="stat-row">
      <el-col :span="6" v-for="card in statCards" :key="card.label">
        <div class="stat-card" :style="{ '--card-color': card.color, '--card-bg': card.bg }">
          <div class="stat-icon-wrap">
            <el-icon :size="22">
              <component :is="card.icon" />
            </el-icon>
          </div>
          <div class="stat-info">
            <div class="stat-num">{{ card.value }}</div>
            <div class="stat-label">{{ card.label }}</div>
          </div>
          <div class="stat-trend" v-if="card.trend">
            <el-icon :size="12"><Top /></el-icon>
            {{ card.trend }}
          </div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="20" class="chart-row">
      <el-col :span="16">
        <div class="chart-card">
          <div class="chart-header">
            <div class="chart-title">咨询趋势</div>
            <div class="chart-subtitle">近7天咨询量变化</div>
          </div>
          <div ref="trendsChart" class="chart-body"></div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="chart-card">
          <div class="chart-header">
            <div class="chart-title">知识来源分布</div>
            <div class="chart-subtitle">按来源类型统计</div>
          </div>
          <div ref="sourceChart" class="chart-body"></div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { getDashboardStats, getDashboardTrends, getSourceDistribution } from '@/api/admin'
import { ChatDotRound, User, Collection, Timer, Top } from '@element-plus/icons-vue'

const stats = reactive({
  todayActiveSessions: 0,
  totalUsers: 0,
  totalKnowledgeItems: 0,
  totalSessions: 0,
})

const statCards = computed(() => [
  { label: '今日活跃会话', value: stats.todayActiveSessions, icon: 'ChatDotRound', color: '#2563eb', bg: '#eff6ff', trend: '' },
  { label: '总用户数', value: stats.totalUsers, icon: 'User', color: '#16a34a', bg: '#f0fdf4', trend: '' },
  { label: '知识库条目', value: stats.totalKnowledgeItems, icon: 'Collection', color: '#d97706', bg: '#fffbeb', trend: '' },
  { label: '总会话数', value: stats.totalSessions, icon: 'Timer', color: '#7c3aed', bg: '#f5f3ff', trend: '' },
])

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
      const msgTrend = res.data.messages || []
      chart.setOption({
        tooltip: { trigger: 'axis', backgroundColor: '#1e293b', borderColor: '#334155', textStyle: { color: '#e2e8f0' } },
        grid: { left: 40, right: 20, top: 20, bottom: 30 },
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
          type: 'line',
          smooth: true,
          data: msgTrend.map((p: any) => p.value),
          lineStyle: { color: '#2563eb', width: 2.5 },
          itemStyle: { color: '#2563eb' },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(37,99,235,0.2)' },
              { offset: 1, color: 'rgba(37,99,235,0.02)' },
            ]),
          },
          symbol: 'circle',
          symbolSize: 6,
        }],
      })
    }
  } catch { /* ignore */ }

  try {
    const res = await getSourceDistribution()
    await nextTick()
    if (sourceChart.value && res.data) {
      const chart = echarts.init(sourceChart.value)
      const colors = ['#2563eb', '#7c3aed', '#16a34a', '#d97706', '#dc2626']
      chart.setOption({
        tooltip: { trigger: 'item', backgroundColor: '#1e293b', borderColor: '#334155', textStyle: { color: '#e2e8f0' } },
        legend: { bottom: 0, textStyle: { color: '#6b7280', fontSize: 12 } },
        series: [{
          type: 'pie',
          radius: ['42%', '68%'],
          center: ['50%', '45%'],
          data: (res.data.sources || []).map((item: any, i: number) => ({
            name: item.sourceName,
            value: item.count,
            itemStyle: { color: colors[i % colors.length] },
          })),
          label: { show: false },
          emphasis: { label: { show: true, fontSize: 13, fontWeight: 'bold' } },
        }],
      })
    }
  } catch { /* ignore */ }
})
</script>

<style scoped>
.dashboard-page {
  max-width: 1400px;
}

.page-title {
  margin-bottom: 24px;
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

.stat-row {
  margin-bottom: 20px;
}

.stat-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: var(--shadow-sm);
  position: relative;
  overflow: hidden;
  transition: all 0.2s;
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: var(--shadow-md);
}

.stat-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: var(--card-color);
  border-radius: 14px 14px 0 0;
}

.stat-icon-wrap {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: var(--card-bg);
  color: var(--card-color);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-info {
  flex: 1;
}

.stat-num {
  font-size: 26px;
  font-weight: 800;
  color: var(--text-primary);
  line-height: 1.2;
  letter-spacing: -0.5px;
}

.stat-label {
  font-size: 13px;
  color: var(--text-muted);
  margin-top: 3px;
}

.stat-trend {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 11px;
  color: #16a34a;
  background: #f0fdf4;
  padding: 3px 7px;
  border-radius: 100px;
  position: absolute;
  top: 14px;
  right: 14px;
}

.chart-row {
  margin-top: 0;
}

.chart-card {
  background: var(--bg-card);
  border: 1px solid var(--border-light);
  border-radius: 14px;
  padding: 20px 24px;
  box-shadow: var(--shadow-sm);
}

.chart-header {
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
  height: 300px;
}
</style>
