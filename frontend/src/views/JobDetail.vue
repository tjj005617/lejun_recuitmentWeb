<template>
  <AppShell :bgImage="false">
    <div class="job-detail">
      <div class="job-detail__container">
        <!-- 主卡片：职位信息 + 描述 + 要求 + 福利 + 公司信息 -->
        <div class="main-card">
          <!-- 顶部：职位标题 + 薪资 + 标签 -->
          <div class="main-card__header">
              <h1 class="job-title">{{ job.title }}</h1>
            <div class="job-salary" v-if="job.salaryMin || job.salaryMax">
              {{ formatSalary(job.salaryMin, job.salaryMax) }}
            </div>
            <div class="job-tags">
              <span class="job-tag" v-if="job.city">{{ job.city }}</span>
              <span class="job-tag" v-if="job.experience">{{ job.experience }}</span>
              <span class="job-tag" v-if="job.education">{{ job.education }}</span>
              <span class="job-tag job-tag--type">{{ formatJobType(job.jobType) }}</span>
            </div>
            <div class="job-meta">
              <span>发布时间：{{ formatDate(job.publishedAt) }}</span>
              <span>浏览：{{ job.viewCount || 0 }}人</span>
              <span>申请：{{ job.applyCount || 0 }}人</span>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="main-card__actions">
            <el-button @click="handleChat">沟通</el-button>
            <el-button type="primary" :disabled="hasApplied" @click="handleApply">
              {{ hasApplied ? '已投递' : '投递' }}
            </el-button>
            <el-button @click="handleFavorite" :type="isFavorite ? 'warning' : 'default'">
              {{ isFavorite ? '已收藏' : '收藏' }}
            </el-button>
          </div>

          <!-- HR信息栏 -->
          <div class="hr-bar" v-if="job.hr" @click="openHrJobs">
            <el-avatar :size="44" class="hr-bar__avatar">
              {{ job.hr.nickname?.charAt(0) || job.hr.username?.charAt(0) || 'HR' }}
            </el-avatar>
            <div class="hr-bar__info">
              <span class="hr-bar__name">{{ job.hr.nickname || job.hr.username }}</span>
              <span class="hr-bar__badge">HR</span>
            </div>
            <el-button type="primary" plain size="small" class="hr-bar__btn" @click.stop="handleChat">和TA沟通</el-button>
          </div>

          <!-- 分割线 -->
          <div class="main-card__divider"></div>

          <!-- 职位描述 -->
          <div class="main-card__section">
            <h2 class="section-title">职位描述</h2>
            <div class="job-description" v-html="job.description || '暂无描述'"></div>
          </div>

          <!-- 任职要求 -->
          <div class="main-card__section" v-if="job.requirements">
            <h2 class="section-title">任职要求</h2>
            <div class="job-requirements" v-html="job.requirements"></div>
          </div>

          <!-- 职位福利 -->
          <div class="main-card__section" v-if="job.benefits">
            <h2 class="section-title">职位福利</h2>
            <div class="job-benefits">
              <el-tag v-for="benefit in jobBenefits" :key="benefit" type="success" effect="plain">
                {{ benefit }}
              </el-tag>
            </div>
          </div>

          <!-- 公司信息（合并在同一卡片内） -->
          <template v-if="company">
            <div class="main-card__divider"></div>
            <div class="company-section" @click="$router.push(`/company/${company.id}`)">
              <div class="company-section__header">
                <div class="company-logo">
                  <img v-if="company.logo" :src="company.logo" :alt="company.name" />
                  <span v-else class="company-logo-text">{{ company.name?.charAt(0) }}</span>
                </div>
                <div class="company-detail">
                  <h3 class="company-name">{{ company.name }}</h3>
                  <div class="company-meta">
                    <span v-if="company.industry">{{ company.industry }}</span>
                    <span v-if="company.scale">· {{ company.scale }}</span>
                    <span v-if="company.type">· {{ company.type }}</span>
                  </div>
                  <div class="company-meta" v-if="company.city || company.address">
                    <span>{{ company.city }}</span>
                    <span v-if="company.address">· {{ company.address }}</span>
                  </div>
                </div>
                <el-icon class="company-arrow"><ArrowRight /></el-icon>
              </div>
              <!-- 公司介绍 -->
              <div class="company-section__desc" v-if="company.description">
                {{ company.description }}
              </div>
              <!-- 公司福利 -->
              <div class="company-section__benefits" v-if="companyBenefits.length > 0">
                <el-tag v-for="b in companyBenefits" :key="b" type="success" effect="plain" size="small">
                  {{ b }}
                </el-tag>
              </div>
              <!-- 公司官网 -->
              <div class="company-section__website" v-if="company.website">
                <span class="label">官网：</span>
                <a :href="company.website" target="_blank" @click.stop>{{ company.website }}</a>
              </div>
            </div>
          </template>
        </div>

        <!-- 猜你喜欢 -->
        <div class="guess-section" v-if="guessJobs.length > 0">
          <div class="guess-section__header">
            <h2 class="guess-section__title">猜你喜欢</h2>
            <el-button text @click="loadGuessJobs">
              <el-icon><Refresh /></el-icon>
              <span>换一批</span>
            </el-button>
          </div>
          <div class="guess-section__list">
            <div
              class="guess-card"
              v-for="item in guessJobs"
              :key="item.id"
              @click="$router.push(`/job/${item.id}`)"
            >
              <div class="guess-card__top">
                <h3 class="guess-card__title">{{ item.title }}</h3>
                <span class="guess-card__salary" v-if="item.salaryMin || item.salaryMax">
                  {{ formatSalary(item.salaryMin, item.salaryMax) }}
                </span>
              </div>
              <div class="guess-card__company">{{ item.companyName }}</div>
              <div class="guess-card__tags">
                <span class="guess-tag" v-if="item.city">{{ item.city }}</span>
                <span class="guess-tag" v-if="item.experience">{{ item.experience }}</span>
                <span class="guess-tag" v-if="item.education">{{ item.education }}</span>
              </div>
            </div>
          </div>
        </div>

        <!-- 简历选择弹窗 -->
        <el-dialog v-model="applyDialogVisible" title="选择简历投递" width="480px" :close-on-click-modal="false">
          <div class="resume-select-list">
            <div
              v-for="resume in userResumes"
              :key="resume.id"
              class="resume-select-item"
              :class="{ active: selectedResumeId === resume.id }"
              @click="selectedResumeId = resume.id"
            >
              <div class="resume-select-item__name">{{ resume.fileName || resume.name || '未命名简历' }}</div>
              <div class="resume-select-item__meta">
                <span v-if="resume.name">{{ resume.name }}</span>
                <span v-if="resume.education"> · {{ resume.education }}</span>
              </div>
            </div>
          </div>
          <template #footer>
            <el-button @click="applyDialogVisible = false">取消</el-button>
            <el-button
              type="primary"
              :disabled="!selectedResumeId"
              :loading="applyLoading"
              @click="confirmApply"
            >
              确认投递
            </el-button>
          </template>
        </el-dialog>

        <!-- HR负责的岗位抽屉 -->
        <el-drawer
          v-model="hrJobsDrawerVisible"
          :title="hrJobsDrawerTitle"
          direction="rtl"
          size="420px"
          :before-close="() => hrJobsDrawerVisible = false"
        >
          <div v-if="hrJobsLoading" class="hr-jobs-loading">
            <el-icon class="is-loading"><Loading /></el-icon>
            <span>加载中...</span>
          </div>
          <div v-else-if="hrJobsList.length === 0" class="hr-jobs-empty">
            <el-empty description="暂无在招岗位" />
          </div>
          <div v-else class="hr-jobs-list">
            <div
              v-for="item in hrJobsList"
              :key="item.id"
              class="hr-job-item"
              :class="{ active: item.id === job.id }"
              @click="hrJobsDrawerVisible = false; $router.push(`/job/${item.id}`)"
            >
              <div class="hr-job-item__top">
                <span class="hr-job-item__title">{{ item.title }}</span>
                <span class="hr-job-item__salary" v-if="item.salaryMin || item.salaryMax">
                  {{ formatSalary(item.salaryMin, item.salaryMax) }}
                </span>
              </div>
              <div class="hr-job-item__tags">
                <span class="hr-job-tag" v-if="item.city">{{ item.city }}</span>
                <span class="hr-job-tag" v-if="item.experience">{{ item.experience }}</span>
                <span class="hr-job-tag" v-if="item.education">{{ item.education }}</span>
              </div>
            </div>
          </div>
        </el-drawer>
      </div>
    </div>
  </AppShell>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowRight, Refresh, Loading } from '@element-plus/icons-vue'
