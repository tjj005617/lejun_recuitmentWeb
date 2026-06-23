package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识图谱大类（每个大类对应一个 MinIO 桶）
 */
@Data
@TableName("kg_category")
public class KgCategory {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 分类名称 */
    private String name;

    /** MinIO bucket名称 */
    private String bucket;

    /** 图标URL或emoji */
    private String icon;

    /** 排序权重 */
    private Integer sortOrder;

    /** 状态：active-启用 disabled-禁用 */
    private String status;

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
