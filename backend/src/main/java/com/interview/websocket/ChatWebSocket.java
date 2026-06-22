package com.interview.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.domain.doc.ChatMessage;
import com.interview.domain.po.Resume;
import com.interview.service.ChatService;
import com.interview.service.MessageService;
import com.interview.service.NotificationCacheService;
import com.interview.service.ResumeService;
import com.interview.sse.SseEmitterManager;
import com.interview.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 聊天 WebSocket 处理器
 *
 * 协议：
 * 客户端→服务端：{type:"text", content:"你好"} | {type:"resume", resumeId:1} | {type:"read"}
 * 服务端→客户端：{type:"history", messages:[...]} | {type:"message", data:{...}} | {type:"read"} | {type:"error"}
 */
@Slf4j
@Component
public class ChatWebSocket extends AbstractWebSocketHandler {

    private final ChatService chatService;
    private final MessageService messageService;
    private final ResumeService resumeService;
    private final NotificationCacheService notificationCacheService;
    private final SseEmitterManager sseEmitterManager;
    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    /** 会话ID → 该会话中的所有 WebSocket 连接 */
    private final Map<String, Set<WebSocketSession>> conversationSessions = new ConcurrentHashMap<>();

    /** sessionId → 连接元信息 */
    private final Map<String, SessionInfo> sessionInfoMap = new ConcurrentHashMap<>();

    /** 消息防重：senderId+content → 上次发送时间戳，5秒内相同内容视为重复 */
    private final Map<String, Long> recentMessages = new ConcurrentHashMap<>();

    public ChatWebSocket(ChatService chatService, MessageService messageService,
                         ResumeService resumeService,
                         NotificationCacheService notificationCacheService,
                         SseEmitterManager sseEmitterManager,
                         JwtUtil jwtUtil, ObjectMapper objectMapper) {
        this.chatService = chatService;
        this.messageService = messageService;
        this.resumeService = resumeService;
        this.notificationCacheService = notificationCacheService;
        this.sseEmitterManager = sseEmitterManager;
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;

        // 每60秒清理过期的去重记录，防止内存泄漏
        java.util.concurrent.ScheduledExecutorService scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
        scheduler.scheduleAtFixedRate(() -> {
            long now = System.currentTimeMillis();
            recentMessages.entrySet().removeIf(e -> now - e.getValue() > 10000);
        }, 60, 60, java.util.concurrent.TimeUnit.SECONDS);
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        // 从URL参数解析token和conversationId
        String query = session.getUri() != null ? session.getUri().getQuery() : null;
        if (query == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        String token = null;
        String conversationId = null;
        for (String param : query.split("&")) {
            String[] kv = param.split("=", 2);
            if ("token".equals(kv[0])) token = kv[1];
            if ("conversationId".equals(kv[0])) conversationId = kv[1];
        }

        if (token == null || conversationId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            session.close(CloseStatus.NOT_ACCEPTABLE);
            return;
        }

        // 增大消息限制，支持视频中继数据
        session.setTextMessageSizeLimit(1024 * 1024); // 1MB（文本消息）
        session.setBinaryMessageSizeLimit(2 * 1024 * 1024); // 2MB（二进制视频帧）

        // 保存连接信息
        SessionInfo info = new SessionInfo(userId, conversationId);
        sessionInfoMap.put(session.getId(), info);
        conversationSessions.computeIfAbsent(conversationId, k -> ConcurrentHashMap.newKeySet()).add(session);

        log.info("聊天WebSocket连接: sessionId={}, userId={}, conversationId={}", session.getId(), userId, conversationId);

        // 发送历史消息
        List<ChatMessage> history = chatService.getHistory(conversationId);
        sendMessage(session, Map.of("type", "history", "messages", history));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            SessionInfo info = sessionInfoMap.get(session.getId());
            if (info == null) return;

            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");

            // 入口日志：记录每条收到的WebSocket消息
            log.info("WS收到: type={}, userId={}, payload={}", type, info.userId, message.getPayload());

            switch (type) {
                case "text" -> handleText(session, info, payload);
                case "resume" -> handleResume(session, info, payload);
                case "read" -> handleRead(session, info);
                case "ack" -> handleAck(session, info, payload);
                // 视频面试信令消息：透传给对方
                case "video_invite", "video_accept", "video_reject", "video_hangup" ->
                        handleVideoSignal(session, info, payload);
                case "video_offer", "video_answer", "video_ice" ->
                        handleVideoTargeted(session, info, payload);
                default -> sendMessage(session, Map.of("type", "error", "message", "未知消息类型: " + type));
            }
        } catch (Exception e) {
            log.error("处理聊天消息失败", e);
            sendMessage(session, Map.of("type", "error", "message", "消息处理失败"));
        }
    }

