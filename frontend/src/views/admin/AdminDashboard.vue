<template>
  <div class="dashboard">
    <!-- 顶部统计卡片 -->
    <div class="stat-cards">
      <div v-for="card in statCards" :key="card.label" class="stat-card">
        <div class="stat-icon" :style="{ background: card.bgColor }">
          <el-icon :size="22" :style="{ color: card.color }"><component :is="card.icon" /></el-icon>
        </div>
        <div class="stat-info">
          <div class="stat-value">{{ card.value }}</div>
          <div class="stat-label">{{ card.label }}</div>
        </div>
        <div class="stat-trend" :class="card.trendUp ? 'up' : 'down'">
          <el-icon :size="12"><component :is="card.trendUp ? 'Top' : 'Bottom'" /></el-icon>
          {{ card.trend }}
        </div>
      </div>
    </div>

    <!-- 中间区域：待处理 + 最近活动 -->
    <div class="mid-grid">
      <!-- 待处理事项 -->
      <div class="panel-card">
        <div class="panel-header">
          <h3>待处理事项</h3>
          <el-button link type="primary" size="small">查看全部</el-button>
        </div>
        <div class="pending-list">
          <div v-for="item in pendingItems" :key="item.label" class="pending-item" @click="$router.push(item.route)">
            <div class="pending-icon" :style="{ background: item.bgColor, color: item.color }">
              <el-icon :size="18"><component :is="item.icon" /></el-icon>
            </div>
            <div class="pending-text">
              <span class="pending-label">{{ item.label }}</span>
              <span class="pending-desc">{{ item.desc }}</span>
            </div>
            <div class="pending-count" :style="{ color: item.color }">{{ item.count }}</div>
          </div>
        </div>
      </div>

      <!-- 最近活动 -->
      <div class="panel-card">
        <div class="panel-header">
          <h3>最近活动</h3>
          <el-button link type="primary" size="small">更多</el-button>
        </div>
        <div class="activity-list">
          <div v-for="(act, i) in recentActivities" :key="i" class="activity-item">
            <div class="activity-dot" :style="{ background: act.color }"></div>
            <div class="activity-content">
              <span class="activity-text" v-html="act.text"></span>
              <span class="activity-time">{{ act.time }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 快捷操作 -->
      <div class="panel-card">
        <div class="panel-header">
          <h3>快捷操作</h3>
        </div>
        <div class="shortcut-grid">
          <div v-for="s in shortcuts" :key="s.label" class="shortcut-item" :style="{ background: s.bgColor }" @click="$router.push(s.route)">
            <el-icon :size="24" :style="{ color: s.color }"><component :is="s.icon" /></el-icon>
            <span :style="{ color: s.color }">{{ s.label }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 趋势图表区 -->
    <div class="chart-section">
      <div class="section-header">
        <h3>数据趋势</h3>
        <el-radio-group v-model="trendRange" size="small">
          <el-radio-button :value="7">近7天</el-radio-button>
          <el-radio-button :value="30">近30天</el-radio-button>
          <el-radio-button :value="90">近90天</el-radio-button>
        </el-radio-group>
      </div>
      <div class="trend-grid">
        <div class="chart-card">
          <div class="chart-title">
            <span>用户增长</span>
            <span class="chart-subtitle">新增注册用户数</span>
          </div>
          <div ref="userTrendRef" class="chart-box"></div>
        </div>
        <div class="chart-card">
          <div class="chart-title">
            <span>投递趋势</span>
            <span class="chart-subtitle">求职申请数量</span>
          </div>
          <div ref="applicationTrendRef" class="chart-box"></div>
        </div>
      </div>
    </div>

    <!-- 分布图表区 -->
    <div class="chart-section">
      <div class="section-header">
        <h3>数据分布</h3>
      </div>
      <div class="dist-grid">
        <div class="chart-card">
          <div class="chart-title">
            <span>职位类型分布</span>
            <span class="chart-subtitle">按工作类型统计</span>
          </div>
          <div ref="jobTypeRef" class="chart-box"></div>
        </div>
        <div class="chart-card">
          <div class="chart-title">
            <span>面试状态分布</span>
            <span class="chart-subtitle">所有面试记录</span>
          </div>
          <div ref="interviewStatusRef" class="chart-box"></div>
        </div>
        <div class="chart-card">
          <div class="chart-title">
            <span>行业职位 TOP10</span>
            <span class="chart-subtitle">按行业统计职位数</span>
          </div>
          <div ref="industryRef" class="chart-box"></div>
        </div>
        <div class="chart-card">
          <div class="chart-title">
            <span>城市职位 TOP10</span>
            <span class="chart-subtitle">按城市统计职位数</span>
          </div>
          <div ref="cityRef" class="chart-box"></div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, watch, markRaw } from 'vue'
import * as echarts from 'echarts'
import {
  User, OfficeBuilding, Suitcase, Document, VideoCamera, TrendCharts,
  Top, Bottom, Bell, Clock, Setting, DataAnalysis
} from '@element-plus/icons-vue'

// === 统计卡片 ===
const statCards = ref([
  { label: '总用户数', value: '2,847', icon: markRaw(User), color: '#3b82f6', bgColor: '#eff6ff', trend: '+12.5%', trendUp: true },
  { label: '总公司数', value: '22', icon: markRaw(OfficeBuilding), color: '#10b981', bgColor: '#ecfdf5', trend: '+2', trendUp: true },
  { label: '总职位数', value: '156', icon: markRaw(Suitcase), color: '#f59e0b', bgColor: '#fffbeb', trend: '+8.3%', trendUp: true },
  { label: '今日新增用户', value: '18', icon: markRaw(TrendCharts), color: '#8b5cf6', bgColor: '#faf5ff', trend: '+5', trendUp: true },
  { label: '今日新增职位', value: '5', icon: markRaw(Document), color: '#ec4899', bgColor: '#fdf2f8', trend: '-2', trendUp: false },
  { label: '待审核职位', value: '12', icon: markRaw(VideoCamera), color: '#ef4444', bgColor: '#fef2f2', trend: '3 新增', trendUp: false },
])

// === 待处理事项 ===
const pendingItems = ref([
  { label: '待审核职位', desc: 'HR新发布的职位等待审核', count: 12, icon: markRaw(Document), color: '#f59e0b', bgColor: '#fffbeb', route: '/admin/jobs' },
  { label: '新注册用户', desc: '今日新注册的求职者', count: 18, icon: markRaw(User), color: '#3b82f6', bgColor: '#eff6ff', route: '/admin/users' },
  { label: '举报处理', desc: '用户举报的内容待处理', count: 3, icon: markRaw(Bell), color: '#ef4444', bgColor: '#fef2f2', route: '/admin/users' },
  { label: '面试异常', desc: '超时未完成的面试', count: 2, icon: markRaw(VideoCamera), color: '#8b5cf6', bgColor: '#faf5ff', route: '/admin/interviews' },
])

// === 最近活动 ===
const recentActivities = ref([
  { text: '<b>hr_xiaomi</b> 发布了新职位 <b>前端开发工程师</b>', time: '5 分钟前', color: '#10b981' },
  { text: '<b>张三</b> 投递了 <b>Java后端开发</b> - 字节跳动', time: '12 分钟前', color: '#3b82f6' },
  { text: '<b>hr_tencent</b> 审核通过了 <b>产品经理</b> 职位', time: '30 分钟前', color: '#f59e0b' },
  { text: '<b>王五</b> 完成了AI面试，得分 <b>85</b>', time: '1 小时前', color: '#8b5cf6' },
  { text: '<b>hr_huawei</b> 认领了 <b>Python开发</b> 职位', time: '2 小时前', color: '#10b981' },
  { text: '<b>赵六</b> 上传了简历 <b>产品经理简历.pdf</b>', time: '3 小时前', color: '#ec4899' },
])

// === 快捷操作 ===
const shortcuts = ref([
  { label: '审核职位', icon: markRaw(Document), color: '#f59e0b', bgColor: '#fffbeb', route: '/admin/jobs' },
  { label: '用户管理', icon: markRaw(User), color: '#3b82f6', bgColor: '#eff6ff', route: '/admin/users' },
  { label: '公司管理', icon: markRaw(OfficeBuilding), color: '#10b981', bgColor: '#ecfdf5', route: '/admin/companies' },
  { label: '标签管理', icon: markRaw(Setting), color: '#8b5cf6', bgColor: '#faf5ff', route: '/admin/benefit-tags' },
])

// === 图表 ===
const trendRange = ref(30)
const userTrendRef = ref()
const applicationTrendRef = ref()
const jobTypeRef = ref()
const interviewStatusRef = ref()
const industryRef = ref()
const cityRef = ref()

let charts = []

const genDates = (n) => {
  const dates = []
  const now = new Date()
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(now)
    d.setDate(d.getDate() - i)
    dates.push(`${d.getMonth() + 1}/${d.getDate()}`)
  }
  return dates
}

