package com.interview.domain.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * 收藏职位VO
 */
@Data
public class JobFavoriteVO {

    private Long id;
    private String title;
    private String city;
    private Integer salaryMin;
    private Integer salaryMax;
    private String jobType;
    private Integer viewCount;
    private Integer applyCount;
    private LocalDateTime publishedAt;
    private String companyName;
    /** 职位是否已删除 */
    private Boolean jobDeleted;
}
