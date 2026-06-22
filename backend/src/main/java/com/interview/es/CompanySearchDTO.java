package com.interview.es;

import lombok.Data;

/**
 * 公司搜索请求参数
 */
@Data
public class CompanySearchDTO {

    /** 关键词（可选） */
    private String keyword;

    /** 城市（可选） */
    private String city;

    /** 行业（可选） */
    private String industry;

    /** 规模（可选） */
    private String scale;

    /** 排序方式：jobCount/viewCount/followCount（默认jobCount） */
    private String sort;

    /** 页码（默认1） */
    private Integer page = 1;

    /** 每页数量（默认20） */
    private Integer size = 20;
}