import AppShell from '@/components/AppShell.vue'
import { getJobDetail, toggleFavorite, checkFavorite, getGuessYouLike } from '@/api/job'
import { applyJob, getMyApplications } from '@/api/application'
import { searchJobsByES } from '@/api/search'
import { getUserResumes, getResume } from '@/api/resume'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { user, isLoggedIn } = userStore

const job = ref({
  id: 1,
  title: 'Java开发工程师',
  companyName: '字节跳动',
  salaryMin: 15000,
  salaryMax: 30000,
  city: '北京',
  experience: '3-5年',
  education: '本科',
  jobType: 'full_time',
  description: '<p>负责公司核心业务系统的开发和维护</p><p>参与系统架构设计和技术选型</p><p>编写高质量、可维护的代码</p>',
  requirements: '<p>1. 3年以上Java开发经验</p><p>2. 熟悉Spring Boot、MyBatis等主流框架</p><p>3. 熟悉MySQL、Redis等数据库</p><p>4. 良好的沟通能力和团队协作精神</p>',
  benefits: ['五险一金', '带薪年假', '弹性工作', '免费三餐', '团建活动'],
  publishedAt: new Date(Date.now() - 86400000).toISOString(),
  viewCount: 256,
  applyCount: 32
})

const company = ref({
  id: 1,
  name: '字节跳动',
  industry: '互联网',
  scale: '10000人以上',
  city: '北京'
})

