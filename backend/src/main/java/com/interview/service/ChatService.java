package com.interview.service;

import com.interview.domain.doc.ChatMessage;

import java.util.List;

/**
 * 聊天服务接口（MongoDB）
 */
public interface ChatService {

    /** 保存消息 */
    ChatMessage saveMessage(ChatMessage message);

    /** 查询会话历史消息（按时间正序） */
    List<ChatMessage> getHistory(String conversationId);

    /** 查询会话历史消息（分页，按时间倒序） */
    List<ChatMessage> getHistory(String conversationId, int page, int size);

    /** 标记会话中发给某用户的消息为已读，返回已读数量 */
    long markAsRead(String conversationId, Long receiverId);

    /** 获取某个用户的总未读消息数 */
    long getUnreadCount(Long userId);

    /** 获取某个用户在某个会话中的未读消息数 */
    long getUnreadCount(String conversationId, Long userId);

    /** 删除指定发送者关于某职位的投递消息（撤回时调用） */
    void deleteApplicationMessage(Long senderId, Long jobId);

    /** 根据ID查询消息 */
    ChatMessage getMessageById(String messageId);

    /** 更新消息投递状态 */
    void updateMessageStatus(String messageId, String status);

    /** 清理重复消息（同会话、同发送者、同内容、5秒内），返回删除数量 */
    int cleanupDuplicateMessages();
}
