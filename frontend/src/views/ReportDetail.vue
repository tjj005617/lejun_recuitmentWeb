<template>
  <div class="report-page">
    <!-- 顶部导航 -->
    <header class="report-header">
      <div class="report-header__inner">
        <h1 class="report-header__title">面试评估报告</h1>
        <button class="report-header__back" @click="$router.push('/')">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
            <path d="M19 12H5M12 19l-7-7 7-7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          返回首页
        </button>
      </div>
    </header>

    <main class="report-main" v-if="report">
      <!-- 综合评分 -->
      <section class="report-card">
        <h2 class="report-card__title">综合评分</h2>
        <div class="score-hero">
          <div class="score-circle">
            <svg viewBox="0 0 120 120" class="score-ring">
              <circle cx="60" cy="60" r="52" fill="none" stroke="#f3f4f6" stroke-width="8"/>
              <circle cx="60" cy="60" r="52" fill="none" stroke="#10b981" stroke-width="8"
                stroke-linecap="round"
                :stroke-dasharray="326.7"
                :stroke-dashoffset="326.7 - (326.7 * Math.min(report.totalScore, 10) / 10)"
                transform="rotate(-90 60 60)"
                class="score-ring__fill"/>
            </svg>
            <div class="score-circle__text">
              <span class="score-circle__num">{{ report.totalScore.toFixed(1) }}</span>
              <span class="score-circle__label">/ 10</span>
            </div>
          </div>
          <div class="score-legend">
            <div class="legend-item" v-for="item in scoreItemsList" :key="item.key">
              <span class="legend-dot" :style="{ background: getScoreColor(item.value) }" />
              <span class="legend-label">{{ item.label }}</span>
              <span class="legend-value">{{ item.value }}</span>
            </div>
          </div>
        </div>
        <div class="radar-wrapper">
          <ScoreRadar :scores="averageScores" />
        </div>
      </section>

      <!-- 基本信息 -->
      <section class="report-card">
        <h2 class="report-card__title">基本信息</h2>
        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">面试轮次</span>
            <span class="info-value">{{ report.qaList.length }} 轮</span>
          </div>
          <div class="info-item">
            <span class="info-label">完成时间</span>
            <span class="info-value">{{ report.completedAt }}</span>
          </div>
        </div>
      </section>

      <!-- 详细评估 -->
      <section class="report-card">
        <h2 class="report-card__title">详细评估</h2>
        <div v-for="(qa, index) in report.qaList" :key="index" class="qa-item">
          <div class="qa-item__header" @click="toggleQa(index)">
            <div class="qa-item__left">
              <span class="qa-item__round">第 {{ qa.round }} 轮</span>
              <span class="qa-item__score-badge" :style="{ background: getScoreColor(parseScores(qa.scores).totalScore || 0) + '20', color: getScoreColor(parseScores(qa.scores).totalScore || 0) }">
                {{ (parseScores(qa.scores).totalScore || 0).toFixed(1) }}
              </span>
            </div>
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" :class="['qa-item__arrow', { 'qa-item__arrow--open': openQaIndex === index }]">
              <path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <Transition name="slide">
            <div v-if="openQaIndex === index" class="qa-item__body">
              <div class="qa-item__section">
                <span class="qa-item__tag qa-item__tag--q">问</span>
                <p>{{ qa.question }}</p>
              </div>
              <div class="qa-item__section">
                <span class="qa-item__tag qa-item__tag--a">答</span>
                <p>{{ qa.answer }}</p>
              </div>
              <div v-if="qa.feedback" class="qa-item__section">
                <span class="qa-item__tag qa-item__tag--f">评</span>
                <p>{{ qa.feedback }}</p>
              </div>
              <div v-if="getRefAnswer(qa)" class="qa-item__section">
                <span class="qa-item__tag qa-item__tag--ref">参</span>
                <p>{{ getRefAnswer(qa) }}</p>
              </div>
              <div class="qa-item__bars">
                <div v-for="item in scoreItems(parseScores(qa.scores))" :key="item.key" class="mini-bar">
                  <span class="mini-bar__label">{{ item.label }}</span>
                  <div class="mini-bar__track">
                    <div class="mini-bar__fill" :style="{ width: item.value * 10 + '%', background: getScoreColor(item.value) }" />
                  </div>
                  <span class="mini-bar__val" :style="{ color: getScoreColor(item.value) }">{{ item.value }}</span>
                </div>
              </div>
            </div>
          </Transition>
        </div>
      </section>

      <!-- 评估总结 -->
      <section class="report-card">
        <h2 class="report-card__title">评估总结</h2>
        <div class="summary-grid">
          <div class="summary-box summary-box--success" v-if="strengths.length">
            <div class="summary-box__icon">+</div>
            <div>
              <div class="summary-box__label">优势</div>
              <div class="summary-box__text">{{ strengths.join('、') }}</div>
            </div>
          </div>
          <div class="summary-box summary-box--warning" v-if="weaknesses.length">
            <div class="summary-box__icon">!</div>
            <div>
              <div class="summary-box__label">需提升</div>
              <div class="summary-box__text">{{ weaknesses.join('、') }}</div>
            </div>
          </div>
          <div class="summary-box summary-box--info">
            <div class="summary-box__icon">*</div>
            <div>
              <div class="summary-box__label">建议</div>
              <div class="summary-box__text">{{ weaknesses.length > 0 ? '建议在' + weaknesses.join('、') + '方面加强学习和实践。' : '继续保持良好表现。' }}</div>
            </div>
          </div>
        </div>
      </section>

      <!-- 录用建议 -->
      <section class="report-card">
        <h2 class="report-card__title">录用建议</h2>
        <div class="recommendation" :class="'recommendation--' + getRecommendationLevel(report.totalScore)">
          <span class="recommendation__text">{{ report.recommendation }}</span>
        </div>
      </section>
    </main>

    <!-- 加载状态 -->
    <main class="report-main" v-else>
      <div class="report-card">
        <el-skeleton :rows="8" animated />
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import ScoreRadar from '@/components/ScoreRadar.vue'

