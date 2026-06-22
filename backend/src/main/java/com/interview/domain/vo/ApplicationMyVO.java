package com.interview.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 我的投递记录VO（求职者视角）
 */
@Data
public class ApplicationMyVO {

    private Long id;
    private Long jobId;
    private String status;
    private LocalDateTime appliedAt;
    private String jobTitle;
    /** 职位是否已删除 */
    private Boolean jobDeleted;
    private String companyName;
}
