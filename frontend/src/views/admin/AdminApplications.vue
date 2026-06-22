<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue"><el-icon :size="22"><Document /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ tableData.length }}</div>
          <div class="stat-label">总申请数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><el-icon :size="22"><Clock /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ countByStatus('pending') }}</div>
          <div class="stat-label">待处理</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple"><el-icon :size="22"><VideoCamera /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ countByStatus('interviewing') }}</div>
          <div class="stat-label">面试中</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><el-icon :size="22"><CircleCheck /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ countByStatus('accepted') }}</div>
          <div class="stat-label">已录用</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red"><el-icon :size="22"><CircleClose /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ countByStatus('rejected') }}</div>
          <div class="stat-label">已拒绝</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon cyan"><el-icon :size="22"><TrendCharts /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">6</div>
          <div class="stat-label">今日新增</div>
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
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 110px" @change="handleSearch">
          <el-option label="待处理" value="pending" />
          <el-option label="已查看" value="viewed" />
          <el-option label="面试中" value="interviewing" />
          <el-option label="已录用" value="accepted" />
          <el-option label="已拒绝" value="rejected" />
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
        <el-button type="warning" plain :disabled="!selectedRows.length" @click="handleBatchMarkViewed">
          批量标记已查看 ({{ selectedRows.length }})
        </el-button>
        <el-button type="danger" plain :disabled="!selectedRows.length" @click="handleBatchReject">
          批量拒绝 ({{ selectedRows.length }})
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
        <el-table-column label="求职者" min-width="180">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="36" :style="{ background: avatarColor(row) }">
                {{ row.applicant?.charAt(0) }}
              </el-avatar>
              <div class="user-text">
                <div class="user-name">{{ row.applicant }}</div>
                <div class="user-contact">{{ row.phone }} · {{ row.email }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="申请职位" min-width="180">
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
        <el-table-column label="简历" width="100" align="center">
          <template #default="{ row }">
            <el-button v-if="row.resumeName" link type="primary" size="small" @click="handleViewResume(row)">
              <el-icon><Document /></el-icon> {{ row.resumeName }}
            </el-button>
            <span v-else class="empty-text">未上传</span>
          </template>
        </el-table-column>
        <el-table-column label="HR" width="100">
          <template #default="{ row }">
            <span v-if="row.hrName" class="hr-text">{{ row.hrName }}</span>
            <span v-else class="empty-text">未分配</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <div class="status-wrapper">
              <span class="status-dot" :class="row.statusCode"></span>
              <el-tag :type="statusTagType(row.statusCode)" size="small" effect="dark" round>
                {{ row.status }}
              </el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="110">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button v-if="row.statusCode === 'pending'" link type="primary" size="small" @click="handleMarkViewed(row)">
                <el-icon><View /></el-icon> 查看
              </el-button>
              <el-button v-if="row.statusCode === 'viewed' || row.statusCode === 'pending'" link type="success" size="small" @click="handleSchedule(row)">
                <el-icon><Calendar /></el-icon> 面试
              </el-button>
              <el-button v-if="row.statusCode === 'interviewing'" link type="success" size="small" @click="handleAccept(row)">
                <el-icon><Check /></el-icon> 录用
              </el-button>
              <el-button v-if="row.statusCode !== 'accepted' && row.statusCode !== 'rejected'" link type="danger" size="small" @click="handleReject(row)">
                <el-icon><Close /></el-icon> 拒绝
              </el-button>
              <el-button link type="primary" size="small" @click="handleViewResume(row)">
                <el-icon><Document /></el-icon> 简历
              </el-button>
              <el-button link type="info" size="small" @click="handleAddNote(row)">
                <el-icon><ChatDotRound /></el-icon> 备注
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

    <!-- 备注弹窗 -->
    <el-dialog v-model="noteVisible" title="申请备注" width="480px" destroy-on-close>
      <div class="note-target">
        <strong>{{ noteTarget?.applicant }}</strong> — {{ noteTarget?.jobTitle }}
      </div>
      <el-input
        v-model="noteText"
        type="textarea"
        :rows="4"
        placeholder="输入备注内容..."
      />
      <template #footer>
        <el-button @click="noteVisible = false">取消</el-button>
        <el-button type="primary" @click="handleNoteSubmit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as XLSX from 'xlsx'
import {
  Document, Clock, VideoCamera, CircleCheck, CircleClose, TrendCharts,
  Search, View, Calendar, Check, Close, ChatDotRound, Download
} from '@element-plus/icons-vue'

// 搜索与筛选
const searchKeyword = ref('')
const filterCompany = ref('')
const filterStatus = ref('')
const filterDateRange = ref(null)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])

