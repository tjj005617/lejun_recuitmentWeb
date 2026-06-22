<template>
  <div class="immersive">
    <!-- 顶部进度栏 -->
    <header class="immersive__header">
      <button class="back-btn" @click="handleExit" title="结束面试">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
          <path d="M19 12H5M12 19l-7-7 7-7"/>
        </svg>
      </button>
      <div class="progress-info">
        <span class="progress-text">第 {{ currentRound }} / {{ totalRounds }} 轮</span>
        <div class="progress-bar">
          <div class="progress-bar__fill" :style="{ width: progressPercent + '%' }" />
        </div>
      </div>
    </header>

    <!-- 主体区域：左侧历史 + 中央内容 -->
    <div class="immersive__body">
      <!-- 左侧历史面板（始终显示） -->
      <aside class="history-panel">
        <div class="history-panel__title">历史对话</div>
        <div class="history-panel__list" ref="historyListRef">
          <template v-for="roundData in groupedMessages" :key="roundData.round">
            <div class="history-round">
              <div class="history-round__header">第 {{ roundData.round }} 轮</div>
              <!-- 问题 -->
              <div class="history-round__question">
                <div class="history-round__label">问题</div>
                <div class="history-round__text">{{ roundData.question }}</div>
              </div>
              <!-- 回答 -->
              <div class="history-round__answer" v-if="roundData.answer">
                <div class="history-round__label">回答</div>
                <div class="history-round__text">{{ roundData.answer }}</div>
              </div>
              <!-- 评分 -->
              <div class="history-round__score" v-if="roundData.evaluation">
                <div class="history-round__score-header">
                  <span class="history-round__score-value">{{ roundData.evaluation.score?.totalScore?.toFixed(1) || '-' }} 分</span>
                </div>
                <div class="history-round__feedback">{{ roundData.evaluation.feedback }}</div>
                <div class="history-round__ref" v-if="roundData.evaluation.referenceAnswer">
                  <div class="history-round__ref-label">参考答案</div>
                  <div class="history-round__ref-text">{{ roundData.evaluation.referenceAnswer }}</div>
                </div>
              </div>
            </div>
          </template>
          <div class="history-panel__empty" v-if="messages.length === 0">
            暂无历史对话
          </div>
        </div>
      </aside>

    <!-- 中央区域 -->
    <main class="immersive__main">
      <!-- AI 头像 + 状态 -->
      <div class="ai-section">
        <div class="ai-avatar" :class="{ 'ai-avatar--active': phase === 'tts_playing' || phase === 'recording' }">
          <img src="@/assets/image/interviewerPortrait.webp" alt="AI面试官" class="ai-avatar__img" />
          <div class="ai-avatar__ring" v-if="phase === 'tts_playing'"></div>
        </div>
        <div class="ai-status">
          <template v-if="phase === 'connecting'">
            <span class="status-dot status-dot--loading"></span>
            <span>正在连接...</span>
          </template>
          <template v-else-if="phase === 'tts_playing'">
            <span class="status-dot status-dot--speaking"></span>
            <span>面试官正在提问...</span>
          </template>
          <template v-else-if="phase === 'waiting_answer'">
            <span class="status-dot status-dot--speaking"></span>
            <span>面试官等待你回答...</span>
          </template>
          <template v-else-if="phase === 'recording'">
            <span class="status-dot status-dot--recording"></span>
            <span>请回答问题</span>
            <span class="recording-hint">（说完请说"回答完毕"）</span>
          </template>
          <template v-else-if="phase === 'processing'">
            <span class="status-dot status-dot--loading"></span>
            <span>正在识别语音...</span>
          </template>
          <template v-else-if="phase === 'scoring'">
            <span class="status-dot status-dot--loading"></span>
            <span>正在评分...</span>
          </template>
          <template v-else-if="phase === 'showing_score'">
            <span class="status-dot status-dot--done"></span>
            <span>评分完成</span>
          </template>
          <template v-else-if="phase === 'completed'">
            <span class="status-dot status-dot--done"></span>
            <span>面试已完成</span>
          </template>
          <template v-else-if="phase === 'error'">
            <span class="status-dot status-dot--error"></span>
            <span>出错了，请重试</span>
          </template>
          <template v-else>
            <span class="status-dot"></span>
            <span>准备就绪</span>
          </template>
        </div>
      </div>

      <!-- 当前问题文本 -->
      <div class="question-display" v-if="currentQuestion">
        <div class="question-label">当前问题</div>
        <div class="question-text">{{ currentQuestion }}</div>
      </div>

      <!-- 面试者回答文本（MiMo ASR 实时字幕） -->
      <div class="answer-display" v-if="currentAnswerText">
        <div class="answer-label">{{ phase === 'recording' || phase === 'processing' ? '正在识别...' : '你的回答' }}</div>
        <div class="answer-text" :class="{ 'answer-text--interim': phase === 'recording' }">
          {{ currentAnswerText }}
        </div>
      </div>

      <!-- 音频波形 -->
      <div class="waveform-container" v-show="phase === 'recording'">
        <canvas ref="canvasRef" class="waveform-canvas" width="320" height="80"></canvas>
        <div class="recording-time">{{ recordingTimeText }}</div>
      </div>

      <!-- 评分卡片 -->
      <Transition name="score-card">
        <div class="score-card" v-if="phase === 'showing_score' && latestScore">
          <div class="score-card__title">本轮评分</div>
          <div class="score-card__items">
            <div class="score-item" v-for="item in scoreItems" :key="item.key">
              <span class="score-item__label">{{ item.label }}</span>
              <div class="score-item__bar-wrap">
                <div class="score-item__bar" :style="{ width: item.value * 10 + '%', background: getScoreColor(item.value) }" />
              </div>
              <span class="score-item__value" :style="{ color: getScoreColor(item.value) }">{{ item.value }}</span>
            </div>
          </div>
          <div class="score-card__feedback" v-if="latestScore.feedback">
            <span class="feedback-icon">💡</span>
            {{ latestScore.feedback }}
          </div>
          <div class="score-card__ref" v-if="latestScore.referenceAnswer">
            <button class="ref-toggle" @click="showRef = !showRef">
              {{ showRef ? '收起参考答案' : '查看参考答案' }}
            </button>
            <div class="ref-text" v-if="showRef">{{ latestScore.referenceAnswer }}</div>
          </div>
        </div>
      </Transition>

      <!-- 开始前的提示 -->
      <div class="start-hint" v-if="phase === 'idle'">
        <p>点击下方按钮开始沉浸式面试</p>
        <p class="start-hint__sub">AI 将自动朗读问题，您通过语音回答</p>
      </div>
    </main>
    </div>

    <!-- 底部控制栏 -->
    <footer class="immersive__footer">
      <template v-if="phase === 'idle' || phase === 'error'">
        <button class="ctrl-btn ctrl-btn--start" @click="startInterview" :disabled="phase === 'connecting'">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="22" height="22">
            <polygon points="5 3 19 12 5 21 5 3"/>
          </svg>
          <span>{{ phase === 'connecting' ? '连接中...' : '开始面试' }}</span>
        </button>
      </template>
      <template v-else-if="phase === 'completed'">
        <button class="ctrl-btn ctrl-btn--view" @click="viewReport">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="22" height="22">
            <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"/>
            <polyline points="14 2 14 8 20 8"/>
            <line x1="16" y1="13" x2="8" y2="13"/>
            <line x1="16" y1="17" x2="8" y2="17"/>
          </svg>
          <span>查看报告</span>
        </button>
      </template>
      <template v-else>
        <div class="footer-row">
          <button v-if="phase === 'recording'" class="ctrl-btn ctrl-btn--stop" @click="stopAndProcessRecording">
            <svg viewBox="0 0 24 24" fill="currentColor" width="18" height="18">
              <rect x="6" y="6" width="12" height="12" rx="2"/>
            </svg>
            <span>回答完毕</span>
          </button>
          <button class="ctrl-btn ctrl-btn--hangup" @click="handleExit" title="结束面试">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="22" height="22">
              <path d="M10.68 13.31a16 16 0 0 0 3.41 2.6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7 2 2 0 0 1 1.72 2v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91"/>
              <line x1="23" y1="1" x2="1" y2="23"/>
            </svg>
          </button>
        </div>
      </template>
    </footer>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { recognizeSpeech } from '@/api/stt'
