<template>
  <AppShell :bgImage="false">
    <div class="upload-page">
      <el-card class="upload-card">
        <template #header>
          <div class="card-header">
            <h2>AI模拟面试</h2>
            <el-button text @click="$router.back()">返回</el-button>
          </div>
        </template>

        <!-- 已有简历选择 -->
        <div v-if="resumeList.length > 0" class="resume-select-section">
          <div class="section-label">选择已有简历</div>
          <div class="resume-select-list">
            <div
              v-for="r in resumeList"
              :key="r.id"
              class="resume-select-item"
              :class="{ active: resumeId === r.id }"
              @click="resumeId = r.id"
            >
              <div class="resume-select-item__icon"> </div>
              <div class="resume-select-item__info">
                <div class="resume-select-item__name">{{ r.fileName }}</div>
                <div class="resume-select-item__meta">
                  <span v-if="r.name">{{ r.name }}</span>
                  <span v-if="r.education">· {{ r.education }}</span>
                </div>
              </div>
              <el-icon v-if="resumeId === r.id" class="resume-select-item__check"><check /></el-icon>
            </div>
          </div>
          <el-divider>或上传新简历</el-divider>
        </div>

        <el-upload
          class="upload-area"
          drag
          :action="`/api/resume/upload`"
          :on-success="handleSuccess"
          :on-error="handleError"
          :before-upload="beforeUpload"
        >
          <el-icon class="el-icon--upload"><upload-filled /></el-icon>
          <div class="el-upload__text">
            拖拽文件到此处，或<em>点击上传</em>
          </div>
          <template #tip>
            <div class="el-upload__tip">
              支持 PDF、DOCX、TXT 格式，文件大小不超过 10MB
            </div>
          </template>
        </el-upload>

        <Transition name="slide-fade">
          <div v-if="resumeId" class="next-step">
            <el-divider />
            <el-form :model="form" label-width="100px">
              <el-form-item label="应聘岗位">
                <el-select v-model="form.jobType" placeholder="请选择岗位类型" style="width: 100%">
                  <el-option label="前端工程师" value="前端工程师" />
                  <el-option label="后端工程师" value="后端工程师" />
                  <el-option label="全栈工程师" value="全栈工程师" />
                  <el-option label="数据工程师" value="数据工程师" />
                  <el-option label="产品经理" value="产品经理" />
                </el-select>
              </el-form-item>

              <!-- 面试模式选择 -->
              <el-form-item label="面试模式">
                <div class="mode-select">
                  <div
                    class="mode-card"
                    :class="{ active: interviewMode === 'text' }"
                    @click="interviewMode = 'text'"
                  >
                    <div class="mode-card__icon">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="28" height="28">
                        <path d="M4 6h16M4 12h16M4 18h10"/>
                      </svg>
                    </div>
                    <div class="mode-card__info">
                      <div class="mode-card__name">文字面试</div>
                      <div class="mode-card__desc">输入文字回答问题</div>
                    </div>
                  </div>
                  <div
                    class="mode-card"
                    :class="{ active: interviewMode === 'immersive' }"
                    @click="interviewMode = 'immersive'"
                  >
                    <div class="mode-card__icon">
                      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5" width="28" height="28">
                        <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
                        <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                        <line x1="12" y1="19" x2="12" y2="23"/>
                        <line x1="8" y1="23" x2="16" y2="23"/>
                      </svg>
                    </div>
                    <div class="mode-card__info">
                      <div class="mode-card__name">沉浸式面试</div>
                      <div class="mode-card__desc">语音对话，像真实面试</div>
                    </div>
                    <span class="mode-card__badge">推荐</span>
                  </div>
                </div>
              </el-form-item>

              <el-form-item>
                <el-button type="primary" @click="startInterview" :disabled="!form.jobType" :loading="creating">
                  开始面试
                </el-button>
              </el-form-item>
            </el-form>
          </div>
        </Transition>
      </el-card>
    </div>

    <!-- 加载遮罩 -->
    <Transition name="fade">
      <div v-if="creating" class="loading-overlay">
        <div class="loading-content">
          <div class="loading-avatar">面</div>
          <div class="loading-text">AI 正在分析你的简历...</div>
          <div class="loading-progress">
            <div class="loading-progress__bar">
              <div class="loading-progress__fill" :style="{ width: progressPercent + '%' }"></div>
            </div>
            <span class="loading-progress__text">{{ progressPercent }}%</span>
          </div>
          <div class="loading-tip">{{ currentTip }}</div>
        </div>
      </div>
    </Transition>
  </AppShell>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElIcon } from 'element-plus'
import { Check, UploadFilled } from '@element-plus/icons-vue'
import axios from 'axios'
import AppShell from '@/components/AppShell.vue'
import { useUserStore } from '@/stores/user'
import { getUserResumes } from '@/api/user'
import { getResume } from '@/api/resume'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const { user, isLoggedIn } = userStore

const resumeId = ref(route.query.resumeId ? Number(route.query.resumeId) : null)
const resumeList = ref([])
const form = ref({ jobType: '' })
const interviewMode = ref('immersive') // 面试模式：text=文字面试, immersive=沉浸式面试
const creating = ref(false)
const progressPercent = ref(0)
let progressTimer = null

const tips = [
  '面试将围绕你的简历内容展开，加油！',
  '回答时尽量条理清晰，分点作答效果更好',
  '遇到不会的问题不要慌，可以适当思考后再回答',
  '展示你的项目经历时，建议用 STAR 法则描述',
  '面试不仅是考核，也是了解岗位的好机会',
  '保持自信，真实展现自己的能力就好'
]
const currentTip = ref(tips[0])
let tipIndex = 0

