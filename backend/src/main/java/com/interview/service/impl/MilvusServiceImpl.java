package com.interview.service.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.interview.config.MilvusConfig;
import com.interview.domain.dto.MilvusSearchResult;
import com.interview.service.MilvusService;
import io.milvus.v2.client.MilvusClientV2;
import io.milvus.v2.common.IndexParam;
import io.milvus.v2.service.collection.request.AddFieldReq;
import io.milvus.v2.service.collection.request.CreateCollectionReq;
import io.milvus.v2.service.collection.request.HasCollectionReq;
import io.milvus.v2.service.collection.request.LoadCollectionReq;
import io.milvus.v2.service.index.request.CreateIndexReq;
import io.milvus.v2.service.vector.request.DeleteReq;
import io.milvus.v2.service.vector.request.InsertReq;
import io.milvus.v2.service.vector.request.SearchReq;
import io.milvus.v2.service.vector.request.data.FloatVec;
import io.milvus.v2.service.vector.response.SearchResp;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Milvus 服务实现（SDK 3.0.x）
 * 管理 job_vectors 集合：创建集合、插入向量、删除向量、相似度搜索
 */
@Slf4j
@Service
public class MilvusServiceImpl implements MilvusService {

    private final MilvusConfig milvusConfig;
    private final MilvusClientV2 client;
    private final Gson gson = new Gson();

    public MilvusServiceImpl(MilvusConfig milvusConfig, ObjectProvider<MilvusClientV2> clientProvider) {
        this.milvusConfig = milvusConfig;
        this.client = clientProvider.getIfAvailable();
    }

    /**
     * 检查 Milvus 是否可用
     */
    private void checkClient() {
        if (client == null) {
            throw new RuntimeException("Milvus 未连接，岗位匹配功能不可用");
        }
    }

    /**
     * 启动时自动初始化集合
     */
    @PostConstruct
    public void init() {
        if (client == null) {
            log.warn("Milvus 客户端未初始化，跳过集合初始化");
            return;
        }
        try {
            initCollection();
            log.info("Milvus 集合初始化完成: {}", milvusConfig.getCollection());
        } catch (Exception e) {
            log.warn("Milvus 集合初始化失败（Milvus 可能未启动）: {}", e.getMessage());
        }
    }

    @Override
    public void initCollection() {
        checkClient();
        String collectionName = milvusConfig.getCollection();

        // 检查集合是否已存在
        Boolean exists = client.hasCollection(HasCollectionReq.builder()
                .collectionName(collectionName)
                .build());
        if (Boolean.TRUE.equals(exists)) {
            log.info("集合已存在: {}", collectionName);
            return;
        }

        log.info("创建集合: {}", collectionName);

        // 1. 构建 Schema
        CreateCollectionReq.CollectionSchema schema = client.createSchema();

        // 主键字段（对应 MySQL job.id）
        schema.addField(AddFieldReq.builder()
                .fieldName("id")
                .dataType(io.milvus.v2.common.DataType.Int64)
                .isPrimaryKey(true)
                .autoID(false)
                .build());

        // 向量字段
        schema.addField(AddFieldReq.builder()
                .fieldName("vector")
                .dataType(io.milvus.v2.common.DataType.FloatVector)
                .dimension(milvusConfig.getDimension())
                .build());

        // 岗位 ID（冗余，方便过滤）
        schema.addField(AddFieldReq.builder()
                .fieldName("job_id")
                .dataType(io.milvus.v2.common.DataType.Int64)
                .build());

        // 岗位分类 ID
        schema.addField(AddFieldReq.builder()
                .fieldName("category_id")
                .dataType(io.milvus.v2.common.DataType.Int64)
                .build());

        // 城市
        schema.addField(AddFieldReq.builder()
                .fieldName("city")
                .dataType(io.milvus.v2.common.DataType.VarChar)
                .maxLength(50)
                .build());

        // 岗位状态
        schema.addField(AddFieldReq.builder()
                .fieldName("status")
                .dataType(io.milvus.v2.common.DataType.VarChar)
                .maxLength(20)
                .build());

        // 2. 创建集合
        client.createCollection(CreateCollectionReq.builder()
                .collectionName(collectionName)
                .collectionSchema(schema)
                .numShards(2)
                .build());

        // 3. 创建向量索引（IVF_FLAT + COSINE）
        Map<String, Object> extraParams = new HashMap<>();
        extraParams.put("nlist", 128);

        client.createIndex(CreateIndexReq.builder()
                .collectionName(collectionName)
                .indexParams(List.of(
                        IndexParam.builder()
                                .fieldName("vector")
                                .indexType(IndexParam.IndexType.IVF_FLAT)
                                .metricType(IndexParam.MetricType.COSINE)
                                .extraParams(extraParams)
                                .build()
                ))
                .sync(Boolean.TRUE)
                .build());

        // 4. 加载集合到内存
        client.loadCollection(LoadCollectionReq.builder()
                .collectionName(collectionName)
                .build());

        log.info("集合创建并加载完成: {}", collectionName);
    }