import { mergeChunksToWavBase64 } from '@/utils/audioChunkEncoder'
import request from '@/api/request'

const route = useRoute()
const router = useRouter()
const interviewId = route.params.id

// ==================== 状态机 ====================
const PHASE = {
  IDLE: 'idle',
  CONNECTING: 'connecting',
  TTS_PLAYING: 'tts_playing',
  WAITING_ANSWER: 'waiting_answer',
  RECORDING: 'recording',
  PROCESSING: 'processing',
  SCORING: 'scoring',
  SHOWING_SCORE: 'showing_score',
  COMPLETED: 'completed',
  ERROR: 'error'
}

const phase = ref(PHASE.IDLE)
const currentRound = ref(0)
const totalRounds = ref(10)
const currentQuestion = ref('')
const messages = ref([])
const latestScore = ref(null)
const showRef = ref(false)
// 按轮次分组的历史消息
const groupedMessages = computed(() => {
  const rounds = {}
  for (const msg of messages.value) {
    const r = msg.round
    if (!rounds[r]) rounds[r] = { round: r, question: '', answer: '', evaluation: null }
    if (msg.type === 'question') rounds[r].question = msg.content
    else if (msg.type === 'user') rounds[r].answer = msg.content
    else if (msg.type === 'evaluation') rounds[r].evaluation = msg
  }
  return Object.values(rounds).sort((a, b) => a.round - b.round)
})
const historyListRef = ref(null)
const recordingStartTime = ref(0)
const recordingTimeText = ref('00:00')
const currentAnswerText = ref('')

// ==================== WebSocket ====================
let ws = null
let wsReconnectCount = 0
const MAX_WS_RECONNECT = 3

const connectWebSocket = () => {
  phase.value = PHASE.CONNECTING
  const wsProtocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsHost = import.meta.env.VITE_WS_HOST || location.host
  ws = new WebSocket(`${wsProtocol}//${wsHost}/ws/interview?id=${interviewId}`)

  ws.onopen = () => {
    wsReconnectCount = 0
    ws.send(JSON.stringify({ type: 'immersive_start', interviewId: Number(interviewId) }))
  }

  ws.onmessage = (event) => {
    const data = JSON.parse(event.data)
    handleWebSocketMessage(data)
  }

  ws.onerror = () => {
    if (phase.value !== PHASE.COMPLETED) {
      phase.value = PHASE.ERROR
    }
  }

  ws.onclose = () => {
    if (phase.value !== PHASE.COMPLETED && phase.value !== PHASE.ERROR) {
      // 尝试重连
      if (wsReconnectCount < MAX_WS_RECONNECT) {
        wsReconnectCount++
        setTimeout(() => connectWebSocket(), 2000)
      } else {
        phase.value = PHASE.ERROR
      }
    }
  }
}

const wsSend = (data) => {
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify(data))
  }
}

const handleWebSocketMessage = (data) => {
  switch (data.type) {
    case 'next_question':
      handleNextQuestion(data)
      break
    case 'evaluation':
      handleEvaluation(data)
      break
    case 'completed':
      handleCompleted(data)
      break
    case 'error':
      ElMessage.error(data.message || '发生错误')
      phase.value = PHASE.ERROR
      break
  }
}

