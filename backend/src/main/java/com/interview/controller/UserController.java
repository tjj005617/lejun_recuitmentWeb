package com.interview.controller;

import cn.hutool.core.bean.BeanUtil;
import com.interview.common.Result;
import com.interview.domain.po.User;
import com.interview.domain.vo.UserAuthVO;
import com.interview.domain.vo.UserInfoVO;
import com.interview.service.UserService;
import com.interview.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户控制器
 */
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    /**
     * 注册
     */
    @PostMapping("/register")
    public Result<?> register(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String nickname = (String) request.get("nickname");
        Integer roleType = request.get("roleType") != null ? Integer.parseInt(request.get("roleType").toString()) : 1;

        User user = userService.register(username, password, nickname, roleType);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRoleType());

        UserAuthVO vo = BeanUtil.copyProperties(user, UserAuthVO.class);
        vo.setToken(token);
        return Result.ok(vo);
    }

    /**
     * 登录
     */
    @PostMapping("/login")
    public Result<?> login(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String password = request.get("password");

        User user = userService.login(username, password);
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRoleType());

        UserAuthVO vo = BeanUtil.copyProperties(user, UserAuthVO.class);
        vo.setToken(token);
        return Result.ok(vo);
    }

    /**
     * 获取用户信息
     */
    @GetMapping("/{id}")
    public Result<?> getUser(@PathVariable Long id) {
        User user = userService.getUserById(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }
        UserInfoVO vo = BeanUtil.copyProperties(user, UserInfoVO.class);
        return Result.ok(vo);
    }

    /**
     * 更新用户信息
     */
    @PutMapping("/{id}")
    public Result<?> updateUser(@PathVariable Long id, @RequestBody Map<String, String> request) {
        String nickname = request.get("nickname");
        String phone = request.get("phone");
        String email = request.get("email");
        userService.updateUser(id, nickname, phone, email);
        return Result.ok("更新成功", null);
    }

    /**
     * 获取用户的简历ID列表
     */
    @GetMapping("/{id}/resumes")
    public Result<?> getUserResumes(@PathVariable Long id) {
        List<Long> resumeIds = userService.getResumeIdsByUserId(id);
        return Result.ok(resumeIds);
    }

    /**
     * 添加简历到用户（最多3份）
     */
    @PostMapping("/{id}/resumes")
    public Result<?> addResume(@PathVariable Long id, @RequestBody Map<String, Long> request) {
        Long resumeId = request.get("resumeId");
        userService.addResumeToUser(id, resumeId);
        return Result.ok("简历关联成功", null);
    }

    /**
     * 移除用户简历
     */
    @DeleteMapping("/{id}/resumes/{resumeId}")
    public Result<?> removeResume(@PathVariable Long id, @PathVariable Long resumeId) {
        userService.removeResumeFromUser(id, resumeId);
        return Result.ok("简历关联已移除", null);
    }
}
