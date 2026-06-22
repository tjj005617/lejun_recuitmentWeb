package com.interview;

import com.interview.domain.doc.ChatMessage;
import com.interview.repository.ChatMessageRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class MongoDBTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    /** 测试 MongoDB 连接，插入并查询一条聊天消息 */
    @Test
    public void testInsertAndQuery() {
        // 构造测试消息
        ChatMessage msg = new ChatMessage();
        msg.setConversationId(ChatMessage.generateConversationId(3L, 5L));
        msg.setSenderId(5L);
        msg.setReceiverId(3L);
        msg.setJobId(1L);
        msg.setType("text");
        msg.setContent("你好，我想投递这个职位");
        msg.setRead(false);
        msg.setCreatedAt(LocalDateTime.now());

        // 插入
        ChatMessage saved = chatMessageRepository.save(msg);
        assertNotNull(saved.getId());
        System.out.println("插入成功，ID: " + saved.getId());

        // 查询会话消息
        var messages = chatMessageRepository.findByConversationIdOrderByCreatedAtAsc("3_5");
        assertFalse(messages.isEmpty());
        System.out.println("查询到 " + messages.size() + " 条消息");

        // 查询未读数
        long unreadCount = chatMessageRepository.countByReceiverIdAndReadFalse(3L);
        System.out.println("用户3未读消息数: " + unreadCount);

        // 清理测试数据
        chatMessageRepository.delete(saved);
        System.out.println("测试数据已清理");
    }
}
