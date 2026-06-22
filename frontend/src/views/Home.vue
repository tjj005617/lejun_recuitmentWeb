<template>
  <AppShell :bgImage="false">
    <!-- Hero区域 -->
    <div class="hero">
      <div class="hero__content">
        <h1 class="hero__title">找理想工作，上俊乐招聘</h1>
        <p class="hero__subtitle">海量职位，精准匹配，让求职更简单</p>
        <div class="hero__search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索职位、公司或关键词"
            size="large"
            class="hero__search-input"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" size="large" class="hero__search-btn" @click="handleSearch">
            搜索职位
          </el-button>
        </div>
        <div class="hero__tags">
          <span class="hero__tag" v-for="tag in hotTags" :key="tag" @click="searchKeyword = tag; handleSearch()">
            {{ tag }}
          </span>
        </div>
      </div>
    </div>

    <!-- 职位分类 -->
    <div class="section categories-section">
      <div class="section__inner">
        <h2 class="section__title">职位分类</h2>
        <div class="categories-grid">
          <div
            class="category-item"
            v-for="cat in categories"
            :key="cat.id"
            @click="searchByCategory(cat)"
          >
            <img :src="cat.icon" class="category-item__icon" :alt="cat.name" />
            <span class="category-item__name">{{ cat.name }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 热门职位 -->
    <div class="section">
      <div class="section__inner">
        <div class="section__header">
          <h2 class="section__title">热门职位</h2>
          <router-link to="/jobs" class="section__more">查看更多 →</router-link>
        </div>
        <div class="jobs-grid">
          <JobCard v-for="job in hotJobs" :key="job.id" :job="job" />
        </div>
      </div>
    </div>

    <!-- 热门公司 -->
    <div class="section">
      <div class="section__inner">
        <div class="section__header">
          <h2 class="section__title">热门公司</h2>
          <router-link to="/companies" class="section__more">查看更多 →</router-link>
        </div>
        <div class="companies-grid">
          <CompanyCard v-for="company in hotCompanies" :key="company.id" :company="company" />
        </div>
      </div>
    </div>

    <!-- AI面试入口 -->
    <div class="section ai-section">
      <div class="section__inner">
        <div class="ai-banner">
          <div class="ai-banner__content">
            <h3 class="ai-banner__title">AI模拟面试</h3>
            <p class="ai-banner__desc">上传简历，AI智能出题，助你面试无忧</p>
            <el-button type="primary" @click="$router.push('/upload')">
              立即体验
            </el-button>
          </div>
          <div class="ai-banner__icon">🤖</div>
        </div>
      </div>
    </div>
  </AppShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import AppShell from '@/components/AppShell.vue'
import JobCard from '@/components/JobCard.vue'
import CompanyCard from '@/components/CompanyCard.vue'
import { getHotJobs, getRecommendJobs } from '@/api/job'
import { getHotCompanies } from '@/api/company'
import { getCategoryTree } from '@/api/category'

const router = useRouter()
const searchKeyword = ref('')

const hotTags = ['Java', '前端', '产品经理', 'UI设计', 'Python', '数据分析']

import techIcon from '@/assets/image/技术类.svg'
import productIcon from '@/assets/image/产品类.svg'
import designIcon from '@/assets/image/设计类.svg'
import operateIcon from '@/assets/image/运营类.svg'
import functionIcon from '@/assets/image/职能类.svg'
import moreIcon from '@/assets/image/更多.svg'

// 分类名称对应的图标映射
const iconMap = {
  '技术类': techIcon,
  '产品类': productIcon,
  '设计类': designIcon,
  '运营类': operateIcon,
  '职能类': functionIcon
}

const categories = ref([])

const hotJobs = ref([
  { id: 1, title: 'Java开发工程师', companyName: '字节跳动', salaryMin: 15000, salaryMax: 30000, city: '北京', experience: '3-5年', education: '本科', jobType: 'full_time', publishedAt: new Date(Date.now() - 86400000).toISOString(), viewCount: 256 },
  { id: 2, title: '前端开发工程师', companyName: '阿里巴巴', salaryMin: 18000, salaryMax: 35000, city: '杭州', experience: '3-5年', education: '本科', jobType: 'full_time', publishedAt: new Date(Date.now() - 172800000).toISOString(), viewCount: 189 },
  { id: 3, title: '产品经理', companyName: '腾讯', salaryMin: 20000, salaryMax: 40000, city: '深圳', experience: '5年以上', education: '本科', jobType: 'full_time', publishedAt: new Date(Date.now() - 259200000).toISOString(), viewCount: 324 },
  { id: 4, title: 'UI设计师', companyName: '网易', salaryMin: 12000, salaryMax: 25000, city: '杭州', experience: '1-3年', education: '本科', jobType: 'full_time', publishedAt: new Date(Date.now() - 345600000).toISOString(), viewCount: 156 },
  { id: 5, title: 'Python开发工程师', companyName: '美团', salaryMin: 16000, salaryMax: 32000, city: '北京', experience: '3-5年', education: '本科', jobType: 'full_time', publishedAt: new Date(Date.now() - 432000000).toISOString(), viewCount: 198 },
  { id: 6, title: '数据分析师', companyName: '滴滴', salaryMin: 15000, salaryMax: 28000, city: '北京', experience: '1-3年', education: '本科', jobType: 'full_time', publishedAt: new Date(Date.now() - 518400000).toISOString(), viewCount: 145 }
])

const hotCompanies = ref([
  { id: 1, name: '字节跳动', industry: '互联网', scale: '10000人以上', city: '北京', jobCount: 128 },
  { id: 2, name: '阿里巴巴', industry: '电子商务', scale: '10000人以上', city: '杭州', jobCount: 96 },
  { id: 3, name: '腾讯', industry: '社交/游戏', scale: '10000人以上', city: '深圳', jobCount: 112 },
  { id: 4, name: '网易', industry: '互联网', scale: '10000人以上', city: '杭州', jobCount: 78 },
  { id: 5, name: '美团', industry: '生活服务', scale: '10000人以上', city: '北京', jobCount: 85 },
  { id: 6, name: '滴滴', industry: '出行服务', scale: '10000人以上', city: '北京', jobCount: 64 }
])

const handleSearch = () => {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/jobs', query: { keyword: searchKeyword.value.trim() } })
  } else {
    router.push('/jobs')
  }
}

