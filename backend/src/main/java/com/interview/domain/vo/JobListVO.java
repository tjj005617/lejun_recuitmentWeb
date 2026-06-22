package com.interview.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 职位列表VO（搜索、热门、我的职位共用）
 */
@Data
public class JobListVO {

    private Long id;
    private String title;
    private String companyName;
    private String city;
    private Integer salaryMin;
    private Integer salaryMax;
    private String jobType;
    private String status;
    private Integer viewCount;
    private Integer applyCount;
    private LocalDateTime publishedAt;
    private Long companyId;
    private Long userId;
    /** 负责HR名称（仅getMyJobs返回） */
    private String hrName;
}