const isFavorite = ref(false)
const guessJobs = ref([])

// 投递相关状态
const hasApplied = ref(false)
const applyDialogVisible = ref(false)
const userResumes = ref([])
const selectedResumeId = ref(null)
const applyLoading = ref(false)

// HR岗位抽屉相关
const hrJobsDrawerVisible = ref(false)
const hrJobsDrawerTitle = ref('')
const hrJobsList = ref([])
const hrJobsLoading = ref(false)

/** 打开HR岗位抽屉 */
const openHrJobs = async () => {
  if (!job.value.hr) return
  hrJobsDrawerTitle.value = `${job.value.hr.nickname || job.value.hr.username} 负责的岗位`
  hrJobsDrawerVisible.value = true
  hrJobsLoading.value = true
  try {
    const res = await searchJobsByES({ hrUserId: job.value.hr.id, size: 50 })
    hrJobsList.value = res?.data?.records || []
  } catch {
    hrJobsList.value = []
  } finally {
    hrJobsLoading.value = false
  }
}

/** 检查当前用户是否已投递该职位 */
const checkAppliedStatus = async () => {
  try {
    const res = await getMyApplications()
    if (res?.data) {
      hasApplied.value = res.data.some(app => app.jobId === Number(route.params.id))
    }
  } catch {
    // 静默
  }
}

/** 点击投递按钮 */
const handleApply = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  if (hasApplied.value) {
    ElMessage.info('您已投递过该职位')
    return
  }

  // 加载用户简历列表
  try {
    const resumeRes = await getUserResumes(user.value.id)
    const resumeIds = resumeRes.data || []
    if (resumeIds.length === 0) {
      ElMessageBox.confirm('您还没有简历，请先上传简历', '提示', {
        confirmButtonText: '去上传',
        showCancelButton: false,
        type: 'info'
      }).then(() => router.push('/user'))
      return
    }

    // 加载每份简历的详情
    const resumeList = []
    for (const id of resumeIds) {
      const detail = await getResume(id)
      if (detail?.data) resumeList.push(detail.data)
    }
    userResumes.value = resumeList

    // 只有一份简历，直接投递
    if (resumeList.length === 1) {
      selectedResumeId.value = resumeList[0].id
      await confirmApply()
    } else {
      // 多份简历，弹窗选择
      applyDialogVisible.value = true
    }
  } catch (e) {
    ElMessage.error('加载简历失败')
  }
}

/** 确认投递 */
const confirmApply = async () => {
  if (!selectedResumeId.value) return
  applyLoading.value = true
  try {
    await applyJob({ jobId: Number(route.params.id), resumeId: selectedResumeId.value })
    hasApplied.value = true
    applyDialogVisible.value = false
    job.value.applyCount = (job.value.applyCount || 0) + 1
    ElMessage.success('投递成功')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '投递失败')
  } finally {
    applyLoading.value = false
  }
}

