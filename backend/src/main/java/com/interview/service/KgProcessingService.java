package com.interview.service;

/**
 * 知识图谱处理流水线服务接口
 */
public interface KgProcessingService {

    /**
     * 异步处理文档：解析 → 提取知识点 → 分析关系 → 构建邻接缓存
     *
     * @param documentId 文档ID
     */
    void processDocument(Long documentId);
}