const searchByCategory = (cat) => {
  router.push({ path: '/jobs', query: { categoryId: cat.id } })
}

onMounted(async () => {
  try {
    const [jobsRes, companiesRes, categoriesRes] = await Promise.all([
      getHotJobs().catch(() => null),
      getHotCompanies().catch(() => null),
      getCategoryTree().catch(() => null)
    ])
    if (jobsRes?.data) hotJobs.value = jobsRes.data
    if (companiesRes?.data) hotCompanies.value = companiesRes.data
    if (categoriesRes?.data) {
      categories.value = categoriesRes.data.map(c => ({
        ...c,
        icon: iconMap[c.name] || moreIcon
      }))
    }
  } catch (e) {
    // 使用默认数据
  }
})
</script>

<style scoped>
/* Hero区域 */
.hero {
  background: url('@/assets/image/background2.webp') no-repeat center 70%;
  background-size: cover;
  padding: 120px 24px 80px;
  position: relative;
}

.hero::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.35);
  z-index: 1;
}

.hero__content {
  position: relative;
  z-index: 2;
  max-width: 700px;
  margin: 0 auto;
  text-align: center;
}

.hero__title {
  font-size: 40px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 12px;
}

.hero__subtitle {
  font-size: 18px;
  color: rgba(255, 255, 255, 0.9);
  margin: 0 0 32px;
}

.hero__search {
  display: flex;
  gap: 12px;
  max-width: 560px;
  margin: 0 auto 16px;
}

