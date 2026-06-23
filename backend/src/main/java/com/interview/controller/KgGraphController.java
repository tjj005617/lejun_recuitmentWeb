package com.interview.controller;

import com.interview.common.Result;
import com.interview.service.KgGraphService;
import com.interview.service.KgVertexService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 知识图谱图数据控制器（公开接口）
 */
@RestController
@RequestMapping("/api/kg/graph")
@RequiredArgsConstructor
public class KgGraphController {

    private final KgGraphService graphService;
    private final KgVertexService vertexService;

    /**
     * 获取指定分类的完整图数据（节点+边，用于 ECharts 渲染）
     */
    @GetMapping("/{categoryId}")
    public Result<?> getGraphData(@PathVariable Long categoryId) {
        return Result.ok(graphService.getGraphData(categoryId));
    }

    /**
     * 获取指定知识点的子图（指定深度的邻居）
     */
    @GetMapping("/vertex/{id}")
    public Result<?> getSubGraph(@PathVariable Long id,
                                  @RequestParam(defaultValue = "1") int depth) {
        return Result.ok(graphService.getSubGraph(id, depth));
    }

    /**
     * 按关键词搜索知识点
     */
    @GetMapping("/search")
    public Result<?> searchVertices(
            @RequestParam String keyword,
            @RequestParam(required = false) Long categoryId) {
        return Result.ok(vertexService.searchByName(keyword, categoryId));
    }

    /**
     * 获取分类的统计信息
     */
    @GetMapping("/stats/{categoryId}")
    public Result<?> getStatistics(@PathVariable Long categoryId) {
        return Result.ok(graphService.getStatistics(categoryId));
    }

    /**
     * 获取知识点的一跳邻居
     */
    @GetMapping("/neighbor/{vertexId}")
    public Result<?> getNeighbors(@PathVariable Long vertexId) {
        return Result.ok(vertexService.getNeighbors(vertexId));
    }
}
