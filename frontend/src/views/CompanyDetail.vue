<template>
  <AppShell :bgImage="false">
    <div class="company-detail">
      <div class="company-detail__container">
        <!-- 单一卡片：头部 + 介绍 + 福利 + 职位 -->
        <div class="main-card">
          <!-- 公司头部 -->
          <div class="company-header">
            <div class="company-logo">
              <img v-if="company.logo" :src="company.logo" :alt="company.name" />
              <span v-else class="company-logo-text">{{ company.name?.charAt(0) }}</span>
            </div>
            <div class="company-info">
              <h1 class="company-name">{{ company.name }}</h1>
              <div class="company-meta">
                <span v-if="company.industry">{{ company.industry }}</span>
                <span v-if="company.scale">· {{ company.scale }}</span>
                <span v-if="company.city">· {{ company.city }}</span>
              </div>
              <div class="company-tags">
                <el-tag v-if="company.type" type="info">{{ company.type }}</el-tag>
                <el-tag v-if="company.website" type="success" effect="plain">
                  <a :href="company.website" target="_blank">官网</a>
                </el-tag>
              </div>
            </div>
            <div class="company-actions">
              <el-button
                :type="isFollowed ? 'success' : 'default'"
                @click="handleFollow"
              >
                <el-icon v-if="isFollowed"><Star /></el-icon>
                {{ isFollowed ? '已关注' : '关注' }}
              </el-button>
            </div>
          </div>

          <!-- 分割线 -->
          <div class="divider" v-if="company.description || companyBenefits.length"></div>

          <!-- 公司介绍 -->
          <div class="section" v-if="company.description">
            <h2 class="section__title">公司介绍</h2>
            <div class="section__content company-description" v-html="company.description"></div>
          </div>

          <!-- 福利待遇 -->
          <div class="section" v-if="companyBenefits.length > 0">
            <h2 class="section__title">福利待遇</h2>
            <div class="section__content company-benefits">
              <el-tag v-for="benefit in companyBenefits" :key="benefit" type="success" effect="plain">
                {{ benefit }}
              </el-tag>
            </div>
          </div>

          <!-- 分割线 -->
          <div class="divider" v-if="jobList.length > 0"></div>

          <!-- 在招职位 -->
          <div class="section" v-if="jobTotal > 0 || hasActiveFilters">
            <h2 class="section__title">在招职位（{{ jobTotal }}）</h2>

            <!-- 筛选条件 -->
            <div class="job-filters">
              <div class="job-filters__row">
                <el-input
                  v-model="filterKeyword"
                  placeholder="搜索职位名称"
                  clearable
                  size="small"
                  style="width: 180px"
                  @keyup.enter="handleFilterChange"
                  @clear="handleFilterChange"
                />
                <el-select v-model="filterCity" placeholder="城市" clearable size="small" style="width: 110px" @change="handleFilterChange">
                  <el-option v-for="c in cities" :key="c" :label="c" :value="c" />
                </el-select>
                <el-select v-model="filterJobType" placeholder="工作类型" clearable size="small" style="width: 120px" @change="handleFilterChange">
                  <el-option label="全职" value="full_time" />
                  <el-option label="兼职" value="part_time" />
                  <el-option label="实习" value="intern" />
                </el-select>
                <el-select v-model="filterExperience" placeholder="经验要求" clearable size="small" style="width: 120px" @change="handleFilterChange">
                  <el-option v-for="exp in experiences" :key="exp" :label="exp" :value="exp === '不限' ? '' : exp" />
                </el-select>
                <el-select v-model="filterEducation" placeholder="学历要求" clearable size="small" style="width: 120px" @change="handleFilterChange">
                  <el-option v-for="edu in educations" :key="edu" :label="edu" :value="edu === '不限' ? '' : edu" />
                </el-select>
                <el-button text type="info" size="small" @click="resetJobFilters" v-if="hasActiveFilters">重置</el-button>
              </div>
            </div>

            <div class="job-list">
              <JobCard v-for="job in jobList" :key="job.id" :job="job" />
            </div>
            <div class="job-pagination" v-if="jobTotal > jobPageSize">
              <el-pagination
                background
                layout="prev, pager, next"
                :total="jobTotal"
                :page-size="jobPageSize"
                :current-page="jobPage"
                @current-change="handleJobPageChange"
              />
            </div>
          </div>
        </div>
      </div>
    </div>
  </AppShell>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star } from '@element-plus/icons-vue'
import AppShell from '@/components/AppShell.vue'
import JobCard from '@/components/JobCard.vue'
import { getCompanyDetail, getCompanyJobs, increaseCompanyViewCount, toggleCompanyFollow, checkCompanyFollowed } from '@/api/company'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const userStore = useUserStore()
const { isLoggedIn } = userStore

const company = ref({
  id: 1,
  name: '字节跳动',
  logo: '',
  industry: '互联网',
  scale: '10000人以上',
  type: '民营企业',
  city: '北京',
  address: '北京市海淀区中关村软件园',
  website: 'https://www.bytedance.com',
  description: '<p>字节跳动是一家全球性的科技公司，旗下产品包括抖音、TikTok、今日头条等。</p><p>公司致力于用技术连接人与信息，让全球每个人都能创作和分享内容。</p>',
  benefits: ['五险一金', '带薪年假', '弹性工作', '免费三餐', '健身房', '团建活动']
})

const jobList = ref([])

const isFollowed = ref(false)

// 职位分页
const jobPage = ref(1)
const jobTotal = ref(0)
const jobPageSize = 8

