package com.interview.controller;

import com.interview.common.Result;
import com.interview.domain.po.UserProfile;
import com.interview.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户档案控制器
 */
@RestController
@RequestMapping("/api/user-profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * 获取用户档案
     */
    @GetMapping("/{userId}")
    public Result<?> getProfile(@PathVariable Long userId) {
        UserProfile profile = userProfileService.getProfile(userId);
        return Result.ok(profile);
    }

    /**
     * 保存/更新用户档案
     */
    @PutMapping("/{userId}")
    public Result<?> saveProfile(@PathVariable Long userId, @RequestBody UserProfile profile) {
        userProfileService.saveProfile(userId, profile);
        return Result.ok("档案保存成功");
    }
}
