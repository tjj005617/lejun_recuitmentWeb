package com.interview.service.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interview.domain.po.KgCategory;
import com.interview.domain.po.KgEdge;
import com.interview.domain.po.KgVertex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识图谱 AI 服务实现
 * MiMo 负责知识点提取，DeepSeek 负责关系分析
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class KgAiServiceImpl implements KgAiService {

    @Qualifier("mimoAiClient")
    private final AiClient mimoAiClient;

    @Qualifier("deepseekAiClient")
    private final AiClient deepseekAiClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public KgCategory classifyCategory(String mdContent, List<KgCategory> categories) {
        // 截取前2000字用于分类判断
        String preview = mdContent.length() > 2000 ? mdContent.substring(0, 2000) : mdContent;

        // 构建分类选项列表
        StringBuilder optionsBuilder = new StringBuilder();
        for (KgCategory cat : categories) {
            optionsBuilder.append("- ").append(cat.getName()).append(" (id: ").append(cat.getId()).append(")\n");
        }

        String systemPrompt = "你是一位技术文档分类专家。根据文档内容判断它属于哪个技术分类。";

        String userPrompt = """
                请判断以下文档属于哪个技术分类。

                可选分类：
                %s

                文档内容预览：
                %s

                要求：
                1. 根据文档的核心内容判断最匹配的分类
                2. 只返回分类名称（如 "Java"、"前端"），不要返回其他内容
                3. 如果无法判断，返回 "未知"

                返回纯文本，不要包含任何格式标记。
                """.formatted(optionsBuilder, preview);

        String response = mimoAiClient.call(systemPrompt, userPrompt, 64);
        String classifiedName = response.trim().replaceAll("^```.*\\n?", "").replaceAll("\\n?```$", "").trim();

        log.info("AI 自动分类结果: {}", classifiedName);

        // 匹配分类
        for (KgCategory cat : categories) {
            if (cat.getName().equals(classifiedName)) {
                return cat;
            }
        }
        // 模糊匹配（忽略大小写和空格）
        for (KgCategory cat : categories) {
            if (cat.getName().equalsIgnoreCase(classifiedName) || classifiedName.contains(cat.getName())) {
                return cat;
            }
        }
        log.warn("AI 分类结果无法匹配: {}", classifiedName);
        return null;
    }

    @Override
    public List<KgVertex> extractVertices(String mdContent, String categoryName) {
        // 截断过长的文档（避免超出 token 限制）
        String content = mdContent.length() > 8000 ? mdContent.substring(0, 8000) + "\n\n...(内容截断)" : mdContent;

        String systemPrompt = "你是一位技术知识图谱构建专家。你的任务是从技术文档中提取结构化的知识点（知识图谱的顶点）。";

        String userPrompt = """
                请从以下技术文档中提取所有关键知识点。

                文档分类：%s
                文档内容：
                %s

                要求：
                1. 每个知识点应该是一个具体的、可独立存在的技术概念
                2. 知识点粒度要适中：不要太粗（如"Java"），也不要太细（如"int"），应该是一个可解释的概念（如"Java泛型"、"HashMap实现原理"）
                3. 为每个知识点分类（vertex_type），可选类型：
                   - concept: 核心概念（如"依赖注入"、"事件循环"）
                   - technology: 技术/框架/工具（如"Spring Boot"、"Redis"）
                   - api: API/接口/方法（如"ConcurrentHashMap.put"、"Promise.all"）
                   - theory: 原理/底层机制（如"垃圾回收算法"、"TCP三次握手"）
                   - example: 示例/实践（如"单例模式的5种写法"）
                   - keyword: 关键术语/缩写（如"CAP定理"、"ACID"）
                4. 为每个知识点提供简要描述（一句话）
                5. 为每个知识点指定子类型(sub_type)，用于更细的分类
                6. 为每个知识点指定属性（properties），如：difficulty(简单/中等/困难), importance(高/中/低), tags(标签数组)

                返回纯JSON数组，格式：
                [
                  {
                    "name": "知识点名称",
                    "vertex_type": "concept",
                    "sub_type": "子类型",
                    "description": "简要描述",
                    "properties": {
                      "difficulty": "中等",
                      "importance": "高",
                      "tags": ["标签1", "标签2"]
                    }
                  }
                ]

                注意：
                - 返回纯JSON数组，不要包含markdown代码块标记
                - 知识点去重，相同知识点只出现一次
                - 知识点数量建议在15-40个之间，根据文档内容调整
                """.formatted(categoryName, content);

        String response = mimoAiClient.call(systemPrompt, userPrompt, 4096);
        log.info("MiMo 提取知识点响应长度: {}", response.length());

        // 解析 AI 返回的 JSON
        List<Map<String, Object>> parsed = parseJsonArray(response);

        return parsed.stream().map(item -> {
            KgVertex vertex = new KgVertex();
            vertex.setName((String) item.get("name"));
            vertex.setVertexType((String) item.get("vertex_type"));
            vertex.setSubType((String) item.get("sub_type"));
            vertex.setDescription((String) item.get("description"));
            // 将 properties 转为 JSON 字符串
            if (item.get("properties") != null) {
                try {
                    vertex.setProperties(objectMapper.writeValueAsString(item.get("properties")));
                } catch (Exception e) {
                    vertex.setProperties("{}");
                }
            }
            return vertex;
        }).filter(v -> v.getName() != null && !v.getName().isBlank())
                .collect(Collectors.toList());
    }

    @Override
    public List<KgEdge> analyzeEdges(List<KgVertex> vertices, String mdContent, String categoryName) {
        // 构建知识点名称列表供 AI 分析
        String vertexListJson;
        try {
            List<Map<String, String>> vertexList = vertices.stream().map(v -> {
                Map<String, String> map = new HashMap<>();
                map.put("id", String.valueOf(v.getId() != null ? v.getId() : v.getName().hashCode()));
                map.put("name", v.getName());
                map.put("type", v.getVertexType());
                map.put("description", v.getDescription());
                return map;
            }).collect(Collectors.toList());
            vertexListJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(vertexList);
        } catch (Exception e) {
            log.error("构建知识点列表JSON失败", e);
            return Collections.emptyList();
        }

        // 截断文档内容作为辅助上下文
        String summary = mdContent.length() > 4000 ? mdContent.substring(0, 4000) + "..." : mdContent;

        String systemPrompt = "你是一位技术知识图谱关系分析专家。你的任务是分析知识点之间的语义关系。";

        String userPrompt = """
                请分析以下知识点之间的关系。

                文档分类：%s
                知识点列表：
                %s

                文档原文摘要（用于辅助理解关系）：
                %s

                要求：
                1. 分析每对知识点之间是否存在有意义的语义关系
                2. 关系类型（edge_label）必须是以下之一：
                   - prerequisite: 前置知识（学A之前需要先学B）
                   - related: 相关知识（A和B有语义关联）
                   - extends: 继承/扩展（A是B的扩展或子概念）
                   - uses: 使用/依赖（A使用了B）
                   - contains: 包含（A包含B，如模块包含子模块）
                   - conflicts: 对比/冲突（A和B是对比关系或容易混淆）
                3. 为每个关系给出置信度权重(weight)，范围0.00-1.00：
                   - 1.00: 文档明确指出的关系
                   - 0.70-0.99: 根据上下文强烈推断的关系
                   - 0.40-0.69: 根据通用知识推断的关系
                4. 给出每个关系的简短描述

                返回纯JSON数组，格式：
                [
                  {
                    "from_name": "起始知识点名称",
                    "to_name": "目标知识点名称",
                    "edge_label": "prerequisite",
                    "weight": 0.85,
                    "description": "关系描述"
                  }
                ]

                注意：
                - 返回纯JSON数组，不要包含markdown代码块标记
                - from_name和to_name必须是上面给出的知识点名称之一
                - 每对知识点最多一种关系
                - 优先提取prerequisite和extends关系
                - 关系数量建议在20-60条之间
                """.formatted(categoryName, vertexListJson, summary);

        String response = deepseekAiClient.call(systemPrompt, userPrompt, 4096);
        log.info("DeepSeek 分析关系响应长度: {}", response.length());

        // 解析 AI 返回的 JSON
        List<Map<String, Object>> parsed = parseJsonArray(response);

        // 构建名称→ID映射
        Map<String, Long> nameToId = new HashMap<>();
        for (KgVertex v : vertices) {
            nameToId.put(v.getName(), v.getId() != null ? v.getId() : (long) v.getName().hashCode());
        }

        return parsed.stream().map(item -> {
            KgEdge edge = new KgEdge();
            String fromName = (String) item.get("from_name");
            String toName = (String) item.get("to_name");

            // 将名称解析为顶点ID
            Long fromId = nameToId.get(fromName);
            Long toId = nameToId.get(toName);
            if (fromId == null || toId == null || fromId.equals(toId)) return null;

            edge.setFromId(fromId);
            edge.setToId(toId);
            edge.setEdgeLabel((String) item.get("edge_label"));

            // 解析权重
            Object weightObj = item.get("weight");
            if (weightObj instanceof Number) {
                edge.setWeight(BigDecimal.valueOf(((Number) weightObj).doubleValue()));
            } else {
                edge.setWeight(new BigDecimal("0.50"));
            }

            edge.setDescription((String) item.get("description"));
            return edge;
        }).filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 解析 AI 返回的 JSON 数组（兼容 markdown 代码块包裹的情况）
     */
    private List<Map<String, Object>> parseJsonArray(String response) {
        try {
            // 清理可能的 markdown 代码块标记
            String cleaned = response.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```(json)?\\s*", "").replaceAll("\\s*```$", "");
            }
            return objectMapper.readValue(cleaned, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("JSON解析失败，尝试正则提取: {}", e.getMessage());
            // 尝试用正则提取 JSON 数组
            try {
                int start = response.indexOf('[');
                int end = response.lastIndexOf(']');
                if (start >= 0 && end > start) {
                    String json = response.substring(start, end + 1);
                    return objectMapper.readValue(json, new TypeReference<>() {});
                }
            } catch (Exception ex) {
                log.error("正则提取JSON也失败", ex);
            }
            return Collections.emptyList();
        }
    }
}
