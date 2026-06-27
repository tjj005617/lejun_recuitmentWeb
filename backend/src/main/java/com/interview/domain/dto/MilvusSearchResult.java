package com.interview.domain.dto;

import lombok.Data;

/**
 * Milvus 向量搜索结果（含相似度分数）
 */
@Data
public class MilvusSearchResult {

    /** 岗位 ID */
    private Long jobId;

    /** 相似度分数（0-1） */
    private Float score;

    public MilvusSearchResult(Long jobId, Float score) {
        this.jobId = jobId;
        this.score = score;
    }
}
