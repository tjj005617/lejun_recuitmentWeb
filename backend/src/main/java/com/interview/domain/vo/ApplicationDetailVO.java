package com.interview.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 职位投递详情VO（HR查看某职位的投递列表）
 */
@Data
public class ApplicationDetailVO {

    private Long id;
    private Long userId;
    private Long resumeId;
    private String status;
    private LocalDateTime appliedAt;
    /** 候选人昵称/用户名 */
    private String username;
}
