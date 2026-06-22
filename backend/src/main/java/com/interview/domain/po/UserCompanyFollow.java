package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户-公司关注关系表
 */
@Data
@TableName("user_company_follow")
public class UserCompanyFollow {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 用户ID */
    private Long userId;

    /** 公司ID */
    private Long companyId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
