<template>
  <AppShell :bgImage="false">
    <div class="hr-dashboard-bg">
    <div class="hr-dashboard">
      <!-- 顶部欢迎区 -->
      <div class="welcome-section">
        <div class="welcome-text">
          <h1>欢迎回来，{{ user?.nickname || 'HR' }}</h1>
          <p class="welcome-desc">以下是您的招聘工作台概览</p>
        </div>
        <div class="welcome-actions">
          <el-button @click="$router.push('/hr/jobs')">管理职位</el-button>
          <el-button type="primary" @click="$router.push('/hr/job/publish')">发布职位</el-button>
        </div>
      </div>

      <!-- 统计卡片 -->
      <div class="stats-grid">
        <div class="stat-card stat-card--green">
          <div class="stat-card__icon">
            <el-icon :size="28"><Briefcase /></el-icon>
          </div>
          <div class="stat-card__info">
            <div class="stat-card__value">{{ stats.jobCount }}</div>
            <div class="stat-card__label">公司岗位</div>
          </div>
        </div>
        <div class="stat-card stat-card--blue">
          <div class="stat-card__icon">
            <el-icon :size="28"><User /></el-icon>
          </div>
          <div class="stat-card__info">
            <div class="stat-card__value">{{ stats.myJobCount }}</div>
            <div class="stat-card__label">我的岗位</div>
          </div>
        </div>
        <div class="stat-card stat-card--orange">
          <div class="stat-card__icon">
            <el-icon :size="28"><Document /></el-icon>
          </div>
          <div class="stat-card__info">
            <div class="stat-card__value">{{ stats.applicationCount }}</div>
            <div class="stat-card__label">收到申请</div>
          </div>
        </div>
        <div class="stat-card stat-card--purple">
          <div class="stat-card__icon">
            <el-icon :size="28"><View /></el-icon>
          </div>
          <div class="stat-card__info">
            <div class="stat-card__value">{{ stats.viewCount }}</div>
            <div class="stat-card__label">职位浏览</div>
          </div>
        </div>
        <div class="stat-card stat-card--cyan">
          <div class="stat-card__icon">
            <el-icon :size="28"><Clock /></el-icon>
          </div>
          <div class="stat-card__info">
            <div class="stat-card__value">{{ stats.newApplicationCount }}</div>
            <div class="stat-card__label">今日新申请</div>
          </div>
        </div>
      </div>

      <!-- 公司信息 + 快捷操作 -->
      <div class="two-col">
        <el-card class="col-card">
          <template #header>
            <div class="card-header">
              <span class="card-header__title">公司信息</span>
              <el-button text type="primary" size="small" @click="$router.push('/hr/company')">编辑</el-button>
            </div>
          </template>
          <div v-if="!company" class="empty-tip">
            <el-empty description="还未创建公司信息" :image-size="80">
              <el-button type="primary" size="small" @click="$router.push('/hr/company')">立即创建</el-button>
            </el-empty>
          </div>
          <div v-else class="company-detail">
            <div class="company-detail__header">
              <div class="company-logo">
                <img v-if="company.logo" :src="company.logo" :alt="company.name" />
                <span v-else class="company-logo-text">{{ company.name?.charAt(0) }}</span>
              </div>
              <div class="company-info">
                <div class="company-detail__name">{{ company.name }}</div>
                <div class="company-detail__row">
                  <span class="detail-tag detail-tag--industry" v-if="company.industry">{{ company.industry }}</span>
                  <span class="detail-tag detail-tag--scale" v-if="company.scale">{{ company.scale }}</span>
                  <span class="detail-tag detail-tag--location" v-if="company.regionPath || company.city">{{ company.regionPath || company.city }}</span>
                </div>
              </div>
            </div>
            <div v-if="company.description" class="company-detail__desc">{{ company.description }}</div>
            <div class="company-detail__footer">
              <span class="company-stat"><el-icon><View /></el-icon> {{ company.viewCount || 0 }} 浏览</span>
              <span class="company-stat"><el-icon><User /></el-icon> {{ company.followCount || 0 }} 关注</span>
            </div>
          </div>
        </el-card>

        <el-card class="col-card">
          <template #header>
            <div class="card-header">
              <span class="card-header__title">快捷操作</span>
            </div>
          </template>
          <div class="quick-actions">
            <div class="action-item" @click="$router.push('/hr/job/publish')">
              <div class="action-item__icon action-item__icon--green">
                <el-icon :size="20"><Plus /></el-icon>
              </div>
              <div class="action-item__text">
                <div class="action-item__title">发布新职位</div>
                <div class="action-item__desc">创建一个新的招聘职位</div>
              </div>
            </div>
            <div class="action-item" @click="$router.push('/hr/jobs')">
              <div class="action-item__icon action-item__icon--blue">
                <el-icon :size="20"><List /></el-icon>
              </div>
              <div class="action-item__text">
                <div class="action-item__title">管理职位</div>
                <div class="action-item__desc">查看和编辑已发布的职位</div>
              </div>
            </div>
            <div class="action-item" @click="$router.push('/hr/applications')">
              <div class="action-item__icon action-item__icon--orange">
                <el-icon :size="20"><User /></el-icon>
              </div>
              <div class="action-item__text">
                <div class="action-item__title">处理申请</div>
                <div class="action-item__desc">查看和管理候选人投递</div>
              </div>
            </div>
            <div class="action-item" @click="$router.push('/hr/company')">
              <div class="action-item__icon action-item__icon--purple">
                <el-icon :size="20"><OfficeBuilding /></el-icon>
              </div>
              <div class="action-item__text">
                <div class="action-item__title">公司主页</div>
                <div class="action-item__desc">完善公司信息展示</div>
              </div>
            </div>
          </div>
        </el-card>
      </div>

      <!-- 最新投递 -->
      <el-card class="full-card">
        <template #header>
          <div class="card-header">
            <span class="card-header__title">最新投递</span>
            <el-button text type="primary" size="small" @click="$router.push('/hr/applications')">查看全部</el-button>
          </div>
        </template>
        <div v-if="recentApplications.length === 0" class="empty-tip">
          <el-empty description="暂无投递记录" :image-size="80" />
        </div>
        <el-table v-else :data="recentApplications" style="width: 100%">
          <el-table-column prop="username" label="候选人" width="140" />
          <el-table-column prop="jobTitle" label="申请职位" min-width="160" />
          <el-table-column prop="status" label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" size="small">{{ getStatusText(row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="appliedAt" label="投递时间" width="160">
            <template #default="{ row }">
              {{ formatDateTime(row.appliedAt) }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
    </div>
  </AppShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Briefcase, Document, View, Clock, Plus, List, User, OfficeBuilding } from '@element-plus/icons-vue'
