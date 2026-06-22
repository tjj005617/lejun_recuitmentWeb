<template>
  <div class="interview-room">
    <!-- Header -->
    <header class="interview-header">
      <div class="interview-header__inner">
        <div class="interview-header__left">
          <div class="interview-header__logo">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.5"/>
              <circle cx="9" cy="10" r="1.5" fill="currentColor"/>
              <circle cx="15" cy="10" r="1.5" fill="currentColor"/>
              <path d="M8 14.5c0 0 1.5 2 4 2s4-2 4-2" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <h1 class="interview-header__title">模拟面试</h1>
        </div>
        <div class="interview-header__right">
          <span class="interview-header__round">第 {{ currentRound }} / 10 轮</span>
          <div class="interview-header__bar">
            <div class="interview-header__bar-fill" :style="{ width: currentRound * 10 + '%' }" />
          </div>
        </div>
      </div>
    </header>

    <!-- Chat Area -->
    <main class="interview-main">
      <div class="chat-container" ref="chatContainer" role="log" aria-live="polite">
        <!-- 时间戳分隔 -->
        <div class="chat-time">{{ currentTime }}</div>

        <template v-for="(msg, index) in messages" :key="index">
          <!-- AI 消息 -->
          <div v-if="msg.type === 'ai'" class="msg msg--ai">
            <div class="msg__avatar">
              <div class="avatar avatar--ai">面</div>
            </div>
            <div class="msg__body">
              <div class="msg__name">面试官</div>
              <div class="msg__bubble msg__bubble--ai">
                <div>{{ msg.content }}</div>
                <!-- 当前问题的朗读按钮 -->
                <div v-if="isCurrentQuestion(msg)" class="msg__tts">
                  <button class="tts-btn" :class="{ playing: ttsPlaying, loading: ttsLoading }"
                          @click="playTTS(msg.content)" :disabled="ttsLoading">
                    <!-- 加载中 -->
                    <svg v-if="ttsLoading" class="tts-btn__icon tts-btn__icon--spin" width="14" height="14" viewBox="0 0 24 24" fill="none">
                      <path d="M12 2v4m0 12v4m-7.07-3.93l2.83-2.83m8.48-8.48l2.83-2.83M2 12h4m12 0h4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    </svg>
                    <!-- 播放中 -->
                    <svg v-else-if="ttsPlaying" width="14" height="14" viewBox="0 0 24 24" fill="none">
                      <rect x="6" y="4" width="4" height="16" rx="1" fill="currentColor"/>
                      <rect x="14" y="4" width="4" height="16" rx="1" fill="currentColor"/>
                    </svg>
                    <!-- 默认 -->
                    <svg v-else width="14" height="14" viewBox="0 0 24 24" fill="none">
                      <path d="M11 5L6 9H2v6h4l5 4V5z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                      <path d="M15.54 8.46a5 5 0 010 7.07" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                      <path d="M19.07 4.93a10 10 0 010 14.14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                    </svg>
                    <span class="tts-btn__text">{{ ttsLoading ? '生成中...' : ttsPlaying ? '停止' : '朗读' }}</span>
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 评分反馈消息 -->
          <div v-else-if="msg.type === 'evaluation'" class="msg msg--ai">
            <div class="msg__avatar">
              <div class="avatar avatar--ai">面</div>
            </div>
            <div class="msg__body">
              <div class="msg__name">评分反馈</div>
              <div class="score-card">
                <div class="score-card__row" v-for="(item) in scoreItems(msg.score)" :key="item.key">
                  <span class="score-card__label">{{ item.label }}</span>
                  <div class="score-card__bar-wrap">
                    <div class="score-card__bar" :style="{ width: item.value * 10 + '%', background: getScoreColor(item.value) }" />
                  </div>
                  <span class="score-card__value" :style="{ color: getScoreColor(item.value) }">{{ item.value }}</span>
                </div>
                <div v-if="msg.feedback" class="score-card__feedback">
                  <span class="score-card__feedback-icon">💡</span>
                  {{ msg.feedback }}
                </div>
                <!-- 参考答案 -->
                <div v-if="msg.referenceAnswer" class="score-card__ref">
                  <button class="score-card__ref-toggle" @click="msg.showRef = !msg.showRef">
                    <svg width="14" height="14" viewBox="0 0 24 24" fill="none" :class="{ 'rotated': msg.showRef }">
                      <path d="M6 9l6 6 6-6" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                    </svg>
                    {{ msg.showRef ? '收起参考答案' : '查看参考答案' }}
                  </button>
                  <Transition name="slide">
                    <div v-if="msg.showRef" class="score-card__ref-text">
                      {{ msg.referenceAnswer }}
                    </div>
                  </Transition>
                </div>
              </div>
            </div>
          </div>

          <!-- 用户消息 -->
          <div v-else-if="msg.type === 'user'" class="msg msg--user">
            <div class="msg__body msg__body--right">
              <div class="msg__name msg__name--right">我</div>
              <div class="msg__bubble msg__bubble--user">{{ msg.content }}</div>
            </div>
            <div class="msg__avatar">
              <div class="avatar avatar--user">我</div>
            </div>
          </div>

          <!-- 系统消息 -->
          <div v-else class="msg msg--system">
            <div class="msg__system-text">{{ msg.content }}</div>
          </div>
        </template>

        <!-- 打字指示器 -->
        <div v-if="isAiTyping" class="msg msg--ai">
          <div class="msg__avatar">
            <div class="avatar avatar--ai">面</div>
          </div>
          <div class="msg__body">
            <div class="msg__name">面试官</div>
            <div class="msg__bubble msg__bubble--ai msg__bubble--typing">
              <span>{{ typingText }}</span>
              <span class="typing-dots"><i /><i /><i /></span>
            </div>
          </div>
        </div>
      </div>

      <!-- Input Area -->
      <div class="chat-input">
        <div class="chat-input__top">
          <button class="chat-exit-btn" @click="exitInterview" :disabled="isCompleted || isAiTyping">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
              <path d="M9 21H5a2 2 0 01-2-2V5a2 2 0 012-2h4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <polyline points="16 17 21 12 16 7" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <line x1="21" y1="12" x2="9" y2="12" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            结束面试
          </button>
        </div>
        <div class="chat-input__wrap">
          <el-input
            v-model="answer"
            type="textarea"
            :rows="2"
            :autosize="{ minRows: 2, maxRows: 6 }"
            placeholder="输入你的回答，Ctrl+Enter 发送"
            :disabled="isCompleted || isAiTyping"
            @keydown.enter.ctrl="submitAnswer"
            resize="none"
          />
          <!-- 语音输入按钮 -->
          <button
            class="chat-input__voice"
            :class="{ recording: isRecording, loading: sttLoading }"
            @click="toggleVoiceInput"
            :disabled="isCompleted || isAiTyping || sttLoading"
            :title="isRecording ? '点击停止录音' : '语音输入'"
          >
            <!-- 录音中：停止方块 -->
            <svg v-if="isRecording" width="18" height="18" viewBox="0 0 24 24" fill="none">
              <rect x="6" y="6" width="12" height="12" rx="2" fill="currentColor"/>
            </svg>
            <!-- 识别中：旋转加载 -->
            <svg v-else-if="sttLoading" class="spin" width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M12 2v4m0 12v4m-7.07-3.93l2.83-2.83m8.48-8.48l2.83-2.83M2 12h4m12 0h4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
            <!-- 空闲：麦克风 -->
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" stroke="currentColor" stroke-width="2"/>
              <path d="M19 10v2a7 7 0 0 1-14 0v-2" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <line x1="12" y1="19" x2="12" y2="23" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <line x1="8" y1="23" x2="16" y2="23" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
            </svg>
          </button>
          <button
            class="chat-input__send"
            @click="submitAnswer"
            :disabled="!answer.trim() || isCompleted || isAiTyping"
            :class="{ 'chat-input__send--active': answer.trim() && !isCompleted && !isAiTyping }"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
              <path d="M22 2L11 13" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <path d="M22 2L15 22L11 13L2 9L22 2Z" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import axios from 'axios'
