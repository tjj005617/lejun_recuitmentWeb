<template>
  <AppShell :bgImage="false">
    <div class="match-page">
      <div class="match-page__container">
        <!-- 左侧面板 -->
        <aside class="sidebar">
          <!-- 简历选择卡片 -->
          <div class="sidebar-card">
            <h3 class="sidebar-card__title">选择简历</h3>
            <div v-if="resumeList.length === 0" class="empty-tip">
              <el-empty description="暂无简历" :image-size="80">
                <el-button type="primary" size="small" @click="$router.push('/upload')">上传简历</el-button>
              </el-empty>
            </div>
            <div v-else class="resume-select-list">
              <div
                v-for="resume in resumeList"
                :key="resume.id"
                class="resume-select-item"
                :class="{ active: selectedResumeId === resume.id }"
                @click="selectResume(resume)"
              >
                <div class="resume-select-item__icon"> </div>
                <div class="resume-select-item__info">
                  <div class="resume-select-item__name">{{ resume.fileName }}</div>
                  <div class="resume-select-item__meta">
                    <span v-if="resume.name">{{ resume.name }}</span>
                    <span v-if="resume.education">· {{ resume.education }}</span>
                  </div>
                </div>
                <el-icon v-if="selectedResumeId === resume.id" class="resume-select-item__check"><check /></el-icon>
              </div>
            </div>
          </div>

          <!-- 筛选条件 -->
          <div class="sidebar-card" v-if="selectedResumeId">
            <h3 class="sidebar-card__title">筛选条件</h3>
            <el-form label-position="top" size="default">
              <el-form-item label="城市">
                <el-input v-model="filters.city" placeholder="如：北京、上海" clearable />
              </el-form-item>
              <el-form-item label="岗位分类">
                <el-select v-model="filters.categoryId" placeholder="全部分类" clearable style="width:100%">
                  <el-option
                    v-for="cat in categoryList"
                    :key="cat.id"
                    :label="cat.name"
                    :value="cat.id"
                  />
                </el-select>
              </el-form-item>
              <el-form-item label="返回数量">
                <el-input-number v-model="filters.topN" :min="5" :max="30" style="width:100%" />
              </el-form-item>
              <el-button type="primary" @click="handleMatch" :loading="loading" style="width:100%">
                开始匹配
              </el-button>
            </el-form>
          </div>
        </aside>

        <!-- 右侧结果区 -->
        <main class="content">
          <div class="content-panel">
            <div class="content-header">
              <h2>匹配结果</h2>
              <span class="match-count" v-if="matchResults.length > 0">
                共找到 <strong>{{ matchResults.length }}</strong> 个匹配岗位
              </span>
            </div>

            <!-- 空状态 -->
            <div v-if="!hasSearched && !loading" class="empty-state">
              <div class="empty-state__icon"> </div>
              <h3>智能岗位匹配</h3>
              <p>选择左侧简历，AI 将根据你的经历智能匹配最合适的岗位</p>
            </div>

            <!-- 加载中 -->
            <div v-if="loading" class="loading-state">
              <div class="loading-spinner"></div>
              <p>AI 正在分析简历并匹配岗位...</p>
            </div>

            <!-- 无结果 -->
            <div v-if="hasSearched && !loading && matchResults.length === 0" class="empty-state">
              <el-empty description="未找到匹配岗位，试试调整筛选条件" />
            </div>

            <!-- 匹配结果列表 -->
            <div v-if="!loading && matchResults.length > 0" class="match-list">
              <div
                v-for="(item, index) in matchResults"
                :key="item.jobId"
                class="match-card"
                @click="$router.push(`/job/${item.jobId}`)"
              >
                <!-- 匹配度标签 -->
                <div class="match-card__score" :class="getScoreClass(item.matchScore)">
                  <span class="match-card__score-num">{{ item.matchScore }}</span>
                  <span class="match-card__score-label">匹配度</span>
                </div>

                <!-- 岗位信息 -->
                <div class="match-card__main">
                  <div class="match-card__header">
                    <h4 class="match-card__title">{{ item.title }}</h4>
                    <el-tag size="small" type="info" effect="plain" v-if="item.categoryName">
                      {{ item.categoryName }}
                    </el-tag>
                  </div>
                  <div class="match-card__company">
                    <img
                      v-if="item.companyLogo"
                      :src="item.companyLogo"
                      class="match-card__logo"
                      :alt="item.companyName"
                    />
                    <span v-else class="match-card__logo-text">{{ item.companyName?.charAt(0) }}</span>
                    <span>{{ item.companyName }}</span>
                  </div>
                  <div class="match-card__meta">
                    <span v-if="item.city"><el-icon><location /></el-icon> {{ item.city }}</span>
                    <span v-if="item.salaryMin || item.salaryMax">
                      <el-icon><money /></el-icon> {{ formatSalary(item.salaryMin, item.salaryMax) }}
                    </span>
                    <span v-if="item.experience"><el-icon><timer /></el-icon> {{ item.experience }}</span>
                    <span v-if="item.education"><el-icon><reading /></el-icon> {{ item.education }}</span>
                  </div>
                  <div class="match-card__desc" v-if="item.description">
                    {{ item.description }}
                  </div>
                </div>

                <el-icon class="match-card__arrow"><arrow-right /></el-icon>
              </div>
            </div>
          </div>
        </main>
      </div>
    </div>
  </AppShell>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Check, Location, Money, Timer, Reading, ArrowRight } from '@element-plus/icons-vue'
