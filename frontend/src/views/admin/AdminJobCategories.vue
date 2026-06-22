<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue"><el-icon :size="22"><Menu /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ totalCategories }}</div>
          <div class="stat-label">总分类数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><el-icon :size="22"><Folder /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ level1Count }}</div>
          <div class="stat-label">一级分类</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><el-icon :size="22"><Document /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ level2Count }}</div>
          <div class="stat-label">二级分类</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon purple"><el-icon :size="22"><Suitcase /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ totalJobs }}</div>
          <div class="stat-label">关联职位</div>
        </div>
      </div>
    </div>

    <!-- 搜索与操作栏 -->
    <div class="filter-card">
      <div class="filter-row">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索分类名称"
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
        <el-button type="primary" @click="handleSearch">
          <el-icon><Search /></el-icon> 搜索
        </el-button>
        <el-button @click="handleReset">重置</el-button>
        <el-divider direction="vertical" />
        <el-button @click="toggleExpand">
          <el-icon><component :is="allExpanded ? 'FolderOpened' : 'Folder'" /></el-icon>
          {{ allExpanded ? '收起全部' : '展开全部' }}
        </el-button>
      </div>
      <div class="filter-right">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon> 新增分类
        </el-button>
      </div>
    </div>

    <!-- 树形表格 -->
    <div class="table-card">
      <el-table
        ref="tableRef"
        :data="filteredData"
        stripe
        row-key="id"
        :default-expand-all="allExpanded"
        :tree-props="{ children: 'children', hasChildren: 'hasChildren' }"
        :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: 600 }"
      >
        <el-table-column label="分类名称" min-width="260">
          <template #default="{ row }">
            <div class="category-cell">
              <div class="category-icon" :style="{ background: row.bgColor, color: row.color }">
                <el-icon :size="16"><component :is="row.icon || 'Folder'" /></el-icon>
              </div>
              <div class="category-info">
                <span class="category-name">{{ row.name }}</span>
                <span v-if="row.description" class="category-desc">{{ row.description }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="职位数" width="200">
          <template #default="{ row }">
            <div class="job-count-cell">
              <div class="job-bar-wrap">
                <div class="job-bar" :style="{ width: jobPercent(row) + '%', background: row.color }"></div>
              </div>
              <span class="job-count-text">{{ row.jobCount }}</span>
            </div>
          </template>
        </el-table-column>
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
        <el-table-column label="操作" width="280" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button v-if="!row.children" link type="primary" size="small" @click="handleAddChild(row)">
                <el-icon><Plus /></el-icon> 子分类
              </el-button>
              <el-button link type="primary" size="small" @click="handleEdit(row)">
                <el-icon><EditPen /></el-icon> 编辑
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
    </div>

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="dialogVisible" :title="dialogTitle" width="480px" destroy-on-close>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="80px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" />
        </el-form-item>
        <el-form-item label="父分类">
          <el-input :model-value="form.parentName || '顶级分类'" disabled />
        </el-form-item>
        <el-form-item label="分类图标">
          <div class="icon-picker-row">
            <div
              v-for="ic in iconOptions"
              :key="ic.name"
              class="icon-option"
              :class="{ active: form.icon === ic.name }"
              :style="{ background: form.icon === ic.name ? ic.bg : '#f8fafc', color: form.icon === ic.name ? ic.fg : '#94a3b8' }"
              @click="form.icon = ic.name; form.bgColor = ic.bg; form.color = ic.fg"
            >
              <el-icon :size="18"><component :is="ic.name" /></el-icon>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="排序权重">
          <el-input-number v-model="form.sort" :min="0" :max="999" :step="1" style="width: 100%" />
          <div class="form-tip">数字越大显示越靠前</div>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="分类描述（可选）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  Menu, Folder, FolderOpened, Document, Suitcase,
  Search, Plus, EditPen, Lock, Unlock, Delete,
  Monitor, Cpu, Promotion, Edit, TrendCharts, Connection, Setting, Star
} from '@element-plus/icons-vue'