const route = useRoute()
const interviewId = route.params.id
const report = ref(null)
const openQaIndex = ref(null)

onMounted(() => { loadReport() })

const loadReport = async () => {
  try {
    const response = await axios.get(`/api/interview/${interviewId}/history`)
    if (response.data.success) {
      // 只加载已回答的问题
      const qaList = response.data.data.filter(qa => qa.answer && qa.scores)
      const totalScore = calculateTotalScore(qaList)
      report.value = {
        qaList,
        totalScore,
        completedAt: new Date().toLocaleString(),
        recommendation: getRecommendation(totalScore)
      }
    }
  } catch (error) {
    console.error('Failed to load report:', error)
  }
}

const toggleQa = (index) => {
  openQaIndex.value = openQaIndex.value === index ? null : index
}

const parseScores = (scoresStr) => {
  if (!scoresStr) return {}
  try { return JSON.parse(scoresStr) } catch { return {} }
}

const getRefAnswer = (qa) => {
  if (qa.referenceAnswer) return qa.referenceAnswer
  const s = parseScores(qa.scores)
  return s.referenceAnswer || ''
}

const scoreItems = (s) => [
  { key: 'accuracy', label: '准确性', value: s.accuracy || 0 },
  { key: 'clarity', label: '清晰度', value: s.clarity || 0 },
  { key: 'logic', label: '逻辑性', value: s.logic || 0 },
  { key: 'depth', label: '深度', value: s.depth || 0 },
  { key: 'practice', label: '实践', value: s.practice || 0 }
]

const scoreItemsList = computed(() => {
  if (!report.value) return []
  return scoreItems(averageScores.value)
})

