package com.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.domain.po.UserProfile;
import com.interview.mapper.UserProfileMapper;
import com.interview.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户档案服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileMapper userProfileMapper;

    /**
     * 获取用户档案，不存在则返回空对象
     */
    @Override
    public UserProfile getProfile(Long userId) {
        UserProfile profile = userProfileMapper.selectOne(
            new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
        );
        if (profile == null) {
            // 返回空档案对象，前端可直接回显
            profile = new UserProfile();
            profile.setUserId(userId);
        }
        return profile;
    }

    /**
     * 保存/更新用户档案（upsert）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveProfile(Long userId, UserProfile profile) {
        UserProfile existing = userProfileMapper.selectOne(
            new LambdaQueryWrapper<UserProfile>()
                .eq(UserProfile::getUserId, userId)
        );

        if (existing == null) {
            // 新增
            profile.setUserId(userId);
            userProfileMapper.insert(profile);
            log.info("创建用户档案: userId={}", userId);
        } else {
            // 更新：将提交的字段覆盖到已有记录
            profile.setId(existing.getId());
            profile.setUserId(userId);
            userProfileMapper.updateById(profile);
            log.info("更新用户档案: userId={}", userId);
        }
    }
}
