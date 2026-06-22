package com.interview.service;

import com.interview.domain.po.User;

import java.util.List;

/**
 * 用户服务接口
 * 定义用户注册、登录、信息管理、简历关联等能力
 * 支持三种角色：求职者(1)、HR(2)、管理员(3)
 */
public interface UserService {
    User register(String username, String password, String nickname, Integer roleType);
    User login(String username, String password);
    User getUserById(Long id);
    User getUserByUsername(String username);
    void addResumeToUser(Long userId, Long resumeId);
    List<Long> getResumeIdsByUserId(Long userId);
    void removeResumeFromUser(Long userId, Long resumeId);
    void updateUser(Long userId, String nickname, String phone, String email);
}