const averageScores = computed(() => {
  if (!report.value) return {}
  const totals = { accuracy: 0, clarity: 0, logic: 0, depth: 0, practice: 0 }
  let count = 0
  report.value.qaList.forEach(qa => {
    const s = parseScores(qa.scores)
    if (s.totalScore != null) {
      totals.accuracy += s.accuracy || 0
      totals.clarity += s.clarity || 0
      totals.logic += s.logic || 0
      totals.depth += s.depth || 0
      totals.practice += s.practice || 0
      count++
    }
  })
  if (count === 0) return totals
  Object.keys(totals).forEach(k => { totals[k] = +(totals[k] / count).toFixed(1) })
  return totals
})

const strengths = computed(() => {
  if (!report.value) return []
  const set = new Set()
  report.value.qaList.forEach(qa => {
    const s = parseScores(qa.scores)
    if (s.accuracy >= 7) set.add('知识准确性')
    if (s.clarity >= 7) set.add('表达清晰度')
    if (s.logic >= 7) set.add('逻辑思维')
    if (s.depth >= 7) set.add('技术深度')
    if (s.practice >= 7) set.add('实践经验')
  })
  return [...set]
})

const weaknesses = computed(() => {
  if (!report.value) return []
  const set = new Set()
  report.value.qaList.forEach(qa => {
    const s = parseScores(qa.scores)
    if (s.accuracy < 5) set.add('知识准确性')
    if (s.clarity < 5) set.add('表达清晰度')
    if (s.logic < 5) set.add('逻辑思维')
    if (s.depth < 5) set.add('技术深度')
    if (s.practice < 5) set.add('实践经验')
  })
  return [...set]
})

const calculateTotalScore = (qaList) => {
  if (qaList.length === 0) return 0
  let total = 0
  let count = 0
  qaList.forEach(qa => {
    if (!qa.scores) return
    try {
      const scores = JSON.parse(qa.scores)
      if (scores && scores.totalScore != null) {
        total += scores.totalScore
        count++
      }
    } catch { /* ignore */ }
  })
  return count > 0 ? total / count : 0
}

const getRecommendation = (score) => {
  if (score >= 8) return '强烈推荐'
  if (score >= 6) return '推荐'
  if (score >= 4) return '待定'
  return '不推荐'
}

const getRecommendationLevel = (score) => {
  if (score >= 8) return 'strong'
  if (score >= 6) return 'good'
  if (score >= 4) return 'maybe'
  return 'no'
}

const getScoreColor = (val) => {
  if (val >= 8) return '#10b981'
  if (val >= 6) return '#10b981'
  if (val >= 4) return '#f59e0b'
  return '#ef4444'
}
</script>

<style scoped>
.report-page {
  min-height: 100vh;
  background: #f5f6f8;
}

/* Header */
.report-header {
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  position: sticky;
  top: 0;
  z-index: 10;
}