const loadGuessJobs = async () => {
  try {
    const res = await getGuessYouLike(route.params.id)
    if (res?.data) {
      guessJobs.value = res.data
    }
  } catch (e) {
    // 忽略
  }
}

const jobBenefits = computed(() => {
  if (!job.value.benefits) return []
  if (Array.isArray(job.value.benefits)) return job.value.benefits
  try {
    return JSON.parse(job.value.benefits)
  } catch {
    return []
  }
})

const companyBenefits = computed(() => {
  if (!company.value?.benefits) return []
  if (Array.isArray(company.value.benefits)) return company.value.benefits
  try {
    return JSON.parse(company.value.benefits)
  } catch {
    return []
  }
})

const formatSalary = (min, max) => {
  if (min && max) {
    return `${(min / 1000).toFixed(0)}K-${(max / 1000).toFixed(0)}K`
  } else if (min) {
    return `${(min / 1000).toFixed(0)}K起`
  } else if (max) {
    return `最高${(max / 1000).toFixed(0)}K`
  }
  return '面议'
}

const formatJobType = (type) => {
  const map = { full_time: '全职', part_time: '兼职', intern: '实习' }
  return map[type] || type
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  return new Date(dateStr).toLocaleDateString('zh-CN')
}

const handleChat = () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  // 跳转聊天页，传递HR userId和职位信息
  router.push({
    path: '/messages',
    query: { hrUserId: job.value.userId, jobId: job.value.id, jobTitle: job.value.title }
  })
}

const handleFavorite = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  try {
    await toggleFavorite(job.value.id)
    isFavorite.value = !isFavorite.value
    ElMessage.success(isFavorite.value ? '已收藏' : '已取消收藏')
  } catch (e) {
    ElMessage.error(e.message || '操作失败')
  }
}

const loadJobDetail = async (jobId) => {
  try {
    const res = await getJobDetail(jobId)
    if (res?.data) {
      job.value = res.data
      company.value = res.data.company
    }
  } catch (e) {
    // 使用默认数据
  }

  // 已登录用户检查收藏状态
  if (isLoggedIn.value) {
    try {
      const favRes = await checkFavorite(jobId)
      if (favRes?.data) {
        isFavorite.value = favRes.data.favorited
      }
    } catch (e) {
      // 忽略
    }
    // 检查是否已投递
    checkAppliedStatus()
  }

  // 加载猜你喜欢
  loadGuessJobs()
}

onMounted(() => {
  loadJobDetail(route.params.id)
})

// 监听路由参数变化，同组件切换时重新加载数据
watch(() => route.params.id, (newId) => {
  if (newId) {
    loadJobDetail(newId)
  }
})
</script>

<style scoped>
.job-detail {
  min-height: calc(100vh - 64px);
  background: #f0f2f5;
}

.job-detail__container {
  max-width: 860px;
  margin: 0 auto;
  padding: 28px 24px;
}

/* 主卡片 */
.main-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

.main-card__header {
  margin-bottom: 20px;
}

.main-card__actions {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.hr-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  margin-bottom: 20px;
}

.hr-bar__avatar {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  font-weight: 600;
  font-size: 16px;
  flex-shrink: 0;
}

.hr-bar__info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.hr-bar__name {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
}

.hr-bar__badge {
  font-size: 11px;
  color: #10b981;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 4px;
  padding: 2px 8px;
  font-weight: 600;
}

.hr-bar__btn {
  flex-shrink: 0;
}

.job-title {
  font-size: 26px;
  font-weight: 700;
  color: #0f172a;
  margin: 0 0 12px;
  line-height: 1.3;
}

.job-salary {
  font-size: 30px;
  font-weight: 800;
  color: #10b981;
  margin-bottom: 16px;
}

.job-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.job-tag {
  background: #f1f5f9;
  color: #475569;
  font-size: 13px;
  padding: 5px 14px;
  border-radius: 6px;
  font-weight: 500;
}

.job-tag--type {
  background: #ecfdf5;
  color: #059669;
}

.job-meta {
  display: flex;
  gap: 20px;
  font-size: 13px;
  color: #94a3b8;
}

/* 操作按钮 */
.main-card__actions {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
}

.main-card__actions .el-button {
  min-width: 100px;
  height: 40px;
  border-radius: 8px;
}

