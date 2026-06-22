<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue"><el-icon :size="22"><PriceTag /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ tableData.length }}</div>
          <div class="stat-label">总标签数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><el-icon :size="22"><OfficeBuilding /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ totalCompanyUsage }}</div>
          <div class="stat-label">公司使用次数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><el-icon :size="22"><Suitcase /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ totalJobUsage }}</div>
          <div class="stat-label">职位使用次数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple"><el-icon :size="22"><TrendCharts /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ mostPopular }}</div>
          <div class="stat-label">最热门标签</div>
        </div>
      </div>
    </div>

    <!-- 搜索与筛选 -->
    <div class="filter-card">
      <div class="filter-row">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索标签名称"
          clearable
          prefix-icon="Search"
          style="width: 220px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 100px" @change="handleSearch">
          <el-option label="启用" value="active" />
          <el-option label="禁用" value="disabled" />
        </el-select>
        <el-select v-model="sortBy" placeholder="排序" style="width: 130px" @change="handleSearch">
          <el-option label="默认排序" value="default" />
          <el-option label="使用量降序" value="usage_desc" />
          <el-option label="使用量升序" value="usage_asc" />
          <el-option label="名称排序" value="name" />
          <el-option label="创建时间" value="created" />
        </el-select>
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
        <el-button @click="handleReset">重置</el-button>
      </div>
      <div class="filter-right">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增标签
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
        <el-table-column label="标签预览" width="160">
          <template #default="{ row }">
            <div class="tag-preview" :style="{ background: row.bgColor, color: row.color, borderColor: row.color }">
              {{ row.name }}
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="标签名称" min-width="130">
          <template #default="{ row }">
            <span class="tag-name">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column label="使用量" min-width="200">
          <template #default="{ row }">
            <div class="usage-cell">
              <div class="usage-bar-wrap">
                <div class="usage-bar" :style="{ width: usagePercent(row) + '%', background: row.color }"></div>
              </div>
              <span class="usage-text">公司 {{ row.companyCount }} · 职位 {{ row.jobCount }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="companyCount" label="使用公司" width="90" align="center" sortable />
        <el-table-column prop="jobCount" label="使用职位" width="90" align="center" sortable />
        <el-table-column prop="sort" label="权重" width="70" align="center" />
        <el-table-column label="状态" width="80" align="center">
          <template #default="{ row }">
            <span class="status-dot" :class="row.status"></span>
            <span :class="['status-text', row.status]">{{ row.status === 'active' ? '启用' : '禁用' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="110">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button link type="primary" size="small" @click="handleEdit(row)">
                <el-icon><EditPen /></el-icon> 编辑
              </el-button>
              <el-button link type="primary" size="small" @click="handleViewUsage(row)">
                <el-icon><DataAnalysis /></el-icon> 使用
              </el-button>
              <el-button
                link :type="row.status === 'active' ? 'warning' : 'success'" size="small"
                @click="handleToggleStatus(row)"
              >
                <el-icon><component :is="row.status === 'active' ? 'Lock' : 'Unlock'" /></el-icon>
                {{ row.status === 'active' ? '禁用' : '启用' }}
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑标签' : '新增标签'" width="480px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="标签名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入标签名称" />
        </el-form-item>
        <el-form-item label="标签颜色">
          <div class="color-picker-row">
            <div
              v-for="c in colorOptions"
              :key="c.name"
              class="color-option"
              :class="{ active: form.color === c.name }"
              :style="{ background: c.bg, color: c.fg, borderColor: c.fg }"
              @click="form.color = c.name; form.bgColor = c.bg; form.fgColor = c.fg"
            >
              {{ form.name || '预览' }}
            </div>
          </div>
        </el-form-item>
        <el-form-item label="排序权重">
          <el-input-number v-model="form.sort" :min="0" :max="999" :step="1" style="width: 100%" />
          <div class="form-tip">数字越大显示越靠前</div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="标签描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>

    <!-- 使用详情弹窗 -->
    <el-dialog v-model="usageVisible" title="标签使用详情" width="520px" destroy-on-close>
      <div class="usage-dialog-header">
        <div class="tag-preview" :style="{ background: usageTarget?.bgColor, color: usageTarget?.color, borderColor: usageTarget?.color }">
          {{ usageTarget?.name }}
        </div>
        <div class="usage-summary">
          <span>公司 <strong>{{ usageTarget?.companyCount }}</strong></span>
          <span>职位 <strong>{{ usageTarget?.jobCount }}</strong></span>
        </div>
      </div>
      <el-divider />
      <div class="usage-section">
        <h4>关联公司</h4>
        <div class="usage-tags">
          <el-tag v-for="c in usageCompanies" :key="c" size="small" effect="plain" round>{{ c }}</el-tag>
        </div>
      </div>
      <div class="usage-section">
        <h4>关联职位</h4>
        <div class="usage-tags">
          <el-tag v-for="j in usageJobs" :key="j" size="small" effect="plain" round type="info">{{ j }}</el-tag>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  PriceTag, OfficeBuilding, Suitcase, TrendCharts,
  Search, Plus, EditPen, DataAnalysis, Lock, Unlock, Delete
} from '@element-plus/icons-vue'

// 搜索与筛选
const searchKeyword = ref('')
const filterStatus = ref('')
const sortBy = ref('default')
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])

