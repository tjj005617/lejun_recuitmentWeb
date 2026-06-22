<template>
  <Transition name="video-call">
    <div v-if="visible" class="video-call-overlay">
      <div class="video-call-container">
        <!-- 远程视频（大屏） -->
        <video ref="remoteVideoEl" autoplay playsinline class="remote-video"></video>

        <!-- 空状态背景 - 仅在没有远程流且没有状态文本时显示 -->
        <div v-if="!hasRemoteVideo" class="empty-state">
          <div class="empty-avatar">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
              <path d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z"/>
            </svg>
          </div>
          <span class="empty-text">等待连接...</span>
        </div>

        <!-- 连接状态提示 -->
        <Transition name="fade">
          <div v-if="statusText" class="status-overlay">
            <div class="status-pulse"></div>
            <span class="status-text">{{ statusText }}</span>
          </div>
        </Transition>

        <!-- 移动端声音恢复提示（自动播放被阻止时显示） -->
        <Transition name="fade">
          <div v-if="noAudioWarning" class="audio-hint-overlay" @click="dismissNoAudioHint">
            <div class="audio-hint-box">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" width="20" height="20">
                <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
                <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
                <line x1="12" y1="19" x2="12" y2="23"/>
                <line x1="8" y1="23" x2="16" y2="23"/>
              </svg>
              <span>点击屏幕启用声音</span>
            </div>
          </div>
        </Transition>

        <!-- 顶部信息栏 -->
        <div class="top-bar">
          <div class="call-badge">
            <span class="badge-dot"></span>
            <span class="badge-text">视频面试</span>
          </div>
          <span v-if="durationText" class="call-duration">{{ durationText }}</span>
        </div>

        <!-- 本地视频（右上角小窗） -->
        <div class="local-video-wrapper" ref="localVideoWrapper">
          <video ref="localVideoEl" autoplay playsinline muted class="local-video"></video>
        </div>

        <!-- 底部控制栏 -->
        <div class="control-bar">
          <button class="ctrl-btn" :class="{ 'ctrl-btn--off': isMuted }" @click="toggleMute" :title="isMuted ? '取消静音' : '静音'">
            <svg v-if="!isMuted" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z"/>
              <path d="M19 10v2a7 7 0 0 1-14 0v-2"/>
              <line x1="12" y1="19" x2="12" y2="23"/>
              <line x1="8" y1="23" x2="16" y2="23"/>
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="1" y1="1" x2="23" y2="23"/>
              <path d="M9 9v3a3 3 0 0 0 5.12 2.12M15 9.34V4a3 3 0 0 0-5.94-.6"/>
              <path d="M17 16.95A7 7 0 0 1 5 12v-2m14 0v2c0 .76-.13 1.49-.35 2.17"/>
              <line x1="12" y1="19" x2="12" y2="23"/>
              <line x1="8" y1="23" x2="16" y2="23"/>
            </svg>
          </button>

          <button class="ctrl-btn ctrl-btn--hangup" @click="hangup" title="挂断">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M10.68 13.31a16 16 0 0 0 3.41 2.6l1.27-1.27a2 2 0 0 1 2.11-.45 12.84 12.84 0 0 0 2.81.7 2 2 0 0 1 1.72 2v3a2 2 0 0 1-2.18 2 19.79 19.79 0 0 1-8.63-3.07 19.5 19.5 0 0 1-6-6 19.79 19.79 0 0 1-3.07-8.67A2 2 0 0 1 4.11 2h3a2 2 0 0 1 2 1.72 12.84 12.84 0 0 0 .7 2.81 2 2 0 0 1-.45 2.11L8.09 9.91"/>
              <line x1="23" y1="1" x2="1" y2="23"/>
            </svg>
          </button>

          <button class="ctrl-btn" :class="{ 'ctrl-btn--off': isCameraOff }" @click="toggleCamera" :title="isCameraOff ? '开启摄像头' : '关闭摄像头'">
            <svg v-if="!isCameraOff" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polygon points="23 7 16 12 23 17 23 7"/>
              <rect x="1" y="5" width="15" height="14" rx="2" ry="2"/>
            </svg>
            <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M16.5 7.5L23 4v16l-6.5-3.5"/>
              <rect x="1" y="5" width="15" height="14" rx="2"/>
              <line x1="1" y1="1" x2="23" y2="23"/>
            </svg>
          </button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<script setup>
