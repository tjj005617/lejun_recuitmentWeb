package com.interview.controller.admin;

import com.interview.sse.KgSseManager;
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
 * 知识图谱处理进度 SSE 订阅端点
 * 管理员上传文档后通过此端点接收实时处理进度
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/kg")
@RequiredArgsConstructor
public class AdminKgSseController {

    private final KgSseManager kgSseManager;
    private final JwtUtil jwtUtil;

    /**
     * 订阅文档处理进度推送
     * 前端通过 EventSource 连接，token 和 documentId 通过查询参数传递
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @RequestParam("token") String token,
            @RequestParam("documentId") Long documentId,
            HttpServletResponse response) {

        // 验证 token（JwtFilter 已校验，这里二次确认）
        Long userId = jwtUtil.getUserId(token);
        if (userId == null) {
            log.warn("KG SSE订阅失败: token无效");
            SseEmitter emitter = new SseEmitter(0L);
            emitter.completeWithError(new RuntimeException("token无效"));
            return emitter;
        }

        // 禁用代理缓冲（nginx/frp），确保SSE流不被缓存
        response.setHeader("X-Accel-Buffering", "no");
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Connection", "keep-alive");

        // 创建 SseEmitter，不超时
        SseEmitter emitter = new SseEmitter(0L);

        // 注册连接
        kgSseManager.addEmitter(documentId, emitter);

        // 连接清理
        emitter.onCompletion(() -> kgSseManager.removeEmitterIfCurrent(documentId, emitter));
        emitter.onTimeout(() -> kgSseManager.removeEmitterIfCurrent(documentId, emitter));
        emitter.onError(e -> kgSseManager.removeEmitterIfCurrent(documentId, emitter));

        // 发送初始心跳确保连接立即建立
        try {
            emitter.send(SseEmitter.event().name("connected").data("ok"));
        } catch (IOException e) {
            log.warn("KG SSE初始心跳发送失败: documentId={}", documentId);
        }

        log.info("KG SSE订阅成功: userId={}, documentId={}", userId, documentId);
        return emitter;
    }
}
