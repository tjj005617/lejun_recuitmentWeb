package com.interview.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 知识图谱处理进度 SSE 连接管理器
 * 按 documentId 管理 SseEmitter，与消息 SSE 完全隔离
 */
@Slf4j
@Component
public class KgSseManager {

    /** documentId → SseEmitter */
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 注册文档处理进度的 SSE 连接
     */
    public void addEmitter(Long documentId, SseEmitter emitter) {
        SseEmitter old = emitters.put(documentId, emitter);
        if (old != null) {
            old.complete();
        }
        log.info("KG SSE连接注册: documentId={}, 当前连接数={}", documentId, emitters.size());
    }

    /**
     * 移除 SSE 连接
     */
    public void removeEmitter(Long documentId) {
        emitters.remove(documentId);
        log.info("KG SSE连接移除: documentId={}, 当前连接数={}", documentId, emitters.size());
    }

    /**
     * 条件移除：仅当 emitter 实例匹配时才移除，防止旧连接回调误删新连接
     */
    public void removeEmitterIfCurrent(Long documentId, SseEmitter emitter) {
        boolean removed = emitters.remove(documentId, emitter);
        if (removed) {
            log.info("KG SSE连接移除(条件匹配): documentId={}, 当前连接数={}", documentId, emitters.size());
        }
    }

    /**
     * 向指定文档的订阅者推送处理进度事件
     *
     * @param documentId 文档ID
     * @param data       进度数据（JSON 序列化）
     */
    public void sendProgress(Long documentId, Object data) {
        SseEmitter emitter = emitters.get(documentId);
        if (emitter == null) {
            log.debug("KG SSE推送跳过（无连接）: documentId={}", documentId);
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                    .name("kg_progress")
                    .data(data));
        } catch (IOException e) {
            log.warn("KG SSE推送失败: documentId={}, error={}", documentId, e.getMessage());
            removeEmitterIfCurrent(documentId, emitter);
        }
    }

    /**
     * 完成并关闭指定文档的 SSE 会话
     */
    public void completeSession(Long documentId) {
        SseEmitter emitter = emitters.remove(documentId);
        if (emitter != null) {
            emitter.complete();
        }
    }
}
