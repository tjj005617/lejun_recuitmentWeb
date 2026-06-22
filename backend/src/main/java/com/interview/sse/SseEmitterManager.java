package com.interview.sse;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * SSE连接管理器
 * 维护 userId → SseEmitter 映射，支持向指定用户推送事件
 */
@Slf4j
@Component
public class SseEmitterManager {

    /** userId → SseEmitter */
    private final ConcurrentHashMap<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    /**
     * 注册用户的SSE连接
     */
    public void addEmitter(Long userId, SseEmitter emitter) {
        // 移除旧连接
        SseEmitter old = emitters.put(userId, emitter);
        if (old != null) {
            old.complete();
        }
        log.info("SSE连接注册: userId={}, 当前在线数={}", userId, emitters.size());
    }

    /**
     * 移除用户的SSE连接
     * 只移除指定的 emitter 实例，防止旧连接的 onCompletion 误删新连接
     */
    public void removeEmitter(Long userId) {
        emitters.remove(userId);
        log.info("SSE连接移除: userId={}, 当前在线数={}", userId, emitters.size());
    }

    /**
     * 移除指定的 emitter 实例（仅当它仍是当前连接时才移除）
     * 用于 onCompletion/onError 回调，防止旧连接回调误删新连接
     */
    public void removeEmitterIfCurrent(Long userId, SseEmitter emitter) {
        boolean removed = emitters.remove(userId, emitter); // ConcurrentHashMap.remove(key, value) 只在值匹配时移除
        if (removed) {
            log.info("SSE连接移除(条件匹配): userId={}, 当前在线数={}", userId, emitters.size());
        }
    }

    /**
     * 向指定用户推送事件
     */
    public void sendToUser(Long userId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(userId);
        if (emitter == null) {
            log.warn("SSE推送跳过（用户无连接）: userId={}, event={}, onlineUsers={}",
                    userId, eventName, emitters.keySet());
            return;
        }
        try {
            emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data));
            log.info("SSE推送成功: userId={}, eventName={}", userId, eventName);
        } catch (IOException e) {
            log.warn("SSE推送失败: userId={}, error={}", userId, e.getMessage());
            removeEmitterIfCurrent(userId, emitter);
        }
    }

    /**
     * 获取当前在线用户数
     */
    public int getOnlineCount() {
        return emitters.size();
    }
}
