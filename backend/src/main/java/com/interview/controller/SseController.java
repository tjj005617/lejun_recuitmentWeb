package com.interview.controller;

import com.interview.sse.SseEmitterManager;
import com.interview.util.JwtUtil;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * SSE订阅控制器
 * 客户端通过 GET /api/message/subscribe 建立长连接，接收实时消息推送
 */
@Slf4j
@RestController
@RequestMapping("/api/message")
@RequiredArgsConstructor
public class SseController {

    private final SseEmitterManager sseEmitterManager;
    private final JwtUtil jwtUtil;

    /**
     * 订阅消息推送
     * 前端通过 EventSource 连接，token通过查询参数传递
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@RequestParam("token") String token, HttpServletResponse response) {
        // 解析token获取userId
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            log.warn("SSE订阅失败: token无效");
            SseEmitter emitter = new SseEmitter(0L);
            emitter.completeWithError(new RuntimeException("token无效"));
            return emitter;
        }

        // 禁用代理缓冲（nginx/frp），确保SSE流不被缓存
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Connection", "keep-alive");

        // 创建SseEmitter，不超时
        SseEmitter emitter = new SseEmitter(0L);

        // 注册连接
        sseEmitterManager.addEmitter(userId, emitter);

        // 连接完成时清理（使用 removeEmitterIfCurrent 防止旧连接回调误删新连接）
        emitter.onCompletion(() -> sseEmitterManager.removeEmitterIfCurrent(userId, emitter));
        emitter.onTimeout(() -> sseEmitterManager.removeEmitterIfCurrent(userId, emitter));
        emitter.onError(e -> sseEmitterManager.removeEmitterIfCurrent(userId, emitter));

        // 发送初始心跳确保连接立即建立
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            log.warn("SSE初始心跳发送失败: userId={}", userId);
        }

        log.info("SSE订阅成功: userId={}", userId);
        return emitter;
    }
}
