package com.interview.service;

import com.interview.domain.vo.KgVertexVO;

import java.util.List;

/**
 * 知识图谱顶点服务接口
 */
public interface KgVertexService {

    /**
     * 按分类查询所有知识点
     */
    List<KgVertexVO> listByCategory(Long categoryId);

    /**
     * 按关键词搜索知识点
     */
    List<KgVertexVO> searchByName(String keyword, Long categoryId);

    /**
     * 获取知识点详情
     */
    KgVertexVO getById(Long id);

    /**
     * 获取知识点的一跳邻居
     */
    List<KgVertexVO> getNeighbors(Long vertexId);
}
