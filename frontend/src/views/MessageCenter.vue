<template>
  <AppShell :bgImage="false">
    <div class="message-center">
      <div class="message-center__container">
        <el-card class="message-card">
          <template #header>
            <div class="card-header">
              <h2>消息中心</h2>
            </div>
          </template>

          <div class="message-layout">
            <!-- 会话列表 -->
            <div class="conversation-list" :class="{ 'conversation-list--hidden': showChat }">
              <div class="conversation-header">
                <el-input
                  v-model="searchKeyword"
                  placeholder="搜索联系人"
                  size="small"
                  prefix-icon="Search"
                />
              </div>

              <div v-if="conversations.length === 0" class="empty-conversations">
                <el-empty description="暂无消息" :image-size="80" />
              </div>

              <div v-else class="conversations">
                <div
                  v-for="conv in filteredConversations"
                  :key="conv.userId"
                  class="conversation-item"
                  :class="{ active: selectedUserId === conv.userId }"
                  @click="selectConversation(conv)"
                >
                  <el-avatar :size="40" class="conversation-avatar" @click.stop="openHrJobs(conv.userId)">
                    {{ conv.nickname?.charAt(0) || 'U' }}
                  </el-avatar>
                  <div class="conversation-info">
                    <div class="conversation-name">
                      {{ conv.nickname || conv.username }}
                      <span v-if="conv.jobTitle" class="conversation-job">· {{ conv.jobTitle }}</span>
                    </div>
                    <div class="conversation-last">{{ conv.lastMessage }}</div>
                  </div>
                  <div class="conversation-meta">
                    <span class="conversation-time">{{ formatTime(conv.lastTime) }}</span>
                    <span v-if="conv.unreadCount > 0" class="conversation-badge">{{ conv.unreadCount }}</span>
                  </div>
                </div>
              </div>
            </div>

            <!-- 聊天区域 -->
            <div class="chat-area" :class="{ 'chat-area--mobile-show': showChat }">
              <template v-if="selectedUserId">
                <div class="chat-header" @click="openHrJobs(selectedUserId)" style="cursor: pointer">
                  <el-icon class="chat-header__back" @click.stop="backToConversations"><ArrowLeft /></el-icon>
                  <el-avatar :size="32" class="chat-header__avatar">
                    {{ selectedConversation?.nickname?.charAt(0) || 'U' }}
                  </el-avatar>
                  <div class="chat-header__info">
                    <span class="chat-title">{{ selectedConversation?.nickname }}</span>
                    <span class="chat-job" v-if="selectedConversation?.jobTitle">
                      关于: {{ selectedConversation.jobTitle }}
                    </span>
                  </div>
                </div>

                <!-- 岗位卡片 -->
                <div v-if="chatJob" class="chat-job-card" @click="$router.push(`/job/${chatJob.id}`)">
                  <div class="chat-job-card__left">
                    <div class="chat-job-card__company" v-if="chatJob.company">
                      <span class="chat-job-card__company-logo">{{ chatJob.company.name?.charAt(0) }}</span>
                      <span class="chat-job-card__company-name">{{ chatJob.company.name }}</span>
                    </div>
                    <div class="chat-job-card__title">{{ chatJob.title }}</div>
                    <div class="chat-job-card__salary" v-if="chatJob.salaryMin || chatJob.salaryMax">
                      {{ formatSalary(chatJob.salaryMin, chatJob.salaryMax) }}
                    </div>
                    <div class="chat-job-card__tags">
                      <span class="chat-job-tag" v-if="chatJob.city">{{ chatJob.city }}</span>
                      <span class="chat-job-tag" v-if="chatJob.experience">{{ chatJob.experience }}</span>
                      <span class="chat-job-tag" v-if="chatJob.education">{{ chatJob.education }}</span>
                    </div>
                    <div class="chat-job-card__desc" v-if="chatJob.description">
                      {{ stripHtml(chatJob.description).substring(0, 60) }}{{ stripHtml(chatJob.description).length > 60 ? '...' : '' }}
                    </div>
                    <div class="chat-job-card__meta">
                      <span v-if="chatJob.viewCount != null">浏览 {{ chatJob.viewCount }}</span>
                      <span v-if="chatJob.applyCount != null"> · 申请 {{ chatJob.applyCount }}</span>
                    </div>
                    <div class="chat-job-card__link">查看详情 ›</div>
                  </div>
                  <div class="chat-job-card__right" v-if="chatJob.hr" @click.stop="openHrJobs(chatJob.userId)">
                    <el-avatar :size="44" class="chat-job-card__hr-avatar">
                      {{ chatJob.hr.nickname?.charAt(0) || chatJob.hr.username?.charAt(0) || 'HR' }}
                    </el-avatar>
                    <span class="chat-job-card__hr-name">{{ chatJob.hr.nickname || chatJob.hr.username }}</span>
                    <span class="chat-job-card__hr-badge">HR</span>
                  </div>
                </div>

                <div class="chat-messages" ref="messagesRef">
                  <div
                    v-for="msg in messages"
                    :key="msg.id"
                    class="message-item"
                    :class="{ 'message-self': msg.senderId === currentUserId }"
                  >
                    <el-avatar :size="32" class="message-avatar" :class="{ 'message-avatar--clickable': msg.senderId !== currentUserId }" @click.stop="msg.senderId !== currentUserId && handleAvatarClick()">
                      {{ msg.senderId === currentUserId ? '我' : (selectedConversation?.nickname?.charAt(0) || 'U') }}
                    </el-avatar>
                    <div class="message-content">
                      <!-- 简历附件消息 -->
                      <div v-if="msg.type === 'resume' && msg.resumeId" class="message-bubble message-bubble--resume">
                        <div class="resume-card">
                          <div class="resume-card__header">
                            <el-icon><Document /></el-icon>
                            <span>简历附件</span>
                          </div>
                          <div class="resume-card__content">
                            <p class="resume-card__text">{{ msg.content }}</p>
                            <div v-if="resumeCache[msg.resumeId]" class="resume-card__info">
                              <p v-if="resumeCache[msg.resumeId].name"><strong>姓名：</strong>{{ resumeCache[msg.resumeId].name }}</p>
                              <p v-if="resumeCache[msg.resumeId].education"><strong>学历：</strong>{{ resumeCache[msg.resumeId].education }}</p>
                              <p v-if="resumeCache[msg.resumeId].skills && resumeCache[msg.resumeId].skills !== '[]'"><strong>技能：</strong>{{ resumeCache[msg.resumeId].skills }}</p>
                            </div>
                            <div v-else class="resume-card__loading">
                              <el-icon class="is-loading"><Loading /></el-icon>
                              加载简历中...
                            </div>
                          </div>
                          <div class="resume-card__footer">
                            <el-button size="small" type="primary" link @click="viewResume(msg.resumeId)">
                              查看完整简历
                            </el-button>
                          </div>
                        </div>
                      </div>
                      <!-- 视频通话消息 -->
                      <div v-else-if="msg.type === 'video_call'" class="message-bubble message-bubble--system">
                        <div class="video-call-msg">
                          <span class="video-call-icon">📹</span>
                          <span class="video-call-text" v-if="msg.videoCallStatus === 'ended'">{{ msg.content }}</span>
                          <span class="video-call-text" v-else>
                            视频面试通话
                            <span v-if="msg.videoCallStatus === 'ringing'">（响铃中）</span>
                            <span v-else-if="msg.videoCallStatus === 'accepted'">（已接听）</span>
                            <span v-else-if="msg.videoCallStatus === 'rejected'">（已拒绝）</span>
                          </span>
                        </div>
                      </div>
                      <!-- 普通文字消息 -->
                      <div v-else class="message-bubble">
                        {{ msg.content }}
                        <!-- 发送状态图标 -->
                        <span v-if="msg.senderId === currentUserId && msg.status === 'sending'" class="message-status">
                          <el-icon class="is-loading"><Loading /></el-icon>
                        </span>
                        <span v-else-if="msg.senderId === currentUserId && msg.status === 'failed'" class="message-status message-status--failed" @click="resendMessage(msg)">
                          <el-icon><WarningFilled /></el-icon>
                        </span>
                      </div>
                      <div class="message-time">{{ formatTime(msg.createdAt) }}</div>
                    </div>
                  </div>
                </div>

                <div class="chat-input">
                  <div class="chat-input__actions">
                    <!-- 求职者：发送简历 + 岗位投递 -->
                    <template v-if="!isHR">
                      <!-- 简历选择下拉框 -->
                      <el-dropdown trigger="click" @command="handleSendResume" :disabled="!myResumes.length">
                        <el-button size="small">
                          <el-icon><Document /></el-icon>
                          发送简历
                          <el-icon class="el-icon--right"><ArrowDown /></el-icon>
                        </el-button>
                        <template #dropdown>
                          <el-dropdown-menu>
                            <el-dropdown-item
                              v-for="r in myResumes"
                              :key="r.id"
                              :command="r.id"
                            >
                              {{ r.fileName || '简历' }} ({{ r.name || '未填写姓名' }})
                            </el-dropdown-item>
                            <el-dropdown-item v-if="myResumes.length === 0" disabled>
                              暂无简历，请先上传
                            </el-dropdown-item>
                          </el-dropdown-menu>
                        </template>
                      </el-dropdown>
                      <el-button size="small" type="primary" @click="handleApplyJob">
                        <el-icon><Position /></el-icon>
                        岗位投递
                      </el-button>
                    </template>
                    <!-- HR：视频面试 -->
                    <template v-else>
                      <el-button size="small" type="primary" @click="handleVideoInterview" :disabled="videoCallVisible || videoInvitePending">
                        <el-icon><VideoCamera /></el-icon>
                        视频面试
                      </el-button>
                    </template>
                  </div>
                  <div class="input-row">
                    <el-input
                      v-model="newMessage"
                      placeholder="输入消息..."
                      @keydown.enter.prevent="handleEnterSend"
                    />
                    <div class="emoji-picker-wrapper">
                      <span class="emoji-btn" @click.stop="showEmojiPicker = !showEmojiPicker">😀</span>
                      <div v-if="showEmojiPicker" class="emoji-panel" @click.stop>
                        <div class="emoji-grid">
                          <span
                            v-for="emoji in emojiList"
                            :key="emoji"
                            class="emoji-item"
                            @click="insertEmoji(emoji)"
                          >{{ emoji }}</span>
                        </div>
                      </div>
                    </div>
                    <!-- 语音输入按钮 -->
                    <button
                      class="voice-btn"
                      :class="{ recording: isRecording, loading: sttLoading }"
                      @click="toggleVoiceInput"
                      :disabled="sttLoading"
                      :title="isRecording ? '点击停止录音' : '语音输入'"
                    >
                      <svg v-if="isRecording" width="16" height="16" viewBox="0 0 24 24" fill="none">
                        <rect x="6" y="6" width="12" height="12" rx="2" fill="currentColor"/>
                      </svg>
                      <svg v-else-if="sttLoading" class="spin" width="16" height="16" viewBox="0 0 24 24" fill="none">
                        <path d="M12 2v4m0 12v4m-7.07-3.93l2.83-2.83m8.48-8.48l2.83-2.83M2 12h4m12 0h4M4.93 4.93l2.83 2.83m8.48 8.48l2.83 2.83" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                      </svg>
                      <svg v-else width="16" height="16" viewBox="0 0 24 24" fill="none">
                        <path d="M12 1a3 3 0 0 0-3 3v8a3 3 0 0 0 6 0V4a3 3 0 0 0-3-3z" stroke="currentColor" stroke-width="2"/>
                        <path d="M19 10v2a7 7 0 0 1-14 0v-2" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                        <line x1="12" y1="19" x2="12" y2="23" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                        <line x1="8" y1="23" x2="16" y2="23" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
                      </svg>
                    </button>
                    <el-button @click="sendTextMessage" :disabled="!newMessage.trim() || isSending">
                      发送
                    </el-button>
                  </div>
                </div>
              </template>

              <template v-else>
                <div class="empty-chat">
                  <el-empty description="选择一个会话开始聊天" />
                </div>
              </template>
            </div>
          </div>
        </el-card>
      </div>
    </div>

    <!-- HR负责岗位抽屉 -->
    <el-drawer
      v-model="hrJobsDrawerVisible"
      :title="hrJobsDrawerTitle"
      direction="rtl"
      size="420px"
      :before-close="() => hrJobsDrawerVisible = false"
    >
      <div v-if="hrJobsLoading" class="hr-jobs-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      <div v-else-if="hrJobsList.length === 0" class="hr-jobs-empty">
        <el-empty description="暂无在招岗位" />
      </div>
      <div v-else class="hr-jobs-list">
        <div
          v-for="item in hrJobsList"
          :key="item.id"
          class="hr-job-item"
          :class="{ active: chatJob && item.id === chatJob.id }"
          @click="hrJobsDrawerVisible = false; $router.push(`/job/${item.id}`)"
        >
          <div class="hr-job-item__top">
            <span class="hr-job-item__title">{{ item.title }}</span>
            <span class="hr-job-item__salary" v-if="item.salaryMin || item.salaryMax">
              {{ formatSalary(item.salaryMin, item.salaryMax) }}
            </span>
          </div>
          <div class="hr-job-item__tags">
            <span class="hr-job-tag" v-if="item.city">{{ item.city }}</span>
            <span class="hr-job-tag" v-if="item.experience">{{ item.experience }}</span>
            <span class="hr-job-tag" v-if="item.education">{{ item.education }}</span>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 求职者信息抽屉 -->
    <el-drawer
      v-model="seekerInfoDrawerVisible"
      title="求职者信息"
      direction="rtl"
      size="380px"
      :before-close="() => seekerInfoDrawerVisible = false"
    >
      <div v-if="seekerInfoLoading" class="hr-jobs-loading">
        <el-icon class="is-loading"><Loading /></el-icon>
        <span>加载中...</span>
      </div>
      <div v-else-if="seekerInfo" class="seeker-info">
        <div class="seeker-info__header">
          <el-avatar :size="56" class="seeker-info__avatar">
            {{ seekerInfo.nickname?.charAt(0) || seekerInfo.username?.charAt(0) || 'U' }}
          </el-avatar>
          <div class="seeker-info__name">{{ seekerInfo.nickname || seekerInfo.username }}</div>
        </div>
        <div class="seeker-info__fields">
          <div class="seeker-info__row" v-if="seekerInfo.phone">
            <span class="seeker-info__label">手机</span>
            <span class="seeker-info__value">{{ seekerInfo.phone }}</span>
          </div>
          <div class="seeker-info__row" v-if="seekerInfo.email">
            <span class="seeker-info__label">邮箱</span>
            <span class="seeker-info__value">{{ seekerInfo.email }}</span>
          </div>
          <div class="seeker-info__row">
            <span class="seeker-info__label">注册时间</span>
            <span class="seeker-info__value">{{ seekerInfo.createdAt?.substring(0, 10) }}</span>
          </div>
        </div>
      </div>
    </el-drawer>

    <!-- 视频面试浮层 -->
    <VideoCall
      ref="videoCallRef"
      :visible="videoCallVisible"
      :localStream="localStream"
      :remoteStream="remoteStream"
      :statusText="videoCallStatus"
      :ringtone="isRinging"
      :relayMode="relayMode"
      @hangup="handleHangup"
      @toggle-mute="handleToggleMute"
      @toggle-camera="handleToggleCamera"
    />

    <!-- 视频邀请弹窗（自定义，用于 iOS 兼容） -->
    <div v-if="showVideoInviteDialog" class="video-invite-dialog-overlay" @click="onDialogInteraction">
      <div class="video-invite-dialog">
        <div class="video-invite-dialog__icon">📹</div>
        <div class="video-invite-dialog__title">视频面试邀请</div>
        <div class="video-invite-dialog__message">HR 邀请您进行视频面试，是否接受？</div>
        <div class="video-invite-dialog__actions">
          <button class="video-invite-dialog__btn video-invite-dialog__btn--reject" @click.stop="onVideoInviteReject">拒绝</button>
          <button class="video-invite-dialog__btn video-invite-dialog__btn--accept" @click.stop="onVideoInviteAccept">接受</button>
        </div>
      </div>
    </div>
  </AppShell>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox, ElNotification } from 'element-plus'
