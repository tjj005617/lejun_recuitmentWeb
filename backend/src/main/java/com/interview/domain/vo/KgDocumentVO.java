package com.interview.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识图谱文档 VO
 */
@Data
public class KgDocumentVO {

    private Long id;
    private String uuid;
    /** 文档标题 */
    private String title;
    /** 原始文件名 */
    private String fileName;
    /** 所属分类名称 */
    private String categoryName;
    /** 处理状态 */
    private String parseStatus;
    /** 当前处理步骤 */
    private String currentStep;
    /** 知识点数量 */
    private Integer vertexCount;
    /** 关系数量 */
    private Integer edgeCount;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 失败原因 */
    private String errorMessage;
}
