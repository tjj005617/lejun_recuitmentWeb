<template>
  <AppShell :bgImage="false">
    <div class="company-search">
      <!-- 搜索栏 -->
      <div class="search-bar">
        <div class="search-bar__inner">
          <el-input
            v-model="keyword"
            placeholder="搜索公司名称、行业或关键词"
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
            <span class="filter-label">行业：</span>
            <div class="filter-options">
              <span
                v-for="ind in industries"
                :key="ind"
                class="filter-option"
                :class="{ active: selectedIndustry === ind }"
                @click="selectedIndustry = selectedIndustry === ind ? '' : ind; handleSearch()"
              >
                {{ ind }}
              </span>
            </div>
          </div>
          <div class="filter-group">
            <span class="filter-label">规模：</span>
            <div class="filter-options">
              <span
                v-for="s in scales"
                :key="s"
                class="filter-option"
                :class="{ active: selectedScale === s }"
                @click="selectedScale = selectedScale === s ? '' : s; handleSearch()"
              >
                {{ s }}
              </span>
            </div>
          </div>
        </div>
      </div>

      <!-- 搜索结果（单栏） -->
      <div class="search-body">
        <div class="results">
          <div class="results-header">
            <span class="results-count">共找到 {{ totalCountDisplay }} 家公司</span>
            <el-select v-model="sortBy" size="small" style="width: 120px" @change="handleSearch">
              <el-option label="职位最多" value="jobCount" />
              <el-option label="浏览最多" value="viewCount" />
              <el-option label="关注最多" value="followCount" />
            </el-select>
          </div>

          <div v-if="companyList.length === 0" class="empty-state">
            <el-empty description="未找到相关公司">
              <el-button type="primary" @click="resetFilters">重置筛选</el-button>
            </el-empty>
          </div>

          <div v-else class="company-list">
            <div
              v-for="company in companyList"
              :key="company.id"
              class="company-card"
              @click="$router.push(`/company/${company.id}`)"
            >
              <div class="company-card__left">
                <div class="company-card__name">{{ company.name }}</div>
                <div class="company-card__tags">
                  <span v-if="company.industry" class="company-card__tag">{{ company.industry }}</span>
                  <span v-if="company.scale" class="company-card__tag">{{ company.scale }}</span>
                  <span v-if="company.city" class="company-card__tag">{{ company.city }}</span>
                </div>
                <div v-if="company.description" class="company-card__desc">
                  {{ company.description.length > 80 ? company.description.substring(0, 80) + '...' : company.description }}
                </div>
              </div>
              <div class="company-card__right">
                <div class="company-card__stat">
                  <span class="company-card__stat-num">{{ company.jobCount || 0 }}</span>
                  <span class="company-card__stat-label">在招职位</span>
                </div>
                <div class="company-card__stat">
                  <span class="company-card__stat-num">{{ company.viewCount || 0 }}</span>
                  <span class="company-card__stat-label">浏览</span>
                </div>
              </div>
            </div>
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
      </div>
    </div>
  </AppShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { Search } from '@element-plus/icons-vue'
import AppShell from '@/components/AppShell.vue'
import { searchCompaniesByES } from '@/api/search'

const route = useRoute()

const keyword = ref(route.query.keyword || '')
const selectedCity = ref('')
const selectedIndustry = ref('')
const selectedScale = ref('')
const sortBy = ref('jobCount')
const currentPage = ref(1)
const pageSize = ref(20)
const totalCount = ref(0)
const totalHitsRelation = ref('eq')
const totalCountDisplay = computed(() => totalHitsRelation.value === 'gte' ? totalCount.value + '+' : totalCount.value)

const cities = ['北京', '上海', '广州', '深圳', '杭州', '成都', '武汉', '南京']
const industries = ['互联网', '金融', '教育', '医疗', '电商', '游戏', '人工智能']
const scales = ['0-20人', '20-99人', '100-499人', '500-999人', '1000-9999人', '10000人以上']

const companyList = ref([])

// 搜索公司（ES）
const handleSearch = async () => {
  try {
    const params = {
      keyword: keyword.value || undefined,
      city: selectedCity.value || undefined,
      industry: selectedIndustry.value || undefined,
      scale: selectedScale.value || undefined,
      sort: sortBy.value,
      page: currentPage.value,
      size: pageSize.value
    }

    const res = await searchCompaniesByES(params)
    if (res?.data) {
      companyList.value = res.data.records || []
      totalCount.value = res.data.total || 0
      totalHitsRelation.value = res.data.totalHitsRelation || 'eq'
    }
  } catch (e) {
    console.error('公司搜索失败', e)
  }
}

const resetFilters = () => {
  keyword.value = ''
  selectedCity.value = ''
  selectedIndustry.value = ''
  selectedScale.value = ''
  currentPage.value = 1
  handleSearch()
}

onMounted(() => {
  if (route.query.keyword) {
    keyword.value = route.query.keyword
  }
  handleSearch()
})
</script>

<style scoped>
.company-search {
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

/* 单栏搜索结果 */
.search-body {
  width: 92%;
  max-width: 1000px;
  margin: 0 auto;
  padding: 24px 0;
}

.results {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
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

.company-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.company-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.company-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(0, 0, 0, 0.1);
  border-color: #10b981;
}

.company-card__left {
  flex: 1;
  min-width: 0;
}

.company-card__name {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 8px;
}

.company-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.company-card__tag {
  background: #f1f5f9;
  color: #475569;
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 4px;
}

.company-card__desc {
  font-size: 13px;
  color: #64748b;
  line-height: 1.5;
}

.company-card__right {
  display: flex;
  gap: 24px;
  flex-shrink: 0;
  margin-left: 24px;
}

.company-card__stat {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.company-card__stat-num {
  font-size: 18px;
  font-weight: 700;
  color: #10b981;
}

.company-card__stat-label {
  font-size: 12px;
  color: #94a3b8;
}

.empty-state {
  padding: 48px 0;
}

.pagination {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

/* 移动端适配 */
@media (max-width: 768px) {
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

  .company-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .company-card__right {
    margin-left: 0;
    margin-top: 12px;
  }
}
</style>
