<template>
  <AppShell :bgImage="false">
    <div class="choice-page">
      <!-- 顶部栏 -->
      <div class="choice-header">
        <div class="choice-header__left">
          <span class="choice-header__title">八股选择题</span>
          <span class="choice-header__progress">{{ currentRound }} / {{ totalRounds }}</span>
        </div>
        <el-button text @click="handleExit">退出</el-button>
      </div>

      <!-- 答题区域 -->
      <div v-if="!submitted" class="choice-body">
        <div class="choice-question">
          <div class="choice-question__num">Q{{ currentRound }}</div>
          <div class="choice-question__text">{{ currentQuestionData?.question }}</div>
        </div>

        <div class="choice-options">
          <div
            v-for="(text, key) in (currentQuestionData?.options || {})"
            :key="key"
            class="choice-option"
            :class="{ active: answers[currentRound] === key }"
            @click="selectOption(key)"
          >
            <span class="choice-option__key">{{ key }}</span>
            <span class="choice-option__text">{{ text }}</span>
          </div>
        </div>

        <div class="choice-nav">
          <el-button :disabled="currentRound <= 1" @click="currentRound--">上一题</el-button>
          <el-button
            v-if="currentRound < totalRounds"
            type="primary"
            @click="currentRound++"
          >下一题</el-button>
          <el-button
            v-else
            type="success"
            @click="confirmSubmit"
          >提交答卷</el-button>
        </div>
      </div>

      <!-- 答题卡 -->
      <div v-if="!submitted" class="choice-card">
        <div class="choice-card__title">答题卡</div>
        <div class="choice-card__grid">
          <div
            v-for="i in totalRounds"
            :key="i"
            class="choice-card__item"
            :class="{
              answered: answers[i],
              current: i === currentRound
            }"
            @click="currentRound = i"
          >{{ i }}</div>
        </div>
        <div class="choice-card__footer">
          <span class="choice-card__answered">已答 {{ answeredCount }} / {{ totalRounds }}</span>
          <el-button
            type="success"
            :disabled="answeredCount === 0"
            @click="confirmSubmit"
          >提交答卷</el-button>
        </div>
      </div>

      <!-- 结果展示 -->
      <div v-if="submitted" class="choice-result">
        <div class="choice-result__score">
          <div class="choice-result__number">{{ resultData?.totalScore }}</div>
          <div class="choice-result__label">总分</div>
          <div class="choice-result__detail">
            正确 {{ resultData?.correctCount }} / {{ resultData?.total }} 题
          </div>
        </div>

        <div class="choice-result__list">
          <div
            v-for="item in resultData?.details"
            :key="item.round"
            class="choice-result__item"
            :class="{ correct: item.correct, wrong: !item.correct }"
          >
            <div class="choice-result__item-header">
              <span class="choice-result__item-num">Q{{ item.round }}</span>
              <span class="choice-result__item-status">{{ item.correct ? '正确' : '错误' }}</span>
            </div>
            <div class="choice-result__item-answers">
              <span>你的答案：{{ item.userAnswer || '未作答' }}</span>
              <span v-if="!item.correct">正确答案：{{ item.correctAnswer }}</span>
            </div>
            <div class="choice-result__item-explanation" v-if="item.explanation">
              {{ item.explanation }}
            </div>
          </div>
        </div>

        <div class="choice-result__actions">
          <el-button @click="$router.push('/user?tab=ai-interview')">返回</el-button>
          <el-button type="primary" @click="$router.push(`/report/${interviewId}`)">查看报告</el-button>
        </div>
      </div>
    </div>
  </AppShell>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import AppShell from '@/components/AppShell.vue'

const route = useRoute()
const router = useRouter()
const interviewId = route.params.id

// 题目数据
const questions = ref([])
const totalRounds = computed(() => questions.value.length)
const currentRound = ref(1)
const answers = ref({}) // { 1: 'A', 2: 'B', ... }

// 当前题目
const currentQuestionData = computed(() => {
  const q = questions.value[currentRound.value - 1]
  if (!q) return null
  // 已经是对象且有 question + options，直接用
  if (typeof q === 'object' && q.question && q.options) return q
  if (typeof q !== 'string') return { question: String(q), options: {} }
  // 尝试解析 JSON 字符串
  try {
    const parsed = JSON.parse(q)
    if (parsed && parsed.question && parsed.options) return parsed
    // 解析成功但结构不对，把整个内容当题目
    return { question: q, options: {} }
  } catch {
    // JSON 解析失败，尝试从原始文本中提取 question 和 options
    return parseRawQuestion(q)
  }
})

/** 从原始文本中尽力提取题目和选项 */
const parseRawQuestion = (raw) => {
  const result = { question: raw, options: {} }
  try {
    // 尝试提取 question 字段
    const qMatch = raw.match(/"question"\s*:\s*"((?:[^"\\]|\\.)*)"/)
    if (qMatch) result.question = qMatch[1].replace(/\\"/g, '"').replace(/\\n/g, '\n')
    // 尝试提取 options 对象
    const oMatch = raw.match(/"options"\s*:\s*(\{[^}]+\})/)
    if (oMatch) {
      const optionsStr = oMatch[1].replace(/\\\"/g, '"')
      result.options = JSON.parse(optionsStr)
    }
  } catch { /* ignore */ }
  return result
}

// 已答数量
const answeredCount = computed(() => Object.keys(answers.value).length)

// 提交状态
const submitted = ref(false)
const resultData = ref(null)

// 选择选项
const selectOption = (key) => {
  answers.value = { ...answers.value, [currentRound.value]: key }
}

