package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 福利标签
 */
@Data
@TableName("benefit_tag")
public class BenefitTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 标签名称 */
    private String name;

    /** 类型：company-公司福利 job-岗位福利 */
    private String type;

    /** 排序权重（越大越靠前） */
    private Integer sortOrder;

    /** 是否启用：1=启用 0=禁用 */
    private Integer enabled;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
