import request from './request'

// 发送消息
export const sendMessage = (data) => {
  return request.post('/message', data)
}

// 获取会话列表
export const getConversations = () => {
  return request.get('/message/conversations')
}

// 获取与某用户的聊天记录
export const getConversation = (userId) => {
  return request.get(`/message/conversation/${userId}`)
}

// 标记消息已读
export const markAsRead = (senderId) => {
  return request.put(`/message/read/${senderId}`)
}

// 获取未读消息数
export const getUnreadCount = () => {
  return request.get('/message/unread-count')
}

// 获取最新一条未读消息（轮询通知用）
export const getLatestUnreadMessage = () => {
  return request.get('/message/latest-unread')
}

// 更新会话关联的职位
export const updateConversationJob = (otherUserId, jobId) => {
  return request.put('/message/conversation/job', { otherUserId, jobId })
}
