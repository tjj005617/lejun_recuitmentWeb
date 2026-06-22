package com.interview.mq;

import com.interview.config.RabbitMQConfig;
import com.interview.domain.doc.ChatMessage;
import com.interview.service.ChatService;
import com.interview.websocket.ChatWebSocket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * ACK 超时消费者
 * 当消息在指定时间内未被接收者 ACK 时，通知发送者消息投递失败
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AckTimeoutConsumer {

    private final ChatService chatService;
    private final ChatWebSocket chatWebSocket;

    /**
     * 处理 ACK 超时消息
     * 检查消息是否已被 ACK，如果未 ACK 则更新状态为 failed 并通知发送者
     */
    @RabbitListener(queues = RabbitMQConfig.ACK_TIMEOUT_QUEUE)
    public void handleAckTimeout(ChatMessage message) {
        try {
            // 查询消息当前状态
            ChatMessage current = chatService.getMessageById(message.getId());
            if (current == null) {
                log.warn("ACK超时: 消息不存在, messageId={}", message.getId());
                return;
            }

            // 如果消息已经是 sent 状态，说明已被 ACK，忽略
            if ("sent".equals(current.getStatus())) {
                log.info("ACK超时: 消息已送达，忽略, messageId={}", message.getId());
                return;
            }

            // 消息未被 ACK，更新状态为 failed
            chatService.updateMessageStatus(message.getId(), "failed");

            // 通知发送者消息投递失败
            ChatMessage failedMessage = chatService.getMessageById(message.getId());
            chatWebSocket.sendToUser(
                failedMessage.getSenderId(),
                failedMessage.getConversationId(),
                Map.of("type", "ack_timeout", "data", failedMessage)
            );

            log.info("ACK超时处理完成: messageId={}, senderId={}", message.getId(), failedMessage.getSenderId());
        } catch (Exception e) {
            log.error("ACK超时处理失败: messageId={}", message.getId(), e);
        }
    }
}
