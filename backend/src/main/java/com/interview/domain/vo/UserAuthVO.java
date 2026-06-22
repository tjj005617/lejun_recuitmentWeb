package com.interview.domain.vo;

import lombok.Data;

/**
 * 用户认证VO（登录、注册响应）
 */
@Data
public class UserAuthVO {

    private Long id;
    private String username;
    private String nickname;
    private Integer roleType;
    private Long companyId;
    private String token;
}
