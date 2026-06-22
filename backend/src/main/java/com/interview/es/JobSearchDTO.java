package com.interview.es;

import lombok.Data;

/**
 * 职位搜索请求参数
 */
@Data
public class JobSearchDTO {

    /** 关键词（可选） */
    private String keyword;

    /** 城市（可选） */
    private String city;

    /** 分类ID（可选） */
    private Long categoryId;

    /** 经验要求（可选） */
    private String experience;

    /** 学历要求（可选） */
    private String education;

    /** 最低薪资（可选） */
    private Integer salaryMin;

    /** 最高薪资（可选） */
    private Integer salaryMax;

    /** 工作类型（可选） */
    private String jobType;

    /** HR用户ID（可选，筛选某HR负责的岗位） */
    private Long hrUserId;

    /** 排序方式：publishedAt/salary/viewCount/applyCount（默认publishedAt） */
    private String sort;

    /** 页码（默认1） */
    private Integer page = 1;

    /** 每页数量（默认20） */
    private Integer size = 20;
}