import { startRecording } from '@/utils/audioRecorder'
import { recognizeSpeech } from '@/api/stt'

const route = useRoute()
const router = useRouter()
const interviewId = route.params.id

const messages = ref([])
const answer = ref('')
const currentRound = ref(0)
const currentQuestion = ref('')
const questions = ref([]) // 全部10道问题
const loading = ref(false)
const isCompleted = ref(false)

// TTS 状态
const ttsLoading = ref(false)
const ttsPlaying = ref(false)

// STT 语音输入状态
const isRecording = ref(false)
const sttLoading = ref(false)
let recorderInstance = null
let ttsAudio = null
const isAiTyping = ref(false)
const typingText = ref('')
const chatContainer = ref(null)
const currentTime = ref('')

let ws = null
let typingInterval = null
let timeInterval = null

const storageKey = `interview_messages_${interviewId}`

const saveMessages = () => {
  try {
    sessionStorage.setItem(storageKey, JSON.stringify(messages.value))
  } catch { /* ignore */ }
}

const loadCachedMessages = () => {
  try {
    const cached = sessionStorage.getItem(storageKey)
    if (cached) {
      const parsed = JSON.parse(cached)
      if (parsed.length > 0) {
        messages.value = parsed
        // 恢复 currentRound 和 currentQuestion
        for (let i = parsed.length - 1; i >= 0; i--) {
          if (parsed[i].type === 'question') {
            currentQuestion.value = parsed[i].content
          }
          if (parsed[i].type === 'question' || parsed[i].type === 'evaluation') {
            if (parsed[i].round) currentRound.value = parsed[i].round
          }
        }
        scrollToBottom()
        return true
      }
    }
  } catch { /* ignore */ }
  return false
}