import { Document, Position, Loading, ArrowDown, WarningFilled, VideoCamera, ArrowLeft } from '@element-plus/icons-vue'
import AppShell from '@/components/AppShell.vue'
import VideoCall from '@/components/VideoCall.vue'
import { getLocalStream, createPeerConnection, createOffer, handleOffer, handleAnswer, addIceCandidate, restartIce, closeConnection, startMediaRelay, stopMediaRelay, createRelayReceiver, isWebCodecsSupported, startWebCodecsSender, createWebCodecsReceiver, createAudioReceiver } from '@/utils/webrtc'
import { useUserStore } from '@/stores/user'
import { useMessageStore } from '@/stores/message'
import { markAsRead, updateConversationJob } from '@/api/message'
import { getUserInfo } from '@/api/user'
import { getJobDetail } from '@/api/job'
import { getCompanyDetail } from '@/api/company'
import { searchJobsByES } from '@/api/search'
import { applyJob } from '@/api/application'
import { getUserResumes, getResume } from '@/api/resume'
import { startRecording } from '@/utils/audioRecorder'
import { recognizeSpeech } from '@/api/stt'
import ringtoneUrl from '@/assets/music/M500003Ni3xB4Wa9Yw.mp3'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const { user, isLoggedIn } = userStore
const messageStore = useMessageStore()

const currentUserId = computed(() => user.value?.id)
const isHR = computed(() => user.value?.roleType === 2)
const searchKeyword = ref('')
const selectedUserId = ref(null)
const newMessage = ref('')

// 同步选中用户到 store（供 AppShell 判断 SSE 是否需要处理视频邀请）
watch(selectedUserId, (val) => { messageStore.selectedUserId.value = val })

// STT 语音输入状态
const isRecording = ref(false)
const sttLoading = ref(false)
let recorderInstance = null
const messagesRef = ref(null)

// 使用全局 store 中的会话列表（登录时已预加载）
const conversations = messageStore.conversations
const messages = ref([])
const resumeCache = ref({})
const myResumes = ref([]) // 当前用户的所有简历
const chatJob = ref(null) // 当前聊天关联的岗位信息
const showEmojiPicker = ref(false)
const showChat = ref(false) // 移动端：是否显示聊天区域

// 常用 emoji 列表
const emojiList = [
  '😀', '😃', '😄', '😁', '😆', '😅', '🤣', '😂',
  '🙂', '🙃', '😉', '😊', '😇', '🥰', '😍', '🤩',
  '😘', '😗', '😚', '😙', '🥲', '😋', '😛', '😜',
  '🤪', '😝', '🤑', '🤗', '🤭', '🤫', '🤔', '🤐',
  '🤨', '😐', '😑', '😶', '😏', '😒', '🙄', '😬',
  '😮', '😯', '😲', '😳', '🥺', '😦', '😧', '😨',
  '😰', '😥', '😢', '😭', '😱', '😖', '😣', '😞',
  '😓', '😩', '😫', '🥱', '😤', '😡', '😠', '🤬',
  '👍', '👎', '👌', '✌️', '🤞', '🤟', '🤘', '🤙',
  '👋', '🤚', '🖐️', '✋', '🖖', '👏', '🙌', '🤝',
  '🙏', '💪', '❤️', '🔥', '⭐', '🎉', '🎊', '✅'
]

// ==================== 离线消息队列（localStorage） ====================
const getPendingQueue = () => {
  try { return JSON.parse(localStorage.getItem('chat_pending_queue') || '[]') } catch { return [] }
}
const savePendingQueue = (queue) => {
  // 限制队列大小，防止 localStorage 溢出
  if (queue.length > 100) queue.splice(0, queue.length - 100)
  localStorage.setItem('chat_pending_queue', JSON.stringify(queue))
}
const addToPendingQueue = (msg) => {
  const queue = getPendingQueue()
  queue.push({ ...msg, _queuedAt: Date.now(), _retryCount: msg._retryCount || 0 })
  savePendingQueue(queue)
}
const removeFromPendingQueue = (tempId) => {
  const queue = getPendingQueue().filter(m => m.tempId !== tempId)
  savePendingQueue(queue)
}

// ACK 超时追踪：tempId → { timer, messageId }
const ackPendingMap = new Map()
const ACK_TIMEOUT = 5000 // 5秒未收到 ACK 则标记失败

// WebSocket 相关
let ws = null
let currentConversationId = null

// WebRTC 视频面试相关（原生 RTCPeerConnection + WebSocket 信令）
const videoCallVisible = ref(false)
const videoCallStatus = ref('') // 连接状态提示文字
const localStream = ref(null)
const remoteStream = ref(null)
const relayMode = ref(false) // 是否在 WebSocket 中继模式
const videoCallRef = ref(null) // VideoCall 组件引用（用于获取 remoteVideoEl）
const videoInvitePending = ref(false) // 防止按钮连点

const relayDataBuffer = [] // videoEl 就绪前缓存的 relay 数据（就绪后立即清空）
let videoCallEnded = false // 挂断后不再响应 video_media
let videoInviteAccepted = false // 对方是否已接受视频邀请（防止 video_media 在接受前自动弹出覆盖层）
let videoCallStartTime = 0 // 通话开始时间戳

// 铃声控制
let ringtoneAudio = null
const isRinging = ref(false)

const playRingtone = () => {
  if (ringtoneAudio) return
  ringtoneAudio = new Audio(ringtoneUrl)
  ringtoneAudio.loop = true
  ringtoneAudio.volume = 0.8
  ringtoneAudio.play().catch(() => {})
  isRinging.value = true
}

