<template>
  <div class="admin-page">
    <div class="page-header">
      <h3>咨询统计</h3>
      <el-date-picker
        v-model="dateRange"
        type="daterange"
        range-separator="至"
        start-placeholder="开始日期"
        end-placeholder="结束日期"
        value-format="YYYY-MM-DD"
      />
    </div>

    <el-row :gutter="24">
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-title">每日咨询量</div>
          <div ref="dailyChart" style="height: 300px;"></div>
        </div>
      </el-col>
      <el-col :span="12">
        <div class="chart-card">
          <div class="chart-title">热门咨询分类</div>
          <div ref="categoryChart" style="height: 300px;"></div>
        </div>
      </el-col>
    </el-row>

    <el-row :gutter="24" style="margin-top: 24px;">
      <el-col :span="24">
        <div class="chart-card">
          <div class="chart-title">知识条目使用排行</div>
          <el-table :data="topKnowledge" stripe>
            <el-table-column type="index" label="排名" width="60" />
            <el-table-column prop="title" label="标题" />
            <el-table-column prop="viewCount" label="引用次数" width="120" />
            <el-table-column prop="evidenceLevel" label="证据等级" width="100" />
          </el-table>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'

const dateRange = ref('')
const dailyChart = ref<HTMLElement>()
const categoryChart = ref<HTMLElement>()
const topKnowledge = ref<any[]>([])

onMounted(async () => {
  await nextTick()
  if (dailyChart.value) {
    const chart = echarts.init(dailyChart.value)
    chart.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
      yAxis: { type: 'value' },
      series: [{ data: [120, 200, 150, 80, 70, 110, 130], type: 'line', smooth: true }],
      grid: { left: 40, right: 20, top: 20, bottom: 30 },
    })
  }
  if (categoryChart.value) {
    const chart = echarts.init(categoryChart.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: '70%',
        data: [
          { name: '呼吸科', value: 35 },
          { name: '消化科', value: 25 },
          { name: '心血管', value: 20 },
          { name: '内分泌', value: 12 },
          { name: '其他', value: 8 },
        ],
      }],
    })
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
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
}

.chart-title {
  font-size: 16px;
  color: #303133;
  margin-bottom: 16px;
  font-weight: 500;
}
</style>
