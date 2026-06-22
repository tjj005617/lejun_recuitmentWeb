package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 职位表
 */
@Data
@TableName("job")
public class Job {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公司ID */
    private Long companyId;

    /** HR用户ID */
    private Long userId;

    /** 职位分类ID */
    private Long categoryId;

    /** 职位名称 */
    private String title;

    /** 工作城市 */
    private String city;

    /** 工作地点 */
    private String address;

    /** 薪资下限 */
    private Integer salaryMin;

    /** 薪资上限 */
    private Integer salaryMax;

    /** 工作类型：full_time/part_time/intern */
    private String jobType;

    /** 经验要求 */
    private String experience;

    /** 学历要求 */
    private String education;

    /** 职位描述 */
    private String description;

    /** 任职要求 */
    private String requirements;

    /** 状态：active/paused/closed */
    private String status;

    /** 浏览次数 */
    private Integer viewCount;

    /** 申请次数 */
    private Integer applyCount;

    private LocalDateTime publishedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableLogic
    private Integer deleted;
}