import { ref, computed, watch, nextTick, onMounted, onUnmounted } from 'vue'
import ringtoneUrl from '@/assets/music/M500003Ni3xB4Wa9Yw.mp3'

const props = defineProps({
  visible: { type: Boolean, default: false },
  localStream: { type: [Object, null], default: null },
  remoteStream: { type: [Object, null], default: null },
  statusText: { type: String, default: '' },
  ringtone: { type: Boolean, default: false },
  relayMode: { type: Boolean, default: false }
})

const emit = defineEmits(['hangup', 'toggle-mute', 'toggle-camera'])

const remoteVideoEl = ref(null)
const localVideoEl = ref(null)
const localVideoWrapper = ref(null)
const isMuted = ref(false)
const isCameraOff = ref(false)

// 检测远程流是否有视频轨道（relay 模式下视频通过 canvas 渲染，不需要 remoteStream）
const hasRemoteVideo = computed(() => {
  if (props.relayMode) return true
  if (!props.remoteStream) return false
  const videoTracks = props.remoteStream.getVideoTracks()
  return videoTracks.length > 0
})

// 通话时长计时
const durationText = ref('')
let durationTimer = null
let durationSeconds = 0

const startDuration = () => {
  durationSeconds = 0
  durationTimer = setInterval(() => {
    durationSeconds++
    const mins = Math.floor(durationSeconds / 60)
    const secs = durationSeconds % 60
    durationText.value = `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`
  }, 1000)
}

const stopDuration = () => {
  if (durationTimer) {
    clearInterval(durationTimer)
    durationTimer = null
  }
  durationText.value = ''
}

// 铃声控制
let ringtoneAudio = null

const playRingtone = () => {
  if (ringtoneAudio) return
  ringtoneAudio = new Audio(ringtoneUrl)
  ringtoneAudio.loop = true
  ringtoneAudio.volume = 0.8
  ringtoneAudio.play().catch(() => {})
}

const stopRingtone = () => {
  if (ringtoneAudio) {
    ringtoneAudio.pause()
    ringtoneAudio.currentTime = 0
    ringtoneAudio = null
  }
}

// 监听 ringtone prop 变化
watch(() => props.ringtone, (shouldPlay) => {
  if (shouldPlay) {
    playRingtone()
  } else {
    stopRingtone()
  }
})

// 连接状态变化时启动计时
watch(() => props.statusText, (newVal, oldVal) => {
  if (oldVal && !newVal) {
    stopRingtone()
    startDuration()
  }
})

// 安全绑定流到 video 元素
const tryBindStreams = async () => {
  await nextTick()
  if (remoteVideoEl.value && props.remoteStream) {
    if (remoteVideoEl.value.srcObject !== props.remoteStream) {
      remoteVideoEl.value.srcObject = props.remoteStream
      remoteVideoEl.value.volume = 1
    }
    remoteVideoEl.value.play().catch(() => {})
  }
  if (localVideoEl.value && props.localStream) {
    if (localVideoEl.value.srcObject !== props.localStream) {
      localVideoEl.value.srcObject = props.localStream
    }
    localVideoEl.value.play().catch(() => {})
  }
}

// 监听流或可见性变化，重新绑定
watch(
  [() => props.remoteStream, () => props.localStream, () => props.visible],
  (newVals, oldVals) => {
    if (props.visible) tryBindStreams()
  },
  { immediate: true }
)

const toggleMute = () => {
  isMuted.value = !isMuted.value
  emit('toggle-mute', isMuted.value)
}

const toggleCamera = () => {
  isCameraOff.value = !isCameraOff.value
  emit('toggle-camera', isCameraOff.value)
}