const companyList = ['字节跳动', '商汤科技', '腾讯', '阿里巴巴', '好未来', '华为', '微软中国', '小米', '百度', '美团']

// 模拟数据
const tableData = ref([
  { id: 1, applicant: '张三', phone: '138****1111', email: 'zhangsan@mail.com', jobTitle: '前端开发工程师', companyName: '字节跳动', companyInitial: '字', industry: '互联网', resumeName: '张三简历.pdf', hrName: 'HR-字节', statusCode: 'pending', status: '待处理', createdAt: '2026-06-20T10:00:00' },
  { id: 2, applicant: '李四', phone: '138****2222', email: 'lisi@mail.com', jobTitle: 'Java后端开发', companyName: '字节跳动', companyInitial: '字', industry: '互联网', resumeName: '李四简历.pdf', hrName: 'HR-字节', statusCode: 'viewed', status: '已查看', createdAt: '2026-06-20T14:30:00' },
  { id: 3, applicant: '王五', phone: '138****3333', email: 'wangwu@mail.com', jobTitle: '算法工程师', companyName: '商汤科技', companyInitial: '商', industry: '人工智能', resumeName: '王五简历.pdf', hrName: 'HR-商汤', statusCode: 'interviewing', status: '面试中', createdAt: '2026-06-19T09:00:00' },
  { id: 4, applicant: '赵六', phone: '138****4444', email: 'zhaoliu@mail.com', jobTitle: '产品经理', companyName: '腾讯', companyInitial: '腾', industry: '互联网', resumeName: '赵六简历.pdf', hrName: 'HR-腾讯', statusCode: 'accepted', status: '已录用', createdAt: '2026-06-18T16:00:00' },
  { id: 5, applicant: '孙七', phone: '138****5555', email: 'sunqi@mail.com', jobTitle: 'UI设计师', companyName: '阿里巴巴', companyInitial: '阿', industry: '电商', resumeName: null, hrName: 'HR-阿里', statusCode: 'rejected', status: '已拒绝', createdAt: '2026-06-17T11:00:00' },
  { id: 6, applicant: '周八', phone: '138****6666', email: 'zhouba@mail.com', jobTitle: '数据分析实习生', companyName: '好未来', companyInitial: '好', industry: '教育', resumeName: '周八简历.pdf', hrName: null, statusCode: 'pending', status: '待处理', createdAt: '2026-06-21T08:00:00' },
  { id: 7, applicant: '吴九', phone: '138****7777', email: 'wujiu@mail.com', jobTitle: 'Python开发', companyName: '华为', companyInitial: '华', industry: '企业服务', resumeName: '吴九简历.pdf', hrName: 'HR-华为', statusCode: 'interviewing', status: '面试中', createdAt: '2026-06-20T15:00:00' },
  { id: 8, applicant: '郑十', phone: '138****8888', email: 'zhengshi@mail.com', jobTitle: '测试工程师', companyName: '微软中国', companyInitial: '微', industry: '互联网', resumeName: '郑十简历.pdf', hrName: 'HR-微软', statusCode: 'viewed', status: '已查看', createdAt: '2026-06-19T13:30:00' },
  { id: 9, applicant: '陈一', phone: '138****9001', email: 'chenyi@mail.com', jobTitle: 'Go后端开发', companyName: '美团', companyInitial: '美', industry: '互联网', resumeName: '陈一简历.pdf', hrName: null, statusCode: 'pending', status: '待处理', createdAt: '2026-06-21T09:30:00' },
  { id: 10, applicant: '林二', phone: '138****9002', email: 'liner@mail.com', jobTitle: '前端开发工程师', companyName: '字节跳动', companyInitial: '字', industry: '互联网', resumeName: '林二简历.pdf', hrName: 'HR-字节', statusCode: 'viewed', status: '已查看', createdAt: '2026-06-20T11:00:00' },
  { id: 11, applicant: '黄三', phone: '138****9003', email: 'huangsan@mail.com', jobTitle: '算法工程师', companyName: '商汤科技', companyInitial: '商', industry: '人工智能', resumeName: null, hrName: null, statusCode: 'pending', status: '待处理', createdAt: '2026-06-21T10:00:00' },
  { id: 12, applicant: '刘四', phone: '138****9004', email: 'liusi@mail.com', jobTitle: '产品经理', companyName: '腾讯', companyInitial: '腾', industry: '互联网', resumeName: '刘四简历.pdf', hrName: 'HR-腾讯', statusCode: 'interviewing', status: '面试中', createdAt: '2026-06-19T14:00:00' },
  { id: 13, applicant: '杨五', phone: '138****9005', email: 'yangwu@mail.com', jobTitle: 'Java后端开发', companyName: '字节跳动', companyInitial: '字', industry: '互联网', resumeName: '杨五简历.pdf', hrName: 'HR-字节', statusCode: 'accepted', status: '已录用', createdAt: '2026-06-16T09:00:00' },
  { id: 14, applicant: '何六', phone: '138****9006', email: 'heliu@mail.com', jobTitle: '运维工程师', companyName: '小米', companyInitial: '小', industry: '互联网', resumeName: '何六简历.pdf', hrName: null, statusCode: 'rejected', status: '已拒绝', createdAt: '2026-06-15T10:00:00' },
  { id: 15, applicant: '马七', phone: '138****9007', email: 'maqi@mail.com', jobTitle: '数据分析师', companyName: '蚂蚁集团', companyInitial: '蚂', industry: '金融', resumeName: '马七简历.pdf', hrName: 'HR-蚂蚁', statusCode: 'viewed', status: '已查看', createdAt: '2026-06-20T16:00:00' },
])

