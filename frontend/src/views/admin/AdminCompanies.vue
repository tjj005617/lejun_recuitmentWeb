<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue"><el-icon :size="22"><OfficeBuilding /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ tableData.length }}</div>
          <div class="stat-label">总公司数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><el-icon :size="22"><Suitcase /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ totalJobs }}</div>
          <div class="stat-label">在招职位</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><el-icon :size="22"><View /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ totalViews }}</div>
          <div class="stat-label">总浏览量</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple"><el-icon :size="22"><Star /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ totalFollows }}</div>
          <div class="stat-label">总关注数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red"><el-icon :size="22"><Timer /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">3</div>
          <div class="stat-label">今日新增</div>
        </div>
      </div>
    </div>

    <!-- 搜索与筛选 -->
    <div class="filter-card">
      <div class="filter-row">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索公司名称"
          clearable
          prefix-icon="Search"
          style="width: 220px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="filterIndustry" placeholder="行业" clearable style="width: 120px" @change="handleSearch">
          <el-option v-for="ind in industries" :key="ind" :label="ind" :value="ind" />
        </el-select>
        <el-select v-model="filterCity" placeholder="城市" clearable style="width: 110px" @change="handleSearch">
          <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
        </el-select>
        <el-select v-model="filterScale" placeholder="规模" clearable style="width: 130px" @change="handleSearch">
          <el-option label="10000人以上" value="10000人以上" />
          <el-option label="1000-9999人" value="1000-9999人" />
          <el-option label="100-999人" value="100-999人" />
          <el-option label="100人以下" value="100人以下" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 100px" @change="handleSearch">
          <el-option label="正常" value="active" />
          <el-option label="禁用" value="disabled" />
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
        <el-button type="danger" plain :disabled="!selectedRows.length" @click="handleBatchDisable">
          批量禁用 ({{ selectedRows.length }})
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
        <el-table-column label="公司信息" min-width="200">
          <template #default="{ row }">
            <div class="company-cell">
              <div class="company-logo" :style="{ background: logoColor(row) }">
                {{ row.name?.charAt(0) }}
              </div>
              <div class="company-text">
                <div class="company-name">{{ row.name }}</div>
                <div class="company-industry">{{ row.industry }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="scale" label="规模" width="130" />
        <el-table-column label="城市" width="90">
          <template #default="{ row }">
            <span class="city-text">{{ row.city }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="85" align="center">
          <template #default="{ row }">
            <span class="status-dot" :class="row.status"></span>
            <span :class="['status-text', row.status]">{{ row.status === 'active' ? '正常' : '禁用' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="jobCount" label="在招职位" width="90" align="center" sortable />
        <el-table-column prop="viewCount" label="浏览量" width="90" align="center" sortable />
        <el-table-column prop="followCount" label="关注数" width="90" align="center" sortable />
        <el-table-column label="创建时间" width="170">
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
              <el-button link type="primary" size="small" @click="handleViewJobs(row)">
                <el-icon><Suitcase /></el-icon> 职位
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

    <!-- 编辑弹窗 -->
    <el-dialog v-model="editVisible" title="编辑公司" width="520px" destroy-on-close>
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="公司名称">
          <el-input v-model="editForm.name" placeholder="请输入公司名称" />
        </el-form-item>
        <el-form-item label="行业">
          <el-select v-model="editForm.industry" placeholder="请选择行业" style="width: 100%">
            <el-option v-for="ind in industries" :key="ind" :label="ind" :value="ind" />
          </el-select>
        </el-form-item>
        <el-form-item label="规模">
          <el-select v-model="editForm.scale" placeholder="请选择规模" style="width: 100%">
            <el-option label="10000人以上" value="10000人以上" />
            <el-option label="1000-9999人" value="1000-9999人" />
            <el-option label="100-999人" value="100-999人" />
            <el-option label="100人以下" value="100人以下" />
          </el-select>
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="editForm.city" placeholder="请输入城市" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="editForm.description" type="textarea" :rows="3" placeholder="公司描述" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 导入弹窗 -->
    <el-dialog v-model="importVisible" title="导入公司" width="520px" destroy-on-close>
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
  OfficeBuilding, Suitcase, View, Star, Timer,
  Search, EditPen, Lock, Unlock, Delete,
  Download, Upload, InfoFilled
} from '@element-plus/icons-vue'

const router = useRouter()

// 搜索与筛选
const searchKeyword = ref('')
const filterIndustry = ref('')
const filterCity = ref('')
const filterScale = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])

const industries = ['互联网', '人工智能', '大数据', '企业服务', '金融', '教育', '游戏', '电商', '医疗', '其他']
const cities = ['北京', '上海', '广州', '深圳', '杭州', '成都', '武汉', '南京']

// 模拟数据
const tableData = ref([
  { id: 1, name: '字节跳动', industry: '互联网', scale: '10000人以上', city: '北京', jobCount: 12, viewCount: 3420, followCount: 128, createdAt: '2026-05-01T10:00:00', status: 'active' },
  { id: 2, name: '阿里巴巴', industry: '电商', scale: '10000人以上', city: '杭州', jobCount: 10, viewCount: 2980, followCount: 115, createdAt: '2026-05-01T14:00:00', status: 'active' },
  { id: 3, name: '腾讯', industry: '互联网', scale: '10000人以上', city: '深圳', jobCount: 9, viewCount: 3100, followCount: 108, createdAt: '2026-05-02T09:00:00', status: 'active' },
  { id: 4, name: '华为', industry: '企业服务', scale: '10000人以上', city: '深圳', jobCount: 8, viewCount: 2800, followCount: 99, createdAt: '2026-05-10T08:00:00', status: 'active' },
  { id: 5, name: '百度', industry: '人工智能', scale: '10000人以上', city: '北京', jobCount: 7, viewCount: 2100, followCount: 88, createdAt: '2026-05-11T10:00:00', status: 'active' },
  { id: 6, name: '小米', industry: '互联网', scale: '10000人以上', city: '北京', jobCount: 6, viewCount: 1850, followCount: 82, createdAt: '2026-05-05T11:00:00', status: 'active' },
  { id: 7, name: '商汤科技', industry: '人工智能', scale: '1000-9999人', city: '上海', jobCount: 5, viewCount: 1200, followCount: 56, createdAt: '2026-05-06T14:30:00', status: 'active' },
  { id: 8, name: '好未来', industry: '教育', scale: '1000-9999人', city: '北京', jobCount: 5, viewCount: 980, followCount: 43, createdAt: '2026-05-07T09:00:00', status: 'active' },
  { id: 9, name: '药明康德', industry: '医疗', scale: '10000人以上', city: '上海', jobCount: 5, viewCount: 760, followCount: 38, createdAt: '2026-05-08T10:00:00', status: 'active' },
  { id: 10, name: '微软中国', industry: '互联网', scale: '1000-9999人', city: '北京', jobCount: 5, viewCount: 1650, followCount: 71, createdAt: '2026-05-09T08:30:00', status: 'active' },
  { id: 11, name: '美团', industry: '互联网', scale: '10000人以上', city: '北京', jobCount: 9, viewCount: 2400, followCount: 95, createdAt: '2026-05-12T09:00:00', status: 'active' },
  { id: 12, name: '京东', industry: '电商', scale: '10000人以上', city: '北京', jobCount: 7, viewCount: 1950, followCount: 78, createdAt: '2026-05-13T10:00:00', status: 'disabled' },
  { id: 13, name: '网易', industry: '游戏', scale: '10000人以上', city: '杭州', jobCount: 6, viewCount: 1600, followCount: 65, createdAt: '2026-05-14T14:00:00', status: 'active' },
  { id: 14, name: '拼多多', industry: '电商', scale: '10000人以上', city: '上海', jobCount: 4, viewCount: 1300, followCount: 52, createdAt: '2026-05-15T09:30:00', status: 'active' },
  { id: 15, name: '蚂蚁集团', industry: '金融', scale: '10000人以上', city: '杭州', jobCount: 6, viewCount: 2200, followCount: 85, createdAt: '2026-05-16T08:00:00', status: 'active' },
])

// 统计计算
const totalJobs = computed(() => tableData.value.reduce((sum, c) => sum + c.jobCount, 0))
const totalViews = computed(() => {
  const v = tableData.value.reduce((sum, c) => sum + c.viewCount, 0)
  return v >= 10000 ? (v / 10000).toFixed(1) + '万' : v.toLocaleString()
})
const totalFollows = computed(() => {
  const v = tableData.value.reduce((sum, c) => sum + c.followCount, 0)
  return v >= 10000 ? (v / 10000).toFixed(1) + '万' : v.toLocaleString()
})

// 筛选后的数据
const filteredData = computed(() => {
  return tableData.value.filter(row => {
    const kw = searchKeyword.value.toLowerCase()
    const matchSearch = !kw || row.name.toLowerCase().includes(kw)
    const matchIndustry = !filterIndustry.value || row.industry === filterIndustry.value
    const matchCity = !filterCity.value || row.city === filterCity.value
    const matchScale = !filterScale.value || row.scale === filterScale.value
    const matchStatus = !filterStatus.value || row.status === filterStatus.value
    return matchSearch && matchIndustry && matchCity && matchScale && matchStatus
  })
})

// 公司 logo 颜色
const logoColor = (row) => {
  const colors = {
    '互联网': '#3b82f6', '人工智能': '#8b5cf6', '电商': '#f59e0b',
    '企业服务': '#10b981', '金融': '#ec4899', '教育': '#06b6d4',
    '游戏': '#ef4444', '医疗': '#14b8a6', '大数据': '#6366f1', '其他': '#6b7280'
  }
  return colors[row.industry] || '#6b7280'
}

// 日期格式化
const formatDate = (dateStr) => {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

// 搜索
const handleSearch = () => { currentPage.value = 1 }

// 重置
const handleReset = () => {
  searchKeyword.value = ''
  filterIndustry.value = ''
  filterCity.value = ''
  filterScale.value = ''
  filterStatus.value = ''
  currentPage.value = 1
}

// 多选
const handleSelectionChange = (rows) => { selectedRows.value = rows }

// 编辑
const editVisible = ref(false)
const editForm = ref({})

const handleEdit = (row) => {
  editForm.value = { ...row }
  editVisible.value = true
}

const handleEditSubmit = () => {
  const idx = tableData.value.findIndex(c => c.id === editForm.value.id)
  if (idx !== -1) {
    tableData.value[idx] = { ...tableData.value[idx], ...editForm.value }
  }
  editVisible.value = false
  ElMessage.success('编辑成功')
}

// 查看职位
const handleViewJobs = (row) => {
  router.push({ path: '/admin/jobs', query: { company: row.name } })
}

// 启用/禁用
const handleToggleStatus = (row) => {
  const newStatus = row.status === 'active' ? 'disabled' : 'active'
  const label = newStatus === 'active' ? '启用' : '禁用'
  ElMessageBox.confirm(`确定要${label}公司「${row.name}」吗？`, '操作确认', {
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
  ElMessageBox.confirm(`确定要删除公司「${row.name}」吗？此操作不可恢复。`, '删除确认', {
    type: 'error',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  }).then(() => {
    tableData.value = tableData.value.filter(c => c.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

// 批量禁用
const handleBatchDisable = () => {
  const names = selectedRows.value.map(r => r.name).join('、')
  ElMessageBox.confirm(`确定要禁用以下公司吗？\n${names}`, '批量禁用', {
    type: 'warning',
    confirmButtonText: '确认禁用',
  }).then(() => {
    selectedRows.value.forEach(row => { row.status = 'disabled' })
    ElMessage.success(`已禁用 ${selectedRows.value.length} 个公司`)
    selectedRows.value = []
  }).catch(() => {})
}

// ===== 导入导出 =====
const importVisible = ref(false)
const uploadRef = ref()
const importFile = ref(null)

// 导出公司数据为 Excel
const handleExport = () => {
  const exportData = filteredData.value.map(row => ({
    'ID': row.id,
    '公司名称': row.name,
    '行业': row.industry,
    '规模': row.scale,
    '城市': row.city,
    '状态': row.status === 'active' ? '正常' : '禁用',
    '在招职位': row.jobCount,
    '浏览量': row.viewCount,
    '关注数': row.followCount,
    '创建时间': formatDate(row.createdAt),
  }))
  const ws = XLSX.utils.json_to_sheet(exportData)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '公司列表')
  // 设置列宽
  ws['!cols'] = [
    { wch: 6 }, { wch: 16 }, { wch: 10 }, { wch: 14 },
    { wch: 8 }, { wch: 8 }, { wch: 10 }, { wch: 10 },
    { wch: 10 }, { wch: 20 },
  ]
  XLSX.writeFile(wb, `公司列表_${new Date().toISOString().slice(0, 10)}.xlsx`)
  ElMessage.success('导出成功')
}

// 下载导入模板
const handleDownloadTemplate = () => {
  const templateData = [
    { '公司名称': '示例公司', '行业': '互联网', '规模': '100-999人', '城市': '北京', '描述': '公司描述信息' },
  ]
  const ws = XLSX.utils.json_to_sheet(templateData)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '导入模板')
  ws['!cols'] = [{ wch: 16 }, { wch: 10 }, { wch: 14 }, { wch: 8 }, { wch: 30 }]
  XLSX.writeFile(wb, '公司导入模板.xlsx')
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

      // 映射字段并添加到表格
      let addedCount = 0
      jsonData.forEach(row => {
        const name = row['公司名称'] || row['name']
        if (!name) return
        tableData.value.push({
          id: Date.now() + addedCount,
          name,
          industry: row['行业'] || row['industry'] || '其他',
          scale: row['规模'] || row['scale'] || '100人以下',
          city: row['城市'] || row['city'] || '',
          description: row['描述'] || row['description'] || '',
          jobCount: 0,
          viewCount: 0,
          followCount: 0,
          createdAt: new Date().toISOString(),
          status: 'active',
        })
        addedCount++
      })

      importVisible.value = false
      importFile.value = null
      if (uploadRef.value) uploadRef.value.clearFiles()
      ElMessage.success(`成功导入 ${addedCount} 条公司数据`)
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

/* 表格卡片 */
.table-card {
  background: #fff;
  border-radius: 10px;
  padding: 0 20px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* 公司信息列 */
.company-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.company-logo {
  width: 38px;
  height: 38px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 16px;
  font-weight: 700;
  flex-shrink: 0;
}

.company-text {
  display: flex;
  flex-direction: column;
}

.company-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  line-height: 1.3;
}

.company-industry {
  font-size: 12px;
  color: #94a3b8;
}

/* 城市 */
.city-text { color: #475569; font-size: 13px; }

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
@media (max-width: 1200px) {
  .stat-cards { grid-template-columns: repeat(3, 1fr); }
}

@media (max-width: 768px) {
  .stat-cards { grid-template-columns: repeat(2, 1fr); }
  .filter-card { flex-direction: column; gap: 10px; align-items: stretch; }
  .filter-row { flex-wrap: wrap; }
}
</style>
