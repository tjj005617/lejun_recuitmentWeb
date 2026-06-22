package com.interview.service.impl;

import com.interview.domain.doc.ChatMessage;
import com.interview.repository.ChatMessageRepository;
import com.interview.service.ChatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import org.bson.types.ObjectId;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 聊天服务实现（MongoDB）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final MongoTemplate mongoTemplate;

    @Override
    public ChatMessage saveMessage(ChatMessage message) {
        if (message.getCreatedAt() == null) {
            message.setCreatedAt(LocalDateTime.now());
        }
        if (message.getRead() == null) {
            message.setRead(false);
        }
        // 自动生成会话ID
        if (message.getConversationId() == null) {
            message.setConversationId(
                ChatMessage.generateConversationId(message.getSenderId(), message.getReceiverId())
            );
        }

        // 幂等性检查：完全匹配 conversationId + senderId + content + type + createdAt
        // createdAt 在消息首次进入时已固定，重试时值不变，因此可精确去重
        Query dedupQuery = new Query(Criteria
                .where("conversationId").is(message.getConversationId())
                .and("senderId").is(message.getSenderId())
                .and("content").is(message.getContent())
                .and("type").is(message.getType())
                .and("createdAt").is(message.getCreatedAt()));
        ChatMessage existing = mongoTemplate.findOne(dedupQuery, ChatMessage.class);
        if (existing != null) {
            log.warn("检测到重复消息，跳过保存: existingId={}, conversationId={}, content={}",
                    existing.getId(), existing.getConversationId(), existing.getContent());
            return existing;
        }

        ChatMessage saved = chatMessageRepository.save(message);
        log.info("聊天消息已保存: id={}, conversationId={}, senderId={}", saved.getId(), saved.getConversationId(), saved.getSenderId());
        return saved;
    }

    @Override
    public List<ChatMessage> getHistory(String conversationId) {
        return chatMessageRepository.findByConversationIdOrderByCreatedAtAsc(conversationId);
    }

    @Override
    public List<ChatMessage> getHistory(String conversationId, int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "createdAt"));
        Query query = new Query(Criteria.where("conversationId").is(conversationId)).with(pageRequest);
        return mongoTemplate.find(query, ChatMessage.class);
    }

    @Override
    public long markAsRead(String conversationId, Long receiverId) {
        Query query = new Query(
            Criteria.where("conversationId").is(conversationId)
                .and("receiverId").is(receiverId)
                .and("read").is(false)
        );
        Update update = new Update().set("read", true);
        var result = mongoTemplate.updateMulti(query, update, ChatMessage.class);
        long count = result.getModifiedCount();
        log.info("标记已读: conversationId={}, receiverId={}, count={}", conversationId, receiverId, count);
        return count;
    }

    @Override
    public long getUnreadCount(Long userId) {
        return chatMessageRepository.countByReceiverIdAndReadFalse(userId);
    }

    @Override
    public long getUnreadCount(String conversationId, Long userId) {
        return chatMessageRepository.countByConversationIdAndReceiverIdAndReadFalse(conversationId, userId);
    }

    @Override
    public void deleteApplicationMessage(Long senderId, Long jobId) {
        Query query = new Query(
            Criteria.where("senderId").is(senderId)
                .and("jobId").is(jobId)
                .and("type").is("resume")
        );
        mongoTemplate.remove(query, ChatMessage.class);
        log.info("撤回投递消息已删除: senderId={}, jobId={}", senderId, jobId);
    }

    @Override
    public ChatMessage getMessageById(String messageId) {
        return chatMessageRepository.findById(messageId).orElse(null);
    }

    @Override
    public void updateMessageStatus(String messageId, String status) {
        // MongoDB _id 是 ObjectId 类型，必须用 ObjectId 匹配，不能用 String
        Query query = new Query(Criteria.where("_id").is(new ObjectId(messageId)));
        Update update = new Update().set("status", status);
        var result = mongoTemplate.updateFirst(query, update, ChatMessage.class);
        log.info("消息状态更新: messageId={}, status={}, matchedCount={}, modifiedCount={}",
                messageId, status, result.getMatchedCount(), result.getModifiedCount());
    }

    @Override
    public int cleanupDuplicateMessages() {
        // 查询所有消息，按创建时间正序
        List<ChatMessage> allMessages = mongoTemplate.find(
                new Query().with(Sort.by(Sort.Direction.ASC, "createdAt")),
                ChatMessage.class);

        // 按 conversationId + senderId + content 分组
        Map<String, List<ChatMessage>> groups = new HashMap<>();
        for (ChatMessage msg : allMessages) {
            String key = msg.getConversationId() + "|" + msg.getSenderId() + "|" + msg.getContent();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(msg);
        }

        // 每组中，5秒内的消息视为重复，只保留最早的一条
        List<String> duplicateIds = new ArrayList<>();
        for (List<ChatMessage> group : groups.values()) {
            if (group.size() < 2) continue;
            ChatMessage prev = group.get(0);
            for (int i = 1; i < group.size(); i++) {
                ChatMessage curr = group.get(i);
                if (prev.getCreatedAt() != null && curr.getCreatedAt() != null) {
                    long seconds = Duration.between(prev.getCreatedAt(), curr.getCreatedAt()).getSeconds();
                    if (seconds <= 5) {
                        duplicateIds.add(curr.getId());
                    } else {
                        prev = curr;
                    }
                } else {
                    // 没有时间戳的也按重复处理
                    duplicateIds.add(curr.getId());
                }
            }
        }

        if (!duplicateIds.isEmpty()) {
            Query deleteQuery = new Query(Criteria.where("_id").in(
                    duplicateIds.stream().map(ObjectId::new).collect(Collectors.toList())));
            var result = mongoTemplate.remove(deleteQuery, ChatMessage.class);
            log.info("清理重复消息: 删除{}条", result.getDeletedCount());
            return (int) result.getDeletedCount();
        }
        return 0;
    }
}