// ==================== 问题处理 ====================
const handleNextQuestion = (data) => {
  currentRound.value = data.round
  totalRounds.value = data.totalRounds
  currentQuestion.value = data.question
  latestScore.value = null
  showRef.value = false
  currentAnswerText.value = ''
  asrAccumulatedText = ''
  speechConfirmedText = ''  // 新问题重置累积文本
  pendingAnswer = ''        // 清除上一轮未确认的回答

  // 关闭上一轮的 ASR WebSocket（如有）
  if (asrWs) { asrWs.close(); asrWs = null }

  // 去重：如果该轮问题已存在（从历史加载），不重复添加
  const alreadyHasQuestion = messages.value.some(
    m => m.type === 'question' && m.round === data.round
  )
  if (!alreadyHasQuestion) {
    messages.value.push({ type: 'question', content: data.question, round: data.round })
  }
  saveMessages()

  // 自动播放 TTS
  phase.value = PHASE.TTS_PLAYING
  playTTSAuto(data.question)
}

// ==================== TTS 自动播放 ====================
let ttsAudio = null

const playTTSAuto = async (text) => {
  stopTTS()

  try {
    const res = await fetch('/api/interview/tts', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ text })
    })

    if (!res.ok) {
      throw new Error(`TTS 请求失败: ${res.status}`)
    }

    const blob = await res.blob()
    const url = URL.createObjectURL(blob)
    ttsAudio = new Audio(url)

    // 监听播放结束事件，切换到等待回答状态
    ttsAudio.onended = () => {
      console.log('[Immersive] TTS 播放完成')
      URL.revokeObjectURL(url)
      ttsAudio = null
      phase.value = PHASE.WAITING_ANSWER
      // 延迟300ms后开始自动录音，给用户反应时间
      setTimeout(() => startAutoRecording(), 300)
    }

    // 监听播放错误
    ttsAudio.onerror = (e) => {
      console.error('[Immersive] TTS 播放错误:', e)
      URL.revokeObjectURL(url)
      ttsAudio = null
      // 播放失败也切换到等待状态，让用户可以手动回答
      phase.value = PHASE.WAITING_ANSWER
      setTimeout(() => startAutoRecording(), 300)
    }

    // 开始播放
    await ttsAudio.play()
  } catch (err) {
    ElMessage.error('语音合成失败: ' + err.message)
    // TTS失败也切换到等待状态，让用户可以手动回答
    phase.value = PHASE.WAITING_ANSWER
    setTimeout(() => startAutoRecording(), 300)
  }
}

const stopTTS = () => {
  if (ttsAudio) {
    ttsAudio.pause()
    ttsAudio.onended = null
    ttsAudio.onerror = null
    ttsAudio = null
  }
}

// ==================== 麦克风 ====================
const micStream = ref(null)

const requestMicPermission = async () => {
  try {
    micStream.value = await navigator.mediaDevices.getUserMedia({
      audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true }
    })
    return true
  } catch (err) {
    if (err.name === 'NotAllowedError') {
      ElMessage.error('请允许麦克风权限以使用沉浸式面试')
    } else if (err.name === 'NotFoundError') {
      ElMessage.error('未检测到麦克风设备')
    } else {
      ElMessage.error('麦克风访问失败: ' + err.message)
    }
    return false
  }
}

// ==================== ASR WebSocket（MiMo 实时语音识别）====================
let asrWs = null
let asrAccumulatedText = '' // ASR 累积的识别文本
let pendingAnswer = ''      // 待确认的回答文本（后端确认后才写入 messages）

/**
 * 连接 ASR WebSocket
 * 每轮面试连接一次，发送 asr_init，处理 asr_evaluation
 */
const connectAsrWebSocket = () => {
  const wsProtocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsHost = import.meta.env.VITE_WS_HOST || location.host
  asrWs = new WebSocket(`${wsProtocol}//${wsHost}/ws/asr`)

  asrWs.onopen = () => {
    // 连接后立即发送 asr_init，告知后端面试 ID 和轮次
    asrWs.send(JSON.stringify({
      type: 'asr_init',
      interviewId: Number(interviewId),
      round: currentRound.value
    }))
    console.log('[ASR] WebSocket 已连接, asr_init sent, round:', currentRound.value)
  }

  asrWs.onmessage = (event) => {
    const data = JSON.parse(event.data)
    if (data.type === 'asr_result') {
      // 中间结果 → 直接用 ASR 返回的完整文本替换（每次返回的是累积全文）
      asrAccumulatedText = data.text || ''
      currentAnswerText.value = asrAccumulatedText
    } else if (data.type === 'asr_final') {
      // ASR 最终识别结果（纯文本，不含评分）
      asrAccumulatedText = data.text
      currentAnswerText.value = data.text
    } else if (data.type === 'asr_evaluation') {
      // 收到评分结果
      console.log('[ASR] 收到评分')
      handleAsrEvaluation(data)
    } else if (data.type === 'asr_error') {
      console.error('[ASR] 错误:', data.message)
      ElMessage.error('语音识别错误: ' + data.message)
    }
  }

  asrWs.onerror = (e) => {
    console.error('[ASR] WebSocket 错误:', e)
  }

  asrWs.onclose = (e) => {
    console.log('[ASR] WebSocket 连接关闭, code:', e.code, 'reason:', e.reason)
  }
}

/**
 * 处理后端推送的评分结果（asr_evaluation）
 */