// 搜索
const searchKeyword = ref('')
const filterStatus = ref('')
const allExpanded = ref(true)
const tableRef = ref()

// 图标选项
const iconOptions = [
  { name: 'Monitor', bg: '#eff6ff', fg: '#3b82f6' },
  { name: 'Cpu', bg: '#faf5ff', fg: '#8b5cf6' },
  { name: 'Edit', bg: '#fff7ed', fg: '#f59e0b' },
  { name: 'TrendCharts', bg: '#ecfdf5', fg: '#10b981' },
  { name: 'Connection', bg: '#ecfeff', fg: '#06b6d4' },
  { name: 'Promotion', bg: '#fdf2f8', fg: '#ec4899' },
  { name: 'Setting', bg: '#f8fafc', fg: '#64748b' },
  { name: 'Star', bg: '#fef2f2', fg: '#ef4444' },
]

// 树形数据
const treeData = ref([
  {
    id: 1, name: '技术开发', description: '软件研发与技术支持', icon: 'Monitor', bgColor: '#eff6ff', color: '#3b82f6', jobCount: 68, sort: 100, status: 'active', createdAt: '2026-05-01T00:00:00',
    children: [
      { id: 11, name: '前端开发', description: 'Web/移动端前端', icon: 'Monitor', bgColor: '#eff6ff', color: '#3b82f6', jobCount: 22, sort: 95, status: 'active', createdAt: '2026-05-01T00:00:00' },
      { id: 12, name: '后端开发', description: '服务端开发', icon: 'Cpu', bgColor: '#faf5ff', color: '#8b5cf6', jobCount: 28, sort: 90, status: 'active', createdAt: '2026-05-01T00:00:00' },
      { id: 13, name: '移动开发', description: 'iOS/Android开发', icon: 'Promotion', bgColor: '#fdf2f8', color: '#ec4899', jobCount: 8, sort: 80, status: 'active', createdAt: '2026-05-02T00:00:00' },
      { id: 14, name: '测试', description: 'QA与自动化测试', icon: 'Setting', bgColor: '#f8fafc', color: '#64748b', jobCount: 10, sort: 75, status: 'active', createdAt: '2026-05-02T00:00:00' },
    ]
  },
  {
    id: 2, name: '人工智能', description: 'AI与机器学习', icon: 'Cpu', bgColor: '#faf5ff', color: '#8b5cf6', jobCount: 25, sort: 95, status: 'active', createdAt: '2026-05-03T00:00:00',
    children: [
      { id: 21, name: '算法工程师', description: '机器学习/深度学习', icon: 'Cpu', bgColor: '#faf5ff', color: '#8b5cf6', jobCount: 15, sort: 90, status: 'active', createdAt: '2026-05-03T00:00:00' },
      { id: 22, name: '数据标注', description: '训练数据处理', icon: 'Edit', bgColor: '#fff7ed', fg: '#f59e0b', jobCount: 5, sort: 70, status: 'active', createdAt: '2026-05-04T00:00:00' },
      { id: 23, name: 'NLP', description: '自然语言处理', icon: 'Connection', bgColor: '#ecfeff', color: '#06b6d4', jobCount: 5, sort: 75, status: 'active', createdAt: '2026-05-04T00:00:00' },
    ]
  },
  {
    id: 3, name: '产品/设计', description: '产品规划与UI设计', icon: 'Edit', bgColor: '#fff7ed', color: '#f59e0b', jobCount: 18, sort: 90, status: 'active', createdAt: '2026-05-05T00:00:00',
    children: [
      { id: 31, name: '产品经理', description: '产品规划与设计', icon: 'Edit', bgColor: '#fff7ed', color: '#f59e0b', jobCount: 10, sort: 85, status: 'active', createdAt: '2026-05-05T00:00:00' },
      { id: 32, name: 'UI/UX设计', description: '界面与交互设计', icon: 'Star', bgColor: '#fef2f2', color: '#ef4444', jobCount: 8, sort: 80, status: 'active', createdAt: '2026-05-05T00:00:00' },
    ]
  },
  {
    id: 4, name: '市场/运营', description: '市场推广与内容运营', icon: 'TrendCharts', bgColor: '#ecfdf5', color: '#10b981', jobCount: 15, sort: 85, status: 'active', createdAt: '2026-05-06T00:00:00',
    children: [
      { id: 41, name: '市场营销', description: '品牌与渠道推广', icon: 'TrendCharts', bgColor: '#ecfdf5', color: '#10b981', jobCount: 8, sort: 80, status: 'active', createdAt: '2026-05-06T00:00:00' },
      { id: 42, name: '内容运营', description: '内容策划与运营', icon: 'Edit', bgColor: '#fff7ed', color: '#f59e0b', jobCount: 7, sort: 75, status: 'active', createdAt: '2026-05-06T00:00:00' },
    ]
  },
  { id: 5, name: '数据', description: '数据分析与治理', icon: 'TrendCharts', bgColor: '#ecfdf5', color: '#10b981', jobCount: 12, sort: 80, status: 'active', createdAt: '2026-05-07T00:00:00' },
  { id: 6, name: '其他', description: '未分类职位', icon: 'Folder', bgColor: '#f8fafc', color: '#64748b', jobCount: 8, sort: 0, status: 'active', createdAt: '2026-05-08T00:00:00' },
])

