package com.interview.service.ai;

import com.interview.domain.po.KgCategory;
import com.interview.domain.po.KgEdge;
import com.interview.domain.po.KgVertex;

import java.util.List;

/**
 * 知识图谱 AI 服务接口
 * 负责从文档中提取知识点（顶点）和分析关系（边）
 */
public interface KgAiService {

    /**
     * 自动判断文档所属分类
     *
     * @param mdContent Markdown 文档内容（截取前2000字用于判断）
     * @param categories 可选分类列表
     * @return 匹配的分类，无法判断时返回 null
     */
    KgCategory classifyCategory(String mdContent, List<KgCategory> categories);

    /**
     * 从 Markdown 文档中提取知识点（顶点）
     *
     * @param mdContent    Markdown 文档内容
     * @param categoryName 分类名称（如 "Java"、"前端"）
     * @return 提取的知识点列表（未持久化，仅返回对象）
     */
    List<KgVertex> extractVertices(String mdContent, String categoryName);

    /**
     * 分析知识点之间的关系（边）
     *
     * @param vertices     已提取的知识点列表
     * @param mdContent    原始文档内容（辅助理解关系）
     * @param categoryName 分类名称
     * @return 关系列表（fromId/toId 暂存为知识点名称的hashCode，需调用方转换为实际ID）
     */
    List<KgEdge> analyzeEdges(List<KgVertex> vertices, String mdContent, String categoryName);
}
