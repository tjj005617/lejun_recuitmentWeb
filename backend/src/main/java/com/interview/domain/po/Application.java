package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 申请表
 */
@Data
@TableName("application")
public class Application {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 职位ID */
    private Long jobId;

    /** 求职者用户ID */
    private Long userId;

    /** 简历ID */
    private Long resumeId;

    /** 状态：pending/screening/interview/offer/rejected/withdrawn */
    private String status;

    /** HR备注 */
    private String hrRemark;

    /** 职位是否已删除 */
    private Integer jobDeleted;

    /** 拒绝原因 */
    private String rejectReason;

    private LocalDateTime appliedAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    @TableLogic
    private Integer deleted;
}