/* 分割线 */
.main-card__divider {
  height: 1px;
  background: linear-gradient(to right, transparent, #e5e7eb, transparent);
  margin-bottom: 24px;
}

/* 内容区块 */
.main-card__section {
  margin-bottom: 24px;
}

.main-card__section:last-child {
  margin-bottom: 0;
}

.section-title {
  font-size: 17px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 14px;
  padding-left: 12px;
  border-left: 3px solid #10b981;
}

.job-description,
.job-requirements {
  font-size: 14px;
  line-height: 1.9;
  color: #475569;
}

.job-description :deep(p),
.job-requirements :deep(p) {
  margin: 0 0 10px;
}

.job-benefits {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.job-benefits :deep(.el-tag) {
  border-radius: 8px;
  padding: 6px 16px;
  font-size: 13px;
}

/* 公司信息区块（合并在主卡片内） */
.company-section {
  cursor: pointer;
  padding: 4px 0;
}

.company-section:hover .company-name {
  color: #10b981;
}

.company-section__header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.company-logo {
  width: 52px;
  height: 52px;
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
  font-size: 22px;
  font-weight: 700;
  color: #fff;
}

.company-detail {
  flex: 1;
}

.company-name {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 4px;
  transition: color 0.2s;
}

.company-meta {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 2px;
}

.company-arrow {
  color: #94a3b8;
  font-size: 18px;
}

.company-section__desc {
  margin-top: 14px;
  font-size: 13px;
  color: #64748b;
  line-height: 1.7;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.company-section__benefits {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 12px;
}

.company-section__benefits :deep(.el-tag) {
  border-radius: 6px;
}

.company-section__website {
  margin-top: 12px;
  font-size: 13px;
  color: #64748b;
}

.company-section__website .label {
  color: #94a3b8;
}

.company-section__website a {
  color: #10b981;
  text-decoration: none;
}

.company-section__website a:hover {
  text-decoration: underline;
}

/* 猜你喜欢 */
.guess-section {
  margin-top: 20px;
}

.guess-section__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 14px;
}

.guess-section__title {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
}

.guess-section__header .el-button {
  color: #64748b;
  font-size: 13px;
}

.guess-section__header .el-button:hover {
  color: #10b981;
}

.guess-section__list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.guess-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
  cursor: pointer;
  transition: all 0.2s;
}

.guess-card:hover {
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.08);
  transform: translateY(-2px);
}

.guess-card__top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 8px;
}

.guess-card__title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.guess-card__salary {
  font-size: 15px;
  font-weight: 700;
  color: #10b981;
  margin-left: 12px;
  white-space: nowrap;
}

.guess-card__company {
  font-size: 13px;
  color: #64748b;
  margin-bottom: 10px;
}

.guess-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.guess-tag {
  background: #f1f5f9;
  color: #64748b;
  font-size: 12px;
  padding: 3px 10px;
  border-radius: 4px;
}

@media (max-width: 768px) {
  .guess-section__list {
    grid-template-columns: 1fr;
  }
}

/* 移动端适配 */
@media (max-width: 768px) {
  .main-card {
    padding: 24px 20px;
  }

  .job-title {
    font-size: 22px;
  }

  .job-salary {
    font-size: 26px;
  }

  .job-meta {
    flex-wrap: wrap;
    gap: 12px;
  }
}

/* 简历选择弹窗 */
.resume-select-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.resume-select-item {
  padding: 16px 20px;
  background: #fafbfc;
  border: 2px solid transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.resume-select-item:hover {
  border-color: #d1d5db;
}

.resume-select-item.active {
  border-color: #10b981;
  background: #ecfdf5;
}

.resume-select-item__name {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 4px;
}

.resume-select-item__meta {
  font-size: 13px;
  color: #64748b;
}

/* HR岗位抽屉 */
.hr-jobs-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 48px 0;
  color: #94a3b8;
  font-size: 14px;
}

.hr-jobs-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hr-job-item {
  padding: 16px;
  background: #fafbfc;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.hr-job-item:hover {
  border-color: #10b981;
  background: #f0fdf4;
}

.hr-job-item.active {
  border-color: #10b981;
  background: #ecfdf5;
}

.hr-job-item__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.hr-job-item__title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hr-job-item__salary {
  font-size: 15px;
  font-weight: 700;
  color: #10b981;
  margin-left: 12px;
  white-space: nowrap;
}

.hr-job-item__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.hr-job-tag {
  font-size: 12px;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
}
</style>