const stopRingtone = () => {
  if (ringtoneAudio) {
    ringtoneAudio.pause()
    ringtoneAudio.currentTime = 0
    ringtoneAudio = null
  }
  isRinging.value = false
}

/** 在用户手势上下文中预热音频元素，解除移动端自动播放限制 */
const warmupAudioElement = () => {
  try {
    // 已有暖场元素且正在播放，无需重复创建
    if (window.__warmupAudioEl && !window.__warmupAudioEl.paused) {
      return
    }
    const el = document.createElement('audio')
    el.style.display = 'none'
    el.setAttribute('playsinline', '')
    el.setAttribute('webkit-playsinline', '')
    // 生成最短的静音 WAV（单字节采样，44字节头+1字节数据）
    const buf = new ArrayBuffer(45)
    const view = new DataView(buf)
    view.setUint32(0, 0x46464952, true) // "RIFF"
    view.setUint32(4, 37, true)
    view.setUint32(8, 0x45564157, true) // "WAVE"
    view.setUint32(12, 0x20746d66, true) // "fmt "
    view.setUint32(16, 16, true)
    view.setUint16(20, 1, true) // PCM
    view.setUint16(22, 1, true) // mono
    view.setUint32(24, 8000, true) // 8kHz
    view.setUint32(28, 8000, true)
    view.setUint16(32, 1, true)
    view.setUint16(34, 8, true) // 8-bit
    view.setUint32(36, 0x61746164, true) // "data"
    view.setUint32(40, 1, true)
    const blob = new Blob([buf], { type: 'audio/wav' })
    el.src = URL.createObjectURL(blob)
    el.loop = true // 循环播放，持续维持全局音频会话解锁
    document.body.appendChild(el)
    el.play().then(() => {
      // 关键：保持 playing 状态以维持 iOS/Android 音频会话解锁
      // 音量设为 0，只作暖场用途，不用于实际音频输出
      el.volume = 0
      el.muted = false // iOS 需要 muted=false 才算真正解锁音频
      window.__warmupAudioEl = el
    }).catch(() => {})

    // 同时解锁 Web Audio API（双重保险：AudioContext 一旦 resume 全局解锁音频）
    try {
      const AudioCtx = window.AudioContext || window.webkitAudioContext
      if (AudioCtx && !window.__audioCtx) {
        const ctx = new AudioCtx()
        ctx.resume().then(() => {
          window.__audioCtx = ctx
        }).catch(() => {})
      } else if (window.__audioCtx && window.__audioCtx.state !== 'running') {
        window.__audioCtx.resume().catch(() => {})
      }
    } catch {}
  } catch {}
}

// 视频邀请弹窗状态（替代 ElMessageBox，iOS 兼容）
const showVideoInviteDialog = ref(false)
let videoInviteResolve = null
let videoInviteDialogPending = false // 防止 SSE 和 WebSocket 同时弹窗

const showVideoInviteConfirm = () => {
  // 如果已有弹窗在显示或等待用户操作，不重复弹出
  if (videoInviteDialogPending || showVideoInviteDialog.value) {
    return new Promise((resolve) => {
      // 返回一个永远不会 resolve 的 Promise，防止重复处理
      // 实际的弹窗会使用已有的 resolve
      console.log('[VideoInvite] 弹窗已存在，忽略重复请求')
    })
  }
  videoInviteDialogPending = true
  return new Promise((resolve) => {
    videoInviteResolve = resolve
    showVideoInviteDialog.value = true
  })
}

// 接受按钮 — 在用户手势中同步获取摄像头（iOS 关键）
const onVideoInviteAccept = async () => {
  showVideoInviteDialog.value = false
  videoInviteDialogPending = false // 重置弹窗锁
  // 在用户手势上下文中预热音频（移动端自动播放策略要求用户手势触发首次播放）
  warmupAudioElement()
  // 在用户手势上下文中同步尝试获取摄像头
  try {
    const stream = await getLocalStream()
    localStream.value = stream
    if (videoInviteResolve) videoInviteResolve({ accepted: true, stream })
  } catch (e) {
    if (videoInviteResolve) videoInviteResolve({ accepted: false, error: e })
  }
  videoInviteResolve = null
}

// 用户与弹窗交互时播放铃声（浏览器要求用户手势才能播放音频）
let dialogRingtonePlayed = false
const onDialogInteraction = () => {
  if (!dialogRingtonePlayed) {
    dialogRingtonePlayed = true
    playRingtone()
  }
}

// 拒绝按钮
const onVideoInviteReject = () => {
  showVideoInviteDialog.value = false
  videoInviteDialogPending = false // 重置弹窗锁
  if (videoInviteResolve) videoInviteResolve({ accepted: false })
  videoInviteResolve = null
}

/** 创建接收器（使用 MediaSource，支持音频） */
/** 尝试清空 relay 缓存：需要 videoInviteAccepted + videoEl 都就绪 */
const tryDrainRelayBuffer = () => {
  if (!videoInviteAccepted || relayDataBuffer.length === 0) return
  const rawRef = videoCallRef.value?.remoteVideoEl
  const videoEl = rawRef?.value ?? rawRef
  if (videoEl && videoEl instanceof HTMLElement) {
    // 检测接收器是否已失败，失败则清理并重新创建
    if (relayReceiver && relayReceiver.failed) {
      console.warn('[Relay] receiver failed during drain, recreating')
      relayReceiver.cleanup()
      relayReceiver = null
    }
    if (!relayReceiver) {
      // 检查缓冲区中第一帧的 codec 类型，选择正确的接收器
      const firstFrame = relayDataBuffer[0]
      const firstCodec = firstFrame?.codec || ''
      const isWebCodecsFrame = firstCodec === 'vp8' || firstCodec === 'opus-audio' || firstCodec === 'aac-audio' || firstCodec === 'webm-audio'
      const isMediaRecorderContainer = firstCodec && firstCodec.startsWith('video/webm')

      if (isWebCodecsFrame && isWebCodecsSupported()) {
        relayReceiver = createWebCodecsReceiver()
      } else if (isMediaRecorderContainer || !isWebCodecsSupported()) {
        relayReceiver = createRelayReceiver()
      } else {
        relayReceiver = isWebCodecsSupported() ? createWebCodecsReceiver() : createRelayReceiver()
      }
    }
    const buffered = [...relayDataBuffer]
    relayDataBuffer.length = 0
    buffered.forEach(item => relayReceiver.handleData(item, videoEl))
  }
}
let peerConnection = null // RTCPeerConnection 实例
let relayReceiver = null // 独立的 relay 接收器实例（每次视频通话创建新实例）
let audioReceiver = null // 独立的音频接收器实例（与视频分离）
let relayDrainTimer = null // 缓冲区排空定时器

/** 启动定时轮询：当有缓存数据且 videoEl 未就绪时，持续尝试排空 */
const startRelayDrainPolling = () => {
  if (relayDrainTimer) return // 已在轮询
  relayDrainTimer = setInterval(() => {
    if (relayDataBuffer.length === 0 || !videoInviteAccepted) {
      clearInterval(relayDrainTimer)
      relayDrainTimer = null
      return
    }
    const rawRef = videoCallRef.value?.remoteVideoEl
    const videoEl = rawRef?.value ?? rawRef
    if (videoEl && videoEl instanceof HTMLElement) {
      tryDrainRelayBuffer()
      clearInterval(relayDrainTimer)
      relayDrainTimer = null
    }
  }, 100)
}

// videoEl 就绪后尝试清空缓存的 relay 数据
watch(videoCallRef, async (comp) => {
  if (!comp) return
  for (let i = 0; i < 30; i++) {
    await nextTick()
    const rawRef = comp.remoteVideoEl
    const videoEl = rawRef?.value ?? rawRef
    if (videoEl && videoEl instanceof HTMLElement) {
      tryDrainRelayBuffer()
      return
    }
    await new Promise(r => setTimeout(r, 100))
  }
  console.error('[Relay] ❌ remoteVideoEl not found after 3s')
})

// 监听 videoCallVisible 变化，当视频浮层显示时等待 remoteVideoEl 就绪并清空缓存
watch(videoCallVisible, async (visible) => {
  if (!visible) return
  // 等待 VideoCall 组件挂载并渲染 video 元素
  for (let i = 0; i < 30; i++) {
    await nextTick()
    const comp = videoCallRef.value
    if (!comp) continue
    const rawRef = comp.remoteVideoEl
    const videoEl = rawRef?.value ?? rawRef
    if (videoEl && videoEl instanceof HTMLElement) {
      tryDrainRelayBuffer()
      return
    }
    await new Promise(r => setTimeout(r, 100))
  }
  console.error('[Relay] ❌ videoCallVisible watcher: remoteVideoEl not found after 3s')
})

// 监听通话状态：连接成功时记录开始时间
let prevVideoCallStatus = ''
watch(videoCallStatus, (newVal) => {
  // 状态从非空变为空 = 通话连接成功
  if (prevVideoCallStatus && !newVal) {
    videoCallStartTime = Date.now()
    isRinging.value = false // 通话接通，停止铃声
  }
  prevVideoCallStatus = newVal
})

let videoJobId = null // 发起视频面试时关联的职位ID
let localStreamPromise = null // 防止 startLocalVideo 重复调用
let videoAutoAccepted = false // 防止自动接受和手动接受重复触发
let videoAcceptHandled = false // 防止 handleVideoAccept 重复触发（WebSocket 重连重发）

// HR 负责岗位抽屉
const hrJobsDrawerVisible = ref(false)
const hrJobsDrawerTitle = ref('')
const hrJobsList = ref([])
const hrJobsLoading = ref(false)

const openHrJobs = async (hrUserId) => {
  if (!hrUserId) return
  hrJobsDrawerTitle.value = '该HR负责的岗位'
  hrJobsDrawerVisible.value = true
  hrJobsLoading.value = true
  try {
    const res = await searchJobsByES({ hrUserId, size: 50 })
    hrJobsList.value = res?.data?.records || []
  } catch {
    hrJobsList.value = []
  } finally {
    hrJobsLoading.value = false
  }
}

// 求职者信息抽屉
const seekerInfoDrawerVisible = ref(false)
const seekerInfoLoading = ref(false)
const seekerInfo = ref(null)

const handleAvatarClick = async () => {
  if (!selectedUserId.value) return
  if (isHR.value) {
    // HR 点击对方头像 → 展示求职者信息
    seekerInfoDrawerVisible.value = true
    seekerInfoLoading.value = true
    try {
      const res = await getUserInfo(selectedUserId.value)
      seekerInfo.value = res?.data || null
    } catch {
      seekerInfo.value = null
    } finally {
      seekerInfoLoading.value = false
    }
  } else {
    // 求职者点击对方头像 → 展示 HR 负责的岗位
    openHrJobs(selectedUserId.value)
  }
}

const selectedConversation = computed(() => {
  return conversations.value.find(c => c.userId === selectedUserId.value)
})

const filteredConversations = computed(() => {
  if (!searchKeyword.value) return conversations.value
  const keyword = searchKeyword.value.toLowerCase()
  return conversations.value.filter(c =>
    c.nickname?.toLowerCase().includes(keyword) ||
    c.username?.toLowerCase().includes(keyword)
  )
})

