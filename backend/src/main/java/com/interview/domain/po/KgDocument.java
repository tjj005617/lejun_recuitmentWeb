package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识图谱文档（上传的 MD 文件）
 */
@Data
@TableName("kg_document")
public class KgDocument {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** UUID */
    private String uuid;

    /** 文档标题 */
    private String title;

    /** 原始文件名 */
    private String fileName;

    /** MinIO object名称 */
    private String objectName;

    /** 所属大类ID */
    private Long categoryId;

    /** MD原文内容 */
    private String rawContent;

    /** 处理状态：pending/processing/completed/failed */
    private String parseStatus;

    /** 提取的知识点数量 */
    private Integer vertexCount;

    /** 生成的关系数量 */
    private Integer edgeCount;

    /** 处理失败原因 */
    private String errorMessage;

    /** 当前处理步骤：parse/classify/extract/analyze/adjacency */
    private String currentStep;

    /** 创建时间 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /** 创建人ID */
    @TableField(fill = FieldFill.INSERT)
    private Long createdBy;

    /** 更新时间 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    /** 修改人ID */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Long updatedBy;

    /** 删除标记 */
    @TableLogic
    private Integer deleted;
}
