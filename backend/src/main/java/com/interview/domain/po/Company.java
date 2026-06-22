package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 公司表
 */
@Data
@TableName("company")
public class Company {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** HR用户ID */
    private Long userId;

    /** 公司名称 */
    private String name;

    /** 公司Logo */
    private String logo;

    /** 所属行业 */
    private String industry;

    /** 公司规模 */
    private String scale;

    /** 公司类型 */
    private String type;

    /** 所在城市（冗余字段，兼容旧数据） */
    private String city;

    /** 省市区ID（区级，level=3） */
    private Long regionId;

    /** 详细地址（街道门牌号） */
    private String address;

    /** 公司官网 */
    private String website;

    /** 公司介绍 */
    private String description;

    /** 浏览量 */
    private Long viewCount;

    /** 关注数 */
    private Long followCount;

    /** 完整省市区路径（非数据库字段，查询时填充） */
    @TableField(exist = false)
    private String regionPath;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableLogic
    private Integer deleted;
}