// 统计计算
const totalCategories = computed(() => {
  let count = 0
  const walk = (list) => { list.forEach(n => { count++; if (n.children) walk(n.children) }) }
  walk(treeData.value)
  return count
})
const level1Count = computed(() => treeData.value.length)
const level2Count = computed(() => {
  let count = 0
  treeData.value.forEach(n => { if (n.children) count += n.children.length })
  return count
})
const totalJobs = computed(() => {
  let sum = 0
  const walk = (list) => { list.forEach(n => { sum += n.jobCount; if (n.children) walk(n.children) }) }
  walk(treeData.value)
  return sum
})

// 最大职位数（进度条基准）
const maxJobs = computed(() => {
  let max = 0
  const walk = (list) => { list.forEach(n => { if (n.jobCount > max) max = n.jobCount; if (n.children) walk(n.children) }) }
  walk(treeData.value)
  return max || 1
})
const jobPercent = (row) => (row.jobCount / maxJobs.value) * 100

// 筛选后的数据
const filteredData = computed(() => {
  const kw = searchKeyword.value.toLowerCase()
  const status = filterStatus.value

  const filterTree = (list) => {
    return list.filter(node => {
      const matchName = !kw || node.name.toLowerCase().includes(kw)
      const matchStatus = !status || node.status === status
      const childMatch = node.children && filterTree(node.children).length > 0
      return (matchName && matchStatus) || childMatch
    }).map(node => {
      if (node.children) {
        return { ...node, children: filterTree(node.children) }
      }
      return node
    })
  }

  return filterTree(treeData.value)
})

// 展开/收起
const toggleExpand = () => {
  allExpanded.value = !allExpanded.value
  // 通过 key 强制重新渲染树
  const data = treeData.value
  treeData.value = []
  setTimeout(() => { treeData.value = data }, 0)
}

// 日期格式化
const formatDate = (dateStr) => {
  if (!dateStr) return '—'
  const d = new Date(dateStr)
  const pad = (n) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

// 搜索
const handleSearch = () => {}

// 重置
const handleReset = () => {
  searchKeyword.value = ''
  filterStatus.value = ''
}

// 新增/编辑弹窗
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({ name: '', parentId: null, parentName: '', icon: 'Folder', bgColor: '#f8fafc', color: '#64748b', sort: 0, description: '' })
const rules = { name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }] }
const dialogTitle = computed(() => {
  if (editingId.value) return '编辑分类'
  return form.parentId ? `新增子分类 - ${form.parentName}` : '新增分类'
})

const handleAdd = () => {
  editingId.value = null
  form.name = ''
  form.parentId = null
  form.parentName = ''
  form.icon = 'Folder'
  form.bgColor = '#f8fafc'
  form.color = '#64748b'
  form.sort = 0
  form.description = ''
  dialogVisible.value = true
}