const handleAsrEvaluation = (data) => {
  // 后端已确认保存回答，此时才写入 messages（确保与数据库一致）
  if (pendingAnswer) {
    messages.value.push({ type: 'user', content: pendingAnswer, round: data.round })
    pendingAnswer = ''
  }

  // 保存评分
  latestScore.value = {
    score: data.score,
    feedback: data.feedback,
    referenceAnswer: data.referenceAnswer
  }
  messages.value.push({
    type: 'evaluation',
    round: data.round,
    score: data.score,
    feedback: data.feedback,
    referenceAnswer: data.referenceAnswer
  })
  saveMessages()

  phase.value = PHASE.SHOWING_SCORE

  // 关闭本轮 ASR WebSocket
  if (asrWs) {
    asrWs.close()
    asrWs = null
  }

  const isLastRound = data.isLastRound || data.round >= totalRounds.value

  if (!isLastRound) {
    // 不是最后一轮 → 延迟 2s 后自动进入下一题（走 handleNextQuestion 统一入口）
    setTimeout(() => {
      const nextRound = data.round + 1
      const questions = JSON.parse(sessionStorage.getItem(`interview_questions_${interviewId}`) || '[]')
      const nextQuestion = questions[nextRound - 1]
      if (nextQuestion) {
        handleNextQuestion({
          round: nextRound,
          question: nextQuestion,
          totalRounds: totalRounds.value
        })
      }
    }, 2000)
  }
  // 最后一轮 → 停留在评分页面，等用户点"查看报告"
}

/**
 * 发送音频分片到 ASR WebSocket
 * @param {string} base64Wav - WAV 格式的 base64 字符串
 * @param {boolean} isFinal - 是否为最终音频
 */
const sendAudioChunk = (base64Wav, isFinal) => {
  if (!asrWs || asrWs.readyState !== WebSocket.OPEN) return
  asrWs.send(JSON.stringify({
    type: isFinal ? 'audio_final' : 'audio_chunk',
    audio: base64Wav
  }))
}

// ==================== 自动录音 + 分片识别 ====================
let mediaRecorder = null
let audioContext = null
let analyser = null
let audioChunks = []        // 所有 webm 分片（用于最终完整音频）
let silenceCheckId = null
let lastSpeechTime = Date.now()
let ambientNoiseLevel = 10
let noiseCalibrated = false
const NOISE_MULTIPLIER = 2.5
const MIN_THRESHOLD = 15
const SILENCE_DURATION = 8000  // 静音 8 秒才触发（给思考留时间，真正判断交给语义模型）

// ==================== Web Speech API 实时字幕 ====================
let speechRecognition = null
let speechConfirmedText = ''  // 已确认的文本（累积）

// ==================== 语义判断定时器 ====================
let semanticCheckId = null  // 周期性语义判断定时器
let semanticChecking = false  // 防止并发请求
const SEMANTIC_CHECK_INTERVAL = 3000  // 每 3 秒检查一次

const recordingTimer = ref(null)

const startAutoRecording = async () => {
  console.log('[Immersive] startAutoRecording, micStream:', !!micStream.value)
  if (!micStream.value) {
    const ok = await requestMicPermission()
    if (!ok) { phase.value = PHASE.ERROR; return }
  }

  phase.value = PHASE.RECORDING
  recordingStartTime.value = Date.now()
  // 不清空 currentAnswerText（保持 Web Speech API 的实时字幕）
  audioChunks = []
  noiseCalibrated = false
  lastSpeechTime = Date.now()
  startRecordingTimer()

  // 连接 ASR WebSocket（如果尚未连接）
  if (!asrWs || asrWs.readyState !== WebSocket.OPEN) {
    connectAsrWebSocket()
  }

  const stream = micStream.value

  // ===== MediaRecorder 分片录制（每 2 秒产生一个 chunk）=====
  mediaRecorder = new MediaRecorder(stream, { mimeType: 'audio/webm;codecs=opus' })

  mediaRecorder.ondataavailable = (e) => {
    if (e.data.size === 0) return
    audioChunks.push(e.data)
  }

  // timeslice=1500：每 1.5 秒触发一次 ondataavailable
  mediaRecorder.start(1500)

  // ===== AnalyserNode 用于波形可视化 + 静音检测 =====
  audioContext = new AudioContext()
  const source = audioContext.createMediaStreamSource(stream)
  analyser = audioContext.createAnalyser()
  analyser.fftSize = 512
  source.connect(analyser)

  drawWaveform()
  startSilenceDetection()

  // 启动 Web Speech API 实时字幕（打字机效果）
  startSpeechRecognition()

  // 启动周期性语义判断（每隔 3s 判断回答是否结束）
  startSemanticCheck()
}

/**
 * 启动 Web Speech API 实时字幕（打字机效果）
 */
const startSpeechRecognition = () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    console.warn('[Speech] 浏览器不支持 Web Speech API')
    return
  }

  speechRecognition = new SpeechRecognition()
  speechRecognition.lang = 'zh-CN'
  speechRecognition.continuous = true
  speechRecognition.interimResults = true

  speechConfirmedText = ''

  speechRecognition.onresult = (event) => {
    let interim = ''
    for (let i = event.resultIndex; i < event.results.length; i++) {
      const transcript = event.results[i][0].transcript
      if (event.results[i].isFinal) {
        speechConfirmedText += transcript
      } else {
        interim += transcript
      }
    }
    // 实时更新字幕（已确认 + 中间文本）
    currentAnswerText.value = speechConfirmedText + interim
  }

  speechRecognition.onerror = (event) => {
    console.warn('[Speech] 错误:', event.error)
  }

  speechRecognition.onend = () => {
    // 如果还在录音，自动重启（浏览器可能自动停）
    if (phase.value === PHASE.RECORDING && speechRecognition) {
      try { speechRecognition.start() } catch {}
    }
  }

  speechRecognition.start()
  console.log('[Speech] Web Speech API 启动')
}

/**
 * 停止 Web Speech API
 */
const stopSpeechRecognition = () => {
  if (speechRecognition) {
    speechRecognition.onend = null
    speechRecognition.stop()
    speechRecognition = null
  }
}

/**
 * 启动周期性语义判断：每隔几秒将 Web Speech API 累积文本发给后端，
 * 由语义模型判断回答是否结束，结束后自动停止录音走评分
 */
