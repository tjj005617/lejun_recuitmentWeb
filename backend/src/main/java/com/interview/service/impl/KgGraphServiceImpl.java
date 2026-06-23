package com.interview.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.domain.po.KgCategory;
import com.interview.domain.po.KgEdge;
import com.interview.domain.po.KgVertex;
import com.interview.domain.vo.KgEdgeVO;
import com.interview.domain.vo.KgGraphVO;
import com.interview.domain.vo.KgVertexVO;
import com.interview.mapper.KgCategoryMapper;
import com.interview.mapper.KgEdgeMapper;
import com.interview.mapper.KgVertexMapper;
import com.interview.service.KgGraphService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 知识图谱图数据服务实现
 */
@Service
@RequiredArgsConstructor
public class KgGraphServiceImpl implements KgGraphService {

    private final KgVertexMapper vertexMapper;
    private final KgEdgeMapper edgeMapper;
    private final KgCategoryMapper categoryMapper;

    @Override
    public KgGraphVO getGraphData(Long categoryId) {
        // 查询该分类下所有顶点
        List<KgVertex> vertices = vertexMapper.selectList(
                new LambdaQueryWrapper<KgVertex>()
                        .eq(KgVertex::getCategoryId, categoryId)
                        .eq(KgVertex::getDeleted, 0)
        );

        if (vertices.isEmpty()) {
            KgGraphVO empty = new KgGraphVO();
            empty.setNodes(Collections.emptyList());
            empty.setEdges(Collections.emptyList());
            empty.setTotalVertices(0);
            empty.setTotalEdges(0);
            KgCategory cat = categoryMapper.selectById(categoryId);
            if (cat != null) empty.setCategoryName(cat.getName());
            return empty;
        }

        // 获取顶点ID集合
        Set<Long> vertexIds = vertices.stream()
                .map(KgVertex::getId)
                .collect(Collectors.toSet());

        // 查询涉及这些顶点的所有边
        List<KgEdge> allEdges = edgeMapper.selectList(
                new LambdaQueryWrapper<KgEdge>()
                        .in(KgEdge::getFromId, vertexIds)
                        .or()
                        .in(KgEdge::getToId, vertexIds)
        );

        // 构建顶点名称映射（用于边的 fromName/toName）
        Map<Long, String> nameMap = vertices.stream()
                .collect(Collectors.toMap(KgVertex::getId, KgVertex::getName));

        // 统计每个顶点的连接数
        Map<Long, Integer> edgeCountMap = new HashMap<>();
        for (KgEdge e : allEdges) {
            edgeCountMap.merge(e.getFromId(), 1, Integer::sum);
            edgeCountMap.merge(e.getToId(), 1, Integer::sum);
        }

        // 构建顶点VO列表
        List<KgVertexVO> nodeVOs = vertices.stream().map(v -> {
            KgVertexVO vo = new KgVertexVO();
            vo.setId(v.getId().longValue());
            vo.setUuid(v.getUuid());
            vo.setName(v.getName());
            vo.setVertexType(v.getVertexType());
            vo.setSubType(v.getSubType());
            vo.setDescription(v.getDescription());
            vo.setProperties(v.getProperties());
            vo.setEdgeCount(edgeCountMap.getOrDefault(v.getId(), 0));
            return vo;
        }).collect(Collectors.toList());

        // 构建边VO列表
        List<KgEdgeVO> edgeVOs = allEdges.stream().map(e -> {
            KgEdgeVO vo = new KgEdgeVO();
            vo.setId(e.getId().longValue());
            vo.setUuid(e.getUuid());
            vo.setFromId(e.getFromId());
            vo.setToId(e.getToId());
            vo.setFromName(nameMap.get(e.getFromId()));
            vo.setToName(nameMap.get(e.getToId()));
            vo.setEdgeLabel(e.getEdgeLabel());
            vo.setWeight(e.getWeight());
            vo.setDescription(e.getDescription());
            return vo;
        }).collect(Collectors.toList());

        // 组装结果
        KgGraphVO graphVO = new KgGraphVO();
        graphVO.setNodes(nodeVOs);
        graphVO.setEdges(edgeVOs);
        graphVO.setTotalVertices(vertices.size());
        graphVO.setTotalEdges(allEdges.size());

        KgCategory cat = categoryMapper.selectById(categoryId);
        if (cat != null) graphVO.setCategoryName(cat.getName());

        return graphVO;
    }

