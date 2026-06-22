package com.interview.service;

import com.interview.domain.po.Message;
import com.interview.domain.vo.ConversationVO;

import java.util.List;
import java.util.Map;

/**
 * 消息服务接口
 */
public interface MessageService {

    /**
     * 发送消息
     */
    Message sendMessage(Long senderId, Message message);

    /**
     * 获取会话列表（每个会话的最后一条消息 + 未读数）
     */
    List<ConversationVO> getConversations(Long userId);

    /**
     * 获取与某用户的聊天记录
     */
    List<Message> getConversation(Long userId, Long otherUserId);

    /**
     * 标记来自某用户的消息为已读
     */
    void markAsRead(Long receiverId, Long senderId);

    /**
     * 获取未读消息总数
     */
    int getUnreadCount(Long userId);

    /**
     * 获取最新一条未读消息（用于轮询通知弹窗）
     * @return 最新未读消息，包含senderName；无未读消息时返回null
     */
    Map<String, Object> getLatestUnreadMessage(Long userId);

    /**
     * 更新会话关联的职位（修改最新消息的jobId）
     */
    void updateConversationJob(Long userId, Long otherUserId, Long jobId);
}
