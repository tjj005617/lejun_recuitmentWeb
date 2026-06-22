package com.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.domain.po.User;
import com.interview.domain.po.UserResume;
import com.interview.mapper.UserMapper;
import com.interview.mapper.UserResumeMapper;
import com.interview.service.UserService;
import com.interview.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final UserResumeMapper userResumeMapper;

    private static final int MAX_RESUME_COUNT = 3;

    /**
     * 注册用户
     */
    @Override
    public User register(String username, String password, String nickname, Integer roleType) {
        // 检查用户名是否已存在
        User existing = getUserByUsername(username);
        if (existing != null) {
            throw new RuntimeException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(PasswordUtil.encode(password)); // 加密存储
        user.setNickname(nickname != null ? nickname : username);
        user.setRoleType(roleType != null ? roleType : 1); // 默认求职者
        userMapper.insert(user);

        log.info("用户注册成功: id={}, username={}, roleType={}", user.getId(), username, user.getRoleType());
        return user;
    }

    /**
     * 用户登录
     */
    @Override
    public User login(String username, String password) {
        User user = getUserByUsername(username);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (!PasswordUtil.matches(password, user.getPassword())) {
            throw new RuntimeException("密码错误");
        }
        return user;
    }

    @Override
    public User getUserById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public User getUserByUsername(String username) {
        return userMapper.selectOne(
            new LambdaQueryWrapper<User>()
                .eq(User::getUsername, username)
        );
    }

    /**
     * 添加简历到用户（最多3份）
     */
    @Override
    public void addResumeToUser(Long userId, Long resumeId) {
        // 检查数量限制
        long count = userResumeMapper.selectCount(
            new LambdaQueryWrapper<UserResume>()
                .eq(UserResume::getUserId, userId)
        );
        if (count >= MAX_RESUME_COUNT) {
            throw new RuntimeException("最多只能上传" + MAX_RESUME_COUNT + "份简历");
        }

        // 检查是否已关联
        UserResume existing = userResumeMapper.selectOne(
            new LambdaQueryWrapper<UserResume>()
                .eq(UserResume::getUserId, userId)
                .eq(UserResume::getResumeId, resumeId)
        );
        if (existing != null) {
            throw new RuntimeException("该简历已关联");
        }

        UserResume userResume = new UserResume();
        userResume.setUserId(userId);
        userResume.setResumeId(resumeId);
        userResumeMapper.insert(userResume);

        log.info("简历关联成功: userId={}, resumeId={}", userId, resumeId);
    }

    /**
     * 获取用户的所有简历ID
     */
    @Override
    public List<Long> getResumeIdsByUserId(Long userId) {
        return userResumeMapper.selectList(
            new LambdaQueryWrapper<UserResume>()
                .eq(UserResume::getUserId, userId)
                .orderByDesc(UserResume::getCreatedAt)
        ).stream()
            .map(UserResume::getResumeId)
            .collect(Collectors.toList());
    }

    /**
     * 移除用户简历关联
     */
    @Override
    public void removeResumeFromUser(Long userId, Long resumeId) {
        userResumeMapper.delete(
            new LambdaQueryWrapper<UserResume>()
                .eq(UserResume::getUserId, userId)
                .eq(UserResume::getResumeId, resumeId)
        );
        log.info("简历关联移除: userId={}, resumeId={}", userId, resumeId);
    }

    @Override
    public void updateUser(Long userId, String nickname, String phone, String email) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        if (nickname != null) user.setNickname(nickname);
        if (phone != null) user.setPhone(phone);
        if (email != null) user.setEmail(email);
        userMapper.updateById(user);
        log.info("用户信息更新: userId={}", userId);
    }
}
