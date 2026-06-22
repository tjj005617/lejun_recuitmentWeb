package com.interview.config.memory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.Map;

/**
 * 面试消息序列化工具
 * 负责 Spring AI Message 对象与 JSON 字符串之间的双向转换，用于 Redis 持久化存储
 * 仅支持文本消息类型：USER（用户回答）、ASSISTANT（AI 生成的问题/评分）、SYSTEM（系统指令）
 *
 * <p>参考实现：xg-Cloud-aigc 的 MyMessage，简化为纯文本场景，去除了 tool call 和 media 支持</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class InterviewMessage {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    /** 消息类型：USER / ASSISTANT / SYSTEM */
    private String messageType;

    /** 消息文本内容 */
    private String textContent;

    /** 消息元数据（Spring AI 用于传递附加信息） */
    private Map<String, Object> metadata = Map.of();

    /**
     * 将 Spring AI Message 序列化为 JSON 字符串
     * 存入 Redis List 时调用
     *
     * @param message Spring AI 消息对象
     * @return JSON 字符串
     */
    public static String toJson(Message message) {
        InterviewMessage msg = new InterviewMessage();
        msg.setMessageType(message.getMessageType().getValue());
        msg.setTextContent(message.getText());
        msg.setMetadata(message.getMetadata());
        try {
            return objectMapper.writeValueAsString(msg);
        } catch (Exception e) {
            throw new RuntimeException("消息序列化失败", e);
        }
    }

    /**
     * 将 JSON 字符串反序列化为 Spring AI Message 对象
     * 从 Redis 读取历史消息时调用
     *
     * @param json JSON 字符串
     * @return Spring AI 消息对象
     */
    public static Message toMessage(String json) {
        try {
            InterviewMessage msg = objectMapper.readValue(json, InterviewMessage.class);
            MessageType type = MessageType.valueOf(msg.getMessageType().toUpperCase());
            return switch (type) {
                case USER -> UserMessage.builder()
                        .text(msg.getTextContent())
                        .metadata(msg.getMetadata())
                        .build();
                case ASSISTANT -> new AssistantMessage(msg.getTextContent(), msg.getMetadata());
                case SYSTEM -> new SystemMessage(msg.getTextContent());
                default -> throw new RuntimeException("不支持的消息类型: " + type);
            };
        } catch (Exception e) {
            throw new RuntimeException("消息反序列化失败", e);
        }
    }
}