/** 生成会话ID（与后端逻辑一致：小ID在前） */
const genConversationId = (userId1, userId2) => {
  return userId1 < userId2 ? `${userId1}_${userId2}` : `${userId2}_${userId1}`
}

/** 建立 WebSocket 连接 */
const connectWebSocket = (conversationId) => {
  disconnectWebSocket()
  currentConversationId = conversationId

  const token = localStorage.getItem('token')
  const protocol = location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsHost = import.meta.env.VITE_WS_HOST || location.host
  const wsUrl = `${protocol}//${wsHost}/ws/chat?token=${token}&conversationId=${conversationId}`
  console.log('[WS] 正在连接:', wsUrl.replace(/token=[^&]+/, 'token=***'))
  ws = new WebSocket(wsUrl)
  ws.binaryType = 'arraybuffer' // 接收二进制帧

  ws.onmessage = (event) => {
    try {
      // 二进制帧：视频中继数据（跳过 JSON 解析）
      if (event.data instanceof ArrayBuffer) {
        handleBinaryVideoFrame(event.data)
        return
      }

      const data = JSON.parse(event.data)
      switch (data.type) {
        case 'history': {
          // 按唯一ID去重
          messages.value = (data.messages || []).filter((m, i, arr) => {
            const mid = m.id || m._id
            return arr.findIndex(x => (x.id || x._id) === mid) === i
          })
          loadResumesForMessages(messages.value)
          nextTick(scrollToBottom)
          // 检查最新一条消息是否是未响应的 video_invite
          if (!route.query.autoAcceptVideo && !videoAutoAccepted && messages.value.length > 0) {
            const lastMsg = messages.value[messages.value.length - 1]
            if (lastMsg.type === 'video_invite' && lastMsg.senderId !== currentUserId.value && !messageStore.videoInviteHandledByWs.value) {
              // 检查后续是否有 accept/reject/hangup 响应（说明邀约已被处理过）
              const inviteIdx = messages.value.length - 1
              const hasResponse = messages.value.slice(inviteIdx + 1).some(m =>
                m.type === 'video_accept' || m.type === 'video_reject' || m.type === 'video_hangup'
                || (m.type === 'text' && m.content && m.content.includes('视频面试通话结束'))
              )
              if (!hasResponse) {
                messageStore.videoInviteHandledByWs.value = true
                handleIncomingVideoInvite({ senderId: lastMsg.senderId, jobId: lastMsg.jobId })
              } else {
                // 邀约已处理，标记为已读清除未读数
                wsSend({ type: 'read' })
              }
            }
          }
          break
        }
        case 'message': {
          const msg = data.data
          const msgId = msg.id || msg._id
          // 移除匹配的乐观更新临时消息
          messages.value = messages.value.filter(m => {
            const isTemp = String(m.id || m._id).startsWith('temp_')
            return !(isTemp && m.content === msg.content && m.senderId === msg.senderId)
          })
          // ID去重：已有相同真实ID则跳过
          if (messages.value.some(m => (m.id || m._id) === msgId)) {
            break
          }
          messages.value.push(msg)
          if (msg.type === 'resume' && msg.resumeId) loadResumeDetail(msg.resumeId)
          nextTick(scrollToBottom)
          if (msg.senderId !== currentUserId.value && msgId) {
            wsSend({ type: 'ack', messageId: msgId })
          }
          break
        }
        case 'ack':
          // 消息送达确认：替换临时 ID 为真实 MongoDB ID + 取消超时
          handleAck(data.messageId, data.tempId)
          break
        case 'ack_timeout':
          // 消息投递超时
          const timeoutMsg = data.data
          const failedMsg = messages.value.find(m => m.id === timeoutMsg.id)
          if (failedMsg) {
            failedMsg.status = 'failed'
          }
          break
        case 'read':
          // 对方已读（可选：显示已读标记）
          break
        // ====== 视频面试信令 ======
        case 'video_invite':
          messageStore.videoInviteHandledByWs.value = true
          handleIncomingVideoInvite(data)
          break
        case 'video_accept':
          handleVideoAccept(data)
          break
        case 'video_reject':
          handleVideoReject(data)
          break
        case 'video_hangup':
          handleVideoHangup(data)
          break
        case 'video_offer':
          handleVideoOffer(data)
          break
        case 'video_answer':
          handleVideoAnswer(data)
          break
        case 'video_ice':
          handleVideoIce(data)
          break
        case 'video_media':
          // 旧版文本帧兼容（已改为二进制帧，此分支不再触发）
          break
        case 'error':
          ElMessage.error(data.message)
          break
      }
    } catch (e) {
      console.error('解析WebSocket消息失败', e)
    }
  }

  ws.onopen = () => {
    console.log('[WS] 连接成功, conversationId=', conversationId)
    messageStore.wsConnected.value = true

    // 重连后自动重发队列中的离线消息
    flushPendingQueue()

    // 从首页跳转过来的自动接受/拒绝视频邀请
    if (route.query.autoAcceptVideo === '1') {
      handleAutoAcceptVideo()
    } else if (route.query.autoRejectVideo === '1') {
      handleAutoRejectVideo()
    }
  }

  ws.onerror = (err) => {
    console.error('[WS] 连接错误', err)
  }

  ws.onclose = (event) => {
    console.warn('[WS] 连接关闭 code=', event.code, 'reason=', event.reason)
    ws = null
    messageStore.wsConnected.value = false
  }
}

/** 断开 WebSocket */
const disconnectWebSocket = () => {
  if (ws) {
    ws.close()
    ws = null
  }
  messageStore.wsConnected.value = false
  currentConversationId = null
}

/** 通过 WebSocket 发送消息 —— 拒绝空内容，防止前端bug导致空消息轰炸 */
const wsSend = (data) => {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    // 任何消息在 WebSocket 未就绪时都打印警告，方便排查
    console.warn(`[wsSend] 消息丢弃！ws=${!!ws} readyState=${ws?.readyState} type=${data.type}`)
    return
  }
  // 文字/简历消息必须有非空content
  if ((data.type === 'text' || data.type === 'resume') && (!data.content || !data.content.trim())) {
    console.warn('[wsSend] 拒绝空消息', data)
    return
  }
  ws.send(JSON.stringify(data))
}

/** 发送二进制帧（视频中继专用） */
const wsSendBinary = (buffer) => {
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    console.warn('[wsSendBinary] WebSocket 未就绪，丢弃二进制帧')
    return
  }
  ws.send(buffer)
}

/** 重连后重发队列中的离线消息 */
const flushPendingQueue = () => {
  const queue = getPendingQueue()
  if (queue.length === 0) return

  const MAX_RETRY = 3
  const now = Date.now()
  const pending = []

  for (const msg of queue) {
    // 超过 24 小时的消息直接丢弃
    if (now - msg._queuedAt > 24 * 60 * 60 * 1000) continue
    // 超过最大重试次数直接丢弃
    if ((msg._retryCount || 0) >= MAX_RETRY) {
      // 标记本地消息为失败
      const localMsg = messages.value.find(m => m.id === msg.tempId)
      if (localMsg) localMsg.status = 'failed'
      continue
    }

    // 重新发送
    if (msg.type === 'text') {
      wsSend({ type: 'text', content: msg.content, tempId: msg.tempId })
    } else if (msg.type === 'resume') {
      wsSend({ type: 'resume', resumeId: msg.resumeId, content: msg.content, tempId: msg.tempId })
    }
    startAckTimeout(msg.tempId)
    pending.push({ ...msg, _retryCount: (msg._retryCount || 0) + 1 })
  }

  savePendingQueue(pending)
  if (pending.length > 0) {
    console.log(`[WS] 重连后重发 ${pending.length} 条离线消息`)
  }
}

/**
 * 处理收到的二进制视频帧
 * 协议：[1B flags] [2B codec长度] [NB codec] [8B senderId] [剩余: 视频数据]
 */
const handleBinaryVideoFrame = (buffer) => {
  const view = new DataView(buffer)
  const totalSize = buffer.byteLength
  if (totalSize < 11) return // 头部至少 1+2+8 = 11B

  const flags = view.getUint8(0)
  const isInit = (flags & 1) === 1
  const codecLen = view.getUint16(1, false) // big-endian
  if (totalSize < 11 + codecLen) return

  const codecBytes = new Uint8Array(buffer, 3, codecLen)
  const codec = new TextDecoder().decode(codecBytes)
  const senderIdHigh = view.getUint32(3 + codecLen, false)
  const senderIdLow = view.getUint32(3 + codecLen + 4, false)
  const senderId = senderIdHigh * 0x100000000 + senderIdLow
  const videoData = new Uint8Array(buffer, 3 + codecLen + 8)

  if (senderId === currentUserId.value || videoCallEnded) return

  const relayData = { init: isInit, codec, data: videoData }

  if (videoInviteAccepted) {
    // 音频帧路由到独立的 audioReceiver
    if (codec === 'opus-audio' || codec === 'aac-audio' || codec === 'webm-audio') {
      if (!audioReceiver) audioReceiver = createAudioReceiver()
      audioReceiver.handleData(relayData)
      return
    }

    // 视频帧路由到 relayReceiver（WebCodecs 或 MediaRecorder）
    const rawRef = videoCallRef.value?.remoteVideoEl
    const videoEl = rawRef?.value ?? rawRef
    if (videoEl && videoEl instanceof HTMLElement) {
      // 检测接收器是否已失败，失败则清理并重新创建
      if (relayReceiver && relayReceiver.failed) {
        relayReceiver.cleanup()
        relayReceiver = null
      }
      if (!relayReceiver) {
        const isWebCodecsFrame = codec === 'vp8'
        const isMediaRecorderContainer = codec && codec.startsWith('video/webm')

        if (isWebCodecsFrame && isWebCodecsSupported()) {
          relayReceiver = createWebCodecsReceiver()
        } else if (isMediaRecorderContainer || !isWebCodecsSupported()) {
          relayReceiver = createRelayReceiver()
        } else {
          relayReceiver = isWebCodecsSupported() ? createWebCodecsReceiver() : createRelayReceiver()
        }
      }
      // 先清空缓存
      if (relayDataBuffer.length > 0) {
        const buffered = [...relayDataBuffer]
        relayDataBuffer.length = 0
        buffered.forEach(item => relayReceiver.handleData(item, videoEl))
      }
      relayReceiver.handleData(relayData, videoEl)
      return
    }
  }
  relayDataBuffer.push(relayData)
  // 启动轮询，等 remoteVideoEl 就绪后自动排空
  startRelayDrainPolling()
}