import AppShell from '@/components/AppShell.vue'
import { useUserStore } from '@/stores/user'
import { getUserResumes } from '@/api/user'
import { getResume } from '@/api/resume'
import { matchJobsByResume } from '@/api/jobMatch'
import { getKgCategories } from '@/api/knowledgeGraph'

const router = useRouter()
const userStore = useUserStore()
const { user, isLoggedIn } = userStore

const resumeList = ref([])
const selectedResumeId = ref(null)
const categoryList = ref([])
const matchResults = ref([])
const loading = ref(false)
const hasSearched = ref(false)

const filters = ref({
  city: '',
  categoryId: '',
  topN: 10
})

// 加载简历列表
const loadResumes = async () => {
  if (!isLoggedIn.value || !user.value?.id) return
  try {
    const res = await getUserResumes(user.value.id)
    if (res.data && res.data.length > 0) {
      const details = await Promise.all(
        res.data.map(id => getResume(id).catch(() => null))
      )
      resumeList.value = details
        .filter(d => d && d.data)
        .map(d => d.data)
    }
  } catch (e) {
    console.error('加载简历列表失败', e)
  }
}

// 加载分类列表
const loadCategories = async () => {
  try {
    const res = await getKgCategories()
    categoryList.value = (res.data || []).filter(c => c.enabled !== false)
  } catch (e) {
    console.error('加载分类列表失败', e)
  }
}

// 选择简历
const selectResume = (resume) => {
  selectedResumeId.value = resume.id
  matchResults.value = []
  hasSearched.value = false
}

// 执行匹配
const handleMatch = async () => {
  if (!selectedResumeId.value) {
    ElMessage.warning('请先选择简历')
    return
  }
  loading.value = true
  hasSearched.value = true
  try {
    const params = { topN: filters.value.topN }
    if (filters.value.city) params.city = filters.value.city
    if (filters.value.categoryId) params.categoryId = filters.value.categoryId
    const res = await matchJobsByResume(selectedResumeId.value, params)
    matchResults.value = res?.data?.matches || []
    if (matchResults.value.length > 0) {
      ElMessage.success(`找到 ${matchResults.value.length} 个匹配岗位`)
    }
  } catch (e) {
    ElMessage.error('匹配失败，请稍后重试')
    matchResults.value = []
  } finally {
    loading.value = false
  }
}

// 匹配度样式
const getScoreClass = (score) => {
  if (score >= 80) return 'score--high'
  if (score >= 60) return 'score--medium'
  return 'score--low'
}

// 薪资格式化
const formatSalary = (min, max) => {
  if (min && max) return `${(min / 1000).toFixed(0)}K-${(max / 1000).toFixed(0)}K`
  if (min) return `${(min / 1000).toFixed(0)}K起`
  if (max) return `最高${(max / 1000).toFixed(0)}K`
  return '面议'
}

onMounted(() => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  loadResumes()
  loadCategories()
})
</script>

<style scoped>
.match-page {
  min-height: calc(100vh - 64px);
  background: #f5f6f8;
  padding: 32px 40px;
}

.match-page__container {
  display: flex;
  gap: 24px;
  max-width: 1200px;
  margin: 0 auto;
}

/* 左侧边栏 */
.sidebar {
  width: 300px;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.sidebar-card {
  background: #fff;
  border-radius: 16px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
}

.sidebar-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 16px;
}