    /** 视频中继帧计数器（采样日志用） */
    private long relayFrameCount = 0;

    /**
     * 处理二进制帧 — 视频中继数据
     * 协议：[1B flags] [2B codec长度] [NB codec] [8B senderId] [剩余: 视频数据]
     *
     * 后端零解析转发：不读取头部、不反序列化视频数据，仅校验最小帧大小后直接转发字节
     * senderId 从 SessionInfo 获取（无需从帧中解析），接收方自行从帧头读取
     */
    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) {
        SessionInfo info = sessionInfoMap.get(session.getId());
        if (info == null) return;

        java.nio.ByteBuffer buffer = message.getPayload();
        int totalSize = buffer.remaining();

        // 最小帧：1B flags + 2B codecLen + 8B senderId = 11B（不解析，仅做边界检查）
        if (totalSize < 11) {
            log.warn("二进制帧过小: {}B, userId={}", totalSize, info.userId);
            return;
        }

        Long receiverId = parseReceiverId(info.conversationId, info.userId);
        // duplicate() 复制 position/limit 但共享数据，避免修改原 buffer
        sendBinaryToUser(receiverId, info.conversationId, buffer.duplicate());

        // 采样日志：每 200 帧打印一次，避免日志风暴
        long count = ++relayFrameCount;
        if (count <= 2 || count % 200 == 0) {
            log.info("视频二进制中继: from={} to={}, size={}B, frame#{}", info.userId, receiverId, totalSize, count);
        }
    }

    /** 处理文字消息 → 投递到 RabbitMQ，由消费者异步持久化和推送 */
    private void handleText(WebSocketSession session, SessionInfo info, Map<String, Object> payload) {
        String content = (String) payload.get("content");

        // 拒绝空消息 —— 从根源杜绝空消息轰炸
        if (content == null || content.trim().isEmpty()) {
            log.warn("拒绝空消息: userId={}, sessionId={}", info.userId, session.getId());
            return;
        }

        Long jobId = payload.get("jobId") != null ? Long.valueOf(payload.get("jobId").toString()) : null;

        // 防重：put返回旧值，原子操作消除并发竞态
        String dedupKey = info.userId + "|" + content;
        long now = System.currentTimeMillis();
        Long lastTime = recentMessages.put(dedupKey, now);
        if (lastTime != null && now - lastTime < 5000) {
            // 拒绝重复，但恢复第一次发送的时间戳，保证去重窗口准确
            recentMessages.put(dedupKey, lastTime);
            log.warn("消息去重拦截: userId={}, content={}, 间隔={}ms",
                    info.userId, content, now - lastTime);
            return;
        }

        // 构造消息
        Long receiverId = parseReceiverId(info.conversationId, info.userId);
        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setConversationId(info.conversationId);
        chatMessage.setSenderId(info.userId);
        chatMessage.setReceiverId(receiverId);
        chatMessage.setJobId(jobId);
        chatMessage.setType("text");
        chatMessage.setContent(content);
        chatMessage.setStatus("sent");

        // 直接写MongoDB（同步，无MQ跳转）
        ChatMessage saved = chatService.saveMessage(chatMessage);
        log.info("消息已保存: id={}, senderId={}, content='{}'", saved.getId(), info.userId, content);

        // 发送 ACK 回执给发送方（携带 MongoDB 真实 ID，前端据此替换临时 ID）
        sendToUser(info.userId, info.conversationId,
                Map.of("type", "ack", "messageId", saved.getId(), "tempId", payload.getOrDefault("tempId", "")));

        // 广播给会话中所有人（接收方已在聊天页时直接收到）
        broadcastToConversation(info.conversationId, Map.of("type", "message", "data", saved));

        // 仅当接收方不在该会话的WebSocket连接中时，才推送SSE通知+加未读数
        // 避免用户已在聊天页看到消息，Redis未读数还虚高
        if (!isUserInConversation(receiverId, info.conversationId)) {
            sendChatNotification(saved);
        }
    }

    /** 给接收方推送通知 */
    private void sendChatNotification(ChatMessage chatMsg) {
        try {
            com.interview.domain.po.Message notice = new com.interview.domain.po.Message();
            notice.setSenderId(chatMsg.getSenderId());
            notice.setReceiverId(chatMsg.getReceiverId());
            notice.setJobId(chatMsg.getJobId());
            notice.setType("chat");
            String content = "resume".equals(chatMsg.getType())
                    ? "[简历] " + chatMsg.getContent()
                    : chatMsg.getContent();
            notice.setContent(content);
            notice.setIsRead(0);
            messageService.sendMessage(chatMsg.getSenderId(), notice);
        } catch (Exception e) {
            log.warn("通知推送失败: id={}", chatMsg.getId(), e);
        }
    }

    /** 处理简历附件消息 → 投递到 RabbitMQ */
    private void handleResume(WebSocketSession session, SessionInfo info, Map<String, Object> payload) {
        Long resumeId = Long.valueOf(payload.get("resumeId").toString());

        // 防重：put返回旧值，原子操作消除并发竞态
        String dedupKey = info.userId + "|resume|" + resumeId;
        long now = System.currentTimeMillis();
        Long lastTime = recentMessages.put(dedupKey, now);
        if (lastTime != null && now - lastTime < 5000) {
            recentMessages.put(dedupKey, lastTime);
            log.warn("简历消息去重拦截: userId={}, resumeId={}", info.userId, resumeId);
            return;
        }
        String content = (String) payload.getOrDefault("content", "请查看我的简历");
        Long jobId = payload.get("jobId") != null ? Long.valueOf(payload.get("jobId").toString()) : null;

        Long receiverId = parseReceiverId(info.conversationId, info.userId);

        String resumeName = "";
        try {
            Resume resume = resumeService.getResumeById(resumeId);
            if (resume != null) {
                resumeName = resume.getFileName() != null ? resume.getFileName() : "简历";
            }
        } catch (Exception e) {
            log.warn("查询简历失败: resumeId={}", resumeId);
        }

        ChatMessage chatMessage = new ChatMessage();
        chatMessage.setConversationId(info.conversationId);
        chatMessage.setSenderId(info.userId);
        chatMessage.setReceiverId(receiverId);
        chatMessage.setJobId(jobId);
        chatMessage.setType("resume");
        chatMessage.setContent(content);
        chatMessage.setResumeId(resumeId);
        chatMessage.setResumeName(resumeName);
        chatMessage.setStatus("sent");

        // 直接写库+广播+通知，无MQ跳转
        ChatMessage saved = chatService.saveMessage(chatMessage);
        log.info("简历消息已保存: id={}, senderId={}", saved.getId(), info.userId);

        // 发送 ACK 回执给发送方
        sendToUser(info.userId, info.conversationId,
                Map.of("type", "ack", "messageId", saved.getId(), "tempId", payload.getOrDefault("tempId", "")));

        broadcastToConversation(info.conversationId, Map.of("type", "message", "data", saved));
        // 仅当接收方不在该会话的WebSocket连接中时，才推送通知
        if (!isUserInConversation(receiverId, info.conversationId)) {
            sendChatNotification(saved);
        }
    }

    /** 处理已读标记 */
    private void handleRead(WebSocketSession session, SessionInfo info) {
        long count = chatService.markAsRead(info.conversationId, info.userId);

        // Redis未读数减少
        if (count > 0) {
            notificationCacheService.decrementUnread(info.userId, (int) count);
        }

        // 通知对方"我已读"
        broadcastToConversation(info.conversationId, Map.of("type", "read", "userId", info.userId));
    }

    /** 处理消息 ACK（接收者确认收到消息） */
    private void handleAck(WebSocketSession session, SessionInfo info, Map<String, Object> payload) {
        String messageId = (String) payload.get("messageId");
        if (messageId == null) return;

        // 更新消息状态为已送达
        chatService.updateMessageStatus(messageId, "sent");

        // 通知发送者消息已送达
        Long receiverId = parseReceiverId(info.conversationId, info.userId);
        broadcastToConversation(info.conversationId, Map.of("type", "ack", "messageId", messageId, "userId", info.userId));

        log.info("消息ACK: messageId={}, userId={}", messageId, info.userId);
    }

    /** 视频信令广播：invite/accept/reject/hangup → 广播给会话中的所有人 */
    private void handleVideoSignal(WebSocketSession session, SessionInfo info, Map<String, Object> payload) {
        String type = (String) payload.get("type");
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", type);
        msg.put("senderId", info.userId);
        // 透传额外字段（jobId 等）
        if (payload.get("jobId") != null) msg.put("jobId", payload.get("jobId"));
        broadcastToConversation(info.conversationId, msg);

        // video_invite 额外处理：存库 + SSE推送 + 未读数，确保轮询兜底也能收到
        if ("video_invite".equals(type)) {
            Long receiverId = parseReceiverId(info.conversationId, info.userId);
            Long jobId = payload.get("jobId") != null ? Long.valueOf(payload.get("jobId").toString()) : null;

            // 1. 存入 MongoDB（轮询兜底：查最新未读消息时能检测到 video_invite 类型）
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setConversationId(info.conversationId);
            chatMessage.setSenderId(info.userId);
            chatMessage.setReceiverId(receiverId);
            chatMessage.setJobId(jobId);
            chatMessage.setType("video_invite");
            chatMessage.setContent("视频面试邀请");
            chatMessage.setRead(false);
            chatMessage.setStatus("sent");
            chatService.saveMessage(chatMessage);

            // 2. 创建未读通知（更新 Redis 未读数，触发前端轮询）
            sendVideoInviteNotification(info.userId, receiverId, jobId);

            // 3. SSE 推送（实时通道）
            Map<String, Object> sseData = new java.util.HashMap<>();
            sseData.put("type", "video_invite");
            sseData.put("senderId", info.userId);
            sseData.put("conversationId", info.conversationId);
            if (jobId != null) sseData.put("jobId", jobId);
            sseEmitterManager.sendToUser(receiverId, "video_invite", sseData);
            log.info("SSE推送视频邀请: receiverId={}, senderId={}", receiverId, info.userId);
        }

        // video_reject/accept/hangup 也存入 MongoDB，供前端判断邀约是否已处理
        if ("video_reject".equals(type) || "video_accept".equals(type) || "video_hangup".equals(type)) {
            Long receiverId = parseReceiverId(info.conversationId, info.userId);
            String content = switch (type) {
                case "video_reject" -> "拒绝了视频面试";
                case "video_accept" -> "接受了视频面试";
                case "video_hangup" -> "挂断了视频面试";
                default -> "";
            };
            ChatMessage chatMessage = new ChatMessage();
            chatMessage.setConversationId(info.conversationId);
            chatMessage.setSenderId(info.userId);
            chatMessage.setReceiverId(receiverId);
            chatMessage.setType(type);
            chatMessage.setContent(content);
            chatMessage.setRead(false);
            chatMessage.setStatus("sent");
            // hangup 携带时长
            if ("video_hangup".equals(type) && payload.get("duration") != null) {
                chatMessage.setContent(content + "，通话时长 " + payload.get("duration"));
            }
            chatService.saveMessage(chatMessage);
        }

        log.info("视频信令广播: type={}, conversationId={}, userId={}", type, info.conversationId, info.userId);
    }

    /** video_invite 通知：存入 MySQL 通知表（触发 Redis 未读数更新，前端轮询兜底） */
    private void sendVideoInviteNotification(Long senderId, Long receiverId, Long jobId) {
        try {
            com.interview.domain.po.Message notice = new com.interview.domain.po.Message();
            notice.setSenderId(senderId);
            notice.setReceiverId(receiverId);
            notice.setJobId(jobId);
            notice.setType("video_invite");
            notice.setContent("视频面试邀请");
            notice.setIsRead(0);
            messageService.sendMessage(senderId, notice);
        } catch (Exception e) {
            log.warn("视频邀请通知推送失败: senderId={}, receiverId={}", senderId, receiverId, e);
        }
    }

    /** 视频信令定向发送：offer/answer/ice/media → 只发给对方 */
    private void handleVideoTargeted(WebSocketSession session, SessionInfo info, Map<String, Object> payload) {
        String type = (String) payload.get("type");
        Long receiverId = parseReceiverId(info.conversationId, info.userId);
        Map<String, Object> msg = new java.util.HashMap<>();
        msg.put("type", type);
        msg.put("senderId", info.userId);
        // 透传所有字段（sdp、candidate、data 等）
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (!"type".equals(entry.getKey()) && entry.getValue() != null) {
                msg.put(entry.getKey(), entry.getValue());
            }
        }
        sendToUser(receiverId, info.conversationId, msg);
        // relay 数据量大，不打印完整 payload
        if ("video_media".equals(type)) {
            log.info("视频中继转发: from={} to={}, dataLen={}", info.userId, receiverId,
                    payload.get("data") != null ? ((String) payload.get("data")).length() : 0);
        } else {
            log.info("视频信令定向发送: type={}, from={} to={}", type, info.userId, receiverId);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SessionInfo info = sessionInfoMap.remove(session.getId());
        if (info != null) {
            Set<WebSocketSession> sessions = conversationSessions.get(info.conversationId);
            if (sessions != null) {
                sessions.remove(session);
                if (sessions.isEmpty()) {
                    conversationSessions.remove(info.conversationId);
                }
            }
            log.info("聊天WebSocket断开: sessionId={}, userId={}, conversationId={}", session.getId(), info.userId, info.conversationId);
        }
    }

    /** 检查指定用户是否在会话中有 WebSocket 连接 */
    public boolean isUserInConversation(Long userId, String conversationId) {
        if (conversationId == null) return false;
        Set<WebSocketSession> sessions = conversationSessions.get(conversationId);
        if (sessions == null) return false;

        for (WebSocketSession session : sessions) {
            SessionInfo info = sessionInfoMap.get(session.getId());
            if (info != null && info.userId().equals(userId) && session.isOpen()) {
                return true;
            }
        }
        return false;
    }

    /** 向会话中的所有连接广播消息（public，供 ApplicationServiceImpl 调用） */
    public void broadcastToConversation(String conversationId, Object message) {
        if (conversationId == null) return;
        Set<WebSocketSession> sessions = conversationSessions.get(conversationId);
        if (sessions == null) return;

        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("序列化消息失败", e);
            return;
        }

        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (Exception e) {
                    log.error("广播消息失败: sessionId={}", session.getId());
                }
            }
        }
    }

    /**
     * 向指定用户发送消息（仅发送给该用户的连接）
     * 用于 ACK 超时等定向通知
     */
    public void sendToUser(Long userId, String conversationId, Object message) {
        Set<WebSocketSession> sessions = conversationSessions.get(conversationId);
        if (sessions == null) return;

        String json;
        try {
            json = objectMapper.writeValueAsString(message);
        } catch (Exception e) {
            log.error("序列化消息失败", e);
            return;
        }

        for (WebSocketSession session : sessions) {
            SessionInfo info = sessionInfoMap.get(session.getId());
            if (info != null && info.userId().equals(userId) && session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(json));
                } catch (Exception e) {
                    log.error("发送消息失败: sessionId={}", session.getId());
                }
            }
        }
    }

    /** 向指定用户发送二进制帧（视频中继专用，零拷贝转发） */
    public void sendBinaryToUser(Long userId, String conversationId, java.nio.ByteBuffer data) {
        Set<WebSocketSession> sessions = conversationSessions.get(conversationId);
        if (sessions == null) return;

        for (WebSocketSession session : sessions) {
            SessionInfo info = sessionInfoMap.get(session.getId());
            if (info != null && info.userId().equals(userId) && session.isOpen()) {
                try {
                    data.rewind(); // 多 session 时确保 position 正确
                    session.sendMessage(new BinaryMessage(data));
                } catch (Exception e) {
                    log.error("发送二进制帧失败: sessionId={}", session.getId());
                }
            }
        }
    }

    /** 从会话ID中解析对方用户ID */
    private Long parseReceiverId(String conversationId, Long selfUserId) {
        String[] parts = conversationId.split("_");
        Long id1 = Long.valueOf(parts[0]);
        Long id2 = Long.valueOf(parts[1]);
        return id1.equals(selfUserId) ? id2 : id1;
    }

    private void sendMessage(WebSocketSession session, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            session.sendMessage(new TextMessage(json));
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }

    /** WebSocket 连接元信息 */
    private record SessionInfo(Long userId, String conversationId) {}
}
