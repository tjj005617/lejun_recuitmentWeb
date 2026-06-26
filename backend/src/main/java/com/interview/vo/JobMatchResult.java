package com.interview.vo;

import lombok.Data;

/**
 * 岗位匹配结果 VO
 */
@Data
public class JobMatchResult {

    /** 岗位 ID */
    private Long jobId;

    /** 岗位名称 */
    private String title;

    /** 公司名称 */
    private String companyName;

    /** 公司 Logo */
    private String companyLogo;

    /** 城市 */
    private String city;

    /** 薪资范围 */
    private Integer salaryMin;
    private Integer salaryMax;

    /** 经验要求 */
    private String experience;

    /** 学历要求 */
    private String education;

    /** 岗位分类名称 */
    private String categoryName;

    /** 匹配度（0-100） */
    private Integer matchScore;

    /** 岗位描述（截取前200字） */
    private String description;
}