/** 选择会话 */
const selectConversation = async (conv) => {
  selectedUserId.value = conv.userId
  conv.unreadCount = 0
  chatJob.value = null
  showChat.value = true // 移动端切换到聊天视图

  // 加载关联岗位详情
  if (conv.jobId) {
    loadChatJob(conv.jobId)
  }

  // 建立 WebSocket 连接（会自动加载历史消息）
  const conversationId = genConversationId(currentUserId.value, conv.userId)
  connectWebSocket(conversationId)

  // 标记已读（MySQL 通知表）
  await markAsRead(conv.userId)
  messageStore.fetchUnreadCount()
}

/** 移动端返回会话列表 */
const backToConversations = () => {
  showChat.value = false
}

/** 加载聊天关联的岗位详情（含公司信息） */
const loadChatJob = async (jobId) => {
  if (!jobId) { chatJob.value = null; return }
  try {
    const res = await getJobDetail(jobId)
    if (res?.data) {
      const job = res.data
      // 补充公司信息（getJobDetail 不返回嵌套 company）
      if (job.companyId) {
        try {
          const companyRes = await getCompanyDetail(job.companyId)
          if (companyRes?.data) job.company = companyRes.data
        } catch {}
      }
      chatJob.value = job
    }
  } catch {
    chatJob.value = null
  }
}

/** 发送文字消息 */
// ==================== STT 语音输入 ====================

/** 停止录音并识别 */
const stopRecordingAndRecognize = async () => {
  isRecording.value = false
  sttLoading.value = true
  try {
    const wavBlob = await recorderInstance.stop()
    const res = await recognizeSpeech(wavBlob)
    newMessage.value = res.data
  } catch (err) {
    ElMessage.error('语音识别失败: ' + (err.response?.data?.message || err.message))
  } finally {
    sttLoading.value = false
  }
}

/** 开始录音（同步获取麦克风，兼容 iOS） */
const startRecordingSync = async (stream) => {
  try {
    recorderInstance = await startRecording(stream)
    isRecording.value = true
  } catch (err) {
    stream.getTracks().forEach(t => t.stop())
    ElMessage.error('录音启动失败: ' + err.message)
  }
}

/** 点击语音按钮：停止录音 或 开始录音 */
const toggleVoiceInput = () => {
  if (isRecording.value) {
    stopRecordingAndRecognize()
  } else {
    // iOS 关键：必须在同步点击上下文中调用 getUserMedia
    navigator.mediaDevices.getUserMedia({ audio: true })
      .then(stream => startRecordingSync(stream))
      .catch(err => ElMessage.error('无法访问麦克风: ' + err.message))
  }
}

// 防重复发送机制 — 内容指纹去重，从根本上杜绝重复
const isSending = ref(false)
let lastSentContent = ''       // 上次发送的内容
let lastSentTime = 0           // 上次发送的时间戳
let sendCount = 0              // 3秒窗口内的发送次数
let sendWindowStart = 0        // 窗口起始时间

/** 回车发送（过滤中文输入法组合输入） */
const handleEnterSend = (e) => {
  if (e.isComposing) return
  sendTextMessage()
}

const sendTextMessage = () => {
  if (isSending.value) return

  // 立刻快照输入框内容，防止后续被清空
  const snapshot = newMessage.value
  const content = snapshot.trim()
  if (!content || !selectedUserId.value) return

  const now = Date.now()

  // 核心防重：相同内容 3 秒内绝不重复发送
  if (content === lastSentContent && now - lastSentTime < 3000) {
    return
  }

  // 频率检测：3秒内发送达到3条 → 锁定5秒
  if (now - sendWindowStart > 3000) {
    sendCount = 0
    sendWindowStart = now
  }
  sendCount++
  if (sendCount >= 3) {
    isSending.value = true
    ElMessage.warning('发送过于频繁，请稍后再试')
    setTimeout(() => { isSending.value = false }, 5000)
    return
  }

  // 先锁住再发送
  isSending.value = true
  lastSentContent = content
  lastSentTime = now
  newMessage.value = ''

  // 乐观更新：先本地显示消息
  const tempId = 'temp_' + Date.now()
  const localMsg = {
    _id: tempId, id: tempId,
    senderId: currentUserId.value,
    content, type: 'text',
    status: 'sending',
    createdAt: new Date().toISOString()
  }
  messages.value.push(localMsg)
  nextTick(scrollToBottom)

  if (ws && ws.readyState === WebSocket.OPEN) {
    // 在线：直接发送，携带 tempId 用于 ACK 匹配
    wsSend({ type: 'text', content, tempId })
  } else {
    // 离线：存入 localStorage 队列，重连后自动重发
    addToPendingQueue({ tempId, type: 'text', content, conversationId: currentConversationId })
    console.log('[SEND] WebSocket 未连接，消息已入队列')
  }

  // ACK 超时检测：5 秒内未收到 ACK 则标记失败
  startAckTimeout(tempId)

  // 200ms后解锁
  setTimeout(() => { isSending.value = false }, 200)
}

/** 启动 ACK 超时检测 */
const startAckTimeout = (tempId) => {
  const timer = setTimeout(() => {
    const pending = ackPendingMap.get(tempId)
    if (pending) {
      // 标记消息为发送失败
      const msg = messages.value.find(m => m.id === tempId)
      if (msg) msg.status = 'failed'
      ackPendingMap.delete(tempId)
      removeFromPendingQueue(tempId)
      console.warn(`[ACK] 超时未收到确认: tempId=${tempId}`)
    }
  }, ACK_TIMEOUT)
  ackPendingMap.set(tempId, { timer })
}

/** 收到 ACK 时调用：替换临时 ID + 取消超时 */
const handleAck = (messageId, tempId) => {
  if (tempId && ackPendingMap.has(tempId)) {
    clearTimeout(ackPendingMap.get(tempId).timer)
    ackPendingMap.delete(tempId)
    removeFromPendingQueue(tempId)
    // 替换临时 ID 为真实 MongoDB ID
    const msg = messages.value.find(m => m.id === tempId)
    if (msg) {
      msg.id = messageId
      msg._id = messageId
      msg.status = 'sent'
    }
  }
}

/** 插入 emoji 到输入框 */
const insertEmoji = (emoji) => {
  newMessage.value += emoji
  showEmojiPicker.value = false
}

/** 格式化薪资 */
const formatSalary = (min, max) => {
  if (min && max) return `${(min / 1000).toFixed(0)}K-${(max / 1000).toFixed(0)}K`
  if (min) return `${(min / 1000).toFixed(0)}K起`
  if (max) return `最高${(max / 1000).toFixed(0)}K`
  return '面议'
}

/** 点击外部关闭表情面板 */
const handleClickOutside = () => {
  showEmojiPicker.value = false
}

/** 去除HTML标签 */
const stripHtml = (html) => {
  if (!html) return ''
  return html.replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').trim()
}

/** 发送简历附件 */
const handleSendResume = async (resumeId) => {
  if (!selectedUserId.value) return

  const resume = myResumes.value.find(r => r.id === resumeId)
  const resumeName = resume?.fileName || '简历'
  const tempId = 'temp_' + Date.now()

  // 乐观更新：先在本地显示简历消息
  messages.value.push({
    _id: tempId, id: tempId,
    senderId: currentUserId.value,
    receiverId: selectedUserId.value,
    type: 'resume', resumeId, resumeName,
    content: '这是我的简历，请查看',
    status: 'sending',
    createdAt: new Date().toISOString()
  })
  nextTick(scrollToBottom)

  if (ws && ws.readyState === WebSocket.OPEN) {
    wsSend({ type: 'resume', resumeId, content: '这是我的简历，请查看', tempId })
  } else {
    addToPendingQueue({ tempId, type: 'resume', resumeId, content: '这是我的简历，请查看', conversationId: currentConversationId })
  }

  startAckTimeout(tempId)
}

/** 查看完整简历 */
const viewResume = async (resumeId) => {
  let resume = resumeCache.value[resumeId]
  if (!resume) {
    try {
      const res = await getResume(resumeId)
      if (res?.data) {
        resume = res.data
        resumeCache.value[resumeId] = resume
      }
    } catch {
      ElMessage.error('获取简历信息失败')
      return
    }
  }
  if (resume?.fileUrl) {
    window.open(resume.fileUrl, '_blank')
  } else {
    ElMessage.info('简历文件暂无下载链接')
  }
}

/** 批量加载消息中关联的简历详情 */
const loadResumesForMessages = async (msgs) => {
  const resumeIds = msgs
    .filter(m => m.type === 'resume' && m.resumeId && !resumeCache.value[m.resumeId])
    .map(m => m.resumeId)

  for (const resumeId of resumeIds) {
    await loadResumeDetail(resumeId)
  }
}

/** 加载单个简历详情 */
const loadResumeDetail = async (resumeId) => {
  if (resumeCache.value[resumeId]) return
  try {
    const res = await getResume(resumeId)
    if (res?.data) {
      resumeCache.value[resumeId] = res.data
    }
  } catch {
    // 静默失败
  }
}

/** 加载当前用户的简历列表（用于发送简历附件） */
const loadMyResumes = async () => {
  if (!user.value?.id) return
  try {
    const idRes = await getUserResumes(user.value.id)
    const resumeIds = idRes.data || []
    const detailPromises = resumeIds.map(id => getResume(id).catch(() => null))
    const results = await Promise.all(detailPromises)
    myResumes.value = results.filter(r => r?.data).map(r => r.data)
  } catch {
    myResumes.value = []
  }
}

/** 岗位投递 */
const handleApplyJob = async () => {
  if (!isLoggedIn.value) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return
  }

  const jobId = route.query.jobId || selectedConversation.value?.jobId
  if (!jobId) {
    ElMessage.warning('无法确定关联职位，请从职位详情页发起投递')
    return
  }

  try {
    const resumeRes = await getUserResumes(user.value.id)
    const resumeIds = resumeRes.data || []
    if (resumeIds.length === 0) {
      ElMessageBox.confirm('您还没有简历，请先上传简历', '提示', {
        confirmButtonText: '去上传',
        showCancelButton: false,
        type: 'info'
      }).then(() => router.push('/user'))
      return
    }

    await applyJob({ jobId: Number(jobId), resumeId: resumeIds[0] })
    ElMessage.success('投递成功')
  } catch (e) {
    ElMessage.error(e.response?.data?.message || '投递失败')
  }
}

// ==================== 视频面试（原生 WebRTC + WebSocket 信令） ====================

/**
 * 创建 PeerConnection 并绑定回调
 * @param {boolean} isInitiator - 是否为发起方（决定谁创建 offer）
 */
let iceRestartCount = 0 // ICE 重启次数（最多尝试 2 次）
const MAX_ICE_RESTART = 2
let connectTimeoutTimer = null // P2P 连接超时定时器（需在连接成功后取消）