// 颜色选项
const colorOptions = [
  { name: 'blue', bg: '#eff6ff', fg: '#3b82f6' },
  { name: 'green', bg: '#ecfdf5', fg: '#10b981' },
  { name: 'orange', bg: '#fff7ed', fg: '#f59e0b' },
  { name: 'purple', bg: '#faf5ff', fg: '#8b5cf6' },
  { name: 'pink', bg: '#fdf2f8', fg: '#ec4899' },
  { name: 'cyan', bg: '#ecfeff', fg: '#06b6d4' },
  { name: 'red', bg: '#fef2f2', fg: '#ef4444' },
  { name: 'gray', bg: '#f8fafc', fg: '#64748b' },
]

// 模拟数据
const tableData = ref([
  { id: 1, name: '五险一金', bgColor: '#eff6ff', color: '#3b82f6', companyCount: 22, jobCount: 120, sort: 100, status: 'active', createdAt: '2026-05-01T00:00:00', description: '基本社会保障' },
  { id: 2, name: '带薪年假', bgColor: '#ecfdf5', color: '#10b981', companyCount: 20, jobCount: 110, sort: 95, status: 'active', createdAt: '2026-05-01T00:00:00', description: '法定及额外年假' },
  { id: 3, name: '弹性工作', bgColor: '#faf5ff', color: '#8b5cf6', companyCount: 18, jobCount: 95, sort: 90, status: 'active', createdAt: '2026-05-02T00:00:00', description: '灵活工作时间' },
  { id: 4, name: '免费三餐', bgColor: '#fff7ed', color: '#f59e0b', companyCount: 15, jobCount: 80, sort: 85, status: 'active', createdAt: '2026-05-02T00:00:00', description: '提供免费工作餐' },
  { id: 5, name: '健身房', bgColor: '#fdf2f8', color: '#ec4899', companyCount: 12, jobCount: 60, sort: 80, status: 'active', createdAt: '2026-05-03T00:00:00', description: '公司内部健身房' },
  { id: 6, name: '团建活动', bgColor: '#ecfeff', color: '#06b6d4', companyCount: 19, jobCount: 100, sort: 88, status: 'active', createdAt: '2026-05-03T00:00:00', description: '定期团队建设' },
  { id: 7, name: '股票期权', bgColor: '#faf5ff', color: '#8b5cf6', companyCount: 8, jobCount: 45, sort: 70, status: 'active', createdAt: '2026-05-04T00:00:00', description: '员工持股计划' },
  { id: 8, name: '年终奖金', bgColor: '#fef2f2', color: '#ef4444', companyCount: 21, jobCount: 115, sort: 98, status: 'active', createdAt: '2026-05-04T00:00:00', description: '年底双薪或多薪' },
  { id: 9, name: '定期体检', bgColor: '#ecfdf5', color: '#10b981', companyCount: 14, jobCount: 70, sort: 75, status: 'active', createdAt: '2026-05-05T00:00:00', description: '年度健康检查' },
  { id: 10, name: '节日福利', bgColor: '#fff7ed', color: '#f59e0b', companyCount: 17, jobCount: 90, sort: 82, status: 'active', createdAt: '2026-05-05T00:00:00', description: '节日礼品/礼金' },
  { id: 11, name: '交通补贴', bgColor: '#eff6ff', color: '#3b82f6', companyCount: 10, jobCount: 55, sort: 65, status: 'active', createdAt: '2026-05-06T00:00:00', description: '通勤交通补助' },
  { id: 12, name: '住房补贴', bgColor: '#fdf2f8', color: '#ec4899', companyCount: 9, jobCount: 48, sort: 60, status: 'active', createdAt: '2026-05-06T00:00:00', description: '租房或购房补贴' },
  { id: 13, name: '补充医疗保险', bgColor: '#ecfeff', color: '#06b6d4', companyCount: 11, jobCount: 58, sort: 68, status: 'active', createdAt: '2026-05-07T00:00:00', description: '商业医疗保险' },
  { id: 14, name: '培训发展', bgColor: '#faf5ff', color: '#8b5cf6', companyCount: 16, jobCount: 85, sort: 78, status: 'active', createdAt: '2026-05-07T00:00:00', description: '内部培训及外部课程' },
  { id: 15, name: '加班补贴', bgColor: '#fef2f2', color: '#ef4444', companyCount: 13, jobCount: 65, sort: 72, status: 'disabled', createdAt: '2026-05-08T00:00:00', description: '加班费或调休' },
])

