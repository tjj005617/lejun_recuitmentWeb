package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 面试记录表
 * 记录每次面试的基本信息：关联用户、简历、岗位类型、轮次进度、状态和总分
 * 状态流转：IN_PROGRESS（进行中）→ COMPLETED（已完成）
 */
@Data
@TableName("interview")
public class Interview {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long resumeId;

    private String jobType;

    private Integer totalRounds;

    private Integer currentRound;

    private String status;

    private Double totalScore;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    private LocalDateTime completedAt;

    @TableLogic
    private Integer deleted;
}
