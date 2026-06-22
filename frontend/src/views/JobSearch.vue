<template>
  <AppShell :bgImage="false">
    <div class="job-search">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <div class="search-bar__inner">
          <el-input
            v-model="keyword"
            placeholder="搜索职位、公司或关键词"
            size="large"
            class="search-input"
            @keyup.enter="handleSearch"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
        </div>
      </div>

      <!-- 筛选条件 -->
      <div class="filters">
        <div class="filters-inner">
          <div class="filter-group">
            <span class="filter-label">城市：</span>
            <div class="filter-options">
              <span
                v-for="city in cities"
                :key="city"
                class="filter-option"
                :class="{ active: selectedCity === city }"
                @click="selectedCity = selectedCity === city ? '' : city; handleSearch()"
              >
                {{ city }}
              </span>
            </div>
          </div>
          <div class="filter-group">
            <span class="filter-label">经验：</span>
            <div class="filter-options">
              <span
                v-for="exp in experiences"
                :key="exp"
                class="filter-option"
                :class="{ active: selectedExperience === exp }"
                @click="selectedExperience = selectedExperience === exp ? '' : exp; handleSearch()"
              >
                {{ exp }}
              </span>
            </div>
          </div>
          <div class="filter-group">
            <span class="filter-label">学历：</span>
            <div class="filter-options">
              <span
                v-for="edu in educations"
                :key="edu"
                class="filter-option"
                :class="{ active: selectedEducation === edu }"
                @click="selectedEducation = selectedEducation === edu ? '' : edu; handleSearch()"
              >
                {{ edu }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 分类栏 + 搜索结果（左） + 公司推荐（右） -->
      <div class="search-body">
        <!-- 左侧分类栏 -->
        <div class="category-sidebar">
          <div class="sidebar-title">职位分类</div>
          <div
            v-for="cat in categoryTree"
            :key="cat.id"
            class="sidebar-group"
          >
            <div
              class="sidebar-parent"
              :class="{ active: selectedParentId === cat.id }"
              @click="selectParent(cat)"
            >
              {{ cat.name }}
            </div>
            <div v-if="selectedParentId === cat.id && cat.children" class="sidebar-children">
              <div
                v-for="child in cat.children"
                :key="child.id"
                class="sidebar-child"
                :class="{ active: selectedCategoryId === child.id }"
                @click="selectCategory(child)"
              >
                {{ child.name }}
              </div>
            </div>
          </div>
        </div>

        <!-- 中间职位列表（70%） -->
        <div class="results">
          <!-- HR筛选提示条 -->
          <div v-if="hrUserId" class="hr-filter-banner">
            <span>正在查看 <strong>{{ hrName || 'HR' }}</strong> 负责的职位</span>
            <el-button type="primary" link @click="hrUserId = null; hrName = ''; handleSearch()">查看全部职位</el-button>
          </div>

          <div class="results-header">
            <span class="results-count">共找到 {{ totalCountDisplay }} 个职位</span>
            <el-select v-model="sortBy" size="small" style="width: 120px" @change="handleSearch">
              <el-option label="最新发布" value="publishedAt" />
              <el-option label="薪资最高" value="salary" />
              <el-option label="最多浏览" value="viewCount" />
              <el-option label="最多申请" value="applyCount" />
            </el-select>
          </div>

          <div v-if="jobList.length === 0" class="empty-state">
            <el-empty description="未找到相关职位">
              <el-button type="primary" @click="resetFilters">重置筛选</el-button>
            </el-empty>
          </div>

          <div v-else class="job-list">
            <JobCard v-for="job in jobList" :key="job.id" :job="job" />
          </div>

          <!-- 分页 -->
          <div v-if="totalCount > 0" class="pagination">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="totalCount"
              layout="prev, pager, next"
              @current-change="handleSearch"
            />
          </div>
        </div>

        <!-- 右侧公司推荐（30%） -->
        <div class="company-sidebar">
          <div class="sidebar-title">热门公司</div>
          <div v-if="companyList.length === 0" class="company-empty">
            暂无推荐公司
          </div>
          <div v-else class="company-list">
            <div
              v-for="company in companyList"
              :key="company.id"
              class="company-card"
              @click="$router.push(`/company/${company.id}`)"
            >
              <div class="company-card__name">{{ company.name }}</div>
              <div class="company-card__info">
                <span v-if="company.industry">{{ company.industry }}</span>
                <span v-if="company.city">· {{ company.city }}</span>
              </div>
              <div class="company-card__jobs">{{ company.jobCount || 0 }}个在招职位</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </AppShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import AppShell from '@/components/AppShell.vue'
import JobCard from '@/components/JobCard.vue'
import { searchJobsByES, searchCompaniesByES } from '@/api/search'
import { getCategoryTree } from '@/api/category'

const route = useRoute()

const keyword = ref(route.query.keyword || '')
const selectedCity = ref('')
const selectedExperience = ref('')
const selectedEducation = ref('')
const sortBy = ref('publishedAt')
const currentPage = ref(1)
const pageSize = ref(20)
const totalCount = ref(0)
const totalHitsRelation = ref('eq')
const totalCountDisplay = computed(() => totalHitsRelation.value === 'gte' ? totalCount.value + '+' : totalCount.value)
const hrUserId = ref(route.query.hrUserId ? Number(route.query.hrUserId) : null)
const hrName = ref(route.query.hrName || '')

// 分类相关
const categoryTree = ref([])
const selectedParentId = ref(null)
const selectedCategoryId = ref(null)

const cities = ['北京', '上海', '广州', '深圳', '杭州', '成都', '武汉', '南京']
const experiences = ['不限', '应届生', '1-3年', '3-5年', '5-10年', '10年以上']
const educations = ['不限', '大专', '本科', '硕士', '博士']

const jobList = ref([])
const companyList = ref([])

// 加载分类树
const loadCategories = async () => {
  try {
    const res = await getCategoryTree()
    categoryTree.value = res.data || []

    const catId = Number(route.query.categoryId)
    if (catId) {
      const parentCat = categoryTree.value.find(c => c.id === catId)
      if (parentCat) {
        selectedParentId.value = catId
        selectedCategoryId.value = null
      } else {
        selectedCategoryId.value = catId
        for (const parent of categoryTree.value) {
          if (parent.children) {
            for (const child of parent.children) {
              if (child.id === catId) {
                selectedParentId.value = parent.id
                break
              }
            }
          }
        }
      }
    }
  } catch (e) {
    console.error('加载分类失败', e)
  }
}

// 选择父分类
const selectParent = (cat) => {
  if (selectedParentId.value === cat.id) {
    selectedParentId.value = null
    selectedCategoryId.value = null
  } else {
    selectedParentId.value = cat.id
    selectedCategoryId.value = null
  }
  currentPage.value = 1
  handleSearch()
}

// 选择子分类
const selectCategory = (child) => {
  selectedCategoryId.value = child.id
  currentPage.value = 1
  handleSearch()
}

// 搜索职位（ES）
const handleSearch = async () => {
  try {
    const params = {
      keyword: keyword.value || undefined,
      city: selectedCity.value || undefined,
      experience: selectedExperience.value === '不限' ? undefined : selectedExperience.value || undefined,
      education: selectedEducation.value === '不限' ? undefined : selectedEducation.value || undefined,
      hrUserId: hrUserId.value || undefined,
      sort: sortBy.value,
      page: currentPage.value,
      size: pageSize.value
    }
    if (selectedCategoryId.value) {
      params.categoryId = selectedCategoryId.value
    } else if (selectedParentId.value) {
      params.categoryId = selectedParentId.value
    }

    const res = await searchJobsByES(params)
    if (res?.data) {
      jobList.value = res.data.records || []
      totalCount.value = res.data.total || 0
      totalHitsRelation.value = res.data.totalHitsRelation || 'eq'
    }
  } catch (e) {
    console.error('搜索失败', e)
  }
}

// 搜索公司（ES，右侧推荐）
const handleCompanySearch = async () => {
  try {
    const res = await searchCompaniesByES({ page: 1, size: 10, sort: 'jobCount' })
    if (res?.data) {
      companyList.value = res.data.records || []
    }
  } catch (e) {
    console.error('公司搜索失败', e)
  }
}

const resetFilters = () => {
  keyword.value = ''
  selectedCity.value = ''
  selectedExperience.value = ''
  selectedEducation.value = ''
  selectedCategoryId.value = null
  selectedParentId.value = null
  hrUserId.value = null
  currentPage.value = 1
  handleSearch()
}

onMounted(async () => {
  await loadCategories()

  if (route.query.keyword) {
    keyword.value = route.query.keyword
  }
  if (route.query.categoryId) {
    const catId = Number(route.query.categoryId)
    const parentCat = categoryTree.value.find(c => c.id === catId)
    if (parentCat) {
      selectedParentId.value = catId
    } else {
      selectedCategoryId.value = catId
    }
  }

  handleSearch()
  handleCompanySearch()
})
</script>

<style scoped>
.job-search {
  min-height: calc(100vh - 64px);
  background: #f5f6f8;
}

/* 搜索栏 */
.search-bar {
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  padding: 20px 24px;
}

.search-bar__inner {
  width: 60%;
  margin: 0 auto;
  display: flex;
  gap: 12px;
}

.search-input {
  flex: 1;
}

/* 筛选条件 */
.filters {
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.filters-inner {
  width: 92%;
  margin: 0 auto;
  padding: 16px 0;
}

.filter-group {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 10px;
}

.filter-group:last-child {
  margin-bottom: 0;
}

.filter-label {
  font-size: 14px;
  font-weight: 500;
  color: #334155;
  min-width: 50px;
  padding-top: 6px;
}

.filter-options {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-option {
  padding: 6px 16px;
  font-size: 14px;
  color: #64748b;
  background: #f1f5f9;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.2s;
}

.filter-option:hover {
  color: #10b981;
  background: #ecfdf5;
}

.filter-option.active {
  color: #fff;
  background: #10b981;
}

/* 三栏布局 */
.search-body {
  width: 92%;
  max-width: 1400px;
  margin: 0 auto;
  padding: 24px 0;
  display: flex;
  gap: 20px;
}

/* 左侧分类栏 */
.category-sidebar {
  width: 220px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  padding: 12px 0;
  height: fit-content;
  position: sticky;
  top: 88px;
}

.sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 8px;
  padding: 4px 20px 12px;
  border-bottom: 1px solid #e5e7eb;
}

.sidebar-group {
  margin-bottom: 2px;
}

.sidebar-parent {
  padding: 10px 20px;
  font-size: 14px;
  color: #475569;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
  box-sizing: border-box;
}

.sidebar-parent:hover {
  background: #f1f5f9;
  color: #10b981;
}

.sidebar-parent.active {
  background: #ecfdf5;
  color: #10b981;
  font-weight: 500;
  border-right: 3px solid #10b981;
}

.sidebar-children {
  padding: 2px 0 4px;
  background: #f8fafc;
}

.sidebar-child {
  padding: 8px 20px 8px 36px;
  font-size: 13px;
  color: #64748b;
  cursor: pointer;
  transition: all 0.2s;
  width: 100%;
  box-sizing: border-box;
}

.sidebar-child:hover {
  color: #10b981;
  background: #ecfdf5;
}

.sidebar-child.active {
  color: #10b981;
  font-weight: 500;
  background: #ecfdf5;
}

/* 中间职位列表 */
.results {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 12px;
  padding: 20px;
}

/* HR筛选提示条 */
.hr-filter-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  margin-bottom: 16px;
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  border-radius: 8px;
  font-size: 14px;
  color: #1e40af;
}

.results-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.results-count {
  font-size: 14px;
  color: #64748b;
}

.job-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.empty-state {
  padding: 48px 0;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* 右侧公司推荐 */
.company-sidebar {
  width: 280px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 12px;
  padding: 12px 0;
  height: fit-content;
  position: sticky;
  top: 88px;
}

.company-empty {
  padding: 20px;
  text-align: center;
  color: #94a3b8;
  font-size: 14px;
}

.company-list {
  display: flex;
  flex-direction: column;
}

.company-card {
  padding: 14px 20px;
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 1px solid #f1f5f9;
}

.company-card:last-child {
  border-bottom: none;
}

.company-card:hover {
  background: #f8fafc;
}

.company-card__name {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  margin-bottom: 4px;
}

.company-card__info {
  font-size: 12px;
  color: #64748b;
  margin-bottom: 4px;
}

.company-card__jobs {
  font-size: 12px;
  color: #10b981;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .search-body {
    flex-direction: column;
  }

  .category-sidebar,
  .company-sidebar {
    width: 100%;
    position: static;
  }

  .search-bar__inner {
    flex-direction: column;
  }

  .filter-group {
    flex-direction: column;
    gap: 8px;
  }

  .filter-label {
    min-width: auto;
    padding-top: 0;
  }
}
</style>