const loadHistoryFromServer = async () => {
  try {
    const res = await axios.get(`/api/interview/${interviewId}/history`)
    if (res.data.success && res.data.data.length > 0) {
      const historyMessages = []
      // 只加载已回答的问题
      res.data.data.filter(qa => qa.answer).forEach(qa => {
        historyMessages.push({ type: 'ai', content: qa.question, round: qa.round })
        historyMessages.push({ type: 'user', content: qa.answer })
        if (qa.feedback) {
          let scores = {}
          try { scores = JSON.parse(qa.scores) } catch { /* ignore */ }
          historyMessages.push({
            type: 'evaluation',
            score: scores,
            feedback: qa.feedback,
            referenceAnswer: qa.referenceAnswer || scores.referenceAnswer || '',
            showRef: false,
            round: qa.round
          })
        }
        currentRound.value = qa.round
        currentQuestion.value = qa.question
      })
      if (historyMessages.length > 0) {
        messages.value = historyMessages
        saveMessages()
        scrollToBottom()
        return true
      }
    }
  } catch { /* ignore */ }
  return false
}

// 从sessionStorage获取问题列表
const loadQuestions = () => {
  try {
    const cached = sessionStorage.getItem(`interview_questions_${interviewId}`)
    if (cached) {
      questions.value = JSON.parse(cached)
      return true
    }
  } catch { /* ignore */ }
  return false
}

const typingTexts = [
  '面试官正在思考',
  '面试官正在组织问题',
  '面试官正在分析你的回答',
  '面试官正在准备下一题'
]

const updateTime = () => {
  const now = new Date()
  currentTime.value = now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' })
}

onMounted(async () => {
  updateTime()
  timeInterval = setInterval(updateTime, 60000)

  // 从sessionStorage获取问题列表
  if (!loadQuestions()) {
    ElMessage.error('问题加载失败，请重新开始面试')
    return
  }

  // 先尝试从缓存恢复，没有则从服务器加载
  if (!loadCachedMessages()) {
    await loadHistoryFromServer()
  }
  connectWebSocket()
})

