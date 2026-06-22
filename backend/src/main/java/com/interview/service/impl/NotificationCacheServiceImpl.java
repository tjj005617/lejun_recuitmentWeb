package com.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.domain.po.Message;
import com.interview.mapper.MessageMapper;
import com.interview.service.NotificationCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * 消息未读数缓存服务实现类
 * Redis key格式：message:unread:{userId}，value为未读数
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationCacheServiceImpl implements NotificationCacheService {

    private final StringRedisTemplate stringRedisTemplate;
    private final MessageMapper messageMapper;

    /** Redis key前缀 */
    private static final String UNREAD_KEY_PREFIX = "message:unread:";

    /**
     * 获取Redis key
     */
    private String getKey(Long userId) {
        return UNREAD_KEY_PREFIX + userId;
    }

    /**
     * 增加未读数（收到新消息时调用）
     */
    @Override
    public void incrementUnread(Long userId) {
        String key = getKey(userId);
        stringRedisTemplate.opsForValue().increment(key);
        log.debug("Redis未读数+1: userId={}", userId);
    }

    /**
     * 减少未读数（标记已读时调用）
     */
    @Override
    public void decrementUnread(Long userId, int count) {
        if (count <= 0) return;
        String key = getKey(userId);
        Long remaining = stringRedisTemplate.opsForValue().decrement(key, count);
        // 防止出现负数
        if (remaining != null && remaining < 0) {
            stringRedisTemplate.opsForValue().set(key, "0");
        }
        log.debug("Redis未读数-{}: userId={}", count, userId);
    }

    /**
     * 获取未读数（Redis优先，无缓存则查DB回填）
     */
    @Override
    public int getUnreadCount(Long userId) {
        String key = getKey(userId);
        String val = stringRedisTemplate.opsForValue().get(key);

        if (val != null) {
            // Redis有缓存，直接返回
            return Integer.parseInt(val);
        }

        // Redis无缓存，查DB回填
        Long count = messageMapper.selectCount(
            new LambdaQueryWrapper<Message>()
                .eq(Message::getReceiverId, userId)
                .eq(Message::getIsRead, 0)
        );
        int unreadCount = count.intValue();
        stringRedisTemplate.opsForValue().set(key, String.valueOf(unreadCount));
        log.debug("Redis缓存回填: userId={}, count={}", userId, unreadCount);
        return unreadCount;
    }

    /**
     * 清零未读数（全部已读时调用）
     */
    @Override
    public void clearUnread(Long userId) {
        String key = getKey(userId);
        stringRedisTemplate.delete(key);
        log.debug("Redis未读数清零: userId={}", userId);
    }
}
