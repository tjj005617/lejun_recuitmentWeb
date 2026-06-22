package com.interview.repository;

import com.interview.domain.doc.ChatMessage;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

/**
 * 聊天消息仓库（MongoDB）
 */
public interface ChatMessageRepository extends MongoRepository<ChatMessage, String> {

    /** 查询某个会话的所有消息（按时间正序） */
    List<ChatMessage> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    /** 查询某个会话的未读消息数 */
    long countByConversationIdAndReceiverIdAndReadFalse(String conversationId, Long receiverId);

    /** 查询某个用户的所有未读消息数 */
    long countByReceiverIdAndReadFalse(Long receiverId);

    /** 标记某个会话中发给某用户的消息为已读 */
    List<ChatMessage> findByConversationIdAndReceiverIdAndReadFalse(String conversationId, Long receiverId);
}