// 统计计算
const totalCompanyUsage = computed(() => tableData.value.reduce((s, t) => s + t.companyCount, 0))
const totalJobUsage = computed(() => {
  const v = tableData.value.reduce((s, t) => s + t.jobCount, 0)
  return v >= 10000 ? (v / 10000).toFixed(1) + '万' : v.toLocaleString()
})
const mostPopular = computed(() => {
  if (!tableData.value.length) return '-'
  return [...tableData.value].sort((a, b) => b.jobCount - a.jobCount)[0]?.name || '-'
})

// 最大使用量（用于计算进度条百分比）
const maxUsage = computed(() => Math.max(...tableData.value.map(t => t.jobCount), 1))
const usagePercent = (row) => (row.jobCount / maxUsage.value) * 100

// 筛选+排序后的数据
const filteredData = computed(() => {
  let data = tableData.value.filter(row => {
    const kw = searchKeyword.value.toLowerCase()
    const matchSearch = !kw || row.name.toLowerCase().includes(kw)
    const matchStatus = !filterStatus.value || row.status === filterStatus.value
    return matchSearch && matchStatus
  })

  // 排序
  if (sortBy.value === 'usage_desc') {
    data = [...data].sort((a, b) => b.jobCount - a.jobCount)
  } else if (sortBy.value === 'usage_asc') {
    data = [...data].sort((a, b) => a.jobCount - b.jobCount)
  } else if (sortBy.value === 'name') {
    data = [...data].sort((a, b) => a.name.localeCompare(b.name, 'zh'))
  } else if (sortBy.value === 'created') {
    data = [...data].sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
  } else {
    data = [...data].sort((a, b) => b.sort - a.sort)
  }

  return data
})

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
  filterStatus.value = ''
  sortBy.value = 'default'
  currentPage.value = 1
}

// 多选
const handleSelectionChange = (rows) => { selectedRows.value = rows }

// 新增
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({ name: '', color: 'blue', bgColor: '#eff6ff', fgColor: '#3b82f6', sort: 0, description: '' })
const rules = { name: [{ required: true, message: '请输入标签名称', trigger: 'blur' }] }