const startSemanticCheck = () => {
  stopSemanticCheck()  // 清除已有定时器
  semanticChecking = false

  semanticCheckId = setInterval(async () => {
    // 只在录音中、有文本、且没有并发请求时检查
    if (phase.value !== PHASE.RECORDING) return
    if (semanticChecking) return
    const text = currentAnswerText.value?.trim()
    if (!text || text.length < 5) return  // 太短不判断

    semanticChecking = true
    try {
      const res = await request.post('/interview/check-complete', { text })
      if (res.data?.complete) {
        console.log('[Semantic] 语义模型判断回答已结束，停止录音')
        stopAndProcessRecording()
      } else {
        console.log('[Semantic] 语义模型判断回答未结束，继续录音')
      }
    } catch (e) {
      console.warn('[Semantic] 语义判断请求失败，继续录音', e)
    } finally {
      semanticChecking = false
    }
  }, SEMANTIC_CHECK_INTERVAL)
}

/** 停止周期性语义判断 */
const stopSemanticCheck = () => {
  if (semanticCheckId) {
    clearInterval(semanticCheckId)
    semanticCheckId = null
  }
  semanticChecking = false
}

/**
 * AnalyserNode 静音检测（检测用户停止说话）
 */
const startSilenceDetection = () => {
  const dataArray = new Uint8Array(analyser.frequencyBinCount)
  const calibrateSamples = []
  const calibrateEndTime = Date.now() + 500
  noiseCalibrated = false

  const checkVolume = () => {
    if (phase.value !== PHASE.RECORDING) return

    analyser.getByteFrequencyData(dataArray)
    const avg = dataArray.reduce((a, b) => a + b, 0) / dataArray.length

    if (!noiseCalibrated) {
      calibrateSamples.push(avg)
      if (Date.now() >= calibrateEndTime) {
        ambientNoiseLevel = calibrateSamples.reduce((a, b) => a + b, 0) / calibrateSamples.length
        noiseCalibrated = true
      }
      lastSpeechTime = Date.now()
      silenceCheckId = requestAnimationFrame(checkVolume)
      return
    }

    const threshold = Math.max(ambientNoiseLevel * NOISE_MULTIPLIER, MIN_THRESHOLD)
    if (avg > threshold) {
      lastSpeechTime = Date.now()
    } else if (Date.now() - lastSpeechTime > SILENCE_DURATION) {
      console.log('[Immersive] 静音检测触发，停止录音')
      stopAndProcessRecording()
      return
    }
    silenceCheckId = requestAnimationFrame(checkVolume)
  }
  silenceCheckId = requestAnimationFrame(checkVolume)
}

/**
 * 停止录音并处理：发送完整音频到 ASR → 后端自动语义判断+评分
 * 不再发送 immersive_answer，后端编排全流程
 */
const stopAndProcessRecording = async () => {
  if (phase.value !== PHASE.RECORDING) return
  phase.value = PHASE.PROCESSING
  stopRecordingTimer()

  // 停止 Web Speech API 实时字幕
  stopSpeechRecognition()

  // 停止周期性语义判断
  stopSemanticCheck()

  // 停止静音检测
  if (silenceCheckId) {
    cancelAnimationFrame(silenceCheckId)
    silenceCheckId = null
  }

  // 关闭 AnalyserNode
  if (audioContext) {
    audioContext.close()
    audioContext = null
  }

  try {
    // 停止 MediaRecorder，获取最后一个 chunk
    await new Promise((resolve) => {
      mediaRecorder.onstop = resolve
      mediaRecorder.stop()
    })

    // 将所有分片合并为完整 webm → 转为 WAV base64
    const completeWavBase64 = await mergeChunksToWavBase64(audioChunks)

    // 发送完整音频到 ASR（audio_final）
    // 后端收到 asr_final 后会自动：评分 → 推送 asr_evaluation
    console.log('[ASR] 发送 audio_final, 音频大小:', (completeWavBase64.length / 1024).toFixed(1), 'KB')
    sendAudioChunk(completeWavBase64, true)

    // 暂存回答文本，等后端 asr_evaluation 确认后再写入 messages
    // 避免刷新时回答在数据库保存前就写入本地缓存，导致重新加载后问答错位
    pendingAnswer = asrAccumulatedText || currentAnswerText.value || ''

    // 等待后端推送 asr_evaluation（评分完成）
    // 不阻塞，由 ASR WebSocket onmessage 回调处理
  } catch (err) {
    ElMessage.error('语音识别失败: ' + err.message)
    phase.value = PHASE.ERROR
  }
}

// ==================== 录音计时 ====================
const startRecordingTimer = () => {
  stopRecordingTimer()
  recordingTimer.value = setInterval(() => {
    const elapsed = Math.floor((Date.now() - recordingStartTime.value) / 1000)
    const mins = String(Math.floor(elapsed / 60)).padStart(2, '0')
    const secs = String(elapsed % 60).padStart(2, '0')
    recordingTimeText.value = `${mins}:${secs}`
  }, 1000)
}

const stopRecordingTimer = () => {
  if (recordingTimer.value) {
    clearInterval(recordingTimer.value)
    recordingTimer.value = null
  }
  recordingTimeText.value = '00:00'
}

// ==================== 波形可视化 ====================
const canvasRef = ref(null)
let waveformAnimId = null

const drawWaveform = () => {
  if (phase.value !== PHASE.RECORDING || !analyser || !canvasRef.value) return

  const canvas = canvasRef.value
  const ctx = canvas.getContext('2d')
  const dataArray = new Uint8Array(analyser.frequencyBinCount)

  analyser.getByteTimeDomainData(dataArray)

  ctx.clearRect(0, 0, canvas.width, canvas.height)
  ctx.lineWidth = 2
  ctx.strokeStyle = '#10b981'
  ctx.beginPath()

  const sliceWidth = canvas.width / dataArray.length
  let x = 0

  for (let i = 0; i < dataArray.length; i++) {
    const v = dataArray[i] / 128.0
    const y = (v * canvas.height) / 2
    if (i === 0) ctx.moveTo(x, y)
    else ctx.lineTo(x, y)
    x += sliceWidth
  }

  ctx.lineTo(canvas.width, canvas.height / 2)
  ctx.stroke()

  waveformAnimId = requestAnimationFrame(drawWaveform)
}

