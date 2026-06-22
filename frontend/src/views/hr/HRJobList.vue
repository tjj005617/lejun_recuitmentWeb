<template>
  <AppShell :bgImage="false">
    <div class="job-list">
      <div class="page-header">
        <h1>职位管理</h1>
        <el-button type="primary" @click="$router.push('/hr/job/publish')">
          发布职位
        </el-button>
      </div>

      <!-- 搜索栏 -->
      <el-card class="search-card">
        <el-form :inline="true" @submit.prevent="handleSearch">
          <el-form-item label="职位名称">
            <el-input v-model="searchForm.keyword" placeholder="搜索职位名称" clearable style="width: 200px" />
          </el-form-item>
          <el-form-item label="城市">
            <el-input v-model="searchForm.city" placeholder="城市" clearable style="width: 120px" />
          </el-form-item>
          <el-form-item label="状态">
            <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width: 120px">
              <el-option label="招聘中" value="active" />
              <el-option label="已暂停" value="paused" />
              <el-option label="已关闭" value="closed" />
            </el-select>
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSearch">搜索</el-button>
            <el-button @click="handleReset">重置</el-button>
          </el-form-item>
        </el-form>
      </el-card>

      <!-- 职位表格 -->
      <el-card>
        <el-table :data="jobs" v-loading="loading" style="width: 100%">
          <el-table-column prop="title" label="职位名称" min-width="180">
            <template #default="{ row }">
              <router-link :to="`/job/${row.id}`" class="job-link">{{ row.title }}</router-link>
            </template>
          </el-table-column>

          <el-table-column prop="city" label="城市" width="100" />

          <el-table-column prop="salaryMin" label="薪资" width="140">
            <template #default="{ row }">
              {{ row.salaryMin && row.salaryMax ? `${row.salaryMin}-${row.salaryMax}元/月` : '面议' }}
            </template>
          </el-table-column>

          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="viewCount" label="浏览" width="80" />

          <el-table-column prop="applyCount" label="投递" width="80" />

          <el-table-column label="负责HR" width="120">
            <template #default="{ row }">
              <el-tag v-if="row.hrName" size="small" type="primary">{{ row.hrName }}</el-tag>
              <el-tag v-else size="small" type="info">待认领</el-tag>
            </template>
          </el-table-column>

          <el-table-column prop="publishedAt" label="发布时间" width="120">
            <template #default="{ row }">
              {{ formatDate(row.publishedAt) }}
            </template>
          </el-table-column>

          <el-table-column label="操作" width="220" fixed="right">
            <template #default="{ row }">
              <el-button v-if="!row.userId" text type="success" size="small" @click="handleClaim(row)">
                认领
              </el-button>
              <el-button text type="primary" size="small" @click="editJob(row.id)">
                编辑
              </el-button>
              <el-button text type="danger" size="small" @click="handleDelete(row)">
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <div v-if="jobs.length === 0 && !loading" class="empty-state">
          <el-empty description="暂无职位">
            <el-button type="primary" @click="$router.push('/hr/job/publish')">发布第一个职位</el-button>
          </el-empty>
        </div>

        <!-- 分页 -->
        <div v-if="total > 0" class="pagination-wrap">
          <el-pagination
            v-model:current-page="pagination.page"
            v-model:page-size="pagination.size"
            :total="total"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="loadJobs()"
            @current-change="loadJobs()"
          />
        </div>
      </el-card>
    </div>
  </AppShell>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AppShell from '@/components/AppShell.vue'
import { getMyJobs, deleteJob, claimJob } from '@/api/job'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const { user } = userStore
const loading = ref(false)
const jobs = ref([])
const total = ref(0)

// 搜索条件
const searchForm = reactive({
  keyword: '',
  city: '',
  status: ''
})

// 分页参数
const pagination = reactive({
  page: 1,
  size: 20
})

// 加载职位列表
const loadJobs = async () => {
  const companyId = user.value?.companyId
  if (!companyId) {
    jobs.value = []
    total.value = 0
    return
  }
  loading.value = true
  try {
    const res = await getMyJobs({
      companyId,
      keyword: searchForm.keyword || undefined,
      city: searchForm.city || undefined,
      status: searchForm.status || undefined,
      page: pagination.page,
      size: pagination.size
    })
    jobs.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (e) {
    ElMessage.error('加载职位列表失败')
  } finally {
    loading.value = false
  }
}

// 搜索
const handleSearch = () => {
  pagination.page = 1
  loadJobs()
}

// 重置搜索条件
const handleReset = () => {
  searchForm.keyword = ''
  searchForm.city = ''
  searchForm.status = ''
  pagination.page = 1
  loadJobs()
}

// 编辑职位
const editJob = (id) => {
  router.push(`/hr/job/${id}/edit`)
}

// 删除职位
const handleDelete = async (job) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除职位「${job.title}」吗？`,
      '确认删除',
      { type: 'warning' }
    )
    await deleteJob(job.id)
    ElMessage.success('删除成功')
    await loadJobs()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '删除失败')
    }
  }
}

// 认领职位
const handleClaim = async (job) => {
  try {
    await ElMessageBox.confirm(
      `确定要认领职位「${job.title}」吗？认领后你将负责该职位的招聘。`,
      '确认认领',
      { type: 'info' }
    )
    await claimJob(job.id)
    ElMessage.success('认领成功')
    await loadJobs()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.response?.data?.message || '认领失败')
    }
  }
}

// 格式化日期
const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

// 状态样式
const getStatusType = (status) => {
  const map = { active: 'success', paused: 'warning', closed: 'info' }
  return map[status] || 'info'
}

const getStatusText = (status) => {
  const map = { active: '招聘中', paused: '已暂停', closed: '已关闭' }
  return map[status] || status
}

onMounted(() => {
  loadJobs()
})
</script>

<style scoped>
.job-list {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.page-header h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 0;
}

.search-card {
  margin-bottom: 16px;
}

.search-card .el-form-item {
  margin-bottom: 0;
}

.job-link {
  color: #3b82f6;
  text-decoration: none;
}

.job-link:hover {
  text-decoration: underline;
}

.empty-state {
  padding: 48px 0;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