const handleAddChild = (row) => {
  editingId.value = null
  form.name = ''
  form.parentId = row.id
  form.parentName = row.name
  form.icon = 'Folder'
  form.bgColor = '#f8fafc'
  form.color = '#64748b'
  form.sort = 0
  form.description = ''
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editingId.value = row.id
  form.name = row.name
  form.parentId = null
  form.parentName = ''
  form.icon = row.icon || 'Folder'
  form.bgColor = row.bgColor || '#f8fafc'
  form.color = row.color || '#64748b'
  form.sort = row.sort || 0
  form.description = row.description || ''
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (editingId.value) {
    // 编辑
    const findAndUpdate = (list) => {
      for (const item of list) {
        if (item.id === editingId.value) {
          item.name = form.name
          item.icon = form.icon
          item.bgColor = form.bgColor
          item.color = form.color
          item.sort = form.sort
          item.description = form.description
          return true
        }
        if (item.children && findAndUpdate(item.children)) return true
      }
      return false
    }
    findAndUpdate(treeData.value)
    ElMessage.success('编辑成功')
  } else {
    // 新增
    const newNode = {
      id: Date.now(),
      name: form.name,
      description: form.description,
      icon: form.icon,
      bgColor: form.bgColor,
      color: form.color,
      jobCount: 0,
      sort: form.sort,
      status: 'active',
      createdAt: new Date().toISOString(),
    }

    if (form.parentId) {
      // 添加子分类
      const findAndAdd = (list) => {
        for (const item of list) {
          if (item.id === form.parentId) {
            if (!item.children) item.children = []
            item.children.push(newNode)
            return true
          }
          if (item.children && findAndAdd(item.children)) return true
        }
        return false
      }
      findAndAdd(treeData.value)
    } else {
      // 添加顶级分类
      treeData.value.push(newNode)
    }
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
}

// 启用/禁用
const handleToggleStatus = (row) => {
  const newStatus = row.status === 'active' ? 'disabled' : 'active'
  const label = newStatus === 'active' ? '启用' : '禁用'
  ElMessageBox.confirm(`确定要${label}分类「${row.name}」吗？`, '操作确认', {
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
  const hasChildren = row.children && row.children.length > 0
  const msg = hasChildren
    ? `确定要删除分类「${row.name}」及其 ${row.children.length} 个子分类吗？`
    : `确定要删除分类「${row.name}」吗？`

  ElMessageBox.confirm(msg, '删除确认', {
    type: 'error',
    confirmButtonText: '删除',
    cancelButtonText: '取消',
  }).then(() => {
    const removeFromTree = (list) => {
      const idx = list.findIndex(n => n.id === row.id)
      if (idx !== -1) { list.splice(idx, 1); return true }
      for (const item of list) {
        if (item.children && removeFromTree(item.children)) return true
      }
      return false
    }
    removeFromTree(treeData.value)
    ElMessage.success('删除成功')
  }).catch(() => {})
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

/* 表格卡片 */
.table-card {
  background: #fff;
  border-radius: 10px;
  padding: 0 20px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}

/* 分类名称列 */
.category-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.category-icon {
  width: 34px;
  height: 34px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.category-info {
  display: flex;
  flex-direction: column;
}

.category-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  line-height: 1.3;
}

.category-desc {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

/* 职位数 */
.job-count-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.job-bar-wrap {
  flex: 1;
  height: 6px;
  background: #f1f5f9;
  border-radius: 3px;
  overflow: hidden;
}

.job-bar {
  height: 100%;
  border-radius: 3px;
  transition: width 0.3s;
}

.job-count-text {
  font-size: 14px;
  font-weight: 600;
  color: #1e293b;
  min-width: 28px;
  text-align: right;
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

/* 图标选择器 */
.icon-picker-row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.icon-option {
  width: 40px;
  height: 40px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  border: 2px solid transparent;
}

.icon-option:hover {
  transform: scale(1.1);
}

.icon-option.active {
  border-color: currentColor;
  box-shadow: 0 0 0 2px rgba(0, 0, 0, 0.06);
}

.form-tip {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 4px;
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