// ==================== 评分处理 ====================
const handleEvaluation = (data) => {
  latestScore.value = {
    score: data.score,
    feedback: data.feedback,
    referenceAnswer: data.referenceAnswer
  }
  messages.value.push({
    type: 'evaluation',
    round: data.round,
    score: data.score,
    feedback: data.feedback,
    referenceAnswer: data.referenceAnswer
  })
  saveMessages()

  phase.value = PHASE.SHOWING_SCORE

  // 后端 AI 判断回答已结束（command=true）→ 自动进入下一题
  // AI 判断回答未结束（command=false）→ 显示评分后重新录音，让用户补充
  const isLastRound = data.round >= totalRounds.value

  if (data.command && !isLastRound) {
    // 回答已结束，不是最后一轮 → 延迟 2s 后自动播放下一题
    setTimeout(() => {
      const nextRound = data.round + 1
      const questions = JSON.parse(sessionStorage.getItem(`interview_questions_${interviewId}`) || '[]')
      const nextQuestion = questions[nextRound - 1]
      if (nextQuestion) {
        currentRound.value = nextRound
        currentQuestion.value = nextQuestion
        phase.value = PHASE.TTS_PLAYING
        playTTSAuto(nextQuestion)
      }
    }, 2000)
  } else if (!data.command && !isLastRound) {
    // 回答未结束 → 显示评分后重新录音，让用户继续补充
    ElMessage.info('回答似乎不完整，请继续补充')
    setTimeout(() => {
      currentAnswerText.value = ''
      phase.value = PHASE.WAITING_ANSWER
      setTimeout(() => startAutoRecording(), 300)
    }, 2000)
  }
  // 最后一轮 → 停留在评分页面，等用户点"查看报告"
}

const handleCompleted = (data) => {
  phase.value = PHASE.COMPLETED
  if (data.report) {
    sessionStorage.setItem(`interview_report_${interviewId}`, data.report)
  }
}

// ==================== 评分展示 ====================
const scoreItems = computed(() => {
  if (!latestScore.value?.score) return []
  const s = latestScore.value.score
  return [
    { key: 'accuracy', label: '准确性', value: s.accuracy || 0 },
    { key: 'clarity', label: '清晰度', value: s.clarity || 0 },
    { key: 'logic', label: '逻辑性', value: s.logic || 0 },
    { key: 'depth', label: '深度', value: s.depth || 0 },
    { key: 'practice', label: '实践性', value: s.practice || 0 }
  ]
})

const getScoreColor = (val) => {
  if (val >= 8) return '#10b981'
  if (val >= 6) return '#f59e0b'
  return '#ef4444'
}

const progressPercent = computed(() => {
  if (totalRounds.value === 0) return 0
  return Math.round((currentRound.value / totalRounds.value) * 100)
})

// ==================== 退出 ====================
const handleExit = async () => {
  try {
    await ElMessageBox.confirm('确定要结束面试吗？', '结束面试', {
      confirmButtonText: '确定结束',
      cancelButtonText: '继续面试',
      type: 'warning'
    })
  } catch {
    return // 用户点了继续
  }
  cleanup()
  // 发送退出
  if (ws && ws.readyState === WebSocket.OPEN) {
    ws.send(JSON.stringify({ type: 'immersive_exit', interviewId: Number(interviewId) }))
  }
  // 跳转报告页
  router.push(`/report/${interviewId}`)
}

const viewReport = () => {
  router.push(`/report/${interviewId}`)
}

// ==================== 开始面试 ====================
const startInterview = async () => {
  const ok = await requestMicPermission()
  if (!ok) return
  connectWebSocket()
}

// ==================== 持久化 ====================
const storageKey = `interview_immersive_${interviewId}`

const saveMessages = () => {
  try {
    sessionStorage.setItem(storageKey, JSON.stringify(messages.value))
  } catch { /* ignore */ }
}

// 新消息自动滚动历史面板到底部
watch(() => messages.value.length, () => {
  nextTick(() => {
    if (historyListRef.value) {
      historyListRef.value.scrollTop = historyListRef.value.scrollHeight
    }
  })
})

const loadCachedMessages = () => {
  try {
    const cached = sessionStorage.getItem(storageKey)
    if (cached) {
      const parsed = JSON.parse(cached)
      if (parsed.length > 0) {
        messages.value = parsed
        // 恢复状态
        for (let i = parsed.length - 1; i >= 0; i--) {
          if (parsed[i].type === 'question') {
            currentQuestion.value = parsed[i].content
            currentRound.value = parsed[i].round
          }
        }
        return true
      }
    }
  } catch { /* ignore */ }
  return false
}

/** 从服务器加载历史记录（sessionStorage 失败时的兜底） */
const loadHistoryFromServer = async () => {
  try {
    const res = await request.get(`/api/interview/${interviewId}/history`)
    if (res.data.success && res.data.data.length > 0) {
      const historyMessages = []
      // 只加载已回答的问题
      res.data.data.filter(qa => qa.answer).forEach(qa => {
        historyMessages.push({ type: 'question', content: qa.question, round: qa.round })
        historyMessages.push({ type: 'user', content: qa.answer, round: qa.round })
        if (qa.feedback) {
          let scores = {}
          try { scores = JSON.parse(qa.scores) } catch { /* ignore */ }
          historyMessages.push({
            type: 'evaluation',
            score: scores,
            feedback: qa.feedback,
            referenceAnswer: qa.referenceAnswer || scores.referenceAnswer || '',
            round: qa.round
          })
        }
        currentRound.value = qa.round
        currentQuestion.value = qa.question
      })
      if (historyMessages.length > 0) {
        messages.value = historyMessages
        saveMessages()
        return true
      }
    }
  } catch { /* ignore */ }
  return false
}

