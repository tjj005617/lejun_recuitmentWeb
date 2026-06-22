package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 面试问答记录表
 * 每轮面试对应一条记录，存储该轮的问题、回答、评分和反馈
 * 问题在面试创建时由 AI 批量生成，回答和评分在 WebSocket 交互中逐步填充
 */
@Data
@TableName("interview_qa")
public class InterviewQA {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long interviewId;

    private Integer round;

    private String question;

    private String answer;

    private String scores;

    private String feedback;

    private String referenceAnswer;

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
