package com.interview.service;

import java.util.List;
import java.util.Map;

/**
 * Milvus 向量数据库服务接口
 * 管理 job_vectors 集合的 CRUD 和向量搜索
 */
public interface MilvusService {

    /**
     * 初始化集合（启动时调用）
     * 如果集合不存在则创建，已存在则跳过
     */
    void initCollection();

    /**
     * 插入/更新岗位向量
     * @param jobIds 岗位 ID 列表
     * @param vectors 对应的向量列表
     * @param categoryIds 分类 ID 列表
     * @param cities 城市列表
     * @param statuses 状态列表
     */
    void upsertJobVectors(List<Long> jobIds, List<List<Float>> vectors,
                          List<Long> categoryIds, List<String> cities, List<String> statuses);

    /**
     * 删除岗位向量
     * @param jobIds 要删除的岗位 ID 列表
     */
    void deleteJobVectors(List<Long> jobIds);

    /**
     * 向量相似度搜索
     * @param queryVector 查询向量
     * @param filters 可选的标量过滤条件（如 city、categoryId）
     * @param topK 返回数量
     * @return 匹配的岗位 ID 列表（按相似度降序）
     */
    List<Long> searchSimilarJobs(List<Float> queryVector, Map<String, String> filters, int topK);
}