// ==================== 清理 ====================
const cleanup = () => {
  try { stopTTS() } catch {}
  try { stopRecordingTimer() } catch {}
  try { stopSpeechRecognition() } catch {}
  try { stopSemanticCheck() } catch {}

  // 关闭 ASR WebSocket
  try {
    if (asrWs) {
      asrWs.close()
      asrWs = null
    }
  } catch {}

  try {
    if (mediaRecorder && mediaRecorder.state === 'recording') {
      mediaRecorder.stop()
    }
  } catch {}

  // 强制释放麦克风（最关键）
  try {
    if (micStream.value) {
      micStream.value.getTracks().forEach(t => t.stop())
      micStream.value = null
    }
  } catch (e) {
    console.error('[Cleanup] 释放麦克风失败:', e)
  }

  try {
    if (audioContext) {
      audioContext.close()
      audioContext = null
    }
  } catch {}

  try {
    if (silenceCheckId) {
      cancelAnimationFrame(silenceCheckId)
      silenceCheckId = null
    }
  } catch {}

  if (waveformAnimId) {
    cancelAnimationFrame(waveformAnimId)
    waveformAnimId = null
  }
}

// ==================== 生命周期 ====================
onMounted(async () => {
  if (!loadCachedMessages()) {
    await loadHistoryFromServer()
  }
})

onUnmounted(() => {
  cleanup()
  if (ws) {
    ws.close()
    ws = null
  }
  window.removeEventListener('beforeunload', handleBeforeUnload)
})

// 路由离开时强制释放麦克风（onUnmounted 在路由切换时不可靠）
onBeforeRouteLeave(() => {
  cleanup()
  if (ws) {
    ws.close()
    ws = null
  }
})

// 关闭/刷新页面时释放麦克风
const handleBeforeUnload = () => {
  cleanup()
}
window.addEventListener('beforeunload', handleBeforeUnload)
</script>

<style scoped>
.immersive {
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: #0c0c0c;
  color: #fff;
  font-family: -apple-system, BlinkMacSystemFont, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  overflow: hidden;
}

/* Header */
.immersive__header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  background: rgba(255, 255, 255, 0.05);
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.back-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: rgba(255, 255, 255, 0.08);
  color: #a3a3a3;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s;
}

.back-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: #fff;
}

.progress-info {
  flex: 1;
}

.progress-text {
  font-size: 13px;
  color: #a3a3a3;
  font-weight: 500;
}

.progress-bar {
  margin-top: 6px;
  height: 4px;
  background: rgba(255, 255, 255, 0.08);
  border-radius: 2px;
  overflow: hidden;
}

.progress-bar__fill {
  height: 100%;
  background: linear-gradient(90deg, #10b981, #059669);
  border-radius: 2px;
  transition: width 0.5s ease;
}

/* Main */
.immersive__body {
  flex: 1;
  display: flex;
  overflow: hidden;
  position: relative;
}

.immersive__main {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  padding-bottom: 100px;
  gap: 32px;
  overflow-y: auto;
}

/* History Panel */
.history-panel {
  width: 360px;
  min-width: 360px;
  background: rgba(255, 255, 255, 0.03);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.history-panel__title {
  padding: 16px 20px;
  font-size: 15px;
  font-weight: 600;
  color: #e5e5e5;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.history-panel__list {
  flex: 1;
  overflow-y: auto;
  padding: 12px 16px;
}

.history-panel__empty {
  text-align: center;
  color: #666;
  padding: 40px 0;
  font-size: 14px;
}

/* 每轮对话卡片 */
.history-round {
  margin-bottom: 16px;
  padding: 14px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.04);
  border-left: 3px solid #10b981;
}

.history-round__header {
  font-size: 13px;
  color: #10b981;
  font-weight: 600;
  margin-bottom: 10px;
}

.history-round__question,
.history-round__answer {
  margin-bottom: 8px;
}

.history-round__label {
  font-size: 11px;
  color: #6b7280;
  text-transform: uppercase;
  letter-spacing: 0.5px;
  margin-bottom: 3px;
}

.history-round__text {
  font-size: 13px;
  color: #d4d4d4;
  line-height: 1.5;
  word-break: break-word;
}

.history-round__answer .history-round__text {
  color: #93c5fd;
}

.history-round__score {
  margin-top: 8px;
  padding-top: 8px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
}

.history-round__score-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 4px;
}

.history-round__score-value {
  font-size: 14px;
  font-weight: 600;
  color: #fbbf24;
}

.history-round__feedback {
  font-size: 12px;
  color: #9ca3af;
  line-height: 1.5;
  margin-bottom: 6px;
}

.history-round__ref {
  margin-top: 6px;
  padding: 8px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.04);
}

.history-round__ref-label {
  font-size: 11px;
  color: #6b7280;
  margin-bottom: 3px;
}

.history-round__ref-text {
  font-size: 12px;
  color: #86efac;
  line-height: 1.5;
  word-break: break-word;
}

/* AI Section */
.ai-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.ai-avatar {
  width: 100px;
  height: 100px;
  border-radius: 50%;
  overflow: hidden;
  position: relative;
  border: 3px solid rgba(255, 255, 255, 0.1);
  transition: border-color 0.3s;
}

.ai-avatar--active {
  border-color: #10b981;
}

.ai-avatar__img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.ai-avatar__ring {
  position: absolute;
  inset: -6px;
  border-radius: 50%;
  border: 2px solid #10b981;
  animation: ringPulse 1.5s ease-out infinite;
}

@keyframes ringPulse {
  0% { transform: scale(0.9); opacity: 1; }
  100% { transform: scale(1.3); opacity: 0; }
}

.ai-status {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  color: #a3a3a3;
  font-weight: 400;
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #525252;
}