const startProgress = () => {
  progressPercent.value = 0
  tipIndex = 0
  currentTip.value = tips[0]
  progressTimer = setInterval(() => {
    if (progressPercent.value < 95) {
      // 越到后面越慢：0-30%每次+2，30-60%每次+1，60-90%每次+0.5，90%以上几乎不动
      let increment
      if (progressPercent.value < 30) increment = 2
      else if (progressPercent.value < 60) increment = 1
      else if (progressPercent.value < 90) increment = 0.5
      else increment = 0.2
      progressPercent.value = Math.min(progressPercent.value + increment, 95)
      progressPercent.value = Math.round(progressPercent.value * 10) / 10
    }
    // 每 20% 切换一条提示
    const newTipIndex = Math.floor(progressPercent.value / 20)
    if (newTipIndex !== tipIndex && newTipIndex < tips.length) {
      tipIndex = newTipIndex
      currentTip.value = tips[tipIndex]
    }
  }, 500)
}

const stopProgress = () => {
  if (progressTimer) {
    clearInterval(progressTimer)
    progressTimer = null
  }
  progressPercent.value = 100
}

// 加载用户简历列表
const loadResumes = async () => {
  if (!isLoggedIn.value || !user.value?.id) return
  try {
    const res = await getUserResumes(user.value.id)
    if (res.data && res.data.length > 0) {
      // 获取每份简历的详细信息
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

// 未登录直接跳转登录页
onMounted(() => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }
  loadResumes()
})

onUnmounted(() => {
  if (progressTimer) clearInterval(progressTimer)
})

const beforeUpload = (file) => {
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isLt10M) {
    ElMessage.error('文件大小不能超过 10MB!')
    return false
  }
  return true
}

const handleSuccess = async (response) => {
  if (response.success) {
    resumeId.value = response.data.id
    ElMessage.success('简历上传成功!')

    // 自动关联简历到用户
    try {
      await axios.post(`/api/user/${user.value.id}/resumes`, { resumeId: response.data.id })
    } catch (e) {
      console.error('关联简历失败', e)
    }
  } else {
    ElMessage.error(response.message || '上传失败')
  }
}

const handleError = () => {
  ElMessage.error('上传失败，请重试')
}

const startInterview = async () => {
  creating.value = true
  startProgress()
  try {
    const response = await axios.post('/api/interview/create', {
      userId: user.value.id,
      resumeId: resumeId.value,
      jobType: form.value.jobType
    })
    if (response.data.success) {
      stopProgress()
      const { interview, questions } = response.data.data
      // 存储问题列表到sessionStorage，面试页面读取
      sessionStorage.setItem(`interview_questions_${interview.id}`, JSON.stringify(questions))
      // 根据选择的面试模式跳转
      if (interviewMode.value === 'immersive') {
        router.push(`/interview/${interview.id}/immersive`)
      } else {
        router.push(`/interview/${interview.id}`)
      }
    } else {
      ElMessage.error(response.data.message || '创建面试失败')
    }
  } catch (error) {
    ElMessage.error('创建面试失败')
  } finally {
    stopProgress()
    creating.value = false
  }
}
</script>

<style scoped>
.upload-page {
  min-height: calc(100vh - 64px);
  background: #f5f6f8;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.upload-card {
  width: min(600px, calc(100vw - 48px));
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header h2 {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
}

.upload-area {
  width: 100%;
}

/* 简历选择区域 */
.resume-select-section {
  margin-bottom: 16px;
}

.section-label {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
  margin-bottom: 12px;
}

.resume-select-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.resume-select-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
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
}

.next-step {
  margin-top: var(--space-4);
}

/* 面试模式选择 */
.mode-select {
  display: flex;
  gap: 12px;
  width: 100%;
}

.mode-card {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s ease;
  position: relative;
}

.mode-card:hover {
  border-color: #d1d5db;
  background: #f9fafb;
}

.mode-card.active {
  border-color: #10b981;
  background: #f0fdf4;
}

.mode-card__icon {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  color: #6b7280;
  transition: all 0.2s ease;
}

.mode-card.active .mode-card__icon {
  background: #10b981;
  color: #fff;
}

.mode-card__info {
  flex: 1;
  min-width: 0;
}

.mode-card__name {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.mode-card__desc {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.mode-card__badge {
  position: absolute;
  top: -8px;
  right: 8px;
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
}

.slide-fade-enter-active {
  transition: all var(--duration-slow) var(--easing-out);
}

.slide-fade-leave-active {
  transition: all var(--duration-fast) var(--easing-in);
}

.slide-fade-enter-from {
  opacity: 0;
  transform: translateY(12px);
}

.slide-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* 加载遮罩 */
.loading-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.95);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
}

.loading-content {
  text-align: center;
}

.loading-avatar {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #10b981, #059669);
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 24px;
  font-weight: 600;
  margin: 0 auto 16px;
  animation: pulse 2s ease-in-out infinite;
}

.loading-text {
  font-size: 16px;
  color: #4b5563;
  margin-bottom: 20px;
}

/* 进度条 */
.loading-progress {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 20px;
}

.loading-progress__bar {
  flex: 1;
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
}

.loading-progress__fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981, #059669);
  border-radius: 3px;
  transition: width 0.3s ease;
}

.loading-progress__text {
  font-size: 13px;
  font-weight: 600;
  color: #10b981;
  min-width: 36px;
  text-align: right;
}

/* 温馨提示 */
.loading-tip {
  font-size: 13px;
  color: #94a3b8;
  line-height: 1.5;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.fade-enter-active, .fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from, .fade-leave-to {
  opacity: 0;
}
</style>
