import { ref } from 'vue'
import { getUnreadCount, getLatestUnreadMessage, getConversations, getConversation, markAsRead } from '@/api/message'
import { ElNotification } from 'element-plus'

/** 模块级共享状态 */
const unreadCount = ref(0)
const conversations = ref([]) // 会话列表（全局共享，登录后预加载）
let eventSource = null
let reconnectTimer = null
let pollingTimer = null // 未读数轮询定时器
let previousUnreadCount = 0 // 上次轮询时的未读数，用于检测新增消息
const selectedUserId = ref(null) // 当前选中的会话用户ID（供 AppShell 判断 WebSocket 是否会处理）
const wsConnected = ref(false) // WebSocket 是否已连接（供 AppShell 判断 SSE 是否需要兜底）
const videoInviteHandledByWs = ref(false) // WebSocket 是否已处理 video_invite（SSE 回调据此决定是否弹窗）
let videoInvitePopupShown = false // 视频邀约弹窗去重：任一通道弹过后置 true，挂断/拒绝后重置

/** 检查 video_invite 是否已被处理（后续有 accept/reject/hangup 消息） */
const isVideoInviteHandled = async (senderId) => {
  try {
    const res = await getConversation(senderId)
    const msgs = res.data || []
    // 找到 video_invite 的位置
    const inviteIdx = msgs.findIndex(m => m.type === 'video_invite')
    if (inviteIdx === -1) return true // 没找到邀约消息，视为已处理
    // 检查后续是否有响应
    return msgs.slice(inviteIdx + 1).some(m =>
      m.type === 'video_accept' || m.type === 'video_reject' || m.type === 'video_hangup'
      || (m.type === 'text' && m.content && m.content.includes('视频面试通话结束'))
    )
  } catch {
    return false // 查询失败，不阻止弹窗
  }
}

/** video_invite 回调（由 AppShell 注册） */
let onVideoInvite = null
/** 待处理的视频邀请缓冲（SSE收到但回调为null时暂存，注册回调时自动flush） */
let pendingVideoInvites = []

/** 注册视频邀请回调，注册后自动处理缓冲中的邀请 */
const setVideoInviteCallback = (callback) => {
  onVideoInvite = callback
  // 回调注册后，立即处理缓冲中的视频邀请（丢弃超过30秒的陈旧邀请）
  if (onVideoInvite && pendingVideoInvites.length > 0) {
    const now = Date.now()
    const valid = pendingVideoInvites.filter(item => now - item._ts < 30000)
    pendingVideoInvites = []
    valid.forEach(item => {
      console.log('[SSE] flush buffered video_invite:', item.data)
      onVideoInvite(item.data)
    })
  }
}

