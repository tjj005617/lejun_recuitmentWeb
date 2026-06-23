package com.interview.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.interview.domain.po.KgAdjacency;
import com.interview.domain.po.KgCategory;
import com.interview.domain.po.KgDocument;
import com.interview.domain.po.KgVertex;
import com.interview.domain.vo.KgVertexVO;
import com.interview.mapper.KgAdjacencyMapper;
import com.interview.mapper.KgCategoryMapper;
import com.interview.mapper.KgDocumentMapper;
import com.interview.mapper.KgVertexMapper;
import com.interview.service.KgVertexService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 知识图谱顶点服务实现
 */
@Service
@RequiredArgsConstructor
public class KgVertexServiceImpl implements KgVertexService {

    private final KgVertexMapper vertexMapper;
    private final KgDocumentMapper documentMapper;
    private final KgCategoryMapper categoryMapper;
    private final KgAdjacencyMapper adjacencyMapper;

    @Override
    public List<KgVertexVO> listByCategory(Long categoryId) {
        List<KgVertex> vertices = vertexMapper.selectList(
                new LambdaQueryWrapper<KgVertex>()
                        .eq(KgVertex::getCategoryId, categoryId)
                        .eq(KgVertex::getDeleted, 0)
                        .orderByAsc(KgVertex::getName)
        );
        return toVOList(vertices);
    }

    @Override
    public List<KgVertexVO> searchByName(String keyword, Long categoryId) {
        LambdaQueryWrapper<KgVertex> wrapper = new LambdaQueryWrapper<KgVertex>()
                .eq(KgVertex::getDeleted, 0)
                .like(KgVertex::getName, keyword);
        if (categoryId != null) {
            wrapper.eq(KgVertex::getCategoryId, categoryId);
        }
        wrapper.last("LIMIT 50");
        return toVOList(vertexMapper.selectList(wrapper));
    }

    @Override
    public KgVertexVO getById(Long id) {
        KgVertex vertex = vertexMapper.selectById(id);
        if (vertex == null) return null;
        List<KgVertexVO> voList = toVOList(List.of(vertex));
        return voList.isEmpty() ? null : voList.get(0);
    }

    @Override
    public List<KgVertexVO> getNeighbors(Long vertexId) {
        // 从邻接缓存表查询一跳邻居
        List<KgAdjacency> adjacencies = adjacencyMapper.selectList(
                new LambdaQueryWrapper<KgAdjacency>()
                        .eq(KgAdjacency::getVertexId, vertexId)
        );
        if (adjacencies.isEmpty()) return Collections.emptyList();

        // 获取邻居顶点ID集合
        Set<Long> neighborIds = adjacencies.stream()
                .map(KgAdjacency::getNeighborId)
                .collect(Collectors.toSet());

        // 查询邻居顶点详情
        List<KgVertex> neighbors = vertexMapper.selectBatchIds(neighborIds);
        return toVOList(neighbors);
    }

    /**
     * 将顶点实体列表转换为VO列表，补充文档标题和分类名称
     */
    private List<KgVertexVO> toVOList(List<KgVertex> vertices) {
        if (vertices.isEmpty()) return Collections.emptyList();

        // 批量查询关联的文档和分类
        Set<Long> docIds = vertices.stream().map(KgVertex::getDocumentId).collect(Collectors.toSet());
        Set<Long> catIds = vertices.stream().map(KgVertex::getCategoryId).collect(Collectors.toSet());

        Map<Long, KgDocument> docMap = docIds.isEmpty() ? Map.of()
                : documentMapper.selectBatchIds(docIds).stream()
                .collect(Collectors.toMap(KgDocument::getId, d -> d));
        Map<Long, KgCategory> catMap = catIds.isEmpty() ? Map.of()
                : categoryMapper.selectBatchIds(catIds).stream()
                .collect(Collectors.toMap(KgCategory::getId, c -> c));

        return vertices.stream().map(v -> {
            KgVertexVO vo = new KgVertexVO();
            BeanUtil.copyProperties(v, vo);
            KgDocument doc = docMap.get(v.getDocumentId());
            if (doc != null) vo.setDocumentTitle(doc.getTitle());
            KgCategory cat = catMap.get(v.getCategoryId());
            if (cat != null) vo.setCategoryName(cat.getName());
            return vo;
        }).collect(Collectors.toList());
    }
}