const initPeerConnection = async (isInitiator) => {
  if (peerConnection) return
  iceRestartCount = 0
  peerConnection = createPeerConnection({
    onRemoteStream: (stream) => {
      // 关键修复：已切换到 Relay 模式时，忽略迟到的 P2P 流，防止两路音频同时播放
      if (relayMode.value) return
      remoteStream.value = stream
      videoCallStatus.value = ''
    },
    onIceCandidate: (candidate) => {
      wsSend({ type: 'video_ice', candidate: candidate.toJSON() })
    },
    onConnected: () => {
      // 关键修复：P2P 连接成功，取消超时定时器，防止误切到 Relay 模式
      if (connectTimeoutTimer) {
        clearTimeout(connectTimeoutTimer)
        connectTimeoutTimer = null
      }
      videoCallStatus.value = ''
      iceRestartCount = 0
    },
    onDisconnected: () => {
      if (peerConnection && iceRestartCount < MAX_ICE_RESTART) {
        iceRestartCount++
        videoCallStatus.value = `连接中断，正在尝试恢复 (${iceRestartCount}/${MAX_ICE_RESTART})...`
        restartIce(peerConnection).then(offer => {
          wsSend({ type: 'video_offer', sdp: offer })
        }).catch(() => {
          switchToRelayMode()
        })
      } else {
        switchToRelayMode()
      }
    },
    onError: () => {}
  })

  // 连接超时检测：3 秒内未 connected 则自动切换到 relay 模式
  connectTimeoutTimer = setTimeout(() => {
    connectTimeoutTimer = null
    if (peerConnection && peerConnection.iceConnectionState !== 'connected' && peerConnection.iceConnectionState !== 'completed') {
      switchToRelayMode()
    }
  }, 3000)

  if (isInitiator) {
    await startLocalVideo()
    videoCallStatus.value = '正在建立连接...'
    videoCallVisible.value = true
    const offer = await createOffer(peerConnection, localStream.value)
    wsSend({ type: 'video_offer', sdp: offer })
  }
}

/** 切换到 WebSocket Media Relay 模式（P2P 失败时的回退） */
/** 启动视频发送（使用 MediaRecorder，包含音频） */
const startVideoSender = () => {
  if (!localStream.value) {
    console.error('[Relay] localStream is null, cannot start sender')
    return
  }
  try {
    // 优先使用 WebCodecs（视频+音频），降级到 MediaRecorder
    if (isWebCodecsSupported()) {
      startWebCodecsSender(localStream.value, wsSendBinary, currentUserId.value, {
        onError: () => {
          // WebCodecs 失败，降级到 MediaRecorder（视频+音频）
          console.log('[Relay] WebCodecs failed, falling back to MediaRecorder')
          startMediaRelay(localStream.value, wsSend, { sendBinary: wsSendBinary, senderId: currentUserId.value })
        }
      })
    } else {
      startMediaRelay(localStream.value, wsSend, { sendBinary: wsSendBinary, senderId: currentUserId.value })
    }
  } catch (e) {
    console.error('[Relay] startVideoSender failed:', e.message)
  }
}

const switchToRelayMode = () => {
  if (relayMode.value) return
  relayMode.value = true

  // 关闭 PeerConnection 释放资源（不再需要 ICE 候选）
  if (peerConnection) {
    try { peerConnection.close() } catch {}
    peerConnection = null
  }

  // 关键修复：清空 remoteStream，防止 P2P 音频与 Relay 音频同时播放导致声音重复
  remoteStream.value = null

  startVideoSender()
}

/** 从首页跳转过来，自动接受视频邀请 — 直接进入 relay 模式 */
const handleAutoAcceptVideo = async () => {
  // 先保存参数（router.replace 会清掉 query）
  const hrUserId = route.query.hrUserId
  const jobTitle = route.query.jobTitle
  router.replace({ path: '/messages', query: { hrUserId, jobTitle } })
  videoAutoAccepted = true
  // 重置邀约弹窗去重标记，否则后续新邀约会被拦截
  messageStore.resetVideoInvitePopup()
  // 注意：音频预热已在 AppShell.vue 的用户手势中完成（SSE 通知弹窗点击"接受"时）
  // 此处不在用户手势上下文中，warmupAudioElement() 会静默失败，不重复调用
  videoInviteAccepted = true

  // 等 WebSocket 就绪再发 accept
  if (!ws || ws.readyState !== WebSocket.OPEN) {
    console.warn('[Relay] ws not ready, waiting...')
    await new Promise((resolve) => {
      const check = setInterval(() => {
        if (ws && ws.readyState === WebSocket.OPEN) {
          clearInterval(check)
          resolve()
        }
      }, 100)
      // 超时 3 秒放弃
      setTimeout(() => { clearInterval(check); resolve() }, 3000)
    })
  }
  wsSend({ type: 'video_accept' })
  try {
    await startLocalVideo()
    relayMode.value = true
    videoCallVisible.value = true
    startVideoSender()
  } catch (e) {
    console.error('[Relay] auto-accept video failed:', e)
    ElMessage.error('无法启动摄像头，请检查设备权限')
    cleanupVideoCall()
  }
}

/** 从首页跳转过来，自动拒绝视频邀请 */
const handleAutoRejectVideo = () => {
  router.replace({ path: '/messages', query: { hrUserId: route.query.hrUserId, jobTitle: route.query.jobTitle } })
  wsSend({ type: 'video_reject' })
  messageStore.resetVideoInvitePopup()
  ElMessage.info('已拒绝视频面试邀请')
}

/** HR发起视频面试 */
const handleVideoInterview = async () => {
  if (!selectedUserId.value) {
    ElMessage.warning('请先选择一个会话')
    return
  }
  if (videoCallVisible.value || videoInvitePending.value) return
  videoInvitePending.value = true
  videoCallEnded = false
  videoInviteAccepted = false
  videoJobId = selectedConversation.value?.jobId || null

  // 尝试获取摄像头，最多重试 2 次
  let stream = null
  for (let attempt = 0; attempt < 2; attempt++) {
    try {
      stream = await startLocalVideo()
      break
    } catch (e) {
      if (attempt === 0) {
        const retry = await ElMessageBox.confirm(
          '摄像头访问失败，请检查权限后重试',
          '摄像头错误',
          { confirmButtonText: '重试', cancelButtonText: '取消', type: 'warning' }
        ).catch(() => null)
        if (!retry) {
          videoInvitePending.value = false
          return
        }
      } else {
        ElMessage.error('无法启动摄像头，请检查设备权限')
        cleanupVideoCall()
        videoInvitePending.value = false
        return
      }
    }
  }

  if (stream) {
    relayMode.value = true
    videoCallVisible.value = true
    isRinging.value = true // HR端播放拨号铃声
    videoCallStatus.value = '正在呼叫中...' // 显示拨打动画
    wsSend({ type: 'video_invite', jobId: videoJobId })
    ElMessage.info('已发送视频面试邀请，等待对方接受')
  }
  videoInvitePending.value = false
}

/** 收到视频面试邀请（求职者端） */
const handleIncomingVideoInvite = async (data) => {
  if (data.senderId === currentUserId.value) return
  if (videoAutoAccepted) return
  // 防重入：已经在视频通话中，不再弹邀请
  if (videoCallVisible.value) return
  videoCallEnded = false
  videoJobId = data.jobId || null

  // 使用自定义弹窗（iOS 兼容：用户手势中同步获取摄像头）
  dialogRingtonePlayed = false
  const result = await showVideoInviteConfirm()
  stopRingtone()

  if (!result.accepted) {
    wsSend({ type: 'video_reject' })
    return
  }

  // 摄像头已在 onVideoInviteAccept 中获取并设置到 localStream
  wsSend({ type: 'video_accept' })
  videoInviteAccepted = true

  // 如果 onVideoInviteAccept 已成功获取流
  if (localStream.value) {
    relayMode.value = true
    videoCallVisible.value = true
    startVideoSender()
    return
  }

  // 兜底：如果流获取失败，尝试重试
  try {
    await startLocalVideo()
    relayMode.value = true
    videoCallVisible.value = true
    startVideoSender()
  } catch (e) {
    ElMessage.error('无法启动摄像头：' + (e.message || '未知错误'))
    wsSend({ type: 'video_reject' })
    videoInviteAccepted = false
    cleanupVideoCall()
  }
}

/** 对方接受了视频面试（HR端）— 发起方已在发邀请时启动 relay，这里只需确认并发送媒体 */
const handleVideoAccept = async (data) => {
  if (data.senderId === currentUserId.value) return
  if (videoAcceptHandled) return
  videoAcceptHandled = true
  videoInviteAccepted = true
  isRinging.value = false // 对方已接受，停止铃声
  videoCallStatus.value = '' // 清除呼叫状态，显示通话界面
  ElMessage.success('对方已接受视频面试')
  startVideoSender()
  for (let i = 0; i < 30; i++) {
    await nextTick()
    const rawRef = videoCallRef.value?.remoteVideoEl
    const el = rawRef?.value ?? rawRef
    if (el && el instanceof HTMLElement) {
      tryDrainRelayBuffer()
      return
    }
    await new Promise(r => setTimeout(r, 100))
  }
  console.error('[Relay] remoteVideoEl not found after 3s')
}

/** 收到 SDP Offer（relay 模式下忽略，P2P 回退时仍可用） */
const handleVideoOffer = async (data) => {
  if (data.senderId === currentUserId.value) return
  if (relayMode.value) return
  // ICE 重启：peerConnection 已存在时收到新 offer，说明是对方触发的 ICE restart
  if (peerConnection) {
    try {
      await peerConnection.setRemoteDescription(new RTCSessionDescription(data.sdp))
      const answer = await peerConnection.createAnswer()
      await peerConnection.setLocalDescription(answer)
      wsSend({ type: 'video_answer', sdp: peerConnection.localDescription })
    } catch (e) {
      console.error('[WebRTC] ICE restart failed:', e.message)
    }
    return
  }
  try {
    if (!localStream.value) {
      await startLocalVideo()
    }
    await initPeerConnection(false)
    const answer = await handleOffer(peerConnection, data.sdp, localStream.value)
    wsSend({ type: 'video_answer', sdp: answer })
  } catch (e) {
    console.error('[WebRTC] handleVideoOffer failed:', e.message)
    cleanupVideoCall()
  }
}

/** 收到 SDP Answer（发起方处理，relay 模式下忽略） */
const handleVideoAnswer = async (data) => {
  if (data.senderId === currentUserId.value) return
  if (relayMode.value) return
  try {
    await handleAnswer(peerConnection, data.sdp)
  } catch (e) {
    console.error('[WebRTC] handleVideoAnswer failed:', e.message)
  }
}

/** 收到 ICE 候选（relay 模式下忽略） */
const handleVideoIce = async (data) => {
  if (data.senderId === currentUserId.value) return
  if (relayMode.value) return
  if (data.candidate && peerConnection) {
    await addIceCandidate(peerConnection, data.candidate)
  }
}

/** 对方拒绝了视频面试（HR端） */
const handleVideoReject = (data) => {
  if (data.senderId === currentUserId.value) return
  ElMessage.warning('对方拒绝了视频面试')
  cleanupVideoCall()
}

/** 收到挂断信号 */
const handleVideoHangup = (data) => {
  if (data.senderId === currentUserId.value) return
  ElMessage.info('对方已挂断视频面试')

  // 关闭邀约弹窗（如果还未接受）
  showVideoInviteDialog.value = false
  if (videoInviteResolve) {
    videoInviteResolve({ accepted: false })
    videoInviteResolve = null
  }

  cleanupVideoCall()
}

