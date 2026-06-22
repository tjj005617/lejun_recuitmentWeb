package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 职位-福利关联
 */
@Data
@TableName("job_benefit")
public class JobBenefit {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 职位ID */
    private Long jobId;

    /** 福利标签ID */
    private Long benefitTagId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