.empty-tip {
  padding: 10px 0;
}

/* 简历选择列表 */
.resume-select-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.resume-select-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 14px;
  border: 1.5px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.resume-select-item:hover {
  border-color: #10b981;
  background: #f0fdf4;
}

.resume-select-item.active {
  border-color: #10b981;
  background: #f0fdf4;
}

.resume-select-item__icon {
  font-size: 24px;
  flex-shrink: 0;
}

.resume-select-item__info {
  flex: 1;
  min-width: 0;
}

.resume-select-item__name {
  font-size: 14px;
  font-weight: 500;
  color: #1f2937;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.resume-select-item__meta {
  font-size: 12px;
  color: #6b7280;
  margin-top: 2px;
}

.resume-select-item__check {
  color: #10b981;
  font-size: 18px;
  flex-shrink: 0;
}

/* 右侧内容区 */
.content {
  flex: 1;
  min-width: 0;
}

.content-panel {
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  min-height: 600px;
}

.content-header {
  padding: 24px 28px;
  border-bottom: 1px solid #f1f5f9;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.content-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
}

.match-count {
  font-size: 14px;
  color: #64748b;
}

.match-count strong {
  color: #10b981;
  font-weight: 600;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  text-align: center;
}

.empty-state__icon {
  font-size: 64px;
  margin-bottom: 16px;
}

.empty-state h3 {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin: 0 0 8px;
}

.empty-state p {
  font-size: 14px;
  color: #64748b;
  margin: 0;
}

/* 加载状态 */
.loading-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
}

.loading-spinner {
  width: 48px;
  height: 48px;
  border: 4px solid #e5e7eb;
  border-top-color: #10b981;
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 16px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.loading-state p {
  font-size: 14px;
  color: #64748b;
}

/* 匹配结果列表 */
.match-list {
  padding: 20px 28px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.match-card {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 20px 24px;
  background: #fafbfc;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.25s;
  border: 1px solid transparent;
}

.match-card:hover {
  background: #f0fdf4;
  border-color: #bbf7d0;
  box-shadow: 0 4px 16px rgba(16, 185, 129, 0.1);
  transform: translateY(-2px);
}

/* 匹配度标签 */
.match-card__score {
  flex-shrink: 0;
  width: 72px;
  height: 72px;
  border-radius: 14px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #f0fdf4;
}

.match-card__score-num {
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
}

.match-card__score-label {
  font-size: 11px;
  margin-top: 4px;
}

.score--high {
  background: linear-gradient(135deg, #d1fae5, #a7f3d0);
}

.score--high .match-card__score-num {
  color: #059669;
}

.score--high .match-card__score-label {
  color: #10b981;
}

.score--medium {
  background: linear-gradient(135deg, #fef3c7, #fde68a);
}

.score--medium .match-card__score-num {
  color: #d97706;
}

.score--medium .match-card__score-label {
  color: #f59e0b;
}

.score--low {
  background: linear-gradient(135deg, #fee2e2, #fecaca);
}

.score--low .match-card__score-num {
  color: #dc2626;
}

.score--low .match-card__score-label {
  color: #ef4444;
}

/* 岗位信息 */
.match-card__main {
  flex: 1;
  min-width: 0;
}

.match-card__header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.match-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #0f172a;
  margin: 0;
}

.match-card__company {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #475569;
  margin-bottom: 8px;
}

.match-card__logo {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  object-fit: cover;
}

.match-card__logo-text {
  width: 24px;
  height: 24px;
  border-radius: 6px;
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.match-card__meta {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  font-size: 13px;
  color: #64748b;
}

.match-card__meta span {
  display: flex;
  align-items: center;
  gap: 4px;
}

.match-card__meta .el-icon {
  font-size: 14px;
  color: #94a3b8;
}

.match-card__desc {
  margin-top: 10px;
  font-size: 13px;
  color: #6b7280;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.match-card__arrow {
  flex-shrink: 0;
  font-size: 20px;
  color: #94a3b8;
  transition: all 0.2s;
}

.match-card:hover .match-card__arrow {
  color: #10b981;
  transform: translateX(4px);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .match-page {
    padding: 16px;
  }

  .match-page__container {
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .match-card {
    flex-direction: column;
    align-items: flex-start;
  }

  .match-card__score {
    width: 100%;
    height: auto;
    flex-direction: row;
    gap: 8px;
    padding: 10px 16px;
  }

  .match-card__arrow {
    display: none;
  }
}
</style>