onUnmounted(() => {
  ws?.close()
  if (typingInterval) clearInterval(typingInterval)
  if (timeInterval) clearInterval(timeInterval)
  // 清理 TTS
  if (ttsAudio) {
    ttsAudio.pause()
    ttsAudio = null
  }
})

const connectWebSocket = () => {
  const wsProtocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsHost = import.meta.env.VITE_WS_HOST || location.host
  ws = new WebSocket(`${wsProtocol}//${wsHost}/ws/interview?id=${interviewId}`)

  ws.onopen = () => {
    // 没有历史记录时，显示第1题
    if (questions.value.length > 0 && messages.value.length === 0) {
      startTypingAnimation('面试官正在准备面试...')
      setTimeout(() => {
        stopTypingAnimation()
        const firstQuestion = questions.value[0]
        messages.value.push({ type: 'ai', content: firstQuestion })
        currentRound.value = 1
        currentQuestion.value = firstQuestion
        saveMessages()
        scrollToBottom()
      }, 1000)
    }
  }

  ws.onmessage = (event) => {
    handleWebSocketMessage(JSON.parse(event.data))
  }

  ws.onerror = () => {
    stopTypingAnimation()
    ElMessage.error('连接失败，请刷新重试')
  }

  ws.onclose = () => stopTypingAnimation()
}

const startTypingAnimation = (text) => {
  isAiTyping.value = true
  typingText.value = text || typingTexts[0]
  let index = 0
  typingInterval = setInterval(() => {
    index = (index + 1) % typingTexts.length
    typingText.value = typingTexts[index]
  }, 2500)
}

const stopTypingAnimation = () => {
  isAiTyping.value = false
  if (typingInterval) {
    clearInterval(typingInterval)
    typingInterval = null
  }
}

const handleWebSocketMessage = (data) => {
  stopTypingAnimation()

  if (data.type === 'evaluation') {
    messages.value.push({
      type: 'evaluation',
      score: data.score || null,
      feedback: data.feedback || '',
      referenceAnswer: data.referenceAnswer || '',
      showRef: false
    })
    saveMessages()
    scrollToBottom()

    // 评分完成后，自动推进到下一题
    const nextRound = currentRound.value + 1
    if (nextRound <= questions.value.length) {
      startTypingAnimation('正在准备下一题...')
      setTimeout(() => {
        stopTypingAnimation()
        const nextQuestion = questions.value[nextRound - 1]
        messages.value.push({ type: 'ai', content: nextQuestion })
        currentRound.value = nextRound
        currentQuestion.value = nextQuestion
        saveMessages()
        scrollToBottom()
      }, 1000)
    }
  } else if (data.type === 'completed') {
    messages.value.push({
      type: 'ai',
      content: '面试结束，正在生成评估报告...'
    })
    saveMessages()
    isCompleted.value = true
    sessionStorage.removeItem(storageKey)
    ElMessage.success('面试已完成')
    setTimeout(() => router.push(`/report/${interviewId}`), 2000)
  } else if (data.type === 'error') {
    ElMessage.error(data.message || '发生错误')
  }
}

const submitAnswer = () => {
  if (!answer.value.trim() || isAiTyping.value) return

  messages.value.push({ type: 'user', content: answer.value })
  saveMessages()

  ws.send(JSON.stringify({
    type: 'answer',
    interviewId,
    round: currentRound.value,
    content: answer.value,
    question: currentQuestion.value
  }))

  answer.value = ''
  scrollToBottom()
  startTypingAnimation('面试官正在分析...')
}

const exitInterview = async () => {
  try {
    await ElMessageBox.confirm(
      '提前结束面试将基于已有对话生成评估报告，确定要结束吗？',
      '结束面试',
      { confirmButtonText: '确定结束', cancelButtonText: '继续面试', type: 'warning' }
    )
  } catch {
    return // 用户取消
  }

  ws.send(JSON.stringify({ type: 'exit', interviewId }))
  startTypingAnimation('正在生成评估报告...')
}

const scrollToBottom = () => {
  nextTick(() => {
    chatContainer.value?.scrollTo({
      top: chatContainer.value.scrollHeight,
      behavior: 'smooth'
    })
  })
}

