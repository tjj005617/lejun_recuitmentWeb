package com.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.interview.domain.po.*;
import com.interview.mapper.*;
import com.interview.service.KgProcessingService;
import com.interview.service.ai.KgAiService;
import com.interview.sse.KgSseManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 知识图谱处理流水线服务实现
 * 异步执行：解析文档 → 提取知识点 → 分析关系 → 构建邻接缓存
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KgProcessingServiceImpl implements KgProcessingService {

    private final KgDocumentMapper documentMapper;
    private final KgVertexMapper vertexMapper;
    private final KgEdgeMapper edgeMapper;
    private final KgAdjacencyMapper adjacencyMapper;
    private final KgProcessingTaskMapper taskMapper;
    private final KgCategoryMapper categoryMapper;
    private final KgAiService kgAiService;
    private final KgSseManager kgSseManager;

    @Override
    @Async("kgExecutor")
    public void processDocument(Long documentId) {
        KgDocument doc = documentMapper.selectById(documentId);
        if (doc == null) {
            log.error("文档不存在: documentId={}", documentId);
            return;
        }

        log.info("开始处理文档: id={}, title={}", doc.getId(), doc.getTitle());

        try {
            // Step 1: 解析文档内容 + 自动分类
            saveTask(doc.getId(), "parse", "running", 10);
            updateDocStatus(doc.getId(), "processing", null, "parse");
            sendProgress(doc.getId(), "parse", "processing", 10, 0, 0, null);
            String content = doc.getRawContent();
            if (content == null || content.isBlank()) {
                doc.setErrorMessage("文档内容为空");
                documentMapper.updateById(doc);
                return;
            }

            // 如果文档没有分类，AI 自动判断分类
            if (doc.getCategoryId() == null) {
                log.info("文档未指定分类，AI 自动分类中: documentId={}", doc.getId());
                List<KgCategory> allCategories = categoryMapper.selectList(
                        new LambdaQueryWrapper<KgCategory>().eq(KgCategory::getStatus, "active"));
                KgCategory matched = kgAiService.classifyCategory(content, allCategories);
                if (matched != null) {
                    doc.setCategoryId(matched.getId());
                    documentMapper.updateById(doc);
                    log.info("AI 自动分类完成: documentId={}, category={}", doc.getId(), matched.getName());
                } else {
                    // 无法判断时默认第一个分类
                    if (!allCategories.isEmpty()) {
                        doc.setCategoryId(allCategories.get(0).getId());
                        documentMapper.updateById(doc);
                        log.warn("AI 无法判断分类，使用默认分类: {}", allCategories.get(0).getName());
                    }
                }
            }
            completeTask(doc.getId(), "parse", "completed", 20);
            sendProgress(doc.getId(), "parse", "processing", 20, 0, 0, null);

            // Step 2: 通过 AI 提取知识点（顶点）
            saveTask(doc.getId(), "extract_vertices", "running", 30);
            updateDocStatus(doc.getId(), "processing", null, "extract");
            sendProgress(doc.getId(), "extract", "processing", 30, 0, 0, null);
            KgCategory category = categoryMapper.selectById(doc.getCategoryId());
            String categoryName = category != null ? category.getName() : "未知";
            List<KgVertex> vertices = kgAiService.extractVertices(content, categoryName);

            // 持久化顶点
            for (KgVertex v : vertices) {
                v.setDocumentId(doc.getId());
                v.setCategoryId(doc.getCategoryId());
                v.setUuid(UUID.randomUUID().toString());
                vertexMapper.insert(v);
            }
            doc.setVertexCount(vertices.size());
            documentMapper.updateById(doc);
            completeTask(doc.getId(), "extract_vertices", "completed", 60);
            log.info("文档知识点提取完成: documentId={}, vertexCount={}", doc.getId(), vertices.size());
            sendProgress(doc.getId(), "extract", "processing", 60, vertices.size(), 0, null);

            // Step 3: 通过 AI 分析关系（边）
            saveTask(doc.getId(), "analyze_edges", "running", 65);
            updateDocStatus(doc.getId(), "processing", null, "analyze");
            sendProgress(doc.getId(), "analyze", "processing", 65, vertices.size(), 0, null);
            // analyzeEdges() 内部已将 from_name/to_name 转为数据库 ID
            List<KgEdge> edges = kgAiService.analyzeEdges(vertices, content, categoryName);

            int edgeCount = 0;
            for (KgEdge e : edges) {
                if (e.getFromId() == null || e.getToId() == null) continue;
                if (e.getFromId().equals(e.getToId())) continue;
                e.setUuid(UUID.randomUUID().toString());
                try {
                    edgeMapper.insert(e);
                    edgeCount++;
                } catch (Exception ex) {
                    log.debug("跳过重复边: {} -> {}", e.getFromId(), e.getToId());
                }
            }
            doc.setEdgeCount(edgeCount);
            documentMapper.updateById(doc);
            completeTask(doc.getId(), "analyze_edges", "completed", 85);
            log.info("文档关系分析完成: documentId={}, edgeCount={}", doc.getId(), edgeCount);
            sendProgress(doc.getId(), "analyze", "processing", 85, vertices.size(), edgeCount, null);

            // Step 4: 构建邻接缓存
            saveTask(doc.getId(), "build_adjacency", "running", 90);
            updateDocStatus(doc.getId(), "processing", null, "adjacency");
            sendProgress(doc.getId(), "adjacency", "processing", 90, vertices.size(), edgeCount, null);
            buildAdjacencyCache(vertices);
            completeTask(doc.getId(), "build_adjacency", "completed", 100);
            sendProgress(doc.getId(), "adjacency", "processing", 100, vertices.size(), edgeCount, null);

            // Step 5: 更新文档状态为完成
            updateDocStatus(doc.getId(), "completed", null, null);
            sendProgress(doc.getId(), "", "completed", 100, vertices.size(), edgeCount, null);
            log.info("文档处理完成: id={}, vertices={}, edges={}", doc.getId(), vertices.size(), edgeCount);

        } catch (Exception e) {
            log.error("文档处理失败: documentId={}", documentId, e);
            updateDocStatus(documentId, "failed", e.getMessage());
            updateCurrentTask(documentId, "failed", e.getMessage());
            sendProgress(documentId, "", "failed", 0, 0, 0, e.getMessage());
        }
    }

    /**
     * 构建邻接缓存表（一跳邻居快速查询）
     */
    private void buildAdjacencyCache(List<KgVertex> vertices) {
        for (KgVertex v : vertices) {
            // 出边邻居
            List<KgEdge> outEdges = edgeMapper.selectList(
                    new LambdaQueryWrapper<KgEdge>().eq(KgEdge::getFromId, v.getId()));
            for (KgEdge e : outEdges) {
                upsertAdjacency(v.getId(), e.getToId(), e.getEdgeLabel(), "outgoing", e.getWeight());
            }

            // 入边邻居
            List<KgEdge> inEdges = edgeMapper.selectList(
                    new LambdaQueryWrapper<KgEdge>().eq(KgEdge::getToId, v.getId()));
            for (KgEdge e : inEdges) {
                upsertAdjacency(v.getId(), e.getFromId(), e.getEdgeLabel(), "incoming", e.getWeight());
            }
        }
    }

    /**
     * 插入或更新邻接记录
     */
    private void upsertAdjacency(Long vertexId, Long neighborId, String label, String direction, BigDecimal weight) {
        KgAdjacency adj = new KgAdjacency();
        adj.setVertexId(vertexId);
        adj.setNeighborId(neighborId);
        adj.setDirectLabel(label);
        adj.setDirection(direction);
        adj.setWeight(weight);
        adj.setCreatedAt(LocalDateTime.now());

        try {
            adjacencyMapper.insert(adj);
        } catch (Exception e) {
            // 唯一键冲突，如果新权重更大则更新
            KgAdjacency update = new KgAdjacency();
            update.setWeight(weight);
            adjacencyMapper.update(update,
                    new LambdaQueryWrapper<KgAdjacency>()
                            .eq(KgAdjacency::getVertexId, vertexId)
                            .eq(KgAdjacency::getNeighborId, neighborId)
                            .eq(KgAdjacency::getDirectLabel, label)
                            .apply("weight < {0}", weight));
        }
    }

    /**
     * 保存处理任务记录
     */
    private void saveTask(Long documentId, String step, String status, int progress) {
        KgProcessingTask task = new KgProcessingTask();
        task.setDocumentId(documentId);
        task.setStep(step);
        task.setStatus(status);
        task.setProgress(progress);
        task.setStartedAt(LocalDateTime.now());
        taskMapper.insert(task);
    }

    /**
     * 完成处理任务
     */
    private void completeTask(Long documentId, String step, String status, int progress) {
        KgProcessingTask task = taskMapper.selectOne(
                new LambdaQueryWrapper<KgProcessingTask>()
                        .eq(KgProcessingTask::getDocumentId, documentId)
                        .eq(KgProcessingTask::getStep, step)
                        .eq(KgProcessingTask::getStatus, "running")
                        .orderByDesc(KgProcessingTask::getId)
                        .last("LIMIT 1")
        );
        if (task != null) {
            task.setStatus(status);
            task.setProgress(progress);
            task.setCompletedAt(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    /**
     * 更新文档处理状态
     */
    private void updateDocStatus(Long documentId, String status) {
        updateDocStatus(documentId, status, null, null);
    }

    private void updateDocStatus(Long documentId, String status, String errorMessage) {
        updateDocStatus(documentId, status, errorMessage, null);
    }

    private void updateDocStatus(Long documentId, String status, String errorMessage, String currentStep) {
        // 使用 LambdaUpdateWrapper 确保 currentStep=null 也能被更新
        // （MyBatis-Plus 默认 updateStrategy=NOT_NULL 会跳过 null 字段）
        var update = new LambdaUpdateWrapper<KgDocument>()
                .eq(KgDocument::getId, documentId)
                .set(KgDocument::getParseStatus, status)
                .set(KgDocument::getCurrentStep, currentStep);
        if (errorMessage != null) {
            update.set(KgDocument::getErrorMessage, errorMessage);
        }
        documentMapper.update(null, update);
    }

    /**
     * 更新当前任务状态（失败时）
     */
    private void updateCurrentTask(Long documentId, String status, String errorMessage) {
        KgProcessingTask task = taskMapper.selectOne(
                new LambdaQueryWrapper<KgProcessingTask>()
                        .eq(KgProcessingTask::getDocumentId, documentId)
                        .eq(KgProcessingTask::getStatus, "running")
                        .orderByDesc(KgProcessingTask::getId)
                        .last("LIMIT 1")
        );
        if (task != null) {
            task.setStatus(status);
            task.setErrorMessage(errorMessage);
            task.setCompletedAt(LocalDateTime.now());
            taskMapper.updateById(task);
        }
    }

    /**
     * 通过 SSE 推送处理进度给前端
     */
    private void sendProgress(Long documentId, String step, String status,
                              int progress, int vertexCount, int edgeCount, String errorMessage) {
        Map<String, Object> data = new HashMap<>();
        data.put("documentId", documentId);
        data.put("step", step);
        data.put("status", status);
        data.put("progress", progress);
        data.put("vertexCount", vertexCount);
        data.put("edgeCount", edgeCount);
        if (errorMessage != null) {
            data.put("errorMessage", errorMessage);
        }
        kgSseManager.sendProgress(documentId, data);
    }
}
