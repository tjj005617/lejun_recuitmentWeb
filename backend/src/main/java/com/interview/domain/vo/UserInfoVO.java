package com.interview.domain.vo;

import lombok.Data;

/**
 * 用户信息VO（个人信息页）
 */
@Data
public class UserInfoVO {

    private Long id;
    private String username;
    private String nickname;
    private String phone;
    private String email;
    private Integer roleType;
    private Long companyId;
}
