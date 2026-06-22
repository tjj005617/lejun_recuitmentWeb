package com.interview.config.memory;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;

/**
 * 基于 Redis 的会话记忆存储实现
 * 实现 Spring AI 的 ChatMemoryRepository 接口，使用 Redis List 作为消息持久化存储
 *
 * <p>Redis 数据结构：List</p>
 * <ul>
 *   <li>Key 格式：ai:interview:{conversationId}，其中 conversationId = {interviewId}_{userId}</li>
 *   <li>Value：每条消息的 JSON 序列化字符串（通过 InterviewMessage 转换）</li>
 * </ul>
 *
 * <p>存储策略：全量替换（saveAll 时先删后写），确保消息列表与内存状态一致</p>
 *
 * <p>参考实现：xg-Cloud-aigc 的 RedisChatMemoryRepository</p>
 */
@RequiredArgsConstructor
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    /** Redis key 前缀，面试会话专用 */
    private static final String KEY_PREFIX = "ai:interview:";

    private final StringRedisTemplate redisTemplate;

    /**
     * 查找所有已存在的会话 ID
     *
     * @return 会话 ID 列表（格式：{interviewId}_{userId}）
     */
    @Override
    public List<String> findConversationIds() {
        Set<String> keys = redisTemplate.keys(KEY_PREFIX + "*");
        if (keys == null) {
            return List.of();
        }
        return keys.stream()
                .map(key -> key.replace(KEY_PREFIX, ""))
                .toList();
    }

    /**
     * 根据会话 ID 加载完整对话历史
     * MessageChatMemoryAdvisor 在每次 ChatClient 调用前自动触发此方法
     *
     * @param conversationId 会话标识（格式：{interviewId}_{userId}）
     * @return 该会话的所有历史消息列表
     */
    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = KEY_PREFIX + conversationId;
        // 读取 Redis List 中的全部消息（0 到 -1 表示从头到尾）
        List<String> jsonMessages = redisTemplate.opsForList().range(key, 0, -1);
        if (jsonMessages == null || jsonMessages.isEmpty()) {
            return List.of();
        }
        // 将每条 JSON 字符串反序列化为 Spring AI Message 对象
        return jsonMessages.stream()
                .map(InterviewMessage::toMessage)
                .toList();
    }

    /**
     * 全量保存消息列表到 Redis
     * MessageChatMemoryAdvisor 在每次 ChatClient 调用后自动触发此方法
     * 采用"先删后写"策略，确保 Redis 数据与内存状态一致
     *
     * @param conversationId 会话标识
     * @param messages       完整的消息列表（包含历史 + 新增）
     */
    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = KEY_PREFIX + conversationId;
        // 先删除旧数据，再逐条写入新数据
        redisTemplate.delete(key);
        for (Message message : messages) {
            redisTemplate.opsForList().rightPush(key, InterviewMessage.toJson(message));
        }
    }

    /**
     * 删除指定会话的全部历史消息
     * 面试结束后可调用此方法清理 Redis 数据
     *
     * @param conversationId 会话标识
     */
    @Override
    public void deleteByConversationId(String conversationId) {
        redisTemplate.delete(KEY_PREFIX + conversationId);
    }
}
