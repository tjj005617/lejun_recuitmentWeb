<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue"><el-icon :size="22"><User /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ tableData.length }}</div>
          <div class="stat-label">总用户数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><el-icon :size="22"><User /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ tableData.filter(u => u.roleType === 1).length }}</div>
          <div class="stat-label">求职者</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><el-icon :size="22"><User /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ tableData.filter(u => u.roleType === 2).length }}</div>
          <div class="stat-label">HR</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red"><el-icon :size="22"><User /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ tableData.filter(u => u.roleType === 3).length }}</div>
          <div class="stat-label">管理员</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple"><el-icon :size="22"><Timer /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">18</div>
          <div class="stat-label">今日新增</div>
        </div>
      </div>
    </div>

    <!-- 搜索与筛选 -->
    <div class="filter-card">
      <div class="filter-row">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索用户名 / 昵称 / 手机号"
          clearable
          prefix-icon="Search"
          style="width: 280px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="filterRole" placeholder="角色" clearable style="width: 120px" @change="handleSearch">
          <el-option label="求职者" :value="1" />
          <el-option label="HR" :value="2" />
          <el-option label="管理员" :value="3" />
        </el-select>
        <el-select v-model="filterStatus" placeholder="状态" clearable style="width: 110px" @change="handleSearch">
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
        <el-table-column prop="id" label="ID" width="65" align="center" />
        <el-table-column label="用户信息" min-width="200">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="36" :style="{ background: avatarColor(row) }">
                {{ row.nickname?.charAt(0) || row.username?.charAt(0) }}
              </el-avatar>
              <div class="user-text">
                <div class="user-name">{{ row.nickname || row.username }}</div>
                <div class="user-account">@{{ row.username }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="角色" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="roleTagType(row.roleType)" size="small" effect="dark" round>
              {{ row.roleName }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="companyName" label="所属公司" min-width="130">
          <template #default="{ row }">
            <span v-if="row.companyName && row.companyName !== '-'" class="company-text">{{ row.companyName }}</span>
            <span v-else class="empty-text">—</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="85" align="center">
          <template #default="{ row }">
            <span class="status-dot" :class="row.status"></span>
            <span :class="['status-text', row.status]">{{ row.status === 'active' ? '正常' : '禁用' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="注册时间" width="160">
          <template #default="{ row }">
            <span class="time-text">{{ formatDate(row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button link type="primary" size="small" @click="handleEdit(row)">
                <el-icon><EditPen /></el-icon> 编辑
              </el-button>
              <el-button link type="primary" size="small" @click="handleChangeRole(row)">
                <el-icon><Switch /></el-icon> 角色
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
    <el-dialog v-model="editVisible" title="编辑用户" width="480px" destroy-on-close>
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="editForm.username" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="editForm.nickname" placeholder="请输入昵称" />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="editForm.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="editForm.email" placeholder="请输入邮箱" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="handleEditSubmit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 角色变更弹窗 -->
    <el-dialog v-model="roleVisible" title="变更角色" width="400px" destroy-on-close>
      <div class="role-dialog-body">
        <p class="role-tip">将用户 <strong>{{ roleTarget?.nickname || roleTarget?.username }}</strong> 的角色更改为：</p>
        <el-radio-group v-model="roleNewType" class="role-radio-group">
          <el-radio :value="1">
            <div class="role-option">
              <el-tag type="info" size="small" effect="dark" round>求职者</el-tag>
              <span>浏览职位、投递简历、参加面试</span>
            </div>
          </el-radio>
          <el-radio :value="2">
            <div class="role-option">
              <el-tag type="warning" size="small" effect="dark" round>HR</el-tag>
              <span>发布职位、管理公司、查看投递</span>
            </div>
          </el-radio>
          <el-radio :value="3">
            <div class="role-option">
              <el-tag type="danger" size="small" effect="dark" round>管理员</el-tag>
              <span>管理所有数据、审核内容</span>
            </div>
          </el-radio>
        </el-radio-group>
      </div>
      <template #footer>
        <el-button @click="roleVisible = false">取消</el-button>
        <el-button type="primary" @click="handleRoleSubmit">确认变更</el-button>
      </template>
    </el-dialog>

    <!-- 导入弹窗 -->
    <el-dialog v-model="importVisible" title="导入用户" width="480px" destroy-on-close>
      <div class="import-body">
        <div class="import-tip">
          <el-icon :size="40" color="#3b82f6"><Upload /></el-icon>
          <p>支持 .xlsx / .xls / .csv 格式</p>
          <p class="import-sub">请先下载模板，按格式填写后上传</p>
        </div>
        <el-upload
          ref="uploadRef"
          drag
          :auto-upload="false"
          :limit="1"
          accept=".xlsx,.xls,.csv"
          :on-change="handleFileChange"
          :on-exceed="handleExceed"
        >
          <el-icon class="el-icon--upload"><Upload /></el-icon>
          <div class="el-upload__text">拖拽文件到此处，或 <em>点击上传</em></div>
        </el-upload>
        <div class="import-actions">
          <el-button type="primary" link @click="handleDownloadTemplate">
            <el-icon><Download /></el-icon> 下载导入模板
          </el-button>
        </div>
      </div>
      <template #footer>
        <el-button @click="importVisible = false">取消</el-button>
        <el-button type="primary" :disabled="!importFile" @click="handleImportSubmit">确认导入</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { User, Timer, Search, EditPen, Switch, Lock, Unlock, Delete, Download, Upload } from '@element-plus/icons-vue'
import * as XLSX from 'xlsx'

// 搜索与筛选
const searchKeyword = ref('')
const filterRole = ref('')
const filterStatus = ref('')
const currentPage = ref(1)
const pageSize = ref(10)
const selectedRows = ref([])

// 模拟数据
const tableData = ref([
  { id: 1, username: 'zhangsan', nickname: '张三', phone: '138****1111', email: 'zhangsan@example.com', roleType: 1, roleName: '求职者', companyName: null, createdAt: '2026-06-01T10:00:00', status: 'active' },
  { id: 2, username: 'lisi', nickname: '李四', phone: '138****2222', email: 'lisi@example.com', roleType: 1, roleName: '求职者', companyName: null, createdAt: '2026-06-02T14:30:00', status: 'active' },
  { id: 3, username: 'wangwu', nickname: '王五', phone: '138****3333', email: 'wangwu@example.com', roleType: 1, roleName: '求职者', companyName: null, createdAt: '2026-06-03T09:00:00', status: 'active' },
  { id: 4, username: 'zhaoliu', nickname: '赵六', phone: '138****4444', email: 'zhaoliu@example.com', roleType: 1, roleName: '求职者', companyName: null, createdAt: '2026-06-04T16:00:00', status: 'disabled' },
  { id: 5, username: 'sunqi', nickname: '孙七', phone: '138****5555', email: 'sunqi@example.com', roleType: 1, roleName: '求职者', companyName: null, createdAt: '2026-06-05T11:20:00', status: 'active' },
  { id: 6, username: 'zhouba', nickname: '周八', phone: '138****6666', email: 'zhouba@example.com', roleType: 1, roleName: '求职者', companyName: null, createdAt: '2026-06-06T08:45:00', status: 'active' },
  { id: 15, username: 'hr_xiaomi', nickname: 'HR-小米', phone: '139****1001', email: 'hr@xiaomi.com', roleType: 2, roleName: 'HR', companyName: '小米', createdAt: '2026-06-10T09:00:00', status: 'active' },
  { id: 16, username: 'hr_shangtang', nickname: 'HR-商汤', phone: '139****1002', email: 'hr@shangtang.com', roleType: 2, roleName: 'HR', companyName: '商汤科技', createdAt: '2026-06-10T09:15:00', status: 'active' },
  { id: 17, username: 'hr_haoweilai', nickname: 'HR-好未来', phone: '139****1003', email: 'hr@haoweilai.com', roleType: 2, roleName: 'HR', companyName: '好未来', createdAt: '2026-06-10T09:30:00', status: 'active' },
  { id: 18, username: 'hr_yaoming', nickname: 'HR-药明康德', phone: '139****1004', email: 'hr@yaoming.com', roleType: 2, roleName: 'HR', companyName: '药明康德', createdAt: '2026-06-10T09:45:00', status: 'active' },
  { id: 19, username: 'hr_microsoft', nickname: 'HR-微软', phone: '139****1005', email: 'hr@microsoft.com', roleType: 2, roleName: 'HR', companyName: '微软中国', createdAt: '2026-06-10T10:00:00', status: 'active' },
  { id: 20, username: 'hr_huawei', nickname: 'HR-华为', phone: '139****1006', email: 'hr@huawei.com', roleType: 2, roleName: 'HR', companyName: '华为', createdAt: '2026-06-11T08:00:00', status: 'active' },
  { id: 21, username: 'hr_bytedance', nickname: 'HR-字节', phone: '139****1007', email: 'hr@bytedance.com', roleType: 2, roleName: 'HR', companyName: '字节跳动', createdAt: '2026-06-11T09:00:00', status: 'active' },
  { id: 22, username: 'hr_tencent', nickname: 'HR-腾讯', phone: '139****1008', email: 'hr@tencent.com', roleType: 2, roleName: 'HR', companyName: '腾讯', createdAt: '2026-06-12T08:30:00', status: 'active' },
  { id: 23, username: 'hr_alibaba', nickname: 'HR-阿里', phone: '139****1009', email: 'hr@alibaba.com', roleType: 2, roleName: 'HR', companyName: '阿里巴巴', createdAt: '2026-06-12T09:00:00', status: 'disabled' },
  { id: 30, username: 'admin', nickname: '超级管理员', phone: '137****0001', email: 'admin@platform.com', roleType: 3, roleName: '管理员', companyName: null, createdAt: '2026-05-01T00:00:00', status: 'active' },
  { id: 31, username: 'admin2', nickname: '运营管理员', phone: '137****0002', email: 'admin2@platform.com', roleType: 3, roleName: '管理员', companyName: null, createdAt: '2026-05-15T10:00:00', status: 'active' },
])

// 筛选后的数据
const filteredData = computed(() => {
  return tableData.value.filter(row => {
    const kw = searchKeyword.value.toLowerCase()
    const matchSearch = !kw || row.username.toLowerCase().includes(kw) || (row.nickname || '').toLowerCase().includes(kw) || row.phone.includes(kw)
    const matchRole = !filterRole.value || row.roleType === filterRole.value
    const matchStatus = !filterStatus.value || row.status === filterStatus.value
    return matchSearch && matchRole && matchStatus
  })
})

// 头像颜色
const avatarColor = (row) => {
  const colors = { 1: '#3b82f6', 2: '#f59e0b', 3: '#ef4444' }
  return colors[row.roleType] || '#6b7280'
}

// 角色标签颜色
const roleTagType = (type) => ({ 1: 'info', 2: 'warning', 3: 'danger' }[type] || 'info')

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
  filterRole.value = ''
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
  const idx = tableData.value.findIndex(u => u.id === editForm.value.id)
  if (idx !== -1) {
    tableData.value[idx] = { ...tableData.value[idx], ...editForm.value }
  }
  editVisible.value = false
  ElMessage.success('编辑成功')
}

// 角色变更
const roleVisible = ref(false)
const roleTarget = ref(null)
const roleNewType = ref(1)

const handleChangeRole = (row) => {
  roleTarget.value = row
  roleNewType.value = row.roleType
  roleVisible.value = true
}

const handleRoleSubmit = () => {
  const roleMap = { 1: '求职者', 2: 'HR', 3: '管理员' }
  const idx = tableData.value.findIndex(u => u.id === roleTarget.value.id)
  if (idx !== -1) {
    tableData.value[idx].roleType = roleNewType.value
    tableData.value[idx].roleName = roleMap[roleNewType.value]
  }
  roleVisible.value = false
  ElMessage.success('角色变更成功')
}

// 启用/禁用
const handleToggleStatus = (row) => {
  const newStatus = row.status === 'active' ? 'disabled' : 'active'
  const label = newStatus === 'active' ? '启用' : '禁用'
  ElMessageBox.confirm(`确定要${label}用户「${row.nickname || row.username}」吗？`, '操作确认', {
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
  ElMessageBox.confirm(`确定要删除用户「${row.nickname || row.username}」吗？此操作不可恢复。`, '删除确认', {
    type: 'error',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  }).then(() => {
    tableData.value = tableData.value.filter(u => u.id !== row.id)
    ElMessage.success('删除成功')
  }).catch(() => {})
}

// 批量禁用
const handleBatchDisable = () => {
  const names = selectedRows.value.map(r => r.nickname || r.username).join('、')
  ElMessageBox.confirm(`确定要禁用以下用户吗？\n${names}`, '批量禁用', {
    type: 'warning',
    confirmButtonText: '确认禁用',
  }).then(() => {
    selectedRows.value.forEach(row => { row.status = 'disabled' })
    ElMessage.success(`已禁用 ${selectedRows.value.length} 个用户`)
    selectedRows.value = []
  }).catch(() => {})
}

// === 导入导出 ===
const importVisible = ref(false)
const importFile = ref(null)
const uploadRef = ref()

// 导出 Excel
const handleExport = () => {
  const data = filteredData.value.map(row => ({
    'ID': row.id,
    '用户名': row.username,
    '昵称': row.nickname,
    '手机号': row.phone,
    '邮箱': row.email,
    '角色': row.roleName,
    '所属公司': row.companyName || '',
    '状态': row.status === 'active' ? '正常' : '禁用',
    '注册时间': formatDate(row.createdAt),
  }))
  const ws = XLSX.utils.json_to_sheet(data)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '用户列表')
  // 设置列宽
  ws['!cols'] = [
    { wch: 6 }, { wch: 14 }, { wch: 12 }, { wch: 14 },
    { wch: 22 }, { wch: 8 }, { wch: 14 }, { wch: 8 }, { wch: 18 },
  ]
  XLSX.writeFile(wb, `用户列表_${new Date().toISOString().slice(0, 10)}.xlsx`)
  ElMessage.success('导出成功')
}

// 下载导入模板
const handleDownloadTemplate = () => {
  const template = [
    { '用户名': '', '昵称': '', '手机号': '', '邮箱': '', '角色': '求职者' },
  ]
  const ws = XLSX.utils.json_to_sheet(template)
  const wb = XLSX.utils.book_new()
  XLSX.utils.book_append_sheet(wb, ws, '用户导入模板')
  ws['!cols'] = [{ wch: 14 }, { wch: 12 }, { wch: 14 }, { wch: 22 }, { wch: 8 }]
  XLSX.writeFile(wb, '用户导入模板.xlsx')
  ElMessage.success('模板已下载')
}

// 文件选择
const handleFileChange = (file) => { importFile.value = file.raw }
const handleExceed = () => { ElMessage.warning('只能上传一个文件') }

// 导入提交
const handleImportSubmit = async () => {
  if (!importFile.value) return
  const file = importFile.value
  const reader = new FileReader()
  reader.onload = (e) => {
    try {
      const wb = XLSX.read(e.target.result, { type: 'array' })
      const ws = wb.Sheets[wb.SheetNames[0]]
      const rows = XLSX.utils.sheet_to_json(ws)
      if (!rows.length) { ElMessage.warning('文件中没有数据'); return }
      let count = 0
      rows.forEach(row => {
        if (row['用户名']) {
          tableData.value.push({
            id: Date.now() + count,
            username: row['用户名'] || '',
            nickname: row['昵称'] || '',
            phone: row['手机号'] || '',
            email: row['邮箱'] || '',
            roleType: row['角色'] === 'HR' ? 2 : row['角色'] === '管理员' ? 3 : 1,
            roleName: row['角色'] || '求职者',
            companyName: row['所属公司'] || null,
            createdAt: new Date().toISOString(),
            status: 'active',
          })
          count++
        }
      })
      ElMessage.success(`成功导入 ${count} 个用户`)
      importVisible.value = false
      importFile.value = null
    } catch {
      ElMessage.error('文件解析失败，请检查格式')
    }
  }
  reader.readAsArrayBuffer(file)
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
.stat-icon.red { background: #fef2f2; color: #ef4444; }
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

/* 表格卡片 */
.table-card {
  background: #fff;
  border-radius: 10px;
  padding: 0 20px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* 用户信息列 */
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

.user-account {
  font-size: 12px;
  color: #94a3b8;
}

/* 空值 */
.empty-text { color: #cbd5e1; }
.company-text { color: #334155; }

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

/* 角色弹窗 */
.role-dialog-body { padding: 8px 0; }
.role-tip { font-size: 14px; color: #475569; margin-bottom: 16px; }

.role-radio-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.role-option {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #64748b;
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

/* 导入弹窗 */
.import-body {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.import-tip {
  text-align: center;
  padding: 8px 0;
}

.import-tip p {
  margin: 8px 0 0;
  font-size: 14px;
  color: #475569;
}

.import-sub {
  font-size: 12px !important;
  color: #94a3b8 !important;
}

.import-actions {
  text-align: center;
}
</style>