// 状态计数
const countByStatus = (status) => tableData.value.filter(r => r.statusCode === status).length

// 筛选后的数据
const filteredData = computed(() => {
  return tableData.value.filter(row => {
    const kw = searchKeyword.value.toLowerCase()
    const matchSearch = !kw || row.applicant.toLowerCase().includes(kw) || row.jobTitle.toLowerCase().includes(kw)
    const matchCompany = !filterCompany.value || row.companyName === filterCompany.value
    const matchStatus = !filterStatus.value || row.statusCode === filterStatus.value
    let matchDate = true
    if (filterDateRange.value && filterDateRange.value.length === 2) {
      const d = new Date(row.createdAt).getTime()
      matchDate = d >= filterDateRange.value[0].getTime() && d <= filterDateRange.value[1].getTime() + 86400000
    }
    return matchSearch && matchCompany && matchStatus && matchDate
  })
})

// 求职者头像颜色
const avatarColor = (row) => {
  const colors = { pending: '#f59e0b', viewed: '#3b82f6', interviewing: '#8b5cf6', accepted: '#10b981', rejected: '#ef4444' }
  return colors[row.statusCode] || '#6b7280'
}

// 公司 logo 颜色
const companyColor = (industry) => {
  const colors = {
    '互联网': '#3b82f6', '人工智能': '#8b5cf6', '电商': '#f59e0b',
    '企业服务': '#10b981', '金融': '#ec4899', '教育': '#06b6d4',
    '游戏': '#ef4444', '医疗': '#14b8a6', '其他': '#6b7280'
  }
  return colors[industry] || '#6b7280'
}

// 状态标签颜色
const statusTagType = (s) => ({
  pending: 'warning', viewed: 'info', interviewing: '', accepted: 'success', rejected: 'danger'
}[s] || 'info')

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
  filterStatus.value = ''
  filterDateRange.value = null
  currentPage.value = 1
}

// 多选
const handleSelectionChange = (rows) => { selectedRows.value = rows }

// 标记已查看
const handleMarkViewed = (row) => {
  row.statusCode = 'viewed'
  row.status = '已查看'
  ElMessage.success(`已标记「${row.applicant}」的申请为已查看`)
}

// 安排面试
const handleSchedule = (row) => {
  ElMessageBox.confirm(`确定为「${row.applicant}」安排「${row.jobTitle}」的面试吗？`, '安排面试', {
    type: 'success',
    confirmButtonText: '确认安排',
  }).then(() => {
    row.statusCode = 'interviewing'
    row.status = '面试中'
    ElMessage.success('面试已安排')
  }).catch(() => {})
}

// 录用
const handleAccept = (row) => {
  ElMessageBox.confirm(`确定录用「${row.applicant}」吗？`, '录用确认', {
    type: 'success',
    confirmButtonText: '确认录用',
  }).then(() => {
    row.statusCode = 'accepted'
    row.status = '已录用'
    ElMessage.success('录用成功')
  }).catch(() => {})
}