import AppShell from '@/components/AppShell.vue'
import { useUserStore } from '@/stores/user'
import { getCompanyDetail } from '@/api/company'
import { getMyJobs } from '@/api/job'
import { getCompanyApplications } from '@/api/application'

const userStore = useUserStore()
const { user } = userStore

const stats = ref({
  jobCount: 0,
  myJobCount: 0,
  applicationCount: 0,
  viewCount: 0,
  newApplicationCount: 0
})

const recentApplications = ref([])
const company = ref(null)

onMounted(async () => {
  await loadDashboardData()
})

const loadDashboardData = async () => {
  try {
    // 加载公司信息（通过用户关联的companyId查询公开接口）
    try {
      const companyId = user.value?.companyId
      if (companyId) {
        const companyRes = await getCompanyDetail(companyId)
        if (companyRes.success) {
          company.value = companyRes.data
        }
      }
    } catch (e) {
      // 公司信息可能不存在
    }

    // 加载公司职位统计
    const currentCompanyId = user.value?.companyId
    try {
      if (!currentCompanyId) {
        stats.value = { jobCount: 0, myJobCount: 0, applicationCount: 0, viewCount: 0 }
        return
      }
      const jobsRes = await getMyJobs({ companyId: currentCompanyId, size: 9999 })
      if (jobsRes.success) {
        const allJobs = jobsRes.data?.records || []
        stats.value.jobCount = allJobs.length
        // 我的岗位：当前HR负责的职位数
        stats.value.myJobCount = allJobs.filter(j => j.userId === user.value?.id).length
        stats.value.viewCount = allJobs.reduce((sum, j) => sum + (j.viewCount || 0), 0)
      }
    } catch (e) {
      // 接口可能还未实现
    }

    // 加载公司所有投递（一次查询，不再逐个职位遍历）
    try {
      const appRes = await getCompanyApplications(currentCompanyId)
      if (appRes.success && appRes.data) {
        const allApps = appRes.data
        // 按投递时间倒序，取最新5条展示
        recentApplications.value = allApps.slice(0, 5)
        stats.value.applicationCount = allApps.length
        stats.value.newApplicationCount = allApps.filter(a => {
          const d = new Date(a.appliedAt)
          const today = new Date()
          return d.toDateString() === today.toDateString()
        }).length
      }
    } catch (e) {
      // 接口可能还未实现
    }
  } catch (e) {
    console.error('加载数据失败', e)
  }
}