// 确认提交
const confirmSubmit = async () => {
  const unanswered = totalRounds.value - answeredCount.value
  let msg = '确定提交答卷？'
  if (unanswered > 0) {
    msg = `还有 ${unanswered} 道题未作答，确定提交？`
  }
  try {
    await ElMessageBox.confirm(msg, '提示', { type: 'warning' })
    await submitAnswers()
  } catch {
    // 用户取消
  }
}

// 提交答案
const submitAnswers = async () => {
  try {
    const payload = { answers: {} }
    for (const [round, ans] of Object.entries(answers.value)) {
      payload.answers[round] = ans
    }
    const res = await axios.post(`/api/interview/${interviewId}/submit-choice`, payload)
    if (res.data.success) {
      resultData.value = res.data.data
      submitted.value = true
    } else {
      ElMessage.error(res.data.message || '提交失败')
    }
  } catch (e) {
    ElMessage.error('提交失败')
  }
}

// 退出
const handleExit = async () => {
  try {
    await ElMessageBox.confirm('确定退出？未提交的答案将丢失', '提示', { type: 'warning' })
    router.push('/user?tab=ai-interview')
  } catch {
    // 取消
  }
}

onMounted(() => {
  // 从 sessionStorage 读取题目
  const stored = sessionStorage.getItem(`interview_questions_${interviewId}`)
  if (stored) {
    try {
      questions.value = JSON.parse(stored)
    } catch {
      ElMessage.error('题目数据加载失败')
      router.back()
    }
  } else {
    ElMessage.error('找不到题目数据')
    router.back()
  }
})
</script>

<style scoped>
.choice-page {
  min-height: calc(100vh - 64px);
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
}

/* 顶部栏 */
.choice-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 24px;
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
}

.choice-header__left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.choice-header__title {
  font-size: 16px;
  font-weight: 600;
  color: #1a1a1a;
}

.choice-header__progress {
  font-size: 14px;
  color: #666;
}

/* 答题区域 */
.choice-body {
  flex: 1;
  max-width: 720px;
  margin: 24px auto;
  padding: 0 24px;
  width: 100%;
}

.choice-question {
  background: #fff;
  border-radius: 8px;
  padding: 24px;
  margin-bottom: 16px;
  border: 1px solid #e5e7eb;
}

.choice-question__num {
  font-size: 13px;
  font-weight: 600;
  color: #4338ca;
  margin-bottom: 8px;
}

.choice-question__text {
  font-size: 16px;
  color: #1a1a1a;
  line-height: 1.6;
}

/* 选项 */
.choice-options {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 24px;
}

.choice-option {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 18px;
  background: #fff;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.15s;
}

.choice-option:hover {
  border-color: #d1d5db;
  background: #fafafa;
}

.choice-option.active {
  border-color: #4338ca;
  background: #eef2ff;
}

.choice-option__key {
  width: 28px;
  height: 28px;
  border-radius: 50%;
  background: #f3f4f6;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  color: #555;
  flex-shrink: 0;
  transition: all 0.15s;
}

.choice-option.active .choice-option__key {
  background: #4338ca;
  color: #fff;
}

.choice-option__text {
  font-size: 15px;
  color: #1a1a1a;
}

/* 导航按钮 */
.choice-nav {
  display: flex;
  justify-content: space-between;
}

/* 答题卡 */
.choice-card {
  background: #fff;
  border-top: 1px solid #e5e7eb;
  padding: 16px 24px;
}

.choice-card__title {
  font-size: 13px;
  font-weight: 600;
  color: #666;
  margin-bottom: 10px;
}

.choice-card__grid {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-bottom: 12px;
}

.choice-card__item {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  border: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #666;
  cursor: pointer;
  transition: all 0.15s;
}

.choice-card__item:hover {
  border-color: #d1d5db;
}

.choice-card__item.answered {
  background: #4338ca;
  border-color: #4338ca;
  color: #fff;
}

.choice-card__item.current {
  border-color: #4338ca;
  box-shadow: 0 0 0 2px rgba(67, 56, 202, 0.2);
}

.choice-card__footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.choice-card__answered {
  font-size: 13px;
  color: #999;
}

/* 结果页 */
.choice-result {
  max-width: 720px;
  margin: 24px auto;
  padding: 0 24px;
  width: 100%;
}

.choice-result__score {
  text-align: center;
  background: #fff;
  border-radius: 8px;
  padding: 32px;
  border: 1px solid #e5e7eb;
  margin-bottom: 20px;
}

.choice-result__number {
  font-size: 48px;
  font-weight: 700;
  color: #4338ca;
}

.choice-result__label {
  font-size: 14px;
  color: #999;
  margin-top: 4px;
}

.choice-result__detail {
  font-size: 14px;
  color: #666;
  margin-top: 8px;
}

.choice-result__list {
  display: flex;
  flex-direction: column;
  gap: 10px;
  margin-bottom: 20px;
}

.choice-result__item {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  border: 1px solid #e5e7eb;
}

.choice-result__item.correct {
  border-left: 3px solid #10b981;
}

.choice-result__item.wrong {
  border-left: 3px solid #ef4444;
}

.choice-result__item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.choice-result__item-num {
  font-size: 13px;
  font-weight: 600;
  color: #4338ca;
}

.choice-result__item-status {
  font-size: 12px;
  font-weight: 600;
}

.choice-result__item.correct .choice-result__item-status {
  color: #10b981;
}

.choice-result__item.wrong .choice-result__item-status {
  color: #ef4444;
}

.choice-result__item-answers {
  display: flex;
  gap: 16px;
  font-size: 13px;
  color: #555;
  margin-bottom: 8px;
}

.choice-result__item-explanation {
  font-size: 13px;
  color: #888;
  padding: 8px 12px;
  background: #f9fafb;
  border-radius: 6px;
  line-height: 1.5;
}

.choice-result__actions {
  display: flex;
  justify-content: center;
  gap: 12px;
}
</style>
