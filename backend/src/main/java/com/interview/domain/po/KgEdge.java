package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 知识图谱边（知识点间的关系）
 */
@Data
@TableName("kg_edge")
public class KgEdge {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** UUID */
    private String uuid;

    /** 起始顶点ID */
    private Long fromId;

    /** 目标顶点ID */
    private Long toId;

    /** 关系标签：prerequisite/related/extends/uses/contains/conflicts */
    private String edgeLabel;

    /** 权重 0.00-1.00 */
    private BigDecimal weight;

    /** 关系描述 */
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
