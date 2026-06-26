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

    /** 面试模式：resume(简历分析) / topic(八股栏目) / hybrid(混合) */
    private String interviewMode;

    /** 八股分类ID列表，逗号分隔（如 "1,3,5"），仅topic/hybrid模式有值 */
    private String categoryIds;

    /** 题型：essay(简答题) / choice(选择题) */
    private String questionType;

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
