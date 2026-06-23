package com.interview.domain.po;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 邻接缓存表（一跳邻居快速查询）
 */
@Data
@TableName("kg_adjacency")
public class KgAdjacency {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 顶点ID */
    private Long vertexId;

    /** 邻居顶点ID */
    private Long neighborId;

    /** 直接关系标签 */
    private String directLabel;

    /** 方向：incoming/outgoing */
    private String direction;

    /** 权重 */
    private BigDecimal weight;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
