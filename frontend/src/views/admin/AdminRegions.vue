<template>
  <div class="page-container">
    <!-- 统计卡片 -->
    <div class="stat-cards">
      <div class="stat-card">
        <div class="stat-icon blue"><el-icon :size="22"><Location /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ totalCount }}</div>
          <div class="stat-label">总地区数</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon red"><el-icon :size="22"><OfficeBuilding /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ level1Count }}</div>
          <div class="stat-label">省级</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon orange"><el-icon :size="22"><House /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ level2Count }}</div>
          <div class="stat-label">市级</div>
        </div>
      </div>
      <div class="stat-card">
        <div class="stat-icon green"><el-icon :size="22"><OfficeBuilding /></el-icon></div>
        <div class="stat-info">
          <div class="stat-value">{{ level3Count }}</div>
          <div class="stat-label">区县级</div>
        </div>
      </div>
    </div>

    <!-- 搜索与操作栏 -->
    <div class="filter-card">
      <div class="filter-row">
        <el-input
          v-model="searchKeyword"
          placeholder="搜索地区名称"
          clearable
          prefix-icon="Search"
          style="width: 220px"
          @keyup.enter="handleSearch"
          @clear="handleSearch"
        />
        <el-select v-model="filterLevel" placeholder="级别" clearable style="width: 100px" @change="handleSearch">
          <el-option label="省级" :value="1" />
          <el-option label="市级" :value="2" />
          <el-option label="区县" :value="3" />
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
          <el-icon><Plus /></el-icon> 新增地区
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
        :tree-props="{ children: 'children' }"
        :header-cell-style="{ background: '#f8fafc', color: '#475569', fontWeight: 600 }"
      >
        <el-table-column label="地区名称" min-width="240">
          <template #default="{ row }">
            <div class="region-cell">
              <div class="region-icon" :class="'level-' + row.level">
                <el-icon :size="14"><component :is="levelIcon(row.level)" /></el-icon>
              </div>
              <div class="region-info">
                <span class="region-name">{{ row.name }}</span>
                <span class="region-code">{{ row.code }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="级别" width="85" align="center">
          <template #default="{ row }">
            <el-tag :type="levelTagType(row.level)" size="small" effect="dark" round>
              {{ levelLabel(row.level) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="jobCount" label="关联职位" width="100" align="center" sortable />
        <el-table-column prop="companyCount" label="关联公司" width="100" align="center" sortable />
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
        <el-table-column label="操作" width="240" fixed="right" align="center">
          <template #default="{ row }">
            <div class="action-btns">
              <el-button v-if="row.level < 3" link type="primary" size="small" @click="handleAddChild(row)">
                <el-icon><Plus /></el-icon> 下级
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
        <el-form-item label="地区名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入地区名称" />
        </el-form-item>
        <el-form-item label="父地区">
          <el-input :model-value="form.parentName || '无（顶级）'" disabled />
        </el-form-item>
        <el-form-item label="行政区划码">
          <el-input v-model="form.code" placeholder="例：110000" maxlength="6" />
          <div class="form-tip">6位国标行政区划代码</div>
        </el-form-item>
        <el-form-item label="排序权重">
          <el-input-number v-model="form.sort" :min="0" :max="999" :step="1" style="width: 100%" />
          <div class="form-tip">数字越大显示越靠前</div>
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
  Location, OfficeBuilding, House,
  Search, Plus, EditPen, Lock, Unlock, Delete, Folder, FolderOpened
} from '@element-plus/icons-vue'

// 搜索
const searchKeyword = ref('')
const filterLevel = ref('')
const allExpanded = ref(true)
const tableRef = ref()

// 树形数据
const treeData = ref([
  {
    id: 1, name: '北京市', code: '110000', level: 1, sort: 100, status: 'active', jobCount: 42, companyCount: 8, createdAt: '2026-05-01T00:00:00',
    children: [
      { id: 11, name: '北京市', code: '110100', level: 2, sort: 100, status: 'active', jobCount: 42, companyCount: 8, createdAt: '2026-05-01T00:00:00', children: [
        { id: 111, name: '海淀区', code: '110108', level: 3, sort: 95, status: 'active', jobCount: 18, companyCount: 4, createdAt: '2026-05-01T00:00:00' },
        { id: 112, name: '朝阳区', code: '110105', level: 3, sort: 90, status: 'active', jobCount: 15, companyCount: 3, createdAt: '2026-05-01T00:00:00' },
        { id: 113, name: '西城区', code: '110102', level: 3, sort: 85, status: 'active', jobCount: 9, companyCount: 1, createdAt: '2026-05-02T00:00:00' },
      ]},
    ]
  },
  {
    id: 2, name: '上海市', code: '310000', level: 1, sort: 99, status: 'active', jobCount: 30, companyCount: 6, createdAt: '2026-05-02T00:00:00',
    children: [
      { id: 21, name: '上海市', code: '310100', level: 2, sort: 100, status: 'active', jobCount: 30, companyCount: 6, createdAt: '2026-05-02T00:00:00', children: [
        { id: 211, name: '浦东新区', code: '310115', level: 3, sort: 95, status: 'active', jobCount: 14, companyCount: 3, createdAt: '2026-05-02T00:00:00' },
        { id: 212, name: '黄浦区', code: '310101', level: 3, sort: 90, status: 'active', jobCount: 10, companyCount: 2, createdAt: '2026-05-02T00:00:00' },
        { id: 213, name: '徐汇区', code: '310104', level: 3, sort: 85, status: 'active', jobCount: 6, companyCount: 1, createdAt: '2026-05-03T00:00:00' },
      ]},
    ]
  },
  {
    id: 3, name: '广东省', code: '440000', level: 1, sort: 98, status: 'active', jobCount: 48, companyCount: 10, createdAt: '2026-05-03T00:00:00',
    children: [
      { id: 31, name: '深圳市', code: '440300', level: 2, sort: 100, status: 'active', jobCount: 28, companyCount: 6, createdAt: '2026-05-03T00:00:00', children: [
        { id: 311, name: '南山区', code: '440305', level: 3, sort: 95, status: 'active', jobCount: 16, companyCount: 4, createdAt: '2026-05-03T00:00:00' },
        { id: 312, name: '福田区', code: '440304', level: 3, sort: 90, status: 'active', jobCount: 12, companyCount: 2, createdAt: '2026-05-03T00:00:00' },
      ]},
      { id: 32, name: '广州市', code: '440100', level: 2, sort: 95, status: 'active', jobCount: 20, companyCount: 4, createdAt: '2026-05-04T00:00:00' },
    ]
  },
  {
    id: 4, name: '浙江省', code: '330000', level: 1, sort: 97, status: 'active', jobCount: 18, companyCount: 4, createdAt: '2026-05-04T00:00:00',
    children: [
      { id: 41, name: '杭州市', code: '330100', level: 2, sort: 100, status: 'active', jobCount: 18, companyCount: 4, createdAt: '2026-05-04T00:00:00' },
    ]
  },
  { id: 5, name: '四川省', code: '510000', level: 1, sort: 90, status: 'active', jobCount: 8, companyCount: 2, createdAt: '2026-05-05T00:00:00' },
  { id: 6, name: '江苏省', code: '320000', level: 1, sort: 92, status: 'active', jobCount: 12, companyCount: 3, createdAt: '2026-05-05T00:00:00' },
  { id: 7, name: '湖北省', code: '420000', level: 1, sort: 88, status: 'active', jobCount: 6, companyCount: 1, createdAt: '2026-05-06T00:00:00' },
  { id: 8, name: '陕西省', code: '610000', level: 1, sort: 85, status: 'active', jobCount: 5, companyCount: 1, createdAt: '2026-05-06T00:00:00' },
  { id: 9, name: '重庆市', code: '500000', level: 1, sort: 86, status: 'active', jobCount: 4, companyCount: 1, createdAt: '2026-05-07T00:00:00' },
])

// 统计计算
const totalCount = computed(() => {
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
const level3Count = computed(() => {
  let count = 0
  treeData.value.forEach(n => {
    if (n.children) n.children.forEach(c => { if (c.children) count += c.children.length })
  })
  return count
})

// 筛选后的数据
const filteredData = computed(() => {
  const kw = searchKeyword.value.toLowerCase()
  const level = filterLevel.value

  const filterTree = (list) => {
    return list.filter(node => {
      const matchName = !kw || node.name.toLowerCase().includes(kw)
      const matchLevel = !level || node.level === level
      const childMatch = node.children && filterTree(node.children).length > 0
      return (matchName && matchLevel) || childMatch
    }).map(node => {
      if (node.children) {
        return { ...node, children: filterTree(node.children) }
      }
      return node
    })
  }

  return filterTree(treeData.value)
})

// 级别图标
const levelIcon = (level) => ({ 1: 'OfficeBuilding', 2: 'House', 3: 'Location' }[level] || 'Location')
const levelLabel = (level) => ({ 1: '省', 2: '市', 3: '区' }[level] || '')
const levelTagType = (level) => ({ 1: 'danger', 2: 'warning', 3: 'info' }[level] || 'info')

// 展开/收起
const toggleExpand = () => {
  allExpanded.value = !allExpanded.value
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
  filterLevel.value = ''
}

// 新增/编辑弹窗
const dialogVisible = ref(false)
const editingId = ref(null)
const formRef = ref()
const form = reactive({ name: '', parentId: null, parentName: '', code: '', sort: 0 })
const rules = { name: [{ required: true, message: '请输入地区名称', trigger: 'blur' }] }
const dialogTitle = computed(() => {
  if (editingId.value) return '编辑地区'
  return form.parentId ? `新增下级 - ${form.parentName}` : '新增地区'
})

const handleAdd = () => {
  editingId.value = null
  form.name = ''
  form.parentId = null
  form.parentName = ''
  form.code = ''
  form.sort = 0
  dialogVisible.value = true
}

const handleAddChild = (row) => {
  editingId.value = null
  form.name = ''
  form.parentId = row.id
  form.parentName = row.name
  form.code = ''
  form.sort = 0
  dialogVisible.value = true
}

const handleEdit = (row) => {
  editingId.value = row.id
  form.name = row.name
  form.parentId = null
  form.parentName = ''
  form.code = row.code || ''
  form.sort = row.sort || 0
  dialogVisible.value = true
}

const handleSubmit = async () => {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  if (editingId.value) {
    const findAndUpdate = (list) => {
      for (const item of list) {
        if (item.id === editingId.value) {
          item.name = form.name
          item.code = form.code
          item.sort = form.sort
          return true
        }
        if (item.children && findAndUpdate(item.children)) return true
      }
      return false
    }
    findAndUpdate(treeData.value)
    ElMessage.success('编辑成功')
  } else {
    const newNode = {
      id: Date.now(),
      name: form.name,
      code: form.code,
      level: form.parentId ? 3 : 1,
      sort: form.sort,
      status: 'active',
      jobCount: 0,
      companyCount: 0,
      createdAt: new Date().toISOString(),
    }

    if (form.parentId) {
      const findAndAdd = (list) => {
        for (const item of list) {
          if (item.id === form.parentId) {
            // 确定子级 level
            newNode.level = item.level + 1
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
  ElMessageBox.confirm(`确定要${label}地区「${row.name}」吗？`, '操作确认', {
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
    ? `确定要删除地区「${row.name}」及其 ${row.children.length} 个下级地区吗？`
    : `确定要删除地区「${row.name}」吗？`

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
.stat-icon.red { background: #fef2f2; color: #ef4444; }
.stat-icon.orange { background: #fff7ed; color: #f59e0b; }
.stat-icon.green { background: #ecfdf5; color: #10b981; }

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

/* 地区名称列 */
.region-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.region-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.region-icon.level-1 { background: #fef2f2; color: #ef4444; }
.region-icon.level-2 { background: #fff7ed; color: #f59e0b; }
.region-icon.level-3 { background: #eff6ff; color: #3b82f6; }

.region-info {
  display: flex;
  flex-direction: column;
}

.region-name {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
  line-height: 1.3;
}

.region-code {
  font-size: 12px;
  color: #94a3b8;
  font-family: monospace;
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

/* 表单提示 */
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
