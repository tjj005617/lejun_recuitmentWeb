package com.interview.domain.vo;

import lombok.Data;

import java.util.List;

/**
 * 知识图谱图数据 VO（用于 ECharts 渲染）
 */
@Data
public class KgGraphVO {

    /** 节点列表 */
    private List<KgVertexVO> nodes;
    /** 边列表 */
    private List<KgEdgeVO> edges;
    /** 分类信息 */
    private String categoryName;
    /** 总顶点数 */
    private Integer totalVertices;
    /** 总边数 */
    private Integer totalEdges;
}
