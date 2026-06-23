package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识图谱顶点（提取的知识点）
 */
@Data
@TableName("kg_vertex")
public class KgVertex {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** UUID */
    private String uuid;

    /** 来源文档ID */
    private Long documentId;

    /** 所属大类ID */
    private Long categoryId;

    /** 知识点名称 */
    private String name;

    /** 类型：concept/technology/api/theory/example/keyword */
    private String vertexType;

    /** 子类型（AI动态分类） */
    private String subType;

    /** 简要描述 */
    private String description;

    /** 扩展属性JSON */
    private String properties;

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
