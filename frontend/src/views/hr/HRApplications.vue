<template>
  <AppShell :bgImage="false">
    <div class="applications">
      <div class="page-header">
        <h1>应聘管理</h1>
      </div>

      <!-- 筛选栏 -->
      <el-card class="filter-card">
        <div class="filter-bar">
          <span class="filter-label">筛选职位：</span>
          <el-select v-model="selectedJobId" placeholder="全部职位" clearable filterable @change="loadApplications" style="width: 300px">
            <el-option
              v-for="job in myJobs"
              :key="job.id"
              :label="job.title"
              :value="job.id"
            />
          </el-select>
          <el-input
            v-model="keyword"
            placeholder="搜索候选人姓名"
            clearable
            prefix-icon="Search"
            style="width: 240px"
            @input="handleSearch"
          />
        </div>
      </el-card>

      <!-- 投递列表 -->
      <el-card>
        <el-table :data="applications" v-loading="loading" style="width: 100%">
          <el-table-column prop="jobTitle" label="应聘职位" width="180" />
          <el-table-column prop="username" label="候选人" width="150" />

          <el-table-column prop="status" label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="appliedAt" label="投递时间" width="180">
            <template #default="{ row }">
              {{ formatDateTime(row.appliedAt) }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'pending'"
                text type="primary" size="small"
                @click="updateStatus(row.id, 'screening')"
              >
                筛选
              </el-button>
              <el-button
                v-if="row.status === 'screening'"
                text type="warning" size="small"
                @click="updateStatus(row.id, 'interview')"
              >
                面试
              </el-button>
              <el-button
                v-if="row.status === 'interview'"
                text type="success" size="small"
                @click="updateStatus(row.id, 'offer')"
              >
                录用
              </el-button>
              <el-button
                v-if="row.status !== 'rejected' && row.status !== 'offer'"
                text type="danger" size="small"
                @click="handleReject(row.id)"
              >
                拒绝
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="applications.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无投递记录" />
        </div>
      </el-card>
    </div>
  </AppShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppShell from '@/components/AppShell.vue'
import { getMyJobs } from '@/api/job'
import { getCompanyApplications, getJobApplications, updateApplicationStatus } from '@/api/application'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const { user } = userStore
const loading = ref(false)
const myJobs = ref([])
const selectedJobId = ref(null)
const keyword = ref('')
const applications = ref([])
const allApplications = ref([])

// 加载当前公司的职位列表
const loadMyJobs = async () => {
  const companyId = user.value?.companyId
  if (!companyId) {
    myJobs.value = []
    return
  }
  try {
    const res = await getMyJobs({ companyId, size: 9999 })
    myJobs.value = res.data?.records || []
  } catch (e) {
    console.error('加载职位列表失败', e)
  }
}

// 加载投递列表
const loadApplications = async () => {
  loading.value = true
  try {
    let res
    if (selectedJobId.value) {
      // 按职位筛选
      res = await getJobApplications(selectedJobId.value)
    } else {
      // 加载公司所有投递
      const companyId = user.value?.companyId
      if (!companyId) {
        applications.value = []
        allApplications.value = []
        return
      }
      res = await getCompanyApplications(companyId)
    }
    allApplications.value = res.data || []
    filterApplications()
  } catch (e) {
    ElMessage.error('加载投递列表失败')
  } finally {
    loading.value = false
  }
}

// 按关键字筛选
const filterApplications = () => {
  if (!keyword.value.trim()) {
    applications.value = allApplications.value
  } else {
    const kw = keyword.value.trim().toLowerCase()
    applications.value = allApplications.value.filter(item =>
      item.username?.toLowerCase().includes(kw)
    )
  }
}

// 搜索防抖
let searchTimer = null
const handleSearch = () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(() => {
    filterApplications()
  }, 300)
}

// 更新投递状态
const updateStatus = async (id, status) => {
  try {
    await updateApplicationStatus(id, { status })
    ElMessage.success('状态更新成功')
    await loadApplications()
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '更新失败')
  }
}

// 拒绝（需要填写拒绝原因）
const handleReject = async (id) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入拒绝原因', '拒绝申请', {
      inputPlaceholder: '拒绝原因（可选）',
      inputType: 'textarea',
      type: 'warning'
    })
    await updateApplicationStatus(id, { status: 'rejected', rejectReason: value || '' })
    ElMessage.success('已拒绝')
    await loadApplications()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '操作失败')
    }
  }
}

// 格式化日期时间
const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

// 状态样式
const getStatusType = (status) => {
  const map = {
    pending: 'info',
    screening: 'warning',
    interview: '',
    offer: 'success',
    rejected: 'danger',
    withdrawn: 'info'
  }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = {
    pending: '待处理',
    screening: '筛选中',
    interview: '面试中',
    offer: '已录用',
    rejected: '已拒绝',
    withdrawn: '已撤回'
  }
  return map[status] || status
}

onMounted(() => {
  loadMyJobs()
  loadApplications()
})
</script>

<style scoped>
.applications {
  min-height: calc(100vh - 64px);
  background: #f5f6f8;
  padding: 24px 40px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 20px 0;
}

.filter-card {
  margin-bottom: 16px;
}

.filter-card :deep(.el-card__body) {
  padding: 16px 20px;
}

.filter-bar {
  display: flex;
  align-items: center;
  gap: 12px;
}

.filter-label {
  font-weight: 500;
  white-space: nowrap;
}

:deep(.el-card__body) {
  padding: 0;
}

.empty-state {
  padding: 48px 0;
}
</style>