// 职位筛选条件
const filterKeyword = ref('')
const filterCity = ref('')
const filterJobType = ref('')
const filterExperience = ref('')
const filterEducation = ref('')
const cities = ['北京', '上海', '广州', '深圳', '杭州', '成都', '武汉', '南京', '重庆', '西安']
const experiences = ['不限', '应届生', '1-3年', '3-5年', '5-10年', '10年以上']
const educations = ['不限', '大专', '本科', '硕士', '博士']

const companyBenefits = computed(() => {
  if (!company.value.benefits) return []
  if (Array.isArray(company.value.benefits)) return company.value.benefits
  try {
    return JSON.parse(company.value.benefits)
  } catch {
    return []
  }
})

// 是否有激活的筛选条件
const hasActiveFilters = computed(() => {
  return filterKeyword.value || filterCity.value || filterJobType.value ||
    (filterExperience.value && filterExperience.value !== '不限') ||
    (filterEducation.value && filterEducation.value !== '不限')
})

/** 关注/取消关注 */
const handleFollow = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await toggleCompanyFollow(company.value.id)
    if (res?.data !== undefined) {
      isFollowed.value = res.data
      ElMessage.success(isFollowed.value ? '已关注' : '已取消关注')
    }
  } catch (e) {
    ElMessage.error('操作失败')
  }
}

// 加载职位分页数据（带筛选）
const loadJobs = async (companyId) => {
  try {
    const params = { page: jobPage.value, size: jobPageSize }
    if (filterKeyword.value) params.keyword = filterKeyword.value
    if (filterCity.value) params.city = filterCity.value
    if (filterJobType.value) params.jobType = filterJobType.value
    if (filterExperience.value && filterExperience.value !== '不限') params.experience = filterExperience.value
    if (filterEducation.value && filterEducation.value !== '不限') params.education = filterEducation.value
    const res = await getCompanyJobs(companyId, params)
    if (res?.data) {
      jobList.value = res.data.records || []
      jobTotal.value = res.data.total || 0
    }
  } catch (e) {
    jobList.value = []
  }
}

// 切换职位页码
const handleJobPageChange = (page) => {
  jobPage.value = page
  loadJobs(route.params.id)
}

// 筛选条件变化时重新加载（回到第1页）
const handleFilterChange = () => {
  jobPage.value = 1
  loadJobs(route.params.id)
}

// 重置筛选条件
const resetJobFilters = () => {
  filterKeyword.value = ''
  filterCity.value = ''
  filterJobType.value = ''
  filterExperience.value = ''
  filterEducation.value = ''
  handleFilterChange()
}

const loadData = async (companyId) => {
  try {
    const companyRes = await getCompanyDetail(companyId)
    if (companyRes?.data) {
      company.value = companyRes.data
    }

    // 加载职位第一页
    await loadJobs(companyId)

    // 增加浏览量
    try {
      await increaseCompanyViewCount(companyId)
    } catch (e) {
      // 浏览量统计失败不影响页面展示
    }

    // 检查是否已关注（需登录）
    if (isLoggedIn.value) {
      try {
        const followRes = await checkCompanyFollowed(companyId)
        if (followRes?.data !== undefined) {
          isFollowed.value = followRes.data
        }
      } catch (e) {
        // 忽略
      }
    }
  } catch (e) {
    // 使用默认数据
  }
}

onMounted(() => {
  loadData(route.params.id)
})

watch(() => route.params.id, (newId) => {
  if (newId) {
    loadData(newId)
  }
})
</script>

<style scoped>
.company-detail {
  min-height: calc(100vh - 64px);
  background: #f5f6f8;
}

.company-detail__container {
  max-width: 900px;
  margin: 0 auto;
  padding: 24px;
}

/* 主卡片 */
.main-card {
  background: #fff;
  border-radius: 16px;
  padding: 32px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
}

/* 公司头部 */
.company-header {
  display: flex;
  align-items: center;
  gap: 24px;
}

.company-logo {
  width: 72px;
  height: 72px;
  border-radius: 16px;
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
  font-size: 30px;
  font-weight: 700;
  color: #fff;
}

.company-info {
  flex: 1;
}

.company-name {
  font-size: 24px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 8px;
}

.company-meta {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 12px;
}

.company-tags {
  display: flex;
  gap: 8px;
}

.company-tags a {
  color: inherit;
  text-decoration: none;
}

.company-actions {
  flex-shrink: 0;
}

.company-actions .el-button {
  min-width: 100px;
}

/* 分割线 */
.divider {
  height: 1px;
  background: linear-gradient(to right, transparent, #e5e7eb, transparent);
  margin: 24px 0;
}

/* 内容区块 */
.section {
  margin-bottom: 0;
}

.section + .section {
  margin-top: 24px;
}

.divider + .section {
  margin-top: 0;
}

.section__title {
  font-size: 17px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 14px;
  padding-left: 12px;
  border-left: 3px solid #10b981;
}

.section__content {
  font-size: 14px;
  line-height: 1.8;
  color: #334155;
}

.company-description :deep(p) {
  margin: 0 0 8px;
}

.company-benefits {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.job-filters {
  margin-bottom: 16px;
}

.job-filters__row {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.job-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.job-pagination {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

.empty-state {
  padding: 32px 0;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .main-card {
    padding: 24px 20px;
  }

  .company-header {
    flex-direction: column;
    text-align: center;
  }

  .company-tags {
    justify-content: center;
  }
}
</style>