.status-dot--speaking {
  background: #10b981;
  animation: dotPulse 1s ease-in-out infinite;
}

.status-dot--recording {
  background: #ef4444;
  animation: dotPulse 0.8s ease-in-out infinite;
}

.status-dot--loading {
  background: #f59e0b;
  animation: dotPulse 1.2s ease-in-out infinite;
}

.status-dot--done {
  background: #10b981;
}

.status-dot--error {
  background: #ef4444;
}

@keyframes dotPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.3; }
}

/* Question Display */
.question-display {
  max-width: 600px;
  text-align: center;
}

.question-label {
  font-size: 12px;
  color: #525252;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 8px;
}

.question-text {
  font-size: 18px;
  line-height: 1.6;
  color: #e5e5e5;
  font-weight: 400;
}

/* Answer Display */
.answer-display {
  max-width: 600px;
  text-align: center;
  animation: fadeIn 0.3s ease;
}

.answer-label {
  font-size: 12px;
  color: #10b981;
  text-transform: uppercase;
  letter-spacing: 1px;
  margin-bottom: 8px;
}

.answer-text {
  font-size: 15px;
  line-height: 1.6;
  color: #d4d4d4;
  padding: 12px 16px;
  background: rgba(16, 185, 129, 0.06);
  border: 1px solid rgba(16, 185, 129, 0.12);
  border-radius: 10px;
  text-align: left;
}

.answer-text--interim {
  color: #a3a3a3;
  background: rgba(255, 255, 255, 0.03);
  border-color: rgba(255, 255, 255, 0.06);
  min-height: 2.4em;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(8px); }
  to { opacity: 1; transform: translateY(0); }
}

/* Waveform */
.waveform-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.waveform-canvas {
  width: 320px;
  height: 80px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
}

.recording-time {
  font-size: 14px;
  color: #ef4444;
  font-variant-numeric: tabular-nums;
  font-weight: 500;
}

.recording-hint {
  font-size: 12px;
  color: #9ca3af;
  margin-left: 4px;
}

/* Score Card */
.score-card {
  max-width: 480px;
  width: 100%;
  background: rgba(255, 255, 255, 0.05);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: 16px;
  padding: 24px;
}

.score-card__title {
  font-size: 14px;
  font-weight: 600;
  color: #a3a3a3;
  margin-bottom: 16px;
}

.score-card__items {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.score-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.score-item__label {
  width: 56px;
  font-size: 13px;
  color: #a3a3a3;
  flex-shrink: 0;
}

.score-item__bar-wrap {
  flex: 1;
  height: 6px;
  background: rgba(255, 255, 255, 0.06);
  border-radius: 3px;
  overflow: hidden;
}

.score-item__bar {
  height: 100%;
  border-radius: 3px;
  transition: width 0.6s ease;
}

.score-item__value {
  width: 24px;
  text-align: right;
  font-size: 14px;
  font-weight: 600;
  font-variant-numeric: tabular-nums;
}

.score-card__feedback {
  margin-top: 16px;
  padding-top: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  font-size: 14px;
  color: #d4d4d4;
  line-height: 1.6;
}

.feedback-icon {
  margin-right: 6px;
}

.score-card__ref {
  margin-top: 12px;
}

.ref-toggle {
  background: none;
  border: none;
  color: #10b981;
  font-size: 13px;
  cursor: pointer;
  padding: 0;
}

.ref-toggle:hover {
  text-decoration: underline;
}

.ref-text {
  margin-top: 8px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  border-radius: 8px;
  font-size: 14px;
  color: #a3a3a3;
  line-height: 1.6;
}

/* Score card transition */
.score-card-enter-active {
  animation: scoreIn 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}
.score-card-leave-active {
  animation: scoreOut 0.3s ease;
}
@keyframes scoreIn {
  from { opacity: 0; transform: translateY(20px) scale(0.95); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}
@keyframes scoreOut {
  from { opacity: 1; transform: translateY(0); }
  to { opacity: 0; transform: translateY(-10px); }
}

/* Start Hint */
.start-hint {
  text-align: center;
  color: #525252;
}

.start-hint p {
  margin: 0;
  font-size: 15px;
}

.start-hint__sub {
  margin-top: 8px !important;
  font-size: 13px !important;
  color: #404040;
}

/* Footer — 绝对定位在主区域底部，与头像垂直对齐 */
.immersive__footer {
  position: absolute;
  bottom: 0;
  left: 360px;
  right: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  z-index: 10;
}

.ctrl-btn {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 32px;
  border-radius: 28px;
  border: none;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s;
}

.ctrl-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.ctrl-btn--start {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
}

.ctrl-btn--start:hover:not(:disabled) {
  transform: scale(1.03);
  box-shadow: 0 4px 20px rgba(16, 185, 129, 0.4);
}

.footer-row {
  display: flex;
  align-items: center;
  gap: 20px;
}

.ctrl-btn--stop {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.ctrl-btn--stop:hover {
  background: rgba(255, 255, 255, 0.18);
}

.ctrl-btn--hangup {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  padding: 0;
  justify-content: center;
  background: #ef4444;
  color: #fff;
}

.ctrl-btn--hangup:hover {
  background: #dc2626;
  transform: scale(1.05);
}

.ctrl-btn--view {
  background: rgba(255, 255, 255, 0.1);
  color: #fff;
  border: 1px solid rgba(255, 255, 255, 0.1);
}

.ctrl-btn--view:hover {
  background: rgba(255, 255, 255, 0.15);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .immersive__header {
    padding: 12px 16px;
  }

  .immersive__main {
    padding: 16px;
    gap: 24px;
  }

  .ai-avatar {
    width: 80px;
    height: 80px;
  }

  .question-text {
    font-size: 16px;
  }

  .waveform-canvas {
    width: 260px;
    height: 60px;
  }

  .score-card {
    padding: 16px;
  }
}
</style>
