package com.interview.mq;

import com.interview.config.RabbitMQConfig;
import com.interview.domain.doc.ChatMessage;
import com.interview.domain.po.Message;
import com.interview.service.ChatService;
import com.interview.service.MessageService;
import com.interview.websocket.ChatWebSocket;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 聊天消息消费者
 * 从 RabbitMQ 消费消息 → 存 MongoDB → WebSocket 推送给在线用户
 * 同时触发通知推送（SSE + Redis 未读数）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ChatMessageConsumer {

    private final ChatService chatService;
    private final ChatWebSocket chatWebSocket;
    private final ChatMessageProducer chatMessageProducer;
    private final MessageService messageService;

    @RabbitListener(queues = RabbitMQConfig.CHAT_QUEUE)
    public void handleMessage(ChatMessage message) {
        // 1. 检查接收者是否在线，决定初始状态
        boolean receiverOnline = chatWebSocket.isUserInConversation(
                message.getReceiverId(), message.getConversationId());
        message.setStatus(receiverOnline ? "sending" : "sent");

        // 2. 持久化到 MongoDB（必须成功，失败则重试）
        log.info("MQ消费: senderId={}, content='{}', conversationId={}",
                message.getSenderId(), message.getContent(), message.getConversationId());
        ChatMessage saved = chatService.saveMessage(message);

        // 3. 以下步骤即使失败也不抛异常，避免重试导致 MongoDB 重复保存
        // 每个步骤独立 try-catch，避免一个失败导致后续步骤被跳过
        if (receiverOnline) {
            try {
                chatMessageProducer.sendDelayedAckTimeout(saved);
            } catch (Exception e) {
                log.warn("ACK超时消息发送失败: id={}", saved.getId(), e);
            }
        }

        try {
            chatWebSocket.broadcastToConversation(
                saved.getConversationId(),
                Map.of("type", "message", "data", saved)
            );
        } catch (Exception e) {
            log.warn("WebSocket广播失败（消息已持久化）: id={}, conversationId={}",
                    saved.getId(), saved.getConversationId(), e);
        }

        try {
            sendNotification(saved);
        } catch (Exception e) {
            log.warn("通知推送失败（消息已持久化）: id={}, conversationId={}",
                    saved.getId(), saved.getConversationId(), e);
        }

        log.info("聊天消息消费成功: id={}, conversationId={}, receiverOnline={}, status={}",
                saved.getId(), saved.getConversationId(), receiverOnline, saved.getStatus());
    }

    /** 发送通知：写 MySQL message 表 + Redis 未读数 + SSE 推送 */
    private void sendNotification(ChatMessage chatMsg) {
        Message notice = new Message();
        notice.setSenderId(chatMsg.getSenderId());
        notice.setReceiverId(chatMsg.getReceiverId());
        notice.setJobId(chatMsg.getJobId());
        notice.setType("chat");
        // 简历附件显示"[简历]"，文字消息显示原文
        String content = "resume".equals(chatMsg.getType())
                ? "[简历] " + chatMsg.getContent()
                : chatMsg.getContent();
        notice.setContent(content);
        notice.setIsRead(0);
        messageService.sendMessage(chatMsg.getSenderId(), notice);
    }
}
