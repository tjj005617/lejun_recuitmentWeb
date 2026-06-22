<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue"><el-icon :size="22"><VideoCamera /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ tableData.length }}</div>
          <div class="stat-label">总面试数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><el-icon :size="22"><CircleCheck /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ countByStatus('completed') }}</div>
          <div class="stat-label">已完成</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple"><el-icon :size="22"><Loading /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ countByStatus('in_progress') }}</div>
          <div class="stat-label">进行中</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red"><el-icon :size="22"><CircleClose /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ countByStatus('cancelled') }}</div>
          <div class="stat-label">已取消</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><el-icon :size="22"><Star /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ avgScore }}</div>
          <div class="stat-label">平均分</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon cyan"><el-icon :size="22"><TrendCharts /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">4</div>
          <div class="stat-label">今日面试</div>
        </div>
      </div>
    </div>

    <!-- 搜索与筛选 -->
    <div class="filter-card">
      <div class="filter-row">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索求职者 / 职位"
          clearable
          prefix-icon="Search"
          style="width: 220px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="filterCompany" placeholder="公司" clearable style="width: 130px" @change="handleSearch">
          <el-option v-for="c in companyList" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="filterType" placeholder="面试类型" clearable style="width: 120px" @change="handleSearch">
          <el-option label="AI面试" value="ai" />
          <el-option label="视频面试" value="video" />
          <el-option label="现场面试" value="onsite" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 110px" @change="handleSearch">
          <el-option label="已完成" value="completed" />
          <el-option label="进行中" value="in_progress" />
          <el-option label="已取消" value="cancelled" />
        </el-select>
        <el-date-picker
          v-model="filterDateRange"
          type="daterange"
          range-separator="至"
          start-placeholder="开始日期"
          end-placeholder="结束日期"
          style="width: 260px"
          @change="handleSearch"
        />
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <div class="filter-right">
        <el-button type="warning" plain @click="handleExport">
          <el-icon><Download /></el-icon> 导出
        </el-button>
        <el-button type="danger" plain :disabled="!selectedRows.length" @click="handleBatchCancel">
          批量取消 ({{ selectedRows.length }})
        </el-button>
      </div>
    </div>

    <!-- 表格 -->
    <div class="table-card">
      <el-table
        :data="filteredData"
        stripe
        style="width: 100%"
        @selection-change="handleSelectionChange"
        :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: 600 }"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column prop="id" label="ID" width="60" align="center" />
        <el-table-column label="求职者" min-width="160">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="36" :style="{ background: avatarColor(row) }">
                {{ row.applicant?.charAt(0) }}
              </el-avatar>
              <div class="user-text">
                <div class="user-name">{{ row.applicant }}</div>
                <div class="user-edu">{{ row.education }} · {{ row.experience }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="申请职位" min-width="170">
          <template #default="{ row }">
            <div class="job-cell">
              <div class="job-title">{{ row.jobTitle }}</div>
              <div class="job-company">
                <div class="mini-logo" :style="{ background: companyColor(row.industry) }">
                  {{ row.companyInitial }}
                </div>
                {{ row.companyName }}
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="面试轮次" width="85" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="roundTagType(row.round)" effect="dark" round>
              {{ row.round }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="面试类型" width="100" align="center">
          <template #default="{ row }">
            <div class="type-badge" :class="row.typeCode">
              <el-icon :size="14"><component :is="typeIcon(row.typeCode)" /></el-icon>
              {{ row.type }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="AI评分" width="100" align="center">
          <template #default="{ row }">
            <div v-if="row.score" class="score-cell">
              <span class="score-main" :class="scoreLevel(row.score)">{{ row.score }}</span>
              <div class="score-bar">
                <div class="score-fill" :style="{ width: row.score + '%', background: scoreColor(row.score) }"></div>
              </div>
            </div>
            <span v-else class="empty-text">—</span>
          </template>
        </el-table-column>
        <el-table-column label="评分详情" min-width="180">
          <template #default="{ row }">
            <div v-if="row.techScore !== undefined" class="score-detail">
              <span class="detail-item"><span class="detail-label">技术</span> {{ row.techScore }}</span>
              <span class="detail-item"><span class="detail-label">沟通</span> {{ row.commScore }}</span>
              <span class="detail-item"><span class="detail-label">逻辑</span> {{ row.logicScore }}</span>
            </div>
            <span v-else class="empty-text">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <div class="status-wrapper">
              <span class="status-dot" :class="row.statusCode"></span>
              <span :class="['status-text', row.statusCode]">{{ row.status }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="时长" width="70" align="center">
          <template #default="{ row }">
            <span class="duration-text">{{ row.duration }}</span>
          </template>
        </el-table-column>
        <el-table-column label="面试时间" width="110">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.startTime) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button v-if="row.statusCode === 'completed'" link type="primary" size="small" @click="handleViewReport(row)">
                <el-icon><DataAnalysis /></el-icon> 报告
              </el-button>
              <el-button v-if="row.statusCode === 'in_progress'" link type="warning" size="small" @click="handleCancel(row)">
                <el-icon><CircleClose /></el-icon> 取消
              </el-button>
              <el-button v-if="row.statusCode === 'completed' || row.statusCode === 'cancelled'" link type="success" size="small" @click="handleRetry(row)">
                <el-icon><RefreshRight /></el-icon> 重面
              </el-button>
              <el-button link type="danger" size="small" @click="handleDelete(row)">
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrap">
        <span class="page-total">共 {{ filteredData.length }} 条</span>
        <el-pagination
          background
          layout="sizes, prev, pager, next"
          :total="filteredData.length"
          :page-sizes="[10, 20, 50]"
          v-model:current-page="currentPage"
          v-model:page-size="pageSize"
          :page-size="pageSize"
        />
      </div>
    </div>

    <!-- AI 报告弹窗 -->
    <el-dialog v-model="reportVisible" title="AI 面试报告" width="560px" destroy-on-close>
      <div class="report-header">
        <div class="report-user">
          <el-avatar :size="42" :style="{ background: '#3b82f6' }">
            {{ reportTarget?.applicant?.charAt(0) }}
          </el-avatar>
          <div>
            <div class="report-name">{{ reportTarget?.applicant }}</div>
            <div class="report-job">{{ reportTarget?.jobTitle }} · {{ reportTarget?.companyName }}</div>
          </div>
        </div>
        <div class="report-score-big" :class="scoreLevel(reportTarget?.score)">
          {{ reportTarget?.score }}
        </div>
      </div>
      <el-divider />
      <div class="report-scores">
        <div class="report-score-item">
          <span class="report-label">技术能力</span>
          <el-progress :percentage="reportTarget?.techScore || 0" :color="scoreColor(reportTarget?.techScore)" />
        </div>
        <div class="report-score-item">
          <span class="report-label">沟通表达</span>
          <el-progress :percentage="reportTarget?.commScore || 0" :color="scoreColor(reportTarget?.commScore)" />
        </div>
        <div class="report-score-item">
          <span class="report-label">逻辑思维</span>
          <el-progress :percentage="reportTarget?.logicScore || 0" :color="scoreColor(reportTarget?.logicScore)" />
        </div>
      </div>
      <el-divider />
      <div class="report-summary">
        <h4>AI 评语</h4>
        <p>{{ reportTarget?.summary || '暂无评语' }}</p>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as XLSX from 'xlsx'
import {
  VideoCamera, CircleCheck, Loading, CircleClose, Star, TrendCharts,
  Search, DataAnalysis, RefreshRight, Delete, Monitor, SetUp, Connection,
  Download
} from '@element-plus/icons-vue'

// 搜索与筛选
const searchKeyword = ref('')
const filterCompany = ref('')
const filterType = ref('')
const filterStatus = ref('')
const filterDateRange = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])

const companyList = ['字节跳动', '商汤科技', '腾讯', '阿里巴巴', '华为', '微软中国', '小米', '美团']

// 模拟数据
const tableData = ref([
  { id: 1, applicant: '王五', education: '硕士', experience: '3-5年', jobTitle: '算法工程师', companyName: '商汤科技', companyInitial: '商', industry: '人工智能', round: '一面', type: 'AI面试', typeCode: 'ai', statusCode: 'completed', status: '已完成', score: 85, techScore: 88, commScore: 80, logicScore: 86, duration: '35min', startTime: '2026-06-20T14:00:00', summary: '候选人技术基础扎实，对深度学习框架有深入理解，沟通表达清晰，逻辑思维较好。建议进入二面考察项目经验。' },
  { id: 2, applicant: '吴九', education: '本科', experience: '1-3年', jobTitle: 'Python开发', companyName: '华为', companyInitial: '华', industry: '企业服务', round: '一面', type: 'AI面试', typeCode: 'ai', statusCode: 'in_progress', status: '进行中', score: null, duration: '-', startTime: '2026-06-22T10:00:00', summary: null },
  { id: 3, applicant: '张三', education: '本科', experience: '3-5年', jobTitle: '前端开发工程师', companyName: '字节跳动', companyInitial: '字', industry: '互联网', round: '二面', type: '视频面试', typeCode: 'video', statusCode: 'completed', status: '已完成', score: 72, techScore: 75, commScore: 68, logicScore: 73, duration: '42min', startTime: '2026-06-19T15:00:00', summary: '候选人前端基础良好，Vue/React 使用熟练，但系统设计能力有待提升，建议加强架构方面学习。' },
  { id: 4, applicant: '李四', education: '本科', experience: '3-5年', jobTitle: 'Java后端开发', companyName: '字节跳动', companyInitial: '字', industry: '互联网', round: '一面', type: 'AI面试', typeCode: 'ai', statusCode: 'completed', status: '已完成', score: 90, techScore: 92, commScore: 86, logicScore: 91, duration: '38min', startTime: '2026-06-18T10:00:00', summary: '候选人 Java 基础非常扎实，对 JVM、并发编程、Spring 全家桶理解深入，沟通能力强，推荐直接进入终面。' },
  { id: 5, applicant: '赵六', education: '本科', experience: '3-5年', jobTitle: '产品经理', companyName: '腾讯', companyInitial: '腾', industry: '互联网', round: '终面', type: '现场面试', typeCode: 'onsite', statusCode: 'completed', status: '已完成', score: 88, techScore: 85, commScore: 92, logicScore: 87, duration: '50min', startTime: '2026-06-17T14:00:00', summary: '候选人产品思维优秀，用户洞察力强，沟通表达流畅，具备独立负责产品线的能力。强烈推荐录用。' },
  { id: 6, applicant: '孙七', education: '本科', experience: '1-3年', jobTitle: 'UI设计师', companyName: '阿里巴巴', companyInitial: '阿', industry: '电商', round: '一面', type: '视频面试', typeCode: 'video', statusCode: 'cancelled', status: '已取消', score: null, duration: '-', startTime: '2026-06-16T11:00:00', summary: null },
  { id: 7, applicant: '郑十', education: '本科', experience: '1-3年', jobTitle: '测试工程师', companyName: '微软中国', companyInitial: '微', industry: '互联网', round: '一面', type: 'AI面试', typeCode: 'ai', statusCode: 'completed', status: '已完成', score: 65, techScore: 62, commScore: 68, logicScore: 64, duration: '30min', startTime: '2026-06-15T09:00:00', summary: '候选人测试基础一般，对自动化测试工具了解较少，沟通尚可。建议补充自动化测试知识后重新面试。' },
  { id: 8, applicant: '陈一', education: '硕士', experience: '3-5年', jobTitle: 'Go后端开发', companyName: '美团', companyInitial: '美', industry: '互联网', round: '一面', type: 'AI面试', typeCode: 'ai', statusCode: 'completed', status: '已完成', score: 82, techScore: 85, commScore: 78, logicScore: 83, duration: '40min', startTime: '2026-06-20T09:00:00', summary: '候选人 Go 语言掌握良好，对微服务架构有一定理解，沟通表达中等偏上。建议进入二面。' },
  { id: 9, applicant: '林二', education: '本科', experience: '3-5年', jobTitle: '前端开发工程师', companyName: '字节跳动', companyInitial: '字', industry: '互联网', round: '一面', type: '视频面试', typeCode: 'video', statusCode: 'in_progress', status: '进行中', score: null, duration: '-', startTime: '2026-06-22T14:00:00', summary: null },
  { id: 10, applicant: '黄三', education: '硕士', experience: '在校生', jobTitle: '算法工程师', companyName: '商汤科技', companyInitial: '商', industry: '人工智能', round: '一面', type: 'AI面试', typeCode: 'ai', statusCode: 'completed', status: '已完成', score: 78, techScore: 80, commScore: 74, logicScore: 79, duration: '33min', startTime: '2026-06-19T10:00:00', summary: '候选人论文质量不错，对深度学习理论理解较好，但工程实践经验不足。建议实习后转正。' },
  { id: 11, applicant: '刘四', education: '本科', experience: '3-5年', jobTitle: '产品经理', companyName: '腾讯', companyInitial: '腾', industry: '互联网', round: '二面', type: '现场面试', typeCode: 'onsite', statusCode: 'completed', status: '已完成', score: 92, techScore: 88, commScore: 95, logicScore: 91, duration: '55min', startTime: '2026-06-18T14:00:00', summary: '候选人综合素质优秀，产品sense强，数据驱动思维好，沟通影响力强。推荐录用，建议定级P6。' },
  { id: 12, applicant: '杨五', education: '本科', experience: '3-5年', jobTitle: 'Java后端开发', companyName: '字节跳动', companyInitial: '字', industry: '互联网', round: '终面', type: '现场面试', typeCode: 'onsite', statusCode: 'completed', status: '已完成', score: 87, techScore: 90, commScore: 84, logicScore: 86, duration: '48min', startTime: '2026-06-16T10:00:00', summary: '候选人技术深度足够，架构设计能力较强，团队协作意识好。推荐录用。' },
  { id: 13, applicant: '何六', education: '本科', experience: '1-3年', jobTitle: '运维工程师', companyName: '小米', companyInitial: '小', industry: '互联网', round: '一面', type: 'AI面试', typeCode: 'ai', statusCode: 'cancelled', status: '已取消', score: null, duration: '-', startTime: '2026-06-15T14:00:00', summary: null },
  { id: 14, applicant: '马七', education: '硕士', experience: '3-5年', jobTitle: '数据分析师', companyName: '蚂蚁集团', companyInitial: '蚂', industry: '金融', round: '一面', type: '视频面试', typeCode: 'video', statusCode: 'completed', status: '已完成', score: 76, techScore: 78, commScore: 72, logicScore: 77, duration: '36min', startTime: '2026-06-17T09:00:00', summary: '候选人数据分析能力中等，SQL 基础好，但 Python 建模经验较少。建议补充机器学习知识。' },
  { id: 15, applicant: '周一', education: '本科', experience: '1-3年', jobTitle: '测试工程师', companyName: '美团', companyInitial: '美', industry: '互联网', round: '一面', type: 'AI面试', typeCode: 'ai', statusCode: 'completed', status: '已完成', score: 70, techScore: 68, commScore: 72, logicScore: 69, duration: '28min', startTime: '2026-06-14T10:00:00', summary: '候选人测试思维一般，手工测试经验丰富但自动化能力不足。建议学习 Selenium/Playwright 后重新面试。' },
])

// 统计计算
const countByStatus = (status) => tableData.value.filter(r => r.statusCode === status).length
const avgScore = computed(() => {
  const scored = tableData.value.filter(r => r.score)
  if (!scored.length) return '-'
  return (scored.reduce((s, r) => s + r.score, 0) / scored.length).toFixed(1)
})

// 筛选后的数据
const filteredData = computed(() => {
  return tableData.value.filter(row => {
    const kw = searchKeyword.value.toLowerCase()
    const matchSearch = !kw || row.applicant.toLowerCase().includes(kw) || row.jobTitle.toLowerCase().includes(kw)
    const matchCompany = !filterCompany.value || row.companyName === filterCompany.value
    const matchType = !filterType.value || row.typeCode === filterType.value
    const matchStatus = !filterStatus.value || row.statusCode === filterStatus.value
    let matchDate = true
    if (filterDateRange.value && filterDateRange.value.length === 2) {
      const d = new Date(row.startTime).getTime()
      matchDate = d >= filterDateRange.value[0].getTime() && d <= filterDateRange.value[1].getTime() + 86400000
    }
    return matchSearch && matchCompany && matchType && matchStatus && matchDate
  })
})

// 求职者头像颜色
const avatarColor = (row) => {
  const colors = { completed: '#10b981', in_progress: '#8b5cf6', cancelled: '#94a3b8' }
  return colors[row.statusCode] || '#6b7280'
}

// 公司 logo 颜色
const companyColor = (industry) => {
  const colors = {
    '互联网': '#3b82f6', '人工智能': '#8b5cf6', '电商': '#f59e0b',
    '企业服务': '#10b981', '金融': '#ec4899', '教育': '#06b6d4', '其他': '#6b7280'
  }
  return colors[industry] || '#6b7280'
}

// 轮次标签颜色
const roundTagType = (round) => ({
  '一面': '', '二面': 'warning', '终面': 'danger'
}[round] || 'info')

// 面试类型图标
const typeIcon = (type) => ({
  ai: 'Monitor', video: 'VideoCamera', onsite: 'Connection'
}[type] || 'VideoCamera')

// 评分颜色
const scoreColor = (score) => {
  if (score >= 80) return '#10b981'
  if (score >= 60) return '#f59e0b'
  return '#ef4444'
}
const scoreLevel = (score) => {
  if (score >= 80) return 'high'
  if (score >= 60) return 'mid'
  return 'low'
}

// 日期格式化
const formatDate = (dateStr) => {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

// 搜索
const handleSearch = () => { currentPage.value = 1 }

// 重置
const handleReset = () => {
  searchKeyword.value = ''
  filterCompany.value = ''
  filterType.value = ''
  filterStatus.value = ''
  filterDateRange.value = null
  currentPage.value = 1
}

// 多选
const handleSelectionChange = (rows) => { selectedRows.value = rows }

// 查看报告
const reportVisible = ref(false)
const reportTarget = ref(null)

const handleViewReport = (row) => {
  reportTarget.value = row
  reportVisible.value = true
}

// 取消面试
const handleCancel = (row) => {
  ElMessageBox.confirm(`确定取消「${row.applicant}」的面试吗？`, '取消面试', {
    type: 'warning',
    confirmButtonText: '确认取消',
  }).then(() => {
    row.statusCode = 'cancelled'
    row.status = '已取消'
    ElMessage.warning('面试已取消')
  }).catch(() => {})
}

// 重新面试
const handleRetry = (row) => {
  ElMessageBox.confirm(`确定为「${row.applicant}」重新安排「${row.jobTitle}」的面试吗？`, '重新面试', {
    type: 'success',
    confirmButtonText: '确认安排',
  }).then(() => {
    row.statusCode = 'in_progress'
    row.status = '进行中'
    row.score = null
    row.techScore = undefined
    row.commScore = undefined
    row.logicScore = undefined
    row.duration = '-'
    row.summary = null
    ElMessage.success('面试已重新安排')
  }).catch(() => {})
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定删除「${row.applicant}」的面试记录吗？此操作不可恢复。`, '删除确认', {
    type: 'error',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  }).then(() => {
    tableData.value = tableData.value.filter(r => r.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

// 批量取消
const handleBatchCancel = () => {
  ElMessageBox.confirm(`确定取消 ${selectedRows.value.length} 场面试吗？`, '批量取消', {
    type: 'warning',
    confirmButtonText: '确认取消',
  }).then(() => {
    selectedRows.value.forEach(row => {
      if (row.statusCode === 'in_progress' || row.statusCode === 'completed') {
        row.statusCode = 'cancelled'
        row.status = '已取消'
      }
    })
    ElMessage.success('操作完成')
    selectedRows.value = []
  }).catch(() => {})
}

// 导出面试数据为 Excel
const handleExport = () => {
  const exportData = filteredData.value.map(row => ({
    'ID': row.id,
    '求职者': row.applicant,
    '学历': row.education,
    '经验': row.experience,
    '申请职位': row.jobTitle,
    '公司': row.companyName,
    '面试轮次': row.round,
    '面试类型': row.type,
    'AI评分': row.score || '',
    '技术分': row.techScore !== undefined ? row.techScore : '',
    '沟通分': row.commScore !== undefined ? row.commScore : '',
    '逻辑分': row.logicScore !== undefined ? row.logicScore : '',
    '时长': row.duration,
    '状态': row.status,
    '面试时间': formatDate(row.startTime),
    'AI评语': row.summary || '',
  }))
  const ws = XLSX.utils.json_to_sheet(exportData)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '面试列表')
  ws['!cols'] = [
    { wch: 6 }, { wch: 10 }, { wch: 8 }, { wch: 10 },
    { wch: 18 }, { wch: 12 }, { wch: 10 }, { wch: 10 },
    { wch: 10 }, { wch: 10 }, { wch: 10 }, { wch: 10 },
    { wch: 10 }, { wch: 10 }, { wch: 12 }, { wch: 40 },
  ]
  XLSX.writeFile(wb, `面试列表_${new Date().toISOString().slice(0, 10)}.xlsx`)
  ElMessage.success('导出成功')
}
</script>

<style scoped>
.page-container {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 统计卡片 */
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

.stat-icon.blue { background: #eff6ff; color: #3b82f6; }
.stat-icon.green { background: #ecfdf5; color: #10b981; }
.stat-icon.purple { background: #faf5ff; color: #8b5cf6; }
.stat-icon.red { background: #fef2f2; color: #ef4444; }
.stat-icon.orange { background: #fff7ed; color: #f59e0b; }
.stat-icon.cyan { background: #ecfeff; color: #06b6d4; }

.stat-value { font-size: 22px; font-weight: 700; color: #0f172a; line-height: 1.2; }
.stat-label { font-size: 12px; color: #94a3b8; margin-top: 2px; }

/* 筛选栏 */
.filter-card {
  background: #fff;
  border-radius: 10px;
  padding: 16px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.filter-row {
  display: flex;
  gap: 10px;
  align-items: center;
}

.filter-right {
  display: flex;
  gap: 8px;
}

/* 表格卡片 */
.table-card {
  background: #fff;
  border-radius: 10px;
  padding: 0 20px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* 求职者列 */
.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-text {
  display: flex;
  flex-direction: column;
}

.user-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  line-height: 1.3;
}

.user-edu {
  font-size: 12px;
  color: #94a3b8;
}

/* 职位列 */
.job-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.job-title {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

.job-company {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #94a3b8;
}

.mini-logo {
  width: 18px;
  height: 18px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 10px;
  font-weight: 700;
  flex-shrink: 0;
}

/* 面试类型 */
.type-badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 10px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
}

.type-badge.ai { background: #faf5ff; color: #8b5cf6; }
.type-badge.video { background: #eff6ff; color: #3b82f6; }
.type-badge.onsite { background: #ecfdf5; color: #10b981; }

/* AI 评分 */
.score-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.score-main {
  font-size: 18px;
  font-weight: 700;
  line-height: 1;
}

.score-main.high { color: #10b981; }
.score-main.mid { color: #f59e0b; }
.score-main.low { color: #ef4444; }

.score-bar {
  width: 60px;
  height: 4px;
  background: #f1f5f9;
  border-radius: 2px;
  overflow: hidden;
}

.score-fill {
  height: 100%;
  border-radius: 2px;
  transition: width 0.3s;
}

/* 评分详情 */
.score-detail {
  display: flex;
  gap: 10px;
}

.detail-item {
  font-size: 12px;
  color: #475569;
}

.detail-label {
  color: #94a3b8;
  margin-right: 2px;
}

/* 状态 */
.status-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.status-dot {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  flex-shrink: 0;
}

.status-dot.completed { background: #10b981; }
.status-dot.in_progress { background: #8b5cf6; }
.status-dot.cancelled { background: #94a3b8; }

.status-text { font-size: 13px; }
.status-text.completed { color: #10b981; }
.status-text.in_progress { color: #8b5cf6; }
.status-text.cancelled { color: #94a3b8; }

/* 时长 */
.duration-text { font-size: 13px; color: #64748b; }

/* 时间 */
.time-text { font-size: 13px; color: #64748b; }

.empty-text { font-size: 13px; color: #cbd5e1; }

/* 操作按钮 */
.action-btns {
  display: flex;
  gap: 2px;
  justify-content: center;
}

.action-btns .el-button { padding: 4px 6px; }

/* 分页 */
.pagination-wrap {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-top: 14px;
  border-top: 1px solid #f1f5f9;
  margin-top: 12px;
}

.page-total { font-size: 13px; color: #94a3b8; }

/* AI 报告弹窗 */
.report-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.report-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.report-name {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
}

.report-job {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 2px;
}

.report-score-big {
  font-size: 42px;
  font-weight: 800;
  line-height: 1;
}

.report-score-big.high { color: #10b981; }
.report-score-big.mid { color: #f59e0b; }
.report-score-big.low { color: #ef4444; }

.report-scores {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.report-score-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.report-label {
  font-size: 13px;
  color: #475569;
  width: 70px;
  flex-shrink: 0;
}

.report-summary h4 {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 8px;
}

.report-summary p {
  font-size: 14px;
  color: #475569;
  line-height: 1.7;
  margin: 0;
}

/* 响应式 */
@media (max-width: 1400px) {
  .stat-cards { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 768px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
  .filter-card { flex-direction: column; gap: 10px; align-items: stretch; }
  .filter-row { flex-wrap: wrap; }
}
</style>
