<template>
  <div class="asr-test">
    <h1>MiMo ASR 实时语音识别测试</h1>
    <p class="subtitle">测试语音能否实时识别成文字输出在屏幕上</p>

    <div class="controls">
      <button
        :class="['btn', isRecording ? 'btn--stop' : 'btn--start']"
        @click="toggleRecording"
      >
        {{ isRecording ? '停止录音' : '开始录音' }}
      </button>
      <span class="status" :class="statusClass">{{ statusText }}</span>
    </div>

    <div class="debug-info">
      <div>当前地址: {{ currentHost }}</div>
      <div>协议: {{ currentProtocol }}</div>
      <div>WS 地址: {{ wsUrl }}</div>
    </div>

    <div class="result-section">
      <h2>实时识别结果（Web Speech API）</h2>
      <div class="text-display" :class="{ 'text-display--active': isRecording }">
        <span v-if="!liveText && !finalText" class="placeholder">点击"开始录音"后说话...</span>
        <span v-if="liveText" class="interim">{{ liveText }}</span>
      </div>
    </div>

    <div class="result-section">
      <h2>MiMo ASR 最终结果</h2>
      <div class="text-display">
        <span v-if="!finalText" class="placeholder">停止录音后显示准确结果...</span>
        <span v-else class="final">{{ finalText }}</span>
      </div>
    </div>

    <div class="log-section">
      <h2>事件日志</h2>
      <div class="log-display" ref="logRef">
        <div v-for="(log, i) in logs" :key="i" :class="'log--' + log.type">
          <span class="log-time">{{ log.time }}</span>
          <span class="log-msg">{{ log.message }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onBeforeUnmount, nextTick } from 'vue'
import { mergeChunksToWavBase64 } from '@/utils/audioChunkEncoder'

const isRecording = ref(false)
const statusText = ref('未连接')
const statusClass = ref('status--idle')
const liveText = ref('')       // Web Speech API 实时字幕
const finalText = ref('')      // MiMo ASR 最终结果
const logs = ref([])
const logRef = ref(null)

const currentHost = location.host
const currentProtocol = location.protocol
const wsProtocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
const wsUrl = `${wsProtocol}//${location.host}/ws/asr`

let ws = null
let recognition = null   // Web Speech API 实例
let mediaRecorder = null
let audioChunks = []
let micStream = null
let speechConfirmedText = ''  // Web Speech API 已确认的文本
let audioContext = null
let analyser = null
let silenceCheckId = null
let lastSpeechTime = 0

const SILENCE_DURATION = 3000  // 静音 3 秒自动停止
const SILENCE_THRESHOLD = 15   // 音量阈值

