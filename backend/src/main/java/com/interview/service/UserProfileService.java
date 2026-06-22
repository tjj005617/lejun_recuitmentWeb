package com.interview.service;

import com.interview.domain.po.UserProfile;

/**
 * 用户档案服务接口
 */
public interface UserProfileService {

    /**
     * 获取用户档案（不存在则返回空对象）
     */
    UserProfile getProfile(Long userId);

    /**
     * 保存/更新用户档案（upsert）
     */
    void saveProfile(Long userId, UserProfile profile);
}