const genRandom = (n, min, max) => Array.from({ length: n }, () => Math.floor(Math.random() * (max - min) + min))

const tooltipStyle = {
  backgroundColor: 'rgba(15, 23, 42, 0.9)',
  borderColor: 'transparent',
  textStyle: { color: '#f8fafc', fontSize: 13 },
  padding: [8, 12],
}

const initCharts = () => {
  charts.forEach(c => c.dispose())
  charts = []
  const dates = genDates(trendRange.value)

  // 用户增长趋势
  const userTrend = echarts.init(userTrendRef.value)
  userTrend.setOption({
    tooltip: { ...tooltipStyle, trigger: 'axis' },
    grid: { left: 45, right: 16, top: 16, bottom: 28 },
    xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 11, color: '#94a3b8' }, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisTick: { show: false } },
    yAxis: { type: 'value', axisLabel: { fontSize: 11, color: '#94a3b8' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    series: [{
      type: 'line', data: genRandom(trendRange.value, 5, 40), smooth: true, symbol: 'circle', symbolSize: 4,
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(59,130,246,0.2)' }, { offset: 1, color: 'rgba(59,130,246,0.01)' }
      ])},
      lineStyle: { color: '#3b82f6', width: 2.5 },
      itemStyle: { color: '#3b82f6', borderColor: '#fff', borderWidth: 2 },
    }]
  })
  charts.push(userTrend)

  // 申请趋势
  const appTrend = echarts.init(applicationTrendRef.value)
  appTrend.setOption({
    tooltip: { ...tooltipStyle, trigger: 'axis' },
    grid: { left: 45, right: 16, top: 16, bottom: 28 },
    xAxis: { type: 'category', data: dates, axisLabel: { fontSize: 11, color: '#94a3b8' }, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisTick: { show: false } },
    yAxis: { type: 'value', axisLabel: { fontSize: 11, color: '#94a3b8' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    series: [{
      type: 'line', data: genRandom(trendRange.value, 10, 60), smooth: true, symbol: 'circle', symbolSize: 4,
      areaStyle: { color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
        { offset: 0, color: 'rgba(16,185,129,0.2)' }, { offset: 1, color: 'rgba(16,185,129,0.01)' }
      ])},
      lineStyle: { color: '#10b981', width: 2.5 },
      itemStyle: { color: '#10b981', borderColor: '#fff', borderWidth: 2 },
    }]
  })
  charts.push(appTrend)

  // 职位类型分布（环形图）
  const jobType = echarts.init(jobTypeRef.value)
  jobType.setOption({
    tooltip: { ...tooltipStyle, trigger: 'item', formatter: '{b}：{c} 个 ({d}%)' },
    legend: { bottom: 4, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 12, color: '#64748b' } },
    series: [{
      type: 'pie', radius: ['42%', '68%'], center: ['50%', '44%'],
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 600 } },
      data: [
        { name: '全职', value: 98, itemStyle: { color: '#3b82f6' } },
        { name: '兼职', value: 32, itemStyle: { color: '#10b981' } },
        { name: '实习', value: 26, itemStyle: { color: '#f59e0b' } },
      ]
    }]
  })
  charts.push(jobType)

  // 面试状态分布（环形图）
  const interviewStatus = echarts.init(interviewStatusRef.value)
  interviewStatus.setOption({
    tooltip: { ...tooltipStyle, trigger: 'item', formatter: '{b}：{c} 场 ({d}%)' },
    legend: { bottom: 4, itemWidth: 10, itemHeight: 10, textStyle: { fontSize: 12, color: '#64748b' } },
    series: [{
      type: 'pie', radius: ['42%', '68%'], center: ['50%', '44%'],
      label: { show: false },
      emphasis: { label: { show: true, fontSize: 14, fontWeight: 600 } },
      data: [
        { name: '已完成', value: 186, itemStyle: { color: '#10b981' } },
        { name: '进行中', value: 14, itemStyle: { color: '#3b82f6' } },
        { name: '已取消', value: 28, itemStyle: { color: '#ef4444' } },
      ]
    }]
  })
  charts.push(interviewStatus)

  // 行业分布（水平柱状图）
  const industry = echarts.init(industryRef.value)
  industry.setOption({
    tooltip: { ...tooltipStyle, trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 85, right: 24, top: 8, bottom: 8 },
    xAxis: { type: 'value', axisLabel: { fontSize: 11, color: '#94a3b8' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    yAxis: {
      type: 'category', inverse: true,
      data: ['其他', '医疗', '电商', '游戏', '教育', '金融', '企业服务', '大数据', '人工智能', '互联网'],
      axisLabel: { fontSize: 12, color: '#475569' }, axisLine: { show: false }, axisTick: { show: false }
    },
    series: [{
      type: 'bar', barWidth: 14,
      data: [8, 10, 14, 16, 18, 22, 24, 28, 32, 48],
      label: { show: true, position: 'right', fontSize: 11, color: '#64748b' },
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
          { offset: 0, color: '#10b981' }, { offset: 1, color: '#34d399' }
        ]),
        borderRadius: [0, 6, 6, 0]
      }
    }]
  })
  charts.push(industry)

  // 城市分布（垂直柱状图）
  const city = echarts.init(cityRef.value)
  city.setOption({
    tooltip: { ...tooltipStyle, trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 45, right: 16, top: 16, bottom: 32 },
    xAxis: {
      type: 'category',
      data: ['重庆', '西安', '南京', '武汉', '成都', '杭州', '深圳', '广州', '上海', '北京'],
      axisLabel: { fontSize: 11, color: '#94a3b8', rotate: 0 }, axisLine: { lineStyle: { color: '#e2e8f0' } }, axisTick: { show: false }
    },
    yAxis: { type: 'value', axisLabel: { fontSize: 11, color: '#94a3b8' }, splitLine: { lineStyle: { color: '#f1f5f9' } } },
    series: [{
      type: 'bar', barWidth: 22,
      data: [6, 8, 10, 12, 14, 18, 22, 26, 30, 42],
      label: { show: true, position: 'top', fontSize: 11, color: '#64748b' },
      itemStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: '#3b82f6' }, { offset: 1, color: '#93c5fd' }
        ]),
        borderRadius: [6, 6, 0, 0]
      }
    }]
  })
  charts.push(city)
}

