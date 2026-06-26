package com.interview.service;

import java.util.List;

/**
 * 文本向量化服务接口
 * 将文本通过 DashScope text-embedding-v3 模型转换为向量
 */
public interface EmbeddingService {

    /**
     * 单条文本转向量
     * @param text 输入文本
     * @return 1024维向量
     */
    List<Float> embed(String text);

    /**
     * 批量文本转向量
     * @param texts 输入文本列表
     * @return 向量列表，与输入一一对应
     */
    List<List<Float>> embedBatch(List<String> texts);
}
