package com.interview.config;

import com.interview.websocket.AsrWebSocket;
import com.interview.websocket.ChatWebSocket;
import com.interview.websocket.InterviewWebSocket;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;

/**
 * WebSocket 配置类
 * 注册面试、聊天、语音识别 WebSocket 处理器
 * 调大缓冲区以支持 ASR 音频分片（base64 编码后可达数百 KB）
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final InterviewWebSocket interviewWebSocket;
    private final ChatWebSocket chatWebSocket;
    private final AsrWebSocket asrWebSocket;

    public WebSocketConfig(InterviewWebSocket interviewWebSocket,
                           ChatWebSocket chatWebSocket,
                           AsrWebSocket asrWebSocket) {
        this.interviewWebSocket = interviewWebSocket;
        this.chatWebSocket = chatWebSocket;
        this.asrWebSocket = asrWebSocket;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // 面试 WebSocket
        registry.addHandler(interviewWebSocket, "/ws/interview")
                .setAllowedOrigins("*");

        // 聊天 WebSocket
        registry.addHandler(chatWebSocket, "/ws/chat")
                .setAllowedOrigins("*");

        // 实时语音识别 WebSocket
        registry.addHandler(asrWebSocket, "/ws/asr")
                .setAllowedOrigins("*");
    }

    /**
     * WebSocket 缓冲区配置
     * 默认最大文本消息 8KB，ASR 音频 base64 后可达数百 KB，需要调大
     */
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        // 最大文本消息 5MB（完整录音音频 base64 可达数 MB）
        container.setMaxTextMessageBufferSize(5 * 1024 * 1024);
        container.setMaxBinaryMessageBufferSize(5 * 1024 * 1024);
        // 空闲超时 30 分钟（支持长录音）
        container.setMaxSessionIdleTimeout(30 * 60 * 1000L);
        return container;
    }
}