.hero__search-input {
  flex: 1;
}

.hero__search-input :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: none;
  border: 2px solid transparent;
}

.hero__search-input :deep(.el-input__wrapper.is-focus) {
  border-color: #10b981;
}

.hero__search-btn {
  border-radius: 8px;
  padding: 0 24px;
  background: #fff;
  color: #10b981;
  border: none;
  font-weight: 600;
}

.hero__search-btn:hover {
  background: #f0fdf4;
}

.hero__tags {
  display: flex;
  justify-content: center;
  gap: 10px;
  flex-wrap: wrap;
}

.hero__tag {
  background: rgba(255, 255, 255, 0.2);
  color: #fff;
  padding: 6px 16px;
  border-radius: 20px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.hero__tag:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 通用区块 */
.section {
  padding: 48px 40px;
  background: #fff;
}

.section + .section {
  border-top: 1px solid #f1f5f9;
}

.section__inner {
  width: 100%;
}

.section__header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.section__title {
  font-size: 24px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
}

.section__more {
  font-size: 14px;
  color: #10b981;
  text-decoration: none;
  font-weight: 500;
}

.section__more:hover {
  text-decoration: underline;
}

/* 职位分类 */
.categories-section {
  background: #f8fafc;
}

.categories-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}

.category-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  padding: 24px 16px;
  background: #fff;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
  border: 1px solid #e5e7eb;
}

.category-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
  border-color: #10b981;
}

.category-item__icon {
  width: 40px;
  height: 40px;
}

.category-item__name {
  font-size: 15px;
  font-weight: 500;
  color: #334155;
}

/* 职位网格 */
.jobs-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

/* 公司网格 */
.companies-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
}

/* AI面试Banner */
.ai-section {
  background: #f8fafc;
}

.ai-banner {
  background: url('@/assets/image/background3.webp') no-repeat center center;
  background-size: cover;
  border-radius: 16px;
  padding: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  color: #fff;
  position: relative;
}

.ai-banner::before {
  content: '';
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.3);
  border-radius: 16px;
}

.ai-banner__content {
  position: relative;
  z-index: 1;
}

.ai-banner__icon {
  position: relative;
  z-index: 1;
}

.ai-banner__title {
  font-size: 28px;
  font-weight: 700;
  margin: 0 0 8px;
}

.ai-banner__desc {
  font-size: 16px;
  margin: 0 0 24px;
  opacity: 0.9;
}

.ai-banner__icon {
  font-size: 80px;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .hero {
    padding: 40px 16px 24px;
  }

  .hero__title {
    font-size: 22px;
  }

  .hero__subtitle {
    font-size: 14px;
    margin-bottom: 20px;
  }

  .hero__search {
    flex-direction: column;
    max-width: 100%;
    gap: 8px;
  }

  .hero__search-btn {
    padding: 0 16px;
    height: 40px;
  }

  .hero__tags {
    gap: 8px;
    margin-top: 12px;
  }

  .hero__tag {
    padding: 4px 12px;
    font-size: 12px;
  }

  .section {
    padding: 24px 16px;
  }

  .section__title {
    font-size: 18px;
  }

  .categories-grid {
    grid-template-columns: repeat(4, 1fr);
    gap: 8px;
  }

  .category-item {
    padding: 12px 4px;
    border-radius: 8px;
    gap: 6px;
  }

  .category-item__icon {
    width: 28px;
    height: 28px;
  }

  .category-item__name {
    font-size: 10px;
  }

  .jobs-grid,
  .companies-grid {
    grid-template-columns: 1fr;
    gap: 12px;
  }

  .ai-banner {
    flex-direction: column;
    text-align: center;
    padding: 24px 16px;
    border-radius: 12px;
  }

  .ai-banner__title {
    font-size: 20px;
  }

  .ai-banner__desc {
    font-size: 14px;
    margin-bottom: 16px;
  }

  .ai-banner__icon {
    font-size: 48px;
    margin-top: 16px;
  }
}
</style>