/** 打开本地摄像头和麦克风（复用 Promise，防止重复调用） */
const startLocalVideo = async () => {
  // 如果已经有流且可用，直接返回
  if (localStream.value && localStream.value.active) {
    return localStream.value
  }
  // 释放之前的流
  if (localStream.value) {
    localStream.value.getTracks().forEach(t => t.stop())
    localStream.value = null
  }
  if (localStreamPromise) return localStreamPromise
  localStreamPromise = (async () => {
    try {
      const stream = await getLocalStream()
      localStream.value = stream
      return stream
    } catch (e) {
      console.error('[WebRTC] startLocalVideo failed:', e.name, e.message)
      ElNotification({
        title: '摄像头访问失败',
        message: e.message || '无法访问摄像头或麦克风',
        type: 'error',
        duration: 8000
      })
      throw e
    } finally {
      localStreamPromise = null
    }
  })()
  return localStreamPromise
}

/** 格式化通话时长 */
const formatCallDuration = (ms) => {
  const secs = Math.floor(ms / 1000)
  const mins = Math.floor(secs / 60)
  const remainSecs = secs % 60
  if (mins > 0) return `${mins}分${remainSecs}秒`
  return `${remainSecs}秒`
}

/** 用户点击挂断 */
const handleHangup = () => {
  // 计算通话时长
  const duration = videoCallStartTime > 0 ? Date.now() - videoCallStartTime : 0
  const durationText = duration > 0 ? formatCallDuration(duration) : null

  wsSend({ type: 'video_hangup', duration: durationText })
  sendVideoCallMessage(durationText)
  cleanupVideoCall()
}

/** 发送视频通话结束消息到聊天 */
const sendVideoCallMessage = (durationText) => {
  const content = durationText
    ? `视频面试通话结束，通话时长 ${durationText}`
    : '视频面试通话结束'
  const localMsg = {
    id: 'temp_video_' + Date.now(),
    senderId: currentUserId.value,
    content,
    type: 'video_call',
    videoCallStatus: 'ended',
    createdAt: new Date().toISOString()
  }
  messages.value.push(localMsg)
  nextTick(scrollToBottom)
  wsSend({ type: 'text', content })
}

/** 切换静音 */
const handleToggleMute = (muted) => {
  if (localStream.value) {
    localStream.value.getAudioTracks().forEach(track => {
      track.enabled = !muted
    })
  }
}

/** 切换摄像头 */
const handleToggleCamera = (cameraOff) => {
  if (localStream.value) {
    localStream.value.getVideoTracks().forEach(track => {
      track.enabled = !cameraOff
    })
  }
}

/** 清理视频通话资源 */
const cleanupVideoCall = () => {
  isRinging.value = false
  stopRingtone()
  // 清理 P2P 超时定时器
  if (connectTimeoutTimer) {
    clearTimeout(connectTimeoutTimer)
    connectTimeoutTimer = null
  }
  closeConnection(peerConnection, localStream.value)
  stopMediaRelay()
  // 清理缓冲区轮询定时器
  if (relayDrainTimer) {
    clearInterval(relayDrainTimer)
    relayDrainTimer = null
  }
  // 清理独立的 relay 接收器实例
  if (relayReceiver) {
    relayReceiver.cleanup()
    relayReceiver = null
  }
  // 清理独立的音频接收器实例
  if (audioReceiver) {
    audioReceiver.cleanup()
    audioReceiver = null
  }
  // 暖场音频元素不应销毁：保持静音循环维持全局音频会话解锁，供下次通话复用
  // 实际音频播放元素（WebCodecs 接收器中创建的）会在 relayReceiver.cleanup() 中清理
  peerConnection = null
  localStream.value = null
  localStreamPromise = null
  videoAutoAccepted = false
  videoAcceptHandled = false
  remoteStream.value = null
  relayMode.value = false
  videoCallVisible.value = false
  relayDataBuffer.length = 0
  videoCallEnded = true
  videoInviteAccepted = false
  videoCallStatus.value = ''
  videoInviteDialogPending = false // 重置弹窗锁，允许下次弹出邀请
  showVideoInviteDialog.value = false // 确保弹窗关闭
  messageStore.videoInviteHandledByWs.value = false
  messageStore.resetVideoInvitePopup()
}

const scrollToBottom = () => {
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
}

/** 重新发送失败的消息 */
/** 重新发送失败的消息 */
const resendMessage = (msg) => {
  const tempId = 'temp_' + Date.now()
  // 替换旧的失败消息为新的临时消息
  const index = messages.value.findIndex(m => m.id === msg.id)
  if (index !== -1) {
    messages.value.splice(index, 1)
  }

  const localMsg = {
    _id: tempId, id: tempId,
    senderId: currentUserId.value,
    content: msg.content, type: msg.type,
    resumeId: msg.resumeId,
    status: 'sending',
    createdAt: new Date().toISOString()
  }
  messages.value.push(localMsg)
  nextTick(scrollToBottom)

  if (ws && ws.readyState === WebSocket.OPEN) {
    if (msg.type === 'resume') {
      wsSend({ type: 'resume', resumeId: msg.resumeId, content: msg.content, tempId })
    } else {
      wsSend({ type: 'text', content: msg.content, tempId })
    }
  } else {
    addToPendingQueue({ tempId, type: msg.type, content: msg.content, resumeId: msg.resumeId, conversationId: currentConversationId })
  }

  startAckTimeout(tempId)
}

const formatTime = (time) => {
  if (!time) return ''
  const date = new Date(time)
  const now = new Date()
  const diff = now - date

  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return `${Math.floor(diff / 60000)}分钟前`
  if (diff < 86400000) return `${Math.floor(diff / 3600000)}小时前`
  if (diff < 604800000) return `${Math.floor(diff / 86400000)}天前`
  return date.toLocaleDateString('zh-CN')
}

onMounted(async () => {
  // 点击外部关闭表情面板
  document.addEventListener('click', handleClickOutside)

  // 会话列表已由 AppShell 登录时预加载，无需重复请求
  // 如果列表为空（比如首次直接访问消息页），补充加载一次
  if (!conversations.value || conversations.value.length === 0) {
    try {
      await messageStore.loadConversations()
    } catch {}
  }

  // 加载当前用户的简历列表
  loadMyResumes()

  // 从职位详情跳转过来时，自动选中HR会话
  const hrUserId = route.query.hrUserId
  const jobId = route.query.jobId ? Number(route.query.jobId) : null
  const jobTitle = route.query.jobTitle || ''
  if (hrUserId) {
    const targetId = Number(hrUserId)
    const existing = conversations.value.find(c => c.userId === targetId)
    if (existing) {
      // 已有会话，检查是否需要切换岗位
      if (jobId && existing.jobId && existing.jobId !== jobId) {
        // 同一HR不同岗位 → 弹窗确认切换
        try {
          await ElMessageBox.confirm(
            `当前会话关联的是「${existing.jobTitle || '其他岗位'}」，是否切换到新岗位继续沟通？`,
            '切换岗位',
            { confirmButtonText: '切换', cancelButtonText: '保留当前', type: 'info' }
          )
          // 用户确认切换 → 更新后端 + 本地
          await updateConversationJob(targetId, jobId)
          existing.jobId = jobId
          existing.jobTitle = jobTitle
        } catch {
          // 用户取消，保留当前岗位
        }
      }
      await selectConversation(existing)
    } else {
      let hrName = 'HR'
      try {
        const userRes = await getUserInfo(targetId)
        if (userRes?.data) {
          hrName = userRes.data.nickname || userRes.data.username || 'HR'
        }
      } catch (e) {
        // 忽略
      }
      const tempConv = {
        userId: targetId,
        nickname: hrName,
        jobId: jobId,
        jobTitle: jobTitle,
        lastMessage: '',
        lastTime: null,
        unreadCount: 0
      }
      conversations.value.unshift(tempConv)
      await selectConversation(tempConv)
    }
  }
})

onUnmounted(() => {
  disconnectWebSocket()
  cleanupVideoCall()
  // 清理所有 ACK 超时定时器
  for (const [, pending] of ackPendingMap) {
    clearTimeout(pending.timer)
  }
  ackPendingMap.clear()
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.message-center {
  min-height: calc(100vh - 64px);
  background: #f5f6f8;
}

.message-center__container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 24px;
}

.message-card :deep(.el-card__body) {
  padding: 0;
  height: calc(100vh - 160px);
}

.card-header {
  padding: 16px 20px;
  border-bottom: 1px solid #e5e7eb;
}

.card-header h2 {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
}

.message-layout {
  display: flex;
  height: 100%;
}

/* 会话列表 */
.conversation-list {
  width: 320px;
  border-right: 1px solid #e5e7eb;
  display: flex;
  flex-direction: column;
}

.conversation-header {
  padding: 12px 16px;
  border-bottom: 1px solid #f1f5f9;
}

.conversations {
  flex: 1;
  overflow-y: auto;
}

.conversation-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  cursor: pointer;
  transition: background 0.2s;
  border-bottom: 1px solid #f8fafc;
}

.conversation-item:hover {
  background: #f8fafc;
}

.conversation-item.active {
  background: #ecfdf5;
  border-left: 3px solid #10b981;
}

.conversation-avatar {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  font-weight: 600;
  flex-shrink: 0;
}

.conversation-info {
  flex: 1;
  min-width: 0;
}

.conversation-name {
  font-size: 14px;
  font-weight: 500;
  color: #0f172a;
  margin-bottom: 4px;
  display: flex;
  align-items: center;
  gap: 4px;
}

.conversation-job {
  font-size: 12px;
  color: #94a3b8;
  font-weight: normal;
}

.conversation-last {
  font-size: 13px;
  color: #64748b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conversation-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 4px;
}

.conversation-time {
  font-size: 11px;
  color: #94a3b8;
}

.conversation-badge {
  background: #dc2626;
  color: #fff;
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 10px;
  min-width: 16px;
  text-align: center;
}

.empty-conversations {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 聊天区域 */
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 12px 20px;
  border-bottom: 1px solid #e5e7eb;
  display: flex;
  align-items: center;
  gap: 10px;
  cursor: pointer;
  transition: background 0.2s;
}

.chat-header:hover {
  background: #f8fafc;
}

.chat-header__avatar {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  font-weight: 600;
  cursor: pointer;
  flex-shrink: 0;
}

.chat-header__avatar:hover {
  opacity: 0.85;
}

/* 返回按钮默认隐藏（桌面端不显示） */
.chat-header__back {
  display: none;
}

.chat-header__info {
  display: flex;
  align-items: baseline;
  gap: 8px;
  min-width: 0;
}

.chat-title {
  font-weight: 600;
  color: #0f172a;
}

.chat-job {
  font-size: 13px;
  color: #64748b;
}