.report-header__inner {
  max-width: 800px;
  margin: 0 auto;
  padding: 14px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.report-header__title {
  font-size: 17px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.report-header__back {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background: transparent;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  color: #4b5563;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.15s;
}

.report-header__back:hover {
  border-color: #10b981;
  color: #10b981;
}

/* Main */
.report-main {
  max-width: 800px;
  margin: 0 auto;
  padding: 20px 16px 40px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* Card */
.report-card {
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 1px 3px rgba(0,0,0,0.04);
}

.report-card__title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0 0 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f3f4f6;
}

/* Score Hero */
.score-hero {
  display: flex;
  align-items: center;
  gap: 32px;
  margin-bottom: 16px;
}

.score-circle {
  position: relative;
  width: 120px;
  height: 120px;
  flex-shrink: 0;
}

.score-ring {
  width: 100%;
  height: 100%;
}

.score-ring__fill {
  transition: stroke-dashoffset 1s ease;
}

.score-circle__text {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-direction: column;
}

.score-circle__num {
  font-size: 28px;
  font-weight: 700;
  color: #1f2937;
  line-height: 1;
}

.score-circle__label {
  font-size: 12px;
  color: #9ca3af;
  margin-top: 2px;
}

.score-legend {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex: 1;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

.legend-label {
  font-size: 13px;
  color: #6b7280;
  flex: 1;
}

.legend-value {
  font-size: 14px;
  font-weight: 600;
  color: #1f2937;
}

.radar-wrapper {
  max-width: 320px;
  margin: 0 auto;
}

/* Info */
.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-label {
  font-size: 12px;
  color: #9ca3af;
}

.info-value {
  font-size: 14px;
  color: #1f2937;
}

/* QA Items */
.qa-item {
  border: 1px solid #f3f4f6;
  border-radius: 10px;
  margin-bottom: 10px;
  overflow: hidden;
}

.qa-item__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.15s;
}

.qa-item__header:hover {
  background: #fafafa;
}

.qa-item__left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.qa-item__round {
  font-size: 14px;
  font-weight: 500;
  color: #374151;
}

.qa-item__score-badge {
  font-size: 12px;
  font-weight: 600;
  padding: 2px 8px;
  border-radius: 10px;
}

.qa-item__arrow {
  color: #9ca3af;
  transition: transform 0.2s;
}

.qa-item__arrow--open {
  transform: rotate(180deg);
}

.qa-item__body {
  padding: 0 16px 16px;
}

.qa-item__section {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
  font-size: 13px;
  line-height: 1.6;
  color: #374151;
}

.qa-item__tag {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  font-weight: 600;
  margin-top: 1px;
}

.qa-item__tag--q { background: #ecfdf5; color: #10b981; }
.qa-item__tag--a { background: #ecfdf5; color: #059669; }
.qa-item__tag--f { background: #fffbeb; color: #d97706; }
.qa-item__tag--ref { background: #eff6ff; color: #2563eb; }

/* Mini bars */
.qa-item__bars {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 6px 16px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #f3f4f6;
}

.mini-bar {
  display: flex;
  align-items: center;
  gap: 6px;
}

.mini-bar__label {
  font-size: 11px;
  color: #9ca3af;
  width: 32px;
  flex-shrink: 0;
}

.mini-bar__track {
  flex: 1;
  height: 4px;
  background: #f3f4f6;
  border-radius: 2px;
  overflow: hidden;
}

.mini-bar__fill {
  height: 100%;
  border-radius: 2px;
}

.mini-bar__val {
  font-size: 11px;
  font-weight: 600;
  width: 16px;
  text-align: right;
}

/* Summary */
.summary-grid {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.summary-box {
  display: flex;
  gap: 12px;
  padding: 14px 16px;
  border-radius: 10px;
}

.summary-box--success { background: #ecfdf5; }
.summary-box--warning { background: #fffbeb; }
.summary-box--info { background: #eff6ff; }

.summary-box__icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 700;
  flex-shrink: 0;
  margin-top: 1px;
}

.summary-box--success .summary-box__icon { background: #059669; color: #fff; }
.summary-box--warning .summary-box__icon { background: #d97706; color: #fff; }
.summary-box--info .summary-box__icon { background: #2563eb; color: #fff; }

.summary-box__label {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 2px;
}

.summary-box--success .summary-box__label { color: #059669; }
.summary-box--warning .summary-box__label { color: #d97706; }
.summary-box--info .summary-box__label { color: #2563eb; }

.summary-box__text {
  font-size: 13px;
  color: #374151;
  line-height: 1.5;
}

/* Recommendation */
.recommendation {
  display: inline-flex;
  padding: 8px 20px;
  border-radius: 8px;
  font-size: 15px;
  font-weight: 600;
}

.recommendation--strong { background: #ecfdf5; color: #059669; }
.recommendation--good { background: #ecfdf5; color: #10b981; }
.recommendation--maybe { background: #fffbeb; color: #d97706; }
.recommendation--no { background: #fef2f2; color: #dc2626; }

/* Slide transition */
.slide-enter-active { transition: all 0.25s ease; }
.slide-leave-active { transition: all 0.2s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; max-height: 0; }
.slide-enter-to, .slide-leave-from { opacity: 1; max-height: 500px; }

@media (max-width: 640px) {
  .score-hero {
    flex-direction: column;
    align-items: flex-start;
    gap: 20px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .qa-item__bars {
    grid-template-columns: 1fr;
  }
}
</style>
