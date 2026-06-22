package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公司-福利关联
 */
@Data
@TableName("company_benefit")
public class CompanyBenefit {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 公司ID */
    private Long companyId;

    /** 福利标签ID */
    private Long benefitTagId;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
