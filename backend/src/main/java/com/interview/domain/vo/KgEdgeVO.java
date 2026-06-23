package com.interview.domain.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 知识图谱边 VO
 */
@Data
public class KgEdgeVO {

    private Long id;
    private String uuid;
    /** 起始顶点ID */
    private Long fromId;
    /** 目标顶点ID */
    private Long toId;
    /** 起始顶点名称 */
    private String fromName;
    /** 目标顶点名称 */
    private String toName;
    /** 关系标签 */
    private String edgeLabel;
    /** 权重 */
    private BigDecimal weight;
    /** 关系描述 */
    private String description;
}