const scoreItems = (score) => [
  { key: 'accuracy', label: '准确性', value: score.accuracy || 0 },
  { key: 'clarity', label: '清晰度', value: score.clarity || 0 },
  { key: 'logic', label: '逻辑性', value: score.logic || 0 },
  { key: 'depth', label: '深度', value: score.depth || 0 },
  { key: 'practice', label: '实践', value: score.practice || 0 }
]

const getScoreColor = (val) => {
  if (val >= 8) return '#10b981'
  if (val >= 6) return '#06b6d4'
  if (val >= 4) return '#f59e0b'
  return '#ef4444'
}

// ==================== TTS 语音朗读 ====================

/** 判断是否为当前轮次的问题 */
const isCurrentQuestion = (msg) => {
  return msg.type === 'ai' && msg.content === currentQuestion.value
}

// ==================== STT 语音输入 ====================

/** 切换录音状态：点击开始 → 再点停止 → 识别 → 填入输入框 */
const toggleVoiceInput = async () => {
  if (isRecording.value) {
    // 停止录音 → 识别 → 填入输入框
    isRecording.value = false
    sttLoading.value = true
    try {
      const wavBlob = await recorderInstance.stop()
      const res = await recognizeSpeech(wavBlob)
      answer.value = res.data  // 识别文字填入输入框，用户确认后手动发送
    } catch (err) {
      ElMessage.error('语音识别失败: ' + (err.response?.data?.message || err.message))
    } finally {
      sttLoading.value = false
    }
  } else {
    // 开始录音
    try {
      recorderInstance = await startRecording()
      isRecording.value = true
    } catch (err) {
      ElMessage.error('无法访问麦克风: ' + err.message)
    }
  }
}

/** 播放/停止 TTS — 使用 MediaSource 实现流式播放 */
const playTTS = (text) => {
  if (ttsPlaying.value) {
    stopTTS()
    return
  }

  // 同步创建 MediaSource + Audio，在用户手势上下文中 play()
  // MediaSource 可以在没有数据的情况下播放，后续异步喂数据即可
  const mediaSource = new MediaSource()
  const url = URL.createObjectURL(mediaSource)
  ttsAudio = new Audio(url)
  ttsAudio.onended = () => { ttsPlaying.value = false; cleanup() }

  let sourceBuffer = null

  const cleanup = () => {
    if (ttsAudio) { ttsAudio = null }
    URL.revokeObjectURL(url)
  }

  mediaSource.addEventListener('sourceopen', () => {
    sourceBuffer = mediaSource.addSourceBuffer('audio/mpeg')
    sourceBuffer.mode = 'sequence'

    // MediaSource 就绪后，异步获取音频数据并喂入
    ttsLoading.value = true
    fetch('/api/interview/tts', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text })
    })
      .then(res => res.arrayBuffer())
      .then(data => {
        // 等上一段 append 完成后再追加
        const appendData = () => {
          if (sourceBuffer.updating) {
            sourceBuffer.addEventListener('updateend', appendData, { once: true })
            return
          }
          sourceBuffer.appendBuffer(data)
        }
        appendData()
      })
      .catch(err => {
        ElMessage.error('语音合成失败: ' + err.message)
        stopTTS()
      })
      .finally(() => { ttsLoading.value = false })
  })

  // 同步 play()，占住用户手势
  ttsAudio.play()
  ttsPlaying.value = true
}

/** 停止播放 */
const stopTTS = () => {
  if (ttsAudio) {
    ttsAudio.pause()
    ttsAudio = null
  }
  ttsPlaying.value = false
}

</script>

<style scoped>
.interview-room {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #f0f2f5;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
}

/* ---- Header ---- */
.interview-header {
  background: #fff;
  border-bottom: 1px solid #e5e7eb;
  flex-shrink: 0;
}

