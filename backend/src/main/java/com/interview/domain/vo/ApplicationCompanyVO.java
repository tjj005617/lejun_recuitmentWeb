package com.interview.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 公司投递列表VO（HR工作台，跨职位投递汇总）
 */
@Data
public class ApplicationCompanyVO {

    private Long id;
    private Long jobId;
    private Long userId;
    private Long resumeId;
    private String status;
    private LocalDateTime appliedAt;
    private String hrRemark;
    private String jobTitle;
    /** 候选人昵称/用户名 */
    private String username;
}