const formatDateTime = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleString('zh-CN')
}

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
</script>

<style scoped>
.hr-dashboard-bg {
  min-height: calc(100vh - 64px);
  background: url('@/assets/image/background4.webp') no-repeat center center;
  background-size: cover;
  background-attachment: fixed;
}

.hr-dashboard {
  width: 80%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 0;
}

/* 欢迎区 */
.welcome-section {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 28px;
}

.welcome-text h1 {
  font-size: 24px;
  font-weight: 600;
  margin: 0 0 6px;
  color: #1e293b;
}

.welcome-desc {
  font-size: 14px;
  color: #94a3b8;
  margin: 0;
}

.welcome-actions {
  display: flex;
  gap: 12px;
}

/* 统计卡片 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 20px;
  border-radius: 12px;
  background: #fff;
  border: 1px solid #e5e7eb;
  transition: box-shadow 0.2s;
}

.stat-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.stat-card__icon {
  width: 52px;
  height: 52px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.stat-card--green .stat-card__icon { background: #ecfdf5; color: #10b981; }
.stat-card--blue .stat-card__icon { background: #eff6ff; color: #3b82f6; }
.stat-card--orange .stat-card__icon { background: #fff7ed; color: #f97316; }
.stat-card--purple .stat-card__icon { background: #f5f3ff; color: #8b5cf6; }
.stat-card--cyan .stat-card__icon { background: #ecfeff; color: #06b6d4; }

.stat-card--green .stat-card__value { color: #10b981; }
.stat-card--blue .stat-card__value { color: #3b82f6; }
.stat-card--orange .stat-card__value { color: #f97316; }
.stat-card--purple .stat-card__value { color: #8b5cf6; }
.stat-card--cyan .stat-card__value { color: #06b6d4; }

.stat-card__value {
  font-size: 28px;
  font-weight: 700;
  line-height: 1.2;
}

.stat-card__label {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 2px;
}

/* 双列布局 */
.two-col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 20px;
  margin-bottom: 24px;
}

.col-card, .full-card {
  border-radius: 12px;
  border: 1px solid #e5e7eb;
}

.col-card :deep(.el-card__body),
.full-card :deep(.el-card__body) {
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid #f1f5f9;
}

.card-header__title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
}

/* 公司信息 */
.company-detail {
  padding: 20px;
}

.company-detail__header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 12px;
}

.company-logo {
  width: 60px;
  height: 60px;
  border-radius: 12px;
  background: linear-gradient(135deg, #10b981, #059669);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  overflow: hidden;
}

.company-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.company-logo-text {
  font-size: 24px;
  font-weight: 700;
  color: #fff;
}

.company-info {
  flex: 1;
}

.company-detail__name {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
}

.company-detail__row {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.detail-tag {
  padding: 4px 12px;
  font-size: 12px;
  border-radius: 6px;
  font-weight: 500;
}

.detail-tag--industry {
  background: #ecfdf5;
  color: #059669;
}

.detail-tag--scale {
  background: #eff6ff;
  color: #3b82f6;
}

.detail-tag--location {
  background: #fff7ed;
  color: #f97316;
}

.company-detail__desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  padding: 12px 0;
  border-top: 1px solid #f1f5f9;
  margin-top: 12px;
}

.company-detail__footer {
  display: flex;
  gap: 20px;
  padding-top: 12px;
  border-top: 1px solid #f1f5f9;
}

.company-stat {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #94a3b8;
}

/* 快捷操作 */
.quick-actions {
  padding: 12px 20px;
}

.action-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 0;
  cursor: pointer;
  border-bottom: 1px solid #f8fafc;
  transition: background 0.15s;
}

.action-item:last-child {
  border-bottom: none;
}

.action-item:hover {
  background: #f8fafc;
  margin: 0 -20px;
  padding-left: 20px;
  padding-right: 20px;
  border-radius: 8px;
}

.action-item__icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.action-item__icon--green { background: #ecfdf5; color: #10b981; }
.action-item__icon--blue { background: #eff6ff; color: #3b82f6; }
.action-item__icon--orange { background: #fff7ed; color: #f97316; }
.action-item__icon--purple { background: #f5f3ff; color: #8b5cf6; }

.action-item__title {
  font-size: 14px;
  font-weight: 500;
  color: #1e293b;
}

.action-item__desc {
  font-size: 12px;
  color: #94a3b8;
  margin-top: 2px;
}

/* 空状态 */
.empty-tip {
  padding: 24px 0;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: repeat(2, 1fr);
  }

  .two-col {
    grid-template-columns: 1fr;
  }

  .welcome-section {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>