// 拒绝
const handleReject = (row) => {
  ElMessageBox.confirm(`确定拒绝「${row.applicant}」的申请吗？`, '拒绝确认', {
    type: 'warning',
    confirmButtonText: '确认拒绝',
  }).then(() => {
    row.statusCode = 'rejected'
    row.status = '已拒绝'
    ElMessage.warning('已拒绝')
  }).catch(() => {})
}

// 查看简历
const handleViewResume = (row) => {
  if (row.resumeName) {
    ElMessage.info(`查看「${row.applicant}」的简历：${row.resumeName}（开发中）`)
  } else {
    ElMessage.warning('该求职者未上传简历')
  }
}

// 备注
const noteVisible = ref(false)
const noteTarget = ref(null)
const noteText = ref('')

const handleAddNote = (row) => {
  noteTarget.value = row
  noteText.value = row.note || ''
  noteVisible.value = true
}

const handleNoteSubmit = () => {
  if (noteTarget.value) {
    noteTarget.value.note = noteText.value
  }
  noteVisible.value = false
  ElMessage.success('备注已保存')
}

// 批量标记已查看
const handleBatchMarkViewed = () => {
  ElMessageBox.confirm(`确定将 ${selectedRows.value.length} 条申请标记为已查看吗？`, '批量操作', {
    type: 'warning',
    confirmButtonText: '确认',
  }).then(() => {
    selectedRows.value.forEach(row => {
      if (row.statusCode === 'pending') {
        row.statusCode = 'viewed'
        row.status = '已查看'
      }
    })
    ElMessage.success('操作完成')
    selectedRows.value = []
  }).catch(() => {})
}

// 批量拒绝
const handleBatchReject = () => {
  ElMessageBox.confirm(`确定拒绝 ${selectedRows.value.length} 条申请吗？`, '批量拒绝', {
    type: 'error',
    confirmButtonText: '确认拒绝',
  }).then(() => {
    selectedRows.value.forEach(row => {
      if (row.statusCode !== 'accepted') {
        row.statusCode = 'rejected'
        row.status = '已拒绝'
      }
    })
    ElMessage.success('操作完成')
    selectedRows.value = []
  }).catch(() => {})
}

// 导出申请数据为 Excel
const handleExport = () => {
  const exportData = filteredData.value.map(row => ({
    'ID': row.id,
    '求职者': row.applicant,
    '手机号': row.phone,
    '邮箱': row.email,
    '申请职位': row.jobTitle,
    '公司': row.companyName,
    '简历': row.resumeName || '未上传',
    'HR': row.hrName || '未分配',
    '状态': row.status,
    '备注': row.note || '',
    '申请时间': formatDate(row.createdAt),
  }))
  const ws = XLSX.utils.json_to_sheet(exportData)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '申请列表')
  ws['!cols'] = [
    { wch: 6 }, { wch: 10 }, { wch: 14 }, { wch: 20 },
    { wch: 20 }, { wch: 12 }, { wch: 16 }, { wch: 10 },
    { wch: 10 }, { wch: 20 }, { wch: 12 },
  ]
  XLSX.writeFile(wb, `申请列表_${new Date().toISOString().slice(0, 10)}.xlsx`)
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
.stat-icon.orange { background: #fff7ed; color: #f59e0b; }
.stat-icon.purple { background: #faf5ff; color: #8b5cf6; }
.stat-icon.red { background: #fef2f2; color: #ef4444; }
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

.user-contact {
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

/* HR */
.hr-text { font-size: 13px; color: #475569; }
.empty-text { font-size: 13px; color: #cbd5e1; }

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

.status-dot.pending { background: #f59e0b; }
.status-dot.viewed { background: #3b82f6; }
.status-dot.interviewing { background: #8b5cf6; }
.status-dot.accepted { background: #10b981; }
.status-dot.rejected { background: #ef4444; }

/* 时间 */
.time-text { font-size: 13px; color: #64748b; }

/* 操作按钮 */
.action-btns {
  display: flex;
  gap: 2px;
  justify-content: center;
  flex-wrap: wrap;
}

.action-btns .el-button { padding: 4px 5px; }

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

/* 备注弹窗 */
.note-target {
  margin-bottom: 12px;
  font-size: 14px;
  color: #334155;
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
