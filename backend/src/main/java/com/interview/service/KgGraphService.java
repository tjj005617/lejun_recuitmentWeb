package com.interview.service;

import com.interview.domain.vo.KgGraphVO;
import com.interview.domain.vo.KgVertexVO;

import java.util.List;
import java.util.Map;

/**
 * 知识图谱图数据服务接口
 */
public interface KgGraphService {

    /**
     * 获取指定分类的完整图数据（节点+边，用于 ECharts 渲染）
     */
    KgGraphVO getGraphData(Long categoryId);

    /**
     * 获取指定知识点的子图（指定深度的邻居）
     */
    KgGraphVO getSubGraph(Long vertexId, int depth);

    /**
     * 获取分类的统计信息
     */
    Map<String, Object> getStatistics(Long categoryId);
}
