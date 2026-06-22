package com.interview.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 公司列表VO（搜索、分页列表共用）
 */
@Data
public class CompanyListVO {

    private Long id;
    private String name;
    private String industry;
    private String scale;
    private String type;
    private Long regionId;
    /** 完整省市区路径 */
    private String regionPath;
    private String city;
    private String address;
    private String website;
    private String description;
    /** 福利标签名称列表 */
    private List<String> benefits;
}