.interview-header__inner {
  max-width: 800px;
  margin: 0 auto;
  padding: 12px 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.interview-header__left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.interview-header__logo {
  width: 32px;
  height: 32px;
  background: linear-gradient(135deg, #10b981, #059669);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
}

.interview-header__title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2937;
  margin: 0;
}

.interview-header__right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.interview-header__round {
  font-size: 13px;
  color: #6b7280;
}

.interview-header__bar {
  width: 100px;
  height: 4px;
  background: #e5e7eb;
  border-radius: 2px;
  overflow: hidden;
}

.interview-header__bar-fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981, #059669);
  border-radius: 2px;
  transition: width 0.4s ease;
}

/* ---- Main ---- */
.interview-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  max-width: 800px;
  width: 100%;
  margin: 0 auto;
  padding: 0 16px;
  overflow: hidden;
}

/* ---- Chat Container ---- */
.chat-container {
  flex: 1;
  overflow-y: auto;
  padding: 20px 0;
  scroll-behavior: smooth;
}

/* ---- Time ---- */
.chat-time {
  text-align: center;
  font-size: 12px;
  color: #9ca3af;
  margin: 16px 0;
}

/* ---- Message ---- */
.msg {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
  padding: 0 4px;
  animation: msg-in 0.3s ease both;
}

.msg--user {
  flex-direction: row-reverse;
}

.msg--system {
  justify-content: center;
}