const handleAdd = () => {
  editingId.value = null
  form.name = ''
  form.color = 'blue'
  form.bgColor = '#eff6ff'
  form.fgColor = '#3b82f6'
  form.sort = 0
  form.description = ''
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editingId.value = row.id
  form.name = row.name
  form.color = row.color
  form.bgColor = row.bgColor
  form.fgColor = row.color
  form.sort = row.sort
  form.description = row.description || ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  if (editingId.value) {
    const item = tableData.value.find(t => t.id === editingId.value)
    if (item) {
      item.name = form.name
      item.color = form.fgColor
      item.bgColor = form.bgColor
      item.sort = form.sort
      item.description = form.description
    }
    ElMessage.success('编辑成功')
  } else {
    tableData.value.push({
      id: Date.now(),
      name: form.name,
      bgColor: form.bgColor,
      color: form.fgColor,
      companyCount: 0,
      jobCount: 0,
      sort: form.sort,
      status: 'active',
      createdAt: new Date().toISOString(),
      description: form.description,
    })
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
}

// 启用/禁用
const handleToggleStatus = (row) => {
  const newStatus = row.status === 'active' ? 'disabled' : 'active'
  const label = newStatus === 'active' ? '启用' : '禁用'
  ElMessageBox.confirm(`确定要${label}标签「${row.name}」吗？`, '操作确认', {
    type: 'warning',
    confirmButtonText: label,
    cancelButtonText: '取消',
  }).then(() => {
    row.status = newStatus
    ElMessage.success(`${label}成功`)
  }).catch(() => {})
}

// 删除
const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除标签「${row.name}」吗？关联记录将被清除。`, '删除确认', {
    type: 'error',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  }).then(() => {
    tableData.value = tableData.value.filter(t => t.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

// 批量删除
const handleBatchDelete = () => {
  const names = selectedRows.value.map(r => r.name).join('、')
  ElMessageBox.confirm(`确定要删除以下标签吗？\n${names}`, '批量删除', {
    type: 'error',
    confirmButtonText: '确认删除',
  }).then(() => {
    const ids = new Set(selectedRows.value.map(r => r.id))
    tableData.value = tableData.value.filter(t => !ids.has(t.id))
    ElMessage.success(`已删除 ${ids.size} 个标签`)
    selectedRows.value = []
  }).catch(() => {})
}

// 查看使用详情
const usageVisible = ref(false)
const usageTarget = ref(null)
const usageCompanies = ref([])
const usageJobs = ref([])

// 模拟使用数据
const usageMockData = {
  1: { companies: ['字节跳动', '阿里巴巴', '腾讯', '华为', '百度', '小米', '美团', '京东'], jobs: ['前端开发', 'Java后端', '算法工程师', '产品经理', 'UI设计师'] },
  2: { companies: ['字节跳动', '阿里巴巴', '腾讯', '华为', '小米'], jobs: ['前端开发', 'Java后端', '产品经理'] },
  3: { companies: ['字节跳动', '腾讯', '百度', '小米'], jobs: ['前端开发', '产品经理', 'UI设计师'] },
  4: { companies: ['字节跳动', '阿里巴巴', '美团'], jobs: ['Java后端', '算法工程师'] },
  5: { companies: ['字节跳动', '腾讯'], jobs: ['前端开发'] },
  6: { companies: ['字节跳动', '阿里巴巴', '腾讯', '华为', '小米', '美团'], jobs: ['前端开发', 'Java后端', '算法工程师', '产品经理'] },
  7: { companies: ['字节跳动', '阿里巴巴'], jobs: ['算法工程师'] },
  8: { companies: ['字节跳动', '阿里巴巴', '腾讯', '华为', '百度', '小米'], jobs: ['前端开发', 'Java后端', '算法工程师', '产品经理', 'UI设计师'] },
  9: { companies: ['字节跳动', '腾讯', '华为'], jobs: ['前端开发', 'Java后端'] },
  10: { companies: ['字节跳动', '阿里巴巴', '腾讯', '小米'], jobs: ['前端开发', 'Java后端', '产品经理'] },
  11: { companies: ['字节跳动', '华为'], jobs: ['Java后端'] },
  12: { companies: ['字节跳动', '阿里巴巴'], jobs: ['前端开发'] },
  13: { companies: ['腾讯', '华为'], jobs: ['Java后端', '算法工程师'] },
  14: { companies: ['字节跳动', '阿里巴巴', '腾讯', '百度'], jobs: ['前端开发', 'Java后端', '产品经理'] },
  15: { companies: ['字节跳动', '小米'], jobs: ['Java后端'] },
}

const handleViewUsage = (row) => {
  usageTarget.value = row
  const mock = usageMockData[row.id] || { companies: [], jobs: [] }
  usageCompanies.value = mock.companies
  usageJobs.value = mock.jobs
  usageVisible.value = true
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
  grid-template-columns: repeat(4, 1fr);
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

/* 标签预览 */
.tag-preview {
  display: inline-flex;
  align-items: center;
  padding: 4px 14px;
  border-radius: 14px;
  font-size: 13px;
  font-weight: 500;
  border: 1px solid;
  white-space: nowrap;
}

/* 标签名 */
.tag-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

/* 使用量 */
.usage-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.usage-bar-wrap {
  width: 100%;
  height: 6px;
  background: #f1f5f9;
  border-radius: 3px;
  overflow: hidden;
}

.usage-bar {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s;
}

.usage-text {
  font-size: 11px;
  color: #94a3b8;
}

/* 状态 */
.status-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

.status-dot.active { background: #10b981; }
.status-dot.disabled { background: #ef4444; }

.status-text { font-size: 13px; }
.status-text.active { color: #10b981; }
.status-text.disabled { color: #ef4444; }

/* 时间 */
.time-text { font-size: 13px; color: #64748b; }

/* 操作按钮 */
.action-btns {
  display: flex;
  gap: 2px;
  justify-content: center;
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

/* 颜色选择器 */
.color-picker-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.color-option {
  padding: 4px 12px;
  border-radius: 12px;
  font-size: 12px;
  font-weight: 500;
  border: 2px solid transparent;
  cursor: pointer;
  transition: all 0.2s;
}

.color-option:hover {
  transform: scale(1.05);
}

.color-option.active {
  border-color: currentColor;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.06);
}

.form-tip {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
}

/* 使用详情弹窗 */
.usage-dialog-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.usage-summary {
  display: flex;
  gap: 20px;
  font-size: 14px;
  color: #475569;
}

.usage-summary strong {
  color: #0f172a;
  font-weight: 700;
}

.usage-section {
  margin-bottom: 16px;
}

.usage-section h4 {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  margin: 0 0 10px;
}

.usage-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

/* 响应式 */
@media (max-width: 1200px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
}

@media (max-width: 768px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
  .filter-card { flex-direction: column; gap: 10px; align-items: stretch; }
  .filter-row { flex-wrap: wrap; }
}
</style>