const hangup = () => {
  stopRingtone()
  stopDuration()
  emit('hangup')
}

// 移动端声音恢复（自动播放被阻止时的用户交互恢复）
const noAudioWarning = ref(false)
let noAudioCheckTimer = null

const showNoAudioHint = () => {
  noAudioWarning.value = true
}

const dismissNoAudioHint = () => {
  noAudioWarning.value = false
  // 在用户手势中重试播放所有音频元素
  document.querySelectorAll('audio').forEach(el => {
    try {
      // 跳过可见的远程视频元素（由 WebRTC 管理）
      if (el === remoteVideoEl.value) return
      if (el === localVideoEl.value) return
      if (el.paused && el.src && el.readyState >= 1) {
        el.play()?.catch(() => {})
      }
    } catch {}
  })
  // 恢复 AudioContext
  try {
    if (window.__audioCtx && window.__audioCtx.state !== 'running') {
      window.__audioCtx.resume()?.catch(() => {})
    }
  } catch {}
  window.__audioStalled = false
}

// 周期性检查音频是否被静默暂停（仅 relay 模式）
const startNoAudioCheck = () => {
  if (noAudioCheckTimer) return
  noAudioCheckTimer = setInterval(() => {
    if (!props.relayMode || !props.visible) return
    const audios = document.querySelectorAll('audio')
    let anyPlaying = false
    let anyStalled = false
    audios.forEach(el => {
      if (el === remoteVideoEl.value || el === localVideoEl.value) return // 跳过通话视频元素
      if (el === window.__warmupAudioEl) return // 忽略暖场元素
      if (!el.paused && el.currentTime > 0) anyPlaying = true
      if (el.paused && el.readyState >= 2 && el.src) anyStalled = true
    })
    // 有音频元素但都不在播放 → 可能被自动播放策略阻止
    if (anyStalled && !anyPlaying && audios.length > 1) {
      window.__audioStalled = true
      showNoAudioHint()
    }
  }, 5000)
}

onMounted(() => {
  window.__showNoAudioHint = showNoAudioHint
  startNoAudioCheck()
})

onUnmounted(() => {
  stopRingtone()
  stopDuration()
  delete window.__showNoAudioHint
  if (noAudioCheckTimer) {
    clearInterval(noAudioCheckTimer)
    noAudioCheckTimer = null
  }
})

// 暴露 remoteVideoEl 给父组件
defineExpose({ remoteVideoEl, showNoAudioHint })
</script>

<style scoped>
/* 过渡动画 */
.video-call-enter-active {
  animation: videoCallIn 0.35s cubic-bezier(0.16, 1, 0.3, 1);
}
.video-call-leave-active {
  animation: videoCallOut 0.25s cubic-bezier(0.4, 0, 1, 1);
}
@keyframes videoCallIn {
  from {
    opacity: 0;
    transform: scale(0.95);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}
@keyframes videoCallOut {
  from {
    opacity: 1;
    transform: scale(1);
  }
  to {
    opacity: 0;
    transform: scale(0.97);
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* 主容器 */
.video-call-overlay {
  position: fixed;
  inset: 0;
  background: #0c0c0c;
  z-index: 9999;
  display: flex;
  align-items: center;
  justify-content: center;
}

.video-call-container {
  width: 100%;
  height: 100%;
  position: relative;
  background: #0c0c0c;
}

/* 远程视频 */
.remote-video {
  width: 100%;
  height: 100%;
  object-fit: contain;
  background: #0c0c0c;
}

/* 空状态 */
.empty-state {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.empty-avatar {
  width: 80px;
  height: 80px;
  border-radius: 50%;
  background: #1a1a1a;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: breathe 2s ease-in-out infinite;
}

.empty-avatar svg {
  width: 40px;
  height: 40px;
  color: #404040;
}

@keyframes breathe {
  0%, 100% { opacity: 0.6; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.05); }
}

.empty-text {
  color: #525252;
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0.5px;
}

/* 状态覆盖层 */
.status-overlay {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
}

.status-pulse {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: rgba(16, 185, 129, 0.15);
  position: relative;
}

.status-pulse::before {
  content: '';
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid #10b981;
  animation: pulse 1.5s ease-out infinite;
}

@keyframes pulse {
  0% {
    transform: scale(0.8);
    opacity: 1;
  }
  100% {
    transform: scale(1.6);
    opacity: 0;
  }
}

.status-text {
  color: #a3a3a3;
  font-size: 14px;
  font-weight: 400;
  letter-spacing: 0.3px;
}

/* 顶部栏 */
.top-bar {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  padding: 20px 24px;
  background: linear-gradient(to bottom, rgba(0, 0, 0, 0.6), transparent);
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 10;
}

.call-badge {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  border-radius: 20px;
}

.badge-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #10b981;
  animation: blink 2s ease-in-out infinite;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.4; }
}

.badge-text {
  color: #fff;
  font-size: 13px;
  font-weight: 500;
}

.call-duration {
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  font-variant-numeric: tabular-nums;
  padding: 6px 12px;
  background: rgba(0, 0, 0, 0.5);
  backdrop-filter: blur(8px);
  border-radius: 20px;
}

/* 本地视频 */
.local-video-wrapper {
  position: absolute;
  top: 80px;
  right: 20px;
  width: 160px;
  height: 120px;
  border-radius: 12px;
  overflow: hidden;
  border: 2px solid rgba(255, 255, 255, 0.1);
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.4);
  z-index: 10;
  background: #1a1a1a;
}

