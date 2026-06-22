<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue"><el-icon :size="22"><Suitcase /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ tableData.length }}</div>
          <div class="stat-label">总职位数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><el-icon :size="22"><CircleCheck /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ tableData.filter(j => j.status === 'active').length }}</div>
          <div class="stat-label">已发布</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><el-icon :size="22"><Clock /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ tableData.filter(j => j.status === 'pending').length }}</div>
          <div class="stat-label">待审核</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple"><el-icon :size="22"><TrendCharts /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">5</div>
          <div class="stat-label">今日新增</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red"><el-icon :size="22"><Document /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ totalApps }}</div>
          <div class="stat-label">总申请数</div>
        </div>
      </div>
    </div>

    <!-- 搜索与筛选 -->
    <div class="filter-card">
      <div class="filter-row">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索职位名称"
          clearable
          prefix-icon="Search"
          style="width: 200px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="filterCompany" placeholder="公司" clearable style="width: 130px" @change="handleSearch">
          <el-option v-for="c in companyList" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="filterCity" placeholder="城市" clearable style="width: 100px" @change="handleSearch">
          <el-option v-for="c in cityList" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="filterJobType" placeholder="类型" clearable style="width: 100px" @change="handleSearch">
          <el-option label="全职" value="全职" />
          <el-option label="兼职" value="兼职" />
          <el-option label="实习" value="实习" />
        </el-select>
        <el-select v-model="filterEducation" placeholder="学历" clearable style="width: 110px" @change="handleSearch">
          <el-option label="大专" value="大专" />
          <el-option label="本科" value="本科" />
          <el-option label="硕士" value="硕士" />
          <el-option label="博士" value="博士" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 110px" @change="handleSearch">
          <el-option label="已发布" value="active" />
          <el-option label="待审核" value="pending" />
          <el-option label="已关闭" value="closed" />
          <el-option label="审核不通过" value="rejected" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <div class="filter-right">
        <el-button type="warning" plain @click="handleExport">
          <el-icon><Download /></el-icon> 导出
        </el-button>
        <el-button type="success" plain @click="importVisible = true">
          <el-icon><Upload /></el-icon> 导入
        </el-button>
        <el-button type="success" plain :disabled="!selectedRows.length || !hasPending" @click="handleBatchApprove">
          批量通过 ({{ pendingSelectedCount }})
        </el-button>
        <el-button type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDelete">
          批量删除 ({{ selectedRows.length }})
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
        <el-table-column label="职位信息" min-width="200">
          <template #default="{ row }">
            <div class="job-cell">
              <div class="job-icon" :style="{ background: jobTypeBg(row.jobType), color: jobTypeColor(row.jobType) }">
                <el-icon :size="16"><Suitcase /></el-icon>
              </div>
              <div class="job-text">
                <div class="job-title">{{ row.title }}</div>
                <div class="job-tags">
                  <el-tag size="small" :type="jobTypeTagType(row.jobType)" effect="dark" round>{{ row.jobType }}</el-tag>
                  <el-tag v-if="row.experience" size="small" type="info" effect="plain" round>{{ row.experience }}</el-tag>
                </div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="公司" width="140">
          <template #default="{ row }">
            <div class="company-cell">
              <div class="company-logo" :style="{ background: companyColor(row.industry) }">
                {{ row.companyInitial }}
              </div>
              <span class="company-name">{{ row.companyName }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="city" label="城市" width="80" />
        <el-table-column label="薪资" width="100">
          <template #default="{ row }">
            <span class="salary-text">{{ row.salary }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="education" label="学历" width="70" align="center">
          <template #default="{ row }">
            <span class="edu-text">{{ row.education }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="appCount" label="申请数" width="80" align="center" sortable />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)" size="small" effect="dark" round>
              {{ row.statusLabel }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="110">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.publishedAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button v-if="row.status === 'pending'" link type="success" size="small" @click="handleApprove(row)">
                <el-icon><Check /></el-icon> 通过
              </el-button>
              <el-button v-if="row.status === 'pending'" link type="warning" size="small" @click="handleReject(row)">
                <el-icon><Close /></el-icon> 驳回
              </el-button>
              <el-button link type="primary" size="small" @click="handleEdit(row)">
                <el-icon><EditPen /></el-icon> 编辑
              </el-button>
              <el-button v-if="row.status === 'active'" link type="warning" size="small" @click="handleDisable(row)">
                <el-icon><Hide /></el-icon> 下架
              </el-button>
              <el-button link type="primary" size="small" @click="handleViewApps(row)">
                <el-icon><Document /></el-icon> 申请
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

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑职位" width="560px" destroy-on-close>
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="职位名称">
          <el-input v-model="editForm.title" placeholder="请输入职位名称" />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="editForm.city" placeholder="请输入城市" />
        </el-form-item>
        <el-form-item label="薪资">
          <el-input v-model="editForm.salary" placeholder="例：20-35K" />
        </el-form-item>
        <el-form-item label="职位类型">
          <el-select v-model="editForm.jobType" style="width: 100%">
            <el-option label="全职" value="全职" />
            <el-option label="兼职" value="兼职" />
            <el-option label="实习" value="实习" />
          </el-select>
        </el-form-item>
        <el-form-item label="学历要求">
          <el-select v-model="editForm.education" style="width: 100%">
            <el-option label="大专" value="大专" />
            <el-option label="本科" value="本科" />
            <el-option label="硕士" value="硕士" />
            <el-option label="博士" value="博士" />
          </el-select>
        </el-form-item>
        <el-form-item label="经验要求">
          <el-input v-model="editForm.experience" placeholder="例：3-5年" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 导入弹窗 -->
    <el-dialog v-model="importVisible" title="导入职位" width="520px" destroy-on-close>
      <div class="import-body">
        <div class="import-tip">
          <el-icon :size="18"><InfoFilled /></el-icon>
          <span>支持 .xlsx / .xls / .csv 格式，单次最多导入 500 条</span>
        </div>
        <el-upload
          ref="uploadRef"
          drag
          :auto-upload="false"
          :limit="1"
          :on-change="handleFileChange"
          :on-exceed="handleExceed"
          accept=".xlsx,.xls,.csv"
        >
          <el-icon class="el-icon--upload" :size="48" color="#d1d5db"><Upload /></el-icon>
          <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
          <template #tip>
            <div class="el-upload__tip">仅支持 xlsx / xls / csv 格式文件</div>
          </template>
        </el-upload>
        <div class="import-actions">
          <el-button @click="handleDownloadTemplate">
            <el-icon><Download /></el-icon> 下载导入模板
          </el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" @click="handleImportSubmit">确认导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as XLSX from 'xlsx'
import {
  Suitcase, CircleCheck, Clock, TrendCharts, Document,
  Search, Check, Close, EditPen, Hide, Delete,
  Download, Upload, InfoFilled
} from '@element-plus/icons-vue'

const router = useRouter()

// 搜索与筛选
const searchKeyword = ref('')
const filterCompany = ref('')
const filterCity = ref('')
const filterJobType = ref('')
const filterEducation = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])

const companyList = ['字节跳动', '阿里巴巴', '腾讯', '华为', '百度', '小米', '商汤科技', '好未来', '微软中国', '美团', '网易', '京东', '蚂蚁集团', '拼多多']
const cityList = ['北京', '上海', '广州', '深圳', '杭州', '成都', '武汉', '南京']

// 模拟数据
const tableData = ref([
  { id: 1, title: '前端开发工程师', companyName: '字节跳动', companyInitial: '字', industry: '互联网', city: '北京', salary: '20-35K', jobType: '全职', education: '本科', experience: '3-5年', appCount: 24, status: 'active', statusLabel: '已发布', publishedAt: '2026-06-15T10:00:00' },
  { id: 2, title: 'Java后端开发', companyName: '字节跳动', companyInitial: '字', industry: '互联网', city: '北京', salary: '25-40K', jobType: '全职', education: '本科', experience: '3-5年', appCount: 18, status: 'active', statusLabel: '已发布', publishedAt: '2026-06-15T11:00:00' },
  { id: 3, title: '算法工程师', companyName: '商汤科技', companyInitial: '商', industry: '人工智能', city: '上海', salary: '30-50K', jobType: '全职', education: '硕士', experience: '3-5年', appCount: 12, status: 'active', statusLabel: '已发布', publishedAt: '2026-06-14T09:00:00' },
  { id: 4, title: '产品经理', companyName: '腾讯', companyInitial: '腾', industry: '互联网', city: '深圳', salary: '25-40K', jobType: '全职', education: '本科', experience: '3-5年', appCount: 30, status: 'active', statusLabel: '已发布', publishedAt: '2026-06-13T14:00:00' },
  { id: 5, title: '数据分析实习生', companyName: '好未来', companyInitial: '好', industry: '教育', city: '北京', salary: '3-5K', jobType: '实习', education: '本科', experience: '在校生', appCount: 8, status: 'pending', statusLabel: '待审核', publishedAt: '2026-06-20T09:00:00' },
  { id: 6, title: 'UI设计师', companyName: '阿里巴巴', companyInitial: '阿', industry: '电商', city: '杭州', salary: '18-30K', jobType: '全职', education: '本科', experience: '1-3年', appCount: 15, status: 'active', statusLabel: '已发布', publishedAt: '2026-06-12T10:00:00' },
  { id: 7, title: 'Python开发', companyName: '华为', companyInitial: '华', industry: '企业服务', city: '深圳', salary: '20-35K', jobType: '全职', education: '本科', experience: '1-3年', appCount: 9, status: 'pending', statusLabel: '待审核', publishedAt: '2026-06-21T08:00:00' },
  { id: 8, title: '市场运营专员', companyName: '小米', companyInitial: '小', industry: '互联网', city: '北京', salary: '15-25K', jobType: '全职', education: '本科', experience: '1-3年', appCount: 6, status: 'rejected', statusLabel: '审核不通过', publishedAt: '2026-06-19T10:00:00' },
  { id: 9, title: '前端兼职开发', companyName: '百度', companyInitial: '百', industry: '人工智能', city: '北京', salary: '150-200/天', jobType: '兼职', education: '本科', experience: '1-3年', appCount: 3, status: 'closed', statusLabel: '已关闭', publishedAt: '2026-06-01T09:00:00' },
  { id: 10, title: '测试工程师', companyName: '微软中国', companyInitial: '微', industry: '互联网', city: '北京', salary: '18-30K', jobType: '全职', education: '本科', experience: '1-3年', appCount: 11, status: 'active', statusLabel: '已发布', publishedAt: '2026-06-11T14:00:00' },
  { id: 11, title: 'Go后端开发', companyName: '美团', companyInitial: '美', industry: '互联网', city: '北京', salary: '25-45K', jobType: '全职', education: '本科', experience: '3-5年', appCount: 14, status: 'active', statusLabel: '已发布', publishedAt: '2026-06-16T09:00:00' },
  { id: 12, title: 'AI算法实习生', companyName: '商汤科技', companyInitial: '商', industry: '人工智能', city: '上海', salary: '4-6K', jobType: '实习', education: '硕士', experience: '在校生', appCount: 7, status: 'active', statusLabel: '已发布', publishedAt: '2026-06-17T10:00:00' },
  { id: 13, title: '游戏策划', companyName: '网易', companyInitial: '网', industry: '游戏', city: '杭州', salary: '15-25K', jobType: '全职', education: '本科', experience: '1-3年', appCount: 10, status: 'pending', statusLabel: '待审核', publishedAt: '2026-06-21T14:00:00' },
  { id: 14, title: '数据分析师', companyName: '蚂蚁集团', companyInitial: '蚂', industry: '金融', city: '杭州', salary: '20-35K', jobType: '全职', education: '硕士', experience: '3-5年', appCount: 16, status: 'active', statusLabel: '已发布', publishedAt: '2026-06-14T11:00:00' },
  { id: 15, title: '运维工程师', companyName: '京东', companyInitial: '京', industry: '电商', city: '北京', salary: '18-30K', jobType: '全职', education: '本科', experience: '3-5年', appCount: 5, status: 'active', statusLabel: '已发布', publishedAt: '2026-06-10T09:00:00' },
])

// 统计计算
const totalApps = computed(() => tableData.value.reduce((sum, j) => sum + j.appCount, 0))

// 筛选后的数据
const filteredData = computed(() => {
  return tableData.value.filter(row => {
    const kw = searchKeyword.value.toLowerCase()
    const matchSearch = !kw || row.title.toLowerCase().includes(kw)
    const matchCompany = !filterCompany.value || row.companyName === filterCompany.value
    const matchCity = !filterCity.value || row.city === filterCity.value
    const matchJobType = !filterJobType.value || row.jobType === filterJobType.value
    const matchEducation = !filterEducation.value || row.education === filterEducation.value
    const matchStatus = !filterStatus.value || row.status === filterStatus.value
    return matchSearch && matchCompany && matchCity && matchJobType && matchEducation && matchStatus
  })
})

// 批量操作相关
const hasPending = computed(() => selectedRows.value.some(r => r.status === 'pending'))
const pendingSelectedCount = computed(() => selectedRows.value.filter(r => r.status === 'pending').length)

// 职位类型样式
const jobTypeBg = (type) => ({ '全职': '#eff6ff', '兼职': '#fffbeb', '实习': '#faf5ff' }[type] || '#f1f5f9')
const jobTypeColor = (type) => ({ '全职': '#3b82f6', '兼职': '#f59e0b', '实习': '#8b5cf6' }[type] || '#6b7280')
const jobTypeTagType = (type) => ({ '全职': 'primary', '兼职': 'warning', '实习': '' }[type] || 'info')

// 公司 logo 颜色
const companyColor = (industry) => {
  const colors = {
    '互联网': '#3b82f6', '人工智能': '#8b5cf6', '电商': '#f59e0b',
    '企业服务': '#10b981', '金融': '#ec4899', '教育': '#06b6d4',
    '游戏': '#ef4444', '医疗': '#14b8a6', '大数据': '#6366f1', '其他': '#6b7280'
  }
  return colors[industry] || '#6b7280'
}

// 状态标签颜色
const statusTagType = (s) => ({
  active: 'success', pending: 'warning', closed: 'info', rejected: 'danger'
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
  filterCity.value = ''
  filterJobType.value = ''
  filterEducation.value = ''
  filterStatus.value = ''
  currentPage.value = 1
}

// 多选
const handleSelectionChange = (rows) => { selectedRows.value = rows }

// 通过审核
const handleApprove = (row) => {
  ElMessageBox.confirm(`确定通过职位「${row.title}」的审核吗？`, '审核确认', {
    type: 'success',
    confirmButtonText: '通过',
  }).then(() => {
    row.status = 'active'
    row.statusLabel = '已发布'
    ElMessage.success('审核通过')
  }).catch(() => {})
}

// 驳回
const handleReject = (row) => {
  ElMessageBox.confirm(`确定驳回职位「${row.title}」吗？`, '驳回确认', {
    type: 'warning',
    confirmButtonText: '驳回',
  }).then(() => {
    row.status = 'rejected'
    row.statusLabel = '审核不通过'
    ElMessage.warning('已驳回')
  }).catch(() => {})
}

// 编辑
const editVisible = ref(false)
const editForm = ref({})

const handleEdit = (row) => {
  editForm.value = { ...row }
  editVisible.value = true
}

const handleEditSubmit = () => {
  const idx = tableData.value.findIndex(j => j.id === editForm.value.id)
  if (idx !== -1) {
    tableData.value[idx] = { ...tableData.value[idx], ...editForm.value }
  }
  editVisible.value = false
  ElMessage.success('编辑成功')
}

// 下架
const handleDisable = (row) => {
  ElMessageBox.confirm(`确定下架职位「${row.title}」吗？`, '下架确认', {
    type: 'warning',
    confirmButtonText: '下架',
  }).then(() => {
    row.status = 'closed'
    row.statusLabel = '已关闭'
    ElMessage.success('已下架')
  }).catch(() => {})
}

// 查看申请
const handleViewApps = (row) => {
  ElMessage.info(`查看「${row.title}」的申请列表（开发中）`)
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除职位「${row.title}」吗？此操作不可恢复。`, '删除确认', {
    type: 'error',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  }).then(() => {
    tableData.value = tableData.value.filter(j => j.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

// 批量通过
const handleBatchApprove = () => {
  const pendingRows = selectedRows.value.filter(r => r.status === 'pending')
  const names = pendingRows.map(r => r.title).join('、')
  ElMessageBox.confirm(`确定通过以下待审核职位吗？\n${names}`, '批量审核', {
    type: 'success',
    confirmButtonText: '全部通过',
  }).then(() => {
    pendingRows.forEach(row => { row.status = 'active'; row.statusLabel = '已发布' })
    ElMessage.success(`已通过 ${pendingRows.length} 个职位`)
    selectedRows.value = []
  }).catch(() => {})
}

// 批量删除
const handleBatchDelete = () => {
  const names = selectedRows.value.map(r => r.title).join('、')
  ElMessageBox.confirm(`确定要删除以下职位吗？此操作不可恢复。\n${names}`, '批量删除', {
    type: 'error',
    confirmButtonText: '确认删除',
  }).then(() => {
    const ids = new Set(selectedRows.value.map(r => r.id))
    tableData.value = tableData.value.filter(j => !ids.has(j.id))
    ElMessage.success(`已删除 ${ids.size} 个职位`)
    selectedRows.value = []
  }).catch(() => {})
}

// ===== 导入导出 =====
const importVisible = ref(false)
const uploadRef = ref()
const importFile = ref(null)

// 导出职位数据为 Excel
const handleExport = () => {
  const exportData = filteredData.value.map(row => ({
    'ID': row.id,
    '职位名称': row.title,
    '公司': row.companyName,
    '城市': row.city,
    '薪资': row.salary,
    '职位类型': row.jobType,
    '学历要求': row.education,
    '经验要求': row.experience,
    '申请数': row.appCount,
    '状态': row.statusLabel,
    '发布时间': formatDate(row.publishedAt),
  }))
  const ws = XLSX.utils.json_to_sheet(exportData)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '职位列表')
  ws['!cols'] = [
    { wch: 6 }, { wch: 20 }, { wch: 12 }, { wch: 8 },
    { wch: 12 }, { wch: 8 }, { wch: 10 }, { wch: 10 },
    { wch: 8 }, { wch: 10 }, { wch: 12 },
  ]
  XLSX.writeFile(wb, `职位列表_${new Date().toISOString().slice(0, 10)}.xlsx`)
  ElMessage.success('导出成功')
}

// 下载导入模板
const handleDownloadTemplate = () => {
  const templateData = [
    { '职位名称': '示例职位', '公司名称': '示例公司', '城市': '北京', '薪资': '20-35K', '职位类型': '全职', '学历要求': '本科', '经验要求': '1-3年' },
  ]
  const ws = XLSX.utils.json_to_sheet(templateData)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '导入模板')
  ws['!cols'] = [{ wch: 16 }, { wch: 16 }, { wch: 8 }, { wch: 12 }, { wch: 8 }, { wch: 10 }, { wch: 10 }]
  XLSX.writeFile(wb, '职位导入模板.xlsx')
  ElMessage.success('模板下载成功')
}

// 文件选择
const handleFileChange = (file) => {
  importFile.value = file
}

// 超出数量限制
const handleExceed = () => {
  ElMessage.warning('只能上传一个文件')
}

// 确认导入
const handleImportSubmit = () => {
  if (!importFile.value) {
    ElMessage.warning('请先选择文件')
    return
  }
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const data = new Uint8Array(e.target.result)
      const workbook = XLSX.read(data, { type: 'array' })
      const sheetName = workbook.SheetNames[0]
      const worksheet = workbook.Sheets[sheetName]
      const jsonData = XLSX.utils.sheet_to_json(worksheet)

      if (jsonData.length === 0) {
        ElMessage.warning('文件中没有数据')
        return
      }
      if (jsonData.length > 500) {
        ElMessage.warning('单次最多导入 500 条')
        return
      }

      let addedCount = 0
      jsonData.forEach(row => {
        const title = row['职位名称'] || row['title']
        const companyName = row['公司名称'] || row['companyName']
        if (!title) return
        tableData.value.push({
          id: Date.now() + addedCount,
          title,
          companyName: companyName || '未知公司',
          companyInitial: (companyName || '未').charAt(0),
          industry: '其他',
          city: row['城市'] || row['city'] || '',
          salary: row['薪资'] || row['salary'] || '',
          jobType: row['职位类型'] || row['jobType'] || '全职',
          education: row['学历要求'] || row['education'] || '本科',
          experience: row['经验要求'] || row['experience'] || '',
          appCount: 0,
          status: 'pending',
          statusLabel: '待审核',
          publishedAt: new Date().toISOString(),
        })
        addedCount++
      })

      importVisible.value = false
      importFile.value = null
      if (uploadRef.value) uploadRef.value.clearFiles()
      ElMessage.success(`成功导入 ${addedCount} 条职位数据`)
    } catch {
      ElMessage.error('文件解析失败，请检查文件格式')
    }
  }
  reader.readAsArrayBuffer(importFile.value.raw)
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
  grid-template-columns: repeat(5, 1fr);
  gap: 14px;
}

.stat-card {
  background: #fff;
  border-radius: 10px;
  padding: 18px 20px;
  display: flex;
  align-items: center;
  gap: 14px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

.stat-icon {
  width: 44px;
  height: 44px;
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

/* 职位信息列 */
.job-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.job-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.job-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.job-title {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  line-height: 1.3;
}

.job-tags {
  display: flex;
  gap: 4px;
}

/* 公司列 */
.company-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}

.company-logo {
  width: 28px;
  height: 28px;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  flex-shrink: 0;
}

.company-name {
  font-size: 13px;
  color: #334155;
}

/* 薪资 */
.salary-text {
  font-size: 13px;
  font-weight: 600;
  color: #ef4444;
}

/* 学历 */
.edu-text {
  font-size: 13px;
  color: #475569;
}

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

/* 导入弹窗 */
.import-body { padding: 8px 0; }

.import-tip {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 14px;
  background: #f0f9ff;
  border-radius: 8px;
  color: #0369a1;
  font-size: 13px;
  margin-bottom: 16px;
}

.import-actions {
  display: flex;
  justify-content: center;
  margin-top: 12px;
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
