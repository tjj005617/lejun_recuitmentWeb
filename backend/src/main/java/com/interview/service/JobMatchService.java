package com.interview.service;

import com.interview.vo.JobMatchResult;

import java.util.List;
import java.util.Map;

/**
 * 岗位匹配服务接口
 * 基于 Milvus 向量搜索实现简历-岗位智能匹配
 */
public interface JobMatchService {

    /**
     * 根据简历匹配岗位
     * @param resumeId 简历 ID
     * @param topN 返回数量
     * @param filters 可选过滤条件（city、categoryId）
     * @return 匹配结果列表（按匹配度降序）
     */
    List<JobMatchResult> matchJobs(Long resumeId, int topN, Map<String, String> filters);

    /**
     * 同步单个岗位到 Milvus
     * @param jobId 岗位 ID
     */
    void syncJobToMilvus(Long jobId);

    /**
     * 批量同步所有 active 岗位到 Milvus
     */
    void syncAllJobsToMilvus();

    /**
     * 从 Milvus 删除岗位向量
     * @param jobId 岗位 ID
     */
    void deleteJobFromMilvus(Long jobId);
}