.msg__avatar {
  flex-shrink: 0;
  padding-top: 2px;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.avatar--ai {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
}

.avatar--user {
  background: #d1fae5;
  color: #059669;
}

.msg__body {
  max-width: 70%;
}

.msg__body--right {
  text-align: right;
}

.msg__name {
  font-size: 12px;
  color: #9ca3af;
  margin-bottom: 4px;
}

.msg__name--right {
  text-align: right;
}

.msg__bubble {
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.msg__bubble--ai {
  background: #fff;
  color: #1f2937;
  border: 1px solid #f3f4f6;
  border-top-left-radius: 4px;
  box-shadow: 0 1px 2px rgba(0,0,0,0.04);
}

.msg__bubble--user {
  background: #10b981;
  color: #fff;
  border-top-right-radius: 4px;
}

.msg__bubble--typing {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #9ca3af;
}

.msg__system-text {
  font-size: 12px;
  color: #9ca3af;
  background: #f3f4f6;
  padding: 4px 12px;
  border-radius: 10px;
}

/* ---- Score Card ---- */
.msg__score-card {
  margin-top: 8px;
}

.score-card {
  background: #fafafa;
  border: 1px solid #f3f4f6;
  border-radius: 10px;
  padding: 12px;
}

.score-card__row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.score-card__row:last-of-type {
  margin-bottom: 0;
}

.score-card__label {
  font-size: 12px;
  color: #6b7280;
  width: 40px;
  flex-shrink: 0;
}

.score-card__bar-wrap {
  flex: 1;
  height: 6px;
  background: #e5e7eb;
  border-radius: 3px;
  overflow: hidden;
}

.score-card__bar {
  height: 100%;
  border-radius: 3px;
  transition: width 0.6s ease;
}

.score-card__value {
  font-size: 13px;
  font-weight: 600;
  width: 20px;
  text-align: right;
}

.score-card__feedback {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f3f4f6;
  font-size: 13px;
  color: #4b5563;
  line-height: 1.5;
  display: flex;
  gap: 6px;
}

.score-card__feedback-icon {
  flex-shrink: 0;
}

/* ---- Reference Answer ---- */
.score-card__ref {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid #f3f4f6;
}

.score-card__ref-toggle {
  display: flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: none;
  color: #10b981;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  padding: 0;
}

.score-card__ref-toggle:hover {
  color: #059669;
}

.score-card__ref-toggle svg {
  transition: transform 0.2s;
}

.score-card__ref-toggle svg.rotated {
  transform: rotate(180deg);
}

.score-card__ref-text {
  margin-top: 8px;
  padding: 10px 12px;
  background: #ecfdf5;
  border-radius: 8px;
  font-size: 13px;
  line-height: 1.6;
  color: #065f46;
}

/* ---- Typing dots ---- */
.typing-dots {
  display: inline-flex;
  gap: 3px;
}

.typing-dots i {
  width: 5px;
  height: 5px;
  background: #9ca3af;
  border-radius: 50%;
  animation: dot-pulse 1.4s infinite ease-in-out both;
}

.typing-dots i:nth-child(1) { animation-delay: -0.32s; }
.typing-dots i:nth-child(2) { animation-delay: -0.16s; }

@keyframes dot-pulse {
  0%, 80%, 100% { transform: scale(0.4); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}

/* ---- Input ---- */
.chat-input {
  padding: 12px 0 16px;
  flex-shrink: 0;
}

.chat-input__top {
  display: flex;
  justify-content: flex-end;
  margin-bottom: 8px;
}

.chat-exit-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  background: transparent;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  color: #6b7280;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.chat-exit-btn:hover:not(:disabled) {
  border-color: #ef4444;
  color: #ef4444;
  background: #fef2f2;
}

.chat-exit-btn:disabled {
  opacity: 0.4;
  cursor: not-allowed;
}

.chat-input__wrap {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  background: #fff;
  border: 1px solid #e5e7eb;
  border-radius: 12px;
  padding: 8px 8px 8px 14px;
  transition: border-color 0.2s;
}

.chat-input__wrap:focus-within {
  border-color: #10b981;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.1);
}

.chat-input :deep(.el-textarea__inner) {
  border: none !important;
  box-shadow: none !important;
  padding: 0;
  background: transparent;
  font-size: 14px;
  line-height: 1.5;
}

/* ---- 语音输入按钮 ---- */
.chat-input__voice {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: #f3f4f6;
  color: #6b7280;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
  flex-shrink: 0;
}

.chat-input__voice:hover:not(:disabled) {
  background: #e5e7eb;
  color: #10b981;
}

.chat-input__voice:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.chat-input__voice.recording {
  background: #fef2f2;
  color: #ef4444;
  animation: pulse-red 1s infinite;
}

.chat-input__voice.loading {
  background: #fffbeb;
  color: #f59e0b;
}

.chat-input__voice .spin {
  animation: spin 1s linear infinite;
}

@keyframes pulse-red {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.chat-input__send {
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 8px;
  background: #e5e7eb;
  color: #9ca3af;
  cursor: not-allowed;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: all 0.2s;
}

.chat-input__send--active {
  background: #10b981;
  color: #fff;
  cursor: pointer;
}

.chat-input__send--active:hover {
  background: #059669;
  transform: scale(1.05);
}

/* ---- Animations ---- */
@keyframes msg-in {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

.slide-enter-active { transition: all 0.25s ease; }
.slide-leave-active { transition: all 0.2s ease; }
.slide-enter-from, .slide-leave-to { opacity: 0; max-height: 0; }
.slide-enter-to, .slide-leave-from { opacity: 1; max-height: 200px; }

/* ---- TTS Button ---- */
.msg__tts {
  margin-top: 8px;
}

.tts-btn {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  background: none;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 4px 12px;
  cursor: pointer;
  color: #64748b;
  font-size: 12px;
  transition: all 0.2s;
}

.tts-btn:hover:not(:disabled) {
  color: #10b981;
  border-color: #10b981;
}

.tts-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.tts-btn.playing {
  color: #10b981;
  border-color: #10b981;
  animation: tts-pulse 1s infinite;
}

.tts-btn.loading {
  color: #f59e0b;
  border-color: #f59e0b;
}

.tts-btn__icon--spin {
  animation: tts-spin 1s linear infinite;
}

.tts-btn__text {
  font-size: 12px;
}

@keyframes tts-pulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.5; }
}

@keyframes tts-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ---- Responsive ---- */
@media (max-width: 640px) {
  .interview-header__inner {
    padding: 10px 16px;
  }

  .interview-main {
    padding: 0 8px;
  }

  .msg__body {
    max-width: 80%;
  }

  .interview-header__bar {
    width: 60px;
  }
}
</style>