/** 连接SSE */
const connectSSE = () => {
  console.log('[SSE] connectSSE called, existing eventSource:', eventSource?.readyState)
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }

  const token = localStorage.getItem('token')
  if (!token) { console.log('[SSE] no token, skip'); return }

  // CONNECTING超时：如果之前就在连接但一直没成功，强制断开重连
  if (eventSource) {
    if (eventSource.readyState === EventSource.OPEN) {
      console.log('[SSE] already OPEN, reuse')
      return
    }
    if (eventSource.readyState === EventSource.CONNECTING) {
      // 超过5秒还在连接中，强制关闭重连
      if (eventSource._connectTime && Date.now() - eventSource._connectTime > 5000) {
        console.log('[SSE] CONNECTING timeout (>5s), force reconnect')
        try { eventSource.close() } catch {}
        eventSource = null
      } else {
        return
      }
    }
    if (eventSource && eventSource.readyState === EventSource.CLOSED) {
      console.log('[SSE] was CLOSED, reconnecting...')
      try { eventSource.close() } catch {}
      eventSource = null
    }
  }

  fetchUnreadCount()

  const apiBase = import.meta.env.VITE_API_BASE_URL || '/api'
  const es = new EventSource(`${apiBase}/message/subscribe?token=${token}`)
  es._connectTime = Date.now()  // 记录连接开始时间，用于CONNECTING超时检测

  es.addEventListener('message', (e) => {
    console.log('[SSE] message event received:', e.data)
    try {
      const data = JSON.parse(e.data)
      console.log('[SSE] parsed data:', data)
      // 弹通知 + 刷新未读数（SSE 是实时通道，收到消息就该更新 badge）
      if (!window.location.pathname.includes('/messages')) {
        showNotification(data)
      }
      fetchUnreadCount()
    } catch (err) {
      console.error('[SSE] parse error:', err)
    }
  })

  es.addEventListener('video_invite', (e) => {
    console.log('[SSE] video_invite received:', e.data)
    try {
      const data = JSON.parse(e.data)
      // 去重：已弹过窗口就不重复弹
      if (videoInvitePopupShown || videoInviteHandledByWs.value) {
        console.log('[SSE] video_invite deduped (popup already shown or handled by ws)')
        return
      }
      if (onVideoInvite) {
        videoInvitePopupShown = true
        onVideoInvite(data)
      } else {
        // 回调未注册（页面切换中），暂存到缓冲，等回调注册后自动处理
        console.log('[SSE] video_invite buffered (no callback yet):', data)
        pendingVideoInvites.push({ data, _ts: Date.now() })
      }
    } catch { /* 静默 */ }
  })

  es.addEventListener('open', () => {
    console.log('[SSE] connection open for userId')
    fetchUnreadCount()
    stopPolling() // SSE 恢复，停止轮询兜底
    // 连接成功后检查是否有未处理的视频邀约（兜底首次进入场景）
    checkPendingVideoInvite()
  })

  es.onerror = (err) => {
    console.error('[SSE] error, readyState=', es.readyState)
    // 无论什么状态，都启动轮询兜底（SSE 已不可靠）
    startPolling()
    try { es.close() } catch {}
    eventSource = null
    reconnectTimer = setTimeout(connectSSE, 3000)
  }

  eventSource = es
}

/** 断开SSE */
const disconnectSSE = () => {
  console.log('[SSE] disconnectSSE called')
  if (reconnectTimer) {
    clearTimeout(reconnectTimer)
    reconnectTimer = null
  }
  stopPolling()
  if (eventSource) {
    try { eventSource.close() } catch {}
    eventSource = null
  }
}

/** 拉取数据库真实未读数 */
const fetchUnreadCount = async () => {
  try {
    const res = await getUnreadCount()
    unreadCount.value = res.data ?? 0
    previousUnreadCount = unreadCount.value
  } catch {
    // 静默失败
  }
}

/** SSE 连接成功后检查是否有未处理的视频邀约（兜底首次进入场景） */
const checkPendingVideoInvite = async () => {
  try {
    const res = await getUnreadCount()
    if ((res.data ?? 0) > 0) {
      const latestRes = await getLatestUnreadMessage()
      const latest = latestRes.data
      if (latest && latest.type === 'video_invite' && onVideoInvite && !videoInviteHandledByWs.value && !videoInvitePopupShown) {
        const handled = await isVideoInviteHandled(latest.senderId)
        if (handled) {
          console.log('[SSE] video_invite already handled on connect, marking as read')
          await markAsRead(latest.senderId).catch(() => {})
        } else {
          console.log('[SSE] found pending video_invite on connect, triggering callback')
          videoInvitePopupShown = true
          onVideoInvite({
            type: 'video_invite',
            senderId: latest.senderId,
            jobId: latest.jobId || null,
            conversationId: null
          })
        }
      }
    }
  } catch {
    // 静默失败
  }
}

/** 加载会话列表（登录后预加载） */
const loadConversations = async () => {
  try {
    const res = await getConversations()
    if (res?.data) {
      conversations.value = res.data
    }
  } catch {
    conversations.value = []
  }
}

