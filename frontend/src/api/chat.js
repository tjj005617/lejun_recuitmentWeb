import request from './request'

/** 获取会话历史消息（HTTP备用，主要通过WebSocket加载） */
export const getChatHistory = (conversationId, page = 0, size = 50) => {
  return request.get(`/chat/history/${conversationId}`, { params: { page, size } })
}

/** 获取某个会话的未读消息数 */
export const getChatUnreadCount = (conversationId) => {
  return request.get(`/chat/unread/${conversationId}`)
}