    @Override
    public void upsertJobVectors(List<Long> jobIds, List<List<Float>> vectors,
                                 List<Long> categoryIds, List<String> cities, List<String> statuses) {
        checkClient();
        String collectionName = milvusConfig.getCollection();

        if (jobIds == null || jobIds.isEmpty()) {
            return;
        }

        // 先删除已有的向量（避免重复）
        deleteJobVectors(jobIds);

        // 构建 JSON 数据行
        List<JsonObject> data = new ArrayList<>();
        for (int i = 0; i < jobIds.size(); i++) {
            JsonObject row = new JsonObject();
            row.addProperty("id", jobIds.get(i));
            row.add("vector", gson.toJsonTree(vectors.get(i)));
            row.addProperty("job_id", jobIds.get(i));
            row.addProperty("category_id", categoryIds.get(i));
            row.addProperty("city", cities.get(i));
            row.addProperty("status", statuses.get(i));
            data.add(row);
        }

        client.insert(InsertReq.builder()
                .collectionName(collectionName)
                .data(data)
                .build());

        log.debug("插入岗位向量: count={}", jobIds.size());
    }

    @Override
    public void deleteJobVectors(List<Long> jobIds) {
        checkClient();
        String collectionName = milvusConfig.getCollection();

        if (jobIds == null || jobIds.isEmpty()) {
            return;
        }

        // 构建删除表达式：job_id in [1, 2, 3, ...]
        StringBuilder expr = new StringBuilder("job_id in [");
        for (int i = 0; i < jobIds.size(); i++) {
            if (i > 0) expr.append(", ");
            expr.append(jobIds.get(i));
        }
        expr.append("]");

        client.delete(DeleteReq.builder()
                .collectionName(collectionName)
                .filter(expr.toString())
                .build());

        log.debug("删除岗位向量: jobIds={}", jobIds);
    }

    @Override
    public List<Long> searchSimilarJobs(List<Float> queryVector, Map<String, String> filters, int topK) {
        checkClient();
        String collectionName = milvusConfig.getCollection();

        // 构建过滤表达式
        List<String> conditions = new ArrayList<>();
        conditions.add("status == \"active\"");

        if (filters != null) {
            if (filters.containsKey("city") && !filters.get("city").isEmpty()) {
                conditions.add("city == \"" + filters.get("city") + "\"");
            }
            if (filters.containsKey("categoryId") && !filters.get("categoryId").isEmpty()) {
                conditions.add("category_id == " + filters.get("categoryId"));
            }
        }

        String filterExpr = String.join(" && ", conditions);

        // 执行向量搜索
        SearchResp searchResp = client.search(SearchReq.builder()
                .collectionName(collectionName)
                .data(List.of(new FloatVec(queryVector)))
                .annsField("vector")
                .metricType(IndexParam.MetricType.COSINE)
                .topK(topK)
                .filter(filterExpr)
                .outputFields(List.of("job_id"))
                .build());

        // 提取结果中的 job_id
        List<Long> jobIds = new ArrayList<>();
        if (searchResp.getSearchResults() != null) {
            for (List<SearchResp.SearchResult> results : searchResp.getSearchResults()) {
                for (SearchResp.SearchResult result : results) {
                    Object jobIdObj = result.getEntity().get("job_id");
                    if (jobIdObj != null) {
                        jobIds.add(((Number) jobIdObj).longValue());
                    }
                }
            }
        }

        log.debug("向量搜索完成: 返回 {} 条结果", jobIds.size());
        return jobIds;
    }

    @Override
    public List<MilvusSearchResult> searchSimilarJobsWithScore(List<Float> queryVector, Map<String, String> filters, int topK) {
        checkClient();
        String collectionName = milvusConfig.getCollection();

        // 构建过滤表达式
        List<String> conditions = new ArrayList<>();
        conditions.add("status == \"active\"");

        if (filters != null) {
            if (filters.containsKey("city") && !filters.get("city").isEmpty()) {
                conditions.add("city == \"" + filters.get("city") + "\"");
            }
            if (filters.containsKey("categoryId") && !filters.get("categoryId").isEmpty()) {
                conditions.add("category_id == " + filters.get("categoryId"));
            }
        }

        String filterExpr = String.join(" && ", conditions);

        // 执行向量搜索
        SearchResp searchResp = client.search(SearchReq.builder()
                .collectionName(collectionName)
                .data(List.of(new FloatVec(queryVector)))
                .annsField("vector")
                .metricType(IndexParam.MetricType.COSINE)
                .topK(topK)
                .filter(filterExpr)
                .outputFields(List.of("job_id"))
                .build());

        // 提取结果中的 job_id 和 score
        List<MilvusSearchResult> results = new ArrayList<>();
        if (searchResp.getSearchResults() != null) {
            for (List<SearchResp.SearchResult> searchResults : searchResp.getSearchResults()) {
                for (SearchResp.SearchResult result : searchResults) {
                    Object jobIdObj = result.getEntity().get("job_id");
                    if (jobIdObj != null) {
                        Long jobId = ((Number) jobIdObj).longValue();
                        Float score = result.getScore();
                        results.add(new MilvusSearchResult(jobId, score));
                    }
                }
            }
        }

        log.debug("向量搜索完成: 返回 {} 条结果", results.size());
        return results;
    }
}