.local-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transform: scaleX(-1);
}

/* 控制栏 */
.control-bar {
  position: absolute;
  bottom: 32px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  background: rgba(24, 24, 24, 0.9);
  backdrop-filter: blur(12px);
  border-radius: 28px;
  border: 1px solid rgba(255, 255, 255, 0.06);
}

.ctrl-btn {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  border: none;
  background: rgba(255, 255, 255, 0.1);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  color: #fff;
}

.ctrl-btn:hover {
  background: rgba(255, 255, 255, 0.15);
}

.ctrl-btn svg {
  width: 20px;
  height: 20px;
}

.ctrl-btn--off {
  background: rgba(239, 68, 68, 0.2);
  color: #f87171;
}

.ctrl-btn--off:hover {
  background: rgba(239, 68, 68, 0.3);
}

.ctrl-btn--hangup {
  width: 56px;
  height: 56px;
  background: #ef4444;
  color: #fff;
  margin: 0 8px;
}

.ctrl-btn--hangup:hover {
  background: #dc2626;
  transform: scale(1.05);
}

.ctrl-btn--hangup:active {
  transform: scale(0.95);
}

/* 移动端适配 */
@media (max-width: 768px) {
  .local-video-wrapper {
    width: 120px;
    height: 90px;
    top: 70px;
    right: 16px;
  }

  .top-bar {
    padding: 16px;
  }

  .control-bar {
    bottom: 24px;
    padding: 8px 12px;
    gap: 10px;
  }

  .ctrl-btn {
    width: 44px;
    height: 44px;
  }

  .ctrl-btn svg {
    width: 18px;
    height: 18px;
  }

  .ctrl-btn--hangup {
    width: 52px;
    height: 52px;
  }
}

/* 移动端声音恢复提示 */
.audio-hint-overlay {
  position: absolute;
  bottom: 100px;
  left: 50%;
  transform: translateX(-50%);
  z-index: 25;
  animation: audioHintPulse 2s ease-in-out infinite;
  cursor: pointer;
}

.audio-hint-box {
  display: flex;
  align-items: center;
  gap: 8px;
  background: rgba(255, 152, 0, 0.9);
  color: #fff;
  padding: 10px 20px;
  border-radius: 24px;
  font-size: 14px;
  font-weight: 500;
  white-space: nowrap;
  box-shadow: 0 4px 16px rgba(255, 152, 0, 0.4);
}

.audio-hint-box svg {
  flex-shrink: 0;
}

@keyframes audioHintPulse {
  0%, 100% { opacity: 1; }
  50% { opacity: 0.7; }
}
</style>