    @Override
    public KgGraphVO getSubGraph(Long vertexId, int depth) {
        // BFS 获取指定深度内的子图
        Set<Long> visited = new HashSet<>();
        Queue<long[]> queue = new LinkedList<>();
        queue.offer(new long[]{vertexId, 0});
        visited.add(vertexId);

        while (!queue.isEmpty()) {
            long[] current = queue.poll();
            long currentId = current[0];
            int currentDepth = (int) current[1];

            if (currentDepth >= depth) continue;

            // 查询当前顶点的邻居
            List<KgEdge> neighbors = edgeMapper.selectList(
                    new LambdaQueryWrapper<KgEdge>()
                            .eq(KgEdge::getFromId, currentId)
                            .or()
                            .eq(KgEdge::getToId, currentId)
            );

            for (KgEdge e : neighbors) {
                long neighborId = e.getFromId() == currentId ? e.getToId() : e.getFromId();
                if (!visited.contains(neighborId)) {
                    visited.add(neighborId);
                    queue.offer(new long[]{neighborId, currentDepth + 1});
                }
            }
        }

        // 查询所有涉及的顶点
        List<KgVertex> vertices = vertexMapper.selectBatchIds(visited);
        Set<Long> vertexIds = vertices.stream().map(KgVertex::getId).collect(Collectors.toSet());

        // 查询涉及这些顶点的边
        List<KgEdge> edges = edgeMapper.selectList(
                new LambdaQueryWrapper<KgEdge>()
                        .in(KgEdge::getFromId, vertexIds)
                        .or()
                        .in(KgEdge::getToId, vertexIds)
        );

        // 构建结果
        Map<Long, String> nameMap = vertices.stream()
                .collect(Collectors.toMap(KgVertex::getId, KgVertex::getName));

        Map<Long, Integer> edgeCountMap = new HashMap<>();
        for (KgEdge e : edges) {
            edgeCountMap.merge(e.getFromId(), 1, Integer::sum);
            edgeCountMap.merge(e.getToId(), 1, Integer::sum);
        }

        List<KgVertexVO> nodeVOs = vertices.stream().map(v -> {
            KgVertexVO vo = new KgVertexVO();
            vo.setId(v.getId().longValue());
            vo.setUuid(v.getUuid());
            vo.setName(v.getName());
            vo.setVertexType(v.getVertexType());
            vo.setSubType(v.getSubType());
            vo.setDescription(v.getDescription());
            vo.setProperties(v.getProperties());
            vo.setEdgeCount(edgeCountMap.getOrDefault(v.getId(), 0));
            return vo;
        }).collect(Collectors.toList());

        List<KgEdgeVO> edgeVOs = edges.stream().map(e -> {
            KgEdgeVO vo = new KgEdgeVO();
            vo.setId(e.getId().longValue());
            vo.setUuid(e.getUuid());
            vo.setFromId(e.getFromId());
            vo.setToId(e.getToId());
            vo.setFromName(nameMap.get(e.getFromId()));
            vo.setToName(nameMap.get(e.getToId()));
            vo.setEdgeLabel(e.getEdgeLabel());
            vo.setWeight(e.getWeight());
            vo.setDescription(e.getDescription());
            return vo;
        }).collect(Collectors.toList());

        KgGraphVO graphVO = new KgGraphVO();
        graphVO.setNodes(nodeVOs);
        graphVO.setEdges(edgeVOs);
        graphVO.setTotalVertices(vertices.size());
        graphVO.setTotalEdges(edges.size());
        return graphVO;
    }

    @Override
    public Map<String, Object> getStatistics(Long categoryId) {
        // 统计顶点数
        long vertexCount = vertexMapper.selectCount(
                new LambdaQueryWrapper<KgVertex>()
                        .eq(KgVertex::getCategoryId, categoryId)
                        .eq(KgVertex::getDeleted, 0)
        );

        // 获取该分类下所有顶点ID
        List<KgVertex> vertices = vertexMapper.selectList(
                new LambdaQueryWrapper<KgVertex>()
                        .eq(KgVertex::getCategoryId, categoryId)
                        .eq(KgVertex::getDeleted, 0)
                        .select(KgVertex::getId)
        );
        Set<Long> vertexIds = vertices.stream().map(KgVertex::getId).collect(Collectors.toSet());

        // 统计边数
        long edgeCount = 0;
        if (!vertexIds.isEmpty()) {
            edgeCount = edgeMapper.selectCount(
                    new LambdaQueryWrapper<KgEdge>()
                            .in(KgEdge::getFromId, vertexIds)
                            .or()
                            .in(KgEdge::getToId, vertexIds)
            );
        }

        // 按顶点类型统计
        List<Map<String, Object>> typeStats = vertexMapper.selectMaps(
                new LambdaQueryWrapper<KgVertex>()
                        .eq(KgVertex::getCategoryId, categoryId)
                        .eq(KgVertex::getDeleted, 0)
                        .select(KgVertex::getVertexType, KgVertex::getId)
                        .groupBy(KgVertex::getVertexType)
        );

        Map<String, Object> stats = new HashMap<>();
        stats.put("vertexCount", vertexCount);
        stats.put("edgeCount", edgeCount);
        stats.put("typeDistribution", typeStats);

        KgCategory cat = categoryMapper.selectById(categoryId);
        if (cat != null) stats.put("categoryName", cat.getName());

        return stats;
    }
}