watch(trendRange, () => initCharts())

onMounted(() => {
  initCharts()
  window.addEventListener('resize', handleResize)
})

const handleResize = () => { charts.forEach(c => c.resize()) }

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  charts.forEach(c => c.dispose())
  charts = []
})
</script>

<style scoped>
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* ===== 统计卡片 ===== */
.stat-cards {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 12px;
}

.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 18px;
  display: flex;
  align-items: center;
  gap: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  position: relative;
}

.stat-icon {
  width: 42px;
  height: 42px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-value { font-size: 22px; font-weight: 700; color: #0f172a; line-height: 1.2; }
.stat-label { font-size: 12px; color: #94a3b8; margin-top: 2px; }

.stat-trend {
  position: absolute;
  top: 10px;
  right: 12px;
  font-size: 11px;
  font-weight: 600;
  display: flex;
  align-items: center;
  gap: 2px;
  padding: 2px 6px;
  border-radius: 6px;
}

.stat-trend.up { color: #10b981; background: #ecfdf5; }
.stat-trend.down { color: #ef4444; background: #fef2f2; }

/* ===== 中间区域 ===== */
.mid-grid {
  display: grid;
  grid-template-columns: 1fr 1.2fr 0.8fr;
  gap: 16px;
}

.panel-card {
  background: #fff;
  border-radius: 10px;
  padding: 18px 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 14px;
}

.panel-header h3 { font-size: 15px; font-weight: 600; color: #0f172a; margin: 0; }

/* 待处理事项 */
.pending-list { display: flex; flex-direction: column; gap: 10px; }

.pending-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 12px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.2s;
}

.pending-item:hover { background: #f8fafc; }

.pending-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.pending-text { flex: 1; display: flex; flex-direction: column; }
.pending-label { font-size: 13px; font-weight: 500; color: #1e293b; }
.pending-desc { font-size: 11px; color: #94a3b8; margin-top: 2px; }
.pending-count { font-size: 20px; font-weight: 700; }

/* 最近活动 */
.activity-list { display: flex; flex-direction: column; gap: 12px; }

.activity-item {
  display: flex;
  gap: 10px;
  align-items: flex-start;
}

.activity-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
  margin-top: 6px;
}

.activity-content { display: flex; flex-direction: column; flex: 1; }
.activity-text { font-size: 13px; color: #334155; line-height: 1.5; }
.activity-text :deep(b) { color: #0f172a; font-weight: 600; }
.activity-time { font-size: 11px; color: #cbd5e1; margin-top: 2px; }

/* 快捷操作 */
.shortcut-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}

.shortcut-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 18px 12px;
  border-radius: 10px;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.shortcut-item:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06); }

.shortcut-item span { font-size: 13px; font-weight: 500; }

/* ===== 图表区域 ===== */
.chart-section {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.section-header h3 { font-size: 15px; font-weight: 600; color: #0f172a; margin: 0; }

.trend-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.dist-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.chart-card {
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  padding: 16px;
}

.chart-title {
  display: flex;
  align-items: baseline;
  gap: 8px;
  margin-bottom: 8px;
}

.chart-title span:first-child { font-size: 14px; font-weight: 600; color: #1e293b; }
.chart-subtitle { font-size: 12px; color: #94a3b8; }

.chart-box { height: 260px; }

/* ===== 响应式 ===== */
@media (max-width: 1400px) {
  .stat-cards { grid-template-columns: repeat(3, 1fr); }
  .mid-grid { grid-template-columns: 1fr 1fr; }
  .mid-grid .panel-card:last-child { grid-column: span 2; }
}

@media (max-width: 1024px) {
  .mid-grid { grid-template-columns: 1fr; }
  .mid-grid .panel-card:last-child { grid-column: span 1; }
  .trend-grid, .dist-grid { grid-template-columns: 1fr; }
}

@media (max-width: 768px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
}
</style>