/** 启动未读数轮询（SSE不可靠时的兜底方案，每5秒检查一次） */
const startPolling = () => {
  if (pollingTimer) return // 已在轮询，不重复启动
  console.log('[Polling] starting polling fallback (every 5s)')
  let tickCount = 0
  pollingTimer = setInterval(async () => {
    tickCount++
    try {
      const res = await getUnreadCount()
      const newCount = res.data ?? 0
      console.log(`[Polling] tick #${tickCount}, unreadCount=${newCount}, prev=${previousUnreadCount}`)
      // 未读数增加 → 查最新未读消息
      if (newCount > previousUnreadCount) {
        const latestRes = await getLatestUnreadMessage()
        const latest = latestRes.data
        console.log('[Polling] latest unread:', latest)
        if (latest) {
          // video_invite 类型 → 走视频邀约回调（弹窗选择）
          if (latest.type === 'video_invite' && onVideoInvite && !videoInviteHandledByWs.value && !videoInvitePopupShown) {
            // 检查邀约是否已被处理（accept/reject/hangup）
            const handled = await isVideoInviteHandled(latest.senderId)
            if (handled) {
              console.log('[Polling] video_invite already handled, marking as read')
              await markAsRead(latest.senderId).catch(() => {})
            } else {
              console.log('[Polling] detected video_invite, triggering callback')
              videoInvitePopupShown = true
              onVideoInvite({
                type: 'video_invite',
                senderId: latest.senderId,
                jobId: latest.jobId || null,
                conversationId: null
              })
            }
          } else if (latest.type !== 'video_invite' && !window.location.pathname.includes('/messages')) {
            // 普通消息且不在消息页 → 弹通知
            showNotification(latest)
          }
        }
      }
      // 定期检查待处理的视频邀约（每6轮 = 30秒），防止未读数已同步但邀约未展示
      if (tickCount % 6 === 0 && onVideoInvite && !videoInviteHandledByWs.value && !videoInvitePopupShown) {
        try {
          const latestRes = await getLatestUnreadMessage()
          const latest = latestRes.data
          if (latest && latest.type === 'video_invite') {
            const handled = await isVideoInviteHandled(latest.senderId)
            if (handled) {
              console.log('[Polling] periodic: video_invite already handled, marking as read')
              await markAsRead(latest.senderId).catch(() => {})
            } else {
              console.log('[Polling] periodic check found pending video_invite, triggering callback')
              videoInvitePopupShown = true
              onVideoInvite({
                type: 'video_invite',
                senderId: latest.senderId,
                jobId: latest.jobId || null,
                conversationId: null
              })
            }
          }
        } catch { /* 静默失败 */ }
      }
      unreadCount.value = newCount
      previousUnreadCount = newCount
    } catch (e) {
      console.error('[Polling] error:', e.message)
    }
  }, 5000)
}

/** 停止未读数轮询 */
const stopPolling = () => {
  if (pollingTimer) {
    clearInterval(pollingTimer)
    pollingTimer = null
    console.log('[SSE] polling stopped')
  }
}

/** 通知去重：5秒内相同内容不重复弹 */
let lastNotifyKey = ''
let lastNotifyTime = 0

/** 弹窗通知 */
const showNotification = (data) => {
  const senderName = data.senderName || '对方'
  const content = data.content || '您有一条新消息'
  const key = senderName + '|' + content
  const now = Date.now()
  // 5秒内相同发送者+内容不重复弹窗
  if (key === lastNotifyKey && now - lastNotifyTime < 5000) {
    console.log('[SSE] notification dedup:', key)
    return
  }
  lastNotifyKey = key
  lastNotifyTime = now
  console.log('[SSE] showNotification:', senderName, content)
  ElNotification({
    title: `${senderName} 发来消息`,
    message: content,
    type: 'info',
    duration: 5000,
    customClass: 'notify-message'
  })
}

/** 重置视频邀约弹窗去重标记（挂断/拒绝/超时后调用） */
const resetVideoInvitePopup = () => {
  videoInvitePopupShown = false
}

export function useMessageStore() {
  return {
    unreadCount,
    conversations,
    selectedUserId,
    wsConnected,
    videoInviteHandledByWs,
    connectSSE,
    disconnectSSE,
    fetchUnreadCount,
    loadConversations,
    setVideoInviteCallback,
    resetVideoInvitePopup
  }
}
