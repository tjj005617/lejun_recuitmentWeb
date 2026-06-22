package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户档案实体类（扩展个人信息）
 */
@Data
@TableName("user_profile")
public class UserProfile {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 关联用户ID */
    private Long userId;

    /** 最高学历 */
    private String education;

    /** 个人优势/自我介绍 */
    private String personalSummary;

    /** 求职状态：looking-找工作 not_looking-暂不考虑 available-随时到岗 */
    private String jobStatus;

    /** 期望城市 */
    private String expectCity;

    /** 期望最低薪资 */
    private Integer expectSalaryMin;

    /** 期望最高薪资 */
    private Integer expectSalaryMax;

    /** 期望职位类型 */
    private String expectJobType;

    /** 工作/实习经历 JSON */
    private String workExperience;

    /** 项目经历 JSON */
    private String projectExperience;

    /** 教育经历 JSON */
    private String educationExperience;

    /** 资格证书 JSON */
    private String certificates;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
