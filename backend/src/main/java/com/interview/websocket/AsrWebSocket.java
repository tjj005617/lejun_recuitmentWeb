package com.interview.websocket;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.service.ImmersiveInterviewOrchestrator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;

/**
 * 实时语音识别 WebSocket 处理器
 *
 * 接收前端音频分片（base64 WAV），调用 MiMo ASR 流式接口，
 * 将识别结果逐字转发给前端实现实时字幕。
 *
 * 消息协议：
 * 前端→后端：
 *   {type:"audio_chunk", audio:"base64_wav"} — 录音过程中的音频分片
 *   {type:"audio_final", audio:"base64_wav"} — 录音结束后的完整音频
 *
 * 后端→前端：
 *   {type:"asr_result", text:"累积文本", isFinal:false} — 中间识别结果
 *   {type:"asr_final", text:"最终文本"} — 最终识别结果
 *   {type:"asr_error", message:"错误信息"} — 错误
 */
@Slf4j
@Component
public class AsrWebSocket extends TextWebSocketHandler {

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.api-url}")
    private String apiUrl;

    private final ObjectMapper objectMapper;
    private final ExecutorService aiExecutor;
    private final ImmersiveInterviewOrchestrator orchestrator;

    /** ASR 会话上下文：每个 WebSocket 连接关联的面试 ID 和轮次 */
    private final ConcurrentHashMap<String, SessionContext> sessionContexts = new ConcurrentHashMap<>();

    /** 会话上下文记录 */
    record SessionContext(Long interviewId, int round) {}

    public AsrWebSocket(ObjectMapper objectMapper,
                        @Qualifier("aiExecutor") ExecutorService aiExecutor,
                        ImmersiveInterviewOrchestrator orchestrator) {
        this.objectMapper = objectMapper;
        this.aiExecutor = aiExecutor;
        this.orchestrator = orchestrator;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("ASR WebSocket 连接建立: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Map<String, Object> payload = objectMapper.readValue(message.getPayload(), Map.class);
            String type = (String) payload.get("type");

            // 处理 asr_init：记录面试 ID 和轮次
            if ("asr_init".equals(type)) {
                Long interviewId = Long.valueOf(payload.get("interviewId").toString());
                int round = Integer.parseInt(payload.get("round").toString());
                sessionContexts.put(session.getId(), new SessionContext(interviewId, round));
                log.info("ASR 初始化: interviewId={}, round={}", interviewId, round);
                return;
            }

            String audioBase64 = (String) payload.get("audio");
            if (audioBase64 == null || audioBase64.isBlank()) {
                sendError(session, "音频数据为空");
                return;
            }

            // 校验会话上下文
            SessionContext ctx = sessionContexts.get(session.getId());
            if (ctx == null) {
                sendError(session, "请先发送 asr_init 消息");
                return;
            }

            boolean isFinal = "audio_final".equals(type);
            // 提取分段序号（前端分段录制时传入，用于结果归属）
            int segmentIndex = payload.containsKey("segmentIndex")
                    ? Integer.parseInt(payload.get("segmentIndex").toString()) : -1;
            log.debug("收到音频{}: {}字节, segmentIndex={}, interviewId={}, round={}",
                    isFinal ? "(最终)" : "(分片)", audioBase64.length(), segmentIndex, ctx.interviewId(), ctx.round());

            // 异步调用 MiMo ASR
            final int segIdx = segmentIndex;
            log.info("收到{}音频，提交异步 ASR 任务: interviewId={}, round={}", isFinal ? "最终" : "分片", ctx.interviewId(), ctx.round());
            CompletableFuture.runAsync(() -> {
                try {
                    log.info("ASR 任务开始执行: interviewId={}, round={}", ctx.interviewId(), ctx.round());
                    byte[] wavBytes = Base64.getDecoder().decode(audioBase64);
                    callMimoAsrStreaming(wavBytes, session, isFinal, ctx, segIdx);
                    log.info("ASR 任务执行完成: interviewId={}, round={}", ctx.interviewId(), ctx.round());
                } catch (Exception e) {
                    log.error("ASR 识别失败: interviewId={}, round={}", ctx.interviewId(), ctx.round(), e);
                    sendError(session, "语音识别失败: " + e.getMessage());
                }
            }, aiExecutor);

        } catch (Exception e) {
            log.error("处理 ASR 消息失败", e);
            sendError(session, "消息处理失败: " + e.getMessage());
        }
    }

    /**
     * 调用 MiMo ASR 流式接口，逐字转发识别结果
     * @param ctx 会话上下文（interviewId, round），用于编排服务回调
     */
    private void callMimoAsrStreaming(byte[] wavBytes, WebSocketSession session, boolean isFinal, SessionContext ctx, int segmentIndex) {
        try {
            String base64Audio = Base64.getEncoder().encodeToString(wavBytes);
            String dataUri = "data:audio/wav;base64," + base64Audio;

            // 构建请求体（复用 MiMoSttService 的格式）
            Map<String, Object> requestBody = Map.of(
                "model", "mimo-v2.5-asr",
                "messages", List.of(
                    Map.of("role", "user", "content", List.of(
                        Map.of("type", "input_audio",
                            "input_audio", Map.of("data", dataUri))
                    ))
                ),
                "asr_options", Map.of("language", "auto"),
                "stream", true
            );

            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

            HttpRequest httpRequest = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/v1/chat/completions"))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(
                    objectMapper.writeValueAsString(requestBody)))
                .build();

            HttpResponse<InputStream> response = client.send(httpRequest,
                HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() != 200) {
                String errorBody = new String(response.body().readAllBytes(), StandardCharsets.UTF_8);
                sendError(session, "ASR API 错误: " + response.statusCode());
                log.error("ASR API 返回错误: {} - {}", response.statusCode(), errorBody);
                return;
            }

            // 流式读取 SSE，逐字转发给前端
            StringBuilder fullText = new StringBuilder();
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8));

            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data: ")) continue;

                String data = line.substring(6);
                if ("[DONE]".equals(data)) break;

                try {
                    JsonNode json = objectMapper.readTree(data);
                    String content = json.at("/choices/0/delta/content").asText(null);
                    if (content != null && !content.isEmpty()) {
                        fullText.append(content);
                        // 实时转发识别结果给前端
                        sendResult(session, fullText.toString(), false, segmentIndex);
                    }
                } catch (Exception ignored) {
                    // 忽略非文本 delta
                }
            }

            // 发送最终结果
            String finalText = fullText.toString().trim();
            if (!finalText.isEmpty()) {
                sendResult(session, finalText, true, segmentIndex);
                log.info("ASR 识别完成{}: segmentIndex={}, text={}", isFinal ? "(最终)" : "(分片)", segmentIndex, finalText);

                // asr_final 且有文本 → 通知编排服务进行语义判断+评分
                if (isFinal && ctx != null) {
                    orchestrator.onAsrFinal(session, ctx.interviewId(), ctx.round(), finalText);
                }
            } else {
                sendResult(session, "", true, segmentIndex);
                log.warn("ASR 未返回识别结果");
            }

        } catch (Exception e) {
            log.error("MiMo ASR 调用失败", e);
            sendError(session, "ASR 调用失败: " + e.getMessage());
        }
    }

    /**
     * 发送识别结果给前端
     */
    private void sendResult(WebSocketSession session, String text, boolean isFinal, int segmentIndex) {
        try {
            java.util.HashMap<String, Object> msg = new java.util.HashMap<>();
            msg.put("type", isFinal ? "asr_final" : "asr_result");
            msg.put("text", text);
            msg.put("isFinal", isFinal);
            if (segmentIndex >= 0) {
                msg.put("segmentIndex", segmentIndex);
            }
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
                }
            }
        } catch (Exception e) {
            log.error("发送 ASR 结果失败", e);
        }
    }

    /**
     * 发送错误消息
     */
    private void sendError(WebSocketSession session, String message) {
        try {
            Map<String, Object> msg = Map.of(
                "type", "asr_error",
                "message", message
            );
            synchronized (session) {
                if (session.isOpen()) {
                    session.sendMessage(new TextMessage(objectMapper.writeValueAsString(msg)));
                }
            }
        } catch (Exception e) {
            log.error("发送 ASR 错误消息失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        SessionContext ctx = sessionContexts.remove(session.getId());
        if (ctx != null) {
            orchestrator.cleanup(ctx.interviewId(), ctx.round());
            log.info("ASR 会话清理: interviewId={}, round={}", ctx.interviewId(), ctx.round());
        }
        log.info("ASR WebSocket 连接关闭: {}", session.getId());
    }
}
