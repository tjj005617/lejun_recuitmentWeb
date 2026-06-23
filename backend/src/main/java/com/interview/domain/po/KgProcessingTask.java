package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识图谱处理任务（异步流水线追踪）
 */
@Data
@TableName("kg_processing_task")
public class KgProcessingTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 文档ID */
    private Long documentId;

    /** 当前步骤：parse/extract_vertices/analyze_edges/build_adjacency */
    private String step;

    /** 状态：running/completed/failed */
    private String status;

    /** 进度百分比 0-100 */
    private Integer progress;

    /** 处理结果摘要 */
    private String resultSummary;

    /** 失败原因 */
    private String errorMessage;

    /** 开始时间 */
    private LocalDateTime startedAt;

    /** 完成时间 */
    private LocalDateTime completedAt;
}
