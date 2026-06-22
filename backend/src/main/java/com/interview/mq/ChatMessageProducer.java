package com.interview.mq;

import com.interview.config.RabbitMQConfig;
import com.interview.domain.doc.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

/**
 * 聊天消息生产者
 * 将聊天消息投递到 RabbitMQ，由消费者异步处理持久化和推送
 * 支持延迟消息用于 ACK 超时检测
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageProducer {

    private final RabbitTemplate rabbitTemplate;

    /** ACK 超时时间（毫秒）：30秒 */
    private static final long ACK_TIMEOUT_MS = 30000;

    /**
     * 发送聊天消息到队列
     */
    public void sendChatMessage(ChatMessage message) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.CHAT_EXCHANGE,
            RabbitMQConfig.CHAT_ROUTING_KEY,
            message
        );
        log.info("聊天消息已投递MQ: conversationId={}, senderId={}", message.getConversationId(), message.getSenderId());
    }

    /**
     * 发送延迟消息到 ACK 超时队列
     * 如果接收者在指定时间内未 ACK，则触发超时处理
     * @param message 原始聊天消息
     */
    public void sendDelayedAckTimeout(ChatMessage message) {
        rabbitTemplate.convertAndSend(
            RabbitMQConfig.DELAYED_EXCHANGE,
            RabbitMQConfig.ACK_TIMEOUT_ROUTING_KEY,
            message,
            msg -> {
                msg.getMessageProperties().setDelayLong(ACK_TIMEOUT_MS);
                return msg;
            }
        );
        log.info("ACK超时延迟消息已投递: messageId={}, delay={}ms", message.getId(), ACK_TIMEOUT_MS);
    }
}
