package com.interview.service;

/**
 * 消息未读数缓存服务接口
 * 使用Redis缓存每个用户的未读消息数，避免每次查库
 */
public interface NotificationCacheService {

    /**
     * 增加未读数（收到新消息时调用）
     */
    void incrementUnread(Long userId);

    /**
     * 减少未读数（标记已读时调用）
     */
    void decrementUnread(Long userId, int count);

    /**
     * 获取未读数（Redis优先，无缓存则查DB回填）
     */
    int getUnreadCount(Long userId);

    /**
     * 清零未读数（全部已读时调用）
     */
    void clearUnread(Long userId);
}