/* 聊天区岗位卡片 */
.chat-job-card {
  display: flex;
  gap: 12px;
  margin: 0 16px 8px;
  padding: 10px 12px;
  background: #fff;
  border-left: 3px solid #10b981;
  border-radius: 6px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
  cursor: pointer;
  transition: all 0.2s;
}

.chat-job-card:hover {
  box-shadow: 0 2px 6px rgba(16, 185, 129, 0.12);
}

.chat-job-card__left {
  flex: 1;
  min-width: 0;
}

.chat-job-card__right {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  width: 80px;
  flex-shrink: 0;
  padding-left: 10px;
  border-left: 1px solid #f0f0f0;
}

.chat-job-card__hr-avatar {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  font-weight: 600;
  font-size: 16px;
}

.chat-job-card__hr-name {
  font-size: 11px;
  color: #0f172a;
  font-weight: 500;
  text-align: center;
  max-width: 72px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.chat-job-card__hr-badge {
  font-size: 10px;
  color: #10b981;
  background: #ecfdf5;
  border: 1px solid #a7f3d0;
  border-radius: 3px;
  padding: 0 6px;
  font-weight: 600;
}

.chat-job-card__company {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 2px;
}

.chat-job-card__company-logo {
  width: 14px;
  height: 14px;
  border-radius: 3px;
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  font-size: 8px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.chat-job-card__company-name {
  font-size: 11px;
  color: #94a3b8;
}

.chat-job-card__title {
  font-size: 13px;
  font-weight: 600;
  color: #0f172a;
}

.chat-job-card__salary {
  font-size: 13px;
  font-weight: 700;
  color: #10b981;
  margin-top: 1px;
}

.chat-job-card__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 4px;
  margin: 4px 0;
}

.chat-job-tag {
  font-size: 11px;
  color: #64748b;
  background: #f8fafc;
  padding: 1px 6px;
  border-radius: 3px;
}

.chat-job-card__desc {
  font-size: 11px;
  color: #94a3b8;
  line-height: 1.5;
  margin-bottom: 4px;
}

.chat-job-card__meta {
  font-size: 11px;
  color: #94a3b8;
  margin-bottom: 4px;
}

.chat-job-card__link {
  font-size: 12px;
  color: #10b981;
  font-weight: 500;
}

.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.message-item {
  display: flex;
  gap: 8px;
  max-width: 70%;
}

.message-self {
  align-self: flex-end;
  flex-direction: row-reverse;
}

.message-avatar {
  background: #e2e8f0;
  color: #64748b;
  font-size: 12px;
  flex-shrink: 0;
}

.message-self .message-avatar {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
}

.message-content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.message-self .message-content {
  align-items: flex-end;
}

.message-bubble {
  background: #f1f5f9;
  padding: 10px 14px;
  border-radius: 12px;
  font-size: 14px;
  line-height: 1.6;
  color: #334155;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
  position: relative;
}

.message-self .message-bubble {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
  box-shadow: 0 1px 3px rgba(16, 185, 129, 0.25);
}

/* 视频通话消息样式 */
.message-bubble--system {
  background: #eff6ff;
  border: 1px solid #bfdbfe;
  text-align: center;
}

.message-self .message-bubble--system {
  background: #eff6ff;
  color: #1e40af;
  box-shadow: 0 1px 3px rgba(59, 130, 246, 0.15);
}

.video-call-msg {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  font-size: 13px;
}

.video-call-icon {
  font-size: 16px;
}

.video-call-text {
  color: #3b82f6;
  font-weight: 500;
}

/* 简历卡片不受自己气泡绿色影响，始终白底 */
.message-self .message-bubble--resume {
  background: #fff;
  color: #334155;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

/* 简历附件卡片 */
.message-bubble--resume {
  background: #fff;
  border: 1px solid #e5e7eb;
  padding: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  overflow: hidden;
  border-radius: 12px;
}

.resume-card {
  min-width: 260px;
  max-width: 320px;
}

.resume-card__header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 14px;
  background: linear-gradient(135deg, #ecfdf5, #d1fae5);
  border-bottom: 1px solid #a7f3d0;
  font-size: 13px;
  font-weight: 600;
  color: #065f46;
}

.resume-card__content {
  padding: 12px 14px;
}

.resume-card__text {
  margin: 0 0 10px;
  font-size: 14px;
  color: #334155;
  line-height: 1.5;
}

.resume-card__info {
  background: #f8fafc;
  border-radius: 8px;
  padding: 10px 12px;
  font-size: 13px;
  color: #475569;
  border: 1px solid #f1f5f9;
}

.resume-card__info p {
  margin: 4px 0;
}

.resume-card__loading {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #94a3b8;
}

.resume-card__footer {
  padding: 8px 14px;
  border-top: 1px solid #f1f5f9;
  background: #fafafa;
}

.message-time {
  font-size: 11px;
  color: #94a3b8;
  margin-top: 2px;
}

/* 消息状态图标 */
.message-status {
  display: inline-flex;
  align-items: center;
  margin-left: 6px;
  font-size: 14px;
  color: #94a3b8;
}

.message-status--failed {
  color: #ef4444;
  cursor: pointer;
}

.message-status--failed:hover {
  color: #dc2626;
}

.chat-input {
  padding: 12px 20px 16px;
  border-top: 1px solid #e5e7eb;
}

.chat-input__actions {
  display: flex;
  gap: 8px;
  margin-bottom: 10px;
}

.chat-input__actions .el-button {
  border-radius: 6px;
}

/* 输入行：表情 + 输入框 + 发送 */
.input-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.input-row .el-input {
  flex: 1;
}

/* 表情选择器 */
.emoji-picker-wrapper {
  position: relative;
  flex-shrink: 0;
}

.emoji-btn {
  font-size: 20px;
  cursor: pointer;
  transition: transform 0.2s;
  user-select: none;
}

.emoji-btn:hover {
  transform: scale(1.2);
}

/* 语音输入按钮 */
.voice-btn {
  width: 32px;
  height: 32px;
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

.voice-btn:hover:not(:disabled) {
  background: #e5e7eb;
  color: #10b981;
}

.voice-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.voice-btn.recording {
  background: #fef2f2;
  color: #ef4444;
  animation: pulse-red 1s infinite;
}

.voice-btn.loading {
  background: #fffbeb;
  color: #f59e0b;
}

.voice-btn .spin {
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

.emoji-panel {
  position: absolute;
  bottom: 36px;
  right: 0;
  width: 320px;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  padding: 12px;
  z-index: 100;
}

.emoji-grid {
  display: grid;
  grid-template-columns: repeat(8, 1fr);
  gap: 4px;
  max-height: 200px;
  overflow-y: auto;
}

.emoji-item {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  font-size: 20px;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.15s;
}

.emoji-item:hover {
  background: #f0fdf4;
  transform: scale(1.2);
}

.empty-chat {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 移动端适配 */
@media (max-width: 768px) {
  .conversation-list {
    width: 100%;
    max-width: 100%;
  }

  .conversation-list--hidden {
    display: none;
  }

  .chat-area {
    display: none;
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    z-index: 150;
    background: #fff;
  }

  .chat-area--mobile-show {
    display: flex;
    flex-direction: column;
  }

  .chat-header__back {
    display: flex;
    font-size: 20px;
    color: #475569;
    cursor: pointer;
    padding: 4px;
    margin-right: 4px;
    border-radius: 6px;
    transition: background 0.2s;
  }

  .chat-header__back:hover {
    background: #f1f5f9;
  }

  .message-card :deep(.el-card__body) {
    height: auto;
    min-height: calc(100vh - 160px);
  }
}

/* HR 负责岗位抽屉 */
.hr-jobs-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  padding: 48px 0;
  color: #94a3b8;
  font-size: 14px;
}

.hr-jobs-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.hr-job-item {
  padding: 16px;
  background: #fafbfc;
  border: 1px solid #e5e7eb;
  border-radius: 10px;
  cursor: pointer;
  transition: all 0.2s;
}

.hr-job-item:hover {
  border-color: #10b981;
  background: #f0fdf4;
}

.hr-job-item.active {
  border-color: #10b981;
  background: #ecfdf5;
}

.hr-job-item__top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 8px;
}

.hr-job-item__title {
  font-size: 15px;
  font-weight: 600;
  color: #0f172a;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hr-job-item__salary {
  font-size: 15px;
  font-weight: 700;
  color: #10b981;
  margin-left: 12px;
  white-space: nowrap;
}

.hr-job-item__tags {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.hr-job-tag {
  font-size: 12px;
  color: #64748b;
  background: #f1f5f9;
  padding: 2px 8px;
  border-radius: 4px;
}

/* 消息头像可点击 */
.message-avatar--clickable {
  cursor: pointer;
  transition: opacity 0.2s;
}

.message-avatar--clickable:hover {
  opacity: 0.75;
}

/* 求职者信息抽屉 */
.seeker-info__header {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-bottom: 20px;
  border-bottom: 1px solid #f0f0f0;
  margin-bottom: 16px;
}

.seeker-info__avatar {
  background: linear-gradient(135deg, #3b82f6, #2563eb);
  color: #fff;
  font-weight: 600;
  font-size: 20px;
  margin-bottom: 12px;
}

.seeker-info__name {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
}

.seeker-info__fields {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.seeker-info__row {
  display: flex;
  align-items: center;
}

.seeker-info__label {
  width: 72px;
  font-size: 13px;
  color: #94a3b8;
  flex-shrink: 0;
}

.seeker-info__value {
  font-size: 14px;
  color: #0f172a;
}

/* 视频邀请弹窗 */
.video-invite-dialog-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  z-index: 10000;
  display: flex;
  align-items: center;
  justify-content: center;
  backdrop-filter: blur(4px);
}

.video-invite-dialog {
  background: #fff;
  border-radius: 16px;
  padding: 32px 24px 24px;
  width: 320px;
  max-width: 90vw;
  text-align: center;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.video-invite-dialog__icon {
  font-size: 48px;
  margin-bottom: 16px;
}

.video-invite-dialog__title {
  font-size: 18px;
  font-weight: 600;
  color: #0f172a;
  margin-bottom: 8px;
}

.video-invite-dialog__message {
  font-size: 14px;
  color: #64748b;
  margin-bottom: 24px;
  line-height: 1.5;
}

.video-invite-dialog__actions {
  display: flex;
  gap: 12px;
}

.video-invite-dialog__btn {
  flex: 1;
  padding: 12px 16px;
  border-radius: 10px;
  border: none;
  font-size: 15px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
}

.video-invite-dialog__btn--reject {
  background: #f1f5f9;
  color: #64748b;
}

.video-invite-dialog__btn--reject:hover {
  background: #e2e8f0;
}

.video-invite-dialog__btn--accept {
  background: linear-gradient(135deg, #10b981, #059669);
  color: #fff;
}

.video-invite-dialog__btn--accept:hover {
  background: linear-gradient(135deg, #059669, #047857);
  box-shadow: 0 4px 12px rgba(16, 185, 129, 0.3);
}
</style>