/** 添加日志 */
function addLog(type, message) {
  const now = new Date()
  const time = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`
  logs.value.push({ type, message, time })
  if (logs.value.length > 200) logs.value.shift()
  nextTick(() => {
    if (logRef.value) logRef.value.scrollTop = logRef.value.scrollHeight
  })
}

/** 连接 ASR WebSocket（用于最终识别） */
function connectWs() {
  addLog('info', `连接 WebSocket: ${wsUrl}`)

  ws = new WebSocket(wsUrl)

  ws.onopen = () => {
    ws.send(JSON.stringify({
      type: 'asr_init',
      interviewId: 0,
      round: 1
    }))
    statusText.value = '已连接'
    statusClass.value = 'status--connected'
    addLog('success', 'WebSocket 连接成功')
  }

  ws.onmessage = (event) => {
    try {
      const data = JSON.parse(event.data)
      if (data.type === 'asr_final') {
        finalText.value = data.text || ''
        addLog('success', `[MiMo 最终结果] ${finalText.value}`)
      } else if (data.type === 'asr_result') {
        addLog('info', `[MiMo 识别中] ${data.text || ''}`)
      } else if (data.type === 'asr_error') {
        addLog('error', `[错误] ${data.message}`)
      }
    } catch (e) {
      addLog('error', `解析消息失败: ${e.message}`)
    }
  }

  ws.onerror = (e) => {
    addLog('error', `WebSocket 错误: ${JSON.stringify(e)}`)
  }

  ws.onclose = (e) => {
    addLog('info', `WebSocket 已关闭: code=${e.code}`)
    ws = null
  }
}

/** 启动 Web Speech API 实时字幕 */
function startSpeechRecognition() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    addLog('error', '浏览器不支持 Web Speech API')
    return
  }

  recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.continuous = true       // 持续识别不自动停
  recognition.interimResults = true   // 返回中间结果（打字机效果）

  recognition.onresult = (event) => {
    let interim = ''
    for (let i = event.resultIndex; i < event.results.length; i++) {
      const transcript = event.results[i][0].transcript
      if (event.results[i].isFinal) {
        speechConfirmedText += transcript
      } else {
        interim += transcript
      }
    }
    // 已确认文本 + 当前中间文本（打字机效果）
    liveText.value = speechConfirmedText + interim
  }

  recognition.onerror = (event) => {
    addLog('error', `Speech API 错误: ${event.error}`)
  }

  recognition.onend = () => {
    // 如果还在录音，自动重启（浏览器可能自动停）
    if (isRecording.value && recognition) {
      try { recognition.start() } catch {}
    }
  }

  recognition.start()
  addLog('success', 'Web Speech API 启动（实时字幕）')
}

/** 停止 Web Speech API */
function stopSpeechRecognition() {
  if (recognition) {
    recognition.onend = null  // 防止自动重启
    recognition.stop()
    recognition = null
  }
}

/** 启动静音检测 */
function startSilenceDetection(stream) {
  audioContext = new (window.AudioContext || window.webkitAudioContext)()
  const source = audioContext.createMediaStreamSource(stream)
  analyser = audioContext.createAnalyser()
  analyser.fftSize = 512
  source.connect(analyser)

  const dataArray = new Uint8Array(analyser.frequencyBinCount)
  lastSpeechTime = Date.now()

  const checkVolume = () => {
    if (!isRecording.value) return

    analyser.getByteFrequencyData(dataArray)
    // 计算平均音量
    let sum = 0
    for (let i = 0; i < dataArray.length; i++) sum += dataArray[i]
    const avg = sum / dataArray.length

    if (avg > SILENCE_THRESHOLD) {
      lastSpeechTime = Date.now()
    }

    // 静音超过阈值 → 自动停止
    const silentTime = Date.now() - lastSpeechTime
    if (silentTime >= SILENCE_DURATION) {
      addLog('info', `静音 ${(silentTime / 1000).toFixed(1)} 秒，自动停止录音`)
      stopRecording()
      return
    }

    silenceCheckId = requestAnimationFrame(checkVolume)
  }

  silenceCheckId = requestAnimationFrame(checkVolume)
  addLog('info', `静音检测启动（阈值 ${SILENCE_DURATION / 1000}s）`)
}

/** 停止静音检测 */
function stopSilenceDetection() {
  if (silenceCheckId) {
    cancelAnimationFrame(silenceCheckId)
    silenceCheckId = null
  }
  if (audioContext) {
    audioContext.close()
    audioContext = null
  }
}

/** 开始录音 */
async function startRecording() {
  try {
    micStream = await navigator.mediaDevices.getUserMedia({
      audio: { echoCancellation: true, noiseSuppression: true, autoGainControl: true }
    })

    liveText.value = ''
    finalText.value = ''
    speechConfirmedText = ''
    audioChunks = []

    // 启动 Web Speech API 实时字幕
    startSpeechRecognition()

    // 启动 MediaRecorder 用于录制完整音频
    mediaRecorder = new MediaRecorder(micStream, { mimeType: 'audio/webm;codecs=opus' })
    mediaRecorder.ondataavailable = (e) => {
      if (e.data.size > 0) audioChunks.push(e.data)
    }
    mediaRecorder.start()

    // 启动静音检测（静音 3 秒自动停止）
    startSilenceDetection(micStream)

    isRecording.value = true
    statusText.value = '录音中...'
    statusClass.value = 'status--recording'
    addLog('success', '开始录音（Web Speech API 字幕 + 静音检测自动停止）')
  } catch (e) {
    addLog('error', `录音启动失败: ${e.message}`)
  }
}

/** 停止录音 → 发送 MiMo ASR 获取准确文本 */
async function stopRecording() {
  if (!isRecording.value) return

  isRecording.value = false
  stopSpeechRecognition()
  stopSilenceDetection()

  statusText.value = '处理中...'
  statusClass.value = 'status--processing'
  addLog('info', '录音停止，发送 MiMo ASR 获取准确文本...')

  // 停止 MediaRecorder
  await new Promise((resolve) => {
    mediaRecorder.onstop = resolve
    mediaRecorder.stop()
  })

  // 合并分片 → WAV → 发送到 MiMo ASR
  try {
    const wavBase64 = await mergeChunksToWavBase64(audioChunks)
    if (ws && ws.readyState === WebSocket.OPEN) {
      ws.send(JSON.stringify({ type: 'audio_final', audio: wavBase64 }))
      addLog('info', `发送最终音频: ${(wavBase64.length / 1024).toFixed(1)} KB`)
    }
  } catch (e) {
    addLog('error', `编码音频失败: ${e.message}`)
  }

  // 停止麦克风
  if (micStream) {
    micStream.getTracks().forEach(t => t.stop())
    micStream = null
  }

  statusText.value = '已完成'
  statusClass.value = 'status--connected'
}

/** 切换录音状态 */
async function toggleRecording() {
  if (isRecording.value) {
    await stopRecording()
  } else {
    if (!ws || ws.readyState !== WebSocket.OPEN) {
      connectWs()
      await new Promise((resolve) => {
        const check = setInterval(() => {
          if (ws && ws.readyState === WebSocket.OPEN) {
            clearInterval(check)
            resolve()
          }
        }, 100)
        setTimeout(() => { clearInterval(check); resolve() }, 5000)
      })
    }
    await startRecording()
  }
}

onBeforeUnmount(() => {
  stopSpeechRecognition()
  stopSilenceDetection()
  if (mediaRecorder && mediaRecorder.state === 'recording') {
    mediaRecorder.stop()
  }
  if (micStream) { micStream.getTracks().forEach(t => t.stop()) }
  if (ws) ws.close()
})
</script>

<style scoped>
.asr-test {
  max-width: 800px;
  margin: 0 auto;
  padding: 32px 20px;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
}

h1 {
  font-size: 24px;
  color: #1a1a1a;
  margin-bottom: 4px;
}

.subtitle {
  color: #666;
  margin-bottom: 24px;
}

.controls {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.debug-info {
  padding: 12px;
  background: #f0f0f0;
  border-radius: 8px;
  font-size: 13px;
  font-family: monospace;
  color: #555;
  margin-bottom: 24px;
  line-height: 1.6;
}

.btn {
  padding: 12px 32px;
  border: none;
  border-radius: 8px;
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn--start {
  background: #4f46e5;
  color: #fff;
}

.btn--start:hover {
  background: #4338ca;
}

.btn--stop {
  background: #ef4444;
  color: #fff;
}

.btn--stop:hover {
  background: #dc2626;
}

.status {
  font-size: 14px;
  padding: 4px 12px;
  border-radius: 12px;
}

.status--idle { background: #f3f4f6; color: #6b7280; }
.status--connected { background: #d1fae5; color: #065f46; }
.status--recording { background: #fee2e2; color: #991b1b; }
.status--processing { background: #fef3c7; color: #92400e; }
.status--error { background: #fee2e2; color: #991b1b; }

.result-section, .log-section {
  margin-bottom: 24px;
}

h2 {
  font-size: 16px;
  color: #374151;
  margin-bottom: 8px;
}

.text-display {
  min-height: 120px;
  padding: 16px;
  border: 2px solid #e5e7eb;
  border-radius: 8px;
  background: #fafafa;
  font-size: 18px;
  line-height: 1.6;
  transition: border-color 0.2s;
}

.text-display--active {
  border-color: #4f46e5;
  background: #fff;
}

.placeholder {
  color: #9ca3af;
}

.interim {
  color: #6b7280;
}

.final {
  color: #1a1a1a;
  font-weight: 500;
}

.log-display {
  height: 300px;
  overflow-y: auto;
  padding: 12px;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
  background: #1e1e1e;
  font-family: 'Consolas', 'Monaco', monospace;
  font-size: 13px;
}

.log--info .log-msg { color: #9cdcfe; }
.log--success .log-msg { color: #6a9955; }
.log--error .log-msg { color: #f48771; }

.log-time {
  color: #6a9955;
  margin-right: 8px;
}

.log-msg {
  word-break: break-all;
}
</style>
